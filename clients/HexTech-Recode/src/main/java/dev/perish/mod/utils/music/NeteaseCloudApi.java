package dev.perish.mod.utils.music;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NeteaseCloudApi {
    public static final NeteaseCloudApi INSTANCE = new NeteaseCloudApi();

    private static String API_BASE_URL = "http://music.api.bwyxy.cn";

    public volatile String cookie = "";
    public volatile boolean isLoggedIn = false;
    public volatile UserProfile userProfile = null;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    private final File cookieFile = new File("perish-hyperion", "netease_cookie.txt");

    private NeteaseCloudApi() {
        loadCookie();
    }

    private String buildUrl(String path) {
        return API_BASE_URL + path;
    }

    public static void setApiBaseUrl(String url) {
        if (url != null && !url.isEmpty()) {
            API_BASE_URL = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        }
    }

    private void loadCookie() {
        try {
            if (cookieFile.exists()) {
                String saved = new String(Files.readAllBytes(cookieFile.toPath()), StandardCharsets.UTF_8).trim();
                if (!saved.isEmpty()) {
                    cookie = saved;
                    isLoggedIn = true;
                    new Thread(this::loadUserProfile).start();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveCookie() {
        try {
            File parent = cookieFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try (FileOutputStream fos = new FileOutputStream(cookieFile)) {
                fos.write(cookie.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String parseCookie(String rawCookie) {
        StringBuilder sb = new StringBuilder();
        Pattern regex = Pattern.compile("(MUSIC_U|MUSIC_A_T|MUSIC_R_T|__csrf)=([^;]+)");
        Matcher matcher = regex.matcher(rawCookie);
        boolean first = true;
        while (matcher.find()) {
            String name = matcher.group(1);
            String value = matcher.group(2);
            if (name.equals("MUSIC_U") || name.equals("__csrf")) {
                if (!first) sb.append("; ");
                sb.append(name).append("=").append(value);
                first = false;
            }
        }
        return sb.toString();
    }

    private String doGet(String url) throws Exception {
        return doGet(url, false);
    }

    private String doGet(String url, boolean captureCookie) throws Exception {
        Request.Builder builder = new Request.Builder().url(url).get();
        if (!cookie.isEmpty()) builder.header("Cookie", cookie);
        try (Response response = client.newCall(builder.build()).execute()) {
            if (captureCookie) {
                List<String> cookies = response.headers("Set-Cookie");
                if (!cookies.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    Pattern regex = Pattern.compile("(MUSIC_U|MUSIC_A_T|MUSIC_R_T|__csrf)=([^;]+)");
                    for (String c : cookies) {
                        Matcher m = regex.matcher(c);
                        while (m.find()) {
                            if (sb.length() > 0) sb.append("; ");
                            sb.append(m.group(1)).append("=").append(m.group(2));
                        }
                    }
                    if (sb.length() > 0) {
                        if (!cookie.isEmpty()) cookie += "; " + sb;
                        else cookie = sb.toString();
                    }
                }
            }
            if (response.isSuccessful() && response.body() != null) {
                return response.body().string();
            }
        }
        return null;
    }

    public boolean sendCaptcha(String phone, String ctcode) {
        try {
            String json = doGet(buildUrl("/captcha/sent?phone=" + phone + "&ctcode=" + ctcode));
            if (json != null) {
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                return obj.has("code") && obj.get("code").getAsInt() == 200;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean loginWithCaptcha(String phone, String captcha, String ctcode) {
        try {
            String json = doGet(buildUrl("/login/cellphone?phone=" + phone + "&captcha=" + captcha + "&countrycode=" + ctcode), true);
            if (json != null) {
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                if (obj.has("code") && obj.get("code").getAsInt() == 200) {
                    String rawCookie = obj.has("cookie") ? obj.get("cookie").getAsString() : "";
                    if (!rawCookie.isEmpty()) {
                        String parsed = parseCookie(rawCookie);
                        if (!parsed.isEmpty()) {
                            if (!cookie.isEmpty()) cookie += "; " + parsed;
                            else cookie = parsed;
                        }
                    }
                    isLoggedIn = true;
                    saveCookie();
                    loadUserProfile();
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public String getQrKey() {
        try {
            String json = doGet(buildUrl("/login/qr/key?timestamp=" + System.currentTimeMillis()));
            if (json != null) {
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                if (obj.has("data")) {
                    return obj.getAsJsonObject("data").get("unikey").getAsString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getQrImage(String key) {
        try {
            String json = doGet(buildUrl("/login/qr/create?key=" + key + "&qrimg=true&timestamp=" + System.currentTimeMillis()));
            if (json != null) {
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                if (obj.has("data")) {
                    return obj.getAsJsonObject("data").get("qrimg").getAsString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public QrStatus checkQrStatus(String key) {
        try {
            String json = doGet(buildUrl("/login/qr/check?key=" + key + "&timestamp=" + System.currentTimeMillis()), true);
            if (json != null) {
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                int code = obj.has("code") ? obj.get("code").getAsInt() : 0;
                switch (code) {
                    case 800: return QrStatus.EXPIRED;
                    case 801: return QrStatus.WAITING;
                    case 802: return QrStatus.SCANNED;
                    case 803:
                        String cookieStr = obj.has("cookie") ? obj.get("cookie").getAsString() : "";
                        if (!cookieStr.isEmpty()) {
                            String parsed = parseCookie(cookieStr);
                            if (!parsed.isEmpty()) {
                                if (!cookie.isEmpty()) cookie += "; " + parsed;
                                else cookie = parsed;
                            }
                        }
                        isLoggedIn = true;
                        saveCookie();
                        loadUserProfile();
                        return QrStatus.SUCCESS;
                    default: return QrStatus.WAITING;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return QrStatus.WAITING;
    }

    public void loadUserProfile() {
        try {
            String json = doGet(buildUrl("/login/status?timestamp=" + System.currentTimeMillis()));
            if (json != null) {
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                if (obj.has("data")) {
                    JsonObject data = obj.getAsJsonObject("data");
                    if (data.has("profile") && !data.get("profile").isJsonNull()) {
                        JsonObject profile = data.getAsJsonObject("profile");
                        userProfile = new UserProfile(
                                profile.has("userId") ? profile.get("userId").getAsLong() : 0L,
                                profile.has("nickname") ? profile.get("nickname").getAsString() : "",
                                profile.has("avatarUrl") ? profile.get("avatarUrl").getAsString() : ""
                        );
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void logout() {
        cookie = "";
        isLoggedIn = false;
        userProfile = null;
        try {
            if (cookieFile.exists()) cookieFile.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Song> getTopNewSongs(int type) {
        List<Song> result = new ArrayList<>();
        try {
            String json = doGet(buildUrl("/top/song?type=" + type));
            if (json != null) {
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                if (obj.has("data")) {
                    JsonArray arr = obj.getAsJsonArray("data");
                    for (int i = 0; i < Math.min(20, arr.size()); i++) {
                        JsonObject item = arr.get(i).getAsJsonObject();
                        String name = item.has("name") ? item.get("name").getAsString() : "";
                        long id = item.has("id") ? item.get("id").getAsLong() : 0L;
                        long duration = item.has("duration") ? item.get("duration").getAsLong() : 0L;
                        String picUrl = "";
                        String singer = "";
                        if (item.has("album") && !item.get("album").isJsonNull()) {
                            JsonObject album = item.getAsJsonObject("album");
                            picUrl = album.has("picUrl") ? album.get("picUrl").getAsString() : "";
                        }
                        if (item.has("artists") && item.get("artists").isJsonArray()) {
                            JsonArray artists = item.getAsJsonArray("artists");
                            if (artists.size() > 0) {
                                singer = artists.get(0).getAsJsonObject().get("name").getAsString();
                            }
                        }
                        result.add(new Song(picUrl, name, singer, id, duration));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public List<Playlist> getRecommendPlaylists(int limit) {
        List<Playlist> result = new ArrayList<>();
        try {
            String json = doGet(buildUrl("/personalized?limit=" + limit));
            if (json != null) {
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                if (obj.has("result")) {
                    JsonArray arr = obj.getAsJsonArray("result");
                    for (int i = 0; i < arr.size(); i++) {
                        JsonObject item = arr.get(i).getAsJsonObject();
                        result.add(new Playlist(
                                item.has("id") ? item.get("id").getAsLong() : 0L,
                                item.has("name") ? item.get("name").getAsString() : "",
                                item.has("picUrl") ? item.get("picUrl").getAsString() : "",
                                item.has("playCount") ? item.get("playCount").getAsLong() : 0L
                        ));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public List<Song> getPlaylistDetail(long id) {
        return getPlaylistTracks(id, 50, 0);
    }

    public Long getUserLikePlaylistId() {
        if (userProfile == null) return null;
        try {
            String json = doGet(buildUrl("/user/playlist?uid=" + userProfile.userId + "&timestamp=" + System.currentTimeMillis()));
            if (json != null) {
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                if (obj.has("playlist")) {
                    JsonArray arr = obj.getAsJsonArray("playlist");
                    if (arr.size() > 0) {
                        return arr.get(0).getAsJsonObject().get("id").getAsLong();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Song> getPlaylistTracks(long id, int limit, int offset) {
        List<Song> result = new ArrayList<>();
        try {
            String json = doGet(buildUrl("/playlist/track/all?id=" + id + "&limit=" + limit + "&offset=" + offset));
            if (json != null) {
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                if (obj.has("songs")) {
                    JsonArray arr = obj.getAsJsonArray("songs");
                    for (int i = 0; i < arr.size(); i++) {
                        JsonObject item = arr.get(i).getAsJsonObject();
                        String name = item.has("name") ? item.get("name").getAsString() : "";
                        long songId = item.has("id") ? item.get("id").getAsLong() : 0L;
                        long duration = item.has("dt") ? item.get("dt").getAsLong() : 0L;
                        String picUrl = "";
                        String singer = "";
                        if (item.has("al") && !item.get("al").isJsonNull()) {
                            JsonObject al = item.getAsJsonObject("al");
                            picUrl = al.has("picUrl") ? al.get("picUrl").getAsString() : "";
                        }
                        if (item.has("ar") && item.get("ar").isJsonArray()) {
                            JsonArray ar = item.getAsJsonArray("ar");
                            if (ar.size() > 0) {
                                singer = ar.get(0).getAsJsonObject().get("name").getAsString();
                            }
                        }
                        result.add(new Song(picUrl, name, singer, songId, duration));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public List<Song> getLikeSongs(int limit, int offset) {
        Long playlistId = getUserLikePlaylistId();
        if (playlistId == null) return new ArrayList<>();
        return getPlaylistTracks(playlistId, limit, offset);
    }

    public SongFile getFile(long id) {
        try {
            String json = doGet(buildUrl("/song/url/v1?id=" + id + "&level=exhigh"));
            if (json != null) {
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                if (obj.has("data")) {
                    JsonArray arr = obj.getAsJsonArray("data");
                    if (arr.size() > 0) {
                        JsonObject data = arr.get(0).getAsJsonObject();
                        String url = data.has("url") && !data.get("url").isJsonNull() ? data.get("url").getAsString() : "";
                        long size = data.has("size") ? data.get("size").getAsLong() : 0L;
                        if (!url.isEmpty()) {
                            new Thread(() -> getLyric(id)).start();
                            return new SongFile(url, size);
                        }
                    }
                }
            }
            json = doGet(buildUrl("/song/url?id=" + id));
            if (json != null) {
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                if (obj.has("data")) {
                    JsonArray arr = obj.getAsJsonArray("data");
                    if (arr.size() > 0) {
                        JsonObject data = arr.get(0).getAsJsonObject();
                        String url = data.has("url") && !data.get("url").isJsonNull() ? data.get("url").getAsString() : "";
                        long size = data.has("size") ? data.get("size").getAsLong() : 0L;
                        new Thread(() -> getLyric(id)).start();
                        return new SongFile(url, size);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new SongFile("", 0);
    }

    public void getLyric(long id) {
        try {
            String json = doGet(buildUrl("/lyric?id=" + id));
            if (json != null) {
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                String lyricStr = "";
                if (obj.has("lrc") && !obj.get("lrc").isJsonNull()) {
                    JsonObject lrc = obj.getAsJsonObject("lrc");
                    lyricStr = lrc.has("lyric") ? lrc.get("lyric").getAsString() : "";
                }
                synchronized (MediaPlayer.INSTANCE.lyric) {
                    MediaPlayer.INSTANCE.lyric.clear();
                    MediaPlayer.INSTANCE.lyric.addAll(LyricLineProcessor.parseLyricLine(lyricStr));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Song> search(String name) {
        List<Song> result = new ArrayList<>();
        try {
            String encoded = URLEncoder.encode(name, "UTF-8");
            String json = doGet(buildUrl("/cloudsearch?keywords=" + encoded));
            if (json != null) {
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                if (obj.has("result")) {
                    JsonObject res = obj.getAsJsonObject("result");
                    if (res.has("songs")) {
                        JsonArray arr = res.getAsJsonArray("songs");
                        for (int i = 0; i < Math.min(30, arr.size()); i++) {
                            JsonObject item = arr.get(i).getAsJsonObject();
                            String songName = item.has("name") ? item.get("name").getAsString() : "";
                            long id = item.has("id") ? item.get("id").getAsLong() : 0L;
                            long duration = item.has("dt") ? item.get("dt").getAsLong() : 0L;
                            String picUrl = "";
                            String singer = "";
                            if (item.has("al") && !item.get("al").isJsonNull()) {
                                JsonObject al = item.getAsJsonObject("al");
                                picUrl = al.has("picUrl") ? al.get("picUrl").getAsString() : "";
                            }
                            if (item.has("ar") && item.get("ar").isJsonArray()) {
                                JsonArray ar = item.getAsJsonArray("ar");
                                if (ar.size() > 0) {
                                    singer = ar.get(0).getAsJsonObject().get("name").getAsString();
                                }
                            }
                            result.add(new Song(picUrl, songName, singer, id, duration));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public boolean playSong(Song song) {
        try {
            SongFile songFile = getFile(song.id);
            if (songFile.url == null || songFile.url.isEmpty()) return false;

            File cacheDir = new File("perish-hyperion", "cache");
            if (!cacheDir.exists()) {
                if (!cacheDir.mkdirs()) return false;
            }

            File file = new File(cacheDir, song.id + ".mp3");
            if (file.exists() && file.length() >= songFile.size) {
                MediaPlayer.INSTANCE.playSound(file, song);
                return true;
            }

            Request downloadReq = new Request.Builder().url(songFile.url).build();
            try (Response dlResp = client.newCall(downloadReq).execute()) {
                if (!dlResp.isSuccessful() || dlResp.body() == null) return false;
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    dlResp.body().byteStream().transferTo(fos);
                }
            }

            if (!file.exists() || file.length() == 0) return false;
            MediaPlayer.INSTANCE.playSound(file, song);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
