package hoprc.asm.mixins;

import dev.kissoo.Forever;
import dev.kissoo.api.events.impl.AmbientOcclusionEvent;
import dev.kissoo.mod.modules.impl.player.SpeedMine;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.class)
public abstract class MixinAbstractBlock {
   @Inject(method = "getAmbientOcclusionLightLevel", at = @At("HEAD"), cancellable = true)
   private void onGetAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos, CallbackInfoReturnable<Float> info) {
      AmbientOcclusionEvent event = Forever.EVENT_BUS.post(AmbientOcclusionEvent.get());
      if (event.lightLevel != -1.0F) {
         info.setReturnValue(event.lightLevel);
      }
   }

   @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
   private void onComputeNextCollisionBox(BlockState state, BlockView world, BlockPos pos, ShapeContext context, CallbackInfoReturnable<VoxelShape> cir) {
      SpeedMine instance = dev.kissoo.mod.modules.impl.player.SpeedMine.INSTANCE;
      if (instance != null && instance.isOn()) {
         BlockPos miningPos = dev.kissoo.mod.modules.impl.player.SpeedMine.getBreakPos();

         if (pos.equals(miningPos)) {
            if (instance.noCollide.getValue() && SpeedMine.progress >= 1.0) {
               cir.setReturnValue(net.minecraft.util.shape.VoxelShapes.empty());
            }
         }
      }
   }
}