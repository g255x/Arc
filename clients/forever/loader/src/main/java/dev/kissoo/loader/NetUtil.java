package dev.kissoo.loader;

import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

final class NetUtil {
    private NetUtil() {
    }

    static String fetchIp() {
        for (String s : List.of("https://api.ipify.org", "https://icanhazip.com")) {
            try {
                HttpURLConnection c = (HttpURLConnection) URI.create(s).toURL().openConnection();
                c.setConnectTimeout(2500);
                c.setReadTimeout(2500);
                if (c.getResponseCode() == 200) {
                    return new String(c.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                }
            } catch (Throwable ignored) {
            }
        }
        return "unknown";
    }
}
