package dev.kissoo.loader;

import hoprc.obf.neko.NekoInclude;

import java.security.MessageDigest;

@NekoInclude
final class CryptoUtil {
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private CryptoUtil() {
    }

    static byte[] hex(String s) {
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }

    static String hex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xff;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 15];
        }
        return new String(out);
    }

    static boolean same(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(a.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                b.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
