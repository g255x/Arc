package dev.kissoo.mod.modules.impl.hud;

import dev.kissoo.api.utils.render.TextUtil;
import hoprc.api.core.impl.FontManager;
import dev.kissoo.mod.modules.HudElement;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

public class WelcomerHud extends Module implements HudElement {
    public static WelcomerHud INSTANCE;
    private int hudX = -1, hudY = 2;

    private final BooleanSetting shadow = add(new BooleanSetting("Shadow", true));
    private final ColorSetting color = add(new ColorSetting("Color", new Color(208, 0, 0)));

    public WelcomerHud() {
        super("Welcomer", Category.HUD);
        setChinese("欢迎语");
        INSTANCE = this;
    }

    private String getText() {
        if (mc.player == null) return "Welcome :')";
        return "Welcome, §f" + mc.player.getName().getString() + " §r:')";
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) { renderElement(drawContext, tickDelta); }

    @Override public String getElementName() { return getChinese() != null ? getChinese() : getName(); }

    @Override
    public int getElementX() {
        if (hudX < 0) return mc.getWindow().getScaledWidth() / 2 - getElementWidth() / 2;
        return hudX;
    }

    @Override public void setElementX(int x) { hudX = x; }
    @Override public int getElementY() { return hudY; }
    @Override public void setElementY(int y) { hudY = y; }
    @Override public int getElementWidth() { return (int) FontManager.getWidth(getText()) + 4; }
    @Override public int getElementHeight() { return (int) FontManager.getFontHeight() + 2; }
    @Override public boolean isElementVisible() { return isOn(); }

    @Override
    public void renderElement(DrawContext context, float tickDelta) {
        TextUtil.drawString(context, getText(), getElementX(), getElementY(), color.getValue().getRGB(), shadow.getValue());
    }
}