package dev.g255.arc.core.impl;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.BlockBreakingProgressEvent;
import dev.g255.arc.api.events.impl.ClientTickEvent;
import dev.g255.arc.api.events.impl.PacketEvent;
import dev.g255.arc.api.events.impl.ServerConnectBeginEvent;
import dev.g255.arc.api.utils.Wrapper;
import dev.g255.arc.api.utils.math.FadeUtils;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.impl.client.AntiCheat;
import dev.g255.arc.mod.modules.impl.player.PacketMine;
import dev.g255.arc.mod.modules.impl.render.BreakESP;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.util.math.MathHelper;

public class BreakManager implements Wrapper {
   public final ConcurrentHashMap<Integer, BreakData> breakMap = new ConcurrentHashMap();
   public final ConcurrentHashMap<Integer, BreakData> doubleMap = new ConcurrentHashMap();

   public BreakManager() {
      Arc.EVENT_BUS.subscribe(this);
   }

   @EventListener
   public void onServerConnectBegin(ServerConnectBeginEvent event) {
      this.breakMap.clear();
      this.doubleMap.clear();
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!Module.nullCheck()) {
         if (AntiCheat.INSTANCE.detectDouble.getValue()) {
            for(int i : Arc.BREAK.doubleMap.keySet()) {
               BreakData breakData = (BreakData)Arc.BREAK.doubleMap.get(i);
               if (breakData == null || breakData.getEntity() == null || mc.world.isAir(breakData.pos) || breakData.timer.passedMs(Math.max(AntiCheat.INSTANCE.minTimeout.getValue() * (double)1000.0F, breakData.breakTime * AntiCheat.INSTANCE.doubleMineTimeout.getValue()))) {
                  Arc.BREAK.doubleMap.remove(i);
               }
            }
         }

         for(BreakData breakData : this.breakMap.values()) {
            breakData.breakTime = Math.max(BreakESP.getBreakTime(breakData.pos, false), (double)50.0F);
            if (PacketMine.unbreakable(breakData.pos)) {
               breakData.fade.setLength(0L);
               breakData.complete = false;
               breakData.failed = true;
            } else if (mc.world.isAir(breakData.pos)) {
               breakData.fade.setLength(0L);
               breakData.complete = true;
               breakData.failed = false;
            } else if (!breakData.complete && breakData.timer.passedMs(breakData.breakTime * AntiCheat.INSTANCE.breakTimeout.getValue())) {
               breakData.fade.setLength(0L);
               breakData.failed = true;
            } else {
               breakData.fade.setLength((long)breakData.breakTime);
            }
         }

      }
   }

   @EventListener
   public void onPacket(PacketEvent.Receive event) {
      if (!Module.nullCheck()) {
         Packet<?> packet = event.getPacket();
         if (packet instanceof BlockBreakingProgressS2CPacket) {
            BlockBreakingProgressS2CPacket packet2 = (BlockBreakingProgressS2CPacket)packet;
            if (packet2.getPos() == null) {
               return;
            }

            BreakData breakData = new BreakData(packet2.getPos(), packet2.getEntityId(), false);
            if (breakData.getEntity() == null) {
               return;
            }

            if (MathHelper.sqrt((float)breakData.getEntity().getEyePos().squaredDistanceTo(packet2.getPos().toCenterPos())) > 8.0F) {
               return;
            }

            if (AntiCheat.INSTANCE.detectDouble.getValue() && packet2.getProgress() != 255) {
               if (packet2.getProgress() != 0) {
                  BreakData doublePos = (BreakData)this.doubleMap.get(packet2.getEntityId());
                  if (doublePos != null) {
                     doublePos.pos = packet2.getPos();
                     doublePos.timer.reset();
                  } else if (!PacketMine.unbreakable(packet2.getPos())) {
                     this.doubleMap.put(packet2.getEntityId(), new BreakData(packet2.getPos(), packet2.getEntityId(), true));
                  }

                  return;
               }

               BreakData doublePos = (BreakData)this.doubleMap.get(packet2.getEntityId());
               if (doublePos != null && doublePos.pos.equals(packet2.getPos()) && !doublePos.timer.passedS((double)150.0F)) {
                  return;
               }
            }

            BreakData current;
            if ((current = (BreakData)this.breakMap.get(packet2.getEntityId())) != null && !current.failed && current.pos.equals(packet2.getPos())) {
               return;
            }

            this.breakMap.put(packet2.getEntityId(), breakData);
            Arc.EVENT_BUS.post(BlockBreakingProgressEvent.get(packet2.getPos(), packet2.getEntityId(), packet2.getProgress()));
            if (AntiCheat.INSTANCE.detectDouble.getValue() && !this.doubleMap.containsKey(packet2.getEntityId()) && !PacketMine.unbreakable(packet2.getPos())) {
               this.doubleMap.put(packet2.getEntityId(), new BreakData(packet2.getPos(), packet2.getEntityId(), true));
            }
         }

      }
   }

   public boolean isMining(BlockPos pos) {
      return this.isMining(pos, true);
   }

   /** 找这个格子上正在进行的挖掘记录（忽略已失败的），没有则 null。 */
   public BreakData getBreakData(BlockPos pos) {
      for(BreakData data : this.breakMap.values()) {
         if (data.getEntity() != null && !data.failed && data.pos.equals(pos)) {
            return data;
         }
      }

      return null;
   }

   public boolean isMining(BlockPos pos, boolean self) {
      if (self && PacketMine.getBreakPos() != null && PacketMine.getBreakPos().equals(pos)) {
         return true;
      } else {
         for(BreakData breakData : this.breakMap.values()) {
            if (breakData.getEntity() != null && !(breakData.getEntity().getEyePos().distanceTo(pos.toCenterPos()) > (double)7.0F) && !breakData.failed && breakData.pos.equals(pos)) {
               return true;
            }
         }

         return false;
      }
   }

   public static class BreakData {
      public BlockPos pos;
      private final int entityId;
      public final FadeUtils fade;
      public final Timer timer;
      public double breakTime;
      public boolean failed = false;
      public boolean complete = false;

      public BreakData(BlockPos pos, int entityId, boolean extraBreak) {
         this.pos = pos;
         this.entityId = entityId;
         this.breakTime = Math.max(BreakESP.getBreakTime(pos, extraBreak), (double)50.0F);
         this.fade = new FadeUtils((long)this.breakTime);
         this.timer = new Timer();
      }

      public Entity getEntity() {
         if (Wrapper.mc.world == null) {
            return null;
         } else {
            Entity entity = Wrapper.mc.world.getEntityById(this.entityId);
            return entity instanceof PlayerEntity ? entity : null;
         }
      }
   }
}
