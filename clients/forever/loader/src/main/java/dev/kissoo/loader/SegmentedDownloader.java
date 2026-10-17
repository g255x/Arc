package dev.kissoo.loader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

final class SegmentedDownloader {
    private static final int MIN_SEGMENTED_SIZE = 8 * 1024 * 1024;
    private static final int MAX_WORKERS = 8;
    private static final int MAX_RETRIES = 4;
    private static final int BUFFER_SIZE = 256 * 1024;
    private static final long UI_UPDATE_INTERVAL_NANOS = 140_000_000L;
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private SegmentedDownloader() {
    }

    record Result(String packageSha256, long bytes) {
    }

    static Result download(String url, Path out, Downloader.Progress progress, String label) throws Exception {
        Files.createDirectories(out.getParent());
        Probe probe = probe(url);
        if (probe.status() == 409) throw new StatusException(409, probe.body());
        if (probe.status() >= 400) throw new IOException("HTTP " + probe.status());

        String sha = probe.header("x-forever-package-sha256");
        long total = probe.contentLength();
        boolean canSegment = probe.acceptRanges()
                && total >= MIN_SEGMENTED_SIZE
                && total < Long.MAX_VALUE
                && !DownloadPause.isPaused();
        if (!canSegment) {
            return single(url, out, progress, label, sha, total);
        }

        Path workDir = out.resolveSibling(out.getFileName().toString() + ".parts");
        Files.createDirectories(workDir);
        AtomicLong done = new AtomicLong(existingBytes(workDir, total, segmentCount(total)));
        AtomicLong lastUiUpdate = new AtomicLong(0L);
        progress(progress, label, done.get(), total, lastUiUpdate, true);

        ExecutorService executor = Executors.newFixedThreadPool(segmentCount(total), new ThreadFactory() {
            private final AtomicLong id = new AtomicLong();

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "forever-segment-download-" + id.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        });
        try {
            int segments = segmentCount(total);
            List<Future<?>> futures = new ArrayList<>();
            long chunk = (total + segments - 1) / segments;
            for (int i = 0; i < segments; i++) {
                long start = i * chunk;
                long end = Math.min(total - 1, start + chunk - 1);
                Path part = workDir.resolve("part-" + i);
                futures.add(executor.submit((java.util.concurrent.Callable<Void>) () -> {
                    segment(url, part, start, end, done, total, progress, label, lastUiUpdate);
                    return null;
                }));
            }
            for (Future<?> f : futures) {
                f.get();
            }
        } catch (Exception e) {
            if (rootCause(e) instanceof RangeUnavailableException) {
                deleteQuietly(workDir);
                return single(url, out, progress, label, sha, total);
            }
            throw e;
        } finally {
            executor.shutdownNow();
        }

        Path tmp = out.resolveSibling(out.getFileName().toString() + ".tmp");
        try (OutputStream output = Files.newOutputStream(tmp)) {
            int segments = segmentCount(total);
            for (int i = 0; i < segments; i++) {
                Path part = workDir.resolve("part-" + i);
                Files.copy(part, output);
            }
        }
        if (Files.size(tmp) != total) {
            throw new IOException("download size mismatch");
        }
        Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING);
        deleteQuietly(workDir);
        progress(progress, label, total, total, lastUiUpdate, true);
        return new Result(sha, total);
    }

    private static Probe probe(String url) throws IOException, InterruptedException {
        HttpRequest req = base(url).method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        long len = resp.headers().firstValueAsLong("content-length").orElse(-1L);
        boolean ranges = resp.headers().firstValue("accept-ranges")
                .map(v -> v.toLowerCase(Locale.ROOT).contains("bytes"))
                .orElse(false);
        return new Probe(resp.statusCode(), len, ranges, resp.headers(), resp.body());
    }

    private static Result single(String url, Path out, Downloader.Progress progress, String label, String knownSha, long knownTotal) throws Exception {
        Path tmp = out.resolveSibling(out.getFileName().toString() + ".tmp");
        Files.deleteIfExists(tmp);
        HttpResponse<InputStream> resp = HTTP.send(base(url).GET().build(), HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() == 409) throw new StatusException(409, readSmall(resp.body()));
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) throw new IOException("HTTP " + resp.statusCode());
        String sha = resp.headers().firstValue("x-forever-package-sha256").orElse(knownSha == null ? "" : knownSha);
        long total = resp.headers().firstValueAsLong("content-length").orElse(knownTotal);
        long done = 0;
        AtomicLong lastUiUpdate = new AtomicLong(0L);
        try (InputStream in = resp.body(); OutputStream os = Files.newOutputStream(tmp)) {
            byte[] buf = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buf)) > 0) {
                DownloadPause.awaitIfPaused();
                os.write(buf, 0, read);
                done += read;
                progress(progress, label, done, total, lastUiUpdate, false);
            }
        }
        progress(progress, label, total > 0 ? total : done, total > 0 ? total : done, lastUiUpdate, true);
        Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING);
        return new Result(sha == null ? "" : sha.trim().toLowerCase(Locale.ROOT), done);
    }

    private static void segment(String url, Path part, long start, long end, AtomicLong done, long total,
                                Downloader.Progress progress, String label, AtomicLong lastUiUpdate) throws Exception {
        long expected = end - start + 1;
        long existing = 0;
        if (Files.exists(part)) {
            long size = Files.size(part);
            if (size > expected) {
                Files.deleteIfExists(part);
            } else {
                existing = size;
            }
        }
        if (existing >= expected) return;
        long from = start + existing;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                HttpRequest req = base(url).header("Range", "bytes=" + from + "-" + end).GET().build();
                HttpResponse<InputStream> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofInputStream());
                if (resp.statusCode() == 200) throw new RangeUnavailableException();
                if (resp.statusCode() < 200 || resp.statusCode() >= 300) throw new IOException("HTTP " + resp.statusCode());
                try (InputStream in = resp.body();
                     OutputStream os = Files.newOutputStream(part, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND)) {
                    byte[] buf = new byte[BUFFER_SIZE];
                    int read;
                    while ((read = in.read(buf)) > 0) {
                        DownloadPause.awaitIfPaused();
                        os.write(buf, 0, read);
                        progress(progress, label, done.addAndGet(read), total, lastUiUpdate, false);
                    }
                }
                if (Files.size(part) >= expected) return;
                from = start + Files.size(part);
            } catch (RangeUnavailableException e) {
                throw e;
            } catch (Exception e) {
                if (attempt == MAX_RETRIES) throw e;
                Thread.sleep(450L * attempt);
                from = start + (Files.exists(part) ? Math.min(Files.size(part), expected) : 0);
            }
        }
    }

    private static HttpRequest.Builder base(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(5))
                .header("User-Agent", LoaderSecrets.userAgent());
    }

    private static int segmentCount(long total) {
        if (total < 64L * 1024L * 1024L) return 4;
        return MAX_WORKERS;
    }

    private static long existingBytes(Path workDir, long total, int segments) {
        long chunk = (total + segments - 1) / segments;
        long sum = 0;
        for (int i = 0; i < segments; i++) {
            try {
                long start = i * chunk;
                long end = Math.min(total - 1, start + chunk - 1);
                long expected = end - start + 1;
                sum += Math.min(Files.size(workDir.resolve("part-" + i)), expected);
            } catch (Exception ignored) {
            }
        }
        return sum;
    }

    private static void progress(Downloader.Progress progress, String label, long done, long total,
                                 AtomicLong lastUiUpdate, boolean force) {
        if (progress == null || total <= 0) return;
        long now = System.nanoTime();
        long last = lastUiUpdate.get();
        if (!force && now - last < UI_UPDATE_INTERVAL_NANOS) return;
        if (!force && !lastUiUpdate.compareAndSet(last, now)) return;
        if (force) lastUiUpdate.set(now);
        int pct = (int) Math.min(100, Math.max(0, done * 100 / total));
        progress.update(12 + (int) Math.min(58, pct * 58L / 100L), label + " " + pct + "%");
    }

    private static String readSmall(InputStream in) {
        try (in) {
            return new String(in.readNBytes(8192), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "";
        }
    }

    private static Throwable rootCause(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null) cur = cur.getCause();
        return cur;
    }

    private static void deleteQuietly(Path path) {
        try {
            if (!Files.exists(path)) return;
            try (java.util.stream.Stream<Path> s = Files.walk(path)) {
                s.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception ignored) {
                    }
                });
            }
        } catch (Exception ignored) {
        }
    }

    static final class StatusException extends IOException {
        private final int status;
        private final String body;

        StatusException(int status, String body) {
            super("HTTP " + status);
            this.status = status;
            this.body = body == null ? "" : body;
        }

        int status() {
            return status;
        }

        String body() {
            return body;
        }
    }

    private record Probe(int status, long contentLength, boolean acceptRanges,
                         java.net.http.HttpHeaders headers, String body) {
        String header(String name) {
            return headers.firstValue(name).orElse("").trim().toLowerCase(Locale.ROOT);
        }
    }

    private static final class RangeUnavailableException extends IOException {
    }
}
