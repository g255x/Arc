package dev.kissoo.loader;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;
import java.util.StringJoiner;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

final class CloudPackageClient {
    private static final SecureRandom RNG = new SecureRandom();

    private CloudPackageClient() {
    }

    record Manifest(String packageId, String plainSha256, String encUrl, String cacheName, String owner, String tokenId) {
    }

    record LaunchSession(String sessionId, byte[] key, byte[] iv, String runtimeName, byte[] watermark, long expiresAt) {
    }

    record DownloadedCache(Path firstPart, Path secondPart, Manifest manifest) {
        Path[] files() {
            return secondPart == null ? new Path[]{firstPart} : new Path[]{firstPart, secondPart};
        }
    }

    record DownloadResult(String packageSha256) {
    }

    static Path prepareRuntime(ClientProfile client, String token, Path gameDir, Downloader.Progress progress, java.util.function.Consumer<String> log) throws Exception {
        Exception last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                Manifest manifest = manifest(client, token);
                if (progress != null) progress.update(8, attempt == 1 ? "Checking encrypted cache" : "Refreshing package state");
                DownloadedCache cache = ensureEncryptedCache(manifest, progress, log);
                LaunchSession session = launchSession(client, token, cache.manifest());
                if (progress != null) progress.update(76, "Decrypting runtime");
                Path runtime = randomRuntimeJar(gameDir, session.runtimeName());
                try {
                    decryptAndPatch(client, cache.files(), runtime, session, token, cache.manifest().owner(), cache.manifest().tokenId());
                } catch (Exception decryptFailure) {
                    if (attempt >= 3 || !isRetryableDecrypt(decryptFailure)) throw decryptFailure;
                    deleteCacheParts(cache.files());
                    if (log != null) log.accept("Encrypted cache mismatch; refreshing package.");
                    last = decryptFailure;
                    sleepBeforeRetry(attempt);
                    continue;
                }
                if (progress != null) progress.update(92, "Runtime ready");
                heartbeatAsync(client, session, runtime);
                return runtime;
            } catch (PackageChangedException | RetryableCloudException e) {
                last = e;
                if (log != null) log.accept("Cloud package refresh required (" + attempt + "/3): " + e.getMessage());
                if (attempt >= 3) break;
                sleepBeforeRetry(attempt);
            } catch (IOException e) {
                if (!isRetryableNetwork(e) || attempt >= 3) throw e;
                last = e;
                if (log != null) log.accept("Cloud package connection interrupted; retrying (" + attempt + "/3).");
                sleepBeforeRetry(attempt);
            }
        }
        if (last != null) throw last;
        throw new IllegalStateException("Cloud package failed");
    }

    static Manifest manifest(ClientProfile client, String token) throws Exception {
        String body = baseBody(client, token, "")
                + ",\"request\":\"manifest\"}";
        String resp = post("/loader/client-manifest", body);
        if (!resp.contains("\"ok\":true")) throw new IllegalStateException(JsonUtil.field(resp, "error"));
        return new Manifest(
                JsonUtil.field(resp, "packageId"),
                JsonUtil.field(resp, "plainSha256"),
                JsonUtil.field(resp, "encUrl"),
                JsonUtil.field(resp, "cacheName"),
                JsonUtil.field(resp, "owner"),
                JsonUtil.field(resp, "tokenId")
        );
    }

    private static LaunchSession launchSession(ClientProfile client, String token, Manifest manifest) throws Exception {
        String body = baseBody(client, token, "")
                + ",\"packageSha256\":\"" + JsonUtil.esc(manifest.plainSha256()) + "\"}";
        String resp = post("/loader/launch-session", body);
        if (!resp.contains("\"ok\":true")) throw new IllegalStateException(JsonUtil.field(resp, "error"));
        return new LaunchSession(
                JsonUtil.field(resp, "sessionId"),
                Base64.getDecoder().decode(JsonUtil.field(resp, "aesKey")),
                Base64.getDecoder().decode(JsonUtil.field(resp, "iv")),
                safeRandomJarName(JsonUtil.field(resp, "runtimeName")),
                Base64.getDecoder().decode(JsonUtil.field(resp, "watermark")),
                AuthClient.parseLong(JsonUtil.field(resp, "expiresAt"))
        );
    }

    private static DownloadedCache ensureEncryptedCache(Manifest manifest, Downloader.Progress progress, java.util.function.Consumer<String> log) throws Exception {
        Path metaDir = LoaderPaths.cacheDir();
        Files.createDirectories(metaDir);
        Path meta = metaDir.resolve(metaName(manifest.packageId(), manifest.plainSha256()));
        String fileName = "";
        String partA = "";
        String partB = "";
        String tempDirText = "";
        if (Files.isRegularFile(meta)) {
            String raw = Files.readString(meta);
            if (manifest.plainSha256().equals(JsonUtil.field(raw, "plainSha256"))) {
                fileName = JsonUtil.field(raw, "file");
                partA = JsonUtil.field(raw, "partA");
                partB = JsonUtil.field(raw, "partB");
                tempDirText = JsonUtil.field(raw, "tempDir");
            }
        }
        Path cachedTempDir = tempDirText.isBlank() ? metaDir : Path.of(tempDirText);
        Path first = resolveCachePart(partA, cachedTempDir, metaDir);
        Path second = resolveCachePart(partB, cachedTempDir, metaDir);
        if (validSplit(first, second)) {
            if (tempDirText.isBlank()) {
                DownloadedCache migrated = moveSplitToTemp(first, second, manifest);
                Files.writeString(meta, metaJson(manifest, migrated.firstPart().getParent(), migrated.firstPart().getFileName().toString(), migrated.secondPart().getFileName().toString()));
                if (log != null) log.accept("Encrypted package cache moved to temp storage.");
                return migrated;
            }
            return new DownloadedCache(first, second, manifest);
        }
        Path enc = resolveCachePart(fileName, cachedTempDir, metaDir);
        if (enc != null && Files.isRegularFile(enc) && Files.size(enc) > 32) {
            DownloadedCache migrated = splitExistingCache(enc, newTempPackageDir(), manifest);
            Files.writeString(meta, metaJson(manifest, migrated.firstPart().getParent(), migrated.firstPart().getFileName().toString(), migrated.secondPart().getFileName().toString()));
            Files.deleteIfExists(enc);
            if (log != null) log.accept("Encrypted package cache split into secure parts.");
            return migrated;
        }
        if (progress != null) progress.update(12, "Downloading encrypted package");
        String randomName = safeRandomBinName(manifest.cacheName());
        Path tempDir = newTempPackageDir();
        Path tmp = tempDir.resolve(randomName + ".tmp");
        DownloadResult result;
        try {
            result = download(LoaderSecrets.apiBase() + AuthClient.enc("/loader/client-enc") + "&" + clientParam(manifest.encUrl(), false), tmp, progress);
        } catch (Exception e) {
            Files.deleteIfExists(tmp);
            deleteEmptyDirectory(tempDir);
            throw e;
        }
        Manifest effective = manifest;
        if (result.packageSha256() != null && !result.packageSha256().isBlank()
                && !result.packageSha256().equalsIgnoreCase(manifest.plainSha256())) {
            effective = new Manifest(
                    metaName(manifest.packageId(), result.packageSha256()),
                    result.packageSha256().toLowerCase(java.util.Locale.ROOT),
                    manifest.encUrl(),
                    manifest.cacheName(),
                    manifest.owner(),
                    manifest.tokenId()
            );
            meta = metaDir.resolve(metaName(effective.packageId(), effective.plainSha256()));
            if (log != null) log.accept("Package hash refreshed from server header.");
        }
        DownloadedCache split = splitDownloadedCache(tmp, tempDir, effective);
        Files.writeString(meta, metaJson(effective, split.firstPart().getParent(), split.firstPart().getFileName().toString(), split.secondPart().getFileName().toString()));
        if (log != null) log.accept("Encrypted package stored securely.");
        cleanupOldCache(tempDir, Set.of(split.firstPart().getFileName().toString(), split.secondPart().getFileName().toString()));
        cleanupOldCache(metaDir, Set.of());
        return split;
    }

    private static String metaJson(Manifest manifest, Path tempDir, String partA, String partB) {
        return "{"
                + "\"plainSha256\":\"" + JsonUtil.esc(manifest.plainSha256()) + "\","
                + "\"tempDir\":\"" + JsonUtil.esc(tempDir == null ? "" : tempDir.toAbsolutePath().normalize().toString()) + "\","
                + "\"partA\":\"" + JsonUtil.esc(partA) + "\","
                + "\"partB\":\"" + JsonUtil.esc(partB) + "\","
                + "\"packageId\":\"" + JsonUtil.esc(manifest.packageId()) + "\""
                + "}";
    }

    private static Path resolveCachePart(String value, Path tempDir, Path fallbackDir) {
        if (value == null || value.isBlank()) return null;
        Path path = Path.of(value);
        if (path.isAbsolute()) return path;
        if (tempDir != null) return tempDir.resolve(value);
        return fallbackDir == null ? path : fallbackDir.resolve(value);
    }

    private static boolean validSplit(Path first, Path second) {
        try {
            return first != null && second != null
                    && Files.isRegularFile(first) && Files.isRegularFile(second)
                    && Files.size(first) > 12 && Files.size(second) > 16
                    && Files.size(first) + Files.size(second) > 32;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static DownloadedCache splitExistingCache(Path source, Path dir, Manifest manifest) throws Exception {
        Path tmp = dir.resolve(randomHex(12) + ".tmp");
        Files.createDirectories(dir);
        Files.copy(source, tmp, StandardCopyOption.REPLACE_EXISTING);
        return splitDownloadedCache(tmp, dir, manifest);
    }

    private static DownloadedCache moveSplitToTemp(Path first, Path second, Manifest manifest) throws Exception {
        Path dir = newTempPackageDir();
        Path nextFirst = dir.resolve(safeRandomBinName(""));
        Path nextSecond = dir.resolve(safeRandomBinName(""));
        while (nextSecond.getFileName().equals(nextFirst.getFileName())) nextSecond = dir.resolve(safeRandomBinName(""));
        Files.move(first, nextFirst, StandardCopyOption.REPLACE_EXISTING);
        Files.move(second, nextSecond, StandardCopyOption.REPLACE_EXISTING);
        return new DownloadedCache(nextFirst, nextSecond, manifest);
    }

    private static DownloadedCache splitDownloadedCache(Path tmp, Path dir, Manifest manifest) throws Exception {
        Files.createDirectories(dir);
        byte[] blob = Files.readAllBytes(tmp);
        Files.deleteIfExists(tmp);
        if (blob.length < 33) throw new IllegalStateException("Encrypted cache is invalid");
        int low = Math.max(13, blob.length / 3);
        int high = Math.max(low + 1, (blob.length * 2) / 3);
        int splitAt = low + RNG.nextInt(Math.max(1, high - low));
        String firstName = safeRandomBinName("");
        String secondName = safeRandomBinName("");
        while (secondName.equals(firstName)) secondName = safeRandomBinName("");
        Path first = dir.resolve(firstName);
        Path second = dir.resolve(secondName);
        Path firstTmp = dir.resolve(firstName + ".tmp");
        Path secondTmp = dir.resolve(secondName + ".tmp");
        Files.write(firstTmp, java.util.Arrays.copyOfRange(blob, 0, splitAt));
        Files.write(secondTmp, java.util.Arrays.copyOfRange(blob, splitAt, blob.length));
        Files.move(firstTmp, first, StandardCopyOption.REPLACE_EXISTING);
        Files.move(secondTmp, second, StandardCopyOption.REPLACE_EXISTING);
        return new DownloadedCache(first, second, manifest);
    }

    private static Path newTempPackageDir() throws Exception {
        Path root = LoaderPaths.tempDir().resolve(randomHex(16));
        Files.createDirectories(root);
        return root;
    }

    private static void deleteEmptyDirectory(Path dir) {
        try {
            Files.deleteIfExists(dir);
        } catch (Throwable ignored) {
        }
    }

    private static String metaName(String packageId, String sha256) {
        String seed = (packageId == null ? "" : packageId) + "|" + (sha256 == null ? "" : sha256);
        byte[] digest;
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            digest = md.digest(seed.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception ignored) {
            return randomHex(12) + ".json";
        }
        return CryptoUtil.hex(digest).substring(0, 24) + ".json";
    }

    private static String clientParam(String encUrl, boolean includeSha) {
        int q = encUrl.indexOf('?');
        String raw = q >= 0 ? encUrl.substring(q + 1) : encUrl;
        StringJoiner out = new StringJoiner("&");
        for (String part : raw.split("&")) {
            if (part.isBlank()) continue;
            int eq = part.indexOf('=');
            String key = eq >= 0 ? part.substring(0, eq) : part;
            String value = eq >= 0 ? part.substring(eq + 1) : "";
            String decodedKey = URLDecoder.decode(key, java.nio.charset.StandardCharsets.UTF_8);
            if ("route".equalsIgnoreCase(decodedKey)) continue;
            if (!includeSha && "sha".equalsIgnoreCase(decodedKey)) continue;
            out.add(URLEncoder.encode(decodedKey, java.nio.charset.StandardCharsets.UTF_8)
                    + "="
                    + URLEncoder.encode(URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8), java.nio.charset.StandardCharsets.UTF_8));
        }
        return out.toString();
    }

    private static DownloadResult download(String url, Path out, Downloader.Progress progress) throws Exception {
        try {
            SegmentedDownloader.Result result = SegmentedDownloader.download(url, out, progress, "Encrypted");
            return new DownloadResult(result.packageSha256() == null ? "" : result.packageSha256().trim().toLowerCase(java.util.Locale.ROOT));
        } catch (SegmentedDownloader.StatusException e) {
            String error = JsonUtil.field(e.body(), "error");
            if (e.status() == 409) throw new PackageChangedException(error.isBlank() ? "package changed" : error);
            throw new IOException("HTTP " + e.status() + (error.isBlank() ? "" : ": " + error));
        }
    }

    private static boolean isRetryableNetwork(IOException e) {
        String m = String.valueOf(e.getMessage()).toLowerCase(java.util.Locale.ROOT);
        return m.contains("unexpected end")
                || m.contains("premature")
                || m.contains("reset")
                || m.contains("timed out")
                || m.contains("closed")
                || m.contains("eof");
    }

    private static boolean isRetryableDecrypt(Exception e) {
        String m = String.valueOf(e.getMessage()).toLowerCase(java.util.Locale.ROOT);
        String n = e.getClass().getName().toLowerCase(java.util.Locale.ROOT);
        return n.contains("aeadbadtagexception")
                || n.contains("badpaddingexception")
                || m.contains("encrypted cache")
                || m.contains("launch key");
    }

    private static void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(650L * attempt);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private static void decryptAndPatch(ClientProfile client, Path[] cacheParts, Path runtime, LaunchSession session,
                                        String token, String owner, String tokenId) throws Exception {
        byte[] blob = readCacheParts(cacheParts);
        if (blob.length < 29) throw new IllegalStateException("Encrypted cache is invalid");
        byte[] iv = java.util.Arrays.copyOfRange(blob, 0, 12);
        if (!java.util.Arrays.equals(iv, session.iv())) throw new IllegalStateException("Launch key does not match encrypted cache");
        byte[] cipherAndTag = java.util.Arrays.copyOfRange(blob, 12, blob.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(session.key(), "AES"), new GCMParameterSpec(128, session.iv()));
        byte[] plain = cipher.doFinal(cipherAndTag);
        Files.createDirectories(runtime.getParent());
        Path raw = runtime.resolveSibling(runtime.getFileName() + ".raw");
        Files.write(raw, plain);
        patchWatermark(client, raw, runtime, session, token, owner, tokenId);
        Files.deleteIfExists(raw);
        runtime.toFile().deleteOnExit();
        cleanupRuntime(runtime.getParent(), runtime.getFileName().toString());
    }

    private static byte[] readCacheParts(Path[] parts) throws Exception {
        if (parts == null || parts.length == 0 || parts[0] == null) throw new IllegalStateException("Encrypted cache is missing");
        if (parts.length == 1 || parts[1] == null) return Files.readAllBytes(parts[0]);
        byte[] first = Files.readAllBytes(parts[0]);
        byte[] second = Files.readAllBytes(parts[1]);
        byte[] out = new byte[first.length + second.length];
        System.arraycopy(first, 0, out, 0, first.length);
        System.arraycopy(second, 0, out, first.length, second.length);
        return out;
    }

    private static void deleteCacheParts(Path[] parts) {
        if (parts == null) return;
        for (Path part : parts) {
            if (part == null) continue;
            try {
                Files.deleteIfExists(part);
            } catch (Throwable ignored) {
            }
        }
    }

    private static void patchWatermark(ClientProfile client, Path input, Path output, LaunchSession session,
                                       String token, String owner, String tokenId) throws Exception {
        String target = client.apiName.equalsIgnoreCase("Strinova") ? "assets/strinova/user.dat" : "assets/forever/user.dat";
        String sessionTarget = client.apiName.equalsIgnoreCase("Strinova") ? "assets/strinova/loader-session.dat" : "assets/forever/loader-session.dat";
        byte[] userData = embeddedUserData(token, owner, tokenId);
        try (JarInputStream in = new JarInputStream(Files.newInputStream(input))) {
            java.util.jar.Manifest jarManifest = in.getManifest();
            try (JarOutputStream out = jarManifest == null
                    ? new JarOutputStream(Files.newOutputStream(output))
                    : new JarOutputStream(Files.newOutputStream(output), jarManifest)) {
            JarEntry e;
            byte[] buf = new byte[16384];
            while ((e = in.getNextJarEntry()) != null) {
                if (e.isDirectory()
                        || e.getName().equals(target)
                        || e.getName().equals(sessionTarget)
                        || (jarManifest != null && e.getName().equalsIgnoreCase("META-INF/MANIFEST.MF"))) continue;
                JarEntry copy = new JarEntry(e.getName());
                copy.setTime(e.getTime());
                out.putNextEntry(copy);
                int read;
                while ((read = in.read(buf)) > 0) out.write(buf, 0, read);
                out.closeEntry();
            }
            out.putNextEntry(new JarEntry(target));
            out.write(userData);
            out.closeEntry();
            out.putNextEntry(new JarEntry(sessionTarget));
            out.write(session.watermark());
            out.closeEntry();
            }
        }
    }

    private static byte[] embeddedUserData(String token, String owner, String tokenId) throws Exception {
        String json = "{"
                + "\"token\":\"" + JsonUtil.esc(token) + "\","
                + "\"user\":\"" + JsonUtil.esc(owner) + "\","
                + "\"uid\":\"" + JsonUtil.esc(tokenId) + "\""
                + "}";
        byte[] iv = new byte[12];
        RNG.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(embeddedUserKey(), "AES"), new GCMParameterSpec(128, iv));
        byte[] encrypted = cipher.doFinal(json.getBytes(StandardCharsets.UTF_8));
        byte[] out = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, out, 0, iv.length);
        System.arraycopy(encrypted, 0, out, iv.length, encrypted.length);
        return out;
    }

    private static byte[] embeddedUserKey() {
        String hex = "a1b2c3d4e5f60718293a4b5c6d7e8f90"
                + "11223344556677889900aabbccddeeff";
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            out[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    | Character.digit(hex.charAt(i + 1), 16));
        }
        return out;
    }

    private static Path randomRuntimeJar(Path gameDir, String runtimeName) throws Exception {
        Path mods = LoaderPaths.runtimeModsDir(gameDir);
        Files.createDirectories(mods);
        return mods.resolve(safeRandomJarName(runtimeName));
    }

    private static String safeRandomJarName(String name) {
        String n = name == null || name.isBlank() ? randomHex(16) + ".jar" : name;
        n = n.replaceAll("[^A-Za-z0-9._-]", "");
        if (!n.endsWith(".jar")) n += ".jar";
        return n;
    }

    private static String safeRandomBinName(String name) {
        String n = name == null || name.isBlank() ? randomHex(16) + ".bin" : name;
        n = n.replaceAll("[^A-Za-z0-9._-]", "");
        if (!n.endsWith(".bin")) n += ".bin";
        return n;
    }

    private static String randomHex(int bytes) {
        byte[] b = new byte[bytes];
        RNG.nextBytes(b);
        return CryptoUtil.hex(b);
    }

    private static void cleanupOldCache(Path dir, Set<String> keepFiles) {
        try (java.util.stream.Stream<Path> s = Files.list(dir)) {
            s.filter(p -> {
                String n = p.getFileName().toString();
                return n.endsWith(".tmp") || (n.endsWith(".bin") && !keepFiles.contains(n) && stale(p, 14L * 24L * 60L * 60L * 1000L));
            }).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (Throwable ignored) {}
            });
        } catch (Throwable ignored) {
        }
    }

    private static boolean stale(Path p, long maxAgeMs) {
        try {
            return System.currentTimeMillis() - Files.getLastModifiedTime(p).toMillis() > maxAgeMs;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void cleanupRuntime(Path dir, String keep) {
        try (java.util.stream.Stream<Path> s = Files.list(dir)) {
            s.filter(p -> p.getFileName().toString().endsWith(".jar") && !p.getFileName().toString().equals(keep)).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (Throwable ignored) {}
            });
        } catch (Throwable ignored) {
        }
    }

    private static String baseBody(ClientProfile client, String token, String extra) {
        return "{"
                + "\"token\":\"" + JsonUtil.esc(token) + "\","
                + "\"hwid\":\"" + JsonUtil.esc(Fingerprint.id()) + "\","
                + "\"client\":\"" + JsonUtil.esc(client.apiName) + "\","
                + "\"version\":\"" + JsonUtil.esc(LoaderSecrets.version()) + "\","
                + "\"loaderNonce\":\"" + JsonUtil.esc(SessionEntropy.nonce()) + "\","
                + "\"loaderProof\":\"" + JsonUtil.esc(SessionEntropy.proof(token, client.apiName)) + "\""
                + extra;
    }

    private static String post(String route, String body) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(LoaderSecrets.apiBase() + AuthClient.enc(route)).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(30_000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("User-Agent", LoaderSecrets.userAgent());
        AuthClient.write(conn, body);
        int code = conn.getResponseCode();
        String resp = AuthClient.read(conn, code);
        conn.disconnect();
        if (code < 200 || code >= 300) throw new IllegalStateException(JsonUtil.field(resp, "error"));
        return resp;
    }

    static void heartbeatAsync(ClientProfile client, LaunchSession session, Path runtime) {
        Thread t = new Thread(() -> {
            String sha = "";
            while (System.currentTimeMillis() / 1000L < session.expiresAt()) {
                try {
                    if (sha.isBlank()) sha = Sha256.file(runtime);
                    String body = "{"
                            + "\"sessionId\":\"" + JsonUtil.esc(session.sessionId()) + "\","
                            + "\"hwid\":\"" + JsonUtil.esc(Fingerprint.id()) + "\","
                            + "\"client\":\"" + JsonUtil.esc(client.apiName) + "\","
                            + "\"runtimeSha256\":\"" + JsonUtil.esc(sha) + "\""
                            + "}";
                    post("/loader/heartbeat", body);
                    Thread.sleep(60_000);
                } catch (Throwable ignored) {
                    break;
                }
            }
        }, "ForeverLoader-Heartbeat");
        t.setDaemon(true);
        t.start();
    }

    private static final class PackageChangedException extends IOException {
        PackageChangedException(String message) {
            super(message == null || message.isBlank() ? "package changed" : message);
        }
    }

    private static final class RetryableCloudException extends IOException {
        RetryableCloudException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
