package dev.perish.mod.modules.impl.misc;

import dev.perish.Perish;
import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.UpdateEvent;
import dev.perish.api.utils.math.Timer;
import dev.perish.api.utils.player.InventoryUtil;
import dev.perish.api.utils.player.MovementUtil;
import dev.perish.api.utils.world.BlockUtil;
import dev.perish.mod.commands.impl.RegearCommand;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.impl.player.SpeedMine;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.EnumSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;
import dev.perish.mod.modules.settings.impl.StringSetting;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class ShulkerGet extends Module {
    public static ShulkerGet INSTANCE;
    public enum Mode { NORMAL("Normal"), SMART("Smart"); private final String name; Mode(String name) { this.name = name; } @Override public String toString() { return name; } }

    private final StringSetting kitName = add(new StringSetting("Kit", ""));
    private final EnumSetting<Mode> mode = add(new EnumSetting<>("Mode", Mode.NORMAL));
    private final BooleanSetting debug = add(new BooleanSetting("Debug", false));
    private final BooleanSetting autoDisable = add(new BooleanSetting("AutoDisable", true));
    private final BooleanSetting useDelay = add(new BooleanSetting("UseDelay", false));
    private final SliderSetting delay = add(new SliderSetting("Delay", .1, 0, 2, .01, useDelay::getValue).setSuffix("s"));
    private final SliderSetting disableTime = add(new SliderSetting("DisableTime", 500, 0, 1000, () -> !useDelay.getValue()));
    public final BooleanSetting rotate = add(new BooleanSetting("Rotate", true));
    private final BooleanSetting place = add(new BooleanSetting("Place", true));
    private final BooleanSetting inventory = add(new BooleanSetting("InventorySwap", true));
    private final BooleanSetting preferOpen = add(new BooleanSetting("PerferOpen", true));
    private final BooleanSetting open = add(new BooleanSetting("Open", true));
    private final SliderSetting range = add(new SliderSetting("Range", 4, 0, 6, .1));
    private final SliderSetting minRange = add(new SliderSetting("MinRange", 1, 0, 3, .1));
    private final BooleanSetting mine = add(new BooleanSetting("Mine", true));
    private final BooleanSetting take = add(new BooleanSetting("Take", true));
    private final SliderSetting ratio = add(new SliderSetting("Ratio", 1, 0, 1, .01, () -> mode.is(Mode.SMART)));
    private final BooleanSetting whitelist = add(new BooleanSetting("Whitelist", false).setParent());
    private final BooleanSetting wGapple = add(new BooleanSetting("Enchanted Golden Apple", false, whitelist::isOpen));
    private final BooleanSetting wObsidian = add(new BooleanSetting("WObsidian", false, whitelist::isOpen));
    private final BooleanSetting wTools = add(new BooleanSetting("Tools", false, whitelist::isOpen));
    private final BooleanSetting wArmor = add(new BooleanSetting("Armor", false, whitelist::isOpen));
    private final BooleanSetting wWeapons = add(new BooleanSetting("Weapons", false, whitelist::isOpen));
    private final BooleanSetting wElytra = add(new BooleanSetting("Elytra", false, whitelist::isOpen));
    private final BooleanSetting wChorus = add(new BooleanSetting("Chorus", false, whitelist::isOpen));
    private final BooleanSetting wPearl = add(new BooleanSetting("WPearl", false, whitelist::isOpen));
    private final BooleanSetting wPiston = add(new BooleanSetting("WPiston", false, whitelist::isOpen));
    private final BooleanSetting wRedstone = add(new BooleanSetting("WRedstone", false, whitelist::isOpen));
    private final BooleanSetting wAnchor = add(new BooleanSetting("WAnchor", false, whitelist::isOpen));
    private final BooleanSetting wGlowstone = add(new BooleanSetting("WGlowstone", false, whitelist::isOpen));
    private final BooleanSetting wTotem = add(new BooleanSetting("WTotem", false, whitelist::isOpen));
    private final SliderSetting minTotem = add(new SliderSetting("MinTotem", 6, 0, 36, 1, whitelist::isOpen));
    private final BooleanSetting wEnderChest = add(new BooleanSetting("WEnderChest", false, whitelist::isOpen));
    private final SliderSetting min = add(new SliderSetting("Min", 2, 0, 36, () -> mode.is(Mode.NORMAL)));
    private final BooleanSetting forceMin = add(new BooleanSetting("ForceMin", false, () -> mode.is(Mode.NORMAL)).setParent());
    private final SliderSetting forceShulker = add(new SliderSetting("ForceShulker", 2, 0, 36, () -> mode.is(Mode.NORMAL) && forceMin.getValue()));
    private final SliderSetting forceTurtle = add(new SliderSetting("ForceTurtle", 0, 0, 36, () -> mode.is(Mode.NORMAL) && forceMin.getValue()));
    private final BooleanSetting forceNoTake = add(new BooleanSetting("ForceNoTake", true, () -> mode.is(Mode.NORMAL) && forceMin.getValue()));
    private final BooleanSetting movePause = add(new BooleanSetting("MovePause", true));
    private final BooleanSetting smart = add(new BooleanSetting("Smart", true, () -> take.getValue() && mode.is(Mode.NORMAL)).setParent());
    private final SliderSetting crystal = add(new SliderSetting("Crystal", 256, 0, 512, () -> mode.is(Mode.NORMAL) && smart.isOpen()));
    private final SliderSetting exp = add(new SliderSetting("Exp", 256, 0, 512, () -> mode.is(Mode.NORMAL) && smart.isOpen()));
    private final SliderSetting totem = add(new SliderSetting("Totem", 6, 0, 36, () -> mode.is(Mode.NORMAL) && smart.isOpen()));
    private final SliderSetting gapple = add(new SliderSetting("Gapple", 128, 0, 512, () -> mode.is(Mode.NORMAL) && smart.isOpen()));
    private final SliderSetting obsidian = add(new SliderSetting("Obsidian", 64, 0, 512, () -> mode.is(Mode.NORMAL) && smart.isOpen()));
    private final SliderSetting web = add(new SliderSetting("Web", 64, 0, 512, () -> mode.is(Mode.NORMAL) && smart.isOpen()));
    private final SliderSetting glowstone = add(new SliderSetting("Glowstone", 256, 0, 512, () -> mode.is(Mode.NORMAL) && smart.isOpen()));
    private final SliderSetting anchor = add(new SliderSetting("Anchor", 256, 0, 512, () -> mode.is(Mode.NORMAL) && smart.isOpen()));
    private final SliderSetting pearl = add(new SliderSetting("Pearl", 16, 0, 64, () -> mode.is(Mode.NORMAL) && smart.isOpen()));
    private final SliderSetting piston = add(new SliderSetting("Piston", 128, 0, 512, () -> mode.is(Mode.NORMAL) && smart.isOpen()));
    private final SliderSetting redstone = add(new SliderSetting("RedStone", 128, 0, 512, () -> mode.is(Mode.NORMAL) && smart.isOpen()));
    private final SliderSetting bed = add(new SliderSetting("Bed", 256, 0, 512, () -> mode.is(Mode.NORMAL) && smart.isOpen()));
    private final SliderSetting speed = add(new SliderSetting("Speed", 1, 0, 8, () -> mode.is(Mode.NORMAL) && smart.isOpen()));
    private final SliderSetting turtle = add(new SliderSetting("Turtle", 1, 0, 8, () -> mode.is(Mode.NORMAL) && smart.isOpen()));
    private final Timer timer = new Timer(), disableTimer = new Timer(), delayTimer = new Timer();
    private BlockPos placePos, openPos;
    private boolean opend;
    private int state;
    public boolean enabledByChestStealer;

    public ShulkerGet() { super("ShulkerGet", Category.Misc); setChinese("自动获取潜影盒"); INSTANCE = this; }

    @Override public void onEnable() {
        if (nullCheck()) return;
        placePos = openPos = null; opend = false; state = 0; disableTimer.reset();
        loadConfiguredKit();
        if (!needsShulkers()) { resumeChestStealer(); disable(); return; }
        if (!place.getValue()) return;
        if (preferOpen.getValue()) for (BlockPos pos : BlockUtil.getSphere(range.getValueFloat())) if (mc.world.getBlockState(pos).getBlock() == Blocks.ENDER_CHEST && mc.world.isAir(pos.up())) { openPos = pos; return; }
        BlockPos target = findBestPlacePos();
        if (target == null) return;
        if (mc.currentScreen instanceof GenericContainerScreen screen && screen.getScreenHandler().getInventory().size() == 27) {
            openPos = target;
            opend = true;
            return;
        }
        int slot = inventory.getValue() ? InventoryUtil.findClassInventorySlot(EnderChestBlock.class) : InventoryUtil.findClass(EnderChestBlock.class);
        if (slot == -1) { sendMessage("§cShulkerGet: No Ender Chest found in inventory."); return; }
        int old = mc.player.getInventory().selectedSlot;
        if (inventory.getValue()) { InventoryUtil.inventorySwap(slot, old); placeBlock(target); InventoryUtil.inventorySwap(slot, old); }
        else { InventoryUtil.switchToSlot(slot); placeBlock(target); InventoryUtil.switchToSlot(old); }
        placePos = target; timer.reset();
    }

    private void loadConfiguredKit() {
        String name = kitName.getValue().trim();
        if (!name.isEmpty() && !RegearCommand.loadKit(name)) {
            sendMessage("§c[ShulkerGet] Kit '" + name + "' not found or invalid.");
        }
    }

    @Override public void onDisable() {
        resumeChestStealer();
        enabledByChestStealer = false; opend = false;
        if (mine.getValue() && placePos != null) SpeedMine.INSTANCE.mine(placePos);
        if (mc.player != null && mc.currentScreen instanceof GenericContainerScreen) mc.player.closeHandledScreen();
    }

    @EventListener public void onUpdate(UpdateEvent event) {
        if (nullCheck() || movePause.getValue() && MovementUtil.isMoving()) return;
        if (!needsShulkers()) { if (autoDisable.getValue()) disable2(); return; }
        boolean forced = isForceMode() || enabledByChestStealer;
        if (!forced && Perish.THREAD.getPlayers().stream().anyMatch(player ->
                player != mc.player && player.distanceTo(mc.player) < 10.0 && !Perish.FRIEND.isFriend(player))) {
            return;
        }
        if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler container && mc.currentScreen instanceof GenericContainerScreen) {
            opend = true;
            if (!take.getValue() || forced && mode.is(Mode.NORMAL) && forceNoTake.getValue()) { if (autoDisable.getValue()) disable2(); return; }
            if (mode.is(Mode.SMART)) {
                boolean active = processSmart(container);
                if (active) disableTimer.reset();
                else if (autoDisable.getValue() && disableTimer.passedMs(disableTime.getValue())) disable2();
            } else {
                boolean active = processNormal(container);
                if (active) disableTimer.reset();
                else if (autoDisable.getValue()) disable2();
            }
            return;
        }
        if (opend) {
            opend = false;
            if (mine.getValue() && openPos != null && mc.world.getBlockState(openPos).getBlock() == Blocks.ENDER_CHEST) SpeedMine.INSTANCE.mine(openPos);
            if (autoDisable.getValue()) disable2();
            return;
        }
        if (open.getValue()) openEnderChest();
        else if (!take.getValue() && autoDisable.getValue()) disable2();
    }

    private void openEnderChest() {
        if (placePos != null
                && Math.sqrt(mc.player.squaredDistanceTo(placePos.toCenterPos())) <= range.getValue()
                && mc.world.isAir(placePos.up())
                && (!timer.passedMs(500) || mc.world.getBlockState(placePos).getBlock() == Blocks.ENDER_CHEST)
                && mc.world.getBlockState(placePos).getBlock() == Blocks.ENDER_CHEST) {
            openPos = placePos;
            BlockUtil.clickBlock(placePos, BlockUtil.getClickSide(placePos), rotate.getValue());
            return;
        }
        for (BlockPos pos : BlockUtil.getSphere(range.getValueFloat())) {
            if (mc.world.isAir(pos.up()) && mc.world.getBlockState(pos).getBlock() == Blocks.ENDER_CHEST) {
                openPos = pos;
                BlockUtil.clickBlock(pos, BlockUtil.getClickSide(pos), rotate.getValue());
                return;
            }
        }
        if (autoDisable.getValue()) disable2();
    }

    private void placeBlock(BlockPos pos) {
        if (pos == null) return;
        Direction side = BlockUtil.getPlaceSide(pos);
        if (side != null) {
            BlockUtil.clickBlock(pos.offset(side), side.getOpposite(), rotate.getValue(), false);
        }
    }

    private BlockPos findBestPlacePos() {
        BlockPos best = null; double distance = Double.MAX_VALUE;
        for (BlockPos pos : BlockUtil.getSphere(range.getValueFloat())) {
            double current = Math.sqrt(mc.player.squaredDistanceTo(pos.toCenterPos()));
            Direction side = BlockUtil.getPlaceSide(pos);
            if (current < minRange.getValue() || !BlockUtil.clientCanPlace(pos, false) || side == null || !mc.world.isAir(pos.offset(side.getOpposite())) || BlockUtil.hasEntity(pos.offset(side.getOpposite()), false)) continue;
            if (current < distance) { best = pos; distance = current; }
        }
        return best;
    }

    private boolean processSmart(GenericContainerScreenHandler container) {
        int needed = getKitShulkerTotal() - getQualifiedShulkerCount();
        if (needed <= 0) {
            if (useDelay.getValue()) {
                if (state < 3) {
                    state = 3;
                    delayTimer.reset();
                }
                if (!delayTimer.passedS(delay.getValue())) return true;
                if (autoDisable.getValue()) disable2();
                return false;
            }
            return false;
        }
        if (useDelay.getValue()) {
            if (state == 0) { state = 1; delayTimer.reset(); return true; }
            if (state == 1 && !delayTimer.passedS(delay.getValue())) return true;
            if (state == 1) { state = 2; delayTimer.reset(); return doSmartSwaps(container, needed); }
            if (state == 2) {
                if (!delayTimer.passedS(delay.getValue())) return true;
                state = 3;
                if (autoDisable.getValue()) disable2();
                return false;
            }
            if (state == 3) {
                if (autoDisable.getValue()) disable2();
                return false;
            }
        }
        return doSmartSwaps(container, needed);
    }

    private boolean doSmartSwaps(GenericContainerScreenHandler container, int needed) {
        int size = container.slots.size() - 36;
        List<Integer> source = new ArrayList<>(), empty = new ArrayList<>(), badShulkers = new ArrayList<>(), trash = new ArrayList<>();
        for (int i = 0; i < size; i++) if (isShulkerQualified(container.getSlot(i).getStack())) source.add(i);
        if (source.isEmpty()) {
            sendMessage("§c[ShulkerGet] No valid shulker found in Ender Chest.");
            disable2();
            Module chest = Perish.MODULE.getModuleByName("ChestStealer");
            if (chest != null && chest.isOn()) chest.disable();
            return false;
        }
        source.sort(
                Comparator.comparingInt((Integer slot) -> shulkerSupply(container.getSlot(slot).getStack()))
                        .reversed()
                        .thenComparingInt(slot -> shulkerKitContent(container.getSlot(slot).getStack()))
        );
        Map<String, Integer> excess = new HashMap<>();
        for (Map.Entry<String, Integer> entry : RegearCommand.currentKit.entrySet()) {
            int amount = getInventoryCount(entry.getKey()) - entry.getValue();
            if (amount > 0) excess.put(entry.getKey(), amount);
        }
        for (int i = size; i < container.slots.size(); i++) {
            ItemStack stack = container.getSlot(i).getStack();
            if (stack.isEmpty()) empty.add(i);
            else if (isShulker(stack) && !isShulkerQualified(stack)) badShulkers.add(i);
            else {
                String key = RegearCommand.getKitKey(stack);
                if (!RegearCommand.currentKit.containsKey(key) || excess.getOrDefault(key, 0) > 0) trash.add(i);
            }
        }
        int moved = 0;
        for (int slot : source) {
            if (moved >= needed) break;
            int target = !badShulkers.isEmpty() ? badShulkers.removeFirst()
                    : !empty.isEmpty() ? empty.removeFirst()
                    : !trash.isEmpty() ? trash.removeFirst() : -1;
            if (target == -1) break;
            swap(container, slot, target); moved++;
        }
        return moved > 0;
    }

    private boolean processNormal(GenericContainerScreenHandler container) {
        int size = container.slots.size() - 36;
        boolean putBack = false;
        for (int i = size; i < container.slots.size(); i++) {
            if (isShulker(container.getSlot(i).getStack()) && !isValidShulker(container.getSlot(i).getStack())) {
                mc.interactionManager.clickSlot(container.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                putBack = true;
                break;
            }
        }
        boolean taken = false;
        for (int i = 0; i < size; i++) {
            if (isValidShulker(container.getSlot(i).getStack())) {
                mc.interactionManager.clickSlot(container.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                taken = true;
                break;
            }
        }
        return putBack || taken;
    }

    private void swap(GenericContainerScreenHandler container, int source, int target) {
        mc.interactionManager.clickSlot(container.syncId, source, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(container.syncId, target, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(container.syncId, source, 0, SlotActionType.PICKUP, mc.player);
    }

    public boolean needsShulkers() {
        if (mode.is(Mode.SMART)) {
            if (RegearCommand.currentKit.isEmpty()) { if (debug.getValue()) sendMessage("§c[ShulkerGet] No kit loaded! Use .regear load <name>."); return false; }
            return getQualifiedShulkerCount() < getKitShulkerTotal();
        }
        return getValidShulkerCount() < min.getValueInt();
    }

    private int getKitShulkerTotal() { return RegearCommand.currentKit.entrySet().stream().filter(entry -> entry.getKey().contains("shulker_box")).mapToInt(Map.Entry::getValue).sum(); }
    private int getQualifiedShulkerCount() { int count = 0; for (int i = 0; i < 36; i++) if (isShulkerQualified(mc.player.getInventory().getStack(i))) count++; return count; }
    private int getValidShulkerCount() { int count = 0; for (int i = 0; i < 36; i++) if (isValidShulker(mc.player.getInventory().getStack(i))) count++; return count; }
    private boolean isForceMode() { return forceMin.getValue() && getValidShulkerCount() < forceShulker.getValueInt() && (forceTurtle.getValueInt() == 0 || InventoryUtil.getPotionCount(StatusEffects.RESISTANCE.value()) < forceTurtle.getValueInt()); }
    private boolean isShulker(ItemStack stack) { return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof ShulkerBoxBlock; }

    private boolean isShulkerQualified(ItemStack shulker) {
        if (!isShulker(shulker)) return false;
        Map<String, Integer> contained = getContents(shulker);
        if (contained.isEmpty()) return false;
        for (Map.Entry<String, Integer> entry : RegearCommand.currentKit.entrySet()) {
            String key = entry.getKey();
            if (isWhitelisted(key)) continue;
            int threshold = key.contains("totem_of_undying") ? minTotem.getValueInt() : (int) Math.ceil(entry.getValue() * ratio.getValue());
            if (getInventoryCount(key) + contained.getOrDefault(key, 0) < threshold) return false;
        }
        return true;
    }

    private boolean isValidShulker(ItemStack stack) {
        if (mode.is(Mode.SMART)) return isShulkerQualified(stack);
        if (!isShulker(stack)) return false;
        Map<String, Integer> contents = getContents(stack);
        if (contents.isEmpty()) return false;
        if (!smart.isOpen()) return true;
        return contents.getOrDefault(Items.END_CRYSTAL.getTranslationKey(), 0) >= crystal.getValueInt()
                || contents.getOrDefault(Items.EXPERIENCE_BOTTLE.getTranslationKey(), 0) >= exp.getValueInt()
                || contents.getOrDefault(Items.ENCHANTED_GOLDEN_APPLE.getTranslationKey(), 0) >= gapple.getValueInt()
                || contents.getOrDefault(Items.TOTEM_OF_UNDYING.getTranslationKey(), 0) >= totem.getValueInt()
                || contents.getOrDefault(Items.OBSIDIAN.getTranslationKey(), 0) >= obsidian.getValueInt()
                || contents.getOrDefault(Items.COBWEB.getTranslationKey(), 0) >= web.getValueInt()
                || contents.getOrDefault(Items.GLOWSTONE.getTranslationKey(), 0) >= glowstone.getValueInt()
                || contents.getOrDefault(Items.RESPAWN_ANCHOR.getTranslationKey(), 0) >= anchor.getValueInt()
                || contents.getOrDefault(Items.ENDER_PEARL.getTranslationKey(), 0) >= pearl.getValueInt()
                || contents.getOrDefault(Items.PISTON.getTranslationKey(), 0) + contents.getOrDefault(Items.STICKY_PISTON.getTranslationKey(), 0) >= piston.getValueInt()
                || contents.getOrDefault(Items.REDSTONE.getTranslationKey(), 0) + contents.getOrDefault(Items.REDSTONE_BLOCK.getTranslationKey(), 0) >= redstone.getValueInt()
                || hasBedContents(stack) >= bed.getValueInt() || getPotionContents(stack, StatusEffects.SPEED.value()) >= speed.getValueInt() || getPotionContents(stack, StatusEffects.RESISTANCE.value()) >= turtle.getValueInt();
    }

    private Map<String, Integer> getContents(ItemStack shulker) {
        ItemStack[] items = new ItemStack[27]; ShulkerViewer.getItemsInContainerItem(shulker, items);
        Map<String, Integer> result = new HashMap<>();
        for (ItemStack item : items) if (!item.isEmpty()) result.merge(RegearCommand.getKitKey(item), item.getCount(), Integer::sum);
        return result;
    }

    private int hasBedContents(ItemStack shulker) { ItemStack[] items = new ItemStack[27]; ShulkerViewer.getItemsInContainerItem(shulker, items); int count = 0; for (ItemStack item : items) if (item.getItem() instanceof BlockItem block && block.getBlock() instanceof BedBlock) count += item.getCount(); return count; }
    private int getPotionContents(ItemStack shulker, net.minecraft.entity.effect.StatusEffect effect) { ItemStack[] items = new ItemStack[27]; ShulkerViewer.getItemsInContainerItem(shulker, items); int count = 0; for (ItemStack item : items) for (StatusEffectInstance instance : item.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT).getEffects()) if (instance.getEffectType().value() == effect) count += item.getCount(); return count; }
    private int getInventoryCount(String key) { int count = 0; for (int i = 0; i < 36; i++) { ItemStack stack = mc.player.getInventory().getStack(i); if (RegearCommand.getKitKey(stack).equals(key)) count += stack.getCount(); } if (RegearCommand.getKitKey(mc.player.getOffHandStack()).equals(key)) count += mc.player.getOffHandStack().getCount(); return count; }
    private int shulkerSupply(ItemStack shulker) { int supply = 0; for (Map.Entry<String, Integer> entry : getContents(shulker).entrySet()) supply += Math.min(entry.getValue(), Math.max(0, RegearCommand.currentKit.getOrDefault(entry.getKey(), 0) - getInventoryCount(entry.getKey()))); return supply; }
    private int shulkerKitContent(ItemStack shulker) { return getContents(shulker).entrySet().stream().filter(entry -> RegearCommand.currentKit.containsKey(entry.getKey())).mapToInt(Map.Entry::getValue).sum(); }
    private boolean isWhitelisted(String key) { return key.contains("shulker_box") || wGapple.getValue() && key.contains("enchanted_golden_apple") || wObsidian.getValue() && key.contains("obsidian") || wTools.getValue() && (key.contains("pickaxe") || key.contains("axe") || key.contains("shovel") || key.contains("hoe")) || wArmor.getValue() && (key.contains("helmet") || key.contains("chestplate") || key.contains("leggings") || key.contains("boots")) || wWeapons.getValue() && (key.contains("sword") || key.contains("axe") || key.contains("bow") || key.contains("crossbow") || key.contains("trident")) || wElytra.getValue() && key.contains("elytra") || wChorus.getValue() && key.contains("chorus_fruit") || wPearl.getValue() && key.contains("ender_pearl") || wPiston.getValue() && key.contains("piston") || wRedstone.getValue() && key.contains("redstone") || wAnchor.getValue() && key.contains("respawn_anchor") || wGlowstone.getValue() && key.contains("glowstone") || wTotem.getValue() && key.contains("totem_of_undying") || wEnderChest.getValue() && key.contains("ender_chest"); }
    private void resumeChestStealer() { if (enabledByChestStealer) { Module chest = Perish.MODULE.getModuleByName("ChestStealer"); if (chest != null && chest.isOff()) chest.enable(); } }
    public void disable2() {
        if (mc.currentScreen instanceof GenericContainerScreen) mc.player.closeHandledScreen();
        disable();
    }
}
