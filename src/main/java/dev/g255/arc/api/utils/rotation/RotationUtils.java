package dev.g255.arc.api.utils.rotation;

import dev.g255.arc.Arc;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.hit.HitResult.Type;

public class RotationUtils {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public static Rotation calculate(Vec3d from, Vec3d to) {
      Vec3d diff = to.subtract(from);
      double distance = Math.hypot(diff.x, diff.z);
      float yaw = (float)(Math.atan2(diff.z, diff.x) * (double)(180F / (float)Math.PI)) - 90.0F;
      float pitch = (float)(-(Math.atan2(diff.y, distance) * (double)(180F / (float)Math.PI)));
      return new Rotation(yaw, pitch);
   }

   public static Rotation calculate(Entity entity) {
      if (mc.player == null) {
         return new Rotation(0.0F, 0.0F);
      } else {
         double eyeHeight = (double)mc.player.getEyeHeight(mc.player.getPose());
         double clamped = Math.max((double)0.0F, Math.min(mc.player.getY() - entity.getY() + eyeHeight, (entity.getBoundingBox().maxY - entity.getBoundingBox().minY) * 0.9));
         return calculate(entity.getPos().add((double)0.0F, clamped, (double)0.0F));
      }
   }

   public static Rotation calculate(Entity entity, boolean adaptive, double range) {
      Rotation normalRotations = calculate(entity);
      HitResult result = RayCastUtil.rayCast(normalRotations, range, 0.0F);
      if (adaptive && (result == null || result.getType() != Type.ENTITY)) {
         Box bb = entity.getBoundingBox();
         double minX = bb.minX;
         double maxX = bb.maxX;
         double minY = bb.minY;
         double maxY = bb.maxY;
         double minZ = bb.minZ;
         double maxZ = bb.maxZ;
         Vec3d basePos = entity.getPos();

         for(double yPercent = (double)1.0F; yPercent >= (double)0.0F; yPercent -= (double)0.25F + Math.random() * 0.1) {
            for(double xPercent = (double)1.0F; xPercent >= (double)-0.5F; xPercent -= (double)0.5F) {
               for(double zPercent = (double)1.0F; zPercent >= (double)-0.5F; zPercent -= (double)0.5F) {
                  double offsetX = (maxX - minX) * xPercent;
                  double offsetY = (maxY - minY) * yPercent;
                  double offsetZ = (maxZ - minZ) * zPercent;
                  Vec3d targetPoint = basePos.add(offsetX, offsetY, offsetZ);
                  Rotation adaptiveRotations = calculate(targetPoint);
                  HitResult rayCastResult = RayCastUtil.rayCast(adaptiveRotations, range, 0.0F);
                  if (rayCastResult != null && rayCastResult.getType() == Type.ENTITY) {
                     return adaptiveRotations;
                  }
               }
            }
         }

         return normalRotations;
      } else {
         return normalRotations;
      }
   }

   public static Rotation calculate(BlockPos to) {
      return calculate(mc.player.getEyePos(), (new Vec3d((double)to.getX(), (double)to.getY(), (double)to.getZ())).add((double)0.5F, (double)0.5F, (double)0.5F));
   }

   public static Rotation calculate(Vec3d to) {
      return calculate(mc.player.getEyePos(), to);
   }

   public static Rotation calculate(Vec3d position, Direction enumFacing) {
      double x = position.x + (double)0.5F;
      double y = position.y + (double)0.5F;
      double z = position.z + (double)0.5F;
      return calculate(new Vec3d(x + (double)enumFacing.getVector().getX() * (double)0.5F, y + (double)enumFacing.getVector().getY() * (double)0.5F, z + (double)enumFacing.getVector().getZ() * (double)0.5F));
   }

   public static Rotation calculate(BlockPos position, Direction enumFacing) {
      double x = (double)position.getX() + (double)0.5F;
      double y = (double)position.getY() + (double)0.5F;
      double z = (double)position.getZ() + (double)0.5F;
      return calculate(new Vec3d(x + (double)enumFacing.getVector().getX() * (double)0.5F, y + (double)enumFacing.getVector().getY() * (double)0.5F, z + (double)enumFacing.getVector().getZ() * (double)0.5F));
   }

   public static Rotation applySensitivityPatch(Rotation rotation) {
      Rotation previousRotation = RotationManager.lastRotations;
      if (previousRotation == null) {
         previousRotation = new Rotation(mc.player.prevYaw, mc.player.prevPitch);
      }

      return applySensitivityPatch(rotation, previousRotation);
   }

   public static Rotation applySensitivityPatch(Rotation rotation, Rotation previousRotation) {
      // 任一输入是 NaN 就直接返回原值：往 mc.player.setYaw 传 NaN 会让原版每帧刷
      // "Invalid entity rotation: NaN, discarding."，而且 NaN 会沿着 lastRotations 一直传播下去。
      if (rotation == null || previousRotation == null
            || Float.isNaN(rotation.getYaw()) || Float.isNaN(rotation.getPitch())
            || Float.isNaN(previousRotation.getYaw()) || Float.isNaN(previousRotation.getPitch())) {
         return rotation;
      }

      float mouseSensitivity = (float)((Double)mc.options.getMouseSensitivity().getValue() * (double)0.6F + (double)0.2F);
      double multiplier = (double)(mouseSensitivity * mouseSensitivity * mouseSensitivity * 8.0F) * 0.15;
      if (multiplier <= (double)0.0F || Double.isNaN(multiplier)) {
         return rotation;
      }

      float yaw = previousRotation.getYaw() + (float)((double)Math.round((double)(rotation.getYaw() - previousRotation.getYaw()) / multiplier) * multiplier);
      float pitch = previousRotation.getPitch() + (float)((double)Math.round((double)(rotation.getPitch() - previousRotation.getPitch()) / multiplier) * multiplier);
      if (Float.isNaN(yaw) || Float.isNaN(pitch)) {
         return rotation;
      }

      return new Rotation(yaw, MathHelper.clamp(pitch, -90.0F, 90.0F));
   }

