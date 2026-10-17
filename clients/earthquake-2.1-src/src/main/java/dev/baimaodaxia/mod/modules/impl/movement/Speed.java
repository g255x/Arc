package dev.baimaodaxia.mod.modules.impl.movement;

import dev.baimaodaxia.Alien;
import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.MoveEvent;
import dev.baimaodaxia.api.events.impl.MovedEvent;
import dev.baimaodaxia.api.events.impl.PacketEvent;
import dev.baimaodaxia.api.events.impl.UpdateEvent;
import dev.baimaodaxia.api.utils.math.Timer;
import dev.baimaodaxia.api.utils.path.BaritoneUtil;
import dev.baimaodaxia.api.utils.player.EntityUtil;
import dev.baimaodaxia.api.utils.player.MovementUtil;
import dev.baimaodaxia.api.utils.world.BlockUtil;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;

public class Speed extends Module {
   public static Speed INSTANCE;
   private final EnumSetting<Mode> mode;
   public final SliderSetting collideSpeed;
   private final BooleanSetting strict;
   private final BooleanSetting boat;
   public final SliderSetting boatExpand;
   public final SliderSetting boatSpeed;
   public final SliderSetting boatJump;
   private final BooleanSetting inWater;
   private final BooleanSetting inBlock;
   private final BooleanSetting airStop;
   private final SliderSetting lagTime;
   private final BooleanSetting jump;
   private final SliderSetting strafeSpeed;
   private final BooleanSetting explosions;
   private final BooleanSetting velocity;
   private final SliderSetting multiplier;
   private final SliderSetting vertical;
   private final SliderSetting coolDown;
   private final BooleanSetting slow;
   private final Timer expTimer;
   private final Timer lagTimer;
   private boolean stop;
   private double speed;
   private double getDistance;
   private int strictTicks;
   private int strafe;
   private int stage;
   private double lastExp;
   private boolean boost;

   public Speed() {
      super("Speed", Module.Category.Movement);
      this.mode = this.add(new EnumSetting("Mode", Speed.Mode.Strafe));
      this.collideSpeed = this.add(new SliderSetting("CollideSpeed", 0.08, (double)0.0F, 0.08, 0.01, () -> this.mode.is(Speed.Mode.Grim)));
      this.strict = this.add(new BooleanSetting("Strict", true, () -> this.mode.is(Speed.Mode.Grim)));
      this.boat = this.add(new BooleanSetting("BoatLongJump", true, () -> this.mode.is(Speed.Mode.Grim)));
      this.boatExpand = this.add(new SliderSetting("BoatExpand", 0.2, (double)0.0F, (double)1.0F, 0.01, () -> this.mode.is(Speed.Mode.Grim)));
      this.boatSpeed = this.add(new SliderSetting("BoatSpeed", 0.2, (double)-2.0F, (double)2.0F, 0.01, () -> this.mode.is(Speed.Mode.Grim)));
      this.boatJump = this.add(new SliderSetting("BoatJump", 0.2, (double)0.0F, (double)2.0F, 0.01, () -> this.mode.is(Speed.Mode.Grim)));
      this.inWater = this.add(new BooleanSetting("InWater", false, () -> !this.mode.is(Speed.Mode.Grim)));
      this.inBlock = this.add(new BooleanSetting("InBlock", false, () -> !this.mode.is(Speed.Mode.Grim)));
      this.airStop = this.add(new BooleanSetting("AirStop", false, () -> !this.mode.is(Speed.Mode.Grim)));
      this.lagTime = this.add(new SliderSetting("LagTime", (double)500.0F, (double)0.0F, (double)1000.0F, (double)1.0F, () -> !this.mode.is(Speed.Mode.Grim)));
      this.jump = this.add(new BooleanSetting("Jump", true, () -> this.mode.is(Speed.Mode.Strafe)));
      this.strafeSpeed = this.add(new SliderSetting("Speed", 0.2873, (double)0.0F, (double)1.0F, 1.0E-4, () -> this.mode.is(Speed.Mode.Strafe)));
      this.explosions = this.add(new BooleanSetting("ExplosionsBoost", false, () -> this.mode.is(Speed.Mode.Strafe)));
      this.velocity = this.add(new BooleanSetting("VelocityBoost", true, () -> this.mode.is(Speed.Mode.Strafe)));
      this.multiplier = this.add(new SliderSetting("H-Factor", (double)1.0F, (double)0.0F, (double)5.0F, 0.01, () -> this.mode.is(Speed.Mode.Strafe)));
      this.vertical = this.add(new SliderSetting("V-Factor", (double)1.0F, (double)0.0F, (double)5.0F, 0.01, () -> this.mode.is(Speed.Mode.Strafe)));
      this.coolDown = this.add(new SliderSetting("CoolDown", (double)1000.0F, (double)0.0F, (double)5000.0F, (double)1.0F, () -> this.mode.is(Speed.Mode.Strafe)));
      this.slow = this.add(new BooleanSetting("Slowness", false, () -> this.mode.is(Speed.Mode.Strafe)));
      this.expTimer = new Timer();
      this.lagTimer = new Timer();
      this.strafe = 4;
      this.setChinese("加速");
      INSTANCE = this;
   }

