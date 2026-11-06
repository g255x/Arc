package dev.g255.arc.mod.modules.impl.player;

import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.PacketEvent;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.asm.accessors.IPlayerMoveC2SPacket;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class NoFall extends Module {
   private final EnumSetting<NoFallMode> mode;
   private final SliderSetting getDistance;

   public NoFall() {
      super("NoFall", "Prevents fall damage.", Module.Category.Player);
      this.mode = this.add(new EnumSetting("Mode", NoFall.NoFallMode.Packet));
      this.getDistance = this.add(new SliderSetting("Distance", (double)3.0F, (double)0.0F, (double)8.0F, 0.1));
      this.setChinese("没有摔落伤害");
   }

   public String getInfo() {
      return ((NoFallMode)this.mode.getValue()).name();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!nullCheck()) {
         if (this.mode.is(NoFall.NoFallMode.Grim) && this.checkFalling()) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY() + 1.0E-9, mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), false));
            mc.player.onLanding();
         }

      }
   }

   private boolean checkFalling() {
      return mc.player.fallDistance > (float)mc.player.getSafeFallDistance() && !mc.player.isOnGround() && !mc.player.isFallFlying();
   }

   @EventListener
   public void onPacketSend(PacketEvent.Send event) {
      if (!nullCheck()) {
         for(ItemStack is : mc.player.getArmorItems()) {
            if (is.getItem() == Items.ELYTRA) {
               return;
            }
         }

         if (this.mode.is(NoFall.NoFallMode.Packet)) {
            Packet<?> packet = event.getPacket();
            if (packet instanceof PlayerMoveC2SPacket) {
               PlayerMoveC2SPacket packet2 = (PlayerMoveC2SPacket)packet;
               if (mc.player.fallDistance >= (float)this.getDistance.getValue()) {
                  ((IPlayerMoveC2SPacket)packet2).setOnGround(true);
               }
            }

         }
      }
   }

   public static enum NoFallMode {
      Packet,
      Grim;

      // $FF: synthetic method
      private static NoFallMode[] $values() {
         return new NoFallMode[]{Packet, Grim};
      }
   }
}
