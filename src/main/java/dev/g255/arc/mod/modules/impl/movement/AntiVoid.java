package dev.g255.arc.mod.modules.impl.movement;

import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.api.utils.player.MovementUtil;
import dev.g255.arc.api.utils.world.BlockPosX;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;

public class AntiVoid extends Module {
   private final SliderSetting voidHeight = this.add(new SliderSetting("VoidHeight", (double)-64.0F, (double)-64.0F, (double)319.0F, (double)1.0F));
   private final SliderSetting height = this.add(new SliderSetting("Height", (double)100.0F, (double)-40.0F, (double)256.0F, (double)1.0F));

   public AntiVoid() {
      super("AntiVoid", "Allows you to fly over void blocks", Module.Category.Movement);
      this.setChinese("反虚空");
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      boolean isVoid = true;

      for(int i = (int)mc.player.getY(); i > this.voidHeight.getValueInt() - 1; --i) {
         if (!mc.world.getBlockState(new BlockPosX(mc.player.getX(), (double)i, mc.player.getZ())).isAir() && mc.world.getBlockState(new BlockPosX(mc.player.getX(), (double)i, mc.player.getZ())).getBlock() != Blocks.VOID_AIR) {
            isVoid = false;
            break;
         }
      }

      if (mc.player.getY() < this.height.getValue() + this.voidHeight.getValue() && isVoid) {
         MovementUtil.setMotionY((double)0.0F);
      }

   }
}
