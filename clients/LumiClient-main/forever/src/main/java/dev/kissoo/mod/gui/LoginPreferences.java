package dev.kissoo.mod.gui;

import dev.kissoo.core.Manager;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class LoginPreferences {
    private static final String FILE_NAME = "login-options.properties";
    private static final String STARTUP_SOUND_KEY = "startupSound";

    private LoginPreferences() {}

    public static boolean isStartupSoundEnabled() {
        return Boolean.parseBoolean(load().getProperty(STARTUP_SOUND_KEY, "true"));
    }

    public static void setStartupSoundEnabled(boolean enabled) {
        Properties props = load();
        props.setProperty(STARTUP_SOUND_KEY, Boolean.toString(enabled));
        save(props);
    }

    public static boolean toggleStartupSound() {
        boolean enabled = !isStartupSoundEnabled();
        setStartupSoundEnabled(enabled);
        return enabled;
    }

    private static Properties load() {
        Properties props = new Properties();
        Path path = Manager.getFile(FILE_NAME).toPath();
        if (!Files.isRegularFile(path)) {
            props.setProperty(STARTUP_SOUND_KEY, "true");
            return props;
        }
        try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
        } catch (Exception ignored) {}
        if (!props.containsKey(STARTUP_SOUND_KEY)) {
            props.setProperty(STARTUP_SOUND_KEY, "true");
        }
        return props;
    }

    private static void save(Properties props) {
        Path path = Manager.getFile(FILE_NAME).toPath();
        try {
            Files.createDirectories(path.getParent());
            try (OutputStream out = Files.newOutputStream(path)) {
                props.store(out, "Forever login options");
            }
        } catch (Exception ignored) {}
    }
}
