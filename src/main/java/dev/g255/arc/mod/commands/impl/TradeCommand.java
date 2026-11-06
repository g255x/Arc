package dev.g255.arc.mod.commands.impl;

import dev.g255.arc.Arc;
import dev.g255.arc.core.impl.PlayerManager;
import dev.g255.arc.mod.commands.Command;
import dev.g255.arc.mod.gui.windows.WindowBase;
import dev.g255.arc.mod.gui.windows.WindowsScreen;
import dev.g255.arc.mod.gui.windows.impl.ItemSelectWindow;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.Items;

public class TradeCommand extends Command {
   public TradeCommand() {
      super("trade", "[\"\"/name/reset/clear/list] | [add/remove] [name]");
   }

   public void runCommand(String[] parameters) {
      if (parameters.length == 0) {
         PlayerManager.screenToOpen = new WindowsScreen(new WindowBase[]{new ItemSelectWindow(Arc.TRADE)});
      } else {
         switch (parameters[0]) {
            case "reset":
               Arc.TRADE.clear();
               Arc.TRADE.add(Items.ENCHANTED_BOOK.getTranslationKey());
               Arc.TRADE.add(Items.DIAMOND_BLOCK.getTranslationKey());
               this.sendChatMessage("§fItems list got reset");
               return;
            case "clear":
               Arc.TRADE.clear();
               this.sendChatMessage("§fItems list got clear");
               return;
            case "list":
               if (Arc.TRADE.getList().isEmpty()) {
                  this.sendChatMessage("§fItems list is empty");
                  return;
               }

               for(String name : Arc.TRADE.getList()) {
                  this.sendChatMessage("§a" + name);
               }

               return;
            case "add":
               if (parameters.length == 2) {
                  Arc.TRADE.add(parameters[1]);
                  this.sendChatMessage("§f" + parameters[1] + (Arc.TRADE.inWhitelist(parameters[1]) ? " §ahas been added" : " §chas been removed"));
                  return;
               }

               this.sendUsage();
               return;
            case "remove":
               if (parameters.length == 2) {
                  Arc.TRADE.remove(parameters[1]);
                  this.sendChatMessage("§f" + parameters[1] + (Arc.TRADE.inWhitelist(parameters[1]) ? " §ahas been added" : " §chas been removed"));
                  return;
               }

               this.sendUsage();
               return;
            default:
               if (parameters.length == 1) {
                  this.sendChatMessage("§f" + parameters[0] + (Arc.TRADE.inWhitelist(parameters[0]) ? " §ais in whitelist" : " §cisn't in whitelist"));
               } else {
                  this.sendUsage();
               }
         }
      }
   }

   public String[] getAutocorrect(int count, List<String> seperated) {
      if (count != 1) {
         return null;
      } else {
         String input = ((String)seperated.getLast()).toLowerCase();
         ArrayList<String> correct = new ArrayList();

         for(String x : List.of("add", "remove", "list", "reset", "clear")) {
            if (input.equalsIgnoreCase(Arc.getPrefix() + "trade") || x.toLowerCase().startsWith(input)) {
               correct.add(x);
            }
         }

         int numCmds = correct.size();
         String[] commands = new String[numCmds];
         int i = 0;

         for(String x : correct) {
            commands[i++] = x;
         }

         return commands;
      }
   }
}
