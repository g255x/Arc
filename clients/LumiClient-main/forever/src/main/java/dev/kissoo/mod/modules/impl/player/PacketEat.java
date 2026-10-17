package dev.kissoo.mod.modules.impl.player;

import dev.kissoo.Forever;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.PacketEvent;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
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
         Module.sendSequencedPacket(
            id -> new PlayerInteractItemC2SPacket(mc.player.getActiveHand(), id, Forever.ROTATION.getLastYaw(), Forever.ROTATION.getLastPitch())
         );
      }
   }

   @EventListener
   public void onPacket(PacketEvent.Send event) {
      if (this.noRelease.getValue()
         && event.getPacket() instanceof PlayerActionC2SPacket packet
         && packet.getAction() == Action.RELEASE_USE_ITEM
         && mc.player.getActiveItem().getItem().getComponents().contains(DataComponentTypes.FOOD)) {
         event.cancel();
      }
   }
}
