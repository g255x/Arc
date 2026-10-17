package dev.perish.mod.gui.config;

import dev.perish.Perish;
import dev.perish.mod.modules.impl.client.ConfigSwitcher;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ConfigSwitcherScreen extends Screen {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private static final int PANEL_W = 560;
    private static final int PANEL_H = 600;
    private static final int PADDING = 18;
    private static final int TOP_BAR_H = 42;
    private static final int SECTION_GAP = 18;
    private static final int LABEL_H = 18;
    private static final int PRESET_CARD_H = 134;
    private static final int FILE_LIST_H = 132;
    private static final int INPUT_BAR_H = 28;
    private static final int SEARCH_BAR_H = 26;
    private static final int BTN_H = 22;
    private static final int BTN_GAP = 6;

    private static final int C_BG          = 0xFF0A0A14;
    private static final int C_PANEL       = 0xF20E0E1C;
    private static final int C_DIVIDER     = 0x22B060FF;
    private static final int C_CARD        = 0xFF14142A;
    private static final int C_CARD_ACTIVE = 0xFF2A1F45;
    private static final int C_CARD_HOVER  = 0xFF1C1C36;
    private static final int C_ROW         = 0xFF14142A;
    private static final int C_ROW_HOVER   = 0xFF1E1E3A;
    private static final int C_ROW_ACTIVE  = 0xFF2A1F45;
    private static final int C_INPUT_BG    = 0xFF10102A;
    private static final int C_BTN         = 0xFFB060FF;
    private static final int C_BTN_HOVER   = 0xFFC080FF;
    private static final int C_BTN_DIS     = 0xFF3A2A55;
    private static final int C_BTN_WARN    = 0xFFCC6644;
    private static final int C_BTN_WARN_H  = 0xFFE08866;
    private static final int C_TEXT        = 0xFFECECFF;
    private static final int C_MUTED       = 0xFF9890C8;
    private static final int C_DIM         = 0xFF5A5275;
    private static final int C_ACCENT      = 0xFFB060FF;
    private static final int C_SUCCESS     = 0xFF55CC80;
    private static final int C_ERROR       = 0xFFEE6677;

    private int panelX, panelY;
    private int ticks = 0;
    private long openStart = 0L;
    private boolean closing = false;
    private long closeStart = 0L;

    private boolean dragging = false;
    private int dragOffsetX, dragOffsetY;

    private boolean p1NameFocused = false;
    private boolean p2NameFocused = false;
    private boolean switchKeyFocused = false;
    private boolean saveNameFocused = false;
    private String saveNameText = "";
    private String searchText = "";
    private boolean searchFocused = false;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    private int applyP1Hover = -1;
    private int applyP2Hover = -1;
    private boolean switchKeyHover = false;
    private boolean saveBtnHover = false;
    private boolean saveInputHover = false;
    private String message = "";
    private boolean messageOk = true;
    private int messageTick = 0;

    private int saveLabelY, saveContentY, saveContentBottom;
    private int presetLabelY, presetContentY, presetContentBottom;
    private int fileLabelY, fileContentY, fileContentBottom;
    private int keyLabelY, keyContentY, keyContentBottom;
    private int hintsY;

    private List<String> cachedFiles = new ArrayList<>();

    public ConfigSwitcherScreen() {
        super(Text.literal("ConfigPresetSwitcher"));
    }

    @Override
    protected void init() {
        super.init();
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        panelX = (sw - PANEL_W) / 2;
        panelY = (sh - PANEL_H) / 2;
        closing = false;
        openStart = System.currentTimeMillis();
        refreshFiles();
    }

    private void refreshFiles() {
        cachedFiles = ConfigSwitcher.INSTANCE.listPresetFiles();
    }

    private List<String> getFilteredFiles() {
        if (searchText.isEmpty()) return cachedFiles;
        List<String> f = new ArrayList<>();
        for (String s : cachedFiles) {
            if (s.toLowerCase().contains(searchText.toLowerCase())) f.add(s);
        }
        return f;
    }

    private float openProgress() {
        if (closing) {
            long d = System.currentTimeMillis() - closeStart;
            return Math.max(0f, 1f - d / 220f);
        }
        long d = System.currentTimeMillis() - openStart;
        return Math.min(1f, d / 280f);
    }

    private boolean hovered(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private void flashMessage(String msg, boolean ok) {
        this.message = msg;
        this.messageOk = ok;
        this.messageTick = 90;
    }

    private void layoutSections() {
        int y = panelY + TOP_BAR_H;
        saveLabelY = y;
        y += LABEL_H + 6;
        saveContentY = y;
        y += INPUT_BAR_H + SECTION_GAP;
        saveContentBottom = y - SECTION_GAP;
        presetLabelY = y;
        y += LABEL_H + 6;
        presetContentY = y;
        y += PRESET_CARD_H + SECTION_GAP;
        presetContentBottom = y - SECTION_GAP;
        fileLabelY = y;
        y += LABEL_H + 6;
        fileContentY = y;
        y += SEARCH_BAR_H + 6 + FILE_LIST_H + SECTION_GAP;
        fileContentBottom = y - SECTION_GAP;
        keyLabelY = y;
        y += LABEL_H + 6;
        keyContentY = y;
        y += INPUT_BAR_H + SECTION_GAP;
        keyContentBottom = y - SECTION_GAP;
        hintsY = y;
    }

    private void fillRoundRect(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (r <= 0) { ctx.fill(x, y, x + w, y + h, color); return; }
        int r2 = r * r;
        for (int dy = 0; dy < h; dy++) {
            if (dy < r) {
                int yd = r - dy;
                int xoff = (int) (r - Math.sqrt(Math.max(0, r2 - yd * yd)));
                ctx.fill(x + xoff, y + dy, x + w - xoff, y + dy + 1, color);
            } else if (dy >= h - r) {
                int yd = dy - (h - r - 1);
                int xoff = (int) (r - Math.sqrt(Math.max(0, r2 - yd * yd)));
                ctx.fill(x + xoff, y + dy, x + w - xoff, y + dy + 1, color);
            } else {
                ctx.fill(x, y + dy, x + w, y + dy + 1, color);
            }
        }
    }

    private void strokeRoundRect(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (r <= 0) {
            ctx.fill(x, y, x + w, y + 1, color);
            ctx.fill(x, y + h - 1, x + w, y + h, color);
            ctx.fill(x, y, x + 1, y + h, color);
            ctx.fill(x + w - 1, y, x + w, y + h, color);
            return;
        }
        if (r > w / 2) r = w / 2;
        if (r > h / 2) r = h / 2;
        int r2 = r * r;
        ctx.fill(x + r, y, x + w - r, y + 1, color);
        ctx.fill(x + r, y + h - 1, x + w - r, y + h, color);
        ctx.fill(x, y + r, x + 1, y + h - r, color);
        ctx.fill(x + w - 1, y + r, x + w, y + h - r, color);
        for (int a = 0; a <= r; a++) {
            int dx = a;
            int dy = (int) Math.round(Math.sqrt((double) Math.max(0, r2 - dx * dx)));
            ctx.fill(x + r - dx, y + r - dy, x + r - dx + 1, y + r - dy + 1, color);
            ctx.fill(x + w - r + dx, y + r - dy, x + w - r + dx + 1, y + r - dy + 1, color);
            ctx.fill(x + r - dx, y + h - r + dy, x + r - dx + 1, y + h - r + dy + 1, color);
            ctx.fill(x + w - r + dx, y + h - r + dy, x + w - r + dx + 1, y + h - r + dy + 1, color);
        }
    }

    private void drawXIcon(DrawContext ctx, int x, int y, int size, int color) {
        for (int i = 0; i < size; i++) {
            ctx.fill(x + i, y + i, x + i + 1, y + i + 1, color);
            ctx.fill(x + size - 1 - i, y + i, x + size - i, y + i + 1, color);
        }
    }

    private void drawSectionLabel(DrawContext ctx, int x, int y, String text, int alpha) {
        ctx.fill(x, y + 2, x + 2, y + 14, (alpha << 24) | (C_ACCENT & 0xFFFFFF));
        ctx.drawTextWithShadow(textRenderer, text, x + 8, y + 2, (alpha << 24) | (C_TEXT & 0xFFFFFF));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ticks++;
        if (closing) {
            if (openProgress() <= 0.02f) { super.close(); return; }
        }
        float op = openProgress();
        if (op <= 0f) return;

        int alpha = (int) (op * 255f);
        if (messageTick > 0) messageTick--;

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        ctx.fill(0, 0, sw, sh, ((alpha * 60 / 255) << 24));

        int bgColor = (alpha << 24) | (C_PANEL & 0x00FFFFFF);
        fillRoundRect(ctx, panelX, panelY, PANEL_W, PANEL_H, 4, bgColor);

        if (op > 0.5f) {
            int sa = (int) ((op - 0.5f) * 2f * 255f);
            int sb = (sa * 0x42 / 255) << 24 | 0xB060FF;
            strokeRoundRect(ctx, panelX, panelY, PANEL_W, PANEL_H, 4, sb);
        }

        int accentBarAlpha = (int) (op * 255f);
        ctx.fillGradient(panelX, panelY, panelX + PANEL_W, panelY + 3,
                (accentBarAlpha << 24) | 0xFF80FF, (accentBarAlpha << 24) | 0xB060FF);

        layoutSections();

        drawTopBar(ctx, mouseX, mouseY, alpha);
        drawSaveSection(ctx, mouseX, mouseY, alpha);
        drawPresetSection(ctx, mouseX, mouseY, alpha);
        drawFileSection(ctx, mouseX, mouseY, alpha);
        drawKeySection(ctx, mouseX, mouseY, alpha);
        drawHints(ctx, alpha);
    }

    private void drawTopBar(DrawContext ctx, int mx, int my, int alpha) {
        ConfigSwitcher mod = ConfigSwitcher.INSTANCE;
        ctx.drawTextWithShadow(textRenderer, "参数预设切换器", panelX + PADDING, panelY + 14, (alpha << 24) | (C_TEXT & 0xFFFFFF));

        String sub = "▸ " + mod.getCurrentPresetName() + " · " + mod.getCurrentPresetTarget();
        ctx.drawTextWithShadow(textRenderer, sub, panelX + PADDING + textRenderer.getWidth("参数预设切换器") + 12, panelY + 14,
                (alpha << 24) | (C_DIM & 0xFFFFFF));

        int cwSize = 18;
        int cwX = panelX + PANEL_W - PADDING - cwSize;
        int cwY = panelY + 12;
        boolean cwHov = hovered(mx, my, cwX, cwY, cwSize, cwSize);
        if (cwHov) fillRoundRect(ctx, cwX, cwY, cwSize, cwSize, 3, (alpha << 24) | (C_CARD_HOVER & 0xFFFFFF));
        drawXIcon(ctx, cwX + 4, cwY + 4, cwSize - 8, (alpha << 24) | (cwHov ? (C_ERROR & 0xFFFFFF) : (C_MUTED & 0xFFFFFF)));
    }

    private void drawSaveSection(DrawContext ctx, int mx, int my, int alpha) {
        int x = panelX + PADDING;
        int w = PANEL_W - PADDING * 2;
        drawSectionLabel(ctx, x, saveLabelY, "保存当前配置为新预设", alpha);

        int inputX = x;
        int inputY = saveContentY;
        int inputW = w - 110;
        int inputH = INPUT_BAR_H;
        saveInputHover = hovered(mx, my, inputX, inputY, inputW, inputH);
        int inputBg = saveNameFocused || saveInputHover
                ? (alpha << 24) | (C_CARD_HOVER & 0xFFFFFF)
                : (alpha << 24) | (C_INPUT_BG & 0xFFFFFF);
        fillRoundRect(ctx, inputX, inputY, inputW, inputH, 3, inputBg);
        if (saveNameFocused) {
            strokeRoundRect(ctx, inputX, inputY, inputW, inputH, 3, (alpha << 24) | (C_ACCENT & 0xFFFFFF));
        } else {
            strokeRoundRect(ctx, inputX, inputY, inputW, inputH, 3, (alpha << 24) | (0x33B060FF));
        }
        if (saveNameText.isEmpty() && !saveNameFocused) {
            ctx.drawTextWithShadow(textRenderer, "e.g. pvp / farm / build", inputX + 10, inputY + 10, (alpha << 24) | (C_DIM & 0xFFFFFF));
        } else {
            String show = saveNameText;
            while (textRenderer.getWidth(show) > inputW - 22 && show.length() > 1) show = show.substring(0, show.length() - 1);
            ctx.drawTextWithShadow(textRenderer, show, inputX + 10, inputY + 10, (alpha << 24) | (C_TEXT & 0xFFFFFF));
            if (saveNameFocused && (ticks / 26) % 2 == 0) {
                int cx = inputX + 10 + textRenderer.getWidth(show) + 1;
                ctx.fill(cx, inputY + 7, cx + 1, inputY + inputH - 7, (alpha << 24) | (C_TEXT & 0xFFFFFF));
            }
        }

        int btnX = inputX + inputW + 10;
        int btnW = 100;
        int btnY = inputY;
        saveBtnHover = hovered(mx, my, btnX, btnY, btnW, inputH);
        int btnColor = saveBtnHover ? (alpha << 24) | (C_BTN_HOVER & 0xFFFFFF) : (alpha << 24) | (C_BTN & 0xFFFFFF);
        fillRoundRect(ctx, btnX, btnY, btnW, inputH, 3, btnColor);
        String btnL = "+  SAVE";
        int blW = textRenderer.getWidth(btnL);
        ctx.drawTextWithShadow(textRenderer, btnL, btnX + btnW / 2 - blW / 2, btnY + 10, (alpha << 24) | 0xFFFFFF);

        if (messageTick > 0 && !message.isEmpty()) {
            int mcol = messageOk ? (C_SUCCESS & 0xFFFFFF) : (C_ERROR & 0xFFFFFF);
            ctx.drawTextWithShadow(textRenderer, message, x, inputY + inputH + 4, (alpha << 24) | mcol);
        }
    }

    private void drawPresetSection(DrawContext ctx, int mx, int my, int alpha) {
        int x = panelX + PADDING;
        int w = PANEL_W - PADDING * 2;
        drawSectionLabel(ctx, x, presetLabelY, "预设槽位", alpha);

        int cardW = (w - BTN_GAP) / 2;
        int p1X = x;
        int p2X = x + cardW + BTN_GAP;
        int cardY = presetContentY;
        ConfigSwitcher mod = ConfigSwitcher.INSTANCE;
        drawPresetCard(ctx, p1X, cardY, cardW, PRESET_CARD_H, 1,
                mod.preset1Name.getValue(), "options.txt", mod.currentPreset == 1, p1NameFocused,
                "options.txt (默认)", applyP1Hover, mx, my, alpha);
        drawPresetCard(ctx, p2X, cardY, cardW, PRESET_CARD_H, 2,
                mod.preset2Name.getValue(), mod.preset2File + ".cfg", mod.currentPreset == 2, p2NameFocused,
                null, applyP2Hover, mx, my, alpha);
    }

    private void drawPresetCard(DrawContext ctx, int x, int y, int w, int h, int idx, String name, String file,
                                boolean active, boolean nameFocused, String fileOverride,
                                int applyHover, int mx, int my, int alpha) {
        ConfigSwitcher mod = ConfigSwitcher.INSTANCE;
        int bg = active ? (alpha << 24) | (C_CARD_ACTIVE & 0xFFFFFF) : (alpha << 24) | (C_CARD & 0xFFFFFF);
        fillRoundRect(ctx, x, y, w, h, 4, bg);
        int borderCol = active ? (alpha << 24) | (C_ACCENT & 0xFFFFFF) : (alpha << 24) | (0x33B060FF);
        if (active) fillRoundRect(ctx, x, y, w, 2, 0, (alpha << 24) | (C_ACCENT & 0xFFFFFF));
        strokeRoundRect(ctx, x, y, w, h, 4, borderCol);

        ctx.drawTextWithShadow(textRenderer, "PRESET " + idx, x + 14, y + 10, (alpha << 24) | (active ? (C_ACCENT & 0xFFFFFF) : (C_DIM & 0xFFFFFF)));
        if (active) {
            int dotX = x + w - 16;
            int dotY = y + 15;
            ctx.fill(dotX - 3, dotY - 3, dotX + 3, dotY + 3, (alpha << 24) | (C_ACCENT & 0xFFFFFF));
            float pulse = (float) (Math.sin(ticks * 0.08) * 0.5 + 0.5);
            int haloA = (int) (alpha * (0.25f + pulse * 0.25f));
            for (int r = 1; r <= 4; r++) {
                int a = (int) (haloA * (1.0f - r / 4f));
                if (a < 0) a = 0;
                int c = (a << 24) | 0xB060FF;
                ctx.fill(dotX - 3 - r, dotY - 3 - r, dotX + 3 + r + 1, dotY + 3 + r + 1, c);
            }
        }

        ctx.drawTextWithShadow(textRenderer, "名字", x + 14, y + 28, (alpha << 24) | (C_DIM & 0xFFFFFF));
        int nameBoxY = y + 40;
        int nameBoxH = 20;
        boolean nameHov = hovered(mx, my, x + 14, nameBoxY, w - 28, nameBoxH);
        int nameBoxBg = nameFocused || nameHov ? (alpha << 24) | (C_CARD_HOVER & 0xFFFFFF) : (alpha << 24) | (C_INPUT_BG & 0xFFFFFF);
        fillRoundRect(ctx, x + 14, nameBoxY, w - 28, nameBoxH, 3, nameBoxBg);
        if (nameFocused) {
            strokeRoundRect(ctx, x + 14, nameBoxY, w - 28, nameBoxH, 3, (alpha << 24) | (C_ACCENT & 0xFFFFFF));
        }
        String nameDisp = name.isEmpty() ? "(empty)" : name;
        int maxW = w - 48;
        while (textRenderer.getWidth(nameDisp) > maxW && nameDisp.length() > 1) nameDisp = nameDisp.substring(0, nameDisp.length() - 1);
        if (!nameDisp.equals(name) && !nameDisp.equals("(empty)")) nameDisp = nameDisp.substring(0, Math.max(0, nameDisp.length() - 1)) + "…";
        ctx.drawTextWithShadow(textRenderer, nameDisp, x + 20, nameBoxY + 6, (alpha << 24) | (nameFocused ? (C_TEXT & 0xFFFFFF) : (C_MUTED & 0xFFFFFF)));
        if (nameFocused && (ticks / 26) % 2 == 0) {
            int cx = x + 20 + textRenderer.getWidth(nameDisp) + 1;
            ctx.fill(cx, nameBoxY + 4, cx + 1, nameBoxY + nameBoxH - 4, (alpha << 24) | (C_TEXT & 0xFFFFFF));
        }

        ctx.drawTextWithShadow(textRenderer, "文件", x + 14, y + 68, (alpha << 24) | (C_DIM & 0xFFFFFF));
        String fileStr = fileOverride != null ? fileOverride : (file.isEmpty() ? "(未选择)" : file);
        int fileCol = fileOverride != null ? (C_MUTED & 0xFFFFFF) : (file.isEmpty() ? (C_ERROR & 0xFFFFFF) : (C_ACCENT & 0xFFFFFF));
        int fileMaxW = w - 32;
        String fileDisp = fileStr;
        while (textRenderer.getWidth(fileDisp) > fileMaxW && fileDisp.length() > 1) fileDisp = fileDisp.substring(0, fileDisp.length() - 1);
        if (!fileDisp.equals(fileStr)) fileDisp = fileDisp.substring(0, Math.max(0, fileDisp.length() - 1)) + "…";
        ctx.drawTextWithShadow(textRenderer, fileDisp, x + 14, y + 80, (alpha << 24) | fileCol);

        int btnY = y + 98;
        int btnH = BTN_H;
        int btnX = x + 14;
        int btnW = w - 28;

        boolean canActivate = (idx == 1) || !mod.preset2File.isEmpty();
        int aHov = hovered(mx, my, btnX, btnY, btnW, btnH) ? 1 : 0;
        int aColor;
        if (!canActivate) aColor = (alpha << 24) | (C_BTN_DIS & 0xFFFFFF);
        else if (aHov == 1) aColor = (alpha << 24) | (C_BTN_HOVER & 0xFFFFFF);
        else aColor = (alpha << 24) | (C_BTN & 0xFFFFFF);
        fillRoundRect(ctx, btnX, btnY, btnW, btnH, 3, aColor);
        String aL = canActivate ? "▶ 激活" : "▶ 未选文件";
        ctx.drawTextWithShadow(textRenderer, aL, btnX + btnW / 2 - textRenderer.getWidth(aL) / 2, btnY + 7,
                (alpha << 24) | (canActivate ? 0xFFFFFF : (C_DIM & 0xFFFFFF)));

        if (idx == 1) {
            applyP1Hover = aHov == 1 ? 0 : -1;
        } else {
            applyP2Hover = aHov == 1 ? 0 : -1;
        }
    }

    private void drawFileSection(DrawContext ctx, int mx, int my, int alpha) {
        int x = panelX + PADDING;
        int w = PANEL_W - PADDING * 2;
        drawSectionLabel(ctx, x, fileLabelY, "选择预设2的文件", alpha);

        int barY = fileContentY;
        fillRoundRect(ctx, x, barY, w, SEARCH_BAR_H, 3, (alpha << 24) | (C_INPUT_BG & 0xFFFFFF));
        strokeRoundRect(ctx, x, barY, w, SEARCH_BAR_H, 3, (alpha << 24) | 0x33B060FF);
        ctx.drawTextWithShadow(textRenderer, "⌕", x + 10, barY + 9, (alpha << 24) | (searchFocused ? (C_ACCENT & 0xFFFFFF) : (C_DIM & 0xFFFFFF)));
        if (searchText.isEmpty() && !searchFocused) {
            ctx.drawTextWithShadow(textRenderer, "搜索...", x + 28, barY + 9, (alpha << 24) | (C_DIM & 0xFFFFFF));
        } else {
            String show = searchText;
            int maxW = w - 44;
            while (textRenderer.getWidth(show) > maxW && show.length() > 1) show = show.substring(0, show.length() - 1);
            ctx.drawTextWithShadow(textRenderer, show, x + 28, barY + 9, (alpha << 24) | (C_TEXT & 0xFFFFFF));
        }

        int listY = barY + SEARCH_BAR_H + 6;
        fillRoundRect(ctx, x, listY, w, FILE_LIST_H, 3, (alpha << 24) | (0xFF10102A & 0xFFFFFF));
        strokeRoundRect(ctx, x, listY, w, FILE_LIST_H, 3, (alpha << 24) | 0x22B060FF);

        List<String> files = getFilteredFiles();
        int rowH = 26;
        int pad = 4;
        int innerTop = listY + pad;
        int innerH = FILE_LIST_H - pad * 2;
        int visible = Math.max(1, innerH / rowH);
        maxScroll = Math.max(0, files.size() - visible);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        ctx.enableScissor(x, innerTop, x + w, innerTop + innerH);
        int startY = innerTop - scrollOffset * rowH;
        String selected = ConfigSwitcher.INSTANCE.preset2File;
        for (int i = 0; i < files.size(); i++) {
            String f = files.get(i);
            int ry = startY + i * rowH;
            if (ry + rowH < innerTop || ry > innerTop + innerH) continue;

            boolean isSel = f.equals(selected);
            boolean isHov = hovered(mx, my, x + 2, ry, w - 4, rowH);
            int rc;
            if (isSel) rc = (alpha << 24) | (C_ROW_ACTIVE & 0xFFFFFF);
            else if (isHov) rc = (alpha << 24) | (C_ROW_HOVER & 0xFFFFFF);
            else rc = 0;
            if (rc != 0) fillRoundRect(ctx, x + 2, ry, w - 4, rowH, 2, rc);
            if (isSel) ctx.fill(x + 2, ry + 5, x + 4, ry + rowH - 5, (alpha << 24) | (C_ACCENT & 0xFFFFFF));

            String mark = isSel ? "●" : "○";
            int mw = textRenderer.getWidth(mark);
            ctx.drawTextWithShadow(textRenderer, mark, x + w - 16 - mw, ry + (rowH - 8) / 2, (alpha << 24) | (isSel ? (C_ACCENT & 0xFFFFFF) : 0x665A5275));

            String label = f + ".cfg";
            int maxLW = w - 44;
            while (textRenderer.getWidth(label) > maxLW && label.length() > 1) label = label.substring(0, label.length() - 1);
            if (!label.equals(f + ".cfg")) label = label.substring(0, Math.max(0, label.length() - 1)) + "…";
            ctx.drawTextWithShadow(textRenderer, label, x + 16, ry + (rowH - 8) / 2, (alpha << 24) | (isSel ? (C_TEXT & 0xFFFFFF) : (C_MUTED & 0xFFFFFF)));
        }
        ctx.disableScissor();

        if (files.isEmpty()) {
            String[] lines = searchText.isEmpty()
                    ? new String[]{"还没有任何预设", "在上方输入名字并点 SAVE"}
                    : new String[]{"没有匹配", "试试别的关键词"};
            int lh = 12;
            int total = lines.length * lh;
            int sy = listY + FILE_LIST_H / 2 - total / 2;
            for (int i = 0; i < lines.length; i++) {
                String ln = lines[i];
                int lw2 = textRenderer.getWidth(ln);
                int lc = (i == 0 ? C_MUTED : C_DIM) & 0xFFFFFF;
                ctx.drawTextWithShadow(textRenderer, ln, x + w / 2 - lw2 / 2, sy + i * lh, (alpha << 24) | lc);
            }
        }
    }

    private void drawKeySection(DrawContext ctx, int mx, int my, int alpha) {
        int x = panelX + PADDING;
        drawSectionLabel(ctx, x, keyLabelY, "切换键", alpha);

        int boxX = x;
        int boxY = keyContentY;
        int boxW = 140;
        int boxH = INPUT_BAR_H;
        switchKeyHover = hovered(mx, my, boxX, boxY, boxW, boxH);
        int bg = switchKeyFocused ? (alpha << 24) | (C_ACCENT & 0xFFFFFF)
                : (switchKeyHover ? (alpha << 24) | (C_CARD_HOVER & 0xFFFFFF) : (alpha << 24) | (C_INPUT_BG & 0xFFFFFF));
        fillRoundRect(ctx, boxX, boxY, boxW, boxH, 3, bg);
        strokeRoundRect(ctx, boxX, boxY, boxW, boxH, 3,
                switchKeyFocused || switchKeyHover ? (alpha << 24) | (C_ACCENT & 0xFFFFFF) : (alpha << 24) | 0x33B060FF);

        ConfigSwitcher mod = ConfigSwitcher.INSTANCE;
        String kLabel = mod.switchKey.getValue() == -1 ? "NONE" : mod.switchKey.getKeyString();
        int kw = textRenderer.getWidth(kLabel);
        ctx.drawTextWithShadow(textRenderer, kLabel, boxX + boxW / 2 - kw / 2, boxY + 10, (alpha << 24) | (C_TEXT & 0xFFFFFF));
        if (switchKeyFocused && (ticks / 30) % 2 == 0) {
            ctx.fill(boxX + 10, boxY + 7, boxX + 12, boxY + boxH - 7, (alpha << 24) | 0xFFFFFF);
        }
    }

    private void drawHints(DrawContext ctx, int alpha) {
        int x = panelX + PADDING;
        String[] lines = {
                "▸ 「激活」切预设 (current.cfg + options 指向), 不覆盖内存",
                "▸ 临时切换不覆盖默认参数, 重启从 current.cfg 自动恢复"
        };
        for (int i = 0; i < lines.length; i++) {
            ctx.drawTextWithShadow(textRenderer, lines[i], x, hintsY + i * 11, (alpha << 24) | (C_MUTED & 0xFFFFFF));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int mx = (int) mouseX, my = (int) mouseY;
        p1NameFocused = false;
        p2NameFocused = false;
        switchKeyFocused = false;
        saveNameFocused = false;
        searchFocused = false;

        int cwSize = 18;
        int cwX = panelX + PANEL_W - PADDING - cwSize;
        int cwY = panelY + 12;
        if (hovered(mx, my, cwX, cwY, cwSize, cwSize)) { close(); return true; }

        if (hovered(mx, my, panelX, panelY, PANEL_W, TOP_BAR_H)) {
            dragging = true;
            dragOffsetX = (int) (mouseX - panelX);
            dragOffsetY = (int) (mouseY - panelY);
            return true;
        }

        int x = panelX + PADDING;
        int w = PANEL_W - PADDING * 2;

        if (hovered(mx, my, x, saveContentY, w - 110, INPUT_BAR_H)) { saveNameFocused = true; return true; }
        if (hovered(mx, my, x + w - 100, saveContentY, 100, INPUT_BAR_H)) {
            if (!saveNameText.trim().isEmpty()) {
                boolean ok = ConfigSwitcher.INSTANCE.saveCurrentAs(saveNameText);
                if (ok) { flashMessage("✓ 保存成功", true); saveNameText = ""; refreshFiles(); }
                else flashMessage("✗ 名称不合法", false);
            } else flashMessage("✗ 名称不能为空", false);
            return true;
        }

        int cardW = (w - BTN_GAP) / 2;
        int p1X = x;
        int p2X = p1X + cardW + BTN_GAP;
        int cardY = presetContentY;
        int nameBoxY = cardY + 40;
        if (hovered(mx, my, p1X + 14, nameBoxY, cardW - 28, 20)) { p1NameFocused = true; return true; }
        if (hovered(mx, my, p2X + 14, nameBoxY, cardW - 28, 20)) { p2NameFocused = true; return true; }

        int btnY = cardY + 98;
        int btnH = BTN_H;
        int btnW = cardW - 28;
        if (hovered(mx, my, p1X + 14, btnY, btnW, btnH)) {
            ConfigSwitcher.INSTANCE.applyPreset(1);
            flashMessage("✓ 激活: Default", true);
            return true;
        }
        if (hovered(mx, my, p2X + 14, btnY, btnW, btnH)) {
            if (!ConfigSwitcher.INSTANCE.preset2File.isEmpty()) {
                ConfigSwitcher.INSTANCE.applyPreset(2);
                flashMessage("✓ 激活: " + ConfigSwitcher.INSTANCE.preset2Name.getValue(), true);
            } else {
                flashMessage("✗ 预设2未选文件", false);
            }
            return true;
        }

        if (hovered(mx, my, x, fileContentY, w, SEARCH_BAR_H)) { searchFocused = true; return true; }
        int listY = fileContentY + SEARCH_BAR_H + 6;
        int rowH = 26;
        int innerTop = listY + 4;
        int innerH = FILE_LIST_H - 8;
        int startY = innerTop - scrollOffset * rowH;
        List<String> files = getFilteredFiles();
        for (int i = 0; i < files.size(); i++) {
            int ry = startY + i * rowH;
            if (ry + rowH < innerTop || ry > innerTop + innerH) continue;
            if (hovered(mx, my, x + 2, ry, w - 4, rowH)) {
                ConfigSwitcher.INSTANCE.setPreset2(files.get(i));
                return true;
            }
        }

        if (hovered(mx, my, x, keyContentY, 140, INPUT_BAR_H)) { switchKeyFocused = true; return true; }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging && button == 0) { panelX = (int) (mouseX - dragOffsetX); panelY = (int) (mouseY - dragOffsetY); return true; }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int x = panelX + PADDING;
        int w = PANEL_W - PADDING * 2;
        int listY = fileContentY + SEARCH_BAR_H + 6;
        if (hovered((int) mouseX, (int) mouseY, x, listY, w, FILE_LIST_H)) {
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) Math.signum(verticalAmount)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { this.close(); return true; }

        if (saveNameFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) { if (!saveNameText.isEmpty()) saveNameText = saveNameText.substring(0, saveNameText.length() - 1); return true; }
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                if (!saveNameText.trim().isEmpty()) {
                    boolean ok = ConfigSwitcher.INSTANCE.saveCurrentAs(saveNameText);
                    if (ok) { flashMessage("✓ 保存成功", true); saveNameText = ""; refreshFiles(); }
                    else flashMessage("✗ 名称不合法", false);
                }
                return true;
            }
        }

        if (switchKeyFocused) {
            ConfigSwitcher mod = ConfigSwitcher.INSTANCE;
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) { mod.switchKey.setValue(-1); Perish.save(); switchKeyFocused = false; return true; }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) { switchKeyFocused = false; return true; }
            mod.switchKey.setValue(keyCode); Perish.save(); switchKeyFocused = false;
            return true;
        }

        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) { if (!searchText.isEmpty()) searchText = searchText.substring(0, searchText.length() - 1); return true; }
        } else if (p1NameFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                String v = ConfigSwitcher.INSTANCE.preset1Name.getValue();
                if (!v.isEmpty()) ConfigSwitcher.INSTANCE.preset1Name.setValue(v.substring(0, v.length() - 1));
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER) { p1NameFocused = false; Perish.save(); return true; }
        } else if (p2NameFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                String v = ConfigSwitcher.INSTANCE.preset2Name.getValue();
                if (!v.isEmpty()) ConfigSwitcher.INSTANCE.preset2Name.setValue(v.substring(0, v.length() - 1));
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER) { p2NameFocused = false; Perish.save(); return true; }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (saveNameFocused) {
            if (chr >= 32 && chr != 127 && saveNameText.length() < 32) {
                char c = chr;
                if (c == ' ' || (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == '-') {
                    saveNameText += c;
                }
            }
            return true;
        }
        if (searchFocused) { if (chr >= 32 && searchText.length() < 64) searchText += chr; return true; }
        if (p1NameFocused) {
            String v = ConfigSwitcher.INSTANCE.preset1Name.getValue();
            if (chr >= 32 && v.length() < 32) ConfigSwitcher.INSTANCE.preset1Name.setValue(v + chr);
            return true;
        }
        if (p2NameFocused) {
            String v = ConfigSwitcher.INSTANCE.preset2Name.getValue();
            if (chr >= 32 && v.length() < 32) ConfigSwitcher.INSTANCE.preset2Name.setValue(v + chr);
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void close() {
        if (!closing) {
            closing = true;
            closeStart = System.currentTimeMillis();
            Perish.save();
            if (ConfigSwitcher.INSTANCE != null && ConfigSwitcher.INSTANCE.isOn()) {
                ConfigSwitcher.INSTANCE.disable();
            }
        } else {
            super.close();
        }
    }

    @Override
    public boolean shouldPause() { return false; }
}
