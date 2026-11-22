/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.AirBlock
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockState
 *  net.minecraft.block.Blocks
 *  net.minecraft.block.RespawnAnchorBlock
 *  net.minecraft.client.util.math.MatrixStack
 *  net.minecraft.enchantment.EnchantmentHelper
 *  net.minecraft.enchantment.Enchantments
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.ItemEntity
 *  net.minecraft.entity.attribute.EntityAttributes
 *  net.minecraft.entity.decoration.ArmorStandEntity
 *  net.minecraft.entity.decoration.EndCrystalEntity
 *  net.minecraft.entity.effect.StatusEffects
 *  net.minecraft.entity.player.PlayerEntity
 *  net.minecraft.item.AirBlockItem
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.Items
 *  net.minecraft.network.packet.Packet
 *  net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
 *  net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket$Action
 *  net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
 *  net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
 *  net.minecraft.registry.entry.RegistryEntry
 *  net.minecraft.screen.slot.SlotActionType
 *  net.minecraft.util.Hand
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.Box
 *  net.minecraft.util.math.Direction
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.util.math.Vec3d
 *  net.minecraft.util.math.Vec3i
 *  net.minecraft.world.BlockView
 *  net.minecraft.world.World
 */
package dev.g255.arc.mod.modules.impl.player;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.ClickBlockEvent;
import dev.g255.arc.api.events.impl.ClientTickEvent;
import dev.g255.arc.api.events.impl.PacketEvent;
import dev.g255.arc.api.events.impl.RotationEvent;
import dev.g255.arc.api.utils.combat.CombatUtil;
import dev.g255.arc.api.utils.math.Easing;
import dev.g255.arc.api.utils.math.FadeUtils;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.api.utils.player.EntityUtil;
import dev.g255.arc.api.utils.player.InventoryUtil;
import dev.g255.arc.api.utils.render.Render3DUtil;
import dev.g255.arc.api.utils.world.BlockUtil;
import dev.g255.arc.asm.accessors.IPlayerMoveC2SPacket;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.impl.client.AntiCheat;
import dev.g255.arc.mod.modules.impl.client.ClientSetting;
import dev.g255.arc.mod.modules.impl.combat.AutoAnchor;
import dev.g255.arc.mod.modules.impl.combat.AutoCrystal;
import dev.g255.arc.mod.modules.impl.combat.CevBreaker;
import dev.g255.arc.mod.modules.impl.combat.Criticals;
import dev.g255.arc.mod.modules.impl.exploit.Blink;
import dev.g255.arc.mod.modules.impl.movement.ElytraFly;
import dev.g255.arc.mod.modules.impl.movement.Velocity;
import dev.g255.arc.mod.modules.settings.enums.SwingSide;
import dev.g255.arc.mod.modules.settings.enums.Timing;
import dev.g255.arc.mod.modules.settings.impl.BindSetting;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.ColorSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AirBlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class PacketMine
extends Module {
    public static PacketMine INSTANCE;
    public static BlockPos secondPos;
    public static double progress;
    private final FadeUtils animationTime = new FadeUtils(1000L);
    private final FadeUtils secondAnim = new FadeUtils(1000L);
    private final DecimalFormat df = new DecimalFormat("0.0");
    private final EnumSetting<Page> page = this.add(new EnumSetting<Page>("Page", Page.General));
    private final SliderSetting stopDelay = this.add(new SliderSetting("StopDelay", 50.0, 0.0, 500.0, 1.0, () -> this.page.is(Page.General)));
    private final SliderSetting startDelay = this.add(new SliderSetting("StartDelay", 200.0, 0.0, 500.0, 1.0, () -> this.page.is(Page.General)));
    private final SliderSetting damage = this.add(new SliderSetting("Damage", 0.7f, 0.0, 2.0, 0.01, () -> this.page.is(Page.General)));
    private final SliderSetting maxBreak = this.add(new SliderSetting("MaxBreak", 3.0, 0.0, 20.0, 1.0, () -> this.page.is(Page.General)));
    public final BooleanSetting noGhostHand = this.add(new BooleanSetting("1.21", false, () -> this.page.is(Page.General)));
    public final BooleanSetting noCollide = this.add(new BooleanSetting("NoCollide", true, () -> this.page.is(Page.General)));
    private final EnumSetting<Timing> timing = this.add(new EnumSetting<Timing>("Timing", Timing.All, () -> this.page.getValue() == Page.General));
    private final BooleanSetting grimDisabler = this.add(new BooleanSetting("GrimDisabler", false, () -> this.page.is(Page.General)));
    private final BooleanSetting fastBypass = this.add(new BooleanSetting("FastBypass", false, () -> this.page.is(Page.General)));
    private final BooleanSetting instant = this.add(new BooleanSetting("Instant", false, () -> this.page.is(Page.General)));
    /** Astra 的 Restart：挖穿预计时间到了还没断，就补一组 ABORT + START，避免服务端把进度吞掉 */
    public final BooleanSetting restart = this.add(new BooleanSetting("Restart", false, () -> this.page.is(Page.General)).setParent());
    private final SliderSetting restartDelay = this.add(new SliderSetting("RestartDelay", 250.0, 0.0, 2000.0, 10.0,
            () -> this.page.is(Page.General) && this.restart.isOpen()));
    private final SliderSetting glassDelay = this.add(new SliderSetting("GlassDelay", 150.0, 0.0, 800.0, 10.0, () -> this.page.is(Page.General)));
    private final BooleanSetting wait = this.add(new BooleanSetting("Wait", true, () -> !this.instant.getValue() && this.page.is(Page.General)));
    private final BooleanSetting mineAir = this.add(new BooleanSetting("MineAir", true, () -> this.wait.getValue() && !this.instant.getValue() && this.page.is(Page.General)));
    private final BooleanSetting hotBar = this.add(new BooleanSetting("HotbarSwap", false, () -> this.page.is(Page.General)));
    private final BooleanSetting doubleBreak = this.add(new BooleanSetting("DoubleBreak", true, () -> this.page.is(Page.General))).setParent();
    public final BooleanSetting autoSwitch = this.add(new BooleanSetting("AutoSwitch", true, () -> this.page.is(Page.General) && this.doubleBreak.isOpen()));
    private final SliderSetting start = this.add(new SliderSetting("Start", 0.9f, 0.0, 2.0, 0.01, () -> this.page.is(Page.General) && this.doubleBreak.isOpen()));
    private final SliderSetting timeOut = this.add(new SliderSetting("TimeOut", 1.2f, 0.0, 2.0, 0.01, () -> this.page.is(Page.General) && this.doubleBreak.isOpen()));
    private final BooleanSetting setAir = this.add(new BooleanSetting("SetAir", false, () -> this.page.is(Page.General)));
    private final BooleanSetting swing = this.add(new BooleanSetting("Swing", true, () -> this.page.is(Page.General)));
    private final BooleanSetting endSwing = this.add(new BooleanSetting("EndSwing", false, () -> this.page.is(Page.General)));
    public final SliderSetting range = this.add(new SliderSetting("Range", 6.0, 3.0, 10.0, 0.1, () -> this.page.is(Page.General)));
    private final EnumSetting<SwingSide> swingMode = this.add(new EnumSetting<SwingSide>("SwingSide", SwingSide.All, () -> this.page.is(Page.General)));
    private final BooleanSetting unbreakableCancel = this.add(new BooleanSetting("UnbreakableCancel", true, () -> this.page.is(Page.Check)));
    private final BooleanSetting switchReset = this.add(new BooleanSetting("SwitchReset", false, () -> this.page.is(Page.Check)));
    private final BooleanSetting preferWeb = this.add(new BooleanSetting("PreferWeb", true, () -> this.page.is(Page.Check)));
    private final BooleanSetting preferHead = this.add(new BooleanSetting("PreferHead", true, () -> this.page.is(Page.Check)));
    private final BooleanSetting farCancel = this.add(new BooleanSetting("FarCancel", false, () -> this.page.is(Page.Check)));
    private final BooleanSetting onlyGround = this.add(new BooleanSetting("OnlyGround", true, () -> this.page.is(Page.Check)));
    private final BooleanSetting checkWeb = this.add(new BooleanSetting("CheckWeb", true, () -> this.page.is(Page.Check)));
    private final BooleanSetting checkGround = this.add(new BooleanSetting("CheckGround", true, () -> this.page.is(Page.Check)));
    private final BooleanSetting smart = this.add(new BooleanSetting("Smart", true, () -> this.page.is(Page.Check) && this.checkGround.getValue()));
    private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", false, () -> this.page.is(Page.Check)).setParent());
    private final BooleanSetting allowOffhand = this.add(new BooleanSetting("AllowOffhand", true, () -> this.page.is(Page.Check) && this.usingPause.isOpen()));
    private final BooleanSetting bypassGround = this.add(new BooleanSetting("BypassGround", true, () -> this.page.is(Page.Check)));
    private final SliderSetting bypassTime = this.add(new SliderSetting("BypassTime", 400, 0, 2000, () -> this.bypassGround.getValue() && this.page.is(Page.Check)));
    private final BindSetting pause = this.add(new BindSetting("Pause", -1, () -> this.page.is(Page.Check)));
    private final BooleanSetting rotate = this.add(new BooleanSetting("StartRotate", true, () -> this.page.is(Page.Rotation)));
    private final BooleanSetting endRotate = this.add(new BooleanSetting("EndRotate", false, () -> this.page.is(Page.Rotation)));
    private final SliderSetting syncTime = this.add(new SliderSetting("Sync", 300, 0, 1000, () -> this.page.is(Page.Rotation)));
    private final BooleanSetting yawStep = this.add(new BooleanSetting("YawStep", false, () -> this.page.is(Page.Rotation)).setParent());
    private final BooleanSetting whenElytra = this.add(new BooleanSetting("FallFlying", true, () -> this.page.is(Page.Rotation) && this.yawStep.isOpen()));
    private final SliderSetting steps = this.add(new SliderSetting("Steps", 0.05, 0.0, 1.0, 0.01, () -> this.page.is(Page.Rotation) && this.yawStep.isOpen()));
    private final BooleanSetting checkFov = this.add(new BooleanSetting("OnlyLooking", true, () -> this.page.is(Page.Rotation) && this.yawStep.isOpen()));
    private final SliderSetting fov = this.add(new SliderSetting("Fov", 20.0, 0.0, 360.0, 0.1, () -> this.page.is(Page.Rotation) && this.yawStep.isOpen()));
    private final SliderSetting priority = this.add(new SliderSetting("Priority", 10, 0, 100, () -> this.page.is(Page.Rotation) && this.yawStep.isOpen()));
    public final BooleanSetting crystal = this.add(new BooleanSetting("Crystal", false, () -> this.page.is(Page.Place)).setParent());
    private final BooleanSetting onlyHeadBomber = this.add(new BooleanSetting("OnlyCev", true, () -> this.page.is(Page.Place) && this.crystal.isOpen()));
    private final BooleanSetting waitPlace = this.add(new BooleanSetting("WaitPlace", true, () -> this.page.is(Page.Place) && this.crystal.isOpen()));
    private final BooleanSetting spamPlace = this.add(new BooleanSetting("SpamPlace", false, () -> this.page.is(Page.Place) && this.crystal.isOpen()));
    private final BooleanSetting afterBreak = this.add(new BooleanSetting("AfterBreak", true, () -> this.page.is(Page.Place) && this.crystal.isOpen()));
    private final BooleanSetting checkDamage = this.add(new BooleanSetting("DetectProgress", true, () -> this.page.is(Page.Place) && this.crystal.isOpen()));
    private final SliderSetting crystalDamage = this.add(new SliderSetting("Progress", 0.9f, 0.0, 1.0, 0.01, () -> this.page.is(Page.Place) && this.crystal.isOpen() && this.checkDamage.getValue()));
    public final BindSetting obsidian = this.add(new BindSetting("Obsidian", -1, () -> this.page.is(Page.Place)));
    private final BindSetting enderChest = this.add(new BindSetting("EnderChest", -1, () -> this.page.is(Page.Place)));
    private final BooleanSetting placeRotate = this.add(new BooleanSetting("PlaceRotate", true, () -> this.page.is(Page.Place)));
    private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true, () -> this.page.is(Page.Place)));
    private final SliderSetting placeDelay = this.add(new SliderSetting("PlaceDelay", 100, 0, 1000, () -> this.page.is(Page.Place)));
    private final BooleanSetting checkDouble = this.add(new BooleanSetting("CheckDouble", false, () -> this.page.is(Page.Render)));
    private final EnumSetting<Animation> animation = this.add(new EnumSetting<Animation>("Animation", Animation.Up, () -> this.page.is(Page.Render)));
    private final EnumSetting<Easing> ease = this.add(new EnumSetting<Easing>("Ease", Easing.CubicInOut, () -> this.page.is(Page.Render)));
    private final EnumSetting<Easing> fadeEase = this.add(new EnumSetting<Easing>("FadeEase", Easing.CubicInOut, () -> this.page.is(Page.Render)));
    private final SliderSetting expandLine = this.add(new SliderSetting("ExpandLine", 0.0, 0.0, 1.0, () -> this.page.is(Page.Render)));
    private final ColorSetting startColor = this.add(new ColorSetting("StartFill", new Color(255, 255, 255, 100), () -> this.page.is(Page.Render)));
    private final ColorSetting startOutlineColor = this.add(new ColorSetting("StartOutline", new Color(255, 255, 255, 100), () -> this.page.is(Page.Render)));
    private final ColorSetting endColor = this.add(new ColorSetting("EndFill", new Color(255, 255, 255, 100), () -> this.page.is(Page.Render)));
    private final ColorSetting endOutlineColor = this.add(new ColorSetting("EndOutline", new Color(255, 255, 255, 100), () -> this.page.is(Page.Render)));
    private final ColorSetting doubleColor = this.add(new ColorSetting("DoubleFill", new Color(88, 94, 255, 100), () -> this.doubleBreak.getValue() && this.page.is(Page.Render)));
    private final ColorSetting doubleOutlineColor = this.add(new ColorSetting("DoubleOutline", new Color(88, 94, 255, 100), () -> this.doubleBreak.getValue() && this.page.is(Page.Render)));
    private final BooleanSetting text = this.add(new BooleanSetting("Text", true, () -> this.page.is(Page.Render)));
    private final BooleanSetting box = this.add(new BooleanSetting("Box", true, () -> this.page.is(Page.Render)));
    private final BooleanSetting outline = this.add(new BooleanSetting("Outline", true, () -> this.page.is(Page.Render)));
    private final Timer mineTimer = new Timer();
    private final Timer sync = new Timer();
    private final Timer secondTimer = new Timer();
    private final Timer delayTimer = new Timer();
    private final Timer placeTimer = new Timer();
    private final Timer startTime = new Timer();
    /** 玻璃的额外延迟：玻璃挖穿时间极短，服务端容易判「提前破坏」，Astra 靠这个间隔兜住 */
    private final Timer glassDelayTimer = new Timer();
    /** Restart 用的「这一格是不是已经补发过一次」跟踪位 */
    private BlockPos restartTrackPos;
    public static boolean ghost;
    public static boolean complete;
    int lastSlot = -1;
    Vec3d directionVec = null;
    Runnable switchBack;
    BlockPos breakPos;
    boolean startPacket = false;
    int breakNumber = 0;
    double breakFinalTime;
    double secondFinalTime;
    boolean sendGroundPacket = false;
    boolean swapped = false;
    int mainSlot = 0;

    public PacketMine() {
        super("PacketMine", Module.Category.Player);
        this.setChinese("\u53d1\u5305\u6316\u6398");
        INSTANCE = this;
    }

    /** 当前挖掘进度 0~1（AutoAnchor 之类的模块要判断「这格快挖穿了」） */
    public double getProgress() {
        return progress;
    }

    public boolean isInstantBreak() {
        return this.instant.getValue();
    }

    public static BlockPos getBreakPos() {
        if (INSTANCE.isOn()) {
            return PacketMine.INSTANCE.breakPos;
        }
        return null;
    }

    @Override
    public String getInfo() {
        if (progress >= 1.0) {
            return "Done";
        }
        return this.df.format(progress * 100.0) + "%";
    }

    @EventListener
    public void onRotate(RotationEvent event) {
        if (this.rotate.getValue() && this.shouldYawStep() && this.directionVec != null && !this.sync.passedMs(this.syncTime.getValue())) {
            event.setTarget(this.directionVec, this.steps.getValueFloat(), this.priority.getValueFloat());
        }
    }

    @Override
    public void onLogin() {
        this.startPacket = false;
        ghost = false;
        complete = false;
        this.breakPos = null;
        secondPos = null;
    }

    @Override
    public void onDisable() {
        this.startPacket = false;
        ghost = false;
        complete = false;
        this.breakPos = null;
    }

    /**
     * Astra 的 Restart 机制：到了预计挖穿时间还没断，就补一组 ABORT + START 重开这一格。
     * 服务端有时收不到 STOP、或者把进度算丢，靠这个把挖掘重新拉起来，
     * 否则会出现「一直挖、方块就是不掉」。
     */
    private void verifyBreakProgress(ClientTickEvent event) {
        if (!event.isPre() || this.instant.getValue() || !this.restart.getValue()) {
            return;
        }
        if (this.breakPos == null || !this.startPacket) {
            this.restartTrackPos = null;
            return;
        }
        if (this.isAir(this.breakPos)) {
            this.restartTrackPos = null;
            return;
        }
        if (this.restartTrackPos == null || !this.restartTrackPos.equals(this.breakPos)) {
            this.restartTrackPos = this.breakPos;
            this.mineTimer.reset();
            return;
        }
        double expectedMs = this.breakFinalTime + this.restartDelay.getValue();
        if (!this.mineTimer.passedMs(expectedMs)) {
            return;
        }
        BlockPos target = this.breakPos;
        Direction side = BlockUtil.getClickSide(target);
        if (side == null) {
            return;
        }
        PacketMine.sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, target, side, id));
        PacketMine.sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, target, side, id));
        ++this.breakNumber;
        this.startTime.reset();
        this.mineTimer.reset();
    }

    /** Astra 的 FastBypass：在头顶高空随便打三下，冲淡「短时间只破坏一个方块」的特征 */
    private void sendFastBypassPackets() {
        if (this.breakPos == null) {
            return;
        }
        BlockPos highPos = new BlockPos(this.breakPos.getX(), 400, this.breakPos.getZ());
        for (int i = 0; i < 3; ++i) {
            int heightOffset = i * 10;
            Direction[] directions = Direction.values();
            Direction randomDir = directions[(int)(Math.random() * (double)directions.length)];
            PacketMine.sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, highPos.up(heightOffset), randomDir, id));
        }
    }

    /** 玻璃类方块：挖穿时间太短，服务端容易判提前破坏，需要额外的 GlassDelay 间隔 */
    private static boolean isGlass(Block block) {
        return block == Blocks.GLASS || block == Blocks.GLASS_PANE
                || block instanceof net.minecraft.block.StainedGlassBlock
                || block instanceof net.minecraft.block.TintedGlassBlock;
    }

    private void autoSwitch() {
        if (this.autoSwitch.getValue() && this.doubleBreak.getValue()) {
            int index = -1;
            if (secondPos != null) {
                float CurrentFastest = 1.0f;
                for (int i = 0; i < 9; ++i) {
                    float destroySpeed;
                    float digSpeed;
                    ItemStack stack = PacketMine.mc.player.getInventory().getStack(i);
                    if (stack == ItemStack.EMPTY || !((digSpeed = (float)EnchantmentHelper.getLevel((RegistryEntry)((RegistryEntry)PacketMine.mc.world.getRegistryManager().get(Enchantments.EFFICIENCY.getRegistryRef()).getEntry(Enchantments.EFFICIENCY).get()), (ItemStack)stack)) + (destroySpeed = stack.getMiningSpeedMultiplier(PacketMine.mc.world.getBlockState(secondPos))) > CurrentFastest)) continue;
                    CurrentFastest = digSpeed + destroySpeed;
                    index = i;
                }
            }
            if (index == -1 || PacketMine.mc.options.useKey.isPressed() || PacketMine.mc.options.attackKey.isPressed() || PacketMine.mc.player.isUsingItem() || !this.secondTimer.passedMs(this.getBreakTime(secondPos, index, this.start.getValue()))) {
                if (this.swapped) {
                    InventoryUtil.switchToSlot(this.mainSlot);
                    this.swapped = false;
                }
            } else if (index != PacketMine.mc.player.getInventory().selectedSlot) {
                this.mainSlot = PacketMine.mc.player.getInventory().selectedSlot;
                InventoryUtil.switchToSlot(index);
                this.swapped = true;
            }
        }
    }

    @EventListener
    public void onTick(ClientTickEvent event) {
        if (PacketMine.nullCheck()) {
            return;
        }
        // Astra 的 Restart：到点还没断就补一组 ABORT + START，别让服务端把挖掘进度吞掉
        this.verifyBreakProgress(event);
        if (this.breakPos != null && PacketMine.mc.world.isAir(this.breakPos)) {
            complete = true;
        }
        if (secondPos != null) {
            int secondSlot = this.getTool(secondPos);
            if (secondSlot == -1) {
                secondSlot = PacketMine.mc.player.getInventory().selectedSlot;
            }
            this.secondFinalTime = this.getBreakTime(secondPos, secondSlot, 1.0);
            if (this.isAir(secondPos) || PacketMine.unbreakable(secondPos)) {
                secondPos = null;
            } else {
                double time = this.getBreakTime(secondPos, PacketMine.mc.player.getInventory().selectedSlot, 1.0);
                if (this.secondTimer.passedMs(time * this.timeOut.getValue())) {
                    secondPos = null;
                }
            }
        }
        if (this.switchBack != null && event.isPre()) {
            this.switchBack.run();
            this.switchBack = null;
        }
        if (this.timing.is(Timing.Pre) && event.isPost() || this.timing.is(Timing.Post) && event.isPre()) {
            return;
        }
        if (PacketMine.mc.player.isDead()) {
            secondPos = null;
        }
        this.autoSwitch();
        if (PacketMine.mc.player.isCreative()) {
            this.startPacket = false;
            ghost = false;
            complete = false;
            this.breakNumber = 0;
            this.breakPos = null;
            progress = 0.0;
            return;
        }
        if (this.breakPos == null) {
            this.breakNumber = 0;
            this.startPacket = false;
            ghost = false;
            complete = false;
            progress = 0.0;
            return;
        }
        // Astra 的位置：紧跟「没有目标」之后、算工具之前。
        // 重生锚作战期间（AutoAnchor 开、下界、背包有锚有荧石）任何锚都不挖，直接把这一格丢掉。
        // 以前只护 AutoAnchor.currentPos 那一格，锚一塞成功 currentPos 就走了，包挖回来就把它敲掉。
        if (BlockUtil.getBlock(this.breakPos) == Blocks.RESPAWN_ANCHOR && AutoAnchor.shouldReserveAnchors()) {
            this.breakNumber = 0;
            this.startPacket = false;
            ghost = false;
            complete = false;
            progress = 0.0;
            this.breakPos = null;
            secondPos = null;
            return;
        }
        int slot = this.getTool(this.breakPos);
        if (slot == -1) {
            slot = PacketMine.mc.player.getInventory().selectedSlot;
        }
        this.breakFinalTime = this.getBreakTime(this.breakPos, slot);
        progress = (double)this.mineTimer.getMs() / this.breakFinalTime;
        if (this.isAir(this.breakPos)) {
            this.breakNumber = 0;
        }
        if ((double)this.breakNumber > this.maxBreak.getValue() - 1.0 && this.maxBreak.getValue() > 0.0 && !complete || !this.wait.getValue() && this.isAir(this.breakPos) && !this.instant.getValue()) {
            if (this.breakPos.equals((Object)secondPos)) {
                secondPos = null;
            }
            this.startPacket = false;
            ghost = false;
            complete = false;
            this.breakNumber = 0;
            this.breakPos = null;
            return;
        }
        if (PacketMine.unbreakable(this.breakPos)) {
            if (this.unbreakableCancel.getValue()) {
                this.breakPos = null;
                this.startPacket = false;
                ghost = false;
                complete = false;
            }
            this.breakNumber = 0;
            return;
        }
        if ((double)MathHelper.sqrt((float)((float)PacketMine.mc.player.getEyePos().squaredDistanceTo(this.breakPos.toCenterPos()))) > this.range.getValue()) {
            if (this.farCancel.getValue()) {
                this.startPacket = false;
                ghost = false;
                complete = false;
                this.breakNumber = 0;
                this.breakPos = null;
            }
            return;
        }
        if (this.usingPause.getValue() && PacketMine.mc.player.isUsingItem() && (!this.allowOffhand.getValue() || PacketMine.mc.player.getActiveHand() == Hand.MAIN_HAND)) {
            return;
        }
        if (this.pause.isPressed()) {
            return;
        }
        if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
            return;
        }
        // 备用保护（主世界/末地、或背包暂时没有锚）：只护住自己刚塞下去、正在等引爆的那一格。
        // Astra 没有这一条，留着是为了主世界塞锚时也不至于自己把锚敲了。
        if (this.breakPos.equals((Object)AutoAnchor.INSTANCE.currentPos) && BlockUtil.getBlock(PacketMine.getBreakPos()) instanceof RespawnAnchorBlock) {
            return;
        }
        if (!this.hotBar.getValue() && !EntityUtil.inInventory()) {
            return;
        }
        if (this.isAir(this.breakPos)) {
            if (this.shouldCrystal()) {
                for (Direction facing : Direction.values()) {
                    CombatUtil.attackCrystal(this.breakPos.offset(facing), this.placeRotate.getValue(), true);
                }
            }
            if (this.placeTimer.passedMs(this.placeDelay.getValue()) && BlockUtil.canPlace(this.breakPos) && PacketMine.mc.currentScreen == null) {
                int obsidian;
                if (this.enderChest.isPressed()) {
                    int eChest = this.findBlock(Blocks.ENDER_CHEST);
                    if (eChest != -1) {
                        int oldSlot = PacketMine.mc.player.getInventory().selectedSlot;
                        this.doSwap(eChest, eChest);
                        BlockUtil.placeBlock(this.breakPos, this.placeRotate.getValue(), true);
                        this.doSwap(oldSlot, eChest);
                        this.placeTimer.reset();
                    }
                } else if (this.obsidian.isPressed() && (obsidian = this.findBlock(Blocks.OBSIDIAN)) != -1) {
                    int hasCrystal = 0;
                    if (this.shouldCrystal()) {
                        for (Entity entity : BlockUtil.getEntities(new Box(this.breakPos.up()))) {
                            if (!(entity instanceof EndCrystalEntity)) continue;
                            hasCrystal = 1;
                            break;
                        }
                    }
                    if (hasCrystal == 0 || this.spamPlace.getValue()) {
                        int oldSlot = PacketMine.mc.player.getInventory().selectedSlot;
                        this.doSwap(obsidian, obsidian);
                        BlockUtil.placeBlock(this.breakPos, this.placeRotate.getValue(), true);
                        this.doSwap(oldSlot, obsidian);
                        this.placeTimer.reset();
                    }
                }
            }
            this.breakNumber = 0;
        } else if (this.canPlaceCrystal(this.breakPos.up()) && this.shouldCrystal() && (this.placeTimer.passedMs(this.placeDelay.getValue()) ? (this.checkDamage.getValue() ? (double)this.mineTimer.getMs() / this.breakFinalTime >= this.crystalDamage.getValue() && !this.placeCrystal() : !this.placeCrystal()) : this.startPacket)) {
            return;
        }
        if (this.waitPlace.getValue()) {
            for (Direction i : Direction.values()) {
                if (!this.breakPos.offset(i).equals((Object)AutoCrystal.INSTANCE.crystalPos)) continue;
                if (!AutoCrystal.INSTANCE.canPlaceCrystal(this.breakPos, false, false)) break;
                return;
            }
        }
        if (!this.delayTimer.passed((long)this.stopDelay.getValue())) {
            return;
        }
        if (this.startPacket) {
            if (this.isAir(this.breakPos)) {
                return;
            }
            // 玻璃挖得太快，服务端会判提前破坏；Astra 用 GlassDelay 卡住节奏
            if (isGlass(BlockUtil.getBlock(this.breakPos)) && !this.glassDelayTimer.passedMs(this.glassDelay.getValue())) {
                return;
            }
            if (this.onlyGround.getValue() && !PacketMine.mc.player.isOnGround()) {
                return;
            }
            if (this.mineTimer.passed((long)this.breakFinalTime)) {
                boolean shouldSwitch;
                if (this.endRotate.getValue() && this.shouldYawStep() && !this.faceVector(this.breakPos.toCenterPos().offset(BlockUtil.getClickSide(this.breakPos), 0.5))) {
                    return;
                }
                int old = PacketMine.mc.player.getInventory().selectedSlot;
                if (this.hotBar.getValue()) {
                    shouldSwitch = slot != old;
                } else {
                    if (slot < 9) {
                        slot += 36;
                    }
                    boolean bl = shouldSwitch = old + 36 != slot;
                }
                if (shouldSwitch) {
                    if (this.hotBar.getValue()) {
                        InventoryUtil.switchToSlot(slot);
                    } else {
                        PacketMine.mc.interactionManager.clickSlot(PacketMine.mc.player.currentScreenHandler.syncId, slot, old, SlotActionType.SWAP, (PlayerEntity)PacketMine.mc.player);
                    }
                }
                int finalSlot = slot;
                this.switchBack = () -> {
                    if (this.endRotate.getValue() && !this.faceVector(this.breakPos.toCenterPos().offset(BlockUtil.getClickSide(this.breakPos), 0.5))) {
                        if (shouldSwitch) {
                            if (this.hotBar.getValue()) {
                                InventoryUtil.switchToSlot(old);
                            } else {
                                PacketMine.mc.interactionManager.clickSlot(PacketMine.mc.player.currentScreenHandler.syncId, finalSlot, old, SlotActionType.SWAP, (PlayerEntity)PacketMine.mc.player);
                                EntityUtil.syncInventory();
                            }
                        }
                        return;
                    }
                    PacketMine.sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, this.breakPos, BlockUtil.getClickSide(this.breakPos), id));
                    if (this.endSwing.getValue()) {
                        EntityUtil.swingHand(Hand.MAIN_HAND, this.swingMode.getValue());
                    }
                    if (shouldSwitch) {
                        if (this.hotBar.getValue()) {
                            InventoryUtil.switchToSlot(old);
                        } else {
                            PacketMine.mc.interactionManager.clickSlot(PacketMine.mc.player.currentScreenHandler.syncId, finalSlot, old, SlotActionType.SWAP, (PlayerEntity)PacketMine.mc.player);
                            EntityUtil.syncInventory();
                        }
                    }
                    ++this.breakNumber;
                    this.delayTimer.reset();
                    this.glassDelayTimer.reset();
                    this.startTime.reset();
                    if (this.afterBreak.getValue() && this.shouldCrystal()) {
                        for (Direction facing : Direction.values()) {
                            CombatUtil.attackCrystal(this.breakPos.offset(facing), this.placeRotate.getValue(), true);
                        }
                    }
                    if (this.setAir.getValue()) {
                        PacketMine.mc.world.setBlockState(this.breakPos, Blocks.AIR.getDefaultState());
                    }
                    if (this.endRotate.getValue() && !this.shouldYawStep()) {
                        Arc.ROTATION.snapBack();
                    }
                    ghost = true;
                };
                if (!this.noGhostHand.getValue()) {
                    this.switchBack.run();
                    this.switchBack = null;
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
            if (this.rotate.getValue()) {
                Vec3i vec3i = side.getVector();
                if (!this.faceVector(this.breakPos.toCenterPos().add(new Vec3d((double)vec3i.getX() * 0.5, (double)vec3i.getY() * 0.5, (double)vec3i.getZ() * 0.5)))) {
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
                    this.secondAnim.setLength((long)breakTime);
                    this.secondTimer.reset();
                    secondPos = this.breakPos;
                }
                this.doDoubleBreak(side);
            }
            PacketMine.sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, this.breakPos, side, id));
            if (this.fastBypass.getValue()) {
                this.sendFastBypassPackets();
            }
            if (this.rotate.getValue() && !this.shouldYawStep()) {
                Arc.ROTATION.snapBack();
            }
            this.startTime.reset();
        }
    }

    private void breakBlock(BlockPos breakPos) {
        PacketMine.mc.world.getBlockState(breakPos).getBlock().onBreak((World)PacketMine.mc.world, breakPos, PacketMine.mc.world.getBlockState(breakPos), (PlayerEntity)PacketMine.mc.player);
    }

    void doDoubleBreak(Direction side) {
        // 第二目标（{@link #mineSecond}）和主挖不同格时，这次的 START/STOP 连发打在它身上，
        // 主挖继续走自己的 START → STOP 计时，两边互不覆盖
        BlockPos target = secondPos != null ? secondPos : this.breakPos;
        Direction dir = target.equals(this.breakPos) ? side : BlockUtil.getClickSide(target);
        if (dir == null) {
            dir = side;
        }
        final BlockPos pos = target;
        final Direction facing = dir;
        PacketMine.sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, facing, id));
        PacketMine.sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, facing, id));
    }

    boolean placeCrystal() {
        int crystal = this.findCrystal();
        if (crystal != -1) {
            int oldSlot = PacketMine.mc.player.getInventory().selectedSlot;
            this.doSwap(crystal, crystal);
            BlockUtil.placeCrystal(this.breakPos.up(), this.placeRotate.getValue());
            this.doSwap(oldSlot, crystal);
            this.placeTimer.reset();
            return !this.waitPlace.getValue();
        }
        return true;
    }

    @EventListener
    public void onAttackBlock(ClickBlockEvent event) {
        int slot;
        if (PacketMine.nullCheck()) {
            return;
        }
        if (PacketMine.mc.player.isCreative()) {
            return;
        }
        event.cancel();
        BlockPos pos = event.getPos();
        if (pos.equals((Object)this.breakPos)) {
            return;
        }
        if (PacketMine.unbreakable(pos)) {
            return;
        }
        if (this.breakPos != null && this.preferWeb.getValue() && BlockUtil.getBlock(this.breakPos) == Blocks.COBWEB) {
            return;
        }
        if (this.breakPos != null && this.preferHead.getValue() && PacketMine.mc.player.isCrawling() && EntityUtil.getPlayerPos(true).up().equals((Object)this.breakPos)) {
            return;
        }
        if (BlockUtil.getClickSideStrict(pos) == null) {
            return;
        }
        if ((double)MathHelper.sqrt((float)((float)PacketMine.mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos()))) > this.range.getValue()) {
            return;
        }
        this.breakPos = pos;
        this.breakNumber = 0;
        this.startPacket = false;
        ghost = false;
        complete = false;
        this.mineTimer.reset();
        this.animationTime.reset();
        if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
            return;
        }
        Direction side = BlockUtil.getClickSide(this.breakPos);
        if (this.rotate.getValue()) {
            Vec3i vec3i = side.getVector();
            if (!this.faceVector(this.breakPos.toCenterPos().add(new Vec3d((double)vec3i.getX() * 0.5, (double)vec3i.getY() * 0.5, (double)vec3i.getZ() * 0.5)))) {
                return;
            }
        }
        if (!this.startTime.passed(this.startDelay.getValueInt())) {
            return;
        }
        if (this.swing.getValue()) {
            EntityUtil.swingHand(Hand.MAIN_HAND, this.swingMode.getValue());
        }
        if (this.doubleBreak.getValue()) {
            if (secondPos == null || this.isAir(secondPos)) {
                int slot2 = this.getTool(this.breakPos);
                if (slot2 == -1) {
                    slot2 = PacketMine.mc.player.getInventory().selectedSlot;
                }
                this.secondFinalTime = this.getBreakTime(this.breakPos, slot2, 1.0);
                this.secondAnim.reset();
                this.secondAnim.setLength((long)this.secondFinalTime);
                this.secondTimer.reset();
                secondPos = this.breakPos;
            }
            this.doDoubleBreak(side);
        }
        if ((slot = this.getTool(this.breakPos)) == -1) {
            slot = PacketMine.mc.player.getInventory().selectedSlot;
        }
        this.breakFinalTime = this.getBreakTime(this.breakPos, slot);
        PacketMine.sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, this.breakPos, side, id));
        if (this.fastBypass.getValue()) {
            this.sendFastBypassPackets();
        }
        if (this.rotate.getValue() && !this.shouldYawStep()) {
            Arc.ROTATION.snapBack();
        }
        this.startTime.reset();
    }

    public void mine(BlockPos pos) {
        if (PacketMine.nullCheck()) {
            return;
        }
        if (PacketMine.mc.player.isCreative()) {
            PacketMine.mc.interactionManager.attackBlock(pos, BlockUtil.getClickSide(pos));
            return;
        }
        if (this.isOff()) {
            PacketMine.mc.interactionManager.attackBlock(pos, BlockUtil.getClickSide(pos));
            return;
        }
        if (pos.equals((Object)this.breakPos)) {
            return;
        }
        if (PacketMine.unbreakable(pos)) {
            return;
        }
        if (this.breakPos != null && this.preferWeb.getValue() && BlockUtil.getBlock(this.breakPos) == Blocks.COBWEB) {
            return;
        }
        if (this.breakPos != null && this.preferHead.getValue() && PacketMine.mc.player.isCrawling() && EntityUtil.getPlayerPos(true).up().equals((Object)this.breakPos)) {
            return;
        }
        if (BlockUtil.getClickSideStrict(pos) == null) {
            return;
        }
        if ((double)MathHelper.sqrt((float)((float)PacketMine.mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos()))) > this.range.getValue()) {
            return;
        }
        this.breakPos = pos;
        this.breakNumber = 0;
        this.startPacket = false;
        ghost = false;
        complete = false;
        this.mineTimer.reset();
        this.animationTime.reset();
    }

    /**
     * 把某一格登记成「副挖（双挖）目标」。
     *
     * <p>主挖（{@code breakPos}）保持不动，这个格子走
     * {@code secondPos} 那条独立计时：{@code doDoubleBreak} 的 START/STOP 连发会打到它身上，
     * 渲染也用 DoubleFill / DoubleOutline 那套颜色。AutoMine 的「主挖啃包围位、副挖啃目标位
     * （Burrow）」就是靠这个接口分配的。</p>
     */
    public void mineSecond(BlockPos pos) {
        if (PacketMine.nullCheck() || pos == null) {
            return;
        }
        if (pos.equals(this.breakPos) || pos.equals(secondPos)) {
            return;
        }
        if (this.isOff()) {
            return;
        }
        if (PacketMine.unbreakable(pos)) {
            return;
        }
        if (BlockUtil.getClickSideStrict(pos) == null) {
            return;
        }
        if ((double)MathHelper.sqrt((float)((float)PacketMine.mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos()))) > this.range.getValue()) {
            return;
        }
        int slot = this.getTool(pos);
        if (slot == -1) {
            slot = PacketMine.mc.player.getInventory().selectedSlot;
        }
        secondPos = pos;
        this.secondFinalTime = this.getBreakTime(pos, slot, 1.0);
        this.secondAnim.reset();
        this.secondAnim.setLength((long)this.secondFinalTime);
        this.secondTimer.reset();
    }

    /** 双挖（PacketMine 自己的 DoubleBreak）开没开 —— AutoMine 靠它决定要不要分配副挖 */
    public boolean isDoubleBreak() {
        return this.doubleBreak.getValue();
    }

    /**
     * 立刻中止对这一格的挖掘。
     *
     * <p>{@link #mine(BlockPos)} 只是把 {@code breakPos} 指到新格子，服务端那一侧还留着旧格的
     * 挖掘进度（START 已经发出去、既没有 STOP 也没有 ABORT）：旧格会在服务端慢慢累积进度，
     * 客户端这边 {@code secondPos} / {@code breakNumber} 也还挂在旧格上。换目标时先过一遍这里，
     * 补一个 ABORT 把服务端的进度作废，再把本地状态清干净。</p>
     */
    public void cancel(BlockPos pos) {
        if (pos == null || !pos.equals(this.breakPos)) {
            return;
        }
        Direction side = BlockUtil.getClickSide(pos);
        if (side != null) {
            PacketMine.sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, pos, side, id));
        }
        if (pos.equals(secondPos)) {
            secondPos = null;
        }
        this.breakPos = null;
        this.breakNumber = 0;
        this.startPacket = false;
        ghost = false;
        complete = false;
        progress = 0.0;
        this.mineTimer.reset();
        this.animationTime.reset();
    }

    boolean faceVector(Vec3d directionVec) {
        if (!this.shouldYawStep()) {
            Arc.ROTATION.lookAt(directionVec);
            return true;
        }
        this.sync.reset();
        this.directionVec = directionVec;
        if (Arc.ROTATION.inFov(directionVec, this.fov.getValueFloat())) {
            return true;
        }
        return !this.checkFov.getValue();
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (this.breakPos != null && PacketMine.mc.world.isAir(this.breakPos)) {
            complete = true;
        }
        if (!PacketMine.mc.player.isCreative()) {
            double ease;
            if (secondPos != null) {
                if (this.isAir(secondPos)) {
                    secondPos = null;
                    return;
                }
                if (!this.checkDouble.getValue() || !secondPos.equals((Object)this.breakPos)) {
                    this.secondAnim.setLength((long)this.secondFinalTime);
                    ease = this.secondAnim.ease(this.ease.getValue());
                    if (this.box.getValue()) {
                        Render3DUtil.drawFill(matrixStack, this.getFillBox(secondPos, ease), this.doubleColor.getValue());
                    }
                    if (this.outline.getValue()) {
                        Render3DUtil.drawBox(matrixStack, this.getOutlineBox(secondPos, ease), this.doubleOutlineColor.getValue());
                    }
                }
            }
            if (this.breakPos != null) {
                progress = (double)this.mineTimer.getMs() / this.breakFinalTime;
                this.animationTime.setLength((long)this.breakFinalTime);
                ease = this.animationTime.ease(this.ease.getValue());
                if (PacketMine.unbreakable(this.breakPos)) {
                    if (this.box.getValue()) {
                        Render3DUtil.drawFill(matrixStack, new Box(this.breakPos), this.startColor.getValue());
                    }
                    if (this.outline.getValue()) {
                        Render3DUtil.drawBox(matrixStack, new Box(this.breakPos), this.startOutlineColor.getValue());
                    }
                    return;
                }
                if (this.box.getValue()) {
                    Render3DUtil.drawFill(matrixStack, this.getFillBox(this.breakPos, ease), this.getColor(this.animationTime.ease(this.fadeEase.getValue())));
                }
                if (this.outline.getValue()) {
                    Render3DUtil.drawBox(matrixStack, this.getOutlineBox(this.breakPos, ease), this.getOutlineColor(this.animationTime.ease(this.fadeEase.getValue())));
                }
                if (this.text.getValue()) {
                    if (this.isAir(this.breakPos)) {
                        Render3DUtil.drawText3D("Waiting", this.breakPos.toCenterPos(), -1);
                    } else if ((double)((int)this.mineTimer.getMs()) < this.breakFinalTime) {
                        Render3DUtil.drawText3D(this.df.format(progress * 100.0) + "%", this.breakPos.toCenterPos(), -1);
                    } else {
                        Render3DUtil.drawText3D("100.0%", this.breakPos.toCenterPos(), -1);
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
        switch (this.animation.getValue().ordinal()) {
            case 1: {
                ease = (1.0 - ease) / 2.0;
                return new Box(pos).shrink(ease, 0.0, ease).shrink(-ease, 0.0, -ease);
            }
            case 3: {
                return new Box((double)pos.getX(), (double)(pos.getY() + 1) - ease, (double)pos.getZ(), (double)(pos.getX() + 1), (double)(pos.getY() + 1), (double)(pos.getZ() + 1));
            }
            case 2: {
                return new Box((double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), (double)(pos.getX() + 1), (double)pos.getY() + ease, (double)(pos.getZ() + 1));
            }
            case 0: {
                ease = (1.0 - ease) / 2.0;
                return new Box(pos).shrink(ease, ease, ease).shrink(-ease, -ease, -ease);
            }
            case 4: {
                return new Box(pos).shrink(ease, ease, ease).shrink(-ease, -ease, -ease);
            }
        }
        return new Box(pos);
    }

    private Box getOutlineBox(BlockPos pos, double ease) {
        ease = Math.min(ease + this.expandLine.getValue(), 1.0);
        switch (this.animation.getValue().ordinal()) {
            case 1: {
                ease = (1.0 - ease) / 2.0;
                return new Box(pos).shrink(ease, 0.0, ease).shrink(-ease, 0.0, -ease);
            }
            case 3: {
                return new Box((double)pos.getX(), (double)(pos.getY() + 1) - ease, (double)pos.getZ(), (double)(pos.getX() + 1), (double)(pos.getY() + 1), (double)(pos.getZ() + 1));
            }
            case 2: {
                return new Box((double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), (double)(pos.getX() + 1), (double)pos.getY() + ease, (double)(pos.getZ() + 1));
            }
            case 0: {
                ease = (1.0 - ease) / 2.0;
                return new Box(pos).shrink(ease, ease, ease).shrink(-ease, -ease, -ease);
            }
            case 4: {
                return new Box(pos).shrink(ease, ease, ease).shrink(-ease, -ease, -ease);
            }
        }
        return new Box(pos);
    }

    @EventListener(priority=-200)
    public void onPacketSend(PacketEvent.Send event) {
        if (PacketMine.nullCheck() || PacketMine.mc.player.isCreative()) {
            return;
        }
        if (event.getPacket() instanceof PlayerMoveC2SPacket) {
            if (this.bypassGround.getValue() && !PacketMine.mc.player.isFallFlying() && this.breakPos != null && !this.isAir(this.breakPos) && this.bypassTime.getValue() > 0.0 && MathHelper.sqrt((float)((float)this.breakPos.toCenterPos().squaredDistanceTo(PacketMine.mc.player.getEyePos()))) <= this.range.getValueFloat() + 2.0f) {
                double breakTime = this.breakFinalTime - this.bypassTime.getValue();
                if (breakTime <= 0.0 || this.mineTimer.passed((long)breakTime)) {
                    this.sendGroundPacket = true;
                    ((IPlayerMoveC2SPacket)event.getPacket()).setOnGround(true);
                }
            } else {
                this.sendGroundPacket = false;
            }
        } else {
            Packet<?> packet = event.getPacket();
            if (packet instanceof UpdateSelectedSlotC2SPacket) {
                UpdateSelectedSlotC2SPacket packet2 = (UpdateSelectedSlotC2SPacket)packet;
                if (packet2.getSelectedSlot() != this.lastSlot) {
                    this.lastSlot = packet2.getSelectedSlot();
                    if (this.switchReset.getValue()) {
                        this.startPacket = false;
                        ghost = false;
                        complete = false;
                        this.mineTimer.reset();
                        this.animationTime.reset();
                    }
                }
            } else {
                packet = event.getPacket();
                if (packet instanceof PlayerActionC2SPacket) {
                    PlayerActionC2SPacket packet3 = (PlayerActionC2SPacket)packet;
                    if (packet3.getAction() == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) {
                        if (this.breakPos == null || !packet3.getPos().equals((Object)this.breakPos)) {
                            return;
                        }
                        if (this.grimDisabler.getValue()) {
                            mc.getNetworkHandler().sendPacket((Packet)new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, packet3.getPos(), packet3.getDirection()));
                        }
                        this.startPacket = true;
                    } else if (packet3.getAction() == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK) {
                        if (this.breakPos == null || !packet3.getPos().equals((Object)this.breakPos)) {
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
    }

    boolean canPlaceCrystal(BlockPos pos) {
        BlockPos obsPos = pos.down();
        BlockPos boost = obsPos.up();
        return !(BlockUtil.getBlock(obsPos) != Blocks.BEDROCK && BlockUtil.getBlock(obsPos) != Blocks.OBSIDIAN || BlockUtil.getClickSideStrict(obsPos) == null || !this.noEntity(boost) || !this.noEntity(boost.up()) || ClientSetting.INSTANCE.lowVersion.getValue() && !PacketMine.mc.world.isAir(boost.up()));
    }

    boolean noEntity(BlockPos pos) {
        for (Entity entity : BlockUtil.getEntities(new Box(pos))) {
            if (entity instanceof ItemEntity || entity instanceof ArmorStandEntity && AntiCheat.INSTANCE.ignoreArmorStand.getValue()) continue;
            return false;
        }
        return true;
    }

    void doSwap(int slot, int inv) {
        if (!this.inventory.getValue()) {
            InventoryUtil.switchToSlot(slot);
        } else {
            InventoryUtil.inventorySwap(inv, PacketMine.mc.player.getInventory().selectedSlot);
        }
    }

    int findCrystal() {
        if (this.inventory.getValue()) {
            return InventoryUtil.findItemInventorySlot(Items.END_CRYSTAL);
        }
        return InventoryUtil.findItem(Items.END_CRYSTAL);
    }

    int findBlock(Block block) {
        if (this.inventory.getValue()) {
            return InventoryUtil.findBlockInventorySlot(block);
        }
        return InventoryUtil.findBlock(block);
    }

    boolean shouldCrystal() {
        return this.crystal.getValue() && (!this.onlyHeadBomber.getValue() || this.obsidian.isPressed()) || CevBreaker.INSTANCE.isOn();
    }

    public static double getBreakTime(BlockPos pos) {
        int slot = INSTANCE.getTool(pos);
        if (slot == -1) {
            slot = PacketMine.mc.player.getInventory().selectedSlot;
        }
        return INSTANCE.getBreakTime(pos, slot);
    }

    double getBreakTime(BlockPos pos, int slot) {
        return this.getBreakTime(pos, slot, this.damage.getValue());
    }

    double getBreakTime(BlockPos pos, int slot, double damage) {
        return (double)(1.0f / this.getBlockStrength(pos, PacketMine.mc.player.getInventory().getStack(slot)) / 20.0f * 1000.0f) * damage;
    }

    float getBlockStrength(BlockPos position, ItemStack itemStack) {
        BlockState state = PacketMine.mc.world.getBlockState(position);
        float hardness = state.getHardness((BlockView)PacketMine.mc.world, position);
        if (hardness < 0.0f) {
            return 0.0f;
        }
        float i = !state.isToolRequired() || itemStack.isSuitableFor(state) ? 30.0f : 100.0f;
        return this.getDigSpeed(state, itemStack) / hardness / i;
    }

    float getDigSpeed(BlockState state, ItemStack itemStack) {
        boolean inWeb;
        int efficiencyModifier;
        float digSpeed = this.getDestroySpeed(state, itemStack);
        if (digSpeed > 1.0f && (efficiencyModifier = EnchantmentHelper.getLevel((RegistryEntry)((RegistryEntry)PacketMine.mc.world.getRegistryManager().get(Enchantments.EFFICIENCY.getRegistryRef()).getEntry(Enchantments.EFFICIENCY).get()), (ItemStack)itemStack)) > 0 && !itemStack.isEmpty()) {
            digSpeed += (float)(StrictMath.pow(efficiencyModifier, 2.0) + 1.0);
        }
        if (PacketMine.mc.player.hasStatusEffect(StatusEffects.HASTE)) {
            digSpeed *= 1.0f + (float)(PacketMine.mc.player.getStatusEffect(StatusEffects.HASTE).getAmplifier() + 1) * 0.2f;
        }
        if (PacketMine.mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            digSpeed *= (switch (PacketMine.mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
                case 0 -> 0.3f;
                case 1 -> 0.09f;
                case 2 -> 0.0027f;
                default -> 8.1E-4f;
            });
        }
        if (PacketMine.mc.player.isSubmergedInWater()) {
            digSpeed *= (float)PacketMine.mc.player.getAttributeInstance(EntityAttributes.PLAYER_SUBMERGED_MINING_SPEED).getValue();
        }
        boolean bl = inWeb = this.checkWeb.getValue() && Arc.PLAYER.isInWeb((PlayerEntity)PacketMine.mc.player) && PacketMine.mc.world.getBlockState(this.breakPos).getBlock() == Blocks.COBWEB;
        if ((!PacketMine.mc.player.isOnGround() || inWeb) && PacketMine.INSTANCE.checkGround.getValue() && (!this.smart.getValue() || Criticals.INSTANCE.mode.is(Criticals.Mode.Ground) && Criticals.INSTANCE.isOn() || PacketMine.mc.player.isFallFlying() || inWeb)) {
            digSpeed /= 5.0f;
        }
        return digSpeed < 0.0f ? 0.0f : digSpeed;
    }

    float getDestroySpeed(BlockState state, ItemStack itemStack) {
        float destroySpeed = 1.0f;
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
        return new Color((int)((double)sR + (double)(eR - sR) * quad), (int)((double)sG + (double)(eG - sG) * quad), (int)((double)sB + (double)(eB - sB) * quad), (int)((double)sA + (double)(eA - sA) * quad));
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
        return new Color((int)((double)sR + (double)(eR - sR) * quad), (int)((double)sG + (double)(eG - sG) * quad), (int)((double)sB + (double)(eB - sB) * quad), (int)((double)sA + (double)(eA - sA) * quad));
    }

    int getTool(BlockPos pos) {
        if (this.hotBar.getValue()) {
            int index = -1;
            float CurrentFastest = 1.0f;
            for (int i = 0; i < 9; ++i) {
                float destroySpeed;
                float digSpeed;
                ItemStack stack = PacketMine.mc.player.getInventory().getStack(i);
                if (stack == ItemStack.EMPTY || !((digSpeed = (float)EnchantmentHelper.getLevel((RegistryEntry)((RegistryEntry)PacketMine.mc.world.getRegistryManager().get(Enchantments.EFFICIENCY.getRegistryRef()).getEntry(Enchantments.EFFICIENCY).get()), (ItemStack)stack)) + (destroySpeed = stack.getMiningSpeedMultiplier(PacketMine.mc.world.getBlockState(pos))) > CurrentFastest)) continue;
                CurrentFastest = digSpeed + destroySpeed;
                index = i;
            }
            return index;
        }
        AtomicInteger slot = new AtomicInteger();
        slot.set(-1);
        float CurrentFastest = 1.0f;
        for (Map.Entry<Integer, ItemStack> entry : InventoryUtil.getInventoryAndHotbarSlots().entrySet()) {
            float destroySpeed;
            float digSpeed;
            if (entry.getValue().getItem() instanceof AirBlockItem || !((digSpeed = (float)EnchantmentHelper.getLevel((RegistryEntry)((RegistryEntry)PacketMine.mc.world.getRegistryManager().get(Enchantments.EFFICIENCY.getRegistryRef()).getEntry(Enchantments.EFFICIENCY).get()), (ItemStack)entry.getValue())) + (destroySpeed = entry.getValue().getMiningSpeedMultiplier(PacketMine.mc.world.getBlockState(pos))) > CurrentFastest)) continue;
            CurrentFastest = digSpeed + destroySpeed;
            slot.set(entry.getKey());
        }
        return slot.get();
    }

    private boolean shouldYawStep() {
        if (!this.whenElytra.getValue() && (PacketMine.mc.player.isFallFlying() || ElytraFly.INSTANCE.isOn() && ElytraFly.INSTANCE.isFallFlying())) {
            return false;
        }
        return this.yawStep.getValue() && !Velocity.INSTANCE.noRotation();
    }

    boolean isAir(BlockPos breakPos) {
        return PacketMine.mc.world.isAir(breakPos) || BlockUtil.getBlock(breakPos) == Blocks.FIRE && BlockUtil.hasCrystal(breakPos);
    }

    /** 这一格是不是「自己刚塞下去、正要引爆」的重生锚：是的话包挖要立刻换目标，别敲自己的锚 */
    public static boolean isProtectedAnchor(BlockPos pos) {
        if (pos == null || mc.world == null) {
            return false;
        }
        if (AutoAnchor.INSTANCE == null || AutoAnchor.INSTANCE.currentPos == null) {
            return false;
        }
        return pos.equals(AutoAnchor.INSTANCE.currentPos) && mc.world.getBlockState(pos).getBlock() instanceof RespawnAnchorBlock;
    }

    public static boolean unbreakable(BlockPos blockPos) {
        Block block = PacketMine.mc.world.getBlockState(blockPos).getBlock();
        return !(block instanceof AirBlock) && (block.getHardness() == -1.0f || block.getHardness() == 100.0f);
    }

    static {
        progress = 0.0;
        ghost = false;
        complete = false;
    }

    public static enum Page {
        General,
        Check,
        Place,
        Rotation,
        Render;

    }

    private static enum Animation {
        Center,
        Grow,
        Up,
        Down,
        Oscillation,
        None;

    }
}

