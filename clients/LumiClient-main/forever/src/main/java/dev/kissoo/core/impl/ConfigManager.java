package dev.kissoo.core.impl;

import dev.kissoo.Forever;
import dev.kissoo.core.Manager;
import dev.kissoo.mod.modules.HudElement;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.Setting;
import dev.kissoo.mod.modules.settings.impl.BindSetting;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import dev.kissoo.mod.modules.settings.impl.StringSetting;
import org.apache.commons.io.IOUtils;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.regex.Pattern;

public class ConfigManager extends Manager {
   public static File options = getFile("options.txt");
   private final Hashtable<String, String> settings = new Hashtable<>();

   public ConfigManager() {
      this.read();
   }

   public void set(String key, String value) {
      this.settings.put(key, value);
   }

   public void set(String key, int value) {
      this.settings.put(key, String.valueOf(value));
   }

   public void set(String key, float value) {
      this.settings.put(key, String.valueOf(value));
   }

   public void set(String key, double value) {
      this.settings.put(key, String.valueOf(value));
   }

   public void set(String key, boolean value) {
      this.settings.put(key, String.valueOf(value));
   }

   public void load() {
      for (Module module : Forever.MODULE.getModules()) {
         try {
            for (Setting setting : module.getSettings()) {
               String line = module.getName() + "_" + setting.getName();
               loadSettingWithBinds(line, setting);
            }
         } catch (Throwable t) {
            System.err.println("[Forever/Config] Skipping broken settings for " + module.getName() + ": " + t.getMessage());
         }

         safeSetModuleState(module, this.getBoolean(module.getName() + "_state", module.getCategory() == Module.Category.HUD));
         safeLoadHud(module);
      }
   }

   private void forceSetAlignMode(HudElement hud, HudElement.AlignMode mode) {
      hud.setAlignMode(mode);
   }

   private void forceSetAnchorX(HudElement hud, int anchorValue) {
      try {
         if (hud.getAlignMode() == HudElement.AlignMode.RIGHT) {
            hud.setRightAnchorX(anchorValue);
         } else {
            hud.setElementX(anchorValue);
         }
      } catch (Exception e) {
         // 如果反射失败，回退到旧方式
         hud.setElementX(anchorValue);
      }
   }

   public void save() {
      PrintWriter printwriter = null;

      try {
         printwriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(options), StandardCharsets.UTF_8));

         for (Module module : Forever.MODULE.getModules()) {
            for (Setting setting : module.getSettings()) {
               String line = module.getName() + "_" + setting.getName();
               writeSetting(printwriter, line, setting);
            }

            printwriter.println(module.getName() + "_state:" + module.isOn());

            if (module instanceof HudElement hud) {
               int anchorValue = getInternalAnchorX(hud);
               printwriter.println(module.getName() + "_hudAnchor:" + anchorValue);
               printwriter.println(module.getName() + "_hudY:" + hud.getElementY());
               printwriter.println(module.getName() + "_hudAlign:" + hud.getAlignMode().name());
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
         System.out.println("[Alien] Failed to save settings");
      } finally {
         IOUtils.closeQuietly(printwriter);
      }
   }
   private int getInternalAnchorX(HudElement hud) {
      try {
         return hud.getAlignMode() == HudElement.AlignMode.RIGHT
                 ? hud.getRightAnchorX()
                 : hud.getElementX();
      } catch (Exception e) {
         return hud.getElementX();
      }
   }

   public void saveBinds() {
      saveToFile(options, true, false);
   }

   public void saveModules() {
      saveToFile(options, false, true);
   }

   public void loadBinds() {
      for (Module module : Forever.MODULE.getModules()) {
         for (Setting setting : module.getSettings()) {
            if (setting instanceof BindSetting sxx) {
               String line = module.getName() + "_" + setting.getName();
               sxx.setValue(this.getInt(line, sxx.getDefaultValue()));
               sxx.setHoldEnable(this.getBoolean(line + "_hold"));
            }
         }
      }
   }

   public void loadModules() {
      for (Module module : Forever.MODULE.getModules()) {
         try {
            for (Setting setting : module.getSettings()) {
               if (setting instanceof BindSetting) {
                  continue;
               }

               String line = module.getName() + "_" + setting.getName();
               loadSingleSetting(line, setting);
            }
         } catch (Throwable t) {
            System.err.println("[Forever/Config] Skipping broken module settings for " + module.getName() + ": " + t.getMessage());
         }

         safeSetModuleState(module, this.getBoolean(module.getName() + "_state", module.getCategory() == Module.Category.HUD));
         safeLoadHud(module);
      }
   }

   private void saveToFile(File targetFile, boolean bindsOnly, boolean modulesOnly) {
      PrintWriter printwriter = null;

      try {
         printwriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(targetFile), StandardCharsets.UTF_8));

         for (Module module : Forever.MODULE.getModules()) {
            for (Setting setting : module.getSettings()) {
               String line = module.getName() + "_" + setting.getName();

               if (bindsOnly) {
                  if (setting instanceof BindSetting sxx) {
                     printwriter.println(line + ":" + sxx.getValue());
                     printwriter.println(line + "_hold:" + sxx.isHoldEnable());
                  }
               } else if (modulesOnly) {
                  if (!(setting instanceof BindSetting)) {
                     writeSetting(printwriter, line, setting);
                  }
               }
            }

            if (modulesOnly) {
               printwriter.println(module.getName() + "_state:" + module.isOn());

               if (module instanceof HudElement hud) {
                  int anchorValue = getInternalAnchorX(hud);
                  printwriter.println(module.getName() + "_hudAnchor:" + anchorValue);
                  printwriter.println(module.getName() + "_hudY:" + hud.getElementY());
                  printwriter.println(module.getName() + "_hudAlign:" + hud.getAlignMode().name());
               }
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
         System.out.println("[Alien] Failed to save settings");
      } finally {
         IOUtils.closeQuietly(printwriter);
      }
   }

