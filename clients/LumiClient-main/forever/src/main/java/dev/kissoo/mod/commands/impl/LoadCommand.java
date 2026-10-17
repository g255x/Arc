package dev.kissoo.mod.commands.impl;

import dev.kissoo.Forever;
import dev.kissoo.core.Manager;
import dev.kissoo.core.impl.ConfigManager;
import dev.kissoo.mod.commands.Command;
import dev.kissoo.mod.modules.impl.client.Fonts;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class LoadCommand extends Command {
   public LoadCommand() {
      super("load", "[binds/module/all/cloud/list/clone] <config>");
   }

   @Override
   public void runCommand(String[] parameters) {
      if (parameters.length == 0) {
         this.sendUsage();
         return;
      }

      String firstArg = parameters[0].toLowerCase();

      if (firstArg.equals("list")) {
         this.sendChatMessage("§7Fetching cloud config list...");
         try {
            String token = "token";
            String hwid  = "hwid";
            String url   = "https://52.175.50.86/api/index" + ".php?route=/cloud-config"
                         + "&token=" + URLEncoder.encode(token, "UTF-8")
                         + "&hwid="  + URLEncoder.encode(hwid,  "UTF-8");
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestMethod("GET");
            int status = conn.getResponseCode();
            if (status != 200) {
               conn.disconnect();
               this.sendChatMessage("§4Failed to fetch cloud config list.");
               return;
            }
            byte[] bytes = conn.getInputStream().readAllBytes();
            conn.disconnect();
            String json = new String(bytes, StandardCharsets.UTF_8);
            if (!json.contains("\"ok\":true")) {
               this.sendChatMessage("§4Server returned error.");
               return;
            }

            // Parse configs array
            java.util.List<CloudConfigEntry> configs = parseCloudConfigList(json);
            if (configs.isEmpty()) {
               this.sendChatMessage("§7No cloud configs available.");
               return;
            }

            this.sendChatMessage("§b§l=== Cloud Configs ===");
            for (CloudConfigEntry cfg : configs) {
               // Format: xin.cfg  上传日期 Test 备注:This is God Config
               String date = formatDate(cfg.createdAt);
               String remarkPart = cfg.remark.isEmpty() ? "" : " §7备注:§f" + cfg.remark;
               this.sendChatMessage(
                  "§a" + cfg.name + " §8[§e" + cfg.type + "§8] §7" + date + " §b" + cfg.uploader + remarkPart
               );
            }
            this.sendChatMessage("§7Use §b.load clone <type> <name> §7to load a config.");
         } catch (Exception e) {
            this.sendChatMessage("§4Error: " + e.getMessage());
         }
         return;
      }

      if (firstArg.equals("clone")) {
         if (parameters.length < 3) {
            this.sendChatMessage("§4Usage: .load clone <binds/module/all> <name>");
            return;
         }
         String cloudType = parameters[1].toLowerCase();
         String cloudName = parameters[2];

         if (!cloudType.equals("binds") && !cloudType.equals("module") && !cloudType.equals("all")) {
            this.sendChatMessage("§4Unknown type: §f" + cloudType + " §7(binds/module/all)");
            return;
         }

         this.sendChatMessage("§7Downloading §b" + cloudName + "§7...");

         String content = fetchCloudConfig(cloudType, cloudName);
         if (content == null) {
            this.sendChatMessage("§4Cloud config not found or download failed.");
            return;
         }

         File tmpFile = Manager.getFile("cfg" + File.separator + "cloud_tmp.cfg");
         try {
            tmpFile.getParentFile().mkdirs();
            try (PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(new FileOutputStream(tmpFile), StandardCharsets.UTF_8))) {
               pw.print(content);
            }

            File savedOptions = ConfigManager.options;
            try {
               ConfigManager.options = tmpFile;
               switch (cloudType) {
                  case "binds": {
                     ConfigManager tmp = new ConfigManager();
                     tmp.loadBinds();
                     break;
                  }
                  case "module": {
                     ConfigManager tmp = new ConfigManager();
                     tmp.loadModules();
                     Fonts.INSTANCE.refresh();
                     break;
                  }
                  case "all": {
                     Forever.CONFIG = new ConfigManager();
                     Forever.CONFIG.load();
                     Fonts.INSTANCE.refresh();
                     break;
                  }
               }
            } finally {
               ConfigManager.options = savedOptions;
            }

            Forever.save();
            this.sendChatMessage("§aCloud config loaded: §f" + cloudName);
         } catch (Exception e) {
            this.sendChatMessage("§4Apply failed: §f" + e.getMessage());
         } finally {
            tmpFile.delete();
         }
         return;
      }

      if (firstArg.equals("cloud")) {
         if (parameters.length < 3) {
            this.sendChatMessage("§4Usage: .load cloud <binds/module/all> <name>");
            return;
         }
         String cloudType = parameters[1].toLowerCase();
         String cloudName = parameters[2];

         if (!cloudType.equals("binds") && !cloudType.equals("module") && !cloudType.equals("all")) {
            this.sendChatMessage("§4Unknown type: §f" + cloudType + " §7(binds/module/all)");
            return;
         }

         this.sendChatMessage("§7Downloading §b" + cloudName + "§7...");

         String content = fetchCloudConfig(cloudType, cloudName);
         if (content == null) {
            this.sendChatMessage("§4Cloud config not found or download failed.");
            return;
         }

         File tmpFile = Manager.getFile("cfg" + File.separator + "cloud_tmp.cfg");
         try {
            tmpFile.getParentFile().mkdirs();
            try (PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(new FileOutputStream(tmpFile), StandardCharsets.UTF_8))) {
               pw.print(content);
            }

            File savedOptions = ConfigManager.options;
            try {
               ConfigManager.options = tmpFile;
               switch (cloudType) {
                  case "binds": {
                     ConfigManager tmp = new ConfigManager();
                     tmp.loadBinds();
                     break;
                  }
                  case "module": {
                     ConfigManager tmp = new ConfigManager();
                     tmp.loadModules();
                     Fonts.INSTANCE.refresh();
                     break;
                  }
                  case "all": {
                     Forever.CONFIG = new ConfigManager();
                     Forever.CONFIG.load();
                     Fonts.INSTANCE.refresh();
                     break;
                  }
               }
            } finally {
               ConfigManager.options = savedOptions;
            }

            Forever.save();
            this.sendChatMessage("§aCloud config loaded: §f" + cloudName);
         } catch (Exception e) {
            this.sendChatMessage("§4Apply failed: §f" + e.getMessage());
         } finally {
            tmpFile.delete();
         }

      } else if (firstArg.equals("binds")) {
         if (parameters.length < 2) {
            this.sendUsage();
            return;
         }
         String name = parameters[1];
         File targetFile = Manager.getFile("cfg" + File.separator + "Binds" + File.separator + name + ".cfg");

         if (!targetFile.exists()) {
            this.sendChatMessage("§4Bind config not found: §f" + name);
            return;
         }

         this.sendChatMessage("§fLoading bind config: §b" + name);

         File savedOptions = ConfigManager.options;
         try {
            ConfigManager.options = targetFile;
            ConfigManager tempConfig = new ConfigManager();
            tempConfig.loadBinds();
         } finally {
            ConfigManager.options = savedOptions;
         }

         Forever.save();
         this.sendChatMessage("§aBind config loaded: §f" + name);

      } else if (firstArg.equals("module")) {
         if (parameters.length < 2) {
            this.sendUsage();
            return;
         }
         String name = parameters[1];
         File targetFile = Manager.getFile("cfg" + File.separator + "Modules" + File.separator + name + ".cfg");

         if (!targetFile.exists()) {
            this.sendChatMessage("§4Module config not found: §f" + name);
            return;
         }

         this.sendChatMessage("§fLoading module config: §b" + name);

         File savedOptions = ConfigManager.options;
         try {
            ConfigManager.options = targetFile;
            ConfigManager tempConfig = new ConfigManager();
            tempConfig.loadModules();
         } finally {
            ConfigManager.options = savedOptions;
         }

         Forever.save();
         Fonts.INSTANCE.refresh();
         this.sendChatMessage("§aModule config loaded: §f" + name);

      } else if (firstArg.equals("all")) {
         if (parameters.length < 2) {
            this.sendUsage();
            return;
         }
         String name = parameters[1];
         File targetFile = Manager.getFile("cfg" + File.separator + "All" + File.separator + name + ".cfg");

         if (!targetFile.exists()) {
            this.sendChatMessage("§4Config not found: §f" + name);
            return;
         }

         this.sendChatMessage("§fLoading config: §b" + name);

         File savedOptions = ConfigManager.options;
         try {
            ConfigManager.options = targetFile;
            Forever.CONFIG = new ConfigManager();
            Forever.CONFIG.load();
         } finally {
            ConfigManager.options = savedOptions;
         }

         Forever.save();
         Fonts.INSTANCE.refresh();
         this.sendChatMessage("§aConfig loaded: §f" + name);

      } else {
         this.sendUsage();
      }
   }

   @Override
   public String[] getAutocorrect(int count, List<String> seperated) {
      if (count == 1) {
         return new String[]{"binds", "module", "all", "cloud", "list", "clone"};
      }

      if (count == 2) {
         if (seperated.size() < 2) {
            return null;
         }
         String sub = seperated.get(1).toLowerCase();
         switch (sub) {
            case "binds":  return listConfigs("Binds");
            case "module": return listConfigs("Modules");
            case "all":    return listConfigs("All");
            case "cloud":  return new String[]{"binds", "module", "all"};
            case "clone":  return new String[]{"binds", "module", "all"};
            default:       return null;
         }
      }

      if (count == 3 && seperated.size() >= 3) {
         String sub = seperated.get(1).toLowerCase();
         if (sub.equals("cloud") || sub.equals("clone")) {
            return fetchCloudNames(seperated.get(2).toLowerCase());
         }
      }

      return null;
   }

   private static class CloudConfigEntry {
      String id;
      String type;
      String name;
      String uploader;
      String remark;
      String createdAt;
   }

   private java.util.List<CloudConfigEntry> parseCloudConfigList(String json) {
      java.util.List<CloudConfigEntry> result = new java.util.ArrayList<>();
      try {
         int configsPos = json.indexOf("\"configs\"");
         if (configsPos < 0) return result;
         int arrayStart = json.indexOf('[', configsPos);
         if (arrayStart < 0) return result;
         int arrayEnd = json.indexOf(']', arrayStart);
         if (arrayEnd < 0) return result;

         String arrayContent = json.substring(arrayStart + 1, arrayEnd);
         int pos = 0;
         while (pos < arrayContent.length()) {
            int objStart = arrayContent.indexOf('{', pos);
            if (objStart < 0) break;
            int objEnd = arrayContent.indexOf('}', objStart);
            if (objEnd < 0) break;

            String objStr = arrayContent.substring(objStart, objEnd + 1);
            CloudConfigEntry entry = new CloudConfigEntry();
            entry.id = extractJsonString(objStr, "id");
            entry.type = extractJsonString(objStr, "type");
            entry.name = extractJsonString(objStr, "name");
            entry.uploader = extractJsonString(objStr, "uploader");
            entry.remark = extractJsonString(objStr, "remark");
            entry.createdAt = extractJsonString(objStr, "createdAt");

            if (!entry.name.isEmpty()) {
               result.add(entry);
            }

            pos = objEnd + 1;
         }
      } catch (Exception ignored) {
      }
      return result;
   }

   private String formatDate(String iso8601) {
      if (iso8601 == null || iso8601.isEmpty()) return "";
      try {
         // ISO format: 2026-06-22T12:34:56+00:00 → extract date part
         if (iso8601.length() >= 10) {
            return iso8601.substring(0, 10);
         }
      } catch (Exception ignored) {
      }
      return iso8601;
   }

   private String fetchCloudConfig(String type, String name) {
      try {
         String token = "token";
         String hwid  = "hwid";
         String url   = "https://52.175.50.86"
                      + "/api/index" + ".php?route=/cloud-config/"
                      + URLEncoder.encode(type, "UTF-8") + "/"
                      + URLEncoder.encode(name, "UTF-8")
                      + "&token=" + URLEncoder.encode(token, "UTF-8")
                      + "&hwid="  + URLEncoder.encode(hwid,  "UTF-8");
         HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
         conn.setConnectTimeout(8000);
         conn.setReadTimeout(8000);
         conn.setRequestMethod("GET");
         int status = conn.getResponseCode();
         if (status != 200) {
            conn.disconnect();
            return null;
         }
         byte[] bytes = conn.getInputStream().readAllBytes();
         conn.disconnect();
         String json = new String(bytes, StandardCharsets.UTF_8);
         if (!json.contains("\"ok\":true")) return null;
         return extractJsonString(json, "content");
      } catch (Exception e) {
         return null;
      }
   }

   private String[] fetchCloudNames(String type) {
      try {
         String token = "token";
         String hwid  = "hwid";
         String url   = "https://52.175.50.86"
                      + "/api/index" + ".php?route=/cloud-config"
                      + "&token=" + URLEncoder.encode(token, "UTF-8")
                      + "&hwid="  + URLEncoder.encode(hwid,  "UTF-8");
         HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
         conn.setConnectTimeout(5000);
         conn.setReadTimeout(5000);
         conn.setRequestMethod("GET");
         int status = conn.getResponseCode();
         if (status != 200) {
            conn.disconnect();
            return null;
         }
         byte[] bytes = conn.getInputStream().readAllBytes();
         conn.disconnect();
         String json = new String(bytes, StandardCharsets.UTF_8);
         List<String> names = new ArrayList<>();
         String typeTag = "\"type\":\"" + type + "\"";
         int pos = 0;
         while ((pos = json.indexOf(typeTag, pos)) >= 0) {
            int nameAt = json.indexOf("\"name\"", pos);
            if (nameAt >= 0 && nameAt - pos < 300) {
               String n = extractJsonString(json.substring(nameAt), "name");
               if (!n.isEmpty()) names.add(n);
            }
            pos++;
         }
         return names.isEmpty() ? null : names.toArray(new String[0]);
      } catch (Exception e) {
         return null;
      }
   }

   private static String extractJsonString(String json, String key) {
      String marker = "\"" + key + "\"";
      int idx = json.indexOf(marker);
      if (idx < 0) return "";
      int colon = json.indexOf(':', idx + marker.length());
      if (colon < 0) return "";
      int start = json.indexOf('"', colon + 1);
      if (start < 0) return "";
      StringBuilder sb = new StringBuilder();
      boolean esc = false;
      for (int i = start + 1; i < json.length(); i++) {
         char c = json.charAt(i);
         if (esc) {
            sb.append(c == 'n' ? '\n' : c == 'r' ? '\r' : c == 't' ? '\t' : c);
            esc = false;
         } else if (c == '\\') {
            esc = true;
         } else if (c == '"') {
            break;
         } else {
            sb.append(c);
         }
      }
      return sb.toString();
   }

   private String[] listConfigs(String subDir) {
      File folder;
      if (subDir.isEmpty()) {
         folder = Manager.getFile("cfg");
      } else {
         folder = Manager.getFile("cfg" + File.separator + subDir);
      }

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

   private String[] mergeArrays(String[]... arrays) {
      int totalLen = 0;
      for (String[] arr : arrays) {
         if (arr != null) totalLen += arr.length;
      }
      if (totalLen == 0) return null;

      String[] result = new String[totalLen];
      int idx = 0;
      for (String[] arr : arrays) {
         if (arr != null) {
            for (String s : arr) {
               result[idx++] = s;
            }
         }
      }
      return result;
   }
}
