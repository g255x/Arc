package dev.perish.mod.gui;

import dev.perish.Perish;
import dev.perish.api.utils.math.Animation;
import dev.perish.api.utils.math.Easing;
import dev.perish.api.utils.render.Render2DUtil;
import dev.perish.mod.gui.hyperion.HyperionMetrics;
import dev.perish.mod.gui.skid.ClickGuiTextUtil;
import dev.perish.mod.modules.impl.client.ClickGui;
import dev.perish.mod.modules.impl.misc.MusicPlayerModule;
import dev.perish.mod.utils.music.*;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;

public class MusicPlayerScreen extends Screen {

    private float panelX, panelY, panelWidth, panelHeight;
    private float sideBarW, bottomBarH;
    private float contentX, contentY, contentW, contentH;

    private int currentPage = 0;
    private final int PAGE_HOME = 0, PAGE_SEARCH = 1, PAGE_LIKE = 2, PAGE_LOGIN = 3;

    private final List<Song> newSongs = new ArrayList<>();
    private final List<Playlist> recommendPlaylists = new ArrayList<>();
    private final List<Song> searchResults = new ArrayList<>();
    private final List<Song> likeSongs = new ArrayList<>();

    private String searchText = "";
    private boolean searchFocused = false;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    private boolean homeLoaded = false;
    private boolean likeLoaded = false;

    private int loginTab = 0;
    private String phoneText = "";
    private String captchaText = "";
    private String loginMessage = "";
    private int captchaCountdown = 0;
    private long captchaStartTime = 0;

    private boolean phoneFocused = false;
    private boolean captchaFocused = false;

    private String qrKey = null;
    private String qrImageBase64 = null;
    private String qrStatusText = "等待获取";
    private boolean qrChecking = false;
    private long lastQrCheckTime = 0;
    private Identifier qrTextureId = null;
    private NativeImageBackedTexture qrDynamicTexture = null;

    private boolean showPlayerDetail = false;

    private final Animation openAnim = new Animation();
    private final Animation closeAnim = new Animation();
    private final Animation detailAnim = new Animation();
    private final Animation[] hoverAnims = new Animation[60];
    private final Animation[] clickAnims = new Animation[60];
    private final Animation playBtnAnim = new Animation();
    private final Animation playClickAnim = new Animation();
    private final Animation prevClickAnim = new Animation();
    private final Animation nextClickAnim = new Animation();
    private final Animation songClickAnim = new Animation();
    private final Animation pageSwitchAnim = new Animation();

    private int prevPage = 0;

    private boolean isClosing = false;
    private double openProgress = 0.0;
    private double closeProgress = 0.0;

    public MusicPlayerScreen() {
        super(Text.literal("MusicPlayer"));
        for (int i = 0; i < hoverAnims.length; i++) {
            hoverAnims[i] = new Animation();
            clickAnims[i] = new Animation();
        }
    }

    private float getOpacity() {
        return MusicPlayerModule.INSTANCE != null ? MusicPlayerModule.INSTANCE.opacity.getValueFloat() / 100f : 1.0f;
    }

