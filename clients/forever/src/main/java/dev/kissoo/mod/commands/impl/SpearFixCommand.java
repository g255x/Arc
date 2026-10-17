package dev.kissoo.mod.commands.impl;

import dev.kissoo.mod.commands.Command;
import dev.kissoo.mod.spearfix.SpearFix;

import java.util.List;

public class SpearFixCommand extends Command {
    public SpearFixCommand() {
        super("spearfix", "noslow <true|false>");
    }

    @Override
    public void runCommand(String[] parameters) {
        if (!handle(parameters, this::sendChatMessage)) {
            this.sendUsage();
        }
    }

    @Override
    public String[] getAutocorrect(int count, List<String> separated) {
        if (count == 1) {
            return new String[]{"noslow"};
        }
        if (count == 2 && separated.size() > 1 && separated.get(1).equalsIgnoreCase("noslow")) {
            return new String[]{"true", "false"};
        }
        return null;
    }

    public static boolean handle(String[] parameters, java.util.function.Consumer<String> feedback) {
        if (parameters.length != 2 || !parameters[0].equalsIgnoreCase("noslow")) {
            return false;
        }

        if (!parameters[1].equalsIgnoreCase("true") && !parameters[1].equalsIgnoreCase("false")) {
            return false;
        }

        boolean enabled = Boolean.parseBoolean(parameters[1]);
        SpearFix.setNoslowEnabled(enabled);
        feedback.accept("§bSpearFix §fnoslow set to §e" + enabled);
        return true;
    }
}
