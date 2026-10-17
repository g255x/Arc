package dev.kissoo.mod.modules.impl.hud;

import dev.kissoo.api.utils.player.EntityUtil;
import dev.kissoo.api.utils.render.ColorUtil;
import dev.kissoo.api.utils.render.TextUtil;
import hoprc.api.core.impl.FontManager;
import dev.kissoo.mod.modules.HudElement;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

import java.awt.*;

public class ArmorHud extends Module implements HudElement {
    public static ArmorHud INSTANCE;

    private int hudX = -1;
    private int hudY = -1;
    private AlignMode alignMode = AlignMode.LEFT;

    private static final int ITEM_SIZE = 16;
    private static final int ITEM_SPACING = 20; // 统一间距
    private static final int ARMOR_SLOTS = 4;

    public final BooleanSetting shadow = add(new BooleanSetting("Shadow", true));
    public final BooleanSetting durability = add(new BooleanSetting("Durability", true));

    private static final Color MIN_COLOR = new Color(196, 0, 0);
    private static final Color MAX_COLOR = new Color(0, 227, 0);

    public ArmorHud() {
        super("Armor", Category.HUD);
        setChinese("装备显示");
        INSTANCE = this;
    }
    private void initDefaultPosition() {
        if (mc != null && mc.getWindow() != null) {
            hudX = mc.getWindow().getScaledWidth() / 2 - getElementWidth() / 2;
            hudY = mc.getWindow().getScaledHeight() - 55;
            alignMode = AlignMode.LEFT;
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
        if (hudX < 0) {
            initDefaultPosition();
        }
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
        if (hudY < 0) {
            initDefaultPosition();
        }
        return hudY;
    }

    @Override
    public void setElementY(int y) {
        hudY = y;
    }

    @Override
    public int getElementWidth() {
        return ITEM_SPACING * ARMOR_SLOTS;
    }

    @Override
    public int getElementHeight() {
        return ITEM_SIZE + (durability.getValue() ? 6 : 0);
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
        if (mc.player == null) return;

        int baseX = getElementX();
        int baseY = getElementY();
        int drawX = baseX;
        for (int i = 3; i >= 0; i--) {
            ItemStack stack = mc.player.getInventory().getArmorStack(i);
            if (!stack.isEmpty()) {
                context.drawItem(stack, drawX, baseY);
                context.drawItemInSlot(mc.textRenderer, stack, drawX, baseY);

                if (durability.getValue() && stack.isDamageable()) {
                    int damage = EntityUtil.getDamagePercent(stack);
                    Color color = ColorUtil.fadeColor(MIN_COLOR, MAX_COLOR, damage / 100.0F);

                    if (FontManager.isCustom()) {
                        FontManager.getSmall().drawString(
                                context.getMatrices(),
                                damage + "%",
                                drawX + 1,
                                baseY - FontManager.getSmall().getFontHeight() / 2.0F,
                                color.getRGB(),
                                shadow.getValue()
                        );
                    } else {
                        TextUtil.drawStringScale(
                                context,
                                damage + "%",
                                drawX + 2,
                                baseY - 9.0F / 4.0F,
                                color.getRGB(),
                                0.5F,
                                shadow.getValue()
                        );
                    }
                }
            }
            drawX += ITEM_SPACING;
        }
    }
}