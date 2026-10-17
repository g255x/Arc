package dev.perish.core.impl;

import com.google.common.base.Splitter;
import dev.perish.Perish;
import dev.perish.core.Manager;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.impl.client.HUD;
import dev.perish.mod.modules.settings.Setting;
import dev.perish.mod.modules.settings.impl.*;
import org.apache.commons.io.IOUtils;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.regex.Pattern;

public class ConfigManager extends Manager {
    public static File options = getFile("options.txt");
    private final Hashtable<String, String> settings = new Hashtable();

    public ConfigManager() {
        this.read();
    }


    public void load() {
        for (Module module : Perish.MODULE.getModules()) {
            for (Setting setting : module.getSettings()) {
                String line = module.getName() + "_" + setting.getName();
                if (setting instanceof BooleanSetting s) {
                    s.setValueWithoutTask(Perish.CONFIG.getBoolean(line, s.getDefaultValue()));
                } else if (setting instanceof SliderSetting sx) {
                    sx.setValue(Perish.CONFIG.getFloat(line, (float) sx.getDefaultValue()));
                } else if (setting instanceof BindSetting sxx) {
                    sxx.setValue(Perish.CONFIG.getInt(line, sxx.getDefaultValue()));
                    sxx.setHoldEnable(Perish.CONFIG.getBoolean(line + "_hold"));
                } else if (setting instanceof EnumSetting<?> sxxx) {
                    sxxx.loadSetting(Perish.CONFIG.getString(line));
                } else if (setting instanceof ColorSetting sxxxx) {
                    sxxxx.setValue(new Color(Perish.CONFIG.getInt(line, sxxxx.getDefaultValue().getRGB()), true));
                    sxxxx.setSync(Perish.CONFIG.getBoolean(line + "Sync", sxxxx.getDefaultSync()));
                    sxxxx.pulse = Perish.CONFIG.getBoolean(line + "Pulse", false);
                    if (sxxxx.injectBoolean) {
                        sxxxx.booleanValue = Perish.CONFIG.getBoolean(line + "Boolean", sxxxx.getDefaultBooleanValue());
                    }
                } else if (setting instanceof StringSetting sxxxxx) {
                    sxxxxx.setValue(Perish.CONFIG.getString(line, sxxxxx.getDefaultValue()));
                }
            }

            module.setState(Perish.CONFIG.getBoolean(module.getName() + "_state", module instanceof HUD));
        }
    }

    public void save() {
        PrintWriter printwriter = null;

        try {
            printwriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(options), StandardCharsets.UTF_8));

            for (Module module : Perish.MODULE.getModules()) {
                for (Setting setting : module.getSettings()) {
                    String line = module.getName() + "_" + setting.getName();
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
                        printwriter.println(line + "Pulse:" + sxxxx.pulse);
                        if (sxxxx.injectBoolean) {
                            printwriter.println(line + "Boolean:" + sxxxx.booleanValue);
                        }
                    } else if (setting instanceof StringSetting sxxxxx) {
                        printwriter.println(line + ":" + sxxxxx.getValue());
                    }
                }

                printwriter.println(module.getName() + "_state:" + module.isOn());
            }
        } catch (Exception var18) {
            var18.printStackTrace();
            System.out.println("[Perish-Hyperion] Failed to save settings");
        } finally {
            IOUtils.closeQuietly(printwriter);
        }
    }


    public void read() {
        Splitter COLON_SPLITTER = Splitter.on(':');

        try {
            if (!options.exists()) {
                return;
            }

            for (String s : IOUtils.readLines(new FileInputStream(options), StandardCharsets.UTF_8)) {
                try {
                    Iterator<String> iterator = COLON_SPLITTER.limit(2).split(s).iterator();
                    this.settings.put(iterator.next(), iterator.next());
                } catch (Exception var6) {
                    System.out.println("Skipping bad option: " + s);
                }
            }
        } catch (Exception var7) {
            var7.printStackTrace();
            System.out.println("[Perish-Hyperion] Failed to load settings");
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
