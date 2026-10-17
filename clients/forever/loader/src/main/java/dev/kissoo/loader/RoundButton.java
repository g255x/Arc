package dev.kissoo.loader;

import javax.swing.JButton;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

final class RoundButton extends JButton {
    private final boolean primary;
    private final Timer animator;
    private float hoverMix;
    private float pressMix;
    private long lastFrameNanos;

    RoundButton(String text, boolean primary) {
        super(text);
        this.primary = primary;
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(new Font("Microsoft YaHei UI", Font.BOLD, 12));
        setForeground(primary ? Palette.INK : Palette.TEXT);
        animator = new Timer(15, e -> animateStep());
        animator.setCoalesce(true);
        getModel().addChangeListener(e -> armAnimation());
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
        float hoverTarget = getModel().isRollover() && isEnabled() ? 1f : 0f;
        float pressTarget = getModel().isPressed() && isEnabled() ? 1f : 0f;
        float oldHover = hoverMix;
        float oldPress = pressMix;
        hoverMix = damp(hoverMix, hoverTarget, elapsed, 0.036f);
        pressMix = damp(pressMix, pressTarget, elapsed, 0.024f);
        if (oldHover != hoverMix || oldPress != pressMix) repaint();
        if (hoverMix == hoverTarget && pressMix == pressTarget) animator.stop();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        boolean enabled = isEnabled();

        Color fill;
        Color textColor;
        if (!enabled) {
            fill = new Color(255, 255, 255, 34);
            textColor = new Color(244, 242, 235, 96);
        } else if (primary) {
            Color primaryHover = Palette.light() ? new Color(54, 50, 42) : new Color(255, 249, 236);
            Color primaryPress = Palette.light() ? new Color(21, 20, 18) : new Color(219, 205, 178);
            fill = mix(mix(Palette.PEARL, primaryHover, hoverMix), primaryPress, pressMix);
            textColor = Palette.INK;
        } else {
            Color hover = Palette.light() ? new Color(255, 255, 255, 228) : new Color(48, 49, 44, 230);
            Color pressed = Palette.light() ? new Color(218, 211, 195, 232) : new Color(28, 29, 26, 230);
            fill = mix(mix(Palette.CARD_DARK, hover, hoverMix), pressed, pressMix);
            textColor = Palette.TEXT;
        }

        float arc = 18f + hoverMix * 3f;
        float pressInset = pressMix * 0.7f;
        Shape buttonShape = new RoundRectangle2D.Float(
                0.75f + pressInset,
                0.75f + pressInset,
                Math.max(1f, getWidth() - 1.5f - pressInset * 2f),
                Math.max(1f, getHeight() - 1.5f - pressInset * 2f),
                arc, arc
        );
        if (enabled && hoverMix > 0.01f) {
            g2.setColor(Palette.light()
                    ? new Color(48, 39, 25, Math.round(22 * hoverMix))
                    : new Color(241, 235, 224, Math.round(28 * hoverMix)));
            g2.fill(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, arc + 2f, arc + 2f));
        }
        g2.setColor(fill);
        g2.fill(buttonShape);
        g2.setColor(Palette.light()
                ? new Color(46, 36, 20, enabled ? 42 + Math.round(38 * hoverMix) : 20)
                : new Color(255, 255, 255, enabled ? 50 + Math.round(42 * hoverMix) : 22));
        g2.draw(buttonShape);

        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        String text = fit(fm, getText(), Math.max(12, getWidth() - 22));
        int x = (getWidth() - fm.stringWidth(text)) / 2;
        int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
        g2.setColor(textColor);
        g2.drawString(text, Math.max(8, x), y);
        g2.dispose();
    }

    private static float damp(float current, float target, float elapsed, float timeConstant) {
        float value = target + (current - target) * (float) Math.exp(-elapsed / timeConstant);
        return Math.abs(target - value) < 0.006f ? target : value;
    }

    private static String fit(FontMetrics fm, String text, int width) {
        if (text == null) return "";
        if (fm.stringWidth(text) <= width) return text;
        String ellipsis = "...";
        int max = Math.max(0, width - fm.stringWidth(ellipsis));
        String s = text;
        while (!s.isEmpty() && fm.stringWidth(s) > max) s = s.substring(0, s.length() - 1);
        return s.isEmpty() ? ellipsis : s + ellipsis;
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
