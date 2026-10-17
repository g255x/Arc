package dev.perish.mod.modules.impl.combat;

import dev.perish.Perish;
import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.ClientTickEvent;
import dev.perish.api.events.impl.LookDirectionEvent;
import dev.perish.api.events.impl.PacketEvent;
import dev.perish.api.events.impl.TimerEvent;
import dev.perish.api.utils.combat.CombatUtil;
import dev.perish.api.utils.math.MathUtil;
import dev.perish.api.utils.math.Timer;
import dev.perish.api.utils.player.EntityUtil;
import dev.perish.api.utils.player.InventoryUtil;
import dev.perish.api.utils.player.MovementUtil;
import dev.perish.api.utils.world.BlockPosX;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.impl.movement.ElytraFly;
import dev.perish.mod.modules.settings.enums.SwingSide;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.ColorSetting;
import dev.perish.mod.modules.settings.impl.EnumSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;
import dev.perish.asm.accessors.IPlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;
import java.awt.Color;
import net.minecraft.client.util.math.MatrixStack;

public class MaceAura extends Module {
    private static final double[] BURROW_OFFSETS = new double[]{
            0.4199999868869781,
            0.7531999805212017,
            0.9999957640154541,
            1.1661092609382138
    };
    private static final double RETURN_STEP = 0.25;
    public static MaceAura INSTANCE;
    private final SliderSetting targetRange = this.add(new SliderSetting("TargetRange", 5.0, 1.0, 8.0, 0.1));
    private final SliderSetting attackRange = this.add(new SliderSetting("AttackRange", 3.0, 1.0, 8.0, 0.1));
    private final SliderSetting triggerDistance = this.add(new SliderSetting("TriggerDistance", 2.0, 0.1, 20.0, 0.1));
    private final SliderSetting lagbackDistance = this.add(new SliderSetting("LagbackDistance", 5.0, 0.0, 10.0, 0.1));
    private final SliderSetting spoofHeight = this.add(new SliderSetting("SpoofHeight", 25.0, 0.0, 2000.0, 0.1));
    private final SliderSetting cooldownTicks = this.add(new SliderSetting("CooldownTicks", 3, 0, 20));
    private final BooleanSetting timerEnabled = this.add(new BooleanSetting("Timer", false).setParent());
    private final SliderSetting timerSpeed = this.add(
            new SliderSetting("TimerSpeed", 2.0, 1.0, 100.0, 0.1, timerEnabled::isOpen).setSuffix("x")
    );
    private final BooleanSetting weaponOnly = this.add(new BooleanSetting("MaceOnly", true));
    private final BooleanSetting silent = this.add(new BooleanSetting("Silent", false));
    private final EnumSetting<RenderMode> renderMode = this.add(new EnumSetting<>("RenderMode", RenderMode.Normal));
    private final ColorSetting silentPlayerBox = this.add(
            new ColorSetting("SilentPlayerBox", new Color(255, 80, 80, 180), () -> this.renderMode.is(RenderMode.Silent))
    );
    //他🐎的这个背包切换用不了，重锤放背包就没伤害不知道为什么
    private final BooleanSetting inventorySwap = this.add(new BooleanSetting("InventorySwap", false, () -> this.silent.getValue()));
    private final BooleanSetting onlyFalling = this.add(new BooleanSetting("OnlyFalling", true));
    private final BooleanSetting onlyAir = this.add(new BooleanSetting("OnlyAir", true));
    private final BooleanSetting autoAttack = this.add(new BooleanSetting("AutoAttack", true));
    private final BooleanSetting set = this.add(new BooleanSetting("Set", false).setParent());
    private final BooleanSetting setGroundSpoof = this.add(new BooleanSetting("GroundSpoof", true, set::isOpen));
    private final BooleanSetting attackPull = this.add(new BooleanSetting("AttackPull", false, set::isOpen));
    private final EnumSetting<LagMode> setLagMode = this.add(new EnumSetting<>("LagMode", LagMode.Fly, set::isOpen));
    private final SliderSetting setDelay = this.add(new SliderSetting("Delay", 400.0, 100.0, 3000.0, 10.0, set::isOpen).setSuffix("ms"));
    private final SliderSetting setSwitchDelay = this.add(new SliderSetting("DelayUntilSwitch", 50.0, 0.0, 1000.0, 10.0, set::isOpen).setSuffix("ms"));
    private final SliderSetting setLagDelay = this.add(new SliderSetting("LagDelay", 80.0, 0.0, 1000.0, 10.0, set::isOpen).setSuffix("ms"));
    private final SliderSetting setAttackDelay = this.add(new SliderSetting("DelayUntilAttack", 80.0, 0.0, 1000.0, 10.0, set::isOpen).setSuffix("ms"));
    private final SliderSetting setAttackRange = this.add(new SliderSetting("SetRange", 5.0, 1.0, 10.0, 0.1, set::isOpen));
    private final SliderSetting setLagFly = this.add(new SliderSetting("LagFly", 4.0, 0.1, 10.0, 0.1, () -> set.isOpen() && setLagMode.is(LagMode.Fly)));
    private final SliderSetting setSmartXZ = this.add(new SliderSetting("SmartXZ", 3.0, 0.0, 10.0, 0.1, () -> set.isOpen() && setLagMode.is(LagMode.Smart)));
    private final SliderSetting setSmartUp = this.add(new SliderSetting("SmartUp", 3.0, 0.0, 10.0, 0.1, () -> set.isOpen() && setLagMode.is(LagMode.Smart)));
    private final SliderSetting setSmartDown = this.add(new SliderSetting("SmartDown", 3.0, 0.0, 10.0, 0.1, () -> set.isOpen() && setLagMode.is(LagMode.Smart)));
    private final SliderSetting setSmartDistance = this.add(new SliderSetting("SmartDist", 2.0, 0.0, 10.0, 0.1, () -> set.isOpen() && setLagMode.is(LagMode.Smart)));
    private final BooleanSetting noWait = this.add(new BooleanSetting("NoWait", false));
    private final EnumSetting<SwingSide> swingMode = this.add(new EnumSetting("Swing", SwingSide.All));
    private PlayerEntity target;
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
    private boolean sendingReturnPackets;
    private PlayerEntity setTarget;
    private Vec3d silentCameraPos;
    private boolean silentMovementReturnLatched;
    private boolean silentElytraReturnLatched;
    private float silentYaw;
    private float silentPitch;
    private float previousSilentYaw;
    private float previousSilentPitch;

