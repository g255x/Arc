package dev.baimaodaxia.core.impl;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.baimaodaxia.api.interfaces.IShaderEffectHook;
import dev.baimaodaxia.api.utils.Wrapper;
import dev.baimaodaxia.api.utils.math.Timer;
import dev.baimaodaxia.mod.modules.impl.render.MotionBlur;
import dev.baimaodaxia.mod.modules.impl.render.ShaderModule;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.satin.api.managed.ManagedShaderEffect;
import org.ladysnake.satin.api.managed.ShaderEffectManager;
public class ShaderManager implements Wrapper {
   static final Timer timer = new Timer();
   private static final List<RenderTask> tasks = new ArrayList();
   public static ManagedShaderEffect DEFAULT_OUTLINE;
   public static ManagedShaderEffect SMOKE_OUTLINE;
   public static ManagedShaderEffect GRADIENT_OUTLINE;
   public static ManagedShaderEffect SNOW_OUTLINE;
   public static ManagedShaderEffect FLOW_OUTLINE;
   public static ManagedShaderEffect RAINBOW_OUTLINE;
   public static ManagedShaderEffect DEFAULT;
   public static ManagedShaderEffect SMOKE;
   public static ManagedShaderEffect GRADIENT;
   public static ManagedShaderEffect SNOW;
   public static ManagedShaderEffect FLOW;
   public static ManagedShaderEffect RAINBOW;
   public static ManagedShaderEffect SKYBOX;
   public static ManagedShaderEffect MOTION_BLUR;
   public float time = 0.0F;
   private MyFramebuffer shaderBuffer;
   private MyFramebuffer motionBlurBuffer;
   private float lastBlurFactor = 0.3F;
   private boolean lastMaxEnabled = false;

   public void renderShader(Runnable runnable, Shader mode) {
      tasks.add(new RenderTask(runnable, mode));
   }

   public void renderShaders() {
      tasks.forEach((t) -> this.applyShader(t.task(), t.shader()));
      tasks.clear();
   }

   public void renderMotionBlur() {
      if (!this.fullNullCheck()) {
         MotionBlur module = MotionBlur.INSTANCE;
         if (module != null && module.isOn()) {
            ManagedShaderEffect effect = MOTION_BLUR;
            if (effect != null && effect.getShaderEffect() != null) {
               float blurFactor = module.factor.getValueFloat();
               boolean maxEnabled = module.max.getValue();
               if (blurFactor != this.lastBlurFactor || maxEnabled != this.lastMaxEnabled) {
                  this.lastBlurFactor = blurFactor;
                  this.lastMaxEnabled = maxEnabled;
                  this.safeSetUniformValue(effect, "Intensity", blurFactor);
                  this.safeSetUniformValue(effect, "Samples", maxEnabled ? 16.0F : 8.0F);
               }

               try {
                  Framebuffer MCBuffer = MinecraftClient.getInstance().getFramebuffer();
                  if (this.motionBlurBuffer == null) {
                     this.motionBlurBuffer = new MyFramebuffer(MCBuffer.textureWidth, MCBuffer.textureHeight);
                  }

                  if (this.motionBlurBuffer.textureWidth != MCBuffer.textureWidth || this.motionBlurBuffer.textureHeight != MCBuffer.textureHeight) {
                     this.motionBlurBuffer.resize(MCBuffer.textureWidth, MCBuffer.textureHeight, false);
                  }

                  PostEffectProcessor processor = effect.getShaderEffect();
                  if (processor != null) {
                     try {
                        ((IShaderEffectHook)processor).alienClient$addHook("bufIn", MCBuffer);
                     } catch (Exception var8) {
                     }
                  }

                  effect.render(mc.getRenderTickCounter().getTickDelta(true));
               } catch (Exception var9) {
               }

            }
         }
      }
   }

