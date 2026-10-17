package dev.kissoo.loader;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.io.File;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

final class SelfProtection {
    private SelfProtection() {
    }

    static boolean isHwidMismatch(String reason) {
        String r = reason == null ? "" : reason.toLowerCase(Locale.ROOT);
        return r.contains("hwid") || r.contains("设备") || r.contains("未授权") || r.contains("mismatch");
    }

    static void warnAndRemoveOwnJar(String reason, Component parent) {
        Path warning = writeWarning(reason);
        scheduleOwnJarDelete();
        JOptionPane.showMessageDialog(parent,
                "HWID 不匹配。不要试图共享您的客户端。\n警告文件已生成：\n" + warning,
                "Forever Loader", JOptionPane.WARNING_MESSAGE);
        System.exit(12);
    }

    private static Path writeWarning(String reason) {
        Path base = ownJarPath().map(Path::getParent).orElse(LoaderPaths.configDir());
        Path warning = base.resolve("ForeverLoader-WARNING.txt");
        String text = "不要试图共享您的客户端。\n"
                + "This personalized Loader is bound to its original token and HWID.\n"
                + "Current HWID: " + Fingerprint.id() + "\n"
                + "Reason: " + (reason == null ? "" : reason) + "\n"
                + "Time: " + TimeText.FULL.format(Instant.now()) + "\n";
        try {
            Files.createDirectories(warning.getParent());
            Files.writeString(warning, text, StandardCharsets.UTF_8);
        } catch (Throwable ignored) {
            warning = java.nio.file.Paths.get(System.getProperty("user.home"), "ForeverLoader-WARNING.txt");
            try {
                Files.writeString(warning, text, StandardCharsets.UTF_8);
            } catch (Throwable ignoredAgain) {
            }
        }
        return warning;
    }

    private static void scheduleOwnJarDelete() {
        Optional<Path> own = ownJarPath();
        if (own.isEmpty()) return;
        Path jar = own.get();
        String name = jar.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".jar") || !name.contains("loader")) return;
        try {
            jar.toFile().deleteOnExit();
            if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
                String cmd = "ping -n 3 127.0.0.1 > nul & del /f /q \"" + jar.toAbsolutePath() + "\"";
                new ProcessBuilder("cmd.exe", "/c", cmd).start();
            } else {
                new ProcessBuilder("sh", "-c", "sleep 2; rm -f \"$1\"", "sh", jar.toAbsolutePath().toString()).start();
            }
        } catch (Throwable ignored) {
        }
    }

    private static Optional<Path> ownJarPath() {
        try {
            CodeSource source = ForeverLoader.class.getProtectionDomain().getCodeSource();
            if (source == null || source.getLocation() == null) return Optional.empty();
            Path path = java.nio.file.Paths.get(source.getLocation().toURI()).toAbsolutePath().normalize();
            return Files.isRegularFile(path) ? Optional.of(path) : Optional.empty();
        } catch (URISyntaxException ignored) {
            return Optional.empty();
        }
    }
}
