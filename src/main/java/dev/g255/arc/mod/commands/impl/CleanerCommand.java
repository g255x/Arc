/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.Items
 */
package dev.g255.arc.mod.commands.impl;

import dev.g255.arc.Arc;
import dev.g255.arc.Arc;
import dev.g255.arc.core.impl.PlayerManager;
import dev.g255.arc.mod.commands.Command;
import dev.g255.arc.mod.gui.windows.WindowsScreen;
import dev.g255.arc.mod.gui.windows.impl.ItemSelectWindow;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.Items;

public class CleanerCommand
extends Command {
    public CleanerCommand() {
        super("cleaner", "[\"\"/name/reset/clear/list] | [add/remove] [name]");
    }

    @Override
    public void runCommand(String[] parameters) {
        if (parameters.length == 0) {
            PlayerManager.screenToOpen = new WindowsScreen(new ItemSelectWindow(Arc.CLEANER));
            return;
        }
        switch (parameters[0]) {
            case "reset": {
                Arc.CLEANER.clear();
                Arc.CLEANER.add(Items.NETHERITE_SWORD.getTranslationKey());
                Arc.CLEANER.add(Items.NETHERITE_PICKAXE.getTranslationKey());
                Arc.CLEANER.add(Items.NETHERITE_HELMET.getTranslationKey());
                Arc.CLEANER.add(Items.NETHERITE_CHESTPLATE.getTranslationKey());
                Arc.CLEANER.add(Items.NETHERITE_LEGGINGS.getTranslationKey());
                Arc.CLEANER.add(Items.NETHERITE_BOOTS.getTranslationKey());
                Arc.CLEANER.add(Items.OBSIDIAN.getTranslationKey());
                Arc.CLEANER.add(Items.ENDER_CHEST.getTranslationKey());
                Arc.CLEANER.add(Items.ENDER_PEARL.getTranslationKey());
                Arc.CLEANER.add(Items.ENCHANTED_GOLDEN_APPLE.getTranslationKey());
                Arc.CLEANER.add(Items.EXPERIENCE_BOTTLE.getTranslationKey());
                Arc.CLEANER.add(Items.COBWEB.getTranslationKey());
                Arc.CLEANER.add(Items.POTION.getTranslationKey());
                Arc.CLEANER.add(Items.SPLASH_POTION.getTranslationKey());
                Arc.CLEANER.add(Items.TOTEM_OF_UNDYING.getTranslationKey());
                Arc.CLEANER.add(Items.END_CRYSTAL.getTranslationKey());
                Arc.CLEANER.add(Items.ELYTRA.getTranslationKey());
                Arc.CLEANER.add(Items.FLINT_AND_STEEL.getTranslationKey());
                Arc.CLEANER.add(Items.PISTON.getTranslationKey());
                Arc.CLEANER.add(Items.STICKY_PISTON.getTranslationKey());
                Arc.CLEANER.add(Items.REDSTONE_BLOCK.getTranslationKey());
                Arc.CLEANER.add(Items.GLOWSTONE.getTranslationKey());
                Arc.CLEANER.add(Items.RESPAWN_ANCHOR.getTranslationKey());
                Arc.CLEANER.add(Items.ANVIL.getTranslationKey());
                this.sendChatMessage("\u00a7fItems list got reset");
                return;
            }
            case "clear": {
                Arc.CLEANER.getList().clear();
                this.sendChatMessage("\u00a7fItems list got clear");
                return;
            }
            case "list": {
                if (Arc.CLEANER.getList().isEmpty()) {
                    this.sendChatMessage("\u00a7fItems list is empty");
                    return;
                }
                for (String name : Arc.CLEANER.getList()) {
                    this.sendChatMessage("\u00a7a" + name);
                }
                return;
            }
            case "add": {
                if (parameters.length == 2) {
                    Arc.CLEANER.add(parameters[1]);
                    this.sendChatMessage("\u00a7f" + parameters[1] + (Arc.CLEANER.inList(parameters[1]) ? " \u00a7ahas been added" : " \u00a7chas been removed"));
                    return;
                }
                this.sendUsage();
                return;
            }
            case "remove": {
                if (parameters.length == 2) {
                    Arc.CLEANER.remove(parameters[1]);
                    this.sendChatMessage("\u00a7f" + parameters[1] + (Arc.CLEANER.inList(parameters[1]) ? " \u00a7ahas been added" : " \u00a7chas been removed"));
                    return;
                }
                this.sendUsage();
                return;
            }
        }
        if (parameters.length == 1) {
            this.sendChatMessage("\u00a7f" + parameters[0] + (Arc.CLEANER.inList(parameters[0]) ? " \u00a7ais in whitelist" : " \u00a7cisn't in whitelist"));
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
                if (!input.equalsIgnoreCase(Arc.getPrefix() + "cleaner") && !x.toLowerCase().startsWith(input)) continue;
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

