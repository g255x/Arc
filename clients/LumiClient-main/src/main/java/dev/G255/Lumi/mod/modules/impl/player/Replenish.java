package dev.G255.Lumi.mod.modules.impl.player;

import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.UpdateEvent;
import dev.G255.Lumi.api.utils.math.Timer;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.settings.impl.EnumSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

public class Replenish extends Module {
    private final EnumSetting<Mode> mode = this.add(new EnumSetting<>("Mode", Mode.QuickMove));
    private final SliderSetting delay = this.add(new SliderSetting("Delay", 2.0, 0.0, 5.0, 0.01).setSuffix("s"));
    private final SliderSetting min = this.add(new SliderSetting("Min", 50, 1, 100)).setSuffix("%");
    private final Timer timer = new Timer();

    public Replenish() {
        super("Replenish", Module.Category.Player);
        this.setChinese("");
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        for (int i = 0; i < 9; ++i) {
            if (!this.replenish(i)) continue;
            this.timer.reset();
            return;
        }
    }

    private boolean replenish(int slot) {
        ItemStack stack = Replenish.mc.player.getInventory().getStack(slot);
        if (stack.isEmpty()) return false;
        if (!stack.isStackable()) return false;

        int percent = (int) ((double) stack.getCount() / (double) stack.getMaxCount() * 100.0);
        if ((double) percent > this.min.getValue()) return false;

        // æ£æ¥å»¶æ?
        if (!this.timer.passedS(this.delay.getValue())) return false;

        for (int i = 9; i < 36; ++i) {
            ItemStack item = Replenish.mc.player.getInventory().getStack(i);
            if (item.isEmpty()) continue;
            if (!canMerge(stack, item)) continue;

            if (this.mode.getValue() == Mode.QuickMove) {
                Replenish.mc.interactionManager.clickSlot(
                        Replenish.mc.player.playerScreenHandler.syncId,
                        i, 0, SlotActionType.QUICK_MOVE,
                        (PlayerEntity) Replenish.mc.player
                );
            } else { // ClickSlot
                Replenish.mc.interactionManager.clickSlot(
                        Replenish.mc.player.playerScreenHandler.syncId,
                        i, 0, SlotActionType.PICKUP,
                        (PlayerEntity) Replenish.mc.player
                );
                Replenish.mc.interactionManager.clickSlot(
                        Replenish.mc.player.playerScreenHandler.syncId,
                        slot + 36, 0, SlotActionType.PICKUP,
                        (PlayerEntity) Replenish.mc.player
                );
                Replenish.mc.interactionManager.clickSlot(
                        Replenish.mc.player.playerScreenHandler.syncId,
                        i, 0, SlotActionType.PICKUP,
                        (PlayerEntity) Replenish.mc.player
                );
            }
            return true;
        }
        return false;
    }

    // åé¨å¤æ­ä¸¤ä¸ªç©åæ¯å¦å¯ä»¥åå¹¶ï¼æ¿ä»£å Sorter.canMergeï¼?
    private boolean canMerge(ItemStack a, ItemStack b) {
        return ItemStack.areItemsAndComponentsEqual(a, b);
    }

    public enum Mode {
        QuickMove,
        ClickSlot
    }
}