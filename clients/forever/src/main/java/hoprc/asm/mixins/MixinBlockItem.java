package hoprc.asm.mixins;

import dev.kissoo.Forever;
import dev.kissoo.api.events.impl.PlaceBlockEvent;
import dev.kissoo.mod.modules.Module;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class MixinBlockItem {
   @Inject(method = "place(Lnet/minecraft/item/ItemPlacementContext;Lnet/minecraft/block/BlockState;)Z", at = @At("RETURN"))
   private void onPlace(@NotNull ItemPlacementContext context, BlockState state, CallbackInfoReturnable<Boolean> info) {
      if (!Module.nullCheck()) {
         if (context.getWorld().isClient) {
            Forever.EVENT_BUS.post(PlaceBlockEvent.get(context.getBlockPos(), state.getBlock()));
         }
      }
   }
}
