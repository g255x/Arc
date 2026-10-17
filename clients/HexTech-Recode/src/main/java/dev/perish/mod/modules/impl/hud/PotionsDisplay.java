package dev.perish.mod.modules.impl.hud;

import dev.perish.api.utils.render.Render2DUtil;
import dev.perish.core.impl.FontManager;
import dev.perish.mod.gui.hud.IHudPosition;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.ColorSetting;
import dev.perish.mod.modules.settings.impl.EnumSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;

public class PotionsDisplay extends Module implements IHudPosition {
    public static PotionsDisplay INSTANCE;
    public final EnumSetting<Mode> mode = this.add(new EnumSetting<>("Mode", Mode.Card));
    public final BooleanSetting font = this.add(new BooleanSetting("Font", true));
    public final ColorSetting effectColor = this.add(new ColorSetting("EffectColor", new Color(255, 255, 255)));
    public final ColorSetting durationColor = this.add(new ColorSetting("DurationColor", new Color(180, 180, 190)));
    public final SliderSetting spacing = this.add(new SliderSetting("Spacing", 3.0, 0.0, 15.0, 0.5));
    public final SliderSetting width = this.add(new SliderSetting("Width", 0.0, 0.0, 300.0, 1.0));
    public final SliderSetting height = this.add(new SliderSetting("Height", 24.0, 12.0, 48.0, 1.0));

    private float hudX = 5, hudY = 100, hudW, hudH;

    public PotionsDisplay() {
        super("Potions", Module.Category.Hud);
        this.setChinese("药水效果");
        INSTANCE = this;
    }

    @Override
    public void onRender2D(DrawContext context, float tickDelta) {
        if (nullCheck()) return;

        var effects = mc.player.getStatusEffects();
        if (effects.isEmpty()) { hudW = 0; hudH = 0; return; }
        if (this.mode.is(Mode.Text)) {
            this.renderTextMode(context, new ArrayList<>(effects));
            return;
        }

        float itemH = this.height.getValueFloat();
        float sp = this.spacing.getValueFloat();
        float totalHeight = effects.size() * (itemH + sp) - sp;
        float startY = Math.max(0.0F, Math.min(hudY, mc.getWindow().getScaledHeight() - totalHeight));
        float maxW = 0;
        int count = 0;

        for (StatusEffectInstance effect : effects) {
            int rawColor = effect.getEffectType().value().getColor();
            Color effectColor = new Color(rawColor, true);
            Color dimColor = new Color(rawColor & 0xFFFFFF | 0x40000000, true);
            Color brightColor = new Color(rawColor & 0xFFFFFF | 0xB0000000, true);

            int duration = effect.getDuration();
            String name = this.getEffectName(effect);
            String durationStr = this.getDuration(effect);

            boolean useFont = this.font.getValue();
            float nameW = useFont ? FontManager.ui.getWidth(name) : mc.textRenderer.getWidth(name);
            float durW = useFont ? FontManager.small.getWidth(durationStr) : mc.textRenderer.getWidth(durationStr);
            float totalW = Math.max(this.width.getValueFloat(), 8 + 18 + 4 + nameW + 4 + durW + 6);
            maxW = Math.max(maxW, totalW);

            float iy = startY + count * (itemH + sp);

            Render2DUtil.drawRoundedRect(context.getMatrices(), hudX, iy, maxW > totalW ? maxW : totalW, itemH, 5f,
                    new Color(10, 10, 15, 190));

            Render2DUtil.drawRoundedRect(context.getMatrices(), hudX, iy, 3, itemH, 2f, brightColor);

            Render2DUtil.horizontalGradient(context.getMatrices(),
                    hudX + 3, iy, hudX + 20, iy + itemH,
                    dimColor, new Color(10, 10, 15, 0));

            try {
                Sprite sprite = mc.getStatusEffectSpriteManager().getSprite(effect.getEffectType());
                float iconX = hudX + 6;
                float iconY = iy + (itemH - 16) / 2f;
                context.drawSprite((int) iconX, (int) iconY, 0, 16, 16, sprite);
            } catch (Exception e) {
                Render2DUtil.drawCircle(context.getMatrices(), hudX + 14, iy + itemH / 2f, 5f, brightColor, 16);
            }

            float textX = hudX + 26;
            float nameY = iy + 3;
            if (useFont) FontManager.ui.drawString(context.getMatrices(), name, textX, nameY, this.effectColor.getValue().getRGB(), true);
            else context.drawText(mc.textRenderer, name, (int) textX, (int) nameY, this.effectColor.getValue().getRGB(), true);

            float durY = iy + (useFont ? FontManager.ui.getFontHeight() : 9) + 3;
            boolean isLow = !effect.isInfinite() && duration < 200;
            int durColor = new Color(this.durationColor.getValue().getRed(), this.durationColor.getValue().getGreen(), this.durationColor.getValue().getBlue(), isLow ? 220 : 200).getRGB();
            if (useFont) FontManager.small.drawString(context.getMatrices(), durationStr, textX, durY, durColor, true);
            else context.drawText(mc.textRenderer, durationStr, (int) textX, (int) durY, durColor, true);

            if (!effect.isInfinite() && duration > 0) {
                float pct = 1.0f - (float) duration / (duration + 200);
                pct = Math.max(0, Math.min(1, pct));
                float barX = textX + durW + 6;
                float barY = durY + 2;
                float barW = 30;
                float barH = 3;
                Render2DUtil.drawRoundedRect(context.getMatrices(), barX, barY, barW, barH, 1.5f,
                        new Color(40, 40, 50, 160));
                Color barColor = pct < 0.5f ? new Color(76, 175, 80, 200)
                        : pct < 0.75f ? new Color(255, 193, 7, 200)
                        : new Color(244, 67, 54, 200);
                Render2DUtil.drawRoundedRect(context.getMatrices(), barX, barY, barW * pct, barH, 1.5f, barColor);
            }

            count++;
        }

        hudW = maxW + 4;
        hudH = count > 0 ? totalHeight : 0;
    }

