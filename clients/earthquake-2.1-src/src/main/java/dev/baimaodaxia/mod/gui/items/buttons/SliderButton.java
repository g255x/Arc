package dev.baimaodaxia.mod.gui.items.buttons;

import dev.baimaodaxia.api.utils.render.ColorUtil;
import dev.baimaodaxia.api.utils.render.Render2DUtil;
import dev.baimaodaxia.mod.gui.ClickGuiScreen;
import dev.baimaodaxia.mod.gui.items.Component;
import dev.baimaodaxia.mod.modules.impl.client.ClickGui;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import net.minecraft.util.Formatting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
public class SliderButton extends Button {
   private final double min;
   private final double max;
   private final double difference;
   public final SliderSetting setting;
   public boolean isListening;
   private String currentString = "";
   private boolean drag = false;

   public SliderButton(SliderSetting setting) {
      super(setting.getName());
      this.setting = setting;
      this.min = setting.getMin();
      this.max = setting.getMax();
      this.difference = this.max - this.min;
   }

   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      this.dragSetting(mouseX, mouseY);
      Render2DUtil.rect(context.getMatrices(), this.x, this.y, this.x + (float)this.width + 7.0F, this.y + (float)this.height - 0.5F, bgButton);
      Color color = ClickGui.getInstance().color.getValue();
      Render2DUtil.rect(context.getMatrices(), this.x, this.y, this.setting.getValue() <= this.min ? this.x : (float)((double)this.x + (double)((float)this.width + 7.0F) * this.partialMultiplier()), this.y + (float)this.height - 0.5F, ColorUtil.injectAlpha(color, ClickGui.getInstance().color.getValue().getAlpha()).getRGB());
      if (this.isListening) {
         String var10001 = this.currentString;
         this.drawString(var10001 + StringButton.getIdleSign(), (double)(this.x + 2.3F), (double)(this.y - 1.7F - (float)ClickGuiScreen.getInstance().getTextOffset(this.isHovering(mouseX, mouseY))), this.getState() ? -1 : textColor);
      } else {
         this.drawString(this.getName() + " " + String.valueOf(Formatting.GRAY) + this.setting.getValueFloat() + this.setting.getSuffix(), (double)(this.x + 2.3F), (double)(this.y - 1.7F - (float)ClickGuiScreen.getInstance().getTextOffset(this.isHovering(mouseX, mouseY))), -1);
      }

   }

   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (this.isHovering(mouseX, mouseY)) {
         sound();
         if (mouseButton == 0) {
            if (this.isListening) {
               this.toggle();
               return;
            }

            this.setSettingFromX(mouseX);
            this.drag = true;
            return;
         }

         if (mouseButton == 1) {
            this.toggle();
         }
      }

   }

   public boolean isHovering(int mouseX, int mouseY) {
      for(Component component : ClickGuiScreen.getInstance().getComponents()) {
         if (component.drag) {
            return false;
         }
      }

      return (float)mouseX >= this.getX() && (float)mouseX <= this.getX() + (float)this.getWidth() + 8.0F && (float)mouseY >= this.getY() && (float)mouseY <= this.getY() + (float)this.height - 1.0F;
   }

   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   public void onKeyTyped(char typedChar, int keyCode) {
      if (this.isListening) {
         this.setString(this.currentString + typedChar);
      }

   }

   public void onKeyPressed(int key) {
      if (this.isListening) {
         switch (key) {
            case 86:
               if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), 341)) {
                  try {
                     String clipboard = (String)Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
                     if (clipboard != null) {
                        this.setString(this.currentString + clipboard);
                     }
                  } catch (Exception var3) {
                  }
               }
               break;
            case 256:
               this.isListening = false;
               break;
            case 257:
            case 335:
               this.enterString();
               break;
            case 259:
               this.setString(StringButton.removeLastChar(this.currentString));
         }
      }

   }

   private void enterString() {
      if (!this.currentString.isEmpty() && this.isNumeric(this.currentString)) {
         this.setting.setValue(Double.parseDouble(this.currentString));
      } else {
         this.setting.setValue(this.setting.getDefaultValue());
      }

      this.onMouseClick();
   }

   public void setString(String newString) {
      this.currentString = newString;
   }

   private void dragSetting(int mouseX, int mouseY) {
      if (this.drag && this.isHovering(mouseX, mouseY) && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) == 1) {
         this.setSettingFromX(mouseX);
      } else {
         this.drag = false;
      }

   }

   public void toggle() {
      this.setString(String.valueOf(this.setting.getValueFloat()));
      this.isListening = !this.isListening;
   }

   public boolean getState() {
      return !this.isListening;
   }

   private void setSettingFromX(int mouseX) {
      double percent = (double)((float)mouseX - this.x) / ((double)this.width + 7.4);
      double result = Math.min(this.setting.getMin() + this.difference * percent, this.max);
      this.setting.setValue(result);
   }

   private double part() {
      return this.setting.getValue() - this.min;
   }

   private double partialMultiplier() {
      return Math.min(this.part() / this.difference, (double)1.0F);
   }

   private boolean isNumeric(String str) {
      return str.matches("-?\\d+(\\.\\d+)?");
   }
}
