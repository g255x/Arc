package dev.kissoo.loader;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

final class Fingerprint {
    private Fingerprint() {
    }

    static String id() {
        try {
            return CryptoUtil.hex(machineFingerprint());
        } catch (Throwable ignored) {
            return "unknown";
        }
    }

    static byte[] storageKey() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        update(digest, "Forever-Loader-Storage-v1");
        update(digest, id());
        update(digest, System.getProperty("user.home"));
        return digest.digest();
    }

    private static byte[] machineFingerprint() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        update(digest, System.getProperty("user.name"));
        update(digest, System.getProperty("os.name"));
        update(digest, System.getProperty("os.arch"));
        update(digest, System.getenv("COMPUTERNAME"));
        update(digest, System.getenv("PROCESSOR_IDENTIFIER"));
        update(digest, System.getenv("PROCESSOR_ARCHITECTURE"));
        return digest.digest();
    }

    private static void update(MessageDigest digest, String value) {
        if (value != null) digest.update(value.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
    }
}
