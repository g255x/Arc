/*
 * Decompiled with CFR 0.152.
 */
package dev.G255.Lumi.mod.commands;

import dev.G255.Lumi.Lumi;
import dev.G255.Lumi.api.utils.Wrapper;
import dev.G255.Lumi.core.impl.CommandManager;
import java.util.List;
import java.util.Objects;

public abstract class Command
implements Wrapper {
    protected final String name;
    protected final String syntax;

    public Command(String name, String syntax) {
        this.name = Objects.requireNonNull(name);
        this.syntax = Objects.requireNonNull(syntax);
    }

    public String getName() {
        return this.name;
    }

    public String getSyntax() {
        return this.syntax;
    }

    public abstract void runCommand(String[] var1);

    public abstract String[] getAutocorrect(int var1, List<String> var2);

    public void sendUsage() {
        this.sendChatMessage("\u00a74Parameter error \u00a7r" + Lumi.getPrefix() + this.getName() + " \u00a77" + this.getSyntax());
    }

    public void sendChatMessage(String message) {
        CommandManager.sendMessage(message);
    }
}
