package dev.g255.arc.mod.modules.impl.client;

import dev.g255.arc.mod.gui.HudElement;
import dev.g255.arc.mod.gui.clickgui.ClickGuiScreen;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.ColorSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.item.ItemStack;
import net.minecraft.client.gui.DrawContext;

public class BackPackHUD extends Module implements HudElement {
   public static BackPackHUD INSTANCE;
   private final BooleanSetting enable = this.add(new BooleanSetting("Enable", true));
   private final SliderSetting x = this.add(new SliderSetting("X", 10, 0, 1000));
   private final SliderSetting y = this.add(new SliderSetting("Y", 50, 0, 1000));
   private final SliderSetting scale = this.add(new SliderSetting("Scale", (double)1.0F, 0.1, (double)3.0F, 0.1));
   private final BooleanSetting showCount = this.add(new BooleanSetting("ShowCount", true));
   private final BooleanSetting borderEnable = this.add(new BooleanSetting("Border", true));
   private final ColorSetting borderColor = this.add(new ColorSetting("BorderColor", new Color(255, 255, 255, 200)));
   private final SliderSetting borderPadding = this.add(new SliderSetting("BorderPadding", (double)2.0F, (double)0.0F, (double)10.0F, (double)1.0F));
   private int cachedWidth = 174;
   private int cachedHeight = 60;

   public BackPackHUD() {
      super("BackPackHUD", Module.Category.HUD);
      this.setChinese("背包显示");
      INSTANCE = this;
   }

   public void onEnable() {
      super.onEnable();
   }

   public void onRender2D(DrawContext ServerConfigException, float f) {
      if (this.enable.getValue() && !nullCheck() && !ClickGuiScreen.isEditingHud()) {
         this.renderAt(ServerConfigException);
      }
   }

   private void renderAt(DrawContext ServerConfigException) {
      if (mc.player != null) {
         float f2 = (float)this.scale.getValue();
         if (f2 <= 0.001F) {
            f2 = 0.1F;
         }

         int n = this.x.getValueInt();
         int n2 = this.y.getValueInt();
         ItemStack[] ItemStackArray = this.getBackpackItems();
         ServerConfigException.getMatrices().push();
         ServerConfigException.getMatrices().translate((double)n, (double)n2, (double)0.0F);
         ServerConfigException.getMatrices().scale(f2, f2, 1.0F);
         if (this.borderEnable.getValue()) {
            this.drawBackpackBorder(ServerConfigException);
         }

         for(int i = 0; i < 3; ++i) {
            for(int j = 0; j < 9; ++j) {
               int n3 = i * 9 + j;
               int n4 = j * 19;
               int n5 = i * 19;
               this.renderSingleItem(ServerConfigException, ItemStackArray[n3], n4, n5);
            }
         }

         ServerConfigException.getMatrices().pop();
         int pad = this.borderEnable.getValue() ? this.borderPadding.getValueInt() : 0;
         this.cachedWidth = Math.max(1, Math.round((170.0F + (float)(2 * pad)) * f2));
         this.cachedHeight = Math.max(1, Math.round((56.0F + (float)(2 * pad)) * f2));
      }
   }

   private void drawBackpackBorder(DrawContext ServerConfigException) {
      Color color = this.borderColor.getValue();
      int n = color.getRGB();
      int n2 = this.borderPadding.getValueInt();
      int n3 = -n2;
      int n4 = -n2;
      int n5 = 170 + 2 * n2;
      int n6 = 56 + 2 * n2;
      this.drawBorderLine(ServerConfigException, n3, n4, n3 + n5, n4, n);
      this.drawBorderLine(ServerConfigException, n3, n4 + n6, n3 + n5, n4 + n6, n);
      this.drawBorderLine(ServerConfigException, n3, n4, n3, n4 + n6, n);
      this.drawBorderLine(ServerConfigException, n3 + n5, n4, n3 + n5, n4 + n6, n);
   }

   private void drawBorderLine(DrawContext ServerConfigException, int n, int n2, int n3, int n4, int n5) {
      if (n == n3) {
         ServerConfigException.fill(n, Math.min(n2, n4), n + 1, Math.max(n2, n4) + 1, n5);
      } else if (n2 == n4) {
         ServerConfigException.fill(Math.min(n, n3), n2, Math.max(n, n3) + 1, n2 + 1, n5);
      }

   }

   private ItemStack[] getBackpackItems() {
      ItemStack[] ItemStackArray = new ItemStack[27];
      if (mc.player == null) {
         return ItemStackArray;
      } else {
         for(int i = 0; i < 27; ++i) {
            ItemStackArray[i] = mc.player.getInventory().getStack(9 + i);
         }

         return ItemStackArray;
      }
   }

   private void renderSingleItem(DrawContext ServerConfigException, ItemStack itemStack, int n, int n2) {
      if (!itemStack.isEmpty()) {
         ServerConfigException.drawItem(itemStack, n, n2);
         if (this.showCount.getValue()) {
            ServerConfigException.drawItemInSlot(mc.textRenderer, itemStack, n, n2);
         }

      }
   }

   public boolean isEnabled() {
      return this.enable.getValue();
   }

   public float getScale() {
      float f = (float)this.scale.getValue();
      return f <= 0.001F ? 0.1F : f;
   }

   public int getX() {
      return this.x.getValueInt();
   }

   public int getY() {
      return this.y.getValueInt();
   }

   public void setX(int nx) {
      this.x.setValue((double)nx);
   }

   public void setY(int ny) {
      this.y.setValue((double)ny);
   }

   public int getWidth() {
      return this.cachedWidth;
   }

   public int getHeight() {
      return this.cachedHeight;
   }

   public boolean isVisible() {
      return this.isOn() && this.enable.getValue();
   }

   public void render(DrawContext context, float tickDelta) {
      this.renderAt(context);
   }

   public boolean isShowCount() {
      return this.showCount.getValue();
   }

   public boolean isBorderEnable() {
      return this.borderEnable.getValue();
   }

   public Color getBorderColor() {
      return this.borderColor.getValue();
   }

   public int getBorderPadding() {
      return this.borderPadding.getValueInt();
   }
}
