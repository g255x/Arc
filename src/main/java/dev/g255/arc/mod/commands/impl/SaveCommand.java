package dev.g255.arc.mod.commands.impl;

import dev.g255.arc.Arc;
import dev.g255.arc.core.Manager;
import dev.g255.arc.core.impl.ConfigManager;
import dev.g255.arc.mod.commands.Command;
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
         File folder = new File(var10002 + File.separator + "arc" + File.separator + "cfg");
         if (!folder.exists()) {
            folder.mkdirs();
         }

         ConfigManager.options = Manager.getFile("cfg" + File.separator + parameters[0] + ".cfg");
         Arc.save();
         ConfigManager.options = Manager.getFile("options.txt");
      } else {
         this.sendChatMessage("§fSaving..");
      }

      Arc.save();
   }

   public String[] getAutocorrect(int count, List<String> seperated) {
      return null;
   }
}
