package dev.g255.arc.mod.modules.impl.misc;

import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.core.impl.CommandManager;
import dev.g255.arc.api.utils.player.InventoryUtil;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.api.utils.world.BlockUtil;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.impl.combat.AutoCrystal;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.block.Blocks;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;

public class AutoEnderChest extends Module {
    public static AutoEnderChest INSTANCE;
    private final BooleanSetting autoDisable = add(new BooleanSetting("AutoDisable", true));
    private final SliderSetting disableTime = add(new SliderSetting("DisableTime", 500, 0, 1000));
    private final BooleanSetting inventorySwap = add(new BooleanSetting("InventorySwap", false));
    private final SliderSetting targetShulkerCount = add(new SliderSetting("TargetShulkers", 2, 0, 36));
    private final BooleanSetting preferCrystal = add(new BooleanSetting("PreferCrystal", false));
    private final BooleanSetting storeItems = add(new BooleanSetting("StoreItems", true));
    private final SliderSetting storeCount = add(new SliderSetting("StoreCount", 5, 1, 36));
    private final Timer timer = new Timer();
    private final Timer disableTimer = new Timer();
    private BlockPos placePos = null;
    private BlockPos openPos = null;
    private boolean opened = false;
    private int shulkersTaken = 0;
    private boolean usingExistingChest = false;
    private boolean crystalAuraOriginalState = false;

    public AutoEnderChest() {
        super("AutoEnderChest", "Automatically places an ender chest and takes items out of it.", Category.Misc);
        this.setChinese("自动末影箱");
        INSTANCE = this;
    }

    private int getShulkerCountInInventory() {
        return InventoryUtil.getItemCount(Items.SHULKER_BOX) +
                InventoryUtil.getItemCount(Items.WHITE_SHULKER_BOX) +
                InventoryUtil.getItemCount(Items.ORANGE_SHULKER_BOX) +
                InventoryUtil.getItemCount(Items.MAGENTA_SHULKER_BOX) +
                InventoryUtil.getItemCount(Items.LIGHT_BLUE_SHULKER_BOX) +
                InventoryUtil.getItemCount(Items.YELLOW_SHULKER_BOX) +
                InventoryUtil.getItemCount(Items.LIME_SHULKER_BOX) +
                InventoryUtil.getItemCount(Items.PINK_SHULKER_BOX) +
                InventoryUtil.getItemCount(Items.GRAY_SHULKER_BOX) +
                InventoryUtil.getItemCount(Items.LIGHT_GRAY_SHULKER_BOX) +
                InventoryUtil.getItemCount(Items.CYAN_SHULKER_BOX) +
                InventoryUtil.getItemCount(Items.PURPLE_SHULKER_BOX) +
                InventoryUtil.getItemCount(Items.BLUE_SHULKER_BOX) +
                InventoryUtil.getItemCount(Items.BROWN_SHULKER_BOX) +
                InventoryUtil.getItemCount(Items.GREEN_SHULKER_BOX) +
                InventoryUtil.getItemCount(Items.RED_SHULKER_BOX) +
                InventoryUtil.getItemCount(Items.BLACK_SHULKER_BOX);
    }

    private int getShulkersNeeded() {
        int currentCount = getShulkerCountInInventory();
        int targetCount = (int) targetShulkerCount.getValue();

        if (currentCount >= targetCount) {
            return 0;
        }

        return targetCount - currentCount;
    }

    private int findEnderChestItem() {
        return InventoryUtil.findItemInventorySlot(Item.fromBlock(Blocks.ENDER_CHEST));
    }

    private void doSwap(int slot) {
        InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
    }

    private BlockPos findExistingEnderChest() {
        BlockPos nearestChest = null;
        float nearestDistance = Float.MAX_VALUE;

        for (BlockPos pos : BlockUtil.getSphere(4.0f)) {
            if (!mc.world.getBlockState(pos.up()).isAir()) continue;
            if (MathHelper.sqrt((float) mc.player.squaredDistanceTo(pos.toCenterPos())) < 1.0f) continue;

            if (mc.world.getBlockState(pos).getBlock() instanceof EnderChestBlock) {
                if (BlockUtil.canClick(pos)) {
                    float distance = MathHelper.sqrt((float) mc.player.squaredDistanceTo(pos.toCenterPos()));
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearestChest = pos;
                    }
                }
            }
        }

