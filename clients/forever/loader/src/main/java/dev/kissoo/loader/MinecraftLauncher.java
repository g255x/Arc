package dev.kissoo.loader;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.io.PrintWriter;
import java.io.StringWriter;

final class MinecraftLauncher {
    private MinecraftLauncher() {
    }

    static boolean launchOffline(ClientProfile profile, Path gameDir, Path clientJar, String username, Consumer<String> log, Downloader.Progress progress) {
        try {
            if (clientJar == null || !Files.isRegularFile(clientJar)) {
                throw new IllegalStateException("Secure runtime is unavailable");
            }
            MinecraftInstaller.LaunchSpec spec = MinecraftInstaller.prepare(profile, gameDir, log, progress);
            String user = sanitizeUsername(username);
            UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + user).getBytes(StandardCharsets.UTF_8));
            ArrayList<String> cmd = new ArrayList<>();
            cmd.add(javaExecutable().toString());
            cmd.add("-Xmx4G");
            cmd.add("-Xms512M");
            cmd.add("-Djava.library.path=" + spec.nativesDir());
            cmd.add("-Dorg.lwjgl.librarypath=" + spec.nativesDir());
            cmd.add("-Dforever.loader.launch=true");
            cmd.add("-Dforever.loader.client=" + profile.apiName);
            cmd.add("-Dfabric.addMods=" + String.join(File.pathSeparator,
                    clientJar.toAbsolutePath().toString(),
                    spec.fabricApiJar().toAbsolutePath().toString(),
                    spec.viaFabricPlusJar().toAbsolutePath().toString()));
            cmd.add("-cp");
            cmd.add(classpath(spec));
            cmd.add(spec.mainClass());
            cmd.add("--username");
            cmd.add(user);
            cmd.add("--version");
            cmd.add(spec.profileName());
            cmd.add("--gameDir");
            cmd.add(gameDir.toString());
            cmd.add("--assetsDir");
            cmd.add(gameDir.resolve("assets").toString());
            cmd.add("--assetIndex");
            cmd.add(spec.assetIndex());
            cmd.add("--uuid");
            cmd.add(uuid.toString().replace("-", ""));
            cmd.add("--accessToken");
            cmd.add("0");
            cmd.add("--userType");
            cmd.add("legacy");
            cmd.add("--versionType");
            cmd.add("ForeverLoader");

