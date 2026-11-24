package dev.g255.arc.asm.mixins;

import dev.g255.arc.mod.gui.MenuBackground;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.MessageScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 「正在保存世界 / 正在加入世界 / 正在退出」提示界面。
 *
 * <p>这类界面自己覆写了 {@code renderBackground}（没走 {@code Screen} 的实现），
 * 所以 {@link MixinScreen} 的统一接管对这些界面是失效的 —— 之前极光在这里不显示
 * 就是这个原因。这里单独接管：先铺极光，再压一层暗色让居中的白字能看清。</p>
 */
@Mixin({MessageScreen.class})
public abstract class MixinMessageScreen {
   @Inject(
      method = {"renderBackground(Lnet/minecraft/client/gui/DrawContext;IIF)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void arc$auroraBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      ci.cancel();
      int width = context.getScaledWindowWidth();
      int height = context.getScaledWindowHeight();
      MenuBackground.renderScreen(context, width, height);
      MenuBackground.dim(context, width, height);
   }
}
