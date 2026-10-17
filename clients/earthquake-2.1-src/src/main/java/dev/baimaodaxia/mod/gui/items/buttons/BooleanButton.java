package dev.baimaodaxia.mod.gui.items.buttons;

import dev.baimaodaxia.api.utils.render.ColorUtil;
import dev.baimaodaxia.api.utils.render.Render2DUtil;
import dev.baimaodaxia.mod.gui.ClickGuiScreen;
import dev.baimaodaxia.mod.modules.impl.client.ClickGui;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public class BooleanButton extends Button {
   private final BooleanSetting setting;

   public BooleanButton(BooleanSetting setting) {
      super(setting.getName());
      this.setting = setting;
   }

   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      Color color = ClickGui.getInstance().color.getValue();
      MatrixStack MatrixStackVargetMatrices = context.getMatrices();
      float f = this.x;
      float f2 = this.y;
      float f3 = this.x + (float)this.width + 7.0F;
      float f4 = this.y + (float)this.height - 0.5F;
      int rgb;
      if (this.getState()) {
         rgb = ColorUtil.injectAlpha(color, ClickGui.getInstance().color.getValue().getAlpha()).getRGB();
      } else {
         rgb = bgButton;
      }

      Render2DUtil.rect(MatrixStackVargetMatrices, f, f2, f3, f4, rgb);
      boolean hovering = this.isHovering(mouseX, mouseY);
      this.drawString(this.getName(), (double)(this.x + 2.3F), (double)(this.y - 1.7F - (float)ClickGuiScreen.getInstance().getTextOffset(hovering)), textColor);
      if (this.setting.hasParent()) {
         this.drawString(this.setting.isOpen() ? "-" : "+", (double)(this.x + (float)this.width - 1.0F), (double)(this.y - 1.7F - (float)ClickGuiScreen.getInstance().getTextOffset(hovering)), textColor);
      }

   }

   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
         sound();
         this.setting.setOpen(!this.setting.isOpen());
      }

   }

   public void toggle() {
      this.setting.setValue(!this.setting.getValue());
   }

   public boolean getState() {
      return this.setting.getValue();
   }
}
