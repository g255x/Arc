package dev.perish.mod.modules.impl.hud;

import dev.perish.api.utils.render.Render2DUtil;
import dev.perish.mod.gui.hud.IHudPosition;
import dev.perish.mod.gui.hud.HudEditorManager;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.impl.ColorSetting;
import java.awt.*;
import net.minecraft.client.gui.DrawContext;

public class CrosshairDisplay extends Module implements IHudPosition {
    public static CrosshairDisplay INSTANCE;
    public final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 200)));
    private float hudX, hudY, hudW = 10, hudH = 10;

    public CrosshairDisplay() {
        super("Crosshair", Category.Hud);
        this.setChinese("准星");
        INSTANCE = this;
    }

    @Override
    public void onRender2D(DrawContext context, float tickDelta) {
        if (!isOn() || nullCheck()) return;
        if (!HudEditorManager.INSTANCE.isEditMode()) return;

        if (hudX == 0 && hudY == 0) {
            hudX = mc.getWindow().getScaledWidth() / 2f - 5f;
            hudY = mc.getWindow().getScaledHeight() / 2f - 5f;
        }

        int color = this.color.getValue().getRGB();
        float gap = 4f;
        float len = 6f;
        float thick = 1.0f;
        float cx = hudX + 5;
        float cy = hudY + 5;

        Render2DUtil.drawRect(context.getMatrices(), cx - len, cy - thick / 2, gap - len + thick, thick, color);
        Render2DUtil.drawRect(context.getMatrices(), cx + gap, cy - thick / 2, len - gap, thick, color);
        Render2DUtil.drawRect(context.getMatrices(), cx - thick / 2, cy - len, thick, gap - len + thick, color);
        Render2DUtil.drawRect(context.getMatrices(), cx - thick / 2, cy + gap, thick, len - gap, color);
    }

    public float getHudX() { return hudX; }
    public float getHudY() { return hudY; }
    public float getHudW() { return hudW; }
    public float getHudH() { return hudH; }
    public void setHudPos(float x, float y) { this.hudX = x; this.hudY = y; }
}
