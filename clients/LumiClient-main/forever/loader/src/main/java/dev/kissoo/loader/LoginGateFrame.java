package dev.kissoo.loader;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

final class LoginGateFrame extends JFrame {
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ForeverLoader-LoginGate");
        t.setDaemon(true);
        return t;
    });
    private final Consumer<AuthSession> onAuthenticated;
    private final JLabel status = Ui.label("Sign in with the website to continue.", 12, Font.PLAIN, Palette.MUTED);
    private final JLabel detail = Ui.label("No token input is available in this loader.", 11, Font.PLAIN, Palette.DIM);
    private final JButton webLogin = Ui.button("Web Login", true);
    private final JButton exit = Ui.button("Exit", false);
    private Timer pollTimer;
    private String loginCode = "";
    private String loginUrl = "";
    private int pollTicks;
    private volatile boolean busy;

    LoginGateFrame(Consumer<AuthSession> onAuthenticated) {
        super("Forever Loader");
        this.onAuthenticated = onAuthenticated;
        setUndecorated(true);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setIconImage(AppIcon.image(64));
        setMinimumSize(new Dimension(520, 370));
        setSize(560, 390);
        setLocationRelativeTo(null);
        setContentPane(root());
        RoundedWindowSupport.install(this, 28);
    }

    void begin() {
        String embedded = EmbeddedToken.readToken();
        if (!embedded.isBlank()) {
            verifyEmbedded(embedded);
        } else {
            Telemetry.reportVisitorOnce();
        }
    }

    private JPanel root() {
        AuroraPanel root = new AuroraPanel();
        root.setLayout(new BorderLayout());
        root.add(new WindowChrome(this), BorderLayout.NORTH);
        WindowResizeHandler.install(this, root);

        JPanel shell = new GlassPanel(28);
        shell.setLayout(new BorderLayout(0, 18));
        shell.setBorder(new EmptyBorder(24, 26, 24, 26));

        JPanel header = Ui.transparent();
        header.setLayout(new BorderLayout(14, 0));
        header.add(new BrandOrb(), BorderLayout.WEST);
        JPanel copy = Ui.transparent();
        copy.setLayout(new java.awt.GridLayout(2, 1));
        copy.add(Ui.label("Forever Loader", 25, Font.PLAIN, Palette.TEXT));
        copy.add(Ui.label("Private access through your account.", 12, Font.PLAIN, Palette.MUTED));
        header.add(copy, BorderLayout.CENTER);
        shell.add(header, BorderLayout.NORTH);

        JPanel center = new RoundPanel(20, Palette.CARD_DARK);
        center.setBorder(new EmptyBorder(20, 22, 20, 22));
        center.setPreferredSize(new Dimension(1, 132));
        center.setMinimumSize(new Dimension(1, 132));
        center.setLayout(new BorderLayout(0, 16));
        center.add(Ui.label("Login required", 20, Font.PLAIN, Palette.TEXT), BorderLayout.NORTH);
        JPanel text = Ui.transparent();
        text.setLayout(new javax.swing.BoxLayout(text, javax.swing.BoxLayout.Y_AXIS));
        status.setAlignmentX(0f);
        detail.setAlignmentX(0f);
        status.setVerticalAlignment(SwingConstants.TOP);
        detail.setVerticalAlignment(SwingConstants.TOP);
        text.add(status);
        text.add(javax.swing.Box.createVerticalStrut(7));
        text.add(detail);
        center.add(text, BorderLayout.CENTER);
        shell.add(center, BorderLayout.CENTER);

        JPanel row = Ui.transparent();
        row.setLayout(new java.awt.GridLayout(1, 2, 12, 0));
        webLogin.setPreferredSize(new Dimension(0, 44));
        exit.setPreferredSize(new Dimension(0, 44));
        webLogin.addActionListener(e -> {
            if (!loginCode.isBlank() && !loginUrl.isBlank()) {
                openBrowser(loginUrl);
            } else {
                startWebLogin();
            }
        });
        exit.addActionListener(e -> disposeAndExit());
        row.add(webLogin);
        row.add(exit);
        shell.add(row, BorderLayout.SOUTH);

        JPanel pad = Ui.transparent();
        pad.setLayout(new BorderLayout());
        pad.setBorder(BorderFactory.createEmptyBorder(12, 18, 18, 18));
        pad.add(shell, BorderLayout.CENTER);
        root.add(pad, BorderLayout.CENTER);
        return root;
    }

    private void verifyEmbedded(String token) {
        setBusy(true, "Verifying personalized loader...", "Please wait.");
        io.submit(() -> {
            AuthClient.AuthResult result = AuthClient.verify(token, ClientProfile.FOREVER);
            SwingUtilities.invokeLater(() -> {
                if (result.allowed()) {
                    AuthSession session = new AuthSession(token, displayOwner(result), result.expiresAt(), true);
                    Telemetry.reportClient(token, ClientProfile.FOREVER);
                    openMain(session);
                } else if (SelfProtection.isHwidMismatch(result.reason())) {
                    setBusy(true, "HWID mismatch. Closing...", "This loader is bound to another device.");
                    SelfProtection.warnAndRemoveOwnJar(result.reason(), this);
                } else {
                    setBusy(false, "Personalized login failed.", result.reason());
                }
            });
        });
    }

    private void startWebLogin() {
        if (busy) return;
        setBusy(true, "Opening secure web login...", "Confirm the request in your browser.");
        io.submit(() -> {
            WebLoginClient.StartResult start = WebLoginClient.start(ClientProfile.FOREVER);
            SwingUtilities.invokeLater(() -> {
                if (!start.ok() || start.code().isBlank() || start.url().isBlank()) {
                    setBusy(false, "Web login could not start.", start.message());
                    return;
                }
                loginCode = start.code();
                loginUrl = start.url();
                pollTicks = 0;
                boolean opened = openBrowser(loginUrl);
                status.setText(opened ? "Waiting for website confirmation..." : "Login link copied to clipboard.");
                detail.setText(opened ? "Finish login in the browser, then return here." : "Paste the copied link into your browser, then confirm login.");
                webLogin.setEnabled(true);
                webLogin.setText("Open Browser Again");
                beginPolling();
            });
        });
    }

    private void beginPolling() {
        if (pollTimer != null) pollTimer.stop();
        pollTimer = new Timer(1800, e -> pollOnce());
        pollTimer.setInitialDelay(800);
        pollTimer.start();
    }

    private void pollOnce() {
        if (loginCode.isBlank()) return;
        pollTicks++;
        io.submit(() -> {
            WebLoginClient.PollResult poll = WebLoginClient.poll(loginCode, ClientProfile.FOREVER);
            SwingUtilities.invokeLater(() -> {
                if (poll.allowed() && poll.session() != null && poll.session().valid()) {
                    if (pollTimer != null) pollTimer.stop();
                    Telemetry.reportClient(poll.session().token(), ClientProfile.FOREVER);
                    openMain(poll.session());
                    return;
                }
                if (poll.pending() && pollTicks < 170) {
                    status.setText("Waiting for website confirmation...");
                    detail.setText("No token is shown or entered in the loader.");
                    return;
                }
                if (pollTimer != null) pollTimer.stop();
                loginCode = "";
                loginUrl = "";
                setBusy(false, "Web login failed.", poll.message());
            });
        });
    }

    private boolean openBrowser(String url) {
        boolean opened = BrowserLauncher.open(url);
        if (!opened) {
            status.setText("Login link copied to clipboard.");
            detail.setText("Paste it into your browser, then confirm login.");
        }
        return opened;
    }

    private void openMain(AuthSession session) {
        LoaderFrame frame = new LoaderFrame(session);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        dispose();
        onAuthenticated.accept(session);
    }

    private void setBusy(boolean busy, String statusText, String detailText) {
        this.busy = busy;
        status.setText(statusText);
        detail.setText(detailText);
        webLogin.setEnabled(!busy);
        exit.setEnabled(true);
        webLogin.setText(busy ? "Working..." : "Web Login");
    }

    private void disposeAndExit() {
        if (pollTimer != null) pollTimer.stop();
        io.shutdownNow();
        dispose();
        System.exit(0);
    }

    private static String displayOwner(AuthClient.AuthResult result) {
        return result.owner() == null || result.owner().isBlank() ? "Verified user" : result.owner();
    }
}
