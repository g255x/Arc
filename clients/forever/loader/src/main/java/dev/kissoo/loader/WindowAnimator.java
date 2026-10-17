package dev.kissoo.loader;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

final class WindowAnimator {
    private static final int MAXIMIZE_DURATION_MS = 245;
    private static final int MINIMIZE_DURATION_MS = 210;
    private final JFrame frame;
    private Timer timer;
    private JWindow ghost;
    private Rectangle normalBounds;
    private boolean animating;

    WindowAnimator(JFrame frame) {
        this.frame = frame;
    }

    boolean isAnimating() {
        return animating;
    }

    boolean isMaximized() {
        return (frame.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
    }

    void toggleMaximize(Runnable finished) {
        if (animating) return;
        boolean restore = isMaximized();
        Rectangle source = frame.getBounds();
        Rectangle target;
        if (restore) {
            target = validNormalBounds();
        } else {
            normalBounds = new Rectangle(source);
            target = usableBounds();
        }
        animate(source, target, MAXIMIZE_DURATION_MS, false, () -> {
            if (restore) {
                frame.setExtendedState(frame.getExtendedState() & ~Frame.MAXIMIZED_BOTH);
                frame.setBounds(target);
            } else {
                frame.setMaximizedBounds(target);
                frame.setBounds(target);
                frame.setExtendedState(frame.getExtendedState() | Frame.MAXIMIZED_BOTH);
            }
            if (finished != null) finished.run();
        });
    }

    void minimize() {
        if (animating) return;
        Rectangle source = frame.getBounds();
        Rectangle target = minimizeTarget(source);
        animate(source, target, MINIMIZE_DURATION_MS, true,
                () -> frame.setState(Frame.ICONIFIED));
    }

    private void animate(Rectangle source, Rectangle target, int durationMs, boolean fade, Runnable completion) {
        BufferedImage image = capture();
        if (image == null || !prepareGhost(image, source)) {
            completion.run();
            return;
        }

        animating = true;
        long started = System.nanoTime();
        if (!setFrameOpacity(0f)) {
            ghost.dispose();
            ghost = null;
            animating = false;
            completion.run();
            return;
        }
        ghost.setVisible(true);
        timer = new Timer(15, e -> {
            float raw = Math.min(1f, (System.nanoTime() - started) / (durationMs * 1_000_000f));
            float eased = easeInOut(raw);
            ghost.setBounds(interpolate(source, target, eased));
            RoundedWindowSupport.apply(ghost, 30);
            if (fade) setGhostOpacity(Math.max(0.12f, 1f - eased * 0.88f));
            if (raw >= 1f) finish(completion);
        });
        timer.setCoalesce(true);
        timer.start();
    }

    private void finish(Runnable completion) {
        if (timer != null) timer.stop();
        try {
            completion.run();
        } finally {
            SwingUtilities.invokeLater(() -> {
                setFrameOpacity(1f);
                if (ghost != null) {
                    ghost.setVisible(false);
                    ghost.dispose();
                    ghost = null;
                }
                frame.revalidate();
                frame.repaint();
                animating = false;
            });
        }
    }

    private BufferedImage capture() {
        try {
            int width = frame.getWidth();
            int height = frame.getHeight();
            if (width <= 1 || height <= 1) return null;
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g2 = image.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            frame.paintAll(g2);
            g2.dispose();
            return image;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private boolean prepareGhost(BufferedImage image, Rectangle bounds) {
        try {
            ghost = new JWindow();
            ghost.setFocusableWindowState(false);
            ghost.setAlwaysOnTop(true);
            ghost.setBackground(new Color(0, 0, 0, 0));
            ghost.setContentPane(new SnapshotPanel(image));
            ghost.setBounds(bounds);
            RoundedWindowSupport.apply(ghost, 30);
            return true;
        } catch (Throwable ignored) {
            if (ghost != null) ghost.dispose();
            ghost = null;
            return false;
        }
    }

    private Rectangle validNormalBounds() {
        Rectangle usable = usableBounds();
        if (normalBounds != null && normalBounds.width >= frame.getMinimumSize().width
                && normalBounds.height >= frame.getMinimumSize().height) {
            Rectangle result = new Rectangle(normalBounds);
            if (!usable.intersects(result)) {
                result.setLocation(usable.x + (usable.width - result.width) / 2,
                        usable.y + (usable.height - result.height) / 2);
            }
            return result;
        }
        int width = Math.min(1060, usable.width);
        int height = Math.min(680, usable.height);
        return new Rectangle(usable.x + (usable.width - width) / 2,
                usable.y + (usable.height - height) / 2, width, height);
    }

    private Rectangle usableBounds() {
        GraphicsConfiguration configuration = frame.getGraphicsConfiguration();
        Rectangle screen = configuration == null
                ? new Rectangle(Toolkit.getDefaultToolkit().getScreenSize())
                : new Rectangle(configuration.getBounds());
        Insets insets = configuration == null ? new Insets(0, 0, 0, 0)
                : Toolkit.getDefaultToolkit().getScreenInsets(configuration);
        return new Rectangle(screen.x + insets.left, screen.y + insets.top,
                screen.width - insets.left - insets.right,
                screen.height - insets.top - insets.bottom);
    }

    private Rectangle minimizeTarget(Rectangle source) {
        Rectangle usable = usableBounds();
        int width = Math.max(120, Math.min(190, source.width / 5));
        int height = Math.max(22, Math.min(34, source.height / 16));
        int x = Math.max(usable.x, Math.min(source.x + source.width / 2 - width / 2, usable.x + usable.width - width));
        int y = usable.y + usable.height - Math.max(2, height / 3);
        return new Rectangle(x, y, width, height);
    }

    private boolean setFrameOpacity(float opacity) {
        try {
            frame.setOpacity(opacity);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void setGhostOpacity(float opacity) {
        try {
            if (ghost != null) ghost.setOpacity(opacity);
        } catch (Throwable ignored) {
        }
    }

    private static float easeInOut(float value) {
        return value < 0.5f
                ? 4f * value * value * value
                : 1f - (float) Math.pow(-2f * value + 2f, 3) / 2f;
    }

    private static Rectangle interpolate(Rectangle from, Rectangle to, float progress) {
        return new Rectangle(
                Math.round(from.x + (to.x - from.x) * progress),
                Math.round(from.y + (to.y - from.y) * progress),
                Math.max(1, Math.round(from.width + (to.width - from.width) * progress)),
                Math.max(1, Math.round(from.height + (to.height - from.height) * progress))
        );
    }

    private static final class SnapshotPanel extends JPanel {
        private final BufferedImage image;

        private SnapshotPanel(BufferedImage image) {
            this.image = image;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(image, 0, 0, getWidth(), getHeight(), null);
            g2.dispose();
        }
    }
}
