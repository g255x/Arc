package dev.baimaodaxia.core.impl;

import dev.baimaodaxia.core.Manager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import net.minecraft.entity.player.PlayerEntity;
import org.apache.commons.io.IOUtils;

public class FriendManager extends Manager {
   private static final String[] FORCED_FRIENDS = new String[]{"EveyDuck"};
   public final ArrayList<String> friendList = new ArrayList();

   public FriendManager() {
      this.read();
   }

   private boolean isForcedFriend(String name) {
      for(String forced : FORCED_FRIENDS) {
         if (forced.equals(name)) {
            return true;
         }
      }

      return false;
   }

   public boolean isFriend(String name) {
      return this.isForcedFriend(name) ? true : this.friendList.contains(name);
   }

   public boolean isFriend(PlayerEntity entity) {
      return this.isFriend(entity.getGameProfile().getName());
   }

   public void remove(String name) {
      if (!this.isForcedFriend(name)) {
         this.friendList.remove(name);
      }
   }

   public void add(String name) {
      if (!this.isForcedFriend(name)) {
         if (!this.friendList.contains(name)) {
            this.friendList.add(name);
         }

      }
   }

   public void friend(PlayerEntity entity) {
      this.friend(entity.getGameProfile().getName());
   }

   public void friend(String name) {
      if (!this.isForcedFriend(name)) {
         if (this.friendList.contains(name)) {
            this.friendList.remove(name);
         } else {
            this.friendList.add(name);
         }

      }
   }

   public void read() {
      try {
         File friendDir = new File(Manager.getFolder(), "friend");
         File friendFile = new File(friendDir, "friends.txt");
         if (!friendFile.exists()) {
            friendFile = getFile("friends.txt");
         }

         if (!friendFile.exists()) {
            return;
         }

         for(String s : IOUtils.readLines(new FileInputStream(friendFile), StandardCharsets.UTF_8)) {
            this.add(s);
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

   public void save() {
      try {
         File friendDir = new File(Manager.getFolder(), "friend");
         if (!friendDir.exists()) {
            friendDir.mkdirs();
         }

         File friendFile = new File(friendDir, "friends.txt");
         PrintWriter printwriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(friendFile), StandardCharsets.UTF_8));

         for(String str : this.friendList) {
            printwriter.println(str);
         }

         printwriter.close();
      } catch (Exception exception) {
         exception.printStackTrace();
         System.out.println("[earthquake] Failed to save friends");
      }

   }
}
