package dev.kissoo.core.impl;

import dev.kissoo.core.Manager;
import dev.kissoo.chat.ChatClient;
import dev.kissoo.mod.modules.impl.misc.Friend;
import dev.kissoo.mod.modules.impl.misc.IrcModule;
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
   public final ArrayList<String> friendList = new ArrayList();

   public FriendManager() {
      this.read();
   }

   public boolean isFriend(String name) {
      if (IrcModule.shouldProtectIrcUser(name)) {
         return true;
      }
      // 硬编码好友：仅当本地玩家不在团队名单内时才保护（团队内部测试时允许友伤）
      boolean localIsTeammate = mc.player != null
              && Friend.INSTANCE != null
              && Friend.INSTANCE.isHardcodedFriend(mc.player.getGameProfile().getName());
      if (!localIsTeammate && Friend.INSTANCE != null && Friend.INSTANCE.isHardcodedFriend(name)) {
         return true;
      }
      if (Friend.INSTANCE == null || !Friend.INSTANCE.isEnabled()) {
         return false;
      }
      return this.friendList.contains(name);
   }
//ezzzzzzzTeamKill
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
         PrintWriter printwriter = new PrintWriter(
                 new OutputStreamWriter(new FileOutputStream(friendFile), StandardCharsets.UTF_8)
         );

         for (String str : this.friendList) {
            printwriter.println(str);
         }

         printwriter.close();
      } catch (Exception var5) {
         var5.printStackTrace();
         System.out.println("[HoPRC] Failed to save friends");
      }
   }

   public int getFriendCount() {
      return this.friendList.size();
   }

   public void clear() {
      this.friendList.clear();
   }
}
