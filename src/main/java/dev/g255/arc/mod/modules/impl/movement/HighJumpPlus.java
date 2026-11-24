package dev.g255.arc.mod.modules.impl.movement;

import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.ClientTickEvent;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.api.utils.player.InventoryUtil;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import net.minecraft.util.Hand;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;

public class HighJumpPlus extends Module {
   public static HighJumpPlus INSTANCE;
   private final Timer timer = new Timer();
   private final SliderSetting upwardVelocity = this.add(new SliderSetting("UpwardVelocity", (double)2.5F, (double)0.5F, (double)5.0F, 0.1));

   public HighJumpPlus() {
      super("HighJump+", "Auto throws wind charge and boosts you upward", Module.Category.Movement);
      this.setChinese("高跳+");
      INSTANCE = this;
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!nullCheck()) {
         if (this.timer.passedS((double)1.0F)) {
            if (!mc.player.isOnGround()) {
               this.timer.reset();
            } else {
               int slot = InventoryUtil.findItem(Items.WIND_CHARGE);
               if (slot != -1) {
                  int oldSlot = mc.player.getInventory().selectedSlot;
                  InventoryUtil.switchToSlot(slot);
                  sendSequencedPacket((id) -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
                  mc.player.addVelocity((double)0.0F, this.upwardVelocity.getValue(), (double)0.0F);
                  InventoryUtil.switchToSlot(oldSlot);
                  this.timer.reset();
               }
            }
         }
      }
   }
}
