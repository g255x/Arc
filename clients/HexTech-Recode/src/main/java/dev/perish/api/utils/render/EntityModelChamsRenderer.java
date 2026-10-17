package dev.perish.api.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.perish.api.utils.Wrapper;
import dev.perish.mod.modules.settings.impl.ColorSetting;
import org.joml.Matrix4f;

import java.awt.*;
import net.minecraft.entity.Entity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.entity.EntityRenderer;

public class EntityModelChamsRenderer implements Wrapper {
    private static final VertexCaptureProvider PROVIDER = new VertexCaptureProvider();
    private static boolean capturing;

    private EntityModelChamsRenderer() {
    }

    public static boolean isCapturing() {
        return capturing;
    }

    public static void render(Entity entity, MatrixStack worldMatrices, float tickDelta, ColorSetting fillSetting, ColorSetting lineSetting, boolean shine, boolean depth) {
        if (mc.world == null || entity == null || (!fillSetting.booleanValue && !lineSetting.booleanValue)) {
            return;
        }

        EntityRenderer<? super Entity> renderer = mc.getEntityRenderDispatcher().getRenderer(entity);
        if (renderer == null) {
            return;
        }

        float x = (float) (MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX()) - mc.getEntityRenderDispatcher().camera.getPos().x);
        float y = (float) (MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY()) - mc.getEntityRenderDispatcher().camera.getPos().y);
        float z = (float) (MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ()) - mc.getEntityRenderDispatcher().camera.getPos().z);
        MatrixStack matrices = new MatrixStack();

        PROVIDER.prepare(fillSetting, lineSetting, worldMatrices.peek().getPositionMatrix(), x, y, z);
        capturing = true;
        RenderSystem.enableBlend();
        if (depth) {
            RenderSystem.enableDepthTest();
        } else {
            RenderSystem.disableDepthTest();
        }
        RenderSystem.depthMask(false);
        if (shine) {
            RenderSystem.blendFunc(770, 32772);
        } else {
            RenderSystem.defaultBlendFunc();
        }
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        try {
            renderer.render(entity, entity.getYaw(), tickDelta, matrices, PROVIDER, 0xF000F0);
        } finally {
            capturing = false;
            PROVIDER.reset();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
        }
    }

    private static class VertexCaptureProvider implements VertexConsumerProvider {
        private final VertexCaptureConsumer consumer = new VertexCaptureConsumer();

        public void prepare(ColorSetting fillSetting, ColorSetting lineSetting, Matrix4f positionMatrix, float x, float y, float z) {
            this.consumer.prepare(fillSetting, lineSetting, positionMatrix, x, y, z);
        }

        public void reset() {
            this.consumer.reset();
        }

        @Override
        public VertexConsumer getBuffer(net.minecraft.client.render.RenderLayer layer) {
            return this.consumer;
        }
    }

    private static class VertexCaptureConsumer implements VertexConsumer {
        private final float[] xs = new float[4];
        private final float[] ys = new float[4];
        private final float[] zs = new float[4];
        private ColorSetting fillSetting;
        private ColorSetting lineSetting;
        private Matrix4f positionMatrix;
        private float offsetX;
        private float offsetY;
        private float offsetZ;
        private int index;

        public void prepare(ColorSetting fillSetting, ColorSetting lineSetting, Matrix4f positionMatrix, float x, float y, float z) {
            this.fillSetting = fillSetting;
            this.lineSetting = lineSetting;
            this.positionMatrix = new Matrix4f(positionMatrix);
            this.offsetX = x;
            this.offsetY = y;
            this.offsetZ = z;
            this.index = 0;
        }

        public void reset() {
            this.fillSetting = null;
            this.lineSetting = null;
            this.positionMatrix = null;
            this.offsetX = 0.0F;
            this.offsetY = 0.0F;
            this.offsetZ = 0.0F;
            this.index = 0;
        }

        @Override
        public VertexConsumer vertex(float x, float y, float z) {
            this.xs[this.index] = x;
            this.ys[this.index] = y;
            this.zs[this.index] = z;
            this.index++;

            if (this.index == 4) {
                this.flushQuad();
                this.index = 0;
            }

            return this;
        }

        private void flushQuad() {
            if (this.fillSetting != null && this.fillSetting.booleanValue) {
                Color color = this.fillSetting.getValue();
                float r = color.getRed() / 255.0F;
                float g = color.getGreen() / 255.0F;
                float b = color.getBlue() / 255.0F;
                float a = color.getAlpha() / 255.0F;
                BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
                buffer.vertex(this.positionMatrix, this.offsetX + this.xs[0], this.offsetY + this.ys[0], this.offsetZ + this.zs[0]).color(r, g, b, a);
                buffer.vertex(this.positionMatrix, this.offsetX + this.xs[1], this.offsetY + this.ys[1], this.offsetZ + this.zs[1]).color(r, g, b, a);
                buffer.vertex(this.positionMatrix, this.offsetX + this.xs[2], this.offsetY + this.ys[2], this.offsetZ + this.zs[2]).color(r, g, b, a);
                buffer.vertex(this.positionMatrix, this.offsetX + this.xs[3], this.offsetY + this.ys[3], this.offsetZ + this.zs[3]).color(r, g, b, a);
                Render3DUtil.endBuilding(buffer);
            }

            if (this.lineSetting != null && this.lineSetting.booleanValue) {
                Color color = this.lineSetting.getValue();
                float r = color.getRed() / 255.0F;
                float g = color.getGreen() / 255.0F;
                float b = color.getBlue() / 255.0F;
                float a = color.getAlpha() / 255.0F;
                BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
                buffer.vertex(this.positionMatrix, this.offsetX + this.xs[0], this.offsetY + this.ys[0], this.offsetZ + this.zs[0]).color(r, g, b, a);
                buffer.vertex(this.positionMatrix, this.offsetX + this.xs[1], this.offsetY + this.ys[1], this.offsetZ + this.zs[1]).color(r, g, b, a);
                buffer.vertex(this.positionMatrix, this.offsetX + this.xs[1], this.offsetY + this.ys[1], this.offsetZ + this.zs[1]).color(r, g, b, a);
                buffer.vertex(this.positionMatrix, this.offsetX + this.xs[2], this.offsetY + this.ys[2], this.offsetZ + this.zs[2]).color(r, g, b, a);
                buffer.vertex(this.positionMatrix, this.offsetX + this.xs[2], this.offsetY + this.ys[2], this.offsetZ + this.zs[2]).color(r, g, b, a);
                buffer.vertex(this.positionMatrix, this.offsetX + this.xs[3], this.offsetY + this.ys[3], this.offsetZ + this.zs[3]).color(r, g, b, a);
                buffer.vertex(this.positionMatrix, this.offsetX + this.xs[3], this.offsetY + this.ys[3], this.offsetZ + this.zs[3]).color(r, g, b, a);
                buffer.vertex(this.positionMatrix, this.offsetX + this.xs[0], this.offsetY + this.ys[0], this.offsetZ + this.zs[0]).color(r, g, b, a);
                Render3DUtil.endBuilding(buffer);
            }
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer light(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return this;
        }
    }
}
