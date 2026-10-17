package dev.kissoo.mod.modules.impl.misc;

import dev.kissoo.Forever;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.utils.world.BlockUtil;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.impl.player.SpeedMine;
import dev.kissoo.mod.modules.impl.render.PlaceRender;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class Nuker extends Module {
   private final SliderSetting range = this.add(new SliderSetting("Range", 4.0, 0.0, 8.0, 0.1));
   private final BooleanSetting down = this.add(new BooleanSetting("Down", false));
   private final BooleanSetting sand = this.add(new BooleanSetting("Sand", false));
   private final SliderSetting breaks = this.add(new SliderSetting("Breaks", 10, 0, 20, this.sand::getValue));

   public Nuker() {
      super("Nuker", Module.Category.Misc);
      this.setChinese("范围挖掘");
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {

      BlockPos miningPos = SpeedMine.getBreakPos();
      if (miningPos == null || mc.world.isAir(miningPos)) {

         if (this.sand.getValue()) {

            if (!mc.player.isOnGround()) {
               return;
            }

            int b = 0;

            for (BlockPos sand : BlockUtil.getSphere(this.range.getValueFloat(), mc.player.getEyePos())) {

               if (mc.world.getBlockState(sand).getBlock() == Blocks.SAND
                       || mc.world.getBlockState(sand).getBlock() == Blocks.RED_SAND) {

                  Direction side = BlockUtil.getClickSideStrict(sand);

                  if (side != null) {

                     PlaceRender.INSTANCE.create(sand);

                     Forever.ROTATION.snapAt(sand.toCenterPos());

                     sendSequencedPacket(id ->
                             new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, sand, side, id)
                     );
                     sendSequencedPacket(id ->
                             new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, sand, side, id)
                     );
                     sendSequencedPacket(id ->
                             new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, sand, side, id)
                     );

                     Forever.ROTATION.snapBack();

                     if (++b >= this.breaks.getValue()) {
                        return;
                     }
                  }
               }
            }

         } else {

            BlockPos pos = this.getBlock();

            if (pos != null
                    && SpeedMine.INSTANCE != null
                    && SpeedMine.INSTANCE.isOn()) {

               SpeedMine.INSTANCE.mine(pos);
            }
         }
      }
   }

   private BlockPos getBlock() {

      BlockPos down = null;

      for (BlockPos pos : BlockUtil.getSphere(this.range.getValueFloat(), mc.player.getEyePos())) {

         if (!mc.world.isAir(pos)
                 && !SpeedMine.unbreakable(pos)
                 && BlockUtil.getClickSideStrict(pos) != null) {

            if (pos.getY() >= mc.player.getY()) {
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