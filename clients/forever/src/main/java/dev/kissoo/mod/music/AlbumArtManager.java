package dev.kissoo.mod.music;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class AlbumArtManager {
    private static final int MAX_CACHE_SIZE = 100;

    private final Set<Long> inFlight = ConcurrentHashMap.newKeySet();
    private final Map<Long, CachedTexture> cache = new LinkedHashMap<Long, CachedTexture>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, CachedTexture> eldest) {
            if (size() > MAX_CACHE_SIZE) {
                MinecraftClient.getInstance().execute(() -> {
                    eldest.getValue().destroy();
                });
                return true;
            }
            return false;
        }
    };

    private volatile String lastDebug = "";

    private static class CachedTexture {
        final Identifier id;
        final NativeImageBackedTexture texture;
        long lastAccessTime;

        CachedTexture(Identifier id, NativeImageBackedTexture texture) {
            this.id = id;
            this.texture = texture;
            this.lastAccessTime = System.currentTimeMillis();
        }

        void destroy() {
            try {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc != null && mc.getTextureManager() != null) {
                    mc.getTextureManager().destroyTexture(id);
                }
                if (texture != null) {
                    texture.close();
                }
            } catch (Throwable ignored) {}
        }
    }

    public String getLastDebug() {
        return lastDebug;
    }

    public Identifier getOrRequest(long songId, String picUrl) {
        if (songId <= 0) return null;
        if (picUrl == null || picUrl.isBlank()) return null;

        MinecraftClient mc = MinecraftClient.getInstance();
        Identifier id = Identifier.of("alien", "music/album_" + songId);

        synchronized (cache) {
            CachedTexture cached = cache.get(songId);
            if (cached != null) {
                cached.lastAccessTime = System.currentTimeMillis();
                return cached.id;
            }
        }

        if (!inFlight.add(songId)) return null;

        CompletableFuture.runAsync(() -> {
            try {
                NativeImage img = downloadNativeImage(picUrl);

                if (img == null) {
                    lastDebug = "albumArt fail id=" + songId;
                    return;
                }

                mc.execute(() -> {
                    try {
                        NativeImageBackedTexture texture = new NativeImageBackedTexture(img);
                        mc.getTextureManager().registerTexture(id, texture);

                        synchronized (cache) {
                            cache.put(songId, new CachedTexture(id, texture));
                        }

                        lastDebug = "albumArt ok id=" + songId;
                    } catch (Throwable t) {
                        lastDebug = "albumArt register fail id=" + songId;
                    }
                });
            } catch (Throwable t) {
                lastDebug = "albumArt ex id=" + songId + " " + t.getClass().getSimpleName();
            } finally {
                inFlight.remove(songId);
            }
        });

        return null;
    }

    public void clearAll() {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            synchronized (cache) {
                for (CachedTexture ct : cache.values()) {
                    ct.destroy();
                }
                cache.clear();
            }
        });
        inFlight.clear();
    }

    public void cleanupOld(long maxAgeMs) {
        long now = System.currentTimeMillis();
        MinecraftClient mc = MinecraftClient.getInstance();

        mc.execute(() -> {
            synchronized (cache) {
                Iterator<Map.Entry<Long, CachedTexture>> it = cache.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<Long, CachedTexture> entry = it.next();
                    if (now - entry.getValue().lastAccessTime > maxAgeMs) {
                        entry.getValue().destroy();
                        it.remove();
                    }
                }
            }
        });
    }

    private static NativeImage downloadNativeImage(String picUrl) {
        NativeImage img = tryDownload(picUrl, true);
        if (img != null) {
            return img;
        }
        return tryDownload(picUrl, false);
    }

    private static NativeImage tryDownload(String picUrl, boolean useParam) {
        try {
            String url = picUrl;
            if (url.startsWith("http://")) {
                url = "https://" + url.substring("http://".length());
            }
            if (useParam && !url.contains("param=")) {
                url = url + (url.contains("?") ? "&" : "?") + "param=130y130";
            }

            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; rv:11.0) like Gecko");
            conn.setRequestProperty("Accept", "image/jpeg, image/png");

            int code = conn.getResponseCode();
            if (code == 200) {
                try (InputStream in = conn.getInputStream()) {
                    return NativeImage.read(in);
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}