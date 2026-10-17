package dev.perish.mod.modules.impl.hud;

import dev.perish.Perish;
import dev.perish.api.utils.render.ColorUtil;
import dev.perish.api.utils.render.Render2DUtil;
import dev.perish.api.utils.render.TextUtil;
import dev.perish.core.impl.FontManager;
import dev.perish.mod.gui.hud.IHudPosition;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.impl.client.HUD;
import dev.perish.mod.modules.settings.impl.*;

import java.awt.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

public class ArrayListDisplay extends Module implements IHudPosition {
    public static ArrayListDisplay INSTANCE;
    public final SliderSetting textOffset = this.add(new SliderSetting("TextOffset", 0.0, -10.0, 10.0, 0.1));
    public final SliderSetting interval = this.add(new SliderSetting("Interval", 2.2, 0.0, 15.0, 0.1));
    public final BooleanSetting font = this.add(new BooleanSetting("Font", false));
    public final EnumSetting<SortOrder> sortOrder = this.add(new EnumSetting<>("SortOrder", SortOrder.ShortFirst));
    public final BooleanSetting onlyBind = this.add(new BooleanSetting("OnlyBind", false));
    public final BooleanSetting lowerCase = this.add(new BooleanSetting("LowerCase", false));
    public final BooleanSetting customColor = this.add(new BooleanSetting("CustomColor", false));
    public final ColorSetting color = this.add(new ColorSetting("Color", new Color(153, 153, 255), this.customColor::getValue));
    public final BooleanSetting backGround = this.add(new BooleanSetting("BackGround", false).setParent());
    public final SliderSetting bgWidth = this.add(new SliderSetting("Width", 0.0, 0.0, 15.0, () -> this.backGround.isOpen()));
    public final SliderSetting borderRadius = this.add(new SliderSetting("BorderRadius", 4.0, 0.0, 20.0, 0.1, () -> this.backGround.isOpen()));
    public final ColorSetting bgColor = this.add(new ColorSetting("BGColor", new Color(0, 0, 0, 100), () -> this.backGround.isOpen()));
    public final ColorSetting rect = this.add(new ColorSetting("Rect", new Color(208, 0, 0)).injectBoolean(false));
    public final ColorSetting glow = this.add(new ColorSetting("Glow", new Color(208, 0, 100)).injectBoolean(false));

    private float hudX, hudY, hudW, hudH;
    private boolean hudPositionInitialized;

    public ArrayListDisplay() {
        super("ArrayList", Module.Category.Hud);
        this.setChinese("模块列表");
        INSTANCE = this;
    }