    public MaceAura() {
        super("MaceAura", Module.Category.Combat);
        this.setChinese("风锤光环");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        this.resetSetCycle();
        this.sendingReturnPackets = false;
        this.silentCameraPos = null;
        this.silentMovementReturnLatched = false;
        this.silentElytraReturnLatched = false;
        this.resetSilentRotation();
    }

    @Override
    public String getInfo() {
        return this.target == null ? null : this.target.getName().getString();
    }

    @Override
    public void onDisable() {
        this.target = null;
        this.fallStart = null;
        this.lastStablePos = null;
        this.cooldown = 0;
        this.falling = false;
        this.resetSetCycle();
        this.sendingReturnPackets = false;
        this.silentCameraPos = null;
        this.silentMovementReturnLatched = false;
        this.silentElytraReturnLatched = false;
    }

    // 当玩家处于下落状态时，通过 TimerEvent 加速游戏 tick
    // 使下落速度加快，从而增加重锤伤害（下落距离更大）
    @EventListener
    public void onTimer(TimerEvent event) {
        if (!this.timerEnabled.getValue()) return;
        if (!this.falling) return;
        event.set(this.timerSpeed.getValueFloat());
    }

    @EventListener
    public void onTick(ClientTickEvent event) {
        if (event.isPost() || nullCheck()) {
            return;
        }

        if (this.cooldown > 0) {
            this.cooldown--;
        }

        // Handle this before fall tracking clears the saved return point for Elytra flight.
        this.handleSilentReturnTriggers();
        this.updateFallTracking();

        if (this.set.getValue()) {
            this.handleSet();
        } else if (this.setStage != 0 || this.setTarget != null || this.setAwaitingReturn) {
            this.resetSetCycle();
        }

        this.target = CombatUtil.getClosestEnemy(this.targetRange.getValue());
        if (this.target == null) {
            return;
        }

        // Return in the same tick as a valid mace attack instead of waiting for
        // the server-side hurtTime update, which adds at least one tick of latency.
        if (this.autoAttack.getValue() && this.falling && this.isAttackable(this.target) && this.attackTarget()) {
            if (this.set.getValue() && this.attackPull.getValue()) {
                this.throwSetWindChargeDown();
            }
            if (this.fallStart != null) {
                this.performReturn(this.fallStart);
            }
            return;
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

        // Check distance to target before performing lagback
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

        // NoWait 关闭时需等待攻击冷却充能完毕（重锤攻击间隔）
        if (!this.noWait.getValue() && mc.player.getAttackCooldownProgress(0.5F) < 1.0F) {
            return false;
        }

        this.performReturn(start);
        return true;
    }

    private void performReturn(Vec3d start) {
        this.sendingReturnPackets = true;
        try {
            this.sendBurrowReturn(start);
        } finally {
            this.sendingReturnPackets = false;
        }

        mc.player.setPosition(start.x, start.y, start.z);
        mc.player.setVelocity(mc.player.getVelocity().x, 0.0, mc.player.getVelocity().z);
        mc.player.fallDistance = 0.0F;
        this.resetFallState();
        this.cooldown = this.cooldownTicks.getValueInt();

        if (this.set.getValue()) {
            // The wind-charge cycle is complete only after MaceAura has pulled back.
            // Resetting here prevents Set from throwing another charge during the return.
            this.resetSetCycle();
        }
    }

    @EventListener
    public void onPacketSend(PacketEvent.Send event) {
        if (nullCheck() || !this.set.getValue() || !this.setGroundSpoof.getValue()) return;
        if (event.getPacket() instanceof PlayerMoveC2SPacket packet && packet.isOnGround()) {
            ((IPlayerMoveC2SPacket) packet).setOnGround(false);
        }
    }

    private void handleSet() {
        if (this.setAwaitingReturn && mc.player.isOnGround() && this.setStage == 0) {
            // Recover if the charge never produced a valid fall/return cycle.
            this.setAwaitingReturn = false;
            this.setLoopTimer.reset();
        }

        PlayerEntity target = this.findSetTarget(this.setAttackRange.getValue());
        if (target == null) {
            this.resetSetState();
            return;
        }

        if (this.setStage == 0) {
            // One wind charge belongs to one pull-back cycle. Do not throw another
            // while falling, during the rebound cooldown, or before the prior return.
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

        if (!this.setHasSwitched && this.setStageTimer.passedMs(this.setSwitchDelay.getValueInt())) {
            int maceSlot = InventoryUtil.findItem(Items.MACE);
            if (maceSlot != -1 && mc.player.getInventory().selectedSlot != maceSlot) {
                mc.player.getInventory().selectedSlot = maceSlot;
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(maceSlot));
            }
            this.setHasSwitched = true;
        }

        if (!this.setHasLagged && this.setStageTimer.passedMs(this.setLagDelay.getValueInt())) {
            this.applySetLagBoost();
            this.setHasLagged = true;
        }

        if (this.setStageTimer.passedMs(this.setAttackDelay.getValueInt())) {
            if (this.falling
                    && this.setTarget != null
                    && this.setTarget.isAlive()
                    && mc.player.distanceTo(this.setTarget) <= this.setAttackRange.getValue()) {
                mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(this.setTarget, mc.player.isSneaking()));
                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                Vec3d start = this.fallStart;
                if (start != null) {
                    this.performReturn(start);
                    return;
                }
            }
            this.resetSetState();
        }
    }

