package dev.baimaodaxia.api.utils.render.panorama.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.baimaodaxia.api.utils.render.panorama.PanoramaRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.ColorHelper.Argb;
import net.minecraft.util.math.ColorHelper;

public final class PanoramaFallbackRenderer implements PanoramaRenderer {
   private final float r;
   private final float g;
   private final float b;

   public PanoramaFallbackRenderer(int background, int foreground) {
      this.r = (float)ColorHelper.Argb.getRed(background) / 255.0F;
      this.g = (float)ColorHelper.Argb.getGreen(background) / 255.0F;
      this.b = (float)ColorHelper.Argb.getBlue(background) / 255.0F;
   }

   public void draw(MinecraftClient client, double time, long frame, float mouseX, float mouseY, int width, int height, float alpha) {
      RenderSystem.clearColor(this.r, this.g, this.b, alpha);
      RenderSystem.clear(16640, MinecraftClient.IS_SYSTEM_MAC);
   }

   public void close() {
   }
}
