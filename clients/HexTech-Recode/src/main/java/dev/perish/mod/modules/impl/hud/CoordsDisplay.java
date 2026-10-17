package dev.perish.mod.modules.impl.hud;

import dev.perish.api.utils.render.TextUtil;
import dev.perish.core.impl.FontManager;
import dev.perish.mod.gui.hud.IHudPosition;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.ColorSetting;
import java.awt.*;
import net.minecraft.world.World;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;

public class CoordsDisplay extends Module implements IHudPosition {
    public static CoordsDisplay INSTANCE;
    public final BooleanSetting font = this.add(new BooleanSetting("Font", false));
    public final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255)));
    public final BooleanSetting lowerCase = this.add(new BooleanSetting("LowerCase", false));

    private float hudX = 5, hudY, hudW, hudH;

    public CoordsDisplay() {
        super("Coords", Module.Category.Hud);
        this.setChinese("坐标");
        INSTANCE = this;
    }

    @Override
    public void onRender2D(DrawContext context, float tickDelta) {
        if (nullCheck()) return;

        boolean inNether = mc.world.getRegistryKey().equals(World.NETHER);
        int px = mc.player.getBlockX();
        int py = mc.player.getBlockY();
        int pz = mc.player.getBlockZ();
        float f = !inNether ? 0.125F : 8.0F;
        int ox = (int)(mc.player.getX() * f);
        int oz = (int)(mc.player.getZ() * f);

        String text = "XYZ §f" + px + ", " + py + ", " + pz +
                " §7[§f" + ox + ", " + oz + "§7]§f";
        if (this.lowerCase.getValue()) text = text.toLowerCase();

        boolean useFont = this.font.getValue();
        boolean shadow = true;

        float tw = useFont ? FontManager.ui.getWidth(text) : mc.textRenderer.getWidth(text);
        float th = useFont ? FontManager.ui.getFontHeight() : 9;

        hudW = tw + 4;
        hudH = th + 2;

        if (hudY == 0) {
            hudY = mc.getWindow().getScaledHeight() - th - (mc.currentScreen instanceof ChatScreen ? 14 : 2);
        }

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
