package dev.G255.Lumi.mod.modules.impl.combat;

import dev.G255.Lumi.Lumi;
import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.ClientTickEvent;
import dev.G255.Lumi.api.events.impl.EntityVelocityUpdateEvent;
import dev.G255.Lumi.api.events.impl.MoveEvent;
import dev.G255.Lumi.api.events.impl.PacketEvent;
import dev.G255.Lumi.api.events.impl.TravelEvent;
import dev.G255.Lumi.api.events.impl.UpdateRotateEvent;
import dev.G255.Lumi.api.utils.combat.CombatUtil;
import dev.G255.Lumi.api.utils.math.PredictUtil;
import dev.G255.Lumi.api.utils.math.Timer;
import dev.G255.Lumi.api.utils.path.PathUtils;
import dev.G255.Lumi.api.utils.path.TPUtils;
import dev.G255.Lumi.api.utils.path.Vec3;
import dev.G255.Lumi.api.utils.player.InventoryUtil;
import dev.G255.Lumi.api.utils.player.MovementUtil;
import dev.G255.Lumi.api.utils.render.Render3DUtil;
import dev.G255.Lumi.core.impl.RotationManager;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.impl.combat.Criticals;

import dev.G255.Lumi.mod.modules.impl.movement.ElytraFlight;
import dev.G255.Lumi.mod.modules.impl.player.InteractTweaks;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.EnumSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.world.World;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.client.util.math.MatrixStack;

