package dev.baimaodaxia.asm.mixins;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.LogoDrawer;

@Mixin({TitleScreen.class})
public abstract class MixinTitleScreen {
   @Inject(
      method = {"render"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/LogoDrawer;draw(Lnet/minecraft/client/gui/DrawContext;IF)V"
)},
      cancellable = true
   )
   private void onRenderLogo(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      ci.cancel();
   }
}
