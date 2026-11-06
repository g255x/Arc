package dev.g255.arc.mod.gui.items.buttons;

import dev.g255.arc.api.utils.math.Animation;
import dev.g255.arc.api.utils.math.Easing;
import dev.g255.arc.api.utils.render.ColorUtil;
import dev.g255.arc.api.utils.render.Render2DUtil;
import dev.g255.arc.mod.gui.ClickGuiScreen;
import dev.g255.arc.mod.gui.items.Component;
import dev.g255.arc.mod.modules.impl.client.ClickGui;
import dev.g255.arc.mod.modules.settings.ChineseTranslator;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import java.awt.Color;
import net.minecraft.util.Formatting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;

public class EnumButton extends Button {
   public final EnumSetting<?> setting;
   private boolean open;
   private final Animation hoverAnimation = new Animation();
   private final Animation textAnimation = new Animation();
   private String lastValue;

   public EnumButton(EnumSetting<?> setting) {
      super(setting.getDisplayName());
      this.setting = setting;
      this.textAnimation.from = (double)1.0F;
      this.textAnimation.to = (double)1.0F;
   }

   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      boolean hovered = this.isHovering(mouseX, mouseY);
      double hoverProgress = this.hoverAnimation.get(hovered ? (double)1.0F : (double)0.0F, 130L, Easing.CubicInOut);
      String valueName = this.setting.getValue().name();
      if (!valueName.equals(this.lastValue)) {
         this.lastValue = valueName;
         this.textAnimation.from = (double)0.0F;
         this.textAnimation.to = (double)0.0F;
      }

      double textProgress = this.textAnimation.get((double)1.0F, 160L, Easing.CubicInOut);
      double baseDelay = this.getColorDelay();
      int baseA = ClickGui.getInstance().alpha.getValueInt();
      int hoverA = ClickGui.getInstance().hoverAlpha.getValueInt();
      int accentA = Math.max(0, Math.min(255, (int)Math.round((double)baseA + (double)(hoverA - baseA) * hoverProgress)));
      Color defaultColor = ClickGui.getInstance().defaultColor.getValue();
      Color hoverColor = ClickGui.getInstance().hoverColor.getValue();
      Color unpressedFill = ColorUtil.fadeColor(new Color(defaultColor.getRed(), defaultColor.getGreen(), defaultColor.getBlue(), defaultColor.getAlpha()), hoverColor, hoverProgress);
      float w = (float)this.width + 7.0F;
      float h = (float)this.height - 0.5F;
      float wipe = w * (float)Math.max((double)0.0F, Math.min((double)1.0F, textProgress));
      if (ClickGui.getInstance().colorMode.getValue() == ClickGui.ColorMode.Spectrum) {
         Render2DUtil.rect(context.getMatrices(), this.x, this.y, this.x + w, this.y + h, unpressedFill.getRGB());
         if (wipe > 0.01F) {
            Render2DUtil.drawLutRect(context.getMatrices(), this.x, this.y, wipe, h, ClickGui.getInstance().getSpectrumLutId(), ClickGui.getInstance().getSpectrumLutHeight(), accentA);
         }
      } else {
         Color accent = ClickGui.getInstance().getColor(baseDelay);
         Color pressedFill = ColorUtil.injectAlpha(accent, accentA);
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
         this.drawString(ChineseTranslator.resetHint(this.setting.getName().equalsIgnoreCase("Page")), (double)(this.x + 2.3F), (double)textY, enableTextColor);
      } else {
         String var10001 = this.getName();
         this.drawString(var10001 + " " + String.valueOf(Formatting.GRAY) + ChineseTranslator.enumValue(valueName), (double)(this.x + 2.3F), (double)textY, hovered ? enableTextColor : defaultTextColor);
      }

      if (this.open) {
         int y = (int)this.y;

         for(Enum e : (Enum[])this.setting.getValue().getDeclaringClass().getEnumConstants()) {
            String s = ChineseTranslator.enumValue(e.name());
            float optionY = this.getCenteredTextY((float)y + (float)this.height + 1.0F, 11.0F);
            this.drawString(s, (double)((float)this.width / 2.0F - (float)this.getWidth(s) / 2.0F + 2.0F + this.x), (double)optionY, valueName.equals(e.name()) ? enableTextColor : defaultTextColor);
            y += 11;
         }
      }

   }

   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (mouseButton == 0 && this.isHovering(mouseX, mouseY) && InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340)) {
         if (!this.setting.getName().equalsIgnoreCase("Page")) {
            Enum defaultValue = this.setting.getDefaultValue();
            this.setting.setEnumValue(defaultValue.name());
            sound();
         } else {
            boolean resetPage = false;

            for(Component component : ClickGuiScreen.getInstance().getComponents()) {
               for(ModuleButton moduleButton : component.getItems()) {
                  if (moduleButton.getModule().getSettings().contains(this.setting)) {
                     moduleButton.resetPageSettingsToDefault(this.setting);
                     resetPage = true;
                     break;
                  }
               }

               if (resetPage) {
                  break;
               }
            }

            sound();
         }
      } else {
         super.mouseClicked(mouseX, mouseY, mouseButton);
         if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
            this.open = !this.open;
            sound();
         } else if (mouseButton == 0 && this.open) {
            int y = (int)this.y;

            for(Enum o : (Enum[])this.setting.getValue().getDeclaringClass().getEnumConstants()) {
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
