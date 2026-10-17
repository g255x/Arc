package dev.kissoo.mod.commands.impl;

import dev.kissoo.Forever;
import dev.kissoo.core.Manager;
import dev.kissoo.core.impl.ConfigManager;
import dev.kissoo.mod.commands.Command;
import java.io.File;
import java.util.List;

public class SaveCommand extends Command {
   public SaveCommand() {
      super("save", "[binds/module/all] <name>");
   }

   @Override
   public void runCommand(String[] parameters) {
      if (parameters.length == 0) {
         this.sendChatMessage("§fSaving..");
         Forever.save();
         return;
      }

      String firstArg = parameters[0].toLowerCase();

      if (firstArg.equals("binds")) {
         if (parameters.length < 2) {
            this.sendUsage();
            return;
         }
         String name = parameters[1];
         this.sendChatMessage("§fSaving binds config named §b" + name);
         ensureFolder("cfg" + File.separator + "Binds");

         File targetFile = Manager.getFile("cfg" + File.separator + "Binds" + File.separator + name + ".cfg");
         File savedOptions = ConfigManager.options;
         try {
            ConfigManager.options = targetFile;
            Forever.CONFIG.saveBinds();
         } finally {
            ConfigManager.options = savedOptions;
         }

         this.sendChatMessage("§aBind config saved: §f" + name);

      } else if (firstArg.equals("module")) {
         if (parameters.length < 2) {
            this.sendUsage();
            return;
         }
         String name = parameters[1];
         this.sendChatMessage("§fSaving module config named §b" + name);
         ensureFolder("cfg" + File.separator + "Modules");

         File targetFile = Manager.getFile("cfg" + File.separator + "Modules" + File.separator + name + ".cfg");
         File savedOptions = ConfigManager.options;
         try {
            ConfigManager.options = targetFile;
            Forever.CONFIG.saveModules();
         } finally {
            ConfigManager.options = savedOptions;
         }

         this.sendChatMessage("§aModule config saved: §f" + name);

      } else if (firstArg.equals("all")) {
         if (parameters.length < 2) {
            this.sendUsage();
            return;
         }
         String name = parameters[1];
         this.sendChatMessage("§fSaving all config named §b" + name);
         ensureFolder("cfg" + File.separator + "All");

         File targetFile = Manager.getFile("cfg" + File.separator + "All" + File.separator + name + ".cfg");
         File savedOptions = ConfigManager.options;
         try {
            ConfigManager.options = targetFile;
            Forever.CONFIG.save();
         } finally {
            ConfigManager.options = savedOptions;
         }

         Forever.save();
         this.sendChatMessage("§aAll config saved: §f" + name);

      } else {
         this.sendUsage();
      }
   }

   @Override
   public String[] getAutocorrect(int count, List<String> seperated) {
      if (count == 1) {
         return new String[]{"binds", "module", "all"};
      }

      if (count == 2) {
         if (seperated.size() < 2) {
            return null;
         }
         String subCommand = seperated.get(1).toLowerCase();
         switch (subCommand) {
            case "binds":
               return listConfigs("Binds");
            case "module":
               return listConfigs("Modules");
            case "all":
               return listConfigs("All");
            default:
               return null;
         }
      }

      return null;
   }

   private void ensureFolder(String relativePath) {
      File folder = Manager.getFile(relativePath);
      if (!folder.exists()) {
         folder.mkdirs();
      }
   }

   private String[] listConfigs(String subDir) {
      File folder = Manager.getFile("cfg" + File.separator + subDir);
      if (!folder.exists() || !folder.isDirectory()) {
         return null;
      }

      File[] files = folder.listFiles((dir, name) -> name.endsWith(".cfg"));
      if (files == null || files.length == 0) {
         return null;
      }

      String[] result = new String[files.length];
      for (int i = 0; i < files.length; i++) {
         String fileName = files[i].getName();
         result[i] = fileName.substring(0, fileName.length() - 4);
      }
      return result;
   }
}