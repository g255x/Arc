package dev.perish.mod.commands.impl;

import dev.perish.Perish;
import dev.perish.core.Manager;
import dev.perish.core.impl.ConfigManager;
import dev.perish.mod.commands.Command;

import java.io.File;
import java.util.List;

public class SaveCommand extends Command {
    public SaveCommand() {
        super("save", "");
    }

    @Override
    public void runCommand(String[] parameters) {
        if (parameters.length == 1) {
            this.sendChatMessage("§fSaving config named " + parameters[0]);
            File folder = new File(mc.runDirectory.getPath() + File.separator + "Perish-Hyperion".toLowerCase() + File.separator + "cfg");
            if (!folder.exists()) {
                folder.mkdirs();
            }

            ConfigManager.options = Manager.getFile("cfg" + File.separator + parameters[0] + ".cfg");
            Perish.save();
            ConfigManager.options = Manager.getFile("options.txt");
        } else {
            this.sendChatMessage("§fSaving..");
        }

        Perish.save();
    }

    @Override
    public String[] getAutocorrect(int count, List<String> seperated) {
        return null;
    }
}
