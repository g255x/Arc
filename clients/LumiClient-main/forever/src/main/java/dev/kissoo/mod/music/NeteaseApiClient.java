package dev.kissoo.mod.music;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NeteaseApiClient {
    private static final Gson GSON = new Gson();
    private static final String GD_API_BASE = "https://music-api.gdstudio.xyz/api.php";
    private static final String GD_DEFAULT_SOURCE = "netease";
    private static final String GD_ATTRIBUTION = "GD音乐台(music.gdstudio.xyz)";

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private volatile Map<String, String> authCookies = new ConcurrentHashMap<>();
    private final Map<String, String> gdPicCache = new ConcurrentHashMap<>();
    private volatile String gdSource = GD_DEFAULT_SOURCE;
    private volatile int gdBitrate = 320;

    public void setGdSource(String source) {
        if (source == null || source.isBlank()) {
            gdSource = GD_DEFAULT_SOURCE;
            return;
        }
        String normalized = source.trim().toLowerCase();
        gdSource = switch (normalized) {
            case "netease", "kuwo", "joox" -> normalized;
            default -> GD_DEFAULT_SOURCE;
        };
    }

    public String getGdSource() {
        return gdSource;
    }

    public void setGdBitrate(int bitrate) {
        gdBitrate = switch (bitrate) {
            case 128, 192, 320, 740, 999 -> bitrate;
            default -> 320;
        };
    }

    public int getGdBitrate() {
        return gdBitrate;
    }

    public void setAuthCookies(Map<String, String> cookies) {
        authCookies.clear();
        if (cookies != null) {
            authCookies.putAll(cookies);
        }
    }

    public void clearAuthCookies() {
        authCookies.clear();
    }

    public boolean isAuthenticated() {
        return authCookies.containsKey("MUSIC_U") || authCookies.containsKey("__csrf");
    }

    private String buildCookieString() {
        if (authCookies.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : authCookies.entrySet()) {
            if (sb.length() > 0) sb.append("; ");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }
    public List<NeteaseUser> searchUsers(String keywords, int limit, int offset) throws IOException, InterruptedException {
        String q = URLEncoder.encode(keywords, StandardCharsets.UTF_8);
        int lim = Math.max(1, Math.min(limit, 30));
        int off = Math.max(0, offset);
        URI uri = URI.create("https://music.163.com/api/search/get?type=1002&s=" + q + "&limit=" + lim + "&offset=" + off);
        HttpRequest req = baseRequest(uri).GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (resp.statusCode() != 200) {
            throw new IOException("user search http " + resp.statusCode());
        }

        JsonObject root = GSON.fromJson(resp.body(), JsonObject.class);
        JsonObject result = root == null ? null : root.getAsJsonObject("result");
        JsonArray userprofiles = result == null ? null : result.getAsJsonArray("userprofiles");

        ArrayList<NeteaseUser> out = new ArrayList<>();
        if (userprofiles == null) return out;

        for (JsonElement el : userprofiles) {
            if (!el.isJsonObject()) continue;
            JsonObject u = el.getAsJsonObject();

            long userId = u.has("userId") ? u.get("userId").getAsLong() : -1;
            String nickname = u.has("nickname") ? u.get("nickname").getAsString() : "Unknown";
            String avatarUrl = u.has("avatarUrl") ? u.get("avatarUrl").getAsString() : null;
            String signature = u.has("signature") ? u.get("signature").getAsString() : "";

            if (userId > 0) {
                out.add(new NeteaseUser(userId, nickname, avatarUrl, signature));
            }
        }

        return out;
    }

    public List<NeteaseSong> getUserLikedSongs(long userId, int limit) throws IOException, InterruptedException {
        URI playlistUri = URI.create("https://music.163.com/api/user/playlist?uid=" + userId + "&limit=1&offset=0");
        HttpRequest playlistReq = baseRequest(playlistUri).GET().build();
        HttpResponse<String> playlistResp = http.send(playlistReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (playlistResp.statusCode() != 200) {
            throw new IOException("user playlist http " + playlistResp.statusCode());
        }

        JsonObject playlistRoot = GSON.fromJson(playlistResp.body(), JsonObject.class);
        JsonArray playlists = playlistRoot == null ? null : playlistRoot.getAsJsonArray("playlist");

        if (playlists == null || playlists.isEmpty()) {
            return new ArrayList<>();
        }
        JsonObject likedPlaylist = playlists.get(0).getAsJsonObject();
        long playlistId = likedPlaylist.get("id").getAsLong();
        return getPlaylistSongs(playlistId, limit);
    }

    public List<NeteaseSong> getPlaylistSongs(long playlistId, int limit) throws IOException, InterruptedException {
        int lim = Math.max(1, Math.min(limit, 1000));
        URI detailUri = URI.create("https://music.163.com/api/v3/playlist/detail?id=" + playlistId + "&n=" + lim);
        HttpRequest detailReq = baseRequest(detailUri).GET().build();
        HttpResponse<String> detailResp = http.send(detailReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (detailResp.statusCode() != 200) {
            throw new IOException("playlist detail http " + detailResp.statusCode());
        }

        JsonObject detailRoot = GSON.fromJson(detailResp.body(), JsonObject.class);
        JsonObject playlist = detailRoot == null ? null : detailRoot.getAsJsonObject("playlist");

        if (playlist == null) {
            return new ArrayList<>();
        }
        JsonArray trackIds = playlist.getAsJsonArray("trackIds");
        if (trackIds == null || trackIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> songIds = new ArrayList<>();
        int count = Math.min(trackIds.size(), lim);
        for (int i = 0; i < count; i++) {
            JsonObject track = trackIds.get(i).getAsJsonObject();
            if (track.has("id")) {
                songIds.add(track.get("id").getAsLong());
            }
        }

        if (songIds.isEmpty()) {
            return new ArrayList<>();
        }
        return getSongDetails(songIds);
    }
    public List<NeteaseSong> getSongDetails(List<Long> songIds) throws IOException, InterruptedException {
        if (songIds == null || songIds.isEmpty()) {
            return new ArrayList<>();
        }
        StringBuilder idsParam = new StringBuilder("[");
        for (int i = 0; i < songIds.size(); i++) {
            if (i > 0) idsParam.append(",");
            idsParam.append(songIds.get(i));
        }
        idsParam.append("]");
        URI detailUri = URI.create("https://music.163.com/api/song/detail?ids=" +
                URLEncoder.encode(idsParam.toString(), StandardCharsets.UTF_8));
        HttpRequest detailReq = baseRequest(detailUri).GET().build();
        HttpResponse<String> detailResp = http.send(detailReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (detailResp.statusCode() != 200) {
            throw new IOException("song detail http " + detailResp.statusCode());
        }

        JsonObject detailRoot = GSON.fromJson(detailResp.body(), JsonObject.class);
        JsonArray songs = detailRoot == null ? null : detailRoot.getAsJsonArray("songs");

        return parseSongs(songs);
    }
    private List<NeteaseSong> parseSongs(JsonArray songs) {
        ArrayList<NeteaseSong> out = new ArrayList<>();
        if (songs == null) return out;

        for (JsonElement el : songs) {
            if (!el.isJsonObject()) continue;
            JsonObject s = el.getAsJsonObject();

            long id = s.has("id") ? s.get("id").getAsLong() : -1;
            String name = s.has("name") ? s.get("name").getAsString() : "Unknown";
            long duration = -1;
            if (s.has("duration")) duration = s.get("duration").getAsLong();
            else if (s.has("dt")) duration = s.get("dt").getAsLong();
            StringBuilder artists = new StringBuilder();
            JsonArray arr = s.has("artists") ? s.getAsJsonArray("artists") :
                    (s.has("ar") ? s.getAsJsonArray("ar") : null);
            if (arr != null) {
                for (JsonElement ael : arr) {
                    if (!ael.isJsonObject()) continue;
                    String an = ael.getAsJsonObject().has("name") ?
                            ael.getAsJsonObject().get("name").getAsString() : "";
                    if (an.isBlank()) continue;
                    if (!artists.isEmpty()) artists.append(" / ");
                    artists.append(an);
                }
            }
            String picUrl = null;
            JsonObject alb = s.has("album") ? s.getAsJsonObject("album") :
                    (s.has("al") ? s.getAsJsonObject("al") : null);
            if (alb != null && alb.has("picUrl")) {
                picUrl = alb.get("picUrl").getAsString();
            }

            if (id > 0) {
                out.add(new NeteaseSong(id, name, artists.toString(), picUrl, duration));
            }
        }

        return out;
    }


    public List<NeteaseSong> search(String keywords, int limit, int offset) throws IOException, InterruptedException {
        String q = URLEncoder.encode(keywords, StandardCharsets.UTF_8);
        int lim = Math.max(1, Math.min(limit, 50));
        int off = Math.max(0, offset);
        int page = Math.max(1, off / lim + 1);

        URI gdUri = URI.create(GD_API_BASE
                + "?types=search"
                + "&source=" + URLEncoder.encode(gdSource, StandardCharsets.UTF_8)
                + "&name=" + q
                + "&count=" + lim
                + "&pages=" + page);
        try {
            HttpResponse<String> gdResp = http.send(gdRequest(gdUri).GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (gdResp.statusCode() == 200) {
                List<NeteaseSong> gdSongs = parseGdSearch(gdResp.body());
                if (!gdSongs.isEmpty()) return gdSongs;
            } else {
                System.err.println("[MusicPlayer] GD search http " + gdResp.statusCode() + ", fallback to netease api");
            }
        } catch (Throwable t) {
            System.err.println("[MusicPlayer] GD search failed, fallback to netease api: " + t.getMessage());
        }

        URI uri = URI.create("https://music.163.com/api/search/get?type=1&s=" + q + "&limit=" + lim + "&offset=" + off);
        HttpRequest req = baseRequest(uri).GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 200) {
            throw new IOException("search http " + resp.statusCode());
        }

        JsonObject root = GSON.fromJson(resp.body(), JsonObject.class);
        JsonObject result = root == null ? null : root.getAsJsonObject("result");
        JsonArray songs = result == null ? null : result.getAsJsonArray("songs");
        if (songs == null || songs.isEmpty()) return new ArrayList<>();
        StringBuilder idsParam = new StringBuilder("[");
        for (int i = 0; i < songs.size(); i++) {
            JsonObject s = songs.get(i).getAsJsonObject();
            if (s.has("id")) {
                if (i > 0) idsParam.append(",");
                idsParam.append(s.get("id").getAsLong());
            }
        }
        idsParam.append("]");
        URI detailUri = URI.create("https://music.163.com/api/song/detail?ids=" +
                URLEncoder.encode(idsParam.toString(), StandardCharsets.UTF_8));
        HttpRequest detailReq = baseRequest(detailUri).GET().build();
        HttpResponse<String> detailResp = http.send(detailReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (detailResp.statusCode() != 200) {
            throw new IOException("detail http " + detailResp.statusCode());
        }

        JsonObject detailRoot = GSON.fromJson(detailResp.body(), JsonObject.class);
        JsonArray detailSongs = detailRoot == null ? null : detailRoot.getAsJsonArray("songs");

        return parseSongs(detailSongs);
    }

    private List<NeteaseSong> parseGdSearch(String json) {
        ArrayList<NeteaseSong> out = new ArrayList<>();
        JsonObject root = GSON.fromJson(json, JsonObject.class);
        JsonArray songs = root == null ? null : root.getAsJsonArray("value");
        if (songs == null) return out;
        for (JsonElement el : songs) {
            if (!el.isJsonObject()) continue;
            JsonObject s = el.getAsJsonObject();
            String idRaw = getString(s, "id");
            if (idRaw.isBlank()) continue;
            long id = parseLongSafe(idRaw);
            if (id <= 0) id = stableSyntheticId(sourceKey(getString(s, "source"), idRaw));
            String name = getString(s, "name");
            if (name.isBlank()) name = "Unknown";
            String artists = joinStringArray(s.getAsJsonArray("artist"));
            String source = getString(s, "source");
            if (source.isBlank()) source = gdSource;
            String picId = getString(s, "pic_id");
            String lyricId = getString(s, "lyric_id");
            if (lyricId.isBlank()) lyricId = idRaw;
            out.add(new NeteaseSong(id, idRaw, name, artists, null, 0, source, picId, lyricId));
        }
        return out;
    }

    public List<NeteaseArtist> searchArtists(String keywords, int limit, int offset) throws IOException, InterruptedException {
        String q = URLEncoder.encode(keywords, StandardCharsets.UTF_8);
        int lim = Math.max(1, Math.min(limit, 50));
        int off = Math.max(0, offset);
        URI uri = URI.create("https://music.163.com/api/search/get?type=100&s=" + q + "&limit=" + lim + "&offset=" + off);
        HttpResponse<String> resp = http.send(baseRequest(uri).GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 200) throw new IOException("artist search http " + resp.statusCode());
        JsonObject root = GSON.fromJson(resp.body(), JsonObject.class);
        JsonObject result = root == null ? null : root.getAsJsonObject("result");
        JsonArray artists = result == null ? null : result.getAsJsonArray("artists");
        ArrayList<NeteaseArtist> out = new ArrayList<>();
        if (artists == null) return out;
        for (JsonElement el : artists) {
            if (!el.isJsonObject()) continue;
            JsonObject a = el.getAsJsonObject();
            long id = a.has("id") ? a.get("id").getAsLong() : -1;
            String name = a.has("name") ? a.get("name").getAsString() : "Unknown";
            String picUrl = a.has("picUrl") ? a.get("picUrl").getAsString() : null;
            if (id > 0) out.add(new NeteaseArtist(id, name, picUrl));
        }
        return out;
    }

    public List<NeteaseSong> artistTopSongs(long artistId, int limit) throws IOException, InterruptedException {
        int lim = Math.max(1, Math.min(limit, 100));
        URI uri = URI.create("https://music.163.com/api/artist/top/song?id=" + artistId + "&limit=" + lim);
        HttpResponse<String> resp = http.send(baseRequest(uri).GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 200) throw new IOException("artist songs http " + resp.statusCode());
        JsonObject root = GSON.fromJson(resp.body(), JsonObject.class);
        JsonArray songs = root == null ? null : root.getAsJsonArray("songs");
        return parseSongs(songs);
    }

    public String lyric(long songId) throws IOException, InterruptedException {
        return lyric(new NeteaseSong(songId, "", "", null, 0));
    }

    public String lyric(NeteaseSong song) throws IOException, InterruptedException {
        long songId = song == null ? -1 : song.id();
        String source = sourceOrDefault(song == null ? "" : song.source());
        String lyricId = song == null ? "" : song.lyricId();
        if (lyricId == null || lyricId.isBlank()) lyricId = song == null ? "" : song.rawId();
        if (lyricId == null || lyricId.isBlank()) lyricId = String.valueOf(songId);
        try {
            URI gdUri = URI.create(GD_API_BASE
                    + "?types=lyric"
                    + "&source=" + URLEncoder.encode(source, StandardCharsets.UTF_8)
                    + "&id=" + URLEncoder.encode(lyricId, StandardCharsets.UTF_8));
            HttpResponse<String> gdResp = http.send(gdRequest(gdUri).GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (gdResp.statusCode() == 200) {
                JsonObject root = GSON.fromJson(gdResp.body(), JsonObject.class);
                String lyric = root == null ? "" : getString(root, "lyric");
                String tlyric = root == null ? "" : getString(root, "tlyric");
                if (!lyric.isBlank()) {
                    if (!tlyric.isBlank()) return lyric + "\n" + tlyric;
                    return lyric;
                }
            }
        } catch (Throwable t) {
            System.err.println("[MusicPlayer] GD lyric failed, fallback to netease api: " + t.getMessage());
        }
        URI uri = URI.create("https://music.163.com/api/song/lyric?id=" + songId + "&lv=1&kv=1&tv=-1");
        HttpRequest req = baseRequest(uri).GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 200) {
            throw new IOException("lyric http " + resp.statusCode());
        }

        JsonObject root = GSON.fromJson(resp.body(), JsonObject.class);
        if (root == null) return "";
        JsonObject lrc = root.has("lrc") && root.get("lrc").isJsonObject() ? root.getAsJsonObject("lrc") : null;
        if (lrc == null || !lrc.has("lyric")) return "";
        return lrc.get("lyric").getAsString();
    }

    public URI streamUrl(long songId) throws IOException, InterruptedException {
        return streamUrl(new NeteaseSong(songId, "", "", null, 0));
    }

    public URI streamUrl(NeteaseSong song) throws IOException, InterruptedException {
        long songId = song == null ? -1 : song.id();
        String source = sourceOrDefault(song == null ? "" : song.source());
        String trackId = song == null ? "" : song.rawId();
        if (trackId == null || trackId.isBlank()) trackId = String.valueOf(songId);
        try {
            URI gdUri = URI.create(GD_API_BASE
                    + "?types=url"
                    + "&source=" + URLEncoder.encode(source, StandardCharsets.UTF_8)
                    + "&id=" + URLEncoder.encode(trackId, StandardCharsets.UTF_8)
                    + "&br=" + gdBitrate);
            HttpResponse<String> gdResp = http.send(gdRequest(gdUri).GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (gdResp.statusCode() == 200) {
                JsonObject root = GSON.fromJson(gdResp.body(), JsonObject.class);
                String url = root == null ? "" : getString(root, "url");
                if (!url.isBlank()) return URI.create(url);
                throw new IOException("GD returned empty url (" + GD_ATTRIBUTION + ")");
            }
        } catch (Throwable t) {
            System.err.println("[MusicPlayer] GD url failed, fallback to netease api: " + t.getMessage());
        }
        if (isAuthenticated()) {
            try {
                String body = "ids=[" + songId + "]&level=exhigh&encodeType=mp3&csrf_token=";
                String csrf = authCookies.getOrDefault("__csrf", "");
                body += csrf;

                URI uri = URI.create("https://music.163.com/api/song/enhance/player/url/v1");
                HttpRequest req = HttpRequest.newBuilder(uri)
                        .timeout(Duration.ofSeconds(10))
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .header("Referer", "https://music.163.com/")
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("Cookie", buildCookieString())
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();

                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (resp.statusCode() == 200) {
                    JsonObject root = GSON.fromJson(resp.body(), JsonObject.class);
                    JsonArray data = root == null ? null : root.getAsJsonArray("data");
                    if (data != null && !data.isEmpty()) {
                        JsonObject item = data.get(0).getAsJsonObject();
                        if (item.has("url") && !item.get("url").isJsonNull()) {
                            String url = item.get("url").getAsString();
                            if (url != null && !url.isBlank()) {
                                return URI.create(url);
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                System.err.println("[NeteaseApi] VIP url fetch failed, fallback: " + t.getMessage());
            }
        }
        return URI.create("https://music.163.com/song/media/outer/url?id=" + songId + ".mp3");
    }

    public String resolvePicUrl(NeteaseSong song) {
        if (song == null) return null;
        if (song.picUrl() != null && !song.picUrl().isBlank()) return song.picUrl();
        String picId = song.picId();
        if (picId == null || picId.isBlank()) return null;
        String source = song.source();
        if (source == null || source.isBlank()) source = GD_DEFAULT_SOURCE;
        String key = source + ":" + picId;
        String cached = gdPicCache.get(key);
        if (cached != null) return cached.isBlank() ? null : cached;
        try {
            URI gdUri = URI.create(GD_API_BASE
                    + "?types=pic"
                    + "&source=" + URLEncoder.encode(source, StandardCharsets.UTF_8)
                    + "&id=" + URLEncoder.encode(picId, StandardCharsets.UTF_8)
                    + "&size=300");
            HttpResponse<String> gdResp = http.send(gdRequest(gdUri).GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (gdResp.statusCode() == 200) {
                JsonObject root = GSON.fromJson(gdResp.body(), JsonObject.class);
                String url = root == null ? "" : getString(root, "url");
                gdPicCache.put(key, url == null ? "" : url);
                return url == null || url.isBlank() ? null : url;
            }
        } catch (Throwable t) {
            System.err.println("[MusicPlayer] GD pic failed: " + t.getMessage());
        }
        gdPicCache.put(key, "");
        return null;
    }

    private HttpRequest.Builder baseRequest(URI uri) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Referer", "https://music.163.com/");
        String cookies = buildCookieString();
        if (!cookies.isEmpty()) {
            builder.header("Cookie", cookies);
        }

        return builder;
    }

    private HttpRequest.Builder gdRequest(URI uri) {
        return HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "ForeverClient/1.0 (GD-Music-Platform; music.gdstudio.xyz)")
                .header("Referer", "https://music.gdstudio.org/");
    }

    private static String getString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return "";
        try {
            return obj.get(key).getAsString();
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static long parseLongSafe(String raw) {
        if (raw == null || raw.isBlank()) return -1;
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static String joinStringArray(JsonArray arr) {
        if (arr == null || arr.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (JsonElement el : arr) {
            if (el == null || el.isJsonNull()) continue;
            String s;
            try {
                s = el.getAsString();
            } catch (Throwable ignored) {
                continue;
            }
            if (s == null || s.isBlank()) continue;
            if (!sb.isEmpty()) sb.append(" / ");
            sb.append(s);
        }
        return sb.toString();
    }

    private String sourceOrDefault(String source) {
        if (source == null || source.isBlank()) return gdSource == null || gdSource.isBlank() ? GD_DEFAULT_SOURCE : gdSource;
        return source.trim().toLowerCase();
    }

    private static String sourceKey(String source, String id) {
        String s = source == null || source.isBlank() ? GD_DEFAULT_SOURCE : source.trim().toLowerCase();
        String i = id == null ? "" : id.trim();
        return s + ":" + i;
    }

    private static long stableSyntheticId(String key) {
        long h = 1125899906842597L;
        for (int i = 0; i < key.length(); i++) {
            h = 31 * h + key.charAt(i);
        }
        return h == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(h);
    }
}
