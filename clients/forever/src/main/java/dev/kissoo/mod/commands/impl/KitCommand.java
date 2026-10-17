package dev.kissoo.mod.commands.impl;

import dev.kissoo.Forever;
import dev.kissoo.core.Manager;
import dev.kissoo.mod.commands.Command;
import net.minecraft.registry.Registries;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class KitCommand extends Command {
   public KitCommand() {
      super("kit", "[list] | [create/delete] [name]");
   }

   @Override
   public void runCommand(String[] parameters) {
      if (parameters.length == 0) {
         this.sendUsage();
      } else {
         String var2 = parameters[0];
         switch (var2) {
            case "list":
               if (parameters.length == 1) {
                  try {
                     for (File file : Manager.getFolder().listFiles()) {
                        if (file.getName().endsWith(".kit")) {
                           String name = file.getName();
                           this.sendChatMessage("Kit: [" + name.substring(0, name.length() - 4) + "]");
                        }
                     }
                  } catch (Exception var12) {
                     var12.printStackTrace();
                  }

                  return;
               }

               this.sendUsage();
               return;
            case "create":
               if (parameters.length != 2) {
                  this.sendUsage();
                  return;
               }

               if (mc.player == null) {
                  return;
               }

               try {
                  File file = Manager.getFile(parameters[1] + ".kit");
                  PrintWriter printwriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
                  for (int i = 0; i < 40; i++) {
                     printwriter.println(i + ":" + Registries.ITEM.getId(mc.player.getInventory().getStack(i).getItem()).toString());
                  }

                  printwriter.close();
                  this.sendChatMessage("\u00a7fKit [" + parameters[1] + "] created (Saved Inventory & Armor - No Offhand)");
               } catch (Exception var11) {
                  this.sendChatMessage("\u00a7fKit [" + parameters[1] + "] create failed");
                  var11.printStackTrace();
               }

               return;
            case "delete":
               if (parameters.length == 2) {
                  try {
                     File file = Manager.getFile(parameters[1] + ".kit");
                     if (file.exists()) {
                        file.delete();
                     }

                     this.sendChatMessage("\u00a7fKit [" + parameters[1] + "] removed");
                  } catch (Exception var10) {
                     var10.printStackTrace();
                  }

                  return;
               }

               this.sendUsage();
               return;
            default:
               this.sendUsage();
         }
      }
   }

   @Override
   public String[] getAutocorrect(int count, List<String> seperated) {
      if (count != 1) {
         return null;
      } else {
         String input = ((String)seperated.getLast()).toLowerCase();
         List<String> correct = new ArrayList<>();

         for (String x : List.of("list", "create", "delete")) {
            if (input.equalsIgnoreCase(Forever.getPrefix() + "kit") || x.toLowerCase().startsWith(input)) {
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