    private void renderTextMode(DrawContext context, List<StatusEffectInstance> effects) {
        boolean useFont = this.font.getValue();
        float lineHeight = useFont ? FontManager.ui.getFontHeight() : 9.0F;
        float maxNameWidth = 0.0F;
        float maxDurationWidth = 0.0F;
        for (StatusEffectInstance effect : effects) {
            String name = this.getEffectName(effect);
            String duration = this.getDuration(effect);
            maxNameWidth = Math.max(maxNameWidth, useFont ? FontManager.ui.getWidth(name) : mc.textRenderer.getWidth(name));
            maxDurationWidth = Math.max(maxDurationWidth, useFont ? FontManager.ui.getWidth(duration) : mc.textRenderer.getWidth(duration));
        }

        float totalWidth = Math.max(this.width.getValueFloat(), maxNameWidth + 6.0F + maxDurationWidth);
        boolean alignRight = hudX + totalWidth / 2.0F >= mc.getWindow().getScaledWidth() / 2.0F;
        float durationX = alignRight ? hudX : hudX + totalWidth - maxDurationWidth;
        float lineSpacing = this.spacing.getValueFloat();
        float totalHeight = effects.size() * (lineHeight + lineSpacing) - lineSpacing;
        float y = Math.max(0.0F, Math.min(hudY, mc.getWindow().getScaledHeight() - totalHeight));
        for (StatusEffectInstance effect : effects) {
            String name = this.getEffectName(effect);
            String duration = this.getDuration(effect);
            float nameWidth = useFont ? FontManager.ui.getWidth(name) : mc.textRenderer.getWidth(name);
            float nameX = alignRight ? hudX + totalWidth - nameWidth : hudX;
            if (useFont) {
                FontManager.ui.drawString(context.getMatrices(), name, nameX, y, this.effectColor.getValue().getRGB(), true);
                FontManager.ui.drawString(context.getMatrices(), duration, durationX, y, this.durationColor.getValue().getRGB(), true);
            } else {
                context.drawText(mc.textRenderer, name, (int) nameX, (int) y, this.effectColor.getValue().getRGB(), true);
                context.drawText(mc.textRenderer, duration, (int) durationX, (int) y, this.durationColor.getValue().getRGB(), true);
            }
            y += lineHeight + lineSpacing;
        }

        hudW = totalWidth;
        hudH = totalHeight;
    }

    private String getEffectName(StatusEffectInstance effect) {
        String name = Text.translatable(effect.getTranslationKey()).getString();
        int amplifier = effect.getAmplifier();
        return amplifier > 0 ? name + " " + toRoman(amplifier + 1) : name;
    }

    private String getDuration(StatusEffectInstance effect) {
        if (effect.isInfinite()) return "\u221e";
        int totalSeconds = effect.getDuration() / 20;
        return totalSeconds / 60 + ":" + String.format("%02d", totalSeconds % 60);
    }

    private String toRoman(int num) {
        return switch (num) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(num);
        };
    }

    public float getHudX() { return hudX; }
    public float getHudY() { return hudY; }
    public float getHudW() { return hudW; }
    public float getHudH() { return hudH; }

    public void setHudPos(float x, float y) { this.hudX = x; this.hudY = y; }

    public enum Mode {
        Card,
        Text
    }
}
