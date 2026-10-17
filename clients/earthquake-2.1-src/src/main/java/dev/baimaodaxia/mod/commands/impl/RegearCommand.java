package dev.baimaodaxia.mod.commands.impl;

import dev.baimaodaxia.core.impl.CommandManager;
import dev.baimaodaxia.mod.commands.Command;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.client.MinecraftClient;
public class RegearCommand extends Command {
   private static final File KITS_FILE;

   public RegearCommand() {
      super("regear", "Manage your regear kits");
   }

   public void runCommand(String[] parameters) {
      if (parameters.length < 1) {
         this.sendUsage();
      } else {
         switch (parameters[0].toLowerCase()) {
            case "save":
            case "create":
               if (parameters.length == 2) {
                  this.saveKit(parameters[1]);
               } else {
                  CommandManager.sendMessage("§cUsage: .regear save <name>");
               }
               break;
            case "load":
            case "set":
               if (parameters.length == 2) {
                  this.setKit(parameters[1]);
               } else {
                  CommandManager.sendMessage("§cUsage: .regear load <name>");
               }
               break;
            case "delete":
            case "del":
               if (parameters.length == 2) {
                  this.deleteKit(parameters[1]);
               } else {
                  CommandManager.sendMessage("§cUsage: .regear delete <name>");
               }
               break;
            case "list":
               this.listKits();
               break;
            case "current":
               this.getCurrentKit();
               break;
            case "clear":
               this.clearCurrent();
               break;
            case "help":
            default:
               this.sendUsage();
         }

      }
   }

   public void sendUsage() {
      CommandManager.sendMessage("§6§lRegear Commands:");
      CommandManager.sendMessage("§e.regear save <name> §7- Save current inventory as kit");
      CommandManager.sendMessage("§e.regear load <name> §7- Load a kit");
      CommandManager.sendMessage("§e.regear delete <name> §7- Delete a kit");
      CommandManager.sendMessage("§e.regear list §7- List all kits");
      CommandManager.sendMessage("§e.regear current §7- Show current kit");
      CommandManager.sendMessage("§e.regear clear §7- Clear current kit");
   }

