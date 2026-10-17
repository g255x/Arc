package dev.kissoo.loader;

import hoprc.obf.neko.NekoInclude;

@NekoInclude
final class CanaryB {
    private CanaryB() {
    }

    static int mix(String s) {
        int x = 0x13579BDF;
        for (int i = s.length() - 1; i >= 0; i--) x = Integer.rotateRight(x + s.charAt(i), 3) ^ 0x45D9F3B;
        return x;
    }
}
