package dev.g255.arc.mod.modules.impl.misc;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.api.utils.world.BlockUtil;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.impl.player.PacketMine;
import dev.g255.arc.mod.modules.impl.render.PlaceRender;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.util.Objects;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;

public class Nuker extends Module {
   private final SliderSetting range = this.add(new SliderSetting("Range", (double)4.0F, (double)0.0F, (double)8.0F, 0.1));
   private final BooleanSetting down = this.add(new BooleanSetting("Down", false));
   private final BooleanSetting sand = this.add(new BooleanSetting("Sand", false));
   private final SliderSetting breaks;

   public Nuker() {
      super("Nuker", Module.Category.Misc);
      BooleanSetting var10008 = this.sand;
      Objects.requireNonNull(var10008);
      this.breaks = this.add(new SliderSetting("Breaks", 10, 0, 20, var10008::getValue));
      this.setChinese("范围挖掘");
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (PacketMine.getBreakPos() == null || mc.world.isAir(PacketMine.getBreakPos())) {
         if (this.sand.getValue()) {
            if (!mc.player.isOnGround()) {
               return;
            }

            int elementCodec = 0;

            for(BlockPos sand : BlockUtil.getSphere(this.range.getValueFloat(), mc.player.getEyePos())) {
               Direction side;
               if ((Blocks.SAND == mc.world.getBlockState(sand).getBlock() || Blocks.RED_SAND == mc.world.getBlockState(sand).getBlock()) && (side = BlockUtil.getClickSideStrict(sand)) != null) {
                  PlaceRender.INSTANCE.create(sand);
                  Arc.ROTATION.snapAt(sand.toCenterPos());
                  sendSequencedPacket((id) -> new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, sand, side, id));
                  sendSequencedPacket((id) -> new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, sand, side, id));
                  sendSequencedPacket((id) -> new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, sand, side, id));
                  Arc.ROTATION.snapBack();
                  ++elementCodec;
                  if ((double)elementCodec >= this.breaks.getValue()) {
                     return;
                  }
               }
            }
         } else {
            BlockPos pos = this.getBlock();
            if (pos != null) {
               PacketMine.INSTANCE.mine(pos);
            }
         }

      }
   }

   private BlockPos getBlock() {
      BlockPos down = null;

      for(BlockPos pos : BlockUtil.getSphere(this.range.getValueFloat(), mc.player.getEyePos())) {
         if (!mc.world.isAir(pos) && !PacketMine.unbreakable(pos) && BlockUtil.getClickSideStrict(pos) != null) {
            if (!((double)pos.getY() < mc.player.getY())) {
               return pos;
            }

            if (down == null && this.down.getValue()) {
               down = pos;
            }
         }
      }

      return down;
   }
}
