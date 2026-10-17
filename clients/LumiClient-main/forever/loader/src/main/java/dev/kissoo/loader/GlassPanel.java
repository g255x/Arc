package dev.kissoo.loader;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

final class GlassPanel extends RoundPanel {
    private BufferedImage blurCache;
    private int cachedWidth = -1;
    private int cachedHeight = -1;
    private int cachedRootWidth = -1;
    private int cachedRootHeight = -1;
    private int cachedX = Integer.MIN_VALUE;
    private int cachedY = Integer.MIN_VALUE;

    GlassPanel(int radius) {
        super(radius, Palette.CARD);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Shape clip = new RoundRectangle2D.Float(0, 0, Math.max(1, getWidth() - 1), Math.max(1, getHeight() - 1), radius, radius);
        Shape originalClip = g2.getClip();
        g2.clip(clip);
        paintBlurredBackground(g2);

        g2.setColor(Palette.light() ? new Color(255, 252, 244, 168) : new Color(18, 19, 16, 154));
        g2.fill(clip);
        float highlightHeight = Math.max(26f, Math.min(96f, getHeight() * 0.18f));
        g2.setPaint(new LinearGradientPaint(
                0f, 0f, 0f, highlightHeight,
                new float[]{0f, 0.35f, 1f},
                Palette.light()
                        ? new Color[]{
                        new Color(255, 255, 255, 104),
                        new Color(255, 255, 255, 32),
                        new Color(255, 255, 255, 0)
                }
                        : new Color[]{
                        new Color(255, 255, 255, 18),
                        new Color(255, 255, 255, 7),
                        new Color(255, 255, 255, 0)
                }
        ));
        g2.fillRect(1, 1, Math.max(1, getWidth() - 2), Math.round(highlightHeight));
        g2.setClip(originalClip);
        g2.setColor(Palette.light() ? new Color(56, 45, 29, 42) : new Color(255, 255, 255, 38));
        g2.draw(clip);
        g2.dispose();
    }

    private void paintBlurredBackground(Graphics2D g2) {
        java.awt.Component root = SwingUtilities.getAncestorOfClass(AuroraPanel.class, this);
        if (!(root instanceof AuroraPanel aurora) || root.getWidth() <= 0 || root.getHeight() <= 0 || getWidth() <= 0 || getHeight() <= 0) {
            g2.setColor(Palette.CARD);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            return;
        }
        Point p = SwingUtilities.convertPoint(this, 0, 0, root);
        boolean stale = blurCache == null
                || cachedWidth != getWidth() || cachedHeight != getHeight()
                || cachedRootWidth != root.getWidth() || cachedRootHeight != root.getHeight()
                || cachedX != p.x || cachedY != p.y;
        if (stale) rebuildBlurCache(aurora, root, p);
        if (blurCache == null) return;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(blurCache, 0, 0, getWidth(), getHeight(), null);
    }

    private void rebuildBlurCache(AuroraPanel aurora, java.awt.Component root, Point p) {
        float scale = 0.14f;
        int sw = Math.max(2, Math.round(getWidth() * scale));
        int sh = Math.max(2, Math.round(getHeight() * scale));
        BufferedImage small = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D bg = small.createGraphics();
        bg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        bg.scale(scale, scale);
        bg.translate(-p.x, -p.y);
        AuroraPanel.paintScene(bg, root.getWidth(), root.getHeight(), aurora.startedAt());
        bg.dispose();
        blurCache = small;
        cachedWidth = getWidth();
        cachedHeight = getHeight();
        cachedRootWidth = root.getWidth();
        cachedRootHeight = root.getHeight();
        cachedX = p.x;
        cachedY = p.y;
    }
}
