package dev.perish.mod.modules.impl.movement;

import dev.perish.Perish;
import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.TravelEvent;
import dev.perish.api.events.impl.UpdateEvent;
import dev.perish.api.events.impl.UpdateRotateEvent;
import dev.perish.api.utils.math.Timer;
import dev.perish.api.utils.player.EntityUtil;
import dev.perish.api.utils.player.InventoryUtil;
import dev.perish.api.utils.player.MovementUtil;
import dev.perish.asm.accessors.IFireworkRocketEntity;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.EnumSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;

import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class ElytraFly extends Module {
    public static ElytraFly INSTANCE;

    public final EnumSetting<Mode> mode = this.add(new EnumSetting<>("Mode", Mode.Grim));

    public enum Mode {
        Grim,
        NCP
    }

    private final SliderSetting fireWorkDelay = this.add(new SliderSetting("FireWorkDelay", 1500.0, 200.0, 10000.0, 50.0, () -> mode.is(Mode.Grim)).setSuffix("ms"));
    private final SliderSetting packetDelay = this.add(new SliderSetting("PacketDelay", 0.0, 0.0, 20.0, 1.0, () -> mode.is(Mode.Grim)));
    private final BooleanSetting noAnimation = this.add(new BooleanSetting("NoAnimation", false, () -> mode.is(Mode.Grim)));
    private final BooleanSetting setFlag = this.add(new BooleanSetting("SetFlag", false, () -> mode.is(Mode.Grim)));
    private final SliderSetting defaultPitch = this.add(new SliderSetting("DefaultPitch", -10.0, -90.0, 90.0, 1.0, () -> mode.is(Mode.Grim)).setSuffix("°"));
    private final SliderSetting grimUpPitch = this.add(new SliderSetting("UpPitch", -45.0, -90.0, 90.0, 1.0, () -> mode.is(Mode.Grim)).setSuffix("°"));
    private final SliderSetting downPitch = this.add(new SliderSetting("DownPitch", 45.0, -90.0, 90.0, 1.0, () -> mode.is(Mode.Grim)).setSuffix("°"));
    private final BooleanSetting controlRotation = this.add(new BooleanSetting("ControlRotation", false, () -> mode.is(Mode.Grim)));
    private final BooleanSetting airStop = this.add(new BooleanSetting("AirStop", false, () -> mode.is(Mode.Grim)));

    private final SliderSetting ncpFireWorkDelay = this.add(new SliderSetting("FireWorkDelay", 1500.0, 200.0, 10000.0, 50.0, () -> mode.is(Mode.NCP)).setSuffix("ms"));
    private final BooleanSetting ncpFirework = this.add(new BooleanSetting("Firework", true, () -> mode.is(Mode.NCP)));
    private final SliderSetting upFactor = this.add(new SliderSetting("UpFactor", 1.0, 0.0, 10.0, () -> mode.is(Mode.NCP)));
    private final SliderSetting downFactor = this.add(new SliderSetting("FallSpeed", 1.0, 0.0, 10.0, () -> mode.is(Mode.NCP)));
    private final SliderSetting speed = this.add(new SliderSetting("Speed", 1.0, 0.1, 10.0, () -> mode.is(Mode.NCP)));

    private final BooleanSetting slownessModifier = this.add(new BooleanSetting("SlownessModifier", false, () -> mode.is(Mode.NCP)).setParent());
    private final SliderSetting slownessSpeed = this.add(new SliderSetting("SlownessSpeed", 1.0, 0.1, 10.0, () -> mode.is(Mode.NCP) && slownessModifier.isOpen()));
    private final BooleanSetting slownessTimer = this.add(new BooleanSetting("SlownessTimer", false, () -> mode.is(Mode.NCP) && slownessModifier.isOpen()).setParent());
    private final SliderSetting slownessTimerValue = this.add(new SliderSetting("TimerSpeed", 1.5, 0.1, 10.0, 0.1, () -> mode.is(Mode.NCP) && slownessTimer.isOpen()));

    private final BooleanSetting speedLimit = this.add(new BooleanSetting("SpeedLimit", true, () -> mode.is(Mode.NCP)));
    private final SliderSetting maxSpeed = this.add(new SliderSetting("MaxSpeed", 2.5, 0.1, 10.0, () -> mode.is(Mode.NCP) && speedLimit.getValue()));
    private final BooleanSetting noDrag = this.add(new BooleanSetting("NoDrag", false, () -> mode.is(Mode.NCP)));
    private final SliderSetting sneakDownSpeed = this.add(new SliderSetting("DownSpeed", 1.0, 0.1, 10.0, () -> mode.is(Mode.NCP)));

    private final Timer fireworkTimer = new Timer();
    private boolean hasElytra;
    private int packetDelayCounter;
    private float controlYaw;
    private float controlPitch;
    private boolean overridePitch;
    private boolean overrideYaw;
    private boolean slownessTimerApplied;

    public ElytraFly() {
        super("ElytraFly", Module.Category.Movement);
        this.setChinese("鞘翅飞行");
        INSTANCE = this;
    }

    @Override
    public String getInfo() {
        return mode.getValue().name();
    }

    @Override
    public void onEnable() {
        if (nullCheck()) return;
        hasElytra = false;
        packetDelayCounter = 0;
        fireworkTimer.setMs(99999999L);
        overridePitch = false;
        overrideYaw = false;
        slownessTimerApplied = false;
    }

    @Override
    public void onDisable() {
        hasElytra = false;
        overridePitch = false;
        overrideYaw = false;
        resetSlownessTimer();
    }

    private boolean isElytraEquipped() {
        if (mc.player == null) return false;
        for (ItemStack stack : mc.player.getArmorItems()) {
            if (stack.getItem() instanceof ElytraItem) return true;
        }
        return false;
    }

    private double getEffectiveNcpSpeed() {
        if (slownessModifier.getValue() && mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
            return slownessSpeed.getValue();
        }
        return speed.getValue();
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (nullCheck()) return;

        if (mode.is(Mode.Grim)) {
            resetSlownessTimer();
            handleGrimMode();
        } else {
            handleNCPMode();
        }
    }

    @EventListener(priority = -9999)
    public void onRotation(UpdateRotateEvent event) {
        if (nullCheck() || !mode.is(Mode.Grim) || !controlRotation.getValue() || !isFallFlying()) return;

        if (overrideYaw) event.setYaw(controlYaw);
        if (overridePitch) event.setPitch(controlPitch);
    }

    @EventListener
    public void onNCPTravel(TravelEvent event) {
        if (nullCheck() || !mode.is(Mode.NCP)) return;
        if (!isElytraEquipped() || !isFallFlying() || event.isPost()) return;

        boolean isJumping = mc.player.input.jumping;
        boolean isSneaking = mc.player.input.sneaking;

        if (!mc.player.isOnGround() && mc.player.getVelocity().y < -0.05) {
            setY(0.0);
        }

        if (ncpFirework.getValue()) {
            if (isSneaking && isJumping) {
                setY(0.0);
            } else if (isSneaking) {
                setY(-sneakDownSpeed.getValue());
            } else if (isJumping) {
                setY(upFactor.getValue());
            } else {
                setY(-3.0E-11 * downFactor.getValue());
            }

            double[] direction = MovementUtil.directionSpeed(getEffectiveNcpSpeed());
            setX(direction[0]);
            setZ(direction[1]);
        } else {
            Vec3d lookVec = getRotationVec(mc.getRenderTickCounter().getTickDelta(true));
            double lookDist = Math.sqrt(lookVec.x * lookVec.x + lookVec.z * lookVec.z);
            double motionDist = Math.sqrt(getX() * getX() + getZ() * getZ());

            if (isSneaking) {
                setY(-sneakDownSpeed.getValue());
            } else if (!isJumping) {
                setY(-3.0E-11 * downFactor.getValue());
            }

            if (isJumping) {
                if (motionDist > upFactor.getValue() / upFactor.getMax()) {
                    double rawUpSpeed = motionDist * 0.01325;
                    setY(getY() + rawUpSpeed * 3.2);
                    if (lookDist > 0.0) {
                        setX(getX() - lookVec.x * rawUpSpeed / lookDist);
                        setZ(getZ() - lookVec.z * rawUpSpeed / lookDist);
                    }
                } else {
                    double[] direction = MovementUtil.directionSpeed(getEffectiveNcpSpeed());
                    setX(direction[0]);
                    setZ(direction[1]);
                }
            }

            if (lookDist > 0.0) {
                setX(getX() + (lookVec.x / lookDist * motionDist - getX()) * 0.1);
                setZ(getZ() + (lookVec.z / lookDist * motionDist - getZ()) * 0.1);
            }

            if (!isJumping) {
                double[] direction = MovementUtil.directionSpeed(getEffectiveNcpSpeed());
                setX(direction[0]);
                setZ(direction[1]);
            }
        }

        if (!noDrag.getValue()) {
            setY(getY() * 0.99F);
            setX(getX() * 0.98F);
            setZ(getZ() * 0.99F);
        }

        double finalDist = Math.sqrt(getX() * getX() + getZ() * getZ());
        if (speedLimit.getValue() && finalDist > maxSpeed.getValue()) {
            setX(getX() * maxSpeed.getValue() / finalDist);
            setZ(getZ() * maxSpeed.getValue() / finalDist);
        }

        event.cancel();
        mc.player.move(MovementType.SELF, mc.player.getVelocity());
    }

    private void handleNCPMode() {
        if (!isElytraEquipped()) {
            resetSlownessTimer();
            return;
        }

        boolean isSneaking = mc.options.sneakKey.isPressed();
        if (!isFallFlying()
                && !mc.player.isOnGround()
                && mc.player.getVelocity().getY() < -0.1
                && mc.player.fallDistance > 0.2F
                && !isSneaking) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            mc.player.startFallFlying();
        }

        boolean shouldApplyTimer = isFallFlying()
                && slownessModifier.getValue()
                && slownessTimer.getValue()
                && mc.player.hasStatusEffect(StatusEffects.SLOWNESS);

        if (shouldApplyTimer) {
            Perish.TIMER.set(slownessTimerValue.getValueFloat());
            slownessTimerApplied = true;
        } else {
            resetSlownessTimer();
        }

        if (isFallFlying() && MovementUtil.isMoving()) {
            handleNCPFirework();
        }
    }

    private void resetSlownessTimer() {
        if (slownessTimerApplied && Perish.TIMER != null) {
            Perish.TIMER.reset();
            slownessTimerApplied = false;
        }
    }

    private void handleGrimMode() {
        if (mc.player.isOnGround()) {
            packetDelayCounter = 0;
            return;
        }

        hasElytra = InventoryUtil.findItemInventorySlot(Items.ELYTRA) != -1;
        if (!hasElytra) return;

        packetDelayCounter++;
        if (packetDelayCounter <= packetDelay.getValueInt()) return;
        packetDelayCounter = 0;

        int elytraHotbar = InventoryUtil.findItem(Items.ELYTRA);
        if (elytraHotbar != -1) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, elytraHotbar, SlotActionType.SWAP, mc.player);
            sendStartFlyingPacket();
            if (shouldUseFirework()) handleGrimFirework();
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, elytraHotbar, SlotActionType.SWAP, mc.player);
        } else {
            int elytraInventory = InventoryUtil.findItemInventorySlot(Items.ELYTRA);
            if (elytraInventory == -1) return;

            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, elytraInventory, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
            sendStartFlyingPacket();
            if (shouldUseFirework()) handleGrimFirework();
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, elytraInventory, 0, SlotActionType.PICKUP, mc.player);
        }

        if (controlRotation.getValue() && isFallFlying()) {
            updateGrimRotation();
        } else {
            overridePitch = false;
            overrideYaw = false;
        }
    }

    private void sendStartFlyingPacket() {
        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        if (setFlag.getValue()) mc.player.startFallFlying();
    }

    private void updateGrimRotation() {
        boolean forward = mc.options.forwardKey.isPressed();
        boolean back = mc.options.backKey.isPressed();
        boolean left = mc.options.leftKey.isPressed();
        boolean right = mc.options.rightKey.isPressed();
        boolean jump = mc.options.jumpKey.isPressed();
        boolean sneak = mc.options.sneakKey.isPressed();
        boolean hasHorizontalInput = forward || back || left || right;
        boolean hasVerticalInput = jump || sneak;

        if (hasHorizontalInput) {
            overrideYaw = true;
            controlYaw = getSprintYaw(mc.player.getYaw());
        } else {
            overrideYaw = false;
        }

        overridePitch = true;
        if (hasVerticalInput) {
            if (hasHorizontalInput) {
                if (jump && !sneak) controlPitch = grimUpPitch.getValueFloat();
                else if (sneak && !jump) controlPitch = downPitch.getValueFloat();
                else controlPitch = defaultPitch.getValueFloat();
            } else {
                if (jump && !sneak) controlPitch = -90.0F;
                else if (sneak && !jump) controlPitch = 90.0F;
                else controlPitch = defaultPitch.getValueFloat();
            }
        } else {
            controlPitch = defaultPitch.getValueFloat();
        }
    }

    private static float getSprintYaw(float yaw) {
        if (mc.options.forwardKey.isPressed() && !mc.options.backKey.isPressed()) {
            if (mc.options.leftKey.isPressed() && !mc.options.rightKey.isPressed()) yaw -= 45.0F;
            else if (mc.options.rightKey.isPressed() && !mc.options.leftKey.isPressed()) yaw += 45.0F;
        } else if (mc.options.backKey.isPressed() && !mc.options.forwardKey.isPressed()) {
            yaw += 180.0F;
            if (mc.options.leftKey.isPressed() && !mc.options.rightKey.isPressed()) yaw += 45.0F;
            else if (mc.options.rightKey.isPressed() && !mc.options.leftKey.isPressed()) yaw -= 45.0F;
        } else if (mc.options.leftKey.isPressed() && !mc.options.rightKey.isPressed()) {
            yaw -= 90.0F;
        } else if (mc.options.rightKey.isPressed() && !mc.options.leftKey.isPressed()) {
            yaw += 90.0F;
        }
        return MathHelper.wrapDegrees(yaw);
    }

    @EventListener
    public void onGrimTravel(TravelEvent event) {
        if (nullCheck() || !mode.is(Mode.Grim) || !airStop.getValue() || !isFallFlying() || event.isPost()) return;

        boolean hasInput = mc.options.forwardKey.isPressed()
                || mc.options.backKey.isPressed()
                || mc.options.leftKey.isPressed()
                || mc.options.rightKey.isPressed()
                || mc.options.jumpKey.isPressed()
                || mc.options.sneakKey.isPressed();

        if (!hasInput) {
            MovementUtil.setMotionX(0.0);
            MovementUtil.setMotionY(0.0);
            MovementUtil.setMotionZ(0.0);
            event.cancel();
            mc.player.move(MovementType.SELF, mc.player.getVelocity());
        }
    }

    private boolean shouldUseFirework() {
        return mc.options.forwardKey.isPressed()
                || mc.options.backKey.isPressed()
                || mc.options.leftKey.isPressed()
                || mc.options.rightKey.isPressed()
                || mc.options.jumpKey.isPressed();
    }

    private void handleNCPFirework() {
        if (!ncpFirework.getValue() || mc.player.isUsingItem()) return;
        if (!fireworkTimer.passed((long) ncpFireWorkDelay.getValue())) return;
        if (hasActiveFirework()) return;
        useFirework();
        fireworkTimer.reset();
    }

    private void handleGrimFirework() {
        if (mc.player.isUsingItem()) return;
        if (!fireworkTimer.passed((long) fireWorkDelay.getValue())) return;
        if (hasActiveFirework()) return;
        useFirework();
        fireworkTimer.reset();
    }

    private boolean hasActiveFirework() {
        for (Entity entity : Perish.THREAD.getEntities()) {
            if (entity instanceof FireworkRocketEntity firework
                    && ((IFireworkRocketEntity) firework).getShooter() == mc.player) {
                return true;
            }
        }
        return false;
    }

    private void useFirework() {
        if (mc.player.getMainHandStack().isOf(Items.FIREWORK_ROCKET)) {
            sendFireworkPacket();
            return;
        }

        int firework = InventoryUtil.findItemInventorySlot(Items.FIREWORK_ROCKET);
        if (firework != -1) {
            InventoryUtil.inventorySwap(firework, mc.player.getInventory().selectedSlot);
            sendFireworkPacket();
            InventoryUtil.inventorySwap(firework, mc.player.getInventory().selectedSlot);
            EntityUtil.syncInventory();
            return;
        }

        firework = InventoryUtil.findItem(Items.FIREWORK_ROCKET);
        if (firework != -1) {
            int oldSlot = mc.player.getInventory().selectedSlot;
            InventoryUtil.switchToSlot(firework);
            sendFireworkPacket();
            InventoryUtil.switchToSlot(oldSlot);
        }
    }

    private void sendFireworkPacket() {
        sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(
                Hand.MAIN_HAND,
                id,
                Perish.ROTATION.getLastYaw(),
                Perish.ROTATION.getLastPitch()
        ));
    }

    private Vec3d getRotationVec(float tickDelta) {
        float yaw = mc.player.getYaw(tickDelta);
        float yawRadians = -yaw * ((float) Math.PI / 180.0F);
        float horizontalCos = MathHelper.cos(yawRadians);
        float horizontalSin = MathHelper.sin(yawRadians);
        return new Vec3d(horizontalSin, 0.0, horizontalCos);
    }

    private double getX() {
        return MovementUtil.getMotionX();
    }

    private void setX(double value) {
        MovementUtil.setMotionX(value);
    }

    private double getY() {
        return MovementUtil.getMotionY();
    }

    private void setY(double value) {
        MovementUtil.setMotionY(value);
    }

    private double getZ() {
        return MovementUtil.getMotionZ();
    }

    private void setZ(double value) {
        MovementUtil.setMotionZ(value);
    }

    public boolean isFallFlying() {
        if (mode.is(Mode.Grim)) {
            return mc.player != null && (mc.player.isFallFlying() || (hasElytra && !mc.player.isOnGround()));
        }
        return mc.player != null && mc.player.isFallFlying();
    }

    public boolean isNoAnimationActive() {
        return isOn() && mode.is(Mode.Grim) && noAnimation.getValue();
    }

    public boolean isPacketMode() {
        return isOn() && mode.is(Mode.Grim);
    }
}
