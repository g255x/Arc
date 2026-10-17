package dev.Lumi.mod.gui.items.buttons;

import dev.Lumi.api.utils.render.ColorUtil;
import dev.Lumi.api.utils.render.Render2DUtil;
import dev.Lumi.mod.gui.clickgui.ClickGuiScreen;
import dev.Lumi.mod.gui.items.Component;
import dev.Lumi.mod.gui.items.Item;
import dev.Lumi.mod.modules.impl.client.ClickGui;
import java.awt.Color;
import net.minecraft.class_332;

public class Button extends Item {
   private boolean state;
   public static int hoverColor = -2007673515;
   public static int defaultColor = 290805077;
   public static int defaultTextColor = -5592406;
   public static int enableTextColor = -1;

   public Button(String name) {
      super(name);
      this.setHeight(15);
   }

   public void drawScreen(class_332 context, int mouseX, int mouseY, float partialTicks) {
      boolean hovered = this.isHovering(mouseX, mouseY);
      float w = (float)this.width + 7.0F;
      float h = (float)this.height - 0.5F;
      float radius = 4.0F;
      Color borderColor = new Color(255, 255, 255, 80);
      if (this.getState()) {
         int a = hovered ? ClickGui.getInstance().hoverAlpha.getValueInt() : ClickGui.getInstance().alpha.getValueInt();
         double baseDelay = this.getColorDelay();
         if (ClickGui.getInstance().colorMode.getValue() == ClickGui.ColorMode.Spectrum) {
            Render2DUtil.drawLutRect(context.method_51448(), this.x, this.y, w, h, ClickGui.getInstance().getSpectrumLutId(), ClickGui.getInstance().getSpectrumLutHeight(), a);
         } else {
            Color color = ClickGui.getInstance().getActiveColor(baseDelay);
            Color fill = ColorUtil.injectAlpha(color, a);
            Render2DUtil.drawRoundedRectWithOutline(context.method_51448(), this.x, this.y, w, h, radius, fill, borderColor);
         }
      } else {
         int bg = !hovered ? defaultColor : hoverColor;
         Color bgColor = new Color(bg, true);
         Render2DUtil.drawRoundedRectWithOutline(context.method_51448(), this.x, this.y, w, h, radius, bgColor, borderColor);
      }

      float textY = this.getCenteredTextY(this.y, (float)this.height - 0.5F);
      this.drawString(this.getName(), (double)(this.x + 2.3F), (double)textY, this.getState() ? enableTextColor : defaultTextColor);
   }

   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
         this.onMouseClick();
      }

   }

   public void onMouseClick() {
      this.state = !this.state;
      this.toggle();
      sound();
   }

   public void toggle() {
   }

   public boolean getState() {
      return this.state;
   }

   public int getHeight() {
      return this.height - 1;
   }

   public boolean isHovering(int mouseX, int mouseY) {
      for(Component component : ClickGuiScreen.getInstance().getComponents()) {
         if (component.drag) {
            return false;
         }
      }

      return (float)mouseX >= this.getX() && (float)mouseX <= this.getX() + (float)this.getWidth() && (float)mouseY >= this.getY() && (float)mouseY <= this.getY() + (float)this.height - 1.0F;
   }
}
