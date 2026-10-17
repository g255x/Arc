package dev.Lumi.mod.commands.impl;

import dev.Lumi.Lumi;
import dev.Lumi.core.impl.ConfigManager;
import dev.Lumi.mod.commands.Command;
import java.util.List;

public class ReloadCommand extends Command {
   public ReloadCommand() {
      super("reload", "");
   }

   public void runCommand(String[] parameters) {
      this.sendChatMessage("§fReloading..");
      Lumi.CONFIG = new ConfigManager();
      Lumi.CONFIG.load();
      Lumi.FRIEND.read();
   }

   public String[] getAutocorrect(int count, List<String> seperated) {
      return null;
   }
}
