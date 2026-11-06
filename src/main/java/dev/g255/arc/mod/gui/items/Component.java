package dev.g255.arc.mod.gui.items;

import dev.g255.arc.api.utils.math.Animation;
import dev.g255.arc.api.utils.math.Easing;
import dev.g255.arc.api.utils.render.ColorUtil;
import dev.g255.arc.api.utils.render.Render2DUtil;
import dev.g255.arc.core.impl.FontManager;
import dev.g255.arc.mod.Mod;
import dev.g255.arc.mod.gui.ClickGuiScreen;
import dev.g255.arc.mod.gui.items.buttons.Button;
import dev.g255.arc.mod.gui.items.buttons.ModuleButton;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.impl.client.ClickGui;
import dev.g255.arc.mod.modules.settings.ChineseTranslator;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;

public class Component extends Mod {
   private final List<ModuleButton> items = new ArrayList();
   private final Module.Category category;
   public boolean drag;
   protected DrawContext context;
   private int x;
   private int y;
   private float animX;
   private float animY;
   private float mouseMoveOffsetX;
   private float mouseMoveOffsetY;
   private final Animation xAnimation = new Animation();
   private final Animation yAnimation = new Animation();
   private final Animation openAnimation = new Animation();
   private final Animation hoverAnimation = new Animation();
   private int x2;
   private int y2;
   private int width;
   private int height;
   private boolean open;
   private boolean hidden = false;

   public Component(String name, Module.Category category, int x, int y, boolean open) {
      super(name);
      this.category = category;
      this.setX(x);
      this.setY(y);
      this.animX = (float)this.x;
      this.animY = (float)this.y;
      if (ClickGui.getInstance() != null) {
         this.setWidth(ClickGui.getInstance().moduleButtonWidth.getValueInt());
         this.setHeight(ClickGui.getInstance().categoryBarHeight.getValueInt() + 5);
      } else {
         this.setWidth(93);
         this.setHeight(18);
      }

      this.open = open;
      this.setupItems();
   }

   public void setupItems() {
   }

   private void drag(int mouseX, int mouseY) {
      if (this.drag) {
         this.x = this.x2 + mouseX;
         this.y = this.y2 + mouseY;
         this.animX = (float)this.x;
         this.animY = (float)this.y;
      }
   }

   private void updatePosition() {
      if (ClickGui.getInstance().scrollAnim.getValue() && !this.drag) {
         int length = Math.max(1, ClickGui.getInstance().scrollAnimLength.getValueInt());
         Easing easing = ClickGui.getInstance().scrollAnimEasing.getValue();
         this.animX = (float)this.xAnimation.get((double)this.x, (long)length, easing);
         this.animY = (float)this.yAnimation.get((double)this.y, (long)length, easing);
      } else {
         this.animX = (float)this.x;
         this.animY = (float)this.y;
         this.xAnimation.from = (double)this.x;
         this.xAnimation.to = (double)this.x;
         this.yAnimation.from = (double)this.y;
         this.yAnimation.to = (double)this.y;
      }
   }

   protected double getColorDelay() {
      return (double)this.getY() / (double)10.0F;
   }

   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      this.context = context;
      this.drag(mouseX, mouseY);
      this.updatePosition();
      int x = this.getX();
      int y = this.getY();
      float targetItemHeight = this.getTotalItemHeight() - 2.0F;
      double openProgress = this.openAnimation.get(this.open ? (double)1.0F : (double)0.0F, 200L, Easing.CubicInOut);
      float totalItemHeight = (float)((double)targetItemHeight * openProgress);
      int categoryWidth = ClickGui.getInstance().categoryWidth.getValueInt();
      float headerX = (float)x + ((float)this.width - (float)categoryWidth) / 2.0F;
      float headerY = (float)y;
      float headerW = (float)categoryWidth;
      float headerH = (float)this.height - 5.0F;
      double headerBaseDelay = this.getColorDelay();
      int topAlpha = ClickGui.getInstance().topAlpha.getValueInt();
      boolean purple = ClickGui.getInstance().ui.getValue() == ClickGui.UI.Purple;
      if (ClickGui.getInstance().colorMode.getValue() == ClickGui.ColorMode.Spectrum) {
         Render2DUtil.drawLutRect(context.getMatrices(), headerX, headerY, headerW, headerH, ClickGui.getInstance().getSpectrumLutId(), ClickGui.getInstance().getSpectrumLutHeight(), topAlpha);
      } else {
         Color topColor = ColorUtil.injectAlpha(ClickGui.getInstance().getColor(headerBaseDelay), topAlpha);
         Render2DUtil.drawRect(context.getMatrices(), headerX, headerY, headerW, headerH, topColor);
      }

