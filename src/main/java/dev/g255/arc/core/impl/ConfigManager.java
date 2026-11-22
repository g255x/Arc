package dev.g255.arc.core.impl;

import com.google.common.base.Splitter;
import dev.g255.arc.Arc;
import dev.g255.arc.core.Manager;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.impl.client.HUD;
import dev.g255.arc.mod.modules.settings.Setting;
import dev.g255.arc.mod.modules.settings.impl.BindSetting;
import dev.g255.arc.mod.modules.settings.impl.BlockListSetting;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.ColorSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import dev.g255.arc.mod.modules.settings.impl.StringSetting;
import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Objects;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;

public class ConfigManager extends Manager {
   public static File options = getFile("options.txt");
   private final Hashtable<String, String> settings = new Hashtable();

   public void load() {
      this.settings.clear();
      Splitter COLON_SPLITTER = Splitter.on(':');

      try {
         if (options.exists()) {
            for(String s : IOUtils.readLines(new FileInputStream(options), StandardCharsets.UTF_8)) {
               try {
                  Iterator iterator = COLON_SPLITTER.limit(2).split(s).iterator();
                  this.settings.put((String)iterator.next(), (String)iterator.next());
               } catch (Exception var9) {
                  System.out.println("Skipping bad option: " + s);
               }
            }
         }
      } catch (Exception exception) {
         exception.printStackTrace();
         System.out.println("[Arc] by G255 - Failed to load settings");
      }

      for(Module module : Arc.MODULE.getModules()) {
         for(Setting setting : module.getSettings()) {
            String var10000 = module.getName();
            String line = var10000 + "_" + setting.getName();
            Objects.requireNonNull(setting);
            if (setting instanceof BooleanSetting s) {
               s.setValueWithoutTask(Arc.CONFIG.getBoolean(line, s.getDefaultValue()));
            } else if (setting instanceof SliderSetting s) {
               s.setValue((double)Arc.CONFIG.getFloat(line, (float)s.getDefaultValue()));
            } else if (setting instanceof BindSetting s) {
               s.setValue(Arc.CONFIG.getInt(line, s.getDefaultValue()));
               s.setHoldEnable(Arc.CONFIG.getBoolean(line + "_hold"));
            } else if (setting instanceof EnumSetting s) {
               s.loadSetting(Arc.CONFIG.getString(line));
            } else if (setting instanceof ColorSetting s) {
               s.setValue(new Color(Arc.CONFIG.getInt(line, s.getDefaultValue().getRGB()), true));
               boolean rainbow;
               if (Arc.CONFIG.getString(line + "Rainbow") != null) {
                  rainbow = Arc.CONFIG.getBoolean(line + "Rainbow", s.getDefaultRainbow());
               } else {
                  rainbow = Arc.CONFIG.getBoolean(line + "Sync", s.getDefaultRainbow());
               }

               s.setRainbow(rainbow);
               if (s.injectBoolean) {
                  s.booleanValue = Arc.CONFIG.getBoolean(line + "Boolean", s.getDefaultBooleanValue());
               }
            } else if (setting instanceof StringSetting s) {
               s.setValue(Arc.CONFIG.getString(line, s.getDefaultValue()));
            } else if (setting instanceof BlockListSetting s) {
               s.load(Arc.CONFIG.getString(line));
            }
         }

         module.setState(Arc.CONFIG.getBoolean(module.getName() + "_state", module instanceof HUD));
      }

      System.out.println("[Arc] by G255 - Settings Loaded");
   }

   public void read() {
      this.load();
   }

   public void save() {
      PrintWriter printwriter = null;

      try {
         printwriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(options), StandardCharsets.UTF_8));

         for(Module module : Arc.MODULE.getModules()) {
            for(Setting setting : module.getSettings()) {
               String var10000 = module.getName();
               String line = var10000 + "_" + setting.getName();
               Objects.requireNonNull(setting);
               if (setting instanceof BooleanSetting s) {
                  printwriter.println(line + ":" + s.getValue());
               } else if (setting instanceof SliderSetting s) {
                  printwriter.println(line + ":" + s.getValue());
               } else if (setting instanceof BindSetting s) {
                  printwriter.println(line + ":" + s.getValue());
                  printwriter.println(line + "_hold:" + s.isHoldEnable());
               } else if (setting instanceof EnumSetting s) {
                  printwriter.println(line + ":" + s.getValue().name());
               } else if (setting instanceof ColorSetting s) {
                  printwriter.println(line + ":" + s.getValue().getRGB());
                  printwriter.println(line + "Rainbow:" + s.rainbow);
                  if (s.injectBoolean) {
                     printwriter.println(line + "Boolean:" + s.booleanValue);
                  }
               } else if (setting instanceof StringSetting s) {
                  printwriter.println(line + ":" + s.getValue());
               } else if (setting instanceof BlockListSetting s) {
                  printwriter.println(line + ":" + s.serialize());
               }
            }

            String var10001 = module.getName();
            printwriter.println(var10001 + "_state:" + module.isOn());
         }

