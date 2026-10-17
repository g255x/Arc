package hoprc.asm.mixins;

import dev.kissoo.Forever;
import dev.kissoo.api.events.impl.ChunkOcclusionEvent;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkOcclusionDataBuilder.class)
public abstract class MixinChunkOcclusionDataBuilder {
   @Inject(method = "markClosed", at = @At("HEAD"), cancellable = true)
   private void onMarkClosed(BlockPos pos, CallbackInfo info) {
      ChunkOcclusionEvent event = Forever.EVENT_BUS.post(ChunkOcclusionEvent.get());
      if (event.isCancelled()) {
         info.cancel();
      }
   }
}
