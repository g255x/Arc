/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager
 *  com.mojang.blaze3d.platform.GlStateManager.DstFactor
 *  com.mojang.blaze3d.platform.GlStateManager.SrcFactor
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.client.gl.Framebuffer
 *  net.minecraft.client.gl.PostEffectProcessor
 *  net.minecraft.util.Identifier
 *  org.jetbrains.annotations.NotNull
 */
package dev.perish.core.impl;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.perish.api.interfaces.IShaderEffectHook;
import dev.perish.api.utils.Wrapper;
import dev.perish.api.utils.math.Timer;
import dev.perish.mod.modules.impl.render.ShaderModule;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.satin.api.managed.ManagedShaderEffect;
import org.ladysnake.satin.api.managed.ShaderEffectManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;

public class ShaderManager
        implements Wrapper {
    static final Timer timer = new Timer();
    private static final List<RenderTask> tasks = new ArrayList<RenderTask>();
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
    public static ManagedShaderEffect SKY;
    public static ManagedShaderEffect SKY_OUTLINE;
    public static ManagedShaderEffect CINE;
    public static ManagedShaderEffect AURORA;
    public static ManagedShaderEffect LOCAL;
    public static ManagedShaderEffect BLOOM;
    public static ManagedShaderEffect BLOOM_OUTLINE;
    public float time = 0.0f;
    private MyFramebuffer shaderBuffer;

    public void renderShader(Runnable runnable, Shader mode) {
        tasks.add(new RenderTask(runnable, mode));
    }

    public void renderShaders() {
        tasks.forEach(t -> this.applyShader(t.task(), t.shader()));
        tasks.clear();
    }

    public void applyShader(Runnable runnable, Shader mode) {
        applyShader(runnable, mode, null);
    }

    public void applyShader(Runnable runnable, Shader mode, Color customColor) {
        if (this.fullNullCheck()) {
            return;
        }
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
        PostEffectProcessor effect = shader.getShaderEffect();
        if (effect != null) {
            ((IShaderEffectHook) effect).alienClient$addHook("bufIn", this.shaderBuffer);
        } else {
            return;
        }
        Framebuffer outBuffer = effect.getSecondaryTarget("bufOut");
        if (outBuffer == null) return;
        this.setupShader(mode, shader, customColor);
        this.shaderBuffer.clear(false);
        MCBuffer.beginWrite(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);
        RenderSystem.backupProjectionMatrix();
        outBuffer.draw(outBuffer.textureWidth, outBuffer.textureHeight, false);
        RenderSystem.restoreProjectionMatrix();
        RenderSystem.disableBlend();
    }

    public ManagedShaderEffect getShader(@NotNull Shader mode) {
        return switch (mode.ordinal()) {
            case 3 -> GRADIENT;
            case 2 -> SMOKE;
            case 1 -> PULSE;
            case 4 -> SNOW;
            case 5 -> FLOW;
            case 6 -> RAINBOW;
            case 7, 8 -> SKY;
            case 9 -> CINE;
            case 10 -> AURORA;
            case 11 -> LOCAL;
            case 12 -> BLOOM;
            default -> DEFAULT;
        };
    }

    public ManagedShaderEffect getShaderOutline(@NotNull Shader mode) {
        return switch (mode.ordinal()) {
            case 3 -> GRADIENT_OUTLINE;
            case 2 -> SMOKE_OUTLINE;
            case 4 -> SNOW_OUTLINE;
            case 5 -> FLOW_OUTLINE;
            case 6 -> RAINBOW_OUTLINE;
            case 1 -> PULSE_OUTLINE;
            case 7, 8 -> SKY_OUTLINE;
            case 12 -> BLOOM_OUTLINE;
            default -> DEFAULT_OUTLINE;
        };
    }

    public void setupShader(Shader shader, ManagedShaderEffect effect) {
        setupShader(shader, effect, null);
    }

    public void setupShader(Shader shader, ManagedShaderEffect effect, Color customColor) {
        ShaderModule module = ShaderModule.INSTANCE;
        Color color = customColor != null ? customColor : module.fill.getValue();
        float alpha = (float) color.getAlpha() / 255.0f;
        this.time = (float) timer.getMs() / 5.0f * module.speed.getValueFloat() * 0.004f;
        if (shader == Shader.Rainbow) {
            effect.setUniformValue("alpha2", alpha);
            effect.setUniformValue("radius", module.radius.getValueFloat());
            effect.setUniformValue("quality", module.smoothness.getValueFloat());
            effect.setUniformValue("divider", module.divider.getValueFloat());
            effect.setUniformValue("maxSample", module.maxSample.getValueFloat());
            effect.setUniformValue("resolution", (float) mc.getWindow().getScaledWidth(), (float) mc.getWindow().getScaledHeight());
            effect.setUniformValue("time", this.time);
            effect.render(mc.getRenderTickCounter().getTickDelta(true));
        } else if (shader == Shader.Gradient) {
            effect.setUniformValue("alpha2", alpha);
            effect.setUniformValue("rgb", (float) module.smoke1.getValue().getRed() / 255.0f, (float) module.smoke1.getValue().getGreen() / 255.0f, (float) module.smoke1.getValue().getBlue() / 255.0f);
            effect.setUniformValue("rgb1", (float) module.smoke2.getValue().getRed() / 255.0f, (float) module.smoke2.getValue().getGreen() / 255.0f, (float) module.smoke2.getValue().getBlue() / 255.0f);
            effect.setUniformValue("rgb2", (float) module.smoke3.getValue().getRed() / 255.0f, (float) module.smoke3.getValue().getGreen() / 255.0f, (float) module.smoke3.getValue().getBlue() / 255.0f);
            effect.setUniformValue("rgb3", (float) module.smoke4.getValue().getRed() / 255.0f, (float) module.smoke4.getValue().getGreen() / 255.0f, (float) module.smoke4.getValue().getBlue() / 255.0f);
            effect.setUniformValue("step", module.step.getValueFloat() * 300.0f);
            effect.setUniformValue("radius", module.radius.getValueFloat());
            effect.setUniformValue("quality", module.smoothness.getValueFloat());
            effect.setUniformValue("divider", module.divider.getValueFloat());
            effect.setUniformValue("maxSample", module.maxSample.getValueFloat());
            effect.setUniformValue("resolution", (float) mc.getWindow().getScaledWidth(), (float) mc.getWindow().getScaledHeight());
            effect.setUniformValue("time", this.time * 300.0f);
            effect.render(mc.getRenderTickCounter().getTickDelta(true));
        } else if (shader == Shader.Smoke) {
            effect.setUniformValue("alpha1", alpha);
            effect.setUniformValue("radius", module.radius.getValueFloat());
            effect.setUniformValue("quality", module.smoothness.getValueFloat());
            effect.setUniformValue("divider", module.divider.getValueFloat());
            effect.setUniformValue("maxSample", module.maxSample.getValueFloat());
            effect.setUniformValue("first", (float) module.smoke1.getValue().getRed() / 255.0f, (float) module.smoke1.getValue().getGreen() / 255.0f, (float) module.smoke1.getValue().getBlue() / 255.0f, (float) module.smoke1.getValue().getAlpha() / 255.0f);
            effect.setUniformValue("second", (float) module.smoke2.getValue().getRed() / 255.0f, (float) module.smoke2.getValue().getGreen() / 255.0f, (float) module.smoke2.getValue().getBlue() / 255.0f);
            effect.setUniformValue("third", (float) module.smoke3.getValue().getRed() / 255.0f, (float) module.smoke3.getValue().getGreen() / 255.0f, (float) module.smoke3.getValue().getBlue() / 255.0f);
            effect.setUniformValue("oct", (int) module.octaves.getValue());
            effect.setUniformValue("resolution", (float) mc.getWindow().getScaledWidth(), (float) mc.getWindow().getScaledHeight());
            effect.setUniformValue("time", this.time);
            effect.render(mc.getRenderTickCounter().getTickDelta(true));
        } else if (shader == Shader.Solid) {
            effect.setUniformValue("mixFactor", alpha);
            effect.setUniformValue("minAlpha", module.alpha.getValueFloat() / 100.0f);
            effect.setUniformValue("radius", module.radius.getValueFloat());
            effect.setUniformValue("quality", module.smoothness.getValueFloat());
            effect.setUniformValue("divider", module.divider.getValueFloat());
            effect.setUniformValue("maxSample", module.maxSample.getValueFloat());
            effect.setUniformValue("color", (float) color.getRed() / 255.0f, (float) color.getGreen() / 255.0f, (float) color.getBlue() / 255.0f);
            effect.setUniformValue("resolution", (float) mc.getWindow().getScaledWidth(), (float) mc.getWindow().getScaledHeight());
            effect.render(mc.getRenderTickCounter().getTickDelta(true));
        } else if (shader == Shader.Pulse) {
            effect.setUniformValue("mixFactor", alpha);
            effect.setUniformValue("minAlpha", module.alpha.getValueFloat() / 100.0f);
            effect.setUniformValue("radius", module.radius.getValueFloat());
            effect.setUniformValue("quality", module.smoothness.getValueFloat());
            effect.setUniformValue("divider", module.divider.getValueFloat());
            effect.setUniformValue("maxSample", module.maxSample.getValueFloat());
            effect.setUniformValue("color", (float) color.getRed() / 255.0f, (float) color.getGreen() / 255.0f, (float) color.getBlue() / 255.0f);
            Color color2 = module.pulse.getValue();
            effect.setUniformValue("color2", (float) color2.getRed() / 255.0f, (float) color2.getGreen() / 255.0f, (float) color2.getBlue() / 255.0f);
            effect.setUniformValue("time", this.time);
            effect.setUniformValue("size", module.pulseSpeed.getValueFloat());
            effect.setUniformValue("resolution", (float) mc.getWindow().getScaledWidth(), (float) mc.getWindow().getScaledHeight());
            effect.render(mc.getRenderTickCounter().getTickDelta(true));
        } else if (shader == Shader.Snow) {
            effect.setUniformValue("color", (float) color.getRed() / 255.0f, (float) color.getGreen() / 255.0f, (float) color.getBlue() / 255.0f, alpha);
            effect.setUniformValue("radius", module.radius.getValueFloat());
            effect.setUniformValue("quality", module.smoothness.getValueFloat());
            effect.setUniformValue("divider", module.divider.getValueFloat());
            effect.setUniformValue("maxSample", module.maxSample.getValueFloat());
            effect.setUniformValue("resolution", (float) mc.getWindow().getScaledWidth(), (float) mc.getWindow().getScaledHeight());
            effect.setUniformValue("time", this.time);
            effect.render(mc.getRenderTickCounter().getTickDelta(true));
        } else if (shader == Shader.Flow) {
            effect.setUniformValue("mixFactor", alpha);
            effect.setUniformValue("radius", module.radius.getValueFloat());
            effect.setUniformValue("quality", module.smoothness.getValueFloat());
            effect.setUniformValue("divider", module.divider.getValueFloat());
            effect.setUniformValue("maxSample", module.maxSample.getValueFloat());
            effect.setUniformValue("resolution", (float) mc.getWindow().getScaledWidth(), (float) mc.getWindow().getScaledHeight());
            effect.setUniformValue("time", this.time);
            effect.render(mc.getRenderTickCounter().getTickDelta(true));
        } else if (shader == Shader.Stars || shader == Shader.Galaxy) {
            effect.setUniformValue("mixFactor", alpha);
            effect.setUniformValue("minAlpha", module.alpha.getValueFloat() / 255.0f);
            effect.setUniformValue("radius", module.radius.getValueFloat());
            effect.setUniformValue("quality", module.smoothness.getValueFloat());
            effect.setUniformValue("divider", module.divider.getValueFloat());
            effect.setUniformValue("maxSample", module.maxSample.getValueFloat());
            effect.setUniformValue("color", (float) color.getRed() / 255.0f, (float) color.getGreen() / 255.0f, (float) color.getBlue() / 255.0f, alpha);
            effect.setUniformValue("resolution", (float) mc.getWindow().getScaledWidth(), (float) mc.getWindow().getScaledHeight());
            effect.setUniformValue("time", this.time);
            effect.setUniformValue("skyMode", shader == Shader.Stars ? 0 : 1);
            effect.render(mc.getRenderTickCounter().getTickDelta(true));
        } else if (shader == Shader.Cine) {
            effect.setUniformValue("mixFactor", alpha);
            effect.setUniformValue("u_Color", (float) color.getRed() / 255.0f, (float) color.getGreen() / 255.0f, (float) color.getBlue() / 255.0f, 1.0f);
            effect.setUniformValue("u_Resolution", (float) mc.getWindow().getScaledWidth(), (float) mc.getWindow().getScaledHeight());
            effect.setUniformValue("u_Time", this.time);
            effect.render(mc.getRenderTickCounter().getTickDelta(true));
        } else if (shader == Shader.Aurora) {
            effect.setUniformValue("mixFactor", alpha);
            effect.setUniformValue("minAlpha", module.alpha.getValueFloat() / 255.0f);
            effect.setUniformValue("radius", module.radius.getValueFloat());
            effect.setUniformValue("quality", module.smoothness.getValueFloat());
            effect.setUniformValue("divider", module.divider.getValueFloat());
            effect.setUniformValue("maxSample", module.maxSample.getValueFloat());
            effect.setUniformValue("color", (float) color.getRed() / 255.0f, (float) color.getGreen() / 255.0f, (float) color.getBlue() / 255.0f);
            effect.setUniformValue("resolution", (float) mc.getWindow().getScaledWidth(), (float) mc.getWindow().getScaledHeight());
            effect.setUniformValue("time", this.time);
            effect.render(mc.getRenderTickCounter().getTickDelta(true));
        } else if (shader == Shader.Local) {
            effect.setUniformValue("mixFactor", alpha);
            effect.setUniformValue("u_Color", (float) color.getRed() / 255.0f, (float) color.getGreen() / 255.0f, (float) color.getBlue() / 255.0f, 1.0f);
            effect.setUniformValue("u_BackgroundColor", 16.0f / 255.0f, 24.0f / 255.0f, 48.0f / 255.0f, 1.0f);
            effect.setUniformValue("u_Resolution", (float) mc.getWindow().getScaledWidth(), (float) mc.getWindow().getScaledHeight());
            effect.setUniformValue("u_Time", this.time);
            effect.render(mc.getRenderTickCounter().getTickDelta(true));
        } else if (shader == Shader.Bloom) {
            float r = (float) color.getRed() / 255.0f;
            float g = (float) color.getGreen() / 255.0f;
            float b = (float) color.getBlue() / 255.0f;
            // outline thickness in pixels (u_Width) from Radius slider
            effect.setUniformValue("u_Width", Math.max(1, (int) module.radius.getValueFloat()));
            // loop step; MUST be >= 1 (it is the for-loop increment)
            effect.setUniformValue("u_GlowQuality", 1);
            effect.setUniformValue("u_FastLines", 0);
            // 2 = draw both fill and outline glow (matches SKIDSPACE default)
            effect.setUniformValue("u_ShapeMode", 2);
            // glow strength; GlowAlpha 0-100 -> 0-1
            effect.setUniformValue("u_GlowMultiplier", Math.max(0.0f, Math.min(1.0f, module.alpha.getValueFloat() / 100.0f)));
            effect.setUniformValue("u_FillColor", r, g, b, alpha);
            effect.setUniformValue("u_OutlineColor", r, g, b, 1.0f);
            effect.setUniformValue("resolution", (float) mc.getWindow().getScaledWidth(), (float) mc.getWindow().getScaledHeight());
            effect.render(mc.getRenderTickCounter().getTickDelta(true));
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
        SKY = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/sky.json"));
        DEFAULT_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/outline.json"), managedShaderEffect -> {
            PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
            if (effect == null) {
                return;
            }
            ((IShaderEffectHook) effect).alienClient$addHook("bufIn", ShaderManager.mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffectHook) effect).alienClient$addHook("bufOut", ShaderManager.mc.worldRenderer.getEntityOutlinesFramebuffer());
        });
        PULSE_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/pulse.json"), managedShaderEffect -> {
            PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
            if (effect == null) {
                return;
            }
            ((IShaderEffectHook) effect).alienClient$addHook("bufIn", ShaderManager.mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffectHook) effect).alienClient$addHook("bufOut", ShaderManager.mc.worldRenderer.getEntityOutlinesFramebuffer());
        });
        SMOKE_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/smoke.json"), managedShaderEffect -> {
            PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
            if (effect == null) {
                return;
            }
            ((IShaderEffectHook) effect).alienClient$addHook("bufIn", ShaderManager.mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffectHook) effect).alienClient$addHook("bufOut", ShaderManager.mc.worldRenderer.getEntityOutlinesFramebuffer());
        });
        GRADIENT_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/gradient.json"), managedShaderEffect -> {
            PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
            if (effect == null) {
                return;
            }
            ((IShaderEffectHook) effect).alienClient$addHook("bufIn", ShaderManager.mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffectHook) effect).alienClient$addHook("bufOut", ShaderManager.mc.worldRenderer.getEntityOutlinesFramebuffer());
        });
        SNOW_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/snow.json"), managedShaderEffect -> {
            PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
            if (effect == null) {
                return;
            }
            ((IShaderEffectHook) effect).alienClient$addHook("bufIn", ShaderManager.mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffectHook) effect).alienClient$addHook("bufOut", ShaderManager.mc.worldRenderer.getEntityOutlinesFramebuffer());
        });
        FLOW_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/flow.json"), managedShaderEffect -> {
            PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
            if (effect == null) {
                return;
            }
            ((IShaderEffectHook) effect).alienClient$addHook("bufIn", ShaderManager.mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffectHook) effect).alienClient$addHook("bufOut", ShaderManager.mc.worldRenderer.getEntityOutlinesFramebuffer());
        });
        RAINBOW_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/rainbow.json"), managedShaderEffect -> {
            PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
            if (effect == null) {
                return;
            }
            ((IShaderEffectHook) effect).alienClient$addHook("bufIn", ShaderManager.mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffectHook) effect).alienClient$addHook("bufOut", ShaderManager.mc.worldRenderer.getEntityOutlinesFramebuffer());
        });
        SKY_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/sky.json"), managedShaderEffect -> {
            PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
            if (effect == null) {
                return;
            }
            ((IShaderEffectHook) effect).alienClient$addHook("bufIn", ShaderManager.mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffectHook) effect).alienClient$addHook("bufOut", ShaderManager.mc.worldRenderer.getEntityOutlinesFramebuffer());
        });
        CINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/cine.json"));
        AURORA = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/aurora.json"));
        LOCAL = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/local.json"));
        BLOOM = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/bloom.json"));
        BLOOM_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/bloom.json"), managedShaderEffect -> {
            PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
            if (effect == null) {
                return;
            }
            ((IShaderEffectHook) effect).alienClient$addHook("bufIn", ShaderManager.mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffectHook) effect).alienClient$addHook("bufOut", ShaderManager.mc.worldRenderer.getEntityOutlinesFramebuffer());
        });
    }

    public boolean fullNullCheck() {
        if (GRADIENT == null || SMOKE == null || DEFAULT == null || FLOW == null || RAINBOW == null || PULSE == null || SKY == null || CINE == null || AURORA == null || LOCAL == null || BLOOM == null || BLOOM_OUTLINE == null || PULSE_OUTLINE == null || GRADIENT_OUTLINE == null || SMOKE_OUTLINE == null || DEFAULT_OUTLINE == null || FLOW_OUTLINE == null || RAINBOW_OUTLINE == null || SKY_OUTLINE == null || this.shaderBuffer == null) {
            if (mc.getFramebuffer() == null) {
                return true;
            }
            this.shaderBuffer = new MyFramebuffer(ShaderManager.mc.getFramebuffer().textureWidth, ShaderManager.mc.getFramebuffer().textureHeight);
            this.reloadShaders();
            return true;
        }
        return false;
    }

    public enum Shader {
        Solid,
        Pulse,
        Smoke,
        Gradient,
        Snow,
        Flow,
        Rainbow,
        Stars,
        Galaxy,
        Cine,
        Aurora,
        Local,
        Bloom
    }

    public record RenderTask(Runnable task, Shader shader) {
    }

    public static class MyFramebuffer
            extends Framebuffer {
        public MyFramebuffer(int width, int height) {
            super(false);
            RenderSystem.assertOnRenderThreadOrInit();
            this.resize(width, height, true);
            this.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        }
    }
}

