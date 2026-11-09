package dev.g255.arc.mod.gui.items;

import dev.g255.arc.api.utils.math.Animation;
import dev.g255.arc.api.utils.math.Easing;
import dev.g255.arc.core.impl.FontManager;
import dev.g255.arc.mod.Mod;
import dev.g255.arc.mod.modules.impl.client.ClickGui;
import java.awt.Color;
import java.util.Objects;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;

public class Item extends Mod {
   public static DrawContext context;
   protected float x;
   protected float y;
   protected int width;
   protected int height;
   private boolean hidden;
   private final Animation visibleHeightAnimation = new Animation();

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

   /** 该行对应的配置项；非配置项（类型栏、按钮）返回 null，用于悬停显示说明 */
   public dev.g255.arc.mod.modules.settings.Setting getSetting() {
      return null;
   }

   /** 悬停在枚举展开的某个选项上时返回该选项名，否则返回 null */
   public String getHoveredOption(int mouseX, int mouseY) {
      return null;
   }

   public boolean isHidden() {
      return this.hidden;
   }

   public void setHidden(boolean hidden) {
      this.hidden = hidden;
   }

   public double getVisibleHeight() {
      double target = this.hidden ? (double)0.0F : (double)this.getHeight();
      return this.visibleHeightAnimation.get(target, 200L, Easing.CubicInOut);
   }

   protected double getColorDelay() {
      return (double)this.y / (double)10.0F;
   }

   protected void drawString(String text, double x, double y, Color color) {
      this.drawString(text, x, y, color.hashCode());
   }

   protected void drawString(String text, double x, double y, int color) {
      boolean shadow = FontManager.isShadowEnabled();
      if (FontManager.isCustomFontEnabled()) {
         FontManager.ui.drawString(context.getMatrices(), text, (double)((int)x), (double)((int)y), color, shadow);
      } else {
         context.drawText(mc.textRenderer, text, (int)x, (int)y, color, shadow);
      }

   }

   protected int getFontHeight() {
      if (FontManager.isCustomFontEnabled()) {
         return (int)FontManager.ui.getFontHeight();
      } else {
         Objects.requireNonNull(mc.textRenderer);
         return 9;
      }
   }

   protected float getCenteredTextY(float baseY, float boxHeight) {
      return baseY + (boxHeight - (float)this.getFontHeight()) / 2.0F + (float)ClickGui.getInstance().textOffset.getValueInt();
   }

   protected int getWidth(String s) {
      return FontManager.isCustomFontEnabled() ? (int)FontManager.ui.getWidth(s) : mc.textRenderer.getWidth(s);
   }
}
