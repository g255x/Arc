package dev.G255.Lumi.core.impl;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

public class LicenseDialog extends JDialog {

    private final JTextField keyField;
    private final JLabel errorLabel;
    private final JButton continueButton;
    private final JCheckBox agreementCheckBox;
    private boolean _ac = false;

    private static String _s(String _b64) {
        return new String(Base64.getDecoder().decode(_b64));
    }

    private static final String LICENSE_TEXT = "版权所有 (c) [2026] [G255 / LumiClient]\n\n闭源软件内部源代码及外部使用许可协议\n\n定义\n1.1 本软件指本项目全部源代码、二进制可执行文件、资源文件以及文档。全部著作权与知识产权归版权方所有，本软件对公众为闭源软件。\n1.2 内部接收方指经版权方授予源代码访问权限的团队成员。\n1.3 外部使用者指获取本软件二进制发行版本，且不属于内部接收方的任何个人或组织。\n1.4 披露行为包括上传至公开代码仓库、转发、泄露、售卖或者分享源代码以及修改后的软件程序。\n1.5 禁止行为包含逆向工程、反编译、反汇编、破解、篡改程序二进制文件、制作衍生作品以及二次分发本软件。\n\n拥有源代码访问权限的内部接收方条款\n版权方授予内部接收方仅限项目内部工作用途的有限许可。\n允许行为：阅读以及审阅源代码；在版权方明确安排下，于内部环境进行调试与评估工作。\n未经版权方书面明确许可，内部接收方不得执行以下任何行为：\n以任何形式向任何第三方披露或者分享源代码以及代码片段。\n向内部或者外部主体分发、交付经过修改的源代码。\n基于本源代码开发、售卖或者发布任何衍生软件或产品。\n复制源代码用于本项目无关的用途。\n内部接收方负有保密义务。保密义务在退出本项目之后持续有效。许可终止时，内部接收方应当销毁自身所持有的全部源代码副本。\n本协议不转移源代码的任何所有权，全部知识产权始终归版权方所有。\n\n仅获取二进制程序的外部使用者条款\n外部使用者获得有限许可，可以在遵守本协议前提下运行、使用本软件的发行二进制程序。\n外部使用者严格禁止执行以下任何行为：\n对本软件进行破解、反编译、反汇编或者逆向工程。\n修改、修补或者篡改程序二进制文件。\n基于本软件制作衍生程序。\n未经版权方书面许可，分发、传播本软件原版或者修改版本。\n移除或者篡改本软件内包含的任何版权声明。\n外部使用者不获得对于源代码的任何访问以及使用权利。\n\n许可终止\n内部接收方一旦离开项目，其源代码访问权限自动终止。\n一旦违反本协议任意条款，对应的使用许可立即终止，违约方应当停止全部相关使用行为。版权方保留追究侵权法律责任的权利。\n\n免责声明\n本软件按现状提供，不存在任何明示或者暗示的担保。\n在适用法律允许的最大范围内，版权方不对因使用本软件所产生的任何直接或者间接损害承担责任。\n\n其他条款\n本协议为本软件完整许可协议。对于协议条款的任何修改或者豁免，均需要版权方书面确认方可生效。";

    private static String _dc() {
        try {
            return (String) LicenseManager._entry(0);
        } catch (Throwable _t) {
            return "00000000000000000000000000000000";
        }
    }

    private static boolean _vk(String _dc, String _key) {
        try {
            Object _r = LicenseManager._entry(2, _dc, _key);
            return _r instanceof Boolean && (Boolean)_r;
        } catch (Throwable _t) {
            return false;
        }
    }

    private static int _vt(String _dc, String _key) {
        try {
            Object _r = LicenseManager._entry(15, _dc, _key);
            return _r instanceof Number ? ((Number)_r).intValue() : 2;
        } catch (Throwable _t) {
            return 2;
        }
    }

    private static void _sv(String _key, boolean _ag) {
        try {
            LicenseManager._entry(3, _key, _ag);
        } catch (Throwable _ignored) {}
    }

    public LicenseDialog(String _sk, boolean _sa) {
        setTitle("LumiClient");
        setModal(true);
        setResizable(false);
        setAlwaysOnTop(true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        try {
            InputStream iconStream = LicenseDialog.class.getResourceAsStream("/icon.png");
            if (iconStream != null) {
                setIconImage(ImageIO.read(iconStream));
                iconStream.close();
            }
        } catch (Exception ignored) {
        }

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(25, 30, 25, 30));
        mainPanel.setBackground(new Color(240, 240, 245));

        JLabel titleLabel = new JLabel("LumiClient");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        titleLabel.setForeground(new Color(20, 20, 20));
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);

