package dev.G255.Lumi.mod.modules.impl.combat;

import dev.G255.Lumi.Lumi;
import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.ClientTickEvent;
import dev.G255.Lumi.api.events.impl.MoveEvent;
import dev.G255.Lumi.api.events.impl.PacketEvent;
import dev.G255.Lumi.api.events.impl.TimerEvent;
import dev.G255.Lumi.api.utils.math.MathUtil;
import dev.G255.Lumi.api.utils.math.Timer;
import dev.G255.Lumi.api.utils.player.EntityUtil;
import dev.G255.Lumi.api.utils.player.InventoryUtil;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.EnumSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import dev.G255.Lumi.mod.modules.settings.enums.SwingSide;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;

public class MaceAura extends Module {
    public static MaceAura INSTANCE;
    private static final double[] BURROW_OFFSETS = new double[]{
            0.4199999868869781,
            0.7531999805212017,
            0.9999957640154541,
            1.1661092609382138
    };

    private final EnumSetting<Mode> mode = this.add(new EnumSetting<>("Mode", Mode.GroundFly));

    private final SliderSetting targetRange = this.add(new SliderSetting("TargetRange", 10.0, 3.0, 30.0, 1.0).setSuffix("m"));
    private final SliderSetting attackRange = this.add(new SliderSetting("AttackRange", 6.0, 2.0, 10.0, 0.1).setSuffix("m"));
    private final SliderSetting cooldownTime = this.add(new SliderSetting("Cooldown", 500.0, 100.0, 2000.0, 10.0).setSuffix("ms"));
    private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
    private final BooleanSetting weaponOnly = this.add(new BooleanSetting("MaceOnly", true));

    private final BooleanSetting track = this.add(new BooleanSetting("Track", true, () -> this.mode.is(Mode.GroundFly)));
    private final SliderSetting trackSpeed = this.add(new SliderSetting("TrackSpeed", 0.2873, 0.05, 0.5, 0.01, () -> this.mode.is(Mode.GroundFly) && this.track.getValue()).setSuffix("/tick"));
    private final BooleanSetting lagback = this.add(new BooleanSetting("Lagback", true, () -> this.mode.is(Mode.GroundFly)));
    private final BooleanSetting onlyOnGround = this.add(new BooleanSetting("OnlyOnGround", true, () -> this.mode.is(Mode.GroundFly)));
    private final BooleanSetting autoBounce = this.add(new BooleanSetting("AutoBounce", false, () -> this.mode.is(Mode.GroundFly)));
    private final SliderSetting bounceHeight = this.add(new SliderSetting("BounceHeight", 6.0, 1.0, 30.0, 1.0, () -> this.mode.is(Mode.GroundFly) && this.autoBounce.getValue()).setSuffix(""));
    private final SliderSetting bounceSpeed = this.add(new SliderSetting("BounceSpeed", 0.6, 0.1, 3.0, 0.05, () -> this.mode.is(Mode.GroundFly) && this.autoBounce.getValue()).setSuffix("/tick"));
    private final SliderSetting bounceRange = this.add(new SliderSetting("BounceRange", 1.0, 0.1, 5.0, 0.1, () -> this.mode.is(Mode.GroundFly) && this.autoBounce.getValue()).setSuffix(""));

