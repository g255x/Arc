package hoprc.api.core.impl;

import dev.kissoo.core.impl.ShaderParams;
import hoprc.obf.zkm.NoFlowObfuscate;
import hoprc.obf.zkm.NoReferencesObfuscate;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import hoprc.api.interfaces.IShaderEffectHook;
import dev.kissoo.api.utils.Wrapper;
import dev.kissoo.api.utils.math.Timer;
import dev.kissoo.mod.modules.impl.render.Ambience;
import dev.kissoo.mod.modules.impl.render.ShaderModule;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.satin.api.managed.ManagedShaderEffect;
import org.ladysnake.satin.api.managed.ShaderEffectManager;

@NoFlowObfuscate
@NoReferencesObfuscate
public class ShaderManager implements Wrapper {
   static final Timer timer = new Timer();
   private static final List<RenderTask> tasks = new ArrayList<>();

   public static ManagedShaderEffect DEFAULT_OUTLINE;
   public static ManagedShaderEffect PULSE_OUTLINE;
   public static ManagedShaderEffect SMOKE_OUTLINE;
   public static ManagedShaderEffect GRADIENT_OUTLINE;
   public static ManagedShaderEffect SNOW_OUTLINE;
   public static ManagedShaderEffect FLOW_OUTLINE;
   public static ManagedShaderEffect RAINBOW_OUTLINE;
   public static ManagedShaderEffect BLOOM_OUTLINE;

   public static ManagedShaderEffect DEFAULT;
   public static ManagedShaderEffect PULSE;
   public static ManagedShaderEffect SMOKE;
   public static ManagedShaderEffect GRADIENT;
   public static ManagedShaderEffect SNOW;
   public static ManagedShaderEffect FLOW;
   public static ManagedShaderEffect RAINBOW;
   public static ManagedShaderEffect CINE;
   public static ManagedShaderEffect BLOOM;

   public static ManagedShaderEffect AURORA;
   public static ManagedShaderEffect NEBULA;
   public static ManagedShaderEffect SUNSET;
   public static ManagedShaderEffect VORTEX;
   public static ManagedShaderEffect WAVES;
   public static ManagedShaderEffect CRYSTAL;
   public static ManagedShaderEffect LOCAL;

   public float time = 0.0F;
   private MyFramebuffer shaderBuffer;

   public void renderShader(Runnable runnable, Shader mode) {
      tasks.add(new RenderTask(runnable, mode, null));
   }

   public void renderShader(Runnable runnable, Shader mode, ShaderParams params) {
      tasks.add(new RenderTask(runnable, mode, params));
   }

   public void renderShaders() {
      tasks.forEach(t -> this.applyShader(t.task(), t.shader(), t.params()));
      tasks.clear();
   }

   public void applyShaderRects(
           List<float[]> rects,
           Shader mode,
           net.minecraft.client.util.math.MatrixStack matrices,
           ShaderParams customParams
   ) {
      if (this.fullNullCheck()) return;
      RenderSystem.assertOnRenderThreadOrInit();
      if (this.shaderBuffer == null || this.shaderBuffer.fbo == -1) return;

      Framebuffer MCBuffer = MinecraftClient.getInstance().getFramebuffer();
      int targetWidth = Math.max(MCBuffer.textureWidth, 1);
      int targetHeight = Math.max(MCBuffer.textureHeight, 1);

      if (this.shaderBuffer.textureWidth != targetWidth
              || this.shaderBuffer.textureHeight != targetHeight) {
         this.shaderBuffer.resize(targetWidth, targetHeight, false);
      }
      if (this.shaderBuffer.fbo == -1) return;

      GlStateManager._glBindFramebuffer(36009, this.shaderBuffer.fbo);
      this.shaderBuffer.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
      this.shaderBuffer.beginWrite(true);

      for (float[] r : rects) {
         dev.kissoo.api.utils.render.Render2DUtil.drawRect(
                 matrices,
                 r[0], r[1], r[2], r[3],
                 new java.awt.Color(255, 255, 255, 255).getRGB()
         );
      }

      this.shaderBuffer.endWrite();
      GlStateManager._glBindFramebuffer(36009, MCBuffer.fbo);
      MCBuffer.beginWrite(false);

      ManagedShaderEffect shader = this.getShader(mode);
      if (shader == null) shader = DEFAULT;

      PostEffectProcessor effect = shader.getShaderEffect();
      if (effect == null) return;

      ((IShaderEffectHook) effect).alienClient$addHook("bufIn", this.shaderBuffer);

      Framebuffer outBuffer = effect.getSecondaryTarget("bufOut");
      if (outBuffer == null) return;
      this.setupShaderWithParams(mode, shader, customParams, false);

      this.shaderBuffer.clear(false);
      MCBuffer.beginWrite(false);
      RenderSystem.enableBlend();
      RenderSystem.blendFuncSeparate(
              SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA,
              SrcFactor.ZERO, DstFactor.ONE
      );
      RenderSystem.backupProjectionMatrix();
      outBuffer.draw(outBuffer.textureWidth, outBuffer.textureHeight, false);
      RenderSystem.restoreProjectionMatrix();
      RenderSystem.disableBlend();
   }

