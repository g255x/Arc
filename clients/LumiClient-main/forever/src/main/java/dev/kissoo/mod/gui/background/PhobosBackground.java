package dev.kissoo.mod.gui.background;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

public final class PhobosBackground {
    private static final float PHOBOS_OFFSET_PER_TICK = 0.01F;
    private static final PhobosBackground INSTANCE = new PhobosBackground();

    private ShaderProgram menuProgram;
    private ShaderProgram loadingProgram;
    private ShaderProgram buttonProgram;
    private float phobosOffset;
    private boolean failed;

    private PhobosBackground() {
    }

    public static PhobosBackground getInstance() {
        return INSTANCE;
    }

    public static void registerShaders() {
        CoreShaderRegistrationCallback.EVENT.register(context -> {
            PhobosBackground phobos = getInstance();
            context.register(Identifier.of("forever", "phobos_menu"), VertexFormats.POSITION, program -> phobos.menuProgram = program);
            context.register(Identifier.of("forever", "phobos_loading"), VertexFormats.POSITION, program -> phobos.loadingProgram = program);
            context.register(Identifier.of("forever", "phobos_menu_button"), VertexFormats.POSITION, program -> phobos.buttonProgram = program);
        });
    }

    public boolean isFailed() {
        return failed;
    }

    public void renderMenu(DrawContext context) {
        ShaderProgram program = this.menuProgram;
        if (program != null) {
            renderRectangle(context.getMatrices(), program, 1.0F, 1.0F, 1.0F, 1.0F,
                    0.0F, 0.0F, context.getScaledWindowWidth(), context.getScaledWindowHeight(), 0.0F);
        }
    }

    public void renderLoading(DrawContext context, float alpha) {
        ShaderProgram program = this.loadingProgram;
        if (program != null) {
            float roundedAlpha = Math.round(Math.max(0.0F, Math.min(1.0F, alpha)) * 100.0F) / 100.0F;
            renderRectangle(context.getMatrices(), program, 0.5F, 0.5F, 0.5F, roundedAlpha,
                    0.0F, 0.0F, this.getScaledWidth(), this.getScaledHeight(), 0.0F);
        }
    }

    public void renderButton(DrawContext context, int x, int y, int width, int height, float alpha) {
        ShaderProgram program = this.buttonProgram;
        if (program == null || width <= 0 || height <= 0) {
            return;
        }

        renderRectangle(context.getMatrices(), program, 1.0F, 1.0F, 1.0F, Math.max(0.0F, Math.min(1.0F, alpha)),
                x, y, x + width, y + height, 10.0F);
    }

    private void renderRectangle(MatrixStack matrices, ShaderProgram program, float red, float green, float blue, float alpha,
                                 float left, float top, float right, float bottom, float timeOffset) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.getWindow() == null || client.getFramebuffer() == null) {
                return;
            }

            Framebuffer framebuffer = client.getFramebuffer();
            framebuffer.beginWrite(false);

            GlUniform time = program.getUniform("time");
            if (time != null) {
                time.set(this.phobosOffset + timeOffset);
            }
            GlUniform color = program.getUniform("color");
            if (color != null) {
                color.set(red, green, blue, alpha);
            }
            GlUniform inputResolution = program.getUniform("InputResolution");
            if (inputResolution != null) {
                inputResolution.set((float) framebuffer.textureWidth, (float) framebuffer.textureHeight);
            }

            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShader(() -> program);

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            buffer.vertex(matrix, right, bottom, 0.0F).color(red, green, blue, alpha);
            buffer.vertex(matrix, left, bottom, 0.0F).color(red, green, blue, alpha);
            buffer.vertex(matrix, left, top, 0.0F).color(red, green, blue, alpha);
            buffer.vertex(matrix, right, top, 0.0F).color(red, green, blue, alpha);
            BufferRenderer.drawWithGlobalProgram(buffer.end());

            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
        } catch (Throwable throwable) {
            if (!this.failed) {
                this.failed = true;
                System.err.println("[Forever] Failed to render Phobos shader");
                throwable.printStackTrace();
            }
        }
    }

    private int getScaledWidth() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client == null || client.getWindow() == null ? 0 : client.getWindow().getScaledWidth();
    }

    private int getScaledHeight() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client == null || client.getWindow() == null ? 0 : client.getWindow().getScaledHeight();
    }

    public void advanceTick() {
        this.phobosOffset += PHOBOS_OFFSET_PER_TICK;
    }
}
