package dev.kissoo.proxy;

import dev.kissoo.core.impl.CommandManager;
import dev.kissoo.Forever;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Mod-side manager for a bundled-but-external ZenithProxy instance.
 *
 * <p>ZenithProxy implements the full Minecraft protocol (world cache + ViaVersion),
 * so it — not us — holds the queue position while the MC client is closed and
 * restores the session seamlessly on reconnect. Our jobs:</p>
 * <ol>
 *   <li>Ship ZenithProxy's launcher inside the mod and extract it per-user.</li>
 *   <li>Launch it as an independent process ({@code cmd /c launch.bat}) that
 *       survives MC closing (so it keeps holding the queue).</li>
 *   <li>Surface a live status (install / login / connecting / ready) so the UI
 *       can guide the user through the one-time per-account login.</li>
 *   <li>Redirect MC's connection to the target server onto localhost.</li>
 * </ol>
 *
 * <p>We deliberately never stop ZenithProxy when MC closes.</p>
 */
public final class ProxyManager {

    private ProxyManager() {}

    /** Lifecycle state, surfaced to {@link ZenithStatusScreen}. */
    public enum State { STOPPED, INSTALLING, STARTING, LOGIN_REQUIRED, CONNECTING, READY, FAILED }

    // Bundled template (extracted into the per-user install dir). Order matters:
    // config.json last so a half-extract still re-triggers on next run.
    private static final String RES_BASE = "assets/forever/zenithproxy/";
    private static final String[] TEMPLATE_FILES = {
            "launch.bat", "launcher-py.zip", "requirements.txt", "launch_config.json", "config.json"
    };

    // ── Loaded configuration (forever/zenith.properties, distribution defaults) ─
    private static boolean enabled       = true;
    private static String  targetHost    = "2b2t.xin";
    private static int     targetPort    = 25565;
    private static int     listenPort    = 25565;
    private static String  zenithDirCfg  = "";          // blank → <gameDir>/zenithproxy
    private static String  launchCommand = "launch.bat";
    private static boolean configLoaded  = false;

    // ── Runtime state ───────────────────────────────────────────────────────
    private static volatile State   state     = State.STOPPED;
    private static volatile String  loginUrl  = "";
    private static volatile String  loginCode = "";
    private static volatile Process proc;
    private static volatile boolean starting  = false;
    private static volatile boolean watcherUp = false;
    /** Set by ZenithChoiceScreen when user picks "直接连接"; consumed once by zenithGate. */
    private static volatile boolean directOnce = false;

    // ── Public API ──────────────────────────────────────────────────────────

    /** Called by {@link ZenithChoiceScreen} to allow the next connection through the gate. */
    public static void requestDirectConnect() { directOnce = true; }

    /** Consumed by zenithGate: returns true and resets the flag. */
    public static boolean consumeDirectOnce() {
        if (directOnce) { directOnce = false; return true; }
        return false;
    }

    /** True if {@code host:port} is the server we tunnel through ZenithProxy. */
    public static boolean isProxiedTarget(String host, int port) {
        loadConfigOnce();
        return enabled && host != null && host.equalsIgnoreCase(targetHost) && port == targetPort;
    }

    /**
     * Returns the local proxy address when the proxy is READY, otherwise returns
     * the original address unchanged. The Mixin's @ModifyArg calls this every
     * time; when the user chose direct connect the gate already let them through
     * so redirectIfProxied is not called in that path at all.
     */
    public static String redirectIfProxied(String host, int port) {
        if (!isProxiedTarget(host, port)) return host + ":" + port;
        if (!isReady()) return host + ":" + port;
        return "localhost:" + listenPort;
    }

    /** ZenithProxy is up AND connected to the target server (safe to join). */
    public static boolean isReady() {
        return state == State.READY && isPortOpen(listenPort);
    }

    public static State  getState()    { return state; }
    public static String getLoginUrl() { return loginUrl; }
    public static String getLoginCode(){ return loginCode; }
    public static int    getListenPort() { loadConfigOnce(); return listenPort; }
    public static String getTargetHost() { loadConfigOnce(); return targetHost; }
    public static int    getTargetPort() { loadConfigOnce(); return targetPort; }
    public static boolean isEnabled()    { loadConfigOnce(); return enabled; }

    /**
     * Idempotently kick off installation + launch in the background, and ensure
     * the log watcher is running. Returns immediately; poll {@link #getState()} /
     * {@link #isReady()} for progress.
     */
    public static void ensureStartedAsync() {
        loadConfigOnce();
        if (!enabled) return;
        if (state == State.FAILED) return; // don't retry after permanent failure
        ensureWatcher();
        if (starting) return;
        if (isPortOpen(listenPort)) return; // already running (this or a prior session)
        starting = true;
        Forever.THREAD.execute(ProxyManager::startSequence);
    }

