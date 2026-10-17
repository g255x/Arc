package dev.kissoo.mod.commands.impl;

import dev.kissoo.chat.ChatClient;
import dev.kissoo.mod.commands.Command;
import dev.kissoo.proxy.ProxyManager;

import java.util.List;

public class ZenithCommand extends Command {
    public ZenithCommand() {
        super("zenith", "[stop|status]");
    }

    @Override
    public void runCommand(String[] parameters) {
        if (parameters.length == 0) {
            this.sendUsage();
            return;
        }

        String action = parameters[0].toLowerCase();
        switch (action) {
            case "stop" -> {
                if (parameters.length > 1 && "all".equalsIgnoreCase(parameters[1])) {
                    if (!ChatClient.isAdmin()) {
                        this.sendChatMessage("[IRC] Admin permission required");
                        return;
                    }
                    ChatClient.send(";+zenith stop");
                    this.sendChatMessage("[Zenith] stop sent to online clients");
                    return;
                }
                ProxyManager.stopProxy();
                this.sendChatMessage("§a排队代理已停止");
            }
            case "status" -> {
                ProxyManager.State state = ProxyManager.getState();
                String statusMsg = switch (state) {
                    case STOPPED        -> "§7已停止";
                    case INSTALLING     -> "§e正在安装...";
                    case STARTING       -> "§e正在启动...";
                    case LOGIN_REQUIRED -> "§b等待登录";
                    case CONNECTING     -> "§e正在连接 " + ProxyManager.getTargetHost();
                    case READY          -> "§a已就绪 (监听端口 " + ProxyManager.getListenPort() + ")";
                    case FAILED         -> "§c启动失败";
                };
                this.sendChatMessage("§f排队代理状态: " + statusMsg);
            }
            default -> this.sendUsage();
        }
    }

    @Override
    public String[] getAutocorrect(int count, List<String> seperated) {
        if (count == 1) {
            return new String[]{"stop", "status"};
        }
        if (count == 2 && !seperated.isEmpty() && "stop".equalsIgnoreCase(seperated.get(0))) {
            return new String[]{"all"};
        }
        return new String[0];
    }
}
