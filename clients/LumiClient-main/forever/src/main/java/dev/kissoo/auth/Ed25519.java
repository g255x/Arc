package dev.kissoo.auth;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.EdECPoint;
import java.security.spec.EdECPublicKeySpec;
import java.security.spec.NamedParameterSpec;

/**
 * 极简 Ed25519 验签（RFC 8032 / libsodium crypto_sign_detached 兼容）。
 *
 * 服务端用 PHP sodium 的 sodium_crypto_sign_detached() 产出 64 字节裸签名
 * （base64 编码后放进 authSignature 字段），客户端只需用公钥验签，无需私钥。
 * Java 的 Signature.getInstance("Ed25519") 用的也是 RFC 8032 裸 64 字节格式，
 * 两边可直接互通；唯一要做的就是把服务端给我们的 32 字节压缩公钥还原成
 * java.security.spec.EdECPoint（即点解压缩）。
 */
public final class Ed25519 {
    private Ed25519() {}

    /** 校验消息的 Ed25519 签名。message/signature/publicKeyBytes 均为原始字节。 */
    public static boolean verify(byte[] message, byte[] signature, byte[] publicKeyBytes) {
        try {
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(decodePublicKey(publicKeyBytes));
            verifier.update(message);
            return verifier.verify(signature);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** 把 32 字节压缩公钥（小端 y + 最高位 x 符号）还原为 JCA 的 PublicKey。 */
    private static PublicKey decodePublicKey(byte[] raw) throws Exception {
        if (raw.length != 32) {
            throw new IllegalArgumentException("Ed25519 public key must be 32 bytes");
        }

        BigInteger p = BigInteger.ONE.shiftLeft(255).subtract(BigInteger.valueOf(19));
        BigInteger d = BigInteger.valueOf(-121665)
                .multiply(BigInteger.valueOf(121666).modInverse(p)).mod(p);

        byte[] tmp = raw.clone();
        boolean xOdd = (tmp[31] & 0x80) != 0;
        tmp[31] &= 0x7F;
        BigInteger y = new BigInteger(1, reverse(tmp));

        // x^2 = (y^2 - 1) / (d*y^2 + 1)  (mod p)
        BigInteger y2 = y.multiply(y).mod(p);
        BigInteger num = y2.subtract(BigInteger.ONE).mod(p);
        BigInteger den = d.multiply(y2).add(BigInteger.ONE).mod(p);
        BigInteger x = sqrt(num.multiply(den.modInverse(p)).mod(p), p);
        if (x == null) {
            throw new IllegalArgumentException("invalid Ed25519 public key");
        }
        if (x.testBit(0) != xOdd) {
            x = p.subtract(x);
        }

        EdECPoint point = new EdECPoint(xOdd, y);
        return KeyFactory.getInstance("Ed25519")
                .generatePublic(new EdECPublicKeySpec(NamedParameterSpec.ED25519, point));
    }

    /** p ≡ 5 (mod 8) 时的模平方根；无解返回 null。 */
    private static BigInteger sqrt(BigInteger a, BigInteger p) {
        BigInteger exp = p.add(BigInteger.valueOf(3)).shiftRight(3); // (p + 3) / 8
        BigInteger v = a.modPow(exp, p);
        if (v.multiply(v).mod(p).equals(a)) {
            return v;
        }
        BigInteger sqrtMinusOne = BigInteger.valueOf(2)
                .modPow(p.subtract(BigInteger.ONE).shiftRight(2), p); // 2^((p-1)/4)
        v = v.multiply(sqrtMinusOne).mod(p);
        return v.multiply(v).mod(p).equals(a) ? v : null;
    }

    private static byte[] reverse(byte[] b) {
        byte[] r = new byte[b.length];
        for (int i = 0; i < b.length; i++) {
            r[i] = b[b.length - 1 - i];
        }
        return r;
    }
}
