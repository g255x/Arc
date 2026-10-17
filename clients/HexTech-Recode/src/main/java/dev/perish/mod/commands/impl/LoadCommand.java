package dev.perish.mod.commands.impl;

import dev.perish.Perish;
import dev.perish.core.Manager;
import dev.perish.core.impl.ConfigManager;
import dev.perish.mod.commands.Command;
import dev.perish.mod.modules.impl.client.Fonts;

import java.io.File;
import java.util.List;

public class LoadCommand extends Command {
    public LoadCommand() {
        super("load", "[config]");
    }

    @Override
    public void runCommand(String[] parameters) {
        if (parameters.length == 0) {
            this.sendUsage();
        } else {
            this.sendChatMessage("§fLoading..");
            ConfigManager.options = Manager.getFile("cfg" + File.separator + parameters[0] + ".cfg");
            Perish.CONFIG = new ConfigManager();
            Perish.CONFIG.load();
            ConfigManager.options = Manager.getFile("options.txt");
            Perish.save();
            Fonts.INSTANCE.refresh();
        }
    }

    @Override
    public String[] getAutocorrect(int count, List<String> seperated) {
        return null;
    }
}
