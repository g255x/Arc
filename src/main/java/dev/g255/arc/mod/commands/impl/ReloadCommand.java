package dev.g255.arc.mod.commands.impl;

import dev.g255.arc.Arc;
import dev.g255.arc.core.impl.ConfigManager;
import dev.g255.arc.mod.commands.Command;
import java.util.List;

public class ReloadCommand extends Command {
   public ReloadCommand() {
      super("reload", "");
   }

   public void runCommand(String[] parameters) {
      this.sendChatMessage("§fReloading..");
      Arc.CONFIG = new ConfigManager();
      Arc.CONFIG.load();
      Arc.CLEANER.read();
      Arc.XRAY.read();
      Arc.TRADE.read();
      Arc.FRIEND.read();
   }

   public String[] getAutocorrect(int count, List<String> seperated) {
      return null;
   }
}
