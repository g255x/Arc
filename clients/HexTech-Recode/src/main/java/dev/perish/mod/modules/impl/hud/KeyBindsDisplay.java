package dev.perish.mod.modules.impl.hud;

import dev.perish.Perish;
import dev.perish.api.utils.render.TextUtil;
import dev.perish.core.impl.FontManager;
import dev.perish.mod.gui.hud.IHudPosition;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.impl.client.HUD;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.ColorSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;

public class KeyBindsDisplay extends Module implements IHudPosition {
    public static KeyBindsDisplay INSTANCE;
    public final BooleanSetting font = this.add(new BooleanSetting("Font", true));
    public final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255)));
    private float hudX = 5, hudY = 100, hudW, hudH;

    public KeyBindsDisplay() {
        super("KeyBinds", Category.Hud);
        this.setChinese("按键绑定");
        INSTANCE = this;
    }

    @Override
    public void onRender2D(DrawContext context, float tickDelta) {
        if (!isOn() || nullCheck()) return;

        List<Module> active = new ArrayList<>();
        for (Module m : Perish.MODULE.getModules()) {
            if (m.isOn() && m.getBindSetting().getValue() != -1) active.add(m);
        }
        if (active.isEmpty()) { hudW = 0; hudH = 0; return; }

        boolean useFont = this.font.getValue();
        float fh = useFont ? FontManager.ui.getFontHeight() : 9;
        float cy = 0;
        float maxW = 0;

        for (Module m : active) {
            String keyName = getKeyName(m.getBindSetting().getValue());
            String text = m.getName() + " : " + keyName;
            int color = this.color.getValue().getRGB();

            float tw = useFont ? FontManager.ui.getWidth(text) : mc.textRenderer.getWidth(text);
            maxW = Math.max(maxW, tw);
            if (this.color.isGlobalPulse()) TextUtil.drawStringSetting(context, text, hudX, hudY + cy, this.color, useFont, false);
            else if (useFont) FontManager.ui.drawString(context.getMatrices(), text, hudX, hudY + cy, color, false);
            else context.drawText(mc.textRenderer, text, (int) hudX, (int) (hudY + cy), color, false);
            cy += fh + 2;
        }

        hudW = maxW + 4;
        hudH = cy;
    }

    private String getKeyName(int code) {
        try {
            return InputUtil.fromKeyCode(code, 0).getLocalizedText().getString();
        } catch (Exception e) {
            return "K" + code;
        }
    }

    public float getHudX() { return hudX; }
    public float getHudY() { return hudY; }
    public float getHudW() { return hudW; }
    public float getHudH() { return hudH; }
    public void setHudPos(float x, float y) { this.hudX = x; this.hudY = y; }
}
