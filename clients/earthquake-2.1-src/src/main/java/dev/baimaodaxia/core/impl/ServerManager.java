package dev.baimaodaxia.core.impl;

import dev.baimaodaxia.earthquake;
import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.GameLeftEvent;
import dev.baimaodaxia.api.events.impl.PacketEvent;
import dev.baimaodaxia.api.utils.Wrapper;
import dev.baimaodaxia.api.utils.math.MathUtil;
import dev.baimaodaxia.api.utils.math.Timer;
import dev.baimaodaxia.mod.modules.impl.client.AntiCheat;
import dev.baimaodaxia.mod.modules.impl.client.Fonts;
import dev.baimaodaxia.mod.modules.impl.combat.AutoLog;
import dev.baimaodaxia.mod.modules.impl.combat.Criticals;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import dev.baimaodaxia.api.utils.InteractTypeUtil;
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
      earthquake.EVENT_BUS.subscribe(this);
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
         if (!(packet instanceof HandSwingC2SPacket) && (!(packet instanceof PlayerInteractEntityC2SPacket) || Criticals.getInteractType((PlayerInteractEntityC2SPacket)packet) != InteractTypeUtil.ATTACK)) {
            if (packet instanceof UpdateSelectedSlotC2SPacket) {
               UpdateSelectedSlotC2SPacket packet2 = (UpdateSelectedSlotC2SPacket)packet;
               if (this.lastSlot != packet2.getSelectedSlot()) {
                  this.lastSlot = packet2.getSelectedSlot();
                  mc.player.resetLastAttackedTicks();
               }
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
      Packet var3 = event.getPacket();
      if (var3 instanceof UpdateSelectedSlotC2SPacket packet) {
         int packetSlot = packet.getSelectedSlot();
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

   public void onUpdate() {
      if (mc.player == null) {
         this.playerNull.reset();
      }

      if (this.worldNull && mc.world != null) {
         Fonts.INSTANCE.refresh();
         AutoLog.loggedOut = false;
         earthquake.MODULE.onLogin();
         this.worldNull = false;
      } else if (!this.worldNull && mc.world == null) {
         earthquake.save();
         earthquake.MODULE.onLogout();
         this.worldNull = true;
      }

   }
}
