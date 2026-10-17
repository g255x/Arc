package dev.kissoo.mod.gui.items.buttons;

import dev.kissoo.api.utils.render.ColorUtil;
import dev.kissoo.api.utils.render.Render2DUtil;
import dev.kissoo.mod.gui.ClickGuiScreen;
import dev.kissoo.mod.gui.items.Component;
import dev.kissoo.mod.gui.items.Item;
import dev.kissoo.mod.modules.impl.client.ClickGui;
import net.minecraft.client.gui.DrawContext;

import java.awt.Color;

public class Button extends Item {
   private boolean state;

   // 重命名静态颜色字段
   public static int hoverBGColor    = -2007673515;
   public static int defaultBGColor  = 290805077;
   public static int defaultTextColor = -5592406;
   public static int enableTextColor  = -1;

   public Button(String name) {
      super(name);
      this.setHeight(15);
   }

   @Override
   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      Color enableBG = ClickGui.getInstance().enableBGColor.getValue();
      int bgColor;
      if (this.getState()) {
         bgColor = this.isHovering(mouseX, mouseY)
                 ? ColorUtil.injectAlpha(enableBG, Math.min(255, enableBG.getAlpha() + 40)).getRGB()
                 : enableBG.getRGB();
      } else {
         bgColor = this.isHovering(mouseX, mouseY) ? hoverBGColor : defaultBGColor;
      }

      Render2DUtil.rect(
              context.getMatrices(),
              this.x, this.y,
              this.x + this.width,
              this.y + this.height - 0.5F,
              bgColor
      );

      this.drawString(
              this.getName(),
              this.x + 2.3F,
              this.y - 2.0F - ClickGuiScreen.getInstance().getTextOffset(),
              this.getState() ? enableTextColor : defaultTextColor
      );
   }
   @Override
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

   public void toggle() {}

   public boolean getState() {
      return this.state;
   }

   @Override
   public int getHeight() {
      return this.height - 1;
   }

   public boolean isHovering(int mouseX, int mouseY) {
      for (Component component : ClickGuiScreen.getInstance().getComponents()) {
         if (component.drag) return false;
      }
      return mouseX >= this.getX()
              && mouseX <= this.getX() + this.getWidth()
              && mouseY >= this.getY()
              && mouseY <= this.getY() + this.height - 1.0F;
   }
}