   public void applyShaderRects(
           List<float[]> rects,
           Shader mode,
           net.minecraft.client.util.math.MatrixStack matrices
   ) {
      ShaderModule module = ShaderModule.INSTANCE;
      ShaderParams params = module != null
              ? ShaderParams.fromModule(module)
              : null;
      this.applyShaderRects(rects, mode, matrices, params);
   }

   public void applyShader(Runnable runnable, Shader mode) {
      this.applyShader(runnable, mode, null);
   }

   public void applyShader(Runnable runnable, Shader mode, ShaderParams customParams) {
      this.applyShaderInternal(runnable, mode, customParams, false);
   }

   /** 逐实体轮廓着色器：捕获实体后套轮廓变体（只描边，身体透明 → 原始外观透出） */
   public void applyOutlineShader(Runnable runnable, Shader mode, ShaderParams customParams) {
      this.applyShaderInternal(runnable, mode, customParams, true);
   }

   private void applyShaderInternal(Runnable runnable, Shader mode, ShaderParams customParams, boolean outline) {
      if (!this.fullNullCheck()) {
         RenderSystem.assertOnRenderThreadOrInit();
         if (this.shaderBuffer == null || this.shaderBuffer.fbo == -1) return;

         Framebuffer MCBuffer = MinecraftClient.getInstance().getFramebuffer();
         int targetWidth = Math.max(MCBuffer.textureWidth, 1);
         int targetHeight = Math.max(MCBuffer.textureHeight, 1);

         if (this.shaderBuffer.textureWidth != targetWidth
                 || this.shaderBuffer.textureHeight != targetHeight) {
            this.shaderBuffer.resize(targetWidth, targetHeight, false);
         }
         if (this.shaderBuffer.fbo == -1) return;

         GlStateManager._glBindFramebuffer(36009, this.shaderBuffer.fbo);
         this.shaderBuffer.beginWrite(true);
         runnable.run();
         this.shaderBuffer.endWrite();
         GlStateManager._glBindFramebuffer(36009, MCBuffer.fbo);
         MCBuffer.beginWrite(false);

         ManagedShaderEffect shader = outline ? this.getShaderOutline(mode) : this.getShader(mode);
         PostEffectProcessor effect = shader.getShaderEffect();
         if (effect != null) {
            ((IShaderEffectHook) effect).alienClient$addHook("bufIn", this.shaderBuffer);
         }

         Framebuffer outBuffer = shader.getShaderEffect().getSecondaryTarget("bufOut");
         if (outBuffer == null) return;
         if (customParams != null) {
            this.setupShaderWithCustomParams(mode, shader, customParams);
         } else {
            this.setupShader(mode, shader);
         }

         this.shaderBuffer.clear(false);
         MCBuffer.beginWrite(false);
         RenderSystem.enableBlend();
         RenderSystem.blendFuncSeparate(
                 SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA,
                 SrcFactor.ZERO, DstFactor.ONE
         );
         RenderSystem.backupProjectionMatrix();
         outBuffer.draw(outBuffer.textureWidth, outBuffer.textureHeight, false);
         RenderSystem.restoreProjectionMatrix();
         RenderSystem.disableBlend();
      }
   }

