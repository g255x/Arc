package dev.kissoo.loader;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;

final class AnimatedCardPanel extends JPanel {
    private static final int DURATION_MS = 235;
    private final CardLayout cards = new CardLayout();
    private final Timer animator;
    private BufferedImage outgoing;
    private BufferedImage incoming;
    private String currentCard;
    private long animationStarted;
    private int direction = 1;
    private boolean animating;

    AnimatedCardPanel() {
        setOpaque(false);
        setLayout(cards);
        setDoubleBuffered(true);
        animator = new Timer(15, e -> animateFrame());
        animator.setCoalesce(true);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                finishAnimation();
            }
        });
    }

    void showCard(String name, int requestedDirection) {
        if (name == null || name.equals(currentCard)) return;
        if (currentCard == null || getWidth() <= 1 || getHeight() <= 1 || !isShowing()) {
            cards.show(this, name);
            currentCard = name;
            revalidate();
            repaint();
            return;
        }

        finishAnimation();
        outgoing = snapshot();
        cards.show(this, name);
        currentCard = name;
        cards.layoutContainer(this);
        layoutTree(this);
        incoming = snapshot();
        if (outgoing == null || incoming == null) {
            finishAnimation();
            return;
        }

        direction = requestedDirection < 0 ? -1 : 1;
        animationStarted = System.nanoTime();
        animating = true;
        animator.restart();
        repaint();
    }

    private void animateFrame() {
        if (!animating) {
            animator.stop();
            return;
        }
        if (progress() >= 1f) {
            finishAnimation();
        } else {
            repaint();
        }
    }

    private float progress() {
        return Math.min(1f, (System.nanoTime() - animationStarted) / (DURATION_MS * 1_000_000f));
    }

    private void finishAnimation() {
        animator.stop();
        animating = false;
        outgoing = null;
        incoming = null;
        repaint();
    }

    private BufferedImage snapshot() {
        int width = getWidth();
        int height = getHeight();
        if (width <= 1 || height <= 1) return null;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D copy = image.createGraphics();
        copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        copy.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        super.paintChildren(copy);
        copy.dispose();
        return image;
    }

    private static void layoutTree(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container nested && child.isVisible()) layoutTree(nested);
        }
    }

    @Override
    protected void paintChildren(Graphics g) {
        if (!animating || outgoing == null || incoming == null) {
            super.paintChildren(g);
            return;
        }

        float t = progress();
        float eased = 1f - (float) Math.pow(1f - t, 3);
        int oldX = Math.round(-direction * 14f * eased);
        int newX = Math.round(direction * 22f * (1f - eased));
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, 1f - eased)));
        g2.drawImage(outgoing, oldX, 0, null);
        g2.setComposite(AlphaComposite.SrcOver.derive(Math.min(1f, eased)));
        g2.drawImage(incoming, newX, 0, null);
        g2.dispose();
    }
}