    private void applySetLagBoost() {
        switch (this.setLagMode.getValue()) {
            case Smart -> {
                List<BlockPos> candidates = new ArrayList<>();
                for (double x = mc.player.getX() - this.setSmartXZ.getValue(); x < mc.player.getX() + this.setSmartXZ.getValue(); x++) {
                    for (double z = mc.player.getZ() - this.setSmartXZ.getValue(); z < mc.player.getZ() + this.setSmartXZ.getValue(); z++) {
                        for (double y = mc.player.getY() - this.setSmartDown.getValue(); y < mc.player.getY() + this.setSmartUp.getValue(); y++) {
                            candidates.add(new BlockPosX(x, y, z));
                        }
                    }
                }

                double bestDistance = Double.MAX_VALUE;
                BlockPos bestPos = null;
                for (BlockPos pos : candidates) {
                    if (!this.canSetMove(pos)) continue;
                    float distance = MathHelper.sqrt((float) mc.player.squaredDistanceTo(pos.toCenterPos().add(0.0, -0.5, 0.0)));
                    if (distance < (float) this.setSmartDistance.getValue()) continue;
                    double squaredDistance = mc.player.squaredDistanceTo(pos.toCenterPos());
                    if (squaredDistance < bestDistance) {
                        bestDistance = squaredDistance;
                        bestPos = pos;
                    }
                }

                if (bestPos != null) {
                    this.sendSetPosition(bestPos.getX() + 0.5, bestPos.getY(), bestPos.getZ() + 0.5, false);
                }
            }
            case Invalid -> {
                for (int i = 0; i < 20; i++) {
                    this.sendSetPosition(mc.player.getX(), mc.player.getY() + 1337.0, mc.player.getZ(), false);
                }
            }
            case TrollHack -> this.sendSetPosition(mc.player.getX(), mc.player.getY() + 2.3400880035762786, mc.player.getZ(), false);
            case ToVoid -> this.sendSetPosition(mc.player.getX(), -70.0, mc.player.getZ(), false);
            case ToVoid2 -> this.sendSetPosition(mc.player.getX(), -7.0, mc.player.getZ(), false);
            case Normal -> this.sendSetPosition(mc.player.getX(), mc.player.getY() + 1.9, mc.player.getZ(), false);
            case Rotation -> {
                mc.getNetworkHandler().sendPacket(new LookAndOnGround(-180.0F, -90.0F, false));
                mc.getNetworkHandler().sendPacket(new LookAndOnGround(180.0F, 90.0F, false));
            }
            case Fly -> this.sendSetPosition(mc.player.getX(), mc.player.getY() + this.setLagFly.getValue(), mc.player.getZ(), false);
            case Glide -> {
                this.sendSetPosition(mc.player.getX(), mc.player.getY() + 1.1001, mc.player.getZ(), false);
                this.sendSetPosition(mc.player.getX(), mc.player.getY() + 1.0605, mc.player.getZ(), false);
                this.sendSetPosition(mc.player.getX(), mc.player.getY() + 1.0802, mc.player.getZ(), false);
                this.sendSetPosition(mc.player.getX(), mc.player.getY() + 1.1127, mc.player.getZ(), false);
            }
        }
    }

