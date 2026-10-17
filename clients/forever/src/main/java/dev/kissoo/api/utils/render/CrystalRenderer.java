package dev.kissoo.api.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.kissoo.api.events.impl.Render3DEvent;
import dev.kissoo.mod.modules.impl.render.Chams;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public class CrystalRenderer {

    // 立方体顶点 N=-4/16  P=+4/16
    private static final float N = -4f / 16f;
    private static final float P =  4f / 16f;

    private static final float[][] CUBE_VERTICES = {
            {N, N, N}, {P, N, N}, {P, P, N}, {N, P, N},
            {N, N, P}, {P, N, P}, {P, P, P}, {N, P, P}
    };

    private static final int[][] CUBE_FACES = {
            {0,3,2}, {0,2,1},
            {4,5,6}, {4,6,7},
            {0,4,7}, {0,7,3},
            {1,2,6}, {1,6,5},
            {3,7,6}, {3,6,2},
            {0,1,5}, {0,5,4}
    };

    private static final int[][] CUBE_EDGES = {
            {0,1}, {1,2}, {2,3}, {3,0},
            {4,5}, {5,6}, {6,7}, {7,4},
            {0,4}, {1,5}, {2,6}, {3,7}
    };

    public static void renderCrystal(Render3DEvent event, EndCrystalEntity entity, Chams module) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.gameRenderer == null) return;

        MatrixStack matrices = event.matrixStack;
        float tickDelta = event.tickDelta;

        var camera = mc.gameRenderer.getCamera();
        var camPos = camera.getPos();

        // ---- 动画参数 ----
        float age = entity.endCrystalAge + tickDelta;

        // 悬浮偏移
        float g = MathHelper.sin(age * 0.2f) / 2.0f + 0.5f;
        g = (g * g + g) * 0.4f;
        float bounceOffset = g - 1.4f;

        float rotY = age * 3.0f;

        // ---- 渲染位置（插值，相对相机）----
        double renderX = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX()) - camPos.x;
        double renderY = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY()) - camPos.y;
        double renderZ = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ()) - camPos.z;

        matrices.push();
        matrices.translate(renderX, renderY, renderZ);

        float scale = 2.0f * module.crystalScale.getValueFloat();
        matrices.scale(scale, scale, scale);
        matrices.translate(0.0f, -0.5f, 0.0f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotY));
        matrices.translate(0.0f, 1.5f + bounceOffset / 2.0f, 0.0f);

        // ---- OpenGL 状态 ----
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        float prevLineWidth = GL11.glGetFloat(GL11.GL_LINE_WIDTH);
        GL11.glLineWidth(module.lineWidth.getValueFloat());

        float sine45 = (float) Math.sin(Math.PI / 4.0);
        Quaternionf tilt = new Quaternionf().setAngleAxis((float)(Math.PI / 3.0), sine45, 0.0f, sine45);

        Color fill    = module.crystalFill.getValue();
        Color outline = module.crystalOutline.getValue();

        // ---- 第一层 ----
        matrices.multiply(tilt);
        renderCube(matrices.peek().getPositionMatrix(), fill, outline);

        // ---- 第二层 ----
        matrices.scale(0.875f, 0.875f, 0.875f);
        matrices.multiply(tilt);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotY));
        renderCube(matrices.peek().getPositionMatrix(), fill, outline);

        // ---- 第三层 ----
        matrices.scale(0.875f, 0.875f, 0.875f);
        matrices.multiply(tilt);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotY));
        renderCube(matrices.peek().getPositionMatrix(), fill, outline);

        // ---- 恢复状态 ----
        GL11.glLineWidth(prevLineWidth);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        matrices.pop();
    }

    private static void renderCube(Matrix4f matrix, Color fill, Color outline) {
        drawCubeFill(matrix, fill);
        drawCubeOutline(matrix, outline);
    }

    // ----------------------------------------------------------------
    //  填充面
    // ----------------------------------------------------------------
    private static void drawCubeFill(Matrix4f matrix, Color color) {
        float r = color.getRed()   / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue()  / 255f;
        float a = color.getAlpha() / 255f;

        Tessellator tess = Tessellator.getInstance();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        // 1.21.1: begin() 返回 BufferBuilder，draw() 在 BufferBuilder 上调用
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        for (int[] face : CUBE_FACES) {
            float[] v0 = CUBE_VERTICES[face[0]];
            float[] v1 = CUBE_VERTICES[face[1]];
            float[] v2 = CUBE_VERTICES[face[2]];
            buf.vertex(matrix, v0[0], v0[1], v0[2]).color(r, g, b, a);
            buf.vertex(matrix, v1[0], v1[1], v1[2]).color(r, g, b, a);
            buf.vertex(matrix, v2[0], v2[1], v2[2]).color(r, g, b, a);
        }

        // 1.21.1 正确提交方式
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private static void drawCubeOutline(Matrix4f matrix, Color color) {
        float r = color.getRed()   / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue()  / 255f;
        float a = color.getAlpha() / 255f;

        Tessellator tess = Tessellator.getInstance();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        for (int[] edge : CUBE_EDGES) {
            float[] v1 = CUBE_VERTICES[edge[0]];
            float[] v2 = CUBE_VERTICES[edge[1]];
            buf.vertex(matrix, v1[0], v1[1], v1[2]).color(r, g, b, a);
            buf.vertex(matrix, v2[0], v2[1], v2[2]).color(r, g, b, a);
        }

        BufferRenderer.drawWithGlobalProgram(buf.end());
    }
}