      if (!purple) {
         Render2DUtil.verticalGradient(context.getMatrices(), headerX, headerY, headerX + headerW, headerY + headerH, new Color(0, 0, 0, 0), new Color(ClickGui.getInstance().endColor.getValue().getRGB()));
      }

      boolean hovering = !this.drag && this.isHovering(mouseX, mouseY);
      double hoverProgress = this.hoverAnimation.get(hovering ? (double)1.0F : (double)0.0F, 130L, Easing.CubicInOut);
      if (hoverProgress > 0.01) {
         Render2DUtil.drawRect(context.getMatrices(), headerX, headerY, headerW, headerH, (new Color(255, 255, 255, (int)Math.round((double)36.0F * hoverProgress))).getRGB());
      }

      if (openProgress > 0.01) {
         if (ClickGui.getInstance().backGround.booleanValue) {
            Render2DUtil.drawRect(context.getMatrices(), (float)x, (float)y + (float)this.height - 5.0F, (float)this.width, (float)(y + this.height) + totalItemHeight - ((float)y + (float)this.height - 5.0F), ColorUtil.injectAlpha(ClickGui.getInstance().backGround.getValue(), ClickGui.getInstance().backgroundAlpha.getValueInt()));
            if (!purple) {
               Render2DUtil.verticalGradient(context.getMatrices(), (float)x, (float)y + (float)this.height - 5.0F, (float)(x + this.width), (float)(y + this.height) + totalItemHeight, new Color(0, 0, 0, 0), new Color(ClickGui.getInstance().endColor.getValue().getRGB()));
            }
         }

         if (ClickGui.getInstance().line.getValue()) {
            float yTop = (float)y + (float)this.height - 5.0F;
            float yBottom = (float)(y + this.height) + totalItemHeight;
            double delayPerPixel = (double)0.25F;
            float leftX = (float)x;
            float rightX = (float)(x + this.width) - 1.0F;
            int alpha = ClickGui.getInstance().topAlpha.getValueInt();
            if (ClickGui.getInstance().colorMode.getValue() == ClickGui.ColorMode.Spectrum) {
               float lineW = 1.0F;
               Render2DUtil.drawLutRect(context.getMatrices(), leftX, yTop, lineW, yBottom - yTop, ClickGui.getInstance().getSpectrumLutId(), ClickGui.getInstance().getSpectrumLutHeight(), alpha);
               Render2DUtil.drawLutRect(context.getMatrices(), rightX - lineW, yTop, lineW, yBottom - yTop, ClickGui.getInstance().getSpectrumLutId(), ClickGui.getInstance().getSpectrumLutHeight(), alpha);
            } else {
               float segment = 2.0F;

               for(float yy = yTop; yy < yBottom; yy += segment) {
                  float yy2 = Math.min(yy + segment, yBottom);
                  int c = ColorUtil.injectAlpha(ClickGui.getInstance().getColor((double)yy * delayPerPixel).getRGB(), alpha);
                  Render2DUtil.drawLine(context.getMatrices(), leftX, yy2, leftX, yy, c);
                  Render2DUtil.drawLine(context.getMatrices(), rightX, yy2, rightX, yy, c);
               }
            }

            int bottomColor = ColorUtil.injectAlpha(ClickGui.getInstance().getColor((double)yBottom * delayPerPixel).getRGB(), alpha);
            Render2DUtil.drawLine(context.getMatrices(), (float)x, yBottom, (float)(x + this.width), yBottom, bottomColor);
         }
      }