    /**
     * 停止代理、删除 MSA token 缓存文件，并重置状态。
     * 下次调用 ensureStartedAsync() 会重新走 device_code 登录流程。
     */
    public static synchronized void switchAccount() {
        stopProxy();
        // 重置启动标志，让 ensureStartedAsync 可以重新启动
        starting  = false;
        watcherUp = false;
        state     = State.STOPPED;
        loginUrl  = "";
        loginCode = "";

        // 删除 ZenithProxy 存储的 MSA token 缓存
        // 实际文件名为 mc_auth_cache.json（位于安装目录根下）
        Path dir = installDir();
        String[] cacheFiles = {
            "mc_auth_cache.json",
            "MSAcache.json",      // 旧版本兼容
            "accounts.json",
        };
        for (String rel : cacheFiles) {
            Path f = dir.resolve(rel);
            try {
                if (Files.deleteIfExists(f)) {
                    System.out.println("[ProxyManager] deleted auth cache: " + f);
                }
            } catch (IOException e) {
                System.err.println("[ProxyManager] failed to delete " + f + ": " + e.getMessage());
            }
        }
    }

    /** Forcefully stop the proxy and its entire process tree. */
    public static synchronized void stopProxy() {
        loadConfigOnce();
        Process p = proc;
        proc = null;
        starting = false;
        loginUrl = "";
        loginCode = "";
        state = State.STOPPED;
        if (p == null || !p.isAlive()) {
            if (isWindows() && isPortOpen(listenPort)) {
                killWindowsZenithListeners();
                waitForPortClose();
            }
            return;
        }

        long pid = p.pid();
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        boolean killedByWindows = false;
        if (isWindows) {
            // taskkill /F /T kills the entire process tree (cmd → python → ZenithProxy).
            // Process.destroy() on Windows only kills cmd.exe and leaves children running.
            try {
                killedByWindows = killWindowsTree(pid);
                waitForPortClose();
            } catch (Exception e) {
                System.err.println("[ProxyManager] taskkill failed, falling back: " + e.getMessage());
            }
        }
        if (!killedByWindows) {
            // Fallback / non-Windows: kill descendants first, then the root.
            p.descendants().forEach(ProcessHandle::destroyForcibly);
            p.destroyForcibly();
        }
        if (isWindows && isPortOpen(listenPort)) {
            killWindowsZenithListeners();
            waitForPortClose();
        }
    }

    // ── Start sequence (background thread) ──────────────────────────────────

    private static void startSequence() {
        try {
            if (isPortOpen(listenPort)) return; // race: someone else started it
            setState(State.INSTALLING);
            ensureInstalled();
            setState(State.STARTING);
            launchProcess();
        } catch (Exception e) {
            setState(State.FAILED);
            log("§c代理启动失败: " + e.getMessage());
            System.err.println("[ProxyManager] start failed: " + e);
        } finally {
            starting = false;
        }
    }

    private static void ensureInstalled() throws IOException {
        Path dir = installDir();
        Files.createDirectories(dir);
        for (String name : TEMPLATE_FILES) {
            Path target = dir.resolve(name);
            boolean refresh = name.equals("launch.bat");
            if (Files.exists(target) && !refresh) continue; // never overwrite config/login data
            try (InputStream in = ProxyManager.class.getClassLoader()
                    .getResourceAsStream(RES_BASE + name)) {
                if (in == null) {
                    throw new IOException("bundled resource missing: " + RES_BASE + name);
                }
                if (refresh) {
                    Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("[ProxyManager] refreshed " + name + " -> " + target);
                    continue;
                }
                Files.copy(in, target);
            }
            System.out.println("[ProxyManager] extracted " + name + " -> " + target);
        }
    }

    private static void launchProcess() throws IOException {
        Path dir = installDir();
        String cmd = launchCommand == null || launchCommand.isBlank() ? "launch.bat" : launchCommand.trim();
        List<String> raw = new ArrayList<>();
        for (String part : cmd.split("\\s+")) {
            if (!part.isEmpty()) raw.add(part);
        }
        if (raw.isEmpty()) {
            throw new IOException("launchCommand is empty or invalid: " + launchCommand);
        }

        // Windows .bat/.cmd must run through the command interpreter.
        List<String> cmdList = new ArrayList<>();
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        String first = raw.get(0).toLowerCase();
        if (isWindows && (first.endsWith(".bat") || first.endsWith(".cmd"))) {
            cmdList.add("cmd");
            cmdList.add("/c");
        }
        cmdList.addAll(raw);

        Path logDir = dir.resolve("log");
        Files.createDirectories(logDir);
        Path launchLog = logDir.resolve("mod-launch.log");

        ProcessBuilder pb = new ProcessBuilder(cmdList);
        pb.directory(dir.toFile());
        pb.redirectErrorStream(true);
        // Redirect to a FILE (not a pipe) so the child does not depend on the MC
        // process and keeps running — holding the queue — after MC closes.
        pb.redirectOutput(launchLog.toFile());

        System.out.println("[ProxyManager] launching: " + String.join(" ", cmdList)
                + "  (cwd=" + dir + ")");
        proc = pb.start();
    }

