package dev.luminous.mod.commands.impl;

import dev.luminous.Nina;
import dev.luminous.core.impl.ConfigManager;
import dev.luminous.mod.commands.Command;
import java.util.List;

public class ReloadCommand extends Command {
   public ReloadCommand() {
      super("reload", "");
   }

   public void runCommand(String[] parameters) {
      this.sendChatMessage("§fReloading..");
      Nina.CONFIG = new ConfigManager();
      Nina.CONFIG.load();
      Nina.CLEANER.read();
      Nina.XRAY.read();
      Nina.TRADE.read();
      Nina.FRIEND.read();
   }

   public String[] getAutocorrect(int count, List seperated) {
      return null;
   }
}