   public ManagedShaderEffect getShader(@NotNull Shader mode) {
      return switch (mode) {
         case Pulse   -> PULSE;
         case Smoke   -> SMOKE;
         case Gradient-> GRADIENT;
         case Snow    -> SNOW;
         case Flow    -> FLOW;
         case Rainbow -> RAINBOW;
         case Cine    -> CINE;
         case Bloom   -> BLOOM;
         case Aurora  -> AURORA;
         case Nebula  -> NEBULA;
         case Sunset  -> SUNSET;
         case Vortex  -> VORTEX;
         case Waves   -> WAVES;
         case Crystal -> CRYSTAL;
         case Local   -> LOCAL;
         default      -> DEFAULT;
      };
   }

   public ManagedShaderEffect getShaderOutline(@NotNull Shader mode) {
      return switch (mode) {
         case Pulse   -> PULSE_OUTLINE;
         case Smoke   -> SMOKE_OUTLINE;
         case Gradient-> GRADIENT_OUTLINE;
         case Snow    -> SNOW_OUTLINE;
         case Flow    -> FLOW_OUTLINE;
         case Rainbow -> RAINBOW_OUTLINE;
         case Bloom   -> BLOOM_OUTLINE;
         default      -> DEFAULT_OUTLINE;
      };
   }

   public void setupShader(Shader shader, ManagedShaderEffect effect) {
      ShaderModule module = ShaderModule.INSTANCE;
      if (module == null) return;
      ShaderParams params = ShaderParams.fromModule(module);
      this.setupShaderWithParams(shader, effect, params, false);
   }

   public void setupShaderWithCustomParams(
           Shader shader,
           ManagedShaderEffect effect,
           ShaderParams params
   ) {
      this.setupShaderWithParams(shader, effect, params, false);
   }

   public void applySky(Runnable runnable) {
      Ambience module = Ambience.INSTANCE;
      if (module == null || !module.shouldRenderSkyShader()) return;
      if (this.fullNullCheck()) return;

      RenderSystem.assertOnRenderThreadOrInit();
      if (this.shaderBuffer == null || this.shaderBuffer.fbo == -1) return;

      Shader shaderMode = module.getSkyShader();
      if (shaderMode == null) shaderMode = Shader.Flow;

      Framebuffer MCBuffer = MinecraftClient.getInstance().getFramebuffer();
      int targetWidth = Math.max(MCBuffer.textureWidth, 1);
      int targetHeight = Math.max(MCBuffer.textureHeight, 1);

      if (this.shaderBuffer.textureWidth != targetWidth
              || this.shaderBuffer.textureHeight != targetHeight) {
         this.shaderBuffer.resize(targetWidth, targetHeight, false);
      }
      if (this.shaderBuffer.fbo == -1) return;

      GlStateManager._glBindFramebuffer(36009, this.shaderBuffer.fbo);
      this.shaderBuffer.setClearColor(0.0F, 0.0F, 0.0F, 1.0F);
      this.shaderBuffer.beginWrite(true);
      runnable.run();
      this.shaderBuffer.endWrite();
      this.shaderBuffer.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);

      GlStateManager._glBindFramebuffer(36009, MCBuffer.fbo);
      MCBuffer.beginWrite(false);

      ManagedShaderEffect shader = this.getShader(shaderMode);
      if (shader == null) shader = DEFAULT;

      PostEffectProcessor effect = shader.getShaderEffect();
      if (effect == null) return;

      ((IShaderEffectHook) effect).alienClient$addHook("bufIn", this.shaderBuffer);

      Framebuffer outBuffer = effect.getSecondaryTarget("bufOut");
      if (outBuffer == null) return;
      this.setupSkyShader(shaderMode, shader, module);

