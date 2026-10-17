package dev.perish.mod.modules.impl.hud;

import dev.perish.api.utils.player.InventoryUtil;
import dev.perish.mod.gui.hud.IHudPosition;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.block.PistonBlock;
import net.minecraft.client.gui.DrawContext;

public class ItemsCounter extends Module implements IHudPosition {
    public static ItemsCounter INSTANCE;
    public final BooleanSetting hideEmpty = this.add(new BooleanSetting("HideEmpty", true));
    public final BooleanSetting crystal = this.add(new BooleanSetting("Crystal", true));
    public final BooleanSetting xp = this.add(new BooleanSetting("XP", true));
    public final BooleanSetting pearl = this.add(new BooleanSetting("Pearl", true));
    public final BooleanSetting obsidian = this.add(new BooleanSetting("Obsidian", true));
    public final BooleanSetting egApple = this.add(new BooleanSetting("E-GApple", true));
    public final BooleanSetting gApple = this.add(new BooleanSetting("GApple", true));
    public final BooleanSetting totem = this.add(new BooleanSetting("Totem", true));
    public final BooleanSetting web = this.add(new BooleanSetting("Web", true));
    public final BooleanSetting anchor = this.add(new BooleanSetting("Anchor", true));
    public final BooleanSetting glowstone = this.add(new BooleanSetting("Glowstone", true));
    public final BooleanSetting piston = this.add(new BooleanSetting("Piston", true));
    public final BooleanSetting redstone = this.add(new BooleanSetting("RedStone", true));
    public final BooleanSetting enderChest = this.add(new BooleanSetting("EnderChest", true));
    public final BooleanSetting firework = this.add(new BooleanSetting("Firework", true));
    public final SliderSetting xOffset = this.add(new SliderSetting("X", 100, 0, 1500));
    public final SliderSetting yOffset = this.add(new SliderSetting("Y", 100, 0, 1000));
    public final SliderSetting offset = this.add(new SliderSetting("Offset", 18, 0, 30));
    private final ItemStack crystalStack = new ItemStack(Items.END_CRYSTAL);

    public ItemsCounter() {
        super("Items", Category.Hud);
        this.setChinese("物品数量");
        INSTANCE = this;
    }

    private float hudX = 100, hudY = 100, hudW, hudH;

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        if (!isOn() || nullCheck()) return;

        int off = this.offset.getValueInt();
        int cx = 0;
        int count = 0;

        ItemEntry[] items = {
                entry(new ItemStack(Items.END_CRYSTAL), this.crystal.getValue()),
                entry(new ItemStack(Items.EXPERIENCE_BOTTLE), this.xp.getValue()),
                entry(new ItemStack(Items.ENDER_PEARL), this.pearl.getValue()),
                entry(new ItemStack(Items.OBSIDIAN), this.obsidian.getValue()),
                entry(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE), this.egApple.getValue()),
                entry(new ItemStack(Items.GOLDEN_APPLE), this.gApple.getValue()),
                entry(new ItemStack(Items.TOTEM_OF_UNDYING), this.totem.getValue()),
                entry(new ItemStack(Items.COBWEB), this.web.getValue()),
                entry(new ItemStack(Items.RESPAWN_ANCHOR), this.anchor.getValue()),
                entry(new ItemStack(Items.GLOWSTONE), this.glowstone.getValue()),
                entry(new ItemStack(Items.REDSTONE_BLOCK), this.redstone.getValue()),
                entry(new ItemStack(Items.ENDER_CHEST), this.enderChest.getValue()),
                entry(new ItemStack(Items.FIREWORK_ROCKET), this.firework.getValue())
        };

        for (ItemEntry item : items) {
            if (!item.enabled) continue;
            int cnt = InventoryUtil.getItemCount(item.stack.getItem());
            if (this.hideEmpty.getValue() && cnt <= 0) continue;
            ItemStack display = item.stack.copy();
            display.setCount(Math.max(cnt, 1));
            drawContext.drawItem(display, (int)(hudX + cx), (int)hudY);
            drawContext.drawItemInSlot(mc.textRenderer, display, (int)(hudX + cx), (int)hudY);
            cx += off;
            count++;
        }

        if (this.piston.getValue()) {
            int pcnt = InventoryUtil.getItemCount(PistonBlock.class);
            if (!this.hideEmpty.getValue() || pcnt > 0) {
                ItemStack ps = new ItemStack(Items.PISTON);
                ps.setCount(Math.max(pcnt, 1));
                drawContext.drawItem(ps, (int)(hudX + cx), (int)hudY);
                drawContext.drawItemInSlot(mc.textRenderer, ps, (int)(hudX + cx), (int)hudY);
                cx += off;
                count++;
            }
        }

        hudW = count > 0 ? cx : 0;
        hudH = count > 0 ? 18 : 0;
    }

    public float getHudX() { return hudX; }
    public float getHudY() { return hudY; }
    public float getHudW() { return hudW; }
    public float getHudH() { return hudH; }
    public void setHudPos(float x, float y) { this.hudX = x; this.hudY = y; }

    private static ItemEntry entry(ItemStack stack, boolean enabled) {
        return new ItemEntry(stack, enabled);
    }

    private static class ItemEntry {
        final ItemStack stack;
        final boolean enabled;
        ItemEntry(ItemStack stack, boolean enabled) { this.stack = stack; this.enabled = enabled; }
    }
}
