package dev.kissoo.loader;

import java.awt.Color;

final class Palette {
    static Color BG = new Color(10, 11, 10);
    static Color CARD = new Color(26, 27, 23, 208);
    static Color CARD_DARK = new Color(12, 13, 12, 150);
    static Color TEXT = new Color(244, 242, 235);
    static Color MUTED = new Color(166, 161, 149);
    static Color DIM = new Color(126, 119, 105);
    static Color PEARL = new Color(241, 235, 224);
    static Color INK = new Color(24, 21, 18);
    static Color ACCENT = new Color(180, 159, 111);
    private static boolean light;

    private Palette() {
    }

    static void apply(LoaderPreferences.Theme theme) {
        light = theme == LoaderPreferences.Theme.LIGHT;
        if (light) {
            BG = new Color(236, 233, 225);
            CARD = new Color(255, 253, 247, 214);
            CARD_DARK = new Color(230, 226, 216, 190);
            TEXT = new Color(28, 27, 24);
            MUTED = new Color(94, 90, 81);
            DIM = new Color(121, 112, 94);
            PEARL = new Color(33, 32, 28);
            INK = new Color(246, 242, 232);
            ACCENT = new Color(156, 131, 82);
        } else {
            BG = new Color(10, 11, 10);
            CARD = new Color(26, 27, 23, 208);
            CARD_DARK = new Color(12, 13, 12, 150);
            TEXT = new Color(244, 242, 235);
            MUTED = new Color(166, 161, 149);
            DIM = new Color(126, 119, 105);
            PEARL = new Color(241, 235, 224);
            INK = new Color(24, 21, 18);
            ACCENT = new Color(180, 159, 111);
        }
    }

    static boolean light() {
        return light;
    }
}
