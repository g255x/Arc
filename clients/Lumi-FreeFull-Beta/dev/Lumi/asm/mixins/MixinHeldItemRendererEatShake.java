package dev.Lumi.asm.mixins;

import dev.Lumi.mod.modules.impl.render.ViewModel;
import net.minecraft.class_759;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin({class_759.class})
public abstract class MixinHeldItemRendererEatShake {
   @ModifyArgs(
      method = {"applyEatOrDrinkTransformation"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V",
   ordinal = 0
)
   )
   private void onApplyEatOrDrinkTransformation(Args args) {
      if (ViewModel.INSTANCE != null && ViewModel.INSTANCE.isOn() && ViewModel.INSTANCE.eatShakeMultiplier.getValue() != (double)1.0F) {
         float currentY = (Float)args.get(1);
         args.set(1, currentY * (float)ViewModel.INSTANCE.eatShakeMultiplier.getValue());
      }

   }
}