    private final SliderSetting triggerDistance = this.add(new SliderSetting("TriggerDist", 2.0, 0.1, 20.0, 0.1, () -> this.mode.is(Mode.PullBack)));
    private final SliderSetting lagbackDistance = this.add(new SliderSetting("LagbackDist", 5.0, 0.0, 10.0, 0.1, () -> this.mode.is(Mode.PullBack)));
    private final SliderSetting spoofHeight = this.add(new SliderSetting("SpoofHeight", 25.0, 0.0, 2000.0, 0.1, () -> this.mode.is(Mode.PullBack)));
    private final SliderSetting cooldownTicks = this.add(new SliderSetting("CooldownTick", 3, 0, 20, () -> this.mode.is(Mode.PullBack)));
    private final BooleanSetting timerEnabled = this.add(new BooleanSetting("Timer", false, () -> this.mode.is(Mode.PullBack)).setParent());
    private final SliderSetting timerSpeed = this.add(new SliderSetting("TimerSpeed", 2.0, 1.0, 100.0, 0.1, () -> this.mode.is(Mode.PullBack) && this.timerEnabled.isOpen()).setSuffix("x"));
    private final BooleanSetting onlyFalling = this.add(new BooleanSetting("OnlyFalling", true, () -> this.mode.is(Mode.PullBack)));
    private final BooleanSetting onlyAir = this.add(new BooleanSetting("OnlyAir", true, () -> this.mode.is(Mode.PullBack)));
    private final BooleanSetting noWait = this.add(new BooleanSetting("NoWait", false, () -> this.mode.is(Mode.PullBack)));
    private final BooleanSetting autoAttack = this.add(new BooleanSetting("AutoAttack", true, () -> this.mode.is(Mode.PullBack)));
    private final BooleanSetting set = this.add(new BooleanSetting("Set", false, () -> this.mode.is(Mode.PullBack)).setParent());
    private final SliderSetting setDelay = this.add(new SliderSetting("SetDelay", 400.0, 100.0, 3000.0, 10.0, () -> this.mode.is(Mode.PullBack) && this.set.isOpen()).setSuffix("ms"));
    private final SliderSetting setLagFly = this.add(new SliderSetting("SetLagFly", 4.0, 0.1, 10.0, 0.1, () -> this.mode.is(Mode.PullBack) && this.set.isOpen()));

    private PlayerEntity target;
    private final Timer attackCooldown = new Timer();
    private long lastAttackTime = 0L;
    private boolean reset = false;
    private boolean bouncingDown = true;
    private Vec3d fallStart;
    private Vec3d lastStablePos;
    private int cooldown;
    private boolean falling;
    private final Timer setLoopTimer = new Timer();
    private final Timer setStageTimer = new Timer();
    private int setStage;
    private boolean setHasSwitched;
    private boolean setHasLagged;
    private boolean setAwaitingReturn;
    private PlayerEntity setTarget;

