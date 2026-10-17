package dev.baimaodaxia.asm.mixins;

import dev.baimaodaxia.api.utils.render.panorama.GlobalState;
import dev.baimaodaxia.api.utils.render.panorama.PanoramaResourceLoader;
import net.minecraft.client.util.Window;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.CubeMapRenderer;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({RotatingCubeMapRenderer.class})
public abstract class MixinRotatingCubeMapRenderer {
   @Unique
   private double time = (double)0.0F;
   @Unique
   private long frame = 0L;

   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   private void increaseTime(DrawContext context, int width, int height, float alpha, float tickDelta, CallbackInfo ci) {
      this.time += (double)tickDelta;
      ++this.frame;
   }

   @Redirect(
      method = {"render"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/CubeMapRenderer;draw(Lnet/minecraft/client/MinecraftClient;FFF)V"
)
   )
   public void glsl_render(CubeMapRenderer instance, MinecraftClient client, float x, float y, float alpha) {
      Window window = client.getWindow();
      int width = window.getWidth();
      int height = window.getHeight();
      float mx = (float)client.mouse.getX() / (float)width;
      float my = (float)client.mouse.getY() / (float)height;
      PanoramaResourceLoader.getRenderer().draw(client, this.time / (double)60.0F, this.frame, mx, my, width, height, alpha);
      GlobalState.nextFrame();
   }
}
