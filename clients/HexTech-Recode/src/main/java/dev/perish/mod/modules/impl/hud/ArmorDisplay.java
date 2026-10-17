package dev.perish.mod.modules.impl.hud;

import dev.perish.core.impl.FontManager;
import dev.perish.mod.gui.hud.IHudPosition;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.ColorSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;
import java.awt.*;
import net.minecraft.item.ItemStack;
import net.minecraft.client.gui.DrawContext;

public class ArmorDisplay extends Module implements IHudPosition {
    public static ArmorDisplay INSTANCE;
    public final BooleanSetting durability = this.add(new BooleanSetting("Durability", true));
    public final BooleanSetting font = this.add(new BooleanSetting("Font", true));
    public final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255)));
    public final SliderSetting offset = this.add(new SliderSetting("Offset", 1.0, 0.0, 100.0, -1.0));

    private float hudX, hudY, hudW, hudH;

    public ArmorDisplay() {
        super("Armor", Module.Category.Hud);
        this.setChinese("护甲显示");
        INSTANCE = this;
    }

    @Override
    public void onRender2D(DrawContext context, float tickDelta) {
        if (nullCheck()) return;

        float sz = 16, sp = 3;
        float tw = sz * 4 + sp * 3;
        float th = sz;
        boolean showDur = this.durability.getValue();
        boolean useFont = this.font.getValue();
        if (showDur) th += (useFont ? FontManager.small.getFontHeight() : 9) + 1;

        if (hudX == 0 && hudY == 0) {
            hudX = mc.getWindow().getScaledWidth() / 2f - 36f;
            hudY = mc.getWindow().getScaledHeight() - 55f;
        }

        ItemStack[] armor = {
                mc.player.getInventory().armor.get(3),
                mc.player.getInventory().armor.get(2),
                mc.player.getInventory().armor.get(1),
                mc.player.getInventory().armor.get(0)
        };

        float cx = 0;
        for (ItemStack a : armor) {
            if (a.isEmpty()) { cx += sz + sp; continue; }
            context.drawItem(a, (int)(hudX + cx), (int)hudY);
            context.drawItemInSlot(mc.textRenderer, a, (int)(hudX + cx), (int)hudY);

            if (showDur) {
                float maxDmg = a.getMaxDamage();
                float dmg = a.getDamage();
                float dur = maxDmg - dmg;
                float pct = maxDmg > 0 ? dur / maxDmg : 1f;
                String ds = String.valueOf((int)dur);
                int dc = this.color.getValue().getRGB();
                float textWidth = useFont ? FontManager.small.getWidth(ds) : mc.textRenderer.getWidth(ds);
                float textX = hudX + cx + (sz - textWidth) / 2;
                if (useFont) FontManager.small.drawString(context.getMatrices(), ds, textX, hudY + sz + 1, dc, true);
                else context.drawText(mc.textRenderer, ds, (int) textX, (int) (hudY + sz + 1), dc, true);
            }
            cx += sz + sp;
        }

        hudW = tw;
        hudH = th;
    }

    public float getHudX() { return hudX; }
    public float getHudY() { return hudY; }
    public float getHudW() { return hudW; }
    public float getHudH() { return hudH; }

    public void setHudPos(float x, float y) { this.hudX = x; this.hudY = y; }
}
