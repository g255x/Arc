package dev.G255.Lumi.core.impl;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

public class LoadingWindow extends JDialog {
    private static LoadingWindow INSTANCE;

    private static volatile boolean _activated;
    private static volatile long _expiryTs;
    private static volatile String _expiryDateStr;

    private static volatile boolean _startupBlocked;
    private static volatile boolean _wantReauth;
    private static final Object _STARTUP_LOCK = new Object();

    private static final long AUTO_DISMISS_MS = 4000L;
    private Timer _autoCloseTimer;

    public static void display() {
        _activated = false;
        _expiryTs = -1;
        _expiryDateStr = null;
        _startupBlocked = false;
        _wantReauth = false;
        _doDisplay();
    }

    public static void displayActivated(long expiryTs, String expiryDateStr) {
        _activated = true;
        _expiryTs = expiryTs;
        _expiryDateStr = expiryDateStr;
        _startupBlocked = true;
        _wantReauth = false;
        _doDisplay();
    }

    private static void _doDisplay() {
        if (INSTANCE != null && INSTANCE.isVisible()) return;
        if (SwingUtilities.isEventDispatchThread()) {
            if (INSTANCE != null) INSTANCE.dispose();
            INSTANCE = new LoadingWindow();
            INSTANCE.setVisible(true);
        } else {
            SwingUtilities.invokeLater(() -> {
                if (INSTANCE != null) INSTANCE.dispose();
                INSTANCE = new LoadingWindow();
                INSTANCE.setVisible(true);
            });
        }
    }

    public static void awaitStartupDecision() {
        if (!_startupBlocked) return;
        synchronized (_STARTUP_LOCK) {
            while (_startupBlocked) {
                try {
                    _STARTUP_LOCK.wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    private static void resumeStartup() {
        synchronized (_STARTUP_LOCK) {
            _startupBlocked = false;
            _STARTUP_LOCK.notifyAll();
        }
    }

    public static boolean isReauthRequested() {
        return _wantReauth;
    }

    public static void close() {
        if (SwingUtilities.isEventDispatchThread()) {
            if (INSTANCE != null) {
                INSTANCE.dispose();
                INSTANCE = null;
            }
            resumeStartup();
        } else {
            SwingUtilities.invokeLater(() -> {
                if (INSTANCE != null) {
                    INSTANCE.dispose();
                    INSTANCE = null;
                }
                resumeStartup();
            });
        }
    }

    public LoadingWindow() {
        setTitle("LumiClient");
        setModal(false);
        setResizable(false);
        setAlwaysOnTop(true);
        setUndecorated(false);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        try {
            InputStream iconStream = LoadingWindow.class.getResourceAsStream("/icon.png");
            if (iconStream != null) {
                setIconImage(ImageIO.read(iconStream));
                iconStream.close();
            }
        } catch (Exception ignored) {}

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(20, 50, 15, 50));
        panel.setBackground(new Color(240, 240, 245));

        JLabel label = new JLabel("Loading...", SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.PLAIN, 16));
        label.setForeground(new Color(50, 50, 50));

        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setPreferredSize(new Dimension(200, 6));
        bar.setBackground(new Color(220, 220, 225));
        bar.setForeground(new Color(50, 120, 220));

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(new Color(240, 240, 245));
        bottomPanel.setBorder(new EmptyBorder(5, 0, 0, 0));

        if (_activated && _expiryDateStr != null) {
            JLabel expiryLabel = new JLabel("激活有效期至:" + _expiryDateStr);
            expiryLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
            expiryLabel.setForeground(new Color(120, 120, 120));
            bottomPanel.add(expiryLabel, BorderLayout.WEST);

            JButton changeButton = new JButton("<html><u>更改激活码</u></html>");
            changeButton.setFont(new Font("SansSerif", Font.PLAIN, 11));
            changeButton.setForeground(new Color(50, 120, 220));
            changeButton.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
            changeButton.setContentAreaFilled(false);
            changeButton.setFocusPainted(false);
            changeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            changeButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    changeButton.setForeground(new Color(200, 50, 50));
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    changeButton.setForeground(new Color(50, 120, 220));
                }
            });
            changeButton.addActionListener(e -> {
                if (_autoCloseTimer != null) {
                    _autoCloseTimer.stop();
                }
                showChangeWarning();
            });
            bottomPanel.add(changeButton, BorderLayout.EAST);
        } else {
            JLabel creditLabel = new JLabel("By-G255");
            creditLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
            creditLabel.setForeground(new Color(160, 160, 160));
            bottomPanel.add(creditLabel, BorderLayout.EAST);
        }

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(new Color(240, 240, 245));
        centerPanel.add(label, BorderLayout.NORTH);
        centerPanel.add(bar, BorderLayout.CENTER);

        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(panel);
        pack();
        setSize(340, 145);
        setLocationRelativeTo(null);

