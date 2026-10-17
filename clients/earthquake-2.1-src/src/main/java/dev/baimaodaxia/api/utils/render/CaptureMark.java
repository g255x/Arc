package dev.baimaodaxia.api.utils.render;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.baimaodaxia.api.utils.Wrapper;
import java.awt.Color;
import net.minecraft.entity.Entity;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.client.render.VertexFormat.DrawMode;
import org.joml.Matrix4f;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.render.VertexFormat;

public class CaptureMark implements Wrapper {
   private static float espValue = 1.0F;
   private static float prevEspValue;
   private static float espSpeed = 1.0F;
   private static boolean flipSpeed;
   private static final Identifier capture = Identifier.of("alien", "textures/hud/elements/capture.png");
   private static float hue = 0.0F;
   private static final float[][] PASTEL_PAIRS = new float[][]{{0.902F, 0.722F, 0.902F, 1.0F, 0.714F, 0.757F}, {0.678F, 0.847F, 0.902F, 0.957F, 0.439F, 0.694F}, {0.678F, 0.847F, 0.902F, 0.565F, 0.933F, 0.565F}, {0.565F, 0.871F, 0.643F, 0.906F, 0.89F, 0.624F}, {0.678F, 0.847F, 0.902F, 1.0F, 0.98F, 0.98F}};
   private static final float[][] VIBRANT_PAIRS = new float[][]{{0.706F, 0.235F, 0.706F, 1.0F, 0.412F, 0.706F}, {0.275F, 0.51F, 0.906F, 0.957F, 0.439F, 0.694F}, {0.196F, 0.588F, 1.0F, 0.196F, 1.0F, 0.196F}, {0.0F, 0.882F, 0.353F, 0.941F, 0.902F, 0.0F}, {0.196F, 0.588F, 1.0F, 1.0F, 1.0F, 1.0F}};

   public static void render(Entity target) {
      render(target, new Color(255, 255, 255, 255));
   }

   public static void render(Entity target, Color color) {
      render(target, color, false);
   }

   public static void render(Entity target, Color color, boolean useRainbow) {
      render(target, color, useRainbow, hue);
   }

   public static void render(Entity target, Color color, float rainbowHue) {
      render(target, color, true, rainbowHue);
   }

   public static void renderGradient(Entity target, Color color, float gradientHue, boolean pastel) {
      renderGradientInternal(target, color, gradientHue, pastel);
   }

