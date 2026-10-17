package dev.baimaodaxia.mod.modules.impl.render;

import dev.baimaodaxia.mod.modules.Module;

public class ForceSneak extends Module {
   public static ForceSneak INSTANCE;

   public ForceSneak() {
      super("ForceSneak", "Makes everyone sneak.", Module.Category.Render);
      this.setChinese("强制蹲下");
      INSTANCE = this;
   }
}
