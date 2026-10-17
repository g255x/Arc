package dev.baimaodaxia.core.impl;

import com.google.common.base.Splitter;
import dev.baimaodaxia.Alien;
import dev.baimaodaxia.core.Manager;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.impl.client.Fonts;
import dev.baimaodaxia.mod.modules.impl.client.HUD;
import dev.baimaodaxia.mod.modules.impl.client.NotificationsHud;
import dev.baimaodaxia.mod.modules.settings.Setting;
import dev.baimaodaxia.mod.modules.settings.impl.BindSetting;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.ColorSetting;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import dev.baimaodaxia.mod.modules.settings.impl.StringSetting;
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

   public ConfigManager() {
      this.loadFromFile();
   }

   public void load() {
      for(Module module : Alien.MODULE.getModules()) {
         for(Setting setting : module.getSettings()) {
            String var10000 = module.getName();
            String line = var10000 + "_" + setting.getName();
            Objects.requireNonNull(setting);
            if (setting instanceof BooleanSetting s) {
               s.setValueWithoutTask(Alien.CONFIG.getBoolean(line, s.getDefaultValue()));
            } else if (setting instanceof SliderSetting s) {
               s.setValue((double)Alien.CONFIG.getFloat(line, (float)s.getDefaultValue()));
            } else if (setting instanceof BindSetting s) {
               s.setValue(Alien.CONFIG.getInt(line, s.getDefaultValue()));
               s.setHoldEnable(Alien.CONFIG.getBoolean(line + "_hold"));
            } else if (setting instanceof EnumSetting s) {
               s.loadSetting(Alien.CONFIG.getString(line));
            } else if (setting instanceof ColorSetting s) {
               s.setValue(new Color(Alien.CONFIG.getInt(line, s.getDefaultValue().getRGB()), true));
               s.setSync(Alien.CONFIG.getBoolean(line + "Sync", s.getDefaultSync()));
               if (s.injectBoolean) {
                  s.booleanValue = Alien.CONFIG.getBoolean(line + "Boolean", s.getDefaultBooleanValue());
               }
            } else if (setting instanceof StringSetting s) {
               s.setValue(Alien.CONFIG.getString(line, s.getDefaultValue()));
            }
         }

         module.setState(Alien.CONFIG.getBoolean(module.getName() + "_state", module instanceof HUD || module instanceof NotificationsHud || module instanceof Fonts));
      }

   }

   public void writeToFile() {
      PrintWriter printwriter = null;

      try {
         printwriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(options), StandardCharsets.UTF_8));

         for(Module module : Alien.MODULE.getModules()) {
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
                  printwriter.println(line + "Sync:" + s.sync);
                  if (s.injectBoolean) {
                     printwriter.println(line + "Boolean:" + s.booleanValue);
                  }
               } else if (setting instanceof StringSetting s) {
                  printwriter.println(line + ":" + s.getValue());
               }
            }

            String var10001 = module.getName();
            printwriter.println(var10001 + "_state:" + module.isOn());
         }

         IOUtils.closeQuietly(printwriter);
      } catch (Exception exception) {
         exception.printStackTrace();
         System.out.println("[Vitality] Failed to save settings");
      } finally {
         IOUtils.closeQuietly(printwriter);
      }

   }

   public void loadFromFile() {
      Splitter COLON_SPLITTER = Splitter.on(':');

      try {
         if (!options.exists()) {
            return;
         }

         for(String s : IOUtils.readLines(new FileInputStream(options), StandardCharsets.UTF_8)) {
            try {
               Iterator iterator = COLON_SPLITTER.limit(2).split(s).iterator();
               this.settings.put((String)iterator.next(), (String)iterator.next());
            } catch (Exception var6) {
               System.out.println("Skipping bad option: " + s);
            }
         }
      } catch (Exception exception) {
         exception.printStackTrace();
         System.out.println("[Vitality] Failed to load settings");
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
}
