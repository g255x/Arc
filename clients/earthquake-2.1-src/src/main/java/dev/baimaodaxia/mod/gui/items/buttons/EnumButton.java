package dev.baimaodaxia.mod.gui.items.buttons;

import dev.baimaodaxia.api.utils.render.ColorUtil;
import dev.baimaodaxia.api.utils.render.Render2DUtil;
import dev.baimaodaxia.mod.gui.ClickGuiScreen;
import dev.baimaodaxia.mod.modules.impl.client.ClickGui;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import java.awt.Color;
import net.minecraft.util.Formatting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public class EnumButton extends Button {
   public final EnumSetting<?> setting;
   private boolean open;

   public EnumButton(EnumSetting<?> setting) {
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
      String var10001 = this.setting.getName();
      this.drawString(var10001 + " " + String.valueOf(Formatting.GRAY) + (this.setting.getValue().name().equalsIgnoreCase("ABC") ? "ABC" : this.setting.getValue().name()), (double)(this.x + 2.3F), (double)(this.y - 1.7F - (float)ClickGuiScreen.getInstance().getTextOffset(hovering)), this.getState() ? enableTextColor : textColor);
      if (this.open) {
         int y = (int)this.y;

         for(Enum<?> e : (Enum[])this.setting.getValue().getDeclaringClass().getEnumConstants()) {
            String s = e.name();
            this.drawString(s, (double)((float)this.width / 2.0F - (float)this.getWidth(s) / 2.0F + 2.0F + this.x), (double)((float)(y + this.height) - 3.0F - (float)ClickGuiScreen.getInstance().getTextOffset()), this.setting.getValue().name().equals(s) ? enableTextColor : textColor);
            y += 11;
         }
      }

   }

   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
         this.open = !this.open;
         sound();
      } else {
         if (mouseButton == 0 && this.open) {
            int y = (int)this.y;

            for(Object o : (Enum[])this.setting.getValue().getDeclaringClass().getEnumConstants()) {
               if ((float)mouseX > this.x && (float)mouseX < this.x + (float)this.width && mouseY >= y + this.height + 1 && mouseY < y + this.height + 11 + 1) {
                  this.setting.setEnumValue(String.valueOf(o));
                  sound();
               }

               y += 11;
            }
         }

      }
   }

   public int getHeight() {
      return super.getHeight() + (this.open ? 11 * ((Enum[])this.setting.getValue().getDeclaringClass().getEnumConstants()).length : 0);
   }

   public void toggle() {
      this.setting.increaseEnum();
   }

   public boolean getState() {
      return true;
   }
}
