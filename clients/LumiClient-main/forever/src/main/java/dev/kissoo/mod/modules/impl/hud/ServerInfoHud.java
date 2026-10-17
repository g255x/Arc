package dev.kissoo.mod.modules.impl.hud;

import dev.kissoo.api.utils.render.TextUtil;
import hoprc.api.core.impl.FontManager;
import dev.kissoo.mod.modules.HudElement;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

public class ServerInfoHud extends Module implements HudElement {
    public static ServerInfoHud INSTANCE;
    private int hudX = 2, hudY = 110;

    private final EnumSetting<Mode> mode = add(new EnumSetting<>("Mode", Mode.IP));
    private final BooleanSetting shadow = add(new BooleanSetting("Shadow", true));
    private final ColorSetting color = add(new ColorSetting("Color", new Color(208, 0, 0)));

    public ServerInfoHud() {
        super("ServerInfo", Category.HUD);
        setChinese("服务器信息");
        INSTANCE = this;
    }

    private String getText() {
        if (mode.getValue() == Mode.IP) {
            return "Server §f" + (mc.isInSingleplayer() ? "SinglePlayer" : (mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "Unknown"));
        } else {
            return "Brand §f" + (mc.isInSingleplayer() ? "Vanilla" : (mc.getNetworkHandler() != null ? mc.getNetworkHandler().getBrand().replaceAll("\\(.*?\\)", "") : "Unknown"));
        }
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) { renderElement(drawContext, tickDelta); }

    @Override public String getElementName() { return getChinese() != null ? getChinese() : getName(); }
    @Override public int getElementX() { return hudX; }
    @Override public void setElementX(int x) { hudX = x; }
    @Override public int getElementY() { return hudY; }
    @Override public void setElementY(int y) { hudY = y; }
    @Override public int getElementWidth() { return (int) FontManager.getWidth(getText()) + 4; }
    @Override public int getElementHeight() { return (int) FontManager.getFontHeight() + 2; }
    @Override public boolean isElementVisible() { return isOn(); }

    @Override
    public void renderElement(DrawContext context, float tickDelta) {
        TextUtil.drawString(context, getText(), hudX, hudY, color.getValue().getRGB(), shadow.getValue());
    }

    private enum Mode { IP, Brand }
}