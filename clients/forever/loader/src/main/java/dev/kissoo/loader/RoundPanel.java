package dev.kissoo.loader;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

class RoundPanel extends JPanel {
    final int radius;
    final Color fill;

    RoundPanel(int radius, Color fill) {
        this.radius = radius;
        this.fill = fill;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        float width = Math.max(1f, getWidth() - 1.5f);
        float height = Math.max(1f, getHeight() - 1.5f);
        Shape shape = new RoundRectangle2D.Float(0.75f, 0.75f, width, height, radius, radius);
        g2.setColor(fill);
        g2.fill(shape);
        g2.setColor(Palette.light() ? new Color(44, 36, 24, 34) : new Color(255, 255, 255, 28));
        g2.setStroke(new BasicStroke(1f));
        g2.draw(shape);
        g2.dispose();
        super.paintComponent(g);
    }
}
