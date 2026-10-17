package dev.kissoo.loader;

import javax.swing.JButton;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

final class SidebarNavButton extends JButton {
    private final String icon;
    private boolean selected;
    private float mix;
    private final Timer animator;

    SidebarNavButton(String icon, String text) {
        super(text);
        this.icon = icon;
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setHorizontalAlignment(LEFT);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(new Font("Microsoft YaHei UI", Font.BOLD, 12));
        setPreferredSize(new java.awt.Dimension(1, 38));
        setMinimumSize(new java.awt.Dimension(1, 38));
        setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 38));
        animator = new Timer(16, e -> tick());
        animator.setCoalesce(true);
        getModel().addChangeListener(e -> startAnimation());
    }

    void setNavHeight(int height) {
        int safeHeight = Math.max(30, Math.min(38, height));
        java.awt.Dimension size = new java.awt.Dimension(1, safeHeight);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, safeHeight));
    }

    void setSelectedNav(boolean selected) {
        if (this.selected == selected) return;
        this.selected = selected;
        startAnimation();
    }

    private void startAnimation() {
        if (!animator.isRunning()) animator.start();
    }

    private void tick() {
        float target = selected || getModel().isRollover() ? 1f : 0f;
        mix += (target - mix) * 0.22f;
        if (Math.abs(target - mix) < 0.01f) {
            mix = target;
            animator.stop();
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        Color base = selected ? Palette.PEARL : (Palette.light()
                ? new Color(43, 36, 22, Math.round(10 + 24 * mix))
                : new Color(255, 255, 255, Math.round(12 + 18 * mix)));
        Color fill = selected ? base : new Color(base.getRed(), base.getGreen(), base.getBlue(), Math.round(base.getAlpha() * mix));
        g2.setColor(fill);
        g2.fill(new RoundRectangle2D.Float(0.5f, 1.5f, w - 1.5f, h - 3f, 15f, 15f));
        g2.setColor(Palette.light()
                ? new Color(45, 36, 22, selected ? 60 : Math.round(24 + 32 * mix))
                : new Color(255, 255, 255, selected ? 54 : Math.round(20 + 24 * mix)));
        g2.draw(new RoundRectangle2D.Float(0.5f, 1.5f, w - 1.5f, h - 3f, 15f, 15f));
        if (selected) {
            g2.setColor(new Color(180, 159, 111, 190));
            g2.fill(new RoundRectangle2D.Float(6f, 12f, 3f, h - 24f, 3f, 3f));
        }

        Color foreground = selected ? Palette.INK : (Palette.light()
                ? new Color(42, 37, 29, Math.round(178 + 64 * mix))
                : new Color(244, 242, 235, Math.round(170 + 70 * mix)));
        paintIcon(g2, 27f, h / 2f, foreground);

        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(foreground);
        int y = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(getText(), 43, y);
        g2.dispose();
    }

    private void paintIcon(Graphics2D g2, float cx, float cy, Color color) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1.55f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (icon) {
            case "overview" -> paintOverview(g2, cx, cy);
            case "clients" -> paintClients(g2, cx, cy);
            case "launch" -> paintLaunch(g2, cx, cy);
            case "local" -> paintLocal(g2, cx, cy);
            case "activity" -> paintActivity(g2, cx, cy);
            default -> g2.draw(new Ellipse2D.Float(cx - 2.5f, cy - 2.5f, 5f, 5f));
        }
    }

    private static void paintOverview(Graphics2D g2, float x, float y) {
        float s = 4.2f;
        g2.draw(new RoundRectangle2D.Float(x - 6f, y - 6f, s, s, 1.3f, 1.3f));
        g2.draw(new RoundRectangle2D.Float(x + 1.8f, y - 6f, s, s, 1.3f, 1.3f));
        g2.draw(new RoundRectangle2D.Float(x - 6f, y + 1.8f, s, s, 1.3f, 1.3f));
        g2.draw(new RoundRectangle2D.Float(x + 1.8f, y + 1.8f, s, s, 1.3f, 1.3f));
    }

    private static void paintClients(Graphics2D g2, float x, float y) {
        g2.draw(new Ellipse2D.Float(x - 5.4f, y - 6.2f, 5.3f, 5.3f));
        g2.draw(new Ellipse2D.Float(x + 0.7f, y - 5.2f, 4.2f, 4.2f));
        g2.draw(new RoundRectangle2D.Float(x - 7f, y + 0.4f, 8.4f, 5.1f, 4f, 4f));
        g2.draw(new RoundRectangle2D.Float(x + 0.3f, y + 1.1f, 6.5f, 4.4f, 3.4f, 3.4f));
    }

    private static void paintLaunch(Graphics2D g2, float x, float y) {
        Path2D.Float play = new Path2D.Float();
        play.moveTo(x - 4.2f, y - 6.2f);
        play.lineTo(x + 6.1f, y);
        play.lineTo(x - 4.2f, y + 6.2f);
        play.closePath();
        g2.fill(play);
    }

    private static void paintLocal(Graphics2D g2, float x, float y) {
        Path2D.Float folder = new Path2D.Float();
        folder.moveTo(x - 7f, y - 4.6f);
        folder.lineTo(x - 1.8f, y - 4.6f);
        folder.lineTo(x + 0.2f, y - 2.4f);
        folder.lineTo(x + 7f, y - 2.4f);
        folder.lineTo(x + 7f, y + 5.2f);
        folder.lineTo(x - 7f, y + 5.2f);
        folder.closePath();
        g2.draw(folder);
    }

    private static void paintActivity(Graphics2D g2, float x, float y) {
        Path2D.Float pulse = new Path2D.Float();
        pulse.moveTo(x - 7f, y + 0.5f);
        pulse.lineTo(x - 3.5f, y + 0.5f);
        pulse.lineTo(x - 1.1f, y - 5.2f);
        pulse.lineTo(x + 1.7f, y + 5.2f);
        pulse.lineTo(x + 4f, y - 1.2f);
        pulse.lineTo(x + 7f, y - 1.2f);
        g2.draw(pulse);
    }
}
