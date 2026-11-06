package dev.g255.arc.asm.mixins;

import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.impl.render.Ambience;
import net.minecraft.block.AbstractBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AbstractBlock.AbstractBlockState.class})
public class MixinAbstractBlockState {
   @Inject(
      method = {"getLuminance"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void getLuminanceHook(CallbackInfoReturnable<Integer> cir) {
      if (!Module.nullCheck()) {
         if (Ambience.INSTANCE.customLuminance.getValue()) {
            cir.setReturnValue(Ambience.INSTANCE.luminance.getValueInt());
         }

      }
   }
}
