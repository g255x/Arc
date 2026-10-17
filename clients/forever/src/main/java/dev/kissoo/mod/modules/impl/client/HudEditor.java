package dev.kissoo.mod.modules.impl.client;

import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.TickEvent;
import dev.kissoo.mod.gui.ClickGuiScreen;
import dev.kissoo.mod.modules.Module;

public class HudEditor extends Module {
    public static HudEditor INSTANCE;

    public HudEditor() {
        super("HudEditor", "HudEditor", Category.Client);
        setChinese("界面编辑器");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player != null) {
            mc.execute(() -> mc.setScreen(ClickGuiScreen.getInstance()));
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.currentScreen instanceof ClickGuiScreen) {
            mc.execute(() -> mc.setScreen(null));
        }
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (!(mc.currentScreen instanceof ClickGuiScreen)) {
            disable();
        }
    }
}