package dev.baimaodaxia.mod.modules.impl.player;

import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.util.Window;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class LowArmorAlert extends Module {
   public static LowArmorAlert INSTANCE;
   private final SliderSetting threshold = this.add(new SliderSetting("Threshold", (double)35.0F, (double)0.0F, (double)100.0F, (double)1.0F));
   private final SliderSetting yOffset = this.add(new SliderSetting("YOffset", (double)220.0F, (double)150.0F, (double)320.0F, (double)1.0F));
   private long lastFlashTime;

   public LowArmorAlert() {
      super("LowArmorAlert", Module.Category.Player);
      this.setChinese("低盔甲耐久警报");
      INSTANCE = this;
   }

   public void onRender2D(DrawContext drawContext, float tickDelta) {
      if (!nullCheck()) {
         double thresholdValue = this.threshold.getValue();
         Window window = this.getMinecraft().getWindow();
         int screenWidth = window.getScaledWidth();
         int screenHeight = window.getScaledHeight();
         int yOffsetValue = (int)this.yOffset.getValue();
         boolean hasLowDurability = false;
         ItemStack helmet = this.getMinecraft().player.getInventory().getArmorStack(3);
         if (!helmet.isEmpty() && helmet.getItem() instanceof ArmorItem) {
            float durabilityPercent = this.getDurabilityPercent(helmet);
            if ((double)durabilityPercent < thresholdValue) {
               hasLowDurability = true;
            }
         }

         ItemStack chestplate = this.getMinecraft().player.getInventory().getArmorStack(2);
         if (!chestplate.isEmpty() && chestplate.getItem() instanceof ArmorItem) {
            float durabilityPercent = this.getDurabilityPercent(chestplate);
            if ((double)durabilityPercent < thresholdValue) {
               hasLowDurability = true;
            }
         }

         ItemStack leggings = this.getMinecraft().player.getInventory().getArmorStack(1);
         if (!leggings.isEmpty() && leggings.getItem() instanceof ArmorItem) {
            float durabilityPercent = this.getDurabilityPercent(leggings);
            if ((double)durabilityPercent < thresholdValue) {
               hasLowDurability = true;
            }
         }

         ItemStack boots = this.getMinecraft().player.getInventory().getArmorStack(0);
         if (!boots.isEmpty() && boots.getItem() instanceof ArmorItem) {
            float durabilityPercent = this.getDurabilityPercent(boots);
            if ((double)durabilityPercent < thresholdValue) {
               hasLowDurability = true;
            }
         }

         if (hasLowDurability) {
            this.drawWarning(drawContext, "Your armor durability is low!", screenWidth, screenHeight, yOffsetValue);
         }

      }
   }

   private float getDurabilityPercent(ItemStack stack) {
      if (!stack.isEmpty() && stack.isDamageable()) {
         int maxDamage = stack.getMaxDamage();
         int currentDamage = stack.getDamage();
         int remainingDurability = maxDamage - currentDamage;
         return (float)remainingDurability / (float)maxDamage * 100.0F;
      } else {
         return 0.0F;
      }
   }

   private void drawWarning(DrawContext drawContext, String text, int screenWidth, int screenHeight, int yOffset) {
      TextRenderer textRenderer = this.getMinecraft().textRenderer;
      int textWidth = textRenderer.getWidth(text);
      int x = (screenWidth - textWidth) / 2;
      int y = screenHeight / 2 - 100 + yOffset;
      long currentTime = System.currentTimeMillis();
      boolean shouldFlash = currentTime - this.lastFlashTime < 250L;
      if (currentTime - this.lastFlashTime >= 500L) {
         this.lastFlashTime = currentTime;
      }

      drawContext.drawTextWithShadow(textRenderer, text, x, y, -65536);
   }

   private MinecraftClient getMinecraft() {
      return MinecraftClient.getInstance();
   }
}