      this.shaderBuffer.clear(false);
      MCBuffer.beginWrite(false);
      RenderSystem.disableBlend();
      RenderSystem.backupProjectionMatrix();
      outBuffer.draw(outBuffer.textureWidth, outBuffer.textureHeight, false);
      RenderSystem.restoreProjectionMatrix();
      RenderSystem.disableBlend();
   }

   private void setupSkyShader(Shader shader, ManagedShaderEffect effect, Ambience module) {
      if (module == null) return;

      Color color = module.skyShaderColor.getValue();
      Color background = module.skyBackgroundColor.getValue();
      this.time = (float) timer.getMs() / 5.0F * module.skySpeed.getValueFloat() * 0.004F;
      float alpha = (float) color.getAlpha() / 255.0F;

      if (shader == Shader.Cine) {
         effect.setUniformValue("mixFactor", alpha);
         effect.setUniformValue("u_Color",
                 (float) color.getRed() / 255.0F,
                 (float) color.getGreen() / 255.0F,
                 (float) color.getBlue() / 255.0F,
                 1.0F);
         effect.setUniformValue("u_Resolution",
                 (float) mc.getWindow().getScaledWidth(),
                 (float) mc.getWindow().getScaledHeight());
         effect.setUniformValue("u_Mouse", 0.0F, 0.0F);
         effect.setUniformValue("u_Scale", 1.0F);
         effect.setUniformValue("u_Time", this.time);
         effect.render(mc.getRenderTickCounter().getTickDelta(true));
      } else if (shader == Shader.Aurora) {
         effect.setUniformValue("mixFactor", alpha);
         effect.setUniformValue("u_Color",
                 (float) color.getRed() / 255.0F,
                 (float) color.getGreen() / 255.0F,
                 (float) color.getBlue() / 255.0F,
                 1.0F);
         effect.setUniformValue("u_Resolution",
                 (float) mc.getWindow().getScaledWidth(),
                 (float) mc.getWindow().getScaledHeight());
         effect.setUniformValue("u_Mouse", 0.0F, 0.0F);
         effect.setUniformValue("u_Scale", 1.0F);
         effect.setUniformValue("u_Time", this.time);
         effect.render(mc.getRenderTickCounter().getTickDelta(true));
      } else if (shader == Shader.Local) {
         effect.setUniformValue("mixFactor", alpha);
         effect.setUniformValue("u_Color",
                 (float) color.getRed() / 255.0F,
                 (float) color.getGreen() / 255.0F,
                 (float) color.getBlue() / 255.0F,
                 1.0F);
         effect.setUniformValue("u_BackgroundColor",
                 (float) background.getRed() / 255.0F,
                 (float) background.getGreen() / 255.0F,
                 (float) background.getBlue() / 255.0F,
                 (float) background.getAlpha() / 255.0F);
         effect.setUniformValue("u_Resolution",
                 (float) mc.getWindow().getScaledWidth(),
                 (float) mc.getWindow().getScaledHeight());
         effect.setUniformValue("u_Mouse", 0.0F, 0.0F);
         effect.setUniformValue("u_Scale", 1.0F);
         effect.setUniformValue("u_Time", this.time);
         effect.render(mc.getRenderTickCounter().getTickDelta(true));
      }
   }

   private void setupShaderWithParams(
           Shader shader,
           ManagedShaderEffect effect,
           ShaderParams p,
           boolean skyMode
   ) {
      if (p == null) return;

      Color color = p.getFillColor();
      this.time = (float) timer.getMs() / 5.0F * p.getSpeed() * 0.004F;

      if (shader == Shader.Rainbow) {
         effect.setUniformValue("alpha2",    (float) color.getAlpha() / 255.0F);
         effect.setUniformValue("radius",    p.getRadius());
         effect.setUniformValue("quality",   p.getSmoothness());
         effect.setUniformValue("divider",   p.getDivider());
         effect.setUniformValue("maxSample", p.getMaxSample());
         effect.setUniformValue("resolution",
                 (float) mc.getWindow().getScaledWidth(),
                 (float) mc.getWindow().getScaledHeight());
         effect.setUniformValue("time", this.time);
         effect.render(mc.getRenderTickCounter().getTickDelta(true));

      } else if (shader == Shader.Gradient) {
         effect.setUniformValue("alpha2",    (float) color.getAlpha() / 255.0F);
         effect.setUniformValue("rgb",
                 p.getSmoke1().getRed()   / 255.0F,
                 p.getSmoke1().getGreen() / 255.0F,
                 p.getSmoke1().getBlue()  / 255.0F);
         effect.setUniformValue("rgb1",
                 p.getSmoke2().getRed()   / 255.0F,
                 p.getSmoke2().getGreen() / 255.0F,
                 p.getSmoke2().getBlue()  / 255.0F);
         effect.setUniformValue("rgb2",
                 p.getSmoke3().getRed()   / 255.0F,
                 p.getSmoke3().getGreen() / 255.0F,
                 p.getSmoke3().getBlue()  / 255.0F);
         effect.setUniformValue("rgb3",
                 p.getSmoke4().getRed()   / 255.0F,
                 p.getSmoke4().getGreen() / 255.0F,
                 p.getSmoke4().getBlue()  / 255.0F);
         effect.setUniformValue("step",      p.getStep() * 300.0F);
         effect.setUniformValue("radius",    p.getRadius());
         effect.setUniformValue("quality",   p.getSmoothness());
         effect.setUniformValue("divider",   p.getDivider());
         effect.setUniformValue("maxSample", p.getMaxSample());
         effect.setUniformValue("resolution",
                 (float) mc.getWindow().getScaledWidth(),
                 (float) mc.getWindow().getScaledHeight());
         effect.setUniformValue("time", this.time * 300.0F);
         effect.render(mc.getRenderTickCounter().getTickDelta(true));

      } else if (shader == Shader.Smoke) {
         effect.setUniformValue("alpha1",    (float) color.getAlpha() / 255.0F);
         effect.setUniformValue("radius",    p.getRadius());
         effect.setUniformValue("quality",   p.getSmoothness());
         effect.setUniformValue("divider",   p.getDivider());
         effect.setUniformValue("maxSample", p.getMaxSample());
         effect.setUniformValue("first",
                 p.getSmoke1().getRed()   / 255.0F,
                 p.getSmoke1().getGreen() / 255.0F,
                 p.getSmoke1().getBlue()  / 255.0F,
                 p.getSmoke1().getAlpha() / 255.0F);
         effect.setUniformValue("second",
                 p.getSmoke2().getRed()   / 255.0F,
                 p.getSmoke2().getGreen() / 255.0F,
                 p.getSmoke2().getBlue()  / 255.0F);
         effect.setUniformValue("third",
                 p.getSmoke3().getRed()   / 255.0F,
                 p.getSmoke3().getGreen() / 255.0F,
                 p.getSmoke3().getBlue()  / 255.0F);
         effect.setUniformValue("oct",       p.getOctaves());
         effect.setUniformValue("resolution",
                 (float) mc.getWindow().getScaledWidth(),
                 (float) mc.getWindow().getScaledHeight());
         effect.setUniformValue("time", this.time);
         effect.render(mc.getRenderTickCounter().getTickDelta(true));

      } else if (shader == Shader.Solid) {
         effect.setUniformValue("mixFactor", (float) color.getAlpha() / 255.0F);
         effect.setUniformValue("minAlpha",  p.getAlpha() / 255.0F);
         effect.setUniformValue("radius",    p.getRadius());
         effect.setUniformValue("quality",   p.getSmoothness());
         effect.setUniformValue("divider",   p.getDivider());
         effect.setUniformValue("maxSample", p.getMaxSample());
         effect.setUniformValue("color",
                 (float) color.getRed()   / 255.0F,
                 (float) color.getGreen() / 255.0F,
                 (float) color.getBlue()  / 255.0F);
         effect.setUniformValue("resolution",
                 (float) mc.getWindow().getScaledWidth(),
                 (float) mc.getWindow().getScaledHeight());
         effect.render(mc.getRenderTickCounter().getTickDelta(true));

      } else if (shader == Shader.Pulse) {
         effect.setUniformValue("mixFactor", (float) color.getAlpha() / 255.0F);
         effect.setUniformValue("minAlpha",  p.getAlpha() / 255.0F);
         effect.setUniformValue("radius",    p.getRadius());
         effect.setUniformValue("quality",   p.getSmoothness());
         effect.setUniformValue("divider",   p.getDivider());
         effect.setUniformValue("maxSample", p.getMaxSample());
         effect.setUniformValue("color",
                 (float) color.getRed()   / 255.0F,
                 (float) color.getGreen() / 255.0F,
                 (float) color.getBlue()  / 255.0F);
         Color pulse = p.getPulseColor();
         effect.setUniformValue("color2",
                 (float) pulse.getRed()   / 255.0F,
                 (float) pulse.getGreen() / 255.0F,
                 (float) pulse.getBlue()  / 255.0F);
         effect.setUniformValue("time", this.time);
         effect.setUniformValue("size", p.getPulseSpeed());
         effect.setUniformValue("resolution",
                 (float) mc.getWindow().getScaledWidth(),
                 (float) mc.getWindow().getScaledHeight());
         effect.render(mc.getRenderTickCounter().getTickDelta(true));

      } else if (shader == Shader.Snow) {
         effect.setUniformValue("color",
                 (float) color.getRed()   / 255.0F,
                 (float) color.getGreen() / 255.0F,
                 (float) color.getBlue()  / 255.0F,
                 (float) color.getAlpha() / 255.0F);
         effect.setUniformValue("radius",    p.getRadius());
         effect.setUniformValue("quality",   p.getSmoothness());
         effect.setUniformValue("divider",   p.getDivider());
         effect.setUniformValue("maxSample", p.getMaxSample());
         effect.setUniformValue("resolution",
                 (float) mc.getWindow().getScaledWidth(),
                 (float) mc.getWindow().getScaledHeight());
         effect.setUniformValue("time", this.time);
         effect.render(mc.getRenderTickCounter().getTickDelta(true));

      } else if (shader == Shader.Flow) {
         effect.setUniformValue("mixFactor", (float) color.getAlpha() / 255.0F);
         effect.setUniformValue("radius",    p.getRadius());
         effect.setUniformValue("quality",   p.getSmoothness());
         effect.setUniformValue("divider",   p.getDivider());
         effect.setUniformValue("maxSample", p.getMaxSample());
         effect.setUniformValue("resolution",
                 (float) mc.getWindow().getScaledWidth(),
                 (float) mc.getWindow().getScaledHeight());
         effect.setUniformValue("time", this.time);
         effect.render(mc.getRenderTickCounter().getTickDelta(true));

      } else if (shader == Shader.Cine) {
         effect.setUniformValue("mixFactor", (float) color.getAlpha() / 255.0F);
         effect.setUniformValue("u_Color",
                 (float) color.getRed()   / 255.0F,
                 (float) color.getGreen() / 255.0F,
                 (float) color.getBlue()  / 255.0F,
                 1.0F);
         effect.setUniformValue("u_Resolution",
                 (float) mc.getWindow().getScaledWidth(),
                 (float) mc.getWindow().getScaledHeight());
         if (skyMode) {
            effect.setUniformValue("u_Mouse", 0.0F, 0.0F);
            effect.setUniformValue("u_Scale", 1.0F);
         }
         effect.setUniformValue("u_Time", this.time);
         effect.render(mc.getRenderTickCounter().getTickDelta(true));

      } else if (shader == Shader.Bloom) {
         effect.setUniformValue("resolution",
                 (float) mc.getWindow().getScaledWidth(),
                 (float) mc.getWindow().getScaledHeight());
         effect.setUniformValue("u_Width", (int) p.getRadius());
         effect.setUniformValue("u_FastLines", 0);
         effect.setUniformValue("u_ShapeMode", 2);
         effect.setUniformValue("u_GlowMultiplier", p.getSmoothness());
         effect.setUniformValue("u_GlowQuality", (int) p.getMaxSample());
         effect.setUniformValue("u_FillColor",
                 (float) color.getRed()   / 255.0F,
                 (float) color.getGreen() / 255.0F,
                 (float) color.getBlue()  / 255.0F,
                 (float) color.getAlpha() / 255.0F);
         effect.setUniformValue("u_OutlineColor",
                 (float) color.getRed()   / 255.0F,
                 (float) color.getGreen() / 255.0F,
                 (float) color.getBlue()  / 255.0F,
                 (float) color.getAlpha() / 255.0F);
         effect.render(mc.getRenderTickCounter().getTickDelta(true));

      } else if (shader == Shader.Aurora
              || shader == Shader.Nebula
              || shader == Shader.Sunset
              || shader == Shader.Vortex
              || shader == Shader.Waves
              || shader == Shader.Crystal
              || shader == Shader.Local) {
         effect.setUniformValue("mixFactor", (float) color.getAlpha() / 255.0F);
         effect.setUniformValue("minAlpha",  p.getAlpha() / 255.0F);
         effect.setUniformValue("radius",    p.getRadius());
         effect.setUniformValue("quality",   p.getSmoothness());
         effect.setUniformValue("divider",   p.getDivider());
         effect.setUniformValue("maxSample", p.getMaxSample());
         effect.setUniformValue("color",
                 (float) color.getRed()   / 255.0F,
                 (float) color.getGreen() / 255.0F,
                 (float) color.getBlue()  / 255.0F);
         effect.setUniformValue("resolution",
                 (float) mc.getWindow().getScaledWidth(),
                 (float) mc.getWindow().getScaledHeight());
         effect.setUniformValue("time", this.time);
         effect.render(mc.getRenderTickCounter().getTickDelta(true));
      }
   }

   public void reloadShaders() {
      DEFAULT  = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/outline.json"));
      SMOKE    = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/smoke.json"));
      GRADIENT = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/gradient.json"));
      SNOW     = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/snow.json"));
      FLOW     = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/flow.json"));
      RAINBOW  = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/rainbow.json"));
      PULSE    = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/pulse.json"));

      try { CINE   = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/cine.json"));   }
      catch (Exception e) { CINE = DEFAULT; }
      try { BLOOM  = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/bloom.json"));  }
      catch (Exception e) { BLOOM = DEFAULT; }
      try { AURORA = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/aurora.json")); }
      catch (Exception e) { AURORA = DEFAULT; }
      try { NEBULA = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/nebula.json")); }
      catch (Exception e) { NEBULA = DEFAULT; }
      try { SUNSET = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/sunset.json")); }
      catch (Exception e) { SUNSET = DEFAULT; }
      try { VORTEX = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/vortex.json")); }
      catch (Exception e) { VORTEX = DEFAULT; }
      try { WAVES  = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/waves.json"));  }
      catch (Exception e) { WAVES = DEFAULT; }
      try { CRYSTAL= ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/crystal.json"));}
      catch (Exception e) { CRYSTAL = DEFAULT; }
      try { LOCAL  = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/local.json"));  }
      catch (Exception e) { LOCAL = DEFAULT; }

      DEFAULT_OUTLINE = ShaderEffectManager.getInstance().manage(
              Identifier.of("shaders/post/outline.json"), managedShaderEffect -> {
                 PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
                 if (effect != null) {
                    ((IShaderEffectHook) effect).alienClient$addHook("bufIn",  mc.worldRenderer.getEntityOutlinesFramebuffer());
                    ((IShaderEffectHook) effect).alienClient$addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
                 }
              });
      PULSE_OUTLINE = ShaderEffectManager.getInstance().manage(
              Identifier.of("shaders/post/pulse.json"), managedShaderEffect -> {
                 PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
                 if (effect != null) {
                    ((IShaderEffectHook) effect).alienClient$addHook("bufIn",  mc.worldRenderer.getEntityOutlinesFramebuffer());
                    ((IShaderEffectHook) effect).alienClient$addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
                 }
              });
      SMOKE_OUTLINE = ShaderEffectManager.getInstance().manage(
              Identifier.of("shaders/post/smoke.json"), managedShaderEffect -> {
                 PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
                 if (effect != null) {
                    ((IShaderEffectHook) effect).alienClient$addHook("bufIn",  mc.worldRenderer.getEntityOutlinesFramebuffer());
                    ((IShaderEffectHook) effect).alienClient$addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
                 }
              });
      GRADIENT_OUTLINE = ShaderEffectManager.getInstance().manage(
              Identifier.of("shaders/post/gradient.json"), managedShaderEffect -> {
                 PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
                 if (effect != null) {
                    ((IShaderEffectHook) effect).alienClient$addHook("bufIn",  mc.worldRenderer.getEntityOutlinesFramebuffer());
                    ((IShaderEffectHook) effect).alienClient$addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
                 }
              });
      SNOW_OUTLINE = ShaderEffectManager.getInstance().manage(
              Identifier.of("shaders/post/snow.json"), managedShaderEffect -> {
                 PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
                 if (effect != null) {
                    ((IShaderEffectHook) effect).alienClient$addHook("bufIn",  mc.worldRenderer.getEntityOutlinesFramebuffer());
                    ((IShaderEffectHook) effect).alienClient$addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
                 }
              });
      FLOW_OUTLINE = ShaderEffectManager.getInstance().manage(
              Identifier.of("shaders/post/flow.json"), managedShaderEffect -> {
                 PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
                 if (effect != null) {
                    ((IShaderEffectHook) effect).alienClient$addHook("bufIn",  mc.worldRenderer.getEntityOutlinesFramebuffer());
                    ((IShaderEffectHook) effect).alienClient$addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
                 }
              });
      RAINBOW_OUTLINE = ShaderEffectManager.getInstance().manage(
              Identifier.of("shaders/post/rainbow.json"), managedShaderEffect -> {
                 PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
                 if (effect != null) {
                    ((IShaderEffectHook) effect).alienClient$addHook("bufIn",  mc.worldRenderer.getEntityOutlinesFramebuffer());
                    ((IShaderEffectHook) effect).alienClient$addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
                 }
              });
      BLOOM_OUTLINE = ShaderEffectManager.getInstance().manage(
              Identifier.of("shaders/post/bloom.json"), managedShaderEffect -> {
                 PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
                 if (effect != null) {
                    ((IShaderEffectHook) effect).alienClient$addHook("bufIn",  mc.worldRenderer.getEntityOutlinesFramebuffer());
                    ((IShaderEffectHook) effect).alienClient$addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
                 }
              });
   }

   public boolean fullNullCheck() {
      boolean shadersExist = GRADIENT != null
              && SMOKE != null
              && DEFAULT != null
              && FLOW != null
              && RAINBOW != null
              && PULSE != null
              && CINE != null
              && BLOOM != null
              && AURORA != null
              && NEBULA != null
              && SUNSET != null
              && VORTEX != null
              && WAVES != null
              && CRYSTAL != null
              && LOCAL != null
              && PULSE_OUTLINE != null
              && GRADIENT_OUTLINE != null
              && SMOKE_OUTLINE != null
              && DEFAULT_OUTLINE != null
              && FLOW_OUTLINE != null
              && RAINBOW_OUTLINE != null
              && BLOOM_OUTLINE != null;
      if (mc.getFramebuffer() == null) {
         return true;
      }
      if (!shadersExist) {
         this.reloadShaders();
         return true;
      }
      if (this.shaderBuffer == null) {
         int w = Math.max(mc.getFramebuffer().textureWidth, 1);
         int h = Math.max(mc.getFramebuffer().textureHeight, 1);
         this.shaderBuffer = new MyFramebuffer(w, h);
         return true;
      }
      if (this.shaderBuffer.fbo == -1) {
         int w = Math.max(mc.getFramebuffer().textureWidth, 1);
         int h = Math.max(mc.getFramebuffer().textureHeight, 1);
         this.shaderBuffer.initFbo(w, h, true);
         return true;
      }

      return false;
   }

   public static class MyFramebuffer extends Framebuffer {
      public MyFramebuffer(int width, int height) {
         super(false);
         RenderSystem.assertOnRenderThreadOrInit();
         int clampedWidth = Math.max(width, 1);
         int clampedHeight = Math.max(height, 1);
         this.initFbo(clampedWidth, clampedHeight, true);
         this.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
      }
   }

   public static record RenderTask(Runnable task, Shader shader, ShaderParams params) {}

   public enum Shader {
      Solid,
      Pulse,
      Smoke,
      Gradient,
      Snow,
      Flow,
      Rainbow,
      Cine,
      Bloom,
      Aurora,
      Nebula,
      Sunset,
      Vortex,
      Waves,
      Crystal,
      Local
   }
}
