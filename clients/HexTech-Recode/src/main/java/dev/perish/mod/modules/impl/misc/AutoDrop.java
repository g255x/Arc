package dev.perish.mod.modules.impl.misc;

import dev.perish.Perish;
import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.UpdateEvent;
import dev.perish.api.utils.math.Timer;
import dev.perish.api.utils.player.EntityUtil;
import dev.perish.core.Manager;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;
import dev.perish.mod.modules.settings.impl.StringSetting;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.block.ShulkerBoxBlock;

public class AutoDrop extends Module {
    public static AutoDrop INSTANCE;

    private final SliderSetting tasksPerTick = this.add(new SliderSetting("TasksPerTick", 2, 1, 20));
    private final SliderSetting delay = this.add(new SliderSetting("Delay", 0.1, 0.0, 5.0, 0.01).setSuffix("s"));
    private final BooleanSetting kit = this.add(new BooleanSetting("Kit", true));
    private final StringSetting kitName = this.add(new StringSetting("KitName", "1"));
    private final BooleanSetting dropWrongItems = this.add(new BooleanSetting("DropWrongItems", true));
    private final BooleanSetting dropExcess = this.add(new BooleanSetting("DropExcess", true));
    private final BooleanSetting ignoreArmor = this.add(new BooleanSetting("IgnoreArmor", true));
    private final BooleanSetting ignoreShulker = this.add(new BooleanSetting("IgnoreShulker", true));
    private final BooleanSetting onlyHotbar = this.add(new BooleanSetting("OnlyHotbar", false));
    private final BooleanSetting dropEnabled = this.add(new BooleanSetting("DropEnabled", true));
    private final BooleanSetting sortItems = this.add(new BooleanSetting("SortItems", true));
    private final BooleanSetting inventoryOnly = this.add(new BooleanSetting("InventoryOnly", false));

    private final Timer timer = new Timer();
    private final Map<Integer, String> kitMap = new ConcurrentHashMap();

    public AutoDrop() {
        super("AutoDrop", Category.Misc);//liwangzi AI Power.不是我写的。
        this.setChinese("自动丢弃");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        if (!nullCheck()) {
            loadKit();
        }
    }

    private void loadKit() {
        Perish.THREAD.execute(() -> {
            this.kitMap.clear();

            try {
                File file = Manager.getFile(this.kitName.getValue() + ".kit");
                if (!file.exists()) {
                    sendMessage("§cKit file not found: " + this.kitName.getValue() + ".kit");
                    return;
                }

                for (String s : IOUtils.readLines(new FileInputStream(file), StandardCharsets.UTF_8)) {
                    String[] split = s.split(":", 3);
                    if (split.length < 2) {
                        continue;
                    }

                    this.kitMap.put(Integer.valueOf(split[0]), split[1]);
                }

                sendMessage("§aLoaded kit: " + this.kitName.getValue() + " with " + this.kitMap.size() + " items");
            } catch (Exception e) {
                e.printStackTrace();
                sendMessage("§cError loading kit: " + e.getMessage());
            }
        });
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (this.timer.passedS(this.delay.getValue())) {
            // Check if we should process based on inventory state
            boolean shouldProcess = !this.inventoryOnly.getValue() || EntityUtil.inInventory();

            if (shouldProcess) {
                for (int i = 0; i < this.tasksPerTick.getValue(); i++) {
                    // Priority 1: Sort items to correct positions
                    if (processSort()) {
                        this.timer.reset();
                        return;
                    }
                    // Priority 2: Drop wrong items
                    if (processDrop()) {
                        this.timer.reset();
                        return;
                    }
                }
            }
        }
    }

