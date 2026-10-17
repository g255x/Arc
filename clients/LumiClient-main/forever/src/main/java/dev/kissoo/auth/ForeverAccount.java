package dev.kissoo.auth;

import net.minecraft.client.MinecraftClient;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Forever-only account session. It is not part of the token/HWID authorization system. */
public final class ForeverAccount {
    private static final String API = "https://52.175.50.86/api/index.php?route=/forever/account";
    private static volatile String username = "";
    private static volatile String session = "";

    static {
        // 安装 TLS 证书固定（幂等）。账号注册/登录可能先于鉴权发生，这里兜底一次。
        PinnedSSL.install();
    }

    private ForeverAccount() {}

    public record Result(boolean ok, String reason) {}

    public static boolean isLoggedIn() { return !username.isBlank() && !session.isBlank(); }
    public static String username() { return username; }
    public static String session() { return session; }

    public static void load() {
        try {
            Path file = file();
            if (!Files.exists(file)) return;
            String json = Files.readString(file);
            username = field(json, "username");
            session = field(json, "session");
        } catch (Exception ignored) {}
    }

    public static Result register(String name, String password) { return submit("/register", name, password); }
    public static Result login(String name, String password) { return submit("/login", name, password); }

    private static Result submit(String path, String name, String password) {
        if (name == null || !name.matches("[A-Za-z0-9_.-]{1,32}")) return new Result(false, "Username must be 1-32 letters, numbers, _, . or -.");
        if (password == null || password.length() < 4) return new Result(false, "Password must contain at least 4 characters.");
        try {
            String body = "{\"username\":\"" + esc(name) + "\",\"password\":\"" + esc(password) + "\",\"hwid\":\"" + esc("unknown") + "\",\"qq\":\"\"}";
            HttpURLConnection conn = (HttpURLConnection) URI.create(API + path).toURL().openConnection();
            conn.setRequestMethod("POST"); conn.setDoOutput(true); conn.setConnectTimeout(5000); conn.setReadTimeout(5000);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            try (OutputStream out = conn.getOutputStream()) { out.write(body.getBytes(StandardCharsets.UTF_8)); }
            InputStream in = conn.getResponseCode() < 400 ? conn.getInputStream() : conn.getErrorStream();
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            if (!json.contains("\"ok\":true")) return new Result(false, field(json, "error"));
            username = field(json, "username"); session = field(json, "session"); save();
            return new Result(true, "");
        } catch (Exception e) { return new Result(false, "Network error: " + e.getClass().getSimpleName()); }
    }

    private static Path file() { return MinecraftClient.getInstance().runDirectory.toPath().resolve("forever-account.json"); }
    private static void save() throws Exception { Files.writeString(file(), "{\"username\":\"" + esc(username) + "\",\"session\":\"" + esc(session) + "\"}"); }
    private static String field(String json, String key) { Matcher m = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"([^\\\"]*)").matcher(json); return m.find() ? m.group(1) : ""; }
    private static String esc(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
}
