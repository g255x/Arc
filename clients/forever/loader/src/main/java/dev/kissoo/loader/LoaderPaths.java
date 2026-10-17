package dev.kissoo.loader;

import java.nio.file.Path;

final class LoaderPaths {
    private LoaderPaths() {
    }

    static Path configDir() {
        return java.nio.file.Paths.get(System.getProperty("user.home"), ".forever-loader");
    }

    static Path tokenFile() {
        return configDir().resolve("session.dat");
    }

    static Path gameDir() {
        String appdata = System.getenv("APPDATA");
        if (appdata != null && !appdata.isBlank()) return java.nio.file.Paths.get(appdata, ".minecraft");
        return java.nio.file.Paths.get(System.getProperty("user.home"), ".minecraft");
    }

    static Path modsDir(Path gameDir) {
        return gameDir.resolve("mods");
    }

    static Path cacheDir() {
        return configDir().resolve("cache").resolve("pkg");
    }

    static Path tempDir() {
        String tmp = System.getProperty("java.io.tmpdir");
        if (tmp != null && !tmp.isBlank()) return java.nio.file.Paths.get(tmp);
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) return java.nio.file.Paths.get(localAppData, "Temp");
        return java.nio.file.Paths.get(System.getProperty("user.home"), "AppData", "Local", "Temp");
    }

    static Path runtimeModsDir(Path gameDir) {
        return configDir().resolve("runtime").resolve(Integer.toHexString(gameDir.toAbsolutePath().normalize().toString().hashCode())).resolve("mods");
    }
}
