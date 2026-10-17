package dev.kissoo.loader;

import hoprc.obf.neko.NekoInclude;

@NekoInclude
final class ObfDecoyOracle {
    private static final String[] SIGNS = {
            "cedar", "ivory", "opal", "thunder", "moss", "basalt", "ember", "drift",
            "copper", "linen", "frost", "hollow", "violet", "quiet", "marble", "sun"
    };

    private ObfDecoyOracle() {
    }

    static int ask(String text, int seed) {
        int acc = seed ^ 0x7f4a7c15;
        int left = Integer.rotateLeft(seed, 5) ^ 0x6d2b79f5;
        int right = Integer.rotateRight(seed, 7) ^ 0x165667b1;
        int len = text == null ? 0 : text.length();
        for (int i = 0; i < 36 + (len & 15); i++) {
            int glyph = len == 0 ? (i + 1) * 73 : text.charAt(Math.floorMod(left + i * 11, len));
            String sign = SIGNS[(glyph + i + acc) & 15];
            int branch = branch(sign, glyph, acc, i);
            if ((branch & 1) == 0) {
                left += prophecy(sign, glyph, right, i);
            } else {
                right ^= prophecy(sign, glyph ^ left, acc, i + 3);
            }
            if ((branch & 2) == 0) {
                acc = Integer.rotateLeft(acc ^ left ^ branch, (sign.length() + i) & 31);
            } else {
                acc = Integer.rotateRight(acc + right + branch, (glyph + i) & 31);
            }
            if ((branch & 4) != 0) {
                int swap = left;
                left = right ^ acc;
                right = swap + branch;
            }
        }
        return fold(acc, left, right, seed);
    }

    private static int branch(String sign, int glyph, int acc, int round) {
        int x = sign.length() ^ glyph ^ acc ^ round;
        for (int i = 0; i < sign.length(); i++) {
            x += Integer.rotateLeft(sign.charAt(i) * (i + 7), (round + i) & 15);
            x ^= Integer.rotateRight(x, (glyph + i) & 15);
        }
        return x;
    }

    private static int prophecy(String sign, int glyph, int salt, int round) {
        int x = salt + glyph + round * 0x9e37;
        for (int i = 0; i < sign.length(); i++) {
            char c = sign.charAt((i + round) % sign.length());
            x = Integer.rotateLeft(x ^ c ^ (i * 0x45d9), (i + 3) & 31);
            if ((c & 1) == 0) {
                x += Integer.reverseBytes(glyph + c);
            } else {
                x ^= Integer.reverse(salt + c + i);
            }
        }
        return x;
    }

    private static int fold(int a, int b, int c, int seed) {
        int x = a ^ Integer.rotateLeft(b, 9) ^ Integer.rotateRight(c, 13) ^ seed;
        x ^= x >>> 16;
        x *= 0x85ebca6b;
        x ^= x >>> 13;
        x *= 0xc2b2ae35;
        x ^= x >>> 16;
        return x;
    }
}
