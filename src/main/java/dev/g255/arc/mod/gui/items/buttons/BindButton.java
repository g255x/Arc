package dev.g255.arc.mod.gui.items.buttons;

import dev.g255.arc.api.utils.math.Animation;
import dev.g255.arc.api.utils.math.Easing;
import dev.g255.arc.api.utils.render.ColorUtil;
import dev.g255.arc.api.utils.render.Render2DUtil;
import dev.g255.arc.mod.modules.impl.client.ClickGui;
import dev.g255.arc.mod.modules.settings.ChineseTranslator;
import dev.g255.arc.mod.modules.settings.impl.BindSetting;
import java.awt.Color;
import net.minecraft.util.Formatting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;

public class BindButton extends Button {
   private final BindSetting setting;
   private final Animation holdAnimation = new Animation();
   private final Animation hoverAnimation = new Animation();
   public boolean isListening;

   public BindButton(BindSetting setting) {
      super(setting.getDisplayName());
      this.setting = setting;
      double initial = setting.isHoldEnable() ? (double)1.0F : (double)0.0F;
      this.holdAnimation.from = initial;
      this.holdAnimation.to = initial;
   }

   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      boolean hovered = this.isHovering(mouseX, mouseY);
      double hoverProgress = this.hoverAnimation.get(hovered ? (double)1.0F : (double)0.0F, 130L, Easing.CubicInOut);
      double holdProgress = this.holdAnimation.get(this.setting.isHoldEnable() ? (double)1.0F : (double)0.0F, 130L, Easing.CubicInOut);
      double baseDelay = this.getColorDelay();
      int baseA = ClickGui.getInstance().alpha.getValueInt();
      int hoverA = ClickGui.getInstance().hoverAlpha.getValueInt();
      int accentA = Math.max(0, Math.min(255, (int)Math.round((double)baseA + (double)(hoverA - baseA) * hoverProgress)));
      Color defaultColor = ClickGui.getInstance().defaultColor.getValue();
      Color hoverColor = ClickGui.getInstance().hoverColor.getValue();
      Color unpressedFill = ColorUtil.fadeColor(new Color(defaultColor.getRed(), defaultColor.getGreen(), defaultColor.getBlue(), defaultColor.getAlpha()), hoverColor, hoverProgress);
      Color pressedFill = ColorUtil.injectAlpha(ClickGui.getInstance().getColor(baseDelay), accentA);
      float w = (float)this.width + 7.0F;
      float h = (float)this.height - 0.5F;
      if (ClickGui.getInstance().colorMode.getValue() == ClickGui.ColorMode.Spectrum) {
         Render2DUtil.rect(context.getMatrices(), this.x, this.y, this.x + w, this.y + h, unpressedFill.getRGB());
         float wipeW = w * (float)Math.max((double)0.0F, Math.min((double)1.0F, holdProgress));
         if (wipeW > 0.01F) {
            Render2DUtil.drawLutRect(context.getMatrices(), this.x, this.y, wipeW, h, ClickGui.getInstance().getSpectrumLutId(), ClickGui.getInstance().getSpectrumLutHeight(), accentA);
         }
      } else {
         float wipe = w * (float)Math.max((double)0.0F, Math.min((double)1.0F, holdProgress));
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
         this.drawString(ChineseTranslator.bindPrompt(), (double)(this.x + 2.3F), (double)textY, enableTextColor);
      } else {
         String str = this.setting.getKeyString();
         if (!this.isListening && this.isHovering(mouseX, mouseY) && InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340)) {
            if (this.setting.getName().equals("Key")) {
               this.drawString(ChineseTranslator.toggleHold(this.setting.isHoldEnable()), (double)(this.x + 2.3F), (double)textY, enableTextColor);
            } else {
               this.drawString(ChineseTranslator.resetHint(false), (double)(this.x + 2.3F), (double)textY, enableTextColor);
            }
         } else {
            String var10001 = this.getName();
            this.drawString(var10001 + " " + String.valueOf(Formatting.GRAY) + str, (double)(this.x + 2.3F), (double)textY, this.getState() ? enableTextColor : defaultTextColor);
         }
      }

   }

   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
         if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340)) {
            if (this.setting.getName().equals("Key")) {
               this.setting.setHoldEnable(!this.setting.isHoldEnable());
               sound();
            } else {
               this.setting.setValue(this.setting.getDefaultValue());
               this.setting.setHoldEnable(false);
               this.setting.setPressed(false);
               this.setting.holding = false;
               sound();
            }
         } else {
            this.onMouseClick();
         }
      } else if (this.isListening) {
         this.setting.setValue(-mouseButton - 2);
         this.onMouseClick();
      }

   }

   public void onKeyPressed(int key) {
      if (this.isListening) {
         this.setting.setValue(key);
         if (this.setting.getKeyString().equalsIgnoreCase("DELETE")) {
            this.setting.setValue(-1);
         }

         this.onMouseClick();
      }

   }

   public void toggle() {
      this.isListening = !this.isListening;
   }

   public boolean getState() {
      return !this.isListening;
   }
}
