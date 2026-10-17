package dev.kissoo.loader;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class LoaderFrame extends JFrame {
    private enum Section {
        OVERVIEW("Overview", "总览", "Dashboard", "仪表盘", "Session and delivery overview.", "会话与客户端分发总览。", "overview"),
        CLIENTS("Clients", "客户端", "Clients", "客户端", "Select the client package you want to prepare.", "选择要准备的私有客户端包。", "clients"),
        LAUNCH("Launch", "启动", "Launch", "启动", "Download, prepare, and start the selected client.", "下载、准备并启动所选客户端。", "launch"),
        LOCAL("Local", "本地", "Local setup", "本地设置", "Minecraft directory and local folders.", "Minecraft 目录与本地文件夹。", "local"),
        ACTIVITY("Activity", "日志", "Activity", "活动日志", "Recent loader events and secure delivery logs.", "最近的 Loader 事件与安全分发日志。", "activity");

        final String navEn;
        final String navZh;
        final String titleEn;
        final String titleZh;
        final String subtitleEn;
        final String subtitleZh;
        final String icon;

        Section(String navEn, String navZh, String titleEn, String titleZh, String subtitleEn, String subtitleZh, String icon) {
            this.navEn = navEn;
            this.navZh = navZh;
            this.titleEn = titleEn;
            this.titleZh = titleZh;
            this.subtitleEn = subtitleEn;
            this.subtitleZh = subtitleZh;
            this.icon = icon;
        }

        String nav() {
            return LoaderPreferences.chinese() ? navZh : navEn;
        }

        String title() {
            return LoaderPreferences.chinese() ? titleZh : titleEn;
        }

        String subtitle() {
            return LoaderPreferences.chinese() ? subtitleZh : subtitleEn;
        }
    }

    private static final DateTimeFormatter EXPIRY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ForeverLoader-IO");
        t.setDaemon(true);
        return t;
    });
    private final JTextField gameDirField = new JTextField(LoaderPaths.gameDir().toString());
    private final JTextArea logArea = new JTextArea();
    private final FooterTextLine footerText = new FooterTextLine(t("Ready", "就绪"));
    private final JLabel accountLabel = new JLabel(t("Not verified", "未验证"));
    private final JLabel expiresLabel = new JLabel(t("Not verified", "未验证"));
    private final ProgressStrip progressStrip = new ProgressStrip();
    private final ActivityFooter activityFooter = new ActivityFooter();
    private final Map<ClientProfile, ClientCardButton> clientButtons = new EnumMap<>(ClientProfile.class);
    private final Map<Section, SidebarNavButton> navButtons = new EnumMap<>(Section.class);
    private final AnimatedCardPanel contentCards = new AnimatedCardPanel();
    private final JLabel headerTitle = Ui.label("", 28, Font.PLAIN, Palette.TEXT);
    private final JLabel headerSubtitle = Ui.label("", 13, Font.PLAIN, Palette.MUTED);
    private final JLabel selectedClientLabel = new JLabel();
    private final JLabel selectedVersionLabel = new JLabel();
    private final JLabel overviewClientLabel = new JLabel();
    private final JLabel overviewVersionLabel = new JLabel();
    private final JLabel launchClientLabel = new JLabel();
    private final JLabel launchVersionLabel = new JLabel();
    private final AuthSession session;
    private ClientProfile selected = ClientProfile.FOREVER;
    private Section activeSection = Section.OVERVIEW;
    private JButton downloadButton;
    private JButton launchButton;
    private JButton pauseButton;
    private JButton languageButton;
    private JButton themeButton;
    private volatile boolean busy;

    LoaderFrame(AuthSession session) {
        this(session, ClientProfile.FOREVER, Section.OVERVIEW, null, "");
    }

    private LoaderFrame(AuthSession session, ClientProfile selected, Section activeSection, Rectangle bounds, String logSeed) {
        super("Forever Loader");
        this.session = session;
        this.selected = selected == null ? ClientProfile.FOREVER : selected;
        Palette.apply(LoaderPreferences.theme());
        setUndecorated(true);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setIconImage(AppIcon.image(64));
        setMinimumSize(new Dimension(980, 620));
        if (bounds == null) {
            setSize(1060, 680);
            setLocationRelativeTo(null);
        } else {
            setBounds(bounds);
        }
        setContentPane(buildRoot());
        RoundedWindowSupport.install(this, 30);
        showAuthenticatedSession();
        refreshButtons();
        selectSection(activeSection == null ? Section.OVERVIEW : activeSection);
        if (logSeed != null && !logSeed.isBlank()) {
            logArea.setText(logSeed);
            logArea.setCaretPosition(logArea.getDocument().getLength());
        } else {
            append("Forever Loader " + LoaderSecrets.version() + " initialized.");
            append((session.personalized() ? "Personalized" : "Web") + " session active for " + session.displayOwner() + ".");
        }
    }

    private JComponent buildRoot() {
        AuroraPanel root = new AuroraPanel();
        root.setLayout(new BorderLayout());
        root.setBorder(new EmptyBorder(0, 0, 0, 0));
        root.add(new WindowChrome(this), BorderLayout.NORTH);

        JPanel workspace = new GlassPanel(28);
        workspace.setLayout(new BorderLayout());
        workspace.add(sidebar(), BorderLayout.WEST);
        workspace.add(mainPanel(), BorderLayout.CENTER);

        JPanel body = Ui.transparent();
        body.setLayout(new BorderLayout());
        body.setBorder(new EmptyBorder(8, 20, 18, 20));
        body.add(workspace, BorderLayout.CENTER);
        root.add(body, BorderLayout.CENTER);
        WindowResizeHandler.install(this, root);
        return root;
    }

    private JPanel sidebar() {
        JPanel sidebar = Ui.transparent();
        sidebar.setPreferredSize(new Dimension(278, 1));
        sidebar.setLayout(new BorderLayout(0, 18));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Palette.light() ? new java.awt.Color(45, 36, 22, 30) : new java.awt.Color(255, 255, 255, 24)),
                new EmptyBorder(20, 20, 20, 18)
        ));
        sidebar.add(brandPanel(), BorderLayout.NORTH);

        JPanel nav = Ui.transparent();
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.add(sidebarSectionLabel(t("WORKSPACE", "工作区")));
        nav.add(Box.createVerticalStrut(6));
        for (Section section : Section.values()) {
            SidebarNavButton button = new SidebarNavButton(section.icon, section.nav());
            button.setAlignmentX(0f);
            button.addActionListener(e -> selectSection(section));
            navButtons.put(section, button);
            nav.add(button);
            nav.add(Box.createVerticalStrut(3));
        }
        nav.setBorder(new EmptyBorder(0, 0, 4, 0));

        JScrollPane navScroll = new JScrollPane(nav);
        navScroll.setOpaque(false);
        navScroll.getViewport().setOpaque(false);
        navScroll.setBorder(BorderFactory.createEmptyBorder());
        navScroll.setViewportBorder(BorderFactory.createEmptyBorder());
        navScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        navScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        navScroll.setWheelScrollingEnabled(true);
        navScroll.getVerticalScrollBar().setUnitIncrement(34);
        navScroll.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        navScroll.getVerticalScrollBar().setMinimumSize(new Dimension(0, 0));
        navScroll.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int viewportHeight = navScroll.getViewport().getExtentSize().height;
                int availableForButtons = Math.max(150, viewportHeight - 34);
                int buttonHeight = Math.max(30, Math.min(38, availableForButtons / Section.values().length - 3));
                navButtons.values().forEach(button -> button.setNavHeight(buttonHeight));
                nav.revalidate();
            }
        });
        sidebar.add(navScroll, BorderLayout.CENTER);
        sidebar.add(sidebarFooter(), BorderLayout.SOUTH);
        return sidebar;
    }

    private JLabel sidebarSectionLabel(String text) {
        JLabel label = Ui.label(text, 10, Font.BOLD, Palette.DIM);
        label.setBorder(new EmptyBorder(0, 4, 0, 0));
        label.setAlignmentX(0f);
        return label;
    }

    private JPanel brandPanel() {
        JPanel p = Ui.transparent();
        p.setLayout(new BorderLayout(14, 0));
        p.setBorder(new EmptyBorder(0, 0, 6, 0));
        BrandOrb orb = new BrandOrb();

        JPanel text = Ui.transparent();
        text.setLayout(new GridLayout(2, 1));
        text.add(Ui.label("Forever Loader", 22, Font.PLAIN, Palette.TEXT));
        text.add(Ui.label(t("One account. Two clients.", "一个账户，两个客户端。"), 12, Font.PLAIN, Palette.MUTED));

        p.add(orb, BorderLayout.WEST);
        p.add(text, BorderLayout.CENTER);
        return p;
    }

    private JPanel sidebarFooter() {
        JPanel p = Ui.transparent();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(tinyStat(t("ACCESS", "访问"), session.personalized() ? t("Personalized loader", "专属启动器") : t("Web Login", "网页登录")));
        return p;
    }

    private JPanel tinyStat(String k, String v) {
        JPanel p = new RoundPanel(16, Palette.CARD_DARK);
        p.setAlignmentX(0f);
        p.setBorder(new EmptyBorder(10, 12, 10, 12));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel title = Ui.label(k, 10, Font.BOLD, Palette.DIM);
        title.setAlignmentX(0f);
        p.add(title);
        p.add(Box.createVerticalStrut(5));

        JTextArea value = new JTextArea(v == null ? "" : v);
        value.setEditable(false);
        value.setFocusable(false);
        value.setOpaque(false);
        value.setBorder(null);
        value.setForeground(Palette.TEXT);
        value.setSelectionColor(new java.awt.Color(180, 159, 111, 110));
        value.setSelectedTextColor(Palette.TEXT);
        value.setMargin(new Insets(0, 0, 0, 0));
        value.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        value.setLineWrap(true);
        value.setWrapStyleWord(true);
        value.setAlignmentX(0f);
        value.setToolTipText(v == null ? "" : v);
        int lines = wrappedLineEstimate(value.getText(), 31);
        value.setRows(lines);
        int valueHeight = value.getFontMetrics(value.getFont()).getHeight() * lines + 3;
        value.setMinimumSize(new Dimension(1, valueHeight));
        value.setPreferredSize(new Dimension(1, valueHeight));
        value.setMaximumSize(new Dimension(Integer.MAX_VALUE, valueHeight));
        p.add(value);

        int titleHeight = title.getFontMetrics(title.getFont()).getHeight();
        int height = Math.max(58, 20 + titleHeight + 5 + valueHeight);
        p.setPreferredSize(new Dimension(1, height));
        p.setMinimumSize(new Dimension(1, height));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return p;
    }

    private static int wrappedLineEstimate(String value, int charactersPerLine) {
        if (value == null || value.isBlank()) return 1;
        int lines = 0;
        for (String logicalLine : value.split("\\R", -1)) {
            lines += Math.max(1, (logicalLine.length() + charactersPerLine - 1) / charactersPerLine);
        }
        return Math.min(6, Math.max(1, lines));
    }

    private JPanel mainPanel() {
        JPanel main = Ui.transparent();
        main.setLayout(new BorderLayout(0, 16));
        main.setBorder(new EmptyBorder(20, 22, 20, 20));
        main.add(header(), BorderLayout.NORTH);
        main.add(center(), BorderLayout.CENTER);
        main.add(footer(), BorderLayout.SOUTH);
        return main;
    }

    private JPanel header() {
        JPanel p = Ui.transparent();
        p.setLayout(new BorderLayout(16, 0));
        JPanel copy = Ui.transparent();
        copy.setLayout(new GridLayout(2, 1));
        copy.add(headerTitle);
        copy.add(headerSubtitle);
        p.add(copy, BorderLayout.CENTER);
        JPanel toggles = Ui.transparent();
        toggles.setLayout(new GridLayout(1, 2, 10, 0));
        languageButton = Ui.button(LoaderPreferences.chinese() ? "中 / EN" : "EN / 中", false);
        languageButton.setPreferredSize(new Dimension(96, 38));
        languageButton.addActionListener(e -> switchLanguage());
        themeButton = Ui.button(LoaderPreferences.theme() == LoaderPreferences.Theme.LIGHT ? t("Light", "浅色") : t("Dark", "深色"), false);
        themeButton.setPreferredSize(new Dimension(96, 38));
        themeButton.addActionListener(e -> switchTheme());
        toggles.add(languageButton);
        toggles.add(themeButton);
        p.add(toggles, BorderLayout.EAST);
        return p;
    }

    private JPanel center() {
        contentCards.add(overviewPage(), Section.OVERVIEW.name());
        contentCards.add(clientsPage(), Section.CLIENTS.name());
        contentCards.add(launchPage(), Section.LAUNCH.name());
        contentCards.add(localPage(), Section.LOCAL.name());
        contentCards.add(activityPage(), Section.ACTIVITY.name());
        return contentCards;
    }

    private JPanel overviewPage() {
        JPanel grid = Ui.transparent();
        grid.setLayout(new GridLayout(1, 2, 16, 0));
        grid.add(authCard());
        grid.add(overviewStatusCard());
        return grid;
    }

    private JPanel overviewStatusCard() {
        JPanel card = new GlassPanel(24);
        card.setBorder(new EmptyBorder(22, 22, 22, 22));
        card.setLayout(new GridBagLayout());
        card.add(sectionCopy(t("Workspace", "工作区"), t("Everything is split into clean sections now, like the web admin panel.", "现在像网站后台一样，每个功能都被拆成清晰的分区。")), cell(0, 0, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 18, 0)));

        JPanel summary = new RoundPanel(18, Palette.CARD_DARK);
        summary.setBorder(new EmptyBorder(14, 16, 14, 16));
        summary.setLayout(new GridLayout(4, 1, 0, 0));
        summary.add(row(t("Selected", "当前选择"), overviewClientLabel));
        summary.add(row("Minecraft", overviewVersionLabel));
        summary.add(row(t("Access", "访问"), new JLabel(session.personalized() ? t("Personalized loader", "专属启动器") : t("Web Login", "网页登录"))));
        summary.add(row(t("Mode", "模式"), new JLabel(t("Private delivery", "私有分发"))));
        card.add(summary, cell(0, 1, 2, 1, 1, 1, GridBagConstraints.BOTH, new Insets(0, 0, 18, 0)));

        JButton launch = Ui.button(t("Open Launch", "打开启动"), true);
        launch.setPreferredSize(new Dimension(0, 44));
        launch.addActionListener(e -> selectSection(Section.LAUNCH));
        JButton clients = Ui.button(t("Select Client", "选择客户端"), false);
        clients.setPreferredSize(new Dimension(0, 44));
        clients.addActionListener(e -> selectSection(Section.CLIENTS));
        JPanel row = Ui.transparent();
        row.setLayout(new GridLayout(1, 2, 14, 0));
        row.add(launch);
        row.add(clients);
        card.add(row, cell(0, 2, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0)));
        return card;
    }

    private JPanel clientsPage() {
        JPanel page = new GlassPanel(24);
        page.setBorder(new EmptyBorder(24, 24, 24, 24));
        page.setLayout(new BorderLayout(0, 18));
        page.add(sectionCopy(t("Client selection", "客户端选择"), t("Pick which private package the loader should prepare. The choice is reflected immediately in Launch.", "选择 Loader 要准备的私有客户端包，选择会立即同步到启动页。")), BorderLayout.NORTH);

        JPanel cards = Ui.transparent();
        cards.setLayout(new GridLayout(1, ClientProfile.values().length, 16, 0));
        for (ClientProfile profile : ClientProfile.values()) {
            ClientCardButton button = new ClientCardButton(profile);
            button.addActionListener(e -> {
                selected = profile;
                refreshButtons();
                append("Selected " + profile.apiName + ".");
                selectSection(Section.LAUNCH);
            });
            clientButtons.put(profile, button);
            cards.add(button);
        }
        page.add(cards, BorderLayout.CENTER);

        JPanel footer = new RoundPanel(18, Palette.CARD_DARK);
        footer.setBorder(new EmptyBorder(14, 16, 14, 16));
        footer.setLayout(new GridLayout(1, 3, 16, 0));
        footer.add(metric(t("Selected", "当前选择"), selectedClientLabel));
        footer.add(metric("Minecraft", selectedVersionLabel));
        footer.add(metric(t("Access", "访问"), new JLabel(session.personalized() ? t("Personalized", "专属") : t("Web Login", "网页登录"))));
        page.add(footer, BorderLayout.SOUTH);
        return page;
    }

    private JPanel launchPage() {
        JPanel page = Ui.transparent();
        page.setLayout(new GridLayout(1, 2, 16, 0));
        page.add(launchCard());
        page.add(launchTipsCard());
        return page;
    }

    private JPanel localPage() {
        JPanel page = new GlassPanel(24);
        page.setBorder(new EmptyBorder(24, 24, 24, 24));
        page.setLayout(new GridBagLayout());
        page.add(sectionCopy(t("Local setup", "本地设置"), t("Choose where Minecraft should be prepared. The loader keeps private packages outside the regular mods folder.", "选择 Minecraft 准备目录。Loader 会把私有包放在常规 mods 文件夹之外。")), cell(0, 0, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 20, 0)));

        JLabel pathLabel = Ui.label(t("MINECRAFT DIRECTORY", "MINECRAFT 目录"), 10, Font.BOLD, Palette.DIM);
        page.add(pathLabel, cell(0, 1, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 6, 0)));
        gameDirField.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        gameDirField.setForeground(Palette.TEXT);
        gameDirField.setCaretColor(Palette.ACCENT);
        gameDirField.setOpaque(false);
        gameDirField.setBorder(new EmptyBorder(0, 14, 0, 14));
        TextFieldShell pathShell = new TextFieldShell(gameDirField);
        pathShell.setPreferredSize(new Dimension(0, 44));
        JButton browse = Ui.button(t("Browse", "浏览"), false);
        browse.setPreferredSize(new Dimension(112, 44));
        browse.addActionListener(e -> chooseGameDir());
        JPanel pathRow = Ui.transparent();
        pathRow.setLayout(new BorderLayout(12, 0));
        pathRow.add(pathShell, BorderLayout.CENTER);
        pathRow.add(browse, BorderLayout.EAST);
        page.add(pathRow, cell(0, 2, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 18, 0)));

        JButton openMods = Ui.button(t("Mods Folder", "Mods 文件夹"), true);
        openMods.setPreferredSize(new Dimension(0, 44));
        openMods.addActionListener(e -> Ui.openFolder(LoaderPaths.modsDir(currentGameDir())));
        JButton openGameDir = Ui.button(t("Game Dir", "游戏目录"), false);
        openGameDir.setPreferredSize(new Dimension(0, 44));
        openGameDir.addActionListener(e -> Ui.openFolder(currentGameDir()));
        JPanel folders = Ui.transparent();
        folders.setLayout(new GridLayout(1, 2, 14, 0));
        folders.add(openMods);
        folders.add(openGameDir);
        page.add(folders, cell(0, 3, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 20, 0)));

        JPanel info = new RoundPanel(18, Palette.CARD_DARK);
        info.setBorder(new EmptyBorder(14, 16, 14, 16));
        info.setLayout(new GridLayout(3, 1, 0, 0));
        info.add(row(t("Runtime cache", "运行缓存"), new JLabel(t("Encrypted and device-bound", "加密并绑定设备"))));
        info.add(row(t("Launch mode", "启动方式"), new JLabel(t("Built-in offline launcher", "内置离线启动"))));
        info.add(row(t("Visibility", "可见性"), new JLabel(t("No regular mods folder exposure", "不暴露在常规 mods 文件夹"))));
        page.add(info, cell(0, 4, 2, 1, 1, 1, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0)));
        return page;
    }

    private JPanel activityPage() {
        JPanel page = Ui.transparent();
        page.setLayout(new BorderLayout());
        page.add(activityCard(true), BorderLayout.CENTER);
        return page;
    }

    private JPanel sectionCopy(String title, String subtitle) {
        JPanel copy = Ui.transparent();
        copy.setLayout(new GridLayout(2, 1));
        copy.add(Ui.label(title, 22, Font.PLAIN, Palette.TEXT));
        copy.add(Ui.label(subtitle, 13, Font.PLAIN, Palette.MUTED));
        return copy;
    }

    private JPanel metric(String key, JLabel value) {
        JPanel p = Ui.transparent();
        p.setLayout(new BorderLayout(0, 4));
        JLabel k = Ui.label(key, 10, Font.BOLD, Palette.DIM);
        value.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 13));
        value.setForeground(Palette.TEXT);
        p.add(k, BorderLayout.NORTH);
        p.add(value, BorderLayout.CENTER);
        return p;
    }

    private JPanel authCard() {
        JPanel auth = new GlassPanel(24);
        auth.setBorder(new EmptyBorder(22, 22, 22, 22));
        auth.setLayout(new GridBagLayout());

        JLabel authTitle = Ui.label(t("Session", "会话"), 22, Font.PLAIN, Palette.TEXT);
        GridBagConstraints c = cell(0, 0, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 16, 0));
        auth.add(authTitle, c);

        JPanel info = new RoundPanel(18, Palette.CARD_DARK);
        info.setLayout(new GridLayout(4, 1, 0, 0));
        info.setBorder(new EmptyBorder(14, 16, 14, 16));
        info.add(row(t("Account", "账户"), accountLabel));
        info.add(row(t("Expires", "到期"), expiresLabel));
        info.add(row(t("Client", "客户端"), new JLabel("Forever / Strinova")));
        info.add(row(t("Access", "访问"), new JLabel(session.personalized() ? t("Personalized", "专属") : t("Web Login", "网页登录"))));
        auth.add(info, cell(0, 1, 2, 1, 1, 1, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0)));
        return auth;
    }

    private JPanel launchCard() {
        JPanel actions = new GlassPanel(24);
        actions.setBorder(new EmptyBorder(22, 22, 22, 22));
        actions.setLayout(new GridBagLayout());

        JLabel launchTitle = Ui.label(t("Delivery", "分发"), 22, Font.PLAIN, Palette.TEXT);
        actions.add(launchTitle, cell(0, 0, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 10, 0)));

        JLabel launchCopy = Ui.label("<html>" + t("Secure delivery is personalized to this account and device.", "安全分发会按当前账户和设备进行个性化准备。") + "</html>", 13, Font.PLAIN, Palette.MUTED);
        launchCopy.setToolTipText(t("Downloads are prepared through the same private account delivery used by the website.", "下载通过与网站一致的私有账户分发流程准备。"));
        actions.add(launchCopy, cell(0, 1, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 18, 0)));

        progressStrip.setMinimumSize(new Dimension(120, 46));
        progressStrip.setPreferredSize(new Dimension(0, 52));
        actions.add(progressStrip, cell(0, 2, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 20, 0)));

        downloadButton = Ui.button(t("Download & Start", "下载并启动"), true);
        downloadButton.setToolTipText(t("Download if needed, then start the selected client", "需要时下载，然后启动所选客户端"));
        downloadButton.setPreferredSize(new Dimension(0, 44));
        downloadButton.addActionListener(e -> downloadSelected(true));

        launchButton = Ui.button(t("Prepare Only", "仅准备"), false);
        launchButton.setToolTipText(t("Prepare the secure package without starting Minecraft", "只准备安全包，不启动 Minecraft"));
        launchButton.setPreferredSize(new Dimension(0, 44));
        launchButton.addActionListener(e -> downloadSelected(false));
        pauseButton = Ui.button(t("Pause", "暂停"), false);
        pauseButton.setToolTipText(t("Pause or resume current download", "暂停或继续当前下载"));
        pauseButton.setPreferredSize(new Dimension(0, 44));
        pauseButton.setEnabled(false);
        pauseButton.addActionListener(e -> togglePause());
        JPanel actionRow = Ui.transparent();
        actionRow.setLayout(new GridLayout(1, 3, 12, 0));
        actionRow.add(downloadButton);
        actionRow.add(launchButton);
        actionRow.add(pauseButton);
        actions.add(actionRow, cell(0, 3, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 14, 0)));

        JPanel info = new RoundPanel(18, Palette.CARD_DARK);
        info.setBorder(new EmptyBorder(14, 16, 14, 16));
        info.setLayout(new GridLayout(3, 1, 0, 0));
        info.add(row(t("Selected", "当前选择"), launchClientLabel));
        info.add(row("Minecraft", launchVersionLabel));
        info.add(row(t("Runtime", "运行包"), new JLabel(t("Private encrypted package", "私有加密包"))));
        actions.add(info, cell(0, 4, 2, 1, 1, 1, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0)));
        return actions;
    }

    private JPanel launchTipsCard() {
        JPanel tips = new GlassPanel(24);
        tips.setBorder(new EmptyBorder(22, 22, 22, 22));
        tips.setLayout(new GridBagLayout());
        tips.add(sectionCopy(t("Launch flow", "启动流程"), t("A cleaner split: choose client, prepare delivery, inspect logs only when you need them.", "流程更干净：选择客户端、准备分发，只在需要时查看日志。")), cell(0, 0, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 18, 0)));

        JPanel steps = new RoundPanel(18, Palette.CARD_DARK);
        steps.setBorder(new EmptyBorder(16, 16, 16, 16));
        steps.setLayout(new GridLayout(4, 1, 0, 0));
        steps.add(row("01", new JLabel(t("Pick Forever or Strinova in Clients", "在客户端中选择 Forever 或 Strinova"))));
        steps.add(row("02", new JLabel(t("Check Minecraft directory in Local", "在本地页检查 Minecraft 目录"))));
        steps.add(row("03", new JLabel(t("Download & Start from Launch", "在启动页下载并启动"))));
        steps.add(row("04", new JLabel(t("Read full delivery logs in Activity", "在日志页查看完整分发记录"))));
        tips.add(steps, cell(0, 1, 2, 1, 1, 1, GridBagConstraints.BOTH, new Insets(0, 0, 18, 0)));

        JButton clients = Ui.button(t("Clients", "客户端"), true);
        clients.setPreferredSize(new Dimension(0, 44));
        clients.addActionListener(e -> selectSection(Section.CLIENTS));
        JButton activity = Ui.button(t("Activity", "日志"), false);
        activity.setPreferredSize(new Dimension(0, 44));
        activity.addActionListener(e -> selectSection(Section.ACTIVITY));
        JPanel row = Ui.transparent();
        row.setLayout(new GridLayout(1, 2, 14, 0));
        row.add(clients);
        row.add(activity);
        tips.add(row, cell(0, 2, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0)));
        return tips;
    }

    private JPanel activityCard(boolean roomy) {
        JPanel card = new GlassPanel(24);
        card.setBorder(new EmptyBorder(22, 22, 22, 22));
        card.setLayout(new BorderLayout(0, 16));
        card.add(sectionCopy(t("Activity", "活动日志"), t("Recent loader events. Scroll wheel works, but the ugly native scrollbar stays hidden.", "最近的 Loader 事件。滚轮可用，但隐藏原生滚动条。")), BorderLayout.NORTH);

        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setOpaque(false);
        logArea.setFont(new Font("Cascadia Mono", Font.PLAIN, roomy ? 13 : 12));
        logArea.setForeground(Palette.MUTED);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setOpaque(false);
        logScroll.getViewport().setOpaque(false);
        logScroll.setBorder(BorderFactory.createEmptyBorder());
        logScroll.setViewportBorder(BorderFactory.createEmptyBorder());
        logScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        logScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        logScroll.getVerticalScrollBar().setUnitIncrement(18);
        logScroll.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        logScroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 0));
        logScroll.setWheelScrollingEnabled(true);
        JPanel logShell = new RoundPanel(18, Palette.CARD_DARK);
        logShell.setMinimumSize(new Dimension(0, 0));
        logShell.setLayout(new BorderLayout());
        logShell.setBorder(new EmptyBorder(15, 16, 15, 16));
        logShell.add(logScroll);
        card.add(logShell, BorderLayout.CENTER);
        return card;
    }

    private GridBagConstraints cell(int x, int y, int w, int h, double wx, double wy, int fill, Insets insets) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.gridwidth = w;
        c.gridheight = h;
        c.weightx = wx;
        c.weighty = wy;
        c.fill = fill;
        c.insets = insets;
        c.anchor = GridBagConstraints.NORTHWEST;
        return c;
    }

    private JPanel row(String key, JLabel value) {
        JPanel p = Ui.transparent();
        p.setLayout(new BorderLayout());
        JLabel k = Ui.label(key, 11, Font.BOLD, Palette.DIM);
        value.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        value.setForeground(Palette.TEXT);
        p.add(k, BorderLayout.WEST);
        p.add(value, BorderLayout.EAST);
        return p;
    }

    private JPanel footer() {
        JPanel p = activityFooter;
        p.setBorder(new EmptyBorder(12, 16, 12, 16));
        p.setLayout(new BorderLayout());
        p.add(footerText, BorderLayout.CENTER);
        return p;
    }

    private boolean handlePersonalizedAuthFailure(AuthClient.AuthResult result) {
        if (!session.personalized() || !SelfProtection.isHwidMismatch(result.reason())) return false;
        setBusy(true, t("HWID mismatch. Closing...", "HWID 不匹配，正在关闭..."));
        append(t("HWID mismatch detected for personalized loader.", "检测到专属启动器 HWID 不匹配。"));
        SelfProtection.warnAndRemoveOwnJar(result.reason(), this);
        return true;
    }

    private void downloadSelected(boolean launchAfter) {
        if (busy) return;
        if (!session.valid()) {
            status(t("Please login first.", "请先登录。"));
            return;
        }
        String token = session.token();
        Path gameDir = currentGameDir();
        setBusy(true, launchAfter ? t("Preparing launch...", "正在准备启动...") : t("Downloading...", "正在下载..."));
        progressStrip.setProgress(0, t("Connecting...", "正在连接..."));
        io.submit(() -> {
            AuthClient.AuthResult auth = AuthClient.verify(token, selected);
            if (!auth.allowed()) {
                SwingUtilities.invokeLater(() -> {
                    if (handlePersonalizedAuthFailure(auth)) return;
                    progressStrip.setProgress(0, t("Session failed", "会话失败"));
                    status(t("Session failed: ", "会话失败：") + auth.reason());
                    append(t("Download blocked: ", "下载已阻止：") + auth.reason());
                    setBusy(false, t("Ready", "就绪"));
                });
                return;
            }
            Telemetry.reportClient(token, selected);
            Path out;
            try {
                out = CloudPackageClient.prepareRuntime(selected, token, gameDir,
                        (percent, detail) -> SwingUtilities.invokeLater(() -> progressStrip.setProgress(percent, detail)),
                        msg -> SwingUtilities.invokeLater(() -> append(msg)));
            } catch (Throwable throwable) {
                out = null;
                SwingUtilities.invokeLater(() -> {
                    progressStrip.setProgress(0, t("Cloud package failed", "云端包失败"));
                    append(t("Secure package failed: ", "安全包失败：") + publicError(throwable));
                });
            }
            final Path runtimeOut = out;
            if (runtimeOut != null) {
                ClientAuthSessionWriter.write(gameDir, token, auth.expiresAt());
            }
            if (runtimeOut != null && launchAfter) {
                SwingUtilities.invokeLater(() -> {
                    showAuthenticatedSession(auth);
                    append(t("Secure runtime prepared. Starting Minecraft...", "安全运行包已准备，正在启动 Minecraft..."));
                    status(t("Preparing built-in offline launch...", "正在准备内置离线启动..."));
                });
                boolean started = MinecraftLauncher.launchOffline(selected, gameDir, runtimeOut,
                        auth.owner().isBlank() ? System.getProperty("user.name", "ForeverUser") : auth.owner(),
                        msg -> SwingUtilities.invokeLater(() -> append(msg)),
                        (percent, detail) -> SwingUtilities.invokeLater(() -> progressStrip.setProgress(percent, detail)));
                SwingUtilities.invokeLater(() -> {
                    status(started ? t("Minecraft started. Loader minimized.", "Minecraft 已启动，Loader 已最小化。") : t("Minecraft could not be started.", "Minecraft 未能启动。"));
                    setBusy(false, "");
                    if (started) {
                        minimizeAfterSuccessfulLaunch();
                    }
                });
            } else {
                SwingUtilities.invokeLater(() -> {
                    if (runtimeOut != null) {
                        showAuthenticatedSession(auth);
                        progressStrip.setProgress(100, t("Cloud cache ready", "云端缓存就绪"));
                        append(t("Secure package prepared successfully.", "安全包已准备完成。"));
                        status(t("Secure package ready for ", "安全包就绪：") + selected.apiName + ".");
                    } else {
                        status(t("Cloud package failed.", "云端包失败。"));
                    }
                    setBusy(false, "");
                });
            }
        });
    }

    private void minimizeAfterSuccessfulLaunch() {
        if (!isShowing()) return;
        new WindowAnimator(this).minimize();
    }

    private void chooseGameDir() {
        JFileChooser chooser = new JFileChooser(currentGameDir().toFile());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            gameDirField.setText(chooser.getSelectedFile().toPath().toString());
        }
    }

    private Path currentGameDir() {
        String text = gameDirField.getText().trim();
        if (text.isBlank()) return LoaderPaths.gameDir();
        return java.nio.file.Paths.get(text);
    }

    private void refreshButtons() {
        clientButtons.forEach((profile, button) -> button.setSelectedProfile(profile == selected));
        String version = selected.minecraft + " / Fabric " + selected.loader;
        selectedClientLabel.setText(selected.title);
        selectedVersionLabel.setText(version);
        overviewClientLabel.setText(selected.title);
        overviewVersionLabel.setText(version);
        launchClientLabel.setText(selected.title);
        launchVersionLabel.setText(version);
        status(t("Selected: ", "已选择：") + selected.title + " / Minecraft " + selected.minecraft);
    }

    private void selectSection(Section section) {
        int direction = Integer.compare(section.ordinal(), activeSection.ordinal());
        activeSection = section;
        headerTitle.setText(section.title());
        headerSubtitle.setText(section.subtitle());
        navButtons.forEach((s, button) -> button.setSelectedNav(s == section));
        contentCards.showCard(section.name(), direction);
        status(section.title() + " · " + selected.title);
        contentCards.revalidate();
        contentCards.repaint();
    }

    private void setBusy(boolean busy, String message) {
        this.busy = busy;
        if (busy) {
            DownloadPause.resume();
            if (pauseButton != null) {
                pauseButton.setText(t("Pause", "暂停"));
                pauseButton.setEnabled(true);
            }
        } else {
            DownloadPause.resume();
            if (pauseButton != null) {
                pauseButton.setText(t("Pause", "暂停"));
                pauseButton.setEnabled(false);
            }
        }
        activityFooter.setActive(busy);
        progressStrip.setActive(busy);
        downloadButton.setEnabled(!busy);
        launchButton.setEnabled(!busy);
        if (!message.isBlank()) status(message);
    }

    private void togglePause() {
        if (!busy) return;
        if (DownloadPause.isPaused()) {
            DownloadPause.resume();
            pauseButton.setText(t("Pause", "暂停"));
            progressStrip.setActive(true);
            progressStrip.setProgress(progressStrip.progress(), t("Resuming...", "继续中..."));
            status(t("Resuming download...", "正在继续下载..."));
            append(t("Download resumed.", "下载已继续。"));
        } else {
            DownloadPause.pause();
            pauseButton.setText(t("Resume", "继续"));
            progressStrip.setActive(false);
            progressStrip.setProgress(progressStrip.progress(), t("Paused", "已暂停"));
            status(t("Download paused.", "下载已暂停。"));
            append(t("Download paused.", "下载已暂停。"));
        }
    }

    private void switchLanguage() {
        if (busy) {
            status(t("Cannot switch language during download.", "下载中暂不能切换语言。"));
            return;
        }
        LoaderPreferences.toggleLanguage();
        recreateMainFrame(t("Language switched.", "语言已切换。"));
    }

    private void switchTheme() {
        if (busy) {
            status(t("Cannot switch theme during download.", "下载中暂不能切换主题。"));
            return;
        }
        LoaderPreferences.toggleTheme();
        recreateMainFrame(t("Theme switched.", "主题已切换。"));
    }

    private void recreateMainFrame(String note) {
        Rectangle bounds = getBounds();
        int state = getExtendedState();
        String logs = logArea.getText();
        LoaderFrame frame = new LoaderFrame(session, selected, activeSection, bounds, logs);
        frame.setExtendedState(state);
        frame.setVisible(true);
        frame.append(note);
        dispose();
    }

    private void status(String text) {
        footerText.setStatus(text);
    }

    private void append(String text) {
        logArea.append("[" + TimeText.TIME.format(Instant.now()) + "] " + text + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void showAuthenticatedSession() {
        accountLabel.setText(session.displayOwner());
        expiresLabel.setText(formatExpiry(session.expiresAt()));
    }

    private void showAuthenticatedSession(AuthClient.AuthResult result) {
        accountLabel.setText(displayOwner(result));
        expiresLabel.setText(formatExpiry(result.expiresAt()));
    }

    private static String displayOwner(AuthClient.AuthResult result) {
        return result.owner() == null || result.owner().isBlank() ? t("Verified user", "已验证用户") : result.owner();
    }

    private static String formatExpiry(long expiresAt) {
        if (expiresAt <= 0) return t("No expiry", "不过期");
        long epochSeconds = expiresAt > 10_000_000_000L ? expiresAt / 1000L : expiresAt;
        try {
            return EXPIRY_TIME.format(Instant.ofEpochSecond(epochSeconds));
        } catch (Throwable ignored) {
            return t("Verified", "已验证");
        }
    }

    private static String publicError(Throwable throwable) {
        String message = throwable == null ? "Unknown error" : String.valueOf(throwable.getMessage());
        if (message.isBlank() || "null".equalsIgnoreCase(message)) {
            return throwable == null ? "Unknown error" : throwable.getClass().getSimpleName();
        }
        return message
                .replaceAll("(?i)[a-f0-9]{16,}\\.(?:bin|jar|tmp|raw)", "secure package")
                .replaceAll("(?i)(?:[a-z]:\\\\|/)[^\\r\\n]*?\\.(?:bin|jar|tmp|raw)", "secure package");
    }

    private static String t(String en, String zh) {
        return LoaderPreferences.chinese() ? zh : en;
    }
}
