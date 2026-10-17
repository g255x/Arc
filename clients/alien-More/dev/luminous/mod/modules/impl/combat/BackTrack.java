package dev.luminous.mod.modules.impl.combat;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.asm.accessors.IEntity;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.class_1297;
import net.minecraft.class_238;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_2596;
import net.minecraft.class_2684;
import net.minecraft.class_2708;
import net.minecraft.class_2777;
import net.minecraft.class_3966;
import net.minecraft.class_4587;

public class BackTrack extends Module {
   private final CopyOnWriteArrayList packets = new CopyOnWriteArrayList();
   private final SliderSetting delay = this.add((new SliderSetting("Delay", (double)100.0F, (double)0.0F, (double)1000.0F, (double)1.0F)).setSuffix("ms"));
   private final SliderSetting maxQueued = this.add(new SliderSetting("MaxQueued", (double)200.0F, (double)0.0F, (double)1000.0F, (double)1.0F));
   private final BooleanSetting render = this.add(new BooleanSetting("Render", true));
   private final ColorSetting fill = this.add((new ColorSetting("Fill", new Color(255, 255, 255, 30), () -> this.render.getValue())).injectBoolean(true));
   private final ColorSetting outline = this.add((new ColorSetting("Outline", new Color(255, 255, 255, 120), () -> this.render.getValue())).injectBoolean(true));
   private class_243 serverPos;

   public BackTrack() {
      super("BackTrack", Module.Category.Combat);
      this.setChinese("打滑回溯");
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
         class_2596 packet = event.getPacket();
         if (packet instanceof class_2708) {
            this.flush();
         } else {
            class_1297 target = this.getTarget();
            if (target != null) {
               if (packet instanceof class_2684) {
                  class_2684 entityPacket = (class_2684)packet;
                  class_1297 entity = entityPacket.method_11645(mc.field_1687);
                  if (entity != null && entity == target) {
                     this.updateServerPos(entityPacket, target);
                     this.queue(packet);
                     event.cancel();
                  }
               } else {
                  if (packet instanceof class_2777) {
                     class_2777 entityPacket = (class_2777)packet;
                     if (entityPacket.method_11916() != target.method_5628()) {
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

   public void onRender3D(class_4587 matrixStack) {
      this.update();
      if (this.render.getValue() && !nullCheck()) {
         class_1297 target = this.getTarget();
         if (target != null && this.serverPos != null) {
            class_238 box = ((IEntity)target).getDimensions().method_30757(this.serverPos).method_1009((double)0.0F, 0.1, (double)0.0F);
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

   private void queue(class_2596 packet) {
      int max = this.maxQueued.getValueInt();
      if (max > 0) {
         this.packets.add(new QueuedPacket(packet, System.currentTimeMillis() + (long)this.delay.getValueInt()));

         while(this.packets.size() > max) {
            QueuedPacket overflow = (QueuedPacket)this.packets.remove(0);
            overflow.apply();
         }

      }
   }

   private class_1297 getTarget() {
      if (Aura.target != null) {
         return Aura.target;
      } else {
         class_239 target = mc.field_1765;
         return target instanceof class_3966 ? ((class_3966)target).method_17782() : null;
      }
   }

   private void updateServerPos(class_2777 packet) {
      this.serverPos = new class_243(packet.method_11917(), packet.method_11919(), packet.method_11918());
   }

   private void updateServerPos(class_2684 packet, class_1297 entity) {
      if (this.serverPos == null) {
         this.serverPos = entity.method_19538();
      }

      double dx = (double)packet.method_36150() / (double)4096.0F;
      double dy = (double)packet.method_36151() / (double)4096.0F;
      double dz = (double)packet.method_36152() / (double)4096.0F;
      this.serverPos = this.serverPos.method_1031(dx, dy, dz);
   }

   private static class QueuedPacket {
      private final class_2596 packet;
      private final long sendAt;

      public QueuedPacket(class_2596 packet, long sendAt) {
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
            this.packet.method_11054(Wrapper.mc.method_1562());
         }

      }
   }
}
