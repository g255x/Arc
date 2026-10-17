package dev.kissoo.mod.modules.impl.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.kissoo.api.utils.skija.SkijaGlowRenderer;
import hoprc.api.core.impl.FontManager;
import hoprc.api.gui.fonts.FontRenderer;
import dev.kissoo.mod.modules.HudElement;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import dev.kissoo.mod.modules.settings.impl.StringSetting;
import dev.kissoo.mod.modules.impl.client.Fonts.FontMode;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.awt.*;

public class WaterMarkHud extends Module implements HudElement {
    public static WaterMarkHud INSTANCE;
    private static final Identifier WURST_TEXTURE = Identifier.of("forever", "textures/images/wurst_128.png");
    private static final float WURST_LOGO_WIDTH = 72.0f;
    private static final float WURST_LOGO_HEIGHT = 18.0f;
    private static final String WURST_VERSION = "v7.44 MC1.20";

    private int hudX = 2, hudY = 2;

    public final EnumSetting<Mode> mode = add(new EnumSetting<>("Mode", Mode.Custom));
    public final EnumSetting<ForeverFontMode> foreverFont = add(new EnumSetting<>("ForeverFont", ForeverFontMode.Default, () -> mode.is(Mode.Forever)));
    public final SliderSetting glitchInterval = add(new SliderSetting("GlitchInterval", 1050.0, 200.0, 3000.0, 50.0, () -> mode.is(Mode.Forever)));
    public final SliderSetting glitchDuration = add(new SliderSetting("GlitchDuration", 230.0, 50.0, 1000.0, 10.0, () -> mode.is(Mode.Forever)));
    public final SliderSetting jitterInterval = add(new SliderSetting("JitterInterval", 25.0, 10.0, 200.0, 5.0, () -> mode.is(Mode.Forever)));

    private final StringSetting title = add(new StringSetting("Title", "%hackname% %version%", () -> mode.is(Mode.Custom) || mode.is(Mode.Forever)));
    private final SliderSetting size = add(new SliderSetting("Size", 8.0, 1.0, 20.0, 0.1, () -> mode.is(Mode.Custom) || mode.is(Mode.Forever)));
    private final BooleanSetting shadow = add(new BooleanSetting("Shadow", true, () -> mode.is(Mode.Custom) || mode.is(Mode.Forever)));
    private final ColorSetting color = add(new ColorSetting("Color", new Color(208, 0, 0), () -> mode.is(Mode.Custom)));
    public final BooleanSetting artist = add(new BooleanSetting("Artist", false, () -> mode.is(Mode.Custom)));
    private final BooleanSetting glow = add(new BooleanSetting("Glow", true, () -> mode.is(Mode.Custom)).setParent());
    private final SliderSetting glowRadius = add(new SliderSetting("GlowRadius", 4.5, 0.1, 10.0, 0.1, () -> mode.is(Mode.Custom) && glow.isOpen()));
    private final SliderSetting glowOpacity = add(new SliderSetting("GlowOpacity", 1.0, 0.1, 1.0, 0.05, () -> mode.is(Mode.Custom) && glow.isOpen()));
    private final SliderSetting glowYOffset = add(new SliderSetting("GlowYOffset", 0.0, -20.0, 10.0, 0.1, () -> mode.is(Mode.Custom) && glow.isOpen()));
    private final BooleanSetting glowGradient = add(new BooleanSetting("Gradient", false, () -> mode.is(Mode.Custom) && glow.isOpen()));
    private final SliderSetting wurstLogoScale = add(new SliderSetting("LogoScale", 1.0, 0.5, 2.0, 0.05, () -> mode.is(Mode.Wurst)));

    private Identifier currentGlowTexture = null;
    private String lastGlowConfig = "";
    private long lastGlowUpdate = 0;
    private long lastGlitchTime = 0;
    private long lastJitterTime = 0;
    private boolean isGlitched = false;
    private float rgbOffsetL = 0;
    private float rgbOffsetR = 0;
    private final float[] sliceY = new float[3];
    private final float[] sliceH = new float[3];
    private final float[] sliceX = new float[3];

