package dev.baimaodaxia.mod.modules.impl.player;

public class InvManager extends Sorter {
   public static InvManager INSTANCE;

   public InvManager() {
      super("InvManager", "背包管理");
      INSTANCE = this;
   }
}
