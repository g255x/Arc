package dev.perish.mod.modules.impl.movement;

import dev.perish.Perish;
import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.eventbus.EventPriority;
import dev.perish.api.events.impl.MoveEvent;
import dev.perish.api.events.impl.PacketEvent;
import dev.perish.api.utils.player.MovementUtil;
import dev.perish.api.utils.math.Timer;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;

public class LongJump extends Module {
    public static LongJump INSTANCE;
    public final SliderSetting strafeSpeed = add(new SliderSetting("Boost", 1, 0, 2, 0.1));
    public final BooleanSetting step = add(new BooleanSetting("Step", false));
    private final BooleanSetting autoDisable = add(new BooleanSetting("AutoDisable", true));
    private final BooleanSetting inBlock = add(new BooleanSetting("InBlock", true));
    private final Timer lagTimer = new Timer();
    boolean needDisable = false;
    private boolean stop;
    private double speed;
    private double distance;
    private int stage;
    private boolean boost;

    public LongJump() {
        super("LongJump", Category.Movement);
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        needDisable = false;
        if (step.getValue() && Step.INSTANCE != null) {
            Step.INSTANCE.enable();
        }
        if (mc.player != null) {
            speed = MovementUtil.getSpeed(false);
            distance = MovementUtil.getDistance2D();
        }
        stage = 4;
    }

    @Override
    public void onDisable() {
        if (step.getValue() && Step.INSTANCE != null && Step.INSTANCE.isOn()) {
            Step.INSTANCE.disable();
        }
    }

    @EventListener(priority = EventPriority.HIGH)
    public void invoke(PacketEvent.Receive event) {
        if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket packet) {
        } else if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            lagTimer.reset();
            if (mc.player != null) {
                this.distance = 0.0;
            }
            this.speed = 0.0;
            this.stage = 4;
        }
    }

    @EventListener
    public void onUpdate() {
        if (!mc.player.isOnGround()) {
            needDisable = true;
        } else {
            if (needDisable && autoDisable.getValue()) {
                disable();
            }
        }
        double dx = mc.player.getX() - mc.player.prevX;
        double dz = mc.player.getZ() - mc.player.prevZ;
        distance = Math.sqrt(dx * dx + dz * dz);
    }

    @EventListener
    public void invoke(MoveEvent event) {
        if (mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)) return;
        MovementUtil.setMotionX(0);
        MovementUtil.setMotionZ(0);
        if (mc.player.isSubmergedInWater() || mc.player.isTouchingWater() || mc.player.isInLava() || mc.player.isRiding() || mc.player.isHoldingOntoLadder() || !inBlock.getValue() && Perish.PLAYER.isInBurrow(mc.player) || mc.player.getAbilities().flying || mc.player.isFallFlying() || !MovementUtil.isMoving()) {
            resetStrafe();
            this.stop = true;
            return;
        }
        if (this.stop) {
            this.stop = false;
            return;
        }
        if (!lagTimer.passedMs(100)) {
            return;
        }
        if (this.stage == 1) {
            this.speed = 1.35 * MovementUtil.getSpeed(false, this.strafeSpeed.getValue()) - 0.01;
        } else if (this.stage == 2 && mc.player.isOnGround()) {
            double yMotion = 0.3999 + MovementUtil.getJumpSpeed();
            MovementUtil.setMotionY(yMotion);
            event.setY(yMotion);
            this.speed = this.speed * (this.boost ? 1.6835 : 1.395);
        } else if (this.stage == 3) {
            this.speed = this.distance - 0.66
                    * (this.distance - MovementUtil.getSpeed(false, this.strafeSpeed.getValue()));
            this.boost = !this.boost;
        } else {
            if ((mc.world.canCollide(null, mc.player.getBoundingBox().offset(0.0, MovementUtil.getMotionY(), 0.0)) || mc.player.collidedSoftly) && this.stage > 0) {
                this.stage = 1;
            }
            this.speed = this.distance - this.distance / 159.0;
        }
        this.speed = Math.min(this.speed, 10);
        this.speed = Math.max(this.speed, MovementUtil.getSpeed(false, this.strafeSpeed.getValue()));
        double n = MovementUtil.getMoveForward();
        double n2 = MovementUtil.getMoveStrafe();
        double n3 = mc.player.getYaw();
        if (n == 0.0 && n2 == 0.0) {
            event.setX(0.0);
            event.setZ(0.0);
        } else if (n != 0.0 && n2 != 0.0) {
            n *= Math.sin(0.7853981633974483);
            n2 *= Math.cos(0.7853981633974483);
        }
        event.setX((n * this.speed * -Math.sin(Math.toRadians(n3)) + n2 * this.speed * Math.cos(Math.toRadians(n3))) * 0.99);
        event.setZ((n * this.speed * Math.cos(Math.toRadians(n3)) - n2 * this.speed * -Math.sin(Math.toRadians(n3))) * 0.99);
        this.stage++;
    }

    public void resetStrafe() {
        speed = 0.0f;
        distance = 0.0;
    }
}