        if (_activated) {
            _autoCloseTimer = new Timer((int)AUTO_DISMISS_MS, evt -> close());
            _autoCloseTimer.setRepeats(false);
            _autoCloseTimer.start();
        }
    }

    private void showChangeWarning() {
        JDialog warning1 = new JDialog(this, "警告", true);
        warning1.setResizable(false);
        warning1.setAlwaysOnTop(true);
        try {
            InputStream iconStream = LoadingWindow.class.getResourceAsStream("/icon.png");
            if (iconStream != null) {
                warning1.setIconImage(ImageIO.read(iconStream));
                iconStream.close();
            }
        } catch (Exception ignored) {}

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(20, 25, 15, 25));
        panel.setBackground(new Color(240, 240, 245));

        JLabel msgLabel = new JLabel("此操作将会使旧激活码（至" + _expiryDateStr + "）失效，你要继续吗？");
        msgLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        msgLabel.setForeground(new Color(30, 30, 30));
        msgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(new Color(240, 240, 245));

        JButton yesBtn = new JButton("是");
        yesBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        yesBtn.setForeground(Color.WHITE);
        yesBtn.setBackground(new Color(200, 50, 50));
        yesBtn.setFocusPainted(false);
        yesBtn.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));

        JButton noBtn = new JButton("否");
        noBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        noBtn.setForeground(Color.WHITE);
        noBtn.setBackground(new Color(50, 120, 220));
        noBtn.setFocusPainted(false);
        noBtn.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));

        btnPanel.add(yesBtn);
        btnPanel.add(Box.createHorizontalStrut(15));
        btnPanel.add(noBtn);

        yesBtn.addActionListener(e -> {
            warning1.dispose();
            showSecondWarning();
        });
        noBtn.addActionListener(e -> {
            warning1.dispose();
            close();
        });

        panel.add(msgLabel);
        panel.add(Box.createVerticalStrut(15));
        panel.add(btnPanel);

        warning1.setContentPane(panel);
        warning1.pack();
        warning1.setMinimumSize(new Dimension(400, 120));
        warning1.setLocationRelativeTo(this);
        warning1.setVisible(true);
    }

    private void showSecondWarning() {
        JDialog warning2 = new JDialog(this, "警告", true);
        warning2.setResizable(false);
        warning2.setAlwaysOnTop(true);
        try {
            InputStream iconStream = LoadingWindow.class.getResourceAsStream("/icon.png");
            if (iconStream != null) {
                warning2.setIconImage(ImageIO.read(iconStream));
                iconStream.close();
            }
        } catch (Exception ignored) {}

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(20, 25, 15, 25));
        panel.setBackground(new Color(255, 245, 245));

        JLabel msg1 = new JLabel("你确定要继续吗？");
        msg1.setFont(new Font("SansSerif", Font.BOLD, 14));
        msg1.setForeground(new Color(200, 50, 50));
        msg1.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel msg2 = new JLabel("这将清除当前激活码，你需要重新激活才能使用。");
        msg2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        msg2.setForeground(new Color(100, 30, 30));
        msg2.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(new Color(255, 245, 245));

        JButton yesBtn = new JButton("是，继续更改");
        yesBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        yesBtn.setForeground(Color.WHITE);
        yesBtn.setBackground(new Color(200, 50, 50));
        yesBtn.setFocusPainted(false);
        yesBtn.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));

        JButton noBtn = new JButton("否，不要更改");
        noBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        noBtn.setForeground(Color.WHITE);
        noBtn.setBackground(new Color(50, 120, 220));
        noBtn.setFocusPainted(false);
        noBtn.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));

        btnPanel.add(yesBtn);
        btnPanel.add(Box.createHorizontalStrut(15));
        btnPanel.add(noBtn);

        yesBtn.addActionListener(e -> {
            warning2.dispose();
            _wantReauth = true;
            close();
        });
        noBtn.addActionListener(e -> {
            warning2.dispose();
            close();
        });

        panel.add(msg1);
        panel.add(Box.createVerticalStrut(5));
        panel.add(msg2);
        panel.add(Box.createVerticalStrut(15));
        panel.add(btnPanel);

        warning2.setContentPane(panel);
        warning2.pack();
        warning2.setMinimumSize(new Dimension(380, 140));
        warning2.setLocationRelativeTo(this);
        warning2.setVisible(true);
    }

    public static boolean showExpiredDialog(String expiryDateStr) {
        final boolean[] result = {false};
        final CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            JDialog dialog = new JDialog((JFrame)null, "LumiClient", true);
            dialog.setResizable(false);
            dialog.setAlwaysOnTop(true);
            try {
                InputStream iconStream = LoadingWindow.class.getResourceAsStream("/icon.png");
                if (iconStream != null) {
                    dialog.setIconImage(ImageIO.read(iconStream));
                    iconStream.close();
                }
            } catch (Exception ignored) {}

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBorder(new EmptyBorder(25, 30, 20, 30));
            panel.setBackground(new Color(255, 245, 245));

            JLabel iconLabel = new JLabel("!", SwingConstants.CENTER);
            iconLabel.setFont(new Font("SansSerif", Font.BOLD, 36));
            iconLabel.setForeground(new Color(200, 50, 50));
            iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel msgLabel = new JLabel("激活码已过期于" + expiryDateStr + "！");
            msgLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
            msgLabel.setForeground(new Color(200, 50, 50));
            msgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            JButton reauthBtn = new JButton("重新激活");
            reauthBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
            reauthBtn.setForeground(Color.WHITE);
            reauthBtn.setBackground(new Color(200, 50, 50));
            reauthBtn.setFocusPainted(false);
            reauthBtn.setBorder(BorderFactory.createEmptyBorder(8, 30, 8, 30));
            reauthBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            reauthBtn.addActionListener(e -> {
                result[0] = true;
                dialog.dispose();
                latch.countDown();
            });

            dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    latch.countDown();
                }
            });

            panel.add(iconLabel);
            panel.add(Box.createVerticalStrut(10));
            panel.add(msgLabel);
            panel.add(Box.createVerticalStrut(20));
            panel.add(reauthBtn);

            dialog.setContentPane(panel);
            dialog.pack();
            dialog.setMinimumSize(new Dimension(350, 170));
            dialog.setLocationRelativeTo(null);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setVisible(true);
        });
        try {
            latch.await();
        } catch (InterruptedException ignored) {}
        return result[0];
    }
}