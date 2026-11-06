package dev.g255.arc.mod.gui.items.buttons;

import dev.g255.arc.api.utils.math.Animation;
import dev.g255.arc.api.utils.math.Easing;
import dev.g255.arc.api.utils.render.ColorUtil;
import dev.g255.arc.api.utils.render.Render2DUtil;
import dev.g255.arc.mod.gui.ClickGuiScreen;
import dev.g255.arc.mod.gui.items.Component;
import dev.g255.arc.mod.modules.impl.client.ClickGui;
import dev.g255.arc.mod.modules.settings.ChineseTranslator;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;

public class BooleanButton extends Button {
   private final BooleanSetting setting;
   private final Animation toggleAnimation = new Animation();
   private final Animation hoverAnimation = new Animation();

   public BooleanButton(BooleanSetting setting) {
      super(setting.getDisplayName());
      this.setting = setting;
      double initial = setting.getValue() ? (double)1.0F : (double)0.0F;
      this.toggleAnimation.from = initial;
      this.toggleAnimation.to = initial;
   }

   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      boolean hovered = this.isHovering(mouseX, mouseY);
      double hoverProgress = this.hoverAnimation.get(hovered ? (double)1.0F : (double)0.0F, 130L, Easing.CubicInOut);
      double toggleProgress = this.toggleAnimation.get(this.getState() ? (double)1.0F : (double)0.0F, 160L, Easing.CubicInOut);
      double baseDelay = this.getColorDelay();
      int baseA = ClickGui.getInstance().alpha.getValueInt();
      int hoverA = ClickGui.getInstance().hoverAlpha.getValueInt();
      int accentA = Math.max(0, Math.min(255, (int)Math.round((double)baseA + (double)(hoverA - baseA) * hoverProgress)));
      Color defaultColor = ClickGui.getInstance().defaultColor.getValue();
      Color hoverColor = ClickGui.getInstance().hoverColor.getValue();
      Color unpressedFill = ColorUtil.fadeColor(new Color(defaultColor.getRed(), defaultColor.getGreen(), defaultColor.getBlue(), defaultColor.getAlpha()), hoverColor, hoverProgress);
      float w = (float)this.width + 7.0F;
      float h = (float)this.height - 0.5F;
      if (ClickGui.getInstance().colorMode.getValue() == ClickGui.ColorMode.Spectrum) {
         Render2DUtil.rect(context.getMatrices(), this.x, this.y, this.x + w, this.y + h, unpressedFill.getRGB());
         float wipeW = w * (float)Math.max((double)0.0F, Math.min((double)1.0F, toggleProgress));
         if (wipeW > 0.01F) {
            Render2DUtil.drawLutRect(context.getMatrices(), this.x, this.y, wipeW, h, ClickGui.getInstance().getSpectrumLutId(), ClickGui.getInstance().getSpectrumLutHeight(), accentA);
         }
      } else {
         Color accent = ClickGui.getInstance().getColor(baseDelay);
         Color pressedFill = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), accentA);
         float wipe = w * (float)Math.max((double)0.0F, Math.min((double)1.0F, toggleProgress));
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
      if (hovered && InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340)) {
         this.drawString(ChineseTranslator.resetHint(false), (double)(this.x + 2.3F), (double)textY, enableTextColor);
      } else {
         this.drawString(this.getName(), (double)(this.x + 2.3F), (double)textY, hovered ? enableTextColor : defaultTextColor);
      }

      if (this.setting.hasParent()) {
         this.drawString(this.setting.isOpen() ? "-" : "+", (double)(this.x + (float)this.width - 1.0F), (double)textY, ClickGui.getInstance().gear.getValue());
      }

   }

   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (mouseButton == 0 && this.isHovering(mouseX, mouseY) && InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340)) {
         if (this.setting.hasParent()) {
            boolean resetChildren = false;

            for(Component component : ClickGuiScreen.getInstance().getComponents()) {
               for(ModuleButton moduleButton : component.getItems()) {
                  if (moduleButton.getModule().getSettings().contains(this.setting)) {
                     moduleButton.resetChildSettingsToDefault(this.setting);
                     resetChildren = true;
                     break;
                  }
               }

               if (resetChildren) {
                  break;
               }
            }
         }

         this.setting.setValue(this.setting.getDefaultValue());
         sound();
      } else {
         super.mouseClicked(mouseX, mouseY, mouseButton);
         if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
            sound();
            this.setting.setOpen(!this.setting.isOpen());
         }

      }
   }

   public void toggle() {
      this.setting.setValue(!this.setting.getValue());
   }

   public boolean getState() {
      return this.setting.getValue();
   }
}
