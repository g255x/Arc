package dev.kissoo.loader;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.nio.file.Path;

final class Ui {
    private Ui() {
    }

    static JLabel label(String text, int size, int style, Color color) {
        JLabel l = new JLabel(text);
        l.setForeground(color);
        l.setFont(new Font("Microsoft YaHei UI", style, size));
        return l;
    }

    static JPanel transparent() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        return p;
    }

    static JButton button(String text, boolean primary) {
        return new RoundButton(text, primary);
    }

    static String shortText(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, Math.max(0, max - 3)) + "...";
    }

    static void openFolder(Path folder) {
        try {
            java.nio.file.Files.createDirectories(folder);
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(folder.toFile());
            } else {
                new ProcessBuilder("explorer.exe", folder.toString()).start();
            }
        } catch (Throwable ignored) {
        }
    }
}
