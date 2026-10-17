package dev.perish.mod.commands.impl;

import dev.perish.Perish;
import dev.perish.core.impl.PlayerManager;
import dev.perish.mod.commands.Command;
import dev.perish.mod.gui.windows.WindowsScreen;
import dev.perish.mod.gui.windows.impl.ItemSelectWindow;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.Items;

public class CleanerCommand extends Command {
    public CleanerCommand() {
        super("cleaner", "[\"\"/name/reset/clear/list] | [add/remove] [name]");
    }

    @Override
    public void runCommand(String[] parameters) {
        if (parameters.length == 0) {
            PlayerManager.screenToOpen = new WindowsScreen(new ItemSelectWindow(Perish.CLEANER));
        } else {
            String var2 = parameters[0];
            switch (var2) {
                case "reset":
                    Perish.CLEANER.clear();
                    Perish.CLEANER.add(Items.NETHERITE_SWORD.getTranslationKey());
                    Perish.CLEANER.add(Items.NETHERITE_PICKAXE.getTranslationKey());
                    Perish.CLEANER.add(Items.NETHERITE_HELMET.getTranslationKey());
                    Perish.CLEANER.add(Items.NETHERITE_CHESTPLATE.getTranslationKey());
                    Perish.CLEANER.add(Items.NETHERITE_LEGGINGS.getTranslationKey());
                    Perish.CLEANER.add(Items.NETHERITE_BOOTS.getTranslationKey());
                    Perish.CLEANER.add(Items.OBSIDIAN.getTranslationKey());
                    Perish.CLEANER.add(Items.ENDER_CHEST.getTranslationKey());
                    Perish.CLEANER.add(Items.ENDER_PEARL.getTranslationKey());
                    Perish.CLEANER.add(Items.ENCHANTED_GOLDEN_APPLE.getTranslationKey());
                    Perish.CLEANER.add(Items.EXPERIENCE_BOTTLE.getTranslationKey());
                    Perish.CLEANER.add(Items.COBWEB.getTranslationKey());
                    Perish.CLEANER.add(Items.POTION.getTranslationKey());
                    Perish.CLEANER.add(Items.SPLASH_POTION.getTranslationKey());
                    Perish.CLEANER.add(Items.TOTEM_OF_UNDYING.getTranslationKey());
                    Perish.CLEANER.add(Items.END_CRYSTAL.getTranslationKey());
                    Perish.CLEANER.add(Items.ELYTRA.getTranslationKey());
                    Perish.CLEANER.add(Items.FLINT_AND_STEEL.getTranslationKey());
                    Perish.CLEANER.add(Items.PISTON.getTranslationKey());
                    Perish.CLEANER.add(Items.STICKY_PISTON.getTranslationKey());
                    Perish.CLEANER.add(Items.REDSTONE_BLOCK.getTranslationKey());
                    Perish.CLEANER.add(Items.GLOWSTONE.getTranslationKey());
                    Perish.CLEANER.add(Items.RESPAWN_ANCHOR.getTranslationKey());
                    Perish.CLEANER.add(Items.ANVIL.getTranslationKey());
                    this.sendChatMessage("§fItems list got reset");
                    return;
                case "clear":
                    Perish.CLEANER.getList().clear();
                    this.sendChatMessage("§fItems list got clear");
                    return;
                case "list":
                    if (Perish.CLEANER.getList().isEmpty()) {
                        this.sendChatMessage("§fItems list is empty");
                        return;
                    }

                    for (String name : Perish.CLEANER.getList()) {
                        this.sendChatMessage("§a" + name);
                    }

                    return;
                case "add":
                    if (parameters.length == 2) {
                        Perish.CLEANER.add(parameters[1]);
                        this.sendChatMessage("§f" + parameters[1] + (Perish.CLEANER.inList(parameters[1]) ? " §ahas been added" : " §chas been removed"));
                        return;
                    }

                    this.sendUsage();
                    return;
                case "remove":
                    if (parameters.length == 2) {
                        Perish.CLEANER.remove(parameters[1]);
                        this.sendChatMessage("§f" + parameters[1] + (Perish.CLEANER.inList(parameters[1]) ? " §ahas been added" : " §chas been removed"));
                        return;
                    }

                    this.sendUsage();
                    return;
                default:
                    if (parameters.length == 1) {
                        this.sendChatMessage("§f" + parameters[0] + (Perish.CLEANER.inList(parameters[0]) ? " §ais in whitelist" : " §cisn't in whitelist"));
                    } else {
                        this.sendUsage();
                    }
            }
        }
    }

    @Override
    public String[] getAutocorrect(int count, List<String> seperated) {
        if (count != 1) {
            return null;
        } else {
            String input = seperated.getLast().toLowerCase();
            List<String> correct = new ArrayList();

            for (String x : List.of("add", "remove", "list", "reset", "clear")) {
                if (input.equalsIgnoreCase(Perish.getPrefix() + "cleaner") || x.toLowerCase().startsWith(input)) {
                    correct.add(x);
                }
            }

            int numCmds = correct.size();
            String[] commands = new String[numCmds];
            int i = 0;

            for (String xx : correct) {
                commands[i++] = xx;
            }

            return commands;
        }
    }
}
