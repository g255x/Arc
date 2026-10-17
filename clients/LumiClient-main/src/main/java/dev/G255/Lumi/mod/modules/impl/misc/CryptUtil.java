package dev.G255.Lumi.mod.modules.impl.misc;

import java.nio.charset.StandardCharsets;

public final class CryptUtil {
    private CryptUtil() {}

    private static final char[] CB = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
    private static final int[] CB_MAP = new int[128];
    private static final int[] PALETTE = {
        0xce7452, 0xda70d6, 0x27c93f, 0xc19a6b, 0xba4a43,
        0x4a90d9, 0xf4a261, 0x6c5ce7, 0xe17055, 0x2ecc71,
        0x9b59b6, 0x3498db, 0xe74c3c, 0x1abc9c, 0xf1c40f,
        0xe67e22
    };

    static {
        for (int i = 0; i < 128; i++) CB_MAP[i] = -1;
        for (int i = 0; i < CB.length; i++) CB_MAP[CB[i]] = i;
    }

    public static String encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int len = data.length;
        for (int i = 0; i < len; i += 3) {
            int b0 = data[i] & 0xFF;
            int b1 = (i + 1 < len) ? data[i + 1] & 0xFF : 0;
            int b2 = (i + 2 < len) ? data[i + 2] & 0xFF : 0;
            int triple = (b0 << 16) | (b1 << 8) | b2;
            sb.append(CB[(triple >> 18) & 0x3F]);
            sb.append(CB[(triple >> 12) & 0x3F]);
            sb.append((i + 1 < len) ? CB[(triple >> 6) & 0x3F] : '=');
            sb.append((i + 2 < len) ? CB[triple & 0x3F] : '=');
        }
        return sb.toString();
    }

    public static byte[] decode(String s) {
        int len = s.length();
        if (len % 4 != 0) throw new IllegalArgumentException("Invalid base64");
        int pads = 0;
        if (len > 0 && s.charAt(len - 1) == '=') pads++;
        if (len > 1 && s.charAt(len - 2) == '=') pads++;
        byte[] out = new byte[len / 4 * 3 - pads];
        int op = 0;
        for (int i = 0; i < len; i += 4) {
            int c0 = CB_MAP[s.charAt(i)];
            int c1 = CB_MAP[s.charAt(i + 1)];
            int c2 = (s.charAt(i + 2) == '=') ? 0 : CB_MAP[s.charAt(i + 2)];
            int c3 = (s.charAt(i + 3) == '=') ? 0 : CB_MAP[s.charAt(i + 3)];
            int triple = (c0 << 18) | (c1 << 12) | (c2 << 6) | c3;
            out[op++] = (byte) ((triple >> 16) & 0xFF);
            if (s.charAt(i + 2) != '=') out[op++] = (byte) ((triple >> 8) & 0xFF);
            if (s.charAt(i + 3) != '=') out[op++] = (byte) (triple & 0xFF);
        }
        return out;
    }

    public static int nearestPaletteIndex(int r, int g, int b) { return nearestPaletteIndex((r << 16) | (g << 8) | b); }

    public static int nearestPaletteIndex(int rgb) {
        int best = 0, bestDist = Integer.MAX_VALUE;
        for (int i = 0; i < PALETTE.length; i++) {
            int dr = ((rgb >> 16) & 0xFF) - ((PALETTE[i] >> 16) & 0xFF);
            int dg = ((rgb >> 8) & 0xFF) - ((PALETTE[i] >> 8) & 0xFF);
            int db = (rgb & 0xFF) - (PALETTE[i] & 0xFF);
            int dist = dr * dr + dg * dg + db * db;
            if (dist < bestDist) { bestDist = dist; best = i; }
        }
        return best;
    }

    public static int paletteColor(int index) { return PALETTE[Math.abs(index) % PALETTE.length]; }

    static byte[] xorStream(byte[] data, String key, String iv) {
        byte[] out = new byte[data.length];
        byte[] kb = key.getBytes(StandardCharsets.UTF_8);
        byte[] ib = iv.getBytes(StandardCharsets.UTF_8);
        byte[] state = new byte[256];
        for (int i = 0; i < 256; i++) state[i] = (byte) i;
        int j = 0;
        for (int i = 0; i < 256; i++) {
            j = (j + (state[i] & 0xFF) + (kb[i % kb.length] & 0xFF) + (ib[i % ib.length] & 0xFF)) & 0xFF;
            byte tmp = state[i]; state[i] = state[j]; state[j] = tmp;
        }
        int a = 0, b = 0;
        for (int i = 0; i < data.length; i++) {
            a = (a + 1) & 0xFF;
            b = (b + (state[a] & 0xFF)) & 0xFF;
            byte tmp = state[a]; state[a] = state[b]; state[b] = tmp;
            out[i] = (byte) (data[i] ^ state[(state[a] + state[b]) & 0xFF]);
        }
        return out;
    }

    static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) sb.append(String.format("%02x", b & 0xFF));
        return sb.toString();
    }

    public static String encryptCodebook(String text, String key) {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        byte[] out = xorStream(data, key, "codebook-v1");
        return encode(out);
    }

    public static String decryptCodebook(String encoded, String key) {
        byte[] data = decode(encoded);
        byte[] out = xorStream(data, key, "codebook-v1");
        return new String(out, StandardCharsets.UTF_8);
    }

    public static String encryptLegacy(String text, String key) {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        byte[] out = xorStream(data, key, "legacy-v2");
        return encode(out);
    }

    public static String decryptLegacy(String encoded, String key) {
        byte[] data = decode(encoded);
        byte[] out = xorStream(data, key, "legacy-v2");
        return new String(out, StandardCharsets.UTF_8);
    }

    public static String decryptAny(String encoded, String key) {
        try { return decryptCodebook(encoded, key); } catch (Exception ignored) {}
        try { return decryptLegacy(encoded, key); } catch (Exception ignored) {}
        return encoded;
    }

    public static String encryptAuto(String text, String key) { return encryptCodebook(text, key); }

    public static String encrypt(String text, String key) { return encryptCodebook(text, key); }
    public static String decrypt(String encoded, String key) { return decryptCodebook(encoded, key); }

    static void putInt24(byte[] buf, int off, int v) { buf[off] = (byte)(v >> 16); buf[off+1] = (byte)(v >> 8); buf[off+2] = (byte)v; }
    static int getInt24(byte[] buf, int off) { return ((buf[off] & 0xFF) << 16) | ((buf[off+1] & 0xFF) << 8) | (buf[off+2] & 0xFF); }
    static void putInt32(byte[] buf, int off, int v) { buf[off] = (byte)(v >> 24); buf[off+1] = (byte)(v >> 16); buf[off+2] = (byte)(v >> 8); buf[off+3] = (byte)v; }
    static int getInt32(byte[] buf, int off) { return ((buf[off] & 0xFF) << 24) | ((buf[off+1] & 0xFF) << 16) | ((buf[off+2] & 0xFF) << 8) | (buf[off+3] & 0xFF); }
    static void putShort16(byte[] buf, int off, short v) { buf[off] = (byte)(v >> 8); buf[off+1] = (byte)v; }
    static short getShort16(byte[] buf, int off) { return (short)(((buf[off] & 0xFF) << 8) | (buf[off+1] & 0xFF)); }
}
