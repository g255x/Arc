package dev.kissoo.loader;

import hoprc.obf.neko.NekoInclude;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

@NekoInclude
final class SessionEntropy {
    private static final SecureRandom RNG = new SecureRandom();
    private static volatile byte[] secret = new byte[32];
    private static volatile String nonce = "";

    private SessionEntropy() {
    }

    static void rotate() {
        byte[] s = new byte[32];
        RNG.nextBytes(s);
        secret = s;
        nonce = CryptoUtil.hex(s).substring(0, 32) + Long.toHexString(System.nanoTime());
    }

    static String nonce() {
        return nonce;
    }

    static String proof(String token, String client) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(secret);
            update(digest, Fingerprint.id());
            update(digest, client);
            update(digest, token == null || token.length() < 8 ? token : token.substring(0, 4) + token.substring(token.length() - 4));
            update(digest, String.valueOf(System.currentTimeMillis() / 30000L));
            digest.update((byte) NoiseMesh.touch(client + nonce));
            return CryptoUtil.hex(digest.digest());
        } catch (Throwable ignored) {
            return nonce;
        }
    }

    private static void update(MessageDigest digest, String value) {
        if (value != null) digest.update(value.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0x2d);
    }
}
