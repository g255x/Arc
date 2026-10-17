package dev.kissoo.loader;

import hoprc.obf.neko.NekoInclude;

@NekoInclude
final class CanaryA {
    private CanaryA() {
    }

    static int mix(String s) {
        int x = 0x61C88647;
        for (int i = 0; i < s.length(); i++) x = Integer.rotateLeft(x ^ s.charAt(i), 5) + 0x9E37;
        return x;
    }
}
