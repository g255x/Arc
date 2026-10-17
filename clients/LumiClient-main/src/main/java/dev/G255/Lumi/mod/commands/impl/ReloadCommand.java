/*
 * Decompiled with CFR 0.152.
 */
package dev.G255.Lumi.mod.commands.impl;

import dev.G255.Lumi.Lumi;
import dev.G255.Lumi.core.impl.ConfigManager;
import dev.G255.Lumi.mod.commands.Command;
import java.util.List;

public class ReloadCommand
extends Command {
    public ReloadCommand() {
        super("reload", "");
    }

    @Override
    public void runCommand(String[] parameters) {
        this.sendChatMessage("\u00a7fReloading..");
        Lumi.CONFIG = new ConfigManager();
        Lumi.CONFIG.load();
        Lumi.FRIEND.read();
    }

    @Override
    public String[] getAutocorrect(int count, List<String> seperated) {
        return null;
    }
}
