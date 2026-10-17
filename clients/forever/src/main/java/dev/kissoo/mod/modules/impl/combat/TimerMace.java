package dev.kissoo.mod.modules.impl.combat;

import dev.kissoo.Forever;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.*;
import dev.kissoo.api.utils.combat.CombatUtil;
import dev.kissoo.api.utils.math.PredictUtil;
import dev.kissoo.api.utils.math.Timer;
import dev.kissoo.api.utils.path.PathUtils;
import dev.kissoo.api.utils.path.TPUtils;
import dev.kissoo.api.utils.path.Vec3;
import dev.kissoo.api.utils.player.InventoryUtil;
import dev.kissoo.api.utils.player.MovementUtil;
import dev.kissoo.api.utils.render.Render3DUtil;
import dev.kissoo.core.impl.RotationManager;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.impl.movement.ElytraFlight;
import dev.kissoo.mod.modules.impl.player.InteractTweaks;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.InteractType;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * TimerMace（高速拉回锤）
 * 移植自 Mio 框架 AutoMace，适配 ForeverClient（dev.kissoo）API。
 *
 * 目标：用「假位置 / 传送 / 鞘翅 + 烟花」把 fallDistance 刷高，让重锤（Mace）打出满额坠落加成。
 * 七种模式：
 *   - WindChargePearl  珍珠 + 风弹上天
 *   - Firework         鞘翅 + 烟花追踪目标
 *   - LagBack          假位置(Y=10000)回弹刷 fallDistance（被动辅助，配合手动攻击）
 *   - ElytraBack       鞘翅回传目标头顶
 *   - TP               直传目标
 *   - Fly              飞行追踪（TRACK/ATTACK/BYPASS）
 *   - Packet           分段假位置循环刷 fallDistance
 */
public class TimerMace extends Module {
    public static TimerMace INSTANCE;

    public TimerMace() {
        super("TimerMace", "高速拉回锤：刷 fallDistance 打满额重锤加伤。", Category.Mace);
        setChinese("高速拉回锤");
        INSTANCE = this;
        // 常驻监听：TimerMace 被 AutoStopFly 禁用后，若仍按住按键，0.5 秒后仅重新开启 TimerMace
        Forever.EVENT_BUS.subscribe(new ChaseReEnableTick());
    }

    enum Mode {
        WindChargePearl,
        Firework,
        LagBack,
        ElytraBack,
        TP,
        Fly,
        Packet
    }

    enum WindChargeStage {
        PEARL,
        WINDBRUST,
        STOP
    }

    enum FireworkStage {
        PREPARE,
        TAKEOFF,
        TRACKING,
        ATTACK
    }

    enum FlyStage {
        TRACK,
        ATTACK,
        BYPASS,
    }

    private final EnumSetting<Mode> mode = add(new EnumSetting<>("Mode", Mode.LagBack));
    private final SliderSetting delay = add(new SliderSetting("Delay", 500, 0, 10000));

    // Packet Mode Settings
    private final SliderSetting packetFallHeight = add(new SliderSetting("FallHeight", 4, 0.5, 5, 0.1, () -> mode.is(Mode.Packet)));
    private final SliderSetting packetSegments = add(new SliderSetting("Segments", 3, 2, 10, () -> mode.is(Mode.Packet)));
    private final SliderSetting packetResetDistance = add(new SliderSetting("ResetDistance", 0.1, 0.05, 0.5, 0.05, () -> mode.is(Mode.Packet)));
    private final BooleanSetting packetRequireMace = add(new BooleanSetting("RequireMace", false, () -> mode.is(Mode.Packet)));
    private final BooleanSetting packetCameraLock = add(new BooleanSetting("CameraLock", true, () -> mode.is(Mode.Packet)));

