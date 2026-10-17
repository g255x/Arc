package dev.baimaodaxia.mod.modules.impl.player;

import dev.baimaodaxia.Alien;
import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.UpdateEvent;
import dev.baimaodaxia.api.utils.player.EntityUtil;
import dev.baimaodaxia.api.utils.player.InventoryUtil;
import dev.baimaodaxia.api.utils.player.MovementUtil;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.impl.movement.ElytraFly;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.Hand;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.network.packet.c2s.play.PickFromInventoryC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.item.Item;

public class AutoArmor extends Module {
   public static AutoArmor INSTANCE;
   private final EnumSetting<EnchantPriority> head;
   private final EnumSetting<EnchantPriority> body;
   private final EnumSetting<EnchantPriority> tights;
   private final EnumSetting<EnchantPriority> feet;
   private final BooleanSetting ignoreCurse;
   private final BooleanSetting noMove;
   private final SliderSetting delay;
   private final BooleanSetting autoElytra;
   private final EnumSetting<HotbarSwapMode> hotbarSwap;
   private final EnumSetting<InventorySwapMode> inventorySwap;
   private int tickDelay;

   public AutoArmor() {
      super("AutoArmor", Module.Category.Player);
      this.head = this.add(new EnumSetting("Head", AutoArmor.EnchantPriority.Protection));
      this.body = this.add(new EnumSetting("Body", AutoArmor.EnchantPriority.Protection));
      this.tights = this.add(new EnumSetting("Tights", AutoArmor.EnchantPriority.Protection));
      this.feet = this.add(new EnumSetting("Feet", AutoArmor.EnchantPriority.Protection));
      this.ignoreCurse = this.add(new BooleanSetting("IgnoreCurse", true));
      this.noMove = this.add(new BooleanSetting("NoMove", false));
      this.delay = this.add(new SliderSetting("Delay", (double)3.0F, (double)0.0F, (double)10.0F, (double)1.0F));
      this.autoElytra = this.add(new BooleanSetting("AutoElytra", true));
      this.hotbarSwap = this.add(new EnumSetting("HotbarSwap", AutoArmor.HotbarSwapMode.Swap));
      this.inventorySwap = this.add(new EnumSetting("InventorySwap", AutoArmor.InventorySwapMode.ClickSlot));
      this.tickDelay = 0;
      this.setChinese("自动穿甲");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (EntityUtil.inInventory()) {
         if (mc.player.playerScreenHandler == mc.player.currentScreenHandler) {
            if (!MovementUtil.isMoving() || !this.noMove.getValue()) {
               if (this.tickDelay > 0) {
                  --this.tickDelay;
               } else {
                  this.tickDelay = this.delay.getValueInt();
                  HashMap<EquipmentSlot, int[]> armorMap = new HashMap(4);
                  armorMap.put(EquipmentSlot.FEET, new int[]{36, this.getProtection(mc.player.getInventory().getStack(36)), -1, -1});
                  armorMap.put(EquipmentSlot.LEGS, new int[]{37, this.getProtection(mc.player.getInventory().getStack(37)), -1, -1});
                  armorMap.put(EquipmentSlot.CHEST, new int[]{38, this.getProtection(mc.player.getInventory().getStack(38)), -1, -1});
                  armorMap.put(EquipmentSlot.HEAD, new int[]{39, this.getProtection(mc.player.getInventory().getStack(39)), -1, -1});

                  for(int s = 0; s < 36; ++s) {
                     if ((mc.player.getInventory().getStack(s).getItem() instanceof ArmorItem || mc.player.getInventory().getStack(s).getItem() == Items.ELYTRA) && (mc.player.getInventory().getStack(s).getItem() != Items.ELYTRA || (!ElytraFly.INSTANCE.isOff() || !this.autoElytra.getValue()) && (!ElytraFly.INSTANCE.isOn() || !ElytraFly.INSTANCE.packet.getValue()))) {
                        int protection = this.getProtection(mc.player.getInventory().getStack(s));
                        EquipmentSlot slot = mc.player.getInventory().getStack(s).getItem() instanceof ElytraItem ? EquipmentSlot.CHEST : ((ArmorItem)mc.player.getInventory().getStack(s).getItem()).getSlotType();

                        for(Map.Entry e : armorMap.entrySet()) {
                           if (this.autoElytra.getValue() && ElytraFly.INSTANCE.isOn() && e.getKey() == EquipmentSlot.CHEST) {
                              if ((mc.player.getInventory().getStack(38).isEmpty() || !(mc.player.getInventory().getStack(38).getItem() instanceof ElytraItem) || !ElytraItem.isUsable(mc.player.getInventory().getStack(38))) && (((int[])e.getValue())[2] == -1 || mc.player.getInventory().getStack(((int[])e.getValue())[2]).isEmpty() || !(mc.player.getInventory().getStack(((int[])e.getValue())[2]).getItem() instanceof ElytraItem) || !ElytraItem.isUsable(mc.player.getInventory().getStack(((int[])e.getValue())[2]))) && !mc.player.getInventory().getStack(s).isEmpty() && mc.player.getInventory().getStack(s).getItem() instanceof ElytraItem && ElytraItem.isUsable(mc.player.getInventory().getStack(s))) {
                                 ((int[])e.getValue())[2] = s;
                              }
                           } else if (protection > 0 && e.getKey() == slot && protection > ((int[])e.getValue())[1] && protection > ((int[])e.getValue())[3]) {
                              ((int[])e.getValue())[2] = s;
                              ((int[])e.getValue())[3] = protection;
                           }
                        }
                     }
                  }

                  for(Map.Entry equipmentSlotEntry : armorMap.entrySet()) {
                     if (((int[])equipmentSlotEntry.getValue())[2] != -1) {
                        if (((int[])equipmentSlotEntry.getValue())[2] < 9) {
                           switch (((HotbarSwapMode)this.hotbarSwap.getValue()).ordinal()) {
                              case 0:
                                 int armorSlot = 44 - ((int[])equipmentSlotEntry.getValue())[0];
                                 mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, armorSlot, ((int[])equipmentSlotEntry.getValue())[2], SlotActionType.SWAP, mc.player);
                                 EntityUtil.syncInventory();
                                 break;
                              case 1:
                                 int old = mc.player.getInventory().selectedSlot;
                                 InventoryUtil.switchToSlot(((int[])equipmentSlotEntry.getValue())[2]);
                                 sendSequencedPacket((id) -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
                                 InventoryUtil.switchToSlot(old);
                           }
                        } else if (mc.player.playerScreenHandler == mc.player.currentScreenHandler) {
                           int armorSlot = 44 - ((int[])equipmentSlotEntry.getValue())[0];
                           int newArmorSlot = ((int[])equipmentSlotEntry.getValue())[2];
                           switch (((InventorySwapMode)this.inventorySwap.getValue()).ordinal()) {
                              case 0:
                                 mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, newArmorSlot, 0, SlotActionType.PICKUP, mc.player);
                                 mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, armorSlot, 0, SlotActionType.PICKUP, mc.player);
                                 if (((int[])equipmentSlotEntry.getValue())[1] != -1) {
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, newArmorSlot, 0, SlotActionType.PICKUP, mc.player);
                                 }
                                 break;
                              case 1:
                                 mc.getNetworkHandler().sendPacket(new PickFromInventoryC2SPacket(newArmorSlot));
                                 sendSequencedPacket((id) -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
                                 mc.getNetworkHandler().sendPacket(new PickFromInventoryC2SPacket(newArmorSlot));
                           }

                           EntityUtil.syncInventory();
                        }

                        return;
                     }
                  }

               }
            }
         }
      }
   }

   private int getProtection(ItemStack is) {
      if (!(is.getItem() instanceof ArmorItem) && is.getItem() != Items.ELYTRA) {
         return !is.isEmpty() ? 0 : -1;
      } else {
         int prot = 0;
         Item item = is.getItem();
         EquipmentSlot slot;
         if (item instanceof ArmorItem) {
            ArmorItem ai = (ArmorItem)item;
            slot = ai.getSlotType();
         } else {
            slot = EquipmentSlot.BODY;
         }

         if (is.getItem() instanceof ElytraItem) {
            if (!ElytraItem.isUsable(is)) {
               return 0;
            }

            prot = 1;
         }

         int blastMultiplier = 1;
         int protectionMultiplier = 1;
         switch (slot) {
            case HEAD:
               if (this.head.is(AutoArmor.EnchantPriority.Protection)) {
                  protectionMultiplier *= 2;
               } else {
                  blastMultiplier *= 2;
               }
               break;
            case BODY:
               if (this.body.is(AutoArmor.EnchantPriority.Protection)) {
                  protectionMultiplier *= 2;
               } else {
                  blastMultiplier *= 2;
               }
               break;
            case LEGS:
               if (this.tights.is(AutoArmor.EnchantPriority.Protection)) {
                  protectionMultiplier *= 2;
               } else {
                  blastMultiplier *= 2;
               }
               break;
            case FEET:
               if (this.feet.is(AutoArmor.EnchantPriority.Protection)) {
                  protectionMultiplier *= 2;
               } else {
                  blastMultiplier *= 2;
               }
         }

         if (is.hasEnchantments()) {
            ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(is);
            if (enchants.getEnchantments().contains(mc.world.getRegistryManager().get(Enchantments.PROTECTION.getRegistryRef()).getEntry(Enchantments.PROTECTION).get())) {
               prot += enchants.getLevel((RegistryEntry)mc.world.getRegistryManager().get(Enchantments.PROTECTION.getRegistryRef()).getEntry(Enchantments.PROTECTION).get()) * protectionMultiplier;
            }

            if (enchants.getEnchantments().contains(mc.world.getRegistryManager().get(Enchantments.BLAST_PROTECTION.getRegistryRef()).getEntry(Enchantments.BLAST_PROTECTION).get())) {
               prot += enchants.getLevel((RegistryEntry)mc.world.getRegistryManager().get(Enchantments.BLAST_PROTECTION.getRegistryRef()).getEntry(Enchantments.BLAST_PROTECTION).get()) * blastMultiplier;
            }

            if (enchants.getEnchantments().contains(mc.world.getRegistryManager().get(Enchantments.BLAST_PROTECTION.getRegistryRef()).getEntry(Enchantments.BINDING_CURSE).get()) && this.ignoreCurse.getValue()) {
               prot = -999;
            }
         }

         return (is.getItem() instanceof ArmorItem ? ((ArmorItem)is.getItem()).getProtection() : 0) + prot;
      }
   }

   private static enum EnchantPriority {
      Blast,
      Protection;

      // $FF: synthetic method
      private static EnchantPriority[] $values() {
         return new EnchantPriority[]{Blast, Protection};
      }
   }

   public static enum HotbarSwapMode {
      Swap,
      Switch;

      // $FF: synthetic method
      private static HotbarSwapMode[] $values() {
         return new HotbarSwapMode[]{Swap, Switch};
      }
   }

   public static enum InventorySwapMode {
      ClickSlot,
      Pick;

      // $FF: synthetic method
      private static InventorySwapMode[] $values() {
         return new InventorySwapMode[]{ClickSlot, Pick};
      }
   }
}
