package dev.kissoo.mod.music;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class MusicPlayerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean desktopLyrics = true;
    public float volume01 = 0.8f;
    public String gdSource = "netease";
    public int gdBitrate = 320;
    public List<String> searchHistory = new ArrayList<>();
    public String lastQuery = "";
    public int lastScroll = 0;

    public static Path path() {
        Path dir = MinecraftClient.getInstance().runDirectory.toPath().resolve("alien").resolve("music");
        return dir.resolve("config.json");
    }

    public static MusicPlayerConfig load() {
        Path p = path();
        try {
            if (!Files.exists(p)) return new MusicPlayerConfig();
            String json = Files.readString(p, StandardCharsets.UTF_8);
            MusicPlayerConfig cfg = GSON.fromJson(json, MusicPlayerConfig.class);
            if (cfg == null) return new MusicPlayerConfig();
            if (cfg.searchHistory == null) cfg.searchHistory = new ArrayList<>();
            cfg.volume01 = clamp01(cfg.volume01);
            if (!isValidSource(cfg.gdSource)) cfg.gdSource = "netease";
            if (!isValidBitrate(cfg.gdBitrate)) cfg.gdBitrate = 320;
            if (cfg.lastQuery == null) cfg.lastQuery = "";
            if (cfg.lastScroll < 0) cfg.lastScroll = 0;
            return cfg;
        } catch (Throwable ignored) {
            return new MusicPlayerConfig();
        }
    }

    public void save() {
        Path p = path();
        try {
            Files.createDirectories(p.getParent());
            Files.writeString(p, GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    public void pushSearch(String q) {
        if (q == null) return;
        String s = q.trim();
        if (s.isEmpty()) return;
        searchHistory.removeIf(x -> x != null && x.equalsIgnoreCase(s));
        searchHistory.add(0, s);
        if (searchHistory.size() > 30) {
            searchHistory = new ArrayList<>(searchHistory.subList(0, 30));
        }
    }

    private static float clamp01(float v) {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }

    private static boolean isValidSource(String source) {
        if (source == null) return false;
        return switch (source.trim().toLowerCase()) {
            case "netease", "kuwo", "joox" -> true;
            default -> false;
        };
    }

    private static boolean isValidBitrate(int bitrate) {
        return bitrate == 128 || bitrate == 192 || bitrate == 320 || bitrate == 740 || bitrate == 999;
    }
}

