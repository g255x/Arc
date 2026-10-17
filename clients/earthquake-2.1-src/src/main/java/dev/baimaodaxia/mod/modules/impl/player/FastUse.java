package dev.baimaodaxia.mod.modules.impl.player;

import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;

public class FastUse extends Module {
   private final SliderSetting delay = this.add(new SliderSetting("Delay", (double)0.0F, (double)0.0F, (double)4.0F, (double)1.0F));

   public FastUse() {
      super("FastUse", Module.Category.Player);
   }

   public void onUpdate() {
      if (mc.itemUseCooldown <= 4 - this.delay.getValueInt()) {
         mc.itemUseCooldown = 0;
      }

   }
}
