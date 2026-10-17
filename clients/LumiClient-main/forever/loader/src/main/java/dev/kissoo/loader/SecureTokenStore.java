package dev.kissoo.loader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Base64;

final class SecureTokenStore {
    private SecureTokenStore() {
    }

    static void save(String token, long expiry) {
        try {
            Files.createDirectories(LoaderPaths.configDir());
            String plain = token + "|" + expiry;
            byte[] key = Fingerprint.storageKey();
            byte[] data = plain.getBytes(StandardCharsets.UTF_8);
            for (int i = 0; i < data.length; i++) data[i] ^= key[i % key.length];
            Files.writeString(LoaderPaths.tokenFile(), Base64.getEncoder().encodeToString(data), StandardCharsets.UTF_8);
        } catch (Throwable ignored) {
        }
    }

    static String loadToken() {
        String[] parts = loadParts();
        return parts.length > 0 ? parts[0] : "";
    }

    static void delete() {
        try {
            Files.deleteIfExists(LoaderPaths.tokenFile());
        } catch (Throwable ignored) {
        }
    }

    private static String[] loadParts() {
        try {
            if (!Files.isRegularFile(LoaderPaths.tokenFile())) return new String[0];
            byte[] data = Base64.getDecoder().decode(Files.readString(LoaderPaths.tokenFile(), StandardCharsets.UTF_8).trim());
            byte[] key = Fingerprint.storageKey();
            for (int i = 0; i < data.length; i++) data[i] ^= key[i % key.length];
            String plain = new String(data, StandardCharsets.UTF_8);
            String[] parts = plain.split("\\|", 2);
            if (parts.length > 1) {
                long exp = AuthClient.parseLong(parts[1]);
                if (exp > 0 && Instant.now().getEpochSecond() >= exp) {
                    delete();
                    return new String[0];
                }
            }
            return parts;
        } catch (Throwable ignored) {
            return new String[0];
        }
    }
}
