package dev.kissoo.mod.modules.impl.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.kissoo.api.utils.Wrapper;
import hoprc.api.core.impl.FontManager;
import hoprc.api.gui.fonts.FontRenderer;
import dev.kissoo.mod.modules.HudElement;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.StatusEffectSpriteManager;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PotionHud extends Module implements HudElement {
    public static PotionHud INSTANCE;

    private int hudX = 2, hudY = 130;

    private final BooleanSetting shadow = add(new BooleanSetting("Shadow", true));
    private final BooleanSetting showIcon = add(new BooleanSetting("ShowIcon", true));
    private final SliderSetting textSize = add(new SliderSetting("TextSize", 8.0, 4.0, 20.0, 0.5));
    private final SliderSetting iconOffset = add(new SliderSetting("IconOffset", 0.0, -10.0, 10.0, 0.1));
    private final SliderSetting spacing = add(new SliderSetting("Spacing", 2.0, 0.0, 10.0, 0.5));
    private final BooleanSetting reverseOnRight = add(new BooleanSetting("ReverseOnRight", true));
    private final BooleanSetting rightAlign = add(new BooleanSetting("RightAlign", true));
    private final ColorSetting color = add(new ColorSetting("Color", new Color(208, 0, 0)));

    public PotionHud() {
        super("Potions", Category.HUD);
        setChinese("药水效果");
        INSTANCE = this;
    }

    private static String getDuration(StatusEffectInstance pe) {
        if (pe.isInfinite()) return "∞";
        int d = pe.getDuration();
        return String.format("%d:%02d", d / 1200, d % 1200 / 20);
    }

    private FontRenderer getPotionFont() {
        return FontManager.get(textSize.getValueFloat());
    }

    private float getPotionFontHeight() {
        return getPotionFont().getFontHeight();
    }

    private int getPotionIconSize() {
        return Math.max((int) (getPotionFontHeight() * 0.8f), 8);
    }

    private List<PotionLine> getPotionLines() {
        List<PotionLine> lines = new ArrayList<>();
        if (mc.player == null) return lines;

        Collection<StatusEffectInstance> effects = mc.player.getStatusEffects();
        for (StatusEffectInstance effect : effects) {
            String name = effect.getEffectType().value().getName().getString();
            int amp = effect.getAmplifier() + 1;
            String duration = getDuration(effect);
            String text = name + " " + amp + " §f" + duration;
            lines.add(new PotionLine(effect, text));
        }

        return lines;
    }

    private boolean isRightSide() {
        if (mc == null || mc.getWindow() == null) return false;
        return getElementX() > mc.getWindow().getScaledWidth() / 2;
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
        List<PotionLine> lines = getPotionLines();
        FontRenderer font = getPotionFont();
        int iconPart = showIcon.getValue() ? getPotionIconSize() + (int) spacing.getValue() : 0;

        int max = 50;
        for (PotionLine line : lines) {
            int w = (int) font.getWidth(line.text) + iconPart;
            if (w > max) max = w;
        }
        return max + 4;
    }

    @Override
    public int getElementHeight() {
        int count = Math.max(getPotionLines().size(), 1);
        return (int) ((getPotionFontHeight() + 2) * count);
    }

    @Override
    public boolean isElementVisible() {
        return isOn();
    }

    @Override
    public void renderElement(DrawContext context, float tickDelta) {
        if (mc.player == null) return;

        List<PotionLine> lines = getPotionLines();
        if (lines.isEmpty()) return;

        FontRenderer font = getPotionFont();
        float fontHeight = font.getFontHeight();
        int iconSize = getPotionIconSize();
        float iconYOff = iconOffset.getValueFloat();
        int gap = (int) spacing.getValue();

        boolean reverse = reverseOnRight.getValue() && isRightSide();
        boolean alignRight = rightAlign.getValue() && isRightSide();

        int totalWidth = getElementWidth() - 4;
        float y = hudY;

        for (PotionLine line : lines) {
            int textWidth = (int) font.getWidth(line.text);
            int lineWidth = textWidth + (showIcon.getValue() ? iconSize + gap : 0);

            float baseX;
            if (alignRight) {
                baseX = hudX + totalWidth - lineWidth;
            } else {
                baseX = hudX;
            }

            float textX;
            float iconX;

            if (showIcon.getValue()) {
                if (reverse) {
                    textX = baseX;
                    iconX = baseX + textWidth + gap;
                } else {
                    iconX = baseX;
                    textX = baseX + iconSize + gap;
                }
            } else {
                textX = baseX;
                iconX = baseX;
            }

            if (showIcon.getValue()) {
                StatusEffectSpriteManager sm = Wrapper.mc.getStatusEffectSpriteManager();
                Sprite sprite = sm.getSprite(line.effect.getEffectType());

                RenderSystem.setShaderColor(1, 1, 1, 1);
                RenderSystem.enableBlend();

                int iconY = (int) (y + (fontHeight - iconSize) / 2.0f + iconYOff);
                context.drawSprite((int) iconX, iconY, 0, iconSize, iconSize, sprite);

                RenderSystem.setShaderColor(1, 1, 1, 1);
            }

            font.drawString(
                    context.getMatrices(),
                    line.text,
                    textX,
                    y,
                    color.getValue().getRed() / 255f,
                    color.getValue().getGreen() / 255f,
                    color.getValue().getBlue() / 255f,
                    color.getValue().getAlpha() / 255f,
                    shadow.getValue()
            );

            y += fontHeight + 2;
        }
    }

    private static class PotionLine {
        private final StatusEffectInstance effect;
        private final String text;

        private PotionLine(StatusEffectInstance effect, String text) {
            this.effect = effect;
            this.text = text;
        }
    }
}