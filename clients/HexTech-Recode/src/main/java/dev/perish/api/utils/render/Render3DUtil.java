package dev.perish.api.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.perish.Perish;
import dev.perish.api.utils.Wrapper;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.*;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.awt.*;
import java.util.ArrayList;

public class Render3DUtil implements Wrapper {
    // 复用同一个缓冲区，避免每次绘制文字都 new 一个 BufferAllocator/Immediate（每个物品每帧都会调用）
    private static final BufferAllocator TEXT_ALLOCATOR = new BufferAllocator(1536);
    private static final Immediate TEXT_IMMEDIATE = VertexConsumerProvider.immediate(TEXT_ALLOCATOR);

    public static void endBuilding(BufferBuilder bb) {
        BuiltBuffer builtBuffer = bb.endNullable();
        if (builtBuffer != null) {
            BufferRenderer.drawWithGlobalProgram(builtBuffer);
        }
    }

    public static MatrixStack matrixFrom(double x, double y, double z) {
        MatrixStack matrices = new MatrixStack();
        Camera camera = mc.gameRenderer.getCamera();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
        matrices.translate(x - camera.getPos().x, y - camera.getPos().y, z - camera.getPos().z);
        return matrices;
    }

    public static void drawText3D(String text, Vec3d vec3d, Color color) {
        drawText3D(text, vec3d.x, vec3d.y, vec3d.z, 0.0, 0.0, 1.0, color.getRGB(), false);
    }

    public static void drawText3D(String text, Vec3d vec3d, int color) {
        drawText3D(text, vec3d.x, vec3d.y, vec3d.z, 0.0, 0.0, 1.0, color, false);
    }

    public static void drawText3D(String text, Vec3d vec3d, int color, boolean customFont) {
        drawText3D(text, vec3d.x, vec3d.y, vec3d.z, 0.0, 0.0, 1.0, color, customFont);
    }

    public static void drawText3D(String text, Vec3d vec3d, double scale, int color, boolean customFont) {
        drawText3D(text, vec3d.x, vec3d.y, vec3d.z, 0.0, 0.0, scale, color, customFont);
    }

    public static void drawText3D(Text text, Vec3d vec3d, double offX, double offY, double scale, Color color) {
        drawText3D(text.getString(), vec3d.x, vec3d.y, vec3d.z, offX, offY, scale, color.getRGB(), false);
    }

