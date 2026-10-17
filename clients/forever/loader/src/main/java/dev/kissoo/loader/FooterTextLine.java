package dev.kissoo.loader;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

final class FooterTextLine extends JComponent {
    private static final int GAP = 24;
    private String status;

    FooterTextLine(String status) {
        this.status = status == null ? "" : status;
        setOpaque(false);
        setMinimumSize(new Dimension(0, 22));
        setPreferredSize(new Dimension(420, 22));
        setToolTipText(fullHint());
    }

    void setStatus(String status) {
        this.status = status == null ? "" : status;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int baseline;

        Font statusFont = new Font("Microsoft YaHei UI", Font.PLAIN, 13);
        Font hintFont = new Font("Microsoft YaHei UI", Font.PLAIN, 12);
        FontMetrics statusMetrics = g2.getFontMetrics(statusFont);
        FontMetrics hintMetrics = g2.getFontMetrics(hintFont);
        baseline = Math.max(
                (getHeight() - statusMetrics.getHeight()) / 2 + statusMetrics.getAscent(),
                (getHeight() - hintMetrics.getHeight()) / 2 + hintMetrics.getAscent()
        );

        String hint = width >= 610 ? fullHint() : width >= 410 ? shortHint() : "";
        int hintWidth = hint.isEmpty() ? 0 : hintMetrics.stringWidth(hint);
        int statusMax = width - hintWidth - (hint.isEmpty() ? 0 : GAP);

        if (statusMax < 140 && !hint.isEmpty()) {
            hint = "";
            hintWidth = 0;
            statusMax = width;
        }

        g2.setFont(statusFont);
        g2.setColor(Palette.TEXT);
        g2.drawString(fit(status, statusMetrics, Math.max(0, statusMax)), 0, baseline);

        if (!hint.isEmpty()) {
            g2.setFont(hintFont);
            g2.setColor(Palette.MUTED);
            g2.drawString(fit(hint, hintMetrics, Math.max(0, width - GAP)), Math.max(0, width - hintWidth), baseline);
        }
        g2.dispose();
    }

    private static String fit(String text, FontMetrics metrics, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) return "";
        if (metrics.stringWidth(text) <= maxWidth) return text;
        String ellipsis = "...";
        int ellipsisWidth = metrics.stringWidth(ellipsis);
        if (maxWidth <= ellipsisWidth) return "";

        int low = 0;
        int high = text.length();
        while (low < high) {
            int mid = (low + high + 1) >>> 1;
            if (metrics.stringWidth(text.substring(0, mid)) + ellipsisWidth <= maxWidth) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return text.substring(0, Math.max(0, low)) + ellipsis;
    }

    private static String fullHint() {
        return LoaderPreferences.chinese()
                ? "个性化客户端保持私有；官网只公开 Loader。"
                : "Personalized jars stay private; only this loader is public on the website.";
    }

    private static String shortHint() {
        return LoaderPreferences.chinese() ? "私有 Loader 分发。" : "Private loader delivery.";
    }
}
