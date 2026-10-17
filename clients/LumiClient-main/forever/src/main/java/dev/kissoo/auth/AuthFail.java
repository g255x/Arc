package dev.kissoo.auth;

import hoprc.obf.neko.NekoInclude;
import hoprc.obf.zkm.ZKMIndy;

@ZKMIndy
public final class AuthFail {
    private AuthFail() {
    }

    @ZKMIndy
    @NekoInclude
    public static void hardStop(String reason) {
        // Public build: legacy guard calls are intentionally non-blocking.
    }
}
