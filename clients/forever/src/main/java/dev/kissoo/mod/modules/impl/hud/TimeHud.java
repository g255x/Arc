package dev.kissoo.mod.modules.impl.hud;

import dev.kissoo.api.utils.render.TextUtil;
import hoprc.api.core.impl.FontManager;
import dev.kissoo.mod.modules.HudElement;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeHud extends Module implements HudElement {
    public static TimeHud INSTANCE;
    private int anchorX = 2, hudY = 98;
    private boolean isRightAligned = false;
    private boolean needsInitAnchor = false;
    private int loadedX = 2;

    private final BooleanSetting shadow = add(new BooleanSetting("Shadow", true));
    private final ColorSetting color = add(new ColorSetting("Color", new Color(208, 0, 0)));

    public TimeHud() {
        super("Time", Category.HUD);
        setChinese("时间显示");
        INSTANCE = this;
    }

    private String getText() {
        return "Time §f" + new SimpleDateFormat("h:mm a", Locale.ENGLISH).format(new Date());
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) { renderElement(drawContext, tickDelta); }

    @Override public String getElementName() { return getChinese() != null ? getChinese() : getName(); }

    @Override
    public int getElementX() {
        if (isRightAligned) return anchorX - getElementWidth();
        return anchorX;
    }

    @Override
    public void setElementX(int x) {
        if (mc == null || mc.getWindow() == null) {
            loadedX = x;
            anchorX = x;
            needsInitAnchor = true;
            return;
        }

        needsInitAnchor = false;
        int screenWidth = mc.getWindow().getScaledWidth();
        int hudCenterX = x + (getElementWidth() / 2);
        if (hudCenterX > screenWidth / 2) {
            isRightAligned = true;
            anchorX = x + getElementWidth();
        } else {
            isRightAligned = false;
            anchorX = x;
        }
    }

    @Override public int getElementY() { return hudY; }
    @Override public void setElementY(int y) { hudY = y; }
    @Override public int getElementWidth() { return (int) FontManager.getWidth(getText()) + 4; }
    @Override public int getElementHeight() { return (int) FontManager.getFontHeight() + 2; }
    @Override public boolean isElementVisible() { return isOn(); }

    @Override
    public void renderElement(DrawContext context, float tickDelta) {
        if (needsInitAnchor) {
            setElementX(loadedX);
        }
        TextUtil.drawString(context, getText(), getElementX(), hudY, color.getValue().getRGB(), shadow.getValue());
    }
}