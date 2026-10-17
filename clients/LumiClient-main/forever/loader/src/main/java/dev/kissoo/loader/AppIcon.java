package dev.kissoo.loader;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

final class AppIcon {
    private AppIcon() {
    }

    static BufferedImage image(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int arc = Math.max(7, size / 4);
        g2.setColor(Palette.PEARL);
        g2.fillRoundRect(1, 1, size - 3, size - 3, arc, arc);
        g2.setColor(new Color(255, 255, 255, 160));
        g2.drawRoundRect(1, 1, size - 3, size - 3, arc, arc);

        Font font = new Font("Georgia", Font.BOLD, Math.round(size * 0.49f));
        GlyphVector glyph = font.createGlyphVector(g2.getFontRenderContext(), "F");
        Rectangle2D bounds = glyph.getVisualBounds();
        float x = (float) ((size - bounds.getWidth()) / 2d - bounds.getX() - size * 0.006d);
        float y = (float) ((size - bounds.getHeight()) / 2d - bounds.getY() - size * 0.004d);
        g2.setColor(Palette.INK);
        g2.drawGlyphVector(glyph, x, y);
        g2.dispose();
        return image;
    }
}
