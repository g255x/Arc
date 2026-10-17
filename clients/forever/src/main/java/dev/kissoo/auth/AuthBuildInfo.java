package dev.kissoo.auth;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

final class AuthBuildInfo {
    private static final String CLIENT_MOD_ID = "forever";

    private AuthBuildInfo() {
    }

    static String clientVersion() {
        return modVersion(CLIENT_MOD_ID, "dev");
    }

    static String minecraftVersion() {
        return modVersion("minecraft", "unknown");
    }

    static String loaderVersion() {
        return modVersion("fabricloader", "unknown");
    }

    static String buildId() {
        return "forever-" + clientVersion()
                + "+mc-" + minecraftVersion()
                + "+loader-" + loaderVersion();
    }

    static String userAgent() {
        return "Forever/" + headerSafe(clientVersion())
                + " Minecraft/" + headerSafe(minecraftVersion())
                + " FabricLoader/" + headerSafe(loaderVersion());
    }

    private static String modVersion(String modId, String fallback) {
        try {
            return FabricLoader.getInstance()
                    .getModContainer(modId)
                    .map(ModContainer::getMetadata)
                    .map(metadata -> metadata.getVersion().getFriendlyString())
                    .filter(version -> version != null && !version.isBlank() && !version.contains("${"))
                    .orElse(fallback);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String headerSafe(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.replaceAll("[^A-Za-z0-9._+\\-]", "_");
    }
}
