package dev.kissoo.mod.gui.items.buttons;

import dev.kissoo.api.utils.render.ColorUtil;
import dev.kissoo.api.utils.render.Render2DUtil;
import dev.kissoo.mod.gui.ClickGuiScreen;
import dev.kissoo.mod.modules.impl.client.ClickGui;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

public class BooleanButton extends Button {
   private final BooleanSetting setting;

   public BooleanButton(BooleanSetting setting) {
      super(setting.getName());
      this.setting = setting;
   }

   @Override
   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      Color color = ClickGui.getInstance().enableBGColor.getValue();
      boolean hovered = this.isHovering(mouseX, mouseY);
      int bg = this.getState()
              ? (hovered
              ? ColorUtil.injectAlpha(color, Math.min(255, color.getAlpha() + 40)).getRGB()
              : color.getRGB())
              : (hovered ? Button.hoverBGColor : Button.defaultBGColor);

      Render2DUtil.rect(context.getMatrices(),
              this.x, this.y,
              this.x + this.width + 7.0F,
              this.y + this.height - 0.5F,
              bg);

      this.drawString(this.getName(),
              this.x + 2.3F,
              this.y - 1.7F - ClickGuiScreen.getInstance().getTextOffset(),
              this.getState() ? Button.enableTextColor : Button.defaultTextColor);

      if (this.setting.hasParent()) {
         this.drawString(this.setting.isOpen() ? "-" : "+",
                 this.x + this.width - 1.0F,
                 this.y - 1.7F - ClickGuiScreen.getInstance().getTextOffset(),
                 ClickGui.getInstance().gear.getValue());
      }
   }


   @Override
   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
         sound();
         this.setting.setOpen(!this.setting.isOpen());
      }
   }

   @Override
   public void toggle() {
      this.setting.setValue(!this.setting.getValue());
   }

   @Override
   public boolean getState() {
      return this.setting.getValue();
   }
}