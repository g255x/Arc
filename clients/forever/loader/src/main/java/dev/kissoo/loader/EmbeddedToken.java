package dev.kissoo.loader;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

final class EmbeddedToken {
    private EmbeddedToken() {
    }

    static String readToken() {
        try (InputStream in = LoaderSecrets.class.getResourceAsStream(LoaderSecrets.embeddedTokenResource())) {
            if (in == null) return "";
            byte[] payload = in.readAllBytes();
            if (payload.length < 12 + 16 + 2) return "";
            byte[] iv = Arrays.copyOfRange(payload, 0, 12);
            byte[] cipherAndTag = Arrays.copyOfRange(payload, 12, payload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(LoaderSecrets.embeddedTokenKey(), "AES"), new GCMParameterSpec(128, iv));
            String json = new String(cipher.doFinal(cipherAndTag), StandardCharsets.UTF_8);
            return JsonUtil.field(json, "token").trim();
        } catch (Throwable ignored) {
            return "";
        }
    }
}
