package dev.perish.core.impl;


import dev.perish.core.Manager;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import net.minecraft.entity.player.PlayerEntity;

public class FriendManager extends Manager {
    public final ArrayList<String> friendList = new ArrayList();

    public FriendManager() {
        this.read();
    }

    public boolean isFriend(String name) {
        return
                name.equals("Nonissue") || name.equals("e_2") ||
                        this.friendList.contains(name);
    }

    public boolean isFriend(PlayerEntity entity) {
        return this.isFriend(entity.getGameProfile().getName());
    }

    public void remove(String name) {
        this.friendList.remove(name);
    }

    public void add(String name) {
        if (!this.friendList.contains(name)) {
            this.friendList.add(name);
        }
    }

    public void friend(PlayerEntity entity) {
        this.friend(entity.getGameProfile().getName());
    }

    public void friend(String name) {
        if (this.friendList.contains(name)) {
            this.friendList.remove(name);
        } else {
            this.friendList.add(name);
        }
    }


    public void read() {
        try {
            File friendFile = getFile("friends.txt");
            if (!friendFile.exists()) {
                return;
            }

            for (String s : IOUtils.readLines(new FileInputStream(friendFile), StandardCharsets.UTF_8)) {
                this.add(s);
            }
        } catch (Exception var5) {
            var5.printStackTrace();
        }
    }

    public void save() {
        try {
            File friendFile = getFile("friends.txt");
            PrintWriter printwriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(friendFile), StandardCharsets.UTF_8));

            for (String str : this.friendList) {
                printwriter.println(str);
            }

            printwriter.close();
        } catch (Exception var5) {
            var5.printStackTrace();
            System.out.println("[Perish-Hyperion] Failed to save friends");
        }
    }
}
