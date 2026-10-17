package dev.kissoo.loader;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

final class LoaderBootstrap {
    private LoaderBootstrap() {
    }

    static void start(String[] args) {
        System.setProperty("sun.java2d.uiScale.enabled", "true");
        SessionEntropy.rotate();
        Palette.apply(LoaderPreferences.theme());
        setupLookAndFeel();

        GuardReport report = LocalEnvironmentGuard.inspect();
        SwingUtilities.invokeLater(() -> {
            if (report.blocked()) {
                GuardNoticeFrame.showAndExit(report);
                return;
            }
            LoginGateFrame gate = new LoginGateFrame(session -> {
                if (report.hasWarnings()) {
                    JOptionPane.showMessageDialog(null,
                            report.summary(),
                            "环境提醒",
                            JOptionPane.WARNING_MESSAGE);
                }
            });
            gate.setVisible(true);
            gate.begin();
        });
    }

    private static void setupLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Throwable ignored) {
        }
    }
}