   public void applyShader(Runnable runnable, Shader mode) {
      if (!this.fullNullCheck()) {
         RenderSystem.assertOnRenderThreadOrInit();
         Framebuffer MCBuffer = MinecraftClient.getInstance().getFramebuffer();
         if (this.shaderBuffer == null) {
            this.shaderBuffer = new MyFramebuffer(MCBuffer.textureWidth, MCBuffer.textureHeight);
         }

         if (this.shaderBuffer.textureWidth != MCBuffer.textureWidth || this.shaderBuffer.textureHeight != MCBuffer.textureHeight) {
            this.shaderBuffer.resize(MCBuffer.textureWidth, MCBuffer.textureHeight, false);
         }

         GlStateManager._glBindFramebuffer(36009, this.shaderBuffer.fbo);
         this.shaderBuffer.beginWrite(true);
         runnable.run();
         this.shaderBuffer.endWrite();
         GlStateManager._glBindFramebuffer(36009, MCBuffer.fbo);
         MCBuffer.beginWrite(false);
         ManagedShaderEffect shader = this.getShader(mode);
         if (shader != null) {
            PostEffectProcessor effect = shader.getShaderEffect();
            if (effect != null) {
               try {
                  ((IShaderEffectHook)effect).alienClient$addHook("bufIn", this.shaderBuffer);
               } catch (Exception var7) {
               }
            }

            Framebuffer outBuffer = shader.getShaderEffect().getSecondaryTarget("bufOut");
            if (outBuffer != null) {
               this.setupShader(mode, shader);
               this.shaderBuffer.clear(false);
               MCBuffer.beginWrite(false);
               RenderSystem.enableBlend();
               RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);
               RenderSystem.backupProjectionMatrix();
               outBuffer.draw(outBuffer.textureWidth, outBuffer.textureHeight, false);
               RenderSystem.restoreProjectionMatrix();
               RenderSystem.disableBlend();
            }
         }
      }
   }

   public ManagedShaderEffect getShader(@NotNull Shader mode) {
      ManagedShaderEffect var10000;
      switch (mode.ordinal()) {
         case 0 -> var10000 = DEFAULT;
         case 1 -> var10000 = SMOKE;
         case 2 -> var10000 = GRADIENT;
         case 3 -> var10000 = SNOW;
         case 4 -> var10000 = FLOW;
         case 5 -> var10000 = RAINBOW;
         default -> var10000 = DEFAULT;
      }

      return var10000;
   }

   public ManagedShaderEffect getShaderOutline(@NotNull Shader mode) {
      ManagedShaderEffect var10000;
      switch (mode.ordinal()) {
         case 0 -> var10000 = DEFAULT_OUTLINE;
         case 1 -> var10000 = SMOKE_OUTLINE;
         case 2 -> var10000 = GRADIENT_OUTLINE;
         case 3 -> var10000 = SNOW_OUTLINE;
         case 4 -> var10000 = FLOW_OUTLINE;
         case 5 -> var10000 = RAINBOW_OUTLINE;
         default -> var10000 = DEFAULT_OUTLINE;
      }

      return var10000;
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

   private void safeSetUniformValue(ManagedShaderEffect effect, String name, float a, float b, float c) {
      try {
         effect.setUniformValue(name, a, b, c);
      } catch (Exception var7) {
      }

   }

   private void safeSetUniformValue(ManagedShaderEffect effect, String name, float a, float b, float c, float d) {
      try {
         effect.setUniformValue(name, a, b, c, d);
      } catch (Exception var8) {
      }

   }

   private void safeSetUniformValue(ManagedShaderEffect effect, String name, int value) {
      try {
         effect.setUniformValue(name, value);
      } catch (Exception var5) {
      }

   }

   public void renderSkyboxFullscreen(Color color, Color backgroundColor, float scale, float speed, float opacity) {
      if (!this.fullNullCheck()) {
         RenderSystem.assertOnRenderThreadOrInit();
         ManagedShaderEffect shader = SKYBOX;
         if (shader != null && shader.getShaderEffect() != null) {
            Framebuffer MCBuffer = MinecraftClient.getInstance().getFramebuffer();
            PostEffectProcessor effect = shader.getShaderEffect();
            if (effect != null) {
               try {
                  ((IShaderEffectHook)effect).alienClient$addHook("bufIn", MCBuffer);
               } catch (Exception var13) {
               }
            }

            Framebuffer outBuffer = shader.getShaderEffect().getSecondaryTarget("bufOut");
            if (outBuffer != null) {
               float t = (float)timer.getMs() / 5.0F * speed * 0.004F;
               this.safeSetUniformValue(shader, "mixFactor", opacity);
               this.safeSetUniformValue(shader, "u_Color", (float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, (float)color.getAlpha() / 255.0F);
               this.safeSetUniformValue(shader, "u_BackgroundColor", (float)backgroundColor.getRed() / 255.0F, (float)backgroundColor.getGreen() / 255.0F, (float)backgroundColor.getBlue() / 255.0F, (float)backgroundColor.getAlpha() / 255.0F);
               this.safeSetUniformValue(shader, "u_Resolution", (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
               this.safeSetUniformValue(shader, "u_Mouse", 0.0F, 0.0F);
               this.safeSetUniformValue(shader, "u_Scale", scale);
               this.safeSetUniformValue(shader, "u_Time", t);

               try {
                  shader.render(mc.getRenderTickCounter().getTickDelta(true));
               } catch (Exception var12) {
               }

               MCBuffer.beginWrite(false);
               RenderSystem.enableBlend();
               RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);
               RenderSystem.backupProjectionMatrix();
               outBuffer.draw(outBuffer.textureWidth, outBuffer.textureHeight, false);
               RenderSystem.restoreProjectionMatrix();
               RenderSystem.disableBlend();
            }
         }
      }
   }

   public void setupShader(Shader shader, ManagedShaderEffect effect) {
      if (effect != null && effect.getShaderEffect() != null) {
         try {
            ShaderModule module = ShaderModule.INSTANCE;
            if (module == null) {
               return;
            }

            Color color = module.fill.getValue();
            this.time = (float)timer.getMs() / 5.0F * module.speed.getValueFloat() * 0.004F;
            if (shader == ShaderManager.Shader.Rainbow) {
               this.safeSetUniformValue(effect, "alpha2", (float)color.getAlpha() / 255.0F);
               this.safeSetUniformValue(effect, "radius", module.radius.getValueFloat());
               this.safeSetUniformValue(effect, "quality", module.smoothness.getValueFloat());
               this.safeSetUniformValue(effect, "divider", module.divider.getValueFloat());
               this.safeSetUniformValue(effect, "maxSample", module.maxSample.getValueFloat());
               this.safeSetUniformValue(effect, "resolution", (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
               this.safeSetUniformValue(effect, "time", this.time);
               effect.render(mc.getRenderTickCounter().getTickDelta(true));
            } else if (shader == ShaderManager.Shader.Gradient) {
               this.safeSetUniformValue(effect, "alpha2", (float)color.getAlpha() / 255.0F);
               this.safeSetUniformValue(effect, "rgb", (float)module.smoke1.getValue().getRed() / 255.0F, (float)module.smoke1.getValue().getGreen() / 255.0F, (float)module.smoke1.getValue().getBlue() / 255.0F);
               this.safeSetUniformValue(effect, "rgb1", (float)module.smoke2.getValue().getRed() / 255.0F, (float)module.smoke2.getValue().getGreen() / 255.0F, (float)module.smoke2.getValue().getBlue() / 255.0F);
               this.safeSetUniformValue(effect, "rgb2", (float)module.smoke3.getValue().getRed() / 255.0F, (float)module.smoke3.getValue().getGreen() / 255.0F, (float)module.smoke3.getValue().getBlue() / 255.0F);
               this.safeSetUniformValue(effect, "rgb3", (float)module.smoke4.getValue().getRed() / 255.0F, (float)module.smoke4.getValue().getGreen() / 255.0F, (float)module.smoke4.getValue().getBlue() / 255.0F);
               this.safeSetUniformValue(effect, "step", module.step.getValueFloat() * 300.0F);
               this.safeSetUniformValue(effect, "radius", module.radius.getValueFloat());
               this.safeSetUniformValue(effect, "quality", module.smoothness.getValueFloat());
               this.safeSetUniformValue(effect, "divider", module.divider.getValueFloat());
               this.safeSetUniformValue(effect, "maxSample", module.maxSample.getValueFloat());
               this.safeSetUniformValue(effect, "resolution", (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
               this.safeSetUniformValue(effect, "time", this.time * 300.0F);
               effect.render(mc.getRenderTickCounter().getTickDelta(true));
            } else if (shader == ShaderManager.Shader.Smoke) {
               this.safeSetUniformValue(effect, "alpha1", (float)color.getAlpha() / 255.0F);
               this.safeSetUniformValue(effect, "radius", module.radius.getValueFloat());
               this.safeSetUniformValue(effect, "quality", module.smoothness.getValueFloat());
               this.safeSetUniformValue(effect, "divider", module.divider.getValueFloat());
               this.safeSetUniformValue(effect, "maxSample", module.maxSample.getValueFloat());
               this.safeSetUniformValue(effect, "first", (float)module.smoke1.getValue().getRed() / 255.0F, (float)module.smoke1.getValue().getGreen() / 255.0F, (float)module.smoke1.getValue().getBlue() / 255.0F, (float)module.smoke1.getValue().getAlpha() / 255.0F);
               this.safeSetUniformValue(effect, "second", (float)module.smoke2.getValue().getRed() / 255.0F, (float)module.smoke2.getValue().getGreen() / 255.0F, (float)module.smoke2.getValue().getBlue() / 255.0F);
               this.safeSetUniformValue(effect, "third", (float)module.smoke3.getValue().getRed() / 255.0F, (float)module.smoke3.getValue().getGreen() / 255.0F, (float)module.smoke3.getValue().getBlue() / 255.0F);
               this.safeSetUniformValue(effect, "oct", (int)module.octaves.getValue());
               this.safeSetUniformValue(effect, "resolution", (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
               this.safeSetUniformValue(effect, "time", this.time);
               effect.render(mc.getRenderTickCounter().getTickDelta(true));
            } else if (shader == ShaderManager.Shader.Solid) {
               this.safeSetUniformValue(effect, "fill", 1.0F);
               this.safeSetUniformValue(effect, "mixFactor", (float)color.getAlpha() / 255.0F);
               this.safeSetUniformValue(effect, "minAlpha", module.alpha.getValueFloat() / 255.0F);
               this.safeSetUniformValue(effect, "radius", module.radius.getValueFloat());
               this.safeSetUniformValue(effect, "quality", module.smoothness.getValueFloat());
               this.safeSetUniformValue(effect, "divider", module.divider.getValueFloat());
               this.safeSetUniformValue(effect, "maxSample", module.maxSample.getValueFloat());
               this.safeSetUniformValue(effect, "color", (float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F);
               this.safeSetUniformValue(effect, "resolution", (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
               effect.render(mc.getRenderTickCounter().getTickDelta(true));
            } else if (shader == ShaderManager.Shader.Snow) {
               this.safeSetUniformValue(effect, "color", (float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, (float)color.getAlpha() / 255.0F);
               this.safeSetUniformValue(effect, "radius", module.radius.getValueFloat());
               this.safeSetUniformValue(effect, "quality", module.smoothness.getValueFloat());
               this.safeSetUniformValue(effect, "divider", module.divider.getValueFloat());
               this.safeSetUniformValue(effect, "maxSample", module.maxSample.getValueFloat());
               this.safeSetUniformValue(effect, "resolution", (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
               this.safeSetUniformValue(effect, "time", this.time);
               effect.render(mc.getRenderTickCounter().getTickDelta(true));
            } else if (shader == ShaderManager.Shader.Flow) {
               this.safeSetUniformValue(effect, "mixFactor", (float)color.getAlpha() / 255.0F);
               this.safeSetUniformValue(effect, "radius", module.radius.getValueFloat());
               this.safeSetUniformValue(effect, "quality", module.smoothness.getValueFloat());
               this.safeSetUniformValue(effect, "divider", module.divider.getValueFloat());
               this.safeSetUniformValue(effect, "maxSample", module.maxSample.getValueFloat());
               this.safeSetUniformValue(effect, "resolution", (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
               this.safeSetUniformValue(effect, "time", this.time);
               effect.render(mc.getRenderTickCounter().getTickDelta(true));
            }
         } catch (Exception var5) {
         }

      }
   }

   public void reloadShaders() {
      try {
         DEFAULT = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/outline.json"));
         SMOKE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/smoke.json"));
         GRADIENT = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/gradient.json"));
         SNOW = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/snow.json"));
         FLOW = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/flow.json"));
         RAINBOW = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/rainbow.json"));
         SKYBOX = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/skybox.json"));
         MOTION_BLUR = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/motion_blur.json"));
         DEFAULT_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/outline.json"), (managedShaderEffect) -> {
            PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
            if (effect != null) {
               ((IShaderEffectHook)effect).alienClient$addHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
               ((IShaderEffectHook)effect).alienClient$addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
            }

         });
         SMOKE_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/smoke.json"), (managedShaderEffect) -> {
            PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
            if (effect != null) {
               ((IShaderEffectHook)effect).alienClient$addHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
               ((IShaderEffectHook)effect).alienClient$addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
            }

         });
         GRADIENT_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/gradient.json"), (managedShaderEffect) -> {
            PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
            if (effect != null) {
               ((IShaderEffectHook)effect).alienClient$addHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
               ((IShaderEffectHook)effect).alienClient$addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
            }

         });
         SNOW_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/snow.json"), (managedShaderEffect) -> {
            PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
            if (effect != null) {
               ((IShaderEffectHook)effect).alienClient$addHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
               ((IShaderEffectHook)effect).alienClient$addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
            }

         });
         FLOW_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/flow.json"), (managedShaderEffect) -> {
            PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
            if (effect != null) {
               ((IShaderEffectHook)effect).alienClient$addHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
               ((IShaderEffectHook)effect).alienClient$addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
            }

         });
         RAINBOW_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/rainbow.json"), (managedShaderEffect) -> {
            PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
            if (effect != null) {
               ((IShaderEffectHook)effect).alienClient$addHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
               ((IShaderEffectHook)effect).alienClient$addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
            }

         });
      } catch (Exception var2) {
      }

   }

   public boolean fullNullCheck() {
      if (mc != null && mc.getFramebuffer() != null) {
         if (DEFAULT != null && SMOKE != null && GRADIENT != null && SNOW != null && FLOW != null && RAINBOW != null && SKYBOX != null && MOTION_BLUR != null && DEFAULT_OUTLINE != null && SMOKE_OUTLINE != null && GRADIENT_OUTLINE != null && SNOW_OUTLINE != null && FLOW_OUTLINE != null && RAINBOW_OUTLINE != null && this.shaderBuffer != null) {
            return false;
         } else {
            if (this.shaderBuffer == null) {
               this.shaderBuffer = new MyFramebuffer(mc.getFramebuffer().textureWidth, mc.getFramebuffer().textureHeight);
            }

            this.reloadShaders();
            return true;
         }
      } else {
         return true;
      }
   }

   public static class MyFramebuffer extends Framebuffer {
      public MyFramebuffer(int width, int height) {
         super(false);
         RenderSystem.assertOnRenderThreadOrInit();
         this.resize(width, height, true);
         this.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
      }
   }

   public static record RenderTask(Runnable task, Shader shader) {
   }

   public static enum Shader {
      Solid,
      Smoke,
      Gradient,
      Snow,
      Flow,
      Rainbow;

      // $FF: synthetic method
      private static Shader[] $values() {
         return new Shader[]{Solid, Smoke, Gradient, Snow, Flow, Rainbow};
      }
   }
}