    private boolean processDrop() {
        if (!this.kit.getValue() || this.kitMap.isEmpty() || !this.dropEnabled.getValue()) {
            return false;
        }

        int startSlot = 0;
        int endSlot = this.onlyHotbar.getValue() ? 9 : 36;

        for (int slot = startSlot; slot < endSlot; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (stack.isEmpty()) {
                continue;
            }

            // Skip armor and shulker boxes if configured
            if (this.ignoreArmor.getValue() && stack.getItem() instanceof ArmorItem) {
                continue;
            }

            if (this.ignoreShulker.getValue() && stack.getItem() instanceof BlockItem blockItem &&
                    blockItem.getBlock() instanceof ShulkerBoxBlock) {
                continue;
            }

            // Check if this slot has a kit definition
            if (this.kitMap.containsKey(slot)) {
                String expectedItem = this.kitMap.get(slot);
                String actualItem = stack.getItem().getTranslationKey();

                // If the item doesn't match the expected item for this slot, drop it
                if (this.dropWrongItems.getValue() && !actualItem.equals(expectedItem)) {
                    dropItem(slot);
                    return true;
                }
            } else if (this.dropWrongItems.getValue()) {
                // If slot is not in kit and we want to drop wrong items, drop it
                dropItem(slot);
                return true;
            }
        }

        return false;
    }

    private void dropItem(int slot) {
        if (!this.dropEnabled.getValue()) {
            return;
        }

        // Safety check: only drop when inventory is open or we have proper access
        if (!EntityUtil.inInventory() && this.inventoryOnly.getValue()) {
            return;
        }

        try {
            int syncId = mc.player.currentScreenHandler.syncId;
            int slotId = slot < 9 ? slot + 36 : slot;
            mc.interactionManager.clickSlot(syncId, slotId, 1, SlotActionType.THROW, mc.player);
        } catch (Exception e) {
            // Silently handle cases where we can't access inventory
        }
    }

    private boolean processSort() {
        if (!this.kit.getValue() || this.kitMap.isEmpty() || !this.sortItems.getValue()) {
            return false;
        }

        // Check if we should sort based on inventory state and settings
        boolean shouldSort = !this.inventoryOnly.getValue() || EntityUtil.inInventory();
        if (!shouldSort) {
            return false;
        }

        int startSlot = 0;
        int endSlot = this.onlyHotbar.getValue() ? 9 : 36;

        // Try to find items that should be moved to their correct kit positions
        for (int targetSlot = startSlot; targetSlot < endSlot; targetSlot++) {
            if (!this.kitMap.containsKey(targetSlot)) {
                continue;
            }

            String expectedItem = this.kitMap.get(targetSlot);
            ItemStack currentStack = mc.player.getInventory().getStack(targetSlot);
            String currentItem = currentStack.isEmpty() ? "" : currentStack.getItem().getTranslationKey();

            // If the current slot doesn't have the expected item, find it elsewhere
            if (!expectedItem.equals(currentItem)) {
                // Search for the expected item in other slots (prioritize non-kit slots first)
                for (int sourceSlot = startSlot; sourceSlot < endSlot; sourceSlot++) {
                    if (sourceSlot == targetSlot) {
                        continue;
                    }

                    ItemStack sourceStack = mc.player.getInventory().getStack(sourceSlot);
                    if (sourceStack.isEmpty()) {
                        continue;
                    }

                    String sourceItem = sourceStack.getItem().getTranslationKey();
                    if (sourceItem.equals(expectedItem)) {
                        // Found the expected item, move it to the target slot
                        moveItem(sourceSlot, targetSlot);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void moveItem(int fromSlot, int toSlot) {
        // Safety check: only move items when inventory is open or when inventoryOnly is false
        boolean canMove = !this.inventoryOnly.getValue() || EntityUtil.inInventory();
        if (!canMove) {
            return;
        }

        try {
            int syncId = mc.player.playerScreenHandler.syncId;
            int fromSlotId = fromSlot < 9 ? fromSlot + 36 : fromSlot;
            int toSlotId = toSlot < 9 ? toSlot + 36 : toSlot;

            // Pick up from source
            mc.interactionManager.clickSlot(syncId, fromSlotId, 0, SlotActionType.PICKUP, mc.player);
            // Place in target
            mc.interactionManager.clickSlot(syncId, toSlotId, 0, SlotActionType.PICKUP, mc.player);
            // Return remaining to source (if any)
            mc.interactionManager.clickSlot(syncId, fromSlotId, 0, SlotActionType.PICKUP, mc.player);
        } catch (Exception e) {
            // Silently handle cases where we can't access inventory
        }
    }

    @Override
    public void onDisable() {
        this.kitMap.clear();
    }

}