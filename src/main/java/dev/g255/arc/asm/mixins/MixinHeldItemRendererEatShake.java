package dev.g255.arc.asm.mixins;

import dev.g255.arc.mod.modules.impl.render.ViewModel;
import net.minecraft.client.render.item.HeldItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * 手持模型 - 进食/饮用动画的晃动倍率。
 * <p>
 * 原版 {@code applyEatOrDrinkTransformation} 里第一处 {@code translate(0, |cos(...)| * 0.1, 0)} 就是
 * 吃东西时手部的上下抖动，这里只把它的 Y 分量乘以倍率：
 * 0 完全不晃、1 为原版、大于 1 会晃得更夸张。
 */
@Mixin({HeldItemRenderer.class})
public abstract class MixinHeldItemRendererEatShake {
   @ModifyArgs(
      method = {"applyEatOrDrinkTransformation"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V",
   ordinal = 0
)
   )
   private void onEatShake(Args args) {
      ViewModel viewModel = ViewModel.INSTANCE;
      if (viewModel == null || !viewModel.isOn()) {
         return;
      }

      double multiplier = viewModel.eatShakeMultiplier.getValue();
      if (multiplier == 1.0) {
         return;
      }

      args.set(1, (Float)args.get(1) * (float)multiplier);
   }
}
