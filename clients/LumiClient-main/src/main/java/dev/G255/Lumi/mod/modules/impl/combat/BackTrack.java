package dev.G255.Lumi.mod.modules.impl.combat;

import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.PacketEvent;
import dev.G255.Lumi.api.events.impl.UpdateEvent;
import dev.G255.Lumi.api.utils.Wrapper;
import dev.G255.Lumi.api.utils.render.Render3DUtil;
import dev.G255.Lumi.asm.accessors.IEntity;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.ColorSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.client.util.math.MatrixStack;

public class BackTrack extends Module {
   private final CopyOnWriteArrayList<QueuedPacket> packets = new CopyOnWriteArrayList<>();
   private final SliderSetting delay = this.add((new SliderSetting("Delay", (double)100.0F, (double)0.0F, (double)1000.0F, (double)1.0F)).setSuffix("ms"));
   private final SliderSetting maxQueued = this.add(new SliderSetting("MaxQueued", (double)200.0F, (double)0.0F, (double)1000.0F, (double)1.0F));
   private final BooleanSetting render = this.add(new BooleanSetting("Render", true));
   private final ColorSetting fill = this.add((new ColorSetting("Fill", new Color(255, 255, 255, 30), () -> this.render.getValue())).injectBoolean(true));
   private final ColorSetting outline = this.add((new ColorSetting("Outline", new Color(255, 255, 255, 120), () -> this.render.getValue())).injectBoolean(true));
   private Vec3d serverPos;

   public BackTrack() {
      super("BackTrack", Module.Category.Combat);
      this.setChinese("");
   }

   public String getInfo() {
      return this.delay.getValueInt() + "ms";
   }

   public void onDisable() {
      this.flush();
   }

   public void onLogout() {
      this.packets.clear();
      this.serverPos = null;
   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive event) {
      if (!nullCheck()) {
         Packet packet = event.getPacket();
         if (packet instanceof PlayerPositionLookS2CPacket) {
            this.flush();
         } else {
            Entity target = this.getTarget();
            if (target != null) {
               if (packet instanceof EntityS2CPacket) {
                  EntityS2CPacket entityPacket = (EntityS2CPacket)packet;
                  Entity entity = entityPacket.getEntity(mc.world);
                  if (entity != null && entity == target) {
                     this.updateServerPos(entityPacket, target);
                     this.queue(packet);
                     event.cancel();
                  }
               } else {
                  if (packet instanceof EntityPositionS2CPacket) {
                     EntityPositionS2CPacket entityPacket = (EntityPositionS2CPacket)packet;
                     if (entityPacket.getEntityId() != target.getId()) {
                        return;
                     }

                     this.updateServerPos(entityPacket);
                     this.queue(packet);
                     event.cancel();
                  }

               }
            }
         }
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.update();
   }

   public void onRender3D(MatrixStack matrixStack) {
      this.update();
      if (this.render.getValue() && !nullCheck()) {
         Entity target = this.getTarget();
         if (target != null && this.serverPos != null) {
            Box box = ((IEntity)target).getDimensions().getBoxAt(this.serverPos).expand((double)0.0F, 0.1, (double)0.0F);
            Render3DUtil.draw3DBox(matrixStack, box, this.fill.getValue(), this.outline.getValue(), this.outline.booleanValue, this.fill.booleanValue);
         }
      }
   }

   private void update() {
      if (nullCheck()) {
         this.packets.clear();
         this.serverPos = null;
      } else {
         long now = System.currentTimeMillis();
         this.packets.removeIf((p) -> p.trySend(now));
      }
   }

   private void flush() {
      if (nullCheck()) {
         this.packets.clear();
         this.serverPos = null;
      } else {
         for(QueuedPacket p : this.packets) {
            p.apply();
         }

         this.packets.clear();
         this.serverPos = null;
      }
   }

   private void queue(Packet packet) {
      int max = this.maxQueued.getValueInt();
      if (max > 0) {
         this.packets.add(new QueuedPacket(packet, System.currentTimeMillis() + (long)this.delay.getValueInt()));

         while(this.packets.size() > max) {
            QueuedPacket overflow = (QueuedPacket)this.packets.remove(0);
            overflow.apply();
         }

      }
   }

   private Entity getTarget() {
      if (Aura.target != null) {
         return Aura.target;
      } else {
         HitResult target = mc.crosshairTarget;
         return target instanceof EntityHitResult ? ((EntityHitResult)target).getEntity() : null;
      }
   }

   private void updateServerPos(EntityPositionS2CPacket packet) {
      this.serverPos = new Vec3d(packet.getX(), packet.getY(), packet.getZ());
   }

   private void updateServerPos(EntityS2CPacket packet, Entity entity) {
      if (this.serverPos == null) {
         this.serverPos = entity.getPos();
      }

      double dx = (double)packet.getDeltaX() / (double)4096.0F;
      double dy = (double)packet.getDeltaY() / (double)4096.0F;
      double dz = (double)packet.getDeltaZ() / (double)4096.0F;
      this.serverPos = this.serverPos.add(dx, dy, dz);
   }

   private static class QueuedPacket {
      private final Packet packet;
      private final long sendAt;

      public QueuedPacket(Packet packet, long sendAt) {
         this.packet = packet;
         this.sendAt = sendAt;
      }

      public boolean trySend(long now) {
         if (now >= this.sendAt) {
            this.apply();
            return true;
         } else {
            return false;
         }
      }

      public void apply() {
         if (this.packet != null) {
            this.packet.apply(Wrapper.mc.getNetworkHandler());
         }

      }
   }
}