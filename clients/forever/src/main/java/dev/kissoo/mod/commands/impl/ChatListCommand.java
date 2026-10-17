package dev.kissoo.mod.commands.impl;

import dev.kissoo.chat.ChatClient;
import dev.kissoo.mod.commands.Command;

import java.util.List;

public class ChatListCommand extends Command {
    public ChatListCommand() {
        super("chatlist", "");
    }

    @Override
    public void runCommand(String[] parameters) {
        ChatClient.fetchHistory();
    }

    @Override
    public String[] getAutocorrect(int count, List<String> separated) {
        return new String[0];
    }
}
