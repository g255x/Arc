package dev.Lumi.asm.mixins;

import dev.Lumi.mod.modules.impl.render.Xray;
import net.minecraft.class_1922;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_2680;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({class_2248.class})
public abstract class MixinBlock {
   @Inject(
      method = {"shouldDrawSide"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void onShouldDrawSide(class_2680 state, class_1922 world, class_2338 pos, class_2350 side, class_2338 blockPos, CallbackInfoReturnable<Boolean> cir) {
      Xray xray = Xray.INSTANCE;
      if (xray != null && xray.isOn()) {
         cir.setReturnValue(xray.shouldRender(state));
      }

   }
}
