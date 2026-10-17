package dev.G255.Lumi.mod.modules.impl.misc;

import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.ClientTickEvent;
import dev.G255.Lumi.mod.modules.Module;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.ProgressScreen;

public class NoTerrainScreen
extends Module {
    public NoTerrainScreen() {
        super("NoTerrainScreen", Module.Category.Misc);
        this.setChinese("\u6ca1\u6709\u52a0\u8f7d\u754c\u9762");
    }

    @EventListener
    public void onEvent(ClientTickEvent event) {
        if (NoTerrainScreen.nullCheck()) {
            return;
        }
        if (NoTerrainScreen.mc.currentScreen instanceof DownloadingTerrainScreen || NoTerrainScreen.mc.currentScreen instanceof ProgressScreen) {
            NoTerrainScreen.mc.currentScreen = null;
        }
    }
}

