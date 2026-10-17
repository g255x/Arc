package dev.baimaodaxia.mod.commands.impl;

import dev.baimaodaxia.Alien;
import dev.baimaodaxia.core.Manager;
import dev.baimaodaxia.core.impl.ConfigManager;
import dev.baimaodaxia.mod.commands.Command;
import java.io.File;
import java.util.List;

public class SaveCommand extends Command {
   public SaveCommand() {
      super("save", "");
   }

   public void runCommand(String[] parameters) {
      if (parameters.length == 1) {
         this.sendChatMessage("§fSaving config named " + parameters[0]);
         String var10002 = mc.runDirectory.getPath();
         File folder = new File(var10002 + File.separator + "earthquake".toLowerCase() + File.separator + "cfg");
         if (!folder.exists()) {
            folder.mkdirs();
         }

         ConfigManager.options = Manager.getFile("cfg" + File.separator + parameters[0] + ".cfg");
         Alien.save();
         ConfigManager.options = Manager.getFile("options.txt");
      } else {
         this.sendChatMessage("§fSaving..");
      }

      Alien.save();
   }

   public String[] getAutocorrect(int count, List<String> seperated) {
      return null;
   }
}
