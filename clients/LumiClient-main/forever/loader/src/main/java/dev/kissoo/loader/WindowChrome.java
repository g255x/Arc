package dev.kissoo.loader;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

final class WindowChrome extends JPanel {
    private enum Control {
        MINIMIZE, MAXIMIZE, CLOSE
    }

    private final JFrame frame;
    private final WindowAnimator windowAnimator;
    private Point pressedOnScreen;
    private Point frameLocation;
    private long lastDragMoveNanos;

    WindowChrome(JFrame frame) {
        this.frame = frame;
        this.windowAnimator = new WindowAnimator(frame);
        setOpaque(false);
        setPreferredSize(new Dimension(1, 44));
        setMinimumSize(new Dimension(1, 44));
        setLayout(new BorderLayout());
        setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 18, 0, 12));
        setCursor(Cursor.getDefaultCursor());

        JPanel brand = Ui.transparent();
        brand.setLayout(new BorderLayout(10, 0));
        JLabel icon = new JLabel(new javax.swing.ImageIcon(AppIcon.image(20)));
        JLabel title = Ui.label("Forever Loader", 12, Font.PLAIN, Palette.TEXT);
        brand.add(icon, BorderLayout.WEST);
        brand.add(title, BorderLayout.CENTER);

        JPanel controls = Ui.transparent();
        controls.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 7));
        controls.add(new ChromeButton(Control.MINIMIZE, windowAnimator::minimize));
        controls.add(new ChromeButton(Control.MAXIMIZE, this::toggleMaximize));
        controls.add(new ChromeButton(Control.CLOSE, () -> frame.dispatchEvent(new java.awt.event.WindowEvent(frame, java.awt.event.WindowEvent.WINDOW_CLOSING))));

        add(brand, BorderLayout.WEST);
        add(controls, BorderLayout.EAST);

        MouseAdapter drag = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    pressedOnScreen = e.getLocationOnScreen();
                    frameLocation = frame.getLocation();
                    lastDragMoveNanos = 0L;
                    AuroraPanel.setInteractionFrozen(WindowChrome.this, true);
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    toggleMaximize();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (pressedOnScreen == null || frameLocation == null || isMaximized()) return;
                long nowNanos = System.nanoTime();
                if (lastDragMoveNanos != 0L && nowNanos - lastDragMoveNanos < 8_000_000L) return;
                lastDragMoveNanos = nowNanos;
                Point now = e.getLocationOnScreen();
                frame.setLocation(frameLocation.x + now.x - pressedOnScreen.x, frameLocation.y + now.y - pressedOnScreen.y);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (pressedOnScreen != null && frameLocation != null && !isMaximized()) {
                    Point now = e.getLocationOnScreen();
                    frame.setLocation(frameLocation.x + now.x - pressedOnScreen.x, frameLocation.y + now.y - pressedOnScreen.y);
                }
                pressedOnScreen = null;
                frameLocation = null;
                lastDragMoveNanos = 0L;
                AuroraPanel.setInteractionFrozen(WindowChrome.this, false);
            }
        };
        addMouseListener(drag);
        addMouseMotionListener(drag);
        brand.addMouseListener(drag);
        brand.addMouseMotionListener(drag);
    }

    private void toggleMaximize() {
        windowAnimator.toggleMaximize(this::repaint);
    }

    private boolean isMaximized() {
        return (frame.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        g2.setPaint(Palette.light()
                ? new java.awt.GradientPaint(0, 0, new Color(243, 239, 229, 232), 0, h, new Color(229, 224, 211, 146))
                : new java.awt.GradientPaint(0, 0, new Color(16, 17, 15, 232), 0, h, new Color(9, 10, 10, 126)));
        g2.fillRect(0, 0, w, h);
        g2.setColor(Palette.light() ? new Color(44, 36, 22, 20) : new Color(255, 255, 255, 16));
        g2.drawLine(0, h - 1, w, h - 1);
        g2.dispose();
        super.paintComponent(g);
    }

    private final class ChromeButton extends JButton {
        private final Control control;
        private final Runnable action;

        ChromeButton(Control control, Runnable action) {
            this.control = control;
            this.action = action;
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(38, 30));
            setMinimumSize(new Dimension(38, 30));
            addActionListener(e -> action.run());
            getModel().addChangeListener(e -> repaint());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            boolean hover = getModel().isRollover();
            boolean press = getModel().isPressed();
            int alpha = hover ? (press ? 54 : 34) : 0;
            Color fill = control == Control.CLOSE
                    ? new Color(210, 70, 70, hover ? (press ? 210 : 164) : 0)
                    : (Palette.light() ? new Color(34, 29, 20, alpha) : new Color(255, 255, 255, alpha));
            g2.setColor(fill);
            g2.fill(new RoundRectangle2D.Float(2f, 4f, getWidth() - 4f, getHeight() - 8f, 12f, 12f));

            g2.setStroke(new BasicStroke(1.55f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(control == Control.CLOSE && hover ? Color.WHITE
                    : (Palette.light() ? new Color(42, 36, 27, hover ? 235 : 178) : new Color(232, 228, 216, hover ? 235 : 178)));
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            switch (control) {
                case MINIMIZE -> g2.drawLine(cx - 6, cy + 5, cx + 6, cy + 5);
                case MAXIMIZE -> {
                    if (isMaximized()) {
                        g2.drawRect(cx - 5, cy - 3, 9, 8);
                        g2.drawRect(cx - 2, cy - 6, 9, 8);
                    } else {
                        g2.drawRect(cx - 5, cy - 5, 10, 10);
                    }
                }
                case CLOSE -> {
                    g2.drawLine(cx - 5, cy - 5, cx + 5, cy + 5);
                    g2.drawLine(cx + 5, cy - 5, cx - 5, cy + 5);
                }
            }
            g2.dispose();
        }
    }
}