    @Override
    public void onRender2D(DrawContext context, float tickDelta) {
        if (nullCheck()) return;

        boolean useFont = this.font.getValue();
        boolean shadow = HUD.INSTANCE.shadow.getValue();
        boolean renderingUp = HUD.INSTANCE.renderingUp.getValue();
        float fontHeight = useFont ? FontManager.ui.getFontHeight() : 9;
        float intervalVal = (float) this.interval.getValue();
        float textOffsetVal = (float) this.textOffset.getValue();

        java.util.List<Module> modules = new java.util.ArrayList<>();
        for (Module m : Perish.MODULE.getModules()) {
            if (this.onlyBind.getValue() && m.getBindSetting().getValue() == -1) continue;
            if (m.isOn() && m.drawn.getValue()) modules.add(m);
        }
        modules.sort((a, b) -> {
            float wa = useFont ? FontManager.ui.getWidth(a.getArrayName()) : mc.textRenderer.getWidth(a.getArrayName());
            float wb = useFont ? FontManager.ui.getWidth(b.getArrayName()) : mc.textRenderer.getWidth(b.getArrayName());
            return this.sortOrder.is(SortOrder.ShortFirst) ? Float.compare(wa, wb) : Float.compare(wb, wa);
        });

        float windowWidth = mc.getWindow().getScaledWidth();
        float maxTextWidth = 0.0F;
        for (Module module : modules) {
            String text = module.getArrayName();
            if (this.lowerCase.getValue()) text = text.toLowerCase();
            maxTextWidth = Math.max(maxTextWidth, useFont ? FontManager.ui.getWidth(text) : mc.textRenderer.getWidth(text));
        }

        float backgroundWidth = (float) this.bgWidth.getValue();
        float previousHudWidth = hudW;
        float newHudWidth = Math.max(maxTextWidth + backgroundWidth + 1.0F, 1.0F);
        if (hudPositionInitialized && previousHudWidth > 0.0F && hudX + previousHudWidth / 2.0F >= windowWidth / 2.0F) {
            hudX += previousHudWidth - newHudWidth;
        }
        hudW = newHudWidth;
        hudH = Math.max(modules.size() > 0 ? modules.size() * (fontHeight + intervalVal) - intervalVal : fontHeight, 1.0F);
        if (!hudPositionInitialized) {
            hudX = windowWidth - hudW - 5.0F;
            hudY = 5.0F;
            hudPositionInitialized = true;
        }

        this.clampHudPosition(windowWidth, mc.getWindow().getScaledHeight());
        boolean alignRight = hudX + hudW / 2.0F >= windowWidth / 2.0F;
        float textLeft = hudX + backgroundWidth / 2.0F + 1.0F;
        float textRight = hudX + hudW - backgroundWidth / 2.0F;
        float counter = 20f;

        float currentY;
        if (renderingUp) {
            currentY = hudY;
        } else {
            currentY = hudY + (modules.size() > 0 ? (modules.size() - 1) * (fontHeight + intervalVal) : 0);
        }

        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            String text = module.getArrayName();
            if (this.lowerCase.getValue()) text = text.toLowerCase();

            float tw = useFont ? FontManager.ui.getWidth(text) : mc.textRenderer.getWidth(text);
            int color = this.customColor.getValue()
                    ? this.color.getValue().getRGB()
                    : ColorUtil.injectAlpha(HUD.INSTANCE.getColor(counter), HUD.INSTANCE.color.getValue().getAlpha());

            float bgX = alignRight ? textRight - tw - backgroundWidth / 2.0F : textLeft - backgroundWidth / 2.0F;
            float bgY = currentY - 1 - intervalVal / 2;
            float bgWVal = tw + backgroundWidth;
            float bgHVal = fontHeight + intervalVal;

            if (this.backGround.getValue()) {
                int bgC = ColorUtil.injectAlpha(
                        this.bgColor.sync ? color : this.bgColor.getValue().getRGB(),
                        this.bgColor.getValue().getAlpha());
                Render2DUtil.drawRoundedRect(context.getMatrices(), bgX, bgY, bgWVal, bgHVal,
                        this.borderRadius.getValueFloat(), new Color(bgC, true));
            }

            if (this.glow.booleanValue) {
                Render2DUtil.drawGlow(context.getMatrices(), bgX, bgY, bgWVal, bgHVal,
                        ColorUtil.injectAlpha(this.glow.sync ? color : this.glow.getValue().getRGB(),
                                this.glow.getValue().getAlpha()));
            }

            if (this.customColor.getValue() && this.color.isGlobalPulse()) {
                TextUtil.drawStringSetting(context, text, alignRight ? textRight - tw : textLeft, currentY + textOffsetVal, this.color, useFont, shadow);
            } else if (useFont) {
                FontManager.ui.drawString(context.getMatrices(), text, alignRight ? textRight - tw : textLeft, currentY + textOffsetVal, color, shadow);
            } else {
                TextUtil.drawString(context, text, alignRight ? textRight - tw : textLeft, currentY + textOffsetVal, color, false, shadow);
            }

            if (this.rect.booleanValue) {
                Render2DUtil.drawRect(context.getMatrices(),
                        alignRight ? textRight + backgroundWidth / 2.0F : textLeft - backgroundWidth / 2.0F - 1.0F,
                        currentY - 1 - intervalVal / 2,
                        1f, fontHeight + intervalVal,
                        this.rect.sync ? color : ColorUtil.injectAlpha(this.rect.getValue(), this.rect.getValue().getAlpha()).getRGB());
            }

            if (renderingUp) currentY += fontHeight + intervalVal;
            else currentY -= fontHeight + intervalVal;
            counter += HUD.INSTANCE.pulseCounter.getValueInt();
        }
    }

    public float getHudX() { return hudX; }
    public float getHudY() { return hudY; }
    public float getHudW() { return hudW; }
    public float getHudH() { return hudH; }

    private void clampHudPosition(float screenWidth, float screenHeight) {
        hudX = MathHelper.clamp(hudX, 0.0F, Math.max(0.0F, screenWidth - hudW));
        hudY = MathHelper.clamp(hudY, 0.0F, Math.max(0.0F, screenHeight - hudH));
    }

    public void setHudPos(float x, float y) {
        this.hudX = x;
        this.hudY = y;
        this.hudPositionInitialized = true;
    }

    public enum SortOrder {
        ShortFirst,
        LongFirst
    }
}
