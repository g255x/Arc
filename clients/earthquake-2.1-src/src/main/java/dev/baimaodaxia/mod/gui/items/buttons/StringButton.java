package dev.baimaodaxia.mod.gui.items.buttons;

import dev.baimaodaxia.api.utils.math.Timer;
import dev.baimaodaxia.api.utils.render.ColorUtil;
import dev.baimaodaxia.api.utils.render.Render2DUtil;
import dev.baimaodaxia.mod.gui.ClickGuiScreen;
import dev.baimaodaxia.mod.modules.impl.client.ClickGui;
import dev.baimaodaxia.mod.modules.settings.impl.StringSetting;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import net.minecraft.util.Formatting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
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
      if (this.isListening) {
         String var10001 = this.currentString;
         this.drawString(var10001 + getIdleSign(), (double)(this.x + 2.3F), (double)(this.y - 1.7F - (float)ClickGuiScreen.getInstance().getTextOffset(this.isHovering(mouseX, mouseY))), this.getState() ? enableTextColor : textColor);
      } else {
         String var12 = this.setting.getName();
         this.drawString(var12 + ": " + String.valueOf(Formatting.GRAY) + this.setting.getValue(), (double)(this.x + 2.3F), (double)(this.y - 1.7F - (float)ClickGuiScreen.getInstance().getTextOffset(this.isHovering(mouseX, mouseY))), this.getState() ? enableTextColor : textColor);
      }

   }

   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      super.mouseClicked(mouseX, mouseY, mouseButton);
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
