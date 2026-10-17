package dev.kissoo.mod.modules.impl.hud;

import dev.kissoo.Forever;
import dev.kissoo.api.utils.render.TextUtil;
import hoprc.api.core.impl.FontManager;
import dev.kissoo.mod.modules.HudElement;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.text.DecimalFormat;

public class SpeedHud extends Module implements HudElement {
    public static SpeedHud INSTANCE;

    private int hudX = 2, hudY = 86;
    private AlignMode alignMode = AlignMode.LEFT;

    private final BooleanSetting shadow = add(new BooleanSetting("Shadow", true));
    private final ColorSetting color = add(new ColorSetting("Color", new Color(208, 0, 0)));
    private final DecimalFormat df = new DecimalFormat("0.0");

    public SpeedHud() {
        super("SpeedHud", Category.HUD);
        setChinese("速度显示");
        INSTANCE = this;
    }

    private String getText() {
        if (mc.player == null) return "Speed §f0.0km/h";
        double x = mc.player.getX() - mc.player.prevX;
        double z = mc.player.getZ() - mc.player.prevZ;
        double dist = Math.sqrt(x * x + z * z) / 1000.0;
        double div = 1.388888888888889E-5;
        float timer = Forever.TIMER.get();
        return "Speed §f" + df.format(dist / div * timer) + "km/h";
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
        if (alignMode == AlignMode.RIGHT) {
            return hudX - getElementWidth();
        }
        return hudX;
    }

    @Override
    public void setElementX(int x) {
        if (alignMode == AlignMode.RIGHT) {
            hudX = x + getElementWidth();
        } else {
            hudX = x;
        }
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
        return (int) FontManager.getWidth(getText()) + 4;
    }

    @Override
    public int getElementHeight() {
        return (int) FontManager.getFontHeight() + 2;
    }

    @Override
    public boolean isElementVisible() {
        return isOn();
    }

    @Override
    public AlignMode getAlignMode() {
        return alignMode;
    }

    @Override
    public void setAlignMode(AlignMode mode) {
        if (mode == alignMode) return;
        if (mode == AlignMode.RIGHT && alignMode == AlignMode.LEFT) {
            hudX = hudX + getElementWidth();
        } else if (mode == AlignMode.LEFT && alignMode == AlignMode.RIGHT) {
            hudX = hudX - getElementWidth();
        }
        alignMode = mode;
    }

    @Override
    public int getRightAnchorX() {
        if (alignMode == AlignMode.RIGHT) {
            return hudX;
        }
        return hudX + getElementWidth();
    }

    @Override
    public void setRightAnchorX(int rightX) {
        if (alignMode == AlignMode.RIGHT) {
            hudX = rightX;
        } else {
            hudX = rightX - getElementWidth();
        }
    }

    @Override
    public void renderElement(DrawContext context, float tickDelta) {
        int renderX = getElementX();
        TextUtil.drawString(context, getText(), renderX, hudY, color.getValue().getRGB(), shadow.getValue());
    }
}