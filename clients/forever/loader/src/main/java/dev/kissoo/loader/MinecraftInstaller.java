package dev.kissoo.loader;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class MinecraftInstaller {
    private static final String VERSION_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private static final String FABRIC_PROFILE = "https://meta.fabricmc.net/v2/versions/loader/%s/%s/profile/json";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            // Minecraft asset downloads are highly parallel. A few proxies/CDNs
            // occasionally close reused HTTP/2 streams mid-transfer and Java only
            // reports that as "closed". HTTP/1.1 is slightly less fancy, but much
            // more stable for long asset batches.
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private MinecraftInstaller() {
    }

    record LaunchSpec(String profileName, String mainClass, List<Path> classpath, Path nativesDir,
                      String assetIndex, Path fabricApiJar, Path viaFabricPlusJar) {
    }

    static LaunchSpec prepare(ClientProfile profile, Path gameDir, Consumer<String> log, Downloader.Progress progress) throws Exception {
        DownloadSource source = DownloadSource.detect(log);
        Files.createDirectories(gameDir);
        Path versions = gameDir.resolve("versions");
        Path libraries = gameDir.resolve("libraries");
        Path assets = gameDir.resolve("assets");
        Path natives = gameDir.resolve("natives").resolve(profile.apiName.toLowerCase(Locale.ROOT) + "-" + profile.minecraft);
        Files.createDirectories(versions);
        Files.createDirectories(libraries);
        Files.createDirectories(assets);
        Files.createDirectories(natives);

        if (progress != null) progress.update(3, "Resolving Minecraft " + profile.minecraft);
        Map<String, Object> versionJson = ensureMinecraftVersion(profile.minecraft, gameDir, source, log);
        if (progress != null) progress.update(24, "Resolving libraries");
        ArrayList<Path> cp = new ArrayList<>();
        downloadMinecraftLibraries(versionJson, libraries, natives, cp, source, log);
        Path clientJar = versions.resolve(profile.minecraft).resolve(profile.minecraft + ".jar");
        cp.add(clientJar);

        if (progress != null) progress.update(46, "Resolving assets");
        String assetIndex = ensureAssets(versionJson, assets, source, log, progress);

        if (progress != null) progress.update(70, "Resolving Fabric");
        Map<String, Object> fabricJson = ensureFabricProfile(profile, gameDir, source, log);
        downloadFabricLibraries(fabricJson, libraries, cp, source, log);
        Path fabricApiJar = ensureFabricApi(profile, libraries, source, log);
        Path viaFabricPlusJar = ensureViaFabricPlus(profile, libraries, source, log);
        String mainClass = MiniJson.str(fabricJson.get("mainClass"));
        if (mainClass.isBlank()) mainClass = "net.fabricmc.loader.impl.launch.knot.KnotClient";

        if (progress != null) progress.update(88, "Environment ready");
        return new LaunchSpec(profile.minecraft + "-fabric-" + profile.loader, mainClass, cp, natives,
                assetIndex, fabricApiJar, viaFabricPlusJar);
    }

    private static Map<String, Object> ensureMinecraftVersion(String mc, Path gameDir, DownloadSource source, Consumer<String> log) throws Exception {
        Path versionDir = gameDir.resolve("versions").resolve(mc);
        Files.createDirectories(versionDir);
        Path jsonFile = versionDir.resolve(mc + ".json");
        String json;
        if (Files.isRegularFile(jsonFile)) {
            json = Files.readString(jsonFile);
        } else {
            String manifest = httpText(VERSION_MANIFEST, source);
            String url = findVersionUrl(manifest, mc);
            if (url.isBlank()) throw new IllegalStateException("Minecraft version not found: " + mc);
            json = httpText(url, source);
            Files.writeString(jsonFile, json);
        }
        Map<String, Object> root = MiniJson.obj(MiniJson.parse(json));
        Map<String, Object> client = MiniJson.obj(MiniJson.get(root, "downloads", "client"));
        String clientUrl = MiniJson.str(client.get("url"));
        String sha1 = MiniJson.str(client.get("sha1"));
        Path clientJar = versionDir.resolve(mc + ".jar");
        downloadIfNeeded(clientUrl, clientJar, sha1, source, log);
        return root;
    }

    private static String findVersionUrl(String manifest, String mc) {
        Map<String, Object> root = MiniJson.obj(MiniJson.parse(manifest));
        for (Object item : MiniJson.arr(root.get("versions"))) {
            Map<String, Object> v = MiniJson.obj(item);
            if (mc.equals(MiniJson.str(v.get("id")))) return MiniJson.str(v.get("url"));
        }
        return "";
    }

    private static void downloadMinecraftLibraries(Map<String, Object> versionJson, Path libraries, Path natives, List<Path> cp, DownloadSource source, Consumer<String> log) throws Exception {
        for (Object item : MiniJson.arr(versionJson.get("libraries"))) {
            Map<String, Object> lib = MiniJson.obj(item);
            if (!allowed(lib)) continue;
            Map<String, Object> artifact = MiniJson.obj(MiniJson.get(lib, "downloads", "artifact"));
            String path = MiniJson.str(artifact.get("path"));
            String url = MiniJson.str(artifact.get("url"));
            String sha1 = MiniJson.str(artifact.get("sha1"));
            if (!path.isBlank() && !url.isBlank()) {
                Path out = libraries.resolve(path.replace('/', java.io.File.separatorChar));
                downloadIfNeeded(url, out, sha1, source, log);
                cp.add(out);
            }
            Map<String, Object> nativesMap = MiniJson.obj(lib.get("natives"));
            String classifier = MiniJson.str(nativesMap.get("windows")).replace("${arch}", arch());
            if (!classifier.isBlank()) {
                Map<String, Object> nativeArtifact = MiniJson.obj(MiniJson.get(lib, "downloads", "classifiers", classifier));
                String nativePath = MiniJson.str(nativeArtifact.get("path"));
                String nativeUrl = MiniJson.str(nativeArtifact.get("url"));
                String nativeSha1 = MiniJson.str(nativeArtifact.get("sha1"));
                if (!nativePath.isBlank() && !nativeUrl.isBlank()) {
                    Path out = libraries.resolve(nativePath.replace('/', java.io.File.separatorChar));
                    downloadIfNeeded(nativeUrl, out, nativeSha1, source, log);
                    extractNatives(out, natives);
                }
            }
        }
    }

    private static boolean allowed(Map<String, Object> lib) {
        List<Object> rules = MiniJson.arr(lib.get("rules"));
        if (rules.isEmpty()) return true;
        boolean allowed = false;
        for (Object item : rules) {
            Map<String, Object> rule = MiniJson.obj(item);
            String action = MiniJson.str(rule.get("action"));
            Map<String, Object> os = MiniJson.obj(rule.get("os"));
            String name = MiniJson.str(os.get("name"));
            boolean applies = name.isBlank() || "windows".equals(name);
            if (applies) allowed = "allow".equals(action);
        }
        return allowed;
    }

    private static String ensureAssets(Map<String, Object> versionJson, Path assets, DownloadSource source, Consumer<String> log, Downloader.Progress progress) throws Exception {
        Map<String, Object> assetIndex = MiniJson.obj(versionJson.get("assetIndex"));
        String id = MiniJson.str(assetIndex.get("id"));
        String url = MiniJson.str(assetIndex.get("url"));
        String sha1 = MiniJson.str(assetIndex.get("sha1"));
        Path indexFile = assets.resolve("indexes").resolve(id + ".json");
        downloadIfNeeded(url, indexFile, sha1, source, log);
        Map<String, Object> index = MiniJson.obj(MiniJson.parse(Files.readString(indexFile)));
        Map<String, Object> objects = MiniJson.obj(index.get("objects"));
        int total = Math.max(objects.size(), 1);
        AtomicInteger done = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(source.assetConcurrency(), r -> {
            Thread thread = new Thread(r, "ForeverLoader-Asset");
            thread.setDaemon(true);
            return thread;
        });
        ArrayList<Future<?>> downloads = new ArrayList<>(objects.size());
        if (log != null) log.accept("Checking " + objects.size() + " assets with " + source.assetConcurrency() + " download workers.");
        for (Object value : objects.values()) {
            Map<String, Object> obj = MiniJson.obj(value);
            String hash = MiniJson.str(obj.get("hash"));
            if (hash.length() < 2) continue;
            Path out = assets.resolve("objects").resolve(hash.substring(0, 2)).resolve(hash);
            String objectUrl = "https://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash;
            downloads.add(pool.submit(() -> {
                downloadIfNeeded(objectUrl, out, hash, source, null);
                int completed = done.incrementAndGet();
                if (progress != null && (completed == total || completed % 24 == 0)) {
                    progress.update(46 + Math.min(22, completed * 22 / total), "Assets " + completed + "/" + total);
                }
                return null;
            }));
        }
        try {
            for (Future<?> download : downloads) download.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception exception) throw exception;
            throw new IllegalStateException(cause == null ? "Asset download failed" : cause.getMessage(), cause);
        } finally {
            pool.shutdownNow();
        }
        return id;
    }

    private static Map<String, Object> ensureFabricProfile(ClientProfile profile, Path gameDir, DownloadSource source, Consumer<String> log) throws Exception {
        String profileName = profile.minecraft + "-fabric-" + profile.loader;
        Path dir = gameDir.resolve("versions").resolve(profileName);
        Files.createDirectories(dir);
        Path file = dir.resolve(profileName + ".json");
        String json;
        if (Files.isRegularFile(file)) {
            json = Files.readString(file);
        } else {
            json = httpText(String.format(Locale.ROOT, FABRIC_PROFILE, profile.minecraft, profile.loader), source);
            Files.writeString(file, json);
        }
        return MiniJson.obj(MiniJson.parse(json));
    }

    private static void downloadFabricLibraries(Map<String, Object> fabricJson, Path libraries, List<Path> cp, DownloadSource source, Consumer<String> log) throws Exception {
        for (Object item : MiniJson.arr(fabricJson.get("libraries"))) {
            Map<String, Object> lib = MiniJson.obj(item);
            String name = MiniJson.str(lib.get("name"));
            String url = MiniJson.str(lib.get("url"));
            if (name.isBlank() || url.isBlank()) continue;
            MavenArtifact artifact = MavenArtifact.of(name, url);
            downloadIfNeeded(artifact.url(), libraries.resolve(artifact.path()), "", source, log);
            cp.add(libraries.resolve(artifact.path()));
        }
    }

    private static Path ensureFabricApi(ClientProfile profile, Path libraries, DownloadSource source, Consumer<String> log) throws Exception {
        String version = profile.fabricApi;
        String relative = "net/fabricmc/fabric-api/fabric-api/" + version + "/fabric-api-" + version + ".jar";
        Path output = libraries.resolve(relative.replace('/', java.io.File.separatorChar));
        downloadIfNeeded("https://maven.fabricmc.net/" + relative, output, "", source, log);
        return output;
    }

    private static Path ensureViaFabricPlus(ClientProfile profile, Path libraries, DownloadSource source, Consumer<String> log) throws Exception {
        String fileName = "ViaFabricPlus-" + profile.viaFabricPlus + ".jar";
        Path output = libraries.resolve("com").resolve("viaversion").resolve("viafabricplus")
                .resolve(profile.viaFabricPlus).resolve(fileName);
        downloadIfNeeded(profile.viaFabricPlusUrl, output, profile.viaFabricPlusSha1, source, log);
        return output;
    }

    private static void downloadIfNeeded(String url, Path out, String sha1, DownloadSource source, Consumer<String> log) throws Exception {
        if (url == null || url.isBlank()) return;
        if (Files.isRegularFile(out) && (sha1 == null || sha1.isBlank() || sha1.equalsIgnoreCase(sha1(out)))) return;
        Files.createDirectories(out.getParent());
        Path tmp = out.resolveSibling(out.getFileName() + ".tmp");
        if (log != null) log.accept("Downloading " + out.getFileName());
        Exception failure = null;
        List<String> candidates = source.candidates(url);
        for (int attempt = 0; attempt < candidates.size(); attempt++) {
            String candidate = candidates.get(attempt);
            int retries = shouldRetry(candidate) ? 3 : 1;
            for (int retry = 1; retry <= retries; retry++) {
                try {
                    DownloadPause.awaitIfPaused();
                    Files.deleteIfExists(tmp);
                    HttpRequest req = HttpRequest.newBuilder(URI.create(candidate))
                            .timeout(Duration.ofMinutes(3))
                            .header("User-Agent", LoaderSecrets.userAgent())
                            .build();
                    HttpResponse<InputStream> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofInputStream());
                    if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                        try (InputStream ignored = resp.body()) {
                            // Close the response before falling back to the next source.
                        }
                        throw new IllegalStateException("HTTP " + resp.statusCode());
                    }
                    try (InputStream in = resp.body(); OutputStream os = Files.newOutputStream(tmp)) {
                        byte[] buffer = new byte[16384];
                        int read;
                        while ((read = in.read(buffer)) >= 0) {
                            if (read == 0) continue;
                            DownloadPause.awaitIfPaused();
                            os.write(buffer, 0, read);
                        }
                    }
                    if (sha1 != null && !sha1.isBlank() && !sha1.equalsIgnoreCase(sha1(tmp))) {
                        throw new IllegalStateException("Hash mismatch: " + out.getFileName());
                    }
                    Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING);
                    return;
                } catch (Exception e) {
                    failure = e;
                    Files.deleteIfExists(tmp);
                    boolean lastRetry = retry >= retries;
                    if (!lastRetry && transientNetworkFailure(e)) {
                        if (log != null) log.accept("Download interrupted for " + out.getFileName() + "; retrying (" + retry + "/" + retries + ").");
                        continue;
                    }
                    if (attempt + 1 < candidates.size() && log != null) {
                        log.accept("Mirror unavailable for " + out.getFileName() + "; retrying official source.");
                    }
                    break;
                }
            }
        }
        String detail = failure == null ? "" : ": " + readableFailure(failure);
        throw new IOException("Download failed for " + out.getFileName() + detail, failure);
    }

    private static String httpText(String url, DownloadSource source) throws Exception {
        Exception failure = null;
        for (String candidate : source.candidates(url)) {
            int retries = shouldRetry(candidate) ? 3 : 1;
            for (int retry = 1; retry <= retries; retry++) {
                try {
                    HttpRequest req = HttpRequest.newBuilder(URI.create(candidate))
                            .timeout(Duration.ofSeconds(45))
                            .header("User-Agent", LoaderSecrets.userAgent())
                            .build();
                    HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
                    if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                        throw new IllegalStateException("HTTP " + resp.statusCode());
                    }
                    return resp.body();
                } catch (Exception e) {
                    failure = e;
                    if (retry < retries && transientNetworkFailure(e)) continue;
                    break;
                }
            }
        }
        throw new IOException("Metadata download failed" + (failure == null ? "" : ": " + readableFailure(failure)), failure);
    }

    private static boolean shouldRetry(String url) {
        return url != null && !url.isBlank();
    }

    private static boolean transientNetworkFailure(Throwable t) {
        for (Throwable x = t; x != null; x = x.getCause()) {
            String msg = String.valueOf(x.getMessage()).toLowerCase(Locale.ROOT);
            String type = x.getClass().getName().toLowerCase(Locale.ROOT);
            if (x instanceof IOException) return true;
            if (msg.contains("closed") || msg.contains("reset") || msg.contains("timed out")
                    || msg.contains("unexpected end") || msg.contains("eof")) return true;
            if (type.contains("timeout") || type.contains("connectexception")) return true;
        }
        return false;
    }

    private static String readableFailure(Throwable t) {
        Throwable x = t;
        while (x.getCause() != null && (x.getMessage() == null || x.getMessage().isBlank() || "closed".equalsIgnoreCase(x.getMessage()))) {
            x = x.getCause();
        }
        String msg = x.getMessage();
        if (msg == null || msg.isBlank()) msg = x.getClass().getSimpleName();
        return msg;
    }

    private static String sha1(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buf = new byte[16384];
            int read;
            while ((read = in.read(buf)) > 0) digest.update(buf, 0, read);
        }
        return CryptoUtil.hex(digest.digest());
    }

    private static void extractNatives(Path jar, Path natives) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(jar))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                String name = e.getName();
                if (e.isDirectory() || name.startsWith("META-INF/") || !(name.endsWith(".dll") || name.endsWith(".so") || name.endsWith(".dylib"))) continue;
                Path out = natives.resolve(Path.of(name).getFileName().toString());
                Files.copy(zis, out, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static String arch() {
        return System.getProperty("os.arch", "").contains("64") ? "64" : "32";
    }

    private record MavenArtifact(String path, String url) {
        static MavenArtifact of(String coord, String repo) {
            String[] p = coord.split(":");
            String group = p.length > 0 ? p[0] : "";
            String artifact = p.length > 1 ? p[1] : "";
            String version = p.length > 2 ? p[2] : "";
            String classifier = p.length > 3 ? "-" + p[3] : "";
            String file = artifact + "-" + version + classifier + ".jar";
            String path = group.replace('.', '/') + "/" + artifact + "/" + version + "/" + file;
            String base = repo.endsWith("/") ? repo : repo + "/";
            return new MavenArtifact(path.replace('/', java.io.File.separatorChar), base + path);
        }
    }
}
