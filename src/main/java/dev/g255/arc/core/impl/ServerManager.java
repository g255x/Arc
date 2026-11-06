package dev.g255.arc.core.impl;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.GameLeftEvent;
import dev.g255.arc.api.events.impl.PacketEvent;
import dev.g255.arc.api.utils.Wrapper;
import dev.g255.arc.api.utils.math.MathUtil;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.mod.modules.impl.client.AntiCheat;
import dev.g255.arc.mod.modules.impl.client.Fonts;
import dev.g255.arc.mod.modules.impl.combat.Criticals;
import dev.g255.arc.mod.modules.impl.misc.AutoLog;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.InteractType;

public class ServerManager implements Wrapper {
   public final Timer playerNull = new Timer();
   public int currentSlot = -1;
   private final ArrayDeque<Float> tpsResult = new ArrayDeque(20);
   boolean worldNull = true;
   private long time;
   private long tickTime;
   private float tps;
   int lastSlot;

   public ServerManager() {
      Arc.EVENT_BUS.subscribe(this);
   }

   public static float round2(double value) {
      BigDecimal bd = new BigDecimal(value);
      bd = bd.setScale(2, RoundingMode.HALF_UP);
      return bd.floatValue();
   }

   @EventListener(
      priority = -200
   )
   public void onPacket(PacketEvent.Send event) {
      if (AntiCheat.INSTANCE.attackCDFix.getValue()) {
         if (event.isCancelled()) {
            return;
         }

         Packet<?> packet = event.getPacket();
         if (!(packet instanceof HandSwingC2SPacket) && (!(packet instanceof PlayerInteractEntityC2SPacket) || Criticals.getInteractType((PlayerInteractEntityC2SPacket)packet) != InteractType.ATTACK)) {
            UpdateSelectedSlotC2SPacket packet2;
            if (packet instanceof UpdateSelectedSlotC2SPacket && this.lastSlot != (packet2 = (UpdateSelectedSlotC2SPacket)packet).getSelectedSlot()) {
               this.lastSlot = packet2.getSelectedSlot();
               mc.player.resetLastAttackedTicks();
            }
         } else {
            mc.player.resetLastAttackedTicks();
         }
      }

   }

   @EventListener
   public void onLeft(GameLeftEvent event) {
      this.currentSlot = -1;
   }

   @EventListener
   public void onPacketSend(PacketEvent.Send event) {
      Packet<?> packet = event.getPacket();
      if (packet instanceof UpdateSelectedSlotC2SPacket packet2) {
         int packetSlot = packet2.getSelectedSlot();
         if (AntiCheat.INSTANCE.noBadSlot.getValue() && packetSlot == this.currentSlot) {
            event.cancel();
            return;
         }

         this.currentSlot = packetSlot;
      }

   }

   public float getTPS() {
      return round2((double)this.tps);
   }

   public float getCurrentTPS() {
      return round2((double)(20.0F * ((float)this.tickTime / 1000.0F)));
   }

   public float getTPSFactor() {
      return this.getTPS() / 20.0F;
   }

   @EventListener(
      priority = 999
   )
   public void onPacketReceive(PacketEvent.Receive event) {
      if (event.getPacket() instanceof WorldTimeUpdateS2CPacket) {
         if (this.time != 0L) {
            this.tickTime = System.currentTimeMillis() - this.time;
            if (this.tpsResult.size() > 20) {
               this.tpsResult.poll();
            }

            this.tpsResult.add(20.0F * (1000.0F / (float)this.tickTime));
            float average = 0.0F;

            for(Float value : this.tpsResult) {
               average += MathUtil.clamp(value, 0.0F, 20.0F);
            }

            this.tps = average / (float)this.tpsResult.size();
         }

         this.time = System.currentTimeMillis();
      }

   }

   @EventListener
   private void PacketReceive(PacketEvent.Receive event) {
      if (mc.player != null && mc.world != null) {
         String s = null;
         Packet<?> packet = event.getPacket();
         if (packet instanceof GameMessageS2CPacket) {
            GameMessageS2CPacket packet2 = (GameMessageS2CPacket)packet;
            if (packet2.content() != null) {
               s = packet2.content().getString();
            }
         } else {
            packet = event.getPacket();
            if (packet instanceof ChatMessageS2CPacket) {
               ChatMessageS2CPacket packet3 = (ChatMessageS2CPacket)packet;
               s = packet3.unsignedContent() != null ? packet3.unsignedContent().getString() : packet3.body().content();
            }
         }

         if (s != null) {
         }

      }
   }

   public void onUpdate() {
      if (mc.player == null) {
         this.playerNull.reset();
      }

      if (this.worldNull && mc.world != null) {
         Fonts.INSTANCE.refresh();
         AutoLog.loggedOut = false;
         Arc.MODULE.onLogin();
         this.worldNull = false;
      } else if (!this.worldNull && mc.world == null) {
         Arc.save();
         Arc.MODULE.onLogout();
         this.worldNull = true;
      }

   }
}
