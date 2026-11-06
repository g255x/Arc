package dev.g255.arc.api.utils.player;

import dev.g255.arc.api.utils.Wrapper;
import dev.g255.arc.asm.accessors.IVec3d;
import dev.g255.arc.mod.modules.impl.movement.HoleSnap;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class MovementUtil implements Wrapper {
   public static boolean isMoving() {
      return (double)mc.player.input.movementForward != (double)0.0F || (double)mc.player.input.movementSideways != (double)0.0F || HoleSnap.INSTANCE.isOn();
   }

   public static boolean isStatic() {
      return mc.player.getVelocity().getX() == (double)0.0F && mc.player.isOnGround() && mc.player.getVelocity().getZ() == (double)0.0F;
   }

   public static boolean isJumping() {
      return mc.player.input.jumping;
   }

   public static double getDistance2D() {
      double xDist = mc.player.getX() - mc.player.prevX;
      double zDist = mc.player.getZ() - mc.player.prevZ;
      return Math.sqrt(xDist * xDist + zDist * zDist);
   }

   public static double getJumpSpeed() {
      double defaultSpeed = (double)0.0F;
      if (mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
         int amplifier = ((StatusEffectInstance)mc.player.getActiveStatusEffects().get(StatusEffects.JUMP_BOOST)).getAmplifier();
         defaultSpeed += (double)(amplifier + 1) * 0.1;
      }

      return defaultSpeed;
   }

   public static double[] directionSpeed(double speed) {
      float forward = mc.player.input.movementForward;
      float side = mc.player.input.movementSideways;
      return directionSpeed(speed, forward, side);
   }

   private static double[] directionSpeed(double speed, float forward, float side) {
      float yaw = mc.player.prevYaw + (mc.player.getYaw() - mc.player.prevYaw) * mc.getRenderTickCounter().getTickDelta(true);
      if (forward != 0.0F) {
         if (side > 0.0F) {
            yaw += (float)(forward > 0.0F ? -45 : 45);
         } else if (side < 0.0F) {
            yaw += (float)(forward > 0.0F ? 45 : -45);
         }

         side = 0.0F;
         if (forward > 0.0F) {
            forward = 1.0F;
         } else if (forward < 0.0F) {
            forward = -1.0F;
         }
      }

      double sin = Math.sin(Math.toRadians((double)(yaw + 90.0F)));
      double cos = Math.cos(Math.toRadians((double)(yaw + 90.0F)));
      double posX = (double)forward * speed * cos + (double)side * speed * sin;
      double posZ = (double)forward * speed * sin - (double)side * speed * cos;
      return new double[]{posX, posZ};
   }

   public static double getMotionX() {
      return mc.player.getVelocity().x;
   }

   public static void setMotionX(double x) {
      ((IVec3d)mc.player.getVelocity()).setX(x);
   }

   public static double getMotionY() {
      return mc.player.getVelocity().y;
   }

   public static void setMotionY(double y) {
      ((IVec3d)mc.player.getVelocity()).setY(y);
   }

   public static double getMotionZ() {
      return mc.player.getVelocity().z;
   }

   public static void setMotionZ(double z) {
      ((IVec3d)mc.player.getVelocity()).setZ(z);
   }

   public static double getSpeed(boolean slowness) {
      double defaultSpeed = 0.2873;
      return getSpeed(slowness, defaultSpeed);
   }

   public static double getSpeed(boolean slowness, double defaultSpeed) {
      if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
         int amplifier = ((StatusEffectInstance)mc.player.getActiveStatusEffects().get(StatusEffects.SPEED)).getAmplifier();
         defaultSpeed *= (double)1.0F + 0.2 * (double)(amplifier + 1);
      }

      if (slowness && mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
         int amplifier = ((StatusEffectInstance)mc.player.getActiveStatusEffects().get(StatusEffects.SLOWNESS)).getAmplifier();
         defaultSpeed /= (double)1.0F + 0.2 * (double)(amplifier + 1);
      }

      if (mc.player.isSneaking()) {
         defaultSpeed /= (double)5.0F;
      }

      return defaultSpeed;
   }
}
