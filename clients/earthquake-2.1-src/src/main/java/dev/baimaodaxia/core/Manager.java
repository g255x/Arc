package dev.baimaodaxia.core;

import java.io.File;
import net.minecraft.client.MinecraftClient;

public class Manager {
   public static final MinecraftClient mc = MinecraftClient.getInstance();

   public static File getFile(String s) {
      File folder = getFolder();
      return new File(folder, s);
   }

   public static File getFolder() {
      String var10002 = mc.runDirectory.getPath();
      File folder = new File(var10002 + File.separator + "earthquake".toLowerCase());
      if (!folder.exists()) {
         folder.mkdirs();
      }

      return folder;
   }
}
