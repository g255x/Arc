package dev.baimaodaxia.mod.gui.items;

import dev.baimaodaxia.api.utils.math.Easing;
import dev.baimaodaxia.api.utils.render.ColorUtil;
import dev.baimaodaxia.api.utils.render.Render2DUtil;
import dev.baimaodaxia.core.impl.FontManager;
import dev.baimaodaxia.mod.Mod;
import dev.baimaodaxia.mod.gui.ClickGuiScreen;
import dev.baimaodaxia.mod.gui.items.buttons.ModuleButton;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.impl.client.ClickGui;
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
      this.setWidth(93);
      this.setHeight(18);
      this.open = open;
      this.setupItems();
   }

   public void setupItems() {
   }

   private void drag(int mouseX, int mouseY) {
      if (this.drag) {
         this.x = this.x2 + mouseX;
         this.y = this.y2 + mouseY;
      }

   }

   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      this.context = context;
      this.drag(mouseX, mouseY);
      float totalItemHeight = this.open ? this.getTotalItemHeight() - 2.0F : 0.0F;
      int color = ColorUtil.injectAlpha(ClickGui.getInstance().color.getValue().getRGB(), ClickGui.getInstance().color.getValue().getAlpha());
      Render2DUtil.drawRect(context.getMatrices(), (float)this.x, (float)this.y - 2.5F, (float)this.width, (float)this.height - 2.5F, color);
      Render2DUtil.drawRect(context.getMatrices(), (float)this.x, (float)this.y - 2.5F, (float)this.width, (float)this.height - 2.5F, color);
      if (this.open) {
         Render2DUtil.drawRect(context.getMatrices(), (float)this.x, (float)(this.y + this.height) - 5.0F, (float)this.width, (float)(this.y + this.height) + totalItemHeight - ((float)(this.y + this.height) - 5.0F), ClickGui.getInstance().backGround.getValue());
         if (ClickGui.getInstance().line.getValue()) {
            Render2DUtil.drawLine(context.getMatrices(), (float)this.x + 0.5F, (float)(this.y + this.height) + totalItemHeight, (float)this.x + 0.5F, (float)(this.y + this.height) - 5.0F, ColorUtil.injectAlpha(ClickGui.getInstance().color.getValue().getRGB(), ClickGui.getInstance().color.getValue().getAlpha()));
            Render2DUtil.drawLine(context.getMatrices(), (float)this.x + 0.3F, (float)(this.y + this.height) + totalItemHeight, (float)this.x + 0.3F, (float)(this.y + this.height) - 5.0F, ColorUtil.injectAlpha(ClickGui.getInstance().color.getValue().getRGB(), ClickGui.getInstance().color.getValue().getAlpha()));
            Render2DUtil.drawLine(context.getMatrices(), (float)this.x + 0.7F, (float)(this.y + this.height) + totalItemHeight, (float)this.x + 0.7F, (float)(this.y + this.height) - 5.0F, ColorUtil.injectAlpha(ClickGui.getInstance().color.getValue().getRGB(), ClickGui.getInstance().color.getValue().getAlpha()));
            Render2DUtil.drawLine(context.getMatrices(), (float)(this.x + this.width), (float)(this.y + this.height) + totalItemHeight, (float)(this.x + this.width), (float)(this.y + this.height) - 5.0F, ColorUtil.injectAlpha(ClickGui.getInstance().color.getValue().getRGB(), ClickGui.getInstance().color.getValue().getAlpha()));
            Render2DUtil.drawLine(context.getMatrices(), (float)(this.x + this.width) - 0.6F, (float)(this.y + this.height) + totalItemHeight, (float)(this.x + this.width) - 0.6F, (float)(this.y + this.height) - 5.0F, ColorUtil.injectAlpha(ClickGui.getInstance().color.getValue().getRGB(), ClickGui.getInstance().color.getValue().getAlpha()));
            Render2DUtil.drawLine(context.getMatrices(), (float)(this.x + this.width) - 0.2F, (float)(this.y + this.height) + totalItemHeight, (float)(this.x + this.width) - 0.2F, (float)(this.y + this.height) - 5.0F, ColorUtil.injectAlpha(ClickGui.getInstance().color.getValue().getRGB(), ClickGui.getInstance().color.getValue().getAlpha()));
            Render2DUtil.drawLine(context.getMatrices(), (float)this.x, (float)(this.y + this.height) + totalItemHeight, (float)(this.x + this.width), (float)(this.y + this.height) + totalItemHeight, ColorUtil.injectAlpha(ClickGui.getInstance().color.getValue().getRGB(), ClickGui.getInstance().color.getValue().getAlpha()));
            Render2DUtil.drawLine(context.getMatrices(), (float)this.x, (float)(this.y + this.height) + totalItemHeight + 0.2F, (float)(this.x + this.width), (float)(this.y + this.height) + totalItemHeight + 0.2F, ColorUtil.injectAlpha(ClickGui.getInstance().color.getValue().getRGB(), ClickGui.getInstance().color.getValue().getAlpha()));
            Render2DUtil.drawLine(context.getMatrices(), (float)this.x, (float)(this.y + this.height) + totalItemHeight + 0.4F, (float)(this.x + this.width), (float)(this.y + this.height) + totalItemHeight + 0.4F, ColorUtil.injectAlpha(ClickGui.getInstance().color.getValue().getRGB(), ClickGui.getInstance().color.getValue().getAlpha()));
            Render2DUtil.drawLine(context.getMatrices(), (float)this.x, (float)(this.y + this.height) + totalItemHeight + 0.6F, (float)(this.x + this.width), (float)(this.y + this.height) + totalItemHeight + 0.6F, ColorUtil.injectAlpha(ClickGui.getInstance().color.getValue().getRGB(), ClickGui.getInstance().color.getValue().getAlpha()));
         }
      }

      this.drawString(this.getName(), (double)((float)this.x + 3.0F), (double)((float)this.y - 1.0F - (float)(-ClickGui.getInstance().titleOffset.getValueInt() - 5)), -1);
      if (this.open) {
         float y = (float)(this.getY() + this.getHeight()) - 3.0F;

         for(ModuleButton item : this.getItems()) {
            if (!item.isHidden()) {
               item.setLocation((float)this.x + 2.0F, y);
               item.setWidth(this.getWidth() - 4);
               if (item.itemHeight <= (double)0.0F && !item.subOpen) {
                  item.drawScreen(context, mouseX, mouseY, partialTicks);
               } else {
                  context.enableScissor((int)item.x, (int)item.y, mc.getWindow().getScaledWidth(), (int)((double)(y + (float)item.getButtonHeight() + 1.5F) + item.itemHeight));
                  item.drawScreen(context, mouseX, mouseY, partialTicks);
                  context.disableScissor();
               }

               y += (float)item.getButtonHeight() + 1.5F + (float)item.itemHeight;
            }
         }
      }

   }

   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
         this.x2 = this.x - mouseX;
         this.y2 = this.y - mouseY;
         ClickGuiScreen.getInstance().getComponents().forEach((component) -> {
            if (component.drag) {
               component.drag = false;
            }

         });
         this.drag = true;
      } else {
         if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
            this.open = !this.open;
            Item.sound();
         } else if (this.open) {
            this.getItems().forEach((item) -> item.mouseClicked(mouseX, mouseY, mouseButton));
         }

      }
   }

   public void mouseReleased(int mouseX, int mouseY, int releaseButton) {
      if (releaseButton == 0) {
         this.drag = false;
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
      return this.x;
   }

   public void setX(int x) {
      this.x = x;
   }

   public int getY() {
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

   private boolean isHovering(int mouseX, int mouseY) {
      return mouseX >= this.getX() && mouseX <= this.getX() + this.getWidth() && mouseY >= this.getY() && mouseY <= this.getY() + this.getHeight() - 5;
   }

   private float getTotalItemHeight() {
      float height = 0.0F;

      for(ModuleButton item : this.getItems()) {
         item.update();
         item.itemHeight = item.animation.get(item.subOpen ? (double)item.getItemHeight() : (double)0.0F, 200L, Easing.CubicInOut);
         height += (float)item.getButtonHeight() + 1.5F + (float)item.itemHeight;
      }

      return height;
   }

   protected void drawString(String text, double x, double y, Color color) {
      this.drawString(text, x, y, color.hashCode());
   }

   protected void drawString(String text, double x, double y, int color) {
      if (ClickGui.getInstance().font.getValue()) {
         FontManager.ui.drawString(this.context.getMatrices(), text, (double)((int)x), (double)((int)y), color, ClickGui.getInstance().shadow.getValue());
      } else {
         this.context.drawText(mc.textRenderer, text, (int)x, (int)y, color, ClickGui.getInstance().shadow.getValue());
      }

   }
}
