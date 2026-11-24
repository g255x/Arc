package dev.g255.arc.mod.commands.impl;

import dev.g255.arc.api.utils.i18n.LangUtil;
import dev.g255.arc.core.impl.CommandManager;
import dev.g255.arc.mod.commands.Command;
import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.net.URI;
import java.util.List;

public class WebCommand extends Command {
   private static final String URL = "https://arc.netlify.app/";

   public WebCommand() {
      super("web", "");
   }

   public void runCommand(String[] parameters) {
      try {
         if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Action.BROWSE)) {
            Desktop.getDesktop().browse(new URI("https://arc.netlify.app/"));
            CommandManager.sendMessage(LangUtil.isChinese() ? "§a[Web] §f正在打开官网: https://arc.netlify.app/" : "§a[Web] §fOpening the website: https://arc.netlify.app/");
         } else {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;
            if (os.contains("win")) {
               pb = new ProcessBuilder(new String[]{"rundll32", "url.dll,FileProtocolHandler", "https://arc.netlify.app/"});
            } else if (os.contains("mac")) {
               pb = new ProcessBuilder(new String[]{"open", "https://arc.netlify.app/"});
            } else {
               pb = new ProcessBuilder(new String[]{"xdg-open", "https://arc.netlify.app/"});
            }

            pb.start();
            CommandManager.sendMessage(LangUtil.isChinese() ? "§a[Web] §f正在打开官网: https://arc.netlify.app/" : "§a[Web] §fOpening the website: https://arc.netlify.app/");
         }
      } catch (Exception var4) {
         CommandManager.sendMessage(LangUtil.isChinese() ? "§c[Web] 打开官网失败，请手动访问: https://arc.netlify.app/" : "§c[Web] Could not open the website, visit it manually: https://arc.netlify.app/");
      }

   }

   public String[] getAutocorrect(int count, List<String> seperated) {
      return null;
   }
}
