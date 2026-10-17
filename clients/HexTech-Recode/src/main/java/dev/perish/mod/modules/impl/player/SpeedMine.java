package dev.perish.mod.modules.impl.player;

import dev.perish.Perish;
import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.ClickBlockEvent;
import dev.perish.api.events.impl.ClientTickEvent;
import dev.perish.api.events.impl.PacketEvent;
import dev.perish.api.events.impl.RotationEvent;
import dev.perish.api.utils.combat.CombatUtil;
import dev.perish.api.utils.math.Easing;
import dev.perish.api.utils.math.FadeUtils;
import dev.perish.api.utils.math.Timer;
import dev.perish.api.utils.player.EntityUtil;
import dev.perish.api.utils.player.InventoryUtil;
import dev.perish.api.utils.render.Render3DUtil;
import dev.perish.api.utils.world.BlockUtil;
import dev.perish.asm.accessors.IPlayerMoveC2SPacket;
import dev.perish.core.impl.RotationManager;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.impl.client.AntiCheat;
import dev.perish.mod.modules.impl.client.ClientSetting;
import dev.perish.mod.modules.impl.combat.AutoAnchor;
import dev.perish.mod.modules.impl.combat.AutoCrystal;
import dev.perish.mod.modules.impl.combat.CevBreaker;
import dev.perish.mod.modules.impl.combat.Criticals;
import dev.perish.mod.modules.impl.exploit.Blink;
import dev.perish.mod.modules.impl.movement.ElytraFly;
import dev.perish.mod.modules.impl.movement.Velocity;
import dev.perish.mod.modules.settings.enums.SwingSide;
import dev.perish.mod.modules.settings.enums.Timing;
import dev.perish.mod.modules.settings.impl.*;
import net.minecraft.block.*;
import net.minecraft.util.Hand;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.AirBlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.math.*;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SpeedMine extends Module {
    public static SpeedMine INSTANCE;
    public static BlockPos secondPos;
    public static double progress = 0.0;
    public static boolean ghost = false;
    public static boolean complete = false;
    private final FadeUtils animationTime = new FadeUtils(1000L);
    private final FadeUtils secondAnim = new FadeUtils(1000L);
    private final DecimalFormat df = new DecimalFormat("0.0");
    private final EnumSetting<SpeedMine.Page> page = this.add(new EnumSetting("Page", SpeedMine.Page.General));
    public final BooleanSetting noGhostHand = this.add(new BooleanSetting("1.21", false, () -> this.page.is(SpeedMine.Page.General)));
    public final BooleanSetting noCollide = this.add(new BooleanSetting("NoCollide", true, () -> this.page.is(SpeedMine.Page.General)));
    public final SliderSetting range = this.add(new SliderSetting("Range", 6.0, 3.0, 10.0, 0.1, () -> this.page.is(SpeedMine.Page.General)));
    public final BooleanSetting crystal = this.add(new BooleanSetting("Crystal", false, () -> this.page.is(SpeedMine.Page.Place)).setParent());
    private final BooleanSetting onlyHeadBomber = this.add(
            new BooleanSetting("OnlyCev", true, () -> this.page.is(SpeedMine.Page.Place) && this.crystal.isOpen())
    );
    private final BooleanSetting waitPlace = this.add(new BooleanSetting("WaitPlace", true, () -> this.page.is(SpeedMine.Page.Place) && this.crystal.isOpen()));
    private final BooleanSetting spamPlace = this.add(new BooleanSetting("SpamPlace", false, () -> this.page.is(SpeedMine.Page.Place) && this.crystal.isOpen()));
    private final BooleanSetting afterBreak = this.add(
            new BooleanSetting("AfterBreak", true, () -> this.page.is(SpeedMine.Page.Place) && this.crystal.isOpen())
    );
    private final BooleanSetting checkDamage = this.add(
            new BooleanSetting("DetectProgress", true, () -> this.page.is(SpeedMine.Page.Place) && this.crystal.isOpen())
    );
    private final SliderSetting crystalDamage = this.add(
            new SliderSetting("Progress", 0.9F, 0.0, 1.0, 0.01, () -> this.page.is(SpeedMine.Page.Place) && this.crystal.isOpen() && this.checkDamage.getValue())
    );
    public final BindSetting obsidian = this.add(new BindSetting("Obsidian", -1, () -> this.page.is(SpeedMine.Page.Place)));
    private final SliderSetting stopDelay = this.add(new SliderSetting("StopDelay", 50.0, 0.0, 500.0, 1.0, () -> this.page.is(SpeedMine.Page.General)));
    private final SliderSetting startDelay = this.add(new SliderSetting("StartDelay", 200.0, 0.0, 500.0, 1.0, () -> this.page.is(SpeedMine.Page.General)));
    private final SliderSetting damage = this.add(new SliderSetting("Damage", 0.7F, 0.0, 2.0, 0.01, () -> this.page.is(SpeedMine.Page.General)));
    private final SliderSetting maxBreak = this.add(new SliderSetting("MaxBreak", 3.0, 0.0, 20.0, 1.0, () -> this.page.is(SpeedMine.Page.General)));
    private final EnumSetting<Timing> timing = this.add(new EnumSetting("Timing", Timing.All, () -> this.page.getValue() == SpeedMine.Page.General));
    private final EnumSetting<SpeedMine.PacketMode> packetMode = this.add(new EnumSetting("PacketMode", SpeedMine.PacketMode.Hyperion, () -> this.page.is(SpeedMine.Page.General)));
    private final BooleanSetting instant = this.add(new BooleanSetting("Instant", false, () -> this.page.is(SpeedMine.Page.General)));
    private final BooleanSetting wait = this.add(new BooleanSetting("Wait", true, () -> !this.instant.getValue() && this.page.is(SpeedMine.Page.General)));
    private final BooleanSetting mineAir = this.add(
            new BooleanSetting("MineAir", true, () -> this.wait.getValue() && !this.instant.getValue() && this.page.is(SpeedMine.Page.General))
    );
    private final BooleanSetting hotBar = this.add(new BooleanSetting("HotbarSwap", false, () -> this.page.is(SpeedMine.Page.General)));
    public final BooleanSetting doubleBreak = this.add(new BooleanSetting("DoubleBreak", true, () -> this.page.is(SpeedMine.Page.General))).setParent();
    public final BooleanSetting autoSwitch = this.add(
            new BooleanSetting("AutoSwitch", true, () -> this.page.is(SpeedMine.Page.General) && this.doubleBreak.isOpen())
    );
    public final BooleanSetting silent = this.add(
            new BooleanSetting("Silent", false, () -> this.page.is(SpeedMine.Page.General) && this.doubleBreak.isOpen() && this.autoSwitch.getValue())
    );
    public final SliderSetting doubleBreakDelay = this.add(
            new SliderSetting("DoubleDelay", 50, 0, 500, () -> this.page.is(SpeedMine.Page.General) && this.doubleBreak.isOpen())
    );
    public final BooleanSetting CCCDisabler = this.add(
            new BooleanSetting("PacketDisabler", false, () -> this.page.is(SpeedMine.Page.General) && this.doubleBreak.isOpen())
    );
    private final SliderSetting start = this.add(
            new SliderSetting("Start", 0.9F, 0.0, 2.0, 0.01, () -> this.page.is(SpeedMine.Page.General) && this.doubleBreak.isOpen())
    );
    private final SliderSetting timeOut = this.add(
            new SliderSetting("TimeOut", 1.2F, 0.0, 2.0, 0.01, () -> this.page.is(SpeedMine.Page.General) && this.doubleBreak.isOpen())
    );
    private final ColorSetting doubleColor = this.add(
            new ColorSetting("DoubleFill", new Color(88, 94, 255, 100), () -> this.doubleBreak.getValue() && this.page.is(SpeedMine.Page.Render))
    );
    private final ColorSetting doubleOutlineColor = this.add(
            new ColorSetting("DoubleOutline", new Color(88, 94, 255, 100), () -> this.doubleBreak.getValue() && this.page.is(SpeedMine.Page.Render))
    );
    private final BooleanSetting setAir = this.add(new BooleanSetting("SetAir", false, () -> this.page.is(SpeedMine.Page.General)));
    private final BooleanSetting swing = this.add(new BooleanSetting("Swing", true, () -> this.page.is(SpeedMine.Page.General)));
    private final BooleanSetting endSwing = this.add(new BooleanSetting("EndSwing", false, () -> this.page.is(SpeedMine.Page.General)));
    private final EnumSetting<SwingSide> swingMode = this.add(new EnumSetting("SwingSide", SwingSide.All, () -> this.page.is(SpeedMine.Page.General)));
    private final BooleanSetting unbreakableCancel = this.add(new BooleanSetting("UnbreakableCancel", true, () -> this.page.is(SpeedMine.Page.Check)));
    private final BooleanSetting switchReset = this.add(new BooleanSetting("SwitchReset", false, () -> this.page.is(SpeedMine.Page.Check)));
    private final BooleanSetting preferWeb = this.add(new BooleanSetting("PreferWeb", true, () -> this.page.is(SpeedMine.Page.Check)));
    private final BooleanSetting preferHead = this.add(new BooleanSetting("PreferHead", true, () -> this.page.is(SpeedMine.Page.Check)));
    private final BooleanSetting farCancel = this.add(new BooleanSetting("FarCancel", false, () -> this.page.is(SpeedMine.Page.Check)));
    private final BooleanSetting onlyGround = this.add(new BooleanSetting("OnlyGround", true, () -> this.page.is(SpeedMine.Page.Check)));
    private final BooleanSetting webBypass = this.add(new BooleanSetting("WebBypass", false, () -> this.page.is(SpeedMine.Page.Check)));
    private final BooleanSetting noWeb = this.add(new BooleanSetting("NoWeb", false, () -> this.page.is(SpeedMine.Page.Check)));
    private final BooleanSetting checkWeb = this.add(new BooleanSetting("CheckWeb", true, () -> this.page.is(SpeedMine.Page.Check)));
    private final BooleanSetting checkGround = this.add(new BooleanSetting("CheckGround", true, () -> this.page.is(SpeedMine.Page.Check)));
    private final BooleanSetting smart = this.add(new BooleanSetting("Smart", true, () -> this.page.is(SpeedMine.Page.Check) && this.checkGround.getValue()));
    private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", false, () -> this.page.is(SpeedMine.Page.Check)).setParent());
    private final BooleanSetting allowOffhand = this.add(
            new BooleanSetting("AllowOffhand", true, () -> this.page.is(SpeedMine.Page.Check) && this.usingPause.isOpen())
    );
    private final BooleanSetting bypassGround = this.add(new BooleanSetting("BypassGround", true, () -> this.page.is(SpeedMine.Page.Check)));
    private final SliderSetting bypassTime = this.add(
            new SliderSetting("BypassTime", 400, 0, 2000, () -> this.bypassGround.getValue() && this.page.is(SpeedMine.Page.Check))
    );
    private final BindSetting pause = this.add(new BindSetting("Pause", -1, () -> this.page.is(SpeedMine.Page.Check)));
    private final BooleanSetting rotate = this.add(new BooleanSetting("StartRotate", true, () -> this.page.is(SpeedMine.Page.Rotation)));
    private final BooleanSetting endRotate = this.add(new BooleanSetting("EndRotate", false, () -> this.page.is(SpeedMine.Page.Rotation)));
    public final BooleanSetting doubleEndRotate = this.add(new BooleanSetting("DoubleEndRotate", false, () -> this.page.is(SpeedMine.Page.Rotation) && this.doubleBreak.getValue()));
    public final BooleanSetting doubleStartRotate = this.add(new BooleanSetting("DoubleStartRotate", false, () -> this.page.is(SpeedMine.Page.Rotation) && this.doubleBreak.getValue()));
    private final SliderSetting syncTime = this.add(new SliderSetting("Sync", 300, 0, 1000, () -> this.page.is(SpeedMine.Page.Rotation)));
    private final BooleanSetting yawStep = this.add(new BooleanSetting("YawStep", false, () -> this.page.is(SpeedMine.Page.Rotation)).setParent());
    private final BooleanSetting whenElytra = this.add(
            new BooleanSetting("FallFlying", true, () -> this.page.is(SpeedMine.Page.Rotation) && this.yawStep.isOpen())
    );
    private final SliderSetting steps = this.add(
            new SliderSetting("Steps", 0.05, 0.0, 1.0, 0.01, () -> this.page.is(SpeedMine.Page.Rotation) && this.yawStep.isOpen())
    );
    private final BooleanSetting checkFov = this.add(
            new BooleanSetting("OnlyLooking", true, () -> this.page.is(SpeedMine.Page.Rotation) && this.yawStep.isOpen())
    );
    private final SliderSetting fov = this.add(
            new SliderSetting("Fov", 20.0, 0.0, 360.0, 0.1, () -> this.page.is(SpeedMine.Page.Rotation) && this.yawStep.isOpen())
    );
    public final SliderSetting priority = this.add(
            new SliderSetting("Priority", 10, 0, 100, () -> this.page.is(SpeedMine.Page.Rotation) && this.yawStep.isOpen())
    );
    private final BindSetting enderChest = this.add(new BindSetting("EnderChest", -1, () -> this.page.is(SpeedMine.Page.Place)));
    private final BooleanSetting placeRotate = this.add(new BooleanSetting("PlaceRotate", true, () -> this.page.is(SpeedMine.Page.Place)));
    private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true, () -> this.page.is(SpeedMine.Page.Place)));
    private final SliderSetting placeDelay = this.add(new SliderSetting("PlaceDelay", 100, 0, 1000, () -> this.page.is(SpeedMine.Page.Place)));
    private final BooleanSetting checkDouble = this.add(new BooleanSetting("CheckDouble", false, () -> this.page.is(SpeedMine.Page.Render)));
    private final EnumSetting<SpeedMine.Animation> animation = this.add(
            new EnumSetting("Animation", SpeedMine.Animation.Up, () -> this.page.is(SpeedMine.Page.Render))
    );
    private final EnumSetting<Easing> ease = this.add(new EnumSetting("Ease", Easing.CubicInOut, () -> this.page.is(SpeedMine.Page.Render)));
    private final EnumSetting<Easing> fadeEase = this.add(new EnumSetting("FadeEase", Easing.CubicInOut, () -> this.page.is(SpeedMine.Page.Render)));
    private final SliderSetting expandLine = this.add(new SliderSetting("ExpandLine", 0.0, 0.0, 1.0, () -> this.page.is(SpeedMine.Page.Render)));
    private final ColorSetting startColor = this.add(new ColorSetting("StartFill", new Color(255, 255, 255, 100), () -> this.page.is(SpeedMine.Page.Render)));
    private final ColorSetting startOutlineColor = this.add(
            new ColorSetting("StartOutline", new Color(255, 255, 255, 100), () -> this.page.is(SpeedMine.Page.Render))
    );
    private final ColorSetting endColor = this.add(new ColorSetting("EndFill", new Color(255, 255, 255, 100), () -> this.page.is(SpeedMine.Page.Render)));
    private final ColorSetting endOutlineColor = this.add(
            new ColorSetting("EndOutline", new Color(255, 255, 255, 100), () -> this.page.is(SpeedMine.Page.Render))
    );
    private final BooleanSetting text = this.add(new BooleanSetting("Text", true, () -> this.page.is(SpeedMine.Page.Render)));
    private final BooleanSetting font = this.add(new BooleanSetting("Font", true, () -> this.page.is(SpeedMine.Page.Render) && this.text.getValue()));
    private final SliderSetting textScale = this.add(new SliderSetting("TextScale", 1.0, 0.1, 5.0, 0.1, () -> this.page.is(SpeedMine.Page.Render) && this.text.getValue()));
    private final BooleanSetting box = this.add(new BooleanSetting("Box", true, () -> this.page.is(SpeedMine.Page.Render)));
    private final BooleanSetting outline = this.add(new BooleanSetting("Outline", true, () -> this.page.is(SpeedMine.Page.Render)));
    private final Timer mineTimer = new Timer();
    private final Timer sync = new Timer();
    private final Timer secondTimer = new Timer();
    private final Timer delayTimer = new Timer();
    private final Timer placeTimer = new Timer();
    private final Timer startTime = new Timer();
    int lastSlot = -1;
    Vec3d directionVec = null;
    Runnable switchBack;
    Runnable secondSwitchBack;
    BlockPos breakPos;
    Direction breakSide;
    Direction secondSide;
    boolean startPacket = false;
    int breakNumber = 0;
    double breakFinalTime;
    double secondFinalTime;
    boolean sendGroundPacket = false;
    boolean swapped = false;
    public boolean secondSent = false;
    public boolean secondStartSent = false;
    public boolean pendingDoubleRotateAfterMain = false;
    public long lastSecondRotateTime = 0;
    public long lastEndBreakTime = 0;
    private long clientTickSequence = 0;
    private long lastEndBreakTick = Long.MIN_VALUE;
    int mainSlot = 0;
    int currentPriority = -1;

    public SpeedMine() {
        super("SpeedMine", Category.Combat);
        this.setChinese("快速挖掘");
        INSTANCE = this;
    }

    public static BlockPos getBreakPos() {
        return INSTANCE.isOn() ? INSTANCE.breakPos : null;
    }

    public static double getBreakTime(BlockPos pos) {
        int slot = INSTANCE.getTool(pos);
        if (slot == -1) {
            slot = mc.player.getInventory().selectedSlot;
        }

        return INSTANCE.getBreakTime(pos, slot);
    }

    public static void setSecondPos(BlockPos pos) {
        secondPos = pos;
        if (INSTANCE != null) {
            INSTANCE.secondStartSent = false;
            INSTANCE.secondSent = false;
            INSTANCE.secondTimer.reset();
            INSTANCE.secondSide = pos != null && pos.equals(INSTANCE.breakPos) ? INSTANCE.breakSide : null;
        }
    }

    public static boolean unbreakable(BlockPos blockPos) {
        Block block = mc.world.getBlockState(blockPos).getBlock();
        return !(block instanceof AirBlock) && (block.getHardness() == -1.0F || block.getHardness() == 100.0F);
    }

    @Override
    public String getInfo() {
        return progress >= 1.0 ? "Done" : this.df.format(progress * 100.0) + "%";
    }

    @EventListener
    public void onRotate(RotationEvent event) {
        boolean anyEndRotate = this.endRotate.getValue() || (this.doubleBreak.getValue() && this.doubleEndRotate.getValue());
        if (this.rotate.getValue() || anyEndRotate || this.doubleStartRotate.getValue()) {
            if (this.shouldYawStep() && this.hasActiveRotationTarget()) {
                float rotationStep = this.getRotationStep();
                if (this.shouldUseExclusiveRotate()) {
                    event.setTargetExclusive(this.directionVec, rotationStep);
                } else {
                    float rotationPriority = this.priority.getValueFloat();
                    if (anyEndRotate && this.currentPriority == 2) {
                        rotationPriority = Math.nextUp(rotationPriority);
                    }
                    event.setTarget(this.directionVec, rotationStep, rotationPriority);
                }
            }
        }
    }

    @Override
    public void onLogin() {
        this.startPacket = false;
        ghost = false;
        complete = false;
        this.breakPos = null;
        this.breakSide = null;
        secondPos = null;
        this.secondSide = null;
        this.secondSent = false;
        this.secondStartSent = false;
        this.pendingDoubleRotateAfterMain = false;
        this.resetRotationState();
        this.resetEndBreakSerial();
    }

    @Override
    public void onDisable() {
        this.startPacket = false;
        ghost = false;
        complete = false;
        this.breakPos = null;
        this.breakSide = null;
        this.secondSide = null;
        this.secondSent = false;
        this.secondStartSent = false;
        this.pendingDoubleRotateAfterMain = false;
        this.resetRotationState();
        this.resetEndBreakSerial();
    }

    private void autoSwitch() {
        if (this.autoSwitch.getValue() && this.doubleBreak.getValue()) {
            if (this.shouldWaitForMainEndRotate()) {
                return;
            }
            if (this.breakPos != null && !this.startPacket && !this.pendingDoubleRotateAfterMain) return;

            if (secondPos != null && secondPos.equals(this.breakPos)) return;

            // 检测方块是否被破坏，如果是则切回原始工具
            if (this.swapped && secondPos != null && this.isAir(secondPos)) {
                if (this.silent.getValue()) {
                    mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(this.mainSlot));
                } else {
                    InventoryUtil.switchToSlot(this.mainSlot);
                }
                this.swapped = false;
                return;
            }

            int index = -1;
            if (secondPos != null && !this.isAir(secondPos)) {
                float CurrentFastest = 1.0F;

                for (int i = 0; i < 9; i++) {
                    ItemStack stack = mc.player.getInventory().getStack(i);
                    if (stack != ItemStack.EMPTY) {
                        float digSpeed = EnchantmentHelper.getLevel(
                                mc.world.getRegistryManager().getWrapperOrThrow(Enchantments.EFFICIENCY.getRegistryRef()).getOptional(Enchantments.EFFICIENCY).get(),
                                stack
                        );
                        float destroySpeed = stack.getMiningSpeedMultiplier(mc.world.getBlockState(secondPos));
                        if (digSpeed + destroySpeed > CurrentFastest) {
                            CurrentFastest = digSpeed + destroySpeed;
                            index = i;
                        }
                    }
                }
            }

            if (index != -1
                    && !mc.options.useKey.isPressed()
                    && !mc.options.attackKey.isPressed()
                    && !mc.player.isUsingItem()
                    && !this.secondSent
                    && this.secondStartSent
                    && this.secondTimer.passedMs(this.getBreakTime(secondPos, index, this.start.getValue()))) {
                if (System.currentTimeMillis() - this.lastEndBreakTime < this.doubleBreakDelay.getValue()) {
                    return;
                }

                Direction secondFacing = this.getSecondFacing();
                Vec3d secondEndVec = secondPos.toCenterPos().offset(secondFacing, 0.5);
                boolean secondEndAllowed = true;
                boolean anyEndRotate = this.endRotate.getValue() || (this.doubleBreak.getValue() && this.doubleEndRotate.getValue());
                if (anyEndRotate && this.shouldYawStep()) {
                    secondEndAllowed = this.faceVector(secondEndVec, this.getSecondEndRotatePriority()) && this.hasServerRotation(secondEndVec);
                    if (!secondEndAllowed) {
                        this.lastSecondRotateTime = System.currentTimeMillis();
                    }

                    // IF we are pendingDoubleRotateAfterMain, we should NOT return here even if not allowed!
                    // We must allow it to fall through so `secondSwitchBack` gets set up and retries continuously!
                    if (!secondEndAllowed && !this.pendingDoubleRotateAfterMain) {
                        return;
                    }
                }

                if (index != mc.player.getInventory().selectedSlot) {
                    this.mainSlot = mc.player.getInventory().selectedSlot;
                    if (this.silent.getValue()) {
                        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(index));
                    } else {
                        InventoryUtil.switchToSlot(index);
                    }
                    this.swapped = true;
                }

                int finalIndex = index;
                BlockPos pendingSecondPos = secondPos;
                this.secondSwitchBack = () -> {
                    if (pendingSecondPos == null || secondPos == null || !pendingSecondPos.equals(secondPos)) {
                        if (this.swapped) {
                            if (this.silent.getValue()) {
                                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(this.mainSlot));
                            } else {
                                InventoryUtil.switchToSlot(this.mainSlot);
                            }
                            this.swapped = false;
                        }
                        this.pendingDoubleRotateAfterMain = false;
                        this.secondSwitchBack = null;
                        return;
                    }

                    Direction retryFacing = this.getCurrentFacing(pendingSecondPos, this.secondSide);
                    if (retryFacing == null) {
                        if (this.swapped) {
                            if (this.silent.getValue()) {
                                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(this.mainSlot));
                            } else {
                                InventoryUtil.switchToSlot(this.mainSlot);
                            }
                            this.swapped = false;
                        }
                        this.pendingDoubleRotateAfterMain = false;
                        this.secondSwitchBack = null;
                        return;
                    }

                    this.secondSide = retryFacing;
                    Vec3d secondRetryVec = pendingSecondPos.toCenterPos().offset(retryFacing, 0.5);
                    boolean secondRetryAllowed = true;
                    if (anyEndRotate) {
                        if (this.pendingDoubleRotateAfterMain) {
                            // Both blocks finished simultaneously: main's switchBack already called snapAt
                            // toward main face this same tick, so hasServerRotation for second face will
                            // always be false right now. Instead of waiting multiple ticks for gradual
                            // yawStep convergence (during which the server may reject the late STOP),
                            // bypass the server-rotation gate and let snapAt below force the rotation
                            // packet immediately before STOP — same as non-yawStep mode does.
                            this.faceVector(secondRetryVec, this.getSecondEndRotatePriority());
                            secondRetryAllowed = true;
                        } else {
                            secondRetryAllowed = this.faceVector(secondRetryVec, this.getSecondEndRotatePriority()) && (!this.shouldYawStep() || this.hasServerRotation(secondRetryVec));
                        }
                    }
                    if (anyEndRotate && !secondRetryAllowed) {
                        this.lastSecondRotateTime = System.currentTimeMillis();
                    } else {
                        // Check at the actual send point. A callback may have been queued before
                        // the other target sent its STOP in the same client tick.
                        if (!this.canSendEndBreak()) {
                            this.lastSecondRotateTime = System.currentTimeMillis();
                            return;
                        }
                        if (anyEndRotate) {
                            Perish.ROTATION.snapAt(secondRetryVec);
                        }

                        sendSequencedPacket(
                                id -> new PlayerActionC2SPacket(
                                        Action.STOP_DESTROY_BLOCK, pendingSecondPos, retryFacing, id
                                )
                        );
                        this.markEndBreakSent();
                        if (this.endSwing.getValue()) {
                            EntityUtil.swingHand(Hand.MAIN_HAND, this.swingMode.getValue());
                        }

                        if (this.swapped) {
                            if (this.silent.getValue()) {
                                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(this.mainSlot));
                            } else {
                                InventoryUtil.switchToSlot(this.mainSlot);
                            }
                            this.swapped = false;
                        }

                        if (anyEndRotate && !this.shouldYawStep()) {
                            Perish.ROTATION.snapBack();
                        }

                        this.pendingDoubleRotateAfterMain = false;
                        this.secondSent = true;

                        // Fix for DoubleEndRotate sometimes failing:
                        // Automatically update currentPriority when ending double break rotate
                        if (anyEndRotate) {
                            this.currentPriority = -1;
                        }
                        this.secondSwitchBack = null;
                    }
                };

                if (!this.noGhostHand.getValue()) {
                    this.secondSwitchBack.run();
                }
            } else if (this.swapped && (index == -1 || this.isAir(secondPos))) {
                if (this.silent.getValue()) {
                    mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(this.mainSlot));
                } else {
                    InventoryUtil.switchToSlot(this.mainSlot);
                }
                this.swapped = false;
            }
        }
    }

    @EventListener
    public void onTick(ClientTickEvent event) {
        if (event.isPre()) {
            this.clientTickSequence++;
            this.currentPriority = -1;
        }
        if (mc.world == null || mc.player == null) return;
        if (!nullCheck()) {
            if (this.noWeb.getValue() && this.isWebPos(this.breakPos)) {
                if (this.breakPos != null && this.breakPos.equals(secondPos)) {
                    SpeedMine.setSecondPos(null);
                }
                this.startPacket = false;
                ghost = false;
                complete = false;
                this.breakNumber = 0;
                this.breakPos = null;
                this.breakSide = null;
                progress = 0.0;
                return;
            }

            if (this.noWeb.getValue() && this.isWebPos(secondPos)) {
                SpeedMine.setSecondPos(null);
            }

            if (this.breakPos != null && mc.world.isAir(this.breakPos)) {
                complete = true;
            }

            if (secondPos != null) {
                int secondSlot = this.getTool(secondPos);
                if (secondSlot == -1) {
                    secondSlot = mc.player.getInventory().selectedSlot;
                }

                this.secondFinalTime = this.getBreakTime(secondPos, secondSlot, 1.0);
                if (!this.isAir(secondPos) && !unbreakable(secondPos)) {
                    double time = this.getBreakTime(secondPos, mc.player.getInventory().selectedSlot, 1.0);
                    if (this.secondTimer.passedMs(time * this.timeOut.getValue()) && System.currentTimeMillis() - this.lastSecondRotateTime > 500) {
                        SpeedMine.setSecondPos(null);
                        this.pendingDoubleRotateAfterMain = false;
                    }
                } else {
                    SpeedMine.setSecondPos(null);
                    this.pendingDoubleRotateAfterMain = false;
                }

                if (this.shouldPrepareSecondEndRotate()) {
                    int rotatePriority = this.getSecondRemaining() < this.getMainRemaining() ? 2 : 1;
                    Vec3d secondEndVec = secondPos.toCenterPos().offset(this.getSecondFacing(), 0.5);
                    if (!this.faceVector(secondEndVec, rotatePriority) || !this.hasServerRotation(secondEndVec)) {
                        this.lastSecondRotateTime = System.currentTimeMillis();
                    }
                }
            }

            if (secondPos != null && !this.secondStartSent) {
                boolean canStart = true;
                Direction secondFacing = this.getSecondFacing();
                if (this.doubleStartRotate.getValue()) {
                    if (this.shouldYawStep()) {
                        canStart = this.faceVector(secondPos.toCenterPos().offset(secondFacing, 0.5), 0);
                        if (!canStart) {
                            this.lastSecondRotateTime = System.currentTimeMillis();
                        }
                    }
                }

                if (canStart) {
                    this.secondTimer.reset();
                    if (!secondPos.equals(this.breakPos)) {
                        sendSequencedPacket(id -> new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, secondPos, secondFacing, id));
                    }
                    this.secondStartSent = true;
                    if (this.doubleStartRotate.getValue() && !this.shouldYawStep()) {
                        Perish.ROTATION.snapBack();
                    }
                }
            }

            if (this.switchBack != null && event.isPre()) {
                this.switchBack.run();
            }

            if (this.secondSwitchBack != null && event.isPre()) {
                this.secondSwitchBack.run();
            }

            if ((!this.timing.is(Timing.Pre) || !event.isPost()) && (!this.timing.is(Timing.Post) || !event.isPre())) {
                if (mc.player.isDead()) {
                    SpeedMine.setSecondPos(null);
                }

                this.autoSwitch();
                if (mc.player.isCreative()) {
                    this.startPacket = false;
                    ghost = false;
                    complete = false;
                    this.breakNumber = 0;
                    this.breakPos = null;
                    this.breakSide = null;
                    progress = 0.0;
                } else if (this.breakPos == null) {
                    this.breakNumber = 0;
                    this.startPacket = false;
                    ghost = false;
                    complete = false;
                    this.breakSide = null;
                    progress = 0.0;
                } else {
                    int slot = this.getTool(this.breakPos);
                    if (slot == -1) {
                        slot = mc.player.getInventory().selectedSlot;
                    }

                    this.breakFinalTime = this.getBreakTime(this.breakPos, slot);
                    progress = this.mineTimer.getMs() / this.breakFinalTime;
                    if (this.isAir(this.breakPos)) {
                        this.breakNumber = 0;
                    }

                    boolean anyEndRotate = this.endRotate.getValue() || (this.doubleBreak.getValue() && this.doubleEndRotate.getValue());
                    int rotatePriority = this.getMainRemaining() <= this.getSecondRemaining() ? 2 : 1;
                    if (this.shouldPrepareMainEndRotate(anyEndRotate)) {
                        this.faceVector(this.breakPos.toCenterPos().offset(this.getMainFacing(), 0.5), rotatePriority);
                    }

                    if ((!(this.breakNumber > this.maxBreak.getValue() - 1.0) || !(this.maxBreak.getValue() > 0.0) || complete)
                            && (this.wait.getValue() || !this.isAir(this.breakPos) || this.instant.getValue())) {
                        if (unbreakable(this.breakPos)) {
                            if (this.unbreakableCancel.getValue()) {
                                this.breakPos = null;
                                this.breakSide = null;
                                this.startPacket = false;
                                ghost = false;
                                complete = false;
                            }

                            this.breakNumber = 0;
                        } else if (MathHelper.sqrt((float) mc.player.getEyePos().squaredDistanceTo(this.breakPos.toCenterPos())) > this.range.getValue()) {
                            if (this.farCancel.getValue()) {
                                this.startPacket = false;
                                ghost = false;
                                complete = false;
                                this.breakNumber = 0;
                                this.breakPos = null;
                                this.breakSide = null;
                            }
                        } else if (!this.usingPause.getValue()
                                || !mc.player.isUsingItem()
                                || this.allowOffhand.getValue() && mc.player.getActiveHand() != Hand.MAIN_HAND) {
                            if (!this.pause.isPressed()) {
                                if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
                                    if (!this.breakPos.equals(AutoAnchor.INSTANCE.currentPos) || !(BlockUtil.getBlock(getBreakPos()) instanceof RespawnAnchorBlock)) {
                                        if (this.hotBar.getValue() || EntityUtil.inInventory()) {
                                            if (this.isAir(this.breakPos)) {
                                                if (this.shouldCrystal()) {
                                                    for (Direction facing : Direction.values()) {
                                                        CombatUtil.attackCrystal(this.breakPos.offset(facing), this.placeRotate.getValue(), true);
                                                    }
                                                }

                                                if (this.placeTimer.passedMs(this.placeDelay.getValue()) && BlockUtil.canPlace(this.breakPos) && mc.currentScreen == null) {
                                                    if (this.enderChest.isPressed()) {
                                                        int eChest = this.findBlock(Blocks.ENDER_CHEST);
                                                        if (eChest != -1) {
                                                            int oldSlot = mc.player.getInventory().selectedSlot;
                                                            this.doSwap(eChest, eChest);
                                                            BlockUtil.placeBlock(this.breakPos, this.placeRotate.getValue(), true);
                                                            this.doSwap(oldSlot, eChest);
                                                            this.placeTimer.reset();
                                                        }
                                                    } else if (this.obsidian.isPressed()) {
                                                        int obsidian = this.findBlock(Blocks.OBSIDIAN);
                                                        if (obsidian != -1) {
                                                            boolean hasCrystal = false;
                                                            if (this.shouldCrystal()) {
                                                                for (Entity entity : BlockUtil.getEntities(new Box(this.breakPos.up()))) {
                                                                    if (entity instanceof EndCrystalEntity) {
                                                                        hasCrystal = true;
                                                                        break;
                                                                    }
                                                                }
                                                            }

                                                            if (!hasCrystal || this.spamPlace.getValue()) {
                                                                int oldSlot = mc.player.getInventory().selectedSlot;
                                                                this.doSwap(obsidian, obsidian);
                                                                BlockUtil.placeBlock(this.breakPos, this.placeRotate.getValue(), true);
                                                                this.doSwap(oldSlot, obsidian);
                                                                this.placeTimer.reset();
                                                            }
                                                        }
                                                    }
                                                }

                                                this.breakNumber = 0;
                                            } else if (this.canPlaceCrystal(this.breakPos.up()) && this.shouldCrystal()) {
                                                if (this.placeTimer.passedMs(this.placeDelay.getValue())) {
                                                    if (this.checkDamage.getValue()) {
                                                        if (this.mineTimer.getMs() / this.breakFinalTime >= this.crystalDamage.getValue() && !this.placeCrystal()) {
                                                            return;
                                                        }
                                                    } else if (!this.placeCrystal()) {
                                                        return;
                                                    }
                                                } else if (this.startPacket) {
                                                    return;
                                                }
                                            }

                                            if (this.waitPlace.getValue()) {
                                                for (Direction i : Direction.values()) {
                                                    if (this.breakPos.offset(i).equals(AutoCrystal.INSTANCE.crystalPos)) {
                                                        if (AutoCrystal.INSTANCE.canPlaceCrystal(this.breakPos, false, false)) {
                                                            return;
                                                        }
                                                        break;
                                                    }
                                                }
                                            }

                                            if (this.delayTimer.passed((long) this.stopDelay.getValue())) {
                                                if (System.currentTimeMillis() - this.lastEndBreakTime < this.doubleBreakDelay.getValue()) {
                                                    return;
                                                }
                                                if (this.startPacket) {
                                                    if (this.isAir(this.breakPos)) {
                                                        return;
                                                    }

                                                    if (this.onlyGround.getValue() && !mc.player.isOnGround()) {
                                                        return;
                                                    }

                                                    if (this.mineTimer.passed((long) this.breakFinalTime)) {
                                                        Direction endFacing = this.getMainFacing();
                                                        if (anyEndRotate
                                                                && this.shouldYawStep()
                                                                && (!this.faceVector(this.breakPos.toCenterPos().offset(endFacing, 0.5), rotatePriority)
                                                                || !this.hasServerRotation(this.breakPos.toCenterPos().offset(endFacing, 0.5)))) {
                                                            return;
                                                        }

                                                        int old = mc.player.getInventory().selectedSlot;
                                                        boolean shouldSwitch;
                                                        if (this.hotBar.getValue()) {
                                                            shouldSwitch = slot != old;
                                                        } else {
                                                            if (slot < 9) {
                                                                slot += 36;
                                                            }

                                                            shouldSwitch = old + 36 != slot;
                                                        }

                                                        if (shouldSwitch) {
                                                            if (this.hotBar.getValue()) {
                                                                InventoryUtil.switchToSlot(slot);
                                                            } else {
                                                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, old, SlotActionType.SWAP, mc.player);
                                                            }
                                                        }

                                                        int finalSlot = slot;
                                                        this.switchBack = () -> {
                                                            if (anyEndRotate
                                                                    && (!this.faceVector(this.breakPos.toCenterPos().offset(endFacing, 0.5), rotatePriority)
                                                                    || !this.hasServerRotation(this.breakPos.toCenterPos().offset(endFacing, 0.5)))) {
                                                                if (shouldSwitch) {
                                                                    if (this.hotBar.getValue()) {
                                                                        InventoryUtil.switchToSlot(old);
                                                                    } else {
                                                                        mc.interactionManager
                                                                                .clickSlot(mc.player.currentScreenHandler.syncId, finalSlot, old, SlotActionType.SWAP, mc.player);
                                                                        EntityUtil.syncInventory();
                                                                    }
                                                                }
                                                            } else {
                                                                // Re-check immediately before the packet so main and second STOP
                                                                // can never be emitted in the same client tick.
                                                                if (!this.canSendEndBreak()) {
                                                                    return;
                                                                }
                                                                if (anyEndRotate) {
                                                                    Perish.ROTATION.snapAt(this.breakPos.toCenterPos().offset(endFacing, 0.5));
                                                                }

                                                                sendSequencedPacket(
                                                                        id -> new PlayerActionC2SPacket(
                                                                                Action.STOP_DESTROY_BLOCK, this.breakPos, endFacing, id
                                                                        )
                                                                );
                                                                this.markEndBreakSent();
                                                                if (this.endSwing.getValue()) {
                                                                    EntityUtil.swingHand(Hand.MAIN_HAND, this.swingMode.getValue());
                                                                }

                                                                if (shouldSwitch) {
                                                                    if (this.hotBar.getValue()) {
                                                                        InventoryUtil.switchToSlot(old);
                                                                    } else {
                                                                        mc.interactionManager
                                                                                .clickSlot(mc.player.currentScreenHandler.syncId, finalSlot, old, SlotActionType.SWAP, mc.player);
                                                                        EntityUtil.syncInventory();
                                                                    }
                                                                }

                                                                this.breakNumber++;
                                                                this.delayTimer.reset();
                                                                this.startTime.reset();
                                                                if (this.afterBreak.getValue() && this.shouldCrystal()) {
                                                                    for (Direction facing : Direction.values()) {
                                                                        CombatUtil.attackCrystal(this.breakPos.offset(facing), this.placeRotate.getValue(), true);
                                                                    }
                                                                }

                                                                if (this.setAir.getValue()) {
                                                                    mc.world.setBlockState(this.breakPos, Blocks.AIR.getDefaultState());
                                                                }

                                                                if (anyEndRotate && !this.shouldYawStep()) {
                                                                    Perish.ROTATION.snapBack();
                                                                }

                                                                this.pendingDoubleRotateAfterMain = this.doubleBreak.getValue()
                                                                        && anyEndRotate
                                                                        && secondPos != null
                                                                        && !secondPos.equals(this.breakPos)
                                                                        && !this.isAir(secondPos)
                                                                        && !this.secondSent;
                                                                // If pendingDoubleRotateAfterMain is true, currentPriority MUST be reset so DoubleEndRotate can kick in
                                                                if (this.pendingDoubleRotateAfterMain) {
                                                                    this.currentPriority = -1;
                                                                }
                                                                ghost = true;
                                                                this.switchBack = null;
                                                            }
                                                        };
                                                        if (!this.noGhostHand.getValue()) {
                                                            this.switchBack.run();
                                                        }
                                                    }
                                                } else {
                                                    if (!this.startTime.passed(this.startDelay.getValueInt())) {
                                                        return;
                                                    }

                                                    if (!this.mineAir.getValue() && this.isAir(this.breakPos)) {
                                                        return;
                                                    }

                                                    Direction side = BlockUtil.getClickSide(this.breakPos);
                                                    this.breakSide = side;
                                                    if (this.rotate.getValue()) {
                                                        Vec3i vec3i = side.getVector();
                                                        if (!this.faceVector(
                                                                this.breakPos.toCenterPos().add(new Vec3d(vec3i.getX() * 0.5, vec3i.getY() * 0.5, vec3i.getZ() * 0.5)), 0
                                                        )) {
                                                            return;
                                                        }
                                                    }

                                                    this.mineTimer.reset();
                                                    this.animationTime.reset();
                                                    if (this.swing.getValue()) {
                                                        EntityUtil.swingHand(Hand.MAIN_HAND, this.swingMode.getValue());
                                                    }

                                                    if (this.doubleBreak.getValue()) {
                                                        if (secondPos == null || this.isAir(secondPos)) {
                                                            double breakTime = this.getBreakTime(this.breakPos, slot, 1.0);
                                                            this.secondAnim.reset();
                                                            this.secondAnim.setLength((long) breakTime);
                                                            SpeedMine.setSecondPos(this.breakPos);
                                                        }

                                                        this.doDoubleBreak(side);
                                                    }

                                                    sendSequencedPacket(id -> new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, this.breakPos, side, id));
                                                    if (this.rotate.getValue() && !this.shouldYawStep()) {
                                                        Perish.ROTATION.snapBack();
                                                    }

                                                    this.startTime.reset();
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        if (this.breakPos.equals(secondPos)) {
                            SpeedMine.setSecondPos(null);
                        }

                        this.startPacket = false;
                        ghost = false;
                        complete = false;
                        this.breakNumber = 0;
                        this.breakPos = null;
                        this.breakSide = null;
                    }
                }
            }
        }
    }

    private void breakBlock(BlockPos breakPos) {
        mc.world.getBlockState(breakPos).getBlock().onBreak(mc.world, breakPos, mc.world.getBlockState(breakPos), mc.player);
    }

    private boolean shouldWaitForMainEndRotate() {
        boolean anyEndRotate = this.endRotate.getValue() || (this.doubleBreak.getValue() && this.doubleEndRotate.getValue());
        if (!anyEndRotate || !this.shouldYawStep() || this.breakPos == null || !this.startPacket || this.isAir(this.breakPos)) {
            return false;
        }
        if (this.switchBack == null) {
            return false;
        }
        if (!this.delayTimer.passed((long) this.stopDelay.getValue()) || !this.mineTimer.passed((long) this.breakFinalTime)) {
            return false;
        }
        if (this.onlyGround.getValue() && !mc.player.isOnGround()) {
            return false;
        }
        return true;
    }

    void doDoubleBreak(Direction side) {
        sendSequencedPacket(id -> new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, this.breakPos, side, id));
        if (this.CCCDisabler.getValue()) {
            for (int i = 0; i < 5; i++) {
                sendSequencedPacket(id -> new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, this.breakPos.up(7891), Direction.DOWN, id));
            }
        }
        sendSequencedPacket(id -> new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, this.breakPos, side, id));
    }

    boolean placeCrystal() {
        int crystal = this.findCrystal();
        if (crystal != -1) {
            int oldSlot = mc.player.getInventory().selectedSlot;
            this.doSwap(crystal, crystal);
            BlockUtil.placeCrystal(this.breakPos.up(), this.placeRotate.getValue());
            this.doSwap(oldSlot, crystal);
            this.placeTimer.reset();
            return !this.waitPlace.getValue();
        } else {
            return true;
        }
    }

    @EventListener
    public void onAttackBlock(ClickBlockEvent event) {
        if (!nullCheck()) {
            if (!mc.player.isCreative()) {
                event.cancel();
                BlockPos pos = event.getPos();
                if (this.noWeb.getValue() && this.isWebPos(pos)) {
                    return;
                }
                if (!pos.equals(this.breakPos)) {
                    if (!unbreakable(pos)) {
                        if (this.breakPos == null || !this.preferWeb.getValue() || BlockUtil.getBlock(this.breakPos) != Blocks.COBWEB) {
                            if (this.breakPos == null
                                    || !this.preferHead.getValue()
                                    || !mc.player.isCrawling()
                                    || !EntityUtil.getPlayerPos(true).up().equals(this.breakPos)) {
                                if (BlockUtil.getClickSideStrict(pos) != null) {
                                    if (!(MathHelper.sqrt((float) mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos())) > this.range.getValue())) {
                                        this.breakPos = pos;
                                        this.breakSide = null;
                                        this.breakNumber = 0;
                                        this.startPacket = false;
                                        ghost = false;
                                        complete = false;
                                        this.mineTimer.reset();
                                        this.animationTime.reset();
                                        if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
                                            Direction side = BlockUtil.getClickSide(this.breakPos);
                                            this.breakSide = side;
                                            if (this.rotate.getValue()) {
                                                Vec3i vec3i = side.getVector();
                                                if (!this.faceVector(this.breakPos.toCenterPos().add(new Vec3d(vec3i.getX() * 0.5, vec3i.getY() * 0.5, vec3i.getZ() * 0.5)), 0)
                                                ) {
                                                    return;
                                                }
                                            }

                                            if (this.startTime.passed(this.startDelay.getValueInt())) {
                                                if (this.swing.getValue()) {
                                                    EntityUtil.swingHand(Hand.MAIN_HAND, this.swingMode.getValue());
                                                }

                                                if (this.doubleBreak.getValue()) {
                                                    if (secondPos == null || this.isAir(secondPos)) {
                                                        int slot = this.getTool(this.breakPos);
                                                        if (slot == -1) {
                                                            slot = mc.player.getInventory().selectedSlot;
                                                        }

                                                        this.secondFinalTime = this.getBreakTime(this.breakPos, slot, 1.0);
                                                        this.secondAnim.reset();
                                                        this.secondAnim.setLength((long) this.secondFinalTime);
                                                        SpeedMine.setSecondPos(this.breakPos);
                                                    }

                                                    this.doDoubleBreak(side);
                                                }

                                                int slot = this.getTool(this.breakPos);
                                                if (slot == -1) {
                                                    slot = mc.player.getInventory().selectedSlot;
                                                }

                                                this.breakFinalTime = this.getBreakTime(this.breakPos, slot);
                                                sendSequencedPacket(id -> new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, this.breakPos, side, id));
                                                if (this.rotate.getValue() && !this.shouldYawStep()) {
                                                    Perish.ROTATION.snapBack();
                                                }

                                                this.startTime.reset();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void mine(BlockPos pos) {
        if (!nullCheck()) {
            if (mc.player.isCreative()) {
                mc.interactionManager.attackBlock(pos, BlockUtil.getClickSide(pos));
            } else if (this.isOff()) {
                mc.interactionManager.attackBlock(pos, BlockUtil.getClickSide(pos));
            } else if (this.noWeb.getValue() && this.isWebPos(pos)) {
                return;
            } else if (!pos.equals(this.breakPos)) {
                if (!unbreakable(pos)) {
                    if (this.breakPos == null || !this.preferWeb.getValue() || BlockUtil.getBlock(this.breakPos) != Blocks.COBWEB) {
                        if (this.breakPos == null
                                || !this.preferHead.getValue()
                                || !mc.player.isCrawling()
                                || !EntityUtil.getPlayerPos(true).up().equals(this.breakPos)) {
                            if (BlockUtil.getClickSideStrict(pos) != null) {
                                if (!(MathHelper.sqrt((float) mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos())) > this.range.getValue())) {
                                    this.breakPos = pos;
                                    this.breakSide = null;
                                    this.breakNumber = 0;
                                    this.startPacket = false;
                                    ghost = false;
                                    complete = false;
                                    this.mineTimer.reset();
                                    this.animationTime.reset();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    boolean faceVector(Vec3d directionVec, int priority) {
        if (!this.shouldYawStep()) {
            Perish.ROTATION.lookAt(directionVec);
            return true;
        }

        boolean sameTarget = this.isSameRotationTarget(this.directionVec, directionVec);
        if (this.directionVec == null || priority > this.currentPriority) {
            this.currentPriority = priority;
            this.directionVec = directionVec;
            this.sync.reset();
        } else if (priority == this.currentPriority) {
            // Equal-priority requests must not keep a different, stale target alive. Returning
            // false also prevents a caller from sending its mining packet while another target
            // owns this tick's rotation slot.
            if (!sameTarget) {
                return false;
            }
            this.sync.reset();
        } else if (!sameTarget) {
            return false;
        }

        return this.hasServerRotation(directionVec);
    }

    private boolean hasActiveRotationTarget() {
        if (this.directionVec == null || this.currentPriority < 0) {
            return false;
        }
        // Sync=0 used to expire immediately because Timer#passedMs(0) is always true. Keep a
        // freshly requested target for at least one normal client tick so RotationEvent sees it.
        long holdMs = Math.max(50L, this.syncTime.getValueInt());
        return !this.sync.passedMs(holdMs);
    }

    private float getRotationStep() {
        float configuredStep = this.steps.getValueFloat();
        if (configuredStep <= 0.0F || this.directionVec == null || this.currentPriority <= 0) {
            return configuredStep;
        }

        double remainingMs = this.getActiveRotationRemaining();
        if (!Double.isFinite(remainingMs)) {
            return configuredStep;
        }
        if (remainingMs <= 0.0) {
            return 1.0F;
        }

        float[] targetAngle = RotationManager.getRotation(this.directionVec);
        float currentYaw = AntiCheat.INSTANCE.serverSide.getValue()
                ? Perish.ROTATION.getLastYaw()
                : Perish.ROTATION.rotationYaw;
        float currentPitch = AntiCheat.INSTANCE.serverSide.getValue()
                ? Perish.ROTATION.getLastPitch()
                : Perish.ROTATION.rotationPitch;
        float yawDelta = Math.abs(MathHelper.wrapDegrees(targetAngle[0] - currentYaw));
        float pitchDelta = Math.abs(targetAngle[1] - currentPitch);
        int ticksLeft = Math.max(1, (int) Math.floor(remainingMs / 50.0));
        float requiredStep = Math.max(yawDelta / (180.0F * ticksLeft), pitchDelta / (90.0F * ticksLeft));

        // Preserve the user's normal turn speed, but recover smoothly if another rotation module
        // consumed part of the preparation window. The small margin avoids arriving one tick late.
        return MathHelper.clamp(Math.max(configuredStep, requiredStep * 1.08F), configuredStep, 1.0F);
    }

    private double getActiveRotationRemaining() {
        boolean mainFirst = this.getMainRemaining() <= this.getSecondRemaining();
        if (this.currentPriority == 2) {
            return mainFirst ? this.getMainRemaining() : this.getSecondRemaining();
        }
        if (this.currentPriority == 1) {
            return mainFirst ? this.getSecondRemaining() : this.getMainRemaining();
        }
        return Double.MAX_VALUE;
    }

    private boolean isSameRotationTarget(Vec3d first, Vec3d second) {
        return first != null && second != null && first.squaredDistanceTo(second) < 1.0E-6;
    }

    private void resetRotationState() {
        this.directionVec = null;
        this.currentPriority = -1;
    }

    private Direction getMainFacing() {
        this.breakSide = this.getCurrentFacing(this.breakPos, this.breakSide);
        return this.breakSide;
    }

    private Direction getSecondFacing() {
        this.secondSide = this.getCurrentFacing(secondPos, this.secondSide);
        return this.secondSide;
    }

    private Direction getCurrentFacing(BlockPos pos, Direction cachedFacing) {
        if (pos == null) {
            return null;
        }
        return cachedFacing != null ? cachedFacing : BlockUtil.getClickSide(pos);
    }

    private boolean hasServerRotation(Vec3d targetVec) {
        float[] angle = RotationManager.getRotation(targetVec);
        float requiredFov = this.checkFov.getValue() ? this.fov.getValueFloat() : 2.0f;
        float yawDifferent = MathHelper.angleBetween(angle[0], Perish.ROTATION.getLastYaw());
        float pitchDifferent = Math.abs(angle[1] - Perish.ROTATION.getLastPitch());
        return yawDifferent * yawDifferent + pitchDifferent * pitchDifferent <= requiredFov * requiredFov;
    }

    private double getMainRemaining() {
        if (this.breakPos == null || !this.startPacket || this.isAir(this.breakPos)) {
            return Double.MAX_VALUE;
        }
        return this.breakFinalTime - this.mineTimer.getMs();
    }

    private double getSecondRemaining() {
        if (secondPos == null || !this.secondStartSent || this.secondSent || this.isAir(secondPos)) {
            return Double.MAX_VALUE;
        }
        int secondSlot = this.getTool(secondPos);
        if (secondSlot == -1) {
            secondSlot = mc.player.getInventory().selectedSlot;
        }
        return this.getBreakTime(secondPos, secondSlot, this.start.getValue()) - this.secondTimer.getMs();
    }

    // Single source of truth for how urgently the second (double-break) target needs the
    // shared rotation slot. Must match the rule used at onTick's shouldPrepareSecondEndRotate
    // call site (getSecondRemaining() < getMainRemaining() ? 2 : 1) so every place that asks
    // for the second target's end-rotate priority agrees. Before this fix, autoSwitch()'s
    // double-end-gate and secondSwitchBack's retry hardcoded priority 2 regardless of urgency,
    // which could out-fight a legitimately more urgent main-break-end rotate for the same
    // tick's rotation slot (see faceVector's tie-break fix above) and cause DoubleEndRotate's
    // STOP_DESTROY_BLOCK to be sent while the player was actually still facing the wrong block.
    private int getSecondEndRotatePriority() {
        return this.getSecondRemaining() < this.getMainRemaining() ? 2 : 1;
    }

    private boolean shouldUseExclusiveRotate() {
        boolean anyEndRotate = this.endRotate.getValue() || (this.doubleBreak.getValue() && this.doubleEndRotate.getValue());
        if (!anyEndRotate) return false;

        boolean mainFirst = this.getMainRemaining() <= this.getSecondRemaining();
        if (this.currentPriority == 2) {
            return mainFirst ? this.isMainEndRotateExclusiveWindow() : this.isSecondEndRotateExclusiveWindow();
        } else if (this.currentPriority == 1) {
            return mainFirst ? this.isSecondEndRotateExclusiveWindow() : this.isMainEndRotateExclusiveWindow();
        }
        return false;
    }

    private boolean shouldPrepareMainEndRotate(boolean anyEndRotate) {
        if (!anyEndRotate || !this.shouldYawStep() || this.breakPos == null || !this.startPacket || this.isAir(this.breakPos)) {
            return false;
        }
        if (!this.delayTimer.passed((long) this.stopDelay.getValue())) {
            return false;
        }
        double remaining = this.breakFinalTime - this.mineTimer.getMs();
        Vec3d targetVec = this.breakPos.toCenterPos().offset(this.getMainFacing(), 0.5);
        return remaining > 0.0 && remaining <= this.getEndRotateLeadMs(targetVec);
    }

    private boolean shouldPrepareSecondEndRotate() {
        boolean anyEndRotate = this.endRotate.getValue() || (this.doubleBreak.getValue() && this.doubleEndRotate.getValue());
        if (!anyEndRotate || !this.shouldYawStep() || secondPos == null || !this.secondStartSent || this.secondSent || this.isAir(secondPos)) {
            return false;
        }
        if (this.breakPos != null && secondPos.equals(this.breakPos)) {
            return false;
        }
        if (this.isMainBreakStillActive() && !this.pendingDoubleRotateAfterMain) {
            return false;
        }
        int secondSlot = this.getTool(secondPos);
        if (secondSlot == -1) {
            secondSlot = mc.player.getInventory().selectedSlot;
        }
        double secondStopTime = this.getBreakTime(secondPos, secondSlot, this.start.getValue());
        double remaining = secondStopTime - this.secondTimer.getMs();
        Vec3d targetVec = secondPos.toCenterPos().offset(this.getSecondFacing(), 0.5);
        return remaining > 0.0 && remaining <= this.getEndRotateLeadMs(targetVec);
    }

    private long getEndRotateLeadMs(Vec3d targetVec) {
        float step = this.steps.getValueFloat();
        if (step <= 0.0F) {
            return 50L;
        }

        float[] targetAngle = RotationManager.getRotation(targetVec);
        float currentYaw = AntiCheat.INSTANCE.serverSide.getValue() ? Perish.ROTATION.getLastYaw() : Perish.ROTATION.rotationYaw;
        float currentPitch = AntiCheat.INSTANCE.serverSide.getValue() ? Perish.ROTATION.getLastPitch() : Perish.ROTATION.rotationPitch;
        float yawDelta = Math.abs(MathHelper.wrapDegrees(targetAngle[0] - currentYaw));
        float pitchDelta = Math.abs(targetAngle[1] - currentPitch);
        float yawPerTick = step * 180.0F;
        float pitchPerTick = step * 90.0F;
        int ticksNeeded = (int) Math.ceil(Math.max(yawDelta / yawPerTick, pitchDelta / pitchPerTick));

        // Do not cap this by Sync. Sync controls how long an unrefreshed target is retained; it is
        // not a turn-time budget. The old cap made low Steps values start rotating too late and
        // forced mining to stall after reaching 100% while the head caught up.
        return Math.max(50L, ticksNeeded * 50L + 75L);
    }

    private boolean isMainEndRotateExclusiveWindow() {
        if (!this.shouldYawStep() || this.breakPos == null || !this.startPacket || this.isAir(this.breakPos)) {
            return false;
        }
        if (!this.delayTimer.passed((long) this.stopDelay.getValue())) {
            return false;
        }
        Vec3d targetVec = this.breakPos.toCenterPos().offset(this.getMainFacing(), 0.5);
        double remaining = this.breakFinalTime - this.mineTimer.getMs();
        return remaining <= this.getEndRotateExclusiveMs(targetVec);
    }

    private boolean isSecondEndRotateExclusiveWindow() {
        boolean anyEndRotate = this.endRotate.getValue() || (this.doubleBreak.getValue() && this.doubleEndRotate.getValue());
        if (!anyEndRotate || !this.shouldYawStep() || secondPos == null || !this.secondStartSent || this.secondSent || this.isAir(secondPos)) {
            return false;
        }
        if (this.breakPos != null && secondPos.equals(this.breakPos)) {
            return false;
        }
        if (this.isMainBreakStillActive() && !this.pendingDoubleRotateAfterMain) {
            return false;
        }
        int secondSlot = this.getTool(secondPos);
        if (secondSlot == -1) {
            secondSlot = mc.player.getInventory().selectedSlot;
        }
        Vec3d targetVec = secondPos.toCenterPos().offset(this.getSecondFacing(), 0.5);
        double remaining = this.getBreakTime(secondPos, secondSlot, this.start.getValue()) - this.secondTimer.getMs();
        return remaining <= this.getEndRotateExclusiveMs(targetVec);
    }

    private long getEndRotateExclusiveMs(Vec3d targetVec) {
        long leadMs = this.getEndRotateLeadMs(targetVec);
        return MathHelper.clamp(leadMs / 2L, 50L, 125L);
    }

    private boolean canSendEndBreak() {
        long minimumGapMs = Math.max(50L, (long) this.doubleBreakDelay.getValue());
        if (this.lastEndBreakTick == this.clientTickSequence) {
            return false;
        }
        return this.lastEndBreakTime == 0L
                || System.currentTimeMillis() - this.lastEndBreakTime >= minimumGapMs;
    }

    private void markEndBreakSent() {
        this.lastEndBreakTime = System.currentTimeMillis();
        this.lastEndBreakTick = this.clientTickSequence;
    }

    private void resetEndBreakSerial() {
        this.lastEndBreakTime = 0L;
        this.lastEndBreakTick = Long.MIN_VALUE;
    }
    private boolean isMainBreakStillActive() {
        return this.breakPos != null && this.startPacket && !this.isAir(this.breakPos);
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (this.breakPos != null && mc.world.isAir(this.breakPos)) {
            complete = true;
        }

        if (!mc.player.isCreative()) {
            if (secondPos != null) {
                if (this.isAir(secondPos)) {
                    SpeedMine.setSecondPos(null);
                    return;
                }

                if (!this.checkDouble.getValue() || !secondPos.equals(this.breakPos)) {
                    this.secondAnim.setLength((long) this.secondFinalTime);
                    double ease = this.secondAnim.ease(this.ease.getValue());
                    if (this.box.getValue()) {
                        Render3DUtil.drawFill(matrixStack, this.getFillBox(secondPos, ease), this.doubleColor.getValue());
                    }

                    if (this.outline.getValue()) {
                        Render3DUtil.drawBox(matrixStack, this.getOutlineBox(secondPos, ease), this.doubleOutlineColor.getValue());
                    }
                }
            }

            if (this.breakPos != null) {
                progress = this.mineTimer.getMs() / this.breakFinalTime;
                this.animationTime.setLength((long) this.breakFinalTime);
                double easex = this.animationTime.ease(this.ease.getValue());
                if (unbreakable(this.breakPos)) {
                    if (this.box.getValue()) {
                        Render3DUtil.drawFill(matrixStack, new Box(this.breakPos), this.startColor.getValue());
                    }

                    if (this.outline.getValue()) {
                        Render3DUtil.drawBox(matrixStack, new Box(this.breakPos), this.startOutlineColor.getValue());
                    }

                    return;
                }

                if (this.box.getValue()) {
                    Render3DUtil.drawFill(
                            matrixStack, this.getFillBox(this.breakPos, easex), this.getColor(this.animationTime.ease(this.fadeEase.getValue()))
                    );
                }

                if (this.outline.getValue()) {
                    Render3DUtil.drawBox(
                            matrixStack, this.getOutlineBox(this.breakPos, easex), this.getOutlineColor(this.animationTime.ease(this.fadeEase.getValue()))
                    );
                }

                if (this.text.getValue()) {
                    if (this.isAir(this.breakPos)) {
                        Render3DUtil.drawText3D("Waiting", this.breakPos.toCenterPos(), this.textScale.getValue(), -1, this.font.getValue());
                    } else if ((int) this.mineTimer.getMs() < this.breakFinalTime) {
                        Render3DUtil.drawText3D(this.df.format(progress * 100.0) + "%", this.breakPos.toCenterPos(), this.textScale.getValue(), -1, this.font.getValue());
                    } else {
                        Render3DUtil.drawText3D("100.0%", this.breakPos.toCenterPos(), this.textScale.getValue(), -1, this.font.getValue());
                    }
                }
            } else {
                progress = 0.0;
            }
        } else {
            progress = 0.0;
        }
    }

    private Box getFillBox(BlockPos pos, double ease) {
        switch (this.animation.getValue()) {
            case Center:
                ease = (1.0 - ease) / 2.0;
                return new Box(pos).shrink(ease, ease, ease).shrink(-ease, -ease, -ease);
            case Grow:
                ease = (1.0 - ease) / 2.0;
                return new Box(pos).shrink(ease, 0.0, ease).shrink(-ease, 0.0, -ease);
            case Up:
                return new Box(
                        pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + ease, pos.getZ() + 1
                );
            case Down:
                return new Box(
                        pos.getX(), pos.getY() + 1 - ease, pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1
                );
            case Oscillation:
                return new Box(pos).shrink(ease, ease, ease).shrink(-ease, -ease, -ease);
            default:
                return new Box(pos);
        }
    }

    private Box getOutlineBox(BlockPos pos, double ease) {
        ease = Math.min(ease + this.expandLine.getValue(), 1.0);
        switch (this.animation.getValue()) {
            case Center:
                ease = (1.0 - ease) / 2.0;
                return new Box(pos).shrink(ease, ease, ease).shrink(-ease, -ease, -ease);
            case Grow:
                ease = (1.0 - ease) / 2.0;
                return new Box(pos).shrink(ease, 0.0, ease).shrink(-ease, 0.0, -ease);
            case Up:
                return new Box(
                        pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + ease, pos.getZ() + 1
                );
            case Down:
                return new Box(
                        pos.getX(), pos.getY() + 1 - ease, pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1
                );
            case Oscillation:
                return new Box(pos).shrink(ease, ease, ease).shrink(-ease, -ease, -ease);
            default:
                return new Box(pos);
        }
    }

    @EventListener(priority = -200)
    public void onPacketSend(PacketEvent.Send event) {
        if (!nullCheck() && !mc.player.isCreative()) {
            if (event.getPacket() instanceof PlayerMoveC2SPacket) {
                boolean bypass = this.shouldWebBypass();
                if (this.bypassGround.getValue() && !mc.player.isFallFlying() && this.bypassTime.getValue() > 0.0) {
                    if (this.breakPos != null && !this.isAir(this.breakPos)
                            && MathHelper.sqrt((float) this.breakPos.toCenterPos().squaredDistanceTo(mc.player.getEyePos())) <= this.range.getValueFloat() + 2.0F) {
                        int slot = this.getTool(this.breakPos);
                        if (slot == -1) {
                            slot = mc.player.getInventory().selectedSlot;
                        }
                        double breakTime = this.getBreakTime(this.breakPos, slot) - this.bypassTime.getValue();
                        if (breakTime <= 0.0 || this.mineTimer.passedMs((long) breakTime)) {
                            bypass = true;
                        }
                    }
                    if (!bypass && secondPos != null && !this.isAir(secondPos)
                            && MathHelper.sqrt((float) secondPos.toCenterPos().squaredDistanceTo(mc.player.getEyePos())) <= this.range.getValueFloat() + 2.0F) {
                        int slot = this.getTool(secondPos);
                        if (slot == -1) {
                            slot = mc.player.getInventory().selectedSlot;
                        }
                        double breakTime = this.getBreakTime(secondPos, slot, 1.0) - this.bypassTime.getValue();
                        if (breakTime <= 0.0 || this.secondTimer.passedMs((long) breakTime)) {
                            bypass = true;
                        }
                    }
                }
                if (bypass) {
                    this.sendGroundPacket = true;
                    ((IPlayerMoveC2SPacket) event.getPacket()).setOnGround(true);
                } else {
                    this.sendGroundPacket = false;
                }
            } else if (event.getPacket() instanceof UpdateSelectedSlotC2SPacket packet) {
                if (packet.getSelectedSlot() != this.lastSlot) {
                    this.lastSlot = packet.getSelectedSlot();
                    if (this.switchReset.getValue()) {
                        this.startPacket = false;
                        ghost = false;
                        complete = false;
                        this.mineTimer.reset();
                        this.animationTime.reset();
                    }
                }
            } else if (event.getPacket() instanceof PlayerActionC2SPacket packetx) {
                if (packetx.getAction() == Action.START_DESTROY_BLOCK) {
                    if (!packetx.getPos().equals(this.breakPos)) {
                        return;
                    }

                    if (this.packetMode.is(SpeedMine.PacketMode.Grim)) {
                        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, packetx.getPos(), packetx.getDirection()));
                    }

                    this.startPacket = true;
                } else if (packetx.getAction() == Action.STOP_DESTROY_BLOCK) {
                    if (!packetx.getPos().equals(this.breakPos)) {
                        return;
                    }

                    if (!this.instant.getValue()) {
                        this.startPacket = false;
                        ghost = false;
                        complete = false;
                    }
                }
            }
        }
    }

    boolean canPlaceCrystal(BlockPos pos) {
        BlockPos obsPos = pos.down();
        BlockPos boost = obsPos.up();
        return (BlockUtil.getBlock(obsPos) == Blocks.BEDROCK || BlockUtil.getBlock(obsPos) == Blocks.OBSIDIAN)
                && BlockUtil.getClickSideStrict(obsPos) != null
                && this.noEntity(boost)
                && this.noEntity(boost.up())
                && (!ClientSetting.INSTANCE.lowVersion.getValue() || mc.world.isAir(boost.up()));
    }

    boolean noEntity(BlockPos pos) {
        for (Entity entity : BlockUtil.getEntities(new Box(pos))) {
            if (!(entity instanceof ItemEntity) && (!(entity instanceof ArmorStandEntity) || !AntiCheat.INSTANCE.ignoreArmorStand.getValue())) {
                return false;
            }
        }

        return true;
    }

    void doSwap(int slot, int inv) {
        if (!this.inventory.getValue()) {
            InventoryUtil.switchToSlot(slot);
        } else {
            InventoryUtil.inventorySwap(inv, mc.player.getInventory().selectedSlot);
        }
    }

    int findCrystal() {
        return this.inventory.getValue() ? InventoryUtil.findItemInventorySlot(Items.END_CRYSTAL) : InventoryUtil.findItem(Items.END_CRYSTAL);
    }

    int findBlock(Block block) {
        return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(block) : InventoryUtil.findBlock(block);
    }

    boolean shouldCrystal() {
        return this.crystal.getValue() && (!this.onlyHeadBomber.getValue() || this.obsidian.isPressed()) || CevBreaker.INSTANCE.isOn();
    }

    double getBreakTime(BlockPos pos, int slot) {
        return this.getBreakTime(pos, slot, this.damage.getValue());
    }

    double getBreakTime(BlockPos pos, int slot, double damage) {
        return 1.0F / this.getBlockStrength(pos, mc.player.getInventory().getStack(slot)) / 20.0F * 1000.0F * damage;
    }

    float getBlockStrength(BlockPos position, ItemStack itemStack) {
        BlockState state = mc.world.getBlockState(position);
        float hardness = state.getHardness(mc.world, position);
        if (hardness < 0.0F) {
            return 0.0F;
        } else {
            float i = state.isToolRequired() && !itemStack.isSuitableFor(state) ? 100.0F : 30.0F;
            return this.getDigSpeed(position, state, itemStack) / hardness / i;
        }
    }

    float getDigSpeed(BlockPos pos, BlockState state, ItemStack itemStack) {
        float digSpeed = this.getDestroySpeed(state, itemStack);
        if (digSpeed > 1.0F) {
            int efficiencyModifier = EnchantmentHelper.getLevel(
                    mc.world.getRegistryManager().getWrapperOrThrow(Enchantments.EFFICIENCY.getRegistryRef()).getOptional(Enchantments.EFFICIENCY).get(), itemStack
            );
            if (efficiencyModifier > 0 && !itemStack.isEmpty()) {
                digSpeed += (float) (StrictMath.pow(efficiencyModifier, 2.0) + 1.0);
            }
        }

        if (mc.player.hasStatusEffect(StatusEffects.HASTE)) {
            digSpeed *= 1.0F + (mc.player.getStatusEffect(StatusEffects.HASTE).getAmplifier() + 1) * 0.2F;
        }

        if (mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            digSpeed *= switch (mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
                case 0 -> 0.3F;
                case 1 -> 0.09F;
                case 2 -> 0.0027F;
                default -> 8.1E-4F;
            };
        }

        if (mc.player.isSubmergedInWater()) {
            digSpeed *= (float) mc.player.getAttributeInstance(EntityAttributes.PLAYER_SUBMERGED_MINING_SPEED).getValue();
        }

        boolean webBypass = this.shouldWebBypass(pos);
        boolean groundBypass = webBypass || this.sendGroundPacket;
        boolean inWeb = this.checkWeb.getValue()
                && !groundBypass
                && Perish.PLAYER.isInWeb(mc.player)
                && this.isWebPos(pos);
        boolean groundedForMining = mc.player.isOnGround() || groundBypass;
        if ((!groundedForMining || inWeb)
                && INSTANCE.checkGround.getValue()
                && (!this.smart.getValue() || Criticals.INSTANCE.mode.is(Criticals.Mode.Ground) && Criticals.INSTANCE.isOn() || mc.player.isFallFlying() || inWeb)) {
            digSpeed /= 5.0F;
        }

        return digSpeed < 0.0F ? 0.0F : digSpeed;
    }

    private boolean shouldWebBypass() {
        return this.webBypass.getValue()
                && this.isMiningActive()
                && Perish.PLAYER.isInWeb(mc.player);
    }

    private boolean shouldWebBypass(BlockPos pos) {
        return this.webBypass.getValue()
                && this.hasMiningTarget(pos)
                && Perish.PLAYER.isInWeb(mc.player);
    }

    private boolean isMiningActive() {
        return this.hasMiningTarget(this.breakPos) || this.hasMiningTarget(secondPos);
    }

    private boolean hasMiningTarget(BlockPos pos) {
        return pos != null && !this.isAir(pos);
    }

    private boolean isWebPos(BlockPos pos) {
        return pos != null && mc.world.getBlockState(pos).getBlock() == Blocks.COBWEB;
    }

    float getDestroySpeed(BlockState state, ItemStack itemStack) {
        float destroySpeed = 1.0F;
        if (itemStack != null && !itemStack.isEmpty()) {
            destroySpeed *= itemStack.getMiningSpeedMultiplier(state);
        }

        return destroySpeed;
    }

    Color getColor(double quad) {
        int sR = this.startColor.getValue().getRed();
        int sG = this.startColor.getValue().getGreen();
        int sB = this.startColor.getValue().getBlue();
        int sA = this.startColor.getValue().getAlpha();
        int eR = this.endColor.getValue().getRed();
        int eG = this.endColor.getValue().getGreen();
        int eB = this.endColor.getValue().getBlue();
        int eA = this.endColor.getValue().getAlpha();
        return new Color((int) (sR + (eR - sR) * quad), (int) (sG + (eG - sG) * quad), (int) (sB + (eB - sB) * quad), (int) (sA + (eA - sA) * quad));
    }

    Color getOutlineColor(double quad) {
        int sR = this.startOutlineColor.getValue().getRed();
        int sG = this.startOutlineColor.getValue().getGreen();
        int sB = this.startOutlineColor.getValue().getBlue();
        int sA = this.startOutlineColor.getValue().getAlpha();
        int eR = this.endOutlineColor.getValue().getRed();
        int eG = this.endOutlineColor.getValue().getGreen();
        int eB = this.endOutlineColor.getValue().getBlue();
        int eA = this.endOutlineColor.getValue().getAlpha();
        return new Color((int) (sR + (eR - sR) * quad), (int) (sG + (eG - sG) * quad), (int) (sB + (eB - sB) * quad), (int) (sA + (eA - sA) * quad));
    }

    int getTool(BlockPos pos) {
        if (this.hotBar.getValue()) {
            int index = -1;
            float CurrentFastest = 1.0F;

            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (stack != ItemStack.EMPTY) {
                    float digSpeed = EnchantmentHelper.getLevel(
                            mc.world.getRegistryManager().getWrapperOrThrow(Enchantments.EFFICIENCY.getRegistryRef()).getOptional(Enchantments.EFFICIENCY).get(),
                            stack
                    );
                    float destroySpeed = stack.getMiningSpeedMultiplier(mc.world.getBlockState(pos));
                    if (digSpeed + destroySpeed > CurrentFastest) {
                        CurrentFastest = digSpeed + destroySpeed;
                        index = i;
                    }
                }
            }

            return index;
        } else {
            AtomicInteger slot = new AtomicInteger();
            slot.set(-1);
            float CurrentFastest = 1.0F;

            for (Map.Entry<Integer, ItemStack> entry : InventoryUtil.getInventoryAndHotbarSlots().entrySet()) {
                if (!(entry.getValue().getItem() instanceof AirBlockItem)) {
                    float digSpeed = EnchantmentHelper.getLevel(
                            mc.world.getRegistryManager().getWrapperOrThrow(Enchantments.EFFICIENCY.getRegistryRef()).getOptional(Enchantments.EFFICIENCY).get(),
                            entry.getValue()
                    );
                    float destroySpeed = entry.getValue().getMiningSpeedMultiplier(mc.world.getBlockState(pos));
                    if (digSpeed + destroySpeed > CurrentFastest) {
                        CurrentFastest = digSpeed + destroySpeed;
                        slot.set(entry.getKey());
                    }
                }
            }

            return slot.get();
        }
    }

    private boolean shouldYawStep() {
        return (this.whenElytra.getValue() || !mc.player.isFallFlying() && (!ElytraFly.INSTANCE.isOn() || !ElytraFly.INSTANCE.isFallFlying())) && this.yawStep.getValue() && !Velocity.INSTANCE.noRotation();
    }

    boolean isAir(BlockPos breakPos) {
        return mc.world.isAir(breakPos) || BlockUtil.getBlock(breakPos) == Blocks.FIRE && BlockUtil.hasCrystal(breakPos) || BlockUtil.getBlock(breakPos) == Blocks.COBWEB;
    }

    private enum Animation {
        Center,
        Grow,
        Up,
        Down,
        Oscillation,
        None
    }

    public enum Page {
        General,
        Check,
        Place,
        Rotation,
        Render
    }

    public enum PacketMode {
        Hyperion,
        Grim
    }
}
