package dev.perish.mod.modules.impl.movement;

import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.MoveEvent;
import dev.perish.api.events.impl.PacketEvent;
import dev.perish.core.impl.CommandManager;
import dev.perish.api.utils.player.EntityUtil;
import dev.perish.api.utils.player.MovementUtil;
import dev.perish.api.utils.math.Timer;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.Setting;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;
import java.lang.reflect.Field;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;

public class Speed
        extends Module {
    public static Speed INSTANCE;
    private final BooleanSetting jump = new BooleanSetting("Jump", true);
    private final BooleanSetting inWater = new BooleanSetting("InWater", false);
    private final BooleanSetting inBlock = new BooleanSetting("InBlock", false);
    private final SliderSetting strafeSpeed = new SliderSetting("Speed", 287.3, 100.0, 1000.0, 0.1);
    private final SliderSetting strafeY = new SliderSetting("StrafeY", 0.99f, 0.1f, 1.2f, 0.01);
    private final BooleanSetting explosions = new BooleanSetting("Explosions", false);
    private final BooleanSetting explosionHorizontal = new BooleanSetting("ExplosionH", true);
    private final BooleanSetting explosionVertical = new BooleanSetting("ExplosionV", true);
    private final SliderSetting explosionMultiplier = new SliderSetting("ExplosionH-Factor", 1.0, 0.0, 5.0, 0.1);
    private final SliderSetting explosionVerticalFactor = new SliderSetting("ExplosionV-Factor", 1.0, 0.0, 5.0, 0.1);
    private final BooleanSetting velocity = new BooleanSetting("Velocity", false);
    private final SliderSetting multiplier = new SliderSetting("H-Factor", 1.0, 0.0, 5.0, 0.1);
    private final SliderSetting vertical = new SliderSetting("V-Factor", 1.0, 0.0, 5.0, 0.1);
    private final SliderSetting coolDown = new SliderSetting("CoolDown", 1000.0, 0.0, 5000.0, 1.0);
    private final SliderSetting lagTime = new SliderSetting("LagTime", 500.0, 0.0, 1000.0, 1.0);
    private final BooleanSetting scaleCap = new BooleanSetting("ScaleCap", false);
    private final BooleanSetting slow = new BooleanSetting("Slowness", false);
    private final BooleanSetting debug = new BooleanSetting("Debug", false);
    private final Timer expTimer = new Timer();
    private final Timer lagTimer = new Timer();
    private boolean stop;
    private double speed;
    private double getDistance;
    private int stage;
    private double lastExp;
    private boolean boost;

    public Speed() {
        super("Speed", Module.Category.Movement);
        INSTANCE = this;
        try {
            for (Field field : Speed.class.getDeclaredFields()) {
                if (!Setting.class.isAssignableFrom(field.getType())) continue;
                Setting setting = (Setting) field.get(this);
                this.addSetting(setting);
            }
        } catch (Exception exception) {
            // empty catch block
        }
    }

    @Override
    public void onEnable() {
        if (mc.player != null) {
            this.speed = MovementUtil.getSpeed(false);
            this.getDistance = MovementUtil.getDistance2D();
        }
        this.stage = 4;
    }

    @EventListener
    public void invoke(PacketEvent.Receive event) {
        Object t = event.getPacket();
        if (t instanceof EntityVelocityUpdateS2CPacket packet) {
            if (mc.player != null && packet.getEntityId() == mc.player.getId() && this.velocity.getValue()) {
                double speed = Math.sqrt(packet.getVelocityX() * packet.getVelocityX() + packet.getVelocityZ() * packet.getVelocityZ()) / 8000.0;
                double d = this.lastExp = this.expTimer.passedMs(this.coolDown.getValueInt()) ? speed : speed - this.lastExp;
                if (this.lastExp > 0.0) {
                    this.expTimer.reset();
                    mc.executeTask(() -> {
                        this.speed += this.lastExp * this.multiplier.getValue();
                        this.getDistance += this.lastExp * this.multiplier.getValue();
                        if (MovementUtil.getMotionY() > 0.0 && this.vertical.getValue() != 0.0) {
                            MovementUtil.setMotionY(MovementUtil.getMotionY() * this.vertical.getValue());
                        }
                    });
                }
            }
        } else if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            this.lagTimer.reset();
            if (mc.player != null) {
                this.getDistance = 0.0;
            }
            this.speed = 0.0;
            this.stage = 4;
        } else {
            Object speed = event.getPacket();
            if (speed instanceof ExplosionS2CPacket packet) {
                if (this.explosions.getValue() && mc.player.squaredDistanceTo(packet.getX(), packet.getY(), packet.getZ()) < 200.0) {
                    double speed2 = Math.sqrt(Math.abs(packet.getPlayerVelocityX() * packet.getPlayerVelocityX()) + Math.abs(packet.getPlayerVelocityZ() * packet.getPlayerVelocityZ()));
                    if (this.debug.getValue()) {
                        CommandManager.sendMessage("explosion speed:" + speed2 + " lastExp:" + this.lastExp);
                    }
                    double d = this.lastExp = this.expTimer.passedMs(this.coolDown.getValueInt()) ? speed2 : speed2 - this.lastExp;
                    if (this.lastExp > 0.0) {
                        if (this.debug.getValue()) {
                            CommandManager.sendMessage("explosion boost");
                        }
                        this.expTimer.reset();

                        if (this.explosionHorizontal.getValue()) {
                            this.speed += this.lastExp * this.explosionMultiplier.getValue();
                            this.getDistance += this.lastExp * this.explosionMultiplier.getValue();
                        }
                        if (this.explosionVertical.getValue() && MovementUtil.getMotionY() > 0.0) {
                            MovementUtil.setMotionY(MovementUtil.getMotionY() * this.explosionVerticalFactor.getValue());
                        }
                    } else if (this.debug.getValue()) {
                        CommandManager.sendMessage("explosion failed boost");
                    }
                }
            }
        }
    }


    @EventListener
    public void onUpdateWalking(MoveEvent event) {
        if (!MovementUtil.isMoving()) {
            MovementUtil.setMotionX(0.0);
            MovementUtil.setMotionZ(0.0);
        }
        this.getDistance = MovementUtil.getDistance2D();
    }


    @EventListener
    public void invoke(MoveEvent event) {
        if (!this.inWater.getValue() && (mc.player.isSubmergedInWater() || mc.player.isTouchingWater()) || mc.player.isHoldingOntoLadder() || !this.inBlock.getValue() && EntityUtil.isInsideBlock()) {
            this.stop = true;
            return;
        }
        if (this.stop) {
            this.stop = false;
            return;
        }
        this.move(event);
    }

    private void move(MoveEvent event) {
        if (!MovementUtil.isMoving() || (HoleSnap.INSTANCE != null && HoleSnap.INSTANCE.isOn())) {
            return;
        }
        if (mc.player.isFallFlying()) {
            return;
        }
        if (!this.lagTimer.passedMs(this.lagTime.getValueInt())) {
            return;
        }

        if (this.stage == 1 && MovementUtil.isMoving()) {
            this.speed = 1.35 * MovementUtil.getSpeed(this.slow.getValue(), this.strafeSpeed.getValue() / 1000.0) - 0.01;
        } else if (this.stage == 2 && mc.player.isOnGround() && MovementUtil.isMoving() && (mc.options.jumpKey.isPressed() || this.jump.getValue())) {
            double yMotion = 0.3999 + MovementUtil.getJumpSpeed();
            MovementUtil.setMotionY(yMotion);
            event.setY(yMotion);
            this.speed *= this.boost ? 1.6835 : 1.395;
        } else if (this.stage == 3) {
            this.speed = this.getDistance - 0.66 * (this.getDistance - MovementUtil.getSpeed(this.slow.getValue(), this.strafeSpeed.getValue() / 1000.0));
            this.boost = !this.boost;
        } else {
            if ((mc.world.canCollide(null, mc.player.getBoundingBox().offset(0.0, MovementUtil.getMotionY(), 0.0)) || mc.player.collidedSoftly) && this.stage > 0) {
                this.stage = MovementUtil.isMoving() ? 1 : 0;
            }
            this.speed = this.getDistance - this.getDistance / 159.0;
        }
        this.speed = Math.max(this.speed, MovementUtil.getSpeed(this.slow.getValue(), this.strafeSpeed.getValue() / 1000.0));
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
        double n4 = this.strafeY.getValue();
        event.setX((n * this.speed * -Math.sin(Math.toRadians(n3)) + n2 * this.speed * Math.cos(Math.toRadians(n3))) * n4);
        event.setZ((n * this.speed * Math.cos(Math.toRadians(n3)) - n2 * this.speed * -Math.sin(Math.toRadians(n3))) * n4);
        if (MovementUtil.isMoving()) {
            ++this.stage;
        }
    }


}

