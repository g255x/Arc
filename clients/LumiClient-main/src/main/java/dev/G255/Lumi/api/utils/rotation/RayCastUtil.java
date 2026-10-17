package dev.G255.Lumi.api.utils.rotation;

import java.util.Optional;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.MinecraftClient;
import dev.G255.Lumi.core.impl.RotationManager;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public final class RayCastUtil {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public static HitResult rayCast(Rotation rotation, double range) {
      return rayCast(rotation, range, 0.0F);
   }

   public static boolean inView(Entity entity) {
      if (mc.player != null && mc.world != null && entity != null) {
         double dist = (double)mc.player.distanceTo(entity);
         double range = Math.min((double)256.0F, dist + (double)2.0F);
         Rotation rotations = RotationUtils.calculate(entity);
         if (!(dist > (double)100.0F) && entity instanceof PlayerEntity) {
            Box bb = entity.getBoundingBox();
            double width = bb.getLengthX();
            double height = bb.getLengthY();
            double depth = bb.getLengthZ();
            Vec3d basePos = entity.getPos();

            for(double yPercent = (double)1.0F; yPercent >= (double)-1.0F; yPercent -= (double)0.5F) {
               for(double xPercent = (double)1.0F; xPercent >= (double)-1.0F; --xPercent) {
                  for(double zPercent = (double)1.0F; zPercent >= (double)-1.0F; --zPercent) {
                     Vec3d targetPoint = basePos.add(width * xPercent, height * yPercent, depth * zPercent);
                     Rotation subRotations = RotationUtils.calculate(targetPoint);
                     HitResult result = rayCast(subRotations, range, 0.2F);
                     if (result != null && result.getType() == Type.ENTITY) {
                        return true;
                     }
                  }
               }
            }

            return false;
         } else {
            HitResult result = rayCast(rotations, range, 0.2F);
            return result != null && result.getType() == Type.ENTITY;
         }
      } else {
         return false;
      }
   }

   public static HitResult rayCast(Rotation rotation, double range, float expand) {
      return rayCast(rotation, range, expand, mc.player);
   }

   public static HitResult rayCast(Rotation rotation, double range, float expand, Entity entity) {
      if (mc.world != null && entity != null) {
         Vec3d eyePos = entity.getEyePos();
         Vec3d lookVec = Vec3d.fromPolar(rotation.getPitch(), rotation.getYaw());
         Vec3d endVec = eyePos.add(lookVec.multiply(range));
         HitResult objectMouseOver = mc.world.raycast(new RaycastContext(eyePos, endVec, ShapeType.OUTLINE, FluidHandling.NONE, entity));
         double distToBlock = range;
         if (objectMouseOver != null && objectMouseOver.getType() != Type.MISS) {
            distToBlock = objectMouseOver.getPos().distanceTo(eyePos);
         }

         Vec3d entitySearchEndVec = endVec;
         Entity pointedEntity = null;
         Vec3d hitVec = null;
         double currentDist = distToBlock;
         Box searchBox = entity.getBoundingBox().stretch(lookVec.multiply(range)).expand((double)1.0F);

         for(Entity candidate : mc.world.getOtherEntities(entity, searchBox, (e) -> !e.isSpectator() && e.canHit())) {
            double collisionSize = (double)(candidate.getTargetingMargin() + expand);
            Box entityBox = candidate.getBoundingBox().expand(collisionSize);
            Optional<Vec3d> intercept = entityBox.raycast(eyePos, entitySearchEndVec);
            if (entityBox.contains(eyePos)) {
               if (currentDist >= (double)0.0F) {
                  pointedEntity = candidate;
                  hitVec = (Vec3d)intercept.orElse(eyePos);
                  currentDist = (double)0.0F;
               }
            } else if (intercept.isPresent()) {
               Vec3d interceptVec = (Vec3d)intercept.get();
               double d3 = eyePos.distanceTo(interceptVec);
               if ((d3 < currentDist || currentDist == (double)0.0F) && (candidate.getRootVehicle() != entity.getRootVehicle() || currentDist == (double)0.0F)) {
                  pointedEntity = candidate;
                  hitVec = interceptVec;
                  currentDist = d3;
               }
            }
         }

         if (pointedEntity == null || !(currentDist < distToBlock) && objectMouseOver != null && objectMouseOver.getType() != Type.MISS) {
            return objectMouseOver;
         } else {
            return new EntityHitResult(pointedEntity, hitVec);
         }
      } else {
         return null;
      }
   }

   public static boolean overBlock(Rotation rotation, Direction enumFacing, BlockPos pos, boolean strict) {
      if (mc.player != null && mc.world != null) {
         Vec3d lookVec = Vec3d.fromPolar(rotation.getPitch(), rotation.getYaw());
         Vec3d eyePos = mc.player.getEyePos();
         double reach = (double)4.5F;
         Vec3d endVec = eyePos.add(lookVec.multiply(reach));
         BlockHitResult result = mc.world.raycast(new RaycastContext(eyePos, endVec, ShapeType.OUTLINE, FluidHandling.NONE, mc.player));
         if (result.getType() == Type.MISS) {
            return false;
         } else {
            return result.getBlockPos().equals(pos) && (!strict || result.getSide() == enumFacing);
         }
      } else {
         return false;
      }
   }

   public static boolean overBlock(Direction enumFacing, BlockPos pos, boolean strict) {
      return overBlock(new Rotation(RotationManager.fixYaw, RotationManager.fixPitch), enumFacing, pos, strict);
   }

   public static Boolean overBlock(Rotation rotation, BlockPos pos) {
      return overBlock(rotation, Direction.UP, pos, false);
   }

   public static Boolean overBlock(Rotation rotation, BlockPos pos, Direction enumFacing) {
      return overBlock(rotation, enumFacing, pos, true);
   }
}