   private void saveKit(String name) {
      if (name.equalsIgnoreCase("pointer")) {
         CommandManager.sendMessage("§c'pointer' is a reserved name!");
      } else {
         try {
            Map<String, Object> kits = this.loadKits();
            if (kits.containsKey(name)) {
               CommandManager.sendMessage("§cKit '" + name + "' already exists!");
               return;
            }

            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) {
               CommandManager.sendMessage("§cNot in game!");
               return;
            }

            StringBuilder inventoryData = new StringBuilder();

            for(int i = 0; i < 36; ++i) {
               ItemStack stack = (ItemStack)mc.player.getInventory().main.get(i);
               if (stack.isEmpty()) {
                  inventoryData.append("minecraft:air");
               } else {
                  String itemKey = stack.getItem().getTranslationKey();
                  if (stack.getItem() instanceof PotionItem) {
                     String baseKey = itemKey.replace("item.minecraft.", "");
                     if (baseKey.contains("potion")) {
                        if (stack.getItem() instanceof SplashPotionItem) {
                           baseKey = "splash_potion";
                        } else {
                           baseKey = "potion";
                        }

                        inventoryData.append("item.minecraft.").append(baseKey);
                     } else {
                        inventoryData.append(itemKey);
                     }
                  } else {
                     inventoryData.append(itemKey);
                  }
               }

               if (i < 35) {
                  inventoryData.append(" ");
               }
            }

            kits.put(name, inventoryData.toString());
            this.saveKits(kits);
            CommandManager.sendMessage("§aKit '" + name + "' saved successfully!");
         } catch (Exception e) {
            CommandManager.sendMessage("§cError saving kit: " + e.getMessage());
            e.printStackTrace();
         }

      }
   }

   private void setKit(String name) {
      try {
         Map<String, Object> kits = this.loadKits();
         if (!kits.containsKey(name) || name.equalsIgnoreCase("pointer")) {
            CommandManager.sendMessage("§cKit '" + name + "' not found!");
            return;
         }

         kits.put("pointer", name);
         this.saveKits(kits);
         CommandManager.sendMessage("§aKit '" + name + "' loaded! Enable ChestStealer in Regear mode to use it.");
      } catch (Exception e) {
         CommandManager.sendMessage("§cError loading kit: " + e.getMessage());
      }

   }

   private void deleteKit(String name) {
      try {
         Map<String, Object> kits = this.loadKits();
         if (!kits.containsKey(name) || name.equalsIgnoreCase("pointer")) {
            CommandManager.sendMessage("§cKit '" + name + "' not found!");
            return;
         }

         kits.remove(name);
         if (kits.containsKey("pointer") && kits.get("pointer").equals(name)) {
            kits.put("pointer", "none");
         }

         this.saveKits(kits);
         CommandManager.sendMessage("§aKit '" + name + "' deleted!");
      } catch (Exception e) {
         CommandManager.sendMessage("§cError deleting kit: " + e.getMessage());
      }

   }

   private void listKits() {
      try {
         Map<String, Object> kits = this.loadKits();
         if (kits.size() <= 1) {
            CommandManager.sendMessage("§cNo kits saved!");
            return;
         }

         CommandManager.sendMessage("§6§lSaved Kits:");

         for(String key : kits.keySet()) {
            if (!key.equals("pointer")) {
               CommandManager.sendMessage("§e- " + key);
            }
         }

         String current = (String)kits.getOrDefault("pointer", "none");
         CommandManager.sendMessage("§7Current kit: §a" + current);
      } catch (Exception e) {
         CommandManager.sendMessage("§cError listing kits: " + e.getMessage());
      }

   }

   private void getCurrentKit() {
      try {
         Map<String, Object> kits = this.loadKits();
         String current = (String)kits.getOrDefault("pointer", "none");
         CommandManager.sendMessage("§7Current kit: §a" + current);
      } catch (Exception e) {
         CommandManager.sendMessage("§cError getting current kit: " + e.getMessage());
      }

   }

   private void clearCurrent() {
      try {
         Map<String, Object> kits = this.loadKits();
         kits.put("pointer", "none");
         this.saveKits(kits);
         CommandManager.sendMessage("§aCurrent kit cleared!");
      } catch (Exception e) {
         CommandManager.sendMessage("§cError clearing current kit: " + e.getMessage());
      }

   }

   private Map<String, Object> loadKits() throws IOException {
      Map<String, Object> kits = new HashMap();
      if (!KITS_FILE.exists()) {
         KITS_FILE.getParentFile().mkdirs();
         KITS_FILE.createNewFile();
         kits.put("pointer", "none");
         this.saveKits(kits);
         return kits;
      } else {
         BufferedReader reader = new BufferedReader(new FileReader(KITS_FILE));

         String line;
         try {
            while((line = reader.readLine()) != null) {
               line = line.trim();
               if (!line.isEmpty() && !line.startsWith("#") && line.contains(":")) {
                  String[] parts = line.split(":", 2);
                  if (parts.length == 2) {
                     kits.put(parts[0].trim(), parts[1].trim());
                  }
               }
            }
         } catch (Throwable var6) {
            try {
               reader.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }

            throw var6;
         }

         reader.close();
         if (!kits.containsKey("pointer")) {
            kits.put("pointer", "none");
         }

         return kits;
      }
   }

   private void saveKits(Map<String, Object> kits) throws IOException {
      BufferedWriter writer = new BufferedWriter(new FileWriter(KITS_FILE));

      try {
         writer.write("# Luminous Regear Kits");
         writer.newLine();
         writer.newLine();

         for(Map.Entry<String, Object> entry : kits.entrySet()) {
            String var10001 = (String)entry.getKey();
            writer.write(var10001 + ": " + String.valueOf(entry.getValue()));
            writer.newLine();
         }
      } catch (Throwable var6) {
         try {
            writer.close();
         } catch (Throwable var5) {
            var6.addSuppressed(var5);
         }

         throw var6;
      }

      writer.close();
   }

   public String[] getAutocorrect(int count, List<String> seperated) {
      if (seperated.size() <= count) {
         if (count == 1) {
            return new String[]{"save", "load", "delete", "list", "current", "clear", "help"};
         } else {
            return count == 2 ? new String[]{"<kit_name>"} : null;
         }
      } else if (count == 1) {
         return new String[]{"save", "load", "delete", "list", "current", "clear", "help"};
      } else if (count == 2) {
         switch (((String)seperated.get(1)).toLowerCase()) {
            case "save":
               return new String[]{"<kit_name>"};
            case "load":
            case "delete":
               try {
                  Map<String, Object> kits = this.loadKits();
                  List<String> kitNames = new ArrayList();

                  for(String key : kits.keySet()) {
                     if (!key.equals("pointer")) {
                        kitNames.add(key);
                     }
                  }

                  return (String[])kitNames.toArray(new String[0]);
               } catch (Exception var10) {
                  return new String[]{"<kit_name>"};
               }
            default:
               return null;
         }
      } else {
         return null;
      }
   }

   static {
      String var10002 = System.getProperty("user.dir");
      KITS_FILE = new File(var10002 + File.separator + "Nina" + File.separator + "kits.cfg");
   }
}
