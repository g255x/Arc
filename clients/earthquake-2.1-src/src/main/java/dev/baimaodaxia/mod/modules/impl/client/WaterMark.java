package dev.baimaodaxia.mod.modules.impl.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.baimaodaxia.earthquake;
import dev.baimaodaxia.api.utils.math.Timer;
import dev.baimaodaxia.api.utils.render.Render2DUtil;
import dev.baimaodaxia.api.utils.render.TextUtil;
import dev.baimaodaxia.core.impl.FontManager;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.ColorSetting;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.Identifier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexFormat.DrawMode;
import org.joml.Matrix4f;

public class WaterMark extends Module {
   public static WaterMark INSTANCE;
   private final EnumSetting<Type> type;
   public final ColorSetting color;
   private final ColorSetting textColor;
   private final SliderSetting x;
   private final SliderSetting y;
   private final SliderSetting imageWidth;
   private final SliderSetting imageHeight;
   private static final Identifier earthquake_IMAGE = Identifier.of("alien", "textures/hud/earthquake.png");
   private int step;
   private final Timer timer;
   private final float minWidth;
   private float width;
   private float height;
   private float textWidth;
   private float textHeight;
   private final Timer slideTimer;
   private float slideProgress;
   private boolean expanding;

   public WaterMark() {
      super("WaterMark", Module.Category.Client);
      this.type = this.add(new EnumSetting("Type", WaterMark.Type.Slide));
      this.color = this.add(new ColorSetting("Color", new Color(16777215, true)));
      this.textColor = this.add(new ColorSetting("TextColor", new Color(255, 255, 255)));
      this.x = this.add(new SliderSetting("X", 2, 0, 2000));
      this.y = this.add(new SliderSetting("Y", 2, 0, 2000));
      this.imageWidth = this.add(new SliderSetting("ImageWidth", (double)120.0F, (double)8.0F, (double)512.0F, (double)1.0F, () -> this.type.getValue() == WaterMark.Type.Image));
      this.imageHeight = this.add(new SliderSetting("ImageHeight", (double)50.0F, (double)8.0F, (double)512.0F, (double)1.0F, () -> this.type.getValue() == WaterMark.Type.Image));
      this.step = 0;
      this.timer = new Timer();
      this.minWidth = 2.0F;
      this.slideTimer = new Timer();
      this.slideProgress = 0.0F;
      this.expanding = true;
      this.setChinese("水印");
      INSTANCE = this;
   }

   public void onRender2D(DrawContext drawContext, float tickDelta) {
      int posX = this.x.getValueInt();
      int posY = this.y.getValueInt();
      if (this.type.getValue() == WaterMark.Type.Simple) {
         String text = "earthquake 2.1";
         TextUtil.drawString(drawContext, text, (double)posX, (double)posY, this.textColor.getValue().getRGB(), true);
      } else if (this.type.getValue() == WaterMark.Type.Slide) {
         this.drawSlideWaterMark(drawContext, posX, posY);
      } else if (this.type.getValue() == WaterMark.Type.Image) {
         this.drawImageWaterMark(drawContext, posX, posY);
      } else {
         boolean isVersion = false;
         this.textWidth = Math.max(FontManager.ui.getWidth("FPS: " + earthquake.FPS.getFps()), FontManager.ui.getWidth("earthquake 2.1"));
         this.textHeight = FontManager.ui.getFontHeight();
         String text;
         if (this.width >= this.textWidth) {
            text = "FPS: " + earthquake.FPS.getFps();
         } else {
            text = "earthquake 2.1";
            isVersion = true;
         }

         if (this.timer.passedMs((double)this.getStepDuration())) {
            this.step = (this.step + 1) % 4;
            this.timer.reset();
         }

         float progress = Math.min((float)this.timer.getMs() / (float)this.getStepDuration(), 1.0F);
         switch (this.step) {
            case 0:
               this.height = this.lerp(this.textHeight, 2.0F, progress);
               this.width = 2.0F;
               break;
            case 1:
               this.width = this.lerp(2.0F, this.textWidth, progress);
               this.height = 2.0F;
               break;
            case 2:
               this.height = this.lerp(2.0F, this.textHeight, progress);
               this.width = this.textWidth;
               break;
            case 3:
               this.width = this.lerp(this.textWidth, 2.0F, progress);
               this.height = this.textHeight;
         }

         Render2DUtil.drawRound(drawContext.getMatrices(), posX, posY, this.width, this.height, 3.0F, this.color.getValue());
         if (isVersion && text.length() > 0) {
            String firstLetter = text.substring(0, 1);
            String remaining = text.substring(1);
            float firstWidth = FontManager.ui.getWidth(firstLetter);
            FontManager.ui.drawString(drawContext.getMatrices(), firstLetter, (double)((int)((float)posX + this.textWidth / 2.0F - FontManager.ui.getWidth(text) / 2.0F)), (double)(posY + 2), this.color.getValue().getRGB());
            FontManager.ui.drawString(drawContext.getMatrices(), remaining, (double)((int)((float)posX + this.textWidth / 2.0F - FontManager.ui.getWidth(text) / 2.0F + firstWidth)), (double)(posY + 2), this.textColor.getValue().getRGB());
         } else {
            FontManager.ui.drawString(drawContext.getMatrices(), text, (double)((int)((float)posX + this.textWidth / 2.0F - FontManager.ui.getWidth(text) / 2.0F)), (double)(posY + 2), this.textColor.getValue().getRGB());
         }

      }
   }