    public static void drawText3D(String text, double x, double y, double z, double offX, double offY, double scale, int color, boolean customFont) {
        RenderSystem.disableDepthTest();
        MatrixStack matrices = matrixFrom(x, y, z);
        Camera camera = mc.gameRenderer.getCamera();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        RenderSystem.enableBlend();
        matrices.translate(offX, offY, 0.0);
        matrices.scale(-0.025F * (float) scale, -0.025F * (float) scale, 1.0F);
        
        if (customFont) {
            float halfWidth = dev.perish.core.impl.FontManager.ui.getWidth(text) / 2.0F;
            dev.perish.core.impl.FontManager.ui.drawString(matrices, text, -halfWidth, 0.0F, color);
        } else {
            int halfWidth = mc.textRenderer.getWidth(text) / 2;
            Immediate immediate = TEXT_IMMEDIATE;
            mc.textRenderer
                    .drawLayer(text, -halfWidth, 0.0F, -1, true, matrices.peek().getPositionMatrix(), immediate, TextLayerType.SEE_THROUGH, 0, 15728880);
            immediate.draw();
            mc.textRenderer
                    .draw(text, -halfWidth, 0.0F, color, false, matrices.peek().getPositionMatrix(), immediate, TextLayerType.SEE_THROUGH, 0, 15728880);
            immediate.draw();
        }
        
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    // 把多段文字拼成一行居中绘制，每段用自己的颜色。
    // 单色的 drawText3D 每段都会各自居中，拿它拼不出并排的多色文本
    public static void drawText3D(String[] parts, int[] colors, Vec3d vec3d, double scale, boolean customFont) {
        if (parts.length == 0 || parts.length != colors.length) {
            return;
        }
        RenderSystem.disableDepthTest();
        MatrixStack matrices = matrixFrom(vec3d.x, vec3d.y, vec3d.z);
        Camera camera = mc.gameRenderer.getCamera();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        RenderSystem.enableBlend();
        matrices.scale(-0.025F * (float) scale, -0.025F * (float) scale, 1.0F);

        if (customFont) {
            float total = 0.0F;
            for (String part : parts) {
                total += dev.perish.core.impl.FontManager.ui.getWidth(part);
            }
            float x = -total / 2.0F;
            for (int i = 0; i < parts.length; i++) {
                dev.perish.core.impl.FontManager.ui.drawString(matrices, parts[i], x, 0.0F, colors[i]);
                x += dev.perish.core.impl.FontManager.ui.getWidth(parts[i]);
            }
        } else {
            int total = 0;
            for (String part : parts) {
                total += mc.textRenderer.getWidth(part);
            }
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            Immediate immediate = TEXT_IMMEDIATE;
            float x = -total / 2.0F;
            for (String part : parts) {
                mc.textRenderer.drawLayer(part, x, 0.0F, -1, true, matrix, immediate, TextLayerType.SEE_THROUGH, 0, 15728880);
                x += mc.textRenderer.getWidth(part);
            }
            immediate.draw();
            x = -total / 2.0F;
            for (int i = 0; i < parts.length; i++) {
                mc.textRenderer.draw(parts[i], x, 0.0F, colors[i], false, matrix, immediate, TextLayerType.SEE_THROUGH, 0, 15728880);
                x += mc.textRenderer.getWidth(parts[i]);
            }
            immediate.draw();
        }

        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    public static void drawFill(MatrixStack matrixStack, Box bb, Color fillColor) {
        draw3DBox(matrixStack, bb, fillColor, new Color(0, 0, 0, 0), false, true);
    }

    public static void drawBox(MatrixStack matrixStack, Box bb, Color outlineColor) {
        draw3DBox(matrixStack, bb, new Color(0, 0, 0, 0), outlineColor, true, false);
    }

    public static void drawBox(MatrixStack matrixStack, Box bb, Color outlineColor, float lineWidth) {
        draw3DBox(matrixStack, bb, new Color(0, 0, 0, 0), outlineColor, true, false, lineWidth);
    }

    public static void drawBoxLine(Box b, Color color, float lineWidth) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        MatrixStack matrices = matrixFrom(b.minX, b.minY, b.minZ);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);

        RenderSystem.lineWidth(lineWidth);
        BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.LINES, VertexFormats.LINES);
        Box box = b.offset(new Vec3d(b.minX, b.minY, b.minZ).negate());
        float x1 = (float) box.minX;
        float y1 = (float) box.minY;
        float z1 = (float) box.minZ;
        float x2 = (float) box.maxX;
        float y2 = (float) box.maxY;
        float z2 = (float) box.maxZ;
        vertexLine(matrices, buffer, x1, y1, z1, x2, y1, z1, color);
        vertexLine(matrices, buffer, x2, y1, z1, x2, y1, z2, color);
        vertexLine(matrices, buffer, x2, y1, z2, x1, y1, z2, color);
        vertexLine(matrices, buffer, x1, y1, z2, x1, y1, z1, color);
        vertexLine(matrices, buffer, x1, y1, z2, x1, y2, z2, color);
        vertexLine(matrices, buffer, x1, y1, z1, x1, y2, z1, color);
        vertexLine(matrices, buffer, x2, y1, z2, x2, y2, z2, color);
        vertexLine(matrices, buffer, x2, y1, z1, x2, y2, z1, color);
        vertexLine(matrices, buffer, x1, y2, z1, x2, y2, z1, color);
        vertexLine(matrices, buffer, x2, y2, z1, x2, y2, z2, color);
        vertexLine(matrices, buffer, x2, y2, z2, x1, y2, z2, color);
        vertexLine(matrices, buffer, x1, y2, z2, x1, y2, z1, color);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static void draw3DBox(MatrixStack matrixStack, Box box, Color fillColor, Color outlineColor) {
        draw3DBox(matrixStack, box, fillColor, outlineColor, true, true);
    }