    private static Color alphaColor(Color c, float alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, Math.round(c.getAlpha() * alpha))));
    }

    private static Color blend(Color a, Color b, float t) {
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    private boolean isHovered(int mx, int my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private String formatPlayCount(long count) {
        if (count >= 100000000) return String.format("%.1f亿", count / 100000000.0);
        if (count >= 10000) return String.format("%.1f万", count / 10000.0);
        return String.valueOf(count);
    }

    private float getClickScale(int idx) {
        float v = (float) clickAnims[Math.min(idx, clickAnims.length - 1)].get(0.0, 200L, Easing.CubicOut);
        return 1.0f - 0.06f * v;
    }

    private void triggerClickAnim(int idx) {
        Animation anim = clickAnims[Math.min(idx, clickAnims.length - 1)];
        anim.from = 0.0;
        anim.to = 0.0;
        anim.get(1.0, 200L, Easing.CubicOut);
    }

    @Override
    protected void init() {
        super.init();
        computeLayout();
        isClosing = false;
        openProgress = 0;
        closeProgress = 0;
        if (!homeLoaded) loadHomeData();
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        computeLayout();
    }

    private void computeLayout() {
        MinecraftClient mc = MinecraftClient.getInstance();
        float screenWidth = mc.getWindow().getScaledWidth();
        float screenHeight = mc.getWindow().getScaledHeight();

        panelWidth = 460;
        panelHeight = 340;
        panelX = (screenWidth - panelWidth) / 2f;
        panelY = (screenHeight - panelHeight) / 2f;

        sideBarW = HyperionMetrics.CATEGORY_WIDTH;
        bottomBarH = 62;

        float pad = HyperionMetrics.PADDING;
        float gap = HyperionMetrics.GAP;

        contentX = panelX + pad + sideBarW + gap;
        contentY = panelY + pad;
        contentW = panelWidth - pad * 3 - sideBarW - gap;
        contentH = panelHeight - pad * 2 - bottomBarH - gap;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        if (isClosing) {
            int closeDur = ClickGui.INSTANCE.getHyperionAnimDuration(200);
            closeProgress = closeAnim.get(0.0, closeDur, Easing.CubicIn);
            if (closeProgress <= 0.01) {
                super.close();
                return;
            }
        } else {
            closeProgress = 0;
        }

        int openDur = ClickGui.INSTANCE.getHyperionAnimDuration(420);
        openProgress = openAnim.get(1.0, openDur, Easing.BackOut);

        double progress = isClosing ? closeProgress : openProgress;
        if (progress <= 0) return;

        float scale = (float) (0.94 + 0.06 * progress);
        float alpha = (float) (progress * getOpacity());

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        float centerX = panelX + panelWidth / 2;
        float centerY = panelY + panelHeight / 2;
        matrices.translate(centerX, centerY, 0);
        matrices.scale(scale, scale, 1);
        matrices.translate(-centerX, -centerY, 0);

        drawShadow(context, alpha);

        if (showPlayerDetail) {
            drawPlayerDetail(context, matrices, mouseX, mouseY, alpha);
        } else {
            drawMainFrame(context, matrices, mouseX, mouseY, alpha);
        }

        matrices.pop();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);

        if (ClickGui.INSTANCE.hyperionBackgroundBlur.getValue()) {
            try {
                float radius = (float) ClickGui.INSTANCE.hyperionBlurRadius.getValue();
                float sw = MinecraftClient.getInstance().getWindow().getScaledWidth();
                float sh = MinecraftClient.getInstance().getWindow().getScaledHeight();
                Perish.BLUR.applyBlur(radius, 0, 0, sw, sh);
            } catch (Exception ignored) {
            }
        }

        int overlayAlpha = ClickGui.INSTANCE.hyperionBackgroundBlur.getValue() ? 45 : 80;
        Render2DUtil.rectFilled(context.getMatrices(), 0, 0,
                MinecraftClient.getInstance().getWindow().getScaledWidth(),
                MinecraftClient.getInstance().getWindow().getScaledHeight(),
                new Color(0, 0, 0, overlayAlpha).getRGB());
    }

    private void drawShadow(DrawContext context, float alpha) {
        for (int i = 6; i >= 1; i--) {
            float offset = i * 2.5f;
            int shadowAlpha = (int) (16 * alpha * (7 - i) / 6);
            Render2DUtil.drawSmoothRoundedRect(context.getMatrices(),
                    panelX - offset, panelY - offset,
                    panelWidth + offset * 2, panelHeight + offset * 2,
                    HyperionMetrics.PANEL_RADIUS + offset,
                    new Color(0, 0, 0, shadowAlpha));
        }
    }

    private void drawMainFrame(DrawContext context, MatrixStack matrices, int mouseX, int mouseY, float alpha) {
        Color panelBg = alphaColor(ClickGui.INSTANCE.getHyperionPanelBgWithAlpha(), alpha);
        Render2DUtil.drawSmoothRoundedRect(matrices, panelX, panelY, panelWidth, panelHeight, HyperionMetrics.PANEL_RADIUS, panelBg);

        Color borderColor = alphaColor(ClickGui.INSTANCE.hyperionBorderColor.getValue(), alpha);
        Render2DUtil.drawSmoothRoundedStroke(matrices, panelX, panelY, panelWidth, panelHeight, HyperionMetrics.PANEL_RADIUS, borderColor);

        drawAccentLine(matrices, alpha);
        drawSideBar(context, matrices, mouseX, mouseY, alpha);

        context.enableScissor((int) contentX, (int) contentY, (int) (contentX + contentW), (int) (contentY + contentH));
        float pageOffset = (float) pageSwitchAnim.get(1.0, ClickGui.INSTANCE.getHyperionAnimDuration(250), Easing.CubicOut);
        matrices.push();
        matrices.translate(0, (1 - pageOffset) * 8, 0);
        switch (currentPage) {
            case 0 -> drawHomePage(context, matrices, contentX + HyperionMetrics.PADDING, contentY + HyperionMetrics.PADDING, contentW - HyperionMetrics.PADDING * 2, contentH - HyperionMetrics.PADDING * 2, mouseX, mouseY, alpha);
            case 1 -> drawSearchPage(context, matrices, contentX + HyperionMetrics.PADDING, contentY + HyperionMetrics.PADDING, contentW - HyperionMetrics.PADDING * 2, contentH - HyperionMetrics.PADDING * 2, mouseX, mouseY, alpha);
            case 2 -> drawLikePage(context, matrices, contentX + HyperionMetrics.PADDING, contentY + HyperionMetrics.PADDING, contentW - HyperionMetrics.PADDING * 2, contentH - HyperionMetrics.PADDING * 2, mouseX, mouseY, alpha);
            case 3 -> drawLoginPage(context, matrices, contentX + HyperionMetrics.PADDING, contentY + HyperionMetrics.PADDING, contentW - HyperionMetrics.PADDING * 2, contentH - HyperionMetrics.PADDING * 2, mouseX, mouseY, alpha);
        }
        matrices.pop();
        context.disableScissor();

        drawBottomPlayerBar(context, matrices, panelX + HyperionMetrics.PADDING, panelY + panelHeight - bottomBarH + HyperionMetrics.PADDING,
                panelWidth - HyperionMetrics.PADDING * 2, bottomBarH - HyperionMetrics.PADDING, mouseX, mouseY, alpha);
    }

    private void drawAccentLine(MatrixStack matrices, float alpha) {
        Color accent = ClickGui.INSTANCE.hyperionAccentColor.getValue();
        float y = panelY + HyperionMetrics.PADDING + 26;
        Render2DUtil.drawSmoothRoundedRect(matrices,
                panelX + HyperionMetrics.PADDING, y,
                panelWidth - HyperionMetrics.PADDING * 2, 1.0f, 0.5f,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (150 * alpha)));
    }

    private void drawSideBar(DrawContext context, MatrixStack matrices, int mouseX, int mouseY, float alpha) {
        float x = panelX + HyperionMetrics.PADDING;
        float y = panelY + HyperionMetrics.PADDING;
        float w = sideBarW;
        float h = panelHeight - HyperionMetrics.PADDING * 2 - bottomBarH;

        Color sectionBg = alphaColor(ClickGui.INSTANCE.getHyperionSectionBgWithAlpha(), alpha);
        Render2DUtil.drawSmoothRoundedRect(matrices, x, y, w, h, HyperionMetrics.SECTION_RADIUS, sectionBg);

        Color primary = alphaColor(ClickGui.INSTANCE.hyperionTextPrimary.getValue(), alpha);
        Color secondary = alphaColor(ClickGui.INSTANCE.hyperionTextSecondary.getValue(), alpha);
        Color accent = alphaColor(ClickGui.INSTANCE.hyperionAccentColor.getValue(), alpha);

        float textH = ClickGuiTextUtil.getHeight();
        float titleY = y + HyperionMetrics.PADDING + 4;
        String title = "Music";
        float titleW = ClickGuiTextUtil.getWidth(title);
        ClickGuiTextUtil.drawStringWithShadow(context, title, x + w / 2f - titleW / 2f, titleY, accent.getRGB());

        float ny = titleY + textH + HyperionMetrics.PADDING + 4;
        if (NeteaseCloudApi.INSTANCE.isLoggedIn && NeteaseCloudApi.INSTANCE.userProfile != null) {
            String nick = NeteaseCloudApi.INSTANCE.userProfile.nickname;
            float nw = ClickGuiTextUtil.getWidth(nick);
            ClickGuiTextUtil.drawStringWithShadow(context, nick, x + w / 2f - nw / 2f, ny, secondary.getRGB());
            ny += textH + HyperionMetrics.PADDING;
        }

        String[] navLabels = {"首页", "搜索", "喜欢"};
        int[] navPages = {0, 1, 2};
        float btnH = HyperionMetrics.ITEM_HEIGHT;
        for (int i = 0; i < navLabels.length; i++) {
            float btnX = x + HyperionMetrics.PADDING;
            float btnY = ny + i * (btnH + 3);
            float btnW = w - HyperionMetrics.PADDING * 2;
            boolean selected = currentPage == navPages[i];
            boolean hovered = isHovered(mouseX, mouseY, btnX, btnY, btnW, btnH);

            float hoverVal = (float) hoverAnims[i].get(hovered ? 1.0 : 0.0, ClickGui.INSTANCE.getHyperionAnimDuration(150), Easing.CubicOut);
            float clickScale = getClickScale(i);

            matrices.push();
            matrices.translate(btnX + btnW / 2f, btnY + btnH / 2f, 0);
            matrices.scale(clickScale, clickScale, 1);
            matrices.translate(-(btnX + btnW / 2f), -(btnY + btnH / 2f), 0);

            if (selected) {
                Color selectedBg = alphaColor(ClickGui.INSTANCE.getHyperionItemBgSelected(), alpha);
                Render2DUtil.drawSmoothRoundedRect(matrices, btnX, btnY, btnW, btnH, HyperionMetrics.COMPONENT_RADIUS, selectedBg);
                Render2DUtil.drawSmoothRoundedRect(matrices, btnX, btnY, 3, btnH, 1.5f, accent);
            } else if (hoverVal > 0.01f) {
                Color hoverBg = alphaColor(ClickGui.INSTANCE.getHyperionItemBgHover(), alpha);
                Color baseBg = alphaColor(ClickGui.INSTANCE.getHyperionItemBg(), alpha);
                Render2DUtil.drawSmoothRoundedRect(matrices, btnX, btnY, btnW, btnH, HyperionMetrics.COMPONENT_RADIUS, blend(baseBg, hoverBg, hoverVal));
            }

            Color textColor = selected ? primary : secondary;
            float tw = ClickGuiTextUtil.getWidth(navLabels[i]);
            ClickGuiTextUtil.drawStringWithShadow(context, navLabels[i], btnX + btnW / 2f - tw / 2f + (selected ? 2 : 0), btnY + btnH / 2f - textH / 2f, textColor.getRGB());

            matrices.pop();
        }

        float bottomY = y + h - HyperionMetrics.PADDING - btnH;
        String bottomLabel = NeteaseCloudApi.INSTANCE.isLoggedIn ? "退出" : "登录";
        float bottomBtnX = x + HyperionMetrics.PADDING;
        float bottomBtnW = w - HyperionMetrics.PADDING * 2;
        boolean bottomHovered = isHovered(mouseX, mouseY, bottomBtnX, bottomY, bottomBtnW, btnH);
        float bhVal = (float) hoverAnims[3].get(bottomHovered ? 1.0 : 0.0, ClickGui.INSTANCE.getHyperionAnimDuration(150), Easing.CubicOut);
        float bClickScale = getClickScale(3);

        matrices.push();
        matrices.translate(bottomBtnX + bottomBtnW / 2f, bottomY + btnH / 2f, 0);
        matrices.scale(bClickScale, bClickScale, 1);
        matrices.translate(-(bottomBtnX + bottomBtnW / 2f), -(bottomY + btnH / 2f), 0);

        if (bhVal > 0.01f) {
            Color hoverBg = alphaColor(ClickGui.INSTANCE.getHyperionItemBgHover(), alpha);
            Color baseBg = alphaColor(ClickGui.INSTANCE.getHyperionItemBg(), alpha);
            Render2DUtil.drawSmoothRoundedRect(matrices, bottomBtnX, bottomY, bottomBtnW, btnH, HyperionMetrics.COMPONENT_RADIUS, blend(baseBg, hoverBg, bhVal));
        }
        float blw = ClickGuiTextUtil.getWidth(bottomLabel);
        ClickGuiTextUtil.drawStringWithShadow(context, bottomLabel, bottomBtnX + bottomBtnW / 2f - blw / 2f, bottomY + btnH / 2f - textH / 2f, secondary.getRGB());
        matrices.pop();
    }

    private void drawSongItem(DrawContext context, MatrixStack matrices, int x, int y, int w, int index, Song song, boolean hovered, boolean isCurrent, float alpha) {
        int rowH = 28;
        int animIdx = Math.min(index + 4, hoverAnims.length - 1);

        float hoverVal = (float) hoverAnims[animIdx].get(hovered ? 1.0 : 0.0, ClickGui.INSTANCE.getHyperionAnimDuration(120), Easing.CubicOut);
        float clickScale = getClickScale(animIdx);

        matrices.push();
        matrices.translate(x + w / 2f, y + rowH / 2f, 0);
        matrices.scale(clickScale, clickScale, 1);
        matrices.translate(-(x + w / 2f), -(y + rowH / 2f), 0);

        if (isCurrent) {
            Color selectedBg = alphaColor(ClickGui.INSTANCE.getHyperionItemBgSelected(), alpha);
            Render2DUtil.drawSmoothRoundedRect(matrices, x, y, w, rowH, HyperionMetrics.COMPONENT_RADIUS, selectedBg);
            Render2DUtil.drawSmoothRoundedRect(matrices, x, y, 3, rowH, 1.5f, alphaColor(ClickGui.INSTANCE.hyperionAccentColor.getValue(), alpha));
        } else if (hoverVal > 0.01f) {
            Color hoverBg = alphaColor(ClickGui.INSTANCE.getHyperionItemBgHover(), alpha);
            Color baseBg = alphaColor(ClickGui.INSTANCE.getHyperionItemBg(), alpha);
            Render2DUtil.drawSmoothRoundedRect(matrices, x, y, w, rowH, HyperionMetrics.COMPONENT_RADIUS, blend(baseBg, hoverBg, hoverVal));
        }

        Color primary = alphaColor(ClickGui.INSTANCE.hyperionTextPrimary.getValue(), alpha);
        Color secondary = alphaColor(ClickGui.INSTANCE.hyperionTextSecondary.getValue(), alpha);
        Color accent = alphaColor(ClickGui.INSTANCE.hyperionAccentColor.getValue(), alpha);

        float textH = ClickGuiTextUtil.getHeight();
        int textX = x + 14;
        String idx = String.valueOf(index + 1);
        Color idxColor = index < 3 ? primary : secondary;
        ClickGuiTextUtil.drawStringWithShadow(context, idx, textX, y + rowH / 2f - textH / 2f, idxColor.getRGB());

        String name = song.name;
        float availW = w - 70;
        if (ClickGuiTextUtil.getWidth(name) > availW) {
            while (ClickGuiTextUtil.getWidth(name + "...") > availW && name.length() > 1) name = name.substring(0, name.length() - 1);
            name += "...";
        }
        Color nameColor = isCurrent ? primary : alphaColor(ClickGui.INSTANCE.hyperionTextPrimary.getValue(), alpha);
        ClickGuiTextUtil.drawStringWithShadow(context, name, textX + 20, y + rowH / 2f - textH / 2f, nameColor.getRGB());

        String singer = song.singer;
        if (ClickGuiTextUtil.getWidth(singer) > 70) {
            while (ClickGuiTextUtil.getWidth(singer + "...") > 70 && singer.length() > 1) singer = singer.substring(0, singer.length() - 1);
            singer += "...";
        }
        float singerX = x + w - ClickGuiTextUtil.getWidth(singer) - 10;
        ClickGuiTextUtil.drawStringWithShadow(context, singer, singerX, y + rowH / 2f - textH / 2f, secondary.getRGB());

        if (isCurrent && MediaPlayer.INSTANCE.isPlaying) {
            drawPlayingIndicator(matrices, x + w - 10, y + 10);
        }

        matrices.pop();
    }

    private void drawHomePage(DrawContext context, MatrixStack matrices, float x, float y, float w, float h, int mouseX, int mouseY, float alpha) {
        Color primary = alphaColor(ClickGui.INSTANCE.hyperionTextPrimary.getValue(), alpha);
        Color secondary = alphaColor(ClickGui.INSTANCE.hyperionTextSecondary.getValue(), alpha);
        Color accent = alphaColor(ClickGui.INSTANCE.hyperionAccentColor.getValue(), alpha);
        Color border = alphaColor(ClickGui.INSTANCE.hyperionBorderColor.getValue(), alpha);
        float textH = ClickGuiTextUtil.getHeight();

        float drawY = y - scrollOffset;

        ClickGuiTextUtil.drawStringWithShadow(context, "热门歌曲", x, drawY, primary.getRGB());
        drawY += textH + 8;

        synchronized (newSongs) {
            for (int i = 0; i < Math.min(5, newSongs.size()); i++) {
                Song song = newSongs.get(i);
                boolean hovered = isHovered(mouseX, mouseY, x, drawY, w, 28);
                boolean isCurrent = MediaPlayer.INSTANCE.currentSong != null && MediaPlayer.INSTANCE.currentSong.id == song.id;
                drawSongItem(context, matrices, (int) x, (int) drawY, (int) w, i, song, hovered, isCurrent, alpha);
                drawY += 30;
            }
        }

        drawY += 6;
        Render2DUtil.drawSmoothRoundedRect(matrices, x, drawY, w, 1, 0.5f, border);
        drawY += 10;

        ClickGuiTextUtil.drawStringWithShadow(context, "推荐歌单", x, drawY, primary.getRGB());
        drawY += textH + 8;

        synchronized (recommendPlaylists) {
            for (int i = 0; i < recommendPlaylists.size(); i++) {
                Playlist pl = recommendPlaylists.get(i);
                boolean hovered = isHovered(mouseX, mouseY, x, drawY, w, 26);
                float hv = (float) hoverAnims[Math.min(i + 10, hoverAnims.length - 1)].get(hovered ? 1.0 : 0.0, ClickGui.INSTANCE.getHyperionAnimDuration(120), Easing.CubicOut);
                if (hv > 0.01f) {
                    Color hoverBg = alphaColor(ClickGui.INSTANCE.getHyperionItemBgHover(), alpha);
                    Color baseBg = alphaColor(ClickGui.INSTANCE.getHyperionItemBg(), alpha);
                    Render2DUtil.drawSmoothRoundedRect(matrices, x, drawY, w, 26, HyperionMetrics.COMPONENT_RADIUS, blend(baseBg, hoverBg, hv));
                }
                Render2DUtil.drawSmoothRoundedRect(matrices, x + 4, drawY + 7, 3, 12, 1.5f, accent);
                String text = pl.name;
                float availW = w - 80;
                if (ClickGuiTextUtil.getWidth(text) > availW) {
                    while (ClickGuiTextUtil.getWidth(text + "...") > availW && text.length() > 1) text = text.substring(0, text.length() - 1);
                    text += "...";
                }
                ClickGuiTextUtil.drawStringWithShadow(context, text, x + 14, drawY + 13 - textH / 2f, secondary.getRGB());
                String count = formatPlayCount(pl.playCount);
                float cw = ClickGuiTextUtil.getWidth(count);
                ClickGuiTextUtil.drawStringWithShadow(context, count, x + w - cw - 10, drawY + 13 - textH / 2f, secondary.getRGB());
                drawY += 28;
            }
        }

        drawY += 6;
        Render2DUtil.drawSmoothRoundedRect(matrices, x, drawY, w, 1, 0.5f, border);
        drawY += 10;

        ClickGuiTextUtil.drawStringWithShadow(context, "新歌速递", x, drawY, primary.getRGB());
        drawY += textH + 8;

        synchronized (newSongs) {
            for (int i = 5; i < Math.min(15, newSongs.size()); i++) {
                Song song = newSongs.get(i);
                boolean hovered = isHovered(mouseX, mouseY, x, drawY, w, 28);
                boolean isCurrent = MediaPlayer.INSTANCE.currentSong != null && MediaPlayer.INSTANCE.currentSong.id == song.id;
                drawSongItem(context, matrices, (int) x, (int) drawY, (int) w, i, song, hovered, isCurrent, alpha);
                drawY += 28;
            }
        }

        maxScroll = Math.max(0, (int) ((drawY + scrollOffset) - (y + h)));
    }

    private void drawSearchPage(DrawContext context, MatrixStack matrices, float x, float y, float w, float h, int mouseX, int mouseY, float alpha) {
        Color primary = alphaColor(ClickGui.INSTANCE.hyperionTextPrimary.getValue(), alpha);
        Color secondary = alphaColor(ClickGui.INSTANCE.hyperionTextSecondary.getValue(), alpha);
        Color accent = alphaColor(ClickGui.INSTANCE.hyperionAccentColor.getValue(), alpha);
        Color border = alphaColor(ClickGui.INSTANCE.hyperionBorderColor.getValue(), alpha);
        Color sectionBg = alphaColor(ClickGui.INSTANCE.getHyperionSectionBgWithAlpha(), alpha);
        float textH = ClickGuiTextUtil.getHeight();

        float searchBarW = w - 70;
        float searchBarH = 26;

        Render2DUtil.drawSmoothRoundedRect(matrices, x, y, searchBarW, searchBarH, HyperionMetrics.COMPONENT_RADIUS, sectionBg);
        Color borderColor = searchFocused ? accent : border;
        Render2DUtil.drawSmoothRoundedStroke(matrices, x, y, searchBarW, searchBarH, HyperionMetrics.COMPONENT_RADIUS, borderColor);

        if (searchText.isEmpty() && !searchFocused) {
            ClickGuiTextUtil.drawStringWithShadow(context, "搜索歌曲、歌手...", x + 10, y + searchBarH / 2f - textH / 2f, secondary.getRGB());
        } else {
            ClickGuiTextUtil.drawStringWithShadow(context, searchText, x + 10, y + searchBarH / 2f - textH / 2f, primary.getRGB());
            if (searchFocused && (System.currentTimeMillis() / 530) % 2 == 0) {
                float cursorX = x + 10 + ClickGuiTextUtil.getWidth(searchText);
                Render2DUtil.rectFilled(matrices, cursorX, y + 7, cursorX + 1, y + 19, primary.getRGB());
            }
        }

        float searchBtnX = x + searchBarW + 8;
        boolean searchBtnHovered = isHovered(mouseX, mouseY, searchBtnX, y, 58, searchBarH);
        float searchHv = (float) hoverAnims[20].get(searchBtnHovered ? 1.0 : 0.0, ClickGui.INSTANCE.getHyperionAnimDuration(150), Easing.CubicOut);
        float searchClickS = getClickScale(20);

        matrices.push();
        matrices.translate(searchBtnX + 29f, y + searchBarH / 2f, 0);
        matrices.scale(searchClickS, searchClickS, 1);
        matrices.translate(-(searchBtnX + 29f), -(y + searchBarH / 2f), 0);

        Color btnBg = searchBtnHovered ? accent : alphaColor(ClickGui.INSTANCE.getHyperionItemBgHover(), alpha);
        Render2DUtil.drawSmoothRoundedRect(matrices, searchBtnX, y, 58, searchBarH, HyperionMetrics.PILL_RADIUS, btnBg);
        float stw = ClickGuiTextUtil.getWidth("搜索");
        Color searchBtnTextColor = searchBtnHovered ? primary : primary;
        ClickGuiTextUtil.drawStringWithShadow(context, "搜索", searchBtnX + 29 - stw / 2f, y + searchBarH / 2f - textH / 2f, searchBtnTextColor.getRGB());
        matrices.pop();

        float listY = y + 34 - scrollOffset;
        synchronized (searchResults) {
            for (int i = 0; i < searchResults.size(); i++) {
                Song song = searchResults.get(i);
                float itemY = listY + i * 30;
                if (itemY + 30 < y + 34 || itemY > y + h) continue;
                boolean hovered = isHovered(mouseX, mouseY, x, itemY, w, 28);
                boolean isCurrent = MediaPlayer.INSTANCE.currentSong != null && MediaPlayer.INSTANCE.currentSong.id == song.id;
                drawSongItem(context, matrices, (int) x, (int) itemY, (int) w, i, song, hovered, isCurrent, alpha);
            }
            maxScroll = Math.max(0, searchResults.size() * 30 - (int) (h - 34));
        }
    }

    private void drawLikePage(DrawContext context, MatrixStack matrices, float x, float y, float w, float h, int mouseX, int mouseY, float alpha) {
        Color primary = alphaColor(ClickGui.INSTANCE.hyperionTextPrimary.getValue(), alpha);
        Color secondary = alphaColor(ClickGui.INSTANCE.hyperionTextSecondary.getValue(), alpha);
        Color disabled = alphaColor(ClickGui.INSTANCE.getHyperionTextDisabled(), alpha);
        float textH = ClickGuiTextUtil.getHeight();

        if (!NeteaseCloudApi.INSTANCE.isLoggedIn) {
            float msgW = ClickGuiTextUtil.getWidth("请先登录");
            ClickGuiTextUtil.drawStringWithShadow(context, "请先登录", x + w / 2f - msgW / 2f, y + h / 2f - textH / 2f, disabled.getRGB());
            return;
        }
        if (!likeLoaded) {
            loadLikeSongs();
            float msgW = ClickGuiTextUtil.getWidth("加载中...");
            ClickGuiTextUtil.drawStringWithShadow(context, "加载中...", x + w / 2f - msgW / 2f, y + h / 2f - textH / 2f, disabled.getRGB());
            return;
        }
        ClickGuiTextUtil.drawStringWithShadow(context, "我喜欢的音乐", x, y, primary.getRGB());
        float listY = y + textH + 8 - scrollOffset;
        synchronized (likeSongs) {
            for (int i = 0; i < likeSongs.size(); i++) {
                Song song = likeSongs.get(i);
                float itemY = listY + i * 30;
                if (itemY + 30 < y + textH + 8 || itemY > y + h) continue;
                boolean hovered = isHovered(mouseX, mouseY, x, itemY, w, 28);
                boolean isCurrent = MediaPlayer.INSTANCE.currentSong != null && MediaPlayer.INSTANCE.currentSong.id == song.id;
                drawSongItem(context, matrices, (int) x, (int) itemY, (int) w, i, song, hovered, isCurrent, alpha);
            }
            maxScroll = Math.max(0, likeSongs.size() * 30 - (int) (h - textH - 8));
        }
    }

    private void drawLoginPage(DrawContext context, MatrixStack matrices, float x, float y, float w, float h, int mouseX, int mouseY, float alpha) {
        Color primary = alphaColor(ClickGui.INSTANCE.hyperionTextPrimary.getValue(), alpha);
        Color secondary = alphaColor(ClickGui.INSTANCE.hyperionTextSecondary.getValue(), alpha);
        Color accent = alphaColor(ClickGui.INSTANCE.hyperionAccentColor.getValue(), alpha);
        Color sectionBg = alphaColor(ClickGui.INSTANCE.getHyperionSectionBgWithAlpha(), alpha);
        float textH = ClickGuiTextUtil.getHeight();

        String title = "登录网易云音乐";
        float titleW = ClickGuiTextUtil.getWidth(title);
        ClickGuiTextUtil.drawStringWithShadow(context, title, x + w / 2f - titleW / 2f, y, primary.getRGB());
        y += textH + 12;

        float tabW = 70, tabH = 24;
        String[] tabs = {"手机号", "二维码"};
        for (int i = 0; i < tabs.length; i++) {
            float tabX = x + w / 2 - 74 + i * 78;
            boolean selected = loginTab == i;
            boolean hovered = isHovered(mouseX, mouseY, tabX, y, tabW, tabH);
            float tabClickS = getClickScale(40 + i);

            matrices.push();
            matrices.translate(tabX + tabW / 2f, y + tabH / 2f, 0);
            matrices.scale(tabClickS, tabClickS, 1);
            matrices.translate(-(tabX + tabW / 2f), -(y + tabH / 2f), 0);

            if (selected) {
                Render2DUtil.drawSmoothRoundedRect(matrices, tabX, y, tabW, tabH, HyperionMetrics.PILL_RADIUS, accent);
            } else if (hovered) {
                Render2DUtil.drawSmoothRoundedRect(matrices, tabX, y, tabW, tabH, HyperionMetrics.PILL_RADIUS, sectionBg);
            } else {
                Render2DUtil.drawSmoothRoundedRect(matrices, tabX, y, tabW, tabH, HyperionMetrics.PILL_RADIUS, alphaColor(ClickGui.INSTANCE.getHyperionItemBg(), alpha));
            }
            float ttw = ClickGuiTextUtil.getWidth(tabs[i]);
            Color tabTextColor = selected ? primary : secondary;
            ClickGuiTextUtil.drawStringWithShadow(context, tabs[i], tabX + tabW / 2f - ttw / 2f, y + tabH / 2f - textH / 2f, tabTextColor.getRGB());
            matrices.pop();
        }
        y += tabH + 10;

        if (loginTab == 0) drawPhoneLogin(context, matrices, x, y, w, mouseX, mouseY, alpha);
        else drawQrLogin(context, matrices, x, y, w, alpha);
    }

    private void drawPhoneLogin(DrawContext context, MatrixStack matrices, float x, float y, float w, int mouseX, int mouseY, float alpha) {
        Color primary = alphaColor(ClickGui.INSTANCE.hyperionTextPrimary.getValue(), alpha);
        Color secondary = alphaColor(ClickGui.INSTANCE.hyperionTextSecondary.getValue(), alpha);
        Color accent = alphaColor(ClickGui.INSTANCE.hyperionAccentColor.getValue(), alpha);
        Color border = alphaColor(ClickGui.INSTANCE.hyperionBorderColor.getValue(), alpha);
        Color sectionBg = alphaColor(ClickGui.INSTANCE.getHyperionSectionBgWithAlpha(), alpha);
        float textH = ClickGuiTextUtil.getHeight();

        ClickGuiTextUtil.drawStringWithShadow(context, "手机号", x, y, secondary.getRGB());
        y += textH + 6;
        Render2DUtil.drawSmoothRoundedRect(matrices, x, y, w, 26, HyperionMetrics.COMPONENT_RADIUS, sectionBg);
        Color phoneBorderColor = phoneFocused ? accent : border;
        Render2DUtil.drawSmoothRoundedStroke(matrices, x, y, w, 26, HyperionMetrics.COMPONENT_RADIUS, phoneBorderColor);
        String phoneDisplay = phoneText.isEmpty() ? "输入手机号" : phoneText;
        Color phoneColor = phoneText.isEmpty() ? secondary : primary;
        ClickGuiTextUtil.drawStringWithShadow(context, phoneDisplay, x + 10, y + 13 - textH / 2f, phoneColor.getRGB());
        if (phoneFocused && (System.currentTimeMillis() / 530) % 2 == 0) {
            float cursorX = x + 10 + ClickGuiTextUtil.getWidth(phoneText);
            Render2DUtil.rectFilled(matrices, cursorX, y + 7, cursorX + 1, y + 19, primary.getRGB());
        }
        y += 34;

        ClickGuiTextUtil.drawStringWithShadow(context, "验证码", x, y, secondary.getRGB());
        y += textH + 6;
        float captchaW = w - 88;
        Render2DUtil.drawSmoothRoundedRect(matrices, x, y, captchaW, 26, HyperionMetrics.COMPONENT_RADIUS, sectionBg);
        Color captchaBorderColor = captchaFocused ? accent : border;
        Render2DUtil.drawSmoothRoundedStroke(matrices, x, y, captchaW, 26, HyperionMetrics.COMPONENT_RADIUS, captchaBorderColor);
        String captchaDisplay = captchaText.isEmpty() ? "输入验证码" : captchaText;
        Color captchaColor = captchaText.isEmpty() ? secondary : primary;
        ClickGuiTextUtil.drawStringWithShadow(context, captchaDisplay, x + 10, y + 13 - textH / 2f, captchaColor.getRGB());
        if (captchaFocused && (System.currentTimeMillis() / 530) % 2 == 0) {
            float cursorX = x + 10 + ClickGuiTextUtil.getWidth(captchaText);
            Render2DUtil.rectFilled(matrices, cursorX, y + 7, cursorX + 1, y + 19, primary.getRGB());
        }

        float captchaBtnX = x + captchaW + 8;
        boolean canSend = captchaCountdown <= 0 && phoneText.length() == 11;
        boolean captchaBtnHovered = isHovered(mouseX, mouseY, captchaBtnX, y, 76, 26);
        Color btnColor = canSend ? (captchaBtnHovered ? accent : alphaColor(ClickGui.INSTANCE.getHyperionItemBgHover(), alpha)) : sectionBg;
        Render2DUtil.drawSmoothRoundedRect(matrices, captchaBtnX, y, 76, 26, HyperionMetrics.PILL_RADIUS, btnColor);
        String btnText = captchaCountdown > 0 ? captchaCountdown + "s" : "获取验证码";
        float btw = ClickGuiTextUtil.getWidth(btnText);
        Color captchaBtnTextColor = canSend ? primary : secondary;
        ClickGuiTextUtil.drawStringWithShadow(context, btnText, captchaBtnX + 38 - btw / 2f, y + 13 - textH / 2f, captchaBtnTextColor.getRGB());
        y += 36;

        if (!loginMessage.isEmpty()) {
            Color msgColor = loginMessage.contains("成功") ? new Color(76, 175, 80) : new Color(229, 57, 53);
            ClickGuiTextUtil.drawStringWithShadow(context, loginMessage, x, y, msgColor.getRGB());
            y += textH + 6;
        }

        boolean canLogin = phoneText.length() == 11 && captchaText.length() >= 4;
        boolean loginBtnHovered = isHovered(mouseX, mouseY, x, y + 4, w, 30);
        float loginClickS = getClickScale(42);

        matrices.push();
        matrices.translate(x + w / 2f, y + 19f, 0);
        matrices.scale(loginClickS, loginClickS, 1);
        matrices.translate(-(x + w / 2f), -(y + 19f), 0);

        Color loginBtnColor = canLogin ? (loginBtnHovered ? accent : alphaColor(ClickGui.INSTANCE.getHyperionItemBgHover(), alpha)) : sectionBg;
        Render2DUtil.drawSmoothRoundedRect(matrices, x, y + 4, w, 30, HyperionMetrics.PILL_RADIUS, loginBtnColor);
        Color loginBtnTextColor = canLogin ? primary : secondary;
        float lw = ClickGuiTextUtil.getWidth("登录");
        ClickGuiTextUtil.drawStringWithShadow(context, "登录", x + w / 2f - lw / 2f, y + 19 - textH / 2f, loginBtnTextColor.getRGB());
        matrices.pop();

        updateCaptchaCountdown();
    }

    private void drawQrLogin(DrawContext context, MatrixStack matrices, float x, float y, float w, float alpha) {
        Color secondary = alphaColor(ClickGui.INSTANCE.hyperionTextSecondary.getValue(), alpha);
        Color sectionBg = alphaColor(ClickGui.INSTANCE.getHyperionSectionBgWithAlpha(), alpha);
        Color primary = alphaColor(ClickGui.INSTANCE.hyperionTextPrimary.getValue(), alpha);
        float textH = ClickGuiTextUtil.getHeight();

        int qrSize = 120;
        float qrX = x + w / 2f - qrSize / 2f;
        Render2DUtil.drawSmoothRoundedRect(matrices, qrX - 8, y - 8, qrSize + 16, qrSize + 16, HyperionMetrics.SECTION_RADIUS, sectionBg);
        Render2DUtil.drawSmoothRoundedRect(matrices, qrX, y, qrSize, qrSize, HyperionMetrics.COMPONENT_RADIUS, Color.WHITE);

        if (qrTextureId != null) {
            context.drawTexture(qrTextureId, (int) qrX + 6, (int) y + 6, 0, 0, qrSize - 12, qrSize - 12, qrSize - 12, qrSize - 12);
        } else {
            String loading = "加载中...";
            float lw = ClickGuiTextUtil.getWidth(loading);
            ClickGuiTextUtil.drawStringWithShadow(context, loading, qrX + qrSize / 2f - lw / 2f, y + qrSize / 2f - textH / 2f, secondary.getRGB());
        }

        y += qrSize + 14;
        Color statusColor = qrStatusText.contains("成功") ? new Color(76, 175, 80) : (qrStatusText.contains("过期") ? new Color(229, 57, 53) : secondary);
        float sw = ClickGuiTextUtil.getWidth(qrStatusText);
        ClickGuiTextUtil.drawStringWithShadow(context, qrStatusText, x + w / 2f - sw / 2f, y, statusColor.getRGB());
        y += textH + 6;

        if (qrStatusText.contains("过期")) {
            float rw = ClickGuiTextUtil.getWidth("点击刷新");
            ClickGuiTextUtil.drawStringWithShadow(context, "点击刷新", x + w / 2f - rw / 2f, y, primary.getRGB());
            y += textH + 4;
        }

        float hw = ClickGuiTextUtil.getWidth("使用网易云音乐APP扫码");
        ClickGuiTextUtil.drawStringWithShadow(context, "使用网易云音乐APP扫码", x + w / 2f - hw / 2f, y, secondary.getRGB());

        checkQrStatusAuto();
    }

    private void drawBottomPlayerBar(DrawContext context, MatrixStack matrices, float x, float y, float w, float h, int mouseX, int mouseY, float alpha) {
        Color sectionBg = alphaColor(ClickGui.INSTANCE.getHyperionSectionBgWithAlpha(), alpha);
        Color border = alphaColor(ClickGui.INSTANCE.hyperionBorderColor.getValue(), alpha);
        Color primary = alphaColor(ClickGui.INSTANCE.hyperionTextPrimary.getValue(), alpha);
        Color secondary = alphaColor(ClickGui.INSTANCE.hyperionTextSecondary.getValue(), alpha);
        Color accent = alphaColor(ClickGui.INSTANCE.hyperionAccentColor.getValue(), alpha);
        float textH = ClickGuiTextUtil.getHeight();

        Render2DUtil.drawSmoothRoundedRect(matrices, x, y, w, h, HyperionMetrics.SECTION_RADIUS, sectionBg);
        Render2DUtil.drawSmoothRoundedRect(matrices, x, y, w, 1, 0.5f, border);

        MediaPlayer mp = MediaPlayer.INSTANCE;
        float progress = mp.totalDuration > 0 ? (float) mp.tick / mp.totalDuration : 0f;
        progress = Math.min(1f, Math.max(0f, progress));

        float barY = y + 5;
        float barH = 4;
        Render2DUtil.drawSmoothRoundedRect(matrices, x + 10, barY, w - 20, barH, barH / 2f, alphaColor(border, 0.7f));
        if (progress > 0) {
            float barW = (w - 20) * progress;
            Render2DUtil.drawSmoothRoundedRect(matrices, x + 10, barY, barW, barH, barH / 2f, accent);
            Render2DUtil.drawSmoothCircle(matrices, x + 10 + barW, barY + barH / 2f, 4, primary, 12);
        }

        float contentY = y + 18;
        if (mp.currentSong != null) {
            float discCx = x + 30;
            float discCy = contentY + 16;
            float discR = 18;
            Render2DUtil.drawSmoothCircle(matrices, discCx, discCy, discR, alphaColor(primary, 0.1f), 32);
            Render2DUtil.drawSmoothCircle(matrices, discCx, discCy, discR - 1, alphaColor(border, 0.6f), 32);
            if (mp.isPlaying) {
                float angle = (System.currentTimeMillis() % 8000) / 8000f * 360f;
                float rad = (float) Math.toRadians(angle);
                float lx = discCx + (float) Math.cos(rad) * (discR - 6);
                float ly = discCy + (float) Math.sin(rad) * (discR - 6);
                Render2DUtil.drawSmoothCircle(matrices, lx, ly, 2.5f, accent, 8);
            }
            Render2DUtil.drawSmoothCircle(matrices, discCx, discCy, 5, alphaColor(primary, 0.15f), 16);

            String songName = mp.currentSong.name;
            float maxNameW = w / 2f - 70;
            if (ClickGuiTextUtil.getWidth(songName) > maxNameW) {
                while (ClickGuiTextUtil.getWidth(songName + "...") > maxNameW && songName.length() > 1) songName = songName.substring(0, songName.length() - 1);
                songName += "...";
            }
            ClickGuiTextUtil.drawStringWithShadow(context, songName, x + 54, contentY + 4, primary.getRGB());
            String timeInfo = mp.currentSong.singer + " · " + mp.formatTime(mp.tick) + "/" + mp.formatTime(mp.totalDuration);
            ClickGuiTextUtil.drawStringWithShadow(context, timeInfo, x + 54, contentY + 16, secondary.getRGB());
        } else {
            ClickGuiTextUtil.drawStringWithShadow(context, "暂无播放", x + 54, contentY + 10, secondary.getRGB());
        }

        float centerX = x + w - 96;
        float btnY = contentY + 7;

        boolean prevHovered = isHovered(mouseX, mouseY, centerX, btnY, 20, 20);
        float prevHv = (float) hoverAnims[30].get(prevHovered ? 1.0 : 0.0, ClickGui.INSTANCE.getHyperionAnimDuration(120), Easing.CubicOut);
        float prevClickV = (float) prevClickAnim.get(0.0, 200L, Easing.CubicOut);
        float prevScale = 1.0f - 0.08f * prevClickV;

        matrices.push();
        matrices.translate(centerX + 10f, btnY + 10f, 0);
        matrices.scale(prevScale, prevScale, 1);
        matrices.translate(-(centerX + 10f), -(btnY + 10f), 0);
        Render2DUtil.drawSmoothRoundedRect(matrices, centerX, btnY, 20, 20, 10, alphaColor(ClickGui.INSTANCE.getHyperionItemBgHover(), alpha));
        String prevIcon = "◀";
        float prevIw = ClickGuiTextUtil.getWidth(prevIcon);
        ClickGuiTextUtil.drawStringWithShadow(context, prevIcon, centerX + 10 - prevIw / 2f, btnY + 10 - textH / 2f, secondary.getRGB());
        matrices.pop();

        float playBtnX = centerX + 28;
        boolean playHovered = isHovered(mouseX, mouseY, playBtnX, btnY - 2, 24, 24);
        float playHv = (float) playBtnAnim.get(playHovered ? 1.0 : 0.0, ClickGui.INSTANCE.getHyperionAnimDuration(150), Easing.CubicOut);
        float playClickV = (float) playClickAnim.get(0.0, 200L, Easing.CubicOut);
        float pulse = mp.isPlaying ? 1f + 0.03f * (float) Math.sin(System.currentTimeMillis() / 350.0) : 1f;
        float playScale = (1f + 0.04f * playHv - 0.08f * playClickV) * pulse;
        float playW = 24 * playScale;
        float playH = 24 * playScale;
        float playOffX = (playW - 24) / 2f;
        float playOffY = (playH - 24) / 2f;
        Render2DUtil.drawSmoothRoundedRect(matrices, playBtnX - playOffX, btnY - 2 - playOffY, playW, playH, 12, accent);
        String playIcon = mp.isPlaying ? "❚❚" : "▶";
        float piw = ClickGuiTextUtil.getWidth(playIcon);
        float iconX = playBtnX + 12 * playScale - piw / 2f;
        float iconY = btnY - 2 + 12 * playScale - textH / 2f;
        ClickGuiTextUtil.drawStringWithShadow(context, playIcon, iconX, iconY, primary.getRGB());

        float nextBtnX = centerX + 58;
        boolean nextHovered = isHovered(mouseX, mouseY, nextBtnX, btnY, 20, 20);
        float nextHv = (float) hoverAnims[31].get(nextHovered ? 1.0 : 0.0, ClickGui.INSTANCE.getHyperionAnimDuration(120), Easing.CubicOut);
        float nextClickV = (float) nextClickAnim.get(0.0, 200L, Easing.CubicOut);
        float nextScale = 1.0f - 0.08f * nextClickV;

        matrices.push();
        matrices.translate(nextBtnX + 10f, btnY + 10f, 0);
        matrices.scale(nextScale, nextScale, 1);
        matrices.translate(-(nextBtnX + 10f), -(btnY + 10f), 0);
        Render2DUtil.drawSmoothRoundedRect(matrices, nextBtnX, btnY, 20, 20, 10, alphaColor(ClickGui.INSTANCE.getHyperionItemBgHover(), alpha));
        String nextIcon = "▶";
        float nextIw = ClickGuiTextUtil.getWidth(nextIcon);
        ClickGuiTextUtil.drawStringWithShadow(context, nextIcon, nextBtnX + 10 - nextIw / 2f, btnY + 10 - textH / 2f, secondary.getRGB());
        matrices.pop();
    }

    private void drawPlayerDetail(DrawContext context, MatrixStack matrices, int mouseX, int mouseY, float alpha) {
        float detail = (float) detailAnim.get(1.0, ClickGui.INSTANCE.getHyperionAnimDuration(300), Easing.CubicOut);
        float a = alpha * detail;

        Color panelBg = alphaColor(ClickGui.INSTANCE.getHyperionPanelBgWithAlpha(), a);
        Color border = alphaColor(ClickGui.INSTANCE.hyperionBorderColor.getValue(), a);
        Color primary = alphaColor(ClickGui.INSTANCE.hyperionTextPrimary.getValue(), a);
        Color secondary = alphaColor(ClickGui.INSTANCE.hyperionTextSecondary.getValue(), a);
        Color accent = alphaColor(ClickGui.INSTANCE.hyperionAccentColor.getValue(), a);
        float textH = ClickGuiTextUtil.getHeight();

        Render2DUtil.drawSmoothRoundedRect(matrices, panelX, panelY, panelWidth, panelHeight, HyperionMetrics.PANEL_RADIUS, panelBg);
        Render2DUtil.drawSmoothRoundedStroke(matrices, panelX, panelY, panelWidth, panelHeight, HyperionMetrics.PANEL_RADIUS, border);

        MediaPlayer mp = MediaPlayer.INSTANCE;

        float topY = panelY + HyperionMetrics.PADDING + 4;
        float closeBtnX = panelX + HyperionMetrics.PADDING;
        float closeBtnY = topY;
        boolean closeHovered = isHovered(mouseX, mouseY, closeBtnX, closeBtnY, 22, 22);
        float closeHv = (float) hoverAnims[40].get(closeHovered ? 1.0 : 0.0, ClickGui.INSTANCE.getHyperionAnimDuration(120), Easing.CubicOut);
        Render2DUtil.drawSmoothRoundedRect(matrices, closeBtnX, closeBtnY, 22, 22, 11, alphaColor(ClickGui.INSTANCE.getHyperionItemBgHover(), a * closeHv));
        float cw = ClickGuiTextUtil.getWidth("✕");
        ClickGuiTextUtil.drawStringWithShadow(context, "✕", closeBtnX + 11 - cw / 2f, closeBtnY + 11 - textH / 2f, secondary.getRGB());

        if (mp.currentSong != null) {
            float nameW = ClickGuiTextUtil.getWidth(mp.currentSong.name);
            ClickGuiTextUtil.drawStringWithShadow(context, mp.currentSong.name, panelX + panelWidth / 2f - nameW / 2f, topY, primary.getRGB());
            float singerW = ClickGuiTextUtil.getWidth(mp.currentSong.singer);
            ClickGuiTextUtil.drawStringWithShadow(context, mp.currentSong.singer, panelX + panelWidth / 2f - singerW / 2f, topY + textH + 2, secondary.getRGB());
        }

        float midY = panelY + 58;
        float discCx = panelX + 90;
        float discCy = midY + 70;
        float discR = 65;
        Render2DUtil.drawSmoothCircle(matrices, discCx, discCy, discR, alphaColor(primary, 0.1f), 64);
        Render2DUtil.drawSmoothCircle(matrices, discCx, discCy, discR - 1, alphaColor(border, 0.6f), 64);
        if (mp.isPlaying) {
            float angle = (System.currentTimeMillis() % 20000) / 20000f * 360f;
            for (int i = 0; i < 4; i++) {
                float ang = (float) Math.toRadians(angle + i * 90);
                float lx = discCx + (float) Math.cos(ang) * (discR - 14);
                float ly = discCy + (float) Math.sin(ang) * (discR - 14);
                Render2DUtil.drawSmoothCircle(matrices, lx, ly, 3.5f, alphaColor(accent, 0.8f), 8);
            }
        }
        Render2DUtil.drawSmoothCircle(matrices, discCx, discCy, 18, alphaColor(primary, 0.15f), 32);

        float lyricX = panelX + 170;
        float lyricW = panelWidth - 180;
        List<LyricLine> lyrics;
        synchronized (mp.lyric) { lyrics = new ArrayList<>(mp.lyric); }
        int currentLyricIdx = LyricLineProcessor.findCurrentIndex(lyrics, mp.tick);

        float lyricDrawY = midY;
        int startIdx = Math.max(0, currentLyricIdx - 4);
        int endIdx = Math.min(lyrics.size(), startIdx + 9);
        for (int i = startIdx; i < endIdx; i++) {
            LyricLine line = lyrics.get(i);
            String text = line.text.isEmpty() ? "···" : line.text;
            boolean isCurrent = i == currentLyricIdx;
            Color color;
            if (isCurrent) {
                color = primary;
            } else {
                int distance = Math.abs(i - currentLyricIdx);
                int al = distance == 1 ? 140 : (distance == 2 ? 70 : 30);
                color = new Color(secondary.getRed(), secondary.getGreen(), secondary.getBlue(), Math.max(0, Math.min(255, Math.round(al * a))));
            }
            float tw = ClickGuiTextUtil.getWidth(text);
            ClickGuiTextUtil.drawStringWithShadow(context, text, lyricX + lyricW / 2f - tw / 2f, lyricDrawY, color.getRGB());
            lyricDrawY += 16;
        }

        float bottomY = panelY + panelHeight - 56;
        float prog = mp.totalDuration > 0 ? (float) mp.tick / mp.totalDuration : 0f;
        prog = Math.min(1f, Math.max(0f, prog));

        float sliderX = panelX + 24;
        float sliderW = panelWidth - 48;
        float sliderH = 4;
        Render2DUtil.drawSmoothRoundedRect(matrices, sliderX, bottomY, sliderW, sliderH, sliderH / 2f, alphaColor(border, 0.7f));
        if (prog > 0) {
            float progW = sliderW * prog;
            Render2DUtil.drawSmoothRoundedRect(matrices, sliderX, bottomY, progW, sliderH, sliderH / 2f, accent);
            Render2DUtil.drawSmoothCircle(matrices, sliderX + progW, bottomY + sliderH / 2f, 5, primary, 12);
        }

        String timeStr = mp.formatTime(mp.tick) + " / " + mp.formatTime(mp.totalDuration);
        float timeW = ClickGuiTextUtil.getWidth(timeStr);
        ClickGuiTextUtil.drawStringWithShadow(context, timeStr, panelX + panelWidth / 2f - timeW / 2f, bottomY + 10, secondary.getRGB());

        float ctrlY = bottomY + 26;
        float centerX = panelX + panelWidth / 2;

        Render2DUtil.drawSmoothRoundedRect(matrices, centerX - 38, ctrlY - 2, 20, 20, 10, alphaColor(ClickGui.INSTANCE.getHyperionItemBgHover(), a));
        String prevIcon = "◀";
        float prevIw = ClickGuiTextUtil.getWidth(prevIcon);
        ClickGuiTextUtil.drawStringWithShadow(context, prevIcon, centerX - 38 + 10 - prevIw / 2f, ctrlY - 2 + 10 - textH / 2f, secondary.getRGB());

        Render2DUtil.drawSmoothRoundedRect(matrices, centerX - 10, ctrlY - 5, 24, 24, 12, accent);
        String playIcon = mp.isPlaying ? "❚❚" : "▶";
        float piw = ClickGuiTextUtil.getWidth(playIcon);
        ClickGuiTextUtil.drawStringWithShadow(context, playIcon, centerX + 2 - piw / 2f, ctrlY - 5 + 12 - textH / 2f, primary.getRGB());

        Render2DUtil.drawSmoothRoundedRect(matrices, centerX + 20, ctrlY - 2, 20, 20, 10, alphaColor(ClickGui.INSTANCE.getHyperionItemBgHover(), a));
        String nextIcon = "▶";
        float nextIw = ClickGuiTextUtil.getWidth(nextIcon);
        ClickGuiTextUtil.drawStringWithShadow(context, nextIcon, centerX + 20 + 10 - nextIw / 2f, ctrlY - 2 + 10 - textH / 2f, secondary.getRGB());

        float volBarX = centerX + 56;
        float volBarW = 50;
        ClickGuiTextUtil.drawStringWithShadow(context, "Vol", volBarX, ctrlY - 2 + 10 - textH / 2f, secondary.getRGB());
        Render2DUtil.drawSmoothRoundedRect(matrices, volBarX + 26, ctrlY + 8, volBarW, 2, 1, alphaColor(border, 0.7f));
        Render2DUtil.drawSmoothRoundedRect(matrices, volBarX + 26, ctrlY + 8, volBarW * mp.volume, 2, 1, accent);
    }

    private void drawPlayingIndicator(MatrixStack matrices, float x, float y) {
        long t = System.currentTimeMillis();
        Color accent = ClickGui.INSTANCE.hyperionAccentColor.getValue();
        for (int i = 0; i < 3; i++) {
            float h = 3 + (float) Math.abs(Math.sin((t + i * 150) / 280.0)) * 8;
            Render2DUtil.rectFilled(matrices, x + i * 4, y + 11 - h, x + i * 4 + 2, y + 11, accent.getRGB());
        }
    }

    private void checkQrStatusAuto() {
        if (qrKey == null || qrChecking || NeteaseCloudApi.INSTANCE.isLoggedIn) return;
        long now = System.currentTimeMillis();
        if (now - lastQrCheckTime < 2000) return;
        lastQrCheckTime = now;
        qrChecking = true;
        new Thread(() -> {
            try {
                QrStatus status = NeteaseCloudApi.INSTANCE.checkQrStatus(qrKey);
                switch (status) {
                    case WAITING -> qrStatusText = "等待扫码";
                    case SCANNED -> qrStatusText = "已扫码，请确认";
                    case SUCCESS -> qrStatusText = "登录成功";
                    case EXPIRED -> qrStatusText = "二维码已过期";
                }
            } catch (Exception e) { e.printStackTrace(); }
            qrChecking = false;
        }).start();
    }

    private void startQrLogin() {
        qrChecking = true;
        new Thread(() -> {
            try {
                String key = NeteaseCloudApi.INSTANCE.getQrKey();
                if (key != null) {
                    qrKey = key;
                    String img = NeteaseCloudApi.INSTANCE.getQrImage(key);
                    if (img != null) { qrImageBase64 = img; loadQrTexture(img); }
                    qrStatusText = "请扫码";
                } else { qrStatusText = "获取二维码失败"; }
            } catch (Exception e) { qrStatusText = "网络错误"; e.printStackTrace(); }
            qrChecking = false;
        }).start();
    }

    private void loadQrTexture(String base64Image) {
        try {
            String base64Data = base64Image.substring(base64Image.indexOf("base64,") + 7);
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(imageBytes));
            NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.isOnThread()) {
                cleanupQrTexture();
                qrDynamicTexture = texture;
                qrTextureId = mc.getTextureManager().registerDynamicTexture("qr_login", texture);
            } else {
                mc.execute(() -> {
                    cleanupQrTexture();
                    qrDynamicTexture = texture;
                    qrTextureId = mc.getTextureManager().registerDynamicTexture("qr_login", texture);
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void cleanupQrTexture() {
        if (qrDynamicTexture != null) { qrDynamicTexture.close(); qrDynamicTexture = null; }
        qrTextureId = null;
    }

    private void resetQrLogin() {
        cleanupQrTexture();
        qrKey = null; qrImageBase64 = null; qrStatusText = "等待获取";
        startQrLogin();
    }

    private void loadLikeSongs() {
        if (!NeteaseCloudApi.INSTANCE.isLoggedIn) return;
        new Thread(() -> {
            try {
                List<Song> songs = NeteaseCloudApi.INSTANCE.getLikeSongs(50, 0);
                synchronized (likeSongs) { likeSongs.clear(); likeSongs.addAll(songs); }
                likeLoaded = true;
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void loadHomeData() {
        new Thread(() -> {
            try {
                List<Song> songs = NeteaseCloudApi.INSTANCE.getTopNewSongs(0);
                synchronized (newSongs) { newSongs.clear(); newSongs.addAll(songs); }
                List<Playlist> pls = NeteaseCloudApi.INSTANCE.getRecommendPlaylists(6);
                synchronized (recommendPlaylists) { recommendPlaylists.clear(); recommendPlaylists.addAll(pls); }
                homeLoaded = true;
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void updateCaptchaCountdown() {
        if (captchaCountdown > 0) {
            long elapsed = (System.currentTimeMillis() - captchaStartTime) / 1000;
            captchaCountdown = Math.max(0, 60 - (int) elapsed);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        if (showPlayerDetail) {
            float closeBtnX = panelX + HyperionMetrics.PADDING, closeBtnY = panelY + HyperionMetrics.PADDING + 4;
            if (isHovered((int) mouseX, (int) mouseY, closeBtnX, closeBtnY, 22, 22)) { showPlayerDetail = false; return true; }
            float bottomY = panelY + panelHeight - 56;
            float sliderX = panelX + 24, sliderW = panelWidth - 48;
            if (isHovered((int) mouseX, (int) mouseY, sliderX, bottomY, sliderW, 6)) {
                float pct = (float) ((mouseX - sliderX) / sliderW);
                MediaPlayer.INSTANCE.seekToPercent(Math.max(0f, Math.min(1f, pct)));
                return true;
            }
            float ctrlY = bottomY + 26;
            float centerX = panelX + panelWidth / 2;
            if (isHovered((int) mouseX, (int) mouseY, centerX - 38, ctrlY - 2, 20, 20)) { prevClickAnim.from = 0; prevClickAnim.to = 0; prevClickAnim.get(1.0, 200L, Easing.CubicOut); MediaPlayer.INSTANCE.playPrevious(); return true; }
            if (isHovered((int) mouseX, (int) mouseY, centerX - 10, ctrlY - 5, 24, 24)) { playClickAnim.from = 0; playClickAnim.to = 0; playClickAnim.get(1.0, 200L, Easing.CubicOut); MediaPlayer.INSTANCE.toggle(); return true; }
            if (isHovered((int) mouseX, (int) mouseY, centerX + 20, ctrlY - 2, 20, 20)) { nextClickAnim.from = 0; nextClickAnim.to = 0; nextClickAnim.get(1.0, 200L, Easing.CubicOut); MediaPlayer.INSTANCE.playNext(); return true; }
            float volBarX = centerX + 56;
            if (isHovered((int) mouseX, (int) mouseY, volBarX + 26, ctrlY, 50, 14)) {
                float vol = (float) ((mouseX - volBarX - 26) / 50);
                MediaPlayer.INSTANCE.setVolume(Math.max(0f, Math.min(1f, vol)));
                return true;
            }
            return true;
        }

        float navStartY = panelY + HyperionMetrics.PADDING + 38;
        if (NeteaseCloudApi.INSTANCE.isLoggedIn && NeteaseCloudApi.INSTANCE.userProfile != null) navStartY += ClickGuiTextUtil.getHeight() + HyperionMetrics.PADDING;
        String[] navLabels = {"首页", "搜索", "喜欢"};
        int[] navPages = {0, 1, 2};
        float btnH = HyperionMetrics.ITEM_HEIGHT;
        for (int i = 0; i < navLabels.length; i++) {
            float btnY = navStartY + i * (btnH + 3);
            if (isHovered((int) mouseX, (int) mouseY, panelX + HyperionMetrics.PADDING * 2, btnY, sideBarW - HyperionMetrics.PADDING * 2, btnH)) {
                triggerClickAnim(i);
                prevPage = currentPage;
                currentPage = navPages[i];
                scrollOffset = 0;
                return true;
            }
        }

        float bottomBtnY = panelY + panelHeight - bottomBarH - HyperionMetrics.PADDING * 2 - HyperionMetrics.ITEM_HEIGHT;
        if (isHovered((int) mouseX, (int) mouseY, panelX + HyperionMetrics.PADDING * 2, bottomBtnY, sideBarW - HyperionMetrics.PADDING * 2, HyperionMetrics.ITEM_HEIGHT)) {
            triggerClickAnim(3);
            if (NeteaseCloudApi.INSTANCE.isLoggedIn) {
                NeteaseCloudApi.INSTANCE.logout();
                synchronized (likeSongs) { likeSongs.clear(); }
                likeLoaded = false;
            } else { currentPage = PAGE_LOGIN; }
            return true;
        }

        float cX = contentX + HyperionMetrics.PADDING;
        float cY = contentY + HyperionMetrics.PADDING;
        float cW = contentW - HyperionMetrics.PADDING * 2;
        float cH = contentH - HyperionMetrics.PADDING * 2;

        if (currentPage == PAGE_HOME) handleHomeClick((int) mouseX, (int) mouseY, cX, cY, cW);
        else if (currentPage == PAGE_SEARCH) handleSearchClick((int) mouseX, (int) mouseY, cX, cY, cW);
        else if (currentPage == PAGE_LIKE) handleLikeClick((int) mouseX, (int) mouseY, cX, cY + ClickGuiTextUtil.getHeight() + 8, cW);
        else if (currentPage == PAGE_LOGIN) handleLoginClick((int) mouseX, (int) mouseY, cX, cY, cW);

        float barY = panelY + panelHeight - bottomBarH + HyperionMetrics.PADDING;
        handlePlayerBarClick((int) mouseX, (int) mouseY, panelX + HyperionMetrics.PADDING, barY, panelWidth - HyperionMetrics.PADDING * 2);

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleHomeClick(int mx, int my, float x, float y, float w) {
        synchronized (newSongs) {
            float drawY = y + ClickGuiTextUtil.getHeight() + 8;
            for (int i = 0; i < Math.min(5, newSongs.size()); i++) {
                if (isHovered(mx, my, x, drawY, w, 28)) {
                    triggerClickAnim(i + 4);
                    int idx = i; Song song = newSongs.get(idx);
                    new Thread(() -> { MediaPlayer.INSTANCE.setPlaylist(new ArrayList<>(newSongs), idx); NeteaseCloudApi.INSTANCE.playSong(song); }).start();
                    return;
                }
                drawY += 30;
            }
            drawY += 17;
            synchronized (recommendPlaylists) {
                for (Playlist pl : recommendPlaylists) {
                    if (isHovered(mx, my, x, drawY, w, 26)) {
                        long plId = pl.id;
                        new Thread(() -> { List<Song> songs = NeteaseCloudApi.INSTANCE.getPlaylistDetail(plId); synchronized (newSongs) { newSongs.clear(); newSongs.addAll(songs); } }).start();
                        return;
                    }
                    drawY += 28;
                }
            }
            drawY += 17;
            for (int i = 5; i < Math.min(15, newSongs.size()); i++) {
                if (isHovered(mx, my, x, drawY, w, 28)) {
                    triggerClickAnim(i + 4);
                    int idx = i; Song song = newSongs.get(idx);
                    new Thread(() -> { MediaPlayer.INSTANCE.setPlaylist(new ArrayList<>(newSongs), idx); NeteaseCloudApi.INSTANCE.playSong(song); }).start();
                    return;
                }
                drawY += 28;
            }
        }
    }

    private void handleSearchClick(int mx, int my, float x, float y, float w) {
        float searchBarW = w - 70;
        searchFocused = isHovered(mx, my, x, y, searchBarW, 26);
        if (isHovered(mx, my, x + searchBarW + 8, y, 58, 26)) { triggerClickAnim(20); performSearch(); return; }
        float listY = y + 34 - scrollOffset;
        synchronized (searchResults) {
            for (int i = 0; i < searchResults.size(); i++) {
                float itemY = listY + i * 30;
                if (isHovered(mx, my, x, itemY, w, 28)) {
                    triggerClickAnim(i + 4);
                    int idx = i; Song song = searchResults.get(idx);
                    new Thread(() -> { MediaPlayer.INSTANCE.setPlaylist(new ArrayList<>(searchResults), idx); NeteaseCloudApi.INSTANCE.playSong(song); }).start();
                    return;
                }
            }
        }
    }

    private void handleLikeClick(int mx, int my, float x, float y, float w) {
        synchronized (likeSongs) {
            for (int i = 0; i < likeSongs.size(); i++) {
                float itemY = y + i * 30 - scrollOffset;
                if (isHovered(mx, my, x, itemY, w, 28)) {
                    triggerClickAnim(i + 4);
                    int idx = i; Song song = likeSongs.get(idx);
                    new Thread(() -> { MediaPlayer.INSTANCE.setPlaylist(new ArrayList<>(likeSongs), idx); NeteaseCloudApi.INSTANCE.playSong(song); }).start();
                    return;
                }
            }
        }
    }

    private void handleLoginClick(int mx, int my, float x, float y, float w) {
        float textH = ClickGuiTextUtil.getHeight();
        float tabY = y + textH + 12;
        for (int i = 0; i < 2; i++) {
            float tabX = x + w / 2 - 74 + i * 78;
            if (isHovered(mx, my, tabX, tabY, 70, 24)) {
                triggerClickAnim(40 + i);
                loginTab = i;
                phoneFocused = false;
                captchaFocused = false;
                if (loginTab == 1 && qrKey == null && !qrChecking) startQrLogin();
                return;
            }
        }
        if (loginTab == 0) {
            float phoneInputY = tabY + 34 + 10;
            float captchaInputY = phoneInputY + 34 + textH + 6;
            float captchaW = w - 88;
            float captchaBtnX = x + captchaW + 8;

            if (isHovered(mx, my, x, phoneInputY, w, 26)) {
                phoneFocused = true;
                captchaFocused = false;
                return;
            }

            if (isHovered(mx, my, x, captchaInputY, captchaW, 26)) {
                phoneFocused = false;
                captchaFocused = true;
                return;
            }

            if (isHovered(mx, my, captchaBtnX, captchaInputY, 76, 26)) {
                if (captchaCountdown <= 0 && phoneText.length() == 11) {
                    new Thread(() -> {
                        boolean success = NeteaseCloudApi.INSTANCE.sendCaptcha(phoneText, "86");
                        if (success) { captchaCountdown = 60; captchaStartTime = System.currentTimeMillis(); loginMessage = "验证码已发送"; }
                        else { loginMessage = "发送失败"; }
                    }).start();
                }
                return;
            }

            float loginBtnY = captchaInputY + 26 + 36 + (loginMessage.isEmpty() ? 0 : (int) textH + 6) + 4;
            if (isHovered(mx, my, x, loginBtnY, w, 30)) {
                triggerClickAnim(42);
                if (phoneText.length() == 11 && captchaText.length() >= 4) {
                    new Thread(() -> {
                        boolean success = NeteaseCloudApi.INSTANCE.loginWithCaptcha(phoneText, captchaText, "86");
                        loginMessage = success ? "登录成功" : "登录失败";
                    }).start();
                }
            }

            phoneFocused = false;
            captchaFocused = false;
        } else {
            if (qrStatusText.contains("过期")) resetQrLogin();
        }
    }

    private void handlePlayerBarClick(int mx, int my, float x, float y, float w) {
        float contentY = y + 18;
        if (MediaPlayer.INSTANCE.currentSong != null && isHovered(mx, my, x + 10, contentY, 90, 32)) { showPlayerDetail = true; return; }
        float centerX = x + w - 96;
        float btnY = contentY + 7;
        if (isHovered(mx, my, centerX, btnY, 20, 20)) { prevClickAnim.from = 0; prevClickAnim.to = 0; prevClickAnim.get(1.0, 200L, Easing.CubicOut); MediaPlayer.INSTANCE.playPrevious(); return; }
        if (isHovered(mx, my, centerX + 28, btnY - 2, 24, 24)) { playClickAnim.from = 0; playClickAnim.to = 0; playClickAnim.get(1.0, 200L, Easing.CubicOut); MediaPlayer.INSTANCE.toggle(); return; }
        if (isHovered(mx, my, centerX + 58, btnY, 20, 20)) { nextClickAnim.from = 0; nextClickAnim.to = 0; nextClickAnim.get(1.0, 200L, Easing.CubicOut); MediaPlayer.INSTANCE.playNext(); return; }
        if (isHovered(mx, my, x + 10, y + 5, w - 20, 4)) {
            float pct = (float) ((mx - x - 10) / (w - 20));
            MediaPlayer.INSTANCE.seekToPercent(Math.max(0f, Math.min(1f, pct)));
        }
    }

    private void performSearch() {
        if (searchText.isBlank()) return;
        new Thread(() -> {
            List<Song> results = NeteaseCloudApi.INSTANCE.search(searchText);
            synchronized (searchResults) { searchResults.clear(); searchResults.addAll(results); }
            scrollOffset = 0;
        }).start();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (showPlayerDetail) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { showPlayerDetail = false; return true; }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (currentPage == PAGE_SEARCH && searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) { if (!searchText.isEmpty()) searchText = searchText.substring(0, searchText.length() - 1); return true; }
            if (keyCode == GLFW.GLFW_KEY_ENTER) { performSearch(); return true; }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { searchFocused = false; return true; }
        }
        if (currentPage == PAGE_LOGIN && loginTab == 0) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (captchaFocused && !captchaText.isEmpty()) { captchaText = captchaText.substring(0, captchaText.length() - 1); return true; }
                else if (phoneFocused && !phoneText.isEmpty()) { phoneText = phoneText.substring(0, phoneText.length() - 1); return true; }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_TAB) {
                if (phoneFocused) { phoneFocused = false; captchaFocused = true; }
                else { captchaFocused = false; phoneFocused = true; }
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (showPlayerDetail) return super.charTyped(chr, modifiers);
        if (currentPage == PAGE_SEARCH && searchFocused) { searchText += chr; return true; }
        if (currentPage == PAGE_LOGIN && loginTab == 0) {
            if (Character.isDigit(chr)) {
                if (phoneFocused && phoneText.length() < 11) { phoneText += chr; return true; }
                else if (captchaFocused && captchaText.length() < 6) { captchaText += chr; return true; }
            }
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) (verticalAmount * 14)));
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void close() {
        if (!isClosing) {
            isClosing = true;
        } else {
            cleanupQrTexture();
            super.close();
        }
    }
}
