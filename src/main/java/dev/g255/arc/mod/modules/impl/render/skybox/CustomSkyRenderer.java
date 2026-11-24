package dev.g255.arc.mod.modules.impl.render.skybox;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.minecraft.world.World;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.Identifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.gl.VertexBuffer.Usage;
import net.minecraft.client.render.VertexFormat.DrawMode;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class CustomSkyRenderer {
   private static final Identifier NEBULA_1 = Identifier.of("arc", "textures/sky/nebula_2.png");
   private static final Identifier NEBULA_2 = Identifier.of("arc", "textures/sky/nebula_3.png");
   private static final Identifier HORIZON = Identifier.of("arc", "textures/sky/nebula_1.png");
   private static final Identifier STARS = Identifier.of("arc", "textures/sky/stars.png");
   private static final Identifier FOG = Identifier.of("arc", "textures/sky/fog.png");
   private VertexBuffer nebula1;
   private VertexBuffer nebula2;
   private VertexBuffer horizon;
   private VertexBuffer stars1;
   private VertexBuffer stars2;
   private VertexBuffer stars3;
   private VertexBuffer stars4;
   private VertexBuffer fog;
   private Vector3f axis1;
   private Vector3f axis2;
   private Vector3f axis3;
   private Vector3f axis4;
   private boolean initialised;

   private void initialise() {
      if (!this.initialised) {
         this.initStars();
         CheckedRandom class_58202 = new CheckedRandom(131L);
         this.axis1 = new Vector3f(class_58202.nextFloat(), class_58202.nextFloat(), class_58202.nextFloat());
         this.axis2 = new Vector3f(class_58202.nextFloat(), class_58202.nextFloat(), class_58202.nextFloat());
         this.axis3 = new Vector3f(class_58202.nextFloat(), class_58202.nextFloat(), class_58202.nextFloat());
         this.axis4 = new Vector3f(class_58202.nextFloat(), class_58202.nextFloat(), class_58202.nextFloat());
         this.axis1.normalize();
         this.axis2.normalize();
         this.axis3.normalize();
         this.axis4.normalize();
         this.initialised = true;
      }

   }

   public void render(World class_19372, float f, Matrix4f matrix4f, Matrix4f matrix4f2) {
      this.render(class_19372, f, (MatrixStack)null, matrix4f, matrix4f2);
   }

   private void render(World class_19372, float f, MatrixStack class_45872, Matrix4f matrix4f, Matrix4f matrix4f2) {
      float f2 = 1.0F;
      Skybox skybox = Skybox.INSTANCE;
      if (skybox != null && skybox.isOn()) {
         this.initialise();
         MatrixStack class_45873 = class_45872;
         if (class_45872 == null) {
            class_45873 = new MatrixStack();
            class_45873.multiplyPositionMatrix(matrix4f);
         }

         float f3 = ((float)class_19372.getTimeOfDay() + f) % 360000.0F * 1.7453292E-5F;
         float f4 = f3 * 2.0F;
         float f5 = f3 * 3.0F;
         BackgroundRenderer.applyFogColor();
         RenderSystem.enableBlend();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA);
         float f6 = 1.0F - BackgroundInfo.blindness;
         float f7 = f6 * 0.2F;
         float f8 = f6 * 0.6F;
         Color color = skybox.horizonColor.getValue();
         Color color2 = skybox.nebula1Color.getValue();
         Color color3 = skybox.nebula2Color.getValue();
         Color color4 = skybox.uvStars1Color.getValue();
         Color color5 = skybox.uvStars2Color.getValue();
         Color color6 = skybox.stars1Color.getValue();
         Color color7 = skybox.stars2Color.getValue();
         if (f6 > 0.0F) {
            class_45873.push();
            class_45873.multiply((new Quaternionf()).rotationXYZ(0.0F, f3, 0.0F));
            RenderSystem.setShaderTexture(0, HORIZON);
            this.renderBuffer(class_45873, matrix4f2, this.horizon, VertexFormats.POSITION_TEXTURE, (float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, 0.7F * f6);
            class_45873.pop();
            class_45873.push();
            class_45873.multiply((new Quaternionf()).rotationXYZ(0.0F, -f3, 0.0F));
            RenderSystem.setShaderTexture(0, NEBULA_1);
            this.renderBuffer(class_45873, matrix4f2, this.nebula1, VertexFormats.POSITION_TEXTURE, (float)color2.getRed() / 255.0F, (float)color2.getGreen() / 255.0F, (float)color2.getBlue() / 255.0F, f7);
            class_45873.pop();
            class_45873.push();
            class_45873.multiply((new Quaternionf()).rotationXYZ(0.0F, f4, 0.0F));
            RenderSystem.setShaderTexture(0, NEBULA_2);
            this.renderBuffer(class_45873, matrix4f2, this.nebula2, VertexFormats.POSITION_TEXTURE, (float)color3.getRed() / 255.0F, (float)color3.getGreen() / 255.0F, (float)color3.getBlue() / 255.0F, f7);
            class_45873.pop();
            if (skybox.stars.getValue()) {
               RenderSystem.setShaderTexture(0, STARS);
               class_45873.push();
               class_45873.multiply((new Quaternionf()).setAngleAxis(f3, this.axis3.x, this.axis3.y, this.axis3.z));
               this.renderBuffer(class_45873, matrix4f2, this.stars3, VertexFormats.POSITION_TEXTURE, (float)color4.getRed() / 255.0F, (float)color4.getGreen() / 255.0F, (float)color4.getBlue() / 255.0F, f8);
               class_45873.pop();
               class_45873.push();
               class_45873.multiply((new Quaternionf()).setAngleAxis(f4, this.axis4.x, this.axis4.y, this.axis4.z));
               this.renderBuffer(class_45873, matrix4f2, this.stars4, VertexFormats.POSITION_TEXTURE, (float)color5.getRed() / 255.0F, (float)color5.getGreen() / 255.0F, (float)color5.getBlue() / 255.0F, f8);
               class_45873.pop();
            }
         }

         float f9 = BackgroundInfo.fogDensity - 1.0F;
         if (f2 > 0.0F) {
            if (f9 > 1.0F) {
               f9 = 1.0F;
            }

            RenderSystem.setShaderTexture(0, FOG);
            this.renderBuffer(class_45873, matrix4f2, this.fog, VertexFormats.POSITION_TEXTURE, BackgroundInfo.fogColorRed, BackgroundInfo.fogColorGreen, BackgroundInfo.fogColorBlue, f9);
         }

         if (f6 > 0.0F && skybox.stars.getValue()) {
            class_45873.push();
            class_45873.multiply((new Quaternionf()).setAngleAxis(f5, this.axis1.x, this.axis1.y, this.axis1.z));
            this.renderBuffer(class_45873, matrix4f2, this.stars1, VertexFormats.POSITION, (float)color6.getRed() / 255.0F, (float)color6.getGreen() / 255.0F, (float)color6.getBlue() / 255.0F, f8);
            class_45873.pop();
            class_45873.push();
            class_45873.multiply((new Quaternionf()).setAngleAxis(f4, this.axis2.x, this.axis2.y, this.axis2.z));
            this.renderBuffer(class_45873, matrix4f2, this.stars2, VertexFormats.POSITION, (float)color7.getRed() / 255.0F, (float)color7.getGreen() / 255.0F, (float)color7.getBlue() / 255.0F, f8);
            class_45873.pop();
         }

         RenderSystem.defaultBlendFunc();
         RenderSystem.disableBlend();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   private void renderBuffer(MatrixStack class_45872, Matrix4f matrix4f, VertexBuffer class_2912, VertexFormat class_2932, float f, float f2, float f3, float f4) {
      RenderSystem.setShaderColor(f, f2, f3, f4);
      class_2912.bind();
      if (class_2932 == VertexFormats.POSITION) {
         class_2912.draw(class_45872.peek().getPositionMatrix(), matrix4f, GameRenderer.getPositionProgram());
      } else {
         class_2912.draw(class_45872.peek().getPositionMatrix(), matrix4f, GameRenderer.getPositionTexProgram());
      }

      VertexBuffer.unbind();
   }

   private void initStars() {
      Tessellator class_2892 = Tessellator.getInstance();
      this.stars1 = this.buildBuffer(class_2892, this.stars1, 0.1F, 0.3F, 3500, 41315L, this::makeStars);
      this.stars2 = this.buildBuffer(class_2892, this.stars2, 0.1F, 0.35F, 2000, 35151L, this::makeStars);
      this.stars3 = this.buildBuffer(class_2892, this.stars3, 0.4F, 1.2F, 1000, 61354L, this::makeUVStars);
      this.stars4 = this.buildBuffer(class_2892, this.stars4, 0.4F, 1.2F, 1000, 61355L, this::makeUVStars);
      this.nebula1 = this.buildBuffer(class_2892, this.nebula1, 40.0F, 60.0F, 30, 11515L, this::makeFarFog);
      this.nebula2 = this.buildBuffer(class_2892, this.nebula2, 40.0F, 60.0F, 10, 14151L, this::makeFarFog);
      this.horizon = this.buildBufferHorizon(class_2892, this.horizon);
      this.fog = this.buildBufferFog(class_2892, this.fog);
   }

   private VertexBuffer buildBuffer(Tessellator class_2892, VertexBuffer class_2912, float f, float f2, int n, long l, BufferFunction bufferFunction) {
      if (class_2912 != null) {
         class_2912.close();
      }

      class_2912 = new VertexBuffer(Usage.STATIC);
      BufferBuilder class_2872 = bufferFunction.make(class_2892, f, f2, n, l);
      BuiltBuffer class_98012 = class_2872.end();
      class_2912.bind();
      class_2912.upload(class_98012);
      return class_2912;
   }

   private VertexBuffer buildBufferHorizon(Tessellator GenerationStep, VertexBuffer class_2912) {
      return this.buildBuffer(GenerationStep, class_2912, 0.0F, 0.0F, 0, 0L, (class_2892, f, f2, n, l) -> this.makeCylinder(class_2892, 16, 50.0F, 100.0F));
   }

   private VertexBuffer buildBufferFog(Tessellator GenerationStep, VertexBuffer class_2912) {
      return this.buildBuffer(GenerationStep, class_2912, 0.0F, 0.0F, 0, 0L, (class_2892, f, f2, n, l) -> this.makeCylinder(class_2892, 16, 50.0F, 70.0F));
   }

   private BufferBuilder makeStars(Tessellator class_2892, float f, float f2, int n, long l) {
      CheckedRandom class_58202 = new CheckedRandom(l);
      RenderSystem.setShader(GameRenderer::getPositionProgram);
      BufferBuilder class_2872 = class_2892.begin(DrawMode.QUADS, VertexFormats.POSITION);

      for(int i = 0; i < n; ++i) {
         float f3 = class_58202.nextFloat() * 2.0F - 1.0F;
         float f4 = class_58202.nextFloat() * 2.0F - 1.0F;
         float f5 = class_58202.nextFloat() * 2.0F - 1.0F;
         float f6 = this.randRange(f, f2, class_58202);
         float f7 = f3 * f3 + f4 * f4 + f5 * f5;
         if (f7 < 1.0F && f7 > 0.001F) {
            f7 = 1.0F / (float)Math.sqrt((double)f7);
            float var34;
            float f8 = (var34 = f3 * f7) * 100.0F;
            float var35;
            float f9 = (var35 = f4 * f7) * 100.0F;
            float var36;
            float f10 = (var36 = f5 * f7) * 100.0F;
            float f11 = (float)Math.atan2((double)var34, (double)var36);
            float f12 = (float)Math.sin((double)f11);
            float f13 = (float)Math.cos((double)f11);
            f11 = (float)Math.atan2((double)((float)Math.sqrt((double)(var34 * var34 + var36 * var36))), (double)var35);
            float f14 = (float)Math.sin((double)f11);
            float f15 = (float)Math.cos((double)f11);
            f11 = class_58202.nextFloat() * (float)Math.PI * 2.0F;
            float f16 = (float)Math.sin((double)f11);
            float f17 = (float)Math.cos((double)f11);

            for(int j = 0; j < 4; ++j) {
               float f18 = (float)((j & 2) - 1) * f6;
               float f19 = (float)((j + 1 & 2) - 1) * f6;
               float f20 = f18 * f17 - f19 * f16;
               float f21 = f19 * f17 + f18 * f16;
               float f22 = f20 * f14 + 0.0F * f15;
               float f23 = 0.0F * f14 - f20 * f15;
               float f24 = f23 * f12 - f21 * f13;
               float f25 = f21 * f12 + f23 * f13;
               class_2872.vertex(f8 + f24, f9 + f22, f10 + f25);
            }
         }
      }

      return class_2872;
   }

   private BufferBuilder makeUVStars(Tessellator class_2892, float f, float f2, int n, long l) {
      CheckedRandom class_58202 = new CheckedRandom(l);
      RenderSystem.setShader(GameRenderer::getPositionTexProgram);
      BufferBuilder class_2872 = class_2892.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

      for(int i = 0; i < n; ++i) {
         float f3 = class_58202.nextFloat() * 2.0F - 1.0F;
         float f4 = class_58202.nextFloat() * 2.0F - 1.0F;
         float f5 = class_58202.nextFloat() * 2.0F - 1.0F;
         float f6 = this.randRange(f, f2, class_58202);
         float f7 = f3 * f3 + f4 * f4 + f5 * f5;
         if (f7 < 1.0F && f7 > 0.001F) {
            f7 = 1.0F / (float)Math.sqrt((double)f7);
            float var37;
            float f8 = (var37 = f3 * f7) * 100.0F;
            float var38;
            float f9 = (var38 = f4 * f7) * 100.0F;
            float var39;
            float f10 = (var39 = f5 * f7) * 100.0F;
            float f11 = (float)Math.atan2((double)var37, (double)var39);
            float f12 = (float)Math.sin((double)f11);
            float f13 = (float)Math.cos((double)f11);
            f11 = (float)Math.atan2((double)((float)Math.sqrt((double)(var37 * var37 + var39 * var39))), (double)var38);
            float f14 = (float)Math.sin((double)f11);
            float f15 = (float)Math.cos((double)f11);
            f11 = class_58202.nextFloat() * (float)Math.PI * 2.0F;
            float f16 = (float)Math.sin((double)f11);
            float f17 = (float)Math.cos((double)f11);
            float f18 = (float)class_58202.nextInt(4) / 4.0F;

            for(int j = 0; j < 4; ++j) {
               float f19 = (float)((j & 2) - 1) * f6;
               float f20 = (float)((j + 1 & 2) - 1) * f6;
               float f21 = f19 * f17 - f20 * f16;
               float f22 = f20 * f17 + f19 * f16;
               float f23 = f21 * f14 + 0.0F * f15;
               float f24 = 0.0F * f14 - f21 * f15;
               float f25 = f24 * f12 - f22 * f13;
               float f26 = f22 * f12 + f24 * f13;
               float f27 = (float)(j >> 1 & 1);
               float f28 = (float)(j + 1 >> 1 & 1) / 4.0F + f18;
               class_2872.vertex(f8 + f25, f9 + f23, f10 + f26).texture(f27, f28);
            }
         }
      }

      return class_2872;
   }

   private BufferBuilder makeFarFog(Tessellator class_2892, float f, float f2, int n, long l) {
      CheckedRandom class_58202 = new CheckedRandom(l);
      RenderSystem.setShader(GameRenderer::getPositionTexProgram);
      BufferBuilder class_2872 = class_2892.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

      for(int i = 0; i < n; ++i) {
         float f3 = class_58202.nextFloat() * 2.0F - 1.0F;
         float f4 = class_58202.nextFloat() - 0.5F;
         float f5 = class_58202.nextFloat() * 2.0F - 1.0F;
         float f6 = this.randRange(f, f2, class_58202);
         float f7 = f3 * f3 + f4 * f4 + f5 * f5;
         float f8 = 2.0F;
         if (f7 < 1.0F && f7 > 0.001F) {
            f7 = f8 / (float)Math.sqrt((double)f7);
            f6 *= f8;
            float var37;
            float f9 = (var37 = f3 * f7) * 100.0F;
            float var38;
            float f10 = (var38 = f4 * f7) * 100.0F;
            float var39;
            float f11 = (var39 = f5 * f7) * 100.0F;
            float f12 = (float)Math.atan2((double)var37, (double)var39);
            float f13 = (float)Math.sin((double)f12);
            float f14 = (float)Math.cos((double)f12);
            f12 = (float)Math.atan2((double)((float)Math.sqrt((double)(var37 * var37 + var39 * var39))), (double)var38);
            float f15 = (float)Math.sin((double)f12);
            float f16 = (float)Math.cos((double)f12);
            f12 = class_58202.nextFloat() * (float)Math.PI * 2.0F;
            float f17 = (float)Math.sin((double)f12);
            float f18 = (float)Math.cos((double)f12);

            for(int j = 0; j < 4; ++j) {
               float f19 = (float)((j & 2) - 1) * f6;
               float f20 = (float)((j + 1 & 2) - 1) * f6;
               float f21 = f19 * f18 - f20 * f17;
               float f22 = f20 * f18 + f19 * f17;
               float f23 = f21 * f15 + 0.0F * f16;
               float f24 = 0.0F * f15 - f21 * f16;
               float f25 = f24 * f13 - f22 * f14;
               float f26 = f22 * f13 + f24 * f14;
               float f27 = (float)(j >> 1 & 1);
               float f28 = (float)(j + 1 >> 1 & 1);
               class_2872.vertex(f9 + f25, f10 + f23, f11 + f26).texture(f27, f28);
            }
         }
      }

      return class_2872;
   }

   private BufferBuilder makeCylinder(Tessellator class_2892, int n, float f, float f2) {
      RenderSystem.setShader(GameRenderer::getPositionTexProgram);
      BufferBuilder class_2872 = class_2892.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

      for(int i = 0; i < n; ++i) {
         float f3 = (float)i * (float)Math.PI * 2.0F / (float)n;
         float f4 = (float)(i + 1) * (float)Math.PI * 2.0F / (float)n;
         float f5 = (float)Math.sin((double)f3) * f2;
         float f6 = (float)Math.cos((double)f3) * f2;
         float f7 = (float)Math.sin((double)f4) * f2;
         float f8 = (float)Math.cos((double)f4) * f2;
         float f9 = (float)i / (float)n;
         float f10 = (float)(i + 1) / (float)n;
         class_2872.vertex(f5, -f, f6).texture(f9, 0.0F);
         class_2872.vertex(f5, f, f6).texture(f9, 1.0F);
         class_2872.vertex(f7, f, f8).texture(f10, 1.0F);
         class_2872.vertex(f7, -f, f8).texture(f10, 0.0F);
      }

      return class_2872;
   }

   private float randRange(float f, float f2, CheckedRandom class_58202) {
      return f + class_58202.nextFloat() * (f2 - f);
   }

   @FunctionalInterface
   interface BufferFunction {
      BufferBuilder make(Tessellator var1, float var2, float var3, int var4, long var5);
   }
}
