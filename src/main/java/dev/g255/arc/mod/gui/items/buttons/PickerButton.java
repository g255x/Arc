package dev.g255.arc.mod.gui.items.buttons;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.g255.arc.api.utils.math.Animation;
import dev.g255.arc.api.utils.math.Easing;
import dev.g255.arc.api.utils.render.ColorUtil;
import dev.g255.arc.api.utils.render.Render2DUtil;
import dev.g255.arc.core.impl.CommandManager;
import dev.g255.arc.mod.modules.impl.client.ClickGui;
import dev.g255.arc.mod.modules.settings.ChineseTranslator;
import dev.g255.arc.mod.modules.settings.impl.ColorSetting;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Objects;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexFormat.DrawMode;
import org.lwjgl.glfw.GLFW;

public class PickerButton extends Button {
   static MatrixStack matrixStack;
   private final ColorSetting setting;
   boolean pickingColor;
   boolean pickingHue;
   boolean pickingAlpha;
   boolean open;
   private final Animation openAnimation = new Animation();
   private final Animation pickerCursorXAnimation = new Animation();
   private final Animation pickerCursorYAnimation = new Animation();
   private final Animation hueCursorAnimation = new Animation();
   private final Animation alphaCursorAnimation = new Animation();
   private final Animation toggleAnimation = new Animation();
   private final Animation hoverAnimation = new Animation();
   private final Animation pickingScaleAnimation = new Animation();
   private boolean indicatorsInitialized;
   float[] hsb = new float[]{1.0F, 1.0F, 1.0F, 1.0F};

   public PickerButton(ColorSetting setting) {
      super(setting.getDisplayName());
      this.setting = setting;
      double initial = this.getState() ? (double)1.0F : (double)0.0F;
      this.toggleAnimation.from = initial;
      this.toggleAnimation.to = initial;
   }

   public static boolean mouseOver(int minX, int minY, int maxX, int maxY, int mX, int mY) {
      return mX >= minX && mY >= minY && mX <= maxX && mY <= maxY;
   }

   public static Color getColor(Color color, float alpha) {
      float red = (float)color.getRed() / 255.0F;
      float green = (float)color.getGreen() / 255.0F;
      float blue = (float)color.getBlue() / 255.0F;
      return new Color(red, green, blue, alpha);
   }

