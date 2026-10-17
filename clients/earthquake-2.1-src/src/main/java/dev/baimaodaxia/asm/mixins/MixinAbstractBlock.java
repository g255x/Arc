package dev.baimaodaxia.asm.mixins;

import dev.baimaodaxia.earthquake;
import dev.baimaodaxia.api.events.impl.AmbientOcclusionEvent;
import dev.baimaodaxia.mod.modules.impl.player.PacketMine;
import net.minecraft.world.BlockView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.AbstractBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AbstractBlock.class})
public abstract class MixinAbstractBlock {
   @Inject(
      method = {"getAmbientOcclusionLightLevel"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onGetAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos, CallbackInfoReturnable<Float> info) {
      AmbientOcclusionEvent event = (AmbientOcclusionEvent)earthquake.EVENT_BUS.post(AmbientOcclusionEvent.get());
      if (event.lightLevel != -1.0F) {
         info.setReturnValue(event.lightLevel);
      }

   }

   @Inject(
      method = {"getCollisionShape"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onComputeNextCollisionBox(BlockState state, BlockView world, BlockPos pos, ShapeContext context, CallbackInfoReturnable<VoxelShape> cir) {
      if (PacketMine.INSTANCE != null && pos.equals(PacketMine.getBreakPos()) && PacketMine.INSTANCE.noCollide.getValue() && PacketMine.ghost) {
         cir.setReturnValue(VoxelShapes.empty());
      }

   }
}
