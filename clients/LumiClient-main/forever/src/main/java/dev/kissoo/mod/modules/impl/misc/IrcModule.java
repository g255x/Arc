package dev.kissoo.mod.modules.impl.misc;

import dev.kissoo.chat.ChatClient;
import dev.kissoo.core.impl.CommandManager;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;

public class IrcModule extends Module {
    public static IrcModule INSTANCE;

    private static final long IRC_FRIEND_DISABLE_COOLDOWN_MS = 15_000L;
    private static long ircFriendCooldownUntil = 0L;

    public final BooleanSetting ircFriend = this.add(new BooleanSetting("IrcFriend", true));
    private final BooleanSetting hideAdmin = this.add(new BooleanSetting("HideAdmin", false, () -> ChatClient.isAdmin()));
    private final BooleanSetting chatHide = this.add(new BooleanSetting("ChatHide", false, () -> ChatClient.isAdmin()));

    public IrcModule() {
        super("IRC", Module.Category.Misc);
        this.setChinese("IRC");
        INSTANCE = this;
        this.state = true;

        ircFriend.injectTask(() -> {
            if (!ircFriend.getValue()) {
                beginIrcFriendCooldown("disabled IrcFriend");
            } else {
                CommandManager.sendMessage("§a[IRC] IrcFriend enabled");
            }
        });
        hideAdmin.injectTask(() -> {
            if (hideAdmin.getValue() && !ChatClient.isAdmin()) {
                hideAdmin.setValueWithoutTask(false);
                CommandManager.sendMessage("§c[IRC] Only Admin can enable HideAdmin");
            }
        });
        chatHide.injectTask(() -> {
            if (chatHide.getValue() && !ChatClient.isAdmin()) {
                chatHide.setValueWithoutTask(false);
                CommandManager.sendMessage("§c[IRC] Only Admin can enable ChatHide");
            }
        });
    }

    public static boolean isIrcFriendOn() {
        return isLocalIrcFriendEnabled();
    }

    public static boolean isLocalIrcFriendEnabled() {
        if (INSTANCE == null) return true;
        return INSTANCE.isOn() && INSTANCE.ircFriend.getValue();
    }

    public static boolean isIrcFriendCooldownActive() {
        return System.currentTimeMillis() < ircFriendCooldownUntil;
    }

    public static long ircFriendCooldownRemainingMs() {
        return Math.max(0L, ircFriendCooldownUntil - System.currentTimeMillis());
    }

    public static boolean shouldProtectIrcUser(String mcName) {
        return ChatClient.isIrcUser(mcName)
                && ChatClient.isIrcFriendEnabled(mcName)
                && (isLocalIrcFriendEnabled() || isIrcFriendCooldownActive());
    }

    public static String ircStatusPrefix(String mcName) {
        if (!ChatClient.isIrcUser(mcName)) return "";
        String ircName = ChatClient.getIrcName(mcName);
        String label = ircName.isEmpty() ? "IRC" : ircName;
        String client = ChatClient.getIrcClient(mcName);
        String clientLabel = client.isEmpty() ? "" : "\u00a7d[" + client + "] ";
        String friend = ChatClient.isIrcFriendEnabled(mcName) ? "§a[Friend] " : "";
        return friend + clientLabel + ChatClient.getIrcColorCode(mcName) + "[" + label + "] \u00a7r";
    }

    private static void beginIrcFriendCooldown(String reason) {
        ircFriendCooldownUntil = Math.max(
                ircFriendCooldownUntil,
                System.currentTimeMillis() + IRC_FRIEND_DISABLE_COOLDOWN_MS
        );
        String msg = "[IRC] " + reason + "; IrcFriend protection cooldown: 15s";
        CommandManager.sendMessage("§e" + msg);
        ChatClient.send(msg);
    }

    public static boolean isHideAdmin() {
        return INSTANCE != null && INSTANCE.hideAdmin.getValue() && ChatClient.isAdmin();
    }

    public static boolean isChatHide() {
        return INSTANCE != null && INSTANCE.chatHide.getValue() && ChatClient.isAdmin();
    }

    @Override
    public void toggle() {
        if (isOn()) {
            beginIrcFriendCooldown("disabled IRC");
        }
        super.toggle();
    }

    @Override
    public void onToggle() {
        if (!isOn()) {
            ChatClient.stop();
        } else {
            ChatClient.init();
        }
    }
}
