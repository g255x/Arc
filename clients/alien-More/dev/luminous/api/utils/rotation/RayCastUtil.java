package dev.luminous.api.utils.rotation;

import java.util.Optional;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_238;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_3959;
import net.minecraft.class_3965;
import net.minecraft.class_3966;
import net.minecraft.class_239.class_240;
import net.minecraft.class_3959.class_242;
import net.minecraft.class_3959.class_3960;

public final class RayCastUtil {
   private static final class_310 mc = class_310.method_1551();

   public static class_239 rayCast(Rotation rotation, double range) {
      return rayCast(rotation, range, 0.0F);
   }

   public static boolean inView(class_1297 entity) {
      if (mc.field_1724 != null && mc.field_1687 != null && entity != null) {
         double dist = (double)mc.field_1724.method_5739(entity);
         double range = Math.min((double)256.0F, dist + (double)2.0F);
         Rotation rotations = RotationUtils.calculate(entity);
         if (!(dist > (double)100.0F) && entity instanceof class_1657) {
            class_238 bb = entity.method_5829();
            double width = bb.method_17939();
            double height = bb.method_17940();
            double depth = bb.method_17941();
            class_243 basePos = entity.method_19538();

            for(double yPercent = (double)1.0F; yPercent >= (double)-1.0F; yPercent -= (double)0.5F) {
               for(double xPercent = (double)1.0F; xPercent >= (double)-1.0F; --xPercent) {
                  for(double zPercent = (double)1.0F; zPercent >= (double)-1.0F; --zPercent) {
                     class_243 targetPoint = basePos.method_1031(width * xPercent, height * yPercent, depth * zPercent);
                     Rotation subRotations = RotationUtils.calculate(targetPoint);
                     class_239 result = rayCast(subRotations, range, 0.2F);
                     if (result != null && result.method_17783() == class_240.field_1331) {
                        return true;
                     }
                  }
               }
            }

            return false;
         } else {
            class_239 result = rayCast(rotations, range, 0.2F);
            return result != null && result.method_17783() == class_240.field_1331;
         }
      } else {
         return false;
      }
   }

   public static class_239 rayCast(Rotation rotation, double range, float expand) {
      return rayCast(rotation, range, expand, mc.field_1724);
   }

   public static class_239 rayCast(Rotation rotation, double range, float expand, class_1297 entity) {
      if (mc.field_1687 != null && entity != null) {
         class_243 eyePos = entity.method_33571();
         class_243 lookVec = class_243.method_1030(rotation.getPitch(), rotation.getYaw());
         class_243 endVec = eyePos.method_1019(lookVec.method_1021(range));
         class_239 objectMouseOver = mc.field_1687.method_17742(new class_3959(eyePos, endVec, class_3960.field_17559, class_242.field_1348, entity));
         double distToBlock = range;
         if (objectMouseOver != null && objectMouseOver.method_17783() != class_240.field_1333) {
            distToBlock = objectMouseOver.method_17784().method_1022(eyePos);
         }

         class_243 entitySearchEndVec = endVec;
         class_1297 pointedEntity = null;
         class_243 hitVec = null;
         double currentDist = distToBlock;
         class_238 searchBox = entity.method_5829().method_18804(lookVec.method_1021(range)).method_1014((double)1.0F);

         for(class_1297 candidate : mc.field_1687.method_8333(entity, searchBox, (e) -> !e.method_7325() && e.method_5863())) {
            double collisionSize = (double)(candidate.method_5871() + expand);
            class_238 entityBox = candidate.method_5829().method_1014(collisionSize);
            Optional<class_243> intercept = entityBox.method_992(eyePos, entitySearchEndVec);
            if (entityBox.method_1006(eyePos)) {
               if (currentDist >= (double)0.0F) {
                  pointedEntity = candidate;
                  hitVec = (class_243)intercept.orElse(eyePos);
                  currentDist = (double)0.0F;
               }
            } else if (intercept.isPresent()) {
               class_243 interceptVec = (class_243)intercept.get();
               double d3 = eyePos.method_1022(interceptVec);
               if ((d3 < currentDist || currentDist == (double)0.0F) && (candidate.method_5668() != entity.method_5668() || currentDist == (double)0.0F)) {
                  pointedEntity = candidate;
                  hitVec = interceptVec;
                  currentDist = d3;
               }
            }
         }

         if (pointedEntity == null || !(currentDist < distToBlock) && objectMouseOver != null && objectMouseOver.method_17783() != class_240.field_1333) {
            return objectMouseOver;
         } else {
            return new class_3966(pointedEntity, hitVec);
         }
      } else {
         return null;
      }
   }

   public static boolean overBlock(Rotation rotation, class_2350 enumFacing, class_2338 pos, boolean strict) {
      if (mc.field_1724 != null && mc.field_1687 != null) {
         class_243 lookVec = class_243.method_1030(rotation.getPitch(), rotation.getYaw());
         class_243 eyePos = mc.field_1724.method_33571();
         double reach = (double)4.5F;
         class_243 endVec = eyePos.method_1019(lookVec.method_1021(reach));
         class_3965 result = mc.field_1687.method_17742(new class_3959(eyePos, endVec, class_3960.field_17559, class_242.field_1348, mc.field_1724));
         if (result.method_17783() == class_240.field_1333) {
            return false;
         } else {
            return result.method_17777().equals(pos) && (!strict || result.method_17780() == enumFacing);
         }
      } else {
         return false;
      }
   }

   public static boolean overBlock(class_2350 enumFacing, class_2338 pos, boolean strict) {
      return overBlock(RotationManager.getRotation(), enumFacing, pos, strict);
   }

   public static Boolean overBlock(Rotation rotation, class_2338 pos) {
      return overBlock(rotation, class_2350.field_11036, pos, false);
   }

   public static Boolean overBlock(Rotation rotation, class_2338 pos, class_2350 enumFacing) {
      return overBlock(rotation, enumFacing, pos, true);
   }
}
