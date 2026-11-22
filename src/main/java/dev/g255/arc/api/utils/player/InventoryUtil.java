package dev.g255.arc.api.utils.player;

import dev.g255.arc.api.utils.Wrapper;
import dev.g255.arc.api.utils.world.BlockUtil;
import dev.g255.arc.mod.modules.impl.client.AntiCheat;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.network.packet.c2s.play.PickFromInventoryC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.component.DataComponentTypes;

public class InventoryUtil implements Wrapper {
   static int lastSlot = -1;
   static int lastSelect = -1;

   public static void inventorySwap(int slot, int selectedSlot) {
      if (slot == lastSlot) {
         switchToSlot(lastSelect);
         lastSlot = -1;
         lastSelect = -1;
      } else if (slot - 36 != selectedSlot) {
         if (EntityUtil.inInventory()) {
            if (AntiCheat.INSTANCE.invSwapBypass.getValue()) {
               if (slot - 36 >= 0) {
                  lastSlot = slot;
                  lastSelect = selectedSlot;
                  switchToSlot(slot - 36);
                  return;
               }

               mc.getNetworkHandler().sendPacket(new PickFromInventoryC2SPacket(slot));
            } else {
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, selectedSlot, SlotActionType.SWAP, mc.player);
               mc.player.getInventory().updateItems();
            }

         }
      }
   }

   public static void switchToSlot(int slot) {
      // 已经是这个槽位了就不要再发：重复的 UpdateSelectedSlot 在服务端看来
      // 就是「一直在切快捷栏」，是发包检测很爱抓的一种形状。
      // 客户端状态本来就和目标一致，不发这一包不会不同步。
      if (mc.player.getInventory().selectedSlot == slot) {
         return;
      }

      mc.player.getInventory().selectedSlot = slot;
      mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
   }

   public static int findItem(Item input) {
      for(int i = 0; i < 9; ++i) {
         Item item = mc.player.getInventory().getStack(i).getItem();
         if (Item.getRawId(item) == Item.getRawId(input)) {
            return i;
         }
      }

      return -1;
   }

   public static int getFood() {
      for(int i = 0; i < 9; ++i) {
         if (mc.player.getInventory().getStack(i).getComponents().contains(DataComponentTypes.FOOD)) {
            return i;
         }
      }

      return -1;
   }

   public static int getPotionCount(StatusEffect targetEffect) {
      int count = 0;

      for(int i = 35; i >= 0; --i) {
         ItemStack itemStack = mc.player.getInventory().getStack(i);
         if (Item.getRawId(itemStack.getItem()) == Item.getRawId(Items.SPLASH_POTION)) {
            PotionContentsComponent potionContentsComponent = (PotionContentsComponent)itemStack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);

            for(StatusEffectInstance effect : potionContentsComponent.getEffects()) {
               if (effect.getEffectType().value() == targetEffect) {
                  count += itemStack.getCount();
               }
            }
         }
      }

      return count;
   }

   public static int getItemCount(Class<?> clazz) {
      int count = 0;

      for(Map.Entry<Integer, ItemStack> entry : getInventoryAndHotbarSlots().entrySet()) {
         if (((ItemStack)entry.getValue()).getItem() instanceof BlockItem && clazz.isInstance(((BlockItem)((ItemStack)entry.getValue()).getItem()).getBlock())) {
            count += ((ItemStack)entry.getValue()).getCount();
         }
      }

      return count;
   }

   public static int getItemCount(Item item) {
      int count = 0;

      for(Map.Entry<Integer, ItemStack> entry : getInventoryAndHotbarSlots().entrySet()) {
         if (((ItemStack)entry.getValue()).getItem() == item) {
            count += ((ItemStack)entry.getValue()).getCount();
         }
      }

      if (mc.player.getOffHandStack().getItem() == item) {
         count += mc.player.getOffHandStack().getCount();
      }

      return count;
   }

   public static int findClass(Class<?> clazz) {
      for(int i = 0; i < 9; ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack != ItemStack.EMPTY) {
            if (clazz.isInstance(stack.getItem())) {
               return i;
            }

            if (stack.getItem() instanceof BlockItem && clazz.isInstance(((BlockItem)stack.getItem()).getBlock())) {
               return i;
            }
         }
      }

      return -1;
   }

   public static int findClassInventorySlot(Class<?> clazz) {
      if (AntiCheat.INSTANCE.priorHotbar.getValue()) {
         for(int i = 0; i < 36; ++i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack != ItemStack.EMPTY) {
               if (clazz.isInstance(stack.getItem())) {
                  return i < 9 ? i + 36 : i;
               }

               if (stack.getItem() instanceof BlockItem && clazz.isInstance(((BlockItem)stack.getItem()).getBlock())) {
                  return i < 9 ? i + 36 : i;
               }
            }
         }
      } else {
         for(int i = 35; i >= 0; --i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack != ItemStack.EMPTY) {
               if (clazz.isInstance(stack.getItem())) {
                  return i < 9 ? i + 36 : i;
               }

               if (stack.getItem() instanceof BlockItem && clazz.isInstance(((BlockItem)stack.getItem()).getBlock())) {
                  return i < 9 ? i + 36 : i;
               }
            }
         }
      }

      return -1;
   }

   public static int findBlock(Block blockIn) {
      for(int i = 0; i < 9; ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack != ItemStack.EMPTY && stack.getItem() instanceof BlockItem && ((BlockItem)stack.getItem()).getBlock() == blockIn) {
            return i;
         }
      }

      return -1;
   }

   /**
    * 在快捷栏里找列表中的第一块可用方块（Hachimi 的 BlockListSetting 走的就是这种「按列表找」）。
    * 列表顺序即优先级：{@code Blocks} 里排在前面的先被使用。
    */
   public static int findBlockInList(java.util.List<Block> blocks) {
      if (blocks == null || blocks.isEmpty()) {
         return -1;
      }

      for(Block block : blocks) {
         for(int i = 0; i < 9; ++i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem && ((BlockItem)stack.getItem()).getBlock() == block) {
               return i;
            }
         }
      }

      return -1;
   }

   /** 背包版（返回的是界面槽位号，配合 {@link #inventorySwap} 使用） */
   public static int findBlockInListInventorySlot(java.util.List<Block> blocks) {
      if (blocks == null || blocks.isEmpty()) {
         return -1;
      }

      for(Block block : blocks) {
         int slot = findBlockInventorySlot(block);
         if (slot != -1) {
            return slot;
         }
      }

      return -1;
   }

   public static int findUnBlock() {
      for(int i = 0; i < 9; ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!(stack.getItem() instanceof BlockItem)) {
            return i;
         }
      }

      return -1;
   }

   public static int findBlock() {
      for(int i = 0; i < 9; ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() instanceof BlockItem && !BlockUtil.isClickable(Block.getBlockFromItem(stack.getItem())) && ((BlockItem)stack.getItem()).getBlock() != Blocks.COBWEB) {
            return i;
         }
      }

      return -1;
   }

   public static int findBlockInventorySlot(Block block) {
      return findItemInventorySlot(block.asItem());
   }

   public static int findItemInventorySlot(Item item) {
      if (AntiCheat.INSTANCE.priorHotbar.getValue()) {
         return findItemInventorySlotFromZero(item.asItem());
      } else {
         for(int i = 35; i >= 0; --i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) {
               return i < 9 ? i + 36 : i;
            }
         }

         return -1;
      }
   }

   public static int findItemInventorySlotFromZero(Item item) {
      for(int i = 0; i < 36; ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() == item) {
            return i < 9 ? i + 36 : i;
         }
      }

      return -1;
   }

   public static Map<Integer, ItemStack> getInventoryAndHotbarSlots() {
      HashMap<Integer, ItemStack> fullInventorySlots = new HashMap();

      for(int current = 0; current <= 35; ++current) {
         fullInventorySlots.put(current, mc.player.getInventory().getStack(current));
      }

      return fullInventorySlots;
   }

   public static int getEmptySlotCount() {
      if (mc.player == null) {
         return 0;
      }

      int count = 0;

      for(Map.Entry<Integer, ItemStack> entry : getInventoryAndHotbarSlots().entrySet()) {
         if (entry.getValue() != ItemStack.EMPTY) {
            continue;
         }

         ++count;
      }

      return count;
   }
}
