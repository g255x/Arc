package dev.perish.mod.modules.impl.client;

import dev.perish.Perish;
import dev.perish.core.Manager;
import dev.perish.core.impl.ConfigManager;
import dev.perish.mod.gui.config.ConfigSwitcherScreen;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.impl.BindSetting;
import dev.perish.mod.modules.settings.impl.StringSetting;
import net.minecraft.util.Formatting;
import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.ClientTickEvent;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ConfigSwitcher extends Module {
    public static ConfigSwitcher INSTANCE;

    public final StringSetting preset1Name = this.add(new StringSetting("Preset1Name", "Default", () -> false));
    public final StringSetting preset2Name = this.add(new StringSetting("Preset2Name", "Preset2", () -> false));
    public final BindSetting switchKey = this.add(new BindSetting("SwitchKey", -1, () -> false));
    public static final File CURRENT_PRESET_FILE = Manager.getFile("current.cfg");
    public static final File PRESET2_FILE_PATH = Manager.getFile("preset2.cfg");

    public String preset2File = "";

    public int currentPreset = 1;
    private boolean switchHolding = false;
    private boolean loading = false;

    public ConfigSwitcher() {
        super("ConfigSwitcher", Module.Category.Client);
        this.setChinese("参数切换");
        INSTANCE = this;
        Perish.EVENT_BUS.subscribe(ConfigSwitcher.class);
    }

    @Override
    public void onEnable() {
        this.currentPreset = readCurrentPreset();
        this.preset2File = readPreset2File();
        pointOptionsToCurrent();
        if (loading) return;
        if (mc.currentScreen == null) {
            mc.setScreen(new ConfigSwitcherScreen());
        }
    }

    @Override
    public void onDisable() {
        if (loading) return;
        if (mc.currentScreen instanceof ConfigSwitcherScreen) {
            mc.currentScreen.close();
        }
    }

    private void pointOptionsToCurrent() {
        String targetFile = this.currentPreset == 1
                ? "options.txt"
                : "cfg" + File.separator + this.preset2File + ".cfg";
        ConfigManager.options = Manager.getFile(targetFile);
    }

    public static int readCurrentPreset() {
        try {
            if (!CURRENT_PRESET_FILE.exists()) {
                writeCurrentPreset(1);
                return 1;
            }
            String content = Files.readString(CURRENT_PRESET_FILE.toPath(), StandardCharsets.UTF_8).trim();
            int idx = Integer.parseInt(content);
            return (idx == 1 || idx == 2) ? idx : 1;
        } catch (Exception e) {
            return 1;
        }
    }

    public static void writeCurrentPreset(int idx) {
        try {
            File parent = CURRENT_PRESET_FILE.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            Files.writeString(CURRENT_PRESET_FILE.toPath(), String.valueOf(idx), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String readPreset2File() {
        try {
            if (!PRESET2_FILE_PATH.exists()) return "";
            return Files.readString(PRESET2_FILE_PATH.toPath(), StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            return "";
        }
    }

    public static void writePreset2File(String name) {
        try {
            File parent = PRESET2_FILE_PATH.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            Files.writeString(PRESET2_FILE_PATH.toPath(), name, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void switchPreset() {
        if (this.preset2File.isEmpty()) {
            this.sendMessage(Formatting.RED + "预设2未选文件");
            return;
        }
        int fromDisk = readCurrentPreset();
        int next = (fromDisk == 1) ? 2 : 1;
        applyPreset(next);
    }

    public void applyPreset(int presetIndex) {
        if (presetIndex == 2 && this.preset2File.isEmpty()) {
            this.sendMessage(Formatting.RED + "预设2未选文件");
            return;
        }
        writeCurrentPreset(presetIndex);
        this.currentPreset = presetIndex;
        pointOptionsToCurrent();

        File target = presetIndex == 1
                ? Manager.getFile("options.txt")
                : Manager.getFile("cfg" + File.separator + this.preset2File + ".cfg");

        if (!target.exists()) {
            this.sendMessage(Formatting.YELLOW + "⚠ " + target.getName() + " 不存在,内存保留");
            return;
        }

        boolean cpsState = this.isOn();
        loading = true;
        try {
            Perish.CONFIG = new ConfigManager();
            Perish.CONFIG.load();
            this.setState(cpsState);
            String displayName = presetIndex == 1 ? this.preset1Name.getValue() : this.preset2Name.getValue();
            this.sendMessage(Formatting.GREEN + "✓ 已激活: " + displayName);
        } finally {
            loading = false;
        }
    }

    public void loadPresetFile(int presetIndex) {
        if (presetIndex == 2 && this.preset2File.isEmpty()) {
            this.sendMessage(Formatting.RED + "预设2未选文件");
            return;
        }
        File target = presetIndex == 1
                ? Manager.getFile("options.txt")
                : Manager.getFile("cfg" + File.separator + this.preset2File + ".cfg");
        if (!target.exists()) {
            this.sendMessage(Formatting.RED + "✗ 文件不存在: " + target.getName());
            return;
        }
        writeCurrentPreset(presetIndex);
        this.currentPreset = presetIndex;
        pointOptionsToCurrent();
        loading = true;
        try {
            Perish.CONFIG = new ConfigManager();
            Perish.CONFIG.load();
            this.sendMessage(Formatting.GREEN + "✓ 已加载: " + target.getName());
        } finally {
            loading = false;
        }
    }

    public void syncCurrentToDefault() {
        File defaultFile = Manager.getFile("options.txt");
        File previous = ConfigManager.options;
        try {
            ConfigManager.options = defaultFile;
            Perish.save();
            this.sendMessage(Formatting.GREEN + "✓ 已覆盖 options.txt");
        } finally {
            ConfigManager.options = previous;
        }
    }

    public List<String> listPresetFiles() {
        List<String> list = new ArrayList<>();
        File cfgFolder = Manager.getFile("cfg");
        if (!cfgFolder.exists()) cfgFolder.mkdirs();
        if (!cfgFolder.isDirectory()) return list;
        File[] files = cfgFolder.listFiles((dir, name) -> name.endsWith(".cfg") && !name.equals("options.cfg"));
        if (files == null) return list;
        for (File f : files) {
            String n = f.getName();
            if (n.endsWith(".cfg")) list.add(n.substring(0, n.length() - 4));
        }
        list.sort(String::compareTo);
        return list;
    }

    public boolean saveCurrentAs(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) return false;
        String name = fileName.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        if (name.isEmpty()) return false;
        File cfgFolder = Manager.getFile("cfg");
        if (!cfgFolder.exists()) cfgFolder.mkdirs();
        File target = Manager.getFile("cfg" + File.separator + name + ".cfg");
        boolean existed = target.exists();
        try {
            target.createNewFile();
        } catch (Exception ignored) {}
        File previous = ConfigManager.options;
        try {
            ConfigManager.options = target;
            Perish.save();
            this.sendMessage(Formatting.GREEN + "✓ 已保存: " + name + ".cfg");
        } finally {
            ConfigManager.options = previous;
        }
        return true;
    }

    public void deletePreset(String fileName) {
        if (fileName == null || fileName.isEmpty()) return;
        File target = Manager.getFile("cfg" + File.separator + fileName + ".cfg");
        if (target.exists() && target.delete()) {
            if (this.preset2File.equals(fileName)) {
                this.preset2File = "";
                writePreset2File("");
                writeCurrentPreset(1);
                this.currentPreset = 1;
            }
            this.sendMessage(Formatting.GREEN + "✓ 已删除: " + fileName + ".cfg");
        }
    }

    public void setPreset2(String fileName) {
        if (readCurrentPreset() == 2) {
            this.sendMessage(Formatting.RED + "✗ 预设2激活中,无法修改");
            return;
        }
        this.preset2File = fileName;
        writePreset2File(fileName);
        if (readCurrentPreset() == 2) {
            this.currentPreset = 2;
            pointOptionsToCurrent();
        }
        this.sendMessage(Formatting.GREEN + "✓ 预设2 → " + fileName);
    }

    public String getCurrentPresetName() {
        this.currentPreset = readCurrentPreset();
        return this.currentPreset == 1 ? this.preset1Name.getValue() : this.preset2Name.getValue();
    }

    public String getCurrentPresetTarget() {
        this.currentPreset = readCurrentPreset();
        return this.currentPreset == 1 ? "options.txt" : "cfg/" + this.preset2File + ".cfg";
    }

    @EventListener
    public static void onTickStatic(ClientTickEvent event) {
        if (event.isPost()) return;
        ConfigSwitcher mod = INSTANCE;
        if (mod == null) return;
        if (mod.loading) return;
        if (mod.switchKey.getValue() == -1) return;
        boolean pressed = mod.switchKey.isPressed();
        if (pressed && !mod.switchHolding) {
            mod.switchPreset();
        }
        mod.switchHolding = pressed;
    }
}
