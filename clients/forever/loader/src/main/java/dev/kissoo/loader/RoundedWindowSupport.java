package dev.kissoo.loader;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Shape;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowStateListener;
import java.awt.geom.RoundRectangle2D;

final class RoundedWindowSupport {
    private static final int MIN_RADIUS = 12;
    private static final int MAX_RADIUS = 34;

    private RoundedWindowSupport() {
    }

    static void install(JFrame frame, int radius) {
        if (frame == null || !frame.isUndecorated() || !isSupported()) return;
        int safeRadius = Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, radius));

        ComponentAdapter componentAdapter = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                apply(frame, safeRadius);
            }

            @Override
            public void componentShown(ComponentEvent e) {
                apply(frame, safeRadius);
            }
        };
        frame.addComponentListener(componentAdapter);

        WindowStateListener stateListener = e -> SwingUtilities.invokeLater(() -> apply(frame, safeRadius));
        frame.addWindowStateListener(stateListener);

        SwingUtilities.invokeLater(() -> apply(frame, safeRadius));
    }

    static void apply(Window window, int radius) {
        if (window == null || !isSupported()) return;
        try {
            if (window instanceof Frame frame && (frame.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
                window.setShape(null);
                return;
            }
            int width = window.getWidth();
            int height = window.getHeight();
            if (width <= 0 || height <= 0) return;
            int safeRadius = Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, radius));
            Shape shape = new RoundRectangle2D.Float(0, 0, width, height, safeRadius, safeRadius);
            window.setShape(shape);
        } catch (Throwable ignored) {
        }
    }

    static void clear(Window window) {
        if (window == null) return;
        try {
            window.setShape(null);
        } catch (Throwable ignored) {
        }
    }

    private static boolean isSupported() {
        try {
            GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice device = environment.getDefaultScreenDevice();
            return device.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
