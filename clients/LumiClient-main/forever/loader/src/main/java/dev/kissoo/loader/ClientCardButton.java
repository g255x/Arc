package dev.kissoo.loader;

import javax.swing.JButton;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

final class ClientCardButton extends JButton {
    final ClientProfile profile;
    private final Timer animator;
    private boolean active;
    private float activeMix;
    private float hoverMix;
    private long lastFrameNanos;

    ClientCardButton(ClientProfile profile) {
        this.profile = profile;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(210, 78));
        setMinimumSize(new Dimension(170, 72));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));

        animator = new Timer(15, e -> animateStep());
        animator.setCoalesce(true);
        getModel().addChangeListener(e -> armAnimation());
    }

    void setSelectedProfile(boolean selected) {
        if (active == selected) return;
        active = selected;
        armAnimation();
    }

    private void armAnimation() {
        if (!animator.isRunning()) {
            lastFrameNanos = System.nanoTime();
            animator.start();
        }
    }

    private void animateStep() {
        long now = System.nanoTime();
        float elapsed = Math.min(0.12f, Math.max(0.001f, (now - lastFrameNanos) / 1_000_000_000f));
        lastFrameNanos = now;
        float oldActive = activeMix;
        float oldHover = hoverMix;
        activeMix = damp(activeMix, active ? 1f : 0f, elapsed, 0.050f);
        hoverMix = damp(hoverMix, getModel().isRollover() ? 1f : 0f, elapsed, 0.038f);
        if (oldActive != activeMix || oldHover != hoverMix) repaint();
        if (activeMix == (active ? 1f : 0f)
                && hoverMix == (getModel().isRollover() ? 1f : 0f)) {
            animator.stop();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int width = Math.max(1, getWidth());
        int height = Math.max(1, getHeight());
        float emphasis = Math.max(activeMix, hoverMix * 0.72f);
        float lift = 1.5f * hoverMix;
        float y = 2f - lift;
        float cardHeight = Math.max(1f, height - 4f);
        float arc = 22f;
        Shape card = new RoundRectangle2D.Float(1f, y, width - 2f, cardHeight, arc, arc);

        g2.setColor(Palette.light()
                ? new Color(70, 58, 35, 14 + Math.round(24 * emphasis))
                : new Color(0, 0, 0, 38 + Math.round(26 * emphasis)));
        g2.fill(new RoundRectangle2D.Float(2f, y + 2.5f, width - 4f, cardHeight, arc, arc));

        Color inactive = Palette.light() ? new Color(255, 252, 244, 210) : new Color(14, 15, 13, 214);
        Color activeFill = Palette.light() ? new Color(39, 37, 32, 236) : new Color(39, 38, 33, 236);
        Color hoverFill = Palette.light() ? new Color(241, 237, 226, 236) : new Color(31, 32, 28, 232);
        Color fill = mix(inactive, activeFill, activeMix);
        fill = mix(fill, hoverFill, hoverMix * (1f - activeMix * 0.45f));
        g2.setColor(fill);
        g2.fill(card);

        if (activeMix > 0.001f) {
            Color rail = Palette.light() ? new Color(238, 225, 196) : new Color(241, 235, 224);
            g2.setColor(new Color(rail.getRed(), rail.getGreen(), rail.getBlue(), Math.round(224 * activeMix)));
            g2.fill(new RoundRectangle2D.Float(1f, y + 12f, 4f, cardHeight - 24f, 4f, 4f));
            g2.setColor(new Color(218, 196, 150, Math.round(25 * activeMix)));
            g2.fill(new RoundRectangle2D.Float(6f, y + 1f, width - 12f, 2f, 12f, 12f));
        }

        g2.setColor(Palette.light()
                ? new Color(48, 38, 20, 28 + Math.round(48 * emphasis))
                : new Color(255, 255, 255, 24 + Math.round(42 * emphasis)));
        g2.draw(card);

        int textX = 20 + Math.round(activeMix * 3f);
        int titleY = Math.round(y) + 30;
        g2.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 16));
        Color activeText = Palette.light() ? Palette.INK : new Color(255, 249, 238);
        g2.setColor(mix(Palette.TEXT, activeText, activeMix));
        g2.drawString(profile.apiName, textX, titleY);

        g2.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 11));
        Color activeMuted = Palette.light() ? new Color(218, 211, 197) : new Color(195, 186, 169);
        g2.setColor(mix(Palette.MUTED, activeMuted, activeMix));
        FontMetrics metrics = g2.getFontMetrics();
        String subtitle = fit(metrics, profile.minecraft + " \u2022 Fabric " + profile.loader, width - textX - 16);
        g2.drawString(subtitle, textX, titleY + 23);
        g2.dispose();
    }

    private static float damp(float current, float target, float elapsed, float timeConstant) {
        float value = target + (current - target) * (float) Math.exp(-elapsed / timeConstant);
        return Math.abs(target - value) < 0.006f ? target : value;
    }

    private static String fit(FontMetrics fm, String text, int width) {
        if (fm.stringWidth(text) <= width) return text;
        String ellipsis = "...";
        int max = Math.max(0, width - fm.stringWidth(ellipsis));
        String value = text;
        while (!value.isEmpty() && fm.stringWidth(value) > max) {
            value = value.substring(0, value.length() - 1);
        }
        return value.isEmpty() ? ellipsis : value + ellipsis;
    }

    private static Color mix(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t)
        );
    }
}
