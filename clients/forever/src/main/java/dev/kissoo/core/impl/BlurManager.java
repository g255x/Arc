package dev.kissoo.core.impl;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.kissoo.api.utils.Wrapper;
import net.minecraft.util.Identifier;
import org.ladysnake.satin.api.managed.ManagedShaderEffect;
import org.ladysnake.satin.api.managed.ShaderEffectManager;

import org.lwjgl.opengl.GL30;

public class BlurManager implements Wrapper {
   public static final ManagedShaderEffect BLUR = ShaderEffectManager.getInstance()
           .manage(Identifier.of("shaders/post/blurarea.json"));

   public void applyBlur(float radius, float startX, float startY, float width, float height) {
      if (mc.getWindow() == null || radius <= 0 || width <= 0 || height <= 0) return;
      int backupFbo = GlStateManager.getBoundFramebuffer();

      float factor = (float) mc.getWindow().getScaleFactor() / 2.0F;
      BLUR.setUniformValue("Radius", radius);
      BLUR.setUniformValue("BlurXY", startX * factor,
              mc.getWindow().getHeight() / 2.0F - (startY + height) * factor);
      BLUR.setUniformValue("BlurCoord", width * factor, height * factor);
      BLUR.setUniformValue("DiagonalLine", 99999.0F, 99999.0F);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      BLUR.render(mc.getRenderTickCounter().getTickDelta(true));
      GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, backupFbo);
      RenderSystem.disableBlend();
   }

   public void applyBlurTrapezoid(float radius, int screenWidth, int screenHeight,
                                  float lineTopX, float lineBottomX) {
      if (mc.getWindow() == null || radius <= 0) return;

      int backupFbo = GlStateManager.getBoundFramebuffer();
      float factor = (float) mc.getWindow().getScaleFactor() / 2.0F;

      BLUR.setUniformValue("Radius", radius);
      BLUR.setUniformValue("BlurXY", 0.0F, 0.0F);
      BLUR.setUniformValue("BlurCoord", screenWidth * factor, screenHeight * factor);
      BLUR.setUniformValue("DiagonalLine", lineTopX * factor, lineBottomX * factor);

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();

      BLUR.render(mc.getRenderTickCounter().getTickDelta(true));
      GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, backupFbo);
      RenderSystem.disableBlend();
   }
}