package dev.g255.arc.mod.modules.impl.combat;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.ClientTickEvent;
import dev.g255.arc.api.events.impl.Render3DEvent;
import dev.g255.arc.api.events.impl.RotationEvent;
import dev.g255.arc.api.utils.combat.CombatUtil;
import dev.g255.arc.api.utils.entity.PlayerEntityPredict;
import dev.g255.arc.api.utils.math.AnimateUtil;
import dev.g255.arc.api.utils.math.ExplosionUtil;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.api.utils.player.EntityUtil;
import dev.g255.arc.api.utils.player.InventoryUtil;
import dev.g255.arc.api.utils.render.ColorUtil;
import dev.g255.arc.api.utils.world.BlockUtil;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.impl.exploit.Blink;
import dev.g255.arc.mod.modules.impl.player.AirPlace;
import dev.g255.arc.mod.modules.impl.player.PacketMine;
import dev.g255.arc.mod.modules.impl.render.PlaceRender;
import dev.g255.arc.mod.modules.settings.enums.SwingSide;
import dev.g255.arc.mod.modules.settings.enums.Timing;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.ColorSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

/**
 * 重生锚光环 —— 逻辑与设置项完全照 Astra-Daily v1.2 那一版搬过来
 * （Item/模块相关类名换成 Arc 的，行为分支不改）。
 */
public class AutoAnchor extends Module {
    public static AutoAnchor INSTANCE;
    static Vec3d placeVec3d;
    static Vec3d curVec3d;

