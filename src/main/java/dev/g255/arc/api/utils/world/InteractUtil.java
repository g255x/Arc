package dev.g255.arc.api.utils.world;

import dev.g255.arc.api.utils.Wrapper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.math.Box;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class InteractUtil implements Wrapper {
   public static HitResult getRtxTarget(float yaw, float pitch, double x, double y, double z) {
      HitResult result = rayTrace((double)5.0F, yaw, pitch, x, y, z);
      Vec3d vec3d = new Vec3d(x, y, z);
      double distancePow2 = (double)25.0F;
      if (result != null) {
         distancePow2 = result.getPos().squaredDistanceTo(vec3d);
      }

      Vec3d vec3d2 = getRotationVector(pitch, yaw);
      Vec3d vec3d3 = vec3d.add(vec3d2.x * (double)5.0F, vec3d2.y * (double)5.0F, vec3d2.z * (double)5.0F);
      EntityHitResult entityHitResult = ProjectileUtil.raycast(mc.player, vec3d, vec3d3, (new Box(x - 0.3, y, z - 0.3, x + 0.3, y + 1.8, z + 0.3)).stretch(vec3d2.multiply((double)5.0F)).expand((double)1.0F, (double)1.0F, (double)1.0F), (entity) -> !entity.isSpectator() && entity.canHit(), distancePow2);
      if (entityHitResult != null) {
         Entity entity2 = entityHitResult.getEntity();
         Vec3d vec3d4 = entityHitResult.getPos();
         double g = vec3d.squaredDistanceTo(vec3d4);
         if ((g < distancePow2 || result == null) && entity2 instanceof LivingEntity) {
            return entityHitResult;
         }
      }

      return result;
   }

   public static HitResult rayTrace(double dst, float yaw, float pitch, double x, double y, double z) {
      Vec3d vec3d = new Vec3d(x, y, z);
      Vec3d vec3d2 = getRotationVector(pitch, yaw);
      Vec3d vec3d3 = vec3d.add(vec3d2.x * dst, vec3d2.y * dst, vec3d2.z * dst);
      return mc.world.raycast(new RaycastContext(vec3d, vec3d3, ShapeType.OUTLINE, FluidHandling.NONE, mc.player));
   }

   private static Vec3d getRotationVector(float yaw, float pitch) {
      return new Vec3d((double)(MathHelper.sin(-pitch * ((float)Math.PI / 180F)) * MathHelper.cos(yaw * ((float)Math.PI / 180F))), (double)(-MathHelper.sin(yaw * ((float)Math.PI / 180F))), (double)(MathHelper.cos(-pitch * ((float)Math.PI / 180F)) * MathHelper.cos(yaw * ((float)Math.PI / 180F))));
   }
}
