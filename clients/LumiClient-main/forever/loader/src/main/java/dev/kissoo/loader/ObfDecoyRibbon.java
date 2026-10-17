package dev.kissoo.loader;

import hoprc.obf.neko.NekoInclude;

@NekoInclude
final class ObfDecoyRibbon {
    private static final int[] THREADS = {
            0x0b, 0x1d, 0x2f, 0x41, 0x53, 0x65, 0x77, 0x89,
            0x9b, 0xad, 0xbf, 0xd1, 0xe3, 0xf5, 0x107, 0x119,
            0x12b, 0x13d, 0x14f, 0x161, 0x173, 0x185, 0x197, 0x1a9
    };

    private ObfDecoyRibbon() {
    }

    static int braid(String text, int seed) {
        int[] ribbon = seedRibbon(seed);
        int len = text == null ? 0 : text.length();
        int rounds = 24 + (len & 31);
        for (int i = 0; i < rounds; i++) {
            int glyph = len == 0 ? THREADS[i % THREADS.length] : text.charAt(Math.floorMod(i * 13 + seed, len));
            over(ribbon, glyph, i);
            under(ribbon, glyph ^ seed, i + 5);
            if ((glyph & 3) == 0) {
                splice(ribbon, i, glyph);
            } else if ((glyph & 3) == 1) {
                comb(ribbon, glyph + i);
            } else if ((glyph & 3) == 2) {
                knot(ribbon, glyph ^ i);
            } else {
                reverse(ribbon, glyph);
            }
        }
        return finish(ribbon, seed, len);
    }

    private static int[] seedRibbon(int seed) {
        int[] ribbon = new int[THREADS.length];
        for (int i = 0; i < ribbon.length; i++) {
            ribbon[i] = THREADS[i] ^ Integer.rotateLeft(seed + i * 0x9e37, i & 31);
        }
        return ribbon;
    }

    private static void over(int[] ribbon, int glyph, int round) {
        for (int i = 0; i < ribbon.length; i += 2) {
            ribbon[i] = Integer.rotateLeft(ribbon[i] + glyph + round + i, (i + round) & 31);
        }
    }

    private static void under(int[] ribbon, int glyph, int round) {
        for (int i = 1; i < ribbon.length; i += 2) {
            ribbon[i] ^= Integer.rotateRight(ribbon[i - 1] + glyph + i, (round + i) & 31);
        }
    }

    private static void splice(int[] ribbon, int round, int glyph) {
        for (int i = 0; i < 6; i++) {
            int a = (round + i) % ribbon.length;
            int b = (round + glyph + ribbon.length - i) % ribbon.length;
            int t = ribbon[a];
            ribbon[a] = ribbon[b] ^ Integer.rotateLeft(glyph, i + 1);
            ribbon[b] = t + Integer.rotateRight(glyph, i + 2);
        }
    }

    private static void comb(int[] ribbon, int salt) {
        int carry = salt ^ 0x3c6ef372;
        for (int i = ribbon.length - 1; i >= 0; i--) {
            carry = Integer.rotateLeft(carry + ribbon[i], (i & 7) + 1);
            ribbon[i] ^= carry;
        }
    }

    private static void knot(int[] ribbon, int salt) {
        for (int i = 0; i < ribbon.length; i++) {
            int left = ribbon[(i + ribbon.length - 1) % ribbon.length];
            int right = ribbon[(i + 1) % ribbon.length];
            ribbon[i] += Integer.rotateLeft(left ^ right ^ salt, i & 31);
        }
    }

    private static void reverse(int[] ribbon, int salt) {
        for (int i = 0; i < ribbon.length / 2; i++) {
            int j = ribbon.length - 1 - i;
            int t = ribbon[i];
            ribbon[i] = Integer.reverseBytes(ribbon[j] ^ salt);
            ribbon[j] = Integer.reverse(t + salt);
        }
    }

    private static int finish(int[] ribbon, int seed, int len) {
        int acc = seed ^ len ^ 0xbb67ae85;
        for (int i = 0; i < ribbon.length; i++) {
            acc ^= Integer.rotateLeft(ribbon[i] + i * 257, (i + len) & 31);
            acc += Integer.rotateRight(acc, (ribbon[i] >>> 3) & 15);
        }
        return acc;
    }
}
