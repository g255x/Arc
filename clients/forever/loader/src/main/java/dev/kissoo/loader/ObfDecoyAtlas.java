package dev.kissoo.loader;

import hoprc.obf.neko.NekoInclude;

@NekoInclude
final class ObfDecoyAtlas {
    private static final int[][] GRID = {
            {7, 31, 19, 43, 5, 97, 11, 73},
            {29, 3, 61, 17, 89, 13, 53, 41},
            {37, 71, 23, 83, 47, 2, 67, 109},
            {101, 59, 79, 107, 127, 131, 139, 149},
            {151, 157, 163, 167, 173, 179, 181, 191},
            {193, 197, 199, 211, 223, 227, 229, 233},
            {239, 241, 251, 257, 263, 269, 271, 277},
            {281, 283, 293, 307, 311, 313, 317, 331}
    };

    private ObfDecoyAtlas() {
    }

    static int trace(String text, int seed) {
        int row = seed & 7;
        int col = (seed >>> 3) & 7;
        int acc = seed ^ 0x31415926;
        int steps = Math.max(24, (text == null ? 0 : text.length()) + 17);
        for (int i = 0; i < steps; i++) {
            int ch = charAt(text, i);
            int cell = GRID[row][col];
            int turn = (ch + cell + i + acc) & 7;
            acc = blend(acc, cell, ch, i);
            switch (turn & 3) {
                case 0 -> row = (row + 1 + (ch & 1)) & 7;
                case 1 -> col = (col + 3 + (cell & 1)) & 7;
                case 2 -> row = (row + 7 - (ch & 3)) & 7;
                default -> col = (col + 5 - (cell & 3)) & 7;
            }
            if ((turn & 4) != 0) {
                int swap = row;
                row = col;
                col = swap;
            }
        }
        return acc ^ (row << 17) ^ (col << 9) ^ GRID[col][row];
    }

    private static int charAt(String text, int index) {
        if (text == null || text.isEmpty()) return 0x55 + index;
        return text.charAt(index % text.length());
    }

    private static int blend(int acc, int cell, int ch, int i) {
        int x = acc + cell * 31 + ch * 17 + i;
        x = Integer.rotateLeft(x, (cell + i) & 15);
        x ^= Integer.rotateRight(acc, (ch + i) & 15);
        return x + 0x6c8e9cf5;
    }
}
