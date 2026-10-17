package dev.kissoo.loader;

import hoprc.obf.neko.NekoInclude;

import java.nio.charset.StandardCharsets;

@NekoInclude
final class CanaryC {
    private CanaryC() {
    }

    static int mix(String s) {
        int x = 0x2468ACE;
        for (byte b : s.getBytes(StandardCharsets.UTF_8)) x = (x * 33) ^ (b & 0xff);
        return x;
    }
}
