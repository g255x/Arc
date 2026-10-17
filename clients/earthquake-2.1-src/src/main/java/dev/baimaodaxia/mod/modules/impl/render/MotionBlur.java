package dev.baimaodaxia.mod.modules.impl.render;

import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;

public class MotionBlur extends Module {
   public static MotionBlur INSTANCE;
   public final SliderSetting factor = this.add(new SliderSetting("BlurFactor", 0.3, (double)0.0F, (double)1.0F, 0.01));
   public final BooleanSetting max = this.add(new BooleanSetting("Max", false));

   public MotionBlur() {
      super("MotionBlur", Module.Category.Render);
      this.setChinese("运动模糊");
      INSTANCE = this;
   }
}
