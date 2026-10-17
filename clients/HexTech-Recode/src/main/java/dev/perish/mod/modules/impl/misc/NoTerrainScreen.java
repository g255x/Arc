package dev.perish.mod.modules.impl.misc;

import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.ClientTickEvent;
import dev.perish.mod.modules.Module;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.ProgressScreen;

public class NoTerrainScreen extends Module {
    public NoTerrainScreen() {
        super("NoTerrainScreen", Module.Category.Misc);
        this.setChinese("没有加载界面");
    }

    @EventListener
    public void onEvent(ClientTickEvent event) {
        if (!nullCheck()) {
            if (mc.currentScreen instanceof DownloadingTerrainScreen || mc.currentScreen instanceof ProgressScreen) {
                mc.currentScreen = null;
            }
        }
    }
}
