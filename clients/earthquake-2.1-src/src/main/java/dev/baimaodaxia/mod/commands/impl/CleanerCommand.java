package dev.baimaodaxia.mod.commands.impl;

import dev.baimaodaxia.earthquake;
import dev.baimaodaxia.core.impl.PlayerManager;
import dev.baimaodaxia.mod.commands.Command;
import dev.baimaodaxia.mod.gui.windows.WindowBase;
import dev.baimaodaxia.mod.gui.windows.WindowsScreen;
import dev.baimaodaxia.mod.gui.windows.impl.ItemSelectWindow;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.Items;
public class CleanerCommand extends Command {
   public CleanerCommand() {
      super("cleaner", "[\"\"/name/reset/clear/list] | [add/remove] [name]");
   }

   public void runCommand(String[] parameters) {
      if (parameters.length == 0) {
         PlayerManager.screenToOpen = new WindowsScreen(new WindowBase[]{new ItemSelectWindow(earthquake.CLEANER)});
      } else {
         switch (parameters[0]) {
            case "reset":
               earthquake.CLEANER.clear();
               earthquake.CLEANER.add(Items.NETHERITE_SWORD.getTranslationKey());
               earthquake.CLEANER.add(Items.NETHERITE_PICKAXE.getTranslationKey());
               earthquake.CLEANER.add(Items.NETHERITE_HELMET.getTranslationKey());
               earthquake.CLEANER.add(Items.NETHERITE_CHESTPLATE.getTranslationKey());
               earthquake.CLEANER.add(Items.NETHERITE_LEGGINGS.getTranslationKey());
               earthquake.CLEANER.add(Items.NETHERITE_BOOTS.getTranslationKey());
               earthquake.CLEANER.add(Items.OBSIDIAN.getTranslationKey());
               earthquake.CLEANER.add(Items.ENDER_CHEST.getTranslationKey());
               earthquake.CLEANER.add(Items.ENDER_PEARL.getTranslationKey());
               earthquake.CLEANER.add(Items.ENCHANTED_GOLDEN_APPLE.getTranslationKey());
               earthquake.CLEANER.add(Items.EXPERIENCE_BOTTLE.getTranslationKey());
               earthquake.CLEANER.add(Items.COBWEB.getTranslationKey());
               earthquake.CLEANER.add(Items.POTION.getTranslationKey());
               earthquake.CLEANER.add(Items.SPLASH_POTION.getTranslationKey());
               earthquake.CLEANER.add(Items.TOTEM_OF_UNDYING.getTranslationKey());
               earthquake.CLEANER.add(Items.END_CRYSTAL.getTranslationKey());
               earthquake.CLEANER.add(Items.ELYTRA.getTranslationKey());
               earthquake.CLEANER.add(Items.FLINT_AND_STEEL.getTranslationKey());
               earthquake.CLEANER.add(Items.PISTON.getTranslationKey());
               earthquake.CLEANER.add(Items.STICKY_PISTON.getTranslationKey());
               earthquake.CLEANER.add(Items.REDSTONE_BLOCK.getTranslationKey());
               earthquake.CLEANER.add(Items.GLOWSTONE.getTranslationKey());
               earthquake.CLEANER.add(Items.RESPAWN_ANCHOR.getTranslationKey());
               earthquake.CLEANER.add(Items.ANVIL.getTranslationKey());
               this.sendChatMessage("§fItems list got reset");
               return;
            case "clear":
               earthquake.CLEANER.getList().clear();
               this.sendChatMessage("§fItems list got clear");
               return;
            case "list":
               if (earthquake.CLEANER.getList().isEmpty()) {
                  this.sendChatMessage("§fItems list is empty");
                  return;
               }

               for(String name : earthquake.CLEANER.getList()) {
                  this.sendChatMessage("§a" + name);
               }

               return;
            case "add":
               if (parameters.length == 2) {
                  earthquake.CLEANER.add(parameters[1]);
                  this.sendChatMessage("§f" + parameters[1] + (earthquake.CLEANER.inList(parameters[1]) ? " §ahas been added" : " §chas been removed"));
                  return;
               }

               this.sendUsage();
               return;
            case "remove":
               if (parameters.length == 2) {
                  earthquake.CLEANER.remove(parameters[1]);
                  this.sendChatMessage("§f" + parameters[1] + (earthquake.CLEANER.inList(parameters[1]) ? " §ahas been added" : " §chas been removed"));
                  return;
               }

               this.sendUsage();
               return;
            default:
               if (parameters.length == 1) {
                  this.sendChatMessage("§f" + parameters[0] + (earthquake.CLEANER.inList(parameters[0]) ? " §ais in whitelist" : " §cisn't in whitelist"));
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
            if (input.equalsIgnoreCase(earthquake.getPrefix() + "cleaner") || x.toLowerCase().startsWith(input)) {
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
