package dev.kissoo.auth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.client.MinecraftClient;
import dev.kissoo.vault.TokenVault;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class KeyAuth {
    // 鉴权服务器地址（HTTPS，自签证书 + 客户端证书固定，见 PinnedSSL；与服务端 PUBLIC_ORIGIN 一致）。
    private static final String API_BASE = "https://52.175.50.86/api/index.php";
    private static final String CLIENT   = "Forever";

    // 服务器 Ed25519 公钥（base64，32 字节）。由服务端 private/cloud-payload-ed25519.php 的 seed 派生。
    // 留空 = 跳过验签（向后兼容，不安全）；填入后客户端才会校验 /verify 响应签名。
    private static final String AUTH_PUBLIC_KEY_B64 = "FEDiRGQX3QLldX6Ay3tznqiFU03ulC4YOAZt+5TS9Jk=";
    // 离线宽限期：服务器不可达时，已验签用户可继续使用的最长时间（7 天）。
    private static final long OFFLINE_GRACE_SECONDS = 7L * 24L * 3600L;

    private static volatile boolean cachedAllowed;
    private static volatile boolean loginRequired = true;
    private static volatile boolean hasResult;
    private static volatile String  storedOwner = "";
    private static volatile String  storedToken = "";
    private static volatile String  lastReason  = "";
    private static volatile String  payloadKey  = "";
    // 服务端签名下发的授权能力对象（替换单一 allowed 布尔）。仅存已验签对象。
    private static volatile Capability activeCapability = Capability.NONE;
    // 上次成功验证时间（epoch 秒），用于离线宽限判断。
    private static volatile long lastVerifiedAt = 0L;

    private static volatile Thread revalidateThread;
    private static final Gson GSON = new Gson();
    private static final SecureRandom RANDOM = new SecureRandom();

    static {
        // 安装 TLS 证书固定：信任本客户端自签服务器证书，其余 CA 证书仍走系统默认信任链。
        PinnedSSL.install();
    }

    private KeyAuth() {}

    public record AuthResult(boolean allowed, String reason) {
        public static AuthResult ok() { return new AuthResult(true, ""); }
    }

    // ── Public API ──────────────────────────────────────────────────────────

    public static boolean isCachedAllowed()  { return cachedAllowed; }
    public static Capability getCapability() { return activeCapability != null ? activeCapability : Capability.NONE; }
    public static boolean isLoginRequired()  { return loginRequired; }
    public static boolean hasToken()         { return !storedToken.isBlank(); }
    public static String  getStoredToken()   { return storedToken; }
    public static String  getOwner()         { return storedOwner; }
    public static boolean isAllowed()        { return cachedAllowed; }
    public static boolean hasResult()        { return hasResult; }
    public static String  lastReason()       { return lastReason; }
    public static long    guardValue()       { return getCapability().allowed() ? 1L : 0L; }
    public static boolean verifySessionSeal(){ return getCapability().allowed() && !getCapability().expired(System.currentTimeMillis() / 1000L); }

    /** 服务器下发的 payload 解密密钥（每 HWID 派生，不在 jar 内）。未鉴权前为空串。 */
    public static String  getPayloadKey()    { return payloadKey; }

    // ── HWID ────────────────────────────────────────────────────────────────

    private static volatile String hwid;

    public static String getHwid() {
        if (hwid == null) {
            hwid = generateHwid();
        }
        return hwid;
    }

    private static String generateHwid() {
        try {
            StringBuilder raw = new StringBuilder();

            appendEnv(raw, "COMPUTERNAME");
            appendEnv(raw, "PROCESSOR_IDENTIFIER");
            appendEnv(raw, "NUMBER_OF_PROCESSORS");
            appendEnv(raw, "PROCESSOR_LEVEL");
            appendEnv(raw, "PROCESSOR_REVISION");
            raw.append(System.getProperty("user.name", ""));
            raw.append(System.getProperty("os.arch", ""));

            try {
                java.io.File c = new java.io.File("C:\\");
                raw.append(c.getTotalSpace());
            } catch (Exception ignored) {}

            String seed = raw.toString();
            if (seed.length() < 16) {
                seed = System.getProperty("user.name", "u")
                        + Runtime.getRuntime().availableProcessors()
                        + Runtime.getRuntime().maxMemory()
                        + System.getProperty("os.version", "");
            }

            return sha256(seed);
        } catch (Exception e) {
            String fallback = System.getProperty("user.name", "fallback")
                    + System.getProperty("os.name", "");
            try {
                return sha256(fallback);
            } catch (Exception ex) {
                return "0000000000000000000000000000000000000000000000000000000000000000";
            }
        }
    }

    private static void appendEnv(StringBuilder sb, String name) {
        try {
            String v = System.getenv(name);
            if (v != null) sb.append(v);
        } catch (Exception ignored) {}
    }

    // ── SHA-256 ─────────────────────────────────────────────────────────────

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }

    // ── Token persistence (plain text) ──────────────────────────────────────

    private static Path tokenFile() {
        return MinecraftClient.getInstance().runDirectory.toPath()
                .resolve("forever-token.json");
    }

    private static void loadToken() {
        try {
            Path file = tokenFile();
            if (!Files.exists(file)) return;

            String raw = Files.readString(file).trim();
            if (TokenVault.isEncrypted(raw)) {
                try {
                    raw = TokenVault.decrypt(raw, getHwid());
                } catch (Exception e) {
                    return; // 解密失败（损坏/篡改/换机）→ 视为无 token，需重新登录
                }
            }
            String token = extractField(raw, "token");
            String owner = extractField(raw, "owner");
            if (!token.isBlank()) {
                storedToken = token;
                storedOwner = owner;
                loginRequired = false;
            }
            // 恢复离线宽限状态：跨重启保留 lastVerifiedAt/expiresAt/hwid，
            // 使已验签用户断网重启后仍在宽限期内可用（offlineGraceActive 依赖它）。
            long persistedVerifiedAt = parseEpoch(extractField(raw, "lastVerifiedAt"));
            if (persistedVerifiedAt > 0) {
                long persistedExpires = parseEpoch(extractField(raw, "expiresAt"));
                String persistedHwid = extractField(raw, "hwid");
                if (persistedHwid.isBlank()) persistedHwid = getHwid();
                activeCapability = new Capability(true, owner, CLIENT, "",
                        persistedHwid, persistedVerifiedAt, persistedExpires);
                lastVerifiedAt = persistedVerifiedAt;
            }
        } catch (Exception ignored) {}
    }

    public static void saveToken(String token, String owner) {
        storedToken = token;
        storedOwner = owner;
        loginRequired = false;
        persistSession();
    }

    /** 把 token + 离线宽限状态一并落盘（跨重启恢复用）。 */
    private static void persistSession() {
        try {
            Capability c = activeCapability != null ? activeCapability : Capability.NONE;
            String json = "{\"token\":\"" + esc(storedToken)
                    + "\",\"owner\":\"" + esc(storedOwner)
                    + "\",\"lastVerifiedAt\":" + lastVerifiedAt
                    + ",\"expiresAt\":" + (c.allowed() ? c.expiresAt() : 0L)
                    + ",\"hwid\":\"" + esc(c.deviceHash().isBlank() ? getHwid() : c.deviceHash()) + "\"}";
            String out = json;
            try {
                out = TokenVault.encrypt(json, getHwid());
            } catch (Exception ignored) {
                // 极端环境下加密失败仍写明文，避免直接丢 token
            }
            Files.writeString(tokenFile(), out);
        } catch (Exception ignored) {}
    }

    // ── Verification ───────────────────────────────────────────────────────

    public static AuthResult authenticate() {
        // 认证已注释，直接返回成功
        hasResult = true;
        cachedAllowed = true;
        loginRequired = false;
        activeCapability = new Capability(true, "user", CLIENT, AuthBuildInfo.clientVersion(),
                getHwid(), System.currentTimeMillis() / 1000L, 0L);
        lastVerifiedAt = System.currentTimeMillis() / 1000L;
        lastReason = "Bypassed.";
        return AuthResult.ok();
    }

    public static AuthResult verifyToken(String token) {
        String hw = getHwid();
        String version = AuthBuildInfo.clientVersion();
        String nonce = generateNonce();

        String body = "{"
                + "\"token\":\""   + esc(token)   + "\","
                + "\"hwid\":\""    + esc(hw)      + "\","
                + "\"client\":\""  + esc(CLIENT)  + "\","
                + "\"version\":\"" + esc(version) + "\","
                + "\"phase\":\"postLogin\","
                + "\"authNonce\":\"" + esc(nonce) + "\""
                + "}";

        try {
            String responseJson = httpPost(API_BASE + "?route=/verify", body);

            try {
                JsonObject json = GSON.fromJson(responseJson, JsonObject.class);
                boolean ok = getBool(json, "ok");

                hasResult = true;
                if (ok) {
                    // 校验服务器签名，防止本地/中间人伪造 "ok":true
                    if (!verifyAuthSignature(json, nonce, token, hw, version)) {
                        cachedAllowed = false;
                        activeCapability = Capability.NONE;
                        lastReason = "Signature verification failed.";
                        return new AuthResult(false, lastReason);
                    }

                    String pkey = getStr(json, "payloadKey");
                    if (!pkey.isBlank()) payloadKey = pkey;

                    String owner = getStr(json, "owner");
                    if (!owner.isBlank()) storedOwner = owner;

                    // 从已验签响应构建能力对象（字段已被 Ed25519 签名覆盖）。
                    activeCapability = new Capability(true, owner, CLIENT, version, hw,
                            parseEpoch(getStr(json, "authIssuedAt")),
                            parseEpoch(getStr(json, "expiresAt")));
                    lastVerifiedAt = System.currentTimeMillis() / 1000L;
                    if (!storedToken.isBlank()) persistSession(); // 持久化离线宽限状态

                    cachedAllowed = true;
                    loginRequired = false;
                    lastReason = "Verified.";
                    return AuthResult.ok();
                } else {
                    cachedAllowed = false;
                    activeCapability = Capability.NONE;
                    String reason = getStr(json, "reason");
                    if (reason.isBlank()) reason = getStr(json, "error");
                    if (reason.isBlank()) reason = "Verification failed.";
                    lastReason = reason;
                    return new AuthResult(false, lastReason);
                }
            } catch (JsonSyntaxException e) {
                // 服务器返回非 JSON（异常）。已配置公钥时视为服务器异常而非明确拒绝：
                // 离线宽限期内保持旧授权，否则拒绝。
                hasResult = true;
                if (!AUTH_PUBLIC_KEY_B64.isBlank()) {
                    if (activeCapability.allowed() && offlineGraceActive()) {
                        cachedAllowed = true;
                        loginRequired = false;
                        return AuthResult.ok();
                    }
                    cachedAllowed = false;
                    activeCapability = Capability.NONE;
                    lastReason = "Malformed server response.";
                    return new AuthResult(false, lastReason);
                }
                boolean ok = responseJson.contains("\"ok\":true");
                cachedAllowed = ok;
                if (!ok) {
                    String reason = extractField(responseJson, "reason");
                    if (reason.isBlank()) reason = extractField(responseJson, "error");
                    if (reason.isBlank()) reason = "Verification failed.";
                    lastReason = reason;
                    return new AuthResult(false, lastReason);
                }
                lastReason = "Verified.";
                loginRequired = false;
                return AuthResult.ok();
            }
        } catch (Exception e) {
            hasResult = true;
            // 网络错误：已验签且在离线宽限期内 → 保持授权；否则拒绝。
            if (activeCapability.allowed() && offlineGraceActive()) {
                cachedAllowed = true;
                loginRequired = false;
                return AuthResult.ok();
            }
            cachedAllowed = false;
            activeCapability = Capability.NONE;
            lastReason = "Network error: " + e.getClass().getSimpleName();
            return new AuthResult(false, lastReason);
        }
    }

    public static void confirmLogin() {
        authenticate();
    }

    // ── Periodic revalidation ──────────────────────────────────────────────

    public static void startPeriodicRevalidation(Object mc) {
        if (revalidateThread != null && revalidateThread.isAlive()) return;
        revalidateThread = new Thread(() -> {
            while (true) {
                try { Thread.sleep(5 * 60 * 1000); } catch (InterruptedException e) { break; }
                if (storedToken.isBlank()) continue;
                verifyToken(storedToken);
            }
        }, "Forever-Auth-Revalidate");
        revalidateThread.setDaemon(true);
        revalidateThread.start();
    }

    // ── Coordinate telemetry → server (DISABLED - 后门已禁用) ────────────────

    /** 坐标上报已禁用。原方法会将玩家坐标发送到远程服务器。 */
    public static void reportCoords(String server, String dim, int x, int y, int z) {
        // 坐标遥测后门已禁用
    }

    // ── Response signature (Ed25519) ────────────────────────────────────────

    private static boolean verifyAuthSignature(JsonObject json, String nonce, String token,
                                               String hw, String version) {
        if (AUTH_PUBLIC_KEY_B64.isBlank()) {
            return true; // 未配置公钥，跳过验签（向后兼容）
        }
        String sig       = getStr(json, "authSignature");
        String issuedAt  = getStr(json, "authIssuedAt");
        String allowed   = getBool(json, "ok") ? "1" : "0";
        String expiresAt = getStr(json, "expiresAt");
        String owner     = getStr(json, "owner");
        if (sig.isBlank()) return false;

        // 防重放：签名签发时间（authIssuedAt）须在本地时钟 ±10 分钟内。
        long issuedEpoch = parseEpoch(issuedAt);
        long now = System.currentTimeMillis() / 1000L;
        if (issuedEpoch > 0 && Math.abs(now - issuedEpoch) > 600L) {
            return false;
        }

        String message = canonicalAuthMessage(nonce, issuedAt, allowed, sha256(token),
                hw, CLIENT, version, expiresAt, owner);
        try {
            byte[] sigBytes = Base64.getDecoder().decode(sig);
            byte[] pubBytes = Base64.getDecoder().decode(AUTH_PUBLIC_KEY_B64);
            return Ed25519.verify(message.getBytes(StandardCharsets.UTF_8), sigBytes, pubBytes);
        } catch (Exception e) {
            return false;
        }
    }

    /** 复刻服务端 auth_canonical_message('ForeverAuthV1', [...]) 的字节级格式。 */
    private static String canonicalAuthMessage(String nonce, String issuedAt, String allowed,
            String tokenHash, String hwid, String client, String version, String expiresAt, String owner) {
        StringBuilder sb = new StringBuilder("ForeverAuthV1\n");
        appendField(sb, nonce);
        appendField(sb, issuedAt);
        appendField(sb, allowed);
        appendField(sb, tokenHash);
        appendField(sb, hwid);
        appendField(sb, client);
        appendField(sb, version);
        appendField(sb, expiresAt);
        appendField(sb, owner);
        return sb.toString();
    }

    /** 服务端用 strlen()（字节长度）做长度前缀，这里用 UTF-8 字节长度对齐。 */
    private static void appendField(StringBuilder sb, String value) {
        String v = value == null ? "" : value;
        sb.append(v.getBytes(StandardCharsets.UTF_8).length).append(':').append(v).append('\n');
    }

    private static String generateNonce() {
        byte[] b = new byte[32];
        RANDOM.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static long parseEpoch(String s) {
        if (s == null || s.isBlank()) return 0L;
        try { return Long.parseLong(s.trim()); } catch (NumberFormatException e) { return 0L; }
    }

    private static boolean offlineGraceActive() {
        long now = System.currentTimeMillis() / 1000L;
        return lastVerifiedAt > 0 && (now - lastVerifiedAt) <= OFFLINE_GRACE_SECONDS;
    }

    // ── HTTP helpers ───────────────────────────────────────────────────────

    private static String httpPost(String url, String body) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("User-Agent", AuthBuildInfo.userAgent());
        try (OutputStream out = conn.getOutputStream()) {
            out.write(body.getBytes(StandardCharsets.UTF_8));
        }
        InputStream in = conn.getResponseCode() < 400
                ? conn.getInputStream() : conn.getErrorStream();
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    // ── JSON helpers ───────────────────────────────────────────────────────

    private static String getStr(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull()
                ? obj.get(key).getAsString() : "";
    }

    private static boolean getBool(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull()
                && obj.get(key).getAsBoolean();
    }

    private static String extractField(String json, String key) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\"" + java.util.regex.Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)")
                .matcher(json);
        if (m.find()) return m.group(1);
        java.util.regex.Matcher mb = java.util.regex.Pattern
                .compile("\"" + java.util.regex.Pattern.quote(key) + "\"\\s*:\\s*(true|false)")
                .matcher(json);
        if (mb.find()) return mb.group(1);
        return "";
    }

    private static String esc(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}