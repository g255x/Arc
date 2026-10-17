package dev.kissoo.mod.modules.impl.hud;

import dev.kissoo.core.impl.BlurManager;
import dev.kissoo.mod.modules.HudElement;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

import java.awt.*;

public class BackpackContents extends Module implements HudElement {
    public static BackpackContents INSTANCE;
    private int hudX = 100, hudY = 100;

    private final SliderSetting slotSize = add(new SliderSetting("SlotSize", 16, 12, 24));
    private final SliderSetting spacing = add(new SliderSetting("Spacing", 2, 0, 6));
    private final BooleanSetting showBackground = add(new BooleanSetting("ShowBackground", true));
    private final SliderSetting blurStrength = add(new SliderSetting("BlurStrength", 50, 0, 100));
    private final BooleanSetting showBorder = add(new BooleanSetting("ShowBorder", true));
    private final ColorSetting borderColor = add(new ColorSetting("BorderColor", new Color(255, 255, 255)));
    private final BooleanSetting showCount = add(new BooleanSetting("ShowCount", true));
    private final BooleanSetting showDurability = add(new BooleanSetting("ShowDurability", true));

    public BackpackContents() {
        super("BackpackContents", Category.HUD);
        setChinese("背包物资显示");
        INSTANCE = this;
    }

    @Override public String getElementName() { return getChinese() != null ? getChinese() : getName(); }
    @Override public int getElementX() { return hudX; }
    @Override public void setElementX(int x) { hudX = x; }
    @Override public int getElementY() { return hudY; }
    @Override public void setElementY(int y) { hudY = y; }

    @Override
    public int getElementWidth() {
        return 9 * slotSize.getValueInt() + 8 * spacing.getValueInt();
    }

    @Override
    public int getElementHeight() {
        return 3 * slotSize.getValueInt() + 2 * spacing.getValueInt();
    }

    @Override public boolean isElementVisible() { return isOn(); }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) { renderElement(drawContext, tickDelta); }

    @Override
    public void renderElement(DrawContext drawContext, float tickDelta) {
        if (mc.player == null) return;

        int ss = slotSize.getValueInt();
        int sp = spacing.getValueInt();
        int tw = getElementWidth();
        int th = getElementHeight();
        int sx = hudX, sy = hudY;

        if (showBackground.getValue()) {
            float percentage = blurStrength.getValueInt() / 100.0f;
            float actualRadius = percentage * 20.0f;

            if (actualRadius > 0) {
                drawContext.draw();
                new BlurManager().applyBlur(actualRadius, sx, sy, tw, th);
            }

            drawContext.fill(sx, sy, sx + tw, sy + th, 0x40000000);
        }

        if (showBorder.getValue()) {
            int c = borderColor.getValue().getRGB();
            drawContext.fill(sx - 1, sy - 1, sx + tw + 1, sy, c);
            drawContext.fill(sx - 1, sy + th, sx + tw + 1, sy + th + 1, c);
            drawContext.fill(sx - 1, sy, sx, sy + th, c);
            drawContext.fill(sx + tw, sy, sx + tw + 1, sy + th, c);
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                ItemStack stack = mc.player.getInventory().getStack(row * 9 + col + 9);
                if (stack.isEmpty()) continue;
                int x = sx + col * (ss + sp);
                int y = sy + row * (ss + sp);
                drawContext.drawItem(stack, x + 1, y + 1);
                if (showCount.getValue() || showDurability.getValue()) {
                    ItemStack rs = stack.copy();
                    if (!showCount.getValue()) rs.setCount(1);
                    drawContext.drawItemInSlot(mc.textRenderer, rs, x + 1, y + 1);
                }
            }
        }
    }
}