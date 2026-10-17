package dev.kissoo.mod.modules.impl.misc;


import dev.kissoo.Forever;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.core.impl.CommandManager;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BindSetting;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;

public class Friend extends Module {
   public static Friend INSTANCE;

   private final BindSetting addFriend = this.add(new BindSetting("AddFriend", -1));
   private final BooleanSetting sendMessage = this.add(new BooleanSetting("SendMessage", false));
   private final BooleanSetting sound = this.add(new BooleanSetting("Sound", true));

   private boolean pressed = false;
   private String pendingPlayer = null;
   private boolean pendingIsAdd = false;
   private int messageDelay = 0;

   public Friend() {
      super("Friend", Module.Category.Misc);
      this.setChinese("好友");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      if (nullCheck()) {
         return;
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (nullCheck()) {
         return;
      }

      if (pendingPlayer != null && messageDelay > 0) {
         messageDelay--;
         if (messageDelay == 0) {
            sendPrivateMessage(pendingPlayer, pendingIsAdd);
            pendingPlayer = null;
         }
      }

      if (this.addFriend.isPressed() && mc.currentScreen == null) {
         if (!this.pressed) {
            if (mc.crosshairTarget instanceof EntityHitResult entityHitResult
                    && entityHitResult.getEntity() instanceof PlayerEntity player) {

               String playerName = player.getGameProfile().getName();

               Forever.FRIEND.friend(player);
               boolean isNowFriend = Forever.FRIEND.friendList.contains(playerName);

               if (this.sound.getValue()) {
                  if (isNowFriend) {
                     mc.world.playSound(
                             mc.player,
                             mc.player.getBlockPos(),
                             SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                             SoundCategory.PLAYERS,
                             1.0F,
                             1.2F
                     );
                  } else {
                     mc.world.playSound(
                             mc.player,
                             mc.player.getBlockPos(),
                             SoundEvents.ENTITY_ITEM_BREAK,
                             SoundCategory.PLAYERS,
                             1.0F,
                             0.8F
                     );
                  }
               }

               if (isNowFriend) {
                  CommandManager.sendMessage("§d♡ §a已添加好友§f: " + playerName + " §d♡ ﾐャ~");
               } else {
                  CommandManager.sendMessage("§7✖ §c已移除好友§f: " + playerName + " §7✖ 呜呜...");
               }

               if (this.sendMessage.getValue()) {
                  scheduleFriendMessage(playerName, isNowFriend);
               }
            }
         }
         this.pressed = true;
      } else {
         this.pressed = false;
      }
   }

   public void scheduleFriendMessage(String playerName, boolean isAdd) {
      pendingPlayer = playerName;
      pendingIsAdd = isAdd;
      messageDelay = 10; // 延迟10 tick (0.5秒)
   }

   public boolean shouldSendMessage() {
      return this.isOn() && this.sendMessage.getValue();
   }

   private void sendPrivateMessage(String playerName, boolean isAdd) {
      try {
         if (mc.getNetworkHandler() == null) {
            return;
         }

         String message;
         if (isAdd) {
            message = "你真的好可爱呀！✿ ヾ(≧▽≦*)o 猫猫超级喜欢你的说~ "
                    + "所以猫猫刚刚偷偷把你加进好友列表啦~ (๑>ᴗ<๑) "
                    + "希望我们能成为好朋友喵~ ♡";
         } else {
            message = "你让猫猫好失望呢... (｡•́︿•̀｡) "
                    + "猫猫的小心心碎成了好多片... "
                    + "猫猫决定不把你当作F了喵... ಥ_ಥ 拜拜啦...";
         }

         mc.getNetworkHandler().sendChatCommand("w " + playerName + " " + message);

         CommandManager.sendMessage(
                 isAdd
                         ? "§d✉ §f已向 §a" + playerName + " §f发送了可爱的消息~ §d喵♡"
                         : "§7✉ §f已向 §c" + playerName + " §f发送了告别消息... §7呜..."
         );
      } catch (Exception e) {
         CommandManager.sendMessage("§c发送私信失败了喵... (๑•́ ₃ •̀๑)");
      }
   }

   public boolean isHardcodedFriend(String name) {
      return name.equalsIgnoreCase("Yoze_awa")
              || name.equalsIgnoreCase("Auto_Anchor")
              || name.equalsIgnoreCase("FlowerASleep")
              || name.equalsIgnoreCase("HoPRCandCHI")
              || name.equalsIgnoreCase("Prilia99")
              || name.equalsIgnoreCase("Xx_qingxiang_xX");
   }

   @Override
   public void onDisable() {
      this.pressed = false;
      this.pendingPlayer = null;
      this.messageDelay = 0;
   }
}