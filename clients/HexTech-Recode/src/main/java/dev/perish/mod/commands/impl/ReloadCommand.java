package dev.perish.mod.commands.impl;

import dev.perish.Perish;
import dev.perish.core.impl.ConfigManager;
import dev.perish.mod.commands.Command;

import java.util.List;

public class ReloadCommand extends Command {
    public ReloadCommand() {
        super("reload", "");
    }

    @Override
    public void runCommand(String[] parameters) {
        this.sendChatMessage("§fReloading..");
        Perish.CONFIG = new ConfigManager();
        Perish.CONFIG.load();
        Perish.CLEANER.read();
        Perish.XRAY.read();
        Perish.TRADE.read();
        Perish.FRIEND.read();
    }

    @Override
    public String[] getAutocorrect(int count, List<String> seperated) {
        return null;
    }
}
