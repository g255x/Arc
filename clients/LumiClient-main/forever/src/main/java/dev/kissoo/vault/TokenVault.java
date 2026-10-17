package dev.kissoo.vault;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 客户端凭据落盘加密（AES-256-GCM）。
 *
 * <p>密钥由 HWID（native 侧派生）经 SHA-256 派生，避免 forever-token.json 明文泄露原始 token。
 * 本类独立于 dev.kissoo.auth 的 native 白名单（soft_exclusions 保留原类名），
 * 供 native 编译后的 KeyAuth 直调。</p>
 *
 * <p>边界说明：这只防「明文落盘」不防「代码执行」——已在机器上能跑代码的攻击者可自行取 HWID。
 * 更强的 DPAPI（绑定 Windows 用户，本机/其他用户无法解）因 native 混淆器对 JNA/JNI 支持未验证，
 * 暂缓；当前服务端 token 只存哈希 + HWID 绑定已能阻断跨机复用。</p>
 */
public final class TokenVault {
    private static final String PREFIX = "FENC1:";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private TokenVault() {}

    /** 加密明文，返回 "FENC1:&lt;iv&gt;.&lt;ct&gt;"（base64url 无填充）。 */
    public static String encrypt(String plaintext, String hwid) throws Exception {
        byte[] iv = new byte[IV_BYTES];
        RANDOM.nextBytes(iv);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(deriveKey(hwid), "AES"),
                new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] ct = c.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        return PREFIX
                + Base64.getUrlEncoder().withoutPadding().encodeToString(iv)
                + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(ct);
    }

    /** 解密 "FENC1:&lt;iv&gt;.&lt;ct&gt;"；失败（损坏/篡改/换机）抛异常，调用方降级为「无 token」。 */
    public static String decrypt(String envelope, String hwid) throws Exception {
        String rest = requirePrefix(envelope);
        int dot = rest.indexOf('.');
        if (dot <= 0) throw new IllegalArgumentException("bad envelope");
        byte[] iv = Base64.getUrlDecoder().decode(rest.substring(0, dot));
        byte[] ct = Base64.getUrlDecoder().decode(rest.substring(dot + 1));
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(deriveKey(hwid), "AES"),
                new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] pt = c.doFinal(ct);
        return new String(pt, StandardCharsets.UTF_8);
    }

    /** 是否为本库加密产物（用于兼容旧明文 JSON）。 */
    public static boolean isEncrypted(String s) {
        return s != null && s.startsWith(PREFIX);
    }

    private static String requirePrefix(String envelope) {
        if (envelope == null || !envelope.startsWith(PREFIX)) {
            throw new IllegalArgumentException("not encrypted");
        }
        return envelope.substring(PREFIX.length());
    }

    private static byte[] deriveKey(String hwid) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest(("forever-token-vault:" + (hwid == null ? "" : hwid))
                .getBytes(StandardCharsets.UTF_8));
    }
}
