package dev.perish.core.impl;


import dev.perish.Perish;

import dev.perish.api.interfaces.IChatHudHook;
import dev.perish.api.utils.Wrapper;
import dev.perish.mod.commands.Command;
import dev.perish.mod.commands.impl.*;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.impl.client.ClientSetting;
import java.util.HashMap;
import net.minecraft.text.Text;

public class CommandManager implements Wrapper {
    private final HashMap<String, Command> commands = new HashMap();

    public CommandManager() {
        this.init();
    }

    public static void sendMessage(String message) {
        mc.execute(() -> {
            if (!Module.nullCheck()) {
                if (ClientSetting.INSTANCE.messageStyle.getValue() == ClientSetting.Style.Earth) {
                    mc.inGameHud.getChatHud().addMessage(Text.of(message));
                } else if (ClientSetting.INSTANCE.messageStyle.getValue() == ClientSetting.Style.Moon) {
                    mc.inGameHud.getChatHud().addMessage(Text.of("§f[§b" + ClientSetting.INSTANCE.hackName.getValue() + "§f] " + message));
                } else {
                    ((IChatHudHook) mc.inGameHud.getChatHud()).alienClient$addMessage(Text.of(ClientSetting.INSTANCE.hackName.getValue() + "§f " + message));
                }
            }
        });
    }

    public static void sendMessageId(String message, int id) {
        mc.execute(
                () -> {
                    if (!Module.nullCheck()) {
                        if (ClientSetting.INSTANCE.messageStyle.getValue() == ClientSetting.Style.Earth) {
                            ((IChatHudHook) mc.inGameHud.getChatHud()).alienClient$addMessage(Text.of(message), id);
                        } else if (ClientSetting.INSTANCE.messageStyle.getValue() == ClientSetting.Style.Moon) {
                            ((IChatHudHook) mc.inGameHud.getChatHud())
                                    .alienClient$addMessage(Text.of("§f[§b" + ClientSetting.INSTANCE.hackName.getValue() + "§f] " + message), id);
                        } else {
                            ((IChatHudHook) mc.inGameHud.getChatHud()).alienClient$addMessage(Text.of(ClientSetting.INSTANCE.hackName.getValue() + "§f " + message), id);
                        }
                    }
                }
        );
    }

    public static void sendChatMessageWidthIdNoSync(String message, int id) {
        mc.execute(() -> {
            if (!Module.nullCheck()) {
                ((IChatHudHook) mc.inGameHud.getChatHud()).alienClient$addMessageOutSync(Text.of("§f" + message), id);
            }
        });
    }

    public void init() {
        this.registerCommand(new AimCommand());
        this.registerCommand(new BindCommand());
        this.registerCommand(new BindsCommand());
        this.registerCommand(new CleanerCommand());
        this.registerCommand(new ClipCommand());
        this.registerCommand(new EsuCommand());
        this.registerCommand(new FakePlayerCommand());
        this.registerCommand(new FriendCommand());
        this.registerCommand(new XrayCommand());
        this.registerCommand(new GamemodeCommand());
        this.registerCommand(new KitCommand());
        this.registerCommand(new LoadCommand());
        this.registerCommand(new PingCommand());
        this.registerCommand(new PrefixCommand());
        this.registerCommand(new RegearCommand());
        this.registerCommand(new RejoinCommand());
        this.registerCommand(new ReloadCommand());
        this.registerCommand(new SaveCommand());
        this.registerCommand(new TeleportCommand());
        this.registerCommand(new TCommand());
        this.registerCommand(new ToggleCommand());
        this.registerCommand(new TradeCommand());
        this.registerCommand(new PeekCommand());
        this.registerCommand(new GcCommand());
    }

    private void registerCommand(Command command) {
        this.commands.put(command.getName(), command);
    }

    public Command getCommandBySyntax(String string) {
        return this.commands.get(string);
    }

    public HashMap<String, Command> getCommands() {
        return this.commands;
    }

    public void command(String[] commandIn) {
        Command command = this.commands.get(commandIn[0].substring(Perish.getPrefix().length()).toLowerCase());
        if (command == null) {
            sendMessage("§4Invalid Command!");
        } else {
            String[] parameterList = new String[commandIn.length - 1];
            System.arraycopy(commandIn, 1, parameterList, 0, commandIn.length - 1);
            if (parameterList.length == 1 && parameterList[0].equals("help")) {
                command.sendUsage();
                return;
            }

            command.runCommand(parameterList);
        }
    }
}