    // ── Log watcher: derive state from ZenithProxy's output ─────────────────

    private static synchronized void ensureWatcher() {
        if (watcherUp) return;
        watcherUp = true;
        Thread t = new Thread(ProxyManager::watchLoop, "zenith-status-watch");
        t.setDaemon(true);
        t.start();
    }

    private static void watchLoop() {
        while (true) {
            try {
                updateStateFromLogs();
            } catch (Exception ignored) {}
            try { Thread.sleep(1000); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * Scan the tail of our launch log (preferred — it captures launcher + proxy
     * stdout) and ZenithProxy's own latest.log, and set state from the last
     * meaningful marker seen.
     */
    private static void updateStateFromLogs() {
        // Don't override an intentional stop from the old log contents.
        if (state == State.STOPPED) return;

        Path dir = installDir();
        String text = readTail(dir.resolve("log").resolve("mod-launch.log"), 64 * 1024);
        if (text.isEmpty()) {
            text = readTail(dir.resolve("log").resolve("latest.log"), 64 * 1024);
        }
        if (text.isEmpty()) return;

        State newState = null;
        String url = null, code = null;
        String connectedMarker = "Connected to " + targetHost;

        for (String line : text.split("\n")) {
            int li = line.indexOf("Login Here:");
            if (li >= 0) {
                url = line.substring(li + "Login Here:".length()).trim();
                newState = State.LOGIN_REQUIRED;
                continue;
            }
            int ci = line.indexOf("Code:");
            if (ci >= 0 && newState == State.LOGIN_REQUIRED) {
                code = line.substring(ci + "Code:".length()).trim();
                continue;
            }
            if (line.contains(connectedMarker)) { newState = State.READY; continue; }
            if (line.contains("Connecting to " + targetHost)) { newState = State.CONNECTING; continue; }
        }

        if (url != null)  loginUrl  = url;
        if (code != null) loginCode = code;
        if (newState != null && newState != state) {
            setState(newState);
        }
    }

    // ── Config loading ──────────────────────────────────────────────────────

    private static synchronized void loadConfigOnce() {
        if (configLoaded) return;
        configLoaded = true;
        Path cfg = gameDir().resolve("forever").resolve("zenith.properties");
        try {
            if (Files.notExists(cfg)) {
                writeDefaultConfig(cfg);
            } else {
                Properties p = new Properties();
                try (InputStream in = Files.newInputStream(cfg)) { p.load(in); }
                enabled       = Boolean.parseBoolean(p.getProperty("enabled", "true").trim());
                targetHost    = p.getProperty("targetHost", "2b2t.xin").trim();
                targetPort    = parseInt(p.getProperty("targetPort", "25565"), 25565);
                listenPort    = parseInt(p.getProperty("listenPort", "25565"), 25565);
                zenithDirCfg  = p.getProperty("zenithDir", "").trim();
                launchCommand = p.getProperty("launchCommand", "launch.bat").trim();
            }
            System.out.println("[ProxyManager] config: enabled=" + enabled
                    + " target=" + targetHost + ":" + targetPort + " listen=" + listenPort
                    + " dir=" + installDir());
        } catch (IOException e) {
            System.err.println("[ProxyManager] config read failed: " + e.getMessage());
        }
    }

    private static void writeDefaultConfig(Path cfg) throws IOException {
        Files.createDirectories(cfg.getParent());
        String content = """
                # ── ZenithProxy session-persistence (auto-managed) ───────────────
                # The mod bundles & launches ZenithProxy for you; you only need to
                # complete the one-time browser login with your own account.

                enabled=true

                # Server tunnelled through ZenithProxy.
                targetHost=2b2t.xin
                targetPort=25565

                # Local port ZenithProxy listens on (matches bundled config.json).
                listenPort=25565

                # Install dir. Blank = <gameDir>/zenithproxy (recommended).
                zenithDir=

                # Launch command (run inside the install dir).
                launchCommand=launch.bat
                """;
        Files.writeString(cfg, content);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static Path gameDir() {
        return MinecraftClient.getInstance().runDirectory.toPath();
    }

    private static Path installDir() {
        loadConfigOnce();
        if (zenithDirCfg != null && !zenithDirCfg.isBlank()) return Path.of(zenithDirCfg);
        return gameDir().resolve("zenithproxy");
    }

    private static boolean isPortOpen(int port) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress("127.0.0.1", port), 400);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static boolean killWindowsTree(long pid) {
        try {
            Process taskkill = new ProcessBuilder("taskkill", "/F", "/T", "/PID", String.valueOf(pid))
                    .redirectErrorStream(true)
                    .start();
            boolean exited = taskkill.waitFor(5, TimeUnit.SECONDS);
            return exited && taskkill.exitValue() == 0;
        } catch (Exception e) {
            System.err.println("[ProxyManager] taskkill failed for pid " + pid + ": " + e.getMessage());
            return false;
        }
    }

    private static void killWindowsZenithListeners() {
        Path dir = installDir().toAbsolutePath().normalize();
        for (Long pid : findWindowsListenPids(listenPort)) {
            if (pid == ProcessHandle.current().pid()) continue;
            if (isZenithProcess(pid, dir)) {
                killWindowsTree(pid);
            } else {
                System.err.println("[ProxyManager] skip listener pid " + pid
                        + " because it does not look like " + dir);
            }
        }
    }

    private static Set<Long> findWindowsListenPids(int port) {
        Set<Long> pids = new LinkedHashSet<>();
        String command = "Get-NetTCPConnection -LocalPort " + port
                + " -State Listen -ErrorAction SilentlyContinue"
                + " | Select-Object -ExpandProperty OwningProcess -Unique";
        try {
            Process ps = new ProcessBuilder("powershell", "-NoProfile", "-Command", command)
                    .redirectErrorStream(true)
                    .start();
            String out;
            try (InputStream in = ps.getInputStream()) {
                out = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            ps.waitFor(5, TimeUnit.SECONDS);
            for (String line : out.split("\\R")) {
                try {
                    if (!line.isBlank()) pids.add(Long.parseLong(line.trim()));
                } catch (NumberFormatException ignored) {}
            }
        } catch (Exception e) {
            System.err.println("[ProxyManager] failed to inspect listen port " + port + ": " + e.getMessage());
        }
        return pids;
    }

    private static boolean isZenithProcess(long pid, Path dir) {
        String command = "$p = Get-CimInstance Win32_Process -Filter \"ProcessId=" + pid + "\";"
                + " if ($p) { ($p.ExecutablePath + ' ' + $p.CommandLine) }";
        try {
            Process ps = new ProcessBuilder("powershell", "-NoProfile", "-Command", command)
                    .redirectErrorStream(true)
                    .start();
            String out;
            try (InputStream in = ps.getInputStream()) {
                out = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            ps.waitFor(5, TimeUnit.SECONDS);
            String normalized = out.toLowerCase().replace('/', '\\');
            String dirText = dir.toString().toLowerCase().replace('/', '\\');
            return normalized.contains(dirText)
                    || normalized.contains("launcher-py.zip")
                    || normalized.contains("zenithproxy.jar");
        } catch (Exception e) {
            System.err.println("[ProxyManager] failed to inspect pid " + pid + ": " + e.getMessage());
            return false;
        }
    }

    private static void waitForPortClose() {
        for (int i = 0; i < 10; i++) {
            if (!isPortOpen(listenPort)) return;
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static String readTail(Path file, int maxBytes) {
        try {
            if (Files.notExists(file)) return "";
            long size = Files.size(file);
            byte[] data;
            if (size <= maxBytes) {
                data = Files.readAllBytes(file);
            } else {
                data = new byte[maxBytes];
                try (var ch = Files.newByteChannel(file)) {
                    ch.position(size - maxBytes);
                    java.nio.ByteBuffer buf = java.nio.ByteBuffer.wrap(data);
                    while (buf.hasRemaining() && ch.read(buf) > 0) { /* fill */ }
                }
            }
            return new String(data, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private static void setState(State s) {
        state = s;
        switch (s) {
            case INSTALLING     -> log("§e正在安装排队代理组件…（首次较慢，需下载）");
            case STARTING       -> log("§e正在启动排队代理…");
            case LOGIN_REQUIRED -> log("§b请在浏览器中登录你的正版账号以启用排队代理");
            case CONNECTING     -> log("§e排队代理正在连接 " + targetHost + " …");
            case READY          -> log("§a排队代理已就绪");
            case FAILED         -> log("§c排队代理启动失败，请查看 zenithproxy/log");
            default -> {}
        }
    }

    private static void log(String msg) {
        try { CommandManager.sendMessage(msg); } catch (Throwable ignored) {}
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return def; }
    }
}