    public WaterMarkHud() {
        super("WaterMark", Category.HUD);
        setChinese("水印");
        INSTANCE = this;
    }

    public void setWatermarkText(String text) {
        this.title.setValue(text);
    }

    public String getWatermarkTextValue() {
        return this.title.getValue();
    }

    private String getWatermarkText() {
        return title.getValue()
                .replaceAll("%version%", "4.0")
                .replaceAll("%hackname%", "Alien");
    }

    private Color getCurrentColor() {
        return color.getValue();
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        renderElement(drawContext, tickDelta);
    }

    @Override
    public String getElementName() {
        return getChinese() != null ? getChinese() : getName();
    }

    @Override
    public int getElementX() {
        return hudX;
    }

    @Override
    public void setElementX(int x) {
        hudX = x;
    }

    @Override
    public int getElementY() {
        return hudY;
    }

    @Override
    public void setElementY(int y) {
        hudY = y;
    }

    @Override
    public int getElementWidth() {
        if (mode.is(Mode.Wurst)) {
            return (int) getWurstWidth();
        }
        if (mode.is(Mode.Forever)) {
            return (int) getForeverFontRenderer().getWidth(getWatermarkText()) + 4;
        }
        if (artist.getValue()) {
            FontRenderer artistFont = getArtistFont();
            if (artistFont != null) {
                return (int) artistFont.getWidth(getWatermarkText()) + 4;
            }
        }
        if (isMinecraftFont()) {
            return (int) (mc.textRenderer.getWidth(getWatermarkText()) * getMinecraftScale()) + 4;
        }
        return (int) getFont().getWidth(getWatermarkText()) + 4;
    }

    @Override
    public int getElementHeight() {
        if (mode.is(Mode.Wurst)) {
            return (int) getWurstHeight();
        }
        if (mode.is(Mode.Forever)) {
            return (int) getForeverFontRenderer().getFontHeight() + 4;
        }
        if (artist.getValue()) {
            FontRenderer artistFont = getArtistFont();
            if (artistFont != null) {
                return (int) artistFont.getFontHeight() + 4;
            }
        }
        if (isMinecraftFont()) {
            return (int) (9.0f * getMinecraftScale()) + 4;
        }
        return (int) getFont().getFontHeight() + 4;
    }

    @Override
    public boolean isElementVisible() {
        return isEnabled();
    }

