package dev.kissoo.auth;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * TLS 证书固定（certificate pinning）。
 *
 * 鉴权服务器用自签证书（裸 IP 拿不到 CA 证书），不能走默认 CA 信任链。这里把
 * 「服务器自签证书的 SPKI（SubjectPublicKeyInfo）SHA-256」固定下来：命中 pin 的证书
 * 直接信任，其余 HTTPS（如网易云音乐 API 等 CA 证书）仍回落到系统默认信任链。
 *
 * 固定值由 E:/LyraeHack/tls/cert.pem 生成：
 *   openssl x509 -in cert.pem -pubkey -noout | openssl pkey -pubin -outform der \
 *     | openssl dgst -sha256 -binary | openssl enc -base64
 */
public final class PinnedSSL {

    /** 服务器自签证书 SPKI 的 SHA-256（base64）。是公钥指纹，不是秘密。 */
    private static final String PIN_SHA256_BASE64 = "PYiY/CcLoMG3CtroqRvVh+oYoa81Qy7+f2VKwsjeaXc=";

    private static volatile boolean installed = false;

    private PinnedSSL() {}

    /** 安装全局默认 SSL 工厂（幂等）：信任链 = 系统默认 CA + 我们的自签证书 pin。 */
    public static synchronized void install() {
        if (installed) return;
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);
            final X509TrustManager defaultTm = (X509TrustManager) tmf.getTrustManagers()[0];

            X509TrustManager pinningTm = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    defaultTm.checkClientTrusted(chain, authType);
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    if (chain != null && chain.length > 0 && matchesPin(chain[0])) {
                        return; // 命中固定指纹，信任自签证书
                    }
                    defaultTm.checkServerTrusted(chain, authType);
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return defaultTm.getAcceptedIssuers();
                }
            };

            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[]{pinningTm}, null);
            HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
            installed = true;
        } catch (Exception e) {
            // 安装失败则回落系统默认信任（自签服务器会握手失败，但不至于让客户端崩）。
            installed = false;
        }
    }

    private static boolean matchesPin(X509Certificate cert) {
        try {
            byte[] spki = cert.getPublicKey().getEncoded(); // X.509 SPKI（DER）
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(spki);
            byte[] pin = Base64.getDecoder().decode(PIN_SHA256_BASE64);
            return MessageDigest.isEqual(digest, pin);
        } catch (Exception e) {
            return false;
        }
    }
}
