package dev.perish.mod.modules.impl.hud;

import dev.perish.api.utils.render.TextUtil;
import dev.perish.core.impl.FontManager;
import dev.perish.mod.gui.hud.IHudPosition;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.ColorSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;
import java.awt.*;
import net.minecraft.client.gui.DrawContext;

public class WaterMarkDisplay extends Module implements IHudPosition {
    public static WaterMarkDisplay INSTANCE;
    public final BooleanSetting font = this.add(new BooleanSetting("Font", false));
    public final ColorSetting color = this.add(new ColorSetting("Color", new Color(153, 153, 255)));
    public final SliderSetting offset = this.add(new SliderSetting("Offset", 1.0, 0.0, 100.0, -1.0));

    private float hudX = 5, hudY = 5, hudW, hudH;

    public WaterMarkDisplay() {
        super("WaterMark", Module.Category.Hud);
        this.setChinese("水印");
        INSTANCE = this;
    }

    @Override
    public void onRender2D(DrawContext context, float tickDelta) {
        if (nullCheck()) return;

        String text = dev.perish.Perish.ClientName + " §f" + dev.perish.Perish.VERSION;
        boolean useFont = this.font.getValue();
        boolean shadow = true;

        float tw = useFont ? FontManager.ui.getWidth(text) : mc.textRenderer.getWidth(text);
        float th = useFont ? FontManager.ui.getFontHeight() : 9;

        hudW = tw + 4;
        hudH = th + 2;

        int color = this.color.getValue().getRGB();

        if (this.color.isGlobalPulse()) {
            TextUtil.drawStringSetting(context, text, hudX + 2, hudY + 1, this.color, useFont, shadow);
        } else if (useFont) {
            FontManager.ui.drawString(context.getMatrices(), text, hudX + 2, hudY + 1, color, shadow);
        } else {
            TextUtil.drawString(context, text, hudX + 2, hudY + 1, color, false, shadow);
        }
    }

    public float getHudX() { return hudX; }
    public float getHudY() { return hudY; }
    public float getHudW() { return hudW; }
    public float getHudH() { return hudH; }

    public void setHudPos(float x, float y) { this.hudX = x; this.hudY = y; }
}
