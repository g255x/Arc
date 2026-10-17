package dev.perish.api.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.perish.api.utils.Wrapper;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.*;
import org.joml.Matrix4f;

import java.awt.*;

public class Render2DUtil implements Wrapper {
    public static void rect(MatrixStack stack, float x1, float y1, float x2, float y2, int color) {
        rectFilled(stack, x1, y1, x2, y2, color);
    }

    public static void arrow(MatrixStack matrixStack, float x, float y, Color color) {
        drawRectWithOutline(matrixStack, x - 1.0F, y - 1.0F, 2.0F, 2.0F, color, Color.BLACK);
    }

    public static void rectFilled(MatrixStack matrix, float x1, float y1, float x2, float y2, int color) {
        float f = (color >> 24 & 0xFF) / 255.0F;
        float g = (color >> 16 & 0xFF) / 255.0F;
        float h = (color >> 8 & 0xFF) / 255.0F;
        float j = (color & 0xFF) / 255.0F;
        if (!(f <= 0.01)) {
            if (x1 < x2) {
                float i = x1;
                x1 = x2;
                x2 = i;
            }

            if (y1 < y2) {
                float i = y1;
                y1 = y2;
                y2 = i;
            }

            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            bufferBuilder.vertex(matrix.peek().getPositionMatrix(), x1, y2, 0.0F).color(g, h, j, f);
            bufferBuilder.vertex(matrix.peek().getPositionMatrix(), x2, y2, 0.0F).color(g, h, j, f);
            bufferBuilder.vertex(matrix.peek().getPositionMatrix(), x2, y1, 0.0F).color(g, h, j, f);
            bufferBuilder.vertex(matrix.peek().getPositionMatrix(), x1, y1, 0.0F).color(g, h, j, f);
            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
            RenderSystem.disableBlend();
        }
    }

