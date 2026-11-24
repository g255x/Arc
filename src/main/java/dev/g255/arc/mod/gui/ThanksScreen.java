package dev.g255.arc.mod.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.g255.arc.Arc;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Arc 的鸣谢页（点主菜单左下角的版本号进来）。
 *
 * <p>版式照原版 {@code CreditsAndAttributionScreen}：主页面是标题 + 三个分栏按钮
 * （鸣谢名单 / 著作权说明 / 许可协议）+ 底部「完成」；点进分栏后只有「返回」。
 *
 * <p>鸣谢名单读 {@code assets/arc/thanks/list.json}（数组或单对象都兼容），
 * 每人一张卡片：头像 / 名字 / 描述，从左到右、从上到下排。
 *
 * <p>头像必须放在 {@code assets/arc/textures/thanks/} 下 —— 只有 {@code textures/} 目录里的图
 * 才会被贴图管理器自动加载，放在 {@code thanks/} 里 drawTexture 会找不到、什么都不画。
 */
public class ThanksScreen extends Screen {
    private static final int VIEW_MAIN = 0;
    private static final int VIEW_THANKS = 1;
    private static final int VIEW_COPYRIGHT = 2;
    private static final int VIEW_LICENSE = 3;

    private static final String REPO_URL = "https://github.com/g255x/Arc";

