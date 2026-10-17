package dev.kissoo.mod.gui.items.buttons;

import dev.kissoo.api.utils.render.ColorUtil;
import dev.kissoo.api.utils.render.Render2DUtil;
import dev.kissoo.mod.gui.ClickGuiScreen;
import dev.kissoo.mod.modules.impl.client.ClickGui;
import dev.kissoo.mod.modules.settings.impl.BindSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Formatting;

import java.awt.*;

public class BindButton extends Button {
   public final BindSetting setting;
   public boolean isListening;

   public BindButton(BindSetting setting) {
      super(setting.getName());
      this.setting = setting;
   }

   @Override
   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      Color color = ClickGui.getInstance().enableBGColor.getValue();
      boolean hovered = this.isHovering(mouseX, mouseY);
      int bg = this.getState()
              ? (hovered ? Button.defaultBGColor : Button.hoverBGColor)
              : (hovered
              ? ColorUtil.injectAlpha(color, Math.min(255, color.getAlpha() + 40)).getRGB()
              : color.getRGB());

      Render2DUtil.rect(context.getMatrices(),
              this.x, this.y,
              this.x + this.width + 7.0F,
              this.y + this.height - 0.5F,
              bg);

      if (this.isListening) {
         this.drawString("Press a Key...",
                 this.x + 2.3F,
                 this.y - 1.7F - ClickGuiScreen.getInstance().getTextOffset(),
                 Button.enableTextColor);
      } else {
         String str = this.setting.getKeyString();
         if (this.isListening
                 || !hovered
                 || !InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340)
                 || !this.setting.getName().equals("Key")) {
            this.drawString(
                    this.setting.getName() + " " + Formatting.GRAY + str,
                    this.x + 2.3F,
                    this.y - 1.7F - ClickGuiScreen.getInstance().getTextOffset(),
                    this.getState() ? Button.enableTextColor : Button.defaultTextColor);
         } else if (this.setting.isHoldEnable()) {
            this.drawString("\u00a77Toggle/\u00a7fHold",
                    this.x + 2.3F,
                    this.y - 1.7F - ClickGuiScreen.getInstance().getTextOffset(),
                    Button.enableTextColor);
         } else {
            this.drawString("\u00a7fToggle\u00a77/Hold",
                    this.x + 2.3F,
                    this.y - 1.7F - ClickGuiScreen.getInstance().getTextOffset(),
                    Button.enableTextColor);
         }
      }
   }

   @Override
   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
         if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340) && this.setting.getName().equals("Key")) {
            this.setting.setHoldEnable(!this.setting.isHoldEnable());
            sound();
         } else {
            this.onMouseClick();
         }
      } else if (this.isListening) {
         this.setting.setValue(-mouseButton - 2);
         this.onMouseClick();
      }
   }

   @Override
   public void onKeyPressed(int key) {
      if (this.isListening) {
         this.setting.setValue(key);
         if (this.setting.getKeyString().equalsIgnoreCase("DELETE")) {
            this.setting.setValue(-1);
         }
         this.isListening = false;
         sound();
      }
   }

   public boolean isListeningForKey() {
      return this.isListening;
   }

   @Override
   public void toggle() {
      this.isListening = !this.isListening;
   }

   @Override
   public boolean getState() {
      return !this.isListening;
   }
}