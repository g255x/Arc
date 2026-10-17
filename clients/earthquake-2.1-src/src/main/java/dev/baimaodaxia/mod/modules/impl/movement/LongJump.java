package dev.baimaodaxia.mod.modules.impl.movement;

import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.MoveEvent;
import dev.baimaodaxia.api.events.impl.MovedEvent;
import dev.baimaodaxia.api.events.impl.PacketEvent;
import dev.baimaodaxia.api.utils.math.Timer;
import dev.baimaodaxia.api.utils.player.MovementUtil;
import dev.baimaodaxia.api.utils.world.BlockUtil;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

public class LongJump extends Module {
   public static LongJump INSTANCE;
   private final EnumSetting<Mode> mode;
   private final SliderSetting strafeSpeed;
   private final BooleanSetting jump;
   private final SliderSetting lagTime;
   private final BooleanSetting airStop;
   private final Timer lagTimer;
   private boolean stop;
   private double speed;
   private double distance;
   private int stage;
   private boolean boost;

   public LongJump() {
      super("LongJump", Module.Category.Movement);
      this.mode = this.add(new EnumSetting("Mode", LongJump.Mode.Strafe));
      this.strafeSpeed = this.add(new SliderSetting("Speed", 0.2873, (double)0.0F, (double)1.0F, 1.0E-4, () -> this.mode.is(LongJump.Mode.Strafe)));
      this.jump = this.add(new BooleanSetting("Jump", true, () -> this.mode.is(LongJump.Mode.Strafe)));
      this.lagTime = this.add(new SliderSetting("LagTime", (double)500.0F, (double)0.0F, (double)1000.0F, (double)1.0F, () -> this.mode.is(LongJump.Mode.Strafe)));
      this.airStop = this.add(new BooleanSetting("AirStop", false));
      this.lagTimer = new Timer();
      this.setChinese("长跳");
      INSTANCE = this;
   }

   public String getInfo() {
      return ((Mode)this.mode.getValue()).name();
   }

   public void onEnable() {
      if (mc.player != null) {
         this.speed = MovementUtil.getSpeed(false);
         this.distance = MovementUtil.getDistance2D();
      }

      this.stage = 4;
   }

   @EventListener(
      priority = 100
   )
   public void invoke(PacketEvent.Receive event) {
      if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
         this.lagTimer.reset();
         this.resetStrafe();
      }

   }

   @EventListener
   public void onMove(MovedEvent event) {
      if (!nullCheck()) {
         double dx = mc.player.getX() - mc.player.prevX;
         double dz = mc.player.getZ() - mc.player.prevZ;
         this.distance = Math.sqrt(dx * dx + dz * dz);
      }

   }

   @EventListener
   public void invoke(MoveEvent event) {
      if (!MovementUtil.isMoving() && this.airStop.getValue()) {
         MovementUtil.setMotionX((double)0.0F);
         MovementUtil.setMotionZ((double)0.0F);
      }

      if (!mc.player.isRiding() && !mc.player.isHoldingOntoLadder() && !mc.player.getAbilities().flying && !mc.player.isFallFlying() && MovementUtil.isMoving()) {
         if (this.mode.is(LongJump.Mode.Strafe)) {
            if (this.stop) {
               this.stop = false;
            } else if (this.lagTimer.passed((long)this.lagTime.getValueInt())) {
               if (this.stage == 1) {
                  this.speed = 1.35 * MovementUtil.getSpeed(false, this.strafeSpeed.getValue()) - 0.01;
               } else if (this.stage != 2 || !mc.player.isOnGround() || !mc.options.jumpKey.isPressed() && !this.jump.getValue()) {
                  if (this.stage == 3) {
                     this.speed = this.distance - 0.66 * (this.distance - MovementUtil.getSpeed(false, this.strafeSpeed.getValue()));
                     this.boost = !this.boost;
                  } else {
                     if ((BlockUtil.canCollide((Entity)null, mc.player.getBoundingBox().offset((double)0.0F, MovementUtil.getMotionY(), (double)0.0F)) || mc.player.collidedSoftly) && this.stage > 0) {
                        this.stage = 1;
                     }

                     this.speed = this.distance - this.distance / (double)159.0F;
                  }
               } else {
                  double yMotion = 0.3999 + MovementUtil.getJumpSpeed();
                  MovementUtil.setMotionY(yMotion);
                  event.setY(yMotion);
                  this.speed *= this.boost ? 1.6835 : 1.395;
               }

               this.speed = Math.min(this.speed, (double)10.0F);
               this.speed = Math.max(this.speed, MovementUtil.getSpeed(false, this.strafeSpeed.getValue()));
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
         }
      } else {
         this.resetStrafe();
         this.stop = true;
      }

   }

   public void resetStrafe() {
      this.stage = 4;
      this.speed = (double)0.0F;
      this.distance = (double)0.0F;
   }

   public static enum Mode {
      Strafe;

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{Strafe};
      }
   }
}
