package dev.kissoo.mod.commands.impl;

import dev.kissoo.chat.ChatClient;
import dev.kissoo.mod.commands.Command;

import java.util.List;

public class ChatCommand extends Command {
    public ChatCommand() {
        super("chat", "<消息内容>");
    }

    @Override
    public void runCommand(String[] parameters) {
        if (parameters.length == 0) {
            sendUsage();
            return;
        }
        ChatClient.send(String.join(" ", parameters));
    }

    @Override
    public String[] getAutocorrect(int count, List<String> separated) {
        return new String[0];
    }
}
