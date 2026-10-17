package dev.kissoo.loader;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

final class DownloadSource {
    private static final String BMCL = "https://bmclapi2.bangbang93.com";
    private final boolean domestic;

    private DownloadSource(boolean domestic) {
        this.domestic = domestic;
    }

    static DownloadSource detect(Consumer<String> log) {
        boolean vpnOrProxy = LocalEnvironmentGuard.networkTunnelDetected();
        DownloadSource source = new DownloadSource(!vpnOrProxy);
        if (log != null) {
            log.accept(vpnOrProxy
                    ? "VPN/proxy detected; using official Minecraft sources."
                    : "Direct network detected; using BMCLAPI acceleration with automatic official fallback.");
        }
        return source;
    }

    int assetConcurrency() {
        return domestic ? 24 : 8;
    }

    List<String> candidates(String officialUrl) {
        if (!domestic || officialUrl == null || officialUrl.isBlank()) return List.of(officialUrl);
        String mirror = mirrorUrl(officialUrl);
        return mirror.equals(officialUrl) ? List.of(officialUrl) : List.of(mirror, officialUrl);
    }

    private static String mirrorUrl(String original) {
        try {
            URI uri = URI.create(original);
            String host = uri.getHost();
            if (host == null) return original;
            host = host.toLowerCase(Locale.ROOT);
            String path = uri.getRawPath() == null ? "" : uri.getRawPath();
            String prefix = switch (host) {
                case "resources.download.minecraft.net" -> BMCL + "/assets";
                case "libraries.minecraft.net", "maven.fabricmc.net" -> BMCL + "/maven";
                case "meta.fabricmc.net" -> BMCL + "/fabric-meta";
                case "piston-meta.mojang.com", "piston-data.mojang.com",
                     "launchermeta.mojang.com", "launcher.mojang.com" -> BMCL;
                default -> "";
            };
            if (prefix.isBlank()) return original;
            String query = uri.getRawQuery();
            return prefix + path + (query == null || query.isBlank() ? "" : "?" + query);
        } catch (Throwable ignored) {
            return original;
        }
    }
}
