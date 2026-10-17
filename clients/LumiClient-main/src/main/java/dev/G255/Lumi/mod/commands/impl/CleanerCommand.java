package dev.G255.Lumi.mod.commands.impl;

import dev.G255.Lumi.Lumi;
import dev.G255.Lumi.Lumi;
import dev.G255.Lumi.core.impl.PlayerManager;
import dev.G255.Lumi.mod.commands.Command;
import dev.G255.Lumi.mod.gui.windows.WindowsScreen;
import dev.G255.Lumi.mod.gui.windows.impl.ItemSelectWindow;
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
            PlayerManager.screenToOpen = new WindowsScreen(new ItemSelectWindow(Lumi.CLEANER));
            return;
        }
        switch (parameters[0]) {
            case "reset": {
                Lumi.CLEANER.clear();
                Lumi.CLEANER.add(Items.NETHERITE_SWORD.getTranslationKey());
                Lumi.CLEANER.add(Items.NETHERITE_PICKAXE.getTranslationKey());
                Lumi.CLEANER.add(Items.NETHERITE_HELMET.getTranslationKey());
                Lumi.CLEANER.add(Items.NETHERITE_CHESTPLATE.getTranslationKey());
                Lumi.CLEANER.add(Items.NETHERITE_LEGGINGS.getTranslationKey());
                Lumi.CLEANER.add(Items.NETHERITE_BOOTS.getTranslationKey());
                Lumi.CLEANER.add(Items.OBSIDIAN.getTranslationKey());
                Lumi.CLEANER.add(Items.ENDER_CHEST.getTranslationKey());
                Lumi.CLEANER.add(Items.ENDER_PEARL.getTranslationKey());
                Lumi.CLEANER.add(Items.ENCHANTED_GOLDEN_APPLE.getTranslationKey());
                Lumi.CLEANER.add(Items.EXPERIENCE_BOTTLE.getTranslationKey());
                Lumi.CLEANER.add(Items.COBWEB.getTranslationKey());
                Lumi.CLEANER.add(Items.POTION.getTranslationKey());
                Lumi.CLEANER.add(Items.SPLASH_POTION.getTranslationKey());
                Lumi.CLEANER.add(Items.TOTEM_OF_UNDYING.getTranslationKey());
                Lumi.CLEANER.add(Items.END_CRYSTAL.getTranslationKey());
                Lumi.CLEANER.add(Items.ELYTRA.getTranslationKey());
                Lumi.CLEANER.add(Items.FLINT_AND_STEEL.getTranslationKey());
                Lumi.CLEANER.add(Items.PISTON.getTranslationKey());
                Lumi.CLEANER.add(Items.STICKY_PISTON.getTranslationKey());
                Lumi.CLEANER.add(Items.REDSTONE_BLOCK.getTranslationKey());
                Lumi.CLEANER.add(Items.GLOWSTONE.getTranslationKey());
                Lumi.CLEANER.add(Items.RESPAWN_ANCHOR.getTranslationKey());
                Lumi.CLEANER.add(Items.ANVIL.getTranslationKey());
                this.sendChatMessage("\u00a7fItems list got reset");
                return;
            }
            case "clear": {
                Lumi.CLEANER.getList().clear();
                this.sendChatMessage("\u00a7fItems list got clear");
                return;
            }
            case "list": {
                if (Lumi.CLEANER.getList().isEmpty()) {
                    this.sendChatMessage("\u00a7fItems list is empty");
                    return;
                }
                for (String name : Lumi.CLEANER.getList()) {
                    this.sendChatMessage("\u00a7a" + name);
                }
                return;
            }
            case "add": {
                if (parameters.length == 2) {
                    Lumi.CLEANER.add(parameters[1]);
                    this.sendChatMessage("\u00a7f" + parameters[1] + (Lumi.CLEANER.inList(parameters[1]) ? " \u00a7ahas been added" : " \u00a7chas been removed"));
                    return;
                }
                this.sendUsage();
                return;
            }
            case "remove": {
                if (parameters.length == 2) {
                    Lumi.CLEANER.remove(parameters[1]);
                    this.sendChatMessage("\u00a7f" + parameters[1] + (Lumi.CLEANER.inList(parameters[1]) ? " \u00a7ahas been added" : " \u00a7chas been removed"));
                    return;
                }
                this.sendUsage();
                return;
            }
        }
        if (parameters.length == 1) {
            this.sendChatMessage("\u00a7f" + parameters[0] + (Lumi.CLEANER.inList(parameters[0]) ? " \u00a7ais in whitelist" : " \u00a7cisn't in whitelist"));
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
                if (!input.equalsIgnoreCase(Lumi.getPrefix() + "cleaner") && !x.toLowerCase().startsWith(input)) continue;
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

