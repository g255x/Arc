package dev.kissoo.loader;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

final class LauncherBridge {
    private LauncherBridge() {
    }

    static void openMinecraftLauncher(Consumer<String> log) {
        try {
            Optional<Path> launcher = findLauncher();
            if (launcher.isPresent()) {
                Path p = launcher.get();
                if (p.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".jar")) {
                    new ProcessBuilder(javaPath(), "-jar", p.toString()).directory(p.getParent().toFile()).start();
                } else {
                    new ProcessBuilder(p.toString()).directory(p.getParent().toFile()).start();
                }
                log.accept("Launcher started: " + p);
                return;
            }
            Ui.openFolder(LoaderPaths.gameDir());
            log.accept("Launcher not detected. Opened .minecraft folder.");
        } catch (Throwable e) {
            log.accept("Launch helper failed: " + e.getMessage());
        }
    }

    private static Optional<Path> findLauncher() {
        List<Path> candidates = new ArrayList<>();
        String configured = System.getenv("FOREVER_LOADER_LAUNCHER");
        if (configured != null && !configured.isBlank()) candidates.add(java.nio.file.Paths.get(configured));
        Path custom = LoaderPaths.configDir().resolve("launcher-path.txt");
        try {
            if (Files.isRegularFile(custom)) candidates.add(java.nio.file.Paths.get(Files.readString(custom).trim()));
        } catch (Throwable ignored) {
        }
        String home = System.getProperty("user.home", "");
        candidates.add(java.nio.file.Paths.get(home, "Desktop", "HMCL.exe"));
        candidates.add(java.nio.file.Paths.get(home, "Downloads", "HMCL.exe"));
        candidates.add(java.nio.file.Paths.get(home, "Desktop", "HMCL.jar"));
        candidates.add(java.nio.file.Paths.get(home, "Downloads", "HMCL.jar"));
        candidates.add(java.nio.file.Paths.get("D:\\illililililililililililillil\\hmcl\\HMCL.exe"));
        candidates.add(java.nio.file.Paths.get("D:\\illililililililililililillil\\hmcl\\HMCL.jar"));
        return candidates.stream().filter(Files::isRegularFile).findFirst();
    }

    private static String javaPath() {
        return System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    }
}
