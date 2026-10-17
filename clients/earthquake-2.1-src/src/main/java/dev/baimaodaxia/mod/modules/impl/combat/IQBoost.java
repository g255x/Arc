package dev.baimaodaxia.mod.modules.impl.combat;

import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;

public class IQBoost extends Module {
   private final SliderSetting iq = this.add(new SliderSetting("IQ", (double)114514.0F, (double)0.0F, (double)114514.0F, (double)1.0F));

   public IQBoost() {
      super("IQBoost", Module.Category.Combat);
      this.setChinese("IQ提升");
   }

   public String getInfo() {
      return String.valueOf(this.iq.getValue());
   }
}
