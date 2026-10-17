package dev.baimaodaxia.mod.gui.items.buttons;

import dev.baimaodaxia.api.utils.render.ColorUtil;
import dev.baimaodaxia.api.utils.render.Render2DUtil;
import dev.baimaodaxia.mod.gui.ClickGuiScreen;
import dev.baimaodaxia.mod.gui.items.Component;
import dev.baimaodaxia.mod.gui.items.Item;
import dev.baimaodaxia.mod.modules.impl.client.ClickGui;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
public class Button extends Item {
   private boolean state;
   public static int bgButton = 290805077;
   public static int textColor = -5592406;
   public static int enableTextColor = -1;

   public Button(String name) {
      super(name);
      this.setHeight(15);
   }

   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      Color bgColor = ClickGui.getInstance().bgColor.getValue();
      Color bgEnable = ClickGui.getInstance().bgEnable.getValue();
      MatrixStack MatrixStackVargetMatrices = context.getMatrices();
      float f = this.x;
      float f2 = this.y;
      float f3 = this.x + (float)this.width;
      float f4 = this.y + (float)this.height - 0.5F;
      int rgb;
      if (this.getState()) {
         if (!this.isHovering(mouseX, mouseY)) {
            rgb = ColorUtil.injectAlpha(bgEnable, ClickGui.getInstance().color.getValue().getAlpha()).getRGB();
         } else {
            rgb = ColorUtil.injectAlpha(bgEnable, ClickGui.getInstance().bgButton.getValue().getAlpha()).getRGB();
         }
      } else {
         rgb = !this.isHovering(mouseX, mouseY) ? bgColor.getRGB() : bgButton;
      }

      Render2DUtil.rect(MatrixStackVargetMatrices, f, f2, f3, f4, rgb);
      this.drawString(this.getName(), (double)(this.x + 2.3F), (double)(this.y - 2.0F - (float)ClickGuiScreen.getInstance().getTextOffset(this.isHovering(mouseX, mouseY))), this.getState() ? enableTextColor : textColor);
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
