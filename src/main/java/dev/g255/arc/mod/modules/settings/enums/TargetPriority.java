package dev.g255.arc.mod.modules.settings.enums;

import dev.g255.arc.api.utils.Wrapper;
import java.util.Comparator;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

/**
 * 目标排序规则，对齐 Hachimi 的 TargetPriority（AutoMine / AutoTrap 共用）。
 */
public enum TargetPriority implements Wrapper {
   LowestDistance,
   HighestDistance,
   LowestHealth,
   HighestHealth,
   ClosestAngle;

   public Comparator<PlayerEntity> comparator() {
      return (a, b) -> this.compare(a, b);
   }

   public int compare(PlayerEntity a, PlayerEntity b) {
      switch (this) {
         case LowestDistance:
            return Double.compare(this.distance(a), this.distance(b));
         case HighestDistance:
            return Double.compare(this.distance(b), this.distance(a));
         case LowestHealth:
            return Float.compare(a.getHealth(), b.getHealth());
         case HighestHealth:
            return Float.compare(b.getHealth(), a.getHealth());
         case ClosestAngle:
         default:
            double angleA = MathHelper.angleBetween(yawTo(a), mc.player.getYaw());
            double angleB = MathHelper.angleBetween(yawTo(b), mc.player.getYaw());
            double pitchA = Math.abs(pitchTo(a) - mc.player.getPitch());
            double pitchB = Math.abs(pitchTo(b) - mc.player.getPitch());
            return Double.compare(angleA * angleA + pitchA * pitchA, angleB * angleB + pitchB * pitchB);
      }
   }

   private double distance(PlayerEntity player) {
      return mc.player.squaredDistanceTo(player.getPos());
   }

   private static float yawTo(PlayerEntity entity) {
      double dx = entity.getX() - mc.player.getX();
      double dz = entity.getZ() - mc.player.getZ();
      return (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
   }

   private static float pitchTo(PlayerEntity entity) {
      double dx = entity.getX() - mc.player.getX();
      double dy = entity.getEyeY() - mc.player.getEyeY();
      double dz = entity.getZ() - mc.player.getZ();
      return (float)(-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
   }
}
