package dev.kissoo.loader;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

final class ProgressStrip extends JComponent {
    private final Timer animator;
    private int targetProgress;
    private float displayProgress;
    private float phase;
    private boolean active;
    private String detail = "Waiting";

    ProgressStrip() {
        setMinimumSize(new Dimension(120, 46));
        setPreferredSize(new Dimension(240, 52));
        animator = new Timer(24, e -> animateStep());
        animator.setCoalesce(true);
    }

    void setActive(boolean active) {
        if (this.active == active) return;
        this.active = active;
        armAnimation();
    }

    void setProgress(int progress, String detail) {
        targetProgress = Math.max(0, Math.min(100, progress));
        this.detail = detail == null ? "" : detail;
        armAnimation();
        repaint();
    }

    int progress() {
        return targetProgress;
    }

    private void armAnimation() {
        if (!animator.isRunning()) animator.start();
    }

    private void animateStep() {
        float delta = targetProgress - displayProgress;
        displayProgress = Math.abs(delta) < 0.22f ? targetProgress : displayProgress + delta * 0.20f;
        if (active) phase = (phase + 0.025f) % 1f;
        repaint();
        if (!active && displayProgress == targetProgress) animator.stop();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = Math.max(1, getWidth());
        int height = Math.max(1, getHeight());
        int arc = Math.min(28, Math.max(18, height / 2));
        g2.setColor(Palette.light() ? new Color(255, 253, 247, 210) : new Color(9, 10, 9, 196));
        g2.fillRoundRect(0, 0, width, height, arc, arc);
        g2.setColor(Palette.light() ? new Color(54, 42, 22, 44) : new Color(255, 255, 255, 30));
        g2.drawRoundRect(0, 0, width - 1, height - 1, arc, arc);

        int innerWidth = Math.max(0, width - 10);
        int progressWidth = Math.round(innerWidth * displayProgress / 100f);
        Shape inner = new RoundRectangle2D.Float(5, 5, innerWidth, Math.max(1, height - 10), arc - 4, arc - 4);
        if (progressWidth > 0) {
            g2.setPaint(Palette.light()
                    ? new GradientPaint(5, 5, new Color(44, 39, 31), width, 5, new Color(154, 128, 78))
                    : new GradientPaint(5, 5, new Color(253, 248, 235), width, 5, new Color(185, 159, 104)));
            g2.fill(new RoundRectangle2D.Float(5, 5, progressWidth, Math.max(1, height - 10), arc - 4, arc - 4));
        }

        if (active) paintActivity(g2, inner, progressWidth, width, height, arc);

        g2.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 13));
        FontMetrics metrics = g2.getFontMetrics();
        String percent = Math.round(displayProgress) + "%";
        int rightPad = 16;
        int percentWidth = metrics.stringWidth(percent);
        int percentX = Math.max(10, width - rightPad - percentWidth);
        int available = Math.max(0, percentX - 28);
        String text = width < 150 ? "" : fit(metrics, detail, available);
        int baseline = height / 2 + 5;

        drawLabels(g2, text, percent, percentX, baseline, Palette.TEXT);
        if (progressWidth > 0) {
            Graphics2D filledText = (Graphics2D) g2.create();
            filledText.clip(new RoundRectangle2D.Float(
                    5, 5, progressWidth, Math.max(1, height - 10), arc - 4, arc - 4
            ));
            drawLabels(filledText, text, percent, percentX, baseline, Palette.INK);
            filledText.dispose();
        }
        g2.dispose();
    }

    private static void drawLabels(Graphics2D g2, String text, String percent, int percentX, int baseline, Color color) {
        g2.setColor(color);
        if (!text.isBlank()) g2.drawString(text, 18, baseline);
        g2.drawString(percent, percentX, baseline);
    }

    private void paintActivity(Graphics2D g2, Shape inner, int progressWidth, int width, int height, int arc) {
        Graphics2D fx = (Graphics2D) g2.create();
        fx.clip(inner);
        if (progressWidth <= 1) {
            int pulseWidth = Math.max(64, Math.min(150, width / 3));
            float x = -pulseWidth + phase * (width + pulseWidth * 2f);
            fx.setPaint(new LinearGradientPaint(
                    x, 0, x + pulseWidth, 0,
                    new float[]{0f, 0.5f, 1f},
                    Palette.light()
                            ? new Color[]{new Color(44, 39, 31, 0), new Color(44, 39, 31, 42), new Color(44, 39, 31, 0)}
                            : new Color[]{new Color(180, 159, 111, 0), new Color(241, 225, 190, 100), new Color(180, 159, 111, 0)}
            ));
            fx.fillRoundRect(Math.round(x), 5, pulseWidth, Math.max(1, height - 10), arc - 4, arc - 4);
        } else {
            fx.clip(new RoundRectangle2D.Float(5, 5, progressWidth, Math.max(1, height - 10), arc - 4, arc - 4));
            int sheenWidth = 92;
            float x = -sheenWidth + phase * (Math.max(progressWidth, 1) + sheenWidth * 2f);
            fx.setPaint(new LinearGradientPaint(
                    x, 0, x + sheenWidth, 0,
                    new float[]{0f, 0.5f, 1f},
                    new Color[]{new Color(255, 255, 255, 0), new Color(255, 255, 255, 92), new Color(255, 255, 255, 0)}
            ));
            fx.fillRect(0, 5, progressWidth + 5, Math.max(1, height - 10));
        }
        fx.dispose();
    }

    private static String fit(FontMetrics fm, String text, int width) {
        if (text == null || text.isBlank()) return "";
        if (fm.stringWidth(text) <= width) return text;
        String ellipsis = "...";
        int max = Math.max(0, width - fm.stringWidth(ellipsis));
        String value = text;
        while (!value.isEmpty() && fm.stringWidth(value) > max) {
            value = value.substring(0, value.length() - 1);
        }
        return value.isEmpty() ? ellipsis : value + ellipsis;
    }
}
