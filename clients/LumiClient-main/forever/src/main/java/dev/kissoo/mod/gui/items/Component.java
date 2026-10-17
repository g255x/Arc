package dev.kissoo.mod.gui.items;

import dev.kissoo.Forever;
import dev.kissoo.api.utils.math.Easing;
import dev.kissoo.api.utils.math.SmoothAnimationUtil;
import dev.kissoo.api.utils.render.ColorUtil;
import dev.kissoo.api.utils.render.Render2DUtil;
import hoprc.api.core.impl.FontManager;
import dev.kissoo.mod.Mod;
import dev.kissoo.mod.gui.ClickGuiScreen;
import dev.kissoo.mod.gui.items.buttons.BindButton;
import dev.kissoo.mod.gui.items.buttons.Button;
import dev.kissoo.mod.gui.items.buttons.ModuleButton;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.impl.client.ClickGui;
import dev.kissoo.mod.modules.impl.client.HudEditor;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Component extends Mod {
   private final List<Item> items = new ArrayList<>();
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
   private float scrollTarget = 0;
   private final SmoothAnimationUtil scrollAnimation = new SmoothAnimationUtil(0, 0.18, 0.05);
   private long lastScrollTime = 0;
   private final SmoothAnimationUtil scrollBarAlpha = new SmoothAnimationUtil(0, 0.12, 0.5);

   private float currentSmoothScroll = 0;

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

   private boolean isHudEditorOpen() {
      if (HudEditor.INSTANCE == null) return false;
      return this.category == Module.Category.HUD && HudEditor.INSTANCE.isOff() || this.category != Module.Category.HUD && HudEditor.INSTANCE.isOn();
   }

   public boolean isMouseOver(double mouseX, double mouseY) {
      if (this.isHudEditorOpen()) return false;
      float maxH = ClickGui.getInstance().maxHeight.getValueFloat();
      float contentH = this.open ? Math.min(getTotalItemHeight(), maxH) : 0;
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height + contentH;
   }

   public void addItem(Item item) {
      this.items.add(item);
   }

   public void setupItems() {
   }

   private void drag(int mouseX, int mouseY) {
      if (this.drag) {
         this.x = this.x2 + mouseX;
         this.y = this.y2 + mouseY;
      }
   }

   public List<float[]> getClippedShaderRects() {
      List<float[]> rects = new ArrayList<>();
      if (this.isHudEditorOpen() || !this.open) return rects;

      float totalItemHeight = this.getTotalItemHeight();
      float maxH = ClickGui.getInstance().maxHeight.getValueFloat();
      boolean needsScroll = totalItemHeight > maxH;
      float clampedHeight = needsScroll ? maxH : totalItemHeight;

      float smoothScroll = this.currentSmoothScroll;

      float panelStartY = this.y + this.height - 5.0F;
      float panelEndY = this.y + this.height + clampedHeight;
      float currentY = this.y + this.height - 3.0F;

      for (Item item : this.items) {
         if (!item.isHidden() && shouldDisplay(item)) {
            float itemY = currentY - smoothScroll;

            if (item instanceof ModuleButton mb && mb.getState()) {
               float buttonH = mb.getButtonHeight() - 0.5F;
               float buttonBottom = itemY + buttonH;

               float drawY = Math.max(itemY, panelStartY);
               float drawBottom = Math.min(buttonBottom, panelEndY);
               float drawH = drawBottom - drawY;

               if (drawH > 0) {
                  rects.add(new float[]{this.x + 2.0F, drawY, this.width - 4.0F, drawH});
               }
            }

            if (item instanceof ModuleButton mb) {
               currentY += mb.getButtonHeight() + 1.5F + (float) mb.itemHeight;
            } else {
               currentY += item.getHeight() + 1.5F;
            }
         }
      }
      return rects;
   }

   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      if (this.isHudEditorOpen()) return;
      this.context = context;
      this.drag(mouseX, mouseY);

      float totalItemHeight = this.open ? this.getTotalItemHeight() : 0.0F;
      float maxH = ClickGui.getInstance().maxHeight.getValueFloat();
      boolean needsScroll = totalItemHeight > maxH;
      float clampedHeight = (this.open && needsScroll) ? maxH : totalItemHeight;

      if (!needsScroll) {
         this.scrollTarget = 0;
         this.scrollAnimation.setTarget(0);
      } else {
         float maxScroll = totalItemHeight - clampedHeight;
         if (this.scrollTarget < 0) this.scrollTarget = 0;
         if (this.scrollTarget > maxScroll) this.scrollTarget = maxScroll;
         this.scrollAnimation.setTarget(this.scrollTarget);
      }

      this.currentSmoothScroll = (float) this.scrollAnimation.update();
      float smoothScroll = this.currentSmoothScroll;

      long timeSinceScroll = System.currentTimeMillis() - lastScrollTime;
      if (needsScroll && timeSinceScroll < 750) {
         this.scrollBarAlpha.setTarget(100);
      } else {
         this.scrollBarAlpha.setTarget(0);
      }
      double barAlpha = this.scrollBarAlpha.update();

      Render2DUtil.drawRect(
              context.getMatrices(),
              this.x, this.y,
              this.width, this.height - 5.0F,
              ClickGui.getInstance().topBarColor.getValue().getRGB()
      );

      if (this.open) {
         if (ClickGui.getInstance().blur.getValue()) {
            Forever.BLUR.applyBlur(
                    1.0F + (ClickGui.getInstance().radius.getValueFloat() - 1.0F) * (float) ClickGui.getInstance().alphaValue,
                    this.x,
                    (float) this.y + this.height - 5.0F,
                    this.width,
                    clampedHeight + 5.0F
            );
         }
         if (ClickGui.getInstance().panelBGColor.booleanValue) {
            Render2DUtil.drawRectWithOutline(
                    context.getMatrices(),
                    this.x,
                    (float) this.y + this.height - 5.0F,
                    this.width,
                    clampedHeight + 5.0F,
                    ClickGui.getInstance().panelBGColor.getValue(),
                    ClickGui.getInstance().lineColor.getValue()
            );
         }

         if (ClickGui.getInstance().line.getValue()) {
            int lineCol = ColorUtil.injectAlpha(
                    ClickGui.getInstance().lineColor.getValue().getRGB(),
                    ClickGui.getInstance().lineColor.getValue().getAlpha()
            );
            Render2DUtil.drawLine(context.getMatrices(),
                    this.x + 0.2F, this.y + this.height + clampedHeight,
                    this.x + 0.2F, (float) this.y + this.height - 5.0F, lineCol);
            Render2DUtil.drawLine(context.getMatrices(),
                    this.x + this.width, this.y + this.height + clampedHeight,
                    this.x + this.width, (float) this.y + this.height - 5.0F, lineCol);
            Render2DUtil.drawLine(context.getMatrices(),
                    this.x, this.y + this.height + clampedHeight,
                    this.x + this.width, this.y + this.height + clampedHeight, lineCol);
         }

         context.enableScissor(
                 (int) this.x,
                 (int) (this.y + this.height - 5),
                 (int) (this.x + this.width),
                 (int) (this.y + this.height + clampedHeight)
         );

         float currentY = this.getY() + this.getHeight() - 3.0F;
         for (Item item : this.getItems()) {
            if (!item.isHidden() && shouldDisplay(item)) {
               item.setLocation(this.x + 2.0F, currentY - smoothScroll);
               item.setWidth(this.getWidth() - 4);
               if (item.getY() + item.getHeight() > this.y
                       && item.getY() < this.y + this.height + clampedHeight + 10) {
                  item.drawScreen(context, mouseX, mouseY, partialTicks);
               }

               if (item instanceof ModuleButton mb) {
                  currentY += mb.getButtonHeight() + 1.5F + (float) mb.itemHeight;
               } else {
                  currentY += item.getHeight() + 1.5F;
               }
            }
         }

         context.disableScissor();
         if (needsScroll && barAlpha > 0.5) {
            int barColorInt = new Color(255, 255, 255, (int) Math.min(255, Math.max(0, barAlpha))).getRGB();
            float barW = 0.8f;
            float barH = Math.max(8f, clampedHeight * (clampedHeight / totalItemHeight) * 0.6f);
            if (barH > clampedHeight - 4) barH = clampedHeight - 4;
            float maxScrollVal = totalItemHeight - clampedHeight;
            float scrollPercent = maxScrollVal > 0 ? smoothScroll / maxScrollVal : 0;
            float barY = scrollPercent * (clampedHeight - barH - 2);
            Render2DUtil.drawRect(
                    context.getMatrices(),
                    this.x + this.width - barW - 0.8f,
                    this.y + this.height + barY,
                    barW,
                    barH,
                    barColorInt
            );
         }
      }

      FontManager.icon.drawString(context.getMatrices(), this.category.getIcon(),
              this.x + 6.0F, this.y + 4.0F, Button.enableTextColor);
      this.drawString(this.getName(),
              this.x + 20.0F,
              this.y - 1.0F - (-ClickGui.getInstance().titleOffset.getValueInt() - 6),
              Button.enableTextColor);
   }

   public void onMouseScroll(double mouseX, double mouseY, double amount) {
      if (this.isHudEditorOpen()) return;
      if (!this.open) return;

      float maxH = ClickGui.getInstance().maxHeight.getValueFloat();
      float totalH = getTotalItemHeight();

      if (totalH > maxH) {
         this.scrollTarget -= (float) (amount * 20);
         this.lastScrollTime = System.currentTimeMillis();
         float maxScroll = totalH - maxH;
         if (this.scrollTarget < 0) this.scrollTarget = 0;
         if (this.scrollTarget > maxScroll) this.scrollTarget = maxScroll;
      }
   }

   private boolean shouldDisplay(Item item) {
      if (item instanceof SearchBarItem) return true;
      if (SearchBarItem.searchText.isEmpty()) return true;
      if (item instanceof ModuleButton mb) {
         String filter = SearchBarItem.searchText.toLowerCase();
         String name = mb.getModule().getName().toLowerCase();
         String display = mb.getModule().getDisplayName().toLowerCase();
         return name.contains(filter) || display.contains(filter);
      }
      return true;
   }

   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (this.isHudEditorOpen()) return;
      if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
         this.x2 = this.x - mouseX;
         this.y2 = this.y - mouseY;
         ClickGuiScreen.getInstance().getComponents().forEach(component -> {
            if (component.drag) {
               component.drag = false;
            }
         });
         this.drag = true;
      } else if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
         this.open = !this.open;
         if (this.open) {
            this.scrollTarget = 0;
            this.scrollAnimation.setTarget(0);
         }
         Item.sound();
      } else if (this.open) {
         this.getItems().forEach(item -> {
            if (shouldDisplay(item)) {
               item.mouseClicked(mouseX, mouseY, mouseButton);
            }
         });
      }
   }

   public void mouseReleased(int mouseX, int mouseY, int releaseButton) {
      if (this.isHudEditorOpen()) return;
      if (releaseButton == 0) {
         this.drag = false;
      }
      if (this.open) {
         this.getItems().forEach(item -> {
            if (shouldDisplay(item)) {
               item.mouseReleased(mouseX, mouseY, releaseButton);
            }
         });
      }
   }

   public void onKeyTyped(char typedChar, int keyCode) {
      if (this.isHudEditorOpen()) return;
      if (this.open) {
         this.getItems().forEach(item -> {
            if (shouldDisplay(item)) {
               item.onKeyTyped(typedChar, keyCode);
            }
         });
      }
   }

   public boolean hasListeningBind() {
      if (this.isHudEditorOpen()) return false;
      if (!this.open) return false;
      for (Item item : this.items) {
         if (item instanceof BindButton bind && bind.isListening) {
            return true;
         }
         if (item instanceof ModuleButton mb) {
            for (Item subItem : mb.getItems()) {
               if (subItem instanceof BindButton bind && bind.isListening) {
                  return true;
               }
            }
         }
      }
      return false;
   }

   public void onKeyPressed(int key) {
      if (this.isHudEditorOpen()) return;
      if (this.open) {
         this.getItems().forEach(item -> {
            if (shouldDisplay(item)) {
               item.onKeyPressed(key);
            }
         });
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

   public final List<Item> getItems() {
      return this.items;
   }

   private boolean isHovering(int mouseX, int mouseY) {
      return mouseX >= this.getX() && mouseX <= this.getX() + this.getWidth() && mouseY >= this.getY() && mouseY <= this.getY() + this.getHeight() - 5;
   }

   private float getTotalItemHeight() {
      float height = 0.0F;
      for (Item item : this.getItems()) {
         if (shouldDisplay(item)) {
            item.update();
            if (item instanceof ModuleButton mb) {
               mb.itemHeight = mb.animation.get(mb.subOpen ? mb.getItemHeight() : 0.0, 200L, Easing.CubicInOut);
               height += mb.getButtonHeight() + 1.5F + (float) mb.itemHeight;
            } else {
               height += item.getHeight() + 1.5F;
            }
         }
      }
      return height;
   }

   protected void drawString(String text, double x, double y, int color) {
      if (ClickGui.getInstance().font.getValue()) {
         int rawAlpha = (color >> 24) & 0xFF;
         float alpha = (rawAlpha == 0) ? 1.0f : rawAlpha / 255.0f;

         FontManager.get().drawString(
                 this.context.getMatrices(),
                 text,
                 (float) x,
                 (float) y,
                 ((color >> 16) & 0xFF) / 255.0f,
                 ((color >> 8)  & 0xFF) / 255.0f,
                 ( color        & 0xFF) / 255.0f,
                 alpha,
                 ClickGui.getInstance().shadow.getValue()
         );
      } else {
         this.context.drawText(mc.textRenderer, text, (int) x, (int) y, color,
                 ClickGui.getInstance().shadow.getValue());
      }
   }
}