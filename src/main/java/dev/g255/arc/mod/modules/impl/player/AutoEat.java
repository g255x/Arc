package dev.g255.arc.mod.modules.impl.player;

import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.api.utils.player.InventoryUtil;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import net.minecraft.util.Hand;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.component.DataComponentTypes;

public class AutoEat extends Module {
   public final SliderSetting hunger = this.add(new SliderSetting("Hunger", 15, 0, 20));
   public final SliderSetting health = this.add(new SliderSetting("Health", 14, 0, 20));
   public final BooleanSetting goldenOnly = this.add(new BooleanSetting("GoldenOnly", false));
   private int prevSlot = -1;
   private int repeated = 0;

   public AutoEat() {
      super("AutoEat", Module.Category.Player);
      this.setChinese("自动进食");
   }

   public void onDisable() {
      mc.options.useKey.setPressed(false);
      if (!nullCheck()) {
         if (this.prevSlot != -1) {
            InventoryUtil.switchToSlot(this.prevSlot);
         }

         this.prevSlot = -1;
         this.repeated = 0;
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!nullCheck() && mc.currentScreen == null && !mc.player.isCreative() && !mc.player.isSpectator()) {
         boolean hungry = mc.player.getHungerManager().getFoodLevel() <= this.hunger.getValueInt();
         boolean low = (double)mc.player.getHealth() <= this.health.getValue();
         if (mc.player.isUsingItem()) {
            ++this.repeated;
            if (this.repeated <= 3) {
               mc.options.useKey.setPressed(true);
            } else {
               mc.options.useKey.setPressed(false);
               mc.interactionManager.stopUsingItem(mc.player);
               if (mc.player.getHungerManager().getFoodLevel() <= this.hunger.getValueInt()) {
                  this.eatNext();
               }
            }

         } else if (hungry || low) {
            if (this.goldenOnly.getValue() && !isGolden(mc.player.getMainHandStack())) {
               this.eatNext();
            } else {
               if (isFood(mc.player.getMainHandStack())) {
                  mc.options.useKey.setPressed(true);
                  mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
               } else {
                  this.eatNext();
               }

            }
         }
      }
   }

   private void eatNext() {
      int slot = InventoryUtil.getFood();
      if (slot != -1) {
         if (this.prevSlot == -1) {
            this.prevSlot = mc.player.getInventory().selectedSlot;
         }

         InventoryUtil.switchToSlot(slot);
         mc.options.useKey.setPressed(true);
         this.repeated = 0;
      }
   }

   private static boolean isFood(ItemStack stack) {
      return stack != null && !stack.isEmpty() && stack.getItem().getComponents().contains(DataComponentTypes.FOOD);
   }

   private static boolean isGolden(ItemStack stack) {
      return stack != null && !stack.isEmpty() && (stack.getItem() == Items.GOLDEN_APPLE || stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE);
   }
}
