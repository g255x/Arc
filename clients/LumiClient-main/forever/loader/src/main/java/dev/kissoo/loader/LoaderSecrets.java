package dev.kissoo.loader;

import hoprc.obf.neko.NekoInclude;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@NekoInclude
final class LoaderSecrets {
    private static final int[] HOST = {57, 37, 37, 33, 107, 126, 126, 63, 52, 58, 62, 127, 48, 63, 37, 56, 50, 57, 52, 34, 37, 127, 33, 38, 126, 48, 33, 56, 126, 56, 63, 53, 52, 41, 127, 33, 57, 33, 110, 35, 62, 36, 37, 52, 108};
    private static final int[] RES = {126, 48, 34, 34, 52, 37, 34, 126, 55, 62, 35, 52, 39, 52, 35, 124, 61, 62, 48, 53, 52, 35, 126, 36, 34, 52, 35, 127, 53, 48, 37};
    private static final int[] KEY = {130, 144, 230, 244, 202, 216, 46, 60, 18, 96, 118, 68, 90, 168, 190, 188, 2, 16, 102, 116, 74, 88, 174, 188, 146, 58, 231, 245, 203, 217, 47, 61};
    private static final byte MASK = 0x51;

    private LoaderSecrets() {
    }

    static String version() {
        return new String(new byte[]{49, 46, 48, 46, 48}, StandardCharsets.UTF_8);
    }

    static String buildId() {
        return join("forever", "-", "loader", "-", version());
    }

    static String apiBase() {
        return decode(HOST);
    }

    static String endpointLabel() {
        try {
            String host = URI.create(apiBase()).getHost();
            return host == null || host.isBlank() ? "configured endpoint" : host;
        } catch (Throwable ignored) {
            return "configured endpoint";
        }
    }

    static String embeddedTokenResource() {
        return decode(RES);
    }

    static byte[] embeddedTokenKey() {
        byte[] out = new byte[KEY.length];
        for (int i = 0; i < KEY.length; i++) {
            out[i] = (byte) ((KEY[i] ^ 0x23) - i);
        }
        return out;
    }

    static String userAgent() {
        return join("Forever", "Loader", "/", version());
    }

    private static String decode(int[] data) {
        char[] out = new char[data.length];
        for (int i = 0; i < data.length; i++) {
            out[i] = (char) (data[i] ^ MASK);
        }
        return new String(out);
    }

    private static String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) sb.append(p);
        return sb.toString();
    }
}
