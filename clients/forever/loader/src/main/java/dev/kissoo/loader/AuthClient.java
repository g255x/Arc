package dev.kissoo.loader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

final class AuthClient {
    private AuthClient() {
    }

    static AuthResult verify(String token, ClientProfile client) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(LoaderSecrets.apiBase() + enc("/verify")).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(7000);
            conn.setReadTimeout(9000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("User-Agent", LoaderSecrets.userAgent());
            String body = "{"
                    + "\"token\":\"" + JsonUtil.esc(token) + "\","
                    + "\"hwid\":\"" + JsonUtil.esc(Fingerprint.id()) + "\","
                    + "\"reportedIp\":\"" + JsonUtil.esc(NetUtil.fetchIp()) + "\","
                    + "\"client\":\"" + JsonUtil.esc(client.apiName) + "\","
                    + "\"version\":\"" + JsonUtil.esc(LoaderSecrets.version()) + "\","
                    + "\"loaderNonce\":\"" + JsonUtil.esc(SessionEntropy.nonce()) + "\","
                    + "\"loaderProof\":\"" + JsonUtil.esc(SessionEntropy.proof(token, client.apiName)) + "\","
                    + "\"guardMesh\":\"" + JsonUtil.esc(GuardMesh.marker()) + "\""
                    + "}";
            write(conn, body);
            int code = conn.getResponseCode();
            String resp = read(conn, code);
            boolean ok = code >= 200 && code < 300 && resp.contains("\"allowed\":true");
            String reason = JsonUtil.field(resp, "reason");
            if (reason.isBlank()) reason = ok ? "Token verified." : JsonUtil.field(resp, "error");
            if (reason.isBlank()) reason = "HTTP " + code;
            return new AuthResult(ok, reason, JsonUtil.field(resp, "owner"), parseLong(JsonUtil.field(resp, "expiresAt")));
        } catch (Throwable e) {
            return new AuthResult(false, "Network error: " + e.getClass().getSimpleName(), "", 0);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    static void write(HttpURLConnection conn, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        conn.setFixedLengthStreamingMode(bytes.length);
        try (OutputStream out = conn.getOutputStream()) {
            out.write(bytes);
        }
    }

    static String read(HttpURLConnection conn, int status) {
        try {
            InputStream in = status >= 400 && conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Throwable ignored) {
            return "";
        }
    }

    static long parseLong(String text) {
        try {
            return text == null || text.isBlank() ? 0 : Long.parseLong(text.trim());
        } catch (Throwable ignored) {
            return 0;
        }
    }

    static String enc(String route) {
        return URLEncoder.encode(route, StandardCharsets.UTF_8);
    }

    record AuthResult(boolean allowed, String reason, String owner, long expiresAt) {
    }
}
