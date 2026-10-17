package dev.kissoo.mod.modules.impl.player;

import dev.kissoo.Forever;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.ClickBlockEvent;
import dev.kissoo.api.events.impl.ClientTickEvent;
import dev.kissoo.api.events.impl.PacketEvent;
import dev.kissoo.api.events.impl.RotationEvent;
import dev.kissoo.api.utils.combat.CombatUtil;
import dev.kissoo.api.utils.math.Easing;
import dev.kissoo.api.utils.math.FadeUtils;
import dev.kissoo.api.utils.math.Timer;
import dev.kissoo.api.utils.player.EntityUtil;
import dev.kissoo.api.utils.player.InventoryUtil;
import dev.kissoo.api.utils.render.Render3DUtil;
import dev.kissoo.api.utils.world.BlockUtil;
import hoprc.asm.accessors.IPlayerMoveC2SPacket;
import dev.kissoo.core.impl.RotationManager;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.impl.client.AntiCheat;
import dev.kissoo.mod.modules.impl.client.ClientSetting;
import dev.kissoo.mod.modules.impl.combat.AutoAnchor;
import dev.kissoo.mod.modules.impl.combat.CrystalBot;
import dev.kissoo.mod.modules.impl.combat.Criticals;
import dev.kissoo.mod.modules.impl.exploit.Blink;

