package dev.kissoo.mod.modules.impl.hud;

import dev.kissoo.api.utils.math.SmoothAnimationUtil;
import dev.kissoo.api.utils.render.Render2DUtil;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.HungerManager;

import java.awt.*;

public class DFHungerBar extends Module {
    public static DFHungerBar INSTANCE;

    public final SliderSetting yOffset = this.add(new SliderSetting("YOffset", -94, -300, 300, 1));

    public final BooleanSetting leftSettings = this.add(new BooleanSetting("LeftBar", true).setParent());
    public final SliderSetting leftWidth = this.add(new SliderSetting("LeftWidth", 71, 20, 300, 1, () -> leftSettings.isOpen()));
    public final SliderSetting leftHeight = this.add(new SliderSetting("LeftHeight", 4, 1, 10, 1, () -> leftSettings.isOpen()));

    public final BooleanSetting rightSettings = this.add(new BooleanSetting("RightBar", true).setParent());
    public final SliderSetting rightWidth = this.add(new SliderSetting("RightWidth", 34, 10, 200, 1, () -> rightSettings.isOpen()));
    public final SliderSetting rightHeight = this.add(new SliderSetting("RightHeight", 4, 1, 10, 1, () -> rightSettings.isOpen()));

    public final BooleanSetting dividerSettings = this.add(new BooleanSetting("Divider", true).setParent());
    public final SliderSetting dividerWidth = this.add(new SliderSetting("DividerWidth", 1, 1, 6, 1, () -> dividerSettings.isOpen()));
    public final SliderSetting dividerGap = this.add(new SliderSetting("DividerGap", 0, 0, 6, 1, () -> dividerSettings.isOpen()));
    public final ColorSetting dividerColor = this.add(new ColorSetting("DividerColor", new Color(0, 0, 0, 100), () -> dividerSettings.isOpen()));

    public final ColorSetting barColor = this.add(new ColorSetting("BarColor", new Color(255, 255, 255, 255)));
    public final ColorSetting bgColor = this.add(new ColorSetting("Background", new Color(136, 136, 136, 136)));

    public final SliderSetting smoothFactor = this.add(new SliderSetting("SmoothFactor", 0.08, 0.01, 0.5, 0.01));

    public final BooleanSetting hideVanilla = this.add(new BooleanSetting("HideVanilla", false));

    private final SmoothAnimationUtil smoothFood = new SmoothAnimationUtil(20, 0.08);
    private final SmoothAnimationUtil smoothSaturation = new SmoothAnimationUtil(5, 0.08);

    public DFHungerBar() {
        super("DFHungerBar", Category.HUD);
        this.setChinese("三角洲体力条");
        INSTANCE = this;
    }

    public static boolean shouldHideVanilla() {
        return INSTANCE != null && INSTANCE.isOn() && INSTANCE.hideVanilla.getValue();
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        if (mc.player == null || mc.world == null) return;

        HungerManager hunger = mc.player.getHungerManager();
        int foodLevel = hunger.getFoodLevel();
        float saturationLevel = hunger.getSaturationLevel();
        float displaySaturation;
        if (foodLevel >= 20) {
            displaySaturation = saturationLevel;
        } else {
            displaySaturation = 0;
        }

        smoothFood.setTarget(foodLevel);
        smoothSaturation.setTarget(displaySaturation);

        double sf = smoothFactor.getValue();
        double smoothFoodVal = updateSmooth(smoothFood, sf);
        double smoothSatVal = updateSmooth(smoothSaturation, sf);

        float foodRatio = (float) Math.max(0, Math.min(smoothFoodVal / 20.0, 1.0));
        float satRatio = (float) Math.max(0, Math.min(smoothSatVal / 20.0, 1.0));

        float lw = leftWidth.getValueFloat();
        float lh = leftHeight.getValueFloat();
        float rw = rightWidth.getValueFloat();
        float rh = rightHeight.getValueFloat();
        float dw = dividerSettings.getValue() ? dividerWidth.getValueFloat() : 0;
        float dg = dividerSettings.getValue() ? dividerGap.getValueFloat() : 0;

        float totalWidth = lw + dg + dw + dg + rw;

        float screenW = mc.getWindow().getScaledWidth();
        float screenH = mc.getWindow().getScaledHeight();
        float startX = (screenW - totalWidth) / 2.0f;
        float baseY = screenH + yOffset.getValueInt();

        MatrixStack matrices = drawContext.getMatrices();

        Color bar = barColor.getValue();
        Color bg = bgColor.getValue();

        float leftX = startX;
        float leftY = baseY - lh / 2.0f;

        Render2DUtil.drawRect(matrices, leftX, leftY, lw, lh, bg);
        if (foodRatio > 0) {
            Render2DUtil.drawRect(matrices, leftX, leftY, lw * foodRatio, lh, bar);
        }

        if (dividerSettings.getValue()) {
            float divX = leftX + lw + dg;
            float divH = Math.max(lh, rh) + 2;
            float divY = baseY - divH / 2.0f;
            Render2DUtil.drawRect(matrices, divX, divY, dw, divH, dividerColor.getValue());
        }

        float rightX = leftX + lw + dg + dw + dg;
        float rightY = baseY - rh / 2.0f;

        Render2DUtil.drawRect(matrices, rightX, rightY, rw, rh, bg);
        if (satRatio > 0) {
            Render2DUtil.drawRect(matrices, rightX, rightY, rw * satRatio, rh, bar);
        }
    }

    private double updateSmooth(SmoothAnimationUtil anim, double factor) {
        double current = anim.get();
        double target = anim.getTarget();
        double diff = target - current;
        if (Math.abs(diff) < 0.01) {
            anim.set(target);
            return target;
        }
        double newVal = current + diff * factor;
        setCurrentOnly(anim, newVal);
        return newVal;
    }

    private void setCurrentOnly(SmoothAnimationUtil anim, double newCurrent) {
        double savedTarget = anim.getTarget();
        anim.set(newCurrent);
        anim.setTarget(savedTarget);
    }
}