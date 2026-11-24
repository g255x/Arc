package dev.g255.arc.mod.modules.impl.player;

import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;

public class ChestStealer extends Module {
   private final EnumSetting<Mode> mode;
   private final SliderSetting delay;
   private final SliderSetting perTick;
   private final BooleanSetting autoDisable;
   private final Timer timer;

   public ChestStealer() {
      super("ChestStealer", "Steals items from the storages you open.", Module.Category.Player);
      this.mode = this.add(new EnumSetting("Mode", ChestStealer.Mode.Steal));
      this.delay = this.add(new SliderSetting("Delay", (double)100.0F, (double)0.0F, (double)1000.0F, (double)10.0F));
      this.perTick = this.add(new SliderSetting("PerTick", (double)1.0F, (double)1.0F, (double)10.0F, (double)1.0F));
      this.autoDisable = this.add(new BooleanSetting("AutoDisable", false));
      this.timer = new Timer();
      this.setChinese("箱子偷取");
   }

   public void onEnable() {
      this.timer.reset();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!nullCheck()) {
         boolean isContainer = mc.currentScreen instanceof GenericContainerScreen;
         boolean isShulker = mc.currentScreen instanceof ShulkerBoxScreen;
         if (isContainer || isShulker) {
            if (this.timer.passed((long)this.delay.getValueInt())) {
               ScreenHandler handler = mc.player.currentScreenHandler;
               int containerEnd;
               if (isContainer) {
                  GenericContainerScreen screen = (GenericContainerScreen)mc.currentScreen;
                  containerEnd = ((GenericContainerScreenHandler)screen.getScreenHandler()).getRows() * 9;
               } else {
                  containerEnd = 27;
               }

               int moved = 0;
               boolean did = false;
               boolean drop = this.mode.getValue() == ChestStealer.Mode.Drop;

               for(int i = 0; i < containerEnd && i < handler.slots.size(); ++i) {
                  Slot slot = handler.getSlot(i);
                  ItemStack stack = slot.getStack();
                  if (!stack.isEmpty()) {
                     if (drop) {
                        mc.interactionManager.clickSlot(handler.syncId, slot.id, 1, SlotActionType.THROW, mc.player);
                     } else {
                        mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                     }

                     did = true;
                     ++moved;
                     this.timer.reset();
                     if (moved >= this.perTick.getValueInt()) {
                        break;
                     }
                  }
               }

               if (this.autoDisable.getValue() && !did) {
                  this.disable();
               }

            }
         }
      }
   }

   public static enum Mode {
      Steal,
      Drop;

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{Steal, Drop};
      }
   }
}