         IOUtils.closeQuietly(printwriter);
      } catch (Exception exception) {
         exception.printStackTrace();
         System.out.println("[Arc] by G255 - Failed to save settings");
      } finally {
         IOUtils.closeQuietly(printwriter);
      }

   }

   public int getInt(String setting, int defaultValue) {
      String s = (String)this.settings.get(setting);
      return s != null && this.isInteger(s) ? Integer.parseInt(s) : defaultValue;
   }

   public float getFloat(String setting, float defaultValue) {
      String s = (String)this.settings.get(setting);
      return s != null && this.isFloat(s) ? Float.parseFloat(s) : defaultValue;
   }

   public boolean getBoolean(String setting) {
      String s = (String)this.settings.get(setting);
      return Boolean.parseBoolean(s);
   }

   public boolean getBoolean(String setting, boolean defaultValue) {
      if (this.settings.get(setting) != null) {
         String s = (String)this.settings.get(setting);
         return Boolean.parseBoolean(s);
      } else {
         return defaultValue;
      }
   }

   public String getString(String setting) {
      return (String)this.settings.get(setting);
   }

   public String getString(String setting, String defaultValue) {
      return this.settings.get(setting) == null ? defaultValue : (String)this.settings.get(setting);
   }

   public boolean isInteger(String str) {
      Pattern pattern = Pattern.compile("^[-+]?[\\d]*$");
      return pattern.matcher(str).matches();
   }

   public boolean isFloat(String str) {
      String pattern = "^[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?$";
      return str.matches(pattern);
   }

   // ===== 多配置文件（cfg）支持：配置页要用 =====

   public static File getCfgFolder() {
      File folder = getFile("cfg");
      if (!folder.exists()) {
         folder.mkdirs();
      }
      return folder;
   }

   public static File getCfgFile(String name) {
      if (name == null || name.isEmpty()) {
         return null;
      }
      return new File(getCfgFolder(), name + ".cfg");
   }

   public static java.util.ArrayList<String> listCfgNames() {
      java.util.ArrayList<String> out = new java.util.ArrayList();
      File[] files = getCfgFolder().listFiles();
      if (files == null) {
         return out;
      }
      for(File f : files) {
         if (f == null || !f.isFile()) {
            continue;
         }
         String n = f.getName();
         if (n == null || !n.toLowerCase().endsWith(".cfg")) {
            continue;
         }
         String base = n.substring(0, n.length() - 4);
         if (!base.isEmpty()) {
            out.add(base);
         }
      }
      out.sort(String::compareToIgnoreCase);
      return out;
   }

   public static String sanitizeCfgName(String name) {
      if (name == null) {
         return "";
      }
      String s = name.trim();
      if (s.isEmpty()) {
         return "";
      }
      StringBuilder out = new StringBuilder();
      for(int i = 0; i < s.length(); ++i) {
         char c = s.charAt(i);
         out.append(Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == ' ' ? c : '_');
      }
      String r = out.toString().trim();
      return r.length() > 32 ? r.substring(0, 32) : r;
   }

   public static String uniqueCfgName(String base) {
      String n = sanitizeCfgName(base);
      if (n.isEmpty()) {
         return "";
      }
      File f = getCfgFile(n);
      if (f != null && !f.exists()) {
         return n;
      }
      for(int i = 1; i < 1000; ++i) {
         String nn = n + "_" + i;
         File ff = getCfgFile(nn);
         if (ff != null && !ff.exists()) {
            return nn;
         }
      }
      return n;
   }

   /** 新建一份「全默认」的配置：空文件即可，读取时所有键都会回落到默认值 */
   public static String createDefaultCfg(String nameInput) {
      String base = sanitizeCfgName(nameInput);
      if (base.isEmpty()) {
         return null;
      }
      String name = uniqueCfgName(base);
      File file = getCfgFile(name);
      if (file == null) {
         return null;
      }
      try {
         if (!file.exists()) {
            file.createNewFile();
         }
         return name;
      } catch (Exception e) {
         e.printStackTrace();
         return null;
      }
   }

   public static String backupCfg(String fromName, String toNameInput) {
      if (fromName == null || fromName.isEmpty()) {
         return null;
      }
      File src = getCfgFile(fromName);
      if (src == null || !src.exists()) {
         return null;
      }
      String baseName = sanitizeCfgName(toNameInput);
      if (baseName.isEmpty()) {
         baseName = sanitizeCfgName(fromName);
      }
      if (baseName.isEmpty()) {
         return null;
      }
      java.time.LocalDate d = java.time.LocalDate.now();
      String prefix = baseName + "_" + d.getYear() + "_" + d.getMonthValue() + "_" + d.getDayOfMonth();
      String name = null;
      for(int i = 1; i < 100000; ++i) {
         String candidate = prefix + "_[" + i + "]";
         File f = getCfgFile(candidate);
         if (f != null && !f.exists()) {
            name = candidate;
            break;
         }
      }
      if (name == null) {
         return null;
      }
      File dst = getCfgFile(name);
      try {
         java.nio.file.Files.copy(src.toPath(), dst.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
         return name;
      } catch (Exception e) {
         e.printStackTrace();
         return null;
      }
   }

   public static void deleteCfg(String name) {
      File f = getCfgFile(name);
      if (f != null && f.exists()) {
         try {
            f.delete();
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }

   /** 把当前状态写进指定 cfg，然后回到 options.txt */
   public static String saveCfg(String nameInput) {
      String n = sanitizeCfgName(nameInput);
      if (n.isEmpty()) {
         return null;
      }
      File prev = options;
      try {
         options = getCfgFile(n);
         Arc.save();
         return n;
      } catch (Exception e) {
         e.printStackTrace();
         return null;
      } finally {
         options = prev;
      }
   }

   /** 从指定 cfg 读入状态，再写回 options.txt 固化成当前配置 */
   public static boolean loadCfg(String name) {
      if (name == null || name.isEmpty()) {
         return false;
      }
      File file = getCfgFile(name);
      if (file == null || !file.exists()) {
         return false;
      }
      File prev = options;
      try {
         options = file;
         Arc.CONFIG.load();
         return true;
      } catch (Exception e) {
         e.printStackTrace();
         return false;
      } finally {
         options = prev;
         try {
            Arc.save();
         } catch (Exception ignored) {
         }
      }
   }
}
