package dev.kissoo.auth;

/**
 * 服务端签名下发的授权能力对象，替代单一 allowed 布尔。
 *
 * 所有字段均来自 /verify 的 Ed25519 签名响应（签名覆盖 nonce/issuedAt/allowed/
 * sha256(token)/hwid/client/version/expiresAt/owner），因此本对象一旦构造即为
 * 已验签的不可信篡改数据。各关键模块分别验证不同字段，而不是共读一个布尔值，
 * 这样单独把某个 native 方法返回值改成 true 无法同时骗过所有独立校验点。
 */
public final class Capability {

    /** 未授权/空能力（在 <clinit> 中构造，作为默认值）。 */
    public static final Capability NONE = new Capability(false, "", "", "", "", 0L, 0L);

    private final boolean allowed;
    private final String owner;        // 用户
    private final String product;      // 产品（client）
    private final String version;      // 版本
    private final String deviceHash;   // 设备散列（hwid）
    private final long issuedAt;       // 签发时间（epoch 秒）
    private final long expiresAt;      // 有效期（epoch 秒；0 = 永久）

    public Capability(boolean allowed, String owner, String product, String version,
                      String deviceHash, long issuedAt, long expiresAt) {
        this.allowed = allowed;
        this.owner = owner == null ? "" : owner;
        this.product = product == null ? "" : product;
        this.version = version == null ? "" : version;
        this.deviceHash = deviceHash == null ? "" : deviceHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public boolean allowed()   { return allowed; }
    public String  owner()     { return owner; }
    public String  product()   { return product; }
    public String  version()   { return version; }
    public String  deviceHash(){ return deviceHash; }
    public long    issuedAt()  { return issuedAt; }
    public long    expiresAt() { return expiresAt; }

    /** 是否已过期（expiresAt <= 0 视为永久有效）。 */
    public boolean expired(long nowEpochSeconds) {
        return expiresAt > 0 && nowEpochSeconds > expiresAt;
    }

    /** 设备散列是否匹配本机（空白散列视为不绑定设备）。 */
    public boolean matchesDevice(String currentHwid) {
        return deviceHash.isBlank() || deviceHash.equals(currentHwid);
    }

    /** 完整有效：允许 + 未过期 + 设备匹配。 */
    public boolean isValid(long nowEpochSeconds, String currentHwid) {
        return allowed && !expired(nowEpochSeconds) && matchesDevice(currentHwid);
    }
}