    public static void draw3DBox(MatrixStack matrixStack, Box box, Color fillColor, Color outlineColor, boolean outline, boolean fill) {
        draw3DBox(matrixStack, box, fillColor, outlineColor, outline, fill, 1.5F);
    }

    public static void draw3DBox(MatrixStack matrixStack, Box box, Color fillColor, Color outlineColor, boolean outline, boolean fill, float lineWidth) {
        box = box.offset(mc.gameRenderer.getCamera().getPos().negate());
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        Matrix4f matrix = matrixStack.peek().getPositionMatrix();
        if (outline) {
            float a = outlineColor.getAlpha() / 255.0F;
            float r = outlineColor.getRed() / 255.0F;
            float g = outlineColor.getGreen() / 255.0F;
            float b = outlineColor.getBlue() / 255.0F;
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            RenderSystem.lineWidth(lineWidth);
            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        }

        if (fill) {
            float a = fillColor.getAlpha() / 255.0F;
            float r = fillColor.getRed() / 255.0F;
            float g = fillColor.getGreen() / 255.0F;
            float b = fillColor.getBlue() / 255.0F;
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        }

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static void drawFadeFill(MatrixStack stack, Box box, Color c, Color c1) {
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f posMatrix = stack.peek().getPositionMatrix();
        float minX = (float) (box.minX - mc.getEntityRenderDispatcher().camera.getPos().getX());
        float minY = (float) (box.minY - mc.getEntityRenderDispatcher().camera.getPos().getY());
        float minZ = (float) (box.minZ - mc.getEntityRenderDispatcher().camera.getPos().getZ());
        float maxX = (float) (box.maxX - mc.getEntityRenderDispatcher().camera.getPos().getX());
        float maxY = (float) (box.maxY - mc.getEntityRenderDispatcher().camera.getPos().getY());
        float maxZ = (float) (box.maxZ - mc.getEntityRenderDispatcher().camera.getPos().getZ());
        buffer.vertex(posMatrix, minX, minY, minZ).color(c.getRGB());
        buffer.vertex(posMatrix, maxX, minY, minZ).color(c.getRGB());
        buffer.vertex(posMatrix, maxX, minY, maxZ).color(c.getRGB());
        buffer.vertex(posMatrix, minX, minY, maxZ).color(c.getRGB());
        buffer.vertex(posMatrix, minX, minY, minZ).color(c.getRGB());
        buffer.vertex(posMatrix, minX, maxY, minZ).color(c1.getRGB());
        buffer.vertex(posMatrix, maxX, maxY, minZ).color(c1.getRGB());
        buffer.vertex(posMatrix, maxX, minY, minZ).color(c.getRGB());
        buffer.vertex(posMatrix, maxX, minY, minZ).color(c.getRGB());
        buffer.vertex(posMatrix, maxX, maxY, minZ).color(c1.getRGB());
        buffer.vertex(posMatrix, maxX, maxY, maxZ).color(c1.getRGB());
        buffer.vertex(posMatrix, maxX, minY, maxZ).color(c.getRGB());
        buffer.vertex(posMatrix, minX, minY, maxZ).color(c.getRGB());
        buffer.vertex(posMatrix, maxX, minY, maxZ).color(c.getRGB());
        buffer.vertex(posMatrix, maxX, maxY, maxZ).color(c1.getRGB());
        buffer.vertex(posMatrix, minX, maxY, maxZ).color(c1.getRGB());
        buffer.vertex(posMatrix, minX, minY, minZ).color(c.getRGB());
        buffer.vertex(posMatrix, minX, minY, maxZ).color(c.getRGB());
        buffer.vertex(posMatrix, minX, maxY, maxZ).color(c1.getRGB());
        buffer.vertex(posMatrix, minX, maxY, minZ).color(c1.getRGB());
        buffer.vertex(posMatrix, minX, maxY, minZ).color(c1.getRGB());
        buffer.vertex(posMatrix, minX, maxY, maxZ).color(c1.getRGB());
        buffer.vertex(posMatrix, maxX, maxY, maxZ).color(c1.getRGB());
        buffer.vertex(posMatrix, maxX, maxY, minZ).color(c1.getRGB());
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static void drawLine(Vec3d start, Vec3d end, Color color) {
        drawLine(start.x, start.getY(), start.z, end.getX(), end.getY(), end.getZ(), color, 1.0F);
    }

    public static void drawLine(double x1, double y1, double z1, double x2, double y2, double z2, Color color, float width) {
        RenderSystem.enableBlend();
        MatrixStack matrices = matrixFrom(x1, y1, z1);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
        RenderSystem.lineWidth(width);
        BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.LINES, VertexFormats.LINES);
        vertexLine(matrices, buffer, 0.0, 0.0, 0.0, (float) (x2 - x1), (float) (y2 - y1), (float) (z2 - z1), color);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableCull();
        RenderSystem.lineWidth(1.0F);
        RenderSystem.disableBlend();
    }

    public static void vertexLine(MatrixStack matrices, VertexConsumer buffer, double x1, double y1, double z1, double x2, double y2, double z2, Color lineColor) {
        Matrix4f model = matrices.peek().getPositionMatrix();
        Entry entry = matrices.peek();
        Vector3f normalVec = getNormal((float) x1, (float) y1, (float) z1, (float) x2, (float) y2, (float) z2);
        buffer.vertex(model, (float) x1, (float) y1, (float) z1)
                .color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), lineColor.getAlpha())
                .normal(entry, normalVec.x(), normalVec.y(), normalVec.z());
        buffer.vertex(model, (float) x2, (float) y2, (float) z2)
                .color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), lineColor.getAlpha())
                .normal(entry, normalVec.x(), normalVec.y(), normalVec.z());
    }

    public static Vector3f getNormal(float x1, float y1, float z1, float x2, float y2, float z2) {
        float xNormal = x2 - x1;
        float yNormal = y2 - y1;
        float zNormal = z2 - z1;
        float normalSqrt = MathHelper.sqrt(xNormal * xNormal + yNormal * yNormal + zNormal * zNormal);
        return new Vector3f(xNormal / normalSqrt, yNormal / normalSqrt, zNormal / normalSqrt);
    }

    public static void drawTargetEsp(MatrixStack stack, @NotNull Entity target, Color color) {
        ArrayList<Vec3d> vecs = new ArrayList();
        ArrayList<Vec3d> vecs1 = new ArrayList();
        ArrayList<Vec3d> vecs2 = new ArrayList();
        double x = target.prevX + (target.getX() - target.prevX) * getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().getX();
        double y = target.prevY + (target.getY() - target.prevY) * getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().getY();
        double z = target.prevZ + (target.getZ() - target.prevZ) * getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().getZ();
        double height = target.getHeight();

        for (int i = 0; i <= 361; i++) {
            double v = Math.sin(Math.toRadians(i));
            double u = Math.cos(Math.toRadians(i));
            Vec3d vec = new Vec3d((float) (u * 0.5), height, (float) (v * 0.5));
            vecs.add(vec);
            double v1 = Math.sin(Math.toRadians((i + 120) % 360));
            double u1 = Math.cos(Math.toRadians(i + 120) % 360.0);
            Vec3d vec1 = new Vec3d((float) (u1 * 0.5), height, (float) (v1 * 0.5));
            vecs1.add(vec1);
            double v2 = Math.sin(Math.toRadians((i + 240) % 360));
            double u2 = Math.cos(Math.toRadians((i + 240) % 360));
            Vec3d vec2 = new Vec3d((float) (u2 * 0.5), height, (float) (v2 * 0.5));
            vecs2.add(vec2);
            height -= 0.004F;
        }

        stack.push();
        stack.translate(x, y, z);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = stack.peek().getPositionMatrix();

        for (int j = 0; j < vecs.size() - 1; j++) {
            float alpha = 1.0F - (j + (float) (System.currentTimeMillis() - Perish.initTime) / 5.0F) % 360.0F / 60.0F;
            bufferBuilder.vertex(matrix, (float) ((Vec3d) vecs.get(j)).x, (float) ((Vec3d) vecs.get(j)).y, (float) ((Vec3d) vecs.get(j)).z)
                    .color(ColorUtil.injectAlpha(ColorUtil.pulseColor(color, (int) (j / 20.0F), 10, 1.0), (int) (alpha * 255.0F)).getRGB());
            bufferBuilder.vertex(matrix, (float) ((Vec3d) vecs.get(j + 1)).x, (float) ((Vec3d) vecs.get(j + 1)).y + 0.1F, (float) ((Vec3d) vecs.get(j + 1)).z)
                    .color(ColorUtil.injectAlpha(ColorUtil.pulseColor(color, (int) (j / 20.0F), 10, 1.0), (int) (alpha * 255.0F)).getRGB());
        }

        endBuilding(bufferBuilder);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        bufferBuilder = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        for (int j = 0; j < vecs1.size() - 1; j++) {
            float alpha = 1.0F - (j + (float) (System.currentTimeMillis() - Perish.initTime) / 5.0F) % 360.0F / 60.0F;
            bufferBuilder.vertex(matrix, (float) ((Vec3d) vecs1.get(j)).x, (float) ((Vec3d) vecs1.get(j)).y, (float) ((Vec3d) vecs1.get(j)).z)
                    .color(ColorUtil.injectAlpha(ColorUtil.pulseColor(color, (int) (j / 20.0F), 10, 1.0), (int) (alpha * 255.0F)).getRGB());
            bufferBuilder.vertex(matrix, (float) ((Vec3d) vecs1.get(j + 1)).x, (float) ((Vec3d) vecs1.get(j + 1)).y + 0.1F, (float) ((Vec3d) vecs1.get(j + 1)).z)
                    .color(ColorUtil.injectAlpha(ColorUtil.pulseColor(color, (int) (j / 20.0F), 10, 1.0), (int) (alpha * 255.0F)).getRGB());
        }

        endBuilding(bufferBuilder);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        bufferBuilder = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        for (int j = 0; j < vecs2.size() - 1; j++) {
            float alpha = 1.0F - (j + (float) (System.currentTimeMillis() - Perish.initTime) / 5.0F) % 360.0F / 60.0F;
            bufferBuilder.vertex(matrix, (float) ((Vec3d) vecs2.get(j)).x, (float) ((Vec3d) vecs2.get(j)).y, (float) ((Vec3d) vecs2.get(j)).z)
                    .color(ColorUtil.injectAlpha(ColorUtil.pulseColor(color, (int) (j / 20.0F), 10, 1.0), (int) (alpha * 255.0F)).getRGB());
            bufferBuilder.vertex(matrix, (float) ((Vec3d) vecs2.get(j + 1)).x, (float) ((Vec3d) vecs2.get(j + 1)).y + 0.1F, (float) ((Vec3d) vecs2.get(j + 1)).z)
                    .color(ColorUtil.injectAlpha(ColorUtil.pulseColor(color, (int) (j / 20.0F), 10, 1.0), (int) (alpha * 255.0F)).getRGB());
        }

        endBuilding(bufferBuilder);
        RenderSystem.enableCull();
        stack.translate(-x, -y, -z);
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        stack.pop();
    }

    public static float getTickDelta() {
        return mc.getRenderTickCounter().getTickDelta(true);
    }
}
