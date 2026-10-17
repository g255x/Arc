package dev.kissoo.mod.gui.music;

import dev.kissoo.Forever;
import dev.kissoo.api.utils.render.Render2DUtil;
import hoprc.api.core.impl.FontManager;
import hoprc.api.gui.fonts.FontRenderer;
import dev.kissoo.mod.music.*;
import dev.kissoo.mod.music.NeteaseLoginManager.LoginCallback;
import dev.kissoo.mod.music.NeteaseLoginManager.LoginState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class MusicPlayerScreen extends Screen {
    private final MusicPlayerService service;
    private final MinecraftClient mc;
    private final MusicPlayerConfig cfg;
    private final NeteaseLoginManager loginManager;

    // ===== 自绘输入框相关 =====
    private String searchText = "";
    private boolean searchFocused = false;
    private int searchCursorPos = 0;
    private long cursorBlinkStart = System.currentTimeMillis();
    private static final long CURSOR_BLINK_MS = 530;

    // ===== 图标常量 =====
    private static final String ICON_LOOP_QUEUE = "\u0042";
    private static final String ICON_LOOP_ONE   = "\u0052";
    private static final String ICON_SONG       = "\u007B";
    private static final String ICON_LYRIC      = "\u0048";
    private static final String ICON_PLAY       = "\u0038";
    private static final String ICON_PAUSE      = "\u0030";
    private static final String ICON_VOL        = "\u00EB";
    private static final String ICON_PREV       = "⏮";
    private static final String ICON_NEXT       = "⏭";
    private static final String ICON_HEART      = "♥";

    // ===== 布局常量 =====
    private static final int SIDEBAR_W = 130;
    private static final int BOTTOM_H  = 60;
    private static final int TOP_H     = 46;
    private static final int ROW_H     = 38;

    // ===== 数据状态 =====
    private volatile boolean searching;
    private volatile List<NeteaseSong> results    = List.of();
    private volatile List<NeteaseUser> userResults = List.of();
    private int scroll;

    private boolean draggingProgress = false;
    private boolean draggingVolume   = false;
    private long    dragSeekMs       = -1;

    // ===== 搜索模式 =====
    private enum SearchMode { SONGS, USERS, USER_LIKED, MY_LIKED }
    private SearchMode currentMode = SearchMode.SONGS;
    private NeteaseUser selectedUser;

    // ===== 侧边栏菜单项 =====
    private enum SidebarItem { DISCOVER, MY_LIKED }
    private SidebarItem activeSidebarItem = SidebarItem.DISCOVER;

    // ===== 登录弹窗 =====
    private boolean showingLoginDialog = false;
    private String  loginStatusMessage = "";
    private int     loginStatusColor   = 0xFFFFFFFF;
    private NativeImageBackedTexture qrTexture;
    private Identifier qrTextureId;

    public MusicPlayerScreen(MusicPlayerService service, MusicPlayerConfig cfg) {
        super(Text.literal("Music Player"));
        this.service      = service;
        this.mc           = MinecraftClient.getInstance();
        this.cfg          = cfg;
        this.loginManager = NeteaseLoginManager.getInstance();
    }

    @Override
    protected void init() {
        scroll = Math.max(0, cfg.lastScroll);
        if (cfg.lastQuery != null && !cfg.lastQuery.isBlank()) {
            searchText      = cfg.lastQuery;
            searchCursorPos = searchText.length();
            doSearch();
        }
    }

    // ==================== 数据请求 ====================

    private void cleanupQrResources() {
        if (qrTexture  != null) { qrTexture.close();  qrTexture  = null; }
        if (qrTextureId != null) { mc.getTextureManager().destroyTexture(qrTextureId); qrTextureId = null; }
    }

    private void logout() {
        loginManager.reset();
        service.api().clearAuthCookies();
        showingLoginDialog = false;
        cleanupQrResources();
        // 如果当前正在查看我的喜欢，退出登录后回到发现
        if (currentMode == SearchMode.MY_LIKED) {
            currentMode        = SearchMode.SONGS;
            activeSidebarItem  = SidebarItem.DISCOVER;
            results            = List.of();
        }
    }

    private void doSearch() {
        String q = searchText;
        if (q == null || q.isBlank() || searching) return;
        cfg.pushSearch(q); cfg.lastQuery = q.trim(); cfg.save();
        searching = true; results = List.of(); userResults = List.of();
        currentMode       = SearchMode.SONGS;
        activeSidebarItem = SidebarItem.DISCOVER;
        scroll = 0;
        CompletableFuture.runAsync(() -> {
            try { results = service.api().search(q.trim(), 50, 0); }
            catch (Throwable t) {
                System.err.println("[MusicPlayer] Search failed: " + t.getMessage());
                results = List.of();
            }
        }).whenComplete((ok, err) -> {
            searching = false;
            if (err != null) System.err.println("[MusicPlayer] Search error: " + err.getMessage());
        });
    }

    private void doSearchUsers() {
        String q = searchText;
        if (q == null || q.isBlank() || searching) return;
        cfg.pushSearch(q); cfg.lastQuery = q.trim(); cfg.save();
        searching = true; results = List.of(); userResults = List.of();
        currentMode       = SearchMode.USERS;
        activeSidebarItem = SidebarItem.DISCOVER;
        scroll = 0;
        CompletableFuture.runAsync(() -> {
            try { userResults = service.api().searchUsers(q.trim(), 30, 0); }
            catch (Throwable t) {
                System.err.println("[MusicPlayer] SearchUser failed: " + t.getMessage());
                userResults = List.of();
            }
        }).whenComplete((ok, err) -> {
            searching = false;
            if (err != null) System.err.println("[MusicPlayer] SearchUser error: " + err.getMessage());
        });
    }

    private void loadUserLikedSongs(NeteaseUser user) {
        if (user == null || searching) return;
        selectedUser = user;
        currentMode  = SearchMode.USER_LIKED;
        searching    = true; results = List.of(); scroll = 0;
        CompletableFuture.runAsync(() -> {
            try { results = service.api().getUserLikedSongs(user.userId(), 200); }
            catch (Throwable t) { results = List.of(); }
        }).whenComplete((ok, err) -> searching = false);
    }

    // 【新增】加载当前登录用户自己喜欢的音乐
    private void loadMyLikedSongs() {
        if (searching) return;
        currentMode       = SearchMode.MY_LIKED;
        activeSidebarItem = SidebarItem.MY_LIKED;
        searching         = true;
        results           = List.of();
        userResults       = List.of();
        scroll            = 0;

        CompletableFuture.runAsync(() -> {
            try {
                // 先获取当前登录用户的 UID
                long uid = fetchCurrentUserUid();
                if (uid <= 0) {
                    System.err.println("[MusicPlayer] Cannot get current user uid");
                    results = List.of();
                    return;
                }
                results = service.api().getUserLikedSongs(uid, 200);
            } catch (Throwable t) {
                System.err.println("[MusicPlayer] Load my liked failed: " + t.getMessage());
                results = List.of();
            }
        }).whenComplete((ok, err) -> searching = false);
    }

    // 【新增】获取当前登录用户的 UID
    private long fetchCurrentUserUid() {
        try {
            Map<String, String> cookies = loginManager.getLoginCookies();
            // 从 Cookie 中尝试直接读取 UID（网易云会在 Cookie 里存 __uid 或 MUSIC_U 解码）
            // 更可靠的方式是调用 /api/nuser/account/get 接口
            java.net.URI uri = java.net.URI.create("https://music.163.com/api/nuser/account/get");
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                    .build();

            StringBuilder cookieStr = new StringBuilder();
            for (Map.Entry<String, String> entry : cookies.entrySet()) {
                if (cookieStr.length() > 0) cookieStr.append("; ");
                cookieStr.append(entry.getKey()).append("=").append(entry.getValue());
            }

            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder(uri)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Referer", "https://music.163.com/")
                    .header("Cookie", cookieStr.toString())
                    .GET()
                    .build();

            java.net.http.HttpResponse<String> resp = client.send(req,
                    java.net.http.HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));

            if (resp.statusCode() == 200) {
                com.google.gson.JsonObject root = new com.google.gson.Gson()
                        .fromJson(resp.body(), com.google.gson.JsonObject.class);
                // 响应结构: {"account": {"id": 12345, ...}, "profile": {...}}
                if (root != null && root.has("account")) {
                    com.google.gson.JsonObject account = root.getAsJsonObject("account");
                    if (account.has("id")) return account.get("id").getAsLong();
                }
                // 兜底：尝试 profile.userId
                if (root != null && root.has("profile")) {
                    com.google.gson.JsonObject profile = root.getAsJsonObject("profile");
                    if (profile.has("userId")) return profile.get("userId").getAsLong();
                }
            }
        } catch (Throwable t) {
            System.err.println("[MusicPlayer] fetchCurrentUserUid error: " + t.getMessage());
        }
        return -1;
    }

    private void togglePlayPause() {
        if (service.getCurrentSong() == null) return;
        if (service.getPlaybackState() == MusicPlayerService.PlaybackState.PLAYING && !service.isPaused())
            service.pause();
        else service.resume();
    }

    // ==================== 登录弹窗 ====================

    private void showLoginDialog() {
        this.showingLoginDialog = true;
        this.loginStatusMessage = "正在获取二维码...";
        this.loginStatusColor   = 0xFFFFFFAA;
        cleanupQrResources();

        loginManager.fetchUnikey().thenAccept(unikey -> {
            if (unikey != null) {
                generateQrTexture(unikey);
                loginManager.startPolling(unikey, new LoginCallback() {
                    @Override
                    public void onStateChange(LoginState newState, String message) {
                        mc.execute(() -> {
                            loginStatusMessage = message;
                            switch (newState) {
                                case WAITING_SCAN -> loginStatusColor = 0xFFFFFFAA;
                                case SCANNED      -> loginStatusColor = 0xFFAAFFAA;
                                case SUCCESS      -> {
                                    loginStatusColor   = 0xFF00FF00;
                                    showingLoginDialog = false;
                                    cleanupQrResources();
                                }
                                case EXPIRED -> loginStatusColor = 0xFFFFAAAA;
                                case FAILED  -> loginStatusColor = 0xFFFF5555;
                            }
                        });
                    }
                    @Override
                    public void onSuccess(Map<String, String> cookies) {
                        mc.execute(() -> service.api().setAuthCookies(cookies));
                    }
                });
            }
        }).exceptionally(t -> {
            mc.execute(() -> { loginStatusMessage = "获取二维码失败"; loginStatusColor = 0xFFFF5555; });
            return null;
        });
    }

    private void generateQrTexture(String unikey) {
        try {
            BufferedImage image = loginManager.generateQrCode(unikey);
            if (image != null) {
                NativeImage nativeImage = new NativeImage(
                        NativeImage.Format.RGBA, image.getWidth(), image.getHeight(), false);
                for (int x = 0; x < image.getWidth(); x++) {
                    for (int y = 0; y < image.getHeight(); y++) {
                        int rgb = image.getRGB(x, y);
                        int a = (rgb >> 24) & 0xFF, r = (rgb >> 16) & 0xFF,
                                g = (rgb >> 8) & 0xFF,  b = rgb & 0xFF;
                        nativeImage.setColor(x, y, (a << 24) | (b << 16) | (g << 8) | r);
                    }
                }
                qrTexture   = new NativeImageBackedTexture(nativeImage);
                qrTextureId = Identifier.of("alien", "music/qrcode_" + System.currentTimeMillis());
                mc.getTextureManager().registerTexture(qrTextureId, qrTexture);
            }
        } catch (Exception e) {
            System.err.println("[MusicPlayer] generateQrTexture failed: " + e.getMessage());
        }
    }

    // ==================== 渲染 ====================

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        int panelW = Math.min(680, this.width - 40);
        int panelH = Math.min(420, this.height - 40);
        int panelX = (this.width  - panelW) / 2;
        int panelY = (this.height - panelH) / 2;

        if (Forever.BLUR != null) Forever.BLUR.applyBlur(30.0F, panelX, panelY, panelW, panelH);

        Render2DUtil.drawRoundedRect(context.getMatrices(), panelX, panelY, panelW, panelH,
                8f, new Color(20, 20, 23, 90));

        context.enableScissor(panelX, panelY, panelX + SIDEBAR_W, panelY + panelH);
        Render2DUtil.drawRoundedRect(context.getMatrices(), panelX, panelY, SIDEBAR_W * 2, panelH,
                8f, new Color(15, 15, 18, 50));
        context.disableScissor();
        int sidebarLineH = panelH - BOTTOM_H;
        Render2DUtil.drawRect(context.getMatrices(), panelX + SIDEBAR_W - 1, panelY, 1, sidebarLineH,
                new Color(255, 255, 255, 12).getRGB());

        renderSidebar(context, mouseX, mouseY, panelX, panelY, panelH);
        renderTopBar(context, mouseX, mouseY, panelX + SIDEBAR_W, panelY, panelW - SIDEBAR_W);
        renderMainContent(context, mouseX, mouseY, panelX, panelY, panelW, panelH);
        renderBottomControlBar(context, mouseX, mouseY, panelX, panelY, panelW, panelH);

        if (showingLoginDialog) {
            drawLoginDialog(context, mouseX, mouseY, panelX, panelY, panelW, panelH);
        }
    }

    // 【核心改动】侧边栏增加「我喜欢的音乐」
    private void renderSidebar(DrawContext ctx, int mouseX, int mouseY, int x, int y, int h) {
        // Logo
        FontManager.get(12f).drawString(ctx.getMatrices(), "🎵 Forever Music", x + 12, y + 20, 0xFFFFFFFF);

        // 分组标签
        FontManager.get(9f).drawString(ctx.getMatrices(), "发现", x + 15, y + 58, 0xFF777777);

        // 「发现音乐」菜单项
        boolean activeDiscover = activeSidebarItem == SidebarItem.DISCOVER;
        boolean hoverDiscover  = isHovered(mouseX, mouseY, x + 10, y + 74, SIDEBAR_W - 20, 26);
        Render2DUtil.drawRoundedRect(ctx.getMatrices(), x + 10, y + 74, SIDEBAR_W - 20, 26,
                6f, new Color(255, 255, 255, activeDiscover ? 25 : (hoverDiscover ? 18 : 8)));
        // 激活指示条
        if (activeDiscover) {
            Render2DUtil.drawRect(ctx.getMatrices(), x + 10, y + 80, 3, 14,
                    new Color(252, 60, 60).getRGB());
        }
        FontManager.get(10f).drawString(ctx.getMatrices(), "发现音乐",
                x + 22, y + 82, activeDiscover ? 0xFFFFFFFF : 0xFFCCCCCC);

        // 登录后才显示「我的音乐」分组
        if (loginManager.isLoggedIn()) {
            FontManager.get(9f).drawString(ctx.getMatrices(), "我的音乐", x + 15, y + 118, 0xFF777777);

            // 「我喜欢的音乐」菜单项
            boolean activeMyLiked = activeSidebarItem == SidebarItem.MY_LIKED;
            boolean hoverMyLiked  = isHovered(mouseX, mouseY, x + 10, y + 134, SIDEBAR_W - 20, 26);
            Render2DUtil.drawRoundedRect(ctx.getMatrices(), x + 10, y + 134, SIDEBAR_W - 20, 26,
                    6f, new Color(255, 255, 255, activeMyLiked ? 25 : (hoverMyLiked ? 18 : 8)));
            if (activeMyLiked) {
                Render2DUtil.drawRect(ctx.getMatrices(), x + 10, y + 140, 3, 14,
                        new Color(252, 60, 60).getRGB());
            }
            // 红色爱心图标
            FontManager.get(10f).drawString(ctx.getMatrices(), ICON_HEART,
                    x + 22, y + 142, new Color(252, 60, 60).getRGB());
            FontManager.get(10f).drawString(ctx.getMatrices(), "我喜欢的音乐",
                    x + 34, y + 142, activeMyLiked ? 0xFFFFFFFF : 0xFFCCCCCC);
        }
    }

    private void renderTopBar(DrawContext ctx, int mouseX, int mouseY, int x, int y, int w) {
        int searchX = x + 20;
        int searchY = y + 13;
        int searchW = 160;

        // 自绘输入框
        drawSearchField(ctx, mouseX, mouseY, searchX, searchY, searchW);

        // 搜索图标
        int searchBtnX = searchX + searchW + 8;
        boolean hoverSong = isHovered(mouseX, mouseY, searchBtnX, searchY, 20, 20);
        FontManager.getIconFont(14f).drawString(ctx.getMatrices(), ICON_SONG,
                searchBtnX, searchY + 3, hoverSong ? 0xFFFFFFFF : 0xFFAAAAAA);

        // 登录区域
        int loginX = x + w - 100;
        if (loginManager.isLoggedIn()) {
            Render2DUtil.drawCircle(ctx.getMatrices(), loginX + 10, searchY + 7, 9f,
                    new Color(252, 60, 60), 32);
            FontManager.get(10f).drawString(ctx.getMatrices(), "已登录",
                    loginX + 26, searchY + 4, 0xFFDDDDDD);
            boolean hoverOut = isHovered(mouseX, mouseY, x + w - 30, searchY, 20, 20);
            FontManager.get(12f).drawString(ctx.getMatrices(), "✕",
                    x + w - 25, searchY + 3, hoverOut ? 0xFFFF5555 : 0xFFAAAAAA);
        } else {
            boolean hoverLogin = isHovered(mouseX, mouseY, loginX, searchY, 80, 20);
            FontManager.get(10f).drawString(ctx.getMatrices(), "点击登录",
                    loginX + 25, searchY + 4, hoverLogin ? 0xFFFFFFFF : 0xFFAAAAAA);
            Render2DUtil.drawCircle(ctx.getMatrices(), loginX + 10, searchY + 7, 9f,
                    new Color(80, 80, 80), 32);
        }
    }

    private void drawSearchField(DrawContext ctx, int mouseX, int mouseY, int x, int y, int w) {
        Render2DUtil.drawRoundedRect(ctx.getMatrices(), x, y, w, 20, 4f,
                new Color(0, 0, 0, searchFocused ? 120 : 80));
        Render2DUtil.drawRoundedStroke(ctx.getMatrices(), x, y, w, 20, 4f,
                new Color(255, 255, 255, searchFocused ? 60 : 25), 32);

        FontRenderer font = FontManager.get(10f);

        if (searchText.isEmpty()) {
            font.drawString(ctx.getMatrices(), "Search...", x + 8, y + 6, 0xFF666666);
        } else {
            // 截断到可视宽度
            String display = searchText;
            while (display.length() > 0 && font.getWidth(display) > w - 16) {
                display = display.substring(1);
            }
            font.drawString(ctx.getMatrices(), display, x + 8, y + 6, 0xFFDDDDDD);

            // 光标
            if (searchFocused) {
                boolean cursorVisible = ((System.currentTimeMillis() - cursorBlinkStart) / CURSOR_BLINK_MS) % 2 == 0;
                if (cursorVisible) {
                    String beforeCursor = searchText.substring(0, searchCursorPos);
                    while (beforeCursor.length() > 0 && font.getWidth(beforeCursor) > w - 16) {
                        beforeCursor = beforeCursor.substring(1);
                    }
                    int cursorX = x + 8 + (int) font.getWidth(beforeCursor);
                    Render2DUtil.drawRect(ctx.getMatrices(), cursorX, y + 4, 1, 12,
                            new Color(255, 255, 255, 200).getRGB());
                }
            }
        }

        // 无文字时也显示光标
        if (searchFocused && searchText.isEmpty()) {
            boolean cursorVisible = ((System.currentTimeMillis() - cursorBlinkStart) / CURSOR_BLINK_MS) % 2 == 0;
            if (cursorVisible) {
                Render2DUtil.drawRect(ctx.getMatrices(), x + 8, y + 4, 1, 12,
                        new Color(255, 255, 255, 200).getRGB());
            }
        }
    }

    private void renderMainContent(DrawContext ctx, int mouseX, int mouseY,
                                   int panelX, int panelY, int panelW, int panelH) {
        int x = panelX + SIDEBAR_W;
        int y = panelY + TOP_H;
        int w = panelW - SIDEBAR_W;

        // 标题
        String title;
        if (currentMode == SearchMode.MY_LIKED) {
            title = "我喜欢的音乐";
        } else if (currentMode == SearchMode.USER_LIKED && selectedUser != null) {
            title = selectedUser.nickname() + " 的红心歌单";
        } else {
            title = searchText.isEmpty() ? "推荐内容" : searchText;
        }
        FontManager.get(14f).drawString(ctx.getMatrices(), title, x + 20, y + 5, 0xFFFFFFFF);

        // 历史记录 Chips（仅在发现/搜索模式下无文字时显示）
        if (currentMode == SearchMode.SONGS && searchText.isEmpty()) {
            drawHistoryChips(ctx, mouseX, mouseY, x + 130, y + 5, w - 150);
        }

        // Tabs（MY_LIKED 模式下不显示 Tab，USER_LIKED 显示返回）
        int tabY = y + 32;
        if (currentMode == SearchMode.USER_LIKED) {
            boolean hoverBack = isHovered(mouseX, mouseY, x + 20, tabY - 5, 40, 20);
            FontManager.get(10f).drawString(ctx.getMatrices(), "< 返回",
                    x + 16, tabY, hoverBack ? 0xFFFFFFFF : 0xFF999999);
        } else if (currentMode != SearchMode.MY_LIKED) {
            boolean hoverTabSong = isHovered(mouseX, mouseY, x + 20, tabY - 5, 30, 20);
            boolean hoverTabUser = isHovered(mouseX, mouseY, x + 60, tabY - 5, 30, 20);

            FontManager.get(11f).drawString(ctx.getMatrices(), "单曲", x + 20, tabY,
                    hoverTabSong || currentMode == SearchMode.SONGS ? 0xFFFFFFFF : 0xFF999999);
            if (currentMode == SearchMode.SONGS)
                Render2DUtil.drawRect(ctx.getMatrices(), x + 23, tabY + 14, 18, 2,
                        new Color(252, 60, 60).getRGB());

            FontManager.get(11f).drawString(ctx.getMatrices(), "用户", x + 60, tabY,
                    hoverTabUser || currentMode == SearchMode.USERS ? 0xFFFFFFFF : 0xFF999999);
            if (currentMode == SearchMode.USERS)
                Render2DUtil.drawRect(ctx.getMatrices(), x + 63, tabY + 14, 18, 2,
                        new Color(252, 60, 60).getRGB());
        }

        // 表头
        int headerY = y + 55;
        FontManager.get(9f).drawString(ctx.getMatrices(), "#",  x + 20, headerY, 0xFF777777);
        FontManager.get(9f).drawString(ctx.getMatrices(),
                currentMode == SearchMode.USERS ? "用户" : "标题", x + 85, headerY, 0xFF777777);
        if (currentMode != SearchMode.USERS) {
            FontManager.get(9f).drawString(ctx.getMatrices(), "专辑", x + w / 2 + 10, headerY, 0xFF777777);
            FontManager.get(9f).drawString(ctx.getMatrices(), "时长", x + w - 60,      headerY, 0xFF777777);
        }

        int listY = headerY + 18;
        int listH = (panelY + panelH - BOTTOM_H) - listY - 2;

        if (searching) {
            FontManager.get(10f).drawString(ctx.getMatrices(), "加载中...", x + 20, listY + 15, 0xFFAAAAAA);
        } else if (currentMode == SearchMode.USERS) {
            drawUserTable(ctx, mouseX, mouseY, x + 15, listY, w - 30, listH);
        } else {
            drawSongTable(ctx, mouseX, mouseY, x + 15, listY, w - 30, listH);
        }
    }

    private void drawHistoryChips(DrawContext ctx, int mouseX, int mouseY, int x, int y, int w) {
        if (cfg.searchHistory == null || cfg.searchHistory.isEmpty()) return;
        int cx = x;
        FontRenderer font = FontManager.get(9.0f);
        for (int i = 0; i < Math.min(5, cfg.searchHistory.size()); i++) {
            String q = cfg.searchHistory.get(i);
            if (q == null || q.isBlank()) continue;
            int chipW = (int) font.getWidth(q) + 16;
            if (cx + chipW > x + w) break;
            boolean hov = isHovered(mouseX, mouseY, cx, y, chipW, 16);
            Render2DUtil.drawRoundedRect(ctx.getMatrices(), cx, y, chipW, 16, 8f,
                    new Color(255, 255, 255, hov ? 35 : 18));
            font.drawString(ctx.getMatrices(), q, cx + 8, y + 4, hov ? 0xFFFFFFFF : 0xFFCCCCCC);
            cx += chipW + 6;
        }
    }

    private void drawSongTable(DrawContext ctx, int mouseX, int mouseY, int x, int y, int w, int h) {
        if (results == null || results.isEmpty()) return;
        ctx.enableScissor(x, y, x + w, y + h);
        int start      = Math.max(0, scroll / ROW_H);
        int maxVisible = Math.max(1, h / ROW_H + 2);
        int yy         = y - (scroll % ROW_H);
        for (int i = start; i < Math.min(results.size(), start + maxVisible); i++) {
            NeteaseSong s       = results.get(i);
            boolean     hovered = isHovered(mouseX, mouseY, x, yy, w, ROW_H);
            if (hovered) Render2DUtil.drawRoundedRect(ctx.getMatrices(), x, yy, w, ROW_H, 4f, new Color(255, 255, 255, 20));
            else if (i % 2 == 1) Render2DUtil.drawRoundedRect(ctx.getMatrices(), x, yy, w, ROW_H, 4f, new Color(255, 255, 255, 5));

            FontManager.get(9f).drawString(ctx.getMatrices(), String.format("%02d", i + 1), x + 5, yy + 14, 0xFF666666);

            Identifier tex = service.getAlbumTexFor(s);
            if (tex != null) ctx.drawTexture(tex, x + 30, yy + 6, 0, 0, 26, 26, 26, 26);
            else Render2DUtil.drawRoundedRect(ctx.getMatrices(), x + 30, yy + 6, 26, 26, 4f, new Color(80, 80, 80, 100));

            String title = s.name().length() > 20 ? s.name().substring(0, 19) + "..." : s.name();
            FontManager.get(10f).drawString(ctx.getMatrices(), title,     x + 65, yy + 8,  0xFFDDDDDD);
            FontManager.get(8f) .drawString(ctx.getMatrices(), s.artists(), x + 65, yy + 22, 0xFF777777);
            FontManager.get(9f) .drawString(ctx.getMatrices(), "单曲",      x + w / 2 - 5, yy + 14, 0xFF888888);

            String time = String.format("%02d:%02d", (s.durationMs() / 1000) / 60, (s.durationMs() / 1000) % 60);
            FontManager.get(9f).drawString(ctx.getMatrices(), time, x + w - 45, yy + 14, 0xFF777777);
            yy += ROW_H;
        }
        ctx.disableScissor();
    }

    private void drawUserTable(DrawContext ctx, int mouseX, int mouseY, int x, int y, int w, int h) {
        if (userResults == null || userResults.isEmpty()) return;
        ctx.enableScissor(x, y, x + w, y + h);
        int start      = Math.max(0, scroll / ROW_H);
        int maxVisible = Math.max(1, h / ROW_H + 2);
        int yy         = y - (scroll % ROW_H);
        for (int i = start; i < Math.min(userResults.size(), start + maxVisible); i++) {
            NeteaseUser u       = userResults.get(i);
            boolean     hovered = isHovered(mouseX, mouseY, x, yy, w, ROW_H);
            if (hovered) Render2DUtil.drawRoundedRect(ctx.getMatrices(), x, yy, w, ROW_H, 4f, new Color(255, 255, 255, 20));
            else if (i % 2 == 1) Render2DUtil.drawRoundedRect(ctx.getMatrices(), x, yy, w, ROW_H, 4f, new Color(255, 255, 255, 5));

            FontManager.get(9f).drawString(ctx.getMatrices(), String.format("%02d", i + 1), x + 5, yy + 14, 0xFF666666);
            Render2DUtil.drawCircle(ctx.getMatrices(), x + 43, yy + 19, 13f, new Color(80, 80, 80), 32);
            FontManager.get(10f).drawString(ctx.getMatrices(), u.nickname(), x + 65, yy + 8, 0xFFDDDDDD);
            String sig = u.signature() == null || u.signature().isBlank() ? "UID: " + u.userId() : u.signature();
            if (sig.length() > 25) sig = sig.substring(0, 24) + "...";
            FontManager.get(8f).drawString(ctx.getMatrices(), sig, x + 65, yy + 22, 0xFF777777);
            yy += ROW_H;
        }
        ctx.disableScissor();
    }

    private void renderBottomControlBar(DrawContext ctx, int mouseX, int mouseY,
                                        int panelX, int panelY, int panelW, int panelH) {
        int x       = panelX;
        int w       = panelW;
        int bottomY = panelY + panelH - BOTTOM_H;

        ctx.enableScissor(x, bottomY, x + w, panelY + panelH);
        Render2DUtil.drawRoundedRect(ctx.getMatrices(), x, bottomY - BOTTOM_H, w, BOTTOM_H * 2,
                8f, new Color(20, 20, 23, 60));
        ctx.disableScissor();
        Render2DUtil.drawRect(ctx.getMatrices(), x, bottomY, w, 1, new Color(255, 255, 255, 12).getRGB());

        // 进度条
        int barX = x, barY = bottomY, barW = w;
        boolean hoverProgress = isHovered(mouseX, mouseY, barX, barY - 4, barW, 10) || draggingProgress;
        int barHeight = hoverProgress ? 4 : 2;
        Render2DUtil.drawRect(ctx.getMatrices(), barX, barY, barW, barHeight,
                new Color(255, 255, 255, 20).getRGB());

        NeteaseSong cur = service.getCurrentSong();
        if (cur != null && cur.durationMs() > 0) {
            long pos = draggingProgress ? dragSeekMs : service.getSongPositionMs();
            double p = Math.max(0.0, Math.min(1.0, pos / (double) cur.durationMs()));
            int fillW = (int) Math.round(barW * p);
            Render2DUtil.drawRect(ctx.getMatrices(), barX, barY, fillW, barHeight,
                    new Color(252, 60, 60, 255).getRGB());
            if (hoverProgress) {
                Render2DUtil.drawCircle(ctx.getMatrices(), barX + fillW, barY + barHeight / 2f, 5f, Color.WHITE, 32);
                if (draggingProgress)
                    dragSeekMs = (long)(Math.max(0.0, Math.min(1.0, (mouseX - barX) / (double) barW)) * cur.durationMs());
            }
        }

        // 左侧歌曲信息
        if (cur != null) {
            Identifier tex = service.getAlbumTexFor(cur);
            if (tex != null) ctx.drawTexture(tex, x + 15, bottomY + 10, 0, 0, 40, 40, 40, 40);
            else Render2DUtil.drawRoundedRect(ctx.getMatrices(), x + 15, bottomY + 10, 40, 40,
                    4f, new Color(80, 80, 80, 100));
            FontManager.get(10f).drawString(ctx.getMatrices(), cur.name(),    x + 65, bottomY + 16, 0xFFFFFFFF);
            FontManager.get(8f) .drawString(ctx.getMatrices(), cur.artists(), x + 65, bottomY + 32, 0xFF888888);
        }

        // 中间控制组（精确居中）
        int centerX = x + w / 2;
        int centerY = bottomY + BOTTOM_H / 2;

        boolean isLoopOne = service.getPlayMode() == MusicPlayerService.PlayMode.LOOP_ONE;
        boolean hoverMode = isHovered(mouseX, mouseY, centerX - 80, centerY - 15, 30, 30);
        {
            FontRenderer f    = FontManager.getIconFont(14f);
            String       icon = isLoopOne ? ICON_LOOP_ONE : ICON_LOOP_QUEUE;
            f.drawString(ctx.getMatrices(), icon,
                    (centerX - 65) - f.getWidth(icon) / 2f,
                    centerY - f.getFontHeight(icon) / 2f,
                    hoverMode ? 0xFFFFFFFF : 0xFFAAAAAA);
        }

        boolean hoverPrev = isHovered(mouseX, mouseY, centerX - 42, centerY - 15, 30, 30);
        {
            FontRenderer f = FontManager.get(16f);
            f.drawString(ctx.getMatrices(), ICON_PREV,
                    (centerX - 27) - f.getWidth(ICON_PREV) / 2f,
                    centerY - f.getFontHeight(ICON_PREV) / 2f,
                    hoverPrev ? 0xFFFFFFFF : 0xFFCCCCCC);
        }

        boolean isPlaying = service.getPlaybackState() == MusicPlayerService.PlaybackState.PLAYING && !service.isPaused();
        boolean hoverPlay = isHovered(mouseX, mouseY, centerX - 16, centerY - 16, 32, 32);
        Render2DUtil.drawCircle(ctx.getMatrices(), centerX, centerY, 16f,
                new Color(252, 60, 60, hoverPlay ? 255 : 220), 32);
        {
            FontRenderer f    = FontManager.getIconFont(18f);
            String       icon = isPlaying ? ICON_PAUSE : ICON_PLAY;
            f.drawString(ctx.getMatrices(), icon,
                    centerX - f.getWidth(icon) / 2f,
                    centerY - f.getFontHeight(icon) / 2f,
                    0xFFFFFFFF);
        }

        boolean hoverNext = isHovered(mouseX, mouseY, centerX + 22, centerY - 15, 30, 30);
        {
            FontRenderer f = FontManager.get(16f);
            f.drawString(ctx.getMatrices(), ICON_NEXT,
                    (centerX + 37) - f.getWidth(ICON_NEXT) / 2f,
                    centerY - f.getFontHeight(ICON_NEXT) / 2f,
                    hoverNext ? 0xFFFFFFFF : 0xFFCCCCCC);
        }

        boolean hoverLyric = isHovered(mouseX, mouseY, centerX + 60, centerY - 15, 30, 30);
        {
            FontRenderer f = FontManager.getIconFont(14f);
            f.drawString(ctx.getMatrices(), ICON_LYRIC,
                    (centerX + 75) - f.getWidth(ICON_LYRIC) / 2f,
                    centerY - f.getFontHeight(ICON_LYRIC) / 2f,
                    cfg.desktopLyrics ? 0xFFFC3C3C : (hoverLyric ? 0xFFFFFFFF : 0xFFAAAAAA));
        }

        // 右侧音量
        int rightX = x + w - 110;
        {
            FontRenderer f = FontManager.getIconFont(14f);
            f.drawString(ctx.getMatrices(), ICON_VOL, rightX,
                    centerY - f.getFontHeight(ICON_VOL) / 2f, 0xFFAAAAAA);
        }

        int volBarX = rightX + 22, volBarY = centerY - 1, volBarW = 60, volBarH = 3;
        boolean hoverVol = isHovered(mouseX, mouseY, volBarX - 5, volBarY - 5, volBarW + 10, 14) || draggingVolume;
        Render2DUtil.drawRoundedRect(ctx.getMatrices(), volBarX, volBarY, volBarW, volBarH,
                1.5f, new Color(255, 255, 255, 30));
        if (draggingVolume) {
            cfg.volume01 = Math.max(0f, Math.min(1f, (mouseX - volBarX) / (float) volBarW));
            service.setVolume01(cfg.volume01);
        }
        int fillVolW = (int)(volBarW * cfg.volume01);
        Render2DUtil.drawRoundedRect(ctx.getMatrices(), volBarX, volBarY, fillVolW, volBarH,
                1.5f, new Color(255, 255, 255, 200));
        if (hoverVol)
            Render2DUtil.drawCircle(ctx.getMatrices(), volBarX + fillVolW, volBarY + 1.5f, 4f, Color.WHITE, 32);
    }

    private void drawLoginDialog(DrawContext ctx, int mouseX, int mouseY,
                                 int panelX, int panelY, int panelW, int panelH) {
        int dialogW = 200, dialogH = 240;
        int dialogX = panelX + (panelW - dialogW) / 2;
        int dialogY = panelY + (panelH - dialogH) / 2;

        Render2DUtil.drawRoundedRect(ctx.getMatrices(), dialogX, dialogY, dialogW, dialogH,
                8f, new Color(25, 25, 28, 240));
        Render2DUtil.drawRoundedStroke(ctx.getMatrices(), dialogX, dialogY, dialogW, dialogH,
                8f, new Color(255, 255, 255, 40), 48);

        FontManager.get(11f).drawString(ctx.getMatrices(), "使用网易云App扫码登录",
                dialogX + 35, dialogY + 15, 0xFFFFFFFF);

        if (qrTextureId != null)
            ctx.drawTexture(qrTextureId, dialogX + 25, dialogY + 40, 0, 0, 150, 150, 150, 150);
        else
            Render2DUtil.drawRoundedRect(ctx.getMatrices(), dialogX + 25, dialogY + 40, 150, 150,
                    4f, new Color(60, 60, 60, 200));

        FontManager.get(9f).drawCenteredString(ctx.getMatrices(), loginStatusMessage,
                dialogX + dialogW / 2, dialogY + 195, loginStatusColor);

        boolean hoverRefresh = isHovered(mouseX, mouseY, dialogX + 50, dialogY + dialogH - 30, 100, 20);
        Render2DUtil.drawRoundedRect(ctx.getMatrices(), dialogX + 50, dialogY + dialogH - 30, 100, 20,
                4f, new Color(255, 255, 255, hoverRefresh ? 30 : 15));
        FontManager.get(9f).drawCenteredString(ctx.getMatrices(), "↻ 重新获取",
                dialogX + dialogW / 2, dialogY + dialogH - 25,
                hoverRefresh ? 0xFFFFFFFF : 0xFFAAAAAA);
    }

    // ==================== 输入事件 ====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int panelW = Math.min(680, this.width - 40);
        int panelH = Math.min(420, this.height - 40);
        int panelX = (this.width  - panelW) / 2;
        int panelY = (this.height - panelH) / 2;
        int mainX  = panelX + SIDEBAR_W;
        int mainW  = panelW - SIDEBAR_W;

        int searchX = mainX + 20;
        int searchY = panelY + 13;
        int searchW = 160;

        // 登录弹窗
        if (showingLoginDialog) {
            int dialogW = 200, dialogH = 240;
            int dialogX = panelX + (panelW - dialogW) / 2;
            int dialogY = panelY + (panelH - dialogH) / 2;
            if (isHovered(mouseX, mouseY, dialogX + 50, dialogY + dialogH - 30, 100, 20)) {
                loginManager.stopPolling(); showLoginDialog(); return true;
            }
            if (!isHovered(mouseX, mouseY, dialogX, dialogY, dialogW, dialogH)) {
                showingLoginDialog = false; loginManager.stopPolling(); cleanupQrResources();
            }
            return true;
        }

        // 输入框点击
        if (isHovered(mouseX, mouseY, searchX, searchY, searchW, 20)) {
            searchFocused     = true;
            cursorBlinkStart  = System.currentTimeMillis();
            searchCursorPos   = searchText.length();
            return true;
        } else {
            searchFocused = false;
        }

        // 【侧边栏：发现音乐】
        if (isHovered(mouseX, mouseY, panelX + 10, panelY + 74, SIDEBAR_W - 20, 26)) {
            activeSidebarItem = SidebarItem.DISCOVER;
            currentMode       = SearchMode.SONGS;
            if (!searchText.isBlank()) doSearch();
            else { results = List.of(); userResults = List.of(); }
            return true;
        }

        // 【侧边栏：我喜欢的音乐（登录后显示）】
        if (loginManager.isLoggedIn() &&
                isHovered(mouseX, mouseY, panelX + 10, panelY + 134, SIDEBAR_W - 20, 26)) {
            loadMyLikedSongs();
            return true;
        }

        // 搜索图标
        int searchBtnX = searchX + searchW + 8;
        if (isHovered(mouseX, mouseY, searchBtnX, searchY, 20, 20)) {
            if (currentMode == SearchMode.USERS) doSearchUsers(); else doSearch(); return true;
        }

        // 登录/注销
        int loginX = mainX + mainW - 100;
        if (loginManager.isLoggedIn() && isHovered(mouseX, mouseY, mainX + mainW - 30, panelY + 16, 20, 20)) {
            logout(); return true;
        }
        if (!loginManager.isLoggedIn() && isHovered(mouseX, mouseY, loginX, panelY + 16, 80, 20)) {
            showLoginDialog(); return true;
        }

        // 历史记录 Chips
        if (currentMode == SearchMode.SONGS && searchText.isEmpty()) {
            int cx = mainX + 130, cy = panelY + TOP_H + 5;
            FontRenderer font = FontManager.get(9.0f);
            if (cfg.searchHistory != null) {
                for (int i = 0; i < Math.min(5, cfg.searchHistory.size()); i++) {
                    String q = cfg.searchHistory.get(i);
                    if (q == null || q.isBlank()) continue;
                    int chipW = (int) font.getWidth(q) + 16;
                    if (cx + chipW > mainX + mainW - 20) break;
                    if (isHovered(mouseX, mouseY, cx, cy, chipW, 16)) {
                        searchText      = q;
                        searchCursorPos = q.length();
                        doSearch();
                        return true;
                    }
                    cx += chipW + 6;
                }
            }
        }

        // Tab 切换
        int tabY = panelY + TOP_H + 32;
        if (currentMode == SearchMode.USER_LIKED) {
            if (isHovered(mouseX, mouseY, mainX + 16, tabY - 5, 40, 20)) {
                currentMode = SearchMode.USERS; return true;
            }
        } else if (currentMode != SearchMode.MY_LIKED) {
            if (isHovered(mouseX, mouseY, mainX + 20, tabY - 5, 30, 20)) {
                currentMode = SearchMode.SONGS; doSearch(); return true;
            }
            if (isHovered(mouseX, mouseY, mainX + 60, tabY - 5, 30, 20)) {
                currentMode = SearchMode.USERS; doSearchUsers(); return true;
            }
        }

        // 列表点击
        int listY = panelY + TOP_H + 73;
        int listH = (panelY + panelH - BOTTOM_H) - listY - 2;
        if (isHovered(mouseX, mouseY, mainX + 15, listY, mainW - 30, listH)) {
            int idx = (int)(mouseY - listY + scroll) / ROW_H;
            if (currentMode == SearchMode.USERS) {
                if (idx >= 0 && idx < userResults.size()) {
                    loadUserLikedSongs(userResults.get(idx)); return true;
                }
            } else {
                if (idx >= 0 && idx < results.size()) {
                    service.setQueue(results, idx); return true;
                }
            }
        }

        // 底部控制
        int bottomY = panelY + panelH - BOTTOM_H;
        int centerX = panelX + panelW / 2;
        int centerY = bottomY + BOTTOM_H / 2;
        int rightX  = panelX + panelW - 110;

        if (isHovered(mouseX, mouseY, panelX, bottomY - 4, panelW, 10)) {
            if (service.getCurrentSong() != null) draggingProgress = true; return true;
        }
        if (isHovered(mouseX, mouseY, rightX + 22, centerY - 5, 60, 14)) {
            draggingVolume = true; return true;
        }
        if (isHovered(mouseX, mouseY, centerX - 80, centerY - 15, 30, 30)) {
            service.setPlayMode(service.getPlayMode() == MusicPlayerService.PlayMode.LOOP_ONE
                    ? MusicPlayerService.PlayMode.QUEUE : MusicPlayerService.PlayMode.LOOP_ONE);
            return true;
        }
        if (isHovered(mouseX, mouseY, centerX - 42, centerY - 15, 30, 30)) { service.prev(); return true; }
        if (isHovered(mouseX, mouseY, centerX - 16, centerY - 16, 32, 32)) { togglePlayPause(); return true; }
        if (isHovered(mouseX, mouseY, centerX + 22, centerY - 15, 30, 30)) { service.next(); return true; }
        if (isHovered(mouseX, mouseY, centerX + 60, centerY - 15, 30, 30)) {
            cfg.desktopLyrics = !cfg.desktopLyrics; cfg.save(); return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchFocused) {
            switch (keyCode) {
                case 257, 335 -> {
                    searchFocused = false;
                    if (currentMode == SearchMode.USERS) doSearchUsers(); else doSearch();
                    return true;
                }
                case 256 -> {
                    if (!searchText.isEmpty()) { searchText = ""; searchCursorPos = 0; }
                    else searchFocused = false;
                    return true;
                }
                case 259 -> {
                    if (searchCursorPos > 0) {
                        searchText = searchText.substring(0, searchCursorPos - 1)
                                + searchText.substring(searchCursorPos);
                        searchCursorPos--;
                        cursorBlinkStart = System.currentTimeMillis();
                    }
                    return true;
                }
                case 261 -> {
                    if (searchCursorPos < searchText.length()) {
                        searchText = searchText.substring(0, searchCursorPos)
                                + searchText.substring(searchCursorPos + 1);
                        cursorBlinkStart = System.currentTimeMillis();
                    }
                    return true;
                }
                case 263 -> { if (searchCursorPos > 0) searchCursorPos--; cursorBlinkStart = System.currentTimeMillis(); return true; }
                case 262 -> { if (searchCursorPos < searchText.length()) searchCursorPos++; cursorBlinkStart = System.currentTimeMillis(); return true; }
                case 268 -> { searchCursorPos = 0; cursorBlinkStart = System.currentTimeMillis(); return true; }
                case 269 -> { searchCursorPos = searchText.length(); cursorBlinkStart = System.currentTimeMillis(); return true; }
            }
            // Ctrl+A
            if (keyCode == 65 && (modifiers & 2) != 0) { searchCursorPos = searchText.length(); return true; }
            return true;
        }

        if (keyCode == 256) {
            if (showingLoginDialog) {
                showingLoginDialog = false; loginManager.stopPolling(); cleanupQrResources(); return true;
            }
            this.close(); return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchFocused) {
            if (chr < 32 || chr == 127) return true;
            if (searchText.length() >= 60) return true;
            searchText = searchText.substring(0, searchCursorPos) + chr + searchText.substring(searchCursorPos);
            searchCursorPos++;
            cursorBlinkStart = System.currentTimeMillis();
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingProgress && service.getCurrentSong() != null) {
            service.seekTo(dragSeekMs); dragSeekMs = -1; draggingProgress = false; return true;
        }
        if (draggingVolume) { draggingVolume = false; cfg.save(); return true; }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        if (showingLoginDialog) return false;
        int listH       = Math.min(420, this.height - 40) - TOP_H - BOTTOM_H - 73;
        int contentSize = currentMode == SearchMode.USERS ? userResults.size() : results.size();
        scroll = Math.max(0, Math.min(scroll - (int)(v * ROW_H),
                Math.max(0, contentSize * ROW_H - listH)));
        cfg.lastScroll = scroll; cfg.save();
        return super.mouseScrolled(mouseX, mouseY, h, v);
    }

    @Override
    public void close() {
        super.close();
        if (loginManager != null) loginManager.stopPolling();
        cleanupQrResources();
    }

    @Override
    public boolean shouldPause() { return false; }

    private boolean isHovered(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}