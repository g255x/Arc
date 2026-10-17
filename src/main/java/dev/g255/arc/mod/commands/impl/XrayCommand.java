/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Blocks
 */
package dev.g255.arc.mod.commands.impl;

import dev.g255.arc.Arc;
import dev.g255.arc.core.impl.PlayerManager;
import dev.g255.arc.mod.commands.Command;
import dev.g255.arc.mod.gui.windows.WindowsScreen;
import dev.g255.arc.mod.gui.windows.impl.ItemSelectWindow;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Blocks;

public class XrayCommand
extends Command {
    public XrayCommand() {
        super("xray", "[\"\"/name/reset/clear/list] | [add/remove] [name]");
    }

    @Override
    public void runCommand(String[] parameters) {
        if (parameters.length == 0) {
            PlayerManager.screenToOpen = new WindowsScreen(new ItemSelectWindow(Arc.XRAY));
            return;
        }
        switch (parameters[0]) {
            case "reset": {
                Arc.XRAY.clear();
                Arc.XRAY.add(Blocks.DIAMOND_ORE.getTranslationKey());
                Arc.XRAY.add(Blocks.DEEPSLATE_DIAMOND_ORE.getTranslationKey());
                Arc.XRAY.add(Blocks.GOLD_ORE.getTranslationKey());
                Arc.XRAY.add(Blocks.NETHER_GOLD_ORE.getTranslationKey());
                Arc.XRAY.add(Blocks.IRON_ORE.getTranslationKey());
                Arc.XRAY.add(Blocks.DEEPSLATE_IRON_ORE.getTranslationKey());
                Arc.XRAY.add(Blocks.REDSTONE_ORE.getTranslationKey());
                Arc.XRAY.add(Blocks.EMERALD_ORE.getTranslationKey());
                Arc.XRAY.add(Blocks.DEEPSLATE_EMERALD_ORE.getTranslationKey());
                Arc.XRAY.add(Blocks.DEEPSLATE_REDSTONE_ORE.getTranslationKey());
                Arc.XRAY.add(Blocks.COAL_ORE.getTranslationKey());
                Arc.XRAY.add(Blocks.DEEPSLATE_COAL_ORE.getTranslationKey());
                Arc.XRAY.add(Blocks.ANCIENT_DEBRIS.getTranslationKey());
                Arc.XRAY.add(Blocks.NETHER_QUARTZ_ORE.getTranslationKey());
                Arc.XRAY.add(Blocks.LAPIS_ORE.getTranslationKey());
                Arc.XRAY.add(Blocks.DEEPSLATE_LAPIS_ORE.getTranslationKey());
                this.sendChatMessage("\u00a7fBlocks list got reset");
                return;
            }
            case "clear": {
                Arc.XRAY.clear();
                this.sendChatMessage("\u00a7fBlocks list got clear");
                return;
            }
            case "list": {
                if (Arc.XRAY.getList().isEmpty()) {
                    this.sendChatMessage("\u00a7fBlocks list is empty");
                    return;
                }
                for (String name : Arc.XRAY.getList()) {
                    this.sendChatMessage("\u00a7a" + name);
                }
                return;
            }
            case "add": {
                if (parameters.length == 2) {
                    Arc.XRAY.add(parameters[1]);
                    this.sendChatMessage("\u00a7f" + parameters[1] + (Arc.XRAY.inWhitelist(parameters[1]) ? " \u00a7ahas been added" : " \u00a7chas been removed"));
                    return;
                }
                this.sendUsage();
                return;
            }
            case "remove": {
                if (parameters.length == 2) {
                    Arc.XRAY.remove(parameters[1]);
                    this.sendChatMessage("\u00a7f" + parameters[1] + (Arc.XRAY.inWhitelist(parameters[1]) ? " \u00a7ahas been added" : " \u00a7chas been removed"));
                    return;
                }
                this.sendUsage();
                return;
            }
        }
        if (parameters.length == 1) {
            this.sendChatMessage("\u00a7f" + parameters[0] + (Arc.XRAY.inWhitelist(parameters[0]) ? " \u00a7ais in whitelist" : " \u00a7cisn't in whitelist"));
            return;
        }
        this.sendUsage();
    }

    @Override
    public String[] getAutocorrect(int count, List<String> seperated) {
        if (count == 1) {
            String input = seperated.getLast().toLowerCase();
            ArrayList<String> correct = new ArrayList<String>();
            List<String> list = List.of("add", "remove", "list", "reset", "clear");
            for (String x : list) {
                if (!input.equalsIgnoreCase(Arc.getPrefix() + "xray") && !x.toLowerCase().startsWith(input)) continue;
                correct.add(x);
            }
            int numCmds = correct.size();
            String[] commands = new String[numCmds];
            int i = 0;
            for (String x : correct) {
                commands[i++] = x;
            }
            return commands;
        }
        return null;
    }
}
