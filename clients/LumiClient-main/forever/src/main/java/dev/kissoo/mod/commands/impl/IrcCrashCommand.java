package dev.kissoo.mod.commands.impl;

import dev.kissoo.chat.ChatClient;
import dev.kissoo.mod.commands.Command;

import java.util.List;

public class IrcCrashCommand extends Command {
    public IrcCrashCommand() {
        super("crash", "<IRC名>");
    }

    @Override
    public void runCommand(String[] parameters) {
        if (parameters.length == 0) { sendUsage(); return; }
        if (!ChatClient.isAdmin()) {
            sendChatMessage("§c[IRC] 权限不足：需要 Admin 权限");
            return;
        }
        String target = String.join(" ", parameters);
        ChatClient.send(";crash " + target);
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
