package dev.g255.arc.mod.modules.impl.combat;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.TotemEvent;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import dev.g255.arc.mod.modules.settings.impl.StringSetting;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public class PopEz extends Module {
   public static PopEz INSTANCE;
   private final SliderSetting randomChars = this.add(new SliderSetting("RandomChars", (double)3.0F, (double)0.0F, (double)20.0F, (double)1.0F));
   public final BooleanSetting slowSend = this.add(new BooleanSetting("SlowSend", false));
   public final StringSetting customMessage = this.add(new StringSetting("CustomMessage", ""));
   private final Random random = new Random();
   private final Timer timer = new Timer();
   private final Map<Integer, Integer> popQueue = new HashMap();

   public PopEz() {
      super("PopEz", Module.Category.Combat);
      this.setChinese("POP嘲讽");
      INSTANCE = this;
   }

   @EventListener
   public void onTotem(TotemEvent event) {
      PlayerEntity player = event.getPlayer();
      if (player != null && player != mc.player && !Arc.FRIEND.isFriend(player)) {
         int count = Arc.POP.getPop(player);
         if (count <= 0) {
            count = 1;
         }

         if (this.slowSend.getValue()) {
            this.popQueue.put(player.getId(), count);
         } else {
            this.sendPopMessage(player, count);
         }
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

         for(Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity) {
               PlayerEntity p = (PlayerEntity)entity;
               if (p.getId() == playerId) {
                  player = p;
                  break;
               }
            }
         }

         if (player != null) {
            this.sendPopMessage(player, popCount);
         }

         this.popQueue.remove(playerId);
      }
   }

   private void sendPopMessage(PlayerEntity player, int count) {
      String raw = this.customMessage.getValue();
      String name = player.getName().getString();
      String message;
      if (raw.isEmpty()) {
         message = name + " has popped " + count + (count == 1 ? " totem." : " totems.");
      } else {
         message = raw.replace("%player%", name).replace("%pop%", String.valueOf(count));
      }

      String rand = this.generateRandomString(this.randomChars.getValueInt());
      if (!rand.isEmpty()) {
         message = message + " " + rand;
      }

      mc.getNetworkHandler().sendChatMessage(message);
   }

   private String generateRandomString(int length) {
      StringBuilder sb = new StringBuilder(length);

      for(int i = 0; i < length; ++i) {
         sb.append("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".charAt(this.random.nextInt("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".length())));
      }

      return sb.toString();
   }
}
