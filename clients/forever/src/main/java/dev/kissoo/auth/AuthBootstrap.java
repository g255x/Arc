package dev.kissoo.auth;

import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AuthBootstrap {
    private static final Logger LOG = LoggerFactory.getLogger("Forever-Auth");
    private static volatile boolean verified;
    private static volatile String username = "";
    private static volatile String hwid = "";
    private static final AuthBootstrap INSTANCE = new AuthBootstrap();

    private AuthBootstrap() {}

    public static void preflight() {}

    public static void bootstrap() {
        // 认证已注释，直接通过
        LOG.info("Auth bypassed — skipping verification.");
        verified = true;
        username = "user";
        hwid = "0000000000000000000000000000000000000000000000000000000000000000";
    }

    /** Called by TokenScreen after successful verification */
    static void markVerified(String user, String hw) {
        verified = true;
        username = user;
        hwid = hw;
    }

    public static void requireVerified() {
        if (!verified) {
            bootstrap();
        }
    }

    public static boolean isVerified() {
        return verified;
    }

    public static String getUsername() {
        return username;
    }

    public static String getHwid() {
        return hwid;
    }

    public static AuthBootstrap getInstance() {
        return INSTANCE;
    }
}