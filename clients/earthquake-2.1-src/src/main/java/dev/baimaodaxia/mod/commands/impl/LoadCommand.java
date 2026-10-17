package dev.baimaodaxia.mod.commands.impl;

import dev.baimaodaxia.Alien;
import dev.baimaodaxia.earthquake;
import dev.baimaodaxia.core.Manager;
import dev.baimaodaxia.core.impl.ConfigManager;
import dev.baimaodaxia.mod.commands.Command;
import dev.baimaodaxia.mod.modules.impl.client.Fonts;
import java.io.File;
import java.util.List;

public class LoadCommand extends Command {
   public LoadCommand() {
      super("load", "[config]");
   }

   public void runCommand(String[] parameters) {
      if (parameters.length == 0) {
         this.sendUsage();
      } else {
         this.sendChatMessage("§fLoading..");
         ConfigManager.options = Manager.getFile("cfg" + File.separator + parameters[0] + ".cfg");
         Alien.CONFIG = new ConfigManager();
         earthquake.CONFIG.load();
         ConfigManager.options = Manager.getFile("options.txt");
         Alien.save();
         Fonts.INSTANCE.refresh();
      }
   }

   public String[] getAutocorrect(int count, List<String> seperated) {
      return null;
   }
}