   public static void drawPickerBase(int pickerX, int pickerY, int pickerWidth, int pickerHeight, float red, float green, float blue, float alpha) {
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)pickerX, (float)pickerY, 0.0F).color(1.0F, 1.0F, 1.0F, 1.0F);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)pickerX, (float)(pickerY + pickerHeight), 0.0F).color(1.0F, 1.0F, 1.0F, 1.0F);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)(pickerX + pickerWidth), (float)(pickerY + pickerHeight), 0.0F).color(red, green, blue, 1.0F);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)(pickerX + pickerWidth), (float)pickerY, 0.0F).color(red, green, blue, 1.0F);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)pickerX, (float)pickerY, 0.0F).color(0.0F, 0.0F, 0.0F, 0.0F);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)pickerX, (float)(pickerY + pickerHeight), 0.0F).color(0.0F, 0.0F, 0.0F, 1.0F);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)(pickerX + pickerWidth), (float)(pickerY + pickerHeight), 0.0F).color(0.0F, 0.0F, 0.0F, 1.0F);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)(pickerX + pickerWidth), (float)pickerY, 0.0F).color(0.0F, 0.0F, 0.0F, 0.0F);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
   }

   public static void drawLeftGradientRect(int left, int top, int right, int bottom, int startColor, int endColor) {
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)right, (float)top, 0.0F).color((float)(endColor >> 24 & 255) / 255.0F, (float)(endColor >> 16 & 255) / 255.0F, (float)(endColor >> 8 & 255) / 255.0F, (float)(endColor >> 24 & 255) / 255.0F);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)left, (float)top, 0.0F).color((float)(startColor >> 16 & 255) / 255.0F, (float)(startColor >> 8 & 255) / 255.0F, (float)(startColor & 255) / 255.0F, (float)(startColor >> 24 & 255) / 255.0F);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)left, (float)bottom, 0.0F).color((float)(startColor >> 16 & 255) / 255.0F, (float)(startColor >> 8 & 255) / 255.0F, (float)(startColor & 255) / 255.0F, (float)(startColor >> 24 & 255) / 255.0F);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)right, (float)bottom, 0.0F).color((float)(endColor >> 24 & 255) / 255.0F, (float)(endColor >> 16 & 255) / 255.0F, (float)(endColor >> 8 & 255) / 255.0F, (float)(endColor >> 24 & 255) / 255.0F);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
   }

   public static void gradient(int minX, int minY, int maxX, int maxY, int startColor, int endColor, boolean left) {
      float startA = (float)(startColor >> 24 & 255) / 255.0F;
      float startR = (float)(startColor >> 16 & 255) / 255.0F;
      float startG = (float)(startColor >> 8 & 255) / 255.0F;
      float startB = (float)(startColor & 255) / 255.0F;
      float endA = (float)(endColor >> 24 & 255) / 255.0F;
      float endR = (float)(endColor >> 16 & 255) / 255.0F;
      float endG = (float)(endColor >> 8 & 255) / 255.0F;
      float endB = (float)(endColor & 255) / 255.0F;
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)minX, (float)minY, 0.0F).color(startR, startG, startB, startA);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)minX, (float)maxY, 0.0F).color(startR, startG, startB, startA);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)maxX, (float)maxY, 0.0F).color(endR, endG, endB, endA);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)maxX, (float)minY, 0.0F).color(endR, endG, endB, endA);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
   }

   public static int gradientColor(int color, int percentage) {
      int r = ((color & 16711680) >> 16) * (100 + percentage) / 100;
      int g = ((color & '\uff00') >> 8) * (100 + percentage) / 100;
      int elementCodec = (color & 255) * (100 + percentage) / 100;
      return (new Color(r, g, elementCodec)).hashCode();
   }

   public static void drawGradientRect(float left, float top, float right, float bottom, int startColor, int endColor, boolean hovered) {
      if (hovered) {
         startColor = gradientColor(startColor, -20);
         endColor = gradientColor(endColor, -20);
      }

      float c = (float)(startColor >> 24 & 255) / 255.0F;
      float c1 = (float)(startColor >> 16 & 255) / 255.0F;
      float c2 = (float)(startColor >> 8 & 255) / 255.0F;
      float c3 = (float)(startColor & 255) / 255.0F;
      float c4 = (float)(endColor >> 24 & 255) / 255.0F;
      float c5 = (float)(endColor >> 16 & 255) / 255.0F;
      float c6 = (float)(endColor >> 8 & 255) / 255.0F;
      float c7 = (float)(endColor & 255) / 255.0F;
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), right, top, 0.0F).color(c1, c2, c3, c);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), left, top, 0.0F).color(c1, c2, c3, c);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), left, bottom, 0.0F).color(c5, c6, c7, c4);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), right, bottom, 0.0F).color(c5, c6, c7, c4);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
   }

   public static String readClipboard() {
      try {
         return (String)Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
      } catch (IOException | UnsupportedFlavorException var1) {
         return null;
      }
   }

   public static void drawOutlineRect(double left, double top, double right, double bottom, Color color, float lineWidth) {
      if (left < right) {
         double i = left;
         left = right;
         right = i;
      }

      if (top < bottom) {
         double j = top;
         top = bottom;
         bottom = j;
      }

      float f3 = (float)(color.getRGB() >> 24 & 255) / 255.0F;
      float f = (float)(color.getRGB() >> 16 & 255) / 255.0F;
      float f1 = (float)(color.getRGB() >> 8 & 255) / 255.0F;
      float f2 = (float)(color.getRGB() & 255) / 255.0F;
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)left, (float)bottom, 0.0F).color(f, f1, f2, f3);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)right, (float)bottom, 0.0F).color(f, f1, f2, f3);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)right, (float)top, 0.0F).color(f, f1, f2, f3);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)left, (float)top, 0.0F).color(f, f1, f2, f3);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
   }

   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      matrixStack = context.getMatrices();
      boolean hovered = this.isHovering(mouseX, mouseY);
      double hoverProgress = this.hoverAnimation.get(hovered ? (double)1.0F : (double)0.0F, 130L, Easing.CubicInOut);
      double toggleProgress = this.toggleAnimation.get(this.getState() ? (double)1.0F : (double)0.0F, 160L, Easing.CubicInOut);
      double baseDelay = this.getColorDelay();
      int baseA = ClickGui.getInstance().alpha.getValueInt();
      int hoverA = ClickGui.getInstance().hoverAlpha.getValueInt();
      int accentA = Math.max(0, Math.min(255, (int)Math.round((double)baseA + (double)(hoverA - baseA) * hoverProgress)));
      Color defaultColor = ClickGui.getInstance().defaultColor.getValue();
      Color hoverColor = ClickGui.getInstance().hoverColor.getValue();
      Color unpressedFill = ColorUtil.fadeColor(new Color(defaultColor.getRed(), defaultColor.getGreen(), defaultColor.getBlue(), defaultColor.getAlpha()), hoverColor, hoverProgress);
      Color color = ClickGui.getInstance().getColor(baseDelay);
      float w = (float)this.width + 7.0F;
      float h = (float)this.height - 0.5F;
      float wipe = w * (float)Math.max((double)0.0F, Math.min((double)1.0F, toggleProgress));
      if (ClickGui.getInstance().colorMode.getValue() == ClickGui.ColorMode.Spectrum) {
         Render2DUtil.rect(context.getMatrices(), this.x, this.y, this.x + w, this.y + h, unpressedFill.getRGB());
         if (wipe > 0.01F) {
            Render2DUtil.drawLutRect(context.getMatrices(), this.x, this.y, wipe, h, ClickGui.getInstance().getSpectrumLutId(), ClickGui.getInstance().getSpectrumLutHeight(), accentA);
         }
      } else {
         Color pressedFill = new Color(color.getRed(), color.getGreen(), color.getBlue(), accentA);
         if (wipe <= 0.01F) {
            Render2DUtil.rect(context.getMatrices(), this.x, this.y, this.x + w, this.y + h, unpressedFill.getRGB());
         } else if (wipe >= w - 0.01F) {
            Render2DUtil.rect(context.getMatrices(), this.x, this.y, this.x + w, this.y + h, pressedFill.getRGB());
         } else {
            Render2DUtil.rect(context.getMatrices(), this.x, this.y, this.x + wipe, this.y + h, pressedFill.getRGB());
            Render2DUtil.rect(context.getMatrices(), this.x + wipe, this.y, this.x + w, this.y + h, unpressedFill.getRGB());
         }
      }

      Render2DUtil.rect(matrixStack, this.x - 1.5F + (float)this.width + 0.6F - 0.5F, this.y + 4.0F, this.x + (float)this.width + 7.0F - 2.5F, this.y + (float)this.height - 5.0F, ColorUtil.injectAlpha(this.setting.getValue(), 255).getRGB());
      float textY = this.getCenteredTextY(this.y, (float)this.height - 0.5F);
      if (this.isHovering(mouseX, mouseY) && InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340)) {
         this.drawString(ChineseTranslator.resetHint(false), (double)(this.x + 2.3F), (double)textY, enableTextColor);
      } else {
         this.drawString(this.getName(), (double)(this.x + 2.3F), (double)textY, enableTextColor);
      }

      double openProgress = this.openAnimation.get(this.open ? (double)1.0F : (double)0.0F, 200L, Easing.CubicInOut);
      if (openProgress > 0.01) {
         int panelHeight = (int)((float)this.width + 7.4F);
         int scissorX1 = (int)this.x - 1;
         int scissorY1 = (int)this.y - 1;
         int scissorX2 = (int)(this.x + (float)this.width + 7.0F) + 1;
         int scissorY2 = (int)Math.round((double)(this.y + (float)this.height) + (double)((float)panelHeight + 41.0F) * openProgress) + 1;
         context.enableScissor(scissorX1, scissorY1, scissorX2, scissorY2);
         int pickerX = (int)this.x;
         int pickerY = (int)(this.y + (float)this.height);
         int pickerWidth = (int)((float)this.width + 7.4F);
         int alphaSliderY = pickerY + panelHeight + 4;
         int hueSliderY = alphaSliderY + 8;
         this.drawPicker(this.setting, pickerX, pickerY, pickerX, hueSliderY, pickerX, alphaSliderY, mouseX, mouseY);
         float actionsRowTopY = (float)pickerY + (float)panelHeight + 18.0F;
         float actionsTextY = this.getCenteredTextY(actionsRowTopY, (float)this.getFontHeight());
         this.drawString(ChineseTranslator.copyLabel(), (double)(this.x + 2.3F), (double)actionsTextY, this.isInsideCopy(mouseX, mouseY) ? enableTextColor : defaultTextColor);
         this.drawString(ChineseTranslator.pasteLabel(), (double)(this.x + (float)this.width - 2.3F - (float)this.getWidth(ChineseTranslator.pasteLabel()) + 11.7F - 4.6F), (double)actionsTextY, this.isInsidePaste(mouseX, mouseY) ? enableTextColor : defaultTextColor);
         float syncRowTopY = actionsRowTopY + (float)this.getFontHeight();
         float syncTextY = this.getCenteredTextY(syncRowTopY, (float)this.getFontHeight());
         this.drawString(ChineseTranslator.rainbowLabel(), (double)(this.x + 2.3F), (double)syncTextY, this.setting.rainbow ? ColorUtil.injectAlpha(color, 255).getRGB() : (this.isInsideRainbow(mouseX, mouseY) ? enableTextColor : defaultTextColor));
         context.disableScissor();
      }

   }

   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (mouseButton == 0 && this.isHovering(mouseX, mouseY) && InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340)) {
         this.pickingColor = false;
         this.pickingHue = false;
         this.pickingAlpha = false;
         this.setting.setValue(this.setting.getDefaultValue());
         this.setting.rainbow = this.setting.getDefaultRainbow();
         if (this.setting.injectBoolean) {
            this.setting.booleanValue = this.setting.getDefaultBooleanValue();
         }

         sound();
      } else {
         if (mouseButton == 0) {
            int pickerX = (int)this.x;
            int pickerY = (int)(this.y + (float)this.height);
            int pickerWidth = (int)((float)this.width + 7.4F);
            int pickerHeight = 78;
            int hueSliderWidth = pickerWidth + 3;
            int hueSliderHeight = 7;
            int alphaSliderHeight = 7;
            int alphaSliderY = pickerY + pickerHeight + 4;
            int hueSliderY = alphaSliderY + alphaSliderHeight + 1;
            if (mouseOver(pickerX, pickerY, pickerX + pickerWidth, pickerY + pickerHeight, mouseX, mouseY)) {
               this.pickingColor = true;
            }

            if (mouseOver(pickerX, hueSliderY, pickerX + hueSliderWidth, hueSliderY + hueSliderHeight, mouseX, mouseY)) {
               this.pickingHue = true;
            }

            if (mouseOver(pickerX, alphaSliderY, pickerX + pickerWidth, alphaSliderY + alphaSliderHeight, mouseX, mouseY)) {
               this.pickingAlpha = true;
            }
         }

         if (this.isHovering(mouseX, mouseY)) {
            if (mouseButton == 1) {
               sound();
               this.open = !this.open;
            } else if (mouseButton == 0 && this.setting.injectBoolean) {
               sound();
               boolean hex = this.setting.booleanValue = !this.setting.booleanValue;
            }
         }

         if (mouseButton == 0 && this.isInsideRainbow(mouseX, mouseY) && this.open) {
            boolean hex = this.setting.rainbow = !this.setting.rainbow;
         }

         if (mouseButton == 0 && this.isInsideCopy(mouseX, mouseY) && this.open) {
            sound();
            String hex = String.format("#%02x%02x%02x%02x", this.setting.getValue().getAlpha(), this.setting.getValue().getRed(), this.setting.getValue().getGreen(), this.setting.getValue().getBlue());
            StringSelection selection = new StringSelection(hex);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
            CommandManager.sendMessage(ChineseTranslator.copiedMsg());
         }

         if (mouseButton == 0 && this.isInsidePaste(mouseX, mouseY) && this.open) {
            try {
               if (readClipboard() != null) {
                  if (((String)Objects.requireNonNull(readClipboard())).startsWith("#")) {
                     String hex = (String)Objects.requireNonNull(readClipboard());
                     int keyCodec = Integer.valueOf(hex.substring(1, 3), 16);
                     int r = Integer.valueOf(hex.substring(3, 5), 16);
                     int g = Integer.valueOf(hex.substring(5, 7), 16);
                     int elementCodec = Integer.valueOf(hex.substring(7, 9), 16);
                     this.setting.setValue(new Color(r, g, elementCodec, keyCodec));
                  } else {
                     String[] color = readClipboard().split(",");
                     this.setting.setValue(new Color(Integer.parseInt(color[0]), Integer.parseInt(color[1]), Integer.parseInt(color[2])));
                  }
               }
            } catch (Exception e) {
               e.printStackTrace();
               CommandManager.sendMessage(ChineseTranslator.badColorMsg());
               this.setting.setValue(-1);
            }
         }

      }
   }

   public boolean getState() {
      return this.setting.booleanValue;
   }

   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   public void mouseReleased(int mouseX, int mouseY, int releaseButton) {
      this.pickingAlpha = false;
      this.pickingHue = false;
      this.pickingColor = false;
   }

   public boolean isInsideCopy(int mouseX, int mouseY) {
      float actionsRowTopY = this.y + (float)this.height + (float)this.width + 7.4F + 18.0F;
      int textY = (int)this.getCenteredTextY(actionsRowTopY, (float)this.getFontHeight());
      return mouseOver((int)((float)((int)this.x) + 2.3F), textY, (int)((float)((int)this.x) + 2.3F) + this.getWidth(ChineseTranslator.copyLabel()), textY + this.getFontHeight(), mouseX, mouseY);
   }

   public boolean isInsideRainbow(int mouseX, int mouseY) {
      float actionsRowTopY = this.y + (float)this.height + (float)this.width + 7.4F + 18.0F;
      float syncRowTopY = actionsRowTopY + (float)this.getFontHeight();
      int textY = (int)this.getCenteredTextY(syncRowTopY, (float)this.getFontHeight());
      return mouseOver((int)((float)((int)this.x) + 2.3F), textY, (int)((float)((int)this.x) + 2.3F) + this.getWidth(ChineseTranslator.rainbowLabel()), textY + this.getFontHeight(), mouseX, mouseY);
   }

   public boolean isInsidePaste(int mouseX, int mouseY) {
      float actionsRowTopY = this.y + (float)this.height + (float)this.width + 7.4F + 18.0F;
      int textY = (int)this.getCenteredTextY(actionsRowTopY, (float)this.getFontHeight());
      int x = (int)(this.x + (float)this.width - 2.3F - (float)this.getWidth(ChineseTranslator.pasteLabel()) + 11.7F - 4.6F);
      return mouseOver(x, textY, x + this.getWidth(ChineseTranslator.pasteLabel()), textY + this.getFontHeight(), mouseX, mouseY);
   }

   public int getHeight() {
      return this.open ? super.getHeight() + (int)((float)this.width + 7.4F) + 41 : super.getHeight();
   }

   public void drawPicker(ColorSetting setting, int pickerX, int pickerY, int hueSliderX, int hueSliderY, int alphaSliderX, int alphaSliderY, int mouseX, int mouseY) {
      int pickerWidth = (int)((float)this.width + 7.4F);
      int pickerHeight = (int)((float)this.width + 7.4F);
      int hueSliderHeight = 7;
      int alphaSliderHeight = 7;
      if (this.pickingColor && (GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) != 1 || !mouseOver(pickerX, pickerY, pickerX + pickerWidth, pickerY + pickerHeight, mouseX, mouseY))) {
         this.pickingColor = false;
      }

      if (!this.pickingColor) {
         this.hsb = Color.RGBtoHSB(setting.getValue().getRed(), setting.getValue().getGreen(), setting.getValue().getBlue(), (float[])null);
      }

      float[] color = new float[]{this.hsb[0], this.hsb[1], this.hsb[2], (float)setting.getValue().getAlpha() / 255.0F};
      if (this.pickingHue && (GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) != 1 || !mouseOver(hueSliderX, hueSliderY, hueSliderX + pickerWidth, hueSliderY + hueSliderHeight, mouseX, mouseY))) {
         this.pickingHue = false;
      }

      if (this.pickingAlpha && (GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) != 1 || !mouseOver(alphaSliderX, alphaSliderY, alphaSliderX + pickerWidth, alphaSliderY + alphaSliderHeight, mouseX, mouseY))) {
         this.pickingAlpha = false;
      }

      if (this.pickingHue) {
         float restrictedX = (float)Math.min(Math.max(hueSliderX, mouseX), hueSliderX + pickerWidth);
         color[0] = (restrictedX - (float)hueSliderX) / (float)pickerWidth;
      }

      if (this.pickingAlpha) {
         float restrictedX = (float)Math.min(Math.max(alphaSliderX, mouseX), alphaSliderX + pickerWidth);
         color[3] = 1.0F - (restrictedX - (float)alphaSliderX) / (float)pickerWidth;
      }

      if (this.pickingColor) {
         float restrictedX = (float)Math.min(Math.max(pickerX, mouseX), pickerX + pickerWidth);
         float restrictedY = (float)Math.min(Math.max(pickerY, mouseY), pickerY + pickerHeight);
         color[1] = (restrictedX - (float)pickerX) / (float)pickerWidth;
         color[2] = 1.0F - (restrictedY - (float)pickerY) / (float)pickerHeight;
      }

      int selectedColor = Color.HSBtoRGB(color[0], 1.0F, 1.0F);
      float selectedRed = (float)(selectedColor >> 16 & 255) / 255.0F;
      float selectedGreen = (float)(selectedColor >> 8 & 255) / 255.0F;
      float selectedBlue = (float)(selectedColor & 255) / 255.0F;
      drawPickerBase(pickerX, pickerY, pickerWidth, pickerHeight, selectedRed, selectedGreen, selectedBlue, color[3]);
      drawOutlineRect((double)pickerX - (double)1.0F, (double)pickerY - (double)1.0F, (double)(pickerX + pickerWidth) + (double)1.0F, (double)(pickerY + pickerHeight) + (double)1.0F, new Color(0, 0, 0, 160), 1.0F);
      int targetCursorX = (int)((float)pickerX + color[1] * (float)pickerWidth);
      int targetCursorY = (int)((float)(pickerY + pickerHeight) - color[2] * (float)pickerHeight);
      int targetHueCursor = (int)((float)hueSliderX + (float)pickerWidth * color[0]);
      int targetAlphaCursor = (int)((float)(alphaSliderX + pickerWidth) - (float)pickerWidth * color[3]);
      if (!this.indicatorsInitialized) {
         this.pickerCursorXAnimation.from = (double)targetCursorX;
         this.pickerCursorXAnimation.to = (double)targetCursorX;
         this.pickerCursorYAnimation.from = (double)targetCursorY;
         this.pickerCursorYAnimation.to = (double)targetCursorY;
         this.hueCursorAnimation.from = (double)targetHueCursor;
         this.hueCursorAnimation.to = (double)targetHueCursor;
         this.alphaCursorAnimation.from = (double)targetAlphaCursor;
         this.alphaCursorAnimation.to = (double)targetAlphaCursor;
         this.indicatorsInitialized = true;
      }

      long pickerCursorLength = this.pickingColor ? 40L : 120L;
      int cursorX = (int)Math.round(this.pickerCursorXAnimation.get((double)targetCursorX, pickerCursorLength, Easing.SineOut));
      int cursorY = (int)Math.round(this.pickerCursorYAnimation.get((double)targetCursorY, pickerCursorLength, Easing.SineOut));
      this.drawHueSlider(hueSliderX, hueSliderY, pickerWidth, hueSliderHeight, color[0], this.pickingHue);
      setting.setValue(getColor(new Color(Color.HSBtoRGB(color[0], color[1], color[2])), color[3]));
      double pickingScale = this.pickingScaleAnimation.get(this.pickingColor ? (double)1.0F : (double)0.0F, 120L, Easing.CubicInOut);
      float cursorScale = 1.0F + 0.8F * (float)pickingScale;
      matrixStack.push();
      matrixStack.translate((float)cursorX, (float)cursorY, 0.0F);
      matrixStack.scale(cursorScale, cursorScale, 1.0F);
      matrixStack.translate((float)(-cursorX), (float)(-cursorY), 0.0F);
      Render2DUtil.arrow(matrixStack, (float)cursorX, (float)cursorY, setting.getValue());
      matrixStack.pop();
      this.drawAlphaSlider(alphaSliderX, alphaSliderY, pickerWidth - 1, alphaSliderHeight, selectedRed, selectedGreen, selectedBlue, color[3], this.pickingAlpha);
   }

   public void drawHueSlider(int x, int y, int width, int height, float hue, boolean snap) {
      int step = 0;
      if (height > width) {
         Render2DUtil.rect(matrixStack, (float)x, (float)y, (float)(x + width), (float)(y + 4), -65536);
         y += 4;

         for(int colorIndex = 0; colorIndex < 6; ++colorIndex) {
            int previousStep = Color.HSBtoRGB((float)step / 6.0F, 1.0F, 1.0F);
            int nextStep = Color.HSBtoRGB((float)(step + 1) / 6.0F, 1.0F, 1.0F);
            drawGradientRect((float)x, (float)y + (float)step * ((float)height / 6.0F), (float)(x + width), (float)y + (float)(step + 1) * ((float)height / 6.0F), previousStep, nextStep, false);
            ++step;
         }

         int targetSliderMinY = (int)((float)y + (float)height * hue) - 4;
         long cursorLength = snap ? 40L : 120L;
         int sliderMinY = (int)Math.round(this.hueCursorAnimation.get((double)targetSliderMinY, cursorLength, Easing.SineOut));
         Render2DUtil.rect(matrixStack, (float)x, (float)(sliderMinY - 1), (float)(x + width), (float)(sliderMinY + 1), -1);
         drawOutlineRect((double)x, (double)(sliderMinY - 1), (double)(x + width), (double)(sliderMinY + 1), Color.BLACK, 1.0F);
      } else {
         for(int colorIndex = 0; colorIndex < 6; ++colorIndex) {
            int previousStep = Color.HSBtoRGB((float)step / 6.0F, 1.0F, 1.0F);
            int nextStep = Color.HSBtoRGB((float)(step + 1) / 6.0F, 1.0F, 1.0F);
            gradient(x + step * (width / 6), y, x + (step + 1) * (width / 6) + 3, y + height, previousStep, nextStep, true);
            ++step;
         }

         int targetSliderMinX = (int)((float)x + (float)width * hue);
         long cursorLength = snap ? 40L : 120L;
         int sliderMinX = (int)Math.round(this.hueCursorAnimation.get((double)targetSliderMinX, cursorLength, Easing.SineOut));
         Render2DUtil.rect(matrixStack, (float)(sliderMinX - 1), (float)y - 1.2F, (float)(sliderMinX + 1), (float)(y + height) + 1.2F, -1);
         drawOutlineRect((double)sliderMinX - 1.2, (double)y - 1.2, (double)sliderMinX + 1.2, (double)(y + height) + 1.2, Color.BLACK, 0.1F);
      }

   }

   public void drawAlphaSlider(int x, int y, int width, int height, float red, float green, float blue, float alpha, boolean snap) {
      boolean left = true;
      int checkerBoardSquareSize = height / 2;

      for(int squareIndex = -checkerBoardSquareSize; squareIndex < width; squareIndex += checkerBoardSquareSize) {
         if (!left) {
            Render2DUtil.rect(matrixStack, (float)(x + squareIndex), (float)y, (float)(x + squareIndex + checkerBoardSquareSize), (float)(y + height), -1);
            Render2DUtil.rect(matrixStack, (float)(x + squareIndex), (float)(y + checkerBoardSquareSize), (float)(x + squareIndex + checkerBoardSquareSize), (float)(y + height), -7303024);
            if (squareIndex < width - checkerBoardSquareSize) {
               int minX = x + squareIndex + checkerBoardSquareSize;
               int maxX = Math.min(x + width, x + squareIndex + checkerBoardSquareSize * 2);
               Render2DUtil.rect(matrixStack, (float)minX, (float)y, (float)maxX, (float)(y + height), -7303024);
               Render2DUtil.rect(matrixStack, (float)minX, (float)(y + checkerBoardSquareSize), (float)maxX, (float)(y + height), -1);
            }
         }

         left = !left;
      }

      drawLeftGradientRect(x, y, x + width, y + height, (new Color(red, green, blue, 1.0F)).getRGB(), 0);
      int targetSliderMinX = (int)((float)(x + width) - (float)width * alpha);
      long cursorLength = snap ? 40L : 120L;
      int sliderMinX = (int)Math.round(this.alphaCursorAnimation.get((double)targetSliderMinX, cursorLength, Easing.SineOut));
      Render2DUtil.rect(matrixStack, (float)(sliderMinX - 1), (float)y - 1.2F, (float)(sliderMinX + 1), (float)(y + height) + 1.2F, -1);
      drawOutlineRect((double)sliderMinX - 1.2, (double)y - 1.2, (double)sliderMinX + 1.2, (double)(y + height) + 1.2, Color.BLACK, 0.1F);
   }
}
