package dev.kissoo.mod.music;

import com.google.gson.*;
import net.minecraft.client.MinecraftClient;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class NeteaseLoginManager {
    private static final String BASE_URL = "https://music.163.com";

    private static Path cookieSavePath() {
        return MinecraftClient.getInstance().runDirectory.toPath()
                .resolve("alien").resolve("music").resolve("login.json");
    }

    private final String sessionCookie;
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private String currentUnikey;
    private LoginState loginState = LoginState.NONE;
    private Map<String, String> loginCookies = new HashMap<>();
    private static NeteaseLoginManager INSTANCE;
    private ScheduledExecutorService scheduler;
    private AtomicBoolean polling = new AtomicBoolean(false);
    private Consumer<Map<String, String>> onCookieChangeListener;

    public static synchronized NeteaseLoginManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new NeteaseLoginManager();
        }
        return INSTANCE;
    }

    public void setOnCookieChangeListener(Consumer<Map<String, String>> listener) {
        this.onCookieChangeListener = listener;
    }

    public NeteaseLoginManager() {
        String randomCsrf = UUID.randomUUID().toString().replace("-", "");
        this.sessionCookie = "os=pc; appver=2.9.7; osver=Microsoft-Windows-10-Professional-build-19044-64bit; channel=netease; __csrf=" + randomCsrf;
        loadCookiesFromDisk();
    }
    private void loadCookiesFromDisk() {
        try {
            Path p = cookieSavePath();
            if (!Files.exists(p)) return;
            String json = Files.readString(p, StandardCharsets.UTF_8);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            Map<String, String> loaded = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                loaded.put(entry.getKey(), entry.getValue().getAsString());
            }
            if (!loaded.isEmpty()) {
                loginCookies.putAll(loaded);
                loginState = LoginState.SUCCESS;
                System.out.println("[NeteaseLoginManager] Loaded saved cookies, auto logged in.");
                if (onCookieChangeListener != null) {
                    onCookieChangeListener.accept(new HashMap<>(loginCookies));
                }
            }
        } catch (Throwable e) {
            System.err.println("[NeteaseLoginManager] Failed to load cookies: " + e.getMessage());
        }
    }

    private void saveCookiesToDisk() {
        try {
            Path p = cookieSavePath();
            Files.createDirectories(p.getParent());
            JsonObject obj = new JsonObject();
            for (Map.Entry<String, String> entry : loginCookies.entrySet()) {
                obj.addProperty(entry.getKey(), entry.getValue());
            }
            Files.writeString(p, new GsonBuilder().setPrettyPrinting().create().toJson(obj),
                    StandardCharsets.UTF_8);
            System.out.println("[NeteaseLoginManager] Cookies saved to disk.");
        } catch (Throwable e) {
            System.err.println("[NeteaseLoginManager] Failed to save cookies: " + e.getMessage());
        }
    }

    private void deleteCookiesFromDisk() {
        try {
            Path p = cookieSavePath();
            Files.deleteIfExists(p);
        } catch (Throwable ignored) {}
    }

    public void reset() {
        stopPolling();
        loginState = LoginState.NONE;
        loginCookies.clear();
        currentUnikey = null;
        deleteCookiesFromDisk();
        if (onCookieChangeListener != null) {
            onCookieChangeListener.accept(null);
        }
    }

    public enum LoginState {
        NONE, WAITING_SCAN, SCANNED, SUCCESS, EXPIRED, FAILED
    }

    public interface LoginCallback {
        void onStateChange(LoginState newState, String message);
        void onSuccess(Map<String, String> cookies);
    }

    public CompletableFuture<String> fetchUnikey() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = BASE_URL + "/api/login/qrcode/unikey";
                String body = "type=1";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .header("Referer", "https://music.163.com/")
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("Cookie", sessionCookie)
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() == 200) {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (json.has("unikey")) {
                        currentUnikey = json.get("unikey").getAsString();
                        return currentUnikey;
                    } else if (json.has("data")) {
                        JsonObject data = json.getAsJsonObject("data");
                        if (data.has("unikey")) {
                            currentUnikey = data.get("unikey").getAsString();
                            return currentUnikey;
                        }
                    }
                }
                loginState = LoginState.FAILED;
                throw new IOException("Failed to get unikey: HTTP " + response.statusCode());
            } catch (Exception e) {
                loginState = LoginState.FAILED;
                throw new RuntimeException("Error in fetchUnikey: " + e.getMessage(), e);
            }
        });
    }

    public BufferedImage generateQrCode(String unikey) {
        try {
            String qrContent = "https://music.163.com/login?codekey=" + unikey;
            com.google.zxing.Writer writer = new com.google.zxing.qrcode.QRCodeWriter();
            com.google.zxing.common.BitMatrix matrix = writer.encode(
                    qrContent, com.google.zxing.BarcodeFormat.QR_CODE, 200, 200);
            BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < 200; x++) {
                for (int y = 0; y < 200; y++) {
                    image.setRGB(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            return image;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void startPolling(String unikey, LoginCallback callback) {
        if (polling.get()) return;
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "NeteaseLogin-Polling");
                t.setDaemon(true);
                return t;
            });
        }
        polling.set(true);
        scheduler.scheduleAtFixedRate(() -> {
            if (!polling.get()) return;
            try {
                long timestamp = System.currentTimeMillis();
                String url = BASE_URL + "/api/login/qrcode/client/login?t=" + timestamp;
                String body = "key=" + unikey + "&type=1";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .header("Referer", "https://music.163.com/")
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("Cookie", sessionCookie)
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() == 200) {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    int code = json.get("code").getAsInt();
                    switch (code) {
                        case 800 -> {
                            loginState = LoginState.EXPIRED;
                            polling.set(false);
                            callback.onStateChange(loginState, "二维码已过期，请点击刷新");
                        }
                        case 801 -> {
                            loginState = LoginState.WAITING_SCAN;
                            callback.onStateChange(loginState, "请使用网易云音乐 APP 扫码");
                        }
                        case 802 -> {
                            loginState = LoginState.SCANNED;
                            callback.onStateChange(loginState, "已扫码，请在手机上确认登录");
                        }
                        case 803 -> {
                            loginState = LoginState.SUCCESS;
                            polling.set(false);
                            List<String> headerCookies = response.headers().allValues("Set-Cookie");
                            for (String cookieStr : headerCookies) {
                                String[] parts = cookieStr.split(";")[0].split("=", 2);
                                if (parts.length == 2) loginCookies.put(parts[0].trim(), parts[1].trim());
                            }
                            if (json.has("cookie")) {
                                String[] cookies = json.get("cookie").getAsString().split("; ");
                                for (String cookie : cookies) {
                                    String[] parts = cookie.split("=", 2);
                                    if (parts.length == 2) loginCookies.put(parts[0].trim(), parts[1].trim());
                                }
                            }
                            saveCookiesToDisk();
                            if (onCookieChangeListener != null) {
                                onCookieChangeListener.accept(new HashMap<>(loginCookies));
                            }
                            callback.onSuccess(loginCookies);
                            callback.onStateChange(loginState, "登录成功");
                        }
                        case 8821 -> {
                            loginState = LoginState.FAILED;
                            polling.set(false);
                            callback.onStateChange(loginState, "网易云风控拦截，请刷新重试");
                        }
                        default -> System.out.println("[NeteaseLoginManager] Unknown code: " + code);
                    }
                }
            } catch (Exception e) {
                callback.onStateChange(LoginState.FAILED, "网络错误: " + e.getMessage());
                polling.set(false);
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    public void stopPolling() {
        polling.set(false);
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) scheduler.shutdownNow();
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
            scheduler = null;
        }
    }

    public LoginState getLoginState() { return loginState; }
    public Map<String, String> getLoginCookies() { return new HashMap<>(loginCookies); }
    public boolean isLoggedIn() { return loginState == LoginState.SUCCESS && !loginCookies.isEmpty(); }
}