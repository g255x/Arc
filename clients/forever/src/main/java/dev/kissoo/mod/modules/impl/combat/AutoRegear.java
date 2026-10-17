package dev.kissoo.mod.modules.impl.combat;

import dev.kissoo.Forever;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.Render2DEvent;
import dev.kissoo.api.events.impl.Render3DEvent;
import dev.kissoo.api.events.impl.RotationEvent;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.utils.math.Timer;
import dev.kissoo.api.utils.player.EntityUtil;
import dev.kissoo.api.utils.player.InventoryUtil;
import dev.kissoo.api.utils.render.ProjectionUtils;
import dev.kissoo.api.utils.render.Render2DUtil;
import dev.kissoo.api.utils.world.BlockPosX;
import dev.kissoo.api.utils.world.BlockUtil;
import dev.kissoo.core.Manager;
import dev.kissoo.core.impl.BreakManager;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.impl.movement.ElytraFlight;
import dev.kissoo.mod.modules.impl.player.AutoArmor;
import dev.kissoo.mod.modules.impl.player.SpeedMine;
import dev.kissoo.mod.modules.settings.impl.*;
import net.minecraft.block.Blocks;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import hoprc.asm.accessors.IContainerComponent;
import org.joml.Vector3f;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutoRegear extends Module {
    public static AutoRegear INSTANCE;

    // ── 基础设置 ──────────────────────────────────────────────────
    public final StringSetting kitName = this.add(new StringSetting("KitName", "default"));
    public final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
    private final SliderSetting rotateSpeed = this.add(new SliderSetting("RotateSpeed", 0.7, 0.1, 1.0, 0.01, () -> this.rotate.getValue()));
    private final SliderSetting rotatePriority = this.add(new SliderSetting("RotatePriority", 100.0, 1.0, 100.0, 1.0, () -> this.rotate.getValue()));

    public final EnumSetting<PlaceType> placeType = this.add(new EnumSetting<>("PlaceType", PlaceType.Random));
    private final SliderSetting placeTryCount = this.add(new SliderSetting("TryCount", 5, 1, 20, () -> this.placeType.getValue() == PlaceType.Burrow || this.placeType.getValue() == PlaceType.Tower));
    private final SliderSetting smartX = this.add(new SliderSetting("SmartXZ", 3.0, 0.0, 10.0, 0.1, () -> this.placeType.getValue() == PlaceType.Burrow));
    private final SliderSetting smartUp = this.add(new SliderSetting("SmartUp", 3.0, 0.0, 10.0, 0.1, () -> this.placeType.getValue() == PlaceType.Burrow));
    private final SliderSetting smartDown = this.add(new SliderSetting("SmartDown", 3.0, 0.0, 10.0, 0.1, () -> this.placeType.getValue() == PlaceType.Burrow));
    private final SliderSetting smartDistance = this.add(new SliderSetting("SmartDistance", 2.0, 0.0, 10.0, 0.1, () -> this.placeType.getValue() == PlaceType.Burrow));

    private final SliderSetting regearDelay = this.add(new SliderSetting("RegearDelay", 50.0, 0.0, 1000.0, 1.0));
    private final SliderSetting perAction = this.add(new SliderSetting("PerAction", 1.0, 1.0, 10.0, 1.0));

    private final SliderSetting placeDelay = this.add(new SliderSetting("PlaceDelay", 200.0, 0.0, 500.0, 10.0));
    private final SliderSetting enderChestDelay = this.add(new SliderSetting("EnderChestDelay", 150.0, 0.0, 300.0, 10.0));
    private final SliderSetting openDelay = this.add(new SliderSetting("OpenDelay", 200.0, 0.0, 500.0, 10.0));
    private final BooleanSetting enderChest = this.add(new BooleanSetting("EnderChest", true));
    private final BooleanSetting autoContinue = this.add(new BooleanSetting("AutoContinue", true));
    private final BooleanSetting checkKitType = this.add(new BooleanSetting("SmartSearch", true));
    private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true));
    private final SliderSetting disableTime = this.add(new SliderSetting("DisableTime", 500, 0, 1000));
    private final BooleanSetting place = this.add(new BooleanSetting("Place", true));
    private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
    private final BooleanSetting open = this.add(new BooleanSetting("Open", true));
    private final SliderSetting range = this.add(new SliderSetting("MaxRange", 4.0, 0.0, 6.0, 0.1));
    private final BooleanSetting mine = this.add(new BooleanSetting("Mine", true));
    private final BooleanSetting take = this.add(new BooleanSetting("Take", true));
    private final BooleanSetting regearArmor = this.add(new BooleanSetting("RegearArmor", true));
    private final BooleanSetting returnExtras = this.add(new BooleanSetting("ReturnExtras", true));
    private final BindSetting placeKey = this.add(new BindSetting("PlaceKey", -1));
    public final BooleanSetting silentDisplay = this.add(new BooleanSetting("SilentDisplay", true));
    private final SliderSetting height = this.add(new SliderSetting("Height", 1.5, 0.0, 3.0, 0.1, () -> silentDisplay.getValue()));
    private final SliderSetting scale = this.add(new SliderSetting("Scale", 1.0, 0.5, 2.0, 0.1, () -> silentDisplay.getValue()));
    private final SliderSetting armorCoin = this.add(new SliderSetting("ArmorCoin", 1000, 1, 5000, 1, () -> checkKitType.getValue()));
    private final SliderSetting weaponCoin = this.add(new SliderSetting("WeaponCoin", 1000, 1, 5000, 1, () -> checkKitType.getValue()));
    private final SliderSetting regearItemCoin = this.add(new SliderSetting("RegearItemCoin", 1, 1, 5000, 1, () -> checkKitType.getValue()));

    // ── XCarryRegear 设置 ─────────────────────────────────────────
    private final BooleanSetting xCarryRegear = this.add(new BooleanSetting("XCarryRegear", false));

    // ── 计时器 ────────────────────────────────────────────────────
    private final Timer timer = new Timer();
    public final Timer timeoutTimer = new Timer();
    private final Timer openTimer = new Timer();
    private final Timer actionTimer = new Timer();
    private final Timer xCarryMoveTimer = new Timer();

    // ── 状态字段 ──────────────────────────────────────────────────
    public BlockPos placePos = null;
    private BlockPos openPos;
    private boolean opend = false;
    private boolean on = false;
    private boolean finishedRegear = false;
    private boolean retrievingFromEnderChest = false;
    private boolean armorPhaseComplete = false;
    private boolean xCarryPhaseComplete = false;
    private int currentTryCount = 0;

    private final Map<Integer, Item> targetKit = new HashMap<>();
    private Vector3f currentScreenPos = null;
    private final List<Pulse> pulses = new ArrayList<>();
    private final List<Runnable> armorActionQueue = new ArrayList<>();
    private int armorActionIndex = 0;
    public boolean silentScreenActive = false;

    public AutoRegear() {
        super("AutoRegear", Module.Category.Combat);
        this.setChinese("自动补给");
        INSTANCE = this;
    }

    // ══════════════════════════════════════════════════════════════
    // ElytraFlight 相关
    // ══════════════════════════════════════════════════════════════

    private boolean isElytraFlyMode() {
        return ElytraFlight.INSTANCE != null
                && ElytraFlight.INSTANCE.isOn()
                && AutoArmor.INSTANCE != null
                && AutoArmor.INSTANCE.isOn()
                && AutoArmor.INSTANCE.autoElytra.getValue();
    }

    private Map<Integer, Item> getEffectiveKit() {
        Map<Integer, Item> effective = new HashMap<>(targetKit);
        if (!isElytraFlyMode()) return effective;

        Item chestItem = effective.getOrDefault(38, Items.AIR);
        if (chestItem instanceof ElytraItem || chestItem == Items.ELYTRA) return effective;

        int elytraSlot = -1;
        for (Map.Entry<Integer, Item> entry : effective.entrySet()) {
            if (entry.getKey() < 36 && entry.getValue() == Items.ELYTRA) {
                elytraSlot = entry.getKey();
                break;
            }
        }

        if (elytraSlot != -1) {
            effective.put(38, Items.ELYTRA);
            effective.put(elytraSlot, chestItem);
        } else if (chestItem != Items.AIR) {
            effective.put(38, Items.ELYTRA);
            for (int i = 0; i < 36; i++) {
                if (!effective.containsKey(i)) {
                    effective.put(i, chestItem);
                    break;
                }
            }
        }
        return effective;
    }

    // ══════════════════════════════════════════════════════════════
    // 物品分类判断
    // ══════════════════════════════════════════════════════════════

    private boolean isArmorItem(Item item) {
        return item == Items.NETHERITE_HELMET || item == Items.NETHERITE_CHESTPLATE
                || item == Items.NETHERITE_LEGGINGS || item == Items.NETHERITE_BOOTS
                || item == Items.DIAMOND_HELMET || item == Items.DIAMOND_CHESTPLATE
                || item == Items.DIAMOND_LEGGINGS || item == Items.DIAMOND_BOOTS
                || item == Items.IRON_HELMET || item == Items.IRON_CHESTPLATE
                || item == Items.IRON_LEGGINGS || item == Items.IRON_BOOTS
                || item == Items.GOLDEN_HELMET || item == Items.GOLDEN_CHESTPLATE
                || item == Items.GOLDEN_LEGGINGS || item == Items.GOLDEN_BOOTS
                || item == Items.CHAINMAIL_HELMET || item == Items.CHAINMAIL_CHESTPLATE
                || item == Items.CHAINMAIL_LEGGINGS || item == Items.CHAINMAIL_BOOTS
                || item == Items.LEATHER_HELMET || item == Items.LEATHER_CHESTPLATE
                || item == Items.LEATHER_LEGGINGS || item == Items.LEATHER_BOOTS
                || item == Items.TURTLE_HELMET || item == Items.ELYTRA;
    }

    private boolean isWeaponItem(Item item) {
        return item == Items.NETHERITE_SWORD || item == Items.DIAMOND_SWORD
                || item == Items.IRON_SWORD || item == Items.GOLDEN_SWORD
                || item == Items.STONE_SWORD || item == Items.WOODEN_SWORD
                || item == Items.NETHERITE_PICKAXE || item == Items.DIAMOND_PICKAXE
                || item == Items.IRON_PICKAXE || item == Items.GOLDEN_PICKAXE
                || item == Items.STONE_PICKAXE || item == Items.WOODEN_PICKAXE;
    }

    private int getItemCoin(Item item) {
        if (isArmorItem(item)) return armorCoin.getValueInt();
        else if (isWeaponItem(item)) return weaponCoin.getValueInt();
        else return regearItemCoin.getValueInt();
    }

    private boolean itemsMatch(Item item1, Item item2) {
        if (item1 == item2) return true;
        boolean p1 = item1 == Items.PISTON || item1 == Items.STICKY_PISTON;
        boolean p2 = item2 == Items.PISTON || item2 == Items.STICKY_PISTON;
        return p1 && p2;
    }

    private boolean strictMatch(Item item1, Item item2) {
        return item1 == item2;
    }

    // ══════════════════════════════════════════════════════════════
    // 状态查询
    // ══════════════════════════════════════════════════════════════

    public boolean hasActiveShulkerScreen() {
        if (mc.player == null) return false;
        return mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler;
    }

    public boolean isWorking() {
        return this.isOn() && (this.hasActiveShulkerScreen() || this.silentScreenActive
                || this.retrievingFromEnderChest || this.opend || this.on
                || !this.timer.passed(2000) || !this.openTimer.passed(2000));
    }

    // ══════════════════════════════════════════════════════════════
    // 旋转事件
    // ══════════════════════════════════════════════════════════════

    @EventListener
    public void onRotate(RotationEvent event) {
        if (this.rotate.getValue()) {
            BlockPos targetPos = null;
            if (this.openPos != null) targetPos = this.openPos;
            else if (this.placePos != null) targetPos = this.placePos;

            if (targetPos != null && this.isWorking()) {
                event.setTarget(targetPos.toCenterPos(), this.rotateSpeed.getValueFloat(), this.rotatePriority.getValueFloat());
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // 辅助方法
    // ══════════════════════════════════════════════════════════════

    private void silentCloseScreen() {
        if (mc.player != null) {
            mc.player.closeHandledScreen();
        }
        this.silentScreenActive = false;
    }

    private BlockPos findNearbyShulkerBlock() {
        for (BlockPos pos : BlockUtil.getSphere((float) this.range.getValue())) {
            if (mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock) {
                if (mc.world.isAir(pos.up()) || BlockUtil.canReplace(pos.up())) {
                    return pos;
                }
            }
        }
        return null;
    }

    private BlockPos findNearbyEnderChestBlock() {
        for (BlockPos pos : BlockUtil.getSphere((float) this.range.getValue())) {
            if (mc.world.getBlockState(pos).getBlock() == Blocks.ENDER_CHEST) {
                if (mc.world.isAir(pos.up()) || BlockUtil.canReplace(pos.up())) {
                    return pos;
                }
            }
        }
        return null;
    }

    // ══════════════════════════════════════════════════════════════
    // onEnable / onDisable
    // ══════════════════════════════════════════════════════════════

    @Override
    public void onEnable() {
        this.opend = false;
        this.openPos = null;
        this.timeoutTimer.reset();
        this.timer.reset();
        this.openTimer.reset();
        this.actionTimer.reset();
        this.xCarryMoveTimer.reset();
        this.placePos = null;
        this.finishedRegear = false;
        this.retrievingFromEnderChest = false;
        this.armorPhaseComplete = false;
        this.xCarryPhaseComplete = false;
        this.armorActionQueue.clear();
        this.armorActionIndex = 0;
        this.pulses.clear();
        this.currentScreenPos = null;
        this.silentScreenActive = false;
        this.currentTryCount = 0;

        if (!loadKit(kitName.getValue()) && take.getValue()) {
            this.disable();
            return;
        }

        // XCarryRegear 模式：如果合成栏有潜影盒，等 onUpdate 驱动搬运，不立即 doPlace
        if (xCarryRegear.getValue() && hasShulkerInCraftSlots()) {
            return;
        }

        if (!nullCheck()) {
            BlockPos nearbyShulker = findNearbyShulkerBlock();
            if (nearbyShulker != null) {
                this.placePos = nearbyShulker;
            } else if (this.place.getValue()) {
                this.doPlace();
            }
        }
    }

    @Override
    public void onDisable() {
        if (this.mine.getValue()) {
            BlockPos targetPos = this.openPos != null ? this.openPos : this.placePos;
            if (targetPos != null && mc.world.getBlockState(targetPos).getBlock() instanceof ShulkerBoxBlock) {
                if (SpeedMine.INSTANCE != null && !SpeedMine.INSTANCE.isOn()) {
                    SpeedMine.INSTANCE.enable();
                }
                if (SpeedMine.INSTANCE != null) {
                    SpeedMine.INSTANCE.mine(targetPos);
                }
            }
        }

        this.opend = false;
        this.openPos = null;
        this.retrievingFromEnderChest = false;
        this.armorPhaseComplete = false;
        this.xCarryPhaseComplete = false;
        this.armorActionQueue.clear();
        this.armorActionIndex = 0;
        this.pulses.clear();
        this.currentScreenPos = null;
        this.currentTryCount = 0;

        if (this.silentScreenActive && mc.player != null) {
            mc.player.closeHandledScreen();
        }
        this.silentScreenActive = false;
    }

    // ══════════════════════════════════════════════════════════════
    // 主更新循环
    // ══════════════════════════════════════════════════════════════

    @EventListener
    public void onUpdate(UpdateEvent event) {
        // ── PlaceKey 热键 ─────────────────────────────────────────
        if (this.placeKey.isPressed() && mc.currentScreen == null && !silentScreenActive) {
            if (!this.on) {
                this.opend = false;
                this.openPos = null;
                this.timeoutTimer.reset();
                this.timer.reset();
                this.openTimer.reset();
                this.actionTimer.reset();
                this.xCarryMoveTimer.reset();
                this.retrievingFromEnderChest = false;
                this.armorPhaseComplete = false;
                this.xCarryPhaseComplete = false;
                this.armorActionQueue.clear();
                this.armorActionIndex = 0;
                this.currentTryCount = 0;
                this.doPlace();
            }
            this.on = true;
        } else {
            this.on = false;
        }

        // ── XCarryRegear 阶段：将合成栏潜影盒搬入背包 ────────────
        if (xCarryRegear.getValue() && !xCarryPhaseComplete) {
            if (tickXCarryMove()) {
                // 本 tick 执行了搬运操作，等待下一个延迟周期
                return;
            }
            // 合成栏已无潜影盒，阶段结束
            xCarryPhaseComplete = true;

            // 阶段完成后触发正常放置流程
            if (!nullCheck()) {
                BlockPos nearbyShulker = findNearbyShulkerBlock();
                if (nearbyShulker != null) {
                    this.placePos = nearbyShulker;
                } else if (this.place.getValue()) {
                    this.doPlace();
                }
            }
        }

        // ── 末影箱取回潜影盒阶段 ──────────────────────────────────
        if (this.retrievingFromEnderChest) {
            if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler container) {
                if (findBestShulker() != -1) {
                    mc.player.closeHandledScreen();
                    this.retrievingFromEnderChest = false;
                    this.openPos = null;
                    this.timer.reset();
                    if (!autoContinue.getValue()) {
                        this.disable();
                    }
                    return;
                }

                if (this.timer.passed(enderChestDelay.getValueInt())) {
                    int bestSlot = -1;

                    if (checkKitType.getValue()) {
                        Map<Integer, Item> effectiveKit = getEffectiveKit();
                        List<Item> missingItems = getMissingItems(effectiveKit);
                        int maxScore = -1;

                        for (int i = 0; i < container.getInventory().size(); i++) {
                            ItemStack stack = container.getSlot(i).getStack();
                            if (isShulkerBox(stack)) {
                                int score = getShulkerScore(stack, missingItems);
                                if (score > maxScore) {
                                    maxScore = score;
                                    bestSlot = i;
                                }
                            }
                        }
                    } else {
                        for (int i = 0; i < container.getInventory().size(); i++) {
                            if (isShulkerBox(container.getSlot(i).getStack())) {
                                bestSlot = i;
                                break;
                            }
                        }
                    }

                    if (bestSlot != -1) {
                        boolean hasSpace = false;
                        for (int i = 0; i < 36; i++) {
                            if (mc.player.getInventory().getStack(i).isEmpty()) {
                                hasSpace = true;
                                break;
                            }
                        }

                        if (!hasSpace) {
                            mc.player.closeHandledScreen();
                            this.openPos = null;
                            this.disable();
                            return;
                        }

                        mc.interactionManager.clickSlot(container.syncId, bestSlot, 0, SlotActionType.QUICK_MOVE, mc.player);
                        this.timer.reset();
                    } else {
                        mc.player.closeHandledScreen();
                        this.openPos = null;
                        this.disable();
                    }
                }
                return;
            } else {
                this.retrievingFromEnderChest = false;
                this.openPos = null;
            }
        }

        // ── 潜影盒已打开阶段 ──────────────────────────────────────
        if (hasActiveShulkerScreen()) {
            ShulkerBoxScreenHandler shulker = (ShulkerBoxScreenHandler) mc.player.currentScreenHandler;
            this.opend = true;
            this.openPos = this.openPos == null ? this.placePos : this.openPos;

            if (silentDisplay.getValue()) {
                this.silentScreenActive = true;
            }

            if (this.take.getValue()) {
                Map<Integer, Item> effectiveKit = getEffectiveKit();
                if (!actionTimer.passed((long) regearDelay.getValue())) return;

                // 护甲阶段
                if (this.regearArmor.getValue() && !armorPhaseComplete) {
                    processArmorPhase(shulker, effectiveKit);
                    return;
                }

                // 常规补给阶段
                int actionsThisCycle = 0;
                boolean totalActed = false;
                while (actionsThisCycle < perAction.getValueInt()) {
                    boolean actedThisStep = false;

                    if (this.returnExtras.getValue() && !actedThisStep) {
                        actedThisStep = returnExtraItems(shulker, effectiveKit);
                    }
                    if (!actedThisStep) actedThisStep = sortInventory(shulker, effectiveKit);
                    if (!actedThisStep) actedThisStep = takeNeededItems(shulker, effectiveKit);

                    if (actedThisStep) {
                        actionsThisCycle++;
                        totalActed = true;
                    } else {
                        break;
                    }
                }

                if (totalActed) {
                    this.timeoutTimer.reset();
                    this.actionTimer.reset();
                } else if (this.autoDisable.getValue()) {
                    if (this.timeoutTimer.passed(this.disableTime.getValueInt())) {
                        this.finishedRegear = true;
                        if (silentScreenActive) {
                            silentCloseScreen();
                        } else {
                            mc.player.closeHandledScreen();
                        }
                        this.disable();
                    }
                }
            }
        } else {
            // ── 潜影盒关闭后处理 ──────────────────────────────────
            if (this.opend) {
                this.opend = false;
                this.silentScreenActive = false;
                this.armorPhaseComplete = false;
                this.armorActionQueue.clear();
                this.armorActionIndex = 0;

                if (this.mine.getValue() && this.openPos != null) {
                    if (mc.world.getBlockState(this.openPos).getBlock() instanceof ShulkerBoxBlock) {
                        if (SpeedMine.INSTANCE != null) {
                            SpeedMine.INSTANCE.mine(this.openPos);
                        }
                    }
                }

                if (this.autoDisable.getValue()) this.disable();
            } else {
                boolean foundExisting = false;

                if (this.open.getValue() && !this.retrievingFromEnderChest) {
                    if (this.openTimer.passed(openDelay.getValueInt())) {
                        foundExisting = processOpening();
                        if (foundExisting) {
                            this.openTimer.reset();
                        }
                    }
                }

                if (!foundExisting && this.place.getValue()) {
                    this.doPlace();
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // XCarryRegear 核心逻辑
    // ══════════════════════════════════════════════════════════════

    /**
     * 检查合成栏槽位 1~4 中是否存在潜影盒
     */
    private boolean hasShulkerInCraftSlots() {
        if (mc.player == null) return false;
        for (int craftSlot = 1; craftSlot <= 4; craftSlot++) {
            ItemStack stack = mc.player.playerScreenHandler.getSlot(craftSlot).getStack();
            if (isShulkerBox(stack)) return true;
        }
        return false;
    }

    /**
     * XCarryRegear 阶段驱动：每次调用将合成栏（槽 1~4）中的一个潜影盒搬到背包第一个空位。
     *
     * @return true  = 执行了一次搬运操作，调用方应 return 等待延迟
     *         false = 合成栏已无潜影盒或背包已满，阶段可结束
     */
    private boolean tickXCarryMove() {
        if (mc.player == null) return false;

        // 受 regearDelay 节流
        if (!xCarryMoveTimer.passed((long) regearDelay.getValue())) return true;

        for (int craftSlot = 1; craftSlot <= 4; craftSlot++) {
            ItemStack craftStack = mc.player.playerScreenHandler.getSlot(craftSlot).getStack();
            if (!isShulkerBox(craftStack)) continue;

            // 找背包/快捷栏的第一个空位（playerScreenHandler 槽号 9~44）
            int emptySlot = findEmptyInventorySlot();
            if (emptySlot == -1) {
                // 背包已满，跳过 XCarry 阶段
                xCarryPhaseComplete = true;
                return false;
            }

            int syncId = mc.player.playerScreenHandler.syncId;

            // 拾取合成栏中的潜影盒
            mc.interactionManager.clickSlot(syncId, craftSlot, 0, SlotActionType.PICKUP, mc.player);
            // 放入背包空位
            mc.interactionManager.clickSlot(syncId, emptySlot, 0, SlotActionType.PICKUP, mc.player);
            // 若光标上仍有残留，放回合成栏
            if (!mc.player.playerScreenHandler.getCursorStack().isEmpty()) {
                mc.interactionManager.clickSlot(syncId, craftSlot, 0, SlotActionType.PICKUP, mc.player);
            }

            xCarryMoveTimer.reset();
            return true;
        }

        // 合成栏 1~4 中已无潜影盒
        return false;
    }

    /**
     * 在 playerScreenHandler 的背包区域（槽 9~44）中找第一个空位。
     *
     * @return playerScreenHandler 槽号，找不到返回 -1
     */
    private int findEmptyInventorySlot() {
        if (mc.player == null) return -1;
        for (int slot = 9; slot <= 44; slot++) {
            if (mc.player.playerScreenHandler.getSlot(slot).getStack().isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    // ══════════════════════════════════════════════════════════════
    // 护甲阶段
    // ══════════════════════════════════════════════════════════════

    private void processArmorPhase(ShulkerBoxScreenHandler shulker, Map<Integer, Item> kit) {
        if (armorActionQueue.isEmpty() && armorActionIndex == 0) {
            buildArmorActionQueue(shulker, kit);
            if (armorActionQueue.isEmpty()) {
                armorPhaseComplete = true;
                return;
            }
        }

        if (armorActionIndex >= armorActionQueue.size()) {
            armorPhaseComplete = true;
            armorActionQueue.clear();
            armorActionIndex = 0;
            this.timeoutTimer.reset();
            return;
        }

        int actionsThisCycle = 0;
        while (actionsThisCycle < perAction.getValueInt() && armorActionIndex < armorActionQueue.size()) {
            armorActionQueue.get(armorActionIndex).run();
            armorActionIndex++;
            actionsThisCycle++;
        }

        this.actionTimer.reset();
        this.timeoutTimer.reset();
    }

    private boolean isPositionBeingMined(BlockPos pos) {
        if (SpeedMine.INSTANCE != null && SpeedMine.INSTANCE.isOn()) {
            if (pos.equals(SpeedMine.getBreakPos())) return true;
        }
        if (Forever.BREAK != null) {
            for (BreakManager.BreakData data : Forever.BREAK.breakMap.values()) {
                if (data != null && pos.equals(data.pos)) return true;
            }
            for (BreakManager.BreakData data : Forever.BREAK.doubleMap.values()) {
                if (data != null && pos.equals(data.pos)) return true;
            }
        }
        return false;
    }

    private void buildArmorActionQueue(ShulkerBoxScreenHandler shulker, Map<Integer, Item> kit) {
        armorActionQueue.clear();
        for (int i = 36; i < 40; i++) {
            if (!kit.containsKey(i)) continue;
            Item desired = kit.get(i);
            if (desired == Items.AIR) continue;

            ItemStack current = mc.player.getInventory().getStack(i);
            if (current.getItem() == desired) continue;
            if (i == 38 && desired == Items.ELYTRA && current.getItem() == Items.ELYTRA && ElytraItem.isUsable(current)) {
                continue;
            }

            int slotId = -1;
            for (int j = 0; j < 27; j++) {
                if (shulker.getSlot(j).getStack().getItem() == desired) {
                    slotId = j;
                    break;
                }
            }
            if (slotId == -1) continue;

            final int finalSlotId = slotId;
            final int armorSlot = i;
            final int hotbar = mc.player.getInventory().selectedSlot;

            armorActionQueue.add(() -> windowClick(shulker.syncId, finalSlotId, hotbar, SlotActionType.SWAP));

            armorActionQueue.add(() -> {
                if (armorSlot == 40) {
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                            PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                            BlockPos.ORIGIN, Direction.DOWN));
                } else {
                    mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(
                            Hand.MAIN_HAND, 0,
                            Forever.ROTATION.getLastYaw(),
                            Forever.ROTATION.getLastPitch()));
                }
            });

            armorActionQueue.add(() -> windowClick(shulker.syncId, finalSlotId, hotbar, SlotActionType.SWAP));
        }
    }

    // ══════════════════════════════════════════════════════════════
    // 潜影盒评分相关
    // ══════════════════════════════════════════════════════════════

    private List<Item> getMissingItems(Map<Integer, Item> kit) {
        List<Item> missing = new ArrayList<>();
        for (Map.Entry<Integer, Item> entry : kit.entrySet()) {
            int slot = entry.getKey();
            Item desired = entry.getValue();
            if (desired == Items.AIR) continue;
            if (slot < 0 || slot >= mc.player.getInventory().size()) continue;

            ItemStack currentStack = mc.player.getInventory().getStack(slot);
            boolean isMissing = false;
            if (!itemsMatch(currentStack.getItem(), desired)) {
                isMissing = true;
            } else if (currentStack.isStackable() && currentStack.getCount() < currentStack.getMaxCount()) {
                isMissing = true;
            }

            if (isMissing) {
                boolean alreadyListed = false;
                for (Item m : missing) {
                    if (itemsMatch(m, desired)) {
                        alreadyListed = true;
                        break;
                    }
                }
                if (!alreadyListed) {
                    missing.add(desired);
                }
            }
        }
        return missing;
    }

    private int getShulkerScore(ItemStack shulkerStack, List<Item> missingItems) {
        int score = 0;
        List<ItemStack> contents = getShulkerContents(shulkerStack);
        for (ItemStack itemInBox : contents) {
            if (itemInBox.isEmpty()) continue;
            Item item = itemInBox.getItem();
            boolean isNeeded = false;
            for (Item missing : missingItems) {
                if (itemsMatch(missing, item)) {
                    isNeeded = true;
                    break;
                }
            }
            if (isNeeded) score += getItemCoin(item);
        }
        return score;
    }

    // ══════════════════════════════════════════════════════════════
    // 查找最优潜影盒 / 末影箱
    // ══════════════════════════════════════════════════════════════

    public int findBestShulker() {
        if (!checkKitType.getValue()) {
            for (int i = 0; i < 36; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (isShulkerBox(stack)) return i < 9 ? i + 36 : i;
            }
            return -1;
        }

        Map<Integer, Item> effectiveKit = getEffectiveKit();
        List<Item> missingItems = getMissingItems(effectiveKit);
        int bestSlot = -1;
        int maxScore = -1;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isShulkerBox(stack)) {
                int score = getShulkerScore(stack, missingItems);
                if (score > maxScore) {
                    maxScore = score;
                    bestSlot = i < 9 ? i + 36 : i;
                }
            }
        }
        return bestSlot;
    }

    public int findEnderChestItem() {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.ENDER_CHEST) {
                return i < 9 ? i + 36 : i;
            }
        }
        return -1;
    }

    // ══════════════════════════════════════════════════════════════
    // 潜影盒工具方法
    // ══════════════════════════════════════════════════════════════

    private boolean isShulkerBox(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ShulkerBoxBlock;
    }

    private List<ItemStack> getShulkerContents(ItemStack itemStack) {
        List<ItemStack> items = new ArrayList<>();
        ComponentMap components = itemStack.getComponents();
        if (components.contains(DataComponentTypes.CONTAINER)) {
            IContainerComponent container = (IContainerComponent) (Object) components.get(DataComponentTypes.CONTAINER);
            items.addAll(container.getStacks());
        } else if (components.contains(DataComponentTypes.BLOCK_ENTITY_DATA)) {
            NbtComponent nbt2 = (NbtComponent) components.get(DataComponentTypes.BLOCK_ENTITY_DATA);
            if (nbt2.contains("Items")) {
                NbtList nbt3 = (NbtList) nbt2.getNbt().get("Items");
                for (int ix = 0; ix < nbt3.size(); ix++) {
                    items.add(ItemStack.fromNbtOrEmpty(mc.player.getRegistryManager(), nbt3.getCompound(ix)));
                }
            }
        }
        return items;
    }

    // ══════════════════════════════════════════════════════════════
    // 渲染
    // ══════════════════════════════════════════════════════════════

    @EventListener
    public void onRender3D(Render3DEvent event) {
        currentScreenPos = null;
        if (!silentDisplay.getValue() || !opend || mc.player == null) return;
        if (!hasActiveShulkerScreen()) return;

        BlockPos targetPos = this.openPos != null ? this.openPos : this.placePos;
        if (targetPos == null) return;

        Vec3d worldCenter = targetPos.toCenterPos().add(0, height.getValue(), 0);
        currentScreenPos = ProjectionUtils.project(worldCenter);
    }

    @EventListener
    public void onRender2D(Render2DEvent event) {
        if (currentScreenPos == null || !silentDisplay.getValue() || !opend) return;
        if (!hasActiveShulkerScreen()) return;

        ShulkerBoxScreenHandler handler = (ShulkerBoxScreenHandler) mc.player.currentScreenHandler;
        DrawContext context = event.drawContext;
        MatrixStack matrices = context.getMatrices();

        float screenX = currentScreenPos.x;
        float screenY = currentScreenPos.y;
        if (screenX < -200 || screenX > mc.getWindow().getScaledWidth() + 200
                || screenY < -200 || screenY > mc.getWindow().getScaledHeight() + 200) return;

        int gridWidth = 18 * 9;
        int gridHeight = 18 * 3;
        int padding = 6;
        float totalWidth = gridWidth + padding * 2;
        float totalHeight = gridHeight + padding * 2;
        float currentScale = (float) scale.getValue();

        matrices.push();
        matrices.translate(screenX, screenY, 0);
        matrices.scale(currentScale, currentScale, 1.0f);

        int startX = (int) (-totalWidth / 2f);
        int startY = (int) (-totalHeight / 2f);
        int endX = (int) (startX + totalWidth);
        int endY = (int) (startY + totalHeight);

        int bgColor = new Color(20, 20, 20, 160).getRGB();
        int borderColor = new Color(100, 100, 100, 200).getRGB();
        context.fill(startX, startY, endX, endY, bgColor);
        context.drawBorder(startX, startY, (int) totalWidth, (int) totalHeight, borderColor);

        for (int i = 0; i < 27; i++) {
            ItemStack stack = handler.getSlot(i).getStack();
            int row = i / 9;
            int col = i % 9;
            int slotX = (int) (startX + padding + col * 18);
            int slotY = (int) (startY + padding + row * 18);
            if (!stack.isEmpty()) {
                context.drawItem(stack, slotX, slotY);
                context.drawItemInSlot(mc.textRenderer, stack, slotX, slotY);
            }
            drawPulse2D(matrices, slotX + 9, slotY + 9, i);
        }

        matrices.pop();
        pulses.removeIf(p -> p.timer.getMs() > p.maxTime);
    }

    private void drawPulse2D(MatrixStack ms, float centerX, float centerY, int slotId) {
        for (Pulse p : pulses) {
            if (p.slotId == slotId) {
                float progress = Math.min(1.0f, (float) p.timer.getMs() / p.maxTime);
                float radius = progress * 10.0f;
                int maxAlpha = 80;
                int alpha = (int) ((1.0f - progress) * maxAlpha);
                if (alpha > 0) {
                    Color pulseColor = new Color(255, 255, 255, alpha);
                    Render2DUtil.drawCircle(ms, centerX, centerY, radius, pulseColor, 12);
                }
            }
        }
    }

    private void windowClick(int syncId, int slotId, int button, SlotActionType type) {
        mc.interactionManager.clickSlot(syncId, slotId, button, type, mc.player);
        if (slotId >= 0 && slotId < 27) {
            pulses.add(new Pulse(slotId));
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Kit 加载
    // ══════════════════════════════════════════════════════════════

    private boolean loadKit(String name) {
        targetKit.clear();
        try {
            File file = Manager.getFile(name + ".kit");
            if (!file.exists()) return false;
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 3) {
                    int slot = Integer.parseInt(parts[0]);
                    if (slot == 40) continue;
                    String idStr = parts[1] + ":" + parts[2];
                    targetKit.put(slot, Registries.ITEM.get(Identifier.of(idStr)));
                }
            }
            reader.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ══════════════════════════════════════════════════════════════
    // 补给逻辑（returnExtras / sortInventory / takeNeededItems）
    // ══════════════════════════════════════════════════════════════

    private boolean returnExtraItems(ShulkerBoxScreenHandler shulker, Map<Integer, Item> kit) {
        for (int playerSlot = 0; playerSlot < 36; playerSlot++) {
            int windowSlot = playerSlot < 9 ? 54 + playerSlot : 27 + (playerSlot - 9);
            ItemStack currentStack = shulker.getSlot(windowSlot).getStack();
            if (currentStack.isEmpty()) continue;

            Item currentItem = currentStack.getItem();
            Item desiredItem = kit.getOrDefault(playerSlot, Items.AIR);
            if (itemsMatch(currentItem, desiredItem)) continue;
            if (isItemNeededElsewhere(currentItem, playerSlot, kit)) continue;
            if (currentItem instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) continue;

            int emptyShulkerSlot = findEmptyShulkerSlot(shulker);
            if (emptyShulkerSlot != -1) {
                windowClick(shulker.syncId, windowSlot, 0, SlotActionType.QUICK_MOVE);
                return true;
            }
        }
        return false;
    }

    private boolean sortInventory(ShulkerBoxScreenHandler shulker, Map<Integer, Item> kit) {
        for (int playerSlot = 0; playerSlot < 36; playerSlot++) {
            Item desiredItem = kit.getOrDefault(playerSlot, Items.AIR);
            if (desiredItem == Items.AIR) continue;

            int windowSlot = playerSlot < 9 ? 54 + playerSlot : 27 + (playerSlot - 9);
            ItemStack currentStack = shulker.getSlot(windowSlot).getStack();
            if (itemsMatch(currentStack.getItem(), desiredItem)) continue;

            int sourcePlayerSlot = findItemInInventory(shulker, desiredItem, playerSlot, kit);
            if (sourcePlayerSlot != -1) {
                int sourceWindowSlot = sourcePlayerSlot < 9 ? 54 + sourcePlayerSlot : 27 + (sourcePlayerSlot - 9);
                windowClick(shulker.syncId, windowSlot, 0, SlotActionType.PICKUP);
                windowClick(shulker.syncId, sourceWindowSlot, 0, SlotActionType.PICKUP);
                windowClick(shulker.syncId, windowSlot, 0, SlotActionType.PICKUP);
                return true;
            }
        }
        return false;
    }

    private boolean takeNeededItems(ShulkerBoxScreenHandler shulker, Map<Integer, Item> kit) {
        for (Map.Entry<Integer, Item> entry : kit.entrySet()) {
            int playerSlot = entry.getKey();
            if (playerSlot >= 36) continue;

            Item desired = entry.getValue();
            if (desired == Items.AIR) continue;

            int windowSlot = playerSlot < 9 ? 54 + playerSlot : 27 + (playerSlot - 9);
            ItemStack currentStack = shulker.getSlot(windowSlot).getStack();

            if (!currentStack.isEmpty()) {
                if (itemsMatch(currentStack.getItem(), desired)) {
                    if (currentStack.getCount() >= currentStack.getMaxCount()) continue;

                    int shulkerSlot = findStrictMatchingItemInShulker(shulker, currentStack);
                    if (shulkerSlot == -1) continue;

                    windowClick(shulker.syncId, shulkerSlot, 0, SlotActionType.PICKUP);
                    windowClick(shulker.syncId, windowSlot, 0, SlotActionType.PICKUP);
                    if (!shulker.getCursorStack().isEmpty()) {
                        windowClick(shulker.syncId, shulkerSlot, 0, SlotActionType.PICKUP);
                    }
                    return true;
                }
                continue;
            }

            int shulkerSlot = findBestItemInShulker(shulker, desired);
            if (shulkerSlot != -1) {
                windowClick(shulker.syncId, shulkerSlot, 0, SlotActionType.PICKUP);
                windowClick(shulker.syncId, windowSlot, 0, SlotActionType.PICKUP);
                if (!shulker.getCursorStack().isEmpty()) {
                    windowClick(shulker.syncId, shulkerSlot, 0, SlotActionType.PICKUP);
                }
                return true;
            }
        }
        return false;
    }

    // ══════════════════════════════════════════════════════════════
    // 背包查找辅助
    // ══════════════════════════════════════════════════════════════

    private boolean isItemNeededElsewhere(Item item, int currentSlot, Map<Integer, Item> kit) {
        for (Map.Entry<Integer, Item> entry : kit.entrySet()) {
            int slotIndex = entry.getKey();
            if (slotIndex >= 36 || slotIndex == currentSlot) continue;
            Item needed = entry.getValue();
            if (itemsMatch(needed, item)) {
                int windowSlot = slotIndex < 9 ? 54 + slotIndex : 27 + (slotIndex - 9);
                if (mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler shulker) {
                    ItemStack stackInSlot = shulker.getSlot(windowSlot).getStack();
                    if (!itemsMatch(stackInSlot.getItem(), needed)) return true;
                }
            }
        }
        return false;
    }

    private int findItemInInventory(ShulkerBoxScreenHandler shulker, Item item, int ignoreSlot, Map<Integer, Item> kit) {
        for (int i = 0; i < 36; i++) {
            if (i == ignoreSlot) continue;
            int windowSlot = i < 9 ? 54 + i : 27 + (i - 9);
            ItemStack stack = shulker.getSlot(windowSlot).getStack();
            if (itemsMatch(stack.getItem(), item)) {
                Item neededAtSource = kit.getOrDefault(i, Items.AIR);
                if (!itemsMatch(neededAtSource, item)) return i;
            }
        }
        return -1;
    }

    private int findBestItemInShulker(ShulkerBoxScreenHandler shulker, Item desired) {
        int bestSlot = -1;
        int bestCount = -1;
        for (int i = 0; i < 27; i++) {
            ItemStack stack = shulker.getSlot(i).getStack();
            if (itemsMatch(stack.getItem(), desired) && stack.getCount() > bestCount) {
                bestCount = stack.getCount();
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private int findStrictMatchingItemInShulker(ShulkerBoxScreenHandler shulker, ItemStack targetStack) {
        for (int i = 0; i < 27; i++) {
            ItemStack shulkerStack = shulker.getSlot(i).getStack();
            if (strictMatch(shulkerStack.getItem(), targetStack.getItem())
                    && ItemStack.areItemsAndComponentsEqual(shulkerStack, targetStack)) {
                return i;
            }
        }
        return -1;
    }

    private int findEmptyShulkerSlot(ShulkerBoxScreenHandler shulker) {
        for (int i = 0; i < 27; i++) {
            if (shulker.getSlot(i).getStack().isEmpty()) return i;
        }
        return -1;
    }

    // ══════════════════════════════════════════════════════════════
    // 放置逻辑
    // ══════════════════════════════════════════════════════════════

    private boolean canMove(BlockPos pos) {
        return mc.world.isAir(pos) && mc.world.isAir(pos.up());
    }

    private boolean processOpening() {
        if (this.placePos != null) {
            boolean isShulker = mc.world.getBlockState(this.placePos).getBlock() instanceof ShulkerBoxBlock;
            boolean isEChest = mc.world.getBlockState(this.placePos).getBlock() == Blocks.ENDER_CHEST;

            if ((isShulker || (isEChest && enderChest.getValue() && retrievingFromEnderChest)) &&
                    !(mc.world.getBlockState(this.placePos).getBlock() instanceof EnderChestBlock && !retrievingFromEnderChest)) {
                if (mc.world.isAir(this.placePos.up()) || BlockUtil.canReplace(this.placePos.up())) {
                    this.openPos = this.placePos;
                    BlockUtil.clickBlock(this.placePos, BlockUtil.getClickSide(this.placePos), false);
                    return true;
                }
            }
        }

        for (BlockPos pos : BlockUtil.getSphere((float) this.range.getValue())) {
            if (mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock) {
                if (mc.world.isAir(pos.up()) || BlockUtil.canReplace(pos.up())) {
                    this.openPos = pos;
                    this.placePos = pos;
                    BlockUtil.clickBlock(pos, BlockUtil.getClickSide(pos), false);
                    return true;
                }
            }
        }
        return false;
    }

    private BlockPos findRandomPlacePos() {
        BlockPos bestPos = null;
        double bestScore = -1000.0;

        for (BlockPos pos : BlockUtil.getSphere((float) this.range.getValue())) {
            if (!mc.world.getBlockState(pos).isReplaceable()) continue;
            if (!mc.world.getBlockState(pos.down()).isSolidBlock(mc.world, pos.down())) continue;
            if (!mc.world.isAir(pos.up()) && !BlockUtil.canReplace(pos.up())) continue;
            if (!mc.world.getOtherEntities(null, new Box(pos)).isEmpty()) continue;

            double dist = MathHelper.sqrt((float) mc.player.squaredDistanceTo(pos.toCenterPos()));
            if (dist < 1.0) continue;
            if (isPositionBeingMined(pos)) continue;

            int surroundCount = 0;
            if (!mc.world.getBlockState(pos.north()).isReplaceable()) surroundCount++;
            if (!mc.world.getBlockState(pos.south()).isReplaceable()) surroundCount++;
            if (!mc.world.getBlockState(pos.east()).isReplaceable()) surroundCount++;
            if (!mc.world.getBlockState(pos.west()).isReplaceable()) surroundCount++;

            double score = (surroundCount * 0.5) - dist;
            if (bestPos == null || score > bestScore) {
                bestScore = score;
                bestPos = pos;
            }
        }
        return bestPos;
    }

    private void executePlacement(int targetSlot, int oldSlot, BlockPos targetPos) {
        if (this.inventory.getValue()) {
            InventoryUtil.inventorySwap(targetSlot, oldSlot);
            BlockUtil.clickBlock(targetPos.down(), Direction.UP, false);
            InventoryUtil.inventorySwap(targetSlot, oldSlot);
        } else {
            InventoryUtil.switchToSlot(targetSlot);
            BlockUtil.clickBlock(targetPos.down(), Direction.UP, false);
            InventoryUtil.switchToSlot(oldSlot);
        }
    }

    private void doPlace() {
        if (findNearbyShulkerBlock() != null) return;
        if (this.placePos != null && mc.world.getBlockState(this.placePos).getBlock() instanceof ShulkerBoxBlock) return;
        if (!this.timer.passed(placeDelay.getValueInt())) return;

        int shulkerSlot = this.findBestShulker();

        if (shulkerSlot == -1) {
            if (enderChest.getValue()) {
                BlockPos existingEChest = findNearbyEnderChestBlock();
                if (existingEChest != null) {
                    this.retrievingFromEnderChest = true;
                    this.openPos = existingEChest;
                    this.placePos = existingEChest;
                    BlockUtil.clickBlock(existingEChest, BlockUtil.getClickSide(existingEChest), false);
                    this.timer.reset();
                    return;
                }
                int eChestSlot = findEnderChestItem();
                if (eChestSlot != -1) {
                    doPlaceEnderChest(eChestSlot);
                    return;
                }
            }
            return;
        }

        int oldSlot = mc.player.getInventory().selectedSlot;
        BlockPos playerFeet = EntityUtil.getPlayerPos(true);
        PlaceType effectiveType = this.placeType.getValue();
        if (effectiveType != PlaceType.Random && EntityUtil.isInsideBlock()) {
            effectiveType = PlaceType.Random;
        }

        switch (effectiveType) {
            case Burrow -> doPlaceBurrowShulker(shulkerSlot, oldSlot, playerFeet);
            case Tower  -> doPlaceTowerShulker(shulkerSlot, oldSlot, playerFeet);
            case Random -> doPlaceRandomShulker(shulkerSlot, oldSlot);
        }
    }

    private void doPlaceEnderChest(int eChestSlot) {
        int oldSlot = mc.player.getInventory().selectedSlot;
        BlockPos bestPos = findRandomPlacePos();
        if (bestPos != null) {
            this.retrievingFromEnderChest = true;
            executePlacement(eChestSlot, oldSlot, bestPos);
            this.placePos = bestPos;
            this.timer.reset();
        }
    }

    private void doPlaceBurrowShulker(int shulkerSlot, int oldSlot, BlockPos playerFeet) {
        if (!mc.player.isOnGround() || !mc.world.getBlockState(playerFeet).isReplaceable()
                || isPositionBeingMined(playerFeet)) return;

        this.currentTryCount++;
        if (this.currentTryCount > this.placeTryCount.getValueInt()) {
            this.disable();
            return;
        }

        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.4199999868869781, mc.player.getZ(), false));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.7531999805212017, mc.player.getZ(), false));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.9999957640154541, mc.player.getZ(), false));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.1661092609382138, mc.player.getZ(), false));
        executePlacement(shulkerSlot, oldSlot, playerFeet);

        List<BlockPos> list = new ArrayList<>();
        for (double x = mc.player.getPos().getX() - this.smartX.getValue();
             x < mc.player.getPos().getX() + this.smartX.getValue(); x++) {
            for (double z = mc.player.getPos().getZ() - this.smartX.getValue();
                 z < mc.player.getPos().getZ() + this.smartX.getValue(); z++) {
                for (double y = mc.player.getPos().getY() - this.smartDown.getValue();
                     y < mc.player.getPos().getY() + this.smartUp.getValue(); y++) {
                    list.add(new BlockPosX(x, y, z));
                }
            }
        }

        double distance = 0.0;
        BlockPos bestSmartPos = null;
        for (BlockPos pos : list) {
            if (this.canMove(pos)
                    && !(MathHelper.sqrt((float) mc.player.squaredDistanceTo(pos.toCenterPos().add(0.0, -0.5, 0.0))) < this.smartDistance.getValue())
                    && (bestSmartPos == null || mc.player.squaredDistanceTo(pos.toCenterPos()) < distance)) {
                bestSmartPos = pos;
                distance = mc.player.squaredDistanceTo(pos.toCenterPos());
            }
        }

        if (bestSmartPos != null) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    bestSmartPos.getX() + 0.5, bestSmartPos.getY(), bestSmartPos.getZ() + 0.5, false));
        } else {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    mc.player.getX(), mc.player.getY() + 2.3400880035762786, mc.player.getZ(), false));
        }

        this.placePos = playerFeet;
        this.timer.reset();
    }

    private void doPlaceTowerShulker(int shulkerSlot, int oldSlot, BlockPos playerFeet) {
        if (!mc.player.isOnGround() || !mc.world.getBlockState(playerFeet).isReplaceable()
                || !mc.world.getBlockState(playerFeet.up(2)).isReplaceable()
                || isPositionBeingMined(playerFeet)) return;

        this.currentTryCount++;
        if (this.currentTryCount > this.placeTryCount.getValueInt()) {
            this.disable();
            return;
        }

        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.42, mc.player.getZ(), false));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.75, mc.player.getZ(), false));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.0, mc.player.getZ(), false));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.16, mc.player.getZ(), false));
        mc.player.setPosition(mc.player.getX(), mc.player.getY() + 1.0, mc.player.getZ());
        executePlacement(shulkerSlot, oldSlot, playerFeet);

        this.placePos = playerFeet;
        this.timer.reset();
    }

    private void doPlaceRandomShulker(int shulkerSlot, int oldSlot) {
        BlockPos bestPos = findRandomPlacePos();
        if (bestPos != null) {
            executePlacement(shulkerSlot, oldSlot, bestPos);
            this.placePos = bestPos;
            this.timer.reset();
        }
    }

    // ══════════════════════════════════════════════════════════════
    // 枚举 & 内部类
    // ══════════════════════════════════════════════════════════════

    public enum PlaceType {
        Random,
        Burrow,
        Tower
    }

    private static class Pulse {
        public final int slotId;
        public final Timer timer;
        public final long maxTime;

        public Pulse(int slotId) {
            this.slotId = slotId;
            this.timer = new Timer();
            this.timer.reset();
            this.maxTime = 400;
        }
    }
}