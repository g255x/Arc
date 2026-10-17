package dev.kissoo.mod.commands.impl;

import dev.kissoo.Forever;
import dev.kissoo.core.impl.PlayerManager;
import dev.kissoo.mod.commands.Command;
import dev.kissoo.mod.gui.windows.WindowsScreen;
import dev.kissoo.mod.gui.windows.impl.ItemSelectWindow;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.Items;

public class CleanerCommand extends Command {
   public CleanerCommand() {
      super("cleaner", "[\"\"/name/reset/clear/list] | [add/remove] [name]");
   }

   @Override
   public void runCommand(String[] parameters) {
      if (parameters.length == 0) {
         PlayerManager.screenToOpen = new WindowsScreen(new ItemSelectWindow(Forever.CLEANER));
      } else {
         String var2 = parameters[0];
         switch (var2) {
            case "reset":
               Forever.CLEANER.clear();
               Forever.CLEANER.add(Items.NETHERITE_SWORD.getTranslationKey());
               Forever.CLEANER.add(Items.NETHERITE_PICKAXE.getTranslationKey());
               Forever.CLEANER.add(Items.NETHERITE_HELMET.getTranslationKey());
               Forever.CLEANER.add(Items.NETHERITE_CHESTPLATE.getTranslationKey());
               Forever.CLEANER.add(Items.NETHERITE_LEGGINGS.getTranslationKey());
               Forever.CLEANER.add(Items.NETHERITE_BOOTS.getTranslationKey());
               Forever.CLEANER.add(Items.OBSIDIAN.getTranslationKey());
               Forever.CLEANER.add(Items.ENDER_CHEST.getTranslationKey());
               Forever.CLEANER.add(Items.ENDER_PEARL.getTranslationKey());
               Forever.CLEANER.add(Items.ENCHANTED_GOLDEN_APPLE.getTranslationKey());
               Forever.CLEANER.add(Items.EXPERIENCE_BOTTLE.getTranslationKey());
               Forever.CLEANER.add(Items.COBWEB.getTranslationKey());
               Forever.CLEANER.add(Items.POTION.getTranslationKey());
               Forever.CLEANER.add(Items.SPLASH_POTION.getTranslationKey());
               Forever.CLEANER.add(Items.TOTEM_OF_UNDYING.getTranslationKey());
               Forever.CLEANER.add(Items.END_CRYSTAL.getTranslationKey());
               Forever.CLEANER.add(Items.ELYTRA.getTranslationKey());
               Forever.CLEANER.add(Items.FLINT_AND_STEEL.getTranslationKey());
               Forever.CLEANER.add(Items.PISTON.getTranslationKey());
               Forever.CLEANER.add(Items.STICKY_PISTON.getTranslationKey());
               Forever.CLEANER.add(Items.REDSTONE_BLOCK.getTranslationKey());
               Forever.CLEANER.add(Items.GLOWSTONE.getTranslationKey());
               Forever.CLEANER.add(Items.RESPAWN_ANCHOR.getTranslationKey());
               Forever.CLEANER.add(Items.ANVIL.getTranslationKey());
               this.sendChatMessage("§fItems list got reset");
               return;
            case "clear":
               Forever.CLEANER.getList().clear();
               this.sendChatMessage("§fItems list got clear");
               return;
            case "list":
               if (Forever.CLEANER.getList().isEmpty()) {
                  this.sendChatMessage("§fItems list is empty");
                  return;
               }

               for (String name : Forever.CLEANER.getList()) {
                  this.sendChatMessage("§a" + name);
               }

               return;
            case "add":
               if (parameters.length == 2) {
                  Forever.CLEANER.add(parameters[1]);
                  this.sendChatMessage("§f" + parameters[1] + (Forever.CLEANER.inList(parameters[1]) ? " §ahas been added" : " §chas been removed"));
                  return;
               }

               this.sendUsage();
               return;
            case "remove":
               if (parameters.length == 2) {
                  Forever.CLEANER.remove(parameters[1]);
                  this.sendChatMessage("§f" + parameters[1] + (Forever.CLEANER.inList(parameters[1]) ? " §ahas been added" : " §chas been removed"));
                  return;
               }

               this.sendUsage();
               return;
            default:
               if (parameters.length == 1) {
                  this.sendChatMessage("§f" + parameters[0] + (Forever.CLEANER.inList(parameters[0]) ? " §ais in whitelist" : " §cisn't in whitelist"));
               } else {
                  this.sendUsage();
               }
         }
      }
   }

   @Override
   public String[] getAutocorrect(int count, List<String> seperated) {
      if (count != 1) {
         return null;
      } else {
         String input = ((String)seperated.getLast()).toLowerCase();
         List<String> correct = new ArrayList();

         for (String x : List.of("add", "remove", "list", "reset", "clear")) {
            if (input.equalsIgnoreCase(Forever.getPrefix() + "cleaner") || x.toLowerCase().startsWith(input)) {
               correct.add(x);
            }
         }

         int numCmds = correct.size();
         String[] commands = new String[numCmds];
         int i = 0;

         for (String xx : correct) {
            commands[i++] = xx;
         }

         return commands;
      }
   }
}
