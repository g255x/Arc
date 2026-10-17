package dev.kissoo.mod.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.kissoo.Forever;
import dev.kissoo.api.utils.render.Render2DUtil;
import dev.kissoo.api.utils.skija.SkijaGlowRenderer;
import hoprc.api.core.impl.FontManager;
import hoprc.api.gui.fonts.FontRenderer;
import dev.kissoo.mod.gui.background.ShaderBackground;
import dev.kissoo.mod.gui.background.VideoBackground;
import dev.kissoo.mod.gui.background.PhobosBackground;
import dev.kissoo.mod.gui.background.MainMenuModePreference;
import dev.kissoo.mod.modules.impl.client.ClientSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.render.*;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundEvent;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import ru.vidtu.ias.screen.AccountScreen;
import java.awt.Color;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ForeverTitleScreen extends TitleScreen {
    private static final Identifier STARTUP_SOUND_ID = Identifier.of("forever", "startup");
    private static final SoundEvent STARTUP_SOUND = SoundEvent.of(STARTUP_SOUND_ID);

    private final List<ShaderMenuButton> shaderButtons = new ArrayList<>();
    private final List<VideoMenuButton> videoButtons = new ArrayList<>();

    private long lastRenderTime;
    private static boolean helloPlayed = false;
    private static boolean helloSoundPlayed = false;
    private static boolean phobosInitialFadePlayed = false;
    private HelloAnimation helloAnimation = null;

    private GlowElement dateElement;
    private GlowElement timeElement;
    private int lastMinute = -1;

    private static final float SLOPE = 0.15f;
    private static final float BLUR_RADIUS = 12f;
    private static final float BOX_PADDING_V = 10f;
    private static final float BTN_GAP = 40f;
    private static final float BOX_PADDING_X = 20f;

    public ForeverTitleScreen() {
        super(shouldDoPhobosInitialFade(), null);
    }

    private static boolean shouldDoPhobosInitialFade() {
        if (!MainMenuModePreference.isPhobosSelected()) {
            return false;
        }
        if (phobosInitialFadePlayed) {
            return false;
        }
        phobosInitialFadePlayed = true;
        return true;
    }

    @Override
    protected void init() {
        super.init();
        if (isPhobosModeSelected()) {
            this.lastRenderTime = System.nanoTime();
            this.helloAnimation = null;
            return;
        }

        this.clearChildren();
        this.lastRenderTime = System.nanoTime();

        if (SkijaGlowRenderer.isAvailable() && !helloPlayed) {
            try {
                helloAnimation = new HelloAnimation();
            } catch (Throwable t) {
                SkijaGlowRenderer.disable(t);
                helloAnimation = null;
                helloPlayed = true;
            }
        }

        initButtons();

        String dateString = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE.MMMM d", Locale.ENGLISH));
        dateElement = SkijaGlowRenderer.isAvailable() ? new GlowElement(dateString, 20f, 6f) : null;
    }

    private void initButtons() {
        shaderButtons.clear();
        videoButtons.clear();

        shaderButtons.add(new ShaderMenuButton(Text.literal("Singleplayer"), () -> this.client.setScreen(new SelectWorldScreen(this))));
        shaderButtons.add(new ShaderMenuButton(Text.literal("Multiplayer"), () -> this.client.setScreen(new MultiplayerScreen(this))));
        shaderButtons.add(new ShaderMenuButton(Text.literal("Options"), () -> this.client.setScreen(new OptionsScreen(this, this.client.options))));
        shaderButtons.add(new ShaderMenuButton(Text.literal("Quit Game"), () -> this.client.scheduleStop()));

        videoButtons.add(new VideoMenuButton("Single", () -> this.client.setScreen(new SelectWorldScreen(this))));
        videoButtons.add(new VideoMenuButton("Multi", () -> this.client.setScreen(new MultiplayerScreen(this))));
        videoButtons.add(new VideoMenuButton("Alts", () -> this.client.setScreen(new AccountScreen(this))));
        videoButtons.add(new VideoMenuButton("Optifines", () -> this.client.setScreen(new OptionsScreen(this, this.client.options))));
        videoButtons.add(new VideoMenuButton("Exit", () -> this.client.scheduleStop()));
    }

    @Override
    protected void renderPanoramaBackground(DrawContext context, float delta) {}

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int screenWidth  = this.width;
        int screenHeight = this.height;

        ClientSetting.MainMenuBg menuMode = ClientSetting.INSTANCE == null
                ? ClientSetting.MainMenuBg.SHADER
                : ClientSetting.INSTANCE.mainMenuBg.getValue();
        boolean isVideoMode = SkijaGlowRenderer.isAvailable() && menuMode == ClientSetting.MainMenuBg.VIDEO;
        boolean isPhobosMode = menuMode == ClientSetting.MainMenuBg.PHOBOS;

        if (isPhobosMode) {
            PhobosBackground phobos = PhobosBackground.getInstance();
            phobos.renderMenu(context);
            if (phobos.isFailed()) {
                ShaderBackground.getInstance().init();
                ShaderBackground.getInstance().render();
            }
            super.render(context, mouseX, mouseY, delta);
            return;
        }

        if (isVideoMode) {
            VideoBackground bg = VideoBackground.getInstance();
            if (!bg.isInitialized() && !bg.isLoading()) {
                bg.initAsync("/assets/forever/videos/mainmenu.mp4", "/assets/forever/mainmenu.mp4");
            }
            bg.render(context, screenWidth, screenHeight);
        } else {
            ShaderBackground.getInstance().init();
            ShaderBackground.getInstance().render();
        }

        long now = System.nanoTime();
        if (lastRenderTime == 0) lastRenderTime = now;
        float frameTime = (now - lastRenderTime) / 1_000_000_000f;
        lastRenderTime = now;
        frameTime = Math.min(frameTime, 0.05f);

        if (isVideoMode) {
            renderVideoUI(context, mouseX, mouseY, screenWidth, screenHeight, frameTime);
        } else {
            renderShaderUI(context, mouseX, mouseY, screenWidth, screenHeight, frameTime);
        }

        if (helloAnimation != null && !helloAnimation.isFinished()) {
            this.playHelloSoundOnce();
            helloAnimation.render(context.getMatrices(), screenWidth, screenHeight);
            if (helloAnimation.isFinished()) {
                helloPlayed = true;
                helloAnimation = null;
            }
        }
    }

    private void playHelloSoundOnce() {
        if (helloSoundPlayed || this.client == null) return;
        helloSoundPlayed = true;
        if (!LoginPreferences.isStartupSoundEnabled()) return;
        this.client.getSoundManager().play(
                PositionedSoundInstance.master(STARTUP_SOUND, 1.0F, 1.0F));
    }

    private void renderVideoUI(DrawContext context, int mouseX, int mouseY,
                               int screenWidth, int screenHeight, float frameTime) {
        context.fill(0, 0, screenWidth, screenHeight, new Color(0, 0, 0, 50).getRGB());
        LocalTime now = LocalTime.now();
        if (now.getMinute() != lastMinute) {
            lastMinute = now.getMinute();
            String timeStr = now.format(DateTimeFormatter.ofPattern("HH:mm"));
            if (timeElement != null) SkijaGlowRenderer.cleanupTexture(timeElement.id);
            timeElement = new GlowElement(timeStr, 65f, 12f);
        }

        if (!dateElement.updateSizes() || !timeElement.updateSizes()) return;
        float dateY = screenHeight / 2f - 130f;
        float dateDrawX = (screenWidth - dateElement.contentW) / 2f;
        dateElement.draw(context, dateDrawX, dateY, 1.0f, Color.WHITE, 2);

        float targetTimeWidth = dateElement.contentW * 0.78f;
        float timeScale      = targetTimeWidth / timeElement.contentW;
        float actualTimeWidth = timeElement.contentW * timeScale;
        float timeDrawX = (screenWidth - actualTimeWidth) / 2f;
        float timeY     = dateY + dateElement.contentH + 2f;
        timeElement.draw(context, timeDrawX, timeY, timeScale, Color.WHITE, 2);
        float totalBtnsWidth = 0;
        boolean allBtnsReady = true;
        for (VideoMenuButton btn : videoButtons) {
            if (!btn.glowElement.updateSizes()) {
                allBtnsReady = false;
                break;
            }
            totalBtnsWidth += btn.glowElement.contentW + BTN_GAP;
        }
        if (!allBtnsReady) return;
        totalBtnsWidth -= BTN_GAP;
        float maxContentH = 0;
        for (VideoMenuButton btn : videoButtons) {
            maxContentH = Math.max(maxContentH, btn.glowElement.contentH);
        }
        float boxHeight = maxContentH + BOX_PADDING_V * 2f;
        float boxWidth  = totalBtnsWidth + BOX_PADDING_X * 2f;
        float boxX      = (screenWidth - boxWidth) / 2f;
        float boxY      = screenHeight - 65f - boxHeight;
        try {
            float blurInset = 3f;
            Forever.BLUR.applyBlur(18f,
                    boxX + blurInset,
                    boxY + blurInset,
                    boxWidth - (blurInset * 2),
                    boxHeight - (blurInset * 2));
        } catch (Exception ignored) {}
        float currentX = boxX + BOX_PADDING_X;

        for (VideoMenuButton button : videoButtons) {
            boolean isHovering = button.isMouseOver(mouseX, mouseY);
            button.hoverAnimation = lerp(button.hoverAnimation, isHovering ? 1.0f : 0.0f, 18f * frameTime);

            float currentScale = 1.0f + 0.12f * button.hoverAnimation;

            Color normalColor = new Color(200, 200, 200, 255);
            Color hoverColor  = new Color(255, 255, 255, 255);
            Color color       = interpolateColorObj(normalColor, hoverColor, button.hoverAnimation);

            float baseW = button.glowElement.contentW;
            float baseH = button.glowElement.contentH;
            float startY = boxY + (boxHeight - baseH) / 2f;

            float scaleOffsetX = (baseW * currentScale - baseW) / 2.0f;
            float scaleOffsetY = (baseH * currentScale - baseH) / 2.0f;
            button.updateBounds(currentX, boxY, baseW, boxHeight);

            int passes = isHovering ? 3 : 1;
            button.glowElement.draw(
                    context,
                    currentX - scaleOffsetX,
                    startY  - scaleOffsetY,
                    currentScale,
                    color,
                    passes
            );

            currentX += baseW + BTN_GAP;
        }
    }
    private void renderShaderUI(DrawContext context, int mouseX, int mouseY,
                                int screenWidth, int screenHeight, float frameTime) {
        float textStartX = 40;
        FontRenderer titleFont  = FontManager.getMiSansFont(22);
        FontRenderer buttonFont = FontManager.getMiSansFont(13);

        float maxTextWidth = 0;
        if (titleFont != null) {
            maxTextWidth = Math.max(maxTextWidth, titleFont.getWidth("Forever Client"));
        } else {
            maxTextWidth = Math.max(maxTextWidth, this.client.textRenderer.getWidth("Forever Client") * 2.6f);
        }

        String versionText = "Forever 1.3.0";
        FontRenderer versionFont = FontManager.getMiSansFont(13);
        if (versionFont != null) {
            maxTextWidth = Math.max(maxTextWidth, versionFont.getWidth(versionText));
        }

        for (ShaderMenuButton button : shaderButtons) {
            float bw = buttonFont != null
                    ? buttonFont.getWidth(button.text.getString())
                    : this.client.textRenderer.getWidth(button.text);
            maxTextWidth = Math.max(maxTextWidth, bw * 1.15f);
        }

        float contentRight  = textStartX + maxTextWidth + 50;
        float lineTopX      = contentRight;
        float lineBottomX   = contentRight + screenHeight * SLOPE;

        try {
            Forever.BLUR.applyBlurTrapezoid(BLUR_RADIUS, screenWidth, screenHeight, lineTopX, lineBottomX);
        } catch (Exception e) {
            try { Forever.BLUR.applyBlur(BLUR_RADIUS, 0, 0, lineTopX, screenHeight); } catch (Exception ignored) {}
        }

        renderDiagonalOverlay(context, screenWidth, screenHeight, lineTopX, lineBottomX);
        renderDiagonalLine(context, screenHeight, lineTopX, lineBottomX);

        float startY = screenHeight / 4.0f - 50;
        MatrixStack matrices = context.getMatrices();

        if (titleFont != null) {
            titleFont.drawStringWithShadow(matrices, "Forever Client", textStartX, startY, -1);
            if (versionFont != null)
                versionFont.drawStringWithShadow(matrices, versionText, textStartX + 2, startY + 32, 0xFFAAAAAA);
        } else {
            matrices.push();
            matrices.translate(textStartX, startY, 0);
            matrices.scale(2.6f, 2.6f, 1.0f);
            context.drawText(this.client.textRenderer, "Forever Client", 0, 0, -1, true);
            matrices.pop();
            context.drawText(this.client.textRenderer, versionText,
                    (int) textStartX + 2, (int) (startY + 32), 0xFFAAAAAA, true);
        }

        float buttonStartY = screenHeight / 4.0f + 60;
        float gap = 38;

        for (ShaderMenuButton button : shaderButtons) {
            boolean isHovering = button.isMouseOver(mouseX, mouseY);
            button.hoverAnimation = lerp(button.hoverAnimation, isHovering ? 1.0f : 0.0f, 15f * frameTime);
            float currentScale = 1.0f + 0.06f * button.hoverAnimation;

            Color normalColor = new Color(170, 170, 170, 255);
            Color hoverColor  = new Color(255, 255, 255, 255);
            int   color       = interpolateColor(normalColor, hoverColor, button.hoverAnimation);

            float baseWidth  = buttonFont != null
                    ? buttonFont.getWidth(button.text.getString())
                    : this.client.textRenderer.getWidth(button.text);
            float baseHeight = buttonFont != null ? buttonFont.getFontHeight() : 9;

            button.updateBounds(textStartX, buttonStartY, baseWidth * currentScale, baseHeight * currentScale);

            matrices.push();
            float heightDiff = (baseHeight * currentScale - baseHeight) / 2.0f;
            matrices.translate(textStartX, buttonStartY - heightDiff, 0);
            matrices.scale(currentScale, currentScale, 1.0f);

            if (buttonFont != null) {
                buttonFont.drawStringWithShadow(matrices, button.text.getString(), 0, 0, color);
            } else {
                context.drawText(this.client.textRenderer, button.text, 0, 0, color, true);
            }
            matrices.pop();

            if (button.hoverAnimation > 0.01f) {
                float lineWidth = baseWidth * button.hoverAnimation;
                int   lineAlpha = (int) (80 * button.hoverAnimation);
                Render2DUtil.drawRect(matrices, textStartX, buttonStartY + baseHeight + 2,
                        lineWidth, 0.5f, new Color(255, 255, 255, lineAlpha).getRGB());
            }
            buttonStartY += gap;
        }
    }

    private void renderDiagonalOverlay(DrawContext context, int screenWidth, int screenHeight,
                                       float lineTopX, float lineBottomX) {
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        int aOpaque = 110;
        buffer.vertex(matrix, 0, 0, 0).color(0, 0, 0, aOpaque);
        buffer.vertex(matrix, 0, screenHeight, 0).color(0, 0, 0, aOpaque);
        buffer.vertex(matrix, lineBottomX - 1.5f, screenHeight, 0).color(0, 0, 0, aOpaque);
        buffer.vertex(matrix, lineTopX  - 1.5f, 0, 0).color(0, 0, 0, aOpaque);
        buffer.vertex(matrix, lineTopX  - 1.5f, 0, 0).color(0, 0, 0, aOpaque);
        buffer.vertex(matrix, lineBottomX - 1.5f, screenHeight, 0).color(0, 0, 0, aOpaque);
        buffer.vertex(matrix, lineBottomX + 1.0f, screenHeight, 0).color(0, 0, 0, 0);
        buffer.vertex(matrix, lineTopX  + 1.0f, 0, 0).color(0, 0, 0, 0);

        BuiltBuffer builtBuffer = buffer.endNullable();
        if (builtBuffer != null) BufferRenderer.drawWithGlobalProgram(builtBuffer);
        RenderSystem.disableBlend();
    }

    private void renderDiagonalLine(DrawContext context, int screenHeight,
                                    float lineTopX, float lineBottomX) {
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        int r = 200, g = 210, b = 220, aCore = 25, aEdge = 0;
        float w = 0.6f;
        buffer.vertex(matrix, lineTopX  - w, 0, 0).color(r, g, b, aEdge);
        buffer.vertex(matrix, lineBottomX - w, screenHeight, 0).color(r, g, b, aEdge);
        buffer.vertex(matrix, lineBottomX,     screenHeight, 0).color(r, g, b, aCore);
        buffer.vertex(matrix, lineTopX,        0, 0).color(r, g, b, aCore);
        buffer.vertex(matrix, lineTopX,        0, 0).color(r, g, b, aCore);
        buffer.vertex(matrix, lineBottomX,     screenHeight, 0).color(r, g, b, aCore);
        buffer.vertex(matrix, lineBottomX + w, screenHeight, 0).color(r, g, b, aEdge);
        buffer.vertex(matrix, lineTopX  + w, 0, 0).color(r, g, b, aEdge);

        BuiltBuffer builtBuffer = buffer.endNullable();
        if (builtBuffer != null) BufferRenderer.drawWithGlobalProgram(builtBuffer);
        RenderSystem.disableBlend();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isPhobosModeSelected()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (helloAnimation != null && !helloAnimation.isFinished()) return false;
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        boolean isVideoMode = SkijaGlowRenderer.isAvailable() &&
                ClientSetting.INSTANCE != null &&
                ClientSetting.INSTANCE.mainMenuBg.getValue() == ClientSetting.MainMenuBg.VIDEO;

        if (isVideoMode) {
            for (VideoMenuButton btn : videoButtons) {
                if (btn.isMouseOver(mouseX, mouseY)) {
                    btn.action.run();
                    this.client.getSoundManager().play(
                            PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    return true;
                }
            }
        } else {
            for (ShaderMenuButton btn : shaderButtons) {
                if (btn.isMouseOver(mouseX, mouseY)) {
                    btn.action.run();
                    this.client.getSoundManager().play(
                            PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isPhobosModeSelected() {
        return ClientSetting.INSTANCE != null
                && ClientSetting.INSTANCE.mainMenuBg.getValue() == ClientSetting.MainMenuBg.PHOBOS;
    }

    @Override
    public void removed() {
        super.removed();
        if (helloAnimation != null) {
            helloAnimation.cleanup();
            helloAnimation = null;
        }
    }
    private static class GlowElement {
        Identifier id;
        float logicalW, logicalH, logicalPad, contentW, contentH;

        GlowElement(String text, float fontSize, float glowRadius) {
            this.id = SkijaGlowRenderer.createGlowTexture(
                    text, fontSize, Color.WHITE, Color.WHITE, glowRadius, false, null);

            double scale = MinecraftClient.getInstance().getWindow().getScaleFactor();
            float physicalPad = Math.max((float) (glowRadius * scale) * 3.0f, 15.0f);
            this.logicalPad = (float) (physicalPad / scale);
        }

        boolean updateSizes() {
            if (!SkijaGlowRenderer.isTextureReady(id)) return false;
            int[] size = SkijaGlowRenderer.getTextureSize(id);
            if (size == null) return false;

            double scale = MinecraftClient.getInstance().getWindow().getScaleFactor();
            this.logicalW  = (float) (size[0] / scale);
            this.logicalH  = (float) (size[1] / scale);
            this.contentW  = this.logicalW - this.logicalPad * 2;
            this.contentH  = this.logicalH - this.logicalPad * 2;
            return true;
        }

        void draw(DrawContext context, float drawX, float drawY, float scale, Color color, int passes) {
            MatrixStack ms = context.getMatrices();
            ms.push();
            ms.translate(drawX, drawY, 0);
            ms.scale(scale, scale, 1.0f);
            ms.translate(-logicalPad, -logicalPad, 0);

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            context.setShaderColor(
                    color.getRed()   / 255f,
                    color.getGreen() / 255f,
                    color.getBlue()  / 255f,
                    color.getAlpha() / 255f);

            for (int i = 0; i < passes; i++) {
                context.drawTexture(id, 0, 0, 0, 0,
                        (int) Math.ceil(logicalW), (int) Math.ceil(logicalH),
                        (int) Math.ceil(logicalW), (int) Math.ceil(logicalH));
            }

            context.setShaderColor(1, 1, 1, 1);
            ms.pop();
        }
    }



    private float lerp(float current, float target, float speed) {
        return current + (target - current) * speed;
    }

    private int interpolateColor(Color color1, Color color2, float fraction) {
        fraction = MathHelper.clamp(fraction, 0f, 1f);
        int r = (int) (color1.getRed()   + (color2.getRed()   - color1.getRed())   * fraction);
        int g = (int) (color1.getGreen() + (color2.getGreen() - color1.getGreen()) * fraction);
        int b = (int) (color1.getBlue()  + (color2.getBlue()  - color1.getBlue())  * fraction);
        int a = (int) (color1.getAlpha() + (color2.getAlpha() - color1.getAlpha()) * fraction);
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    private Color interpolateColorObj(Color color1, Color color2, float fraction) {
        fraction = MathHelper.clamp(fraction, 0f, 1f);
        int r = (int) (color1.getRed()   + (color2.getRed()   - color1.getRed())   * fraction);
        int g = (int) (color1.getGreen() + (color2.getGreen() - color1.getGreen()) * fraction);
        int b = (int) (color1.getBlue()  + (color2.getBlue()  - color1.getBlue())  * fraction);
        int a = (int) (color1.getAlpha() + (color2.getAlpha() - color1.getAlpha()) * fraction);
        return new Color(r, g, b, a);
    }
    private static class ShaderMenuButton {
        final Text text;
        final Runnable action;
        float x, y, width, height;
        float hoverAnimation = 0f;

        ShaderMenuButton(Text text, Runnable action) {
            this.text   = text;
            this.action = action;
        }

        void updateBounds(float x, float y, float width, float height) {
            this.x = x; this.y = y; this.width = width; this.height = height;
        }

        boolean isMouseOver(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }

    private static class VideoMenuButton {
        final Runnable action;
        float x, y, width, height;
        float hoverAnimation = 0f;
        GlowElement glowElement;

        VideoMenuButton(String text, Runnable action) {
            this.action      = action;
            this.glowElement = new GlowElement(text, 14f, 5f);
        }

        void updateBounds(float x, float y, float width, float height) {
            this.x = x; this.y = y; this.width = width; this.height = height;
        }

        boolean isMouseOver(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }
}