    @Override
    public void renderElement(DrawContext context, float tickDelta) {
        if (!isEnabled()) {
            return;
        }

        if (mode.is(Mode.Wurst)) {
            cleanupGlowTexture();
            renderWurstWatermark(context);
            return;
        }

        String text = getWatermarkText();
        if (text == null || text.isEmpty()) {
            cleanupGlowTexture();
            return;
        }

        if (mode.is(Mode.Forever)) {
            cleanupGlowTexture();
            renderForeverWatermark(context, text);
            return;
        }

        renderWatermark(context, text);
    }
    private void renderForeverWatermark(DrawContext context, String text) {
        MatrixStack ms = context.getMatrices();
        FontRenderer fr = getForeverFontRenderer();
        float textWidth = fr.getWidth(text);
        float textHeight = fr.getFontHeight();

        long time = System.currentTimeMillis();
        long interval = (long) glitchInterval.getValue();
        long duration = (long) glitchDuration.getValue();
        long jitterRate = (long) jitterInterval.getValue();

        long timeSinceLast = time - lastGlitchTime;

        if (timeSinceLast > interval) {
            lastGlitchTime = time;
            lastJitterTime = time;
            isGlitched = true;
            generateGlitchOffsets(textHeight);

        } else if (isGlitched && timeSinceLast > duration) {
            isGlitched = false;
            resetGlitchOffsets();

        } else if (isGlitched) {
            if (time - lastJitterTime > jitterRate) {
                lastJitterTime = time;
                jitterGlitchOffsets(textHeight);
            }
        }
        drawForeverRGBText(ms, fr, text, hudX, hudY, rgbOffsetL, rgbOffsetR);
        if (isGlitched) {
            for (int i = 0; i < 3; i++) {
                if (sliceX[i] == 0) continue;

                int sx = hudX - 15;
                int sy = (int) (hudY + sliceY[i]);
                int sw = (int) textWidth + 30;
                int sh = (int) sliceH[i];

                context.enableScissor(sx, sy, sx + sw, sy + sh);
                drawForeverRGBText(ms, fr, text, hudX + sliceX[i], hudY, rgbOffsetL * 1.8f, rgbOffsetR * 1.8f);
                context.disableScissor();
            }
        }
    }
    private void generateGlitchOffsets(float textHeight) {
        rgbOffsetL = (float) ((Math.random() - 0.5) * 6);
        rgbOffsetR = (float) ((Math.random() - 0.5) * 6);
        for (int i = 0; i < 3; i++) {
            sliceY[i] = (float) (Math.random() * textHeight);
            sliceH[i] = (float) (Math.random() * (textHeight / 2.5));
            sliceX[i] = (float) ((Math.random() - 0.5) * 12);
        }
    }
    private void jitterGlitchOffsets(float textHeight) {
        rgbOffsetL = (float) ((Math.random() - 0.5) * 6);
        rgbOffsetR = (float) ((Math.random() - 0.5) * 6);
        if (Math.random() > 0.5) {
            for (int i = 0; i < 3; i++) {
                sliceX[i] = (float) ((Math.random() - 0.5) * 12);
            }
        }
    }
    private void resetGlitchOffsets() {
        rgbOffsetL = 0;
        rgbOffsetR = 0;
        for (int i = 0; i < 3; i++) {
            sliceX[i] = 0;
        }
    }

    private void drawForeverRGBText(MatrixStack ms, FontRenderer fr, String text, float x, float y, float offL, float offR) {
        RenderSystem.enableBlend();
        if (offL != 0 || offR != 0) {
            fr.drawString(ms, text, x + offL, y, 0.0f, 1.0f, 1.0f, 0.75f, false);
            fr.drawString(ms, text, x + offR, y, 1.0f, 0.0f, 0.15f, 0.75f, false);
        }
        fr.drawString(ms, text, x, y, 1.0f, 1.0f, 1.0f, 1.0f, shadow.getValue());
    }

    private FontRenderer getForeverFontRenderer() {
        float s = size.getValueFloat();
        FontRenderer fr = null;
        if (foreverFont.is(ForeverFontMode.OXANIUM)) {
            fr = FontManager.getOxaniumFont(s);
        } else if (foreverFont.is(ForeverFontMode.ORBITRON)) {
            fr = FontManager.getOrbitronFont(s);
        }
        return fr != null ? fr : getFont();
    }

    private void renderWatermark(DrawContext context, String text) {
        MatrixStack ms = context.getMatrices();
        Color textColor = getCurrentColor();

        if (!glow.getValue()) {
            cleanupGlowTexture();
            drawPlainText(context, ms, text, textColor);
            return;
        }

        if (!artist.getValue() && isMinecraftFont()) {
            cleanupGlowTexture();
            drawMinecraftGlow(context, ms, text, textColor);
            return;
        }

        Color glowColor = new Color(
                textColor.getRed(),
                textColor.getGreen(),
                textColor.getBlue(),
                Math.min(255, (int) (textColor.getAlpha() * glowOpacity.getValue()))
        );
        boolean artistFont = artist.getValue();
        String forceFont = artistFont ? "borel-regular.ttf" : null;
        float fontSize = size.getValueFloat();
        String config = String.format(
                "%s_%.1f_%.2f_%.1f_%08x_%08x_%b_%b_%s_%.1f_%s",
                text,
                glowRadius.getValue(),
                glowOpacity.getValue(),
                glowYOffset.getValue(),
                textColor.getRGB(),
                glowColor.getRGB(),
                glowGradient.getValue(),
                artistFont,
                FontManager.getMode().name(),
                fontSize,
                forceFont
        );

        boolean needsUpdate = currentGlowTexture == null
                || !config.equals(lastGlowConfig)
                || System.currentTimeMillis() - lastGlowUpdate > 5000;

        if (needsUpdate) {
            cleanupGlowTexture();
            currentGlowTexture = SkijaGlowRenderer.createGlowTexture(
                    text,
                    fontSize,
                    textColor,
                    glowColor,
                    glowRadius.getValueFloat(),
                    glowGradient.getValue(),
                    glowGradient.getValue() ? glowColor : null,
                    forceFont
            );
            lastGlowConfig = config;
            lastGlowUpdate = System.currentTimeMillis();
        }

        if (currentGlowTexture != null && SkijaGlowRenderer.isTextureReady(currentGlowTexture)) {
            drawGlowTexture(context, ms, text);
            return;
        }

        drawPlainText(context, ms, text, textColor);
    }

