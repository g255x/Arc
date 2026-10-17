package dev.baimaodaxia.mod.gui.items.buttons;

import dev.baimaodaxia.mod.gui.ClickGuiScreen;
import dev.baimaodaxia.mod.modules.settings.impl.BindSetting;
import net.minecraft.util.Formatting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;

public class BindButton extends Button {
   private final BindSetting setting;
   public boolean isListening;

   public BindButton(BindSetting setting) {
      super(setting.getName());
      this.setting = setting;
   }

   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      boolean hovering = this.isHovering(mouseX, mouseY);
      if (this.isListening) {
         this.drawString("Press a Key...", (double)(this.x + 2.3F), (double)(this.y - 1.7F - (float)ClickGuiScreen.getInstance().getTextOffset(hovering)), enableTextColor);
      } else {
         String str = this.setting.getKeyString();
         if (!this.isListening && hovering && InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340) && this.setting.getName().equals("Key")) {
            if (this.setting.isHoldEnable()) {
               this.drawString("§7Toggle/§fHold", (double)(this.x + 2.3F), (double)(this.y - 1.7F - (float)ClickGuiScreen.getInstance().getTextOffset(hovering)), enableTextColor);
            } else {
               this.drawString("§fToggle§7/Hold", (double)(this.x + 2.3F), (double)(this.y - 1.7F - (float)ClickGuiScreen.getInstance().getTextOffset(hovering)), enableTextColor);
            }
         } else {
            String var10001 = this.setting.getName();
            this.drawString(var10001 + " " + String.valueOf(Formatting.GRAY) + str, (double)(this.x + 2.3F), (double)(this.y - 1.7F - (float)ClickGuiScreen.getInstance().getTextOffset(hovering)), this.getState() ? enableTextColor : textColor);
         }

      }
   }

   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
         if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340) && this.setting.getName().equals("Key")) {
            this.setting.setHoldEnable(!this.setting.isHoldEnable());
            sound();
         } else {
            this.onMouseClick();
         }
      } else {
         if (this.isListening) {
            this.setting.setValue(-mouseButton - 2);
            this.onMouseClick();
         }

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
