package dev.g255.arc.mod.modules.impl.player;

import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.ItemStack;

public class Replenish extends Module {
   private final EnumSetting<Mode> mode;
   private final SliderSetting delay;
   private final SliderSetting min;
   private final SliderSetting forceDelay;
   private final SliderSetting forceMin;
   private final Timer timer;

   public Replenish() {
      super("Replenish", Module.Category.Player);
      this.mode = this.add(new EnumSetting("Mode", Replenish.Mode.QuickMove));
      this.delay = this.add((new SliderSetting("Delay", (double)2.0F, (double)0.0F, (double)5.0F, 0.01)).setSuffix("s"));
      this.min = this.add(new SliderSetting("Min", 50, 1, 100)).setSuffix("%");
      this.forceDelay = this.add((new SliderSetting("ForceDelay", 0.2, (double)0.0F, (double)4.0F, 0.01)).setSuffix("s"));
      this.forceMin = this.add(new SliderSetting("ForceMin", 16, 1, 100)).setSuffix("%");
      this.timer = new Timer();
      this.setChinese("物品栏补充");
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      for(int i = 0; i < 9; ++i) {
         if (this.replenish(i)) {
            this.timer.reset();
            return;
         }
      }

   }

   private boolean replenish(int slot) {
      ItemStack stack = mc.player.getInventory().getStack(slot);
      if (stack.isEmpty()) {
         return false;
      } else if (!stack.isStackable()) {
         return false;
      } else {
         int percent = (int)((double)stack.getCount() / (double)stack.getMaxCount() * (double)100.0F);
         if ((double)percent > this.min.getValue()) {
            return false;
         } else {
            for(int i = 9; i < 36; ++i) {
               ItemStack item = mc.player.getInventory().getStack(i);
               if (!item.isEmpty() && item.getItem() == stack.getItem() && stack.isStackable() && ItemStack.areItemsAndComponentsEqual(stack, item)) {
                  if ((float)percent > this.forceMin.getValueFloat()) {
                     if (!this.timer.passedS(this.delay.getValue())) {
                        return false;
                     }
                  } else if (!this.timer.passedS(this.forceDelay.getValue())) {
                     return false;
                  }

                  switch (((Mode)this.mode.getValue()).ordinal()) {
                     case 0:
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                        break;
                     case 1:
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot + 36, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
                  }

                  return true;
               }
            }

            return false;
         }
      }
   }

   public static enum Mode {
      QuickMove,
      ClickSlot;

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{QuickMove, ClickSlot};
      }
   }
}
