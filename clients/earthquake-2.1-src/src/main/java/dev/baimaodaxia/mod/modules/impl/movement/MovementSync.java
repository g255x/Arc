package dev.baimaodaxia.mod.modules.impl.movement;

import dev.baimaodaxia.mod.modules.Module;

public class MovementSync extends Module {
   public static MovementSync INSTANCE;

   public MovementSync() {
      super("MovementSync", Module.Category.Movement);
      this.setChinese("移动同步");
      INSTANCE = this;
   }
}
