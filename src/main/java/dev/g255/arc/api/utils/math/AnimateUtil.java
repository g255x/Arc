package dev.g255.arc.api.utils.math;

import dev.g255.arc.Arc;
import dev.g255.arc.api.utils.Wrapper;

public class AnimateUtil implements Wrapper {
   public static float deltaTime() {
      return Arc.FPS.getFps() > 5 ? 1.0F / (float)Arc.FPS.getFps() : 0.016F;
   }

   public static float fast(float end, float start, float multiple) {
      float clampedDelta = MathUtil.clamp(deltaTime() * multiple, 0.0F, 1.0F);
      return (1.0F - clampedDelta) * end + clampedDelta * start;
   }

   public static double animate(double current, double endPoint, double speed) {
      if (speed >= (double)1.0F) {
         return endPoint;
      } else if (speed == (double)0.0F) {
         return current;
      } else {
         boolean shouldContinueAnimation = endPoint > current;
         double dif = Math.max(endPoint, current) - Math.min(endPoint, current);
         if (Math.abs(dif) <= 0.001) {
            return endPoint;
         } else {
            double factor = dif * speed;
            return current + (shouldContinueAnimation ? factor : -factor);
         }
      }
   }
}
