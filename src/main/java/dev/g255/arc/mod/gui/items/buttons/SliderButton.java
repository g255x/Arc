package dev.g255.arc.mod.gui.items.buttons;

import dev.g255.arc.api.utils.math.Animation;
import dev.g255.arc.api.utils.math.Easing;
import dev.g255.arc.api.utils.render.ColorUtil;
import dev.g255.arc.api.utils.render.Render2DUtil;
import dev.g255.arc.mod.gui.ClickGuiScreen;
import dev.g255.arc.mod.gui.items.Component;
import dev.g255.arc.mod.modules.impl.client.ClickGui;
import dev.g255.arc.mod.modules.settings.ChineseTranslator;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.util.Formatting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.SelectionManager;
import org.lwjgl.glfw.GLFW;

public class SliderButton extends Button {
   private final double min;
   private final double max;
   private final double difference;
   public final SliderSetting setting;
   public boolean isListening;
   private String currentString = "";
   private boolean drag = false;
   private final Animation valueAnimation = new Animation();
   private final Animation hoverAnimation = new Animation();

   public SliderButton(SliderSetting setting) {
      super(setting.getDisplayName());
      this.setting = setting;
      this.min = setting.getMin();
      this.max = setting.getMax();
      this.difference = this.max - this.min;
      double initial = this.partialMultiplier();
      this.valueAnimation.from = initial;
      this.valueAnimation.to = initial;
   }

   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      this.dragSetting(mouseX, mouseY);
      boolean hovered = this.isHovering(mouseX, mouseY);
      double hoverProgress = this.hoverAnimation.get(hovered ? (double)1.0F : (double)0.0F, 130L, Easing.CubicInOut);
      int baseA = ClickGui.getInstance().alpha.getValueInt();
      int hoverA = ClickGui.getInstance().hoverAlpha.getValueInt();
      int a = Math.max(0, Math.min(255, (int)Math.round((double)baseA + (double)(hoverA - baseA) * hoverProgress)));
      Color defaultColor = ClickGui.getInstance().defaultColor.getValue();
      Color hoverColor = ClickGui.getInstance().hoverColor.getValue();
      Color unpressedFill = ColorUtil.fadeColor(new Color(defaultColor.getRed(), defaultColor.getGreen(), defaultColor.getBlue(), defaultColor.getAlpha()), hoverColor, hoverProgress);
      Render2DUtil.rect(context.getMatrices(), this.x, this.y, this.x + (float)this.width + 7.0F, this.y + (float)this.height - 0.5F, unpressedFill.getRGB());
      double baseDelay = this.getColorDelay();
      double fillProgress = this.valueAnimation.get(this.partialMultiplier(), 200L, Easing.CubicInOut);
      float filledX = (float)((double)this.x + (double)((float)this.width + 7.0F) * fillProgress);
      float h = (float)this.height - 0.5F;
      if (ClickGui.getInstance().colorMode.getValue() == ClickGui.ColorMode.Spectrum) {
         float fillW = Math.max(0.0F, filledX - this.x);
         Render2DUtil.drawLutRect(context.getMatrices(), this.x, this.y, fillW, h, ClickGui.getInstance().getSpectrumLutId(), ClickGui.getInstance().getSpectrumLutHeight(), a);
      } else {
         Color color = ClickGui.getInstance().getColor(baseDelay);
         Render2DUtil.rect(context.getMatrices(), this.x, this.y, filledX, this.y + (float)this.height - 0.5F, ColorUtil.injectAlpha(color, a).getRGB());
      }

      float textY = this.getCenteredTextY(this.y, (float)this.height - 0.5F);
      if (this.isListening) {
         String var10001 = this.currentString;
         this.drawString(var10001 + StringButton.getIdleSign(), (double)(this.x + 2.3F), (double)textY, this.getState() ? enableTextColor : defaultTextColor);
      } else if (hovered && InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340)) {
         this.drawString(ChineseTranslator.resetHint(false), (double)(this.x + 2.3F), (double)textY, enableTextColor);
      } else {
         this.drawString(this.getName() + " " + String.valueOf(Formatting.GRAY) + this.setting.getValueFloat() + this.setting.getSuffix(), (double)(this.x + 2.3F), (double)textY, enableTextColor);
      }

   }

   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (mouseButton == 0 && this.isHovering(mouseX, mouseY) && InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340)) {
         this.drag = false;
         this.isListening = false;
         this.currentString = "";
         this.setting.setValue(this.setting.getDefaultValue());
         sound();
      } else {
         if (this.isHovering(mouseX, mouseY)) {
            sound();
            if (mouseButton == 0) {
               if (this.isListening) {
                  this.toggle();
               } else {
                  this.setSettingFromX(mouseX);
                  this.drag = true;
               }
            } else if (mouseButton == 1) {
               this.toggle();
            }
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
                  String var10001 = this.currentString;
                  this.setString(var10001 + SelectionManager.getClipboard(mc));
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
      float var10001 = this.setting.getValueFloat();
      this.setString("" + var10001);
      this.isListening = !this.isListening;
   }

   public boolean getState() {
      return !this.isListening;
   }

   private void setSettingFromX(int mouseX) {
      double percent = (double)((float)mouseX - this.x) / ((double)this.width + 7.4);
      double result = this.setting.getMin() + this.difference * percent;
      result = Math.max(this.min, Math.min(result, this.max));
      this.setting.setValue(result);
   }

   private double part() {
      return this.setting.getValue() - this.min;
   }

   private double partialMultiplier() {
      return Math.max((double)0.0F, Math.min(this.part() / this.difference, (double)1.0F));
   }

   public void resetFillAnimation() {
      this.valueAnimation.from = (double)0.0F;
      this.valueAnimation.to = (double)0.0F;
   }

   private boolean isNumeric(String str) {
      return str.matches("-?\\d+(\\.\\d+)?");
   }
}