   public String getInfo() {
      return ((Mode)this.mode.getValue()).name();
   }

   public void onEnable() {
      if (mc.player != null) {
         this.speed = MovementUtil.getSpeed(false);
         this.getDistance = MovementUtil.getDistance2D();
      }

      this.stage = 4;
   }

   @EventListener(
      priority = 100
   )
   public void invoke(PacketEvent.Receive event) {
      if (!BaritoneUtil.isActive()) {
         if (this.mode.is(Speed.Mode.Strafe)) {
            Packet<?> packet = event.getPacket();
            if (packet instanceof EntityVelocityUpdateS2CPacket) {
               EntityVelocityUpdateS2CPacket packet2 = (EntityVelocityUpdateS2CPacket)packet;
               if (mc.player != null && packet2.getEntityId() == mc.player.getId() && this.velocity.getValue()) {
                  double speed = Math.sqrt(packet2.getVelocityX() * packet2.getVelocityX() + packet2.getVelocityZ() * packet2.getVelocityZ());
                  double d = this.lastExp = this.expTimer.passed((long)this.coolDown.getValueInt()) ? speed : speed - this.lastExp;
                  if (this.lastExp > (double)0.0F) {
                     this.expTimer.reset();
                     this.speed += this.lastExp * this.multiplier.getValue();
                     this.getDistance += this.lastExp * this.multiplier.getValue();
                     if (MovementUtil.getMotionY() > (double)0.0F && this.vertical.getValue() != (double)0.0F) {
                        MovementUtil.setMotionY(MovementUtil.getMotionY() * this.vertical.getValue());
                     }
                  }
               }
            } else {
               Packet<?> speed = event.getPacket();
               if (speed instanceof ExplosionS2CPacket) {
                  ExplosionS2CPacket packet3 = (ExplosionS2CPacket)speed;
                  if (this.explosions.getValue()) {
                     Vec3d vec3d = new Vec3d(packet3.getX(), packet3.getY(), packet3.getZ());
                     if (mc.player.getPos().distanceTo(vec3d) < (double)15.0F) {
                        double speed2 = Math.sqrt((double)(packet3.getPlayerVelocityX() * packet3.getPlayerVelocityX() + packet3.getPlayerVelocityZ() * packet3.getPlayerVelocityZ()));
                        double d = this.lastExp = this.expTimer.passed((long)this.coolDown.getValueInt()) ? speed2 : speed2 - this.lastExp;
                        if (this.lastExp > (double)0.0F) {
                           this.expTimer.reset();
                           this.speed += this.lastExp * this.multiplier.getValue();
                           this.getDistance += this.lastExp * this.multiplier.getValue();
                           if (MovementUtil.getMotionY() > (double)0.0F) {
                              MovementUtil.setMotionY(MovementUtil.getMotionY() * this.vertical.getValue());
                           }
                        }
                     }
                  }
               }
            }
         }

         if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            this.lagTimer.reset();
            this.resetStrafe();
         }

      }
   }

   @EventListener
   public void onMove(MovedEvent event) {
      if (!nullCheck()) {
         double dx = mc.player.getX() - mc.player.prevX;
         double dz = mc.player.getZ() - mc.player.prevZ;
         this.getDistance = Math.sqrt(dx * dx + dz * dz);
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.mode.is(Speed.Mode.Grim)) {
         if (!MovementUtil.isMoving()) {
            return;
         }

         int collisions = 0;
         Box box = this.strict.getValue() ? mc.player.getBoundingBox() : mc.player.getBoundingBox().expand((double)1.0F);

         for(Entity entity : Alien.THREAD.getEntities()) {
            Box entityBox = entity.getBoundingBox();
            if (this.boat.getValue() && mc.player.isOnGround() && entity instanceof BoatEntity && box.intersects(entityBox.expand(this.boatExpand.getValue()))) {
               double yaw = Math.toRadians((double)Sprint.getSprintYaw(mc.player.getYaw()));
               double boost = this.boatSpeed.getValue();
               mc.player.setVelocity(-Math.sin(yaw) * boost, this.boatJump.getValue(), Math.cos(yaw) * boost);
               return;
            }

            if (box.intersects(entityBox) && this.canCauseSpeed(entity)) {
               ++collisions;
            }
         }

         double yaw = Math.toRadians((double)Sprint.getSprintYaw(mc.player.getYaw()));
         double boost = this.collideSpeed.getValue() * (double)collisions;
         mc.player.addVelocity(-Math.sin(yaw) * boost, (double)0.0F, Math.cos(yaw) * boost);
      }

   }

   private boolean canCauseSpeed(Entity entity) {
      return entity != mc.player && entity instanceof LivingEntity && !(entity instanceof ArmorStandEntity);
   }

   @EventListener
   public void invoke(MoveEvent event) {
      if (!MovementUtil.isMoving() && this.airStop.getValue() && !this.mode.is(Speed.Mode.Grim)) {
         MovementUtil.setMotionX((double)0.0F);
         MovementUtil.setMotionZ((double)0.0F);
      }

      if ((this.inWater.getValue() || !mc.player.isSubmergedInWater() && !mc.player.isTouchingWater() && !mc.player.isInLava()) && !mc.player.isRiding() && !mc.player.isHoldingOntoLadder() && (this.inBlock.getValue() || !EntityUtil.isInsideBlock()) && !mc.player.getAbilities().flying && !mc.player.isFallFlying() && MovementUtil.isMoving()) {
         if (this.mode.is(Speed.Mode.Strafe)) {
            if (this.stop) {
               this.stop = false;
            } else if (this.lagTimer.passed((long)this.lagTime.getValueInt())) {
               if (this.stage == 1) {
                  this.speed = 1.35 * MovementUtil.getSpeed(this.slow.getValue(), this.strafeSpeed.getValue()) - 0.01;
               } else if (this.stage != 2 || !mc.player.isOnGround() || !mc.options.jumpKey.isPressed() && !this.jump.getValue()) {
                  if (this.stage == 3) {
                     this.speed = this.getDistance - 0.66 * (this.getDistance - MovementUtil.getSpeed(this.slow.getValue(), this.strafeSpeed.getValue()));
                     this.boost = !this.boost;
                  } else {
                     if ((BlockUtil.canCollide((Entity)null, mc.player.getBoundingBox().offset((double)0.0F, MovementUtil.getMotionY(), (double)0.0F)) || mc.player.collidedSoftly) && this.stage > 0) {
                        this.stage = 1;
                     }

                     this.speed = this.getDistance - this.getDistance / (double)159.0F;
                  }
               } else {
                  double yMotion = 0.3999 + MovementUtil.getJumpSpeed();
                  MovementUtil.setMotionY(yMotion);
                  event.setY(yMotion);
                  this.speed *= this.boost ? 1.6835 : 1.395;
               }

               this.speed = Math.min(this.speed, (double)10.0F);
               this.speed = Math.max(this.speed, MovementUtil.getSpeed(this.slow.getValue(), this.strafeSpeed.getValue()));
               double n = (double)mc.player.input.movementForward;
               double n2 = (double)mc.player.input.movementSideways;
               double n3 = (double)mc.player.getYaw();
               if (n == (double)0.0F && n2 == (double)0.0F) {
                  event.setX((double)0.0F);
                  event.setZ((double)0.0F);
               } else if (n != (double)0.0F && n2 != (double)0.0F) {
                  n *= Math.sin((Math.PI / 4D));
                  n2 *= Math.cos((Math.PI / 4D));
               }

               event.setX((n * this.speed * -Math.sin(Math.toRadians(n3)) + n2 * this.speed * Math.cos(Math.toRadians(n3))) * 0.99);
               event.setZ((n * this.speed * Math.cos(Math.toRadians(n3)) - n2 * this.speed * -Math.sin(Math.toRadians(n3))) * 0.99);
               ++this.stage;
            }
         } else {
            double speedEffect = (double)1.0F;
            double slowEffect = (double)1.0F;
            if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
               double amplifier = (double)mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier();
               speedEffect = (double)1.0F + 0.2 * (amplifier + (double)1.0F);
            }

            if (mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
               double amplifier = (double)mc.player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier();
               slowEffect = (double)1.0F + 0.2 * (amplifier + (double)1.0F);
            }

            double base = (double)0.2873F * speedEffect / slowEffect;
            float jumpEffect = 0.0F;
            if (mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
               jumpEffect += (float)(mc.player.getStatusEffect(StatusEffects.JUMP_BOOST).getAmplifier() + 1) * 0.1F;
            }

            if (this.mode.getValue() == Speed.Mode.StrafeStrict) {
               if (!this.lagTimer.passed((long)this.lagTime.getValueInt())) {
                  return;
               }

               if (this.strafe == 1) {
                  this.speed = (double)1.35F * base - (double)0.01F;
               } else if (this.strafe == 2) {
                  if (mc.player.input.jumping || !mc.player.isOnGround()) {
                     return;
                  }

                  float jump = 0.39999995F + jumpEffect;
                  event.setY((double)jump);
                  MovementUtil.setMotionY((double)jump);
                  this.speed *= 2.149;
               } else if (this.strafe == 3) {
                  double moveSpeed = 0.66 * (this.getDistance - base);
                  this.speed = this.getDistance - moveSpeed;
               } else {
                  if ((!mc.world.isSpaceEmpty(mc.player, mc.player.getBoundingBox().offset((double)0.0F, mc.player.getVelocity().getY(), (double)0.0F)) || mc.player.verticalCollision) && this.strafe > 0) {
                     this.strafe = 1;
                  }

                  this.speed = this.getDistance - this.getDistance / (double)159.0F;
               }

               ++this.strictTicks;
               this.speed = Math.max(this.speed, base);
               double baseMax = 0.465 * speedEffect / slowEffect;
               double baseMin = 0.44 * speedEffect / slowEffect;
               this.speed = Math.min(this.speed, this.strictTicks > 25 ? baseMax : baseMin);
               if (this.strictTicks > 50) {
                  this.strictTicks = 0;
               }

               Vec2f motion = this.handleStrafeMotion((float)this.speed);
               event.setX((double)motion.x);
               event.setZ((double)motion.y);
               ++this.strafe;
            }

         }
      } else {
         this.resetStrafe();
         this.stop = true;
      }
   }

   public Vec2f handleStrafeMotion(float speed) {
      float forward = mc.player.input.movementForward;
      float strafe = mc.player.input.movementSideways;
      float yaw = mc.player.prevYaw + (mc.player.getYaw() - mc.player.prevYaw) * mc.getRenderTickCounter().getTickDelta(true);
      if (forward == 0.0F && strafe == 0.0F) {
         return Vec2f.ZERO;
      } else {
         if (forward != 0.0F) {
            if (strafe >= 1.0F) {
               yaw += forward > 0.0F ? -45.0F : 45.0F;
               strafe = 0.0F;
            } else if (strafe <= -1.0F) {
               yaw += forward > 0.0F ? 45.0F : -45.0F;
               strafe = 0.0F;
            }

            if (forward > 0.0F) {
               forward = 1.0F;
            } else if (forward < 0.0F) {
               forward = -1.0F;
            }
         }

         float rx = (float)Math.cos(Math.toRadians((double)yaw));
         float rz = (float)(-Math.sin(Math.toRadians((double)yaw)));
         return new Vec2f(forward * speed * rz + strafe * speed * rx, forward * speed * rx - strafe * speed * rz);
      }
   }

   public void resetStrafe() {
      this.strafe = 4;
      this.strictTicks = 0;
      this.speed = (double)0.0F;
      this.getDistance = (double)0.0F;
   }

   public static enum Mode {
      Strafe,
      StrafeStrict,
      Grim;

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{Strafe, StrafeStrict, Grim};
      }
   }
}
