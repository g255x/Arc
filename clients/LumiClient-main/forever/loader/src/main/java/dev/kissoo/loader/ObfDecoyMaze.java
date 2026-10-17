package dev.kissoo.loader;

import hoprc.obf.neko.NekoInclude;

@NekoInclude
final class ObfDecoyMaze {
    private static final int WIDTH = 9;
    private static final int HEIGHT = 9;
    private static final byte[] TILES = {
            1, 0, 2, 3, 1, 4, 0, 2, 5,
            2, 5, 1, 0, 3, 1, 4, 0, 2,
            3, 1, 4, 2, 0, 5, 1, 3, 0,
            0, 2, 5, 1, 4, 0, 3, 1, 2,
            4, 3, 0, 5, 1, 2, 0, 4, 1,
            1, 0, 3, 4, 2, 1, 5, 0, 3,
            5, 2, 1, 0, 3, 4, 1, 2, 0,
            0, 4, 2, 1, 5, 3, 0, 1, 4,
            3, 1, 0, 2, 4, 0, 5, 3, 1
    };

    private ObfDecoyMaze() {
    }

    static int route(String text, int seed) {
        Cursor cursor = new Cursor(Math.floorMod(seed, WIDTH), Math.floorMod(seed >>> 4, HEIGHT), seed);
        int len = text == null ? 0 : text.length();
        int steps = 45 + (len & 31);
        for (int i = 0; i < steps; i++) {
            int glyph = len == 0 ? (i * 17 + 0x5d) : text.charAt(Math.floorMod(i + cursor.x * 3 + cursor.y * 5, len));
            int tile = tile(cursor.x, cursor.y);
            int turn = (glyph ^ tile ^ cursor.energy ^ i) & 7;
            move(cursor, turn, glyph, tile, i);
            cursor.energy = pulse(cursor.energy, cursor.x, cursor.y, glyph, tile, i);
            if ((i & 7) == 3) {
                cursor.energy ^= lookAround(cursor, glyph);
            }
        }
        return cursor.energy ^ (cursor.x << 24) ^ (cursor.y << 16);
    }

    private static void move(Cursor cursor, int turn, int glyph, int tile, int round) {
        int dx = 0;
        int dy = 0;
        switch (turn & 3) {
            case 0 -> dx = 1 + (tile & 1);
            case 1 -> dy = 1 + (glyph & 1);
            case 2 -> dx = -1 - (tile & 1);
            default -> dy = -1 - (glyph & 1);
        }
        if ((turn & 4) != 0) {
            int swap = dx;
            dx = dy;
            dy = swap;
        }
        cursor.x = Math.floorMod(cursor.x + dx + (round & 1), WIDTH);
        cursor.y = Math.floorMod(cursor.y + dy - ((round >>> 1) & 1), HEIGHT);
    }

    private static int pulse(int energy, int x, int y, int glyph, int tile, int round) {
        int v = energy ^ (x * 0x45d9f3b) ^ (y * 0x119de1f3) ^ (glyph * 257) ^ tile;
        v = Integer.rotateLeft(v, (round + tile + 5) & 31);
        v += Integer.rotateRight(energy, (glyph + round) & 15);
        return v;
    }

    private static int lookAround(Cursor cursor, int glyph) {
        int acc = glyph ^ 0x243f6a88;
        for (int yy = -1; yy <= 1; yy++) {
            for (int xx = -1; xx <= 1; xx++) {
                int t = tile(Math.floorMod(cursor.x + xx, WIDTH), Math.floorMod(cursor.y + yy, HEIGHT));
                acc ^= Integer.rotateLeft(t * 0x10101 + xx * 17 + yy * 31, (t + glyph) & 15);
            }
        }
        return acc;
    }

    private static int tile(int x, int y) {
        return TILES[y * WIDTH + x] & 0xff;
    }

    private static final class Cursor {
        private int x;
        private int y;
        private int energy;

        private Cursor(int x, int y, int energy) {
            this.x = x;
            this.y = y;
            this.energy = energy;
        }
    }
}