    /** 跟随游戏语言：只有中文客户端用中文，其余一律英文 */
    private static boolean chinese() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) {
            return false;
        }
        String lang = client.options.language;
        return lang != null && lang.toLowerCase().startsWith("zh");
    }

    private static String t(String zh, String en) {
        return chinese() ? zh : en;
    }

    private static String mainTitle() {
        return t("鸣谢与著作权说明", "Credits & Attribution");
    }

    /**
     * 仓库链接那行，和原版一样用 {@link Style} 表达超链接：
     * 前半段普通文本，后半段带 {@code ClickEvent.OpenUrl} + 下划线 + hover 提示。
     * 下划线由 {@code TextRenderer} 按 style 自己画，不需要手动画线、也不自己改颜色。
     */
    private static Text repoLink() {
        Style style = Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, REPO_URL))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(REPO_URL)))
                .withUnderline(true);
        return Text.literal(t("本项目以 MIT 许可证开源  ", "This project is MIT-licensed  "))
                .append(Text.literal(REPO_URL).setStyle(style));
    }

    private static final int COLOR_TITLE = 0xFFFFFFFF;
    private static final int COLOR_SECTION = 0xFFFFD166;
    private static final int COLOR_BODY = 0xFFC8CDD4;
    private static final int COLOR_DIM = 0xFF8A9099;

    private static final int CARD_W = 150;
    private static final int CARD_H = 96;
    private static final int CARD_GAP = 12;
    private static final int AVATAR = 40;

    private static final String[] PROJECTS = new String[]{
            "[MIT] Deepseek Client - Developed by beyond627",
            "[MIT] Frog Client v1.04 - Developed by IDhammaI",
            "[GNU] LeavesHack - Developed by Leaves_awa",
            "[GNU] Alien-Nightly - Developed by ItzLuminous & NotXiguaJerry",
            "[MIT] Astra-Daily - Developed by G255 & エロ死刑!£ ぬ & 时音_infrasound",
            "[???] Forever - Developed by kiss_O_o & saobiHoPRC",
            "[???] OpenHachimi - Developed by Hachimi.me"
    };

    /** 原版 MIT 协议全文 */
    private static final String[] LICENSE_LINES = new String[]{
            "MIT License",
            "",
            "Copyright (c) 2025-2026 G255",
            "",
            "Permission is hereby granted, free of charge, to any person obtaining a copy",
            "of this software and associated documentation files (the \"Software\"), to deal",
            "in the Software without restriction, including without limitation the rights",
            "to use, copy, modify, merge, publish, distribute, sublicense, and/or sell",
            "copies of the Software, and to permit persons to whom the Software is",
            "furnished to do so, subject to the following conditions:",
            "",
            "The above copyright notice and this permission notice shall be included in all",
            "copies or substantial portions of the Software.",
            "",
            "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR",
            "IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,",
            "FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE",
            "AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER",
            "LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,",
            "OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE",
            "SOFTWARE."
    };

    /** 只加载一次，页面来回开关不用反复读盘 */
    private static List<Contributor> contributors;
    private static boolean loadAttempted;

    private final Screen parent;
    private int view = VIEW_MAIN;
    private int scrollOffset;
    private int contentHeight;
    /** 「开源及协议」那一行在屏幕上的矩形 */
    private int linkY;
    private int linkH;

    public ThanksScreen(Screen parent) {
        super(Text.literal(mainTitle()));
        this.parent = parent;
    }

    // ---------------------------------------------------------------- 数据

    private static final class Contributor {
        final String name;
        final String description;
        final Identifier texture;
        /** 40×40 的 ARGB 像素（已从原图最近邻缩放），画的时候直接 fill，不走纹理系统 */
        int[] pixels;
        boolean registered;

        Contributor(String name, String description, String profile) {
            this.name = name == null ? "" : name;
            this.description = description == null ? "" : description;
            Identifier id = null;
            if (profile != null && !profile.isEmpty()) {
                try {
                    // 图片放在 assets/arc/thanks/ 下（不是 textures/ 的子目录），
                    // 所以不能指望资源包扫描，必须自己读进来注册成纹理（见 measureTexture）
                    id = Identifier.of("arc", "thanks/" + profile);
                } catch (Exception ignored) {
                    id = null;
                }
            }
            this.texture = id;
        }
    }

    private void ensureLoaded() {
        if (loadAttempted) {
            return;
        }
        contributors = new ArrayList<>();
        if (this.client == null) {
            return;
        }
        Identifier listId = Identifier.of("arc", "thanks/list.json");
        var resource = this.client.getResourceManager().getResource(listId);
        if (resource.isEmpty()) {
            return;
        }
        try (InputStreamReader reader = new InputStreamReader(resource.get().getInputStream(), StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (root.isJsonArray()) {
                JsonArray array = root.getAsJsonArray();
                for (JsonElement element : array) {
                    this.addContributor(element);
                }
            } else if (root.isJsonObject()) {
                this.addContributor(root);
            }
        } catch (Exception e) {
            // 文件写坏时名单会是空的；控制台留一行方便定位（界面上不打扰玩家）
            System.out.println("[Arc] thanks/list.json parse failed: " + e);
            return;
        }
        // 只有真的读到条目才锁定；否则下次进页面重试 —— 手改完 JSON 不用重启客户端
        if (!contributors.isEmpty()) {
            loadAttempted = true;
        }
        for (Contributor contributor : contributors) {
            this.measureTexture(contributor);
        }
    }

    private void addContributor(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return;
        }
        JsonObject object = element.getAsJsonObject();
        String name = optString(object, "name");
        String description = optString(object, "description");
        String profile = optString(object, "profile");
        if (name.isEmpty() && profile.isEmpty()) {
            return;
        }
        contributors.add(new Contributor(name, description, profile));
    }

    private static String optString(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        try {
            return object.get(key).getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }

    /**
     * 把头像解码成 40×40 的像素表，绘制时直接 {@code context.fill} 逐行画。
     *
     * <p>不再走 {@code drawTexture}：纹理注册、UV、Sampler 状态这条路已经试过两轮，
     * 结果都是「注册成功但画出来是空的」。GUI 的 fill 走的是 position_color 那条管线，
     * 和卡片底色完全一样，只要像素数据读到了就一定能显示。
     */
    private void measureTexture(Contributor contributor) {
        if (contributor.texture == null || this.client == null || contributor.registered) {
            return;
        }
        var resource = this.client.getResourceManager().getResource(contributor.texture);
        if (resource.isEmpty()) {
            return;
        }
        byte[] raw;
        try (var stream = resource.get().getInputStream()) {
            raw = stream.readAllBytes();
        } catch (Throwable t) {
            return;
        }
        // PNG 走 MC 自带的 NativeImage；JPEG 它读不了（会抛异常），退回 ImageIO 解一遍。
        int[] px = scaleByNativeImage(raw);
        if (px == null) {
            px = scaleByImageIo(raw);
        }
        if (px == null) {
            // PNG 和 JPEG 都没解出来：控制台留一行，界面上退回首字母
            System.out.println("[Arc] avatar decode failed: " + contributor.texture);
            return;
        }
        contributor.pixels = px;
        contributor.registered = true;
    }

    /** 用 MC 的 NativeImage 解码（PNG）。失败返回 null。 */
    private int[] scaleByNativeImage(byte[] raw) {
        try (NativeImage image = NativeImage.read(new ByteArrayInputStream(raw))) {
            int sw = image.getWidth();
            int sh = image.getHeight();
            if (sw <= 0 || sh <= 0) {
                return null;
            }
            int[] px = new int[AVATAR * AVATAR];
            for (int ty = 0; ty < AVATAR; ++ty) {
                int sy = ty * sh / AVATAR;
                for (int tx = 0; tx < AVATAR; ++tx) {
                    int sx = tx * sw / AVATAR;
                    // NativeImage.getColor 返回 ABGR，这里换成 ARGB
                    int abgr = image.getColor(sx, sy);
                    int a = (abgr >>> 24) & 0xFF;
                    int b = (abgr >>> 16) & 0xFF;
                    int g = (abgr >>> 8) & 0xFF;
                    int r = abgr & 0xFF;
                    px[ty * AVATAR + tx] = (a << 24) | (r << 16) | (g << 8) | b;
                }
            }
            return px;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** 退回 ImageIO 解码（JPEG 用这条）。BufferedImage.getRGB 本身就是 ARGB。失败返回 null。 */
    private int[] scaleByImageIo(byte[] raw) {
        try {
            java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(new ByteArrayInputStream(raw));
            if (image == null) {
                return null;
            }
            int sw = image.getWidth();
            int sh = image.getHeight();
            if (sw <= 0 || sh <= 0) {
                return null;
            }
            int[] px = new int[AVATAR * AVATAR];
            for (int ty = 0; ty < AVATAR; ++ty) {
                int sy = ty * sh / AVATAR;
                for (int tx = 0; tx < AVATAR; ++tx) {
                    int sx = tx * sw / AVATAR;
                    px[ty * AVATAR + tx] = image.getRGB(sx, sy);
                }
            }
            return px;
        } catch (Throwable ignored) {
            return null;
        }
    }

    // ---------------------------------------------------------------- 布局

    private int perRow() {
        int usable = Math.max(CARD_W, this.width - 48);
        return Math.max(1, (usable + CARD_GAP) / (CARD_W + CARD_GAP));
    }

    private int cardRows() {
        int count = contributors == null ? 0 : contributors.size();
        return count == 0 ? 0 : (count + this.perRow() - 1) / this.perRow();
    }

    private void setView(int newView) {
        this.view = newView;
        this.scrollOffset = 0;
        this.clearAndInit();
    }

    // ---------------------------------------------------------------- 按钮

    @Override
    protected void init() {
        this.ensureLoaded();
        // 上次可能因为资源还没同步而没注册上，这里补一次（成功的会跳过）
        if (contributors != null) {
            for (Contributor contributor : contributors) {
                this.measureTexture(contributor);
            }
        }
        this.scrollOffset = 0;
        int cx = this.width / 2;
        int buttonW = 200;
        int buttonH = 20;
        int gap = 6;
        int doneY = this.height - 40;

        if (this.view == VIEW_MAIN) {
            // 和原版 CreditsAndAttributionScreen 一样：按钮组在屏幕上方约 15% 处，底部单独一个「完成」
            int y = Math.max(80, this.height * 15 / 100);
            this.addDrawableChild(ButtonWidget.builder(Text.literal(t("鸣谢名单", "Credits")),
                    b -> this.setView(VIEW_THANKS)).dimensions(cx - buttonW / 2, y, buttonW, buttonH).build());
            this.addDrawableChild(ButtonWidget.builder(Text.literal(t("著作权说明", "Copyright")),
                    b -> this.setView(VIEW_COPYRIGHT)).dimensions(cx - buttonW / 2, y + buttonH + gap, buttonW, buttonH).build());
            this.addDrawableChild(ButtonWidget.builder(Text.literal(t("许可协议", "License")),
                    b -> this.setView(VIEW_LICENSE)).dimensions(cx - buttonW / 2, y + (buttonH + gap) * 2, buttonW, buttonH).build());
            this.addDrawableChild(ButtonWidget.builder(Text.literal(t("完成", "Done")),
                    b -> this.close()).dimensions(cx - 100, doneY, 200, buttonH).build());
        } else {
            // 分栏页只有「返回」，回主页面
            this.addDrawableChild(ButtonWidget.builder(Text.literal(t("返回", "Back")),
                    b -> this.setView(VIEW_MAIN)).dimensions(cx - 100, doneY, 200, buttonH).build());
        }
    }

    // ---------------------------------------------------------------- 渲染

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // 原版点 Mojang 进的是「模糊 + 暗色蒙版」的界面：这里保留极光，再套同一层蒙版
        // 和主菜单同一套极光背景，再压一层暗色蒙版（原版 credits 页那种“背景变暗”的观感）
        MenuBackground.renderScreen(context, this.width, this.height);
        context.fill(0, 0, this.width, this.height, 0x88101218);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        switch (this.view) {
            case VIEW_THANKS -> this.renderThanks(context);
            case VIEW_COPYRIGHT -> this.renderCopyright(context, mouseX, mouseY);
            case VIEW_LICENSE -> this.renderLicense(context);
            default -> this.renderMain(context);
        }
    }

    private void renderMain(DrawContext context) {
        int centerX = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(mainTitle()), centerX, 30, COLOR_TITLE);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Arc " + Arc.VERSION), centerX, 46, COLOR_DIM);
    }

    private void renderThanks(DrawContext context) {
        int centerX = this.width / 2;
        int y = 24 - this.scrollOffset;
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(t("鸣谢名单", "Credits")), centerX, y, COLOR_TITLE);
        y += 26;

        int perRow = this.perRow();
        int cardsWidth = perRow * CARD_W + (perRow - 1) * CARD_GAP;
        int startX = centerX - cardsWidth / 2;

        if (contributors == null || contributors.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(t("（list.json 里还没有条目）", "(no entries in list.json)")), centerX, y + 8, COLOR_DIM);
            y += 30;
        } else {
            for (int i = 0; i < contributors.size(); ++i) {
                int row = i / perRow;
                int col = i % perRow;
                this.drawCard(context, contributors.get(i), startX + col * (CARD_W + CARD_GAP), y + row * (CARD_H + CARD_GAP));
            }
            y += this.cardRows() * (CARD_H + CARD_GAP);
        }
        this.contentHeight = y + 60;
    }

    private void renderCopyright(DrawContext context, int mouseX, int mouseY) {
        int centerX = this.width / 2;
        int y = 24 - this.scrollOffset;
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(t("著作权说明", "Copyright")), centerX, y, COLOR_TITLE);
        y += 26;

        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(t("开源及协议", "Open Source & License")), centerX, y, COLOR_SECTION);
        y += 16;
        Text link = repoLink();
        int linkWidth = this.textRenderer.getWidth(link);
        int linkX = centerX - linkWidth / 2;
        // 原版就是这样画的：白字 + style 自带下划线，不做 hover 变色
        context.drawTextWithShadow(this.textRenderer, link, linkX, y, 0xFFFFFFFF);
        this.linkY = y;
        this.linkH = this.textRenderer.fontHeight + 2;
        y += 26;

        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(t("涉及到的开源项目", "Open Source Projects")), centerX, y, COLOR_SECTION);
        y += 16;
        for (String project : PROJECTS) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(project), centerX, y, COLOR_BODY);
            y += 12;
        }
        this.contentHeight = y + 60;
    }

    private void renderLicense(DrawContext context) {
        int centerX = this.width / 2;
        int y = 20 - this.scrollOffset;
        for (String line : LICENSE_LINES) {
            if (line.isEmpty()) {
                y += 6;
                continue;
            }
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(line), centerX, y, COLOR_BODY);
            y += 11;
        }
        this.contentHeight = y + 60;
    }

    private void drawCard(DrawContext context, Contributor contributor, int x, int y) {
        context.fill(x, y, x + CARD_W, y + CARD_H, 0x66000000);
        context.fill(x, y, x + CARD_W, y + 1, 0x334FC3F7);
        context.fill(x, y + CARD_H - 1, x + CARD_W, y + CARD_H, 0x334FC3F7);

        int avatarX = x + (CARD_W - AVATAR) / 2;
        int avatarY = y + 8;
        context.fill(avatarX - 1, avatarY - 1, avatarX + AVATAR + 1, avatarY + AVATAR + 1, 0x40FFFFFF);
        if (contributor.pixels != null) {
            // 按行合并同色像素，避免 1600 次 fill
            for (int row = 0; row < AVATAR; ++row) {
                int startCol = 0;
                int color = contributor.pixels[row * AVATAR];
                for (int col = 1; col <= AVATAR; ++col) {
                    int next = col < AVATAR ? contributor.pixels[row * AVATAR + col] : 0;
                    if (col < AVATAR && next == color) {
                        continue;
                    }
                    if ((color >>> 24) > 8) {
                        context.fill(avatarX + startCol, avatarY + row, avatarX + col, avatarY + row + 1, color);
                    }
                    startCol = col;
                    color = next;
                }
            }
        } else {
            // 解码失败时至少画个首字母，不是一块空灰
            String initial = contributor.name.isEmpty() ? "?" : contributor.name.substring(0, 1).toUpperCase();
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(initial),
                    avatarX + AVATAR / 2, avatarY + AVATAR / 2 - 4, 0xFFFFFFFF);
        }

        int nameY = avatarY + AVATAR + 6;
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(contributor.name),
                x + CARD_W / 2, nameY, COLOR_TITLE);
        if (!contributor.description.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(contributor.description),
                    x + CARD_W / 2, nameY + 11, COLOR_DIM);
        }
    }

    // ---------------------------------------------------------------- 输入

    private boolean isInsideLink(double mouseX, double mouseY, int y) {
        int linkWidth = this.textRenderer.getWidth(repoLink());
        int linkX = this.width / 2 - linkWidth / 2;
        return mouseX >= linkX - 6 && mouseX <= linkX + linkWidth + 6
                && mouseY >= y - 2 && mouseY <= y + this.textRenderer.fontHeight + 2;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.view == VIEW_COPYRIGHT && this.isInsideLink(mouseX, mouseY, this.linkY)) {
            // 原版点击带 ClickEvent 的文本就是走 handleTextClick
            this.handleTextClick(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, REPO_URL)));
            return true;
        }
        if (button == 1) {
            if (this.view == VIEW_MAIN) {
                this.close();
            } else {
                this.setView(VIEW_MAIN);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int max = Math.max(0, this.contentHeight - (this.height - 70));
        this.scrollOffset = (int)Math.max(0, Math.min(max, this.scrollOffset - verticalAmount * 18));
        return true;
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
