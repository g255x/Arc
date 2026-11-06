package dev.g255.arc.api.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.g255.arc.api.utils.Wrapper;
import java.awt.Color;
import java.util.function.DoubleFunction;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexFormat.DrawMode;
import org.joml.Matrix4f;

public class Render2DUtil implements Wrapper {
   public static void rect(MatrixStack stack, float x1, float y1, float x2, float y2, int color) {
      rectFilled(stack, x1, y1, x2, y2, color);
   }

   public static void arrow(MatrixStack matrixStack, float x, float y, Color color) {
      drawRectWithOutline(matrixStack, x - 1.0F, y - 1.0F, 2.0F, 2.0F, color, Color.BLACK);
   }

   public static void rectFilled(MatrixStack matrix, float x1, float y1, float x2, float y2, int color) {
      float f = (float)(color >> 24 & 255) / 255.0F;
      float g = (float)(color >> 16 & 255) / 255.0F;
      float h = (float)(color >> 8 & 255) / 255.0F;
      float j = (float)(color & 255) / 255.0F;
      if (!((double)f <= 0.01)) {
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

   public static void drawSegmentedRect(MatrixStack matrices, float x, float y, float width, float height, float segment, DoubleFunction<Integer> colorAtY) {
      if (!(width <= 0.0F) && !(height <= 0.0F) && !(segment <= 0.0F)) {
         float y2 = y + height;
         float top = Math.min(y, y2);
         float bottom = Math.max(y, y2);

         for(float yy = top; yy < bottom; yy += segment) {
            float yy2 = Math.min(yy + segment, bottom);
            int c = (Integer)colorAtY.apply((double)yy);
            drawRect(matrices, x, yy, width, yy2 - yy, c);
         }

      }
   }

   public static void drawLutRect(MatrixStack matrices, float x, float y, float width, float height, Identifier texture, int lutHeight, int alpha) {
      if (width != 0.0F && height != 0.0F && lutHeight > 0) {
         float x2 = x + width;
         float y2 = y + height;
         float left = Math.min(x, x2);
         float right = Math.max(x, x2);
         float top = Math.min(y, y2);
         float bottom = Math.max(y, y2);
         float denom = (float)Math.max(1, lutHeight - 1);
         float vTop = Math.max(0.0F, Math.min(1.0F, top / denom));
         float vBottom = Math.max(0.0F, Math.min(1.0F, bottom / denom));
         float a = (float)Math.max(0, Math.min(255, alpha)) / 255.0F;
         Matrix4f matrix = matrices.peek().getPositionMatrix();
         RenderSystem.enableBlend();
         RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
         RenderSystem.setShaderTexture(0, texture);
         BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         float u = 0.5F;
         bufferBuilder.vertex(matrix, left, bottom, 0.0F).texture(u, vBottom).color(1.0F, 1.0F, 1.0F, a);
         bufferBuilder.vertex(matrix, right, bottom, 0.0F).texture(u, vBottom).color(1.0F, 1.0F, 1.0F, a);
         bufferBuilder.vertex(matrix, right, top, 0.0F).texture(u, vTop).color(1.0F, 1.0F, 1.0F, a);
         bufferBuilder.vertex(matrix, left, top, 0.0F).texture(u, vTop).color(1.0F, 1.0F, 1.0F, a);
         BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
         RenderSystem.disableBlend();
      }
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

      for(int i = 0; i <= segments; ++i) {
         double keyCodec = (double)i * (Math.PI * 2D) / (double)segments;
         float x = (float)((double)cx + Math.cos(keyCodec) * (double)r);
         float y = (float)((double)cy + Math.sin(keyCodec) * (double)r);
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

      for(int i = 0; i <= seg; ++i) {
         double keyCodec = (-Math.PI / 2D) + (double)i * (Math.PI / 2D) / (double)seg;
         buffer.vertex(matrix, (float)((double)x2 + Math.cos(keyCodec) * (double)r), (float)((double)y1 + Math.sin(keyCodec) * (double)r), 0.0F).color(c.getRGB());
      }

      for(int var21 = 0; var21 <= seg; ++var21) {
         double keyCodec = (double)0.0F + (double)var21 * (Math.PI / 2D) / (double)seg;
         buffer.vertex(matrix, (float)((double)x2 + Math.cos(keyCodec) * (double)r), (float)((double)y2 + Math.sin(keyCodec) * (double)r), 0.0F).color(c.getRGB());
      }

      for(int var22 = 0; var22 <= seg; ++var22) {
         double keyCodec = (Math.PI / 2D) + (double)var22 * (Math.PI / 2D) / (double)seg;
         buffer.vertex(matrix, (float)((double)x1 + Math.cos(keyCodec) * (double)r), (float)((double)y2 + Math.sin(keyCodec) * (double)r), 0.0F).color(c.getRGB());
      }

      for(int var23 = 0; var23 <= seg; ++var23) {
         double keyCodec = Math.PI + (double)var23 * (Math.PI / 2D) / (double)seg;
         buffer.vertex(matrix, (float)((double)x1 + Math.cos(keyCodec) * (double)r), (float)((double)y1 + Math.sin(keyCodec) * (double)r), 0.0F).color(c.getRGB());
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
      double t = (double)(System.currentTimeMillis() % (long)(1000.0F / Math.max(0.001F, speed))) / (double)(1000.0F / Math.max(0.001F, speed));

      for(int i2 = 0; i2 <= seg; ++i2) {
         double keyCodec = (-Math.PI / 2D) + (double)i2 * (Math.PI / 2D) / (double)seg;
         float hue = (float)((keyCodec + (Math.PI * 2D)) / (Math.PI * 2D) + t) % 1.0F;
         int rgb2 = Color.HSBtoRGB(hue, 1.0F, 1.0F);
         int argb2 = alpha << 24 | rgb2 & 16777215;
         buffer.vertex(matrix, (float)((double)x2 + Math.cos(keyCodec) * (double)r), (float)((double)y1 + Math.sin(keyCodec) * (double)r), 0.0F).color(argb2);
      }

      for(int var53 = 0; var53 <= seg; ++var53) {
         double keyCodec = (double)0.0F + (double)var53 * (Math.PI / 2D) / (double)seg;
         float hue = (float)((keyCodec + (Math.PI * 2D)) / (Math.PI * 2D) + t) % 1.0F;
         int rgb2 = Color.HSBtoRGB(hue, 1.0F, 1.0F);
         int argb2 = alpha << 24 | rgb2 & 16777215;
         buffer.vertex(matrix, (float)((double)x2 + Math.cos(keyCodec) * (double)r), (float)((double)y2 + Math.sin(keyCodec) * (double)r), 0.0F).color(argb2);
      }

      for(int var54 = 0; var54 <= seg; ++var54) {
         double keyCodec = (Math.PI / 2D) + (double)var54 * (Math.PI / 2D) / (double)seg;
         float hue = (float)((keyCodec + (Math.PI * 2D)) / (Math.PI * 2D) + t) % 1.0F;
         int rgb2 = Color.HSBtoRGB(hue, 1.0F, 1.0F);
         int argb2 = alpha << 24 | rgb2 & 16777215;
         buffer.vertex(matrix, (float)((double)x1 + Math.cos(keyCodec) * (double)r), (float)((double)y2 + Math.sin(keyCodec) * (double)r), 0.0F).color(argb2);
      }

      for(int var55 = 0; var55 <= seg; ++var55) {
         double keyCodec = Math.PI + (double)var55 * (Math.PI / 2D) / (double)seg;
         float hue = (float)((keyCodec + (Math.PI * 2D)) / (Math.PI * 2D) + t) % 1.0F;
         int rgb2 = Color.HSBtoRGB(hue, 1.0F, 1.0F);
         int argb2 = alpha << 24 | rgb2 & 16777215;
         buffer.vertex(matrix, (float)((double)x1 + Math.cos(keyCodec) * (double)r), (float)((double)y1 + Math.sin(keyCodec) * (double)r), 0.0F).color(argb2);
      }

      BufferRenderer.drawWithGlobalProgram(buffer.end());
      BufferBuilder inner = Tessellator.getInstance().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
      float rIn = Math.max(0.5F, r - 1.2F);

      for(int i = 0; i <= seg; ++i) {
         double a2 = (-Math.PI / 2D) + (double)i * (Math.PI / 2D) / (double)seg;
         float hue2 = (float)((a2 + (Math.PI * 2D)) / (Math.PI * 2D) + t) % 1.0F;
         int rgb = Color.HSBtoRGB(hue2, 1.0F, 1.0F);
         int argb = alpha << 24 | rgb & 16777215;
         inner.vertex(matrix, (float)((double)x2 + Math.cos(a2) * (double)rIn), (float)((double)y1 + Math.sin(a2) * (double)rIn), 0.0F).color(argb);
      }

      for(int var38 = 0; var38 <= seg; ++var38) {
         double a3 = (double)0.0F + (double)var38 * (Math.PI / 2D) / (double)seg;
         float hue3 = (float)((a3 + (Math.PI * 2D)) / (Math.PI * 2D) + t) % 1.0F;
         int rgb = Color.HSBtoRGB(hue3, 1.0F, 1.0F);
         int argb = alpha << 24 | rgb & 16777215;
         inner.vertex(matrix, (float)((double)x2 + Math.cos(a3) * (double)rIn), (float)((double)y2 + Math.sin(a3) * (double)rIn), 0.0F).color(argb);
      }

      for(int var39 = 0; var39 <= seg; ++var39) {
         double a4 = (Math.PI / 2D) + (double)var39 * (Math.PI / 2D) / (double)seg;
         float hue4 = (float)((a4 + (Math.PI * 2D)) / (Math.PI * 2D) + t) % 1.0F;
         int rgb = Color.HSBtoRGB(hue4, 1.0F, 1.0F);
         int argb = alpha << 24 | rgb & 16777215;
         inner.vertex(matrix, (float)((double)x1 + Math.cos(a4) * (double)rIn), (float)((double)y2 + Math.sin(a4) * (double)rIn), 0.0F).color(argb);
      }

      for(int var40 = 0; var40 <= seg; ++var40) {
         double a5 = Math.PI + (double)var40 * (Math.PI / 2D) / (double)seg;
         float hue5 = (float)((a5 + (Math.PI * 2D)) / (Math.PI * 2D) + t) % 1.0F;
         int rgb = Color.HSBtoRGB(hue5, 1.0F, 1.0F);
         int argb = alpha << 24 | rgb & 16777215;
         inner.vertex(matrix, (float)((double)x1 + Math.cos(a5) * (double)rIn), (float)((double)y1 + Math.sin(a5) * (double)rIn), 0.0F).color(argb);
      }

      BufferRenderer.drawWithGlobalProgram(inner.end());
      RenderSystem.disableBlend();
   }

   public static void drawRoundedRect(MatrixStack matrices, float x, float y, float width, float height, float radius, Color c) {
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
         int seg = 48;
         float cx = x + r;
         float cy = y + r;
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
         buffer.vertex(matrix, cx, cy, 0.0F).color(c.getRGB());

         for(int i = 0; i <= seg; ++i) {
            double keyCodec = Math.PI + (double)i * (Math.PI / 2D) / (double)seg;
            buffer.vertex(matrix, (float)((double)cx + Math.cos(keyCodec) * (double)r), (float)((double)cy + Math.sin(keyCodec) * (double)r), 0.0F).color(c.getRGB());
         }

         BufferRenderer.drawWithGlobalProgram(buffer.end());
         cx = x + width - r;
         cy = y + r;
         buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
         buffer.vertex(matrix, cx, cy, 0.0F).color(c.getRGB());

         for(int var19 = 0; var19 <= seg; ++var19) {
            double keyCodec = (Math.PI * 1.5D) + (double)var19 * (Math.PI / 2D) / (double)seg;
            buffer.vertex(matrix, (float)((double)cx + Math.cos(keyCodec) * (double)r), (float)((double)cy + Math.sin(keyCodec) * (double)r), 0.0F).color(c.getRGB());
         }

         BufferRenderer.drawWithGlobalProgram(buffer.end());
         cx = x + width - r;
         cy = y + height - r;
         buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
         buffer.vertex(matrix, cx, cy, 0.0F).color(c.getRGB());

         for(int var20 = 0; var20 <= seg; ++var20) {
            double keyCodec = (double)0.0F + (double)var20 * (Math.PI / 2D) / (double)seg;
            buffer.vertex(matrix, (float)((double)cx + Math.cos(keyCodec) * (double)r), (float)((double)cy + Math.sin(keyCodec) * (double)r), 0.0F).color(c.getRGB());
         }

         BufferRenderer.drawWithGlobalProgram(buffer.end());
         cx = x + r;
         cy = y + height - r;
         buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
         buffer.vertex(matrix, cx, cy, 0.0F).color(c.getRGB());

         for(int var21 = 0; var21 <= seg; ++var21) {
            double keyCodec = (Math.PI / 2D) + (double)var21 * (Math.PI / 2D) / (double)seg;
            buffer.vertex(matrix, (float)((double)cx + Math.cos(keyCodec) * (double)r), (float)((double)cy + Math.sin(keyCodec) * (double)r), 0.0F).color(c.getRGB());
         }

         BufferRenderer.drawWithGlobalProgram(buffer.end());
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
      }
   }

   public static void drawRightRoundedRect(MatrixStack matrices, float x, float y, float width, float height, float radius, Color c) {
      if (!(width <= 0.0F) && !(height <= 0.0F)) {
         if (radius <= 0.0F) {
            drawRect(matrices, x, y, width, height, c);
         } else {
            float r = Math.min(radius, Math.min(width, height) / 2.0F);
            int capR = Math.max(0, Math.min(255, (int)Math.round((double)c.getRed() * 0.85)));
            int capG = Math.max(0, Math.min(255, (int)Math.round((double)c.getGreen() * 0.85)));
            int capB = Math.max(0, Math.min(255, (int)Math.round((double)c.getBlue() * 0.85)));
            drawCircle(matrices, x + width - r, y + height / 2.0F, r, new Color(capR, capG, capB, c.getAlpha()), 64);
            drawRect(matrices, x, y, width - r, height, c);
            if (height > 2.0F * r) {
               drawRect(matrices, x + width - r, y + r, r, height - 2.0F * r, c);
            }

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            int seg = 48;
            float cx = x + width - r;
            float cy = y + r;
            BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
            buffer.vertex(matrix, cx, cy, 0.0F).color(c.getRGB());

            for(int i = 0; i <= seg; ++i) {
               double a = (Math.PI * 1.5D) + (double)i * (Math.PI / 2D) / (double)seg;
               buffer.vertex(matrix, (float)((double)cx + Math.cos(a) * (double)r), (float)((double)cy + Math.sin(a) * (double)r), 0.0F).color(c.getRGB());
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            cx = x + width - r;
            cy = y + height - r;
            buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
            buffer.vertex(matrix, cx, cy, 0.0F).color(c.getRGB());

            for(int i = 0; i <= seg; ++i) {
               double a = (double)0.0F + (double)i * (Math.PI / 2D) / (double)seg;
               buffer.vertex(matrix, (float)((double)cx + Math.cos(a) * (double)r), (float)((double)cy + Math.sin(a) * (double)r), 0.0F).color(c.getRGB());
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
         }
      }
   }

   public static void drawDropShadow(MatrixStack matrices, float x, float y, float width, float height, float radius) {
   }
}
