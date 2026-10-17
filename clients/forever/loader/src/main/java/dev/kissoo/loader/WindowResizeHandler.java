package dev.kissoo.loader;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

final class WindowResizeHandler extends MouseAdapter {
    private static final int BORDER = 7;
    private final JFrame frame;
    private int dragCursor = Cursor.DEFAULT_CURSOR;
    private Point dragStart;
    private Rectangle startBounds;
    private long lastResizeNanos;

    private WindowResizeHandler(JFrame frame) {
        this.frame = frame;
    }

    static void install(JFrame frame, JComponent root) {
        WindowResizeHandler handler = new WindowResizeHandler(frame);
        root.addMouseListener(handler);
        root.addMouseMotionListener(handler);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (isMaximized()) {
            ((JComponent) e.getComponent()).setCursor(Cursor.getDefaultCursor());
            return;
        }
        ((JComponent) e.getComponent()).setCursor(Cursor.getPredefinedCursor(cursorFor(e.getPoint(), e.getComponent().getWidth(), e.getComponent().getHeight())));
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e) || isMaximized()) return;
        dragCursor = cursorFor(e.getPoint(), e.getComponent().getWidth(), e.getComponent().getHeight());
        if (dragCursor == Cursor.DEFAULT_CURSOR) return;
        dragStart = e.getLocationOnScreen();
        startBounds = frame.getBounds();
        lastResizeNanos = 0L;
        AuroraPanel.setInteractionFrozen(e.getComponent(), true);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (dragStart == null || startBounds == null || dragCursor == Cursor.DEFAULT_CURSOR || isMaximized()) return;
        long nowNanos = System.nanoTime();
        if (lastResizeNanos != 0L && nowNanos - lastResizeNanos < 10_000_000L) return;
        lastResizeNanos = nowNanos;
        Point now = e.getLocationOnScreen();
        int dx = now.x - dragStart.x;
        int dy = now.y - dragStart.y;
        Rectangle b = new Rectangle(startBounds);
        Dimension min = frame.getMinimumSize();

        boolean west = dragCursor == Cursor.W_RESIZE_CURSOR || dragCursor == Cursor.NW_RESIZE_CURSOR || dragCursor == Cursor.SW_RESIZE_CURSOR;
        boolean east = dragCursor == Cursor.E_RESIZE_CURSOR || dragCursor == Cursor.NE_RESIZE_CURSOR || dragCursor == Cursor.SE_RESIZE_CURSOR;
        boolean north = dragCursor == Cursor.N_RESIZE_CURSOR || dragCursor == Cursor.NW_RESIZE_CURSOR || dragCursor == Cursor.NE_RESIZE_CURSOR;
        boolean south = dragCursor == Cursor.S_RESIZE_CURSOR || dragCursor == Cursor.SW_RESIZE_CURSOR || dragCursor == Cursor.SE_RESIZE_CURSOR;

        if (west) {
            int nextWidth = startBounds.width - dx;
            if (nextWidth >= min.width) {
                b.x = startBounds.x + dx;
                b.width = nextWidth;
            }
        }
        if (east) b.width = Math.max(min.width, startBounds.width + dx);
        if (north) {
            int nextHeight = startBounds.height - dy;
            if (nextHeight >= min.height) {
                b.y = startBounds.y + dy;
                b.height = nextHeight;
            }
        }
        if (south) b.height = Math.max(min.height, startBounds.height + dy);
        frame.setBounds(b);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dragStart = null;
        startBounds = null;
        dragCursor = Cursor.DEFAULT_CURSOR;
        lastResizeNanos = 0L;
        AuroraPanel.setInteractionFrozen(e.getComponent(), false);
    }

    private boolean isMaximized() {
        return (frame.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
    }

    private static int cursorFor(Point p, int w, int h) {
        boolean left = p.x <= BORDER;
        boolean right = p.x >= w - BORDER;
        boolean top = p.y <= BORDER;
        boolean bottom = p.y >= h - BORDER;
        if (top && left) return Cursor.NW_RESIZE_CURSOR;
        if (top && right) return Cursor.NE_RESIZE_CURSOR;
        if (bottom && left) return Cursor.SW_RESIZE_CURSOR;
        if (bottom && right) return Cursor.SE_RESIZE_CURSOR;
        if (left) return Cursor.W_RESIZE_CURSOR;
        if (right) return Cursor.E_RESIZE_CURSOR;
        if (top) return Cursor.N_RESIZE_CURSOR;
        if (bottom) return Cursor.S_RESIZE_CURSOR;
        return Cursor.DEFAULT_CURSOR;
    }
}
