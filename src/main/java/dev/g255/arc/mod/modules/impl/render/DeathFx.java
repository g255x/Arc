package dev.g255.arc.mod.modules.impl.render;

import dev.g255.arc.api.utils.skia.Skia;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Font;
import io.github.humbleui.types.Rect;
import java.awt.Color;
import java.lang.reflect.Method;

public final class DeathFx {
   public static final String TEXT = "Ezzzzzz";
   private static Method layerMethod;
   private static boolean layerResolved;
   private static float cachedSize = -1.0F;
   private static float cachedAvailW = -1.0F;
   private static Font cachedFont;
   private static float cachedW;
   private static float cachedH;

   private DeathFx() {
   }

   public static int pushLayer(float f, int n, int n2, int n3, int n4) {
      Canvas canvas = Skia.getCanvas();
      if (canvas == null) {
         return 0;
      } else {
         int n5 = Math.round(f * 255.0F);
         if (n5 < 0) {
            n5 = 0;
         }

         if (n5 > 255) {
            n5 = 255;
         }

         if (!layerResolved) {
            layerResolved = true;

            try {
               for(Method method : canvas.getClass().getMethods()) {
                  Class<?>[] classArray = method.getParameterTypes();
                  if (method.getName().equals("saveLayerAlpha") && classArray.length == 2 && !classArray[0].isPrimitive() && classArray[1] == Integer.TYPE) {
                     layerMethod = method;
                     break;
                  }
               }
            } catch (Throwable var14) {
            }
         }

         if (layerMethod != null) {
            try {
               Object throwable = layerMethod.invoke(canvas, Rect.makeXYWH((float)n, (float)n2, (float)n3, (float)n4), n5);
               if (throwable instanceof Integer) {
                  return (Integer)throwable;
               }
            } catch (Throwable var13) {
            }

            try {
               Object object = layerMethod.invoke(canvas, null, n5);
               if (object instanceof Integer) {
                  return (Integer)object;
               }
            } catch (Throwable var12) {
            }
         }

         return canvas.save();
      }
   }

   public static long clampMs(double d) {
      long l = (long)d;
      return l < 1L ? 1L : l;
   }

   public static boolean inWindow(long l, long l2, double d, double d2) {
      if (l <= 0L) {
         return false;
      } else {
         return l2 - l < clampMs(d) + clampMs(d2);
      }
   }

   public static float alphaFactor(long l, long l2, double d, double d2) {
      if (l <= 0L) {
         return 1.0F;
      } else {
         long l4 = l2 - l;
         long l5 = clampMs(d);
         long l3;
         if (l4 >= l5 + (l3 = clampMs(d2))) {
            return 0.0F;
         } else {
            return l4 <= l5 ? 1.0F : 1.0F - (float)(l4 - l5) / (float)l3;
         }
      }
   }

   public static void drawCard(int n, int n2, int n3, int n4, float f) {
      if (n3 > 10 && n4 > 10 && !(f <= 0.001F)) {
         float f2 = (float)n3 - 8.0F;
         float f3 = (float)n4 - 8.0F;
         float f4 = Math.min(f3 * 0.9F, 22.0F);
         if (f4 < 4.0F) {
            f4 = 4.0F;
         }

         if (f4 != cachedSize || f2 != cachedAvailW) {
            Font font = Skia.getDefaultFont("Ezzzzzz", f4, true);
            float f5 = Skia.getStringWidth("Ezzzzzz", font);
            if (f5 > f2 && f5 > 0.0F) {
               if ((f4 *= f2 / f5) < 4.0F) {
                  f4 = 4.0F;
               }

               font = Skia.getDefaultFont("Ezzzzzz", f4, true);
               f5 = Skia.getStringWidth("Ezzzzzz", font);
            }

            cachedSize = f4;
            cachedAvailW = f2;
            cachedFont = font;
            cachedW = f5;
            cachedH = Skia.getFontHeight(font);
         }

         int n5;
         if ((n5 = Math.round(255.0F * f)) < 0) {
            n5 = 0;
         }

         if (n5 > 255) {
            n5 = 255;
         }

         Color color = new Color(248, 248, 248, n5);
         Skia.drawText("Ezzzzzz", (float)n + ((float)n3 - cachedW) / 2.0F, (float)n2 + ((float)n4 - cachedH) / 2.0F, color, cachedFont);
      }
   }
}