   private void writeSetting(PrintWriter printwriter, String line, Setting setting) {
      if (setting instanceof BooleanSetting s) {
         printwriter.println(line + ":" + s.getValue());
      } else if (setting instanceof SliderSetting sx) {
         printwriter.println(line + ":" + sx.getValue());
      } else if (setting instanceof BindSetting sxx) {
         printwriter.println(line + ":" + sxx.getValue());
         printwriter.println(line + "_hold:" + sxx.isHoldEnable());
      } else if (setting instanceof EnumSetting<?> sxxx) {
         printwriter.println(line + ":" + sxxx.getValue().name());
      } else if (setting instanceof ColorSetting sxxxx) {
         printwriter.println(line + ":" + sxxxx.getValue().getRGB());
         printwriter.println(line + "Sync:" + sxxxx.sync);
         if (sxxxx.injectBoolean) {
            printwriter.println(line + "Boolean:" + sxxxx.booleanValue);
         }
      } else if (setting instanceof StringSetting sxxxxx) {
         printwriter.println(line + ":" + sxxxxx.getValue());
      }
   }

   private void loadSingleSetting(String line, Setting setting) {
      if (setting instanceof BooleanSetting s) {
         s.setValueWithoutTask(this.getBoolean(line, s.getDefaultValue()));
      } else if (setting instanceof SliderSetting sx) {
         sx.setValueUnclamped(this.getFloat(line, (float) sx.getDefaultValue()));
      } else if (setting instanceof EnumSetting<?> sxxx) {
         sxxx.loadSetting(this.getString(line));
      } else if (setting instanceof ColorSetting sxxxx) {
         sxxxx.setValue(new Color(this.getInt(line, sxxxx.getDefaultValue().getRGB()), true));
         sxxxx.setSync(this.getBoolean(line + "Sync", sxxxx.getDefaultSync()));
         if (sxxxx.injectBoolean) {
            sxxxx.booleanValue = this.getBoolean(line + "Boolean", sxxxx.getDefaultBooleanValue());
         }
      } else if (setting instanceof StringSetting sxxxxx) {
         sxxxxx.setValue(this.getString(line, sxxxxx.getDefaultValue()));
      }
   }

   private void loadSettingWithBinds(String line, Setting setting) {
      if (setting instanceof BindSetting sxx) {
         sxx.setValue(this.getInt(line, sxx.getDefaultValue()));
         sxx.setHoldEnable(this.getBoolean(line + "_hold"));
      } else {
         loadSingleSetting(line, setting);
      }
   }

   private void safeSetModuleState(Module module, boolean state) {
      try {
         module.setState(state);
      } catch (Throwable t) {
         System.err.println("[Forever/Config] Failed to apply state for " + module.getName() + ": " + t.getMessage());
         try {
            if (module.isOn()) {
               module.disable();
            }
         } catch (Throwable ignored) {
         }
      }
   }

   private void safeLoadHud(Module module) {
      if (!(module instanceof HudElement hud)) {
         return;
      }
      try {
         String keyAlign = module.getName() + "_hudAlign";
         String keyAnchor = module.getName() + "_hudAnchor";
         String keyY = module.getName() + "_hudY";
         String alignStr = this.getString(keyAlign);
         if (alignStr != null) {
            try {
               forceSetAlignMode(hud, HudElement.AlignMode.valueOf(alignStr));
            } catch (IllegalArgumentException ignored) {
            }
         }
         if (this.getString(keyAnchor) != null) {
            forceSetAnchorX(hud, this.getInt(keyAnchor, 0));
         }
         if (this.getString(keyY) != null) {
            hud.setElementY(this.getInt(keyY, 0));
         }
      } catch (Throwable t) {
         System.err.println("[Forever/Config] Skipping broken HUD placement for " + module.getName() + ": " + t.getMessage());
      }
   }

   public void read() {
      com.google.common.base.Splitter COLON_SPLITTER = com.google.common.base.Splitter.on(':');

      try {
         if (!options.exists()) {
            return;
         }

         for (String s : IOUtils.readLines(new FileInputStream(options), StandardCharsets.UTF_8)) {
            try {
               Iterator<String> iterator = COLON_SPLITTER.limit(2).split(s).iterator();
               this.settings.put(iterator.next(), iterator.next());
            } catch (Exception e) {
               System.out.println("Skipping bad option: " + s);
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
         System.out.println("[Lyrae] Failed to load settings");
      }
   }

   public int getInt(String setting, int defaultValue) {
      String s = this.settings.get(setting);
      return s != null && this.isInteger(s) ? Integer.parseInt(s) : defaultValue;
   }

   public float getFloat(String setting, float defaultValue) {
      String s = this.settings.get(setting);
      return s != null && this.isFloat(s) ? Float.parseFloat(s) : defaultValue;
   }

   public boolean getBoolean(String setting) {
      String s = this.settings.get(setting);
      return Boolean.parseBoolean(s);
   }

   public boolean getBoolean(String setting, boolean defaultValue) {
      if (this.settings.get(setting) != null) {
         String s = this.settings.get(setting);
         return Boolean.parseBoolean(s);
      } else {
         return defaultValue;
      }
   }

   public String getString(String setting) {
      return this.settings.get(setting);
   }

   public String getString(String setting, String defaultValue) {
      return this.settings.get(setting) == null ? defaultValue : this.settings.get(setting);
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
