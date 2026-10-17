package dev.kissoo.loader;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

final class AuroraPanel extends JPanel {
    private final long started = System.currentTimeMillis();
    private volatile boolean interactionFrozen;
    private BufferedImage sceneCache;
    private int sceneWidth = -1;
    private int sceneHeight = -1;

    AuroraPanel() {
        setOpaque(true);
        setBackground(Palette.BG);
        javax.swing.Timer timer = new javax.swing.Timer(120, e -> {
            if (isShowing() && !interactionFrozen) repaint();
        });
        timer.setCoalesce(true);
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        ensureSceneCache();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (sceneCache != null) g2.drawImage(sceneCache, 0, 0, null);
        if (!interactionFrozen) {
            paintAmbientMotion(g2, getWidth(), getHeight(), started);
        }
        g2.dispose();
    }

    void setInteractionFrozen(boolean frozen) {
        if (interactionFrozen == frozen) return;
        interactionFrozen = frozen;
        if (!frozen) repaint();
    }

    boolean isInteractionFrozen() {
        return interactionFrozen;
    }

    static void setInteractionFrozen(Component component, boolean frozen) {
        Component root = SwingUtilities.getAncestorOfClass(AuroraPanel.class, component);
        if (root instanceof AuroraPanel aurora) {
            aurora.setInteractionFrozen(frozen);
        }
    }

