package dev.kissoo.mod.commands.impl;

import dev.kissoo.Forever;
import dev.kissoo.mod.commands.Command;
import dev.kissoo.mod.modules.impl.hud.MiniMap;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class MiniMapCommand extends Command {
    
    public MiniMapCommand() {
        super("minimap", "waypoint [list] | [create/delete] [name] [hexColor]");
    }

    @Override
    public void runCommand(String[] parameters) {
        if (parameters.length == 0 || !parameters[0].equalsIgnoreCase("waypoint")) {
            this.sendUsage();
            return;
        }

        if (parameters.length == 1) {
            this.sendUsage();
            return;
        }

        String action = parameters[1].toLowerCase();

        switch (action) {
            case "list":
                if (MiniMap.WAYPOINTS.isEmpty()) {
                    this.sendChatMessage("§c当前没有保存任何路径点。");
                } else {
                    this.sendChatMessage("§a=== MiniMap 路径点列表 ===");
                    for (MiniMap.Waypoint wp : MiniMap.WAYPOINTS) {
                        this.sendChatMessage("§f- " + wp.name + " §7(X:" + (int)wp.x + " Y:" + (int)wp.y + " Z:" + (int)wp.z + ")");
                    }
                }
                break;

            case "create":
                if (parameters.length < 3) {
                    this.sendChatMessage("§c用法: minimap waypoint create <名称> [十六进制颜色, 如#FF0000]");
                    return;
                }
                
                if (mc.player == null) return;

                String name = parameters[2];
                Color color = Color.YELLOW; // 默认颜色
                if (parameters.length >= 4) {
                    try {
                        color = Color.decode(parameters[3]);
                    } catch (Exception e) {
                        this.sendChatMessage("§c颜色格式错误，已使用默认颜色。");
                    }
                }
                MiniMap.WAYPOINTS.removeIf(wp -> wp.name.equalsIgnoreCase(name));

                MiniMap.WAYPOINTS.add(new MiniMap.Waypoint(
                        name,
                        mc.player.getX(),
                        mc.player.getY(),
                        mc.player.getZ(),
                        color
                ));
                
                MiniMap.saveWaypoints();
                this.sendChatMessage("§a成功创建路径点: §f" + name);
                break;

            case "delete":
                if (parameters.length != 3) {
                    this.sendChatMessage("§c用法: minimap waypoint delete <名称>");
                    return;
                }

                String targetName = parameters[2];
                boolean removed = MiniMap.WAYPOINTS.removeIf(wp -> wp.name.equalsIgnoreCase(targetName));

                if (removed) {
                    MiniMap.saveWaypoints();
                    this.sendChatMessage("§a成功删除路径点: §f" + targetName);
                } else {
                    this.sendChatMessage("§c找不到名为 '" + targetName + "' 的路径点。");
                }
                break;

            default:
                this.sendUsage();
                break;
        }
    }

    @Override
    public String[] getAutocorrect(int count, List<String> seperated) {
        String input = seperated.getLast().toLowerCase();
        List<String> correct = new ArrayList<>();
        if (count == 1) {
            if ("waypoint".startsWith(input) || input.equalsIgnoreCase(Forever.getPrefix() + "minimap")) {
                correct.add("waypoint");
            }
        }
        else if (count == 2 && seperated.get(1).equalsIgnoreCase("waypoint")) {
            for (String x : List.of("list", "create", "delete")) {
                if (x.startsWith(input)) {
                    correct.add(x);
                }
            }
        }
        else if (count == 3 && seperated.get(2).equalsIgnoreCase("delete")) {
            for (MiniMap.Waypoint wp : MiniMap.WAYPOINTS) {
                if (wp.name.toLowerCase().startsWith(input)) {
                    correct.add(wp.name);
                }
            }
        }

        return correct.toArray(new String[0]);
    }
}