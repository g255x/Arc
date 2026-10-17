package dev.perish.mod.modules.impl.misc;

import dev.perish.Perish;
import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.UpdateEvent;
import dev.perish.api.utils.math.Timer;
import dev.perish.api.utils.player.EntityUtil;
import dev.perish.api.utils.player.InventoryUtil;
import dev.perish.api.utils.world.BlockUtil;
import dev.perish.mod.commands.impl.RegearCommand;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.impl.player.SpeedMine;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;
import dev.perish.mod.modules.settings.impl.StringSetting;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public class ChestStealer extends Module {
    public static ChestStealer INSTANCE;
    private final StringSetting kitName = add(new StringSetting("Kit", ""));
    private final BooleanSetting shulkerGet = add(new BooleanSetting("ShulkerGet", false));
    private final BooleanSetting autoDisable = add(new BooleanSetting("AutoDisable", true));
    private final BooleanSetting useDelay = add(new BooleanSetting("UseDelay", false));
    private final SliderSetting delay = add(new SliderSetting("Delay", 0.1, 0, 2.0, 0.01, useDelay::getValue).setSuffix("s"));
    private final SliderSetting disableTime = add(new SliderSetting("DisableTime", 500, 0, 1000, () -> !useDelay.getValue()));
    private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
    private final BooleanSetting place = add(new BooleanSetting("Place", true));
    private final BooleanSetting inventory = add(new BooleanSetting("InventorySwap", true));
    private final BooleanSetting preferOpen = add(new BooleanSetting("PerferOpen", true));
    private final BooleanSetting open = add(new BooleanSetting("Open", true));
    private final SliderSetting range = add(new SliderSetting("Range", 4.0, 0.0, 6.0, .1));
    private final SliderSetting minRange = add(new SliderSetting("MinRange", 1.0, 0.0, 3.0, .1));
    private final BooleanSetting mine = add(new BooleanSetting("Mine", true));
    private final BooleanSetting take = add(new BooleanSetting("Take", true));
    private final BooleanSetting smart = add(new BooleanSetting("Smart", true, take::getValue).setParent());
    private final BooleanSetting useKit = add(new BooleanSetting("UseKit", false, () -> take.getValue() && smart.isOpen()).setParent());
    private final BooleanSetting organizeLayout = add(new BooleanSetting("OrganizeLayout", true, () -> take.getValue() && smart.isOpen() && useKit.getValue()));
    private final BooleanSetting check = add(new BooleanSetting("Check", false, () -> take.getValue() && smart.isOpen()).setParent());
    private final SliderSetting ratio = add(new SliderSetting("Ratio", 1.0, 0.0, 1.0, .01, () -> take.getValue() && smart.isOpen() && useKit.getValue() && !check.getValue()));
    private final BooleanSetting after = add(new BooleanSetting("After", true, () -> take.getValue() && smart.isOpen() && useKit.getValue() && !check.getValue()));
    private final SliderSetting afterDelay = add(new SliderSetting("AfterDelay", 500, 0, 5000, 10, () -> take.getValue() && smart.isOpen() && useKit.getValue() && !check.getValue() && after.getValue()).setSuffix("ms"));
    private final SliderSetting checkThreshold = add(new SliderSetting("CheckThreshold", 1, 0, 512, () -> take.getValue() && smart.isOpen() && check.getValue()));
    private final BooleanSetting debug = add(new BooleanSetting("Debug", false));
    private final BooleanSetting white = add(new BooleanSetting("Whitelist", false).setParent());
    private final BooleanSetting wGapple = add(new BooleanSetting("Enchanted Golden Apple", false, white::isOpen));
    private final BooleanSetting wObsidian = add(new BooleanSetting("WObsidian", false, white::isOpen));
    private final BooleanSetting wTools = add(new BooleanSetting("Tools", false, white::isOpen));
    private final BooleanSetting wArmor = add(new BooleanSetting("Armor", false, white::isOpen));
    private final BooleanSetting wWeapons = add(new BooleanSetting("Weapons", false, white::isOpen));
    private final BooleanSetting wElytra = add(new BooleanSetting("Elytra", false, white::isOpen));
    private final BooleanSetting wChorus = add(new BooleanSetting("Chorus", false, white::isOpen));
    private final BooleanSetting wPearl = add(new BooleanSetting("WPearl", false, white::isOpen));
    private final BooleanSetting wPiston = add(new BooleanSetting("WPiston", false, white::isOpen));
    private final BooleanSetting wRedstone = add(new BooleanSetting("WRedstone", false, white::isOpen));
    private final BooleanSetting wAnchor = add(new BooleanSetting("WAnchor", false, white::isOpen));
    private final BooleanSetting wGlowstone = add(new BooleanSetting("WGlowstone", false, white::isOpen));
    private final SliderSetting minTotem = add(new SliderSetting("MinTotem", 6, 0, 36, 1, white::isOpen));
    private final BooleanSetting wEnderChest = add(new BooleanSetting("WEnderChest", false, white::isOpen));
    // Retained source settings for legacy Smart mode.
    private final SliderSetting crystal = add(new SliderSetting("Crystal", 256, 0, 512, () -> take.getValue() && smart.isOpen() && !useKit.getValue()));
    private final SliderSetting exp = add(new SliderSetting("Exp", 256, 0, 512, () -> take.getValue() && smart.isOpen() && !useKit.getValue()));
    private final SliderSetting totem = add(new SliderSetting("Totem", 6, 0, 36, () -> take.getValue() && smart.isOpen() && !useKit.getValue()));
    private final SliderSetting gapple = add(new SliderSetting("Gapple", 128, 0, 512, () -> take.getValue() && smart.isOpen() && !useKit.getValue()));
    private final SliderSetting obsidian = add(new SliderSetting("Obsidian", 64, 0, 512, () -> take.getValue() && smart.isOpen() && !useKit.getValue()));
    private final SliderSetting web = add(new SliderSetting("Web", 64, 0, 512, () -> take.getValue() && smart.isOpen() && !useKit.getValue()));
    private final SliderSetting glowstone = add(new SliderSetting("Glowstone", 256, 0, 512, () -> take.getValue() && smart.isOpen() && !useKit.getValue()));
    private final SliderSetting anchor = add(new SliderSetting("Anchor", 256, 0, 512, () -> take.getValue() && smart.isOpen() && !useKit.getValue()));
    private final SliderSetting pearl = add(new SliderSetting("Pearl", 16, 0, 64, () -> take.getValue() && smart.isOpen() && !useKit.getValue()));
    private final SliderSetting piston = add(new SliderSetting("Piston", 128, 0, 512, () -> take.getValue() && smart.isOpen() && !useKit.getValue()));
    private final SliderSetting redstone = add(new SliderSetting("RedStone", 128, 0, 512, () -> take.getValue() && smart.isOpen() && !useKit.getValue()));
    private final SliderSetting bed = add(new SliderSetting("Bed", 256, 0, 512, () -> take.getValue() && smart.isOpen() && !useKit.getValue()));
    private final SliderSetting speed = add(new SliderSetting("Speed", 1, 0, 8, () -> take.getValue() && smart.isOpen() && !useKit.getValue()));
    private final SliderSetting turtle = add(new SliderSetting("Turtle", 1, 0, 8, () -> take.getValue() && smart.isOpen() && !useKit.getValue()));
    private final Timer timer = new Timer();
    private final Timer disableTimer = new Timer();
    private final Timer delayTimer = new Timer();
    private final Timer shulkerGetTimer = new Timer();
    public BlockPos placePos;
    private BlockPos openPos;
    private boolean opend = false;
    private boolean waitingForShulkerGet;
    private boolean swapped;
    private boolean layoutOrganizePending;
    private boolean nearestShulkerFailureReported;
    private int state;

    public ChestStealer() {
        super("ChestStealer", Category.Misc);
        setChinese("自动补给");
        INSTANCE = this;
        useKit.injectTask(() -> {
            if (!useKit.getValue() && check.getValue()) {
                check.setValue(false);
            }
        });
        check.injectTask(() -> {
            if (!check.getValue() && useKit.getValue()) {
                useKit.setValue(false);
            }
        });
    }

    @Override
    public void onEnable() {
        openPos = null;
        placePos = null;
        opend = false;
        waitingForShulkerGet = false;
        swapped = false;
        layoutOrganizePending = false;
        nearestShulkerFailureReported = false;
        state = 0;
        disableTimer.reset();
        if (nullCheck()) return;
        loadConfiguredKit();
        if (shulkerGet.getValue() && !useKit.getValue()) triggerShulkerGet(false, true);
    }

    @Override
    public void onDisable() {
        opend = false;
        waitingForShulkerGet = false;
        layoutOrganizePending = false;
        if (mine.getValue() && placePos != null && mc.world != null
                && mc.world.getBlockState(placePos).getBlock() instanceof ShulkerBoxBlock
                && SpeedMine.INSTANCE != null) {
            SpeedMine.INSTANCE.mine(placePos);
        }
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (nullCheck()) return;
        if (useKit.getValue() && waitingForShulkerGet) {
            Module module = Perish.MODULE.getModuleByName("ShulkerGet");
            if (module != null && module.isOn()) {
                shulkerGetTimer.reset();
                return;
            }
            if (!shulkerGetTimer.passedMs(afterDelay.getValue())) return;
            waitingForShulkerGet = false;
        }
        boolean validScreen = mc.currentScreen instanceof ShulkerBoxScreen || mc.currentScreen instanceof GenericContainerScreen;
        if (validScreen) {
            opend = true;
            if (useKit.getValue()) {
                if (layoutOrganizePending) {
                    if (organizeLayout.getValue()) {
                        organizeSavedLayout(mc.player.currentScreenHandler);
                    }
                    layoutOrganizePending = false;
                } else {
                    handleKitSupply(mc.player.currentScreenHandler);
                }
            }
            else handleLegacySteal(mc.player.currentScreenHandler);
            return;
        }
        if (useKit.getValue()) {
            state = 0;
            swapped = false;
            layoutOrganizePending = false;
        }
        if (opend) {
            opend = false;
            if (mine.getValue()) {
                if (openPos != null && mc.world.getBlockState(openPos).getBlock() instanceof ShulkerBoxBlock) {
                    SpeedMine.INSTANCE.mine(openPos);
                } else {
                    openPos = null;
                    for (BlockPos pos : BlockUtil.getSphere(range.getValueFloat())) {
                        if (mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock && mc.world.isAir(pos.up())) {
                            openPos = pos;
                            SpeedMine.INSTANCE.mine(pos);
                            break;
                        }
                    }
                }
            }
            if (autoDisable.getValue()) disable(); else if (!useDelay.getValue()) disable2();
            return;
        }
        if (useKit.getValue()) handleKitSearchAndPlace(); else handleLegacySearchAndPlace();
    }

    private void handleKitSearchAndPlace() {
        if (RegearCommand.currentKit.isEmpty()) { sendMessage("§c[ChestStealer] No kit loaded! Use .regear load <name>."); disable(); return; }
        int slot = getBestQualifiedShulkerSlot();
        if (slot == -1) {
            if (debug.getValue() && !nearestShulkerFailureReported) {
                reportNearestShulkerFailure();
                nearestShulkerFailureReported = true;
            }
            if (shulkerGet.getValue() && triggerShulkerGet(true, false)) return;
            if (autoDisable.getValue()) disable();
            return;
        }
        nearestShulkerFailureReported = false;
        placeShulker(slot);
    }

    private void loadConfiguredKit() {
        String name = kitName.getValue().trim();
        if (!name.isEmpty() && !RegearCommand.loadKit(name)) {
            sendMessage("§c[ChestStealer] Kit '" + name + "' not found or invalid.");
        }
    }

    private boolean triggerShulkerGet(boolean wait, boolean requireMissingShulkers) {
        Module module = Perish.MODULE.getModuleByName("ShulkerGet");
        if (module instanceof ShulkerGet getter && (!requireMissingShulkers || getter.needsShulkers())) {
            getter.enabledByChestStealer = true;
            getter.enable();
            if (wait && after.getValue()) { waitingForShulkerGet = true; shulkerGetTimer.reset(); } else disable();
            return true;
        }
        return false;
    }

    private void handleLegacySearchAndPlace() {
        int slot = inventory.getValue() ? InventoryUtil.findClassInventorySlot(ShulkerBoxBlock.class) : InventoryUtil.findClass(ShulkerBoxBlock.class);
        if (slot == -1) { if (autoDisable.getValue()) disable(); return; }
        placeShulker(slot);
    }

    private void placeShulker(int slot) {
        if (!place.getValue()) return;
        BlockPos pos = findBestPlacePos();
        if (pos == null) return;
        int old = mc.player.getInventory().selectedSlot;
        if (inventory.getValue()) {
            InventoryUtil.inventorySwap(slot, old);
            BlockUtil.placeBlock(pos, rotate.getValue());
            InventoryUtil.inventorySwap(slot, old);
            EntityUtil.syncInventory();
        } else {
            InventoryUtil.switchToSlot(slot);
            BlockUtil.placeBlock(pos, rotate.getValue());
            InventoryUtil.switchToSlot(old);
        }
        placePos = pos;
        timer.reset();
        if (open.getValue()) { openPos = pos; BlockUtil.clickBlock(pos, BlockUtil.getClickSide(pos), rotate.getValue()); }
    }

    private BlockPos findBestPlacePos() {
        BlockPos best = null;
        double distance = Double.MAX_VALUE;
        for (BlockPos pos : BlockUtil.getSphere(range.getValueFloat())) {
            double current = Math.sqrt(mc.player.squaredDistanceTo(pos.toCenterPos()));
            if (current < minRange.getValue() || !BlockUtil.clientCanPlace(pos, false)) continue;
            Direction side = BlockUtil.getPlaceSide(pos);
            if (side == null || !mc.world.isAir(pos.offset(side.getOpposite())) || BlockUtil.hasEntity(pos.offset(side.getOpposite()), false)) continue;
            if (current < distance) { distance = current; best = pos; }
        }
        return best;
    }

    private void handleKitSupply(ScreenHandler container) {
        if (!take.getValue()) return;
        if (useDelay.getValue()) {
            if (state == 0) { delayTimer.reset(); state = 1; return; }
            if (state == 1 && delayTimer.passedS(delay.getValue())) { performKitSwap(container); layoutOrganizePending = organizeLayout.getValue(); state = 2; delayTimer.reset(); return; }
            if (state == 2 && delayTimer.passedS(delay.getValue())) { mc.player.closeHandledScreen(); state = 3; if (autoDisable.getValue()) disable(); }
        } else if (!swapped) { performKitSwap(container); layoutOrganizePending = organizeLayout.getValue(); swapped = true; }
        else if (autoDisable.getValue()) disable2();
    }

    private void performKitSwap(ScreenHandler container) {
        if (RegearCommand.currentKit.isEmpty()) return;
        int invStart = container.slots.size() - 36;
        Map<Integer, ItemStack> virtualInventory = new HashMap<>();
        Map<String, Integer> counts = new HashMap<>();
        Map<String, List<Integer>> slots = new HashMap<>();
        List<Integer> empty = new ArrayList<>(), trash = new ArrayList<>();
        for (int i = invStart; i < container.slots.size(); i++) {
            ItemStack stack = container.getSlot(i).getStack();
            virtualInventory.put(i, stack.copy());
            if (stack.isEmpty()) empty.add(i);
            else { String key = RegearCommand.getKitKey(stack); counts.merge(key, stack.getCount(), Integer::sum); slots.computeIfAbsent(key, ignored -> new ArrayList<>()).add(i); }
        }
        Map<String, Integer> missing = new HashMap<>();
        for (Map.Entry<String, Integer> entry : RegearCommand.currentKit.entrySet()) if (counts.getOrDefault(entry.getKey(), 0) < entry.getValue()) missing.put(entry.getKey(), entry.getValue() - counts.getOrDefault(entry.getKey(), 0));

        if (!missing.isEmpty()) {
            for (int i = invStart; i < container.slots.size(); i++) {
                ItemStack stack = virtualInventory.get(i);
                if (!stack.isEmpty() && !RegearCommand.currentKit.containsKey(RegearCommand.getKitKey(stack))) trash.add(i);
            }
            for (Map.Entry<String, List<Integer>> entry : slots.entrySet()) {
                int required = RegearCommand.currentKit.getOrDefault(entry.getKey(), 0);
                int current = counts.get(entry.getKey());
                if (!RegearCommand.currentKit.containsKey(entry.getKey()) || current <= required) continue;
                entry.getValue().sort((left, right) -> Integer.compare(virtualInventory.get(left).getCount(), virtualInventory.get(right).getCount()));
                for (int slot : entry.getValue()) {
                    int stackSize = virtualInventory.get(slot).getCount();
                    if (current - stackSize >= required) {
                        trash.add(slot);
                        current -= stackSize;
                    }
                }
            }

            for (int source = 0; source < invStart; source++) {
                ItemStack sourceStack = container.getSlot(source).getStack();
                String key = RegearCommand.getKitKey(sourceStack);
                int required = missing.getOrDefault(key, 0);
                if (sourceStack.isEmpty() || required <= 0) continue;

                int destination = findMergeSlot(virtualInventory, invStart, container.slots.size(), key, trash);
                boolean merge = destination != -1;
                if (destination == -1 && !empty.isEmpty()) destination = empty.removeFirst();
                if (destination == -1 && !trash.isEmpty()) destination = trash.removeFirst();
                if (destination == -1) continue;

                ItemStack destinationStack = virtualInventory.get(destination);
                int moved = Math.min(sourceStack.getCount(), required);
                if (merge) {
                    moved = Math.min(moved, destinationStack.getMaxCount() - destinationStack.getCount());
                    destinationStack.setCount(destinationStack.getCount() + moved);
                } else {
                    ItemStack copy = sourceStack.copy();
                    copy.setCount(moved);
                    virtualInventory.put(destination, copy);
                }
                if (moved <= 0) continue;
                clickSwap(container, source, destination);
                missing.put(key, required - moved);
            }
        }
    }

    private int findMergeSlot(Map<Integer, ItemStack> inventory, int start, int end, String key, List<Integer> trash) {
        for (int i = start; i < end; i++) {
            ItemStack stack = inventory.get(i);
            if (!trash.contains(i) && !stack.isEmpty() && RegearCommand.getKitKey(stack).equals(key) && stack.getCount() < stack.getMaxCount()) return i;
        }
        return -1;
    }

    private void clickSwap(ScreenHandler handler, int source, int destination) {
        mc.interactionManager.clickSlot(handler.syncId, source, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(handler.syncId, destination, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(handler.syncId, source, 0, SlotActionType.PICKUP, mc.player);
    }

    /**
     * Rebuilds saved slots only after taking from the container.  Handler player slots are ordered as
     * main inventory (9-35), then hotbar (0-8), which is deliberately not the PlayerInventory order.
     */
    private void organizeSavedLayout(ScreenHandler handler) {
        if (RegearCommand.currentLayout.isEmpty() || handler.slots.size() < 36) return;

        int invStart = handler.slots.size() - 36;
        Map<Integer, ItemStack> inventory = new HashMap<>();
        for (int inventorySlot = 0; inventorySlot < 36; inventorySlot++) {
            inventory.put(inventorySlot, handler.getSlot(handlerSlotForInventory(invStart, inventorySlot)).getStack().copy());
        }

        for (int target = 0; target < 36; target++) {
            RegearCommand.LayoutSlot wanted = RegearCommand.currentLayout.get(target);
            if (wanted == null || wanted.isEmpty()) continue;

            // A donor swap advances a cycle while preserving every stack. The following iterations then
            // fill an under-sized target precisely by right-clicking only the required number of items.
            for (int attempts = 0; attempts < 36; attempts++) {
                ItemStack targetStack = inventory.get(target);
                if (matchesKey(targetStack, wanted.key)) {
                    int needed = wanted.count - targetStack.getCount();
                    if (needed <= 0) break;
                    int donor = findLayoutDonor(inventory, target, wanted.key);
                    if (donor == -1) break;
                    moveItems(handler, invStart, inventory, donor, target, needed);
                } else {
                    int donor = findLayoutDonor(inventory, target, wanted.key);
                    if (donor == -1) break;
                    swapInventorySlots(handler, invStart, inventory, donor, target);
                }
            }
        }
        moveExcessToSavedEmptySlots(handler, invStart, inventory);
        reportLayoutGaps(inventory);
    }

    private int handlerSlotForInventory(int invStart, int inventorySlot) {
        return inventorySlot < 9 ? invStart + 27 + inventorySlot : invStart + inventorySlot - 9;
    }

    private int findLayoutDonor(Map<Integer, ItemStack> inventory, int target, String key) {
        for (int slot = 0; slot < 36; slot++) {
            if (slot == target || !matchesKey(inventory.get(slot), key)) continue;
            RegearCommand.LayoutSlot saved = RegearCommand.currentLayout.get(slot);
            // Do not break a slot that already has exactly its requested stack; excess stacks remain donors.
            if (saved != null && !saved.isEmpty() && saved.key.equals(key) && inventory.get(slot).getCount() == saved.count) continue;
            return slot;
        }
        return -1;
    }

    private boolean matchesKey(ItemStack stack, String key) {
        return !stack.isEmpty() && RegearCommand.getKitKey(stack).equals(key);
    }

    private void swapInventorySlots(ScreenHandler handler, int invStart, Map<Integer, ItemStack> inventory, int first, int second) {
        clickSwap(handler, handlerSlotForInventory(invStart, first), handlerSlotForInventory(invStart, second));
        ItemStack firstStack = inventory.get(first);
        inventory.put(first, inventory.get(second));
        inventory.put(second, firstStack);
    }

    private void moveItems(ScreenHandler handler, int invStart, Map<Integer, ItemStack> inventory, int source, int destination, int requested) {
        ItemStack sourceStack = inventory.get(source);
        ItemStack destinationStack = inventory.get(destination);
        int capacity = destinationStack.isEmpty() ? sourceStack.getMaxCount() : destinationStack.getMaxCount() - destinationStack.getCount();
        int moved = Math.min(requested, Math.min(sourceStack.getCount(), capacity));
        if (moved <= 0) return;
        ItemStack movedStack = sourceStack.copy();

        int sourceHandlerSlot = handlerSlotForInventory(invStart, source);
        int destinationHandlerSlot = handlerSlotForInventory(invStart, destination);
        mc.interactionManager.clickSlot(handler.syncId, sourceHandlerSlot, 0, SlotActionType.PICKUP, mc.player);
        for (int click = 0; click < moved; click++) {
            mc.interactionManager.clickSlot(handler.syncId, destinationHandlerSlot, 1, SlotActionType.PICKUP, mc.player);
        }
        if (moved < sourceStack.getCount()) {
            mc.interactionManager.clickSlot(handler.syncId, sourceHandlerSlot, 0, SlotActionType.PICKUP, mc.player);
        }

        sourceStack.decrement(moved);
        if (destinationStack.isEmpty()) {
            movedStack.setCount(moved);
            inventory.put(destination, movedStack);
        } else {
            destinationStack.increment(moved);
        }
    }

    private void moveExcessToSavedEmptySlots(ScreenHandler handler, int invStart, Map<Integer, ItemStack> inventory) {
        for (int source = 0; source < 36; source++) {
            RegearCommand.LayoutSlot saved = RegearCommand.currentLayout.get(source);
            ItemStack sourceStack = inventory.get(source);
            if (saved == null || saved.isEmpty() || !matchesKey(sourceStack, saved.key) || sourceStack.getCount() <= saved.count) continue;

            for (int destination = 0; destination < 36 && sourceStack.getCount() > saved.count; destination++) {
                RegearCommand.LayoutSlot destinationSaved = RegearCommand.currentLayout.get(destination);
                ItemStack destinationStack = inventory.get(destination);
                if (destinationSaved == null || !destinationSaved.isEmpty()
                        || !destinationStack.isEmpty() && !matchesKey(destinationStack, saved.key)) continue;
                moveItems(handler, invStart, inventory, source, destination, sourceStack.getCount() - saved.count);
            }
        }
    }

    private void reportLayoutGaps(Map<Integer, ItemStack> inventory) {
        if (!debug.getValue()) return;
        Map<String, Integer> available = new HashMap<>();
        Map<String, Integer> required = new HashMap<>();
        List<String> gaps = new ArrayList<>();
        for (ItemStack stack : inventory.values()) {
            if (!stack.isEmpty()) available.merge(RegearCommand.getKitKey(stack), stack.getCount(), Integer::sum);
        }
        for (RegearCommand.LayoutSlot slot : RegearCommand.currentLayout.values()) {
            if (!slot.isEmpty()) required.merge(slot.key, slot.count, Integer::sum);
        }
        for (Map.Entry<Integer, RegearCommand.LayoutSlot> entry : RegearCommand.currentLayout.entrySet()) {
            RegearCommand.LayoutSlot wanted = entry.getValue();
            ItemStack actual = inventory.get(entry.getKey());
            if (wanted.isEmpty() || matchesKey(actual, wanted.key) && actual.getCount() >= wanted.count) continue;
            String reason = available.getOrDefault(wanted.key, 0) == 0 ? "缺少匹配物品"
                    : available.getOrDefault(wanted.key, 0) < required.getOrDefault(wanted.key, 0) ? "数量不足" : "无法归位";
            gaps.add("槽位 " + entry.getKey() + "（" + reason + "）");
        }
        if (!gaps.isEmpty()) {
            int end = Math.min(gaps.size(), 8);
            sendMessage("§e[ChestStealer] 固定槽位未满足：§f" + String.join("§7, §f", gaps.subList(0, end))
                    + (gaps.size() > end ? "§7 等 " + gaps.size() + " 项" : ""));
        }
    }

    private int getBestQualifiedShulkerSlot() {
        Map<String, Integer> missing = missingInventoryItems();
        int best = -1, bestSupply = -1, leastContent = Integer.MAX_VALUE;
        for (int i = 0; i < 36; i++) {
            ItemStack shulker = mc.player.getInventory().getStack(i);
            if (!isShulkerQualified(shulker)) continue;
            ItemStack[] contents = new ItemStack[27];
            ShulkerViewer.getItemsInContainerItem(shulker, contents);
            int supply = 0, content = 0;
            for (ItemStack item : contents) {
                String key = RegearCommand.getKitKey(item);
                if (RegearCommand.currentKit.containsKey(key)) content += item.getCount();
                supply += Math.min(item.getCount(), missing.getOrDefault(key, 0));
            }
            if (supply > bestSupply || supply == bestSupply && content < leastContent) { best = i < 9 ? i + 36 : i; bestSupply = supply; leastContent = content; }
        }
        return best;
    }

    private Map<String, Integer> missingInventoryItems() {
        Map<String, Integer> current = new HashMap<>(), missing = new HashMap<>();
        for (int i = 0; i < 36; i++) { ItemStack stack = mc.player.getInventory().getStack(i); if (!stack.isEmpty()) current.merge(RegearCommand.getKitKey(stack), stack.getCount(), Integer::sum); }
        for (Map.Entry<String, Integer> entry : RegearCommand.currentKit.entrySet()) if (current.getOrDefault(entry.getKey(), 0) < entry.getValue()) missing.put(entry.getKey(), entry.getValue() - current.getOrDefault(entry.getKey(), 0));
        return missing;
    }

    private boolean isShulkerQualified(ItemStack shulker) {
        if (!(shulker.getItem() instanceof BlockItem block && block.getBlock() instanceof ShulkerBoxBlock)) return false;
        ItemStack[] contents = new ItemStack[27];
        ShulkerViewer.getItemsInContainerItem(shulker, contents);
        Map<String, Integer> contained = new HashMap<>();
        for (ItemStack item : contents) if (!item.isEmpty()) contained.merge(RegearCommand.getKitKey(item), item.getCount(), Integer::sum);
        if (contained.isEmpty()) return false;
        for (Map.Entry<String, Integer> entry : RegearCommand.currentKit.entrySet()) {
            String key = entry.getKey();
            if (isWhitelisted(key)) continue;
            int total = getInventoryCount(key) + contained.getOrDefault(key, 0);
            int threshold = check.getValue() ? checkThreshold.getValueInt() : key.contains("totem_of_undying") ? minTotem.getValueInt() : (int) Math.ceil(entry.getValue() * ratio.getValue());
            if (total < threshold) return false;
        }
        return true;
    }

    private void reportNearestShulkerFailure() {
        int nearestSlot = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!(stack.getItem() instanceof BlockItem block && block.getBlock() instanceof ShulkerBoxBlock)) continue;

            // All inventory slots are equally reachable, so retain the first one for a stable report.
            if (nearestSlot == -1) {
                nearestSlot = i;
            }
        }

        if (nearestSlot == -1) {
            sendMessage("§e[ChestStealer] 未找到潜影盒，无法补给。");
            return;
        }

        ItemStack shulker = mc.player.getInventory().getStack(nearestSlot);
        ItemStack[] contents = new ItemStack[27];
        ShulkerViewer.getItemsInContainerItem(shulker, contents);
        Map<String, Integer> contained = new HashMap<>();
        for (ItemStack item : contents) {
            if (!item.isEmpty()) {
                contained.merge(RegearCommand.getKitKey(item), item.getCount(), Integer::sum);
            }
        }

        if (contained.isEmpty()) {
            sendMessage("§e[ChestStealer] 最近的潜影盒（背包槽位 " + nearestSlot + "）为空。");
            return;
        }

        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : RegearCommand.currentKit.entrySet()) {
            String key = entry.getKey();
            if (isWhitelisted(key)) continue;

            int required = check.getValue()
                    ? checkThreshold.getValueInt()
                    : key.contains("totem_of_undying")
                            ? minTotem.getValueInt()
                            : (int) Math.ceil(entry.getValue() * ratio.getValue());
            int available = getInventoryCount(key) + contained.getOrDefault(key, 0);
            if (available < required) {
                missing.add(getReadableKitName(key) + " " + available + "/" + required);
            }
        }

        if (missing.isEmpty()) {
            sendMessage("§e[ChestStealer] 最近的潜影盒未通过资格检查。");
        } else {
            sendMessage("§e[ChestStealer] 最近的潜影盒缺少：§c" + String.join("§7, §c", missing));
        }
    }

    private String getReadableKitName(String key) {
        int colorSeparator = key.indexOf('#');
        return Text.translatable(colorSeparator == -1 ? key : key.substring(0, colorSeparator)).getString();
    }

    private int getInventoryCount(String key) {
        int count = 0;
        for (int i = 0; i < 36; i++) { ItemStack stack = mc.player.getInventory().getStack(i); if (RegearCommand.getKitKey(stack).equals(key)) count += stack.getCount(); }
        if (RegearCommand.getKitKey(mc.player.getOffHandStack()).equals(key)) count += mc.player.getOffHandStack().getCount();
        return count;
    }

    private boolean isWhitelisted(String key) {
        if (key.contains("shulker_box")) return true;
        if (wGapple.getValue() && key.contains("enchanted_golden_apple") || wObsidian.getValue() && key.contains("obsidian") || wElytra.getValue() && key.contains("elytra") || wChorus.getValue() && key.contains("chorus_fruit") || wPearl.getValue() && key.contains("ender_pearl") || wPiston.getValue() && key.contains("piston") || wRedstone.getValue() && key.contains("redstone") || wAnchor.getValue() && key.contains("respawn_anchor") || wGlowstone.getValue() && key.contains("glowstone") || wEnderChest.getValue() && key.contains("ender_chest")) return true;
        return wTools.getValue() && (key.contains("pickaxe") || key.contains("axe") || key.contains("shovel") || key.contains("hoe")) || wArmor.getValue() && (key.contains("helmet") || key.contains("chestplate") || key.contains("leggings") || key.contains("boots")) || wWeapons.getValue() && (key.contains("sword") || key.contains("axe") || key.contains("bow") || key.contains("trident") || key.contains("crossbow"));
    }

    private void handleLegacySteal(ScreenHandler handler) {
        if (!take.getValue()) return;
        int size = handler.slots.size() - 36;
        for (int i = 0; i < size; i++) if (handler.getSlot(i).hasStack()) mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
        if (autoDisable.getValue()) disable2();
    }

    private void disable2() {
        if (disableTimer.passedMs(disableTime.getValue())) { mc.player.closeHandledScreen(); disable(); }
    }
}