package dev.g255.arc.mod.modules.impl.movement;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.MoveEvent;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.api.utils.path.BaritoneUtil;
import dev.g255.arc.api.utils.player.EntityUtil;
import dev.g255.arc.api.utils.player.MovementUtil;
import dev.g255.arc.api.utils.world.BlockUtil;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;

public class LongJump extends Module {
   public static LongJump INSTANCE;
   private final EnumSetting<Mode> mode;
   private final SliderSetting jumpSpeed;
   private final BooleanSetting inLiquid;
   private final BooleanSetting useTimer;
   private final BooleanSetting autoDisable;
   private final Timer jumpTimer;
   private double speed;
   private double distance;
   private int stage;
   private boolean flip;
   private boolean jumped;

   public LongJump() {
      super("LongJump", "Makes you jump fast 'n far.", Module.Category.Movement);
      this.mode = this.add(new EnumSetting("Mode", LongJump.Mode.Plain));
      this.jumpSpeed = this.add(new SliderSetting("JumpSpeed", (double)1.0F, 0.1, (double)3.0F, 0.05));
      this.inLiquid = this.add(new BooleanSetting("InLiquid", false));
      this.useTimer = this.add(new BooleanSetting("UseTimer", true));
      this.autoDisable = this.add(new BooleanSetting("AutoDisable", false));
      this.jumpTimer = new Timer();
      this.setChinese("远跳");
      INSTANCE = this;
   }

   public void onEnable() {
      this.stage = 4;
      this.flip = false;
      this.jumped = false;
      this.jumpTimer.reset();
   }

   public void onDisable() {
      Arc.TIMER.reset();
      mc.options.jumpKey.setPressed(false);
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!nullCheck()) {
         double dx = mc.player.getX() - mc.player.prevX;
         double dz = mc.player.getZ() - mc.player.prevZ;
         this.distance = Math.sqrt(dx * dx + dz * dz);
      }
   }

   @EventListener
   public void onMove(MoveEvent event) {
      if (!nullCheck() && !LongJump.BaritoneCheck.isActive()) {
         if (!mc.player.isFallFlying() && !mc.player.isSpectator() && !EntityUtil.isInsideBlock() && (this.inLiquid.getValue() || !mc.player.isTouchingWater() && !mc.player.isInLava())) {
            double yMotion = event.getY();
            double base = MovementUtil.getSpeed(false, 0.4 * (double)2.0F * this.jumpSpeed.getValue());
            if (this.useTimer.getValue()) {
               if (this.jumped && !this.jumpTimer.passed(250L)) {
                  Arc.TIMER.set(1.0886F);
               } else {
                  Arc.TIMER.tryReset();
               }
            }

            boolean grimHop = this.mode.is(LongJump.Mode.GrimHop);
            if (!MovementUtil.isMoving()) {
               this.stage = 4;
            } else if (this.stage == 1) {
               this.speed = grimHop ? 1.3 * base - 0.01 : 1.4 * base - 0.01;
            } else if (this.stage == 2 && mc.player.isOnGround() && mc.player.input.jumping) {
               yMotion = 0.39 + MovementUtil.getJumpSpeed();
               MovementUtil.setMotionY(yMotion);
               event.setY(yMotion);
               this.speed *= this.flip ? 1.6835 : 1.395;
               this.jumped = true;
               this.jumpTimer.reset();
            } else if (this.stage == 3) {
               this.speed = this.distance - 0.66 * (this.distance - base);
               this.flip = !this.flip;
            } else {
               if ((BlockUtil.canCollide(mc.player.getBoundingBox().offset((double)0.0F, MovementUtil.getMotionY(), (double)0.0F)) || mc.player.collidedSoftly) && this.stage > 0) {
                  this.stage = 1;
               }

               this.speed = base * (double)2.0F;
            }

            if (this.autoDisable.getValue() && this.jumped && mc.player.isOnGround() && this.stage > 3) {
               this.disable();
            } else {
               this.speed = Math.min(this.speed, (double)10.0F);
               this.speed = Math.max(this.speed, base);
               double forward = (double)mc.player.input.movementForward;
               double side = (double)mc.player.input.movementSideways;
               double yaw = (double)mc.player.getYaw();
               if (forward == (double)0.0F && side == (double)0.0F) {
                  event.setX((double)0.0F);
                  event.setZ((double)0.0F);
               } else {
                  if (forward != (double)0.0F && side != (double)0.0F) {
                     forward *= Math.sin((Math.PI / 4D));
                     side *= Math.cos((Math.PI / 4D));
                  }

                  event.setX((forward * this.speed * -Math.sin(Math.toRadians(yaw)) + side * this.speed * Math.cos(Math.toRadians(yaw))) * 0.99);
                  event.setZ((forward * this.speed * Math.cos(Math.toRadians(yaw)) - side * this.speed * -Math.sin(Math.toRadians(yaw))) * 0.99);
                  ++this.stage;
               }

            }
         }
      }
   }

   public static enum Mode {
      Plain,
      GrimHop;

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{Plain, GrimHop};
      }
   }

   private static final class BaritoneCheck {
      static boolean isActive() {
         try {
            return BaritoneUtil.isActive();
         } catch (Throwable var1) {
            return false;
         }
      }
   }
}