    public final EnumSetting<Page> page = this.add(new EnumSetting<Page>("Page", Page.General));
    public final SliderSetting range = this.add(new SliderSetting("Range", 5.0, 0.0, 6.0, 0.1, () -> this.page.getValue() == Page.General).setSuffix("m"));
    public final SliderSetting targetRange = this.add(new SliderSetting("TargetRange", 8.0, 0.1, 12.0, 0.1, () -> this.page.getValue() == Page.General).setSuffix("m"));
    private final BooleanSetting preferCrystal = this.add(new BooleanSetting("PreferCrystal", false, () -> this.page.getValue() == Page.General));
    private final BooleanSetting thread = this.add(new BooleanSetting("Thread", false, () -> this.page.getValue() == Page.General));
    private final BooleanSetting inventorySwap = this.add(new BooleanSetting("InventorySwap", true, () -> this.page.getValue() == Page.General));
    private final BooleanSetting breakCrystal = this.add(new BooleanSetting("BreakCrystal", true, () -> this.page.getValue() == Page.General));
    private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", true, () -> this.page.getValue() == Page.General));
    private final EnumSetting<SwingSide> swingMode = this.add(new EnumSetting<SwingSide>("Swing", SwingSide.All, () -> this.page.getValue() == Page.General));
    private final EnumSetting<Timing> timing = this.add(new EnumSetting<Timing>("Timing", Timing.All, () -> this.page.getValue() == Page.General));
    private final SliderSetting placeDelay = this.add(new SliderSetting("PlaceDelay", 100.0, 0.0, 500.0, 1.0, () -> this.page.getValue() == Page.General).setSuffix("ms"));
    private final SliderSetting fillDelay = this.add(new SliderSetting("FillDelay", 100.0, 0.0, 500.0, 1.0, () -> this.page.getValue() == Page.General).setSuffix("ms"));
    private final SliderSetting breakDelay = this.add(new SliderSetting("BreakDelay", 100.0, 0.0, 500.0, 1.0, () -> this.page.getValue() == Page.General).setSuffix("ms"));
    private final SliderSetting spamDelay = this.add(new SliderSetting("SpamDelay", 200.0, 0.0, 1000.0, 1.0, () -> this.page.getValue() == Page.General).setSuffix("ms"));
    private final SliderSetting updateDelay = this.add(new SliderSetting("UpdateDelay", 200.0, 0.0, 1000.0, 1.0, () -> this.page.getValue() == Page.General).setSuffix("ms"));
    private final BooleanSetting spam = this.add(new BooleanSetting("Spam", true, () -> this.page.getValue() == Page.General).setParent());
    private final BooleanSetting mineSpam = this.add(new BooleanSetting("OnlyMining", true, () -> this.page.getValue() == Page.General && this.spam.isOpen()));
    private final BooleanSetting spamPlace = this.add(new BooleanSetting("Fast", true, () -> this.page.getValue() == Page.General).setParent());
    private final BooleanSetting inSpam = this.add(new BooleanSetting("WhenSpamming", true, () -> this.page.getValue() == Page.General && this.spamPlace.isOpen()));
    public final SliderSetting minDamage = this.add(new SliderSetting("Min", 4.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == Page.Interact).setSuffix("hp"));
    public final SliderSetting breakMin = this.add(new SliderSetting("ExplosionMin", 4.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == Page.Interact).setSuffix("hp"));
    public final SliderSetting headDamage = this.add(new SliderSetting("ForceHead", 7.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == Page.Interact).setSuffix("hp"));
    private final BooleanSetting noSuicide = this.add(new BooleanSetting("NoSuicide", true, () -> this.page.getValue() == Page.Interact));
    private final BooleanSetting smart = this.add(new BooleanSetting("Smart", true, () -> this.page.getValue() == Page.Interact));
    private final BooleanSetting terrainIgnore = this.add(new BooleanSetting("TerrainIgnore", true, () -> this.page.getValue() == Page.Interact));
    private final SliderSetting minPrefer = this.add(new SliderSetting("Prefer", 7.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == Page.Interact).setSuffix("hp"));
    private final SliderSetting maxSelfDamage = this.add(new SliderSetting("MaxSelf", 8.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == Page.Interact).setSuffix("hp"));
    private final SliderSetting selfPredict = this.add(new SliderSetting("SelfPredict", 4, 0, 10, () -> this.page.getValue() == Page.Predict).setSuffix("ticks"));
    private final SliderSetting predictTicks = this.add(new SliderSetting("Predict", 4, 0, 10, () -> this.page.getValue() == Page.Predict).setSuffix("ticks"));
    private final BooleanSetting antiSelfTrap = this.add(new BooleanSetting("AntiSelfTrap", true, () -> this.page.getValue() == Page.General));
    private final SliderSetting mineProgress = this.add(new SliderSetting("MineProgress", 0.9, 0.5, 1.0, 0.05, () -> this.page.getValue() == Page.General && this.antiSelfTrap.getValue()));
    private final SliderSetting placeAttempts = this.add(new SliderSetting("PlaceAttempts", 2.0, 1.0, 4.0, 1.0, () -> this.page.getValue() == Page.General));
    private final SliderSetting simulation = this.add(new SliderSetting("Simulation", 5.0, 0.0, 20.0, 1.0, () -> this.page.getValue() == Page.Predict));
    private final SliderSetting maxMotionY = this.add(new SliderSetting("MaxMotionY", 0.34, 0.0, 2.0, 0.01, () -> this.page.getValue() == Page.Predict));
    private final BooleanSetting step = this.add(new BooleanSetting("Step", false, () -> this.page.getValue() == Page.Predict));
    private final BooleanSetting doubleStep = this.add(new BooleanSetting("DoubleStep", false, () -> this.page.getValue() == Page.Predict));
    private final BooleanSetting jump = this.add(new BooleanSetting("Jump", false, () -> this.page.getValue() == Page.Predict));
    private final BooleanSetting inBlockPause = this.add(new BooleanSetting("InBlockPause", true, () -> this.page.getValue() == Page.Predict));
    private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true, () -> this.page.getValue() == Page.Rotate));
    private final BooleanSetting yawStep = this.add(new BooleanSetting("YawStep", false, () -> this.page.getValue() == Page.Rotate));
    private final SliderSetting steps = this.add(new SliderSetting("Steps", 1.0, 0.0, 1.0, 0.01, () -> this.page.getValue() == Page.Rotate));
    private final BooleanSetting checkFov = this.add(new BooleanSetting("OnlyLooking", false, () -> this.page.getValue() == Page.Rotate));
    private final SliderSetting fov = this.add(new SliderSetting("Fov", 10.0, 0.0, 360.0, 0.1, () -> this.page.getValue() == Page.Rotate).setSuffix("°"));
    private final SliderSetting priority = this.add(new SliderSetting("Priority", 30, 0, 100, () -> this.page.getValue() == Page.Rotate));
    private final BooleanSetting assist = this.add(new BooleanSetting("Assist", true, () -> this.page.getValue() == Page.Assist));
    private final BooleanSetting obsidian = this.add(new BooleanSetting("Obsidian", true, () -> this.page.getValue() == Page.Assist));
    private final BooleanSetting checkMine = this.add(new BooleanSetting("DetectMining", true, () -> this.page.getValue() == Page.Assist));
    private final SliderSetting assistRange = this.add(new SliderSetting("AssistRange", 10.0, 0.0, 20.0, 0.1, () -> this.page.getValue() == Page.Assist).setSuffix("m"));
    private final SliderSetting assistDamage = this.add(new SliderSetting("AssistDamage", 6.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == Page.Assist).setSuffix("hp"));
    private final SliderSetting assistDelay = this.add(new SliderSetting("AssistDelay", 0.1, 0.0, 1.0, 0.01, () -> this.page.getValue() == Page.Assist).setSuffix("s"));
    private final EnumSetting<Aura.TargetESP> mode = this.add(new EnumSetting<Aura.TargetESP>("TargetESP", Aura.TargetESP.None, () -> this.page.getValue() == Page.Render));
    private final ColorSetting color = this.add(new ColorSetting("TargetColor", new Color(419430399, true), () -> this.page.getValue() == Page.Render));
    private final ColorSetting outlineColor = this.add(new ColorSetting("TargetOutlineColor", new Color(419430399, true), () -> this.page.getValue() == Page.Render));
    private final BooleanSetting render = this.add(new BooleanSetting("Render", true, () -> this.page.getValue() == Page.Render));
    private final BooleanSetting shrink = this.add(new BooleanSetting("Shrink", false, () -> this.page.getValue() == Page.Render && this.render.getValue()));
    private final ColorSetting box = this.add(new ColorSetting("Box", new Color(-1049473294, true), () -> this.page.getValue() == Page.Render && this.render.getValue()).injectBoolean(true));
    private final ColorSetting fill = this.add(new ColorSetting("Fill", new Color(1282823407, true), () -> this.page.getValue() == Page.Render && this.render.getValue()).injectBoolean(true));
    private final SliderSetting sliderSpeed = this.add(new SliderSetting("SliderSpeed", 0.92, 0.0, 1.0, 0.01, () -> this.page.getValue() == Page.Render && this.render.getValue()));
    private final SliderSetting startFadeTime = this.add(new SliderSetting("StartFade", 0.1, 0.0, 2.0, 0.01, () -> this.page.getValue() == Page.Render && this.render.getValue()).setSuffix("s"));
    private final SliderSetting fadeSpeed = this.add(new SliderSetting("FadeSpeed", 1.0, 0.01, 1.0, 0.01, () -> this.page.getValue() == Page.Render && this.render.getValue()));

    final ArrayList<BlockPos> chargeList = new ArrayList<>();
    private int pendingAttempts;
    private final Timer delayTimer = new Timer();
    private final Timer calcTimer = new Timer();
    private final Timer noPosTimer = new Timer();
    private final Timer assistTimer = new Timer();
    public Vec3d directionVec = null;
    public PlayerEntity displayTarget;
    public BlockPos currentPos;
    public BlockPos tempPos;
    double fade = 0.0;
    BlockPos assistPos;

    public AutoAnchor() {
        super("AutoAnchor", Module.Category.Combat);
        this.setChinese("重生锚光环");
        INSTANCE = this;
        Arc.EVENT_BUS.subscribe(new AnchorRender());
    }

    @Override
    public void onEnable() {
        placeVec3d = null;
        curVec3d = null;
    }

    @Override
    public String getInfo() {
        return this.displayTarget != null && this.currentPos != null ? this.displayTarget.getName().getString() : null;
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (this.displayTarget != null && this.currentPos != null) {
            Aura.doRender(matrixStack, mc.getRenderTickCounter().getTickDelta(true), this.displayTarget,
                    this.color.getValue(), this.outlineColor.getValue(), this.mode.getValue());
        }
    }

    @EventListener
    public void onRotate(RotationEvent event) {
        if (this.currentPos != null && this.rotate.getValue() && this.yawStep.getValue() && this.directionVec != null) {
            event.setTarget(this.directionVec, this.steps.getValueFloat(), this.priority.getValueFloat());
        }
    }

    @Override
    public void onDisable() {
        this.tempPos = null;
        this.currentPos = null;
        placeVec3d = null;
        curVec3d = null;
    }

    public void onThread() {
        if (this.isOff() || AutoAnchor.nullCheck()) {
            return;
        }
        // Astra 原版：末地不工作（那边重生锚炸不出伤害），目标直接清掉
        if (AutoAnchor.mc.world != null && AutoAnchor.mc.world.getRegistryKey() == net.minecraft.world.World.END) {
            this.currentPos = null;
            return;
        }
        if (!this.thread.getValue()) {
            return;
        }
        if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
            this.currentPos = null;
            return;
        }
        if (AutoCrystal.INSTANCE.isOn() && AutoCrystal.INSTANCE.crystalPos != null && this.preferCrystal.getValue()) {
            this.currentPos = null;
            return;
        }
        int anchor = this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.RESPAWN_ANCHOR) : InventoryUtil.findBlock(Blocks.RESPAWN_ANCHOR);
        int glowstone = this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.GLOWSTONE) : InventoryUtil.findBlock(Blocks.GLOWSTONE);
        int unBlock = this.inventorySwap.getValue() ? anchor : InventoryUtil.findUnBlock();
        if (anchor == -1) {
            this.currentPos = null;
            return;
        }
        if (glowstone == -1) {
            this.currentPos = null;
            return;
        }
        if (unBlock == -1) {
            this.currentPos = null;
            return;
        }
        if (AutoAnchor.mc.player.isSneaking()) {
            this.currentPos = null;
            return;
        }
        if (this.usingPause.getValue() && AutoAnchor.mc.player.isUsingItem()) {
            this.currentPos = null;
            return;
        }
        this.calc();
    }

    @EventListener
    public void onTick(ClientTickEvent event) {
        BlockPos pos;
        if (AutoAnchor.nullCheck()) {
            return;
        }
        // Astra 原版：末地不工作，目标直接清掉（和 onThread 一致）
        if (AutoAnchor.mc.world != null && AutoAnchor.mc.world.getRegistryKey() == net.minecraft.world.World.END) {
            this.currentPos = null;
            return;
        }
        if (this.timing.is(Timing.Pre) && event.isPost() || this.timing.is(Timing.Post) && event.isPre()) {
            return;
        }
        int anchor = this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.RESPAWN_ANCHOR) : InventoryUtil.findBlock(Blocks.RESPAWN_ANCHOR);
        int glowstone = this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.GLOWSTONE) : InventoryUtil.findBlock(Blocks.GLOWSTONE);
        int unBlock = this.inventorySwap.getValue() ? anchor : InventoryUtil.findUnBlock();
        int old = AutoAnchor.mc.player.getInventory().selectedSlot;
        if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
            this.currentPos = null;
            return;
        }
        if (AutoCrystal.INSTANCE.isOn() && AutoCrystal.INSTANCE.crystalPos != null && this.preferCrystal.getValue()) {
            this.currentPos = null;
            return;
        }
        if (anchor == -1) {
            this.currentPos = null;
            return;
        }
        if (glowstone == -1) {
            this.currentPos = null;
            return;
        }
        if (unBlock == -1) {
            this.currentPos = null;
            return;
        }
        if (AutoAnchor.mc.player.isSneaking()) {
            this.currentPos = null;
            return;
        }
        if (this.usingPause.getValue() && AutoAnchor.mc.player.isUsingItem()) {
            this.currentPos = null;
            return;
        }
        if (this.inventorySwap.getValue() && !EntityUtil.inInventory()) {
            return;
        }
        if (this.assist.getValue()) {
            this.onAssist();
        }
        if (!this.thread.getValue()) {
            this.calc();
        }
        if ((pos = this.currentPos) != null) {
            if (this.breakCrystal.getValue()) {
                CombatUtil.attackCrystal(new BlockPos((Vec3i)pos), this.rotate.getValue(), false);
            }
            boolean shouldSpam = this.spam.getValue() && (!this.mineSpam.getValue() || Arc.BREAK.isMining(pos));
            if (shouldSpam) {
                if (!this.delayTimer.passed((long)this.spamDelay.getValueFloat())) {
                    return;
                }
                this.delayTimer.reset();
                if (BlockUtil.canPlace(pos, this.range.getValue(), this.breakCrystal.getValue())) {
                    this.placeBlock(pos, this.rotate.getValue(), anchor);
                }
                if (!this.chargeList.contains(pos)) {
                    this.delayTimer.reset();
                    this.clickBlock(pos, BlockUtil.getClickSide(pos), this.rotate.getValue(), glowstone);
                    this.chargeList.add(pos);
                }
                this.chargeList.remove(pos);
                this.clickBlock(pos, BlockUtil.getClickSide(pos), this.rotate.getValue(), unBlock);
                if (this.spamPlace.getValue() && this.inSpam.getValue()) {
                    if (this.yawStep.getValue() && this.checkFov.getValue()) {
                        Direction side = BlockUtil.getClickSide(pos);
                        Vec3d directionVec = new Vec3d((double)pos.getX() + 0.5 + (double)side.getVector().getX() * 0.5, (double)pos.getY() + 0.5 + (double)side.getVector().getY() * 0.5, (double)pos.getZ() + 0.5 + (double)side.getVector().getZ() * 0.5);
                        if (Arc.ROTATION.inFov(directionVec, this.fov.getValueFloat())) {
                            CombatUtil.modifyPos = pos;
                            CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
                            this.placeBlock(pos, this.rotate.getValue(), anchor);
                            CombatUtil.modifyPos = null;
                        }
                    } else {
                        CombatUtil.modifyPos = pos;
                        CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
                        this.placeBlock(pos, this.rotate.getValue(), anchor);
                        CombatUtil.modifyPos = null;
                    }
                }
            } else if (BlockUtil.canPlace(pos, this.range.getValue(), this.breakCrystal.getValue())) {
                if (!this.delayTimer.passed((long)this.placeDelay.getValueFloat())) {
                    return;
                }
                this.delayTimer.reset();
                this.placeBlock(pos, this.rotate.getValue(), anchor);
            } else if (BlockUtil.getBlock(pos) == Blocks.RESPAWN_ANCHOR) {
                if (!this.chargeList.contains(pos)) {
                    if (!this.delayTimer.passed((long)this.fillDelay.getValueFloat())) {
                        return;
                    }
                    this.delayTimer.reset();
                    this.clickBlock(pos, BlockUtil.getClickSide(pos), this.rotate.getValue(), glowstone);
                    this.chargeList.add(pos);
                } else {
                    if (!this.delayTimer.passed((long)this.breakDelay.getValueFloat())) {
                        return;
                    }
                    this.delayTimer.reset();
                    this.chargeList.remove(pos);
                    this.clickBlock(pos, BlockUtil.getClickSide(pos), this.rotate.getValue(), unBlock);
                    if (this.spamPlace.getValue()) {
                        if (this.yawStep.getValue() && this.checkFov.getValue()) {
                            Direction side = BlockUtil.getClickSide(pos);
                            Vec3d directionVec = new Vec3d((double)pos.getX() + 0.5 + (double)side.getVector().getX() * 0.5, (double)pos.getY() + 0.5 + (double)side.getVector().getY() * 0.5, (double)pos.getZ() + 0.5 + (double)side.getVector().getZ() * 0.5);
                            if (Arc.ROTATION.inFov(directionVec, this.fov.getValueFloat())) {
                                CombatUtil.modifyPos = pos;
                                CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
                                this.placeBlock(pos, this.rotate.getValue(), anchor);
                                CombatUtil.modifyPos = null;
                            }
                        } else {
                            CombatUtil.modifyPos = pos;
                            CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
                            this.placeBlock(pos, this.rotate.getValue(), anchor);
                            CombatUtil.modifyPos = null;
                        }
                    }
                }
            }
            if (!this.inventorySwap.getValue()) {
                this.doSwap(old);
            }
        }
    }

    private void calc() {
        if (AutoAnchor.nullCheck()) {
            return;
        }
        if (!this.calcTimer.passed((long)this.updateDelay.getValueFloat())) {
            return;
        }
        this.calcTimer.reset();
        PlayerEntityPredict selfPredict = new PlayerEntityPredict((PlayerEntity)AutoAnchor.mc.player, this.maxMotionY.getValue(), this.selfPredict.getValueInt(), this.simulation.getValueInt(), this.step.getValue(), this.doubleStep.getValue(), this.jump.getValue(), this.inBlockPause.getValue());
        this.tempPos = null;
        double placeDamage = this.minDamage.getValue();
        double breakDamage = this.breakMin.getValue();
        boolean anchorFound = false;
        List<PlayerEntity> enemies = CombatUtil.getEnemies(this.targetRange.getValue());
        ArrayList<PlayerEntityPredict> list = new ArrayList<>();
        for (PlayerEntity player : enemies) {
            list.add(new PlayerEntityPredict(player, this.maxMotionY.getValue(), this.predictTicks.getValueInt(), this.simulation.getValueInt(), this.step.getValue(), this.doubleStep.getValue(), this.jump.getValue(), this.inBlockPause.getValue()));
        }

        for (PlayerEntityPredict pap : list) {
            double selfDamage;
            BlockPos pos = EntityUtil.getEntityPos((Entity)pap.player, true).up(2);
            if (!BlockUtil.canPlace(pos, this.range.getValue(), this.breakCrystal.getValue())
                    && (BlockUtil.getBlock(pos) != Blocks.RESPAWN_ANCHOR || BlockUtil.getClickSideStrict(pos) == null)) {
                continue;
            }
            if ((selfDamage = this.getAnchorDamage(pos, selfPredict.player, selfPredict.predict)) > this.maxSelfDamage.getValue()) {
                continue;
            }
            if (this.noSuicide.getValue() && selfDamage > (double)(AutoAnchor.mc.player.getHealth() + AutoAnchor.mc.player.getAbsorptionAmount())) {
                continue;
            }
            double damage = this.getAnchorDamage(pos, pap.player, pap.predict);
            if (damage <= (double)this.headDamage.getValueFloat()) {
                continue;
            }
            if (this.smart.getValue() && selfDamage > damage) {
                continue;
            }
            this.displayTarget = pap.player;
            this.tempPos = pos;
            break;
        }

        // 自己在挖的方块马上要空出来（会被对方踩进来）：先在那两格塞锚
        if (this.antiSelfTrap.getValue() && this.tempPos == null && PacketMine.INSTANCE != null) {
            double progress = PacketMine.INSTANCE.getProgress();
            BlockPos minePos = PacketMine.getBreakPos();
            if (minePos != null && (PacketMine.INSTANCE.isInstantBreak() || progress >= this.mineProgress.getValue())) {
                // 原版在这一步是「正在挖的就是锚 → 直接 return」：这时候靶子会留在上一轮的值上，
                // 不去改 currentPos，避免和正在进行的锚操作抢位置
                if (BlockUtil.getBlock(minePos) == Blocks.RESPAWN_ANCHOR) {
                    return;
                }
                BlockPos[] mineTargets = new BlockPos[]{minePos, minePos.down()};
                for (BlockPos pos : mineTargets) {
                    if (!BlockUtil.canPlace(pos, this.range.getValue(), this.breakCrystal.getValue())) {
                        continue;
                    }
                    CombatUtil.modifyPos = pos;
                    CombatUtil.modifyBlockState = Blocks.OBSIDIAN.getDefaultState();
                    boolean skip = BlockUtil.getClickSideStrict(pos) == null;
                    CombatUtil.modifyPos = null;
                    CombatUtil.modifyBlockState = null;
                    if (skip) {
                        continue;
                    }
                    double mineSelf = this.getAnchorDamage(pos, selfPredict.player, selfPredict.predict);
                    if (mineSelf > this.maxSelfDamage.getValue()) {
                        continue;
                    }
                    if (this.noSuicide.getValue() && mineSelf > (double)(AutoAnchor.mc.player.getHealth() + AutoAnchor.mc.player.getAbsorptionAmount())) {
                        continue;
                    }
                    for (PlayerEntityPredict pap : list) {
                        double mineDamage = this.getAnchorDamage(pos, pap.player, pap.predict);
                        if (mineDamage < this.minDamage.getValue() || this.smart.getValue() && mineSelf > mineDamage) {
                            continue;
                        }
                        this.displayTarget = pap.player;
                        this.tempPos = pos;
                        this.pendingAttempts = this.placeAttempts.getValueInt();
                        break;
                    }
                    if (this.tempPos != null) {
                        break;
                    }
                }
            }
        }

        if (this.tempPos == null) {
            for (BlockPos pos : BlockUtil.getSphere(this.range.getValueFloat() + 1.0f, AutoAnchor.mc.player.getEyePos())) {
                for (PlayerEntityPredict pap : list) {
                    double damage;
                    double selfDamage2;
                    if (BlockUtil.getBlock(pos) != Blocks.RESPAWN_ANCHOR) {
                        if (anchorFound || !BlockUtil.canPlace(pos, this.range.getValue(), this.breakCrystal.getValue())) {
                            continue;
                        }
                        CombatUtil.modifyPos = pos;
                        CombatUtil.modifyBlockState = Blocks.OBSIDIAN.getDefaultState();
                        boolean skip = BlockUtil.getClickSideStrict(pos) == null;
                        CombatUtil.modifyPos = null;
                        if (skip || !((damage = this.getAnchorDamage(pos, pap.player, pap.predict)) >= placeDamage)) {
                            continue;
                        }
                        if ((selfDamage2 = this.getAnchorDamage(pos, selfPredict.player, selfPredict.predict)) > this.maxSelfDamage.getValue()) {
                            continue;
                        }
                        if (this.noSuicide.getValue() && selfDamage2 > (double)(AutoAnchor.mc.player.getHealth() + AutoAnchor.mc.player.getAbsorptionAmount())) {
                            continue;
                        }
                        if (this.smart.getValue() && selfDamage2 > damage) {
                            continue;
                        }
                        this.displayTarget = pap.player;
                        placeDamage = damage;
                        this.tempPos = pos;
                        continue;
                    }
                    double damage2 = this.getAnchorDamage(pos, pap.player, pap.predict);
                    if (BlockUtil.getClickSideStrict(pos) == null || !(damage2 >= breakDamage)) {
                        continue;
                    }
                    if (damage2 >= this.minPrefer.getValue()) {
                        anchorFound = true;
                    }
                    if (!anchorFound && damage2 < placeDamage) {
                        continue;
                    }
                    double selfDamage = this.getAnchorDamage(pos, selfPredict.player, selfPredict.predict);
                    if (selfDamage > this.maxSelfDamage.getValue()) {
                        continue;
                    }
                    if (this.noSuicide.getValue() && selfDamage > (double)(AutoAnchor.mc.player.getHealth() + AutoAnchor.mc.player.getAbsorptionAmount())) {
                        continue;
                    }
                    if (this.smart.getValue() && selfDamage > damage2) {
                        continue;
                    }
                    this.displayTarget = pap.player;
                    breakDamage = damage2;
                    this.tempPos = pos;
                }
            }
        }

        this.currentPos = this.tempPos;
    }

    public double getAnchorDamage(BlockPos anchorPos, PlayerEntity target, PlayerEntity predict) {
        if (this.terrainIgnore.getValue()) {
            CombatUtil.terrainIgnore = true;
        }
        double damage = (double)ExplosionUtil.anchorDamage(anchorPos, (LivingEntity)target, (LivingEntity)predict);
        CombatUtil.terrainIgnore = false;
        return damage;
    }

    public void placeBlock(BlockPos pos, boolean rotate, int slot) {
        // 先登记「放置显示」：这时方块还是空气，PlaceRender 会画成灰红的“试图放置”框；
        // 真放下去了（世界更新后不再是空气）同一格会转成正常的淡出动画。
        // 放在最前面是有意的 —— 就算下面因为没找到贴面 / 空气检测没发包，也照样能看见这次尝试。
        this.placeRender(pos);
        if (BlockUtil.allowAirPlace()) {
            this.airPlace(pos, rotate, slot);
            return;
        }
        Direction side = BlockUtil.getPlaceSide(pos);
        if (side != null) {
            this.clickBlock(pos.offset(side), side.getOpposite(), rotate, slot);
        }
    }

    /** 把这一格交给「放置显示」（PlaceRender）渲染一次尝试/放置 */
    private void placeRender(BlockPos pos) {
        if (pos == null || PlaceRender.INSTANCE == null) {
            return;
        }
        // 直接建条目，不走 BlockUtil.placedPos：那张表还被 AntiCheat 的 multiPlace 判定当作
        // 「刚放过」的依据，塞进去会顺带影响别的模块。placeBlock 都跑在客户端 tick 线程上，
        // 和 PlaceRender 的 renderMap 同一线程，直接写没有并发问题。
        // 第二个参数是「试图放置」框至少保留的毫秒数：锚基本一次就成，世界下一 tick 就
        // 不是空气了，不强制留一会儿的话红框根本看不见。
        PlaceRender.INSTANCE.create(pos, 160L);
    }

    public boolean faceVector(Vec3d directionVec) {
        if (!this.yawStep.getValue()) {
            Arc.ROTATION.lookAt(directionVec);
            return true;
        }
        this.directionVec = directionVec;
        if (Arc.ROTATION.inFov(directionVec, this.fov.getValueFloat())) {
            return true;
        }
        return !this.checkFov.getValue();
    }

    public void clickBlock(BlockPos pos, Direction side, boolean rotate, int slot) {
        if (pos == null) {
            return;
        }
        Vec3d directionVec = new Vec3d((double)pos.getX() + 0.5 + (double)side.getVector().getX() * 0.5, (double)pos.getY() + 0.5 + (double)side.getVector().getY() * 0.5, (double)pos.getZ() + 0.5 + (double)side.getVector().getZ() * 0.5);
        if (rotate && !this.faceVector(directionVec)) {
            return;
        }
        this.doSwap(slot);
        EntityUtil.swingHand(Hand.MAIN_HAND, this.swingMode.getValue());
        BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
        if (BlockUtil.allowAirPlace() || !AutoAnchor.mc.world.getBlockState(pos).isAir() && AutoAnchor.mc.world.getBlockState(pos).getFluidState().isEmpty()) {
            int attempts = Math.max(1, this.pendingAttempts);
            this.pendingAttempts = 0;
            for (int attempt = 0; attempt < attempts; attempt++) {
                Module.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, id));
            }
            if (this.inventorySwap.getValue()) {
                this.doSwap(slot);
            }
            if (rotate && !this.yawStep.getValue()) {
                Arc.ROTATION.snapBack();
            }
        }
    }

    public void airPlace(BlockPos pos, boolean rotate, int slot) {
        if (pos == null) {
            return;
        }
        this.placeRender(pos);
        Direction side = BlockUtil.getClickSide(pos);
        Vec3d directionVec = new Vec3d((double)pos.getX() + 0.5 + (double)side.getVector().getX() * 0.5, (double)pos.getY() + 0.5 + (double)side.getVector().getY() * 0.5, (double)pos.getZ() + 0.5 + (double)side.getVector().getZ() * 0.5);
        if (rotate && !this.faceVector(directionVec)) {
            return;
        }
        this.doSwap(slot);
        boolean bypass = AirPlace.INSTANCE.grimBypass.getValue();
        if (bypass) {
            AutoAnchor.mc.getNetworkHandler().sendPacket((Packet)new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, new BlockPos(0, 0, 0), Direction.DOWN));
        }
        EntityUtil.swingHand(Hand.MAIN_HAND, this.swingMode.getValue());
        BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
        Module.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(bypass ? Hand.OFF_HAND : Hand.MAIN_HAND, result, id));
        if (bypass) {
            AutoAnchor.mc.getNetworkHandler().sendPacket((Packet)new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, new BlockPos(0, 0, 0), Direction.DOWN));
        }
        if (this.inventorySwap.getValue()) {
            this.doSwap(slot);
        }
        if (rotate && !this.yawStep.getValue()) {
            Arc.ROTATION.snapBack();
        }
    }

    private void doSwap(int slot) {
        if (this.inventorySwap.getValue()) {
            InventoryUtil.inventorySwap(slot, AutoAnchor.mc.player.getInventory().selectedSlot);
        } else {
            InventoryUtil.switchToSlot(slot);
        }
    }

    public void onAssist() {
        BlockPos placePos;
        this.assistPos = null;
        int anchor = this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.RESPAWN_ANCHOR) : InventoryUtil.findBlock(Blocks.RESPAWN_ANCHOR);
        int glowstone = this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.GLOWSTONE) : InventoryUtil.findBlock(Blocks.GLOWSTONE);
        int old = AutoAnchor.mc.player.getInventory().selectedSlot;
        if (anchor == -1) {
            return;
        }
        if (this.obsidian.getValue()) {
            anchor = this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN) : InventoryUtil.findBlock(Blocks.OBSIDIAN);
            if (anchor == -1) {
                return;
            }
        }
        if (glowstone == -1) {
            return;
        }
        if (AutoAnchor.mc.player.isSneaking()) {
            return;
        }
        if (this.usingPause.getValue() && AutoAnchor.mc.player.isUsingItem()) {
            return;
        }
        if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
            return;
        }
        if (!this.assistTimer.passed((long)(this.assistDelay.getValueFloat() * 1000.0f))) {
            return;
        }
        this.assistTimer.reset();
        ArrayList<PlayerEntityPredict> list = new ArrayList<>();
        for (PlayerEntity player : CombatUtil.getEnemies(this.assistRange.getValue())) {
            list.add(new PlayerEntityPredict(player, this.maxMotionY.getValue(), this.predictTicks.getValueInt(), this.simulation.getValueInt(), this.step.getValue(), this.doubleStep.getValue(), this.jump.getValue(), this.inBlockPause.getValue()));
        }
        double bestDamage = this.assistDamage.getValue();
        for (PlayerEntityPredict pap : list) {
            double damage;
            double damage2;
            BlockPos pos = EntityUtil.getEntityPos((Entity)pap.player, true).up(2);
            if (AutoAnchor.mc.world.getBlockState(pos).getBlock() == Blocks.RESPAWN_ANCHOR) {
                return;
            }
            if (BlockUtil.clientCanPlace(pos, false) && (damage = this.getAnchorDamage(pos, pap.player, pap.predict)) >= bestDamage) {
                bestDamage = damage;
                this.assistPos = pos;
            }
            for (Direction i : Direction.values()) {
                if (i == Direction.UP || i == Direction.DOWN || !BlockUtil.clientCanPlace(pos.offset(i), false)) {
                    continue;
                }
                if (!((damage2 = this.getAnchorDamage(pos.offset(i), pap.player, pap.predict)) >= bestDamage)) {
                    continue;
                }
                bestDamage = damage2;
                this.assistPos = pos.offset(i);
            }
        }
        if (this.assistPos != null && BlockUtil.getPlaceSide(this.assistPos, this.range.getValue()) == null && (placePos = this.getHelper(this.assistPos)) != null) {
            // 辅助补位这一步也要出「放置显示」：目标格是 assistPos（锚将来要放的地方），
            // 真正垫的是 placePos 那块黑曜石，两个都登记，谁先落地谁走淡出
            this.placeRender(this.assistPos);
            this.placeRender(placePos);
            this.doSwap(anchor);
            BlockUtil.placeBlock(placePos, this.rotate.getValue());
            if (this.inventorySwap.getValue()) {
                this.doSwap(anchor);
            } else {
                this.doSwap(old);
            }
        }
    }

    public BlockPos getHelper(BlockPos pos) {
        for (Direction i : Direction.values()) {
            if (this.checkMine.getValue() && Arc.BREAK.isMining(pos.offset(i))) {
                continue;
            }
            if (!BlockUtil.isStrictDirection(pos.offset(i), i.getOpposite()) || !BlockUtil.canPlace(pos.offset(i))) {
                continue;
            }
            return pos.offset(i);
        }
        return null;
    }

    public static boolean shouldReserveAnchors() {
        if (AutoAnchor.mc.player == null || AutoAnchor.mc.world == null) {
            return false;
        }
        // 原版：不在下界就直接返回 false（主世界/末地里锚炸不出伤害，不用占着背包位）
        if (AutoAnchor.mc.world.getRegistryKey() != net.minecraft.world.World.NETHER) {
            return false;
        }
        if (INSTANCE == null || !INSTANCE.isOn()) {
            return false;
        }
        boolean anchor = InventoryUtil.findItem(net.minecraft.item.Items.RESPAWN_ANCHOR) != -1
                || InventoryUtil.findItemInventorySlot(net.minecraft.item.Items.RESPAWN_ANCHOR) != -1;
        if (!anchor) {
            return false;
        }
        return InventoryUtil.findItem(net.minecraft.item.Items.GLOWSTONE) != -1
                || InventoryUtil.findItemInventorySlot(net.minecraft.item.Items.GLOWSTONE) != -1;
    }

    public enum Page {
        General,
        Interact,
        Predict,
        Rotate,
        Assist,
        Render;

    }

    public class AnchorRender {
        @EventListener
        public void onRender3D(Render3DEvent event) {
            BlockPos currentPos = AutoAnchor.INSTANCE.currentPos;
            if (currentPos != null) {
                AutoAnchor.this.noPosTimer.reset();
                placeVec3d = currentPos.toCenterPos();
            }
            if (placeVec3d == null) {
                return;
            }
            AutoAnchor.this.fade = AutoAnchor.this.fadeSpeed.getValue() >= 1.0
                    ? (AutoAnchor.this.noPosTimer.passed((long)(AutoAnchor.this.startFadeTime.getValue() * 1000.0)) ? 0.0 : 0.5)
                    : AnimateUtil.animate(AutoAnchor.this.fade, AutoAnchor.this.noPosTimer.passed((long)(AutoAnchor.this.startFadeTime.getValue() * 1000.0)) ? 0.0 : 0.5, AutoAnchor.this.fadeSpeed.getValue() / 10.0);
            if (AutoAnchor.this.fade == 0.0) {
                curVec3d = null;
                return;
            }
            curVec3d = curVec3d == null || AutoAnchor.this.sliderSpeed.getValue() >= 1.0
                    ? placeVec3d
                    : new Vec3d(AnimateUtil.animate(AutoAnchor.curVec3d.x, AutoAnchor.placeVec3d.x, AutoAnchor.this.sliderSpeed.getValue() / 10.0),
                            AnimateUtil.animate(AutoAnchor.curVec3d.y, AutoAnchor.placeVec3d.y, AutoAnchor.this.sliderSpeed.getValue() / 10.0),
                            AnimateUtil.animate(AutoAnchor.curVec3d.z, AutoAnchor.placeVec3d.z, AutoAnchor.this.sliderSpeed.getValue() / 10.0));
            if (AutoAnchor.this.render.getValue()) {
                Box cbox = new Box(curVec3d, curVec3d);
                cbox = AutoAnchor.this.shrink.getValue() ? cbox.expand(AutoAnchor.this.fade) : cbox.expand(0.5);
                if (AutoAnchor.this.fill.booleanValue) {
                    event.drawFill(cbox, ColorUtil.injectAlpha(AutoAnchor.this.fill.getValue(), (int)((double)AutoAnchor.this.fill.getValue().getAlpha() * AutoAnchor.this.fade * 2.0)));
                }
                if (AutoAnchor.this.box.booleanValue) {
                    event.drawBox(cbox, ColorUtil.injectAlpha(AutoAnchor.this.box.getValue(), (int)((double)AutoAnchor.this.box.getValue().getAlpha() * AutoAnchor.this.fade * 2.0)));
                }
            }
        }
    }
}
