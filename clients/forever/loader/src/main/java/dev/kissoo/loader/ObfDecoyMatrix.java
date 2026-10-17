package dev.kissoo.loader;

import hoprc.obf.neko.NekoInclude;

@NekoInclude
final class ObfDecoyMatrix {
    private static final int[] BASE = {
            0x13, 0x57, 0x9b, 0xdf,
            0x24, 0x68, 0xac, 0xe0,
            0x35, 0x79, 0xbd, 0xf1,
            0x46, 0x8a, 0xce, 0x02
    };

    private ObfDecoyMatrix() {
    }

    static int matrix(String text, int seed) {
        int[] box = new int[16];
        for (int i = 0; i < box.length; i++) {
            box[i] = BASE[i] ^ seed ^ Integer.rotateLeft(i * 0x45d9f3b, i);
        }
        int len = text == null ? 0 : text.length();
        int rounds = 10 + (len & 7);
        for (int r = 0; r < rounds; r++) {
            int glyph = len == 0 ? (r * 29 + 17) : text.charAt(Math.floorMod(r * 7 + box[r & 15], len));
            row(box, r & 3, glyph, r);
            column(box, (r >>> 1) & 3, seed ^ glyph, r);
            diagonal(box, r, glyph + seed);
            if ((glyph & 1) == 0) {
                twist(box, r + glyph);
            } else {
                fold(box, glyph ^ seed);
            }
        }
        return reduce(box, seed, len);
    }

    private static void row(int[] box, int row, int glyph, int round) {
        int offset = row << 2;
        int carry = glyph ^ round;
        for (int i = 0; i < 4; i++) {
            int at = offset + i;
            box[at] = Integer.rotateLeft(box[at] + carry + i * 31, (round + i) & 31);
            carry ^= box[at];
        }
    }

    private static void column(int[] box, int col, int salt, int round) {
        int carry = salt + 0x6a09e667;
        for (int i = 0; i < 4; i++) {
            int at = (i << 2) + col;
            box[at] ^= Integer.rotateRight(carry + box[at], (round + i + 3) & 31);
            carry += box[at] ^ (i * 0x9e37);
        }
    }

    private static void diagonal(int[] box, int round, int salt) {
        int a = (round & 3) * 5;
        int b = ((round + 1) & 3) * 3;
        int c = ((round + 2) & 3) * 5;
        int d = ((round + 3) & 3) * 3;
        int mix = box[a & 15] ^ box[b & 15] ^ box[c & 15] ^ box[d & 15] ^ salt;
        box[a & 15] += Integer.rotateLeft(mix, 3);
        box[b & 15] ^= Integer.rotateRight(mix, 7);
        box[c & 15] -= Integer.reverseBytes(mix);
        box[d & 15] += Integer.reverse(mix);
    }

    private static void twist(int[] box, int salt) {
        for (int i = 0; i < 8; i++) {
            int a = (i + salt) & 15;
            int b = (15 - i + (salt >>> 4)) & 15;
            int t = box[a];
            box[a] = Integer.rotateLeft(box[b] ^ salt, i + 1);
            box[b] = Integer.rotateRight(t + salt, i + 2);
        }
    }

    private static void fold(int[] box, int salt) {
        for (int i = 0; i < box.length; i++) {
            int n = box[(i + 5) & 15] ^ box[(i + 11) & 15] ^ salt;
            box[i] = Integer.rotateLeft(box[i] + n, i & 31);
        }
    }

    private static int reduce(int[] box, int seed, int len) {
        int acc = seed ^ len ^ 0x510e527f;
        for (int i = 0; i < box.length; i++) {
            acc ^= Integer.rotateLeft(box[i] + i * 0x10001, i + 3);
            acc += Integer.rotateRight(acc, (box[i] + i) & 15);
        }
        return acc;
    }
}
