package dev.Lumi.asm.mixins;

import dev.Lumi.mod.modules.impl.render.Xray;
import net.minecraft.class_1922;
import net.minecraft.class_2338;
import net.minecraft.class_4970;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({class_4970.class_4971.class})
public class MixinAbstractBlockState {
   @Inject(
      method = {"getAmbientOcclusionLightLevel"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void getAmbientOcclusionLightLevelHook(class_1922 world, class_2338 pos, CallbackInfoReturnable<Float> info) {
      Xray xray = Xray.INSTANCE;
      if (xray != null && xray.isOn()) {
         info.setReturnValue(1.0F);
      }

   }
}
