package dev.kissoo.loader;

import hoprc.obf.neko.NekoInclude;

@NekoInclude
final class ObfDecoyState {
    private enum Mode {
        IDLE, WARM, TRACE, SPLIT, MERGE, SEAL, SLEEP
    }

    private ObfDecoyState() {
    }

    static int pulse(String text, int seed) {
        Mode mode = Mode.values()[Math.floorMod(seed, Mode.values().length)];
        int a = seed ^ 0x1234abcd;
        int b = ~seed;
        int len = text == null ? 0 : text.length();
        for (int i = 0; i < 40 + (len & 15); i++) {
            int ch = len == 0 ? i + 101 : text.charAt(Math.floorMod(i + a, len));
            switch (mode) {
                case IDLE -> {
                    a += ch ^ i;
                    mode = Mode.WARM;
                }
                case WARM -> {
                    b ^= Integer.rotateLeft(a, i & 31);
                    mode = (ch & 1) == 0 ? Mode.TRACE : Mode.SPLIT;
                }
                case TRACE -> {
                    a = Integer.rotateRight(a ^ ch ^ b, 5);
                    mode = Mode.MERGE;
                }
                case SPLIT -> {
                    b = Integer.reverseBytes(b + ch + i);
                    mode = Mode.SEAL;
                }
                case MERGE -> {
                    a ^= b + ch * 131;
                    mode = Mode.SEAL;
                }
                case SEAL -> {
                    b += Integer.rotateLeft(a ^ ch, 7);
                    mode = Mode.SLEEP;
                }
                case SLEEP -> {
                    a ^= Integer.rotateRight(b, 11);
                    mode = Mode.values()[(mode.ordinal() + ch + i) % Mode.values().length];
                }
            }
        }
        return a ^ Integer.rotateLeft(b, mode.ordinal() + 3);
    }
}
