package dev.kissoo.mod.commands.impl;

import dev.kissoo.Forever;
import dev.kissoo.core.impl.ConfigManager;
import dev.kissoo.mod.commands.Command;
import java.util.List;

public class ReloadCommand extends Command {
   public ReloadCommand() {
      super("reload", "");
   }

   @Override
   public void runCommand(String[] parameters) {
      this.sendChatMessage("§fReloading..");
      Forever.CONFIG = new ConfigManager();
      Forever.CONFIG.load();
      Forever.CLEANER.read();
      Forever.XRAY.read();
      Forever.TRADE.read();
      Forever.FRIEND.read();
   }

   @Override
   public String[] getAutocorrect(int count, List<String> seperated) {
      return null;
   }
}
