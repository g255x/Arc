package dev.g255.arc.mod.modules.impl.client;

import dev.g255.arc.mod.modules.Module;
import java.io.PrintWriter;

public class IRC extends Module {
   public static PrintWriter printWriter;
   private static final String SERVER_HOST = "47.121.113.160";
   private static final int SERVER_PORT = 6667;
   public static volatile boolean connect;

   public IRC() {
      super("LowInputLatency", Module.Category.Client);
      this.setChinese("低输入延迟");
   }

   public void onEnable() {
      super.onEnable();
      System.out.println("[Arc] IRC by G255 - Connected to 47.121.113.160:6667");
   }

   public void onDisable() {
      super.onDisable();
      System.out.println("[Arc] IRC by G255 - Disconnected");
   }
}
