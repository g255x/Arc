/*
 * Decompiled with CFR 0.152.
 */
package dev.G255.Lumi.mod.commands.impl;

import dev.G255.Lumi.Lumi;
import dev.G255.Lumi.core.impl.ConfigManager;
import dev.G255.Lumi.mod.commands.Command;
import java.util.List;

public class SaveCommand
extends Command {
    public SaveCommand() {
        super("save", "");
    }

    @Override
    public void runCommand(String[] parameters) {
        if (parameters.length == 1) {
            this.sendChatMessage("\u00a7fSaving config named " + parameters[0]);
            ConfigManager.saveCfg(parameters[0]);
        } else {
            this.sendChatMessage("\u00a7fSaving..");
        }
        Lumi.save();
    }

    @Override
    public String[] getAutocorrect(int count, List<String> seperated) {
        return null;
    }
}
