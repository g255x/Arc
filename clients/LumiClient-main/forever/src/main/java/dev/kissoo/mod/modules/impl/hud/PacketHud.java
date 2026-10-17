package dev.kissoo.mod.modules.impl.hud;

import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.PacketEvent;
import dev.kissoo.api.utils.math.Timer;
import dev.kissoo.api.utils.render.TextUtil;
import hoprc.api.core.impl.FontManager;
import dev.kissoo.mod.modules.HudElement;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

public class PacketHud extends Module implements HudElement {
    public static PacketHud INSTANCE;
    private int hudX = 2, hudY = 122;

    private final BooleanSetting shadow = add(new BooleanSetting("Shadow", true));
    private final ColorSetting color = add(new ColorSetting("Color", new Color(208, 0, 0)));

    private int incomingPackets = 0, outgoingPackets = 0;
    private int lastIn = 0, lastOut = 0;
    private final Timer resetTimer = new Timer();

    public PacketHud() {
        super("Packets", Category.HUD);
        setChinese("数据包显示");
        INSTANCE = this;
    }

    private String getText() {
        return "Packets §f" + lastIn + "<-" + lastOut;
    }

    @EventListener
    public void onReceive(PacketEvent.Receive e) {
        incomingPackets++;
    }

    @EventListener
    public void onSend(PacketEvent.Send e) {
        outgoingPackets++;
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        if (resetTimer.passedMs(150)) {
            lastIn = incomingPackets;
            lastOut = outgoingPackets;
            incomingPackets = 0;
            outgoingPackets = 0;
            resetTimer.reset();
        }
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
    public void renderElement(DrawContext context, float tickDelta) {
        TextUtil.drawString(context, getText(), hudX, hudY, color.getValue().getRGB(), shadow.getValue());
    }
}