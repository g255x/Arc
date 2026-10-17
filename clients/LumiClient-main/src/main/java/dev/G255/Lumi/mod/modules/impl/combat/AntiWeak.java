package dev.G255.Lumi.mod.modules.impl.combat;

import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.PacketEvent;
import dev.G255.Lumi.api.utils.math.Timer;
import dev.G255.Lumi.api.utils.player.EntityUtil;
import dev.G255.Lumi.api.utils.player.InventoryUtil;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.EnumSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;

public class AntiWeak extends Module {
   private final EnumSetting swapMode;
   private final SliderSetting delay;
   private final BooleanSetting onlyCrystal;
   private final Timer delayTimer;
   boolean ignore;
   private PlayerInteractEntityC2SPacket lastPacket;

   public AntiWeak() {
      super("AntiWeak", Module.Category.Combat);
      this.swapMode = this.add(new EnumSetting("SwapMode", AntiWeak.SwapMode.Inventory));
      this.delay = this.add((new SliderSetting("Delay", 100, 0, 500)).setSuffix("ms"));
      this.onlyCrystal = this.add(new BooleanSetting("OnlyCrystal", true));
      this.delayTimer = new Timer();
      this.ignore = false;
      this.lastPacket = null;
      this.setChinese("");
   }

   public String getInfo() {
      return ((SwapMode)this.swapMode.getValue()).name();
   }

   @EventListener(
      priority = 200
   )
   public void onPacketSend(PacketEvent.Send event) {
      if (!nullCheck()) {
         if (!event.isCancelled()) {
            if (!this.ignore) {
               if (mc.player.getStatusEffect(StatusEffects.WEAKNESS) != null) {
                  if (!(mc.player.getMainHandStack().getItem() instanceof SwordItem)) {
                     if (this.delayTimer.passedMs(this.delay.getValue())) {
                        Packet<?> packet2 = event.getPacket();
                        PlayerInteractEntityC2SPacket packet;
                        if (packet2 instanceof PlayerInteractEntityC2SPacket && Criticals.getInteractType(packet = (PlayerInteractEntityC2SPacket)packet2) == PlayerInteractEntityC2SPacket.InteractType.ATTACK) {
                           if (this.onlyCrystal.getValue() && !(Criticals.getEntity(packet) instanceof EndCrystalEntity)) {
                              return;
                           }

                           this.lastPacket = packet;
                           this.delayTimer.reset();
                           this.ignore = true;
                           this.doAnti();
                           this.ignore = false;
                           event.cancel();
                        }

                     }
                  }
               }
            }
         }
      }
   }

   private void doAnti() {
      if (this.lastPacket != null) {
         int strong = this.swapMode.getValue() != AntiWeak.SwapMode.Inventory ? InventoryUtil.findClass(SwordItem.class) : InventoryUtil.findClassInventorySlot(SwordItem.class);
         if (strong != -1) {
            int old = mc.player.getInventory().selectedSlot;
            if (this.swapMode.getValue() != AntiWeak.SwapMode.Inventory) {
               InventoryUtil.switchToSlot(strong);
            } else {
               InventoryUtil.inventorySwap(strong, mc.player.getInventory().selectedSlot);
            }

            mc.getNetworkHandler().sendPacket(this.lastPacket);
            if (this.swapMode.getValue() != AntiWeak.SwapMode.Inventory) {
               if (this.swapMode.getValue() != AntiWeak.SwapMode.Normal) {
                  InventoryUtil.switchToSlot(old);
               }
            } else {
               InventoryUtil.inventorySwap(strong, mc.player.getInventory().selectedSlot);
               EntityUtil.syncInventory();
            }

         }
      }
   }

   public static enum SwapMode {
      Normal,
      Silent,
      Inventory;

      // $FF: synthetic method
      private static SwapMode[] $values() {
         return new SwapMode[]{Normal, Silent, Inventory};
      }
   }
}
