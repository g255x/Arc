package dev.kissoo.loader;

import hoprc.obf.neko.NekoInclude;

@NekoInclude
final class ObfDecoyLedger {
    private enum Op {
        CREDIT, DEBIT, SPLIT, HOLD, MIRROR, CLOSE
    }

    private static final long[] BOOK = {
            0x9e3779b97f4a7c15L, 0x6a09e667f3bcc909L, 0xbb67ae8584caa73bL,
            0x3c6ef372fe94f82bL, 0xa54ff53a5f1d36f1L, 0x510e527fade682d1L,
            0x1f83d9abfb41bd6bL, 0x5be0cd19137e2179L, 0x27d4eb2f165667c5L
    };

    private ObfDecoyLedger() {
    }

    static int balance(String text, int seed) {
        long debit = seed ^ BOOK[0];
        long credit = (~seed & 0xffffffffL) ^ BOOK[1];
        long memo = BOOK[2] + length(text);
        Op op = Op.values()[Math.floorMod(seed, Op.values().length)];
        int rounds = 28 + (length(text) & 15);
        for (int i = 0; i < rounds; i++) {
            int glyph = glyph(text, i, seed);
            long entry = entry(glyph, i, memo, BOOK[i % BOOK.length]);
            switch (op) {
                case CREDIT -> {
                    credit += rotate(entry ^ debit, i + glyph);
                    memo ^= credit + BOOK[(i + 3) % BOOK.length];
                    op = Op.SPLIT;
                }
                case DEBIT -> {
                    debit -= rotate(entry + credit, i ^ glyph);
                    memo += debit ^ BOOK[(i + 5) % BOOK.length];
                    op = Op.HOLD;
                }
                case SPLIT -> {
                    long half = entry ^ (credit >>> 1) ^ (debit << 1);
                    credit ^= half + BOOK[(glyph + i) & 7];
                    debit += Long.reverseBytes(half);
                    op = (half & 1L) == 0L ? Op.MIRROR : Op.DEBIT;
                }
                case HOLD -> {
                    memo = Long.rotateLeft(memo + entry + glyph, (i & 31) + 1);
                    op = (memo & 3L) == 0L ? Op.CLOSE : Op.CREDIT;
                }
                case MIRROR -> {
                    long m = Long.reverse(entry ^ memo);
                    debit ^= m;
                    credit += Long.rotateRight(m, (glyph & 31) + 1);
                    op = Op.CLOSE;
                }
                case CLOSE -> {
                    long close = (debit ^ credit ^ memo) + entry;
                    debit += close & 0xffffL;
                    credit ^= close >>> 17;
                    memo -= Long.rotateLeft(close, (i & 15) + 3);
                    op = Op.values()[Math.floorMod((int) close + i + glyph, Op.values().length)];
                }
            }
        }
        long out = debit ^ Long.rotateLeft(credit, 13) ^ Long.rotateRight(memo, 29);
        out ^= out >>> 33;
        out *= 0xff51afd7ed558ccdL;
        out ^= out >>> 33;
        return (int) (out ^ (out >>> 32));
    }

    private static int length(String text) {
        return text == null ? 0 : text.length();
    }

    private static int glyph(String text, int index, int seed) {
        if (text == null || text.isEmpty()) return index * 41 + seed;
        return text.charAt(Math.floorMod(index * 3 + seed, text.length()));
    }

    private static long entry(int glyph, int round, long memo, long salt) {
        long x = salt ^ memo ^ ((long) glyph << 32) ^ round;
        x = Long.rotateLeft(x + 0x94d049bb133111ebL, (round & 31) + 1);
        x ^= Long.rotateRight(x, (glyph & 31) + 1);
        return x;
    }

    private static long rotate(long value, int amount) {
        return Long.rotateLeft(value, Math.floorMod(amount, 63) + 1);
    }
}
