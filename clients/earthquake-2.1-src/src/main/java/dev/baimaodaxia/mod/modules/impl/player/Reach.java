package dev.baimaodaxia.mod.modules.impl.player;

import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;

public class Reach extends Module {
   public static Reach INSTANCE;
   public final SliderSetting distance = this.add(new SliderSetting("Distance", (double)5.0F, (double)1.0F, (double)15.0F, 0.1));

   public Reach() {
      super("Reach", Module.Category.Player);
      INSTANCE = this;
   }
}