    private boolean throwSetWindChargeDown() {
        int windChargeSlot;
        boolean fromInventory = false;

        if (mc.player.getMainHandStack().getItem() == Items.WIND_CHARGE) {
            windChargeSlot = mc.player.getInventory().selectedSlot;
        } else if ((windChargeSlot = InventoryUtil.findItem(Items.WIND_CHARGE)) != -1) {
            // The wind charge is already in the hotbar.
        } else if ((windChargeSlot = InventoryUtil.findItemInventorySlotFromZero(Items.WIND_CHARGE)) != -1) {
            fromInventory = true;
        } else {
            return false;
        }

        float yaw = mc.player.getYaw();
        float pitch = 90.0F;
        Perish.ROTATION.snapAt(yaw, pitch);

        if (mc.player.getMainHandStack().getItem() == Items.WIND_CHARGE) {
            mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, yaw, pitch));
            mc.player.swingHand(Hand.MAIN_HAND);
        } else if (fromInventory) {
            InventoryUtil.inventorySwap(windChargeSlot, mc.player.getInventory().selectedSlot);
            mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, yaw, pitch));
            mc.player.swingHand(Hand.MAIN_HAND);
            InventoryUtil.inventorySwap(windChargeSlot, mc.player.getInventory().selectedSlot);
            EntityUtil.syncInventory();
        } else {
            int oldSlot = mc.player.getInventory().selectedSlot;
            InventoryUtil.switchToSlot(windChargeSlot);
            mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, yaw, pitch));
            mc.player.swingHand(Hand.MAIN_HAND);
            InventoryUtil.switchToSlot(oldSlot);
        }

        Perish.ROTATION.snapBack();
        return true;
    }

    private boolean canSetMove(BlockPos pos) {
        return mc.world.isAir(pos) && mc.world.isAir(pos.up());
    }

    private void sendSetPosition(double x, double y, double z, boolean onGround) {
        mc.getNetworkHandler().sendPacket(new PositionAndOnGround(x, y, z, onGround));
    }

    private void resetSetState() {
        this.setStage = 0;
        this.setHasSwitched = false;
        this.setHasLagged = false;
        this.setTarget = null;
        this.setLoopTimer.reset();
    }

    private void resetSetCycle() {
        this.resetSetState();
        this.setAwaitingReturn = false;
    }

    private PlayerEntity findSetTarget(double maxRange) {
        PlayerEntity best = null;
        double bestDistance = Double.MAX_VALUE;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !player.isAlive() || Perish.FRIEND.isFriend(player)) continue;
            double distance = mc.player.distanceTo(player);
            if (distance > maxRange || distance >= bestDistance) continue;
            bestDistance = distance;
            best = player;
        }
        return best;
    }

    private void sendBurrowReturn(Vec3d start) {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();

        for (double offset : BURROW_OFFSETS) {
            this.sendPosition(x, y + offset, z);
        }

        Vec3d burrowTop = new Vec3d(x, y + BURROW_OFFSETS[BURROW_OFFSETS.length - 1], z);
        this.sendHorizontalStepPackets(burrowTop, start);
        Vec3d spoofPos = new Vec3d(start.x, start.y + this.spoofHeight.getValue(), start.z);
        this.sendTeleportPadding(burrowTop, spoofPos);
        this.sendFullPosition(spoofPos.x, spoofPos.y, spoofPos.z, false);
        this.sendTeleportPadding(spoofPos, start);
        this.sendFullPosition(start.x, start.y, start.z, true);
    }

    private void sendHorizontalStepPackets(Vec3d from, Vec3d target) {
        double x = from.x;
        double z = from.z;
        double y = from.y;

        while (Math.abs(x - target.x) > RETURN_STEP) {
            x += x > target.x ? -RETURN_STEP : RETURN_STEP;
            this.sendPosition(x, y, z);
        }

        while (Math.abs(z - target.z) > RETURN_STEP) {
            z += z > target.z ? -RETURN_STEP : RETURN_STEP;
            this.sendPosition(x, y, z);
        }

        if (x != target.x || z != target.z) {
            this.sendPosition(target.x, y, target.z);
        }
    }

    private void sendTeleportPadding(Vec3d from, Vec3d to) {
        int packetsRequired = (int) Math.ceil(from.distanceTo(to) / 10.0) - 1;

        for (int i = 0; i < packetsRequired; i++) {
            sendPacket(new OnGroundOnly(true));
        }
    }

    private void sendPosition(double x, double y, double z) {
        sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false));
    }

    private void sendFullPosition(double x, double y, double z, boolean onGround) {
        sendPacket(new Full(x, y, z, mc.player.getYaw(), mc.player.getPitch(), onGround));
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
            if (this.renderMode.is(RenderMode.Silent)) {
                this.silentCameraPos = this.fallStart.add(0.0, mc.player.getEyeHeight(mc.player.getPose()), 0.0);
                this.resetSilentRotation();
            }
        }
    }

    private void handleSilentReturnTriggers() {
        if (!this.renderMode.is(RenderMode.Silent)) {
            this.silentMovementReturnLatched = false;
            this.silentElytraReturnLatched = false;
            return;
        }

        if (MovementUtil.isMoving()) {
            if (!this.silentMovementReturnLatched) {
                this.silentMovementReturnLatched = true;
                this.returnForSilentTrigger();
            }
        } else {
            this.silentMovementReturnLatched = false;
        }

        boolean elytraFlying = ElytraFly.INSTANCE != null
                && ElytraFly.INSTANCE.isOn()
                && ElytraFly.INSTANCE.isFallFlying();
        if (elytraFlying) {
            if (!this.silentElytraReturnLatched) {
                this.silentElytraReturnLatched = true;
                this.returnForSilentTrigger();
            }
        } else {
            this.silentElytraReturnLatched = false;
        }
    }

    private void returnForSilentTrigger() {
        if (this.falling && this.fallStart != null) {
            this.performReturn(this.fallStart);
        }
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (this.isSilentRenderActive()) {
            dev.perish.api.utils.render.Render3DUtil.drawBox(matrixStack, mc.player.getBoundingBox(), this.silentPlayerBox.getValue());
        }
        this.previousSilentYaw = this.silentYaw;
        this.previousSilentPitch = this.silentPitch;
    }

    @EventListener
    public void onLookDirection(LookDirectionEvent event) {
        if (!this.isSilentRenderActive()) {
            return;
        }

        this.silentYaw += (float) event.getCursorDeltaX() * 0.15F;
        this.silentPitch = MathHelper.clamp(this.silentPitch + (float) event.getCursorDeltaY() * 0.15F, -90.0F, 90.0F);
        event.cancel();
    }

    public boolean isSilentRenderActive() {
        return this.isOn()
                && this.renderMode.is(RenderMode.Silent)
                && this.falling
                && this.silentCameraPos != null
                && this.hasHorizontalSilentCameraTarget();
    }

    public boolean shouldSuppressNoFall() {
        return this.isOn() && this.falling && this.target != null && CombatUtil.isValid(this.target);
    }

    private boolean isHorizontallyAttackable(Entity entity) {
        if (!CombatUtil.isValid(entity)) {
            return false;
        }

        Vec3d hitVec = this.getAttackVec(entity);
        double x = mc.player.getX() - hitVec.x;
        double z = mc.player.getZ() - hitVec.z;
        return x * x + z * z <= this.attackRange.getValue() * this.attackRange.getValue();
    }

    private boolean hasHorizontalSilentCameraTarget() {
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (this.isHorizontallyAttackable(player)) {
                return true;
            }
        }
        return false;
    }

    public Vec3d getSilentCameraPos() {
        return this.silentCameraPos;
    }

    public float getSilentYaw() {
        return MathHelper.lerp(mc.getRenderTickCounter().getTickDelta(true), this.previousSilentYaw, this.silentYaw);
    }

    public float getSilentPitch() {
        return MathHelper.lerp(mc.getRenderTickCounter().getTickDelta(true), this.previousSilentPitch, this.silentPitch);
    }

    private void resetSilentRotation() {
        if (mc.player == null) {
            return;
        }
        this.silentYaw = mc.player.getYaw();
        this.silentPitch = mc.player.getPitch();
        this.previousSilentYaw = this.silentYaw;
        this.previousSilentPitch = this.silentPitch;
    }

    private boolean canUseMace() {
        if (mc.player.getMainHandStack().getItem() == Items.MACE) {
            return true;
        }

        if (!this.silent.getValue()) {
            return false;
        }

        return this.getMaceSlot() != -1;
    }

    private boolean isAttackable(Entity entity) {
        if (!CombatUtil.isValid(entity)) {
            return false;
        }

        Vec3d hitVec = this.getAttackVec(entity);
        return mc.player.getEyePos().distanceTo(hitVec) <= this.attackRange.getValue();
    }

    private Vec3d getAttackVec(Entity entity) {
        return MathUtil.getClosestPointToBox(mc.player.getEyePos(), entity.getBoundingBox());
    }

    private boolean attackTarget() {
        int oldSlot = mc.player.getInventory().selectedSlot;
        int maceSlot = this.getMaceSlot();
        boolean swapped = false;
        Vec3d hitVec = this.getAttackVec(this.target);

        if (mc.player.getMainHandStack().getItem() != Items.MACE) {
            if (!this.silent.getValue() || maceSlot == -1) {
                return false;
            }

            this.doSwap(maceSlot);
            swapped = true;
        }

        Perish.ROTATION.lookAt(hitVec);
        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(this.target, mc.player.isSneaking()));
        mc.player.resetLastAttackedTicks();
        EntityUtil.swingHand(Hand.MAIN_HAND, this.swingMode.getValue());
        Perish.ROTATION.snapBack();

        if (swapped) {
            if (this.inventorySwap.getValue()) {
                this.doSwap(maceSlot);
                EntityUtil.syncInventory();
            } else {
                InventoryUtil.switchToSlot(oldSlot);
            }
        }
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

    private void doSwap(int slot) {
        if (this.inventorySwap.getValue()) {
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
        } else {
            InventoryUtil.switchToSlot(slot);
        }
    }

    private int getMaceSlot() {
        return this.inventorySwap.getValue() ? InventoryUtil.findItemInventorySlot(Items.MACE) : InventoryUtil.findItem(Items.MACE);
    }

    private void resetFallState() {
        this.fallStart = null;
        this.falling = false;
        this.silentCameraPos = null;
    }

    public enum LagMode {
        Smart,
        Invalid,
        TrollHack,
        ToVoid,
        ToVoid2,
        Normal,
        Rotation,
        Fly,
        Glide
    }

    public enum RenderMode {
        Normal,
        Silent
    }
}
