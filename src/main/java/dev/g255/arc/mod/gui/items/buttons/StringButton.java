package dev.g255.arc.mod.gui.items.buttons;

import dev.g255.arc.api.utils.math.Animation;
import dev.g255.arc.api.utils.math.Easing;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.api.utils.render.ColorUtil;
import dev.g255.arc.api.utils.render.Render2DUtil;
import dev.g255.arc.mod.modules.impl.client.ClickGui;
import dev.g255.arc.mod.modules.settings.ChineseTranslator;
import dev.g255.arc.mod.modules.settings.impl.StringSetting;
import java.awt.Color;
import net.minecraft.util.Formatting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.SelectionManager;

public class StringButton extends Button {
   private static final Timer idleTimer = new Timer();
   private static boolean idle;
   private final StringSetting setting;
   public boolean isListening;
   private String currentString = "";
   private final Animation fillAnimation = new Animation();
   private final Animation hoverAnimation = new Animation();

   public StringButton(StringSetting setting) {
      super(setting.getDisplayName());
      this.setting = setting;
      double initial = this.getState() ? (double)1.0F : (double)0.0F;
      this.fillAnimation.from = initial;
      this.fillAnimation.to = initial;
   }

   public static String removeLastChar(String str) {
      String output = "";
      if (str != null && !str.isEmpty()) {
         output = str.substring(0, str.length() - 1);
      }

      return output;
   }

   public static String getIdleSign() {
      if (idleTimer.passed(500L)) {
         idle = !idle;
         idleTimer.reset();
      }

      return idle ? "_" : "";
   }

   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      boolean hovered = this.isHovering(mouseX, mouseY);
      double hoverProgress = this.hoverAnimation.get(hovered ? (double)1.0F : (double)0.0F, 130L, Easing.CubicInOut);
      double fillProgress = this.fillAnimation.get(this.getState() ? (double)1.0F : (double)0.0F, 160L, Easing.CubicInOut);
      double baseDelay = this.getColorDelay();
      int baseA = ClickGui.getInstance().alpha.getValueInt();
      int hoverA = ClickGui.getInstance().hoverAlpha.getValueInt();
      int accentA = Math.max(0, Math.min(255, (int)Math.round((double)baseA + (double)(hoverA - baseA) * hoverProgress)));
      Color defaultColor = ClickGui.getInstance().defaultColor.getValue();
      Color hoverColor = ClickGui.getInstance().hoverColor.getValue();
      Color unpressedFill = ColorUtil.fadeColor(new Color(defaultColor.getRed(), defaultColor.getGreen(), defaultColor.getBlue(), defaultColor.getAlpha()), hoverColor, hoverProgress);
      float w = (float)this.width + 7.0F;
      float h = (float)this.height - 0.5F;
      float wipe = w * (float)Math.max((double)0.0F, Math.min((double)1.0F, fillProgress));
      if (ClickGui.getInstance().colorMode.getValue() == ClickGui.ColorMode.Spectrum) {
         Render2DUtil.rect(context.getMatrices(), this.x, this.y, this.x + w, this.y + h, unpressedFill.getRGB());
         if (wipe > 0.01F) {
            Render2DUtil.drawLutRect(context.getMatrices(), this.x, this.y, wipe, h, ClickGui.getInstance().getSpectrumLutId(), ClickGui.getInstance().getSpectrumLutHeight(), accentA);
         }
      } else {
         Color pressedFill = ColorUtil.injectAlpha(ClickGui.getInstance().getColor(baseDelay), accentA);
         if (wipe <= 0.01F) {
            Render2DUtil.rect(context.getMatrices(), this.x, this.y, this.x + w, this.y + h, unpressedFill.getRGB());
         } else if (wipe >= w - 0.01F) {
            Render2DUtil.rect(context.getMatrices(), this.x, this.y, this.x + w, this.y + h, pressedFill.getRGB());
         } else {
            Render2DUtil.rect(context.getMatrices(), this.x, this.y, this.x + wipe, this.y + h, pressedFill.getRGB());
            Render2DUtil.rect(context.getMatrices(), this.x + wipe, this.y, this.x + w, this.y + h, unpressedFill.getRGB());
         }
      }

      float textY = this.getCenteredTextY(this.y, (float)this.height - 0.5F);
      if (this.isListening) {
         String var10001 = this.currentString;
         this.drawString(var10001 + getIdleSign(), (double)(this.x + 2.3F), (double)textY, this.getState() ? enableTextColor : defaultTextColor);
      } else if (this.isHovering(mouseX, mouseY) && InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340)) {
         this.drawString(ChineseTranslator.resetHint(false), (double)(this.x + 2.3F), (double)textY, enableTextColor);
      } else {
         String var23 = this.getName();
         this.drawString(var23 + ": " + String.valueOf(Formatting.GRAY) + this.setting.getValue(), (double)(this.x + 2.3F), (double)textY, this.getState() ? enableTextColor : defaultTextColor);
      }

   }

   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (mouseButton == 0 && this.isHovering(mouseX, mouseY) && InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340)) {
         this.isListening = false;
         this.currentString = "";
         this.setting.setValue(this.setting.getDefaultValue());
         sound();
      } else {
         super.mouseClicked(mouseX, mouseY, mouseButton);
      }
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
               this.setString(removeLastChar(this.currentString));
         }
      }

   }

   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   private void enterString() {
      if (this.currentString.isEmpty()) {
         this.setting.setValue(this.setting.getDefaultValue());
      } else {
         this.setting.setValue(this.currentString);
      }

      this.onMouseClick();
   }

   public void toggle() {
      this.setString(this.setting.getValue());
      this.isListening = !this.isListening;
   }

   public boolean getState() {
      return !this.isListening;
   }

   public void setString(String newString) {
      this.currentString = newString;
   }
}
