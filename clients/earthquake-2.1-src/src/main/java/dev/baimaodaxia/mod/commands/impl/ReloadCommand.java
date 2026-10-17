package dev.baimaodaxia.mod.commands.impl;

import dev.baimaodaxia.earthquake;
import dev.baimaodaxia.core.impl.ConfigManager;
import dev.baimaodaxia.mod.commands.Command;
import java.util.List;
public class ReloadCommand extends Command {
   public ReloadCommand() {
      super("reload", "");
   }

   public void runCommand(String[] parameters) {
      this.sendChatMessage("§fReloading..");
      earthquake.CONFIG = new ConfigManager();
      earthquake.CONFIG.load();
      earthquake.CLEANER.read();
      earthquake.XRAY.read();
      earthquake.TRADE.read();
      earthquake.FRIEND.read();
   }

   public String[] getAutocorrect(int count, List<String> seperated) {
      return null;
   }
}