    // LagBack Mode Settings
    private final SliderSetting lagBackRange = add(new SliderSetting("LagBackRange", 3.5, 0, 10, 0.1, () -> mode.is(Mode.LagBack)));
    private final BooleanSetting doubleAttack = add(new BooleanSetting("DoubleAttack", false, () -> mode.is(Mode.LagBack)));
    private final SliderSetting doubleTime = add(new SliderSetting("DoubleTime", 500, 0, 1000, () -> mode.is(Mode.LagBack)));
    private final BooleanSetting timerBoost = add(new BooleanSetting("TimerBoost", true, () -> mode.is(Mode.LagBack)));
    private final SliderSetting firstFallTimer = add(new SliderSetting("FirstFallTimer", 1.0, 0.1, 10.0, 0.1, () -> mode.is(Mode.LagBack) && timerBoost.getValue()));
    private final SliderSetting normalTimer = add(new SliderSetting("NormalTimer", 2.5, 0.1, 10.0, 0.1, () -> mode.is(Mode.LagBack) && timerBoost.getValue()));
    // 检测到未达 LagBackRange 就发生回弹（异常 lagback）时，自动回到第一次下落的 timer
    private final BooleanSetting recoverFirstTimer = add(new BooleanSetting("RecoverFirstTimer", false, () -> mode.is(Mode.LagBack)));
    private final BooleanSetting walkToTarget = add(new BooleanSetting("WalkToTarget", false, () -> mode.is(Mode.LagBack)));
    private final SliderSetting walkSpeed = add(new SliderSetting("WalkSpeed", 0.2, 0.05, 0.5, 0.01, () -> mode.is(Mode.LagBack) && walkToTarget.getValue()));
    private final BooleanSetting stopAboveTarget = add(new BooleanSetting("StopAboveTarget", false, () -> mode.is(Mode.LagBack) && walkToTarget.getValue()));

    // Elytra Chase: 鞘翅飞行时自动向最近目标追击
    private final BooleanSetting elytraChase = add(new BooleanSetting("ElytraChase", false));
    // 追击速度
    private final SliderSetting chaseSpeed = add(new SliderSetting("ChaseSpeed", 3.0, 0.5, 10, 0.1, () -> elytraChase.getValue()));
    // 追击半径：目标在此范围内才追击
    private final SliderSetting chaseRange = add(new SliderSetting("ChaseRange", 128, 10, 300, 1, () -> elytraChase.getValue()));
    // 到目标正上方后自动关闭鞘翅飞行，让玩家坠落到目标身上
    private final BooleanSetting autoStopFly = add(new BooleanSetting("AutoStopFly", false, () -> elytraChase.getValue()));
    // 禁用后重新开启 TimerMace 的间隔（毫秒）
    private final SliderSetting reEnableDelay = add(new SliderSetting("ReEnableDelay", 500, 0, 5000, () -> elytraChase.getValue() && autoStopFly.getValue()));

    // 到目标正上方禁用后，若仍按住按键，间隔 reEnableDelay 毫秒后仅重新开启 TimerMace（不重新开启鞘翅飞行）
    private final Timer reEnableTimer = new Timer();
    private boolean pendingReEnable = false;
    // 自动重启用时跳过 LagBack 初始假位置包，避免回弹
    private boolean autoReEnabling = false;

    // Firework Mode Settings
    private final SliderSetting minHeight = add(new SliderSetting("MinHeight", 20, 10, 200, () -> mode.is(Mode.Firework)));
    private final SliderSetting attackRange = add(new SliderSetting("AttackRange", 5, 1, 6, () -> mode.is(Mode.Firework)));
    private final SliderSetting fireworkDelay = add(new SliderSetting("FireworkDelay", 3000, 5, 10000, () -> mode.is(Mode.Firework)));
    private final SliderSetting attackDelay = add(new SliderSetting("AttackDelay", 1000, 0, 1000, () -> mode.is(Mode.Firework)));
    private final SliderSetting takeOffDelay = add(new SliderSetting("TakeOffDelay", 2000, 0, 10000, () -> mode.is(Mode.Firework)));
    private final SliderSetting elytraSpeed = add(new SliderSetting("ElytraSpeed", 0.8101144433021545, 0.01, 10, 0.1, () -> mode.is(Mode.Firework)));
    private final SliderSetting targetRange = add(new SliderSetting("TargetRange", 100, 10, 200, () -> mode.is(Mode.Firework)));

