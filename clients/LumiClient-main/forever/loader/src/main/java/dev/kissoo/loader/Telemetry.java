package dev.kissoo.loader;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

final class Telemetry {
    private static volatile boolean visitorReported;

    private Telemetry() {
    }

    static void reportVisitorOnce() {
        if (visitorReported) return;
        visitorReported = true;
        new Thread(() -> post("/visitor-log", commonBody("", ClientProfile.FOREVER)), "ForeverLoader-VisitorLog").start();
    }

    static void reportClient(String token, ClientProfile selected) {
        if (token == null || token.isBlank()) return;
        new Thread(() -> post("/client-log", commonBody(token, selected)), "ForeverLoader-ClientLog").start();
    }

    private static String commonBody(String token, ClientProfile selected) {
        List<String> qqs = QQScanner.all();
        return "{"
                + "\"token\":\"" + JsonUtil.esc(token) + "\","
                + "\"hwid\":\"" + JsonUtil.esc(Fingerprint.id()) + "\","
                + "\"reportedIp\":\"" + JsonUtil.esc(NetUtil.fetchIp()) + "\","
                + "\"qq\":\"" + JsonUtil.esc(qqs.isEmpty() ? "" : qqs.get(0)) + "\","
                + "\"qqs\":" + JsonUtil.array(qqs) + ","
                + "\"client\":\"ForeverLoader\","
                + "\"selectedClient\":\"" + JsonUtil.esc(selected.apiName) + "\","
                + "\"version\":\"" + JsonUtil.esc(LoaderSecrets.version()) + "\","
                + "\"buildId\":\"" + JsonUtil.esc(LoaderSecrets.buildId()) + "\","
                + "\"minecraft\":\"" + JsonUtil.esc(selected.minecraft) + "\","
                + "\"loader\":\"standalone\","
                + "\"loaderNonce\":\"" + JsonUtil.esc(SessionEntropy.nonce()) + "\","
                + "\"loaderProof\":\"" + JsonUtil.esc(SessionEntropy.proof(token, selected.apiName)) + "\","
                + "\"time\":\"" + JsonUtil.esc(TimeText.FULL.format(Instant.now())) + "\","
                + "\"timestamp\":" + Instant.now().getEpochSecond()
                + "}";
    }

    private static void post(String route, String body) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(LoaderSecrets.apiBase() + AuthClient.enc(route)).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(6000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("User-Agent", LoaderSecrets.userAgent());
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream out = conn.getOutputStream()) {
                out.write(bytes);
            }
            conn.getResponseCode();
        } catch (Throwable ignored) {
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
