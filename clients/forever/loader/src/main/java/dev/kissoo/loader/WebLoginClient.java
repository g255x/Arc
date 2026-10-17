package dev.kissoo.loader;

import java.net.HttpURLConnection;
import java.net.URI;

final class WebLoginClient {
    private WebLoginClient() {
    }

    static StartResult start(ClientProfile client) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(LoaderSecrets.apiBase() + AuthClient.enc("/web-login/start")).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(7000);
            conn.setReadTimeout(9000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("User-Agent", LoaderSecrets.userAgent());
            String body = "{"
                    + "\"hwid\":\"" + JsonUtil.esc(Fingerprint.id()) + "\","
                    + "\"username\":\"" + JsonUtil.esc(System.getProperty("user.name", "")) + "\","
                    + "\"client\":\"" + JsonUtil.esc(client.apiName) + "\","
                    + "\"version\":\"" + JsonUtil.esc(LoaderSecrets.version()) + "\""
                    + "}";
            AuthClient.write(conn, body);
            int code = conn.getResponseCode();
            String resp = AuthClient.read(conn, code);
            if (code >= 200 && code < 300 && resp.contains("\"ok\":true")) {
                return new StartResult(true, JsonUtil.field(resp, "code"), JsonUtil.field(resp, "url"),
                        AuthClient.parseLong(JsonUtil.field(resp, "expiresIn")), "Web login started.");
            }
            String error = JsonUtil.field(resp, "error");
            return new StartResult(false, "", "", 0, error.isBlank() ? "Web login failed." : error);
        } catch (Throwable t) {
            return new StartResult(false, "", "", 0, "Network error: " + t.getClass().getSimpleName());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    static PollResult poll(String code, ClientProfile client) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(LoaderSecrets.apiBase() + AuthClient.enc("/web-login/poll")).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(7000);
            conn.setReadTimeout(9000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("User-Agent", LoaderSecrets.userAgent());
            String body = "{"
                    + "\"code\":\"" + JsonUtil.esc(code) + "\","
                    + "\"hwid\":\"" + JsonUtil.esc(Fingerprint.id()) + "\","
                    + "\"client\":\"" + JsonUtil.esc(client.apiName) + "\","
                    + "\"version\":\"" + JsonUtil.esc(LoaderSecrets.version()) + "\""
                    + "}";
            AuthClient.write(conn, body);
            int status = conn.getResponseCode();
            String resp = AuthClient.read(conn, status);
            boolean ok = status >= 200 && status < 300 && resp.contains("\"ok\":true");
            boolean allowed = ok && resp.contains("\"allowed\":true");
            boolean pending = ok && resp.contains("\"pending\":true");
            if (allowed) {
                return new PollResult(true, false, new AuthSession(
                        JsonUtil.field(resp, "token"),
                        JsonUtil.field(resp, "owner"),
                        AuthClient.parseLong(JsonUtil.field(resp, "expiresAt")),
                        false
                ), "Web login confirmed.");
            }
            String reason = JsonUtil.field(resp, "reason");
            if (reason.isBlank()) reason = JsonUtil.field(resp, "error");
            if (reason.isBlank()) reason = pending ? "Waiting for website confirmation." : "Web login failed.";
            return new PollResult(false, pending, null, reason);
        } catch (Throwable t) {
            return new PollResult(false, true, null, "Waiting for website confirmation.");
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    record StartResult(boolean ok, String code, String url, long expiresIn, String message) {
    }

    record PollResult(boolean allowed, boolean pending, AuthSession session, String message) {
    }
}
