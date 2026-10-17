package dev.kissoo.mod.commands.impl;

import dev.kissoo.core.impl.CommandManager;
import dev.kissoo.mod.commands.Command;
import dev.kissoo.mod.modules.impl.misc.PearlRun;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import dev.kissoo.Forever;
import java.util.ArrayList;
import java.util.List;

public class PearlWhitelistCommand extends Command {

    public PearlWhitelistCommand() {
        super("pearlwl", "[list] | [add/del] [name]");
    }

    @Override
    public void runCommand(String[] parameters) {
        if (parameters.length == 0) {
            this.sendUsage();
            return;
        }

        if (PearlRun.INSTANCE == null) {
            CommandManager.sendMessage("§cPearlRun module is not initialized!");
            return;
        }

        String action = parameters[0].toLowerCase();
        List<String> whitelist = PearlRun.INSTANCE.whitelist;

        switch (action) {
            case "list":
                if (whitelist.isEmpty()) {
                    this.sendChatMessage("§fPearl Whitelist is empty");
                    return;
                }
                this.sendChatMessage("§ePearl Whitelist: §a" + String.join(", ", whitelist));
                break;

            case "add":
                if (parameters.length != 2) {
                    this.sendUsage();
                    return;
                }

                String addName = parameters[1];
                if (whitelist.contains(addName)) {
                    this.sendChatMessage("§f" + addName + " §eis already in the whitelist");
                    return;
                }

                whitelist.add(addName);
                PearlRun.INSTANCE.saveWhitelist();

                playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.2F);
                CommandManager.sendMessage("§d✿ §a已添加珍珠白名单§f: " + addName + " §d✿");
                break;

            case "del":
                if (parameters.length != 2) {
                    this.sendUsage();
                    return;
                }

                String removeName = parameters[1];
                if (!whitelist.contains(removeName)) {
                    this.sendChatMessage("§f" + removeName + " §ewas not in the whitelist");
                    return;
                }

                whitelist.remove(removeName);
                PearlRun.INSTANCE.saveWhitelist();

                playSound(SoundEvents.ENTITY_ITEM_BREAK, 1.0F, 0.8F);
                CommandManager.sendMessage("§7✖ §c已移除珍珠白名单§f: " + removeName + " §7✖");
                break;

            default:
                if (parameters.length == 1) {
                    this.sendChatMessage("§f" + parameters[0] +
                            (whitelist.contains(parameters[0])
                                    ? " §ais in pearl whitelist"
                                    : " §cisn't in pearl whitelist"));
                } else {
                    this.sendUsage();
                }
        }
    }

    private void playSound(net.minecraft.sound.SoundEvent sound, float volume, float pitch) {
        if (mc.world != null && mc.player != null) {
            mc.world.playSound(
                    mc.player,
                    mc.player.getBlockPos(),
                    sound,
                    SoundCategory.PLAYERS,
                    volume,
                    pitch
            );
        }
    }

    @Override
    public String[] getAutocorrect(int count, List<String> separated) {
        if (separated.isEmpty()) return null;

        String input = separated.get(separated.size() - 1).toLowerCase();

        if (count == 1) {
            List<String> correct = new ArrayList<>();
            for (String x : List.of("add", "del", "list")) {
                if (input.equalsIgnoreCase(Forever.getPrefix() + "pearlwl") || x.toLowerCase().startsWith(input)) {
                    correct.add(x);
                }
            }
            return correct.toArray(new String[0]);
        }

        if (count == 2) {
            String action = separated.get(0).toLowerCase();

            if (action.equals("add") && mc.world != null) {
                return mc.world.getPlayers().stream()
                        .map(p -> p.getGameProfile().getName())
                        .filter(name -> name.toLowerCase().startsWith(input))
                        .toArray(String[]::new);
            }

            if (action.equals("del") && PearlRun.INSTANCE != null) {
                return PearlRun.INSTANCE.whitelist.stream()
                        .filter(name -> name.toLowerCase().startsWith(input))
                        .toArray(String[]::new);
            }
        }

        return null;
    }
}