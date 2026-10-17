package dev.kissoo.mod.gui.items;

import hoprc.api.core.impl.FontManager;
import dev.kissoo.mod.Mod;
import dev.kissoo.mod.modules.impl.client.ClickGui;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

import java.awt.*;

public class Item extends Mod {
   public static DrawContext context;
   protected float x;
   protected float y;
   protected int width;
   protected int height;
   private boolean hidden;

   public Item(String name) {
      super(name);
   }

   public void setLocation(float x, float y) {
      this.x = x;
      this.y = y;
   }

   public static void sound() {
      if (ClickGui.getInstance().sound.getValue()) {
         mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, ClickGui.getInstance().soundPitch.getValueFloat()));
      }
   }

   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
   }

   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
   }

   public void mouseReleased(int mouseX, int mouseY, int releaseButton) {
   }

   public void update() {
   }

   public void onKeyTyped(char typedChar, int keyCode) {
   }

   public void onKeyPressed(int key) {
   }

   public float getX() {
      return this.x;
   }

   public float getY() {
      return this.y;
   }

   public int getWidth() {
      return this.width;
   }

   public void setWidth(int width) {
      this.width = width;
   }

   public int getHeight() {
      return this.height;
   }

   public void setHeight(int height) {
      this.height = height;
   }

   public boolean isHidden() {
      return this.hidden;
   }

   public void setHidden(boolean hidden) {
      this.hidden = hidden;
   }

   protected void drawString(String text, double x, double y, Color color) {
      this.drawString(text, x, y, color.getRGB() | (color.getAlpha() << 24));
   }
   protected void drawString(String text, double x, double y, int color) {
      if (ClickGui.getInstance().font.getValue()) {
         int rawAlpha = (color >> 24) & 0xFF;
         float alpha = (rawAlpha == 0) ? 1.0f : rawAlpha / 255.0f;

         FontManager.get().drawString(
                 context.getMatrices(),
                 text,
                 (float) x,
                 (float) y,
                 ((color >> 16) & 0xFF) / 255.0f,
                 ((color >> 8)  & 0xFF) / 255.0f,
                 ( color        & 0xFF) / 255.0f,
                 alpha,
                 ClickGui.getInstance().shadow.getValue()
         );
      } else {
         context.drawText(mc.textRenderer, text, (int) x, (int) y, color,
                 ClickGui.getInstance().shadow.getValue());
      }
   }

   protected int getFontHeight() {
      return ClickGui.getInstance().font.getValue()
              ? (int) FontManager.get().getFontHeight()
              : 9;
   }

   protected int getWidth(String s) {
      return ClickGui.getInstance().font.getValue()
              ? (int) FontManager.get().getWidth(s)
              : mc.textRenderer.getWidth(s);
   }
}
