package dev.baimaodaxia.mod.modules.impl.movement;

import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.UpdateEvent;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class VClip extends Module {
   private final EnumSetting<Mode> mode;
   private final SliderSetting blocks;

   public VClip() {
      super("VClip", Module.Category.Movement);
      this.mode = this.add(new EnumSetting("Mode", VClip.Mode.Jump));
      this.blocks = this.add(new SliderSetting("Blocks", (double)1.0F, (double)1.0F, (double)10.0F, () -> this.mode.is(VClip.Mode.Teleport)));
      this.setChinese("纵向穿墙");
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.disable();
      switch (((Mode)this.mode.getValue()).ordinal()) {
         case 0:
            double posX = mc.player.getX();
            double posY = (double)Math.round(mc.player.getY());
            double posZ = mc.player.getZ();
            boolean onGround = mc.player.isOnGround();
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(posX, posY, posZ, onGround));
            double halfY = 0.005;
            double var12;
            mc.player.setPosition(posX, var12 = posY - halfY, posZ);
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(posX, var12, posZ, onGround));
            mc.player.setPosition(posX, posY = var12 - halfY * (double)300.0F, posZ);
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(posX, posY, posZ, onGround));
            break;
         case 1:
            mc.player.setPosition(mc.player.getX(), mc.player.getY() + (double)3.0F, mc.player.getZ());
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true));
            break;
         case 2:
            int blockCount = this.blocks.getValueInt();
            double currentY = mc.player.getY();

            for(int i = 0; i < blockCount; ++i) {
               mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), currentY + 0.4199999868869781, mc.player.getZ(), false));
               mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), currentY + 0.7531999805212017, mc.player.getZ(), false));
               ++currentY;
            }

            mc.player.setPosition(mc.player.getX(), currentY, mc.player.getZ());
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true));
      }

   }

   public static enum Mode {
      Glitch,
      Teleport,
      Jump;

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{Glitch, Teleport, Jump};
      }
   }
}
