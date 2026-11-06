package dev.g255.arc.asm.mixins;

import dev.g255.arc.mod.modules.impl.movement.NoSlow;
import net.minecraft.item.ItemConvertible;
import net.minecraft.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Block.class})
public abstract class MixinBlock implements ItemConvertible {
   @Inject(
      at = {@At("HEAD")},
      method = {"getVelocityMultiplier()F"},
      cancellable = true
   )
   private void onGetVelocityMultiplier(CallbackInfoReturnable<Float> cir) {
      if (NoSlow.INSTANCE.soulSand() && cir.getReturnValueF() < 1.0F) {
         cir.setReturnValue(1.0F);
      }

   }
}
