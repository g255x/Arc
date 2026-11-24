package dev.g255.arc.api.utils.render;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.g255.arc.api.utils.Wrapper;
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

public class CaptureMark implements Wrapper {
   private static float espValue = 1.0F;
   private static float prevEspValue;
   private static float espSpeed = 1.0F;
   private static boolean flipSpeed;
   private static final Identifier capture = Identifier.of("arc", "textures/hud/elements/capture.png");
   private static float hue = 0.0F;

   public static void render(Entity target) {
      render(target, new Color(255, 255, 255, 255));
   }

   public static void render(Entity target, Color color) {
      render(target, color, false);
   }

   public static void render(Entity target, Color color, boolean useRainbow) {
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
            RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
            RenderSystem.setShaderTexture(0, capture);
            matrices.translate((double)-0.75F, (double)-0.75F, -0.01);
            float alpha = (float)color.getAlpha() / 255.0F;
            if (useRainbow) {
               float[] color1 = getRainbowColor(hue);
               float[] color2 = getRainbowColor((hue + 0.25F) % 1.0F);
               float[] color3 = getRainbowColor((hue + 0.5F) % 1.0F);
               float[] color4 = getRainbowColor((hue + 0.75F) % 1.0F);
               Matrix4f matrix = matrices.peek().getPositionMatrix();
               RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
               BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
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
               BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
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
