package dev.g255.arc.mod.modules.impl.misc;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.core.impl.CommandManager;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Formatting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;

public class VisualRange extends Module {
   private final BooleanSetting join = this.add(new BooleanSetting("Join", true));
   private final BooleanSetting leave = this.add(new BooleanSetting("Leave", true));
   private final BooleanSetting players = this.add(new BooleanSetting("Players", true));
   private final BooleanSetting friends = this.add(new BooleanSetting("Friends", true));
   private final BooleanSetting enemies = this.add(new BooleanSetting("Enemies", true));
   private final BooleanSetting nakeds = this.add((new BooleanSetting("Nakeds", false)).setParent());
   private final BooleanSetting playSound = this.add(new BooleanSetting("PlaySound", true));
   private final BooleanSetting onlyEnemies = this.add(new BooleanSetting("OnlyEnemies", false));
   private final List<String> previous = new ArrayList();

   public VisualRange() {
      super("VisualRange", "Informs you of players who enter/leave your visual range.", Module.Category.Misc);
      this.setChinese("视野提示");
   }

   public void onDisable() {
      this.previous.clear();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!nullCheck()) {
         List<String> current = new ArrayList();

         for(PlayerEntity player : mc.world.getPlayers()) {
            if (player != mc.player && this.shouldTrack(player)) {
               current.add(player.getName().getString());
            }
         }

         for(String name : current) {
            if (!this.previous.contains(name) && this.join.getValue()) {
               String var10000 = String.valueOf(Formatting.WHITE);
               CommandManager.sendMessage(var10000 + name + String.valueOf(Formatting.WHITE) + " entered visual range.");
               this.notifySound();
            }
         }

         for(String name : this.previous) {
            if (!current.contains(name) && this.leave.getValue()) {
               String var9 = String.valueOf(Formatting.WHITE);
               CommandManager.sendMessage(var9 + name + String.valueOf(Formatting.WHITE) + " left visual range.");
               this.notifySound();
            }
         }

         this.previous.clear();
         this.previous.addAll(current);
      }
   }

   private boolean shouldTrack(PlayerEntity player) {
      boolean isFriend = Arc.FRIEND.isFriend(player);
      boolean isEnemy = !isFriend;
      if (isFriend) {
         return this.friends.getValue();
      } else if (this.nakeds.getValue() && player.getArmor() <= 0) {
         return true;
      } else if (!this.onlyEnemies.getValue()) {
         return this.players.getValue();
      } else {
         return this.enemies.getValue() && isEnemy;
      }
   }

   private void notifySound() {
      if (this.playSound.getValue()) {
         mc.world.playSound((PlayerEntity)null, mc.player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0F, 1.0F);
      }
   }
}
