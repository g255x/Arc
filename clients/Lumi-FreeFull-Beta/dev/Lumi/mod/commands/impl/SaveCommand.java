package dev.Lumi.mod.commands.impl;

import dev.Lumi.Lumi;
import dev.Lumi.core.impl.ConfigManager;
import dev.Lumi.mod.commands.Command;
import java.util.List;

public class SaveCommand extends Command {
   public SaveCommand() {
      super("save", "");
   }

   public void runCommand(String[] parameters) {
      if (parameters.length == 1) {
         this.sendChatMessage("§fSaving config named " + parameters[0]);
         ConfigManager.saveCfg(parameters[0]);
      } else {
         this.sendChatMessage("§fSaving..");
      }

      Lumi.save();
   }

   public String[] getAutocorrect(int count, List<String> seperated) {
      return null;
   }
}
