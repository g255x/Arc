package dev.kissoo.mod.modules.impl.hud;

import dev.kissoo.api.utils.render.TextUtil;
import hoprc.api.core.impl.FontManager;
import dev.kissoo.mod.modules.HudElement;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

public class DirectionHud extends Module implements HudElement {
    public static DirectionHud INSTANCE;
    private int hudX = 2, hudY = -1;

    private final BooleanSetting shadow = add(new BooleanSetting("Shadow", true));
    private final ColorSetting color = add(new ColorSetting("Color", new Color(208, 0, 0)));

    public DirectionHud() {
        super("Direction", Category.HUD);
        setChinese("方向显示");
        INSTANCE = this;
    }

    private String getText() {
        if (mc.player == null) return "North §7[§f-Z§7]";
        float yaw = mc.player.getYaw();
        yaw = ((yaw % 360) + 360) % 360;
        String f, a;
        if (yaw >= 337.5 || yaw < 22.5) { f = "South"; a = "+Z"; }
        else if (yaw < 67.5) { f = "Southwest"; a = "-X +Z"; }
        else if (yaw < 112.5) { f = "West"; a = "-X"; }
        else if (yaw < 157.5) { f = "Northwest"; a = "-X -Z"; }
        else if (yaw < 202.5) { f = "North"; a = "-Z"; }
        else if (yaw < 247.5) { f = "Northeast"; a = "+X -Z"; }
        else if (yaw < 292.5) { f = "East"; a = "+X"; }
        else { f = "Southeast"; a = "+X +Z"; }
        return f + " §7[§f" + a + "§7]";
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) { renderElement(drawContext, tickDelta); }

    @Override public String getElementName() { return getChinese() != null ? getChinese() : getName(); }
    @Override public int getElementX() { return hudX; }
    @Override public void setElementX(int x) { hudX = x; }

    @Override
    public int getElementY() {
        if (hudY < 0) return mc.getWindow().getScaledHeight() - (int) FontManager.getFontHeight() * 2 - 4;
        return hudY;
    }

    @Override public void setElementY(int y) { hudY = y; }
    @Override public int getElementWidth() { return (int) FontManager.getWidth(getText()) + 4; }
    @Override public int getElementHeight() { return (int) FontManager.getFontHeight() + 2; }
    @Override public boolean isElementVisible() { return isOn(); }

    @Override
    public void renderElement(DrawContext context, float tickDelta) {
        TextUtil.drawString(context, getText(), getElementX(), getElementY(), color.getValue().getRGB(), shadow.getValue());
    }
}