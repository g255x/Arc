package dev.kissoo.mod.gui.background;

import dev.kissoo.mod.modules.impl.client.ClientSetting;
import net.minecraft.client.MinecraftClient;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Reads the saved menu mode before ClientSetting and ConfigManager exist. */
public final class MainMenuModePreference {
    private static final String CONFIG_KEY = "ClientSetting_MainMenuBg:";
    private static Boolean savedPhobosMode;

    private MainMenuModePreference() {
    }

    public static boolean isPhobosSelected() {
        if (ClientSetting.INSTANCE != null) {
            return ClientSetting.INSTANCE.mainMenuBg.getValue() == ClientSetting.MainMenuBg.PHOBOS;
        }
        if (savedPhobosMode == null) {
            savedPhobosMode = readSavedMode();
        }
        return savedPhobosMode;
    }

    private static boolean readSavedMode() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            Path runDirectory = client != null && client.runDirectory != null
                    ? client.runDirectory.toPath()
                    : Path.of(System.getProperty("user.dir", "."));
            Path options = runDirectory.resolve("lyrae").resolve("options.txt");
            if (!Files.isRegularFile(options)) {
                return true;
            }
            for (String line : Files.readAllLines(options, StandardCharsets.UTF_8)) {
                if (line.startsWith(CONFIG_KEY)) {
                    return "PHOBOS".equalsIgnoreCase(line.substring(CONFIG_KEY.length()).trim());
                }
            }
        } catch (Throwable throwable) {
            System.err.println("[Forever] Could not read startup main-menu mode: " + throwable.getMessage());
        }
        return true;
    }
}