      float nameFontHeight = FontManager.isCustomFontEnabled() ? FontManager.ui.getFontHeight() : 9.0F;
      float nameY = headerY + (headerH - nameFontHeight) / 2.0F + (float)ClickGui.getInstance().titleOffset.getValueInt();
      String title = ChineseTranslator.category(this.category.name());
      if (purple) {
         this.drawScaledString(title, (double)(headerX + 5.0F), (double)nameY, Button.enableTextColor, nameFontHeight, (float)hoverProgress);
         int iconColor = ClickGui.getInstance().defaultTextColor.getValue().getRGB();
         this.drawExpandIcon((double)(headerX + headerW - 8.0F), (double)nameY, iconColor, (float)openProgress);
      } else {
         float iconY = headerY + (headerH - FontManager.icon.getFontHeight()) / 2.0F;
         FontManager.icon.drawString(context.getMatrices(), this.category.getIcon(), (double)(headerX + 6.0F), (double)iconY, Button.enableTextColor);
         this.drawScaledString(title, (double)(headerX + 20.0F), (double)nameY, Button.enableTextColor, nameFontHeight, (float)hoverProgress);
      }

      if (openProgress > 0.01) {
         int panelX1 = x - 1;
         int panelY1 = (int)((float)y + (float)this.height - 6.0F);
         int panelX2 = x + this.width + 1;
         int panelY2 = (int)Math.round((double)(y + this.height) + (double)totalItemHeight) + 1;
         context.enableScissor(panelX1, panelY1, panelX2, panelY2);
         float yOff = (float)(this.getY() + this.getHeight()) - 3.0F;

         for(ModuleButton item : this.getItems()) {
            if (!item.isHidden()) {
               item.setLocation((float)x + 2.0F, yOff);
               item.setWidth(this.getWidth() - 4);
               if (!(item.itemHeight > (double)0.0F) && !item.subOpen) {
                  item.drawScreen(context, mouseX, mouseY, partialTicks);
               } else {
                  int scissorX1 = (int)item.x - 1;
                  int scissorY1 = (int)item.y - 1;
                  int scissorX2 = (int)(item.x + (float)item.getWidth() + 1.0F);
                  int scissorY2 = (int)((double)(yOff + (float)item.getButtonHeight() + this.getItemGap()) + item.itemHeight) + 1;
                  context.enableScissor(scissorX1, scissorY1, scissorX2, scissorY2);
                  item.drawScreen(context, mouseX, mouseY, partialTicks);
                  context.disableScissor();
               }

               yOff += (float)item.getButtonHeight() + this.getItemGap() + (float)item.itemHeight;
            }
         }

         context.disableScissor();
      }

   }

   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
         if (ClickGui.getInstance().mouseMove.getValue()) {
            this.setX(this.getX());
            this.setY(this.getY());
            this.animX = (float)this.x;
            this.animY = (float)this.y;
            this.xAnimation.from = (double)this.x;
            this.xAnimation.to = (double)this.x;
            this.yAnimation.from = (double)this.y;
            this.yAnimation.to = (double)this.y;
         }

         this.x2 = this.getX() - mouseX;
         this.y2 = this.getY() - mouseY;
         ClickGuiScreen.getInstance().getComponents().forEach((component) -> {
            if (component.drag) {
               component.drag = false;
            }

         });
         this.drag = true;
      } else if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
         this.open = !this.open;
         Item.sound();
      } else if (this.open) {
         this.getItems().forEach((item) -> item.mouseClicked(mouseX, mouseY, mouseButton));
      }
   }

   public void mouseReleased(int mouseX, int mouseY, int releaseButton) {
      boolean wasDragging = this.drag;
      if (releaseButton == 0) {
         this.drag = false;
      }

      // 拖动结束后把位置写进配置，下次打开（甚至重启游戏）保持玩家摆好的布局
      if (wasDragging && ClickGui.getInstance() != null) {
         ClickGui.getInstance().saveLayout();
      }

      if (this.open) {
         this.getItems().forEach((item) -> item.mouseReleased(mouseX, mouseY, releaseButton));
      }
   }

   public void onKeyTyped(char typedChar, int keyCode) {
      if (this.open) {
         this.getItems().forEach((item) -> item.onKeyTyped(typedChar, keyCode));
      }
   }

   public void onKeyPressed(int key) {
      if (this.open) {
         this.getItems().forEach((item) -> item.onKeyPressed(key));
      }
   }

   public void addButton(ModuleButton button) {
      this.items.add(button);
   }

   public int getX() {
      return (int)(this.animX + this.mouseMoveOffsetX);
   }

   public int getTargetX() {
      return this.x;
   }

   public void setX(int x) {
      this.x = x;
   }

   public int getY() {
      return (int)(this.animY + this.mouseMoveOffsetY);
   }

   public int getTargetY() {
      return this.y;
   }

   public void setY(int y) {
      this.y = y;
   }

   public int getWidth() {
      return this.width;
   }

   public void setWidth(int width) {
      this.width = width;
   }

   public int getHeight() {
      return this.height;
   }

   public void setHeight(int height) {
      this.height = height;
   }

   public boolean isHidden() {
      return this.hidden;
   }

   public void setHidden(boolean hidden) {
      this.hidden = hidden;
   }

   public boolean isOpen() {
      return this.open;
   }

   public final List<ModuleButton> getItems() {
      return this.items;
   }

   public void setMouseMoveOffset(float x, float y) {
      this.mouseMoveOffsetX = x;
      this.mouseMoveOffsetY = y;
   }

   private boolean isHovering(int mouseX, int mouseY) {
      int categoryWidth = ClickGui.getInstance().categoryWidth.getValueInt();
      int hx = Math.round((float)this.getX() + ((float)this.getWidth() - (float)categoryWidth) / 2.0F);
      int hy = this.getY();
      int hh = this.getHeight() - 5;
      return mouseX >= hx && mouseX <= hx + categoryWidth && mouseY >= hy && mouseY <= hy + hh;
   }

   private float getTotalItemHeight() {
      float height = 0.0F;

      for(ModuleButton item : this.getItems()) {
         item.update();
         double openProgress = item.animation.get(item.subOpen ? (double)1.0F : (double)0.0F, 200L, Easing.CubicInOut);
         item.itemHeight = item.getVisibleItemHeight() * openProgress;
         height += (float)item.getButtonHeight() + this.getItemGap() + (float)item.itemHeight;
      }

      return height;
   }

   private float getItemGap() {
      return ClickGui.getInstance().ui.getValue() == ClickGui.UI.Purple ? 1.0F : 1.5F;
   }

   private void drawScaledString(String text, double x, double y, int color, float textHeight, float hoverProgress) {
      if (hoverProgress <= 0.01F) {
         this.drawString(text, x, y, color);
      } else {
         float scale = 1.0F + 0.03F * hoverProgress;
         float pivotY = (float)y + textHeight / 2.0F;
         this.context.getMatrices().push();
         this.context.getMatrices().translate(x, (double)pivotY, (double)0.0F);
         this.context.getMatrices().scale(scale, scale, 1.0F);
         this.context.getMatrices().translate(-x, (double)(-pivotY), (double)0.0F);
         this.drawString(text, x, y, color);
         this.context.getMatrices().pop();
      }
   }

   private void drawExpandIcon(double x, double y, int color, float openProgress) {
      int alpha = color >>> 24 & 255;
      float plusProgress = openProgress < 0.5F ? 1.0F - openProgress * 2.0F : 0.0F;
      float minusProgress = openProgress < 0.5F ? 0.0F : (openProgress - 0.5F) * 2.0F;
      if (plusProgress > 0.01F) {
         this.drawString("+", x, y, ColorUtil.injectAlpha(color, Math.round((float)alpha * plusProgress)));
      }

      if (minusProgress > 0.01F) {
         this.drawString("-", x, y, ColorUtil.injectAlpha(color, Math.round((float)alpha * minusProgress)));
      }

   }

   protected void drawString(String text, double x, double y, Color color) {
      this.drawString(text, x, y, color.hashCode());
   }

   protected void drawString(String text, double x, double y, int color) {
      boolean shadow = FontManager.isShadowEnabled();
      if (FontManager.isCustomFontEnabled()) {
         FontManager.ui.drawString(this.context.getMatrices(), text, (double)((int)x), (double)((int)y), color, shadow);
      } else {
         this.context.drawText(mc.textRenderer, text, (int)x, (int)y, color, shadow);
      }

   }
}
