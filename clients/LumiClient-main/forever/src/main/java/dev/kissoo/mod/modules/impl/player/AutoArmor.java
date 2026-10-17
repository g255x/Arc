package dev.kissoo.mod.modules.impl.player;

import dev.kissoo.Forever;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.ClientTickEvent;
import dev.kissoo.api.events.impl.Render2DEvent;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.utils.player.EntityUtil;
import dev.kissoo.api.utils.player.InventoryUtil;
import dev.kissoo.api.utils.player.MovementUtil;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.impl.combat.Aura;
import dev.kissoo.mod.modules.impl.movement.ElytraFlight;
import dev.kissoo.mod.modules.settings.impl.BindSetting;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.PickFromInventoryC2SPacket;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

public class AutoArmor extends Module {
   public static AutoArmor INSTANCE;

   private final EnumSetting<EnchantPriority> head = this.add(new EnumSetting<>("Head", EnchantPriority.Protection));
   private final EnumSetting<EnchantPriority> body = this.add(new EnumSetting<>("Body", EnchantPriority.Protection));
   private final EnumSetting<EnchantPriority> tights = this.add(new EnumSetting<>("Tights", EnchantPriority.Protection));
   private final EnumSetting<EnchantPriority> feet = this.add(new EnumSetting<>("Feet", EnchantPriority.Protection));
   private final BooleanSetting ignoreCurse = this.add(new BooleanSetting("IgnoreCurse", true));
   private final BooleanSetting noMove = this.add(new BooleanSetting("NoMove", false));
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 3.0, 0.0, 10.0, 1.0));
   public final BooleanSetting autoElytra = this.add(new BooleanSetting("AutoElytra", true));
   private final EnumSetting<HotbarSwapMode> hotbarSwap = this.add(new EnumSetting<>("HotbarSwap", HotbarSwapMode.Swap));
   private final EnumSetting<InventorySwapMode> inventorySwap = this.add(new EnumSetting<>("InventorySwap", InventorySwapMode.ClickSlot));

   private final BooleanSetting switchArmor = this.add(new BooleanSetting("SwitchArmor", false).setParent());
   private final EnumSetting<NotifyMode> notifyMode = this.add(new EnumSetting<>("NotifyMode", NotifyMode.Hud, switchArmor::isOpen));
   private final SliderSetting hudX = this.add(new SliderSetting("HudX", 50.0, 0.0, 100.0, 0.1, () -> switchArmor.isOpen() && notifyMode.is(NotifyMode.Hud)).setSuffix("%"));
   private final SliderSetting hudY = this.add(new SliderSetting("HudY", 85.0, 0.0, 100.0, 0.1, () -> switchArmor.isOpen() && notifyMode.is(NotifyMode.Hud)).setSuffix("%"));
   private final BooleanSetting autoSwitch = this.add(new BooleanSetting("AutoSwitch", false, switchArmor::isOpen));

   private final EnumSetting<AutoSwitchMode> switchMode = this.add(new EnumSetting<>("SwitchMode", AutoSwitchMode.Item, () -> switchArmor.isOpen() && autoSwitch.getValue()));
   private final SliderSetting switchRange = this.add(new SliderSetting("EnemySwordRange", 10.0, 1.0, 20.0, 0.1, () -> switchArmor.isOpen() && autoSwitch.getValue() && switchMode.is(AutoSwitchMode.Item)));

   private final BindSetting switchArmorKey = this.add(new BindSetting("SwitchArmorKey", -1, () -> switchArmor.isOpen() && !autoSwitch.getValue()));
   private final BooleanSetting switchHead = this.add(new BooleanSetting("SwitchHead", true, switchArmor::isOpen));
   private final BooleanSetting switchChest = this.add(new BooleanSetting("SwitchChest", true, switchArmor::isOpen));
   private final BooleanSetting switchLegs = this.add(new BooleanSetting("SwitchLegs", true, switchArmor::isOpen));
   private final BooleanSetting switchFeet = this.add(new BooleanSetting("SwitchFeet", true, switchArmor::isOpen));
   private final SliderSetting switchDelay = this.add(new SliderSetting("SwitchDelay", 1.0, 0.0, 5.0, 1.0, switchArmor::isOpen).setSuffix("ticks"));

   private int tickDelay = 0;
   private boolean switchKeyPressed = false;
   private boolean isSwitching = false;
   private List<SwitchTask> switchTasks = new ArrayList<>();
   private int switchTickDelay = 0;
   private EnchantType switchTargetType = EnchantType.NONE;
   private EnchantType currentAutoSwitchType = EnchantType.NONE;
   private EnchantType currentDisplayType = EnchantType.NONE;

   public AutoArmor() {
      super("AutoArmor", Module.Category.Player);
      this.setChinese("自动穿甲");
      INSTANCE = this;
      Forever.EVENT_BUS.subscribe(new SwitchArmorTick());
   }

   @Override
   public void onDisable() {
      isSwitching = false;
      switchTasks.clear();
      currentAutoSwitchType = EnchantType.NONE;
      currentDisplayType = EnchantType.NONE;
   }

   private boolean isHoldingWeaponOrAuraSilent() {
      if (mc.player == null) return false;
      if (Aura.INSTANCE != null && Aura.INSTANCE.isOn() && Aura.target != null) {
         if (Aura.INSTANCE.switchMode.getValue() == Aura.SwitchMode.Silent) {
            return true;
         }
      }
      return mc.player.getMainHandStack().getItem() instanceof SwordItem || mc.player.getMainHandStack().getItem() instanceof AxeItem;
   }

   private void sendChatMessage(Text message) {
      if (mc.player == null) return;
      mc.player.sendMessage(
              Text.literal("")
                      .append(Text.literal("[").formatted(Formatting.DARK_GRAY))
                      .append(Text.literal(Forever.NAME).formatted(Formatting.LIGHT_PURPLE))
                      .append(Text.literal("] ").formatted(Formatting.DARK_GRAY))
                      .append(Text.literal("AutoArmor").formatted(Formatting.AQUA))
                      .append(Text.literal(" >> ").formatted(Formatting.GRAY))
                      .append(message),
              false
      );
   }

   private Text getEnchantTypeText(EnchantType type) {
      return switch (type) {
         case PROTECTION -> Text.literal("Protection").formatted(Formatting.GREEN);
         case BLAST_PROTECTION -> Text.literal("Blast Protection").formatted(Formatting.RED);
         case NONE -> Text.literal("None").formatted(Formatting.GRAY);
      };
   }

   private boolean isAnyElytraModuleOn() {
      return ElytraFlight.INSTANCE != null && ElytraFlight.INSTANCE.isOn();
   }
   private boolean isAnyElytraModuleBlockingAutoArmor() {
      return ElytraFlight.INSTANCE != null
              && ElytraFlight.INSTANCE.isOn()
              && ElytraFlight.INSTANCE.mode.is(ElytraFlight.Mode.Grim);
   }

   private boolean shouldHandleAutoElytra() {
      return this.autoElytra.getValue()
              && isAnyElytraModuleOn()
              && !isAnyElytraModuleBlockingAutoArmor();
   }

   public class SwitchArmorTick {
      @EventListener
      public void onTick(ClientTickEvent event) {
         if (Module.nullCheck() || event.isPost()) return;
         if (!switchArmor.getValue()) return;

         if (autoSwitch.getValue()) {
            handleAutoSwitch();
            return;
         }

         if (mc.currentScreen != null) {
            switchKeyPressed = false;
            return;
         }

         if (switchArmorKey.isPressed()) {
            if (!switchKeyPressed) {
               switchKeyPressed = true;
               startSwitchArmor(null);
            }
         } else {
            switchKeyPressed = false;
         }
      }
   }

   private void handleAutoSwitch() {
      if (mc.player == null || mc.world == null) return;
      if (isSwitching) return;
      if (mc.player.playerScreenHandler != mc.player.currentScreenHandler) return;

      EnchantType desired = EnchantType.BLAST_PROTECTION;

      if (this.switchMode.is(AutoSwitchMode.Item)) {
         if (isHoldingWeaponOrAuraSilent()) {
            desired = EnchantType.PROTECTION;
         } else if (mc.player.isUsingItem()) {
            boolean enemyHoldingSword = false;
            double range = this.switchRange.getValue();

            for (AbstractClientPlayerEntity target : Forever.THREAD.getPlayers()) {
               if (target == null || target == mc.player || !target.isAlive()) continue;
               if (Forever.FRIEND.isFriend(target)) continue;

               if (mc.player.distanceTo(target) <= range) {
                  if (target.getMainHandStack().getItem() instanceof SwordItem ||
                          target.getOffHandStack().getItem() instanceof SwordItem) {
                     enemyHoldingSword = true;
                     break;
                  }
               }
            }

            if (enemyHoldingSword) {
               desired = EnchantType.PROTECTION;
            }
         }
      } else if (this.switchMode.is(AutoSwitchMode.InsideBlock)) {
         if (EntityUtil.isInsideBlock()) {
            desired = EnchantType.PROTECTION;
         } else {
            desired = EnchantType.BLAST_PROTECTION;
         }
      }

      if (desired == currentAutoSwitchType) return;
      EnchantType currentWearing = detectCurrentArmorType();
      if (currentWearing == desired) {
         currentAutoSwitchType = desired;
         return;
      }

      currentAutoSwitchType = desired;
      startSwitchArmor(desired);
   }

   private void startSwitchArmor(EnchantType forceTarget) {
      if (mc.player == null || mc.world == null) return;
      if (mc.player.playerScreenHandler != mc.player.currentScreenHandler) return;

      switchTasks.clear();

      EnchantType targetType;
      if (forceTarget != null) {
         targetType = forceTarget;
      } else {
         EnchantType current = detectCurrentArmorType();
         targetType = (current == EnchantType.BLAST_PROTECTION)
                 ? EnchantType.PROTECTION
                 : EnchantType.BLAST_PROTECTION;
      }

      if (switchHead.getValue()) addSwitchTask(EquipmentSlot.HEAD, 39, targetType);
      if (switchChest.getValue()) addSwitchTask(EquipmentSlot.CHEST, 38, targetType);
      if (switchLegs.getValue()) addSwitchTask(EquipmentSlot.LEGS, 37, targetType);
      if (switchFeet.getValue()) addSwitchTask(EquipmentSlot.FEET, 36, targetType);

      if (!switchTasks.isEmpty()) {
         isSwitching = true;
         switchTickDelay = 0;
         switchTargetType = targetType;
         updateEnchantPriorities(targetType);
      }
   }

   private void updateEnchantPriorities(EnchantType targetType) {
      EnchantPriority newPriority = (targetType == EnchantType.BLAST_PROTECTION)
              ? EnchantPriority.Blast
              : EnchantPriority.Protection;

      if (switchHead.getValue()) head.setValue(newPriority);
      if (switchChest.getValue()) body.setValue(newPriority);
      if (switchLegs.getValue()) tights.setValue(newPriority);
      if (switchFeet.getValue()) feet.setValue(newPriority);
   }

   private EnchantType detectCurrentArmorType() {
      int protCount = 0;
      int blastCount = 0;

      int[][] slots = {
              {39, switchHead.getValue() ? 1 : 0},
              {38, switchChest.getValue() ? 1 : 0},
              {37, switchLegs.getValue() ? 1 : 0},
              {36, switchFeet.getValue() ? 1 : 0}
      };

      for (int[] s : slots) {
         if (s[1] == 0) continue;
         EnchantType t = getMainEnchantType(mc.player.getInventory().getStack(s[0]));
         if (t == EnchantType.PROTECTION) protCount++;
         else if (t == EnchantType.BLAST_PROTECTION) blastCount++;
      }

      if (blastCount > protCount) return EnchantType.BLAST_PROTECTION;
      return EnchantType.PROTECTION;
   }

   private void addSwitchTask(EquipmentSlot slot, int armorInventoryIndex, EnchantType targetType) {
      ItemStack currentArmor = mc.player.getInventory().getStack(armorInventoryIndex);

      if (currentArmor.isEmpty() || !(currentArmor.getItem() instanceof ArmorItem)) return;

      EnchantType currentType = getMainEnchantType(currentArmor);
      if (currentType == targetType) return;

      int bestSlot = -1;
      int bestProtection = -1;

      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem armorItem)) continue;
         if (armorItem.getSlotType() != slot) continue;

         EnchantType stackType = getMainEnchantType(stack);
         if (stackType != targetType) continue;

         if (ignoreCurse.getValue() && hasBindingCurse(stack)) continue;

         int prot = getRawProtection(stack);
         if (prot > bestProtection) {
            bestProtection = prot;
            bestSlot = i;
         }
      }

      if (bestSlot != -1) {
         switchTasks.add(new SwitchTask(slot, armorInventoryIndex, bestSlot));
      }
   }

   private int getRawProtection(ItemStack is) {
      if (!(is.getItem() instanceof ArmorItem armorItem)) return 0;
      int prot = armorItem.getProtection();
      if (is.hasEnchantments()) {
         ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(is);
         try {
            var protEntry = mc.world.getRegistryManager()
                    .getWrapperOrThrow(Enchantments.PROTECTION.getRegistryRef())
                    .getOptional(Enchantments.PROTECTION);
            if (protEntry.isPresent() && enchants.getEnchantments().contains(protEntry.get())) {
               prot += enchants.getLevel((RegistryEntry) protEntry.get());
            }
            var blastEntry = mc.world.getRegistryManager()
                    .getWrapperOrThrow(Enchantments.BLAST_PROTECTION.getRegistryRef())
                    .getOptional(Enchantments.BLAST_PROTECTION);
            if (blastEntry.isPresent() && enchants.getEnchantments().contains(blastEntry.get())) {
               prot += enchants.getLevel((RegistryEntry) blastEntry.get());
            }
         } catch (Exception ignored) {
         }
      }
      return prot;
   }

   private EnchantType getMainEnchantType(ItemStack stack) {
      if (stack.isEmpty() || !stack.hasEnchantments()) return EnchantType.NONE;
      ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(stack);
      int protLevel = 0, blastLevel = 0;
      try {
         var protEntry = mc.world.getRegistryManager()
                 .getWrapperOrThrow(Enchantments.PROTECTION.getRegistryRef())
                 .getOptional(Enchantments.PROTECTION);
         if (protEntry.isPresent() && enchants.getEnchantments().contains(protEntry.get())) {
            protLevel = enchants.getLevel((RegistryEntry) protEntry.get());
         }
         var blastEntry = mc.world.getRegistryManager()
                 .getWrapperOrThrow(Enchantments.BLAST_PROTECTION.getRegistryRef())
                 .getOptional(Enchantments.BLAST_PROTECTION);
         if (blastEntry.isPresent() && enchants.getEnchantments().contains(blastEntry.get())) {
            blastLevel = enchants.getLevel((RegistryEntry) blastEntry.get());
         }
      } catch (Exception e) {
         return EnchantType.NONE;
      }
      if (blastLevel > protLevel) return EnchantType.BLAST_PROTECTION;
      if (protLevel > 0) return EnchantType.PROTECTION;
      if (blastLevel > 0) return EnchantType.BLAST_PROTECTION;
      return EnchantType.NONE;
   }

   private boolean hasBindingCurse(ItemStack stack) {
      if (!stack.hasEnchantments()) return false;
      try {
         ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(stack);
         var bindingEntry = mc.world.getRegistryManager()
                 .getWrapperOrThrow(Enchantments.BINDING_CURSE.getRegistryRef())
                 .getOptional(Enchantments.BINDING_CURSE);
         return bindingEntry.isPresent() && enchants.getEnchantments().contains(bindingEntry.get());
      } catch (Exception e) {
         return false;
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (mc.player == null || mc.world == null) return;
      if (this.switchArmor.getValue()) {
         currentDisplayType = detectCurrentArmorType();
      }

      if (isSwitching && !switchTasks.isEmpty()) {
         if (mc.player.playerScreenHandler != mc.player.currentScreenHandler) return;

         if (switchTickDelay > 0) {
            switchTickDelay--;
            return;
         }

         SwitchTask task = switchTasks.remove(0);
         executeSwitchTask(task);
         switchTickDelay = switchDelay.getValueInt();
         if (switchTasks.isEmpty()) {
            isSwitching = false;
            if (this.notifyMode.is(NotifyMode.Chat)) {
               sendChatMessage(Text.literal("Switched to ")
                       .formatted(Formatting.WHITE)
                       .append(getEnchantTypeText(switchTargetType)));
            }
         }
         return;
      }

      if (EntityUtil.inInventory()) {
         if (mc.player.playerScreenHandler == mc.player.currentScreenHandler) {
            if (!MovementUtil.isMoving() || !this.noMove.getValue()) {
               if (this.tickDelay > 0) {
                  this.tickDelay--;
               } else {
                  this.tickDelay = this.delay.getValueInt();
                  Map<EquipmentSlot, int[]> armorMap = new HashMap<>(4);
                  armorMap.put(EquipmentSlot.FEET, new int[]{36, this.getProtection(mc.player.getInventory().getStack(36)), -1, -1});
                  armorMap.put(EquipmentSlot.LEGS, new int[]{37, this.getProtection(mc.player.getInventory().getStack(37)), -1, -1});
                  armorMap.put(EquipmentSlot.CHEST, new int[]{38, this.getProtection(mc.player.getInventory().getStack(38)), -1, -1});
                  armorMap.put(EquipmentSlot.HEAD, new int[]{39, this.getProtection(mc.player.getInventory().getStack(39)), -1, -1});

                  for (int s = 0; s < 36; s++) {
                     ItemStack stack = mc.player.getInventory().getStack(s);

                     if (!(stack.getItem() instanceof ArmorItem || stack.getItem() == Items.ELYTRA)) {
                        continue;
                     }

                     boolean allowElytra = stack.getItem() != Items.ELYTRA || shouldHandleAutoElytra();
                     if (!allowElytra) {
                        continue;
                     }

                     int protection = this.getProtection(stack);
                     EquipmentSlot slot = stack.getItem() instanceof ElytraItem
                             ? EquipmentSlot.CHEST
                             : ((ArmorItem) stack.getItem()).getSlotType();

                     for (Map.Entry<EquipmentSlot, int[]> e : armorMap.entrySet()) {
                        if (shouldHandleAutoElytra() && e.getKey() == EquipmentSlot.CHEST) {
                           if ((mc.player.getInventory().getStack(38).isEmpty()
                                   || !(mc.player.getInventory().getStack(38).getItem() instanceof ElytraItem)
                                   || !ElytraItem.isUsable(mc.player.getInventory().getStack(38)))
                                   && (e.getValue()[2] == -1
                                   || mc.player.getInventory().getStack(e.getValue()[2]).isEmpty()
                                   || !(mc.player.getInventory().getStack(e.getValue()[2]).getItem() instanceof ElytraItem)
                                   || !ElytraItem.isUsable(mc.player.getInventory().getStack(e.getValue()[2])))
                                   && !stack.isEmpty()
                                   && stack.getItem() instanceof ElytraItem
                                   && ElytraItem.isUsable(stack)) {
                              e.getValue()[2] = s;
                           }
                        } else if (protection > 0 && e.getKey() == slot && protection > e.getValue()[1] && protection > e.getValue()[3]) {
                           e.getValue()[2] = s;
                           e.getValue()[3] = protection;
                        }
                     }
                  }

                  for (Map.Entry<EquipmentSlot, int[]> equipmentSlotEntry : armorMap.entrySet()) {
                     if (equipmentSlotEntry.getValue()[2] != -1) {
                        if (equipmentSlotEntry.getValue()[2] < 9) {
                           switch ((HotbarSwapMode) this.hotbarSwap.getValue()) {
                              case Swap:
                                 int armorSlot = 44 - equipmentSlotEntry.getValue()[0];
                                 mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, armorSlot, equipmentSlotEntry.getValue()[2], SlotActionType.SWAP, mc.player);
                                 EntityUtil.syncInventory();
                                 break;
                              case Switch:
                                 int old = mc.player.getInventory().selectedSlot;
                                 InventoryUtil.switchToSlot(equipmentSlotEntry.getValue()[2]);
                                 sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Forever.ROTATION.getLastYaw(), Forever.ROTATION.getLastPitch()));
                                 InventoryUtil.switchToSlot(old);
                           }
                        } else if (mc.player.playerScreenHandler == mc.player.currentScreenHandler) {
                           int armorSlot = 44 - equipmentSlotEntry.getValue()[0];
                           int newArmorSlot = equipmentSlotEntry.getValue()[2];
                           switch ((InventorySwapMode) this.inventorySwap.getValue()) {
                              case ClickSlot:
                                 mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, newArmorSlot, 0, SlotActionType.PICKUP, mc.player);
                                 mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, armorSlot, 0, SlotActionType.PICKUP, mc.player);
                                 if (equipmentSlotEntry.getValue()[1] != -1) {
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, newArmorSlot, 0, SlotActionType.PICKUP, mc.player);
                                 }
                                 break;
                              case Pick:
                                 mc.getNetworkHandler().sendPacket(new PickFromInventoryC2SPacket(newArmorSlot));
                                 sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Forever.ROTATION.getLastYaw(), Forever.ROTATION.getLastPitch()));
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

   @EventListener
   public void onRender2D(Render2DEvent event) {
      if (mc.player == null || mc.world == null) return;
      if (!this.switchArmor.getValue() || !this.notifyMode.is(NotifyMode.Hud)) return;
      if (currentDisplayType == EnchantType.NONE) return;

      String text = "Armor: ";
      int color = 0xFFFFFF;

      if (currentDisplayType == EnchantType.PROTECTION) {
         text += "Protection";
         color = 0x55FF55;
      } else if (currentDisplayType == EnchantType.BLAST_PROTECTION) {
         text += "Blast Protection";
         color = 0xFF5555;
      }

      int textWidth = mc.textRenderer.getWidth(text);
      int screenWidth = mc.getWindow().getScaledWidth();
      int screenHeight = mc.getWindow().getScaledHeight();
      int x = (int) (screenWidth * (hudX.getValue() / 100.0)) - (textWidth / 2);
      int y = (int) (screenHeight * (hudY.getValue() / 100.0));
      x = Math.max(0, Math.min(x, screenWidth - textWidth));
      y = Math.max(0, Math.min(y, screenHeight - mc.textRenderer.fontHeight));

      event.drawContext.drawTextWithShadow(mc.textRenderer, text, x, y, color);
   }

   private void executeSwitchTask(SwitchTask task) {
      int armorScreenSlot = 44 - task.armorInventoryIndex;
      int itemSlot = task.inventorySlot;

      if (itemSlot < 9) {
         switch ((HotbarSwapMode) this.hotbarSwap.getValue()) {
            case Swap:
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, armorScreenSlot, itemSlot, SlotActionType.SWAP, mc.player);
               EntityUtil.syncInventory();
               break;
            case Switch:
               int old = mc.player.getInventory().selectedSlot;
               InventoryUtil.switchToSlot(itemSlot);
               sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Forever.ROTATION.getLastYaw(), Forever.ROTATION.getLastPitch()));
               InventoryUtil.switchToSlot(old);
               break;
         }
      } else {
         switch ((InventorySwapMode) this.inventorySwap.getValue()) {
            case ClickSlot:
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, itemSlot, 0, SlotActionType.PICKUP, mc.player);
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, armorScreenSlot, 0, SlotActionType.PICKUP, mc.player);
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, itemSlot, 0, SlotActionType.PICKUP, mc.player);
               break;
            case Pick:
               mc.getNetworkHandler().sendPacket(new PickFromInventoryC2SPacket(itemSlot));
               sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Forever.ROTATION.getLastYaw(), Forever.ROTATION.getLastPitch()));
               mc.getNetworkHandler().sendPacket(new PickFromInventoryC2SPacket(itemSlot));
               break;
         }
         EntityUtil.syncInventory();
      }
   }

   private int getProtection(ItemStack is) {
      if (is.getItem() instanceof ArmorItem || is.getItem() == Items.ELYTRA) {
         int prot = 0;
         EquipmentSlot slot = is.getItem() instanceof ArmorItem ai ? ai.getSlotType() : EquipmentSlot.BODY;
         if (is.getItem() instanceof ElytraItem) {
            if (!ElytraItem.isUsable(is)) return 0;
            prot = 1;
         }

         int blastMultiplier = 1, protectionMultiplier = 1;
         switch (slot) {
            case HEAD:
               if (this.head.is(EnchantPriority.Protection)) protectionMultiplier *= 2;
               else blastMultiplier *= 2;
               break;
            case BODY:
               if (this.body.is(EnchantPriority.Protection)) protectionMultiplier *= 2;
               else blastMultiplier *= 2;
               break;
            case LEGS:
               if (this.tights.is(EnchantPriority.Protection)) protectionMultiplier *= 2;
               else blastMultiplier *= 2;
               break;
            case FEET:
               if (this.feet.is(EnchantPriority.Protection)) protectionMultiplier *= 2;
               else blastMultiplier *= 2;
               break;
         }

         if (is.hasEnchantments()) {
            ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(is);
            if (enchants.getEnchantments().contains(mc.world.getRegistryManager().getWrapperOrThrow(Enchantments.PROTECTION.getRegistryRef()).getOptional(Enchantments.PROTECTION).get())) {
               prot += enchants.getLevel((RegistryEntry) mc.world.getRegistryManager().getWrapperOrThrow(Enchantments.PROTECTION.getRegistryRef()).getOptional(Enchantments.PROTECTION).get()) * protectionMultiplier;
            }
            if (enchants.getEnchantments().contains(mc.world.getRegistryManager().getWrapperOrThrow(Enchantments.BLAST_PROTECTION.getRegistryRef()).getOptional(Enchantments.BLAST_PROTECTION).get())) {
               prot += enchants.getLevel(mc.world.getRegistryManager().getWrapperOrThrow(Enchantments.BLAST_PROTECTION.getRegistryRef()).getOptional(Enchantments.BLAST_PROTECTION).get()) * blastMultiplier;
            }
            if (enchants.getEnchantments().contains(mc.world.getRegistryManager().getWrapperOrThrow(Enchantments.BLAST_PROTECTION.getRegistryRef()).getOptional(Enchantments.BINDING_CURSE).get()) && this.ignoreCurse.getValue()) {
               prot = -999;
            }
         }
         return (is.getItem() instanceof ArmorItem ? ((ArmorItem) is.getItem()).getProtection() : 0) + prot;
      } else {
         return !is.isEmpty() ? 0 : -1;
      }
   }

   private static class SwitchTask {
      final EquipmentSlot slot;
      final int armorInventoryIndex;
      final int inventorySlot;

      SwitchTask(EquipmentSlot slot, int armorInventoryIndex, int inventorySlot) {
         this.slot = slot;
         this.armorInventoryIndex = armorInventoryIndex;
         this.inventorySlot = inventorySlot;
      }
   }

   public enum AutoSwitchMode {
      Item, InsideBlock
   }

   public enum NotifyMode {
      Chat, Hud, None
   }

   private enum EnchantType { PROTECTION, BLAST_PROTECTION, NONE }
   private enum EnchantPriority { Blast, Protection }
   public enum HotbarSwapMode { Swap, Switch }
   public enum InventorySwapMode { ClickSlot, Pick }
}