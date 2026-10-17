package dev.kissoo.loader;

import hoprc.obf.neko.NekoInclude;

@NekoInclude
final class ObfDecoyClockwork {
    private static final int[] GEARS = {
            17, 23, 31, 43, 47, 59, 61, 71, 83, 97, 109, 127
    };

    private ObfDecoyClockwork() {
    }

    static int tick(String text, int seed) {
        Wheel primary = new Wheel(seed ^ 0x243f6a88, seed & 31);
        Wheel secondary = new Wheel(~seed ^ 0x85a308d3, (seed >>> 5) & 31);
        Wheel tertiary = new Wheel(seed + 0x13198a2e, (seed >>> 11) & 31);
        int len = text == null ? 0 : text.length();
        int rounds = 30 + (len & 15);
        for (int i = 0; i < rounds; i++) {
            int glyph = len == 0 ? GEARS[i % GEARS.length] + i : text.charAt(Math.floorMod(i * 9 + primary.phase, len));
            primary.turn(glyph + GEARS[i % GEARS.length], i);
            secondary.turn(primary.value ^ glyph, i + 3);
            tertiary.turn(secondary.value + primary.phase + glyph, i + 7);
            if ((glyph & 1) == 0) {
                mesh(primary, secondary, tertiary, glyph, i);
            } else {
                mesh(tertiary, primary, secondary, glyph ^ seed, i + 5);
            }
        }
        int out = primary.value ^ Integer.rotateLeft(secondary.value, 9) ^ Integer.rotateRight(tertiary.value, 13);
        out += (primary.phase << 20) ^ (secondary.phase << 10) ^ tertiary.phase;
        return finalize(out, seed, len);
    }

    private static void mesh(Wheel a, Wheel b, Wheel c, int glyph, int round) {
        int carry = a.value ^ b.value ^ c.value ^ glyph ^ round;
        a.value += Integer.rotateLeft(carry + a.phase, (round & 7) + 1);
        b.value ^= Integer.rotateRight(carry + b.phase, (glyph & 7) + 2);
        c.value -= Integer.reverseBytes(carry + c.phase);
        a.phase = (a.phase + glyph + GEARS[round % GEARS.length]) & 31;
        b.phase = (b.phase + a.phase + round) & 31;
        c.phase = (c.phase + b.phase + glyph) & 31;
    }

    private static int finalize(int value, int seed, int len) {
        int x = value ^ seed ^ len ^ 0x1f83d9ab;
        for (int i = 0; i < GEARS.length; i++) {
            x = Integer.rotateLeft(x + GEARS[i] * 0x10101, (i + len) & 31);
            x ^= Integer.rotateRight(seed + i * 0x7f4a, i + 1);
        }
        return x;
    }

    private static final class Wheel {
        private int value;
        private int phase;

        private Wheel(int value, int phase) {
            this.value = value;
            this.phase = phase;
        }

        private void turn(int input, int round) {
            value = Integer.rotateLeft(value ^ input ^ (phase * 0x45d9f3b), (phase + round) & 31);
            value += Integer.rotateRight(input + round, (phase & 7) + 1);
            phase = (phase + input + round + 1) & 31;
        }
    }
}
