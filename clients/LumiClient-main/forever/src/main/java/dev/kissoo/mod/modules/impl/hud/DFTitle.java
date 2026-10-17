package dev.kissoo.mod.modules.impl.hud;

import dev.kissoo.api.utils.render.Render2DUtil;
import hoprc.api.core.impl.FontManager;
import dev.kissoo.mod.modules.HudElement;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.*;

public class DFTitle extends Module implements HudElement {
    public static DFTitle INSTANCE;

    public enum Mode {
        Normal, Medium, Hard
    }

    private int hudX = 10;
    private int hudY = 8;

    public final EnumSetting<Mode> mode = this.add(new EnumSetting<>("Mode", Mode.Normal));
    public final SliderSetting paddingY = this.add(new SliderSetting("HeightAdjust", 0, -10, 10, 0.5));
    public final SliderSetting textYOffset = this.add(new SliderSetting("TextYOffset", 3, -10, 10, 0.5));

    private static final int BG_ALPHA = 29;
    private static final float PADDING_X = 8.0f;
    private static final boolean SHADOW = false;

    public DFTitle() {
        super("DFTitle", Category.HUD);
        this.setChinese("三角洲标题");
        INSTANCE = this;
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

    private String getDisplayString() {
        return switch (mode.getValue()) {
            case Medium -> "机密";
            case Hard -> "绝密";
            case Normal -> "常规";
        };
    }

    private Color getTextColor() {
        return switch (mode.getValue()) {
            case Hard -> new Color(255, 0, 0, 255);
            default -> Color.WHITE;
        };
    }

    @Override
    public int getElementWidth() {
        float textWidth = FontManager.getWidth(getDisplayString());
        return (int) (textWidth + (PADDING_X * 2));
    }

    @Override
    public int getElementHeight() {
        float textHeight = FontManager.getFontHeight();
        return (int) (textHeight + (paddingY.getValueFloat() * 2));
    }

    @Override
    public boolean isElementVisible() {
        return isOn();
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        renderElement(drawContext, tickDelta);
    }

    @Override
    public void renderElement(DrawContext drawContext, float tickDelta) {
        if (mc.player == null || mc.world == null) return;

        String displayString = getDisplayString();
        Color textColor = getTextColor();

        float rectWidth = getElementWidth();
        float rectHeight = getElementHeight();

        float x = hudX;
        float y = hudY;

        MatrixStack matrices = drawContext.getMatrices();
        Render2DUtil.drawRect(matrices, x, y, rectWidth, rectHeight, new Color(0, 0, 0, BG_ALPHA));

        float textX = x + PADDING_X;
        float textY = y + paddingY.getValueFloat() + textYOffset.getValueFloat();

        drawContextText(drawContext, displayString, textX, textY, textColor, SHADOW);
    }

    private void drawContextText(DrawContext drawContext, String text, float x, float y, Color color, boolean shadow) {
        MatrixStack matrices = drawContext.getMatrices();
        if (FontManager.isCustom()) {
            FontManager.get().drawString(
                    matrices,
                    text,
                    x,
                    y,
                    color.getRed() / 255.0f,
                    color.getGreen() / 255.0f,
                    color.getBlue() / 255.0f,
                    color.getAlpha() / 255.0f,
                    shadow
            );
        } else {
            drawContext.drawText(mc.textRenderer, text, (int) x, (int) y, color.getRGB(), shadow);
        }
    }
}