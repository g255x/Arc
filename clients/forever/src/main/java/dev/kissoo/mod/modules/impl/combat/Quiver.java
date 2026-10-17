package dev.kissoo.mod.modules.impl.combat;

import dev.kissoo.Forever;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.events.impl.UpdateRotateEvent;
import dev.kissoo.api.utils.player.InventoryUtil;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BindSetting;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import net.minecraft.item.BowItem;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class Quiver extends Module {
   private final BooleanSetting instant = this.add(new BooleanSetting("InstantRotate", true));
   private final SliderSetting time = this.add(new SliderSetting("Time", 0.11F, 0.0, 1.0, 0.01));
   private final BooleanSetting onlyPress = this.add(new BooleanSetting("OnlyPress", false));
   private final BindSetting key = this.add(new BindSetting("ActiveKey", -1));
   boolean bow = false;
   boolean pressed = false;
   boolean switching = false;
   int startSlot;

   public Quiver() {
      super("Quiver", Module.Category.Combat);
      this.setChinese("头顶射箭");
   }

   @Override
   public void onEnable() {
      this.bow = false;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.key.isPressed()) {
         if (!this.pressed && !this.switching) {
            int bow = InventoryUtil.findItem(Items.BOW);
            if (bow != -1) {
               this.startSlot = mc.player.getInventory().selectedSlot;
               InventoryUtil.switchToSlot(bow);
               mc.options.useKey.setPressed(true);
               this.switching = true;
               this.pressed = true;
            }
         }
      } else {
         this.pressed = false;
      }

      if (this.switching && (!mc.options.useKey.isPressed() || mc.player.isUsingItem() && mc.player.getActiveItem().getItem() != Items.BOW)) {
         InventoryUtil.switchToSlot(this.startSlot);
         this.switching = false;
      }

      this.bow = mc.player.isUsingItem()
         && (mc.player.getActiveHand() == Hand.MAIN_HAND ? mc.player.getMainHandStack() : mc.player.getOffHandStack()).getItem() instanceof BowItem;
      if (this.bow && (!this.onlyPress.getValue() || this.switching) && BowItem.getPullProgress(mc.player.getItemUseTime()) >= this.time.getValue()) {
         if (this.instant.getValue()) {
            Forever.ROTATION.snapAt(Forever.ROTATION.rotationYaw, -90.0F);
         }

         mc.options.useKey.setPressed(false);
         mc.interactionManager.stopUsingItem(mc.player);
         if (this.instant.getValue()) {
            Forever.ROTATION.snapBack();
         }
      }
   }

   @EventListener
   public void onRotate(UpdateRotateEvent event) {
      if (this.bow && !this.instant.getValue()) {
         event.setPitch(-90.0F);
      }
   }
}
