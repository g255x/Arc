package dev.kissoo.loader;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

final class BrandOrb extends JComponent {
    BrandOrb() {
        Dimension size = new Dimension(44, 44);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int width = Math.max(1, getWidth());
        int height = Math.max(1, getHeight());
        float side = Math.max(1f, Math.min(width, height));
        float originX = (width - side) / 2f;
        float originY = (height - side) / 2f;
        float arc = Math.max(8f, side * 0.20f);

        g2.setColor(new Color(0, 0, 0, 58));
        g2.fill(new RoundRectangle2D.Float(originX + 2.5f, originY + 3f, side - 5f, side - 5f, arc, arc));
        g2.setColor(Palette.PEARL);
        g2.fill(new RoundRectangle2D.Float(originX + 0.5f, originY + 0.5f, side - 1.5f, side - 1.5f, arc, arc));
        g2.setColor(new Color(255, 255, 255, 132));
        g2.draw(new RoundRectangle2D.Float(originX + 1f, originY + 1f, side - 2.5f, side - 2.5f, arc - 1f, arc - 1f));

        Font font = new Font("Georgia", Font.BOLD, Math.max(16, Math.round(side * 0.55f)));
        GlyphVector glyph = font.createGlyphVector(g2.getFontRenderContext(), "F");
        Rectangle2D bounds = glyph.getVisualBounds();
        float x = (float) (originX + (side - bounds.getWidth()) / 2d - bounds.getX() - 0.25d);
        float y = (float) (originY + (side - bounds.getHeight()) / 2d - bounds.getY() - 0.25d);
        g2.setColor(Palette.INK);
        g2.drawGlyphVector(glyph, x, y);
        g2.dispose();
    }
}
