package dev.kissoo.loader;

import hoprc.obf.neko.NekoInclude;

@NekoInclude
final class ObfDecoyGraph {
    private static final byte[] EDGES = {
            3, 7, 11, 2, 9, 14, 5, 1,
            12, 4, 6, 15, 8, 10, 13, 0,
            5, 2, 14, 7, 1, 11, 4, 9,
            0, 13, 3, 12, 15, 6, 10, 8
    };

    private ObfDecoyGraph() {
    }

    static int walk(String text, int seed) {
        int node = seed & 15;
        int acc = seed ^ 0x27182818;
        int len = text == null ? 0 : text.length();
        int rounds = 32 + (len & 31);
        for (int i = 0; i < rounds; i++) {
            int edgeA = EDGES[(node + i) & 31] & 15;
            int edgeB = EDGES[((node << 1) + i + 7) & 31] & 15;
            int glyph = len == 0 ? i * 33 : text.charAt((i * 5 + node) % len);
            int next = (edgeA ^ edgeB ^ glyph ^ acc) & 15;
            acc = step(acc, node, next, glyph, i);
            node = next;
        }
        return acc ^ (node * 0x01010101);
    }

    private static int step(int acc, int node, int next, int glyph, int round) {
        int x = acc ^ (node * 0x45d9f3b) ^ (next * 0x119de1f3) ^ glyph;
        x = Integer.rotateLeft(x, (round + node) & 31);
        x += Integer.rotateRight(acc, (next + 3) & 31);
        if (((x >>> (round & 15)) & 1) == 0) {
            x ^= 0xa5a5a5a5;
        } else {
            x += 0x3c6ef372;
        }
        return x;
    }
}
