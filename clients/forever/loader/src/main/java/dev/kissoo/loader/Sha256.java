package dev.kissoo.loader;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

final class Sha256 {
    private Sha256() {
    }

    static String file(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buf = new byte[16384];
            int read;
            while ((read = in.read(buf)) > 0) digest.update(buf, 0, read);
        }
        return CryptoUtil.hex(digest.digest());
    }
}
