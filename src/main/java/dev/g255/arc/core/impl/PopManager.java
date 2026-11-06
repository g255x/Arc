package dev.g255.arc.core.impl;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.DeathEvent;
import dev.g255.arc.api.events.impl.PacketEvent;
import dev.g255.arc.api.events.impl.TotemEvent;
import dev.g255.arc.api.utils.Wrapper;
import dev.g255.arc.mod.modules.Module;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.client.network.AbstractClientPlayerEntity;

public class PopManager implements Wrapper {
   public final HashMap<String, Integer> popContainer = new HashMap();
   private final List<PlayerEntity> deadPlayer = new ArrayList();

   public PopManager() {
      this.init();
   }

   public void init() {
      Arc.EVENT_BUS.subscribe(this);
   }

   public int getPop(String s) {
      return (Integer)this.popContainer.getOrDefault(s, 0);
   }

   public int getPop(PlayerEntity player) {
      return this.getPop(player.getName().getString());
   }

   public void onUpdate() {
      if (!Module.nullCheck()) {
         for(AbstractClientPlayerEntity player : Arc.THREAD.getPlayers()) {
            if (player != null && player.isDead()) {
               if (!this.deadPlayer.contains(player)) {
                  Arc.EVENT_BUS.post(DeathEvent.get(player));
                  this.onDeath(player);
                  this.deadPlayer.add(player);
               }
            } else {
               this.deadPlayer.remove(player);
            }
         }

      }
   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive event) {
      if (!Module.nullCheck()) {
         Packet<?> packet2 = event.getPacket();
         Entity entity;
         EntityStatusS2CPacket packet;
         if (packet2 instanceof EntityStatusS2CPacket && (packet = (EntityStatusS2CPacket)packet2).getStatus() == 35 && (entity = packet.getEntity(mc.world)) instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity)entity;
            this.onTotemPop(player);
         }

      }
   }

   public void onDeath(PlayerEntity player) {
      this.popContainer.remove(player.getName().getString());
   }

   public void onTotemPop(PlayerEntity player) {
      int l_Count = 1;
      if (this.popContainer.containsKey(player.getName().getString())) {
         l_Count = (Integer)this.popContainer.get(player.getName().getString());
         HashMap var10000 = this.popContainer;
         String var10001 = player.getName().getString();
         ++l_Count;
         var10000.put(var10001, l_Count);
      } else {
         this.popContainer.put(player.getName().getString(), l_Count);
      }

      Arc.EVENT_BUS.post(TotemEvent.get(player));
   }
}
