package dev.g255.arc.core.impl;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.g255.arc.api.interfaces.IShaderEffectHook;
import dev.g255.arc.api.utils.Wrapper;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.mod.modules.impl.render.ShaderModule;
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
   public static ManagedShaderEffect PULSE_OUTLINE;
   public static ManagedShaderEffect SMOKE_OUTLINE;
   public static ManagedShaderEffect GRADIENT_OUTLINE;
   public static ManagedShaderEffect SNOW_OUTLINE;
   public static ManagedShaderEffect FLOW_OUTLINE;
   public static ManagedShaderEffect RAINBOW_OUTLINE;
   public static ManagedShaderEffect DEFAULT;
   public static ManagedShaderEffect PULSE;
   public static ManagedShaderEffect SMOKE;
   public static ManagedShaderEffect GRADIENT;
   public static ManagedShaderEffect SNOW;
   public static ManagedShaderEffect FLOW;
   public static ManagedShaderEffect RAINBOW;
   /** 主菜单/所有非游戏内界面的极光背景（Hachimi 同款 post effect）。 */
   public static ManagedShaderEffect MENU;
   public float time = 0.0F;
   private boolean shaderDisabled = false;
   private MyFramebuffer shaderBuffer;

   public void renderShader(Runnable runnable, Shader mode) {
      tasks.add(new RenderTask(runnable, mode));
   }

   public void renderShaders() {
      if (!this.shaderDisabled) {
         try {
            tasks.forEach((t) -> this.applyShader(t.task(), t.shader()));
            tasks.clear();
         } catch (Exception var2) {
            this.shaderDisabled = true;
            tasks.clear();
         }

      }
   }

   public void applyShader(Runnable runnable, Shader mode) {
      if (!this.shaderDisabled && !this.fullNullCheck()) {
         try {
            RenderSystem.assertOnRenderThreadOrInit();
            Framebuffer MCBuffer = MinecraftClient.getInstance().getFramebuffer();
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
            if (shader == null) {
               return;
            }

            PostEffectProcessor effect = shader.getShaderEffect();
            if (effect == null) {
               return;
            }

            ((IShaderEffectHook)effect).arc$addHook("bufIn", this.shaderBuffer);
            Framebuffer outBuffer = effect.getSecondaryTarget("bufOut");
            if (outBuffer == null) {
               return;
            }

            this.setupShader(mode, shader);
            this.shaderBuffer.clear(false);
            MCBuffer.beginWrite(false);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA, SrcFactor.ZERO, DstFactor.ONE);
            RenderSystem.backupProjectionMatrix();
            outBuffer.draw(outBuffer.textureWidth, outBuffer.textureHeight, false);
            RenderSystem.restoreProjectionMatrix();
            RenderSystem.disableBlend();
         } catch (Exception var7) {
            this.shaderDisabled = true;
         }

      }
   }

   public ManagedShaderEffect getShader(@NotNull Shader mode) {
      ManagedShaderEffect var10000;
      switch (mode.ordinal()) {
         case 1 -> var10000 = PULSE;
         case 2 -> var10000 = SMOKE;
         case 3 -> var10000 = GRADIENT;
         case 4 -> var10000 = SNOW;
         case 5 -> var10000 = FLOW;
         case 6 -> var10000 = RAINBOW;
         default -> var10000 = DEFAULT;
      }

      return var10000;
   }

   public ManagedShaderEffect getShaderOutline(@NotNull Shader mode) {
      ManagedShaderEffect var10000;
      switch (mode.ordinal()) {
         case 1 -> var10000 = PULSE_OUTLINE;
         case 2 -> var10000 = SMOKE_OUTLINE;
         case 3 -> var10000 = GRADIENT_OUTLINE;
         case 4 -> var10000 = SNOW_OUTLINE;
         case 5 -> var10000 = FLOW_OUTLINE;
         case 6 -> var10000 = RAINBOW_OUTLINE;
         default -> var10000 = DEFAULT_OUTLINE;
      }

      return var10000;
   }

   public void setupShader(Shader shader, ManagedShaderEffect effect) {
      ShaderModule module = ShaderModule.INSTANCE;
      Color color = module.fill.getValue();
      this.time = (float)timer.getMs() / 5.0F * module.speed.getValueFloat() * 0.004F;
      if (shader == ShaderManager.Shader.Rainbow) {
         safeSetUniformValue(effect, "alpha2", (float)color.getAlpha() / 255.0F);
         safeSetUniformValue(effect, "radius", module.radius.getValueFloat());
         safeSetUniformValue(effect, "quality", module.smoothness.getValueFloat());
         safeSetUniformValue(effect, "divider", module.divider.getValueFloat());
         safeSetUniformValue(effect, "maxSample", module.maxSample.getValueFloat());
         safeSetUniformValue(effect, "resolution", (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
         safeSetUniformValue(effect, "time", this.time);
         renderEffect(effect);
      } else if (shader == ShaderManager.Shader.Gradient) {
         // gradient.fsh 只声明了 alpha2/factor/moreGradient/oct 等 uniform，没有 rgb/rgb1..3/step。
         // 原来这里照抄了主菜单渐变着色器的 uniform，Satin 找不到 uniform 会直接抛异常
         // （世界加载时崩在 GlUniform.set），所以只设置 gradient.fsh 真实存在的项。
         safeSetUniformValue(effect, "alpha2", (float)color.getAlpha() / 255.0F);
         safeSetUniformValue(effect, "radius", module.radius.getValueFloat());
         safeSetUniformValue(effect, "quality", module.smoothness.getValueFloat());
         safeSetUniformValue(effect, "divider", module.divider.getValueFloat());
         safeSetUniformValue(effect, "maxSample", module.maxSample.getValueFloat());
         safeSetUniformValue(effect, "resolution", (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
         safeSetUniformValue(effect, "time", this.time * 300.0F);
         // gradient.fsh 的 fbm 要靠 oct 才会真正采样，p 的缩放靠 moreGradient，warp 强度靠 factor。
         // 这三项不传就是 0：循环次数为 0、坐标恒为 0，整块区域会退化成一片死色。
         safeSetUniformValue(effect, "oct", module.octaves.getValueFloat());
         safeSetUniformValue(effect, "factor", 1.0F);
         safeSetUniformValue(effect, "moreGradient", 1.0F);
         renderEffect(effect);
      } else if (shader == ShaderManager.Shader.Smoke) {
         safeSetUniformValue(effect, "alpha1", (float)color.getAlpha() / 255.0F);
         safeSetUniformValue(effect, "radius", module.radius.getValueFloat());
         safeSetUniformValue(effect, "quality", module.smoothness.getValueFloat());
         safeSetUniformValue(effect, "divider", module.divider.getValueFloat());
         safeSetUniformValue(effect, "maxSample", module.maxSample.getValueFloat());
         safeSetUniformValue(effect, "first", (float)module.smoke1.getValue().getRed() / 255.0F, (float)module.smoke1.getValue().getGreen() / 255.0F, (float)module.smoke1.getValue().getBlue() / 255.0F, (float)module.smoke1.getValue().getAlpha() / 255.0F);
         safeSetUniformValue(effect, "second", (float)module.smoke2.getValue().getRed() / 255.0F, (float)module.smoke2.getValue().getGreen() / 255.0F, (float)module.smoke2.getValue().getBlue() / 255.0F);
         safeSetUniformValue(effect, "third", (float)module.smoke3.getValue().getRed() / 255.0F, (float)module.smoke3.getValue().getGreen() / 255.0F, (float)module.smoke3.getValue().getBlue() / 255.0F);
         safeSetUniformValue(effect, "oct", module.octaves.getValueFloat());
         safeSetUniformValue(effect, "resolution", (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
         safeSetUniformValue(effect, "time", this.time);
         renderEffect(effect);
      } else if (shader == ShaderManager.Shader.Solid) {
         safeSetUniformValue(effect, "mixFactor", (float)color.getAlpha() / 255.0F);
         safeSetUniformValue(effect, "minAlpha", module.alpha.getValueFloat() / 255.0F);
         safeSetUniformValue(effect, "radius", module.radius.getValueFloat());
         safeSetUniformValue(effect, "quality", module.smoothness.getValueFloat());
         safeSetUniformValue(effect, "divider", module.divider.getValueFloat());
         safeSetUniformValue(effect, "maxSample", module.maxSample.getValueFloat());
         safeSetUniformValue(effect, "color", (float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F);
         safeSetUniformValue(effect, "resolution", (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
         renderEffect(effect);
      } else if (shader == ShaderManager.Shader.Pulse) {
         safeSetUniformValue(effect, "mixFactor", (float)color.getAlpha() / 255.0F);
         safeSetUniformValue(effect, "minAlpha", module.alpha.getValueFloat() / 255.0F);
         safeSetUniformValue(effect, "radius", module.radius.getValueFloat());
         safeSetUniformValue(effect, "quality", module.smoothness.getValueFloat());
         safeSetUniformValue(effect, "divider", module.divider.getValueFloat());
         safeSetUniformValue(effect, "maxSample", module.maxSample.getValueFloat());
         safeSetUniformValue(effect, "color", (float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F);
         Color color2 = module.pulse.getValue();
         safeSetUniformValue(effect, "color2", (float)color2.getRed() / 255.0F, (float)color2.getGreen() / 255.0F, (float)color2.getBlue() / 255.0F);
         safeSetUniformValue(effect, "time", this.time);
         safeSetUniformValue(effect, "size", module.pulseSpeed.getValueFloat());
         safeSetUniformValue(effect, "resolution", (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
         renderEffect(effect);
      } else if (shader == ShaderManager.Shader.Snow) {
         safeSetUniformValue(effect, "color", (float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, (float)color.getAlpha() / 255.0F);
         safeSetUniformValue(effect, "radius", module.radius.getValueFloat());
         safeSetUniformValue(effect, "quality", module.smoothness.getValueFloat());
         safeSetUniformValue(effect, "divider", module.divider.getValueFloat());
         safeSetUniformValue(effect, "maxSample", module.maxSample.getValueFloat());
         safeSetUniformValue(effect, "resolution", (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
         safeSetUniformValue(effect, "time", this.time);
         renderEffect(effect);
      } else if (shader == ShaderManager.Shader.Flow) {
         safeSetUniformValue(effect, "mixFactor", (float)color.getAlpha() / 255.0F);
         safeSetUniformValue(effect, "radius", module.radius.getValueFloat());
         safeSetUniformValue(effect, "quality", module.smoothness.getValueFloat());
         safeSetUniformValue(effect, "divider", module.divider.getValueFloat());
         safeSetUniformValue(effect, "maxSample", module.maxSample.getValueFloat());
         safeSetUniformValue(effect, "resolution", (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
         safeSetUniformValue(effect, "time", this.time);
         renderEffect(effect);
      }

   }

   public void reloadShaders() {
      DEFAULT = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/outline.json"));
      SMOKE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/smoke.json"));
      GRADIENT = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/gradient.json"));
      SNOW = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/snow.json"));
      FLOW = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/flow.json"));
      RAINBOW = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/rainbow.json"));
      PULSE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/pulse.json"));
      // 主菜单极光：和 Hachimi 一样走 arc 命名空间的 post effect
      // （shaders/post/menu.json → shaders/program/menu.json + menu.fsh）
      MENU = ShaderEffectManager.getInstance().manage(Identifier.of("arc", "shaders/post/menu.json"));
      DEFAULT_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/outline.json"), (managedShaderEffect) -> {
         PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
         if (effect != null) {
            ((IShaderEffectHook)effect).arc$addHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffectHook)effect).arc$addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
         }
      });
      PULSE_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/pulse.json"), (managedShaderEffect) -> {
         PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
         if (effect != null) {
            ((IShaderEffectHook)effect).arc$addHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffectHook)effect).arc$addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
         }
      });
      SMOKE_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/smoke.json"), (managedShaderEffect) -> {
         PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
         if (effect != null) {
            ((IShaderEffectHook)effect).arc$addHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffectHook)effect).arc$addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
         }
      });
      GRADIENT_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/gradient.json"), (managedShaderEffect) -> {
         PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
         if (effect != null) {
            ((IShaderEffectHook)effect).arc$addHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffectHook)effect).arc$addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
         }
      });
      SNOW_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/snow.json"), (managedShaderEffect) -> {
         PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
         if (effect != null) {
            ((IShaderEffectHook)effect).arc$addHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffectHook)effect).arc$addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
         }
      });
      FLOW_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/flow.json"), (managedShaderEffect) -> {
         PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
         if (effect != null) {
            ((IShaderEffectHook)effect).arc$addHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffectHook)effect).arc$addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
         }
      });
      RAINBOW_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/rainbow.json"), (managedShaderEffect) -> {
         PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
         if (effect != null) {
            ((IShaderEffectHook)effect).arc$addHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffectHook)effect).arc$addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
         }
      });
   }

   /**
    * 所有 uniform 都必须走这里。
    *
    * Satin 的 setUniformValue 最终落到 GlUniform.set，而 GlUniform 按 GLSL 里声明的真实类型持有
    * floatData / intData：参数类型对不上（例如 oct 在 fsh 里是 float，却传了 int）或者名字在 fsh
    * 里根本不存在时，Satin 会抛异常。这条调用链挂在 WorldRenderer 的 redirect 上，异常没人接手就
    * 直接把渲染线程打死（历史崩溃：NullPointerException: this.intData is null at GlUniform.set）。
    * 所以这里兜住所有 Throwable：最多丢一个 uniform，绝不让游戏崩。
    */
   private void safeSetUniformValue(ManagedShaderEffect shader, String name, float v1) {
      try {
         shader.setUniformValue(name, v1);
      } catch (Throwable t) {
         this.reportUniformFailure(name, t);
      }

   }

   private void safeSetUniformValue(ManagedShaderEffect shader, String name, float v1, float v2) {
      try {
         shader.setUniformValue(name, v1, v2);
      } catch (Throwable t) {
         this.reportUniformFailure(name, t);
      }

   }

   private void safeSetUniformValue(ManagedShaderEffect shader, String name, float v1, float v2, float v3) {
      try {
         shader.setUniformValue(name, v1, v2, v3);
      } catch (Throwable t) {
         this.reportUniformFailure(name, t);
      }

   }

   private void safeSetUniformValue(ManagedShaderEffect shader, String name, float v1, float v2, float v3, float v4) {
      try {
         shader.setUniformValue(name, v1, v2, v3, v4);
      } catch (Throwable t) {
         this.reportUniformFailure(name, t);
      }

   }

   private void renderEffect(ManagedShaderEffect shader) {
      try {
         shader.render(mc.getRenderTickCounter().getTickDelta(true));
      } catch (Throwable t) {
         this.reportUniformFailure("render", t);
      }

   }

   private static final java.util.Set<String> WARNED_UNIFORMS = new java.util.HashSet<>();

   private void reportUniformFailure(String name, Throwable t) {
      if (WARNED_UNIFORMS.add(name)) {
         System.out.println("[Arc] skipped bad shader uniform '" + name + "' -> " + t);
      }

   }

   public boolean fullNullCheck() {
      if (GRADIENT != null && SMOKE != null && DEFAULT != null && FLOW != null && RAINBOW != null && PULSE != null && PULSE_OUTLINE != null && GRADIENT_OUTLINE != null && SMOKE_OUTLINE != null && DEFAULT_OUTLINE != null && FLOW_OUTLINE != null && RAINBOW_OUTLINE != null && this.shaderBuffer != null) {
         return false;
      } else if (mc.getFramebuffer() == null) {
         return true;
      } else {
         this.shaderBuffer = new MyFramebuffer(mc.getFramebuffer().textureWidth, mc.getFramebuffer().textureHeight);
         this.reloadShaders();
         return true;
      }
   }

   public static record RenderTask(Runnable task, Shader shader) {
   }

   public static enum Shader {
      Solid,
      Pulse,
      Smoke,
      Gradient,
      Snow,
      Flow,
      Rainbow;

      // $FF: synthetic method
      private static Shader[] $values() {
         return new Shader[]{Solid, Pulse, Smoke, Gradient, Snow, Flow, Rainbow};
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
}
