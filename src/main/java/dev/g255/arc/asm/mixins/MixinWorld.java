package dev.g255.arc.asm.mixins;

import dev.g255.arc.Arc;
import dev.g255.arc.api.utils.Wrapper;
import dev.g255.arc.api.utils.combat.CombatUtil;
import dev.g255.arc.mod.modules.impl.client.ClientSetting;
import dev.g255.arc.mod.modules.impl.combat.SelfTrap;
import dev.g255.arc.mod.modules.impl.player.InteractTweaks;
import net.minecraft.world.World;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({World.class})
public abstract class MixinWorld {
   @Inject(
      method = {"getBlockState"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void blockStateHook(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
      if (Wrapper.mc.world != null && Wrapper.mc.world.isInBuildLimit(pos)) {
         if (SelfTrap.airList.contains(pos)) {
            cir.setReturnValue(Blocks.AIR.getDefaultState());
            return;
         }

         if (!ClientSetting.INSTANCE.mioCompatible.getValue()) {
            boolean terrainIgnore = CombatUtil.terrainIgnore;
            BlockPos modifyPos = CombatUtil.modifyPos;
            BlockState modifyBlockState = CombatUtil.modifyBlockState;
            if (!terrainIgnore && modifyPos == null) {
               if (InteractTweaks.INSTANCE.isActive && Wrapper.mc.world.getChunk(pos.getX() >> 4, pos.getZ() >> 4).getBlockState(pos).getBlock() == Blocks.BEDROCK) {
                  cir.setReturnValue(Blocks.AIR.getDefaultState());
               }
            } else {
               WorldChunk worldChunk2 = Wrapper.mc.world.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
               BlockState tempState2 = worldChunk2.getBlockState(pos);
               if (modifyPos != null && modifyBlockState != null && pos.equals(modifyPos)) {
                  cir.setReturnValue(modifyBlockState);
                  return;
               }

               if (terrainIgnore) {
                  if (Arc.HOLE.isHard(tempState2.getBlock())) {
                     return;
                  }

                  cir.setReturnValue(Blocks.AIR.getDefaultState());
               }
            }
         }
      }

   }
}
