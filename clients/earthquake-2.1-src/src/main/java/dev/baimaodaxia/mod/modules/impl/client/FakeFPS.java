package dev.baimaodaxia.mod.modules.impl.client;

import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.Render2DEvent;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import java.util.Random;
public class FakeFPS extends Module {
   public static FakeFPS INSTANCE;
   private final SliderSetting multiplier = this.add(new SliderSetting("Multiplier", (double)2.0F, (double)0.5F, (double)10.0F, 0.1));
   private final SliderSetting variance = this.add(new SliderSetting("Variance", (double)5.0F, (double)0.0F, (double)50.0F, (double)1.0F));
   private final BooleanSetting smooth = this.add(new BooleanSetting("Smooth", true));
   private int realFps = 0;
   private int fakeFps = 0;
   private int displayFps = 0;
   private final Random random = new Random();

   public FakeFPS() {
      super("FakeFPS", Module.Category.Client);
      this.setChinese("假FPS");
      INSTANCE = this;
   }

   @EventListener
   public void onRender2D(Render2DEvent event) {
      if (!nullCheck()) {
         int currentRealFps = mc.getCurrentFps();
         this.realFps = currentRealFps;
         int baseFakeFps = (int)((double)currentRealFps * this.multiplier.getValue());
         int varianceValue = (int)((double)baseFakeFps * (this.variance.getValue() / (double)100.0F));
         int randomVariance = this.random.nextInt(varianceValue * 2 + 1) - varianceValue;
         int targetFps = baseFakeFps + randomVariance;
         targetFps = Math.max(1, Math.min(targetFps, 10000));
         if (this.smooth.getValue()) {
            this.displayFps = (int)((double)this.displayFps + (double)(targetFps - this.displayFps) * 0.3);
         } else {
            this.displayFps = targetFps;
         }

         this.fakeFps = this.displayFps;
         if (mc.fpsDebugString != null) {
            this.updateFPSDisplay(this.fakeFps);
         }

      }
   }

   private void updateFPSDisplay(int fpsToShow) {
      String debug = mc.fpsDebugString;
      if (debug != null && !debug.isEmpty()) {
         String[] parts = debug.split(" ");
         if (parts.length > 0) {
            try {
               Integer.parseInt(parts[0]);
               parts[0] = String.valueOf(fpsToShow);
               mc.fpsDebugString = String.join(" ", parts);
            } catch (NumberFormatException var6) {
               int space = debug.indexOf(32);
               if (space > 0) {
                  mc.fpsDebugString = fpsToShow + debug.substring(space);
               }
            }
         }
      }

   }

   public int getRealFPS() {
      return this.realFps;
   }

   public int getFakeFPS() {
      return this.isOn() ? this.fakeFps : this.realFps;
   }

   public double getMultiplier() {
      return this.multiplier.getValue();
   }

   public void setMultiplier(double value) {
      this.multiplier.setValue(value);
   }
}
