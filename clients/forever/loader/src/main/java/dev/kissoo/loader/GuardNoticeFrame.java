package dev.kissoo.loader;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;

final class GuardNoticeFrame extends JFrame {
    private int seconds = 5;

    static void showAndExit(GuardReport report) {
        new GuardNoticeFrame(report).setVisible(true);
    }

    private GuardNoticeFrame(GuardReport report) {
        super("Forever Loader - Environment blocked");
        setUndecorated(true);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setSize(620, 360);
        setLocationRelativeTo(null);

        AuroraPanel root = new AuroraPanel();
        root.setBorder(new EmptyBorder(24, 26, 24, 26));
        root.setLayout(new BorderLayout(0, 16));

        JLabel title = Ui.label("请先关闭冲突环境", 28, Font.PLAIN, Palette.TEXT);
        JLabel sub = Ui.label("检测到 HWID 工具、虚拟机或启动器完整性异常，Loader 将在 5 秒后关闭。", 13, Font.PLAIN, Palette.MUTED);

        JPanel header = Ui.transparent();
        header.setLayout(new GridLayout(2, 1));
        header.add(title);
        header.add(sub);

        JTextArea area = new JTextArea(report.summary());
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        area.setForeground(Palette.TEXT);
        area.setOpaque(false);

        JPanel areaShell = new GlassPanel(20);
        areaShell.setLayout(new BorderLayout());
        areaShell.setBorder(new EmptyBorder(16, 16, 16, 16));
        areaShell.add(area);

        JLabel countdown = Ui.label("Closing in 5s", 15, Font.BOLD, Palette.PEARL);
        root.add(header, BorderLayout.NORTH);
        root.add(areaShell, BorderLayout.CENTER);
        root.add(countdown, BorderLayout.SOUTH);
        setContentPane(root);
        RoundedWindowSupport.install(this, 28);

        javax.swing.Timer timer = new javax.swing.Timer(1000, e -> {
            seconds--;
            countdown.setText("Closing in " + seconds + "s");
            if (seconds <= 0) {
                ((javax.swing.Timer) e.getSource()).stop();
                System.exit(11);
            }
        });
        timer.start();
    }
}
