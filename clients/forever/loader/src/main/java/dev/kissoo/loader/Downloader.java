package dev.kissoo.loader;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Downloader {
    private Downloader() {
    }

    interface Progress {
        void update(int percent, String detail);
    }

    static Path downloadPersonalized(ClientProfile client, String token, Path gameDir, Progress progress) {
        Path mods = LoaderPaths.modsDir(gameDir);
        HttpURLConnection conn = null;
        try {
            Files.createDirectories(mods);
            conn = (HttpURLConnection) URI.create(LoaderSecrets.apiBase() + AuthClient.enc("/download-personalized-client")).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(180_000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("User-Agent", LoaderSecrets.userAgent());
            String body = "{"
                    + "\"token\":\"" + JsonUtil.esc(token) + "\","
                    + "\"hwid\":\"" + JsonUtil.esc(Fingerprint.id()) + "\","
                    + "\"client\":\"" + JsonUtil.esc(client.apiName) + "\","
                    + "\"version\":\"" + JsonUtil.esc(LoaderSecrets.version()) + "\","
                    + "\"loaderNonce\":\"" + JsonUtil.esc(SessionEntropy.nonce()) + "\","
                    + "\"loaderProof\":\"" + JsonUtil.esc(SessionEntropy.proof(token, client.apiName)) + "\""
                    + "}";
            AuthClient.write(conn, body);
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                String resp = AuthClient.read(conn, code);
                if (progress != null) progress.update(0, "HTTP " + code + " " + JsonUtil.field(resp, "reason"));
                return null;
            }
            String fileName = fileName(conn, client);
            Path tmp = mods.resolve(fileName + ".tmp");
            Path output = mods.resolve(fileName);
            long total = conn.getContentLengthLong();
            long done = 0;
            long lastAt = System.currentTimeMillis();
            long lastBytes = 0;
            try (InputStream in = conn.getInputStream(); OutputStream out = Files.newOutputStream(tmp)) {
                byte[] buf = new byte[16384];
                int read;
                while ((read = in.read(buf)) >= 0) {
                    if (read == 0) continue;
                    DownloadPause.awaitIfPaused();
                    out.write(buf, 0, read);
                    done += read;
                    long now = System.currentTimeMillis();
                    if (now - lastAt >= 350) {
                        int pct = total > 0 ? (int) Math.min(99, done * 100 / total) : 0;
                        if (progress != null) progress.update(pct, speed(done - lastBytes, now - lastAt));
                        lastAt = now;
                        lastBytes = done;
                    }
                }
            }
            if (Files.size(tmp) < 1024) {
                Files.deleteIfExists(tmp);
                if (progress != null) progress.update(0, "Invalid file");
                return null;
            }
            cleanOld(mods, client, fileName);
            Files.move(tmp, output, StandardCopyOption.REPLACE_EXISTING);
            if (progress != null) progress.update(100, "Complete");
            return output;
        } catch (Throwable e) {
            if (progress != null) progress.update(0, e.getClass().getSimpleName());
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String fileName(HttpURLConnection conn, ClientProfile client) {
        String cd = conn.getHeaderField("Content-Disposition");
        if (cd != null) {
            Matcher m = Pattern.compile("filename=\"?([^\";]+)\"?").matcher(cd);
            if (m.find() && m.group(1).endsWith(".jar")) return sanitize(m.group(1));
        }
        String header = conn.getHeaderField("X-Forever-File-Name");
        if (header != null && header.endsWith(".jar")) return sanitize(header);
        return client.apiName + "Client.jar";
    }

    private static String sanitize(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]+", "_");
    }

    private static void cleanOld(Path mods, ClientProfile client, String keep) {
        try (java.util.stream.Stream<Path> s = Files.list(mods)) {
            s.filter(p -> {
                String n = p.getFileName().toString();
                return n.endsWith(".jar") && n.startsWith(client.jarPrefix) && !n.equals(keep);
            }).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (Throwable ignored) {
                }
            });
        } catch (Throwable ignored) {
        }
    }

    private static String speed(long bytes, long ms) {
        if (ms <= 0) return "...";
        double bps = bytes * 1000.0 / ms;
        if (bps > 1024 * 1024) return String.format(Locale.ROOT, "%.1f MB/s", bps / 1024 / 1024);
        if (bps > 1024) return String.format(Locale.ROOT, "%.0f KB/s", bps / 1024);
        return String.format(Locale.ROOT, "%.0f B/s", bps);
    }
}