   private void drawSlideWaterMark(DrawContext drawContext, int posX, int posY) {
      String text = "earthquake 2.1";
      this.textWidth = FontManager.ui.getWidth(text);
      this.textHeight = FontManager.ui.getFontHeight();
      float padding = 4.0F;
      float bgWidth = this.textWidth + padding * 2.0F;
      float bgHeight = this.textHeight + padding;
      long expandDuration = 800L;
      long holdDuration = 2000L;
      long shrinkDuration = 800L;
      if (this.expanding) {
         if (this.slideTimer.passedMs((double)expandDuration)) {
            this.expanding = false;
            this.slideTimer.reset();
         }

         this.slideProgress = Math.min((float)this.slideTimer.getMs() / (float)expandDuration, 1.0F);
         this.slideProgress = this.easeOutCubic(this.slideProgress);
      } else if (this.slideTimer.passedMs((double)(holdDuration + shrinkDuration))) {
         this.expanding = true;
         this.slideTimer.reset();
      } else if (this.slideTimer.passedMs((double)holdDuration)) {
         float shrinkProgress = (float)(this.slideTimer.getMs() - holdDuration) / (float)shrinkDuration;
         this.slideProgress = 1.0F - this.easeInCubic(shrinkProgress);
      } else {
         this.slideProgress = 1.0F;
      }

      float currentWidth = bgWidth * this.slideProgress;
      Render2DUtil.drawRect(drawContext.getMatrices(), (float)posX, (float)posY, currentWidth, bgHeight, this.color.getValue());
      if (this.slideProgress > 0.5F) {
         float textAlpha = (this.slideProgress - 0.5F) * 2.0F;
         int alpha = (int)((float)this.textColor.getValue().getAlpha() * textAlpha);
         Color textCol = new Color(this.textColor.getValue().getRed(), this.textColor.getValue().getGreen(), this.textColor.getValue().getBlue(), alpha);
         float textOffset = (1.0F - this.slideProgress) * bgWidth * 0.5F;
         FontManager.ui.drawString(drawContext.getMatrices(), text, (double)((int)((float)posX + padding + textOffset)), (double)((int)((float)posY + padding / 2.0F)), textCol.getRGB());
      }

   }

   private float easeOutCubic(float t) {
      return 1.0F - (float)Math.pow((double)(1.0F - t), (double)3.0F);
   }

   private void drawImageWaterMark(DrawContext drawContext, int posX, int posY) {
      float w = this.imageWidth.getValueFloat();
      float h = this.imageHeight.getValueFloat();
      this.drawTexturedQuad(drawContext.getMatrices(), earthquake_IMAGE, (float)posX, (float)posY, w, h);
   }

   private void drawTexturedQuad(MatrixStack matrices, Identifier texture, float x, float y, float width, float height) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionTexProgram);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.setShaderTexture(0, texture);
      BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      buffer.vertex(matrix, x, y + height, 0.0F).texture(0.0F, 1.0F);
      buffer.vertex(matrix, x + width, y + height, 0.0F).texture(1.0F, 1.0F);
      buffer.vertex(matrix, x + width, y, 0.0F).texture(1.0F, 0.0F);
      buffer.vertex(matrix, x, y, 0.0F).texture(0.0F, 0.0F);
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.disableBlend();
   }

   private float easeInCubic(float t) {
      return t * t * t;
   }

   private float lerp(float start, float end, float progress) {
      return start + (end - start) * progress;
   }

   private long getStepDuration() {
      long var10000;
      switch (this.step) {
         case 0 -> var10000 = 600L;
         case 1 -> var10000 = 800L;
         case 2 -> var10000 = 800L;
         case 3 -> var10000 = 600L;
         default -> var10000 = 1000L;
      }

      return var10000;
   }

   private static enum Type {
      Default,
      Simple,
      Slide,
      Image;

      // $FF: synthetic method
      private static Type[] $values() {
         return new Type[]{Default, Simple, Slide, Image};
      }
   }
}