   private static void render(Entity target, Color color, boolean useRainbow, float rainbowHue) {
      if (mc.world != null && mc.player != null && mc.gameRenderer != null && target != null) {
         try {
            double tickDelta = (double)Render3DUtil.getTickDelta();
            double tPosX = interpolate(target.lastRenderX, target.getX(), tickDelta) - mc.gameRenderer.getCamera().getPos().x;
            double tPosY = interpolate(target.lastRenderY, target.getY(), tickDelta) - mc.gameRenderer.getCamera().getPos().y;
            double tPosZ = interpolate(target.lastRenderZ, target.getZ(), tickDelta) - mc.gameRenderer.getCamera().getPos().z;
            MatrixStack matrices = new MatrixStack();
            RenderSystem.disableDepthTest();
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(mc.gameRenderer.getCamera().getYaw() + 180.0F));
            matrices.translate(tPosX, tPosY + (double)(target.getEyeHeight(target.getPose()) / 2.0F), tPosZ);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(interpolateFloat(prevEspValue, espValue, tickDelta)));
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            RenderSystem.setShaderTexture(0, capture);
            matrices.translate((double)-0.75F, (double)-0.75F, -0.01);
            float alpha = (float)color.getAlpha() / 255.0F;
            if (useRainbow) {
               float h = rainbowHue % 1.0F;
               float[] color1 = getRainbowColor(h);
               float[] color2 = getRainbowColor((h + 0.25F) % 1.0F);
               float[] color3 = getRainbowColor((h + 0.5F) % 1.0F);
               float[] color4 = getRainbowColor((h + 0.75F) % 1.0F);
               Matrix4f matrix = matrices.peek().getPositionMatrix();
               RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
               BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
               buffer.vertex(matrix, 0.0F, 1.5F, 0.0F).texture(0.0F, 1.0F).color(color1[0], color1[1], color1[2], alpha);
               buffer.vertex(matrix, 1.5F, 1.5F, 0.0F).texture(1.0F, 1.0F).color(color2[0], color2[1], color2[2], alpha);
               buffer.vertex(matrix, 1.5F, 0.0F, 0.0F).texture(1.0F, 0.0F).color(color3[0], color3[1], color3[2], alpha);
               buffer.vertex(matrix, 0.0F, 0.0F, 0.0F).texture(0.0F, 0.0F).color(color4[0], color4[1], color4[2], alpha);
               BufferRenderer.drawWithGlobalProgram(buffer.end());
            } else {
               float red = (float)color.getRed() / 255.0F;
               float green = (float)color.getGreen() / 255.0F;
               float blue = (float)color.getBlue() / 255.0F;
               RenderSystem.setShaderColor(red, green, blue, alpha);
               Matrix4f matrix = matrices.peek().getPositionMatrix();
               RenderSystem.setShader(GameRenderer::getPositionTexProgram);
               BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
               buffer.vertex(matrix, 0.0F, 1.5F, 0.0F).texture(0.0F, 1.0F);
               buffer.vertex(matrix, 1.5F, 1.5F, 0.0F).texture(1.0F, 1.0F);
               buffer.vertex(matrix, 1.5F, 0.0F, 0.0F).texture(1.0F, 0.0F);
               buffer.vertex(matrix, 0.0F, 0.0F, 0.0F).texture(0.0F, 0.0F);
               BufferRenderer.drawWithGlobalProgram(buffer.end());
            }

            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         } catch (Exception e) {
            e.printStackTrace();
         }

      }
   }

   public static void tick() {
      prevEspValue = espValue;
      espValue += espSpeed;
      if (espSpeed > 25.0F) {
         flipSpeed = true;
      }

      if (espSpeed < -25.0F) {
         flipSpeed = false;
      }

      espSpeed = flipSpeed ? espSpeed - 0.5F : espSpeed + 0.5F;
      hue = (hue + 0.01F) % 1.0F;
   }

   private static double interpolate(double prev, double current, double delta) {
      return prev + (current - prev) * delta;
   }

   private static float interpolateFloat(float prev, float current, double delta) {
      return prev + (current - prev) * (float)delta;
   }

   private static void renderGradientInternal(Entity target, Color color, float gradientHue, boolean pastel) {
      if (mc.world != null && mc.player != null && mc.gameRenderer != null && target != null) {
         try {
            double tickDelta = (double)Render3DUtil.getTickDelta();
            double tPosX = interpolate(target.lastRenderX, target.getX(), tickDelta) - mc.gameRenderer.getCamera().getPos().x;
            double tPosY = interpolate(target.lastRenderY, target.getY(), tickDelta) - mc.gameRenderer.getCamera().getPos().y;
            double tPosZ = interpolate(target.lastRenderZ, target.getZ(), tickDelta) - mc.gameRenderer.getCamera().getPos().z;
            MatrixStack matrices = new MatrixStack();
            RenderSystem.disableDepthTest();
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(mc.gameRenderer.getCamera().getYaw() + 180.0F));
            matrices.translate(tPosX, tPosY + (double)(target.getEyeHeight(target.getPose()) / 2.0F), tPosZ);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(interpolateFloat(prevEspValue, espValue, tickDelta)));
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            RenderSystem.setShaderTexture(0, capture);
            matrices.translate((double)-0.75F, (double)-0.75F, -0.01);
            float alpha = (float)color.getAlpha() / 255.0F;
            float[][] pairs = pastel ? PASTEL_PAIRS : VIBRANT_PAIRS;
            float scaledHue = gradientHue * (float)pairs.length;
            int pairIndex = (int)scaledHue % pairs.length;
            int nextPairIndex = (pairIndex + 1) % pairs.length;
            float blend = scaledHue - (float)((int)scaledHue);
            float t = smoothstep(blend);
            float topR = lerp(pairs[pairIndex][0], pairs[nextPairIndex][0], t);
            float topG = lerp(pairs[pairIndex][1], pairs[nextPairIndex][1], t);
            float topB = lerp(pairs[pairIndex][2], pairs[nextPairIndex][2], t);
            float botR = lerp(pairs[pairIndex][3], pairs[nextPairIndex][3], t);
            float botG = lerp(pairs[pairIndex][4], pairs[nextPairIndex][4], t);
            float botB = lerp(pairs[pairIndex][5], pairs[nextPairIndex][5], t);
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            buffer.vertex(matrix, 0.0F, 1.5F, 0.0F).texture(0.0F, 1.0F).color(topR, topG, topB, alpha);
            buffer.vertex(matrix, 1.5F, 1.5F, 0.0F).texture(1.0F, 1.0F).color(topR, topG, topB, alpha);
            buffer.vertex(matrix, 1.5F, 0.0F, 0.0F).texture(1.0F, 0.0F).color(botR, botG, botB, alpha);
            buffer.vertex(matrix, 0.0F, 0.0F, 0.0F).texture(0.0F, 0.0F).color(botR, botG, botB, alpha);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         } catch (Exception e) {
            e.printStackTrace();
         }

      }
   }

   private static float lerp(float a, float b, float t) {
      return a + (b - a) * t;
   }

   private static float smoothstep(float t) {
      return t * t * (3.0F - 2.0F * t);
   }

   private static float[] getPastelColor(float hue, float saturation) {
      Color c = Color.getHSBColor(hue % 1.0F, saturation, 1.0F);
      return new float[]{(float)c.getRed() / 255.0F, (float)c.getGreen() / 255.0F, (float)c.getBlue() / 255.0F};
   }

   private static float[] getRainbowColor(float hue) {
      float h = hue * 6.0F;
      int i = (int)h;
      float f = h - (float)i;
      float q = 1.0F - f;
      float r;
      float g;
      float b;
      switch (i % 6) {
         case 0:
            r = 1.0F;
            g = f;
            b = 0.0F;
            break;
         case 1:
            r = q;
            g = 1.0F;
            b = 0.0F;
            break;
         case 2:
            r = 0.0F;
            g = 1.0F;
            b = f;
            break;
         case 3:
            r = 0.0F;
            g = q;
            b = 1.0F;
            break;
         case 4:
            r = f;
            g = 0.0F;
            b = 1.0F;
            break;
         case 5:
            r = 1.0F;
            g = 0.0F;
            b = q;
            break;
         default:
            r = 1.0F;
            g = 0.0F;
            b = 0.0F;
      }

      return new float[]{r, g, b};
   }
}
