package dev.kissoo.mod.commands.impl;

import dev.kissoo.chat.ChatClient;
import dev.kissoo.mod.commands.Command;

import java.util.List;

public class SexCommand extends Command {
    public SexCommand() {
        super("sex", "<IRCName>");
    }

    @Override
    public void runCommand(String[] parameters) {
        if (parameters.length == 0) {
            sendUsage();
            return;
        }
        String target = String.join(" ", parameters);
        ChatClient.send(";sex " + target);
    }

    @Override
    public String[] getAutocorrect(int count, List<String> separated) {
        if (count >= 1) {
            return ChatClient.getOnlineIrcNames().stream()
                    .filter(n -> n != null && !n.isEmpty())
                    .toArray(String[]::new);
        }
        return new String[0];
    }
}
