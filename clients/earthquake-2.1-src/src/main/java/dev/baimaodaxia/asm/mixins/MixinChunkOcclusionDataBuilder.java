package dev.baimaodaxia.asm.mixins;

import dev.baimaodaxia.earthquake;
import dev.baimaodaxia.api.events.impl.ChunkOcclusionEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ChunkOcclusionDataBuilder.class})
public abstract class MixinChunkOcclusionDataBuilder {
   @Inject(
      method = {"markClosed"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onMarkClosed(BlockPos pos, CallbackInfo info) {
      ChunkOcclusionEvent event = (ChunkOcclusionEvent)earthquake.EVENT_BUS.post(ChunkOcclusionEvent.get());
      if (event.isCancelled()) {
         info.cancel();
      }

   }
}
