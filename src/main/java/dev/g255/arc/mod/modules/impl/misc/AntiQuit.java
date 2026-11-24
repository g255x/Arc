package dev.g255.arc.mod.modules.impl.misc;

import dev.g255.arc.mod.modules.Module;

public class AntiQuit extends Module {
   public AntiQuit() {
      super("AntiQuit", "Prevents you from quitting the game/server accidentally.", Module.Category.Misc);
      this.setChinese("防误退出");
   }
}
