package dev.kissoo.loader;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

final class ClientAuthSessionWriter {
    private static final int VERSION = 1;
    private static final SecureRandom RNG = new SecureRandom();
    private static final byte[] PEPPER = {
            0x41, 0x75, 0x74, 0x68, 0x54, 0x6F, 0x6B, 0x65,
            0x6E, 0x53, 0x74, 0x6F, 0x72, 0x65, 0x56, 0x31
    };

    private ClientAuthSessionWriter() {
    }

    static void write(Path gameDir, String token, long expiryEpoch) {
        if (gameDir == null || token == null || token.isBlank()) return;
        try {
            Path tokenFile = gameDir.toAbsolutePath().normalize()
                    .resolve("Forever")
                    .resolve(".auth-session");
            byte[] salt = randomBytes(16);
            byte[] iv = randomBytes(12);
            byte[] key = deriveKey(salt, gameDir);
            byte[] data = (token.trim() + "|" + Math.max(0L, expiryEpoch)).getBytes(StandardCharsets.UTF_8);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(xorLayer(data, key, iv));

            ByteBuffer buf = ByteBuffer.allocate(1 + salt.length + iv.length + encrypted.length);
            buf.put((byte) VERSION);
            buf.put(salt);
            buf.put(iv);
            buf.put(encrypted);
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(buf.array());

            Files.createDirectories(tokenFile.getParent());
            Files.writeString(tokenFile, encoded, StandardCharsets.UTF_8);
        } catch (Throwable ignored) {
        }
    }

    private static byte[] deriveKey(byte[] salt, Path gameDir) throws Exception {
        byte[] machine = storageKey(gameDir);
        byte[] mixed = new byte[PEPPER.length + machine.length];
        System.arraycopy(PEPPER, 0, mixed, 0, PEPPER.length);
        System.arraycopy(machine, 0, mixed, PEPPER.length, machine.length);

        PBEKeySpec spec = new PBEKeySpec(hexEncode(mixed).toCharArray(), salt, 20_000, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    private static byte[] storageKey(Path gameDir) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        update(digest, "Forever-Auth-Storage-v2");
        update(digest, System.getProperty("user.name"));
        update(digest, System.getProperty("os.name"));
        update(digest, System.getProperty("os.arch"));
        update(digest, System.getenv("COMPUTERNAME"));
        update(digest, System.getenv("PROCESSOR_IDENTIFIER"));
        update(digest, System.getenv("PROCESSOR_ARCHITECTURE"));
        update(digest, gameDir.toAbsolutePath().normalize().toString());
        return digest.digest();
    }

    private static byte[] xorLayer(byte[] data, byte[] key, byte[] salt) {
        byte[] out = Arrays.copyOf(data, data.length);
        for (int i = 0; i < out.length; i++) {
            out[i] ^= key[i % key.length] ^ salt[i % salt.length];
        }
        return out;
    }

    private static byte[] randomBytes(int len) {
        byte[] b = new byte[len];
        RNG.nextBytes(b);
        return b;
    }

    private static void update(MessageDigest digest, String value) {
        if (value != null) digest.update(value.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
    }

    private static String hexEncode(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) sb.append(String.format("%02x", b & 0xFF));
        return sb.toString();
    }
}
