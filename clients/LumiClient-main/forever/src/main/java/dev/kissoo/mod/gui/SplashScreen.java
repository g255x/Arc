package dev.kissoo.mod.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;

public class SplashScreen {
    private static JFrame splashFrame;
    private static final String SPLASH_PATH = "/assets/forever/splash.png";
    private static final long MAX_DISPLAY_MS = 30000L;
    private static volatile boolean closeRequested;
    /**
     * Show the splash image while Minecraft initializes. It is closed from
     * Forever once the client has finished loading, so the init black screen
     * never becomes visible.
     */
    public static void showSwingSplash() {
        try {
            closeRequested = false;
            InputStream is = SplashScreen.class.getResourceAsStream(SPLASH_PATH);
            if (is == null) {
                System.err.println("[Forever] Splash image not found at " + SPLASH_PATH);
                return;
            }

            BufferedImage splashImage = javax.imageio.ImageIO.read(is);
            is.close();

            if (splashImage == null) {
                System.err.println("[Forever] Failed to decode splash image");
                return;
            }

            // Create undecorated window centered on screen
            splashFrame = new JFrame();
            splashFrame.setUndecorated(true);
            splashFrame.setResizable(false);
            splashFrame.setAlwaysOnTop(true);
            splashFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

            // Scale image to reasonable size if too large
            int maxWidth = 800;
            int maxHeight = 600;
            int imgW = splashImage.getWidth();
            int imgH = splashImage.getHeight();

            if (imgW > maxWidth || imgH > maxHeight) {
                double scale = Math.min((double)maxWidth / imgW, (double)maxHeight / imgH);
                imgW = (int)(imgW * scale);
                imgH = (int)(imgH * scale);
                Image scaled = splashImage.getScaledInstance(imgW, imgH, Image.SCALE_SMOOTH);
                BufferedImage temp = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = temp.createGraphics();
                g.drawImage(scaled, 0, 0, null);
                g.dispose();
                splashImage = temp;
            }

            JLabel imageLabel = new JLabel(new ImageIcon(splashImage));
            splashFrame.getContentPane().add(imageLabel);
            splashFrame.pack();

            // Center on screen
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int x = (screenSize.width - splashFrame.getWidth()) / 2;
            int y = (screenSize.height - splashFrame.getHeight()) / 2;
            splashFrame.setLocation(x, y);

            splashFrame.setVisible(true);
            startFailSafeCloser();
        } catch (Exception e) {
            System.err.println("[Forever] Failed to show splash screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void startFailSafeCloser() {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(MAX_DISPLAY_MS);
            } catch (InterruptedException ignored) {
            }
            closeSplash();
        }, "Forever-Splash-FailSafe");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Close the splash screen
     */
    public static void closeSplash() {
        closeRequested = true;
        JFrame frame = splashFrame;
        if (frame != null) {
            Runnable dispose = () -> {
                if (splashFrame == frame) {
                    frame.setVisible(false);
                    frame.dispose();
                    splashFrame = null;
                } else {
                    frame.dispose();
                }
            };
            if (SwingUtilities.isEventDispatchThread()) {
                dispose.run();
            } else {
                try {
                    SwingUtilities.invokeAndWait(dispose);
                } catch (Exception e) {
                    frame.dispose();
                    splashFrame = null;
                }
            }
        }
    }

    /**
     * Ensure splash is closed (call from client started event as fallback)
     */
    public static void ensureClosed() {
        closeSplash();
    }
}
