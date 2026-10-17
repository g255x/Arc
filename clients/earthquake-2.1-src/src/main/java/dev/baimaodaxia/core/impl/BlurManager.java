package dev.baimaodaxia.core.impl;

import dev.baimaodaxia.api.interfaces.IShaderEffectHook;
import dev.baimaodaxia.api.utils.Wrapper;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.util.Identifier;
import org.ladysnake.satin.api.managed.ManagedShaderEffect;
import org.ladysnake.satin.api.managed.ShaderEffectManager;

public class BlurManager implements Wrapper {
   public static final ManagedShaderEffect BLUR = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/blurarea.json"));
   private MyFramebuffer blurBuffer;
   private float lLumidius = -1.0F;

   public void applyBlur(float radius, float startX, float startY, float width, float height) {
      this.applyFullScreenBlur(radius);
   }

   public void applyFullScreenBlur(float radius) {
      if (mc.getWindow() != null && BLUR != null && BLUR.getShaderEffect() != null) {
         try {
            Framebuffer MCBuffer = mc.getFramebuffer();
            if (this.blurBuffer == null) {
               this.blurBuffer = new MyFramebuffer(MCBuffer.textureWidth, MCBuffer.textureHeight);
            }

            if (this.blurBuffer.textureWidth != MCBuffer.textureWidth || this.blurBuffer.textureHeight != MCBuffer.textureHeight) {
               this.blurBuffer.resize(MCBuffer.textureWidth, MCBuffer.textureHeight, false);
            }

            if (radius != this.lLumidius) {
               this.lLumidius = radius;
               this.safeSetUniformValue(BLUR, "Radius", radius);
            }

            PostEffectProcessor processor = BLUR.getShaderEffect();
            if (processor != null) {
               try {
                  ((IShaderEffectHook)processor).alienClient$addHook("bufIn", MCBuffer);
               } catch (Exception var5) {
               }
            }

            BLUR.render(mc.getRenderTickCounter().getTickDelta(true));
         } catch (Exception var6) {
         }

      }
   }

   private void safeSetUniformValue(ManagedShaderEffect effect, String name, float value) {
      try {
         effect.setUniformValue(name, value);
      } catch (Exception var5) {
      }

   }

   private void safeSetUniformValue(ManagedShaderEffect effect, String name, float a, float b) {
      try {
         effect.setUniformValue(name, a, b);
      } catch (Exception var6) {
      }

   }

   public static class MyFramebuffer extends Framebuffer {
      public MyFramebuffer(int width, int height) {
         super(true);
         this.resize(width, height, true);
         this.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
      }
   }
}
