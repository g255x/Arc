package dev.kissoo.mod.modules.impl.player;

import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.utils.combat.CombatUtil;
import dev.kissoo.api.utils.player.InventoryUtil;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import dev.kissoo.mod.modules.settings.impl.StringSetting;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.screen.slot.SlotActionType;

public class Driller extends Module {
   private final SliderSetting range = this.add(new SliderSetting("Range", 5.0, 1.0, 20.0, 0.5)).setSuffix(" blocks");
   private final SliderSetting totem = this.add(new SliderSetting("Totem", 1, 0, 36));
   private final SliderSetting health = this.add(new SliderSetting("Health", 15.0, 1.0, 36.0, 0.5)).setSuffix(" hp");
   private final StringSetting offsetY = this.add(new StringSetting("OffsetY", "-70"));
   private boolean pendingFly = false;

   public Driller() {
      super("Driller", Module.Category.Player);
      this.setChinese("钻地");
   }

   @Override
   public void onDisable() {
      pendingFly = false;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) {
         return;
      }

      if (mc.player.getY() < -64.0) {
         pendingFly = false;
         return;
      }

      if (pendingFly) {
         if (!mc.player.isOnGround()) {
            pendingFly = false;
            doFlyClip();
         }
         return;
      }
      if (CombatUtil.getClosestEnemy(range.getValue()) == null) {
         return;
      }

      int totemCount = InventoryUtil.getItemCount(Items.TOTEM_OF_UNDYING);
      int totemThreshold = totem.getValueInt();

      if (totemCount > totemThreshold) {
         return;
      } else if (totemCount == totemThreshold) {
         float totalHealth = mc.player.getHealth() + mc.player.getAbsorptionAmount();
         if (totalHealth >= health.getValue()) {
            return;
         }
      }

      startSequence();
   }
   private void startSequence() {
      if (mc.player.isOnGround()) {
         mc.player.jump();
         pendingFly = true;
      } else {
         doFlyClip();
      }
   }
   private void doFlyClip() {
      double targetY = parseOffsetY();
      equipElytra();
      mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
      mc.player.startFallFlying();
      double x = mc.player.getX();
      double z = mc.player.getZ();
      mc.player.setPosition(x, targetY, z);
      mc.getNetworkHandler().sendPacket(new PositionAndOnGround(x, targetY, z, false));
   }
   private void equipElytra() {
      for (net.minecraft.item.ItemStack is : mc.player.getArmorItems()) {
         if (is.getItem() instanceof net.minecraft.item.ElytraItem) {
            return;
         }
      }

      int elytraHotbar = InventoryUtil.findItem(Items.ELYTRA);
      if (elytraHotbar != -1) {
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, elytraHotbar, SlotActionType.SWAP, mc.player);
         return;
      }

      int elytraInventory = InventoryUtil.findItemInventorySlot(Items.ELYTRA);
      if (elytraInventory == -1) {
         return;
      }
      mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, elytraInventory, 0, SlotActionType.PICKUP, mc.player);
      mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
      mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, elytraInventory, 0, SlotActionType.PICKUP, mc.player);
   }

   private double parseOffsetY() {
      try {
         return Double.parseDouble(offsetY.getValue().trim());
      } catch (NumberFormatException e) {
         return -70.0;
      }
   }

   @Override
   public String getInfo() {
      return offsetY.getValue();
   }
}