    public MaceAura() {
        super("MaceAura", Module.Category.Combat);
        this.setChinese("");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (!nullCheck()) {
            this.target = null;
            this.lastAttackTime = 0L;
            this.reset = false;
            this.attackCooldown.reset();
            this.bouncingDown = true;
            this.fallStart = null;
            this.lastStablePos = null;
            this.cooldown = 0;
            this.falling = false;
            this.resetSetCycle();
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.target = null;
        this.reset = false;
        this.bouncingDown = true;
        this.fallStart = null;
        this.lastStablePos = null;
        this.cooldown = 0;
        this.falling = false;
        this.resetSetCycle();
    }

    @Override
    public String getInfo() {
        return this.target != null ? this.target.getName().getString() : null;
    }

    private void resetSetCycle() {
        this.setStage = 0;
        this.setHasSwitched = false;
        this.setHasLagged = false;
        this.setTarget = null;
        this.setAwaitingReturn = false;
        this.setLoopTimer.reset();
    }

    @EventListener
    public void onTimer(TimerEvent event) {
        if (this.mode.is(Mode.PullBack) && this.timerEnabled.getValue() && this.falling) {
            event.set(this.timerSpeed.getValueFloat());
        }
    }

    @EventListener
    public void onTick(ClientTickEvent event) {
        if (nullCheck() || event.isPost()) {
            return;
        }

        if (this.mode.is(Mode.GroundFly)) {
            this.onTickFly();
        } else if (this.mode.is(Mode.PullBack)) {
            this.onTickPullBack();
        }
    }

    private void onTickFly() {
        if (!this.attackCooldown.passedMs((double) ((long) this.cooldownTime.getValue()))) {
            return;
        }

        this.target = this.getTarget();
        if (this.target == null) {
            return;
        }

        if (Lumi.FRIEND.isFriend(this.target)) {
            this.target = null;
            return;
        }

        if ((double) mc.player.distanceTo(this.target) > this.attackRange.getValue()) {
            return;
        }

        if (this.onlyOnGround.getValue() && !mc.player.isOnGround() && !this.autoBounce.getValue()) {
            return;
        }

        int slot = this.findMaceSlot();
        if (slot == -1) {
            return;
        }

        if (this.rotate.getValue()) {
            Vec3d hitVec = MathUtil.getClosestPointToBox(mc.player.getEyePos(), this.target.getBoundingBox());
            if (hitVec != null) {
                Lumi.ROTATION.lookAt(hitVec);
            }
        }

        if (System.currentTimeMillis() - this.lastAttackTime > 1000L) {
            this.attackTargetFly(slot);
            this.lastAttackTime = System.currentTimeMillis();
            this.attackCooldown.reset();
        }
    }

    private void onTickPullBack() {
        if (this.cooldown > 0) {
            this.cooldown--;
        }

        this.updateFallTracking();

        if (this.set.getValue()) {
            this.handleSet();
        } else if (this.setStage != 0 || this.setTarget != null || this.setAwaitingReturn) {
            this.resetSetCycle();
        }

        this.target = this.getTarget();
        if (this.target == null) {
            return;
        }

        if (this.autoAttack.getValue() && this.falling && this.isAttackable(this.target)) {
            if (this.attackTargetPullBack()) {
                if (this.fallStart != null) {
                    this.performReturn(this.fallStart);
                }
                return;
            }
        }

        if (this.weaponOnly.getValue() && !this.canUseMace()) {
            return;
        }

        if (this.onlyAir.getValue() && mc.player.isOnGround()) {
            this.resetFallState();
            return;
        }

        if (mc.player.isTouchingWater() || mc.player.isClimbing() || mc.player.isFallFlying()) {
            this.resetFallState();
            return;
        }

        if (this.onlyFalling.getValue() && mc.player.getVelocity().y >= 0.0) {
            return;
        }

        if (this.fallStart == null || this.fallStart.y - mc.player.getY() < this.triggerDistance.getValue()) {
            return;
        }

        if (this.cooldown > 0) {
            return;
        }

        if (this.lagbackDistance.getValue() > 0.0) {
            double distanceToTarget = mc.player.distanceTo(this.target);
            if (distanceToTarget > this.lagbackDistance.getValue()) {
                return;
            }
        }

        if (this.shouldReturnBeforeLanding() && this.doCycle()) {
            this.cooldown = this.cooldownTicks.getValueInt();
        }
    }

    private boolean doCycle() {
        Vec3d start = this.fallStart;
        if (start == null) {
            return false;
        }

        if (!this.noWait.getValue() && mc.player.getAttackCooldownProgress(0.5F) < 1.0F) {
            return false;
        }

        this.performReturn(start);
        return true;
    }

    private void performReturn(Vec3d start) {
        this.sendBurrowReturn(start);
        mc.player.setPosition(start.x, start.y, start.z);
        mc.player.setVelocity(mc.player.getVelocity().x, 0.0, mc.player.getVelocity().z);
        mc.player.fallDistance = 0.0F;
        this.resetFallState();
        this.cooldown = this.cooldownTicks.getValueInt();

        if (this.set.getValue()) {
            this.resetSetCycle();
        }
    }

    private void sendBurrowReturn(Vec3d start) {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();

        for (double offset : BURROW_OFFSETS) {
            sendPositionPacket(x, y + offset, z);
        }

        Vec3d burrowTop = new Vec3d(x, y + BURROW_OFFSETS[BURROW_OFFSETS.length - 1], z);
        sendHorizontalStepPackets(burrowTop, start);
        Vec3d spoofPos = new Vec3d(start.x, start.y + this.spoofHeight.getValue(), start.z);
        sendTeleportPadding(burrowTop, spoofPos);
        sendFullPosition(spoofPos.x, spoofPos.y, spoofPos.z, false);
        sendTeleportPadding(spoofPos, start);
        sendFullPosition(start.x, start.y, start.z, true);
    }

    private void sendHorizontalStepPackets(Vec3d from, Vec3d target) {
        double x = from.x;
        double z = from.z;
        double y = from.y;
        double step = 0.25;

        while (Math.abs(x - target.x) > step) {
            x += x > target.x ? -step : step;
            sendPositionPacket(x, y, z);
        }
        while (Math.abs(z - target.z) > step) {
            z += z > target.z ? -step : step;
            sendPositionPacket(x, y, z);
        }
        if (x != target.x || z != target.z) {
            sendPositionPacket(target.x, y, target.z);
        }
    }

    private void sendTeleportPadding(Vec3d from, Vec3d to) {
        int packetsRequired = (int) Math.ceil(from.distanceTo(to) / 10.0) - 1;
        for (int i = 0; i < packetsRequired; i++) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
        }
    }