        JLabel devLabel = new JLabel("Developer version");
        devLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        devLabel.setForeground(new Color(200, 50, 50));
        devLabel.setAlignmentX(CENTER_ALIGNMENT);

        String deviceCode = _dc();

        JLabel deviceCodeTitle = new JLabel("Your device code:");
        deviceCodeTitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        deviceCodeTitle.setForeground(new Color(100, 100, 100));
        deviceCodeTitle.setAlignmentX(CENTER_ALIGNMENT);

        JTextField deviceCodeField = new JTextField(deviceCode);
        deviceCodeField.setFont(new Font("Monospaced", Font.PLAIN, 13));
        deviceCodeField.setEditable(false);
        deviceCodeField.setForeground(new Color(30, 100, 180));
        deviceCodeField.setBackground(Color.WHITE);
        deviceCodeField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 190), 1),
                new EmptyBorder(6, 10, 6, 10)));
        deviceCodeField.setMaximumSize(new Dimension(600, 32));
        deviceCodeField.setAlignmentX(CENTER_ALIGNMENT);
        deviceCodeField.setHorizontalAlignment(JTextField.CENTER);

        JLabel keyTitle = new JLabel("Enter your key below");
        keyTitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        keyTitle.setForeground(new Color(100, 100, 100));
        keyTitle.setAlignmentX(CENTER_ALIGNMENT);

        keyField = new JTextField();
        if (_sk != null && !_sk.isEmpty()) {
            keyField.setText(_sk);
        }
        keyField.setFont(new Font("Monospaced", Font.PLAIN, 13));
        keyField.setForeground(new Color(50, 50, 50));
        keyField.setBackground(Color.WHITE);
        keyField.setCaretColor(new Color(50, 50, 50));
        keyField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 190), 1),
                new EmptyBorder(6, 10, 6, 10)));
        keyField.setMaximumSize(new Dimension(600, 32));
        keyField.setAlignmentX(CENTER_ALIGNMENT);
        keyField.setHorizontalAlignment(JTextField.CENTER);

        JPanel agreementPanel = new JPanel();
        agreementPanel.setLayout(new BoxLayout(agreementPanel, BoxLayout.X_AXIS));
        agreementPanel.setBackground(new Color(240, 240, 245));
        agreementPanel.setAlignmentX(CENTER_ALIGNMENT);
        agreementPanel.setMaximumSize(new Dimension(500, 25));

        agreementCheckBox = new JCheckBox();
        agreementCheckBox.setBackground(new Color(240, 240, 245));
        agreementCheckBox.setSelected(_sa);

        JLabel agreementLabel = new JLabel("<html><u>我已阅读并同意【闭源软件内部源代码及外部使用许可协议】</u></html>");
        agreementLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        agreementLabel.setForeground(new Color(50, 120, 220));
        agreementLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        agreementLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showLicenseWindow();
            }
        });

        agreementPanel.add(agreementCheckBox);
        agreementPanel.add(Box.createHorizontalStrut(5));
        agreementPanel.add(agreementLabel);
        agreementPanel.add(Box.createHorizontalGlue());

        errorLabel = new JLabel(" ");
        errorLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        errorLabel.setForeground(new Color(220, 60, 60));
        errorLabel.setAlignmentX(CENTER_ALIGNMENT);

        continueButton = new JButton("Continue");
        continueButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        continueButton.setForeground(Color.WHITE);
        continueButton.setBackground(new Color(50, 120, 220));
        continueButton.setFocusPainted(false);
        continueButton.setBorder(BorderFactory.createEmptyBorder(8, 30, 8, 30));
        continueButton.setAlignmentX(CENTER_ALIGNMENT);
        continueButton.setMaximumSize(new Dimension(200, 40));
        continueButton.setEnabled(_sa);
        if (!_sa) {
            continueButton.setBackground(new Color(160, 160, 160));
        }

        agreementCheckBox.addActionListener(e -> {
            boolean selected = agreementCheckBox.isSelected();
            continueButton.setEnabled(selected);
            if (selected) {
                continueButton.setBackground(new Color(50, 120, 220));
            } else {
                continueButton.setBackground(new Color(160, 160, 160));
            }
        });

        continueButton.addActionListener(e -> {
            String key = keyField.getText().trim();
            if (key.isEmpty()) {
                errorLabel.setText("请输入激活码");
                return;
            }
            continueButton.setEnabled(false);
            continueButton.setBackground(new Color(160, 160, 160));
            errorLabel.setText("");
            LoadingWindow.display();
            new Thread(() -> {
                int result = _vt(deviceCode, key);
                SwingUtilities.invokeLater(() -> {
                    LoadingWindow.close();
                    if (result == 0) {
                        _sv(key, agreementCheckBox.isSelected());
                        _ac = true;
                        long _et = _getExpiry();
                        String _ed = _formatExpiry(_et);
                        showSuccessDialog(_ed);
                        dispose();
                    } else {
                        errorLabel.setForeground(new Color(200, 50, 50));
                        switch (result) {
                            case 1: errorLabel.setText("激活码格式错误"); break;
                            case 2: errorLabel.setText("激活码验证失败，请输入正确的激活码"); break;
                            case 3: errorLabel.setText("激活码已过期"); break;
                            case 4: errorLabel.setText("无法连接验证服务器，请检查网络后重试"); break;
                            default: errorLabel.setText("验证失败 (错误码: " + result + ")");
                        }
                        keyField.setText("");
                        continueButton.setEnabled(agreementCheckBox.isSelected());
                        if (agreementCheckBox.isSelected()) {
                            continueButton.setBackground(new Color(50, 120, 220));
                        }
                    }
                });
            }).start();
        });

        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(2));
        mainPanel.add(devLabel);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(deviceCodeTitle);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(deviceCodeField);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(keyTitle);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(keyField);
        mainPanel.add(Box.createVerticalStrut(12));
        mainPanel.add(agreementPanel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(errorLabel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(continueButton);
        mainPanel.add(Box.createVerticalStrut(10));

        setContentPane(mainPanel);
        pack();
        setSize(520, 440);
        setLocationRelativeTo(null);

        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                keyField.requestFocusInWindow();
            }
        });

        keyField.addActionListener(e -> {
            if (continueButton.isEnabled()) {
                continueButton.doClick();
            }
        });
    }

    private void showLicenseWindow() {
        JDialog licenseDialog = new JDialog(this, "闭源软件内部源代码及外部使用许可协议", false);
        licenseDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        licenseDialog.setSize(650, 500);
        licenseDialog.setLocationRelativeTo(this);

        try {
            InputStream iconStream = LicenseDialog.class.getResourceAsStream("/icon.png");
            if (iconStream != null) {
                licenseDialog.setIconImage(ImageIO.read(iconStream));
                iconStream.close();
            }
        } catch (Exception ignored) {
        }

        JTextArea textArea = new JTextArea(LICENSE_TEXT);
        textArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBackground(new Color(250, 250, 250));
        textArea.setForeground(new Color(30, 30, 30));
        textArea.setBorder(new EmptyBorder(15, 15, 15, 15));
        textArea.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(null);

        licenseDialog.add(scrollPane);
        licenseDialog.setVisible(true);
    }

    private void showSuccessDialog(String _ed) {
        JDialog dialog = new JDialog(this, "验证成功", true);
        dialog.setResizable(false);
        dialog.setAlwaysOnTop(true);
        try {
            InputStream iconStream = LicenseDialog.class.getResourceAsStream("/icon.png");
            if (iconStream != null) {
                dialog.setIconImage(ImageIO.read(iconStream));
                iconStream.close();
            }
        } catch (Exception ignored) {}

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(20, 30, 15, 30));
        panel.setBackground(new Color(240, 240, 245));

        JLabel label = new JLabel("验证成功！有效期至" + _ed);
        label.setFont(new Font("SansSerif", Font.PLAIN, 15));
        label.setForeground(new Color(30, 30, 30));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton okButton = new JButton("确定");
        okButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        okButton.setForeground(Color.WHITE);
        okButton.setBackground(new Color(50, 150, 50));
        okButton.setFocusPainted(false);
        okButton.setBorder(BorderFactory.createEmptyBorder(6, 28, 6, 28));
        okButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        okButton.addActionListener(e -> dialog.dispose());

        panel.add(label);
        panel.add(Box.createVerticalStrut(15));
        panel.add(okButton);

        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(300, 130));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private static long _getExpiry() {
        try {
            Object _r = LicenseManager._entry(12);
            if (_r instanceof Long) return (Long)_r;
        } catch (Throwable _t) {}
        return -1L;
    }

    private static String _formatExpiry(long _ts) {
        if (_ts < 0) return "无限期";
        if (_ts == 0) return "已过期";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        return sdf.format(new Date(_ts * 1000));
    }

    public boolean isLicenseAccepted() {
        return _ac;
    }
}