    private void drawPlainText(DrawContext context, MatrixStack matrixStack, String text, Color textColor) {
        if (artist.getValue()) {
            FontRenderer artistFont = getArtistFont();
            if (artistFont != null) {
                artistFont.drawString(
                        matrixStack,
                        text,
                        hudX,
                        hudY,
                        textColor.getRed() / 255f,
                        textColor.getGreen() / 255f,
                        textColor.getBlue() / 255f,
                        textColor.getAlpha() / 255f,
                        shadow.getValue()
                );
                return;
            }
        }

        if (isMinecraftFont()) {
            drawMinecraftText(context, matrixStack, text, textColor);
            return;
        }

        getFont().drawString(
                matrixStack,
                text,
                hudX,
                hudY,
                textColor.getRed() / 255f,
                textColor.getGreen() / 255f,
                textColor.getBlue() / 255f,
                textColor.getAlpha() / 255f,
                shadow.getValue()
        );
    }

    private void drawGlowTexture(DrawContext context, MatrixStack matrixStack, String text) {
        float textWidth = getTextWidth(text);
        float textHeight = getTextHeight();
        float glowPadding = glowRadius.getValueFloat() * 3.0f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1, 1, 1, 1);

        matrixStack.push();
        matrixStack.translate(hudX - glowPadding, hudY + glowYOffset.getValueFloat() - glowPadding, 0);
        context.drawTexture(
                currentGlowTexture,
                0, 0,
                0, 0,
                (int) (textWidth + glowPadding * 2),
                (int) (textHeight + glowPadding * 2),
                (int) (textWidth + glowPadding * 2),
                (int) (textHeight + glowPadding * 2)
        );
        matrixStack.pop();

        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    private void renderWurstWatermark(DrawContext context) {
        MatrixStack matrixStack = context.getMatrices();
        float scale = wurstLogoScale.getValueFloat();
        float scaledWidth = WURST_LOGO_WIDTH * scale;
        float scaledHeight = WURST_LOGO_HEIGHT * scale;
        float textScale = 0.95f;
        float textWidth = mc.textRenderer.getWidth(WURST_VERSION) * textScale;
        float backgroundWidth = textWidth + scaledWidth + 3.0f;
        float backgroundHeight = 11.0f * scale;
        float logoY = hudY + 3.0f * scale;
        float backgroundY = logoY + 7.0f - 2.0f * scale;

        context.fill(
                hudX,
                (int) backgroundY,
                (int) (hudX + backgroundWidth),
                (int) (backgroundY + backgroundHeight),
                new Color(255, 255, 255, 80).getRGB()
        );

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        matrixStack.push();
        matrixStack.translate(hudX, logoY, 0.0f);
        matrixStack.scale(scale, scale, 1.0f);
        context.drawTexture(
                WURST_TEXTURE,
                0,
                0,
                0.0f,
                0.0f,
                (int) WURST_LOGO_WIDTH,
                (int) WURST_LOGO_HEIGHT,
                (int) WURST_LOGO_WIDTH,
                (int) WURST_LOGO_HEIGHT
        );
        matrixStack.pop();

        matrixStack.push();
        matrixStack.translate(hudX + scaledWidth + scale, logoY + 7.0f, 0.0f);
        matrixStack.scale(textScale, textScale, 1.0f);
        context.drawText(mc.textRenderer, WURST_VERSION, 0, 0, Color.WHITE.getRGB(), false);
        matrixStack.pop();

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private float getWurstWidth() {
        float scale = wurstLogoScale.getValueFloat();
        float textScale = 0.95f;
        return mc.textRenderer.getWidth(WURST_VERSION) * textScale + WURST_LOGO_WIDTH * scale + 3.0f;
    }

    private float getWurstHeight() {
        return 21.0f * wurstLogoScale.getValueFloat();
    }

    private void drawMinecraftGlow(DrawContext context, MatrixStack matrixStack, String text, Color textColor) {
        float radius = glowRadius.getValueFloat();
        float yOffset = glowYOffset.getValueFloat();
        int layers = Math.max(1, (int) Math.ceil(radius * 2.0f));

        for (int i = layers; i > 0; i--) {
            float offset = i * 0.35f;
            int alpha = Math.min(255, (int) (textColor.getAlpha() * glowOpacity.getValue() / (i + 1)));
            Color glowColor = new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), alpha);
            drawMinecraftText(context, matrixStack, text, glowColor, -offset, yOffset - offset);
            drawMinecraftText(context, matrixStack, text, glowColor, offset, yOffset - offset);
            drawMinecraftText(context, matrixStack, text, glowColor, -offset, yOffset + offset);
            drawMinecraftText(context, matrixStack, text, glowColor, offset, yOffset + offset);
        }

