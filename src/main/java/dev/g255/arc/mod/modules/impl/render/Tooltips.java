package dev.g255.arc.mod.modules.impl.render;

import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;

public class Tooltips extends Module {
   private final BooleanSetting shulker = this.add(new BooleanSetting("Shulker", true));
   private final BooleanSetting bee = this.add(new BooleanSetting("Beehive", true));

   public Tooltips() {
      super("Tooltips", "Shows advanced tooltips for containers.", Module.Category.Render);
      this.setChinese("高级提示");
   }
}