    private void sendPositionPacket(double x, double y, double z) {
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false));
    }

    private void sendFullPosition(double x, double y, double z, boolean onGround) {
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, mc.player.getYaw(), mc.player.getPitch(), onGround));
    }

    private void updateFallTracking() {
        if (mc.player.isTouchingWater() || mc.player.isClimbing() || mc.player.isFallFlying()) {
            this.lastStablePos = mc.player.getPos();
            this.resetFallState();
            return;
        }

        if (mc.player.isOnGround() || mc.player.getVelocity().y >= 0.0) {
            this.lastStablePos = mc.player.getPos();
            this.falling = false;
            return;
        }

        if (!this.falling) {
            this.falling = true;
            this.fallStart = this.lastStablePos != null ? this.lastStablePos : mc.player.getPos();
        }
    }

    private void handleSet() {
        if (this.setAwaitingReturn && mc.player.isOnGround() && this.setStage == 0) {
            this.setAwaitingReturn = false;
            this.setLoopTimer.reset();
        }

        PlayerEntity target = this.getTarget();
        if (target == null) {
            this.resetSetCycle();
            return;
        }

        if (this.setStage == 0) {
            if (!this.setAwaitingReturn
                    && !this.falling
                    && this.cooldown <= 0
                    && this.setLoopTimer.passedMs(this.setDelay.getValueInt())
                    && this.throwSetWindChargeDown()) {
                this.setTarget = target;
                this.setAwaitingReturn = true;
                this.setStageTimer.reset();
                this.setHasSwitched = false;
                this.setHasLagged = false;
                this.setStage = 1;
            }
            return;
        }

        if (!this.setHasSwitched) {
            int maceSlot = InventoryUtil.findItem(Items.MACE);
            if (maceSlot != -1 && mc.player.getInventory().selectedSlot != maceSlot) {
                mc.player.getInventory().selectedSlot = maceSlot;
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(maceSlot));
            }
            this.setHasSwitched = true;
        }

        if (!this.setHasLagged && this.setStageTimer.passedMs(80)) {
            sendPositionPacket(mc.player.getX(), mc.player.getY() + this.setLagFly.getValue(), mc.player.getZ());
            this.setHasLagged = true;
        }

        if (this.setStageTimer.passedMs(80)) {
            if (this.falling
                    && this.setTarget != null
                    && this.setTarget.isAlive()
                    && mc.player.distanceTo(this.setTarget) <= this.attackRange.getValue()) {
                mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(this.setTarget, mc.player.isSneaking()));
                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                Vec3d start = this.fallStart;
                if (start != null) {
                    this.performReturn(start);
                    return;
                }
            }
            this.resetSetCycle();
        }
    }

    private boolean throwSetWindChargeDown() {
        int windChargeSlot;
        if (mc.player.getMainHandStack().getItem() == Items.WIND_CHARGE) {
            windChargeSlot = mc.player.getInventory().selectedSlot;
        } else if ((windChargeSlot = InventoryUtil.findItem(Items.WIND_CHARGE)) != -1) {
        } else {
            return false;
        }

        float yaw = mc.player.getYaw();
        float pitch = 90.0F;
        Lumi.ROTATION.snapAt(yaw, pitch);

        int oldSlot = mc.player.getInventory().selectedSlot;
        if (mc.player.getMainHandStack().getItem() != Items.WIND_CHARGE) {
            InventoryUtil.switchToSlot(windChargeSlot);
        }
        mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, yaw, pitch));
        mc.player.swingHand(Hand.MAIN_HAND);
        if (mc.player.getMainHandStack().getItem() != Items.WIND_CHARGE) {
            InventoryUtil.switchToSlot(oldSlot);
        }

        Lumi.ROTATION.snapBack();
        return true;
    }

    private boolean shouldReturnBeforeLanding() {
        long timeToGround = getTimeToGroundMs();
        if (timeToGround == Long.MAX_VALUE) {
            return false;
        }
        return timeToGround <= 42L + getPingMs();
    }

    private long getTimeToGroundMs() {
        if (mc.player.getVelocity().y >= 0.0) {
            return Long.MAX_VALUE;
        }

        Vec3d from = mc.player.getPos().add(0.0, 0.01, 0.0);
        HitResult hit = mc.world.raycast(new RaycastContext(
                from,
                from.subtract(0.0, 256.0, 0.0),
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));
        if (hit.getType() == HitResult.Type.MISS) {
            return Long.MAX_VALUE;
        }

        double remainingDistance = mc.player.getY() - hit.getPos().y;
        if (remainingDistance <= 0.0) {
            return 0L;
        }

        double velocityY = mc.player.getVelocity().y;
        double fallen = 0.0;
        double millisecondsPerTick = this.timerEnabled.getValue()
                ? 50.0 / this.timerSpeed.getValue()
                : 50.0;
        for (int tick = 1; tick <= 200; tick++) {
            velocityY = (velocityY - 0.08) * 0.98;
            fallen -= velocityY;
            if (fallen >= remainingDistance) {
                return (long) Math.ceil(tick * millisecondsPerTick);
            }
        }
        return Long.MAX_VALUE;
    }

    private int getPingMs() {
        if (mc.getNetworkHandler() == null || mc.player == null) {
            return 0;
        }
        var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return entry == null ? 0 : entry.getLatency();
    }

    private boolean canUseMace() {
        return mc.player.getMainHandStack().getItem() == Items.MACE
                || InventoryUtil.findItem(Items.MACE) != -1;
    }

    private boolean isAttackable(PlayerEntity entity) {
        if (entity == null || !entity.isAlive()) {
            return false;
        }
        Vec3d hitVec = MathUtil.getClosestPointToBox(mc.player.getEyePos(), entity.getBoundingBox());
        return mc.player.getEyePos().distanceTo(hitVec) <= this.attackRange.getValue();
    }

    private boolean attackTargetPullBack() {
        int oldSlot = mc.player.getInventory().selectedSlot;
        int maceSlot = InventoryUtil.findItem(Items.MACE);
        boolean swapped = false;

        if (mc.player.getMainHandStack().getItem() != Items.MACE) {
            if (maceSlot == -1) {
                return false;
            }
            InventoryUtil.switchToSlot(maceSlot);
            swapped = true;
        }

        Vec3d hitVec = MathUtil.getClosestPointToBox(mc.player.getEyePos(), this.target.getBoundingBox());
        Lumi.ROTATION.lookAt(hitVec);
        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(this.target, mc.player.isSneaking()));
        mc.player.resetLastAttackedTicks();
        EntityUtil.swingHand(Hand.MAIN_HAND, SwingSide.All);
        Lumi.ROTATION.snapBack();

        if (swapped) {
            InventoryUtil.switchToSlot(oldSlot);
        }
        return true;
    }

    @EventListener
    public void onMove(MoveEvent event) {
        if (nullCheck()) {
            return;
        }

        if (this.mode.is(Mode.GroundFly)) {
            this.onMoveFly(event);
        }
    }

    private void onMoveFly(MoveEvent event) {
        if (this.target == null) {
            return;
        }

        double dX = this.target.getX() - mc.player.getX();
        double dZ = this.target.getZ() - mc.player.getZ();
        double horizontalDist = Math.sqrt(dX * dX + dZ * dZ);
        boolean inBounceRange = horizontalDist <= this.bounceRange.getValue();

        if (this.track.getValue() && !inBounceRange && !mc.player.isOnGround()
                && (double) mc.player.distanceTo(this.target) > this.attackRange.getValue()
                && System.currentTimeMillis() - this.lastAttackTime >= 200L
                && horizontalDist > 0.0) {
            double speed = this.trackSpeed.getValue();
            event.setX(dX / horizontalDist * speed);
            event.setZ(dZ / horizontalDist * speed);
        }

        if (this.autoBounce.getValue() && !mc.player.isOnGround()) {
            if (inBounceRange) {
                event.setX(0.0);
                event.setZ(0.0);
            }

            double targetFeetY = this.target.getY();
            double targetFloor = targetFeetY + 0.5;
            double targetCeiling = targetFeetY + this.bounceHeight.getValue();
            double speed = this.bounceSpeed.getValue();
            if (this.bouncingDown) {
                double nextY = mc.player.getY() - speed;
                if (nextY > targetFloor) {
                    event.setY(-speed);
                } else {
                    event.setY(targetFloor - mc.player.getY());
                    this.bouncingDown = false;
                }
            } else {
                double nextY = mc.player.getY() + speed;
                if (nextY < targetCeiling) {
                    event.setY(speed);
                } else {
                    event.setY(targetCeiling - mc.player.getY());
                    this.bouncingDown = true;
                }
            }
        }
    }

    @EventListener
    public void onPacketReceive(PacketEvent.Receive event) {
        if (nullCheck()) {
            return;
        }

        if (this.mode.is(Mode.GroundFly)) {
            if (event.getPacket() instanceof net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket && this.reset) {
                if (mc.player != null && mc.getNetworkHandler() != null) {
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                            mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.isOnGround()));
                }
                this.reset = false;
            }

            if (event.getPacket() instanceof net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket && this.reset) {
                if (mc.player != null && mc.getNetworkHandler() != null) {
                    net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket packet =
                            (net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket) event.getPacket();
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                            packet.getX(), packet.getY(), packet.getZ(), mc.player.isOnGround()));
                }
                this.reset = false;
            }
        }
    }

    private void attackTargetFly(int slot) {
        if (mc.player == null || this.target == null) {
            return;
        }

        int previousSlot = mc.player.getInventory().selectedSlot;
        Vec3d previous = mc.player.getPos();
        if (slot != previousSlot) {
            mc.player.getInventory().selectedSlot = slot;
        }

        mc.interactionManager.attackEntity(mc.player, this.target);
        mc.player.swingHand(Hand.MAIN_HAND);
        if (slot != previousSlot) {
            mc.player.getInventory().selectedSlot = previousSlot;
        }

        if (this.lagback.getValue()) {
            if (mc.getNetworkHandler() != null) {
                if (this.autoBounce.getValue() && !mc.player.isOnGround()) {
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                            mc.player.getX(), this.target.getY(), mc.player.getZ(), false));
                }
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        mc.player.getX(), mc.player.getY() + 1.0, mc.player.getZ(), false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        previous.x, previous.y, previous.z, false));
            }
            this.reset = true;
        }

        if (this.rotate.getValue()) {
            Lumi.ROTATION.snapBack();
        }
    }

    private PlayerEntity getTarget() {
        PlayerEntity optimalTarget = null;
        double closestDistance = Double.MAX_VALUE;
        float range = (float) this.targetRange.getValue();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player != mc.player && player.isAlive()
                    && player.getHealth() > 0.0F
                    && !Lumi.FRIEND.isFriend(player)
                    && !(mc.player.squaredDistanceTo(player) > (double) (range * range))) {
                double distance = mc.player.distanceTo(player);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    optimalTarget = player;
                }
            }
        }
        return optimalTarget;
    }

    private int findMaceSlot() {
        for (int i = 0; i < 9; ++i) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.MACE) {
                return i;
            }
        }
        return -1;
    }

    private void resetFallState() {
        this.fallStart = null;
        this.falling = false;
    }

    public enum Mode {
        GroundFly,
        PullBack
    }
}