        drawMinecraftText(context, matrixStack, text, textColor, 0.0f, yOffset);
    }

    private void drawMinecraftText(DrawContext context, MatrixStack matrixStack, String text, Color textColor) {
        drawMinecraftText(context, matrixStack, text, textColor, 0.0f, 0.0f);
    }

    private void drawMinecraftText(DrawContext context, MatrixStack matrixStack, String text, Color textColor, float offsetX, float offsetY) {
        float scale = getMinecraftScale();
        matrixStack.push();
        matrixStack.translate(hudX + offsetX, hudY + offsetY, 0.0f);
        matrixStack.scale(scale, scale, 1.0f);
        context.drawText(mc.textRenderer, text, 0, 0, textColor.getRGB(), shadow.getValue());
        matrixStack.pop();
    }

    private FontRenderer getArtistFont() {
        return FontManager.getArtistFont(size.getValueFloat());
    }

    private FontRenderer getFont() {
        return FontManager.get(size.getValueFloat());
    }

    private float getTextWidth(String text) {
        if (artist.getValue()) {
            FontRenderer artistFont = getArtistFont();
            if (artistFont != null) {
                return artistFont.getWidth(text);
            }
        }
        if (isMinecraftFont()) {
            return mc.textRenderer.getWidth(text) * getMinecraftScale();
        }
        return getFont().getWidth(text);
    }

    private float getTextHeight() {
        if (artist.getValue()) {
            FontRenderer artistFont = getArtistFont();
            if (artistFont != null) {
                return artistFont.getFontHeight();
            }
        }
        if (isMinecraftFont()) {
            return 9.0f * getMinecraftScale();
        }
        return getFont().getFontHeight();
    }

    private boolean isMinecraftFont() {
        return FontManager.getMode() == FontMode.MINECRAFT;
    }

    private float getMinecraftScale() {
        return size.getValueFloat() / 8.0f;
    }

    private void cleanupGlowTexture() {
        if (currentGlowTexture != null) {
            SkijaGlowRenderer.cleanupTexture(currentGlowTexture);
            currentGlowTexture = null;
        }
        lastGlowConfig = "";
        lastGlowUpdate = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        cleanupGlowTexture();
    }

    public enum Mode {
        Custom,
        Wurst,
        Forever
    }

    public enum ForeverFontMode {
        Default,
        OXANIUM,
        ORBITRON
    }
}