import dev.kissoo.mod.modules.impl.movement.ElytraFlight;
import dev.kissoo.mod.modules.impl.movement.Velocity;
import dev.kissoo.mod.modules.settings.enums.SwingSide;
import dev.kissoo.mod.modules.settings.enums.Timing;
import dev.kissoo.mod.modules.settings.impl.*;
import net.minecraft.block.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.text.Text;
import net.minecraft.item.AirBlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
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
    private final BooleanSetting grimDisabler = this.add(new BooleanSetting("GrimDisabler", false, () -> this.page.is(SpeedMine.Page.General)));
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
    int mainSlot = 0;
    int currentPriority = -1;
    private boolean manualTarget = false;

    public SpeedMine() {
        super("SpeedMine", Module.Category.Player);
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
            if (this.shouldYawStep() && this.directionVec != null && !this.sync.passedMs(this.syncTime.getValue())) {
                if (this.shouldUseExclusiveRotate()) {
                    event.setTarget(this.directionVec, this.steps.getValueFloat(), Float.MAX_VALUE);
                } else {
                    float rotationPriority = this.priority.getValueFloat();
                    if (anyEndRotate && this.currentPriority == 2) {
                        rotationPriority = Math.nextUp(rotationPriority);
                    }
                    event.setTarget(this.directionVec, this.steps.getValueFloat(), rotationPriority);
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
        this.manualTarget = false;
        secondPos = null;
        this.secondSide = null;
        this.secondSent = false;
        this.secondStartSent = false;
        this.pendingDoubleRotateAfterMain = false;
    }

    @Override
    public void onDisable() {
        this.startPacket = false;
        ghost = false;
        complete = false;
        this.breakPos = null;
        this.breakSide = null;
        this.manualTarget = false;
        this.secondSide = null;
        this.secondSent = false;
        this.secondStartSent = false;
        this.pendingDoubleRotateAfterMain = false;
    }

    private void autoSwitch() {
        if (this.autoSwitch.getValue() && this.doubleBreak.getValue()) {
            if (this.shouldWaitForMainEndRotate()) {
                return;
            }
            if (this.breakPos != null && !this.startPacket && !this.pendingDoubleRotateAfterMain) return;

            if (secondPos != null && secondPos.equals(this.breakPos)) return;

            // 妫€娴嬫柟鍧楁槸鍚﹁鐮村潖锛屽鏋滄槸鍒欏垏鍥炲師濮嬪伐鍏?
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
                    secondEndAllowed = this.faceVector(secondEndVec, 2) && this.hasServerRotation(secondEndVec);
                    if (!secondEndAllowed) {
                        this.lastSecondRotateTime = System.currentTimeMillis();
                    }
                    // #region debug-point B:auto-switch-end-gate
                    this.debugReport(
                            "B",
                            "SpeedMine.autoSwitch:326",
                            "double-end-gate",
                            "{\"secondPos\":\"" + this.debugPos(secondPos)
                                    + "\",\"secondSent\":" + this.secondSent
                                    + ",\"secondStartSent\":" + this.secondStartSent
                                    + ",\"swapped\":" + this.swapped
                                    + ",\"currentPriority\":" + this.currentPriority
                                    + ",\"directionVec\":\"" + this.debugVec(this.directionVec)
                                    + "\",\"targetVec\":\"" + this.debugVec(secondEndVec)
                                    + "\",\"allowed\":" + secondEndAllowed + "}"
                    );
                    // #endregion

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
                        secondRetryAllowed = this.faceVector(secondRetryVec, 2) && (!this.shouldYawStep() || this.hasServerRotation(secondRetryVec));
                    }
                    if (anyEndRotate && !secondRetryAllowed) {
                        this.lastSecondRotateTime = System.currentTimeMillis();
                        // #region debug-point C:second-switchback-blocked
                        this.debugReport(
                                "C",
                                "SpeedMine.secondSwitchBack:359",
                                "double-end-retry-blocked",
                                "{\"secondPos\":\"" + this.debugPos(secondPos)
                                        + "\",\"swapped\":" + this.swapped
                                        + ",\"currentPriority\":" + this.currentPriority
                                        + ",\"directionVec\":\"" + this.debugVec(this.directionVec)
                                        + "\",\"targetVec\":\"" + this.debugVec(secondRetryVec)
                                        + "\",\"pendingAfterMain\":" + this.pendingDoubleRotateAfterMain + "}"
                        );
                        // #endregion
                    } else {
                        if (anyEndRotate) {
                            Forever.ROTATION.snapAt(secondRetryVec);
                        }

                        sendSequencedPacket(
                                id -> new PlayerActionC2SPacket(
                                        Action.STOP_DESTROY_BLOCK, pendingSecondPos, retryFacing, id
                                )
                        );
                        this.lastEndBreakTime = System.currentTimeMillis();
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
                            Forever.ROTATION.snapBack();
                        }

                        this.pendingDoubleRotateAfterMain = false;
                        this.secondSent = true;
                        // #region debug-point D:second-switchback-success
                        this.debugReport(
                                "D",
                                "SpeedMine.secondSwitchBack:393",
                                "double-end-stop-sent",
                                "{\"secondPos\":\"" + this.debugPos(secondPos)
                                        + "\",\"mainSlot\":" + this.mainSlot
                                        + ",\"swapped\":" + this.swapped
                                        + ",\"directionVec\":\"" + this.debugVec(this.directionVec)
                                        + "\",\"targetVec\":\"" + this.debugVec(secondRetryVec) + "\"}"
                        );
                        // #endregion

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
                        // #region debug-point E:second-pos-reset
                        this.debugReport(
                                "E",
                                "SpeedMine.onTick:440",
                                "second-pos-timeout-reset",
                                "{\"secondPos\":\"" + this.debugPos(secondPos)
                                        + "\",\"secondStartSent\":" + this.secondStartSent
                                        + ",\"secondSent\":" + this.secondSent
                                        + ",\"pendingAfterMain\":" + this.pendingDoubleRotateAfterMain + "}"
                        );
                        // #endregion
                        SpeedMine.setSecondPos(null);
                        this.pendingDoubleRotateAfterMain = false;
                    }
                } else {
                    // #region debug-point E:second-pos-reset
                    this.debugReport(
                            "E",
                            "SpeedMine.onTick:452",
                            "second-pos-invalid-reset",
                            "{\"secondPos\":\"" + this.debugPos(secondPos)
                                    + "\",\"isAir\":" + this.isAir(secondPos)
                                    + ",\"unbreakable\":" + unbreakable(secondPos)
                                    + ",\"secondStartSent\":" + this.secondStartSent
                                    + ",\"secondSent\":" + this.secondSent + "}"
                    );
                    // #endregion
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
                // #region debug-point A:double-start-check
                this.debugReport(
                        "A",
                        "SpeedMine.onTick:468",
                        "double-start-check",
                        "{\"secondPos\":\"" + this.debugPos(secondPos)
                                + "\",\"breakPos\":\"" + this.debugPos(this.breakPos)
                                + "\",\"secondStartSent\":" + this.secondStartSent
                                + ",\"secondSent\":" + this.secondSent
                                + ",\"currentPriority\":" + this.currentPriority
                                + ",\"directionVec\":\"" + this.debugVec(this.directionVec)
                                + "\",\"canStart\":" + canStart
                                + ",\"doubleStartRotate\":" + this.doubleStartRotate.getValue()
                                + ",\"yawStep\":" + this.shouldYawStep() + "}"
                );
                // #endregion

                if (canStart) {
                    this.secondTimer.reset();
                    if (!secondPos.equals(this.breakPos)) {
                        sendSequencedPacket(id -> new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, secondPos, secondFacing, id));
                    }
                    this.secondStartSent = true;
                    if (this.doubleStartRotate.getValue() && !this.shouldYawStep()) {
                        Forever.ROTATION.snapBack();
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
                                                    if (this.breakPos.offset(i).equals(CrystalBot.INSTANCE.crystalPos)) {
                                                        if (CrystalBot.INSTANCE.canPlaceCrystal(this.breakPos)) {
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
                                                                if (anyEndRotate) {
                                                                    Forever.ROTATION.snapAt(this.breakPos.toCenterPos().offset(endFacing, 0.5));
                                                                }

                                                                sendSequencedPacket(
                                                                        id -> new PlayerActionC2SPacket(
                                                                                Action.STOP_DESTROY_BLOCK, this.breakPos, endFacing, id
                                                                        )
                                                                );
                                                                this.lastEndBreakTime = System.currentTimeMillis();
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
                                                                    Forever.ROTATION.snapBack();
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
                                                        Forever.ROTATION.snapBack();
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

    // #region debug-point helper:double-end-rotate
    private void debugReport(String hypothesisId, String location, String msg, String dataJson) {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                String url = "http://127.0.0.1:7777/event";
                String sessionId = "double-end-rotate";
                java.nio.file.Path envPath = java.nio.file.Paths.get(".dbg", "double-end-rotate.env");
                if (java.nio.file.Files.exists(envPath)) {
                    for (String line : java.nio.file.Files.readAllLines(envPath)) {
                        if (line.startsWith("DEBUG_SERVER_URL=")) {
                            url = line.substring("DEBUG_SERVER_URL=".length());
                        } else if (line.startsWith("DEBUG_SESSION_ID=")) {
                            sessionId = line.substring("DEBUG_SESSION_ID=".length());
                        }
                    }
                }

                String body = "{\"sessionId\":\"" + sessionId
                        + "\",\"runId\":\"pre-fix\""
                        + ",\"hypothesisId\":\"" + hypothesisId
                        + "\",\"location\":\"" + location
                        + "\",\"msg\":\"[DEBUG] " + msg
                        + "\",\"data\":" + dataJson + "}";
                java.net.http.HttpClient.newHttpClient().send(
                        java.net.http.HttpRequest.newBuilder(java.net.URI.create(url))
                                .header("Content-Type", "application/json")
                                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                                .build(),
                        java.net.http.HttpResponse.BodyHandlers.discarding()
                );
            } catch (Exception ignored) {
            }
        });
    }

    private String debugPos(BlockPos pos) {
        return pos == null ? "null" : pos.toShortString();
    }

    private String debugVec(Vec3d vec) {
        return vec == null ? "null" : String.format(java.util.Locale.ROOT, "%.3f,%.3f,%.3f", vec.x, vec.y, vec.z);
    }
    // #endregion

    void doDoubleBreak(Direction side) {
        sendSequencedPacket(id -> new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, this.breakPos, side, id));
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
                                        this.manualTarget = true;
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
                                                    Forever.ROTATION.snapBack();
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
                                    this.manualTarget = false;
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

    public boolean shouldPauseForManualTarget() {
        if (!this.manualTarget) {
            return false;
        }
        if (this.breakPos == null
                || unbreakable(this.breakPos)
                || this.isAir(this.breakPos)
                || MathHelper.sqrt((float) mc.player.getEyePos().squaredDistanceTo(this.breakPos.toCenterPos())) > this.range.getValue()) {
            this.manualTarget = false;
            return false;
        }
        return true;
    }

    boolean faceVector(Vec3d directionVec, int priority) {
        if (!this.shouldYawStep()) {
            Forever.ROTATION.lookAt(directionVec);
            return true;
        } else {
            if (this.directionVec == null || priority > this.currentPriority) {
                this.currentPriority = priority;
                this.sync.reset();
                this.directionVec = directionVec;
            } else if (priority == this.currentPriority) {
                this.sync.reset();
                if (this.directionVec != null && !this.directionVec.equals(directionVec)) {
                    this.directionVec = directionVec;
                }
            } else {
                if (this.directionVec != null && !this.directionVec.equals(directionVec)) {
                    return false;
                }
            }
            float requiredFov = this.checkFov.getValue() ? this.fov.getValueFloat() : 2.0f;
            return Forever.ROTATION.inFov(directionVec, requiredFov);
        }
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
        float yawDifferent = MathHelper.angleBetween(angle[0], Forever.ROTATION.getLastYaw());
        float pitchDifferent = Math.abs(angle[1] - Forever.ROTATION.getLastPitch());
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
        float[] targetAngle = RotationManager.getRotation(targetVec);
        float currentYaw = AntiCheat.INSTANCE.serverSide.getValue() ? Forever.ROTATION.getLastYaw() : Forever.ROTATION.rotationYaw;
        float currentPitch = AntiCheat.INSTANCE.serverSide.getValue() ? Forever.ROTATION.getLastPitch() : Forever.ROTATION.rotationPitch;
        float yawDelta = Math.abs(MathHelper.wrapDegrees(targetAngle[0] - currentYaw));
        float pitchDelta = Math.abs(targetAngle[1] - currentPitch);
        float yawPerTick = Math.max(this.steps.getValueFloat() * 180.0F, 0.1F);
        float pitchPerTick = Math.max(this.steps.getValueFloat() * 90.0F, 0.1F);
        int ticksNeeded = (int) Math.ceil(Math.max(yawDelta / yawPerTick, pitchDelta / pitchPerTick));
        long estimatedMs = (long) (ticksNeeded * 50L + 25L);
        return MathHelper.clamp(estimatedMs, 50L, (long) this.syncTime.getValueInt() + 150L);
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
                        Render3DUtil.drawText3D(Text.of("Waiting"), this.breakPos.toCenterPos().x, this.breakPos.toCenterPos().y, this.breakPos.toCenterPos().z, 0.0, 0.0, this.textScale.getValue(), -1);
                    } else if ((int) this.mineTimer.getMs() < this.breakFinalTime) {
                        Render3DUtil.drawText3D(Text.of(this.df.format(progress * 100.0) + "%"), this.breakPos.toCenterPos().x, this.breakPos.toCenterPos().y, this.breakPos.toCenterPos().z, 0.0, 0.0, this.textScale.getValue(), -1);
                    } else {
                        Render3DUtil.drawText3D(Text.of("100.0%"), this.breakPos.toCenterPos().x, this.breakPos.toCenterPos().y, this.breakPos.toCenterPos().z, 0.0, 0.0, this.textScale.getValue(), -1);
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

                    if (this.grimDisabler.getValue()) {
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
        return this.crystal.getValue() && (!this.onlyHeadBomber.getValue() || this.obsidian.isPressed());
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
                && Forever.PLAYER.isInWeb(mc.player)
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
                && Forever.PLAYER.isInWeb(mc.player);
    }

    private boolean shouldWebBypass(BlockPos pos) {
        return this.webBypass.getValue()
                && this.hasMiningTarget(pos)
                && Forever.PLAYER.isInWeb(mc.player);
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
        return (this.whenElytra.getValue() || !mc.player.isFallFlying() && (!ElytraFlight.INSTANCE.isOn() || !ElytraFlight.INSTANCE.isFallFlying())) && this.yawStep.getValue() && !Velocity.INSTANCE.noRotation();
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
}

