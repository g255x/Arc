/*
 * Decompiled with CFR 0.152.
 */
package dev.g255.arc.mod.modules.impl.combat;

import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.StringSetting;
import java.util.ArrayList;
import java.util.List;

public class Panic
extends Module {
    private final BooleanSetting restoreOnDisable;
    private final StringSetting whitelist;
    private final List<Module> disabledModules = new ArrayList<Module>();

    public Panic() {
        super("Panic", "\u7d27\u6025\u505c\u6b62\u6240\u6709\u529f\u80fd", Module.Category.Combat);
        this.setChinese("\u7d27\u6025\u505c\u6b62\u6240\u6709\u529f\u80fd");
        this.restoreOnDisable = this.add(new BooleanSetting("\u6062\u590d\u542f\u7528", true));
        this.whitelist = this.add(new StringSetting("\u767d\u540d\u5355", ""));
    }

    @Override
    public void onEnable() {
        this.disabledModules.clear();
        List<Module> allModules = this.getAllModules();
        String[] whitelistNames = this.whitelist.getValue().split(",");
        ArrayList<String> whitelistModules = new ArrayList<String>();
        for (String name : whitelistNames) {
            whitelistModules.add(name.trim().toLowerCase());
        }
        for (Module module : allModules) {
            if (module == this || this.shouldWhitelist(module, whitelistModules) || !module.isOn()) continue;
            this.disabledModules.add(module);
            module.disable();
        }
        this.sendMessage("\u00a7c\u5df2\u7d27\u6025\u505c\u6b62 " + this.disabledModules.size() + " \u4e2a\u529f\u80fd");
    }

    @Override
    public void onDisable() {
        if (this.restoreOnDisable.getValue() && !this.disabledModules.isEmpty()) {
            int restoredCount = 0;
            for (Module module : this.disabledModules) {
                if (!module.isOff()) continue;
                module.enable();
                ++restoredCount;
            }
            this.sendMessage("\u00a7a\u5df2\u6062\u590d " + restoredCount + " \u4e2a\u529f\u80fd");
        }
        this.disabledModules.clear();
    }

    @Override
    public String getInfo() {
        return this.disabledModules.isEmpty() ? "" : "" + this.disabledModules.size();
    }

    private List<Module> getAllModules() {
        // 旧实现用 "dev.luminous.mod..." 反射拿 INSTANCE，包名重构后一个都找不到，
        // 导致"紧急停止"其实什么都没关。这里直接读 ModuleManager 注册表。
        return new ArrayList<Module>(dev.g255.arc.Arc.MODULE.getModules());
    }

    private boolean shouldWhitelist(Module module, List<String> whitelistModules) {
        if (module == null) {
            return false;
        }
        String moduleName = module.getName().toLowerCase();
        for (String whitelistName : whitelistModules) {
            if (!moduleName.equals(whitelistName.toLowerCase())) continue;
            return true;
        }
        return false;
    }

    public void panicNow() {
        if (!this.isOn()) {
            this.enable();
        }
    }

    public void restoreNow() {
        if (this.isOn()) {
            this.disable();
        }
    }
}

