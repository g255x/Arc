package dev.kissoo.mod.modules.impl.player;

import dev.kissoo.Forever;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.utils.math.Timer;
import dev.kissoo.core.Manager;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import dev.kissoo.mod.modules.settings.impl.StringSetting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Sorter extends Module {
   public static Sorter INSTANCE;

   private final StringSetting kitName = this.add(new StringSetting("KitName", "default"));
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 100, 0, 1000).setSuffix("ms"));
   private final SliderSetting safeRange = this.add(new SliderSetting("Range", 10, 0, 64).setSuffix("m"));
   private final BooleanSetting dropExtras = this.add(new BooleanSetting("DropExtras", true));

   // --- XCarry 相关设置 ---
   private final BooleanSetting xCarry = this.add(new BooleanSetting("XCarry", false).setParent());
   private final EnumSetting<XCarryMode> xCarryMode = this.add(new EnumSetting<>("XCarryMode", XCarryMode.NetheriteArmor, xCarry::isOpen));
   private final SliderSetting xCarryDelay = this.add(new SliderSetting("XCarryDelay", 50, 0, 500, xCarry::isOpen).setSuffix("ms"));

   // 各个模式的保留阈值设定（已去除后缀）
   private final SliderSetting armorKeepSets = this.add(new SliderSetting("ArmorKeepSets", 1, 0, 5, () -> xCarry.isOpen() && xCarryMode.is(XCarryMode.NetheriteArmor)));
   private final SliderSetting crystalKeepStacks = this.add(new SliderSetting("CrystalKeepStacks", 1, 0, 10, () -> xCarry.isOpen() && xCarryMode.is(XCarryMode.Crystal)));
   private final SliderSetting turtleKeepAmount = this.add(new SliderSetting("TurtleKeepAmount", 1, 0, 10, () -> xCarry.isOpen() && xCarryMode.is(XCarryMode.TurtlePotion)));
   private final SliderSetting totemKeepAmount = this.add(new SliderSetting("TotemKeepAmount", 2, 0, 10, () -> xCarry.isOpen() && xCarryMode.is(XCarryMode.Totem)));

   private final Timer timer = new Timer();
   private final Timer xCarryTimer = new Timer();
   private final Map<Integer, Item> targetKit = new HashMap<>();

   public enum XCarryMode {
      NetheriteArmor, Crystal, TurtlePotion, Totem
   }

   public Sorter() {
      super("Sorter", Category.Player);
      this.setChinese("背包整理");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      if (mc.player == null) return;
      loadKit(kitName.getValue());
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (mc.player == null || mc.world == null) return;
      if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
         return;
      }

      if (!isSafe()) return;

      // 优先执行 XCarry 逻辑，附带独立的 XCarry 延迟
      if (xCarry.getValue() && xCarryTimer.passed(xCarryDelay.getValueInt())) {
         if (doXCarryLogic()) {
            xCarryTimer.reset();
            return; // 只要执行了操作就直接 return 等待下一个延迟，保证不瞬发
         }
      }

      // Sorter 自身的整理逻辑（同样吃其自带的 delay）
      if (timer.passed(delay.getValueInt())) {
         if (doCleanLogic()) {
            timer.reset();
         } else if (doSortLogic()) {
            timer.reset();
         }
      }
   }

   private boolean doXCarryLogic() {
      switch (xCarryMode.getValue()) {
         case NetheriteArmor:
            return handleXCarryArmor();
         case Crystal:
            return handleXCarryItem(Items.END_CRYSTAL);
         case TurtlePotion:
            return handleXCarryTurtlePotion();
         case Totem:
            return handleXCarryTotem();
      }
      return false;
   }

   // ----- XCarry 逻辑实现（带保留阈值与防死循环算法） -----

   private boolean handleXCarryArmor() {
      Item[] targets = {Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS};
      int targetSets = armorKeepSets.getValueInt(); // 目标保留套数

      for (int i = 0; i < 4; i++) {
         int craftSlot = i + 1; // 合成栏 1-4
         Item target = targets[i];
         ItemStack craftStack = mc.player.playerScreenHandler.getSlot(craftSlot).getStack();

         // 统计全身（装备栏 5-8 + 背包 9-44）该部位的总数
         int count = countArmorInInvAndEquipped(target);

         if (count < targetSets) {
            // 身上不够目标套数，必须从合成栏取回
            if (craftStack.getItem() == target) {
               shiftClickSlot(craftSlot);
               return true;
            }
         } else if (count > targetSets) {
            // 身上多余了目标套数，往合成栏存放
            if (craftStack.isEmpty()) {
               int invSlot = findItemInInv(target);
               if (invSlot != -1) {
                  moveToCraft(invSlot, craftSlot);
                  return true;
               }
            } else if (craftStack.getItem() != target) {
               // 把占位置的杂物踢下来
               shiftClickSlot(craftSlot);
               return true;
            }
         } else {
            // 身上恰好达到设定套数（处于稳定保留状态），维持不变
            // 如果合成栏有杂物，为了保持整洁，踢下来
            if (!craftStack.isEmpty() && craftStack.getItem() != target) {
               shiftClickSlot(craftSlot);
               return true;
            }
         }
      }
      return false;
   }

   private boolean handleXCarryItem(Item targetItem) {
      // 统计身上（主背包和快捷栏 9-44）水晶的总个数
      int total = countTotalItemsInInv(targetItem);
      int targetCount = crystalKeepStacks.getValueInt() * 64; // 目标保留数量 = 设定的组数 * 64

      for (int i = 1; i <= 4; i++) {
         ItemStack craftStack = mc.player.playerScreenHandler.getSlot(i).getStack();

         if (total < targetCount) {
            // 身上没达到设定的保留组数，从合成栏拿回补充
            if (craftStack.isOf(targetItem)) {
               shiftClickSlot(i);
               return true;
            }
         } else if (total > targetCount) {
            // 身上超出了保留组数，往合成栏存放多余的水晶
            if (!craftStack.isEmpty() && !craftStack.isOf(targetItem)) {
               // 把占位杂物踢下来
               shiftClickSlot(i);
               return true;
            }

            // 寻找一堆可以放进合成栏的水晶（前提是放进去后，身上依然>=设定组数，杜绝鬼畜死循环）
            int invSlot = findSafeStackToPush(targetItem, total, targetCount);
            if (invSlot != -1) {
               if (craftStack.isEmpty()) {
                  moveToCraft(invSlot, i);
                  return true;
               } else if (craftStack.getCount() < 64) {
                  // 合成栏未满，将身上的合过去
                  mergeToCraft(invSlot, i);
                  return true;
               }
            }
         } else {
            // 身上恰好满足设定的组数，不需要存也不需要取
            if (!craftStack.isEmpty() && !craftStack.isOf(targetItem)) {
               shiftClickSlot(i);
               return true;
            }
         }
      }
      return false;
   }

   private boolean handleXCarryTurtlePotion() {
      // 统计身上神龟药水的瓶数 (9-44)
      int count = countTurtlePotionInInv();
      int targetCount = turtleKeepAmount.getValueInt();

      for (int i = 1; i <= 4; i++) {
         ItemStack craftStack = mc.player.playerScreenHandler.getSlot(i).getStack();
         boolean isTurtle = isTurtlePotion(craftStack);

         if (count < targetCount) {
            // 不够设定瓶数，从合成栏拿回
            if (isTurtle) {
               shiftClickSlot(i);
               return true;
            }
         } else if (count > targetCount) {
            // 超出设定瓶数，放进合成栏
            if (craftStack.isEmpty()) {
               int invSlot = findTurtlePotionInInv();
               if (invSlot != -1) {
                  moveToCraft(invSlot, i);
                  return true;
               }
            } else if (!isTurtle) {
               // 踢下杂物
               shiftClickSlot(i);
               return true;
            }
         } else {
            // 处于稳定状态，踢下杂物
            if (!craftStack.isEmpty() && !isTurtle) {
               shiftClickSlot(i);
               return true;
            }
         }
      }
      return false;
   }

   private boolean handleXCarryTotem() {
      // 统计图腾总数（9~45，囊括主手副手）
      int count = countTotems();
      int targetCount = totemKeepAmount.getValueInt();

      for (int i = 1; i <= 4; i++) {
         ItemStack craftStack = mc.player.playerScreenHandler.getSlot(i).getStack();
         boolean isTotem = craftStack.getItem() == Items.TOTEM_OF_UNDYING;

         if (count < targetCount) {
            // 不够设定个数，从合成栏拿回
            if (isTotem) {
               shiftClickSlot(i);
               return true;
            }
         } else if (count > targetCount) {
            // 超出设定个数，放进合成栏
            if (craftStack.isEmpty()) {
               // 安全机制：只从背包和快捷栏寻找图腾（9~44），绝对不动副手(45)上的图腾
               int invSlot = findTotemToMove();
               if (invSlot != -1) {
                  moveToCraft(invSlot, i);
                  return true;
               }
            } else if (!isTotem) {
               // 踢下杂物
               shiftClickSlot(i);
               return true;
            }
         } else {
            // 处于稳定状态，清理合成栏异物
            if (!craftStack.isEmpty() && !isTotem) {
               shiftClickSlot(i);
               return true;
            }
         }
      }
      return false;
   }

   // --- 工具人辅助方法 ---

   private void shiftClickSlot(int slot) {
      mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, 0, SlotActionType.QUICK_MOVE, mc.player);
   }

   private void moveToCraft(int invSlot, int emptyCraftSlot) {
      int syncId = mc.player.playerScreenHandler.syncId;
      mc.interactionManager.clickSlot(syncId, invSlot, 0, SlotActionType.PICKUP, mc.player);
      mc.interactionManager.clickSlot(syncId, emptyCraftSlot, 0, SlotActionType.PICKUP, mc.player);
   }

   private void mergeToCraft(int invSlot, int craftSlot) {
      int syncId = mc.player.playerScreenHandler.syncId;
      mc.interactionManager.clickSlot(syncId, invSlot, 0, SlotActionType.PICKUP, mc.player);
      mc.interactionManager.clickSlot(syncId, craftSlot, 0, SlotActionType.PICKUP, mc.player);
      // 把鼠标上残留的再放回背包原位
      mc.interactionManager.clickSlot(syncId, invSlot, 0, SlotActionType.PICKUP, mc.player);
   }

   private int countArmorInInvAndEquipped(Item item) {
      int count = 0;
      // 遍历 5~44 囊括了装备栏(5-8)、主背包(9-35)、快捷栏(36-44)
      for (int i = 5; i <= 44; i++) {
         if (mc.player.playerScreenHandler.getSlot(i).getStack().getItem() == item) {
            count++;
         }
      }
      return count;
   }

   private int countTotalItemsInInv(Item item) {
      int count = 0;
      // 只算主背包和快捷栏
      for (int i = 9; i <= 44; i++) {
         ItemStack stack = mc.player.playerScreenHandler.getSlot(i).getStack();
         if (stack.getItem() == item) {
            count += stack.getCount();
         }
      }
      return count;
   }

   private int countTurtlePotionInInv() {
      int count = 0;
      for (int i = 9; i <= 44; i++) {
         if (isTurtlePotion(mc.player.playerScreenHandler.getSlot(i).getStack())) {
            count++;
         }
      }
      return count;
   }

   private int countTotems() {
      int count = 0;
      // 囊括主背包(9~35)、快捷栏(36~44)、副手(45)
      for (int i = 9; i <= 45; i++) {
         if (mc.player.playerScreenHandler.getSlot(i).getStack().getItem() == Items.TOTEM_OF_UNDYING) {
            count++;
         }
      }
      return count;
   }

   private int findItemInInv(Item item) {
      for (int i = 9; i <= 44; i++) {
         if (mc.player.playerScreenHandler.getSlot(i).getStack().getItem() == item) {
            return i;
         }
      }
      return -1;
   }

   // 寻找可以推往合成栏且不导致自身跌破阈值的最优堆栈
   private int findSafeStackToPush(Item targetItem, int total, int targetCount) {
      int bestSlot = -1;
      int maxCount = -1;
      for (int i = 9; i <= 44; i++) {
         ItemStack stack = mc.player.playerScreenHandler.getSlot(i).getStack();
         if (stack.getItem() == targetItem) {
            // 核心公式：必须保证把这堆（即便是散格）全塞进合成栏后，身上的剩余总数依然 >= 目标保留数
            if (total - stack.getCount() >= targetCount) {
               if (stack.getCount() > maxCount) {
                  maxCount = stack.getCount();
                  bestSlot = i;
               }
            }
         }
      }
      return bestSlot;
   }

   private int findTurtlePotionInInv() {
      for (int i = 9; i <= 44; i++) {
         if (isTurtlePotion(mc.player.playerScreenHandler.getSlot(i).getStack())) {
            return i;
         }
      }
      return -1;
   }

   private int findTotemToMove() {
      // 为了安全考虑，只从 9~44 (背包和快捷栏) 取出图腾扔进合成栏，决不动副手(45)
      for (int i = 9; i <= 44; i++) {
         if (mc.player.playerScreenHandler.getSlot(i).getStack().getItem() == Items.TOTEM_OF_UNDYING) {
            return i;
         }
      }
      return -1;
   }

   private boolean isTurtlePotion(ItemStack stack) {
      if (stack.isEmpty()) return false;
      if (stack.getItem() == Items.POTION || stack.getItem() == Items.SPLASH_POTION || stack.getItem() == Items.LINGERING_POTION) {
         PotionContentsComponent potionContents = stack.get(DataComponentTypes.POTION_CONTENTS);
         if (potionContents != null && potionContents.potion().isPresent()) {
            String potionId = potionContents.potion().get().getKey().get().getValue().toString();
            // 通过获取到的原版键名字符串来快速判断神龟
            return potionId.contains("turtle_master");
         }
      }
      return false;
   }

   // ----- Sorter 自身的整理逻辑保持不变 -----

   private boolean isSafe() {
      if (safeRange.getValue() <= 0) return true;
      double rangeSq = Math.pow(safeRange.getValue(), 2);
      for (PlayerEntity player : mc.world.getPlayers()) {
         if (player == mc.player) continue;
         if (Forever.FRIEND.isFriend(player)) continue;
         if (mc.player.squaredDistanceTo(player.getPos()) < rangeSq) {
            return false;
         }
      }
      return true;
   }

   public static boolean canMerge(ItemStack stack1, ItemStack stack2) {
      if (stack1.isEmpty() || stack2.isEmpty()) return false;
      if (!stack1.isOf(stack2.getItem())) return false;
      if (!ItemStack.areItemsAndComponentsEqual(stack1, stack2)) return false;
      return stack1.isStackable() && stack1.getCount() < stack1.getMaxCount();
   }

   private boolean isPiston(Item item) {
      return item == Items.PISTON || item == Items.STICKY_PISTON;
   }

   private boolean isLeatherChestplate(Item item) { return item == Items.LEATHER_CHESTPLATE; }
   private boolean isChainmailChestplate(Item item) { return item == Items.CHAINMAIL_CHESTPLATE; }
   private boolean isIronChestplate(Item item) { return item == Items.IRON_CHESTPLATE; }
   private boolean isGoldenChestplate(Item item) { return item == Items.GOLDEN_CHESTPLATE; }
   private boolean isDiamondChestplate(Item item) { return item == Items.DIAMOND_CHESTPLATE; }
   private boolean isNetheriteChestplate(Item item) { return item == Items.NETHERITE_CHESTPLATE; }

   private boolean isShulkerBox(Item item) {
      return Registries.ITEM.getId(item).getPath().contains("shulker_box");
   }

   private boolean isChestplate(Item item) {
      return item == Items.ELYTRA || isLeatherChestplate(item) || isChainmailChestplate(item) ||
              isIronChestplate(item) || isGoldenChestplate(item) || isDiamondChestplate(item) || isNetheriteChestplate(item);
   }

   private boolean itemsMatch(Item item1, Item item2) {
      if (item1 == item2) return true;
      if (isPiston(item1) && isPiston(item2)) return true;
      if (isChestplate(item1) && isChestplate(item2)) return true;
      if (isShulkerBox(item1) && isShulkerBox(item2)) return true;
      return false;
   }

   private boolean doCleanLogic() {
      if (!dropExtras.getValue()) return false;
      for (int slot = 0; slot < 36; slot++) {
         ItemStack stack = mc.player.getInventory().getStack(slot);
         if (stack.isEmpty()) continue;

         Item currentItem = stack.getItem();
         Item desiredItem = targetKit.getOrDefault(slot, Items.AIR);
         if (itemsMatch(currentItem, desiredItem)) continue;

         if (isItemNeededElsewhere(currentItem)) {
            continue;
         }
         throwItem(slot);
         return true;
      }
      return false;
   }

   private boolean doSortLogic() {
      for (int i = 0; i < 36; i++) {
         if (!targetKit.containsKey(i)) continue;

         Item targetItem = targetKit.get(i);
         ItemStack currentStack = mc.player.getInventory().getStack(i);
         if (itemsMatch(currentStack.getItem(), targetItem)) continue;
         if (targetItem == Items.AIR) continue;

         int sourceSlot = findBestSourceSlot(targetItem, i);
         if (sourceSlot != -1) {
            swap(i, sourceSlot);
            return true;
         }
      }
      return false;
   }

   private boolean isItemNeededElsewhere(Item item) {
      for (Map.Entry<Integer, Item> entry : targetKit.entrySet()) {
         int slotIndex = entry.getKey();
         if (slotIndex >= 36) continue;

         Item needed = entry.getValue();
         boolean isNeeded = itemsMatch(needed, item);

         if (isNeeded) {
            ItemStack stackInSlot = mc.player.getInventory().getStack(slotIndex);
            boolean hasNeeded = itemsMatch(stackInSlot.getItem(), needed);

            if (!hasNeeded) {
               return true;
            }
         }
      }
      return false;
   }

   private int findBestSourceSlot(Item targetItem, int ignoreSlot) {
      for (int i = 0; i < 36; i++) {
         if (i == ignoreSlot) continue;
         ItemStack stack = mc.player.getInventory().getStack(i);
         boolean itemMatches = itemsMatch(stack.getItem(), targetItem);

         if (itemMatches) {
            Item neededAtSource = targetKit.getOrDefault(i, Items.AIR);
            boolean sourceNeedsThis = itemsMatch(neededAtSource, targetItem);

            if (!sourceNeedsThis) {
               return i;
            }
         }
      }
      return -1;
   }

   private void throwItem(int slot) {
      int uiSlot = mapToUiSlot(slot);
      if (uiSlot == -1) return;

      mc.interactionManager.clickSlot(
              mc.player.playerScreenHandler.syncId,
              uiSlot,
              1,
              SlotActionType.THROW,
              mc.player
      );
   }

   private void swap(int slotA, int slotB) {
      int uiSlotA = mapToUiSlot(slotA);
      int uiSlotB = mapToUiSlot(slotB);
      if (uiSlotA == -1 || uiSlotB == -1) return;

      int syncId = mc.player.playerScreenHandler.syncId;

      mc.interactionManager.clickSlot(syncId, uiSlotA, 0, SlotActionType.PICKUP, mc.player);
      mc.interactionManager.clickSlot(syncId, uiSlotB, 0, SlotActionType.PICKUP, mc.player);
      mc.interactionManager.clickSlot(syncId, uiSlotA, 0, SlotActionType.PICKUP, mc.player);
   }

   private int mapToUiSlot(int inventoryIndex) {
      if (inventoryIndex < 0 || inventoryIndex > 35) return -1;
      if (inventoryIndex < 9) return 36 + inventoryIndex;
      return inventoryIndex;
   }

   private void loadKit(String name) {
      targetKit.clear();
      try {
         File file = Manager.getFile(name + ".kit");
         if (!file.exists()) {
            this.sendMessage("§cKit not found: " + name);
            return;
         }

         BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
         String line;
         while ((line = reader.readLine()) != null) {
            String[] parts = line.split(":");
            if (parts.length >= 3) {
               try {
                  int slot = Integer.parseInt(parts[0]);
                  if (slot >= 0 && slot < 36) {
                     String idStr = parts[1] + ":" + parts[2];
                     Identifier identifier = Identifier.of(idStr);
                     Item item = Registries.ITEM.get(identifier);
                     targetKit.put(slot, item);
                  }
               } catch (NumberFormatException ignored) {}
            }
         }
         reader.close();
         this.sendMessage("§aSorter loaded kit: " + name);
      } catch (Exception e) {
         e.printStackTrace();
         this.sendMessage("§cError loading kit!");
      }
   }
}