package dev.g255.arc.mod.commands.impl;

import dev.g255.arc.Arc;
import dev.g255.arc.core.Manager;
import dev.g255.arc.core.impl.ConfigManager;
import dev.g255.arc.mod.commands.Command;
import dev.g255.arc.mod.modules.impl.client.Fonts;
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
         Arc.CONFIG = new ConfigManager();
         Arc.CONFIG.load();
         ConfigManager.options = Manager.getFile("options.txt");
         Arc.save();
         Fonts.INSTANCE.refresh();
      }
   }

   public String[] getAutocorrect(int count, List<String> seperated) {
      return null;
   }
}