    private void ensureSceneCache() {
        if (getWidth() <= 0 || getHeight() <= 0) return;
        if (sceneCache != null && sceneWidth == getWidth() && sceneHeight == getHeight()) return;
        sceneWidth = getWidth();
        sceneHeight = getHeight();
        sceneCache = new BufferedImage(sceneWidth, sceneHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D cached = sceneCache.createGraphics();
        cached.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        paintScene(cached, sceneWidth, sceneHeight, started);
        cached.dispose();
    }

    private static void paintAmbientMotion(Graphics2D g2, int width, int height, long started) {
        if (width <= 0 || height <= 0) return;
        float t = (System.currentTimeMillis() - started) / 4600f;
        float breathe = 0.5f + 0.5f * (float) Math.sin(t);
        paintGlow(g2, Math.round(width * .82f), Math.round(height * .16f), new Color(238, 238, 232, 7 + Math.round(8 * breathe)), Math.max(260, Math.round(width * .34f)));
        paintGlow(g2, Math.round(width * .18f), Math.round(height * .92f), new Color(210, 210, 206, 5 + Math.round(7 * (1f - breathe))), Math.max(300, Math.round(width * .38f)));
        paintBreathingContours(g2, width, height, breathe);
    }

    long startedAt() {
        return started;
    }

    static void paintScene(Graphics2D g2, int width, int height, long started) {
        if (width <= 0 || height <= 0) return;
        if (Palette.light()) {
            g2.setPaint(new GradientPaint(0, 0, new Color(246, 243, 235), width, height, new Color(216, 211, 199)));
        } else {
            g2.setPaint(new GradientPaint(0, 0, new Color(5, 6, 6), width, height, new Color(22, 22, 20)));
        }
        g2.fillRect(0, 0, width, height);

        if (Palette.light()) {
            paintGlow(g2, Math.round(width * .82f), -70, new Color(255, 255, 255, 86), Math.max(420, Math.round(width * .44f)));
            paintGlow(g2, Math.round(width * .16f), height + 80, new Color(184, 172, 148, 42), Math.max(430, Math.round(width * .48f)));
            paintGlow(g2, Math.round(width * .58f), Math.round(height * .52f), new Color(255, 255, 255, 38), Math.max(360, Math.round(width * .34f)));
        } else {
            paintGlow(g2, Math.round(width * .84f), -70, new Color(242, 242, 238, 22), Math.max(420, Math.round(width * .44f)));
            paintGlow(g2, Math.round(width * .20f), height + 80, new Color(225, 225, 220, 16), Math.max(430, Math.round(width * .48f)));
            paintGlow(g2, Math.round(width * .55f), Math.round(height * .55f), new Color(255, 255, 255, 7), Math.max(360, Math.round(width * .34f)));
        }

        paintTailoredLines(g2, width, height);
        paintSilkCurves(g2, width, height);
        paintMatteGrain(g2, width, height);
        paintVignette(g2, width, height);
    }

    private static void paintBreathingContours(Graphics2D g2, int width, int height, float breathe) {
        g2.setStroke(new BasicStroke(1.15f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int alpha = 8 + Math.round(8 * breathe);
        g2.setColor(new Color(255, 255, 255, alpha));
        int cx = Math.round(width * .76f);
        int cy = Math.round(height * .35f);
        int rx = Math.max(180, Math.round(width * (.24f + .012f * breathe)));
        int ry = Math.max(140, Math.round(height * (.20f + .010f * (1f - breathe))));
        for (int i = 0; i < 3; i++) {
            g2.drawOval(cx - rx - i * 38, cy - ry - i * 25, (rx + i * 38) * 2, (ry + i * 25) * 2);
        }
    }

    private static void paintSilkCurves(Graphics2D g2, int width, int height) {
        g2.setStroke(new BasicStroke(1.05f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 5; i++) {
            float y = height * (0.18f + i * 0.14f);
            Path2D path = new Path2D.Float();
            path.moveTo(-60, y);
            path.curveTo(width * .22f, y - 48 - i * 5f, width * .44f, y + 58 + i * 4f, width + 70, y - 18);
            g2.setColor(new Color(255, 255, 255, 7 + i));
            g2.draw(path);
            g2.translate(0, 2);
            g2.setColor(new Color(0, 0, 0, 20));
            g2.draw(path);
            g2.translate(0, -2);
        }
    }

    private static void paintTailoredLines(Graphics2D g2, int width, int height) {
        g2.setColor(Palette.light() ? new Color(45, 39, 29, 14) : new Color(255, 255, 255, 9));
        for (int x = -height; x < width + height; x += 118) {
            g2.drawLine(x, 0, x + Math.round(height * .38f), height);
        }
        g2.setColor(Palette.light() ? new Color(255, 255, 255, 34) : new Color(0, 0, 0, 28));
        for (int x = -height / 2; x < width + height; x += 236) {
            g2.drawLine(x, 0, x + Math.round(height * .42f), height);
        }
    }

    private static void paintMatteGrain(Graphics2D g2, int width, int height) {
        for (int y = 3; y < height; y += 11) {
            for (int x = 5; x < width; x += 13) {
                int n = x * 73428767 ^ y * 912271 ^ width * 31 ^ height * 17;
                n ^= (n >>> 13);
                n *= 1274126177;
                int alpha = 2 + Math.abs(n % 5);
                if ((n & 8) == 0) {
                    g2.setColor(new Color(255, 255, 255, alpha));
                } else {
                    g2.setColor(new Color(0, 0, 0, alpha + 2));
                }
                g2.fillRect(x, y, 1, 1);
            }
        }
    }

    private static void paintVignette(Graphics2D g2, int width, int height) {
        int radius = Math.max(width, height);
        RadialGradientPaint vignette = new RadialGradientPaint(new Point(width / 2, height / 2), radius,
                new float[]{0f, .72f, 1f},
                Palette.light()
                        ? new Color[]{new Color(255, 255, 255, 0), new Color(110, 98, 78, 20), new Color(56, 45, 29, 72)}
                        : new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 38), new Color(0, 0, 0, 142)});
        g2.setPaint(vignette);
        g2.fillRect(0, 0, width, height);
    }

    private static void paintGlow(Graphics2D g2, int x, int y, Color color, int size) {
        RadialGradientPaint p = new RadialGradientPaint(new Point(x, y), size,
                new float[]{0f, 1f}, new Color[]{color, new Color(color.getRed(), color.getGreen(), color.getBlue(), 0)});
        g2.setPaint(p);
        g2.fillOval(x - size, y - size, size * 2, size * 2);
    }
}