   public static Rotation relateToPlayerRotation(Rotation rotation) {
      Rotation previousRotation = new Rotation(mc.player.prevYaw, mc.player.prevPitch);
      float yaw = previousRotation.getYaw() + MathHelper.wrapDegrees(rotation.getYaw() - previousRotation.getYaw());
      float pitch = MathHelper.clamp(rotation.getPitch(), -90.0F, 90.0F);
      return new Rotation(yaw, pitch);
   }

   public static Rotation resetRotation(Rotation rotation) {
      if (rotation == null) {
         return null;
      } else {
         float yaw = rotation.getYaw() + MathHelper.wrapDegrees(mc.player.getYaw() - rotation.getYaw());
         float pitch = mc.player.getPitch();
         return new Rotation(yaw, pitch);
      }
   }

   public static Rotation move(Rotation targetRotation, double speed) {
      return move(RotationManager.lastRotations, targetRotation, speed);
   }

   public static Rotation move(Rotation lastRotation, Rotation targetRotation, double speed) {
      if (speed != (double)0.0F) {
         double deltaYaw = (double)MathHelper.wrapDegrees(targetRotation.getYaw() - lastRotation.getYaw());
         double deltaPitch = (double)(targetRotation.getPitch() - lastRotation.getPitch());
         double distance = Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);
         // 已经对准时 distance 为 0，deltaYaw / distance 会得到 NaN
         if (distance <= 1.0E-6) {
            return new Rotation(0.0F, 0.0F);
         }

         double distributionYaw = Math.abs(deltaYaw / distance);
         double distributionPitch = Math.abs(deltaPitch / distance);
         double maxYaw = speed * distributionYaw;
         double maxPitch = speed * distributionPitch;
         float moveYaw = (float)Math.max(Math.min(deltaYaw, maxYaw), -maxYaw);
         float movePitch = (float)Math.max(Math.min(deltaPitch, maxPitch), -maxPitch);
         return new Rotation(moveYaw, movePitch);
      } else {
         return new Rotation(0.0F, 0.0F);
      }
   }

   public static Rotation smooth(Rotation targetRotation, double speed) {
      return smooth(RotationManager.lastRotations, targetRotation, speed);
   }

   public static Rotation smooth(Rotation lastRotation, Rotation targetRotation, double speed) {
      float yaw = targetRotation.getYaw();
      float pitch = targetRotation.getPitch();
      float lastYaw = lastRotation.getYaw();
      float lastPitch = lastRotation.getPitch();
      if (speed != (double)0.0F) {
         Rotation move = move(targetRotation, speed);
         yaw = lastYaw + move.getYaw();
         pitch = lastPitch + move.getPitch();

         for(int i = 1; i <= (int)((float)Arc.FPS.getFps() / 20.0F + (float)Math.random() * 10.0F); ++i) {
            if ((double)(Math.abs(move.getYaw()) + Math.abs(move.getPitch())) > 1.0E-4) {
               yaw = (float)((double)yaw + (Math.random() - (double)0.5F) / (double)1000.0F);
               pitch = (float)((double)pitch - Math.random() / (double)200.0F);
            }

            Rotation rotations = new Rotation(yaw, pitch);
            Rotation fixedRotations = applySensitivityPatch(rotations, lastRotation);
            yaw = shortestYaw(lastYaw, fixedRotations.getYaw());
            pitch = Math.max(-90.0F, Math.min(90.0F, fixedRotations.getPitch()));
         }
      }

      return new Rotation(yaw, pitch);
   }

   public static double getDistance(Entity entity) {
      Vec3d eyes = mc.player.getEyePos();
      Box aabb = entity.getBoundingBox();
      double x = MathHelper.clamp(eyes.x, aabb.minX, aabb.maxX);
      double y = MathHelper.clamp(eyes.y, aabb.minY, aabb.maxY);
      double z = MathHelper.clamp(eyes.z, aabb.minZ, aabb.maxZ);
      return Math.sqrt(eyes.squaredDistanceTo(x, y, z));
   }

   public static double getDistance(PlayerEntity player, Entity entity) {
      Vec3d eyes = player.getEyePos();
      Box aabb = entity.getBoundingBox();
      double x = MathHelper.clamp(eyes.x, aabb.minX, aabb.maxX);
      double y = MathHelper.clamp(eyes.y, aabb.minY, aabb.maxY);
      double z = MathHelper.clamp(eyes.z, aabb.minZ, aabb.maxZ);
      return Math.sqrt(eyes.squaredDistanceTo(x, y, z));
   }

   public static float shortestYaw(float from, float to) {
      return from + MathHelper.wrapDegrees(to - from);
   }
}