        return nearestChest;
    }

    @Override
    public void onEnable() {
        openPos = null;
        disableTimer.reset();
        placePos = null;
        opened = false;
        usingExistingChest = false;
        shulkersTaken = 0;
        if (this.preferCrystal.getValue() && AutoCrystal.INSTANCE != null) {
            if (AutoCrystal.INSTANCE.isOn() && !crystalAuraOriginalState) {
                crystalAuraOriginalState = true;
                AutoCrystal.INSTANCE.disable();
            }
        }

        if (nullCheck()) return;
        int shulkersNeeded = getShulkersNeeded();
        if (shulkersNeeded <= 0) {
            CommandManager.sendMessage("§a[✓] You already have enough Shadow Boxes (" + getShulkerCountInInventory() + "piece)");
            disable();
            return;
        }

        CommandManager.sendMessage("§a[✓] Need to take " + shulkersNeeded + " a Shadowbox");

        int oldSlot = mc.player.getInventory().selectedSlot;

        BlockPos existingChest = findExistingEnderChest();
        if (existingChest != null) {
            openPos = existingChest;
            usingExistingChest = true;

            BlockUtil.clickBlock(existingChest, BlockUtil.getClickSide(existingChest), true);
            return;
        }

        double distance = 100;
        BlockPos bestPos = null;

        for (BlockPos pos : BlockUtil.getSphere(4.0f)) {
            if (!mc.world.getBlockState(pos.up()).isAir()) continue;
            if (MathHelper.sqrt((float) mc.player.squaredDistanceTo(pos.toCenterPos())) < 1.0f) continue;
            if (!BlockUtil.clientCanPlace(pos, false)
                    || !BlockUtil.isStrictDirection(pos.offset(Direction.DOWN), Direction.UP)
                    || !BlockUtil.canClick(pos.offset(Direction.DOWN))
            ) continue;

            if (bestPos == null || MathHelper.sqrt((float) mc.player.squaredDistanceTo(pos.toCenterPos())) < distance) {
                distance = MathHelper.sqrt((float) mc.player.squaredDistanceTo(pos.toCenterPos()));
                bestPos = pos;
            }
        }

        if (bestPos != null) {
            int slot = findEnderChestItem();
            if (slot == -1) {
                CommandManager.sendMessage("§c[!] Ender Chest not found");
                disable();
                return;
            }
            doSwap(slot);
            placeBlock(bestPos);
            placePos = bestPos;

            if (inventorySwap.getValue()) {
                doSwap(slot);
            } else {
                doSwap(oldSlot);
            }
            timer.reset();
        } else {
            CommandManager.sendMessage("§c[!] Placement location not found");
            disable();
        }
    }

    private void placeBlock(BlockPos pos) {
        BlockUtil.clickBlock(pos.offset(Direction.DOWN), Direction.UP, true);
    }

    @Override
    public void onDisable() {
        opened = false;
        if (crystalAuraOriginalState && AutoCrystal.INSTANCE != null) {
            AutoCrystal.INSTANCE.enable();
            crystalAuraOriginalState = false;
        }
    }
    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (nullCheck()) return;
        if (!(mc.currentScreen instanceof GenericContainerScreen)) {
            if (opened) {
                opened = false;
                if (autoDisable.getValue()) {
                    disableWithClose();
                }
                return;
            }
            if (placePos != null &&
                    MathHelper.sqrt((float) mc.player.squaredDistanceTo(placePos.toCenterPos())) <= 4.0f &&
                    mc.world.isAir(placePos.up()) &&
                    (!timer.passedMs(500) || mc.world.getBlockState(placePos).getBlock() instanceof EnderChestBlock)) {

                if (mc.world.getBlockState(placePos).getBlock() instanceof EnderChestBlock) {
                    openPos = placePos;
                    BlockUtil.clickBlock(placePos, BlockUtil.getClickSide(placePos), true);
                }
            } else {
                BlockPos existingChest = findExistingEnderChest();
                if (existingChest != null) {
                    openPos = existingChest;
                    BlockUtil.clickBlock(existingChest, BlockUtil.getClickSide(existingChest), true);
                } else if (autoDisable.getValue()) {
                    CommandManager.sendMessage("§c[!] Ender Chest not found");
                    disable();
                }
            }
            return;
        }
        opened = true;
        int shulkersNeeded = getShulkersNeeded();
        if (shulkersNeeded <= 0) {
            CommandManager.sendMessage("§a[✓] You already have enough Shadow Boxes (" + getShulkerCountInInventory() + "piece)");
            if (autoDisable.getValue()) {
                disableWithClose();
            }
            return;
        }
        if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler container) {
            boolean tookItem = false;
            int emptySlots = InventoryUtil.getEmptySlotCount();

            if (emptySlots == 0 && storeItems.getValue()) {
                int storedCount = storeItemsToEnderChest(container);
                if (storedCount > 0) {
                    CommandManager.sendMessage("§a[✓] Stored " + storedCount + " items to Ender Chest");
                    emptySlots = InventoryUtil.getEmptySlotCount();
                }
            }

            for (Slot slot : container.slots) {
                if (slot.id < 27 && !slot.getStack().isEmpty()) {
                    ItemStack stack = slot.getStack();
                    Item item = stack.getItem();
                    if (item instanceof BlockItem &&
                            ((BlockItem) item).getBlock() instanceof ShulkerBoxBlock) {
                        if (InventoryUtil.getEmptySlotCount() > 0) {
                            mc.interactionManager.clickSlot(container.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                            tookItem = true;
                            shulkersTaken++;
                            if (getShulkersNeeded() <= 0) {
                                CommandManager.sendMessage("§a[✓] Taken " + shulkersTaken + " a Shadowless Box");
                                if (autoDisable.getValue()) {
                                    disableWithClose();
                                }
                                break;
                            }
                        }
                    }
                }
            }
            if (!tookItem) {
                CommandManager.sendMessage("§c[!] Phantom Shadow Box not found or insufficient backpack space");
            }
            if (autoDisable.getValue() && !tookItem) {
                disableWithClose();
            }
        }
    }

    private void disableWithClose() {
        if (disableTimer.passedMs(disableTime.getValueInt())) {
            mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
            mc.player.closeHandledScreen();
            disable();
        }
    }

    private int storeItemsToEnderChest(GenericContainerScreenHandler container) {
        int storedCount = 0;
        int targetStore = (int) storeCount.getValue();

        for (int i = 27; i < 63 && storedCount < targetStore; i++) {
            Slot slot = container.getSlot(i);
            if (slot.hasStack()) {
                ItemStack stack = slot.getStack();
                if (stack.getItem() instanceof BlockItem &&
                        ((BlockItem) stack.getItem()).getBlock() instanceof ShulkerBoxBlock) {
                    continue;
                }

                for (int enderSlot = 0; enderSlot < 27; enderSlot++) {
                    Slot targetSlot = container.getSlot(enderSlot);
                    if (!targetSlot.hasStack()) {
                        if (mc.interactionManager != null) {
                            mc.interactionManager.clickSlot(container.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                        }
                        storedCount++;
                        break;
                    }
                }
            }
        }

        return storedCount;
    }
}
