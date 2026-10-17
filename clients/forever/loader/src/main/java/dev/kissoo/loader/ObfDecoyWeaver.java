package dev.kissoo.loader;

import hoprc.obf.neko.NekoInclude;

import java.nio.charset.StandardCharsets;

@NekoInclude
final class ObfDecoyWeaver {
    private static final int[] RINGS = {
            0x6d2b79f5, 0x1b873593, 0x85ebca6b, 0xc2b2ae35,
            0x27d4eb2f, 0x165667b1, 0xd3a2646c, 0xfd7046c5,
            0xb55a4f09, 0x9e3779b9, 0x7f4a7c15, 0x94d049bb,
            0x2545f491, 0x369dea0f, 0x632be59b, 0x85157af5
    };

    private ObfDecoyWeaver() {
    }

    static int weave(String text, int seed) {
        byte[] data = text == null ? new byte[0] : text.getBytes(StandardCharsets.UTF_8);
        int x = seed ^ 0x4f1bbcdc;
        int y = data.length * 0x45d9f3b;
        for (int i = 0; i < data.length + RINGS.length; i++) {
            int b = i < data.length ? data[i] & 0xff : RINGS[i - data.length] >>> (i & 15);
            int r = RINGS[i & 15];
            x = Integer.rotateLeft(x + b + r, (i & 7) + 3) ^ y;
            y = Integer.rotateRight(y ^ (b * 257) ^ r, (i & 5) + 5);
            if ((i & 3) == 0) {
                x ^= foldLane(y, r, i);
            } else if ((i & 3) == 1) {
                y += foldLane(x, b + r, i + 11);
            } else if ((i & 3) == 2) {
                x -= Integer.reverseBytes(y ^ r);
            } else {
                y ^= Integer.reverse(x + b);
            }
        }
        return finish(x, y, seed);
    }

    private static int foldLane(int a, int b, int round) {
        int z = a ^ b ^ (round * 0x9e37);
        z ^= z << 13;
        z ^= z >>> 17;
        z ^= z << 5;
        return Integer.rotateLeft(z, round & 31);
    }

    private static int finish(int x, int y, int seed) {
        int z = x ^ Integer.rotateLeft(y, 11) ^ seed;
        z ^= z >>> 16;
        z *= 0x7feb352d;
        z ^= z >>> 15;
        z *= 0x846ca68b;
        z ^= z >>> 16;
        return z;
    }
}