    public static void horizontalGradient(MatrixStack matrices, float x1, float y1, float x2, float y2, Color startColor, Color endColor) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix, x1, y1, 0.0F).color(startColor.getRGB());
        bufferBuilder.vertex(matrix, x1, y2, 0.0F).color(startColor.getRGB());
        bufferBuilder.vertex(matrix, x2, y2, 0.0F).color(endColor.getRGB());
        bufferBuilder.vertex(matrix, x2, y1, 0.0F).color(endColor.getRGB());
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.disableBlend();
    }

    public static void horizontalGradient(MatrixStack matrices, float x1, float y1, float x2, float y2, int startColor, int endColor) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix, x1, y1, 0.0F).color(startColor);
        bufferBuilder.vertex(matrix, x1, y2, 0.0F).color(startColor);
        bufferBuilder.vertex(matrix, x2, y2, 0.0F).color(endColor);
        bufferBuilder.vertex(matrix, x2, y1, 0.0F).color(endColor);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.disableBlend();
    }

    public static void verticalGradient(MatrixStack matrices, float x1, float y1, float x2, float y2, Color startColor, Color endColor) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix, x1, y1, 0.0F).color(startColor.getRGB());
        bufferBuilder.vertex(matrix, x2, y1, 0.0F).color(startColor.getRGB());
        bufferBuilder.vertex(matrix, x2, y2, 0.0F).color(endColor.getRGB());
        bufferBuilder.vertex(matrix, x1, y2, 0.0F).color(endColor.getRGB());
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.disableBlend();
    }

    public static void verticalGradient(MatrixStack matrices, float x1, float y1, float x2, float y2, int startColor, int endColor) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix, x1, y1, 0.0F).color(startColor);
        bufferBuilder.vertex(matrix, x2, y1, 0.0F).color(startColor);
        bufferBuilder.vertex(matrix, x2, y2, 0.0F).color(endColor);
        bufferBuilder.vertex(matrix, x1, y2, 0.0F).color(endColor);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.disableBlend();
    }

    public static void setRectPoints(BufferBuilder bufferBuilder, Matrix4f matrix, float x, float y, float x1, float y1, Color c1, Color c2, Color c3, Color c4) {
        bufferBuilder.vertex(matrix, x, y1, 0.0f).color(c1.getRGB());
        bufferBuilder.vertex(matrix, x1, y1, 0.0f).color(c2.getRGB());
        bufferBuilder.vertex(matrix, x1, y, 0.0f).color(c3.getRGB());
        bufferBuilder.vertex(matrix, x, y, 0.0f).color(c4.getRGB());
    }

    public static void drawLine(MatrixStack matrices, float x, float y, float x1, float y1, int color) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix, x, y, 0.0F).color(color);
        bufferBuilder.vertex(matrix, x1, y1, 0.0F).color(color);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.disableBlend();
    }

    public static void drawRectWithOutline(MatrixStack matrices, float x, float y, float width, float height, Color c, Color c2) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, x, y + height, 0.0F).color(c.getRGB());
        buffer.vertex(matrix, x + width, y + height, 0.0F).color(c.getRGB());
        buffer.vertex(matrix, x + width, y, 0.0F).color(c.getRGB());
        buffer.vertex(matrix, x, y, 0.0F).color(c.getRGB());
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        buffer = Tessellator.getInstance().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, x, y + height, 0.0F).color(c2.getRGB());
        buffer.vertex(matrix, x + width, y + height, 0.0F).color(c2.getRGB());
        buffer.vertex(matrix, x + width, y, 0.0F).color(c2.getRGB());
        buffer.vertex(matrix, x, y, 0.0F).color(c2.getRGB());
        buffer.vertex(matrix, x, y + height, 0.0F).color(c2.getRGB());
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
    }

    public static void drawRect(MatrixStack matrices, float x, float y, float width, float height, int c) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix, x, y + height, 0.0F).color(c);
        bufferBuilder.vertex(matrix, x + width, y + height, 0.0F).color(c);
        bufferBuilder.vertex(matrix, x + width, y, 0.0F).color(c);
        bufferBuilder.vertex(matrix, x, y, 0.0F).color(c);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.disableBlend();
    }

    public static void drawRect(MatrixStack matrices, float x, float y, float width, float height, Color c) {
        drawRect(matrices, x, y, width, height, c.getRGB());
    }

    public static void drawRect(DrawContext drawContext, float x, float y, float width, float height, Color c) {
        drawRect(drawContext.getMatrices(), x, y, width, height, c);
    }

    public static void drawBorderedRect(MatrixStack matrices, float x1, float y1, float x2, float y2, float borderWidth, int fillColor, int borderColor) {
        float left = Math.min(x1, x2);
        float right = Math.max(x1, x2);
        float top = Math.min(y1, y2);
        float bottom = Math.max(y1, y2);
        float border = Math.max(0.0F, borderWidth);

        rectFilled(matrices, left, top, right, bottom, fillColor);
        if (border <= 0.0F) {
            return;
        }

        rectFilled(matrices, left, top, right, top + border, borderColor);
        rectFilled(matrices, left, bottom - border, right, bottom, borderColor);
        rectFilled(matrices, left, top + border, left + border, bottom - border, borderColor);
        rectFilled(matrices, right - border, top + border, right, bottom - border, borderColor);
    }

    public static boolean isHovered(double mouseX, double mouseY, double x, double y, double width, double height) {
        return mouseX >= x && mouseX - width <= x && mouseY >= y && mouseY - height <= y;
    }

    public static void drawGlow(MatrixStack matrices, float x, float y, float width, float height, int color) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int startColor = ColorUtil.injectAlpha(color, 20);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        float halfWidth = width / 2.0F;
        float halfHeight = height / 2.0F;
        float centerX = x + halfWidth;
        float centerY = y + halfHeight;
        float x2 = x + width;
        float y2 = y + height;
        bufferBuilder.vertex(matrix, centerX, centerY, 0.0F).color(color);
        bufferBuilder.vertex(matrix, x, centerY, 0.0F).color(startColor);
        bufferBuilder.vertex(matrix, x, y, 0.0F).color(startColor);
        bufferBuilder.vertex(matrix, centerX, y, 0.0F).color(startColor);
        bufferBuilder.vertex(matrix, centerX, centerY, 0.0F).color(color);
        bufferBuilder.vertex(matrix, centerX, y, 0.0F).color(startColor);
        bufferBuilder.vertex(matrix, x2, y, 0.0F).color(startColor);
        bufferBuilder.vertex(matrix, x2, centerY, 0.0F).color(startColor);
        bufferBuilder.vertex(matrix, centerX, centerY, 0.0F).color(color);
        bufferBuilder.vertex(matrix, x, centerY, 0.0F).color(startColor);
        bufferBuilder.vertex(matrix, x, y2, 0.0F).color(startColor);
        bufferBuilder.vertex(matrix, centerX, y2, 0.0F).color(startColor);
        bufferBuilder.vertex(matrix, centerX, centerY, 0.0F).color(color);
        bufferBuilder.vertex(matrix, x2, centerY, 0.0F).color(startColor);
        bufferBuilder.vertex(matrix, x2, y2, 0.0F).color(startColor);
        bufferBuilder.vertex(matrix, centerX, y2, 0.0F).color(startColor);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }

    public static void drawCircle(MatrixStack matrices, float cx, float cy, float r, Color c, int segments) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix, cx, cy, 0.0F).color(c.getRGB());

        for (int i = 0; i <= segments; i++) {
            double a = i * (Math.PI * 2) / segments;
            float x = (float) (cx + Math.cos(a) * r);
            float y = (float) (cy + Math.sin(a) * r);
            bufferBuilder.vertex(matrix, x, y, 0.0F).color(c.getRGB());
        }

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static void drawPill(MatrixStack matrices, float x, float y, float width, float height, Color c) {
        float r = height / 2.0F;
        drawRect(matrices, x + r, y, width - 2.0F * r, height, c);
        drawCircle(matrices, x + r, y + r, r, c, 64);
        drawCircle(matrices, x + width - r, y + r, r, c, 64);
    }

    public static void drawSmoothCircle(MatrixStack matrices, float cx, float cy, float r, Color c, int segments) {
        if (c.getAlpha() <= 3 || r <= 0.0F) return;
        int seg = Math.max(segments, 32);
        float feather = Math.min(1.0F, r * 0.28F);
        if (feather < 0.35F) {
            drawCircle(matrices, cx, cy, r, c, seg);
            return;
        }

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        int cr = c.getRed(), cg = c.getGreen(), cb = c.getBlue(), ca = c.getAlpha();
        float innerR = Math.max(0.0F, r - feather);

        BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < seg; i++) {
            double a0 = i * (Math.PI * 2) / seg;
            double a1 = (i + 1) * (Math.PI * 2) / seg;

            float ix0 = (float) (cx + Math.cos(a0) * innerR);
            float iy0 = (float) (cy + Math.sin(a0) * innerR);
            float ix1 = (float) (cx + Math.cos(a1) * innerR);
            float iy1 = (float) (cy + Math.sin(a1) * innerR);

            float ox0 = (float) (cx + Math.cos(a0) * r);
            float oy0 = (float) (cy + Math.sin(a0) * r);
            float ox1 = (float) (cx + Math.cos(a1) * r);
            float oy1 = (float) (cy + Math.sin(a1) * r);

            buffer.vertex(matrix, cx, cy, 0.0F).color(cr, cg, cb, ca);
            buffer.vertex(matrix, ix0, iy0, 0.0F).color(cr, cg, cb, ca);
            buffer.vertex(matrix, ix1, iy1, 0.0F).color(cr, cg, cb, ca);

            buffer.vertex(matrix, ix0, iy0, 0.0F).color(cr, cg, cb, ca);
            buffer.vertex(matrix, ox0, oy0, 0.0F).color(cr, cg, cb, 0);
            buffer.vertex(matrix, ix1, iy1, 0.0F).color(cr, cg, cb, ca);

            buffer.vertex(matrix, ix1, iy1, 0.0F).color(cr, cg, cb, ca);
            buffer.vertex(matrix, ox0, oy0, 0.0F).color(cr, cg, cb, 0);
            buffer.vertex(matrix, ox1, oy1, 0.0F).color(cr, cg, cb, 0);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static void drawSmoothPill(MatrixStack matrices, float x, float y, float width, float height, Color c) {
        if (c.getAlpha() <= 3 || width <= 0.0F || height <= 0.0F) return;
        float r = Math.min(height / 2.0F, width / 2.0F);
        float feather = Math.min(1.0F, r * 0.28F);
        if (feather < 0.35F) {
            drawPill(matrices, x, y, width, height, c);
            return;
        }

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        int cr = c.getRed(), cg = c.getGreen(), cb = c.getBlue(), ca = c.getAlpha();
        int seg = Math.max(6, (int) (r * 1.4F));
        int n = seg * 4 + 4;
        float[][] boundary = new float[n][2];
        float[][] outer = new float[n][2];

        float x1 = x + r, x2 = x + width - r;
        float y1 = y + r, y2 = y + height - r;
        float cx = x + width / 2.0F;
        float cy = y + height / 2.0F;
        int idx = 0;

        for (int i = 0; i <= seg; i++) {
            double a = Math.PI + i * (Math.PI / 2) / seg;
            float bx = (float) (x1 + Math.cos(a) * r);
            float by = (float) (y1 + Math.sin(a) * r);
            boundary[idx][0] = bx;
            boundary[idx][1] = by;
            outer[idx][0] = (float) (bx + Math.cos(a) * feather);
            outer[idx][1] = (float) (by + Math.sin(a) * feather);
            idx++;
        }
        boundary[idx][0] = x2;
        boundary[idx][1] = y;
        outer[idx][0] = x2;
        outer[idx][1] = y - feather;
        idx++;
        for (int i = 1; i <= seg; i++) {
            double a = 3.0 * Math.PI / 2.0 + i * (Math.PI / 2) / seg;
            float bx = (float) (x2 + Math.cos(a) * r);
            float by = (float) (y1 + Math.sin(a) * r);
            boundary[idx][0] = bx;
            boundary[idx][1] = by;
            outer[idx][0] = (float) (bx + Math.cos(a) * feather);
            outer[idx][1] = (float) (by + Math.sin(a) * feather);
            idx++;
        }
        boundary[idx][0] = x + width;
        boundary[idx][1] = y2;
        outer[idx][0] = x + width + feather;
        outer[idx][1] = y2;
        idx++;
        for (int i = 1; i <= seg; i++) {
            double a = i * (Math.PI / 2) / seg;
            float bx = (float) (x2 + Math.cos(a) * r);
            float by = (float) (y2 + Math.sin(a) * r);
            boundary[idx][0] = bx;
            boundary[idx][1] = by;
            outer[idx][0] = (float) (bx + Math.cos(a) * feather);
            outer[idx][1] = (float) (by + Math.sin(a) * feather);
            idx++;
        }
        boundary[idx][0] = x1;
        boundary[idx][1] = y + height;
        outer[idx][0] = x1;
        outer[idx][1] = y + height + feather;
        idx++;
        for (int i = 1; i <= seg; i++) {
            double a = Math.PI / 2.0 + i * (Math.PI / 2) / seg;
            float bx = (float) (x1 + Math.cos(a) * r);
            float by = (float) (y2 + Math.sin(a) * r);
            boundary[idx][0] = bx;
            boundary[idx][1] = by;
            outer[idx][0] = (float) (bx + Math.cos(a) * feather);
            outer[idx][1] = (float) (by + Math.sin(a) * feather);
            idx++;
        }

        BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < n; i++) {
            float[] b0 = boundary[i];
            float[] b1 = boundary[(i + 1) % n];
            float[] o0 = outer[i];
            float[] o1 = outer[(i + 1) % n];

            buffer.vertex(matrix, cx, cy, 0.0F).color(cr, cg, cb, ca);
            buffer.vertex(matrix, b0[0], b0[1], 0.0F).color(cr, cg, cb, ca);
            buffer.vertex(matrix, b1[0], b1[1], 0.0F).color(cr, cg, cb, ca);

            buffer.vertex(matrix, b0[0], b0[1], 0.0F).color(cr, cg, cb, ca);
            buffer.vertex(matrix, o0[0], o0[1], 0.0F).color(cr, cg, cb, 0);
            buffer.vertex(matrix, b1[0], b1[1], 0.0F).color(cr, cg, cb, ca);

            buffer.vertex(matrix, b1[0], b1[1], 0.0F).color(cr, cg, cb, ca);
            buffer.vertex(matrix, o0[0], o0[1], 0.0F).color(cr, cg, cb, 0);
            buffer.vertex(matrix, o1[0], o1[1], 0.0F).color(cr, cg, cb, 0);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static void drawToggle(MatrixStack matrices, float x, float y, float width, float height, double progress, Color trackOffColor, Color trackOnColor, Color knobColor) {
        float padding = 1.5F;
        float trackHeight = height - padding * 2.0F;
        float trackY = y + padding;
        float trackR = trackHeight / 2.0F;
        float knobRadius = trackR - 0.5F;

        Color trackColor = blend(trackOffColor, trackOnColor, (float) progress);
        drawSmoothPill(matrices, x, trackY, width, trackHeight, trackColor);

        float travel = width - knobRadius * 2.0F - padding * 2.0F;
        float knobX = x + padding + knobRadius + (float) (progress * travel);
        float knobY = y + height / 2.0F;

        Color shadowColor = new Color(0, 0, 0, 48);
        drawSmoothCircle(matrices, knobX, knobY + 0.6F, knobRadius + 0.6F, shadowColor, 32);
        drawSmoothCircle(matrices, knobX, knobY, knobRadius, knobColor, 40);
    }

    private static Color blend(Color a, Color b, float t) {
        float clamped = Math.max(0.0F, Math.min(1.0F, t));
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * clamped),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * clamped),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * clamped),
                (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * clamped));
    }

    public static void drawSmoothRoundedRect(MatrixStack matrices, float x, float y, float width, float height, float radius, Color c) {
        if (c.getAlpha() <= 3) return;
        float feather = Math.min(1.0F, radius * 0.18F);
        if (feather <= 0.25F) {
            drawRoundedRect(matrices, x, y, width, height, radius, c, 64);
            return;
        }

        int rings = 8;
        for (int ring = 0; ring <= rings; ring++) {
            float t = ring / (float) rings;
            float offset = t * feather;
            int alpha = (int) (c.getAlpha() * (1.0F - t));
            if (alpha <= 1) continue;
            Color ringColor = new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            drawRoundedRect(matrices, x - offset, y - offset, width + offset * 2, height + offset * 2, radius + offset, ringColor, 64);
        }
    }

    public static void drawSmoothRoundedStroke(MatrixStack matrices, float x, float y, float width, float height, float radius, Color c) {
        if (c.getAlpha() <= 3) return;
        float r = Math.min(radius, Math.min(width, height) / 2.0F);
        float half = 0.5F;
        float feather = 0.5F;
        int seg = 48;

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        int cr = c.getRed(), cg = c.getGreen(), cb = c.getBlue(), ca = c.getAlpha();

        float x1 = x + r, x2 = x + width - r;
        float y1 = y + r, y2 = y + height - r;

        java.util.ArrayList<float[]> path = new java.util.ArrayList<>();

        for (int i = 0; i <= seg; i++) {
            double a = Math.PI + i * (Math.PI / 2) / seg;
            path.add(new float[]{(float)(x1 + Math.cos(a)*r), (float)(y1 + Math.sin(a)*r), (float)Math.cos(a), (float)Math.sin(a)});
        }
        path.add(new float[]{x2, y, 0, -1});
        for (int i = 1; i <= seg; i++) {
            double a = 3*Math.PI/2 + i * (Math.PI / 2) / seg;
            path.add(new float[]{(float)(x2 + Math.cos(a)*r), (float)(y1 + Math.sin(a)*r), (float)Math.cos(a), (float)Math.sin(a)});
        }
        path.add(new float[]{x + width, y2, 1, 0});
        for (int i = 1; i <= seg; i++) {
            double a = i * (Math.PI / 2) / seg;
            path.add(new float[]{(float)(x2 + Math.cos(a)*r), (float)(y2 + Math.sin(a)*r), (float)Math.cos(a), (float)Math.sin(a)});
        }
        path.add(new float[]{x1, y + height, 0, 1});
        for (int i = 1; i <= seg; i++) {
            double a = Math.PI/2 + i * (Math.PI / 2) / seg;
            path.add(new float[]{(float)(x1 + Math.cos(a)*r), (float)(y2 + Math.sin(a)*r), (float)Math.cos(a), (float)Math.sin(a)});
        }
        path.add(new float[]{x, y1, -1, 0});

        BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        int total = path.size();
        for (int i = 0; i < total; i++) {
            float[] p0 = path.get(i);
            float[] p1 = path.get((i + 1) % total);

            float o0x = p0[0] + p0[2] * (half + feather), o0y = p0[1] + p0[3] * (half + feather);
            float o1x = p1[0] + p1[2] * (half + feather), o1y = p1[1] + p1[3] * (half + feather);
            float m0x = p0[0] + p0[2] * half, m0y = p0[1] + p0[3] * half;
            float m1x = p1[0] + p1[2] * half, m1y = p1[1] + p1[3] * half;
            float n0x = p0[0] - p0[2] * half, n0y = p0[1] - p0[3] * half;
            float n1x = p1[0] - p1[2] * half, n1y = p1[1] - p1[3] * half;
            float i0x = p0[0] - p0[2] * (half + feather), i0y = p0[1] - p0[3] * (half + feather);
            float i1x = p1[0] - p1[2] * (half + feather), i1y = p1[1] - p1[3] * (half + feather);

            buffer.vertex(matrix, o0x, o0y, 0).color(cr, cg, cb, 0);
            buffer.vertex(matrix, o1x, o1y, 0).color(cr, cg, cb, 0);
            buffer.vertex(matrix, m1x, m1y, 0).color(cr, cg, cb, ca);
            buffer.vertex(matrix, m0x, m0y, 0).color(cr, cg, cb, ca);

            buffer.vertex(matrix, m0x, m0y, 0).color(cr, cg, cb, ca);
            buffer.vertex(matrix, m1x, m1y, 0).color(cr, cg, cb, ca);
            buffer.vertex(matrix, n1x, n1y, 0).color(cr, cg, cb, ca);
            buffer.vertex(matrix, n0x, n0y, 0).color(cr, cg, cb, ca);

            buffer.vertex(matrix, n0x, n0y, 0).color(cr, cg, cb, ca);
            buffer.vertex(matrix, n1x, n1y, 0).color(cr, cg, cb, ca);
            buffer.vertex(matrix, i1x, i1y, 0).color(cr, cg, cb, 0);
            buffer.vertex(matrix, i0x, i0y, 0).color(cr, cg, cb, 0);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static void drawRoundedStroke(MatrixStack matrices, float x, float y, float width, float height, float radius, Color c, int seg) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        float r = Math.min(radius, Math.min(width, height) / 2.0F);
        float x1 = x + r;
        float y1 = y + r;
        float x2 = x + width - r;
        float y2 = y + height - r;

        for (int i = 0; i <= seg; i++) {
            double a = (-Math.PI / 2) + i * (Math.PI / 2) / seg;
            buffer.vertex(matrix, (float) (x2 + Math.cos(a) * r), (float) (y1 + Math.sin(a) * r), 0.0F).color(c.getRGB());
        }

        for (int i = 0; i <= seg; i++) {
            double a = 0.0 + i * (Math.PI / 2) / seg;
            buffer.vertex(matrix, (float) (x2 + Math.cos(a) * r), (float) (y2 + Math.sin(a) * r), 0.0F).color(c.getRGB());
        }

        for (int i = 0; i <= seg; i++) {
            double a = (Math.PI / 2) + i * (Math.PI / 2) / seg;
            buffer.vertex(matrix, (float) (x1 + Math.cos(a) * r), (float) (y2 + Math.sin(a) * r), 0.0F).color(c.getRGB());
        }

        for (int i = 0; i <= seg; i++) {
            double a = Math.PI + i * (Math.PI / 2) / seg;
            buffer.vertex(matrix, (float) (x1 + Math.cos(a) * r), (float) (y1 + Math.sin(a) * r), 0.0F).color(c.getRGB());
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
    }

    public static void drawRainbowRoundedStroke(MatrixStack matrices, float x, float y, float width, float height, float radius, int seg, float speed, int alpha) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        float r = Math.min(radius, Math.min(width, height) / 2.0F);
        float x1 = x + r;
        float y1 = y + r;
        float x2 = x + width - r;
        float y2 = y + height - r;
        double t = (double) (System.currentTimeMillis() % (long) (1000.0F / Math.max(0.001F, speed))) / (1000.0F / Math.max(0.001F, speed));

        for (int i = 0; i <= seg; i++) {
            double a = (-Math.PI / 2) + i * (Math.PI / 2) / seg;
            float hue = (float) ((a + (Math.PI * 2)) / (Math.PI * 2) + t) % 1.0F;
            int rgb = Color.HSBtoRGB(hue, 1.0F, 1.0F);
            int argb = alpha << 24 | rgb & 16777215;
            buffer.vertex(matrix, (float) (x2 + Math.cos(a) * r), (float) (y1 + Math.sin(a) * r), 0.0F).color(argb);
        }

        for (int i = 0; i <= seg; i++) {
            double a = 0.0 + i * (Math.PI / 2) / seg;
            float hue = (float) ((a + (Math.PI * 2)) / (Math.PI * 2) + t) % 1.0F;
            int rgb = Color.HSBtoRGB(hue, 1.0F, 1.0F);
            int argb = alpha << 24 | rgb & 16777215;
            buffer.vertex(matrix, (float) (x2 + Math.cos(a) * r), (float) (y2 + Math.sin(a) * r), 0.0F).color(argb);
        }

        for (int i = 0; i <= seg; i++) {
            double a = (Math.PI / 2) + i * (Math.PI / 2) / seg;
            float hue = (float) ((a + (Math.PI * 2)) / (Math.PI * 2) + t) % 1.0F;
            int rgb = Color.HSBtoRGB(hue, 1.0F, 1.0F);
            int argb = alpha << 24 | rgb & 16777215;
            buffer.vertex(matrix, (float) (x1 + Math.cos(a) * r), (float) (y2 + Math.sin(a) * r), 0.0F).color(argb);
        }

        for (int i = 0; i <= seg; i++) {
            double a = Math.PI + i * (Math.PI / 2) / seg;
            float hue = (float) ((a + (Math.PI * 2)) / (Math.PI * 2) + t) % 1.0F;
            int rgb = Color.HSBtoRGB(hue, 1.0F, 1.0F);
            int argb = alpha << 24 | rgb & 16777215;
            buffer.vertex(matrix, (float) (x1 + Math.cos(a) * r), (float) (y1 + Math.sin(a) * r), 0.0F).color(argb);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        BufferBuilder inner = Tessellator.getInstance().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        float rIn = Math.max(0.5F, r - 1.2F);

        for (int i = 0; i <= seg; i++) {
            double a = (-Math.PI / 2) + i * (Math.PI / 2) / seg;
            float hue = (float) ((a + (Math.PI * 2)) / (Math.PI * 2) + t) % 1.0F;
            int rgb = Color.HSBtoRGB(hue, 1.0F, 1.0F);
            int argb = alpha << 24 | rgb & 16777215;
            inner.vertex(matrix, (float) (x2 + Math.cos(a) * rIn), (float) (y1 + Math.sin(a) * rIn), 0.0F).color(argb);
        }

        for (int i = 0; i <= seg; i++) {
            double a = 0.0 + i * (Math.PI / 2) / seg;
            float hue = (float) ((a + (Math.PI * 2)) / (Math.PI * 2) + t) % 1.0F;
            int rgb = Color.HSBtoRGB(hue, 1.0F, 1.0F);
            int argb = alpha << 24 | rgb & 16777215;
            inner.vertex(matrix, (float) (x2 + Math.cos(a) * rIn), (float) (y2 + Math.sin(a) * rIn), 0.0F).color(argb);
        }

        for (int i = 0; i <= seg; i++) {
            double a = (Math.PI / 2) + i * (Math.PI / 2) / seg;
            float hue = (float) ((a + (Math.PI * 2)) / (Math.PI * 2) + t) % 1.0F;
            int rgb = Color.HSBtoRGB(hue, 1.0F, 1.0F);
            int argb = alpha << 24 | rgb & 16777215;
            inner.vertex(matrix, (float) (x1 + Math.cos(a) * rIn), (float) (y2 + Math.sin(a) * rIn), 0.0F).color(argb);
        }

        for (int i = 0; i <= seg; i++) {
            double a = Math.PI + i * (Math.PI / 2) / seg;
            float hue = (float) ((a + (Math.PI * 2)) / (Math.PI * 2) + t) % 1.0F;
            int rgb = Color.HSBtoRGB(hue, 1.0F, 1.0F);
            int argb = alpha << 24 | rgb & 16777215;
            inner.vertex(matrix, (float) (x1 + Math.cos(a) * rIn), (float) (y1 + Math.sin(a) * rIn), 0.0F).color(argb);
        }

        BufferRenderer.drawWithGlobalProgram(inner.end());
        RenderSystem.disableBlend();
    }

    public static void drawRoundedRect(MatrixStack matrices, float x, float y, float width, float height, float radius, Color c) {
        drawRoundedRect(matrices, x, y, width, height, radius, c, 64);
    }

    public static void drawRoundedRect(MatrixStack matrices, float x, float y, float width, float height, float radius, Color c, int seg) {
        if (radius <= 0.0F) {
            drawRect(matrices, x, y, width, height, c);
        } else {
            float r = Math.min(radius, Math.min(width, height) / 2.0F);
            drawRect(matrices, x + r, y, width - 2.0F * r, height, c);
            drawRect(matrices, x, y + r, r, height - 2.0F * r, c);
            drawRect(matrices, x + width - r, y + r, r, height - 2.0F * r, c);
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            int segments = Math.max(seg, 16);
            float cx = x + r;
            float cy = y + r;
            BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
            buffer.vertex(matrix, cx, cy, 0.0F).color(c.getRGB());

            for (int i = 0; i <= segments; i++) {
                double a = Math.PI + i * (Math.PI / 2) / segments;
                buffer.vertex(matrix, (float) (cx + Math.cos(a) * r), (float) (cy + Math.sin(a) * r), 0.0F).color(c.getRGB());
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            cx = x + width - r;
            cy = y + r;
            buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
            buffer.vertex(matrix, cx, cy, 0.0F).color(c.getRGB());

            for (int i = 0; i <= segments; i++) {
                double a = (Math.PI * 3.0 / 2.0) + i * (Math.PI / 2) / segments;
                buffer.vertex(matrix, (float) (cx + Math.cos(a) * r), (float) (cy + Math.sin(a) * r), 0.0F).color(c.getRGB());
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            cx = x + width - r;
            cy = y + height - r;
            buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
            buffer.vertex(matrix, cx, cy, 0.0F).color(c.getRGB());

            for (int i = 0; i <= segments; i++) {
                double a = 0.0 + i * (Math.PI / 2) / segments;
                buffer.vertex(matrix, (float) (cx + Math.cos(a) * r), (float) (cy + Math.sin(a) * r), 0.0F).color(c.getRGB());
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            cx = x + r;
            cy = y + height - r;
            buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
            buffer.vertex(matrix, cx, cy, 0.0F).color(c.getRGB());

            for (int i = 0; i <= segments; i++) {
                double a = (Math.PI / 2) + i * (Math.PI / 2) / segments;
                buffer.vertex(matrix, (float) (cx + Math.cos(a) * r), (float) (cy + Math.sin(a) * r), 0.0F).color(c.getRGB());
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }

    public static void drawDropShadow(MatrixStack matrices, float x, float y, float width, float height, float radius) {
    }

    public static void setupRender() {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static void endRender() {
        RenderSystem.disableBlend();
    }
}
