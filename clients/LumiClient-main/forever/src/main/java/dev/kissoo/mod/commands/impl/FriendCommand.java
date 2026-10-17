package dev.kissoo.mod.commands.impl;

import dev.kissoo.Forever;
import dev.kissoo.core.impl.CommandManager;
import dev.kissoo.mod.commands.Command;
import dev.kissoo.mod.modules.impl.misc.Friend;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.List;

public class FriendCommand extends Command {
   public FriendCommand() {
      super("friend", "[list] | [add/del] [name]");
   }

   @Override
   public void runCommand(String[] parameters) {
      if (parameters.length == 0) {
         this.sendUsage();
         return;
      }

      String action = parameters[0].toLowerCase();

      switch (action) {
         case "list":
            if (Forever.FRIEND.friendList.isEmpty()) {
               this.sendChatMessage("§fFriends list is empty");
               return;
            }

            List<String> friends = new ArrayList<>(Forever.FRIEND.friendList);
            StringBuilder message = new StringBuilder("§eFriends §a");

            for (int i = 0; i < friends.size(); i++) {
               if (i > 0) message.append(", ");
               message.append(friends.get(i));
               if ((i + 1) % 4 == 0 || i == friends.size() - 1) {
                  this.sendChatMessage(message.toString());
                  message = new StringBuilder("§a");
               }
            }
            break;

         case "add":
            if (parameters.length != 2) {
               this.sendUsage();
               return;
            }

            String addName = parameters[1];
            boolean wasAlreadyFriend = Forever.FRIEND.isFriend(addName);

            if (wasAlreadyFriend) {
               this.sendChatMessage("§f" + addName + " §eis already a friend");
               return;
            }
            Forever.FRIEND.add(addName);
            if (Friend.INSTANCE != null && Friend.INSTANCE.isOn()) {
               playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.2F);
            }
            CommandManager.sendMessage("§d♡ §a已添加好友§f: " + addName + " §d♡ ﾐャ~");
            if (Friend.INSTANCE != null && Friend.INSTANCE.shouldSendMessage()) {
               Friend.INSTANCE.scheduleFriendMessage(addName, true);
            }
            break;

         case "del":
            if (parameters.length != 2) {
               this.sendUsage();
               return;
            }

            String removeName = parameters[1];
            boolean wasRemoved = Forever.FRIEND.isFriend(removeName);

            if (!wasRemoved) {
               this.sendChatMessage("§f" + removeName + " §ewas not a friend");
               return;
            }
            Forever.FRIEND.remove(removeName);
            if (Friend.INSTANCE != null && Friend.INSTANCE.isOn()) {
               playSound(SoundEvents.ENTITY_ITEM_BREAK, 1.0F, 0.8F);
            }
            CommandManager.sendMessage("§7✖ §c已移除好友§f: " + removeName + " §7✖ 呜呜...");
            if (Friend.INSTANCE != null && Friend.INSTANCE.shouldSendMessage()) {
               Friend.INSTANCE.scheduleFriendMessage(removeName, false);
            }
            break;

         default:
            if (parameters.length == 1) {
               this.sendChatMessage("§f" + parameters[0] +
                       (Forever.FRIEND.isFriend(parameters[0])
                               ? " §ais friended"
                               : " §cisn't friended"));
            } else {
               this.sendUsage();
            }
      }
   }

   private void playSound(net.minecraft.sound.SoundEvent sound, float volume, float pitch) {
      if (mc.world != null && mc.player != null) {
         mc.world.playSound(
                 mc.player,
                 mc.player.getBlockPos(),
                 sound,
                 SoundCategory.PLAYERS,
                 volume,
                 pitch
         );
      }
   }

   @Override
   public String[] getAutocorrect(int count, List<String> separated) {
      if (separated.isEmpty()) return null;

      String input = separated.get(separated.size() - 1).toLowerCase();

      if (count == 1) {
         List<String> correct = new ArrayList<>();
         for (String x : List.of("add", "del", "list")) {
            if (input.equalsIgnoreCase(Forever.getPrefix() + "friend") || x.toLowerCase().startsWith(input)) {
               correct.add(x);
            }
         }
         return correct.toArray(new String[0]);
      }

      if (count == 2) {
         String action = separated.get(0).toLowerCase();

         if (action.equals("add") && mc.world != null) {
            return mc.world.getPlayers().stream()
                    .map(p -> p.getGameProfile().getName())
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .toArray(String[]::new);
         }

         if (action.equals("del")) {
            return Forever.FRIEND.friendList.stream()
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .toArray(String[]::new);
         }
      }

      return null;
   }
}