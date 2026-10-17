/*
 * Decompiled with CFR 0.152.
 */
package dev.G255.Lumi.mod.modules.impl.misc;

import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.DeathEvent;
import dev.G255.Lumi.api.events.impl.UpdateEvent;
import dev.G255.Lumi.api.utils.math.Timer;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.settings.impl.StringSetting;

public class AutoKit
        extends Module {
    public static AutoKit INSTANCE;
    final StringSetting command = this.add(new StringSetting("Command", "kit 1"));
    boolean kit = false;
    final Timer timer = new Timer();

    public AutoKit() {
        super("AutoKit", Module.Category.Misc);
        this.setChinese("\u81ea\u52a8\u914d\u88c5\u547d\u4ee4");
        INSTANCE = this;
    }

    @Override
    public void onLogin() {
        this.kit = true;
        this.timer.reset();
    }

    @EventListener
    public void onDeath(DeathEvent event) {
        if (event.getPlayer() == AutoKit.mc.player) {
            this.kit = true;
            this.timer.reset();
        }
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (this.kit && this.timer.passedS(2.0)) {
            this.kit = false;
            AutoKit.mc.player.networkHandler.sendCommand(this.command.getValue());
        }
    }
}