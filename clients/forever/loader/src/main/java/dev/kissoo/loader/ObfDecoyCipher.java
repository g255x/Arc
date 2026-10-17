package dev.kissoo.loader;

import hoprc.obf.neko.NekoInclude;

import java.nio.charset.StandardCharsets;

@NekoInclude
final class ObfDecoyCipher {
    private ObfDecoyCipher() {
    }

    static int curl(String text, int seed) {
        byte[] data = text == null ? new byte[0] : text.getBytes(StandardCharsets.UTF_8);
        int left = seed ^ 0xabcdef01;
        int right = ~seed ^ 0x10203040;
        int rounds = 18 + (data.length & 15);
        for (int r = 0; r < rounds; r++) {
            int k = key(data, r, seed);
            left ^= round(right, k, r);
            right ^= round(left, Integer.reverse(k), r + 7);
            int swap = left;
            left = right;
            right = swap;
        }
        return left ^ Integer.rotateLeft(right, data.length & 31);
    }

    private static int key(byte[] data, int round, int seed) {
        int k = seed + round * 0x9e3779b9;
        for (int i = 0; i < Math.max(4, data.length); i++) {
            int b = data.length == 0 ? i * 19 : data[(i + round) % data.length] & 0xff;
            k = Integer.rotateLeft(k ^ b ^ (i * 0x7f4a), (i + round) & 15);
        }
        return k;
    }

    private static int round(int value, int key, int round) {
        int x = value + key;
        x ^= Integer.rotateLeft(x, (round & 7) + 3);
        x += Integer.rotateRight(key, (round & 11) + 5);
        x ^= 0x5a5a5a5a + round;
        return x;
    }
}
