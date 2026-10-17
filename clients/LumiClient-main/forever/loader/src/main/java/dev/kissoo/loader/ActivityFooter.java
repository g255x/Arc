package dev.kissoo.loader;

import javax.swing.Timer;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

final class ActivityFooter extends RoundPanel {
    private final Timer animator;
    private boolean active;
    private float intensity;
    private float phase;

    ActivityFooter() {
        super(20, Palette.light() ? new Color(255, 255, 255, 112) : new Color(255, 255, 255, 18));
        animator = new Timer(30, e -> animateStep());
        animator.setCoalesce(true);
    }

    void setActive(boolean active) {
        if (this.active == active) return;
        this.active = active;
        if (!animator.isRunning()) animator.start();
    }

    private void animateStep() {
        float target = active ? 1f : 0f;
        float delta = target - intensity;
        intensity = Math.abs(delta) < 0.018f ? target : intensity + delta * (active ? 0.18f : 0.12f);
        if (active) phase = (phase + 0.018f) % 1f;
        repaint();
        if (!active && intensity == 0f) animator.stop();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (intensity <= 0.001f || getWidth() <= 1 || getHeight() <= 1) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Shape clip = new RoundRectangle2D.Float(0, 0, getWidth() - 1f, getHeight() - 1f, radius, radius);
        g2.clip(clip);

        float center = -180f + phase * (getWidth() + 360f);
        float alpha = intensity;
        g2.setPaint(new LinearGradientPaint(
                center - 170f, 0f, center + 170f, 0f,
                new float[]{0f, 0.38f, 0.5f, 0.62f, 1f},
                new Color[]{
                        new Color(218, 196, 150, 0),
                        new Color(218, 196, 150, Math.round(11 * alpha)),
                        new Color(255, 247, 228, Math.round(34 * alpha)),
                        new Color(218, 196, 150, Math.round(11 * alpha)),
                        new Color(218, 196, 150, 0)
                }
        ));
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.setPaint(new LinearGradientPaint(
                center - 105f, 0f, center + 105f, 0f,
                new float[]{0f, 0.5f, 1f},
                new Color[]{
                        new Color(180, 159, 111, 0),
                        new Color(245, 229, 194, Math.round(150 * alpha)),
                        new Color(180, 159, 111, 0)
                }
        ));
        g2.fillRect(0, getHeight() - 2, getWidth(), 2);
        g2.dispose();
    }
}
