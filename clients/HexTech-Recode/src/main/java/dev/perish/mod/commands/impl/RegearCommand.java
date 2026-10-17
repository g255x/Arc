package dev.perish.mod.commands.impl;

import dev.perish.core.Manager;
import dev.perish.mod.commands.Command;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;

/**
 * Stores the aggregate item requirements consumed by ChestStealer and ShulkerGet.
 * Files intentionally use a separate extension so they do not conflict with slot-based .kit files.
 */
public class RegearCommand extends Command {
    public static final Map<String, Integer> currentKit = new LinkedHashMap<>();
    /**
     * The saved 0-35 player-inventory layout. Empty saved slots are present with an empty key and zero count.
     * Legacy .regear files do not contain layout entries, so this map remains empty when one is loaded.
     */
    public static final Map<Integer, LayoutSlot> currentLayout = new LinkedHashMap<>();

    public static final class LayoutSlot {
        public final String key;
        public final int count;

        public LayoutSlot(String key, int count) {
            this.key = key;
            this.count = count;
        }

        public boolean isEmpty() {
            return key.isEmpty() || count <= 0;
        }
    }

    public RegearCommand() {
        super("regear", "list | save <name> | load <name> | delete <name>");
    }

    @Override
    public void runCommand(String[] parameters) {
        if (parameters.length == 1 && "list".equalsIgnoreCase(parameters[0])) {
            File[] files = Manager.getFolder().listFiles((dir, name) -> name.endsWith(".regear"));
            if (files != null) {
                for (File file : files) {
                    sendChatMessage("Regear: [" + file.getName().substring(0, file.getName().length() - 7) + "]");
                }
            }
            return;
        }
        if (parameters.length != 2) {
            sendUsage();
            return;
        }

        String action = parameters[0].toLowerCase();
        File file = Manager.getFile(parameters[1] + ".regear");
        switch (action) {
            case "save" -> save(file, parameters[1]);
            case "load" -> load(file, parameters[1]);
            case "delete" -> {
                if (file.exists() && file.delete()) {
                    if (file.getName().equals(parameters[1] + ".regear")) {
                        currentKit.clear();
                        currentLayout.clear();
                    }
                    sendChatMessage("§fRegear [" + parameters[1] + "] removed");
                } else {
                    sendChatMessage("§cRegear [" + parameters[1] + "] not found");
                }
            }
            default -> sendUsage();
        }
    }

    private void save(File file, String name) {
        if (mc.player == null) return;
        Map<String, Integer> kit = new LinkedHashMap<>();
        Map<Integer, LayoutSlot> layout = new LinkedHashMap<>();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            addStack(kit, stack);
            layout.put(i, stack.isEmpty()
                    ? new LayoutSlot("", 0)
                    : new LayoutSlot(getKitKey(stack), stack.getCount()));
        }
        addStack(kit, mc.player.getOffHandStack());
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.println("# hyperion-regear-v2");
            for (int i = 0; i < 36; i++) {
                LayoutSlot slot = layout.get(i);
                writer.println("slot." + i + "=" + slot.key + "\t" + slot.count);
            }
            for (Map.Entry<String, Integer> entry : kit.entrySet()) {
                writer.println(entry.getKey() + "=" + entry.getValue());
            }
            currentKit.clear();
            currentKit.putAll(kit);
            currentLayout.clear();
            currentLayout.putAll(layout);
            sendChatMessage("§fRegear [" + name + "] saved");
        } catch (Exception exception) {
            sendChatMessage("§cRegear [" + name + "] save failed");
            exception.printStackTrace();
        }
    }

    private void load(File file, String name) {
        if (!loadKit(name)) {
            sendChatMessage("§cRegear [" + name + "] not found or invalid");
            return;
        }
        sendChatMessage("§fRegear [" + name + "] loaded (" + currentKit.size() + " item types"
                + (currentLayout.isEmpty() ? ", legacy counts" : ", fixed layout") + ")");
    }

    /**
     * Loads a named kit without requiring a chat command. Modules use this for their GUI Kit setting.
     *
     * @return true when the kit exists and was parsed successfully
     */
    public static boolean loadKit(String name) {
        File file = Manager.getFile(name + ".regear");
        if (!file.exists()) {
            currentKit.clear();
            currentLayout.clear();
            return false;
        }

        Map<String, Integer> kit = new LinkedHashMap<>();
        Map<Integer, LayoutSlot> layout = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int separator = line.lastIndexOf('=');
                if (separator <= 0) continue;
                String entry = line.substring(0, separator);
                String value = line.substring(separator + 1);
                if (entry.startsWith("slot.")) {
                    int slot = Integer.parseInt(entry.substring(5));
                    int countSeparator = value.lastIndexOf('\t');
                    if (slot < 0 || slot >= 36 || countSeparator < 0) continue;
                    String key = value.substring(0, countSeparator);
                    int count = Integer.parseInt(value.substring(countSeparator + 1));
                    if (key.isEmpty() || count <= 0) layout.put(slot, new LayoutSlot("", 0));
                    else layout.put(slot, new LayoutSlot(key, count));
                } else {
                    int count = Integer.parseInt(value);
                    if (count > 0) kit.put(entry, count);
                }
            }
            if (!layout.isEmpty() && kit.isEmpty()) {
                for (LayoutSlot slot : layout.values()) {
                    if (!slot.isEmpty()) kit.merge(slot.key, slot.count, Integer::sum);
                }
            }
            currentKit.clear();
            currentKit.putAll(kit);
            currentLayout.clear();
            currentLayout.putAll(layout);
            return true;
        } catch (Exception exception) {
            currentKit.clear();
            currentLayout.clear();
            exception.printStackTrace();
            return false;
        }
    }

    private static void addStack(Map<String, Integer> kit, ItemStack stack) {
        if (!stack.isEmpty()) kit.merge(getKitKey(stack), stack.getCount(), Integer::sum);
    }

    public static String getKitKey(ItemStack stack) {
        if (stack.isEmpty()) return "";
        String key = stack.getItem().getTranslationKey();
        if (stack.getItem() instanceof PotionItem) {
            PotionContentsComponent potion = stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);
            key += "#" + potion.getColor();
        }
        return key;
    }

    @Override
    public String[] getAutocorrect(int count, List<String> separated) {
        if (count != 1) return null;
        String input = separated.getLast().toLowerCase();
        return List.of("list", "save", "load", "delete").stream()
                .filter(option -> option.startsWith(input))
                .toArray(String[]::new);
    }
}
