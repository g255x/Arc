package dev.g255.arc.mod.modules.impl.render;

import dev.g255.arc.api.utils.render.ColorUtil;
import dev.g255.arc.core.impl.FontManager;
import dev.g255.arc.mod.gui.HudElement;
import dev.g255.arc.mod.gui.clickgui.ClickGuiScreen;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

/**
 * 装备显示（从 Frog 的 ArmorHudModule 移植）。
 *
 * <p>Frog 的版式：每件装备固定 20px 一格的横向槽位，图标 16px，耐久百分比压在图标上方，
 * 颜色按剩余耐久从红渐变到绿，没有底板。
 *
 * <p>和旧版的区别：不再用 Left/Right 锚点做坐标镜像（那套在 HUD 编辑器里换锚点时会让位置整体
 * 跳到屏幕另一侧），现在直接存屏幕坐标，元素框尺寸按实际槽位实时算。
 */
public class ArmorHUD extends Module implements HudElement {
    public static ArmorHUD INSTANCE;

    /** Frog 的渐变端点：0% 红 → 100% 绿 */
    private static final Color MIN_COLOR = new Color(196, 0, 0);
    private static final Color MAX_COLOR = new Color(0, 227, 0);
    private static final int SLOT_GAP = 4;

    private final SliderSetting x = this.add(new SliderSetting("X", 20, 0, 1500));
    private final SliderSetting y = this.add(new SliderSetting("Y", 200, 0, 1000));
    private final SliderSetting size = this.add(new SliderSetting("Size", 16.0, 8.0, 32.0, 1.0));
    private final BooleanSetting durability = this.add(new BooleanSetting("Durability", true));

    public ArmorHUD() {
        super("ArmorHUD", "Displays your armor status on screen", Module.Category.HUD);
        this.setChinese("装备显示");
        INSTANCE = this;
    }

    @Override
    public void onRender2D(DrawContext context, float tickDelta) {
        // HUD 页的编辑器会自己调 render()，这里让开，避免同一帧画两遍
        if (!nullCheck() && !ClickGuiScreen.isEditingHud()) {
            this.renderAt(context, tickDelta);
        }
    }

    @Override
    public void render(DrawContext context, float tickDelta) {
        this.renderAt(context, tickDelta);
    }

    private void renderAt(DrawContext context, float tickDelta) {
        if (nullCheck()) {
            return;
        }
        boolean showDurability = this.durability.getValue();
        float fontSize = FontManager.ui.getFontHeight();
        int topPad = showDurability ? (int)fontSize : 0;
        int cell = this.getSlotSize();
        int px = this.getX();
        int py = this.getY() + topPad;

        int slot = 0;
        for (ItemStack armor : mc.player.getInventory().armor) {
            int ix = px + slot * cell;
            if (!armor.isEmpty()) {
                context.drawItem(armor, ix, py);
                context.drawItemInSlot(mc.textRenderer, armor, ix, py);
                if (showDurability) {
                    int percent = damagePercent(armor);
                    String text = percent + "%";
                    Color color = ColorUtil.fadeColor(MIN_COLOR, MAX_COLOR, percent / 100.0);
                    // Frog 是不居中、紧贴格子上方左沿画的；这里保持同样的观感
                    FontManager.ui.drawString(context.getMatrices(), text,
                            (double)ix + 1.0, (double)((float)py - fontSize), color.getRGB(),
                            FontManager.isShadowEnabled());
                }
            }
            ++slot;
        }
    }

    /** 单格宽度：图标尺寸 + Frog 的 4px 间距 */
    private int getSlotSize() {
        return this.size.getValueInt() + SLOT_GAP;
    }

    /** 剩余耐久百分比（不可损坏的物品按 100 算） */
    private static int damagePercent(ItemStack stack) {
        if (!stack.isDamageable()) {
            return 100;
        }
        int max = stack.getMaxDamage();
        if (max <= 0) {
            return 100;
        }
        return Math.max(0, (max - stack.getDamage()) * 100 / max);
    }

    @Override
    public String getDisplayName() {
        return super.getDisplayName();
    }

    @Override
    public int getX() {
        return this.x.getValueInt();
    }

    @Override
    public void setX(int nx) {
        this.x.setValue((double)nx);
    }

    @Override
    public int getY() {
        return this.y.getValueInt();
    }

    @Override
    public void setY(int ny) {
        this.y.setValue((double)ny);
    }

    @Override
    public int getWidth() {
        int slots = mc.player != null ? mc.player.getInventory().armor.size() : 4;
        return Math.max(1, slots * this.getSlotSize());
    }

    @Override
    public int getHeight() {
        int topPad = this.durability.getValue() ? (int)FontManager.ui.getFontHeight() : 0;
        return this.size.getValueInt() + topPad;
    }

    @Override
    public boolean isVisible() {
        return this.isOn();
    }
}
