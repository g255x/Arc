package dev.kissoo.core.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class UserConfig {
    private static final File CONFIG_FILE = new File("config/forever_user.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private String lastUsername = "";
    private long lastLoginTime = 0;

    public static UserConfig load() {
        if (!CONFIG_FILE.exists()) {
            return new UserConfig();
        }
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            return GSON.fromJson(reader, UserConfig.class);
        } catch (Exception e) {
            return new UserConfig();
        }
    }

    public void save() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getLastUsername() { return lastUsername; }
    public void setLastUsername(String username) { this.lastUsername = username; }
}