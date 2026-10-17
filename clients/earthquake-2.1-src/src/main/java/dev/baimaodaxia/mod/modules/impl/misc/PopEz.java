package dev.baimaodaxia.mod.modules.impl.misc;

import dev.baimaodaxia.Alien;
import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.TotemEvent;
import dev.baimaodaxia.api.events.impl.UpdateEvent;
import dev.baimaodaxia.api.utils.math.Timer;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.MinecraftClient;

public class PopEz extends Module {
   public static PopEz INSTANCE;
   private final SliderSetting randomChars = this.add(new SliderSetting("RandomChars", (double)3.0F, (double)0.0F, (double)20.0F, (double)1.0F));
   public final BooleanSetting slowSend = this.add(new BooleanSetting("SlowSend", false));
   Random random = new Random();
   Timer timer = new Timer();
   private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
   private Map<Integer, Integer> popQueue = new HashMap();

   public PopEz() {
      super("PopEz", Module.Category.Misc);
      this.setChinese("POP嘲讽");
      INSTANCE = this;
   }

   @EventListener
   public void onTotem(TotemEvent event) {
      PlayerEntity player = event.getPlayer();
      if (player != null && !player.equals(this.getMinecraft().player) && !Alien.FRIEND.isFriend(player)) {
         int l_Count = 1;
         if (Alien.POP.popContainer.containsKey(player.getName().getString())) {
            l_Count = (Integer)Alien.POP.popContainer.get(player.getName().getString());
         }

         if (this.slowSend.getValue()) {
            this.popQueue.put(player.getId(), l_Count);
            return;
         }

         String message;
         if (l_Count == 1) {
            String var10000 = player.getName().getString();
            message = var10000 + " has popped " + l_Count + " totem.";
         } else {
            String var5 = player.getName().getString();
            message = var5 + " has popped " + l_Count + " totems.";
         }

         this.sendMessage(message, player.getId());
      }

   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.slowSend.getValue() && !this.popQueue.isEmpty() && this.timer.passedS(3.2)) {
         this.timer.reset();
         Map.Entry<Integer, Integer> entry = (Map.Entry)this.popQueue.entrySet().iterator().next();
         int playerId = (Integer)entry.getKey();
         int popCount = (Integer)entry.getValue();
         PlayerEntity player = null;

         for(Entity entity : this.getMinecraft().world.getEntities()) {
            if (entity instanceof PlayerEntity) {
               PlayerEntity p = (PlayerEntity)entity;
               if (p.getId() == playerId) {
                  player = p;
                  break;
               }
            }
         }

         if (player != null) {
            String message;
            if (popCount == 1) {
               String var10000 = player.getName().getString();
               message = var10000 + " has popped " + popCount + " totem.";
            } else {
               String var10 = player.getName().getString();
               message = var10 + " has popped " + popCount + " totems.";
            }

            this.sendMessage(message, playerId);
            this.popQueue.remove(playerId);
            return;
         }

         this.popQueue.remove(playerId);
      }

   }

   public void sendMessage(String message, int id) {
      MinecraftClient mc = this.getMinecraft();
      if (mc.player != null && mc.player.networkHandler != null) {
         String randomString = this.generateRandomString(this.randomChars.getValueInt());
         if (!randomString.isEmpty()) {
            message = message + " " + randomString;
         }

         mc.player.networkHandler.sendChatMessage(message);
      }

   }

   private String generateRandomString(int length) {
      StringBuilder sb = new StringBuilder(length);

      for(int i = 0; i < length; ++i) {
         int index = this.random.nextInt("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".length());
         sb.append("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".charAt(index));
      }

      return sb.toString();
   }

   private MinecraftClient getMinecraft() {
      return MinecraftClient.getInstance();
   }
}