            if (progress != null) progress.update(96, "Starting Minecraft");
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(gameDir.toFile());
            pb.redirectErrorStream(true);
            Path logs = LoaderPaths.configDir().resolve("logs");
            Files.createDirectories(logs);
            Path gameLog = logs.resolve(profile.apiName.toLowerCase(Locale.ROOT) + "-latest.log");
            pb.redirectOutput(gameLog.toFile());
            Process process = pb.start();
            if (progress != null) progress.update(98, "Verifying Minecraft startup");
            if (process.waitFor(12, TimeUnit.SECONDS)) {
                int exitCode = process.exitValue();
                if (progress != null) progress.update(0, "Minecraft exited during startup");
                if (log != null) {
                    log.accept("Minecraft exited during startup (code " + exitCode + ").");
                    String cause = startupFailure(gameLog);
                    if (!cause.isBlank()) log.accept(cause);
                    log.accept("Startup log: " + gameLog.toAbsolutePath());
                }
                return false;
            }
            if (progress != null) progress.update(100, "Started");
            if (log != null) log.accept("Started " + profile.apiName + " as offline user " + user + ".");
            return true;
        } catch (Throwable t) {
            if (progress != null) progress.update(0, "Launch failed");
            if (log != null) {
                log.accept("Launch failed: " + describe(t));
                log.accept("If this is a Microsoft-only server, use the client's built-in account login after startup.");
                writeCrashDetail(profile, t, log);
            }
            return false;
        }
    }

    private static String classpath(MinecraftInstaller.LaunchSpec spec) {
        StringBuilder sb = new StringBuilder();
        String sep = File.pathSeparator;
        for (Path p : spec.classpath()) {
            if (sb.length() > 0) sb.append(sep);
            sb.append(p);
        }
        return sb.toString();
    }

    private static Path javaExecutable() {
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        String launcher = windows ? "javaw.exe" : "java";
        ArrayList<Path> candidates = new ArrayList<>();
        addJavaHome(candidates, System.getProperty("java.home"), launcher);

        String appPath = System.getProperty("jpackage.app-path", "");
        if (!appPath.isBlank()) addBundledRuntime(candidates, Path.of(appPath), launcher);
        ProcessHandle.current().info().command().ifPresent(command -> addBundledRuntime(candidates, Path.of(command), launcher));
        addJavaHome(candidates, System.getenv("JAVA_HOME"), launcher);
        candidates.add(Path.of("runtime", "bin", launcher).toAbsolutePath());

        for (Path candidate : candidates) {
            if (candidate != null && Files.isRegularFile(candidate)) return candidate.toAbsolutePath().normalize();
        }
        throw new IllegalStateException("Bundled Java runtime launcher is missing. Please download the complete Loader package again.");
    }

    private static void addJavaHome(ArrayList<Path> candidates, String javaHome, String launcher) {
        if (javaHome == null || javaHome.isBlank()) return;
        candidates.add(Path.of(javaHome).resolve("bin").resolve(launcher));
    }

    private static void addBundledRuntime(ArrayList<Path> candidates, Path appExecutable, String launcher) {
        Path parent = appExecutable.toAbsolutePath().getParent();
        if (parent != null) candidates.add(parent.resolve("runtime").resolve("bin").resolve(launcher));
    }

    private static String startupFailure(Path logFile) {
        try {
            if (!Files.isRegularFile(logFile)) return "";
            String text = Files.readString(logFile);
            int marker = text.lastIndexOf("Caused by:");
            if (marker < 0) marker = text.lastIndexOf("Exception in thread");
            if (marker < 0) return "";
            int end = text.indexOf('\n', marker);
            String line = text.substring(marker, end < 0 ? text.length() : end).trim();
            return line.length() > 260 ? line.substring(0, 260) : line;
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String describe(Throwable t) {
        if (t == null) return "unknown";
        StringBuilder sb = new StringBuilder(t.getClass().getSimpleName());
        String msg = t.getMessage();
        if (msg != null && !msg.isBlank()) sb.append(": ").append(msg);
        Throwable cause = t.getCause();
        int depth = 0;
        while (cause != null && depth++ < 3) {
            sb.append(" <- ").append(cause.getClass().getSimpleName());
            String causeMsg = cause.getMessage();
            if (causeMsg != null && !causeMsg.isBlank()) sb.append(": ").append(causeMsg);
            cause = cause.getCause();
        }
        return sb.toString();
    }

    private static void writeCrashDetail(ClientProfile profile, Throwable t, Consumer<String> log) {
        try {
            Path logs = LoaderPaths.configDir().resolve("logs");
            Files.createDirectories(logs);
            String name = profile == null ? "loader" : profile.apiName.toLowerCase(Locale.ROOT);
            Path detail = logs.resolve(name + "-launch-error.log");
            try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
                t.printStackTrace(pw);
                Files.writeString(detail, sw.toString(), StandardCharsets.UTF_8);
            }
            log.accept("Launch error log: " + detail.toAbsolutePath());
        } catch (Throwable ignored) {
        }
    }

    private static String sanitizeUsername(String username) {
        String u = username == null ? "" : username.trim();
        if (u.isBlank() || u.equalsIgnoreCase("verified") || u.equalsIgnoreCase("personalized loader")) {
            u = System.getProperty("user.name", "ForeverUser");
        }
        u = u.replaceAll("[^A-Za-z0-9_]", "_");
        if (u.length() < 3) u = "ForeverUser";
        if (u.length() > 16) u = u.substring(0, 16);
        return u;
    }
}
