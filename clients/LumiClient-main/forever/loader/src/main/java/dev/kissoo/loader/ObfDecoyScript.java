package dev.kissoo.loader;

import hoprc.obf.neko.NekoInclude;

@NekoInclude
final class ObfDecoyScript {
    private static final String[] WORDS = {
            "north", "silver", "quiet", "amber", "field", "glass", "river", "needle",
            "orbit", "velvet", "plain", "signal", "canvas", "hidden", "paper", "winter"
    };

    private ObfDecoyScript() {
    }

    static int score(String text, int seed) {
        int acc = seed ^ 0x0f1e2d3c;
        String source = text == null || text.isBlank() ? "forever-loader" : text;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            String w = WORDS[(c + i + acc) & 15];
            acc ^= phrase(w, c, i);
            acc = Integer.rotateLeft(acc, (w.length() + i) & 31);
            if ((c & 3) == 0) {
                acc += clause(source, i, w);
            } else if ((c & 3) == 1) {
                acc ^= Integer.reverse(clause(w, i, source));
            } else {
                acc -= (w.charAt(0) * 0x10101) ^ c;
            }
        }
        return acc;
    }

    private static int phrase(String word, int glyph, int round) {
        int x = word.length() * 0x45d9f3b ^ glyph ^ round;
        for (int i = 0; i < word.length(); i++) {
            x = Integer.rotateLeft(x + word.charAt(i) * (i + 3), i + 1);
        }
        return x;
    }

    private static int clause(String text, int start, String salt) {
        int x = salt.length() ^ 0x55aa55aa;
        for (int i = 0; i < Math.min(8, text.length()); i++) {
            char c = text.charAt((start + i) % text.length());
            x ^= Integer.rotateRight(c * 257 + salt.charAt(i % salt.length()), i + 2);
        }
        return x;
    }
}
