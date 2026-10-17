package dev.kissoo.mod.modules.impl.hud;

import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.ClientTickEvent;
import dev.kissoo.api.utils.math.Timer;
import dev.kissoo.api.utils.render.TextUtil;
import hoprc.api.core.impl.FontManager;
import dev.kissoo.mod.modules.HudElement;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;

import java.awt.*;

public class PingHud extends Module implements HudElement {
    public static PingHud INSTANCE;

    private int hudX = 2, hudY = 62;
    private AlignMode alignMode = AlignMode.LEFT;

    private final BooleanSetting shadow = add(new BooleanSetting("Shadow", true));
    private final ColorSetting color = add(new ColorSetting("Color", new Color(208, 0, 0)));
    private int cachedPing = 0;
    private final Timer refreshTimer = new Timer();

    public PingHud() {
        super("Ping", Category.HUD);
        setChinese("延迟显示");
        INSTANCE = this;
    }

    private String getText() {
        return "Ping §f" + cachedPing + "ms";
    }

    @EventListener
    public void onTick(ClientTickEvent event) {
        if (nullCheck() || !event.isPost()) return;
        if (!refreshTimer.passedMs(1000)) return;
        refreshTimer.reset();

        if (mc.getNetworkHandler() == null || mc.player == null) return;

        PlayerListEntry entry = mc.getNetworkHandler()
                .getPlayerListEntry(mc.player.getUuid());

        if (entry != null) {
            cachedPing = entry.getLatency();
        }
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
        TextUtil.drawString(
                context,
                getText(),
                renderX,
                hudY,
                color.getValue().getRGB(),
                shadow.getValue()
        );
    }
}