public class TimerMace
extends Module {
    public static TimerMace INSTANCE;
    private final EnumSetting<Mode> mode = this.add(new EnumSetting("Mode", Mode.LagBack));
    private final SliderSetting delay = this.add(new SliderSetting("Delay", 500, 0, 10000));
    private final SliderSetting packetFallHeight = this.add(new SliderSetting("FallHeight", 4.0, 0.5, 5.0, 0.1, () -> this.mode.is(Mode.Packet)));
    private final SliderSetting packetSegments = this.add(new SliderSetting("Segments", 3, 2, 10, () -> this.mode.is(Mode.Packet)));
    private final SliderSetting packetResetDistance = this.add(new SliderSetting("ResetDistance", 0.1, 0.05, 0.5, 0.05, () -> this.mode.is(Mode.Packet)));
    private final BooleanSetting packetRequireMace = this.add(new BooleanSetting("RequireMace", false, () -> this.mode.is(Mode.Packet)));
    private final BooleanSetting packetCameraLock = this.add(new BooleanSetting("CameraLock", true, () -> this.mode.is(Mode.Packet)));
    private final SliderSetting lagBackRange = this.add(new SliderSetting("LagBackRange", 3.5, 0.0, 10.0, 0.1, () -> this.mode.is(Mode.LagBack)));
    private final BooleanSetting doubleAttack = this.add(new BooleanSetting("DoubleAttack", false, () -> this.mode.is(Mode.LagBack)));
    private final SliderSetting doubleTime = this.add(new SliderSetting("DoubleTime", 500, 0, 1000, () -> this.mode.is(Mode.LagBack)));
    private final BooleanSetting timerBoost = this.add(new BooleanSetting("TimerBoost", true, () -> this.mode.is(Mode.LagBack)));
    private final SliderSetting firstFallTimer = this.add(new SliderSetting("FirstFallTimer", 1.0, 0.1, 10.0, 0.1, () -> this.mode.is(Mode.LagBack) && this.timerBoost.getValue()));
    private final SliderSetting normalTimer = this.add(new SliderSetting("NormalTimer", 1.0, 0.1, 10.0, 0.1, () -> this.mode.is(Mode.LagBack) && this.timerBoost.getValue()));
    private final BooleanSetting recoverFirstTimer = this.add(new BooleanSetting("RecoverFirstTimer", false, () -> this.mode.is(Mode.LagBack)));
    private final BooleanSetting walkToTarget = this.add(new BooleanSetting("WalkToTarget", false, () -> this.mode.is(Mode.LagBack)));
    private final SliderSetting walkSpeed = this.add(new SliderSetting("WalkSpeed", 0.2, 0.05, 0.5, 0.01, () -> this.mode.is(Mode.LagBack) && this.walkToTarget.getValue()));
    private final BooleanSetting stopAboveTarget = this.add(new BooleanSetting("StopAboveTarget", true, () -> this.mode.is(Mode.Firework)));
    private final BooleanSetting elytraChase = this.add(new BooleanSetting("ElytraChase", false, () -> this.mode.is(Mode.Firework)));
    private final SliderSetting chaseSpeed = this.add(new SliderSetting("ChaseSpeed", 1.0, 0.1, 5.0, 0.1, () -> this.mode.is(Mode.Firework) && this.elytraChase.getValue()));
    private final SliderSetting chaseRange = this.add(new SliderSetting("ChaseRange", 5.0, 0.0, 10.0, 0.1, () -> this.mode.is(Mode.Firework) && this.elytraChase.getValue()));
    private final BooleanSetting autoStopFly = this.add(new BooleanSetting("AutoStopFly", false, () -> this.mode.is(Mode.Firework) && (this.elytraChase.getValue() || this.stopAboveTarget.getValue())));
    private final SliderSetting reEnableDelay = this.add(new SliderSetting("ReEnableDelay", 1000, 0, 10000, () -> this.mode.is(Mode.Firework) && (this.elytraChase.getValue() || this.stopAboveTarget.getValue())));
    private final Timer reEnableTimer = new Timer();
    private boolean pendingReEnable = false;
    private boolean autoReEnabling = false;
    private final SliderSetting minHeight = this.add(new SliderSetting("MinHeight", 20, 10, 200, () -> this.mode.is(Mode.Firework)));
    private final SliderSetting attackRange = this.add(new SliderSetting("AttackRange", 5, 1, 6, () -> this.mode.is(Mode.Firework)));
    private final SliderSetting fireworkDelay = this.add(new SliderSetting("FireworkDelay", 3000, 5, 10000, () -> this.mode.is(Mode.Firework)));
    private final SliderSetting attackDelay = this.add(new SliderSetting("AttackDelay", 1000, 0, 1000, () -> this.mode.is(Mode.Firework)));
    private final SliderSetting takeOffDelay = this.add(new SliderSetting("TakeOffDelay", 2000, 0, 10000, () -> this.mode.is(Mode.Firework)));
    private final SliderSetting elytraSpeed = this.add(new SliderSetting("ElytraSpeed", 0.8101144433021545, 0.01, 10.0, 0.1, () -> this.mode.is(Mode.Firework)));
    private final SliderSetting targetRange = this.add(new SliderSetting("TargetRange", 100, 10, 200, () -> this.mode.is(Mode.Firework)));
    private final SliderSetting flySpeed = this.add(new SliderSetting("FlySpeed", 4.0, 0.0, 10.0, 0.1, () -> this.mode.is(Mode.Fly)));
    private final SliderSetting trackHeight = this.add(new SliderSetting("TrackHeight", 20, 0, 100, () -> this.mode.is(Mode.Fly) || this.mode.is(Mode.ElytraBack)));
    private final SliderSetting trackRange = this.add(new SliderSetting("TrackRange", 5, 0, 10, () -> this.mode.is(Mode.Fly) || this.mode.is(Mode.ElytraBack)));
    private final SliderSetting predictTicks = this.add(new SliderSetting("PredictTicks", 3, 0, 10, () -> this.mode.is(Mode.Fly)));
    private final BooleanSetting inventorySwap = this.add(new BooleanSetting("InventorySwap", true));
    private final BooleanSetting disableInteract = this.add(new BooleanSetting("DisableInteract", true));
    private final Timer delayTimer = new Timer();
    private FireworkStage fireworkStage = FireworkStage.PREPARE;
    private final Timer fireworkTimer = new Timer();
    private final Timer attackTimer = new Timer();
    private final Timer takeOffTimer = new Timer();
    private BlockPos startPos = null;
    private LivingEntity target = null;
    private Vec3d directionVec = null;
    boolean prev;
    float prevPitch;
    float prevYaw;
    private ArrayList<Vec3> path = new ArrayList();
    private int pathIndex = 0;
    private boolean elytra = false;
    private WindChargeStage windChargeStage = WindChargeStage.PEARL;
    private final Timer doubleTimer = new Timer();
    private boolean isDoubleAttack;
    private Vec3d lagBackPos;
    private double fallDistance;
    private PlayerEntity lastAttack;
    private boolean firstLagConsumed;
    private boolean timerActive;
    private boolean interactDisabledByMe = false;
    private FlyStage flyStage = FlyStage.TRACK;
    private double packetGroundY;
    private double packetCameraX;
    private double packetCameraY;
    private double packetCameraZ;

    public TimerMace() {
        super("TimerMace", "高速拉回锤：刷 fallDistance 打满额重锤加伤。", Module.Category.Mace);
        INSTANCE = this;
        Lumi.EVENT_BUS.subscribe(new ChaseReEnableTick(this));
    }

    public String getInfo() {
        return switch (this.mode.getValue()) {
            case WindChargePearl -> this.windChargeStage.name();
            case Firework -> this.fireworkStage.name();
            case LagBack -> String.valueOf((int) this.fallDistance);
            case Fly -> this.flyStage.name();
            case Packet -> this.packetFallHeight.getValueInt() + "x";
            default -> null;
        };
    }

    @EventListener
    private void onRotate(UpdateRotateEvent event) {
        if (this.directionVec != null) {
            float[] rotation = RotationManager.getRotation((Vec3d)this.directionVec);
            event.setYaw(rotation[0]);
            event.setPitch(rotation[1]);
        }
    }

    @EventListener
    private void onTravel(TravelEvent event) {
        if (this.directionVec != null) {
            float[] rotation = RotationManager.getRotation((Vec3d)this.directionVec);
            if (event.isPre()) {
                this.prev = true;
                this.prevPitch = TimerMace.mc.player.getPitch();
                this.prevYaw = TimerMace.mc.player.getYaw();
                TimerMace.mc.player.setYaw(rotation[0]);
                TimerMace.mc.player.setPitch(rotation[1]);
            } else if (this.prev) {
                this.prev = false;
                TimerMace.mc.player.setYaw(this.prevYaw);
                TimerMace.mc.player.setPitch(this.prevPitch);
            }
        }
    }

    @EventListener
    private void onMove(MoveEvent event) {
        PlayerEntity target;
        if (this.isDoubleAttack) {
            event.setY(0.0);
        }
        if (this.mode.is(Mode.LagBack) && this.walkToTarget.getValue() && !TimerMace.mc.player.isFallFlying() && TimerMace.mc.player.getVelocity().y < 0.0 && (target = CombatUtil.getClosestEnemy((double)128.0)) != null) {
            double dx = target.getX() - TimerMace.mc.player.getX();
            double dz = target.getZ() - TimerMace.mc.player.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (this.stopAboveTarget.getValue() && dist < 3.0) {
                return;
            }
            Vec3d dir = new Vec3d(dx, 0.0, dz).normalize();
            event.setX(dir.x * this.walkSpeed.getValue());
            event.setZ(dir.z * this.walkSpeed.getValue());
        }
    }

    @EventListener
    private void onChaseTravel(TravelEvent event) {
        double dz;
        if (event.isPost() || TimerMace.nullCheck() || !this.elytraChase.getValue()) {
            return;
        }
        if (!ElytraFlight.INSTANCE.isOn() || !ElytraFlight.INSTANCE.isFallFlying()) {
            return;
        }
        if (this.mode.is(Mode.Fly) || this.mode.is(Mode.Firework)) {
            return;
        }
        PlayerEntity target = CombatUtil.getClosestEnemy((double)this.chaseRange.getValue());
        if (target == null) {
            return;
        }
        double dx = target.getX() - TimerMace.mc.player.getX();
        double dist = Math.sqrt(dx * dx + (dz = target.getZ() - TimerMace.mc.player.getZ()) * dz);
        if (dist < 1.0E-4) {
            return;
        }
        if (this.autoStopFly.getValue() && dist < 3.0) {
            ElytraFlight.INSTANCE.disable();
            this.disable();
            this.reEnableTimer.reset();
            this.pendingReEnable = true;
            return;
        }
        double speed = this.chaseSpeed.getValue();
        MovementUtil.setMotionX((double)(dx / dist * speed));
        MovementUtil.setMotionZ((double)(dz / dist * speed));
        event.cancel();
        TimerMace.mc.player.move(MovementType.SELF, TimerMace.mc.player.getVelocity());
    }

    @EventListener
    private void onFly(MoveEvent event) {
        Vec3d goal;
        if (!this.mode.is(Mode.Fly)) {
            return;
        }
        this.target = CombatUtil.getClosestEnemy((double)128.0);
        if (this.target == null) {
            this.flyStage = FlyStage.BYPASS;
            BlockPos highPos = TimerMace.mc.player.getBlockPos();
            while (TimerMace.mc.world.getBlockState(highPos).getBlock() != Blocks.AIR && highPos.getY() > TimerMace.mc.world.getBottomY()) {
                highPos = highPos.down();
            }
            goal = new Vec3d(TimerMace.mc.player.getX(), (double)highPos.getY() + 0.5, TimerMace.mc.player.getZ());
        } else {
            Vec3d predict = PredictUtil.getPos((PlayerEntity)this.target, (int)this.predictTicks.getValueInt());
            double d1 = Math.abs(this.target.getX() - TimerMace.mc.player.getX());
            double d2 = Math.abs(this.target.getZ() - TimerMace.mc.player.getZ());
            switch (this.flyStage) {
                case TRACK: {
                    goal = predict.add(0.0, this.trackHeight.getValue(), 0.0);
                    if (!(d1 < this.trackRange.getValue()) || !(d2 < this.trackRange.getValue()) || !(TimerMace.mc.player.getY() - this.target.getY() >= this.trackHeight.getValue())) break;
                    this.flyStage = FlyStage.ATTACK;
                    break;
                }
                case ATTACK: {
                    goal = predict.add(0.0, -3.0, 0.0);
                    if (!(d1 > this.trackRange.getValue()) && !(d2 > this.trackRange.getValue()) && !(TimerMace.mc.player.getY() - predict.getY() < 1.5)) break;
                    this.flyStage = FlyStage.TRACK;
                    break;
                }
                default: {
                    this.flyStage = FlyStage.TRACK;
                    goal = predict.add(0.0, this.trackHeight.getValue(), 0.0);
                }
            }
        }
        Vec3d motion = goal.subtract(TimerMace.mc.player.getPos()).normalize().multiply(this.flySpeed.getValue());
        event.setX(motion.getX());
        event.setY(motion.getY());
        event.setZ(motion.getZ());
    }

    public void onRender3D(MatrixStack matrixStack) {
        if (this.path == null || this.path.isEmpty()) {
            return;
        }
        Iterator<Vec3> iterator = this.path.iterator();
        Vec3 node = iterator.next();
        while (iterator.hasNext()) {
            Vec3 s = iterator.next();
            Render3DUtil.drawLine((Vec3d)node.mc(), (Vec3d)s.mc(), (Color)Color.RED);
            node = s;
        }
    }

    public void onEnable() {
        if (TimerMace.nullCheck()) {
            return;
        }
        if (this.disableInteract.getValue() && InteractTweaks.INSTANCE != null && InteractTweaks.INSTANCE.isOn()) {
            InteractTweaks.INSTANCE.disable();
            this.interactDisabledByMe = true;
        }
        this.windChargeStage = WindChargeStage.PEARL;
        this.fireworkStage = FireworkStage.PREPARE;
        this.flyStage = FlyStage.BYPASS;
        this.packetGroundY = this.findPacketGroundY();
        this.packetCameraX = TimerMace.mc.player.getX();
        this.packetCameraY = TimerMace.mc.player.getY() + (double)TimerMace.mc.player.getEyeHeight(TimerMace.mc.player.getPose());
        this.packetCameraZ = TimerMace.mc.player.getZ();
        this.fallDistance = 0.0;
        this.isDoubleAttack = false;
        this.lagBackPos = TimerMace.mc.player.getPos();
        this.directionVec = null;
        this.target = null;
        this.path.clear();
        this.pathIndex = 0;
        this.elytra = false;
        this.firstLagConsumed = false;
        this.timerActive = false;
        if (this.mode.is(Mode.LagBack)) {
            if (!this.autoReEnabling) {
                this.doLagBack();
            }
            this.firstLagConsumed = false;
        }
    }

    public void onDisable() {
        if (this.interactDisabledByMe && InteractTweaks.INSTANCE != null && !InteractTweaks.INSTANCE.isOn()) {
            InteractTweaks.INSTANCE.enable();
            this.interactDisabledByMe = false;
        }
        this.directionVec = null;
        this.target = null;
        this.isDoubleAttack = false;
        this.resetTimer();
        this.firstLagConsumed = false;
    }

    @EventListener
    private void onTick(ClientTickEvent event) {
        if (event.isPost()) {
            return;
        }
        if (TimerMace.nullCheck()) {
            this.resetTimer();
            return;
        }
        if (!this.mode.is(Mode.LagBack)) {
            this.resetTimer();
        }
        switch (this.mode.getValue()) {
            case Firework: {
                this.doFireworkMode();
                break;
            }
            case WindChargePearl: {
                this.doWindChargeMode();
                break;
            }
            case LagBack: {
                this.doLagBackMode();
                break;
            }
            case ElytraBack: {
                this.doElytraBackMode();
                break;
            }
            case TP: {
                this.doTpMode();
                break;
            }
        }
    }

    @EventListener
    private void onPacketModeTick(ClientTickEvent event) {
        if (!this.mode.is(Mode.Packet) || TimerMace.nullCheck() || !event.isPre()) {
            return;
        }
        if (this.packetRequireMace.getValue() && TimerMace.mc.player.getMainHandStack().getItem() != Items.MACE) {
            return;
        }
        if (TimerMace.mc.player.getY() <= this.packetGroundY + this.packetResetDistance.getValue()) {
            this.teleportPacketUpInSegments();
            TimerMace.mc.player.setVelocity(TimerMace.mc.player.getVelocity().x, 0.0, TimerMace.mc.player.getVelocity().z);
        }
    }

    @EventListener
    private void onAttack(PacketEvent.Sent event) {
        if (!TimerMace.nullCheck() && event.getPacket() instanceof PlayerInteractEntityC2SPacket packet) {
            if (Criticals.getInteractType(packet) == PlayerInteractEntityC2SPacket.InteractType.ATTACK) {
                Entity entity = Criticals.getEntity(packet);
                if (entity instanceof PlayerEntity player) {
                    this.lastAttack = player;
                }
            }
        }
    }

    @EventListener
    private void onHurt(PacketEvent.Receive event) {
        if (!TimerMace.nullCheck() && this.lastAttack != null && event.getPacket() instanceof EntityStatusS2CPacket packet) {
            if (packet.getStatus() == 4 && packet.getEntity(TimerMace.mc.world) == this.lastAttack) {
                this.lastAttack = null;
                if (this.mode.is(Mode.LagBack)) {
                    this.doLagBack();
                }
            }
        }
    }

    private void doTpMode() {
        PlayerEntity target = CombatUtil.getClosestEnemy((double)10000.0);
        if (target == null) {
            return;
        }
        if (TimerMace.mc.player.getY() < target.getY() + 1.5) {
            return;
        }
        if (!this.delayTimer.passedMs(this.delay.getValue())) {
            return;
        }
        this.delayTimer.reset();
        Vec3d newPos = target.getPos();
        TPUtils.newTeleport((Vec3d)newPos);
        TimerMace.mc.player.setVelocity(TimerMace.mc.player.getVelocity().getX(), 0.0, TimerMace.mc.player.getVelocity().getZ());
        TimerMace.mc.player.setPosition(newPos.getX(), newPos.getY(), newPos.getZ());
    }

    private void teleportPacketUpInSegments() {
        double x = TimerMace.mc.player.getX();
        double startY = TimerMace.mc.player.getY();
        double z = TimerMace.mc.player.getZ();
        double segmentHeight = this.packetFallHeight.getValue();
        int segmentCount = this.packetSegments.getValueInt();
        double targetY = startY + segmentHeight * (double)segmentCount;
        for (int step = 1; step <= segmentCount; ++step) {
            double packetY = startY + segmentHeight * (double)step;
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, packetY, z, false));
        }
        TimerMace.mc.player.setPosition(x, targetY, z);
    }

    private double findPacketGroundY() {
        BlockPos pos = TimerMace.mc.player.getBlockPos();
        int worldBottom = TimerMace.mc.world.getBottomY();
        while (pos.getY() > worldBottom) {
            BlockPos below = pos.down();
            if (TimerMace.mc.world.getBlockState(below).blocksMovement()) {
                return (double)below.getY() + 1.0;
            }
            pos = below;
        }
        return TimerMace.mc.player.getY();
    }

    public boolean shouldLockPacketCamera() {
        return this.isOn() && this.mode.is(Mode.Packet) && this.packetCameraLock.getValue() && !TimerMace.nullCheck() && TimerMace.mc.options.getPerspective().isFirstPerson();
    }

    public double getPacketCameraY() {
        return this.packetCameraY;
    }

    public double getPacketCameraX() {
        return this.packetCameraX;
    }

    public double getPacketCameraZ() {
        return this.packetCameraZ;
    }

    public boolean canPacketAttackFromLockedCamera(Entity target) {
        if (TimerMace.mc.player.getMainHandStack().getItem() != Items.MACE) {
            return true;
        }
        return TimerMace.mc.player.getVelocity().y < 0.0 && TimerMace.mc.player.fallDistance > 1.5f && TimerMace.mc.player.squaredDistanceTo(target) < 36.0;
    }

    public void updatePacketGround() {
        this.packetGroundY = this.findPacketGroundY();
    }

    private void doElytraBackMode() {
        PlayerEntity target = CombatUtil.getClosestEnemy((double)10000.0);
        if (target == null) {
            return;
        }
        if (Math.abs(target.getX() - TimerMace.mc.player.getX()) > this.trackRange.getValue() || Math.abs(target.getZ() - TimerMace.mc.player.getZ()) > this.trackRange.getValue()) {
            return;
        }
        if (TimerMace.mc.player.getY() - target.getY() < this.trackHeight.getValue()) {
            return;
        }
        if (!this.delayTimer.passedMs(this.delay.getValue())) {
            return;
        }
        this.delayTimer.reset();
        Vec3d to = new Vec3d(TimerMace.mc.player.getX(), target.getY() + 3.5, TimerMace.mc.player.getZ());
        TimerMace.mc.player.setVelocity(TimerMace.mc.player.getVelocity().x, -0.2, TimerMace.mc.player.getVelocity().z);
        if ((double)TimerMace.mc.player.fallDistance > 0.3 && !TimerMace.mc.player.isFallFlying()) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(TimerMace.mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            TimerMace.mc.player.startFallFlying();
        }
        TPUtils.newTeleport((Vec3d)to);
        TimerMace.mc.player.setPosition(to.x, to.y, to.z);
    }

    private void doLagBackMode() {
        this.handleLagBackTimer();
        BlockPos bottom = TimerMace.mc.player.getBlockPos();
        while (TimerMace.mc.world.isAir(bottom) && bottom.getY() > -64) {
            bottom = bottom.down();
        }
        this.fallDistance = this.lagBackPos.getY() - (double)bottom.getY() - 0.5;
        if (ElytraFlight.INSTANCE.isOn() || this.fallDistance < this.lagBackRange.getValue()) {
            this.isDoubleAttack = false;
            this.lagBackPos = TimerMace.mc.player.getPos();
            return;
        }
        if (TimerMace.mc.player.getY() - bottom.toCenterPos().getY() - 0.5 > this.lagBackRange.getValue()) {
            this.isDoubleAttack = false;
            this.doubleTimer.reset();
            return;
        }
        if (this.doubleAttack.getValue() && !this.doubleTimer.passedMs(this.doubleTime.getValue())) {
            this.isDoubleAttack = true;
            return;
        }
        this.doLagBack();
    }

    @EventListener
    private void onEntityVelocityUpdate(EntityVelocityUpdateEvent event) {
        if (this.mode.is(Mode.LagBack) && event.getEntity() == TimerMace.mc.player && event.getY() > 0.0) {
            if (this.recoverFirstTimer.getValue() && this.fallDistance < this.lagBackRange.getValue()) {
                this.firstLagConsumed = false;
                return;
            }
            this.doLagBack();
        }
    }

    private void doLagBack() {
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(TimerMace.mc.player.getX(), 10000.0, TimerMace.mc.player.getZ(), false));
        this.firstLagConsumed = true;
    }

    private void handleLagBackTimer() {
        if (!this.timerBoost.getValue() || ElytraFlight.INSTANCE.isOn() || TimerMace.mc.player.isFallFlying()) {
            this.resetTimer();
            return;
        }
        Lumi.TIMER.set(this.getTimerValue());
        this.timerActive = true;
    }

    private void resetTimer() {
        if (this.timerActive) {
            Lumi.TIMER.reset();
            this.timerActive = false;
        }
    }

    private float getTimerValue() {
        return !this.firstLagConsumed ? this.firstFallTimer.getValueFloat() : this.normalTimer.getValueFloat();
    }

    private void doWindChargeMode() {
        if (!this.delayTimer.passedMs(this.delay.getValue())) {
            return;
        }
        this.delayTimer.reset();
        switch (this.windChargeStage) {
            case PEARL: {
                this.doPearl();
                break;
            }
            case WINDBRUST: {
                this.doWindCharge(Direction.UP);
                break;
            }
            case STOP: {
                this.disable();
                break;
            }
        }
    }

    public void doPearl() {
        int pearl;
        int n = pearl = this.inventorySwap.getValue() ? InventoryUtil.findItemInventorySlot((Item)Items.ENDER_PEARL) : InventoryUtil.findItem((Item)Items.ENDER_PEARL);
        if (pearl == -1) {
            this.sendMessage("§cNo pearls");
            this.disable();
            return;
        }
        Lumi.ROTATION.snapAt(Lumi.ROTATION.lastYaw, -90.0f);
        int select = TimerMace.mc.player.getInventory().selectedSlot;
        if (this.inventorySwap.getValue()) {
            InventoryUtil.inventorySwap((int)pearl, (int)select);
            TimerMace.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Lumi.ROTATION.lastYaw, -90.0f));
            InventoryUtil.inventorySwap((int)pearl, (int)select);
        } else {
            InventoryUtil.switchToSlot((int)pearl);
            TimerMace.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Lumi.ROTATION.lastYaw, -90.0f));
            InventoryUtil.switchToSlot((int)select);
        }
        this.windChargeStage = WindChargeStage.WINDBRUST;
    }

    public void doWindCharge(Direction direction) {
        int windCharge;
        int n = windCharge = this.inventorySwap.getValue() ? InventoryUtil.findItemInventorySlot((Item)Items.WIND_CHARGE) : InventoryUtil.findItem((Item)Items.WIND_CHARGE);
        if (windCharge == -1) {
            this.sendMessage("§cNo wind charges");
            this.disable();
            return;
        }
        float pitch = switch (direction) {
            case UP -> -90;
            case DOWN -> 90;
            default -> 0;
        };
        Lumi.ROTATION.snapAt(Lumi.ROTATION.lastYaw, pitch);
        int select = TimerMace.mc.player.getInventory().selectedSlot;
        if (this.inventorySwap.getValue()) {
            InventoryUtil.inventorySwap((int)windCharge, (int)select);
            TimerMace.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Lumi.ROTATION.lastYaw, pitch));
            InventoryUtil.inventorySwap((int)windCharge, (int)select);
        } else {
            InventoryUtil.switchToSlot((int)windCharge);
            TimerMace.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Lumi.ROTATION.lastYaw, pitch));
            InventoryUtil.switchToSlot((int)select);
        }
        this.windChargeStage = WindChargeStage.STOP;
    }

    private void doFireworkMode() {
        switch (this.fireworkStage) {
            case PREPARE: {
                this.doPrepare();
                break;
            }
            case TAKEOFF: {
                this.doTakeoff();
                break;
            }
            case TRACKING: {
                this.doTracking();
                break;
            }
            case ATTACK: {
                this.doAttack();
                break;
            }
        }
    }

    private void doPrepare() {
        int maceSlot = InventoryUtil.findItem((Item)Items.MACE);
        if (maceSlot == -1) {
            this.sendMessage("§cNo Mace");
            this.disable();
            return;
        }
        int firework = InventoryUtil.findItem((Item)Items.FIREWORK_ROCKET);
        if (firework == -1) {
            this.sendMessage("§cNo Firework Rockets");
            this.disable();
            return;
        }
        this.startPos = TimerMace.mc.player.getBlockPos();
        this.fireworkStage = FireworkStage.TAKEOFF;
        this.takeOffTimer.reset();
    }

    private void doTakeoff() {
        this.swapElytra();
        if (!TimerMace.mc.player.isFallFlying() || !this.takeOffTimer.passedMs(this.takeOffDelay.getValue())) {
            this.directionVec = null;
            this.startFallFlying();
            return;
        }
        if (TimerMace.mc.player.getY() >= (double)this.startPos.getY() + this.minHeight.getValue()) {
            this.fireworkStage = FireworkStage.TRACKING;
            this.directionVec = null;
            return;
        }
        this.directionVec = new Vec3d(TimerMace.mc.player.getX(), TimerMace.mc.player.getY() + 100.0, TimerMace.mc.player.getZ());
        if (!this.fireworkTimer.passedMs(this.fireworkDelay.getValue())) {
            return;
        }
        this.useFirework();
        this.fireworkTimer.reset();
    }

    private void doTracking() {
        double distance;
        if (this.target == null || this.target.isDead() || !this.target.isAlive()) {
            this.target = CombatUtil.getClosestEnemy((double)this.targetRange.getValue());
            if (this.target == null) {
                return;
            }
        }
        if ((distance = (double)TimerMace.mc.player.distanceTo((Entity)this.target)) <= this.attackRange.getValue()) {
            this.attackTimer.reset();
            this.fireworkStage = FireworkStage.ATTACK;
            return;
        }
        if (this.path.isEmpty() || this.pathIndex >= this.path.size()) {
            this.path = this.calculatePathToTarget();
            this.pathIndex = 0;
        }
        if (!this.path.isEmpty()) {
            Vec3 targetPos = this.path.get(this.pathIndex);
            double distToPoint = TimerMace.mc.player.getPos().distanceTo(new Vec3d(targetPos.x(), targetPos.y(), targetPos.z()));
            if (distToPoint < 2.0) {
                ++this.pathIndex;
            }
            this.directionVec = targetPos.mc();
            if (TimerMace.mc.player.isFallFlying() && this.fireworkTimer.passedMs(this.fireworkDelay.getValue())) {
                this.useFirework();
                this.fireworkTimer.reset();
            }
        }
        if (!TimerMace.mc.player.isFallFlying()) {
            this.startFallFlying();
        }
    }

    private void doAttack() {
        this.swapChestplate();
        if (!this.attackTimer.passedMs(this.attackDelay.getValue())) {
            return;
        }
        int maceSlot = InventoryUtil.findItem((Item)Items.MACE);
        if (maceSlot != -1) {
            InventoryUtil.switchToSlot((int)maceSlot);
            if (this.target != null && this.target.isAlive()) {
                this.directionVec = this.target.getPos();
                mc.getNetworkHandler().sendPacket((Packet)PlayerInteractEntityC2SPacket.attack((Entity)this.target, (boolean)TimerMace.mc.player.isSneaking()));
                mc.getNetworkHandler().sendPacket((Packet)PlayerInteractEntityC2SPacket.attack((Entity)this.target, (boolean)TimerMace.mc.player.isSneaking()));
                TimerMace.mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
        this.fireworkStage = FireworkStage.PREPARE;
        this.target = null;
        this.path.clear();
        this.pathIndex = 0;
    }

    private void swapElytra() {
        this.elytra = true;
    }

    private void swapChestplate() {
        this.elytra = false;
    }

    public boolean shouldElytra() {
        return this.isOn() && this.elytra;
    }

    private void startFallFlying() {
        if ((double)TimerMace.mc.player.fallDistance > 0.3) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(TimerMace.mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            TimerMace.mc.player.startFallFlying();
        }
    }

    private void useFirework() {
        int firework = InventoryUtil.findItem((Item)Items.FIREWORK_ROCKET);
        if (firework != -1) {
            int oldSlot = TimerMace.mc.player.getInventory().selectedSlot;
            InventoryUtil.switchToSlot((int)firework);
            TimerMace.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, TimerMace.mc.player.getYaw(), TimerMace.mc.player.getPitch()));
            InventoryUtil.switchToSlot((int)oldSlot);
        }
    }

    private ArrayList<Vec3> calculatePathToTarget() {
        if (this.target == null) {
            return new ArrayList<Vec3>();
        }
        Vec3 start = new Vec3(TimerMace.mc.player.getX(), TimerMace.mc.player.getY(), TimerMace.mc.player.getZ());
        Vec3 end = new Vec3(this.target.getX(), this.target.getY(), this.target.getZ());
        return PathUtils.computePath((Vec3)start, (Vec3)end);
    }

    public static enum Mode {
        WindChargePearl,
        Firework,
        LagBack,
        ElytraBack,
        TP,
        Fly,
        Packet
    }

    public static enum WindChargeStage {
        PEARL,
        WINDBRUST,
        STOP
    }

    public static enum FireworkStage {
        PREPARE,
        TAKEOFF,
        TRACKING,
        ATTACK
    }

    public static enum FlyStage {
        TRACK,
        ATTACK,
        BYPASS
    }

    public static class ChaseReEnableTick {
        private final TimerMace parent;

        public ChaseReEnableTick(TimerMace parent) {
            this.parent = parent;
        }

        @EventListener
        public void onTick(ClientTickEvent event) {
            if (parent.autoReEnabling) {
                parent.autoReEnabling = false;
                ElytraFlight.INSTANCE.enable();
            }
        }
    }
}