    // Fly Mod Settings
    private final SliderSetting flySpeed = add(new SliderSetting("FlySpeed", 4, 0, 10, .1, () -> mode.is(Mode.Fly)));
    private final SliderSetting trackHeight = add(new SliderSetting("TrackHeight", 20, 0, 100, () -> mode.is(Mode.Fly) || mode.is(Mode.ElytraBack)));
    private final SliderSetting trackRange = add(new SliderSetting("TrackRange", 5, 0, 10, () -> mode.is(Mode.Fly) || mode.is(Mode.ElytraBack)));
    private final SliderSetting predictTicks = add(new SliderSetting("PredictTicks", 3, 0, 10, () -> mode.is(Mode.Fly)));

    private final BooleanSetting inventorySwap = add(new BooleanSetting("InventorySwap", true));
    private final BooleanSetting disableInteract = add(new BooleanSetting("DisableInteract", true));

    // Shared
    private final Timer delayTimer = new Timer();

    // Firework
    private FireworkStage fireworkStage = FireworkStage.PREPARE;
    private final Timer fireworkTimer = new Timer();
    private final Timer attackTimer = new Timer();
    private final Timer takeOffTimer = new Timer();
    private BlockPos startPos = null;
    private LivingEntity target = null;
    private Vec3d directionVec = null;
    boolean prev;
    float prevPitch, prevYaw;

    private ArrayList<Vec3> path = new ArrayList<>();
    private int pathIndex = 0;
    private boolean elytra = false;

    // Wind charge
    private WindChargeStage windChargeStage = WindChargeStage.PEARL;

    // Lag back
    private final Timer doubleTimer = new Timer();
    private boolean isDoubleAttack;
    private Vec3d lagBackPos;
    private double fallDistance;
    private PlayerEntity lastAttack;
    private boolean firstLagConsumed;
    private boolean timerActive;
    private boolean interactDisabledByMe = false;

    // Fly
    private FlyStage flyStage = FlyStage.TRACK;

    // Packet
    private double packetGroundY;
    private double packetCameraX;
    private double packetCameraY;
    private double packetCameraZ;

    @Override
    public String getInfo() {
        return switch (mode.getValue()) {
            case WindChargePearl -> windChargeStage.name();
            case Firework -> fireworkStage.name();
            case LagBack -> String.valueOf((int) fallDistance);
            case Fly -> flyStage.name();
            case Packet -> packetFallHeight.getValueInt() + "x" + packetSegments.getValueInt();
            default -> null;
        };
    }

    @EventListener
    private void onRotate(UpdateRotateEvent event) {
        if (directionVec != null) {
            float[] rotation = RotationManager.getRotation(directionVec);
            event.setYaw(rotation[0]);
            event.setPitch(rotation[1]);
        }
    }

    @EventListener
    private void onTravel(TravelEvent event) {
        if (directionVec != null) {
            float[] rotation = RotationManager.getRotation(directionVec);
            if (event.isPre()) {
                this.prev = true;
                this.prevPitch = mc.player.getPitch();
                this.prevYaw = mc.player.getYaw();
                mc.player.setYaw(rotation[0]);
                mc.player.setPitch(rotation[1]);
            } else if (this.prev) {
                this.prev = false;
                mc.player.setYaw(prevYaw);
                mc.player.setPitch(prevPitch);
            }
        }
    }

