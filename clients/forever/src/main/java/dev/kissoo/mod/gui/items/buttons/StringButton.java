package dev.kissoo.mod.gui.items.buttons;

import dev.kissoo.api.utils.math.Timer;
import dev.kissoo.api.utils.render.ColorUtil;
import dev.kissoo.api.utils.render.Render2DUtil;
import dev.kissoo.mod.gui.ClickGuiScreen;
import dev.kissoo.mod.modules.impl.client.ClickGui;
import dev.kissoo.mod.modules.settings.impl.StringSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.util.Formatting;

import java.awt.*;

public class StringButton extends Button {
   private static final Timer idleTimer = new Timer();
   private static boolean idle;
   private final StringSetting setting;
   public boolean isListening;
   private String currentString = "";

   public StringButton(StringSetting setting) {
      super(setting.getName());
      this.setting = setting;
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

      if (this.isListening) {
         this.drawString(this.currentString + getIdleSign(),
                 this.x + 2.3F,
                 this.y - 1.7F - ClickGuiScreen.getInstance().getTextOffset(),
                 this.getState() ? Button.enableTextColor : Button.defaultTextColor);
      } else {
         this.drawString(this.setting.getName() + ": " + Formatting.GRAY + this.setting.getValue(),
                 this.x + 2.3F,
                 this.y - 1.7F - ClickGuiScreen.getInstance().getTextOffset(),
                 this.getState() ? Button.enableTextColor : Button.defaultTextColor);
      }
   }



   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      super.mouseClicked(mouseX, mouseY, mouseButton);
   }

   @Override
   public void onKeyTyped(char typedChar, int keyCode) {
      if (this.isListening) {
         this.setString(this.currentString + typedChar);
      }
   }

   @Override
   public void onKeyPressed(int key) {
      if (this.isListening) {
         switch (key) {
            case 86:
               if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), 341)) {
                  this.setString(this.currentString + SelectionManager.getClipboard(mc));
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

   @Override
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

   @Override
   public void toggle() {
      this.setString(this.setting.getValue());
      this.isListening = !this.isListening;
   }

   @Override
   public boolean getState() {
      return !this.isListening;
   }

   public void setString(String newString) {
      this.currentString = newString;
   }
}