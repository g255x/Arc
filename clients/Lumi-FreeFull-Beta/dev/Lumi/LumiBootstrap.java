package dev.Lumi;

import net.fabricmc.api.ModInitializer;

public class LumiBootstrap implements ModInitializer {
   public void onInitialize() {
      System.setProperty("java.awt.headless", "false");
      (new Lumi()).onInitialize();
   }
}