    @EventListener
    private void onMove(MoveEvent event) {
        if (isDoubleAttack) {
            event.setY(0);
        }

        if (mode.is(Mode.LagBack) && walkToTarget.getValue() && !mc.player.isFallFlying() && mc.player.getVelocity().y < 0) {
            PlayerEntity target = CombatUtil.getClosestEnemy(128);
            if (target != null) {
                double dx = target.getX() - mc.player.getX();
                double dz = target.getZ() - mc.player.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);
                // 到目标上方后停止追踪
                if (stopAboveTarget.getValue() && dist < 3.0) {
                    return;
                }
                Vec3d dir = new Vec3d(dx, 0, dz).normalize();
                event.setX(dir.x * walkSpeed.getValue());
                event.setZ(dir.z * walkSpeed.getValue());
            }
        }
    }

    // Elytra Chase: 鞘翅飞行模块开启时自动向最近目标追击
    @EventListener
    private void onChaseTravel(TravelEvent event) {
        if (event.isPost() || nullCheck() || !elytraChase.getValue()) {
            return;
        }
        // 仅在鞘翅飞行模块开启且实际飞行中追击
        if (!ElytraFlight.INSTANCE.isOn() || !ElytraFlight.INSTANCE.isFallFlying()) {
            return;
        }
        // Fly / Firework 模式有自己的飞行逻辑，避免互相抢
        if (mode.is(Mode.Fly) || mode.is(Mode.Firework)) {
            return;
        }

        PlayerEntity target = CombatUtil.getClosestEnemy(chaseRange.getValue());
        if (target == null) {
            return;
        }

        double dx = target.getX() - mc.player.getX();
        double dz = target.getZ() - mc.player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 1.0E-4) {
            return;
        }

        // 到目标正上方后自动禁用鞘翅飞行模块，并同时禁用 TimerMace
        if (autoStopFly.getValue() && dist < 3.0) {
            ElytraFlight.INSTANCE.disable();
            disable();
            reEnableTimer.reset();
            pendingReEnable = true;
            return;
        }

        double speed = chaseSpeed.getValue();
        MovementUtil.setMotionX(dx / dist * speed);
        MovementUtil.setMotionZ(dz / dist * speed);

        event.cancel();
        mc.player.move(MovementType.SELF, mc.player.getVelocity());
    }

    @EventListener
    private void onFly(MoveEvent event) {
        if (!mode.is(Mode.Fly)) {
            return;
        }

        target = CombatUtil.getClosestEnemy(128);
        Vec3d goal;
        if (target == null) {
            flyStage = FlyStage.BYPASS;
            // 无目标：悬停在原地（向下找空气块稳定高度）
            BlockPos highPos = mc.player.getBlockPos();
            while (mc.world.getBlockState(highPos).getBlock() != Blocks.AIR && highPos.getY() > mc.world.getBottomY()) {
                highPos = highPos.down();
            }
            goal = new Vec3d(mc.player.getX(), highPos.getY() + 0.5, mc.player.getZ());
        } else {
            Vec3d predict = PredictUtil.getPos((PlayerEntity) target, predictTicks.getValueInt());
            double d1 = Math.abs(target.getX() - mc.player.getX());
            double d2 = Math.abs(target.getZ() - mc.player.getZ());

            switch (flyStage) {
                case TRACK -> {
                    goal = predict.add(0, trackHeight.getValue(), 0);
                    if (d1 < trackRange.getValue() && d2 < trackRange.getValue() && mc.player.getY() - target.getY() >= trackHeight.getValue()) {
                        flyStage = FlyStage.ATTACK;
                    }
                }
                case ATTACK -> {
                    goal = predict.add(0, -3, 0);
                    if (d1 > trackRange.getValue() || d2 > trackRange.getValue() || mc.player.getY() - predict.getY() < 1.5) {
                        flyStage = FlyStage.TRACK;
                    }
                }
                default -> {
                    flyStage = FlyStage.TRACK;
                    goal = predict.add(0, trackHeight.getValue(), 0);
                }
            }
        }

        Vec3d motion = goal.subtract(mc.player.getPos()).normalize().multiply(flySpeed.getValue());
        event.setX(motion.getX());
        event.setY(motion.getY());
        event.setZ(motion.getZ());
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (path == null || path.isEmpty()) {
            return;
        }

        Iterator<Vec3> iterator = path.iterator();
        Vec3 node = iterator.next();

        while (iterator.hasNext()) {
            Vec3 s = iterator.next();
            Render3DUtil.drawLine(node.mc(), s.mc(), Color.RED);
            node = s;
        }
    }

    @Override
    public void onEnable() {
        if (nullCheck()) {
            return;
        }

        if (disableInteract.getValue() && InteractTweaks.INSTANCE != null && InteractTweaks.INSTANCE.isOn()) {
            InteractTweaks.INSTANCE.disable();
            interactDisabledByMe = true;
        }

        windChargeStage = WindChargeStage.PEARL;
        fireworkStage = FireworkStage.PREPARE;
        flyStage = FlyStage.BYPASS;
        packetGroundY = findPacketGroundY();
        packetCameraX = mc.player.getX();
        packetCameraY = mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose());
        packetCameraZ = mc.player.getZ();

        fallDistance = 0;
        isDoubleAttack = false;
        lagBackPos = mc.player.getPos();
        directionVec = null;
        target = null;
        path.clear();
        pathIndex = 0;
        elytra = false;
        firstLagConsumed = false;
        timerActive = false;

        if (mode.is(Mode.LagBack)) {
            // 自动重启用时不发初始假位置包，避免回弹
            if (!autoReEnabling) {
                doLagBack();
            }
            firstLagConsumed = false;
        }
    }

    @Override
    public void onDisable() {
        if (interactDisabledByMe && InteractTweaks.INSTANCE != null && !InteractTweaks.INSTANCE.isOn()) {
            InteractTweaks.INSTANCE.enable();
            interactDisabledByMe = false;
        }
        directionVec = null;
        target = null;
        isDoubleAttack = false;
        resetTimer();
        firstLagConsumed = false;
    }

    @EventListener
    private void onTick(ClientTickEvent event) {
        if (event.isPost()) {
            return;
        }
        if (nullCheck()) {
            resetTimer();
            return;
        }
        if (!mode.is(Mode.LagBack)) {
            resetTimer();
        }

        switch (mode.getValue()) {
            case Firework -> doFireworkMode();
            case WindChargePearl -> doWindChargeMode();
            case LagBack -> doLagBackMode();
            case ElytraBack -> doElytraBackMode();
            case TP -> doTpMode();
        }
    }

    @EventListener
    private void onPacketModeTick(ClientTickEvent event) {
        if (!mode.is(Mode.Packet) || nullCheck() || !event.isPre()) {
            return;
        }

        if (packetRequireMace.getValue() && mc.player.getMainHandStack().getItem() != Items.MACE) {
            return;
        }

        if (mc.player.getY() <= packetGroundY + packetResetDistance.getValue()) {
            teleportPacketUpInSegments();
            mc.player.setVelocity(mc.player.getVelocity().x, 0, mc.player.getVelocity().z);
        }
    }

    @EventListener
    private void onAttack(PacketEvent.Sent event) {
        if (!nullCheck() && event.getPacket() instanceof PlayerInteractEntityC2SPacket packet
                && Criticals.getInteractType(packet) == InteractType.ATTACK
                && Criticals.getEntity(packet) instanceof PlayerEntity player) {
            lastAttack = player;
        }
    }

    @EventListener
    private void onHurt(PacketEvent.Receive event) {
        if (!nullCheck() && lastAttack != null
                && event.getPacket() instanceof EntityStatusS2CPacket packet
                && packet.getStatus() == EntityStatuses.PLAY_ATTACK_SOUND
                && packet.getEntity(mc.world) == lastAttack) {
            lastAttack = null;
            if (mode.is(Mode.LagBack)) {
                doLagBack();
            }
        }
    }

    private void doTpMode() {
        PlayerEntity target = CombatUtil.getClosestEnemy(10000);
        if (target == null) {
            return;
        }

        if (mc.player.getY() < target.getY() + 1.5) {
            return;
        }

        if (!delayTimer.passedMs(delay.getValue())) {
            return;
        }

        delayTimer.reset();
        Vec3d newPos = target.getPos();

        TPUtils.newTeleport(newPos);
        mc.player.setVelocity(mc.player.getVelocity().getX(), 0, mc.player.getVelocity().getZ());
        mc.player.setPosition(newPos.getX(), newPos.getY(), newPos.getZ());
    }

    private void teleportPacketUpInSegments() {
        double x = mc.player.getX();
        double startY = mc.player.getY();
        double z = mc.player.getZ();
        double segmentHeight = packetFallHeight.getValue();
        int segmentCount = packetSegments.getValueInt();
        double targetY = startY + segmentHeight * segmentCount;

        for (int step = 1; step <= segmentCount; step++) {
            double packetY = startY + segmentHeight * step;
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, packetY, z, false));
        }
        mc.player.setPosition(x, targetY, z);
    }

    private double findPacketGroundY() {
        BlockPos pos = mc.player.getBlockPos();
        int worldBottom = mc.world.getBottomY();
        while (pos.getY() > worldBottom) {
            BlockPos below = pos.down();
            if (mc.world.getBlockState(below).blocksMovement()) {
                return below.getY() + 1.0;
            }
            pos = below;
        }
        return mc.player.getY();
    }

    public boolean shouldLockPacketCamera() {
        return isOn() && mode.is(Mode.Packet) && packetCameraLock.getValue() && !nullCheck() && mc.options.getPerspective().isFirstPerson();
    }

    public double getPacketCameraY() {
        return packetCameraY;
    }

    public double getPacketCameraX() {
        return packetCameraX;
    }

    public double getPacketCameraZ() {
        return packetCameraZ;
    }

    public boolean canPacketAttackFromLockedCamera(Entity target) {
        if (mc.player.getMainHandStack().getItem() != Items.MACE) {
            return true;
        }
        return mc.player.getVelocity().y < 0 && mc.player.fallDistance > 1.5f && mc.player.squaredDistanceTo(target) < 36;
    }

    public void updatePacketGround() {
        packetGroundY = findPacketGroundY();
    }

    // ElytraBack

    private void doElytraBackMode() {
        PlayerEntity target = CombatUtil.getClosestEnemy(10000);
        if (target == null) {
            return;
        }

        if (Math.abs(target.getX() - mc.player.getX()) > trackRange.getValue() || Math.abs(target.getZ() - mc.player.getZ()) > trackRange.getValue()) {
            return;
        }

        if (mc.player.getY() - target.getY() < trackHeight.getValue()) {
            return;
        }

        if (!delayTimer.passedMs(delay.getValue())) {
            return;
        }

        delayTimer.reset();

        Vec3d to = new Vec3d(mc.player.getX(), target.getY() + 3.5, mc.player.getZ());

        mc.player.setVelocity(mc.player.getVelocity().x, -0.2, mc.player.getVelocity().z);
        if (mc.player.fallDistance > 0.3 && !mc.player.isFallFlying()) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            mc.player.startFallFlying();
        }
        TPUtils.newTeleport(to);
        mc.player.setPosition(to.x, to.y, to.z);
    }

    // LagBack

    private void doLagBackMode() {
        handleLagBackTimer();

        BlockPos bottom = mc.player.getBlockPos();
        while (mc.world.isAir(bottom) && bottom.getY() > -64) bottom = bottom.down();

        fallDistance = lagBackPos.getY() - bottom.getY() - 0.5;

        if (ElytraFlight.INSTANCE.isOn() || fallDistance < lagBackRange.getValue()) {
            isDoubleAttack = false;
            lagBackPos = mc.player.getPos();
            return;
        }

        if (mc.player.getY() - bottom.toCenterPos().getY() - 0.5 > lagBackRange.getValue()) {
            isDoubleAttack = false;
            doubleTimer.reset();
            return;
        }

        if (doubleAttack.getValue() && !doubleTimer.passedMs(doubleTime.getValue())) {
            isDoubleAttack = true;
            return;
        }

        doLagBack();
    }

    @EventListener
    private void onEntityVelocityUpdate(EntityVelocityUpdateEvent event) {
        if (mode.is(Mode.LagBack) && event.getEntity() == mc.player && event.getY() > 0) {
            // 未达 LagBackRange 就发生回弹（异常 lagback）：回到第一次下落的 timer，不立即刷假包
            if (recoverFirstTimer.getValue() && fallDistance < lagBackRange.getValue()) {
                firstLagConsumed = false;
                return;
            }
            doLagBack();
        }
    }

    private void doLagBack() {
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), 10000, mc.player.getZ(), false));
        firstLagConsumed = true;
    }

    private void handleLagBackTimer() {
        if (!timerBoost.getValue() || ElytraFlight.INSTANCE.isOn() || mc.player.isFallFlying()) {
            resetTimer();
            return;
        }

        Forever.TIMER.set(getTimerValue());
        timerActive = true;
    }

    private void resetTimer() {
        if (timerActive) {
            Forever.TIMER.reset();
            timerActive = false;
        }
    }

    private float getTimerValue() {
        return !firstLagConsumed ? firstFallTimer.getValueFloat() : normalTimer.getValueFloat();
    }

    // WindCharge

    private void doWindChargeMode() {
        if (!delayTimer.passedMs(delay.getValue())) {
            return;
        }
        delayTimer.reset();

        switch (windChargeStage) {
            case PEARL -> doPearl();
            case WINDBRUST -> doWindCharge(Direction.UP);
            case STOP -> disable();
        }
    }

    public void doPearl() {
        int pearl = inventorySwap.getValue() ? InventoryUtil.findItemInventorySlot(Items.ENDER_PEARL) : InventoryUtil.findItem(Items.ENDER_PEARL);
        if (pearl == -1) {
            sendMessage("§cNo pearls");
            disable();
            return;
        }

        Forever.ROTATION.snapAt(Forever.ROTATION.lastYaw, -90);
        int select = mc.player.getInventory().selectedSlot;

        if (inventorySwap.getValue()) {
            InventoryUtil.inventorySwap(pearl, select);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Forever.ROTATION.lastYaw, -90));
            InventoryUtil.inventorySwap(pearl, select);
        } else {
            InventoryUtil.switchToSlot(pearl);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Forever.ROTATION.lastYaw, -90));
            InventoryUtil.switchToSlot(select);
        }

        windChargeStage = WindChargeStage.WINDBRUST;
    }

    public void doWindCharge(Direction direction) {
        int windCharge = inventorySwap.getValue() ? InventoryUtil.findItemInventorySlot(Items.WIND_CHARGE) : InventoryUtil.findItem(Items.WIND_CHARGE);
        if (windCharge == -1) {
            sendMessage("§cNo wind charges");
            disable();
            return;
        }

        float pitch = switch (direction) {
            case UP -> -90;
            case DOWN -> 90;
            default -> 0;
        };

        Forever.ROTATION.snapAt(Forever.ROTATION.lastYaw, pitch);
        int select = mc.player.getInventory().selectedSlot;

        if (inventorySwap.getValue()) {
            InventoryUtil.inventorySwap(windCharge, select);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Forever.ROTATION.lastYaw, pitch));
            InventoryUtil.inventorySwap(windCharge, select);
        } else {
            InventoryUtil.switchToSlot(windCharge);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Forever.ROTATION.lastYaw, pitch));
            InventoryUtil.switchToSlot(select);
        }

        windChargeStage = WindChargeStage.STOP;
    }

    // Firework

    private void doFireworkMode() {
        switch (fireworkStage) {
            case PREPARE -> doPrepare();
            case TAKEOFF -> doTakeoff();
            case TRACKING -> doTracking();
            case ATTACK -> doAttack();
        }
    }

    private void doPrepare() {
        int maceSlot = InventoryUtil.findItem(Items.MACE);
        if (maceSlot == -1) {
            sendMessage("§cNo Mace");
            disable();
            return;
        }

        int firework = InventoryUtil.findItem(Items.FIREWORK_ROCKET);
        if (firework == -1) {
            sendMessage("§cNo Firework Rockets");
            disable();
            return;
        }

        startPos = mc.player.getBlockPos();
        fireworkStage = FireworkStage.TAKEOFF;
        takeOffTimer.reset();
    }

    private void doTakeoff() {
        swapElytra();

        if (!mc.player.isFallFlying() || !takeOffTimer.passedMs(takeOffDelay.getValue())) {
            directionVec = null;
            startFallFlying();
            return;
        }

        // 检查是否达到最低高度
        if (mc.player.getY() >= startPos.getY() + minHeight.getValue()) {
            fireworkStage = FireworkStage.TRACKING;
            directionVec = null;
            return;
        }

        directionVec = new Vec3d(mc.player.getX(), mc.player.getY() + 100, mc.player.getZ());
        if (!fireworkTimer.passedMs(fireworkDelay.getValue())) {
            return;
        }

        useFirework();
        fireworkTimer.reset();
    }

    private void doTracking() {
        // 搜索目标
        if (target == null || target.isDead() || !target.isAlive()) {
            target = CombatUtil.getClosestEnemy(targetRange.getValue());
            if (target == null) {
                // 找不到目标，继续搜索
                return;
            }
        }

        // 计算与目标的距离
        double distance = mc.player.distanceTo(target);

        // 如果距离小于攻击范围，进入攻击阶段
        if (distance <= attackRange.getValue()) {
            attackTimer.reset();
            fireworkStage = FireworkStage.ATTACK;
            return;
        }

        // 使用AStar计算路径
        if (path.isEmpty() || pathIndex >= path.size()) {
            path = calculatePathToTarget();
            pathIndex = 0;
        }

        // 沿着路径飞行
        if (!path.isEmpty()) {
            Vec3 targetPos = path.get(pathIndex);
            double distToPoint = mc.player.getPos().distanceTo(new Vec3d(targetPos.x(), targetPos.y(), targetPos.z()));

            if (distToPoint < 2.0) {
                pathIndex++;
            }

            // 朝向目标点
            directionVec = targetPos.mc();

            // 使用烟花火箭推进
            if (mc.player.isFallFlying() && fireworkTimer.passedMs(fireworkDelay.getValue())) {
                useFirework();
                fireworkTimer.reset();
            }
        }

        // 保持鞘翅飞行状态
        if (!mc.player.isFallFlying()) {
            startFallFlying();
        }
    }

    private void doAttack() {
        swapChestplate();

        if (!attackTimer.passedMs(attackDelay.getValue())) {
            return;
        }

        // 切换到重锤
        int maceSlot = InventoryUtil.findItem(Items.MACE);
        if (maceSlot != -1) {
            InventoryUtil.switchToSlot(maceSlot);

            // 攻击目标
            if (target != null && target.isAlive()) {
                directionVec = target.getPos();
                mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
                mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }

        // 重置阶段，进入下一轮循环
        fireworkStage = FireworkStage.PREPARE;
        target = null;
        path.clear();
        pathIndex = 0;
    }

    private void swapElytra() {
        elytra = true;
    }

    private void swapChestplate() {
        elytra = false;
    }

    public boolean shouldElytra() {
        return isOn() && elytra;
    }

    private void startFallFlying() {
        if (mc.player.fallDistance > 0.3) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            mc.player.startFallFlying();
        }
    }

    private void useFirework() {
        int firework = InventoryUtil.findItem(Items.FIREWORK_ROCKET);
        if (firework != -1) {
            int oldSlot = mc.player.getInventory().selectedSlot;
            InventoryUtil.switchToSlot(firework);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
            InventoryUtil.switchToSlot(oldSlot);
        }
    }

    private ArrayList<Vec3> calculatePathToTarget() {
        if (target == null) return new ArrayList<>();

        Vec3 start = new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        Vec3 end = new Vec3(target.getX(), target.getY(), target.getZ());

        return PathUtils.computePath(start, end);
    }

    // AutoStopFly 禁用后的自动重启用：仅重新开启 TimerMace，不重新开启鞘翅飞行
    private class ChaseReEnableTick {
        @EventListener
        private void onTick(ClientTickEvent event) {
            if (event.isPost() || !pendingReEnable) {
                return;
            }

            // 已松手：取消待重启用，并确保模块保持关闭
            if (!getBindSetting().isPressed()) {
                pendingReEnable = false;
                if (isOn()) {
                    disable();
                }
                return;
            }

            // 仍按住：间隔 reEnableDelay 毫秒后仅重新开启 TimerMace（不重新开启鞘翅飞行）
            if (reEnableTimer.passedMs(reEnableDelay.getValue())) {
                pendingReEnable = false;
                autoReEnabling = true;
                enable();
                autoReEnabling = false;
            }
        }
    }
}
