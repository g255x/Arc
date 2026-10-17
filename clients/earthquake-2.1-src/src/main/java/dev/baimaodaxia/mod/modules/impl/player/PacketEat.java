package dev.baimaodaxia.mod.modules.impl.player;

import dev.baimaodaxia.Alien;
import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.PacketEvent;
import dev.baimaodaxia.api.events.impl.UpdateEvent;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;

public class PacketEat extends Module {
   public static PacketEat INSTANCE;
   private final BooleanSetting deSync = this.add(new BooleanSetting("DeSync", false));
   private final BooleanSetting noRelease = this.add(new BooleanSetting("NoRelease", true));

   public PacketEat() {
      super("PacketEat", Module.Category.Player);
      this.setChinese("发包进食");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.deSync.getValue() && mc.player.isUsingItem() && mc.player.getActiveItem().getItem().getComponents().contains(DataComponentTypes.FOOD)) {
         Module.sendSequencedPacket((id) -> new PlayerInteractItemC2SPacket(mc.player.getActiveHand(), id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
      }

   }

   @EventListener
   public void onPacket(PacketEvent.Send event) {
      PlayerActionC2SPacket packet;
      Packet<?> packet2;
      if (this.noRelease.getValue() && (packet2 = event.getPacket()) instanceof PlayerActionC2SPacket && (packet = (PlayerActionC2SPacket)packet2).getAction() == Action.RELEASE_USE_ITEM && mc.player.getActiveItem().getItem().getComponents().contains(DataComponentTypes.FOOD)) {
         event.cancel();
      }

   }
}
