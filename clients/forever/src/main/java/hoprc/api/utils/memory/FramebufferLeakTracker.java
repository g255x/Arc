package hoprc.api.utils.memory;

import dev.kissoo.api.utils.Wrapper;
import net.minecraft.client.gl.Framebuffer;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class FramebufferLeakTracker implements Wrapper {
    public static final FramebufferLeakTracker INSTANCE = new FramebufferLeakTracker();

    private final ConcurrentHashMap<Integer, FramebufferRecord> activeFramebuffers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CallSiteStats> callSiteStats = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Framebuffer> pendingDeletion = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<LeakReport> leakReports = new ConcurrentLinkedQueue<>();

    private volatile boolean enabled = true;
    private volatile boolean autoFix = true;
    private volatile boolean logEnabled = false; // 默认关闭调试日志，防止刷屏
    private volatile int maxAlivePerCallSite = 4;
    private volatile long staleTimeoutMs = 15_000;
    private volatile int maxTotalFramebuffers = 32;
    private volatile long scanIntervalMs = 5_000;

    private final AtomicLong totalCreated = new AtomicLong(0);
    private final AtomicLong totalDeleted = new AtomicLong(0);
    private final AtomicLong totalLeaksFixed = new AtomicLong(0);
    private final AtomicLong totalMemoryFreed = new AtomicLong(0);
    private long lastScanTime = 0;

    public void onFramebufferCreated(Framebuffer fb) {
        if (!enabled || fb == null) return;

        int id = System.identityHashCode(fb);
        String callSite = captureCallSite();
        long now = System.currentTimeMillis();

        FramebufferRecord record = new FramebufferRecord(
                new WeakReference<>(fb), id, callSite, now
        );

        activeFramebuffers.put(id, record);
        totalCreated.incrementAndGet();

        CallSiteStats stats = callSiteStats.computeIfAbsent(callSite, CallSiteStats::new);
        stats.totalCreated.incrementAndGet();
        stats.currentAlive.incrementAndGet();
        stats.lastCreateTime = now;

        if (logEnabled) {
            int alive = activeFramebuffers.size();
            log("CREATE [%d alive] @ %s", alive, callSite);
        }

        checkCallSiteOverflow(callSite, fb);
    }

    public void onFramebufferDeleted(Framebuffer fb) {
        if (!enabled || fb == null) return;

        int id = System.identityHashCode(fb);
        FramebufferRecord removed = activeFramebuffers.remove(id);

        if (removed != null) {
            totalDeleted.incrementAndGet();
            String callSite = removed.callSite;

            CallSiteStats stats = callSiteStats.get(callSite);
            if (stats != null) {
                stats.currentAlive.decrementAndGet();
                stats.totalDeleted.incrementAndGet();
            }

            if (logEnabled) {
                long aliveMs = System.currentTimeMillis() - removed.createTime;
                log("DELETE [%d alive] lived %dms @ %s",
                        activeFramebuffers.size(), aliveMs, callSite);
            }
        }
    }

    public void onFramebufferAccessed(Framebuffer fb) {
        if (!enabled || fb == null) return;
        int id = System.identityHashCode(fb);
        FramebufferRecord record = activeFramebuffers.get(id);
        if (record != null) {
            record.lastAccessTime = System.currentTimeMillis();
            record.accessCount++;
        }
    }

    public void tick() {
        if (!enabled) return;
        processPendingDeletions();
        long now = System.currentTimeMillis();
        if (now - lastScanTime >= scanIntervalMs) {
            lastScanTime = now;
            scanForLeaks();
        }
    }

    public void onRenderTick() {
        processPendingDeletions();
    }

    private void scanForLeaks() {
        long now = System.currentTimeMillis();
        Framebuffer mainFb = mc != null ? mc.getFramebuffer() : null;

        int staleCount = 0;
        int gcedCount = 0;
        int totalAlive = 0;
        long totalEstimatedMem = 0;

        List<FramebufferRecord> toRelease = new ArrayList<>();

        Iterator<Map.Entry<Integer, FramebufferRecord>> it = activeFramebuffers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, FramebufferRecord> entry = it.next();
            FramebufferRecord record = entry.getValue();
            Framebuffer fb = record.ref.get();
            if (fb == null) {
                it.remove();
                gcedCount++;
                CallSiteStats stats = callSiteStats.get(record.callSite);
                if (stats != null) {
                    stats.currentAlive.decrementAndGet();
                    stats.gcLeaks.incrementAndGet();
                }

                leakReports.add(new LeakReport(
                        record.callSite, record.getWidth(), record.getHeight(),
                        record.getEstimatedSize(), "GC_WITHOUT_DELETE",
                        now - record.createTime, record.accessCount
                ));

                if (logEnabled) {
                    log("⚠ GC LEAK: (~%dKB) created at %s — Object GC'd but delete() was never called!",
                            record.getEstimatedSize() / 1024, record.callSite);
                }
                continue;
            }
            if (fb == mainFb) {
                record.lastAccessTime = now;
                totalAlive++;
                totalEstimatedMem += record.getEstimatedSize();
                continue;
            }

            totalAlive++;
            totalEstimatedMem += record.getEstimatedSize();
            long idleTime = now - record.lastAccessTime;
            if (idleTime > staleTimeoutMs) {
                staleCount++;
                if (autoFix) {
                    toRelease.add(record);
                } else {
                    leakReports.add(new LeakReport(
                            record.callSite, record.getWidth(), record.getHeight(),
                            record.getEstimatedSize(), "STALE",
                            idleTime, record.accessCount
                    ));
                }
            }
        }

        for (FramebufferRecord record : toRelease) {
            Framebuffer fb = record.ref.get();
            if (fb != null && fb != mainFb) {
                activeFramebuffers.remove(record.id);
                pendingDeletion.add(fb);
                totalLeaksFixed.incrementAndGet();
                totalMemoryFreed.addAndGet(record.getEstimatedSize());

                CallSiteStats stats = callSiteStats.get(record.callSite);
                if (stats != null) {
                    stats.currentAlive.decrementAndGet();
                    stats.autoFixed.incrementAndGet();
                }

                leakReports.add(new LeakReport(
                        record.callSite, record.getWidth(), record.getHeight(),
                        record.getEstimatedSize(), "AUTO_FIXED",
                        now - record.lastAccessTime, record.accessCount
                ));

                if (logEnabled) {
                    log("🔧 AUTO-FIX: Released stale %dx%d (~%dKB) idle %ds @ %s",
                            record.getWidth(), record.getHeight(), record.getEstimatedSize() / 1024,
                            (now - record.lastAccessTime) / 1000, record.callSite);
                }
            }
        }

        if (autoFix && totalAlive > maxTotalFramebuffers) {
            releaseExcessFramebuffers(mainFb, totalAlive - maxTotalFramebuffers);
        }
    }

    private void checkCallSiteOverflow(String callSite, Framebuffer newFb) {
        if (!autoFix) return;

        CallSiteStats stats = callSiteStats.get(callSite);
        if (stats == null) return;

        int alive = stats.currentAlive.get();
        if (alive <= maxAlivePerCallSite) return;
        Framebuffer mainFb = mc != null ? mc.getFramebuffer() : null;

        List<FramebufferRecord> sameSite = new ArrayList<>();
        for (FramebufferRecord record : activeFramebuffers.values()) {
            if (record.callSite.equals(callSite)) {
                Framebuffer fb = record.ref.get();
                if (fb != null && fb != mainFb && fb != newFb) {
                    sameSite.add(record);
                }
            }
        }
        sameSite.sort(Comparator.comparingLong(r -> r.createTime));
        int toRemove = alive - maxAlivePerCallSite;

        for (int i = 0; i < Math.min(toRemove, sameSite.size()); i++) {
            FramebufferRecord old = sameSite.get(i);
            Framebuffer fb = old.ref.get();
            if (fb != null) {
                activeFramebuffers.remove(old.id);
                pendingDeletion.add(fb);
                totalLeaksFixed.incrementAndGet();
                totalMemoryFreed.addAndGet(old.getEstimatedSize());
                stats.currentAlive.decrementAndGet();
                stats.autoFixed.incrementAndGet();

                if (logEnabled) {
                    log("🔧 OVERFLOW-FIX: %s has %d alive (max %d), releasing created %dms ago",
                            callSite, alive, maxAlivePerCallSite,
                            System.currentTimeMillis() - old.createTime);
                }
            }
        }
    }

    private void releaseExcessFramebuffers(Framebuffer mainFb, int count) {
        List<FramebufferRecord> candidates = new ArrayList<>();
        for (FramebufferRecord record : activeFramebuffers.values()) {
            Framebuffer fb = record.ref.get();
            if (fb != null && fb != mainFb) {
                candidates.add(record);
            }
        }

        candidates.sort(Comparator.comparingLong(r -> r.lastAccessTime));

        for (int i = 0; i < Math.min(count, candidates.size()); i++) {
            FramebufferRecord record = candidates.get(i);
            Framebuffer fb = record.ref.get();
            if (fb != null) {
                activeFramebuffers.remove(record.id);
                pendingDeletion.add(fb);
                totalLeaksFixed.incrementAndGet();
                totalMemoryFreed.addAndGet(record.getEstimatedSize());

                CallSiteStats stats = callSiteStats.get(record.callSite);
                if (stats != null) {
                    stats.currentAlive.decrementAndGet();
                    stats.autoFixed.incrementAndGet();
                }

                if (logEnabled) {
                    log("🔧 GLOBAL-LIMIT: Releasing %dx%d @ %s", record.getWidth(), record.getHeight(), record.callSite);
                }
            }
        }
    }

    private void processPendingDeletions() {
        if (mc == null || !mc.isOnThread()) return;

        Framebuffer fb;
        while ((fb = pendingDeletion.poll()) != null) {
            try {
                fb.delete();
            } catch (Exception e) {
                if (logEnabled) {
                    log("Error deleting framebuffer: %s", e.getMessage());
                }
            }
        }
    }

    private String captureCallSite() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        StringBuilder sb = new StringBuilder();
        int found = 0;

        for (int i = 2; i < stack.length && found < 3; i++) {
            String cls = stack[i].getClassName();
            String method = stack[i].getMethodName();
            if (cls.contains("FramebufferLeakTracker")) continue;
            if (cls.contains("mixin") || cls.contains("Mixin")) continue;
            if (cls.contains("$") && cls.contains("mixin")) continue;
            if (cls.startsWith("java.") || cls.startsWith("sun.")) continue;
            if (cls.contains("Framebuffer") && (method.equals("<init>") || method.equals("init"))) continue;

            if (found > 0) sb.append(" <- ");
            String simpleName = cls.substring(cls.lastIndexOf('.') + 1);
            sb.append(simpleName).append(".").append(method).append(":").append(stack[i].getLineNumber());
            found++;
        }

        return found > 0 ? sb.toString() : "Unknown";
    }

    private static void log(String fmt, Object... args) {
        System.out.println("[MemoryFix] " + String.format(fmt, args));
    }

    private static class FramebufferRecord {
        final WeakReference<Framebuffer> ref;
        final int id;
        final String callSite;
        final long createTime;
        volatile long lastAccessTime;
        volatile int accessCount;

        FramebufferRecord(WeakReference<Framebuffer> ref, int id, String callSite, long createTime) {
            this.ref = ref;
            this.id = id;
            this.callSite = callSite;
            this.createTime = createTime;
            this.lastAccessTime = createTime;
            this.accessCount = 0;
        }

        public int getWidth() {
            Framebuffer fb = ref.get();
            return fb != null ? fb.textureWidth : 0;
        }

        public int getHeight() {
            Framebuffer fb = ref.get();
            return fb != null ? fb.textureHeight : 0;
        }

        public long getEstimatedSize() {
            return (long) getWidth() * getHeight() * 8;
        }
    }

    private static class CallSiteStats {
        final String callSite;
        final AtomicInteger totalCreated = new AtomicInteger(0);
        final AtomicInteger totalDeleted = new AtomicInteger(0);
        final AtomicInteger currentAlive = new AtomicInteger(0);
        final AtomicInteger gcLeaks = new AtomicInteger(0);
        final AtomicInteger autoFixed = new AtomicInteger(0);
        volatile long lastCreateTime;

        CallSiteStats(String callSite) {
            this.callSite = callSite;
        }
    }

    public static class LeakReport {
        public final String callSite;
        public final int width, height;
        public final long estimatedSize;
        public final String type;
        public final long idleTimeMs;
        public final int accessCount;
        public final long timestamp;

        LeakReport(String callSite, int width, int height, long estimatedSize,
                   String type, long idleTimeMs, int accessCount) {
            this.callSite = callSite;
            this.width = width;
            this.height = height;
            this.estimatedSize = estimatedSize;
            this.type = type;
            this.idleTimeMs = idleTimeMs;
            this.accessCount = accessCount;
            this.timestamp = System.currentTimeMillis();
        }
    }
}