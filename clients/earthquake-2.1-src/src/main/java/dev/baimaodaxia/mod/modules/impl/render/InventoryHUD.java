package dev.baimaodaxia.mod.modules.impl.render;

import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.item.ItemStack;
import net.minecraft.client.gui.DrawContext;

public class InventoryHUD extends Module {
   public static InventoryHUD INSTANCE;
   public final SliderSetting invX = this.add(new SliderSetting("InvX", 100, 0, 1000));
   public final SliderSetting invY = this.add(new SliderSetting("InvY", 100, 0, 1000));
   public final BooleanSetting showItemCount = this.add(new BooleanSetting("ShowItemCount", true));
   public final BooleanSetting showEmptySlots = this.add(new BooleanSetting("ShowEmptySlots", false));
   public final SliderSetting invScale = this.add(new SliderSetting("InvScale", (double)1.0F, (double)0.5F, (double)2.0F, 0.1));

   public InventoryHUD() {
      super("InventoryHUD", Module.Category.Render);
      this.setChinese("背包显示");
      INSTANCE = this;
   }

   public void onRender2D(DrawContext drawContext, float tickDelta) {
      if (mc.player != null && mc.world != null) {
         this.renderInventory(drawContext);
      }
   }

   private void renderInventory(DrawContext drawContext) {
      int startX = this.invX.getValueInt();
      int startY = this.invY.getValueInt();
      float scale = (float)this.invScale.getValue();
      drawContext.getMatrices().push();
      drawContext.getMatrices().scale(scale, scale, 1.0F);
      int scaledX = (int)((float)startX / scale);
      int scaledY = (int)((float)startY / scale);

      for(int i = 9; i < 36; ++i) {
         ItemStack itemStack = mc.player.getInventory().getStack(i);
         if (this.showEmptySlots.getValue() || !itemStack.isEmpty()) {
            int slotIndex = i - 9;
            int row = slotIndex / 9;
            int col = slotIndex % 9;
            int x = scaledX + col * 16;
            int y = scaledY + row * 16;
            if (!itemStack.isEmpty()) {
               drawContext.drawItem(itemStack, x, y);
               drawContext.drawItemInSlot(mc.textRenderer, itemStack, x, y);
               if (this.showItemCount.getValue() && itemStack.getCount() > 1) {
                  String countText = String.valueOf(itemStack.getCount());
                  int textWidth = mc.textRenderer.getWidth(countText);
                  drawContext.getMatrices().push();
                  drawContext.getMatrices().scale(0.8F, 0.8F, 1.0F);
                  drawContext.drawText(mc.textRenderer, countText, (int)(((float)(x + 16) - (float)textWidth * 0.8F) / 0.8F), (int)((float)(y + 16 - 8) / 0.8F), Color.WHITE.getRGB(), true);
                  drawContext.getMatrices().pop();
               }
            } else if (this.showEmptySlots.getValue()) {
               drawContext.fill(x, y, x + 16, y + 16, 1090519039);
               drawContext.drawBorder(x, y, 16, 16, -2130706433);
            }
         }
      }

      drawContext.getMatrices().pop();
   }
}
