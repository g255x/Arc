package dev.perish.mod.modules.impl.client;

import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.UpdateEvent;
import dev.perish.mod.gui.hud.HudEditorManager;
import dev.perish.mod.gui.hud.HudEditorScreen;
import dev.perish.mod.modules.Module;

public class HudEditor extends Module {
    public static HudEditor INSTANCE;

    public HudEditor() {
        super("HudEditor", Module.Category.Client);
        this.setChinese("HUD编辑器");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        if (nullCheck()) {
            disable();
            return;
        }
        HudEditorManager.INSTANCE.setEditMode(true);
        mc.setScreen(HudEditorScreen.getInstance());
    }

    @Override
    public void onDisable() {
        HudEditorManager.INSTANCE.setEditMode(false);
        if (mc.currentScreen instanceof HudEditorScreen) {
            mc.currentScreen.close();
        }
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (!(mc.currentScreen instanceof HudEditorScreen)) {
            disable();
        }
    }
}
