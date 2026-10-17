package dev.baimaodaxia.mod.modules.impl.player;

import dev.baimaodaxia.Alien;
import dev.baimaodaxia.earthquake;
import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.UpdateEvent;
import dev.baimaodaxia.api.utils.math.Timer;
import dev.baimaodaxia.api.utils.player.EntityUtil;
import dev.baimaodaxia.api.utils.player.InventoryUtil;
import dev.baimaodaxia.core.Manager;
import dev.baimaodaxia.mod.gui.windows.WindowBase;
import dev.baimaodaxia.mod.gui.windows.WindowsScreen;
import dev.baimaodaxia.mod.gui.windows.impl.ItemSelectWindow;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import dev.baimaodaxia.mod.modules.settings.impl.StringSetting;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.component.DataComponentTypes;
import org.apache.commons.io.IOUtils;
import net.minecraft.item.Item;

public class Sorter extends Module {
   public static Sorter INSTANCE;
   final int[] stealCountList;
   private final SliderSetting tasksPerTicks;
   private final SliderSetting delay;
   private final BooleanSetting stack;
   private final EnumSetting<Mode> trashMode;
   private final BooleanSetting edit;
   private final BooleanSetting sort;
   private final BooleanSetting kit;
   private final StringSetting kitName;
   private final BooleanSetting drop;
   private final BooleanSetting trash;
   private final BooleanSetting rename;
   private final BooleanSetting kitExceed;
   private final BooleanSetting exceed;
   private final SliderSetting crystal;
   private final SliderSetting exp;
   private final SliderSetting totem;
   private final SliderSetting eGapple;
   private final SliderSetting gapple;
   private final SliderSetting obsidian;
   private final SliderSetting web;
   private final SliderSetting glowstone;
   private final SliderSetting anchor;
   private final SliderSetting pearl;
   private final SliderSetting piston;
   private final SliderSetting redstone;
   private final SliderSetting ladder;
   private final SliderSetting bed;
   private final SliderSetting speed;
   private final SliderSetting turtle;
   private final SliderSetting strength;
   private final Timer timer;
   private final Map<Integer, String> kitMap;
   private String kitNameCache;
   private boolean kitLoaded;

   public Sorter() {
      this("Sorter", "背包整理");
   }

   protected Sorter(String name, String chinese) {
      super(name, Module.Category.Player);
      this.stealCountList = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
      this.tasksPerTicks = this.add(new SliderSetting("TasksPerTick", 2, 1, 20));
      this.delay = this.add((new SliderSetting("Delay", 0.1, (double)0.0F, (double)5.0F, 0.01)).setSuffix("s"));
      this.stack = this.add(new BooleanSetting("Stack", true));
      this.trashMode = this.add(new EnumSetting("TrashMode", Sorter.Mode.Whitelist));
      this.edit = this.add((new BooleanSetting("EditTrash", false)).injectTask(this::openGui));
      this.sort = this.add(new BooleanSetting("Sort", true));
      this.kit = this.add((new BooleanSetting("Kit", false)).injectTask(this::onEnable));
      this.kitName = this.add(new StringSetting("KitName", "kit1"));
      this.drop = this.add((new BooleanSetting("Drop", true)).setParent());
      BooleanSetting var10006 = this.drop;
      Objects.requireNonNull(var10006);
      this.trash = this.add(new BooleanSetting("Trash", true, var10006::isOpen));
      var10006 = this.drop;
      Objects.requireNonNull(var10006);
      this.rename = this.add(new BooleanSetting("Rename", true, var10006::isOpen));
      var10006 = this.drop;
      Objects.requireNonNull(var10006);
      this.kitExceed = this.add(new BooleanSetting("KitExceed", true, var10006::isOpen));
      var10006 = this.drop;
      Objects.requireNonNull(var10006);
      this.exceed = this.add(new BooleanSetting("Exceed", true, var10006::isOpen));
      this.crystal = this.add(new SliderSetting("Crystal", 4, 0, 12, () -> this.drop.isOpen() && this.exceed.isOpen()));
      this.exp = this.add(new SliderSetting("Exp", 4, 0, 12, () -> this.drop.isOpen() && this.exceed.isOpen()));
      this.totem = this.add(new SliderSetting("Totem", 6, 0, 36, () -> this.drop.isOpen() && this.exceed.isOpen()));
      this.eGapple = this.add(new SliderSetting("EGapple", 2, 0, 12, () -> this.drop.isOpen() && this.exceed.isOpen()));
      this.gapple = this.add(new SliderSetting("Gapple", 2, 0, 12, () -> this.drop.isOpen() && this.exceed.isOpen()));
      this.obsidian = this.add(new SliderSetting("Obsidian", 1, 0, 12, () -> this.drop.isOpen() && this.exceed.isOpen()));
      this.web = this.add(new SliderSetting("Web", 1, 0, 12, () -> this.drop.isOpen() && this.exceed.isOpen()));
      this.glowstone = this.add(new SliderSetting("Glowstone", 1, 0, 12, () -> this.drop.isOpen() && this.exceed.isOpen()));
      this.anchor = this.add(new SliderSetting("Anchor", 1, 0, 12, () -> this.drop.isOpen() && this.exceed.isOpen()));
      this.pearl = this.add(new SliderSetting("Pearl", 1, 0, 8, () -> this.drop.isOpen() && this.exceed.isOpen()));
      this.piston = this.add(new SliderSetting("Piston", 1, 0, 12, () -> this.drop.isOpen() && this.exceed.isOpen()));
      this.redstone = this.add(new SliderSetting("RedStone", 1, 0, 12, () -> this.drop.isOpen() && this.exceed.isOpen()));
      this.ladder = this.add(new SliderSetting("Ladder", 2, 0, 12, () -> this.drop.isOpen() && this.exceed.isOpen()));
      this.bed = this.add(new SliderSetting("Bed", 4, 0, 12, () -> this.drop.isOpen() && this.exceed.isOpen()));
      this.speed = this.add(new SliderSetting("Speed", 1, 0, 8, () -> this.drop.isOpen() && this.exceed.isOpen()));
      this.turtle = this.add(new SliderSetting("Resistance", 1, 0, 8, () -> this.drop.isOpen() && this.exceed.isOpen()));
      this.strength = this.add(new SliderSetting("Strength", 1, 0, 8, () -> this.drop.isOpen() && this.exceed.isOpen()));
      this.timer = new Timer();
      this.kitMap = new ConcurrentHashMap();
      this.kitNameCache = "";
      this.kitLoaded = false;
      this.setChinese(chinese);
      if (this.getClass() == Sorter.class) {
         INSTANCE = this;
      }

   }

   public void onEnable() {
      if (!nullCheck()) {
         this.ensureKitLoaded();
      }
   }

   public static int getItemCount(Item item) {
      int count = 0;

      for(Map.Entry<Integer, ItemStack> entry : InventoryUtil.getInventoryAndHotbarSlots().entrySet()) {
         if (((ItemStack)entry.getValue()).getItem() == item) {
            ++count;
         }
      }

      if (mc.player.getOffHandStack().getItem() == item) {
         ++count;
      }

      return count;
   }

   public static int getItemCount(Class<?> clazz) {
      int count = 0;

      for(Map.Entry<Integer, ItemStack> entry : InventoryUtil.getInventoryAndHotbarSlots().entrySet()) {
         if (((ItemStack)entry.getValue()).getItem() instanceof BlockItem && clazz.isInstance(((BlockItem)((ItemStack)entry.getValue()).getItem()).getBlock())) {
            ++count;
         }
      }

      return count;
   }

   private void openGui() {
      this.edit.setValueWithoutTask(false);
      if (!nullCheck()) {
         mc.setScreen(new WindowsScreen(new WindowBase[]{new ItemSelectWindow(earthquake.CLEANER)}));
      }

   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.kit.getValue() || this.trashMode.is(Sorter.Mode.Kit)) {
         this.ensureKitLoaded();
      }

      if (this.timer.passedS(this.delay.getValue())) {
         if (!this.handleContainerSteal()) {
            if (EntityUtil.inInventory()) {
               if (this.exceed.getValue()) {
                  this.updateItem();
               }

               for(int i = 0; (double)i < this.tasksPerTicks.getValue(); ++i) {
                  this.tweak();
               }

            }
         }
      }
   }

   private boolean handleContainerSteal() {
      if (!this.kit.getValue()) {
         return false;
      } else {
         ScreenHandler handler = mc.player.currentScreenHandler;
         int containerSize = -1;
         if (handler instanceof GenericContainerScreenHandler) {
            containerSize = ((GenericContainerScreenHandler)handler).getInventory().size();
         } else if (handler instanceof ShulkerBoxScreenHandler) {
            containerSize = 27;
         }

         if (containerSize <= 0) {
            return false;
         } else {
            boolean moved = false;

            for(int i = 0; (double)i < this.tasksPerTicks.getValue(); ++i) {
               Slot target = null;

               for(Slot slot : handler.slots) {
                  if (slot.id >= containerSize) {
                     break;
                  }

                  if (slot.hasStack()) {
                     target = slot;
                     break;
                  }
               }

               if (target == null) {
                  break;
               }

               mc.interactionManager.clickSlot(handler.syncId, target.id, 0, SlotActionType.QUICK_MOVE, mc.player);
               moved = true;
            }

            if (moved) {
               this.timer.reset();
            }

            return moved;
         }
      }
   }

   private void tweak() {
      if (this.drop.getValue()) {
         for(int slot1 = 35; slot1 >= 0; --slot1) {
            ItemStack stack = mc.player.getInventory().getStack(slot1);
            if (!stack.isEmpty() && this.shouldDrop(stack)) {
               this.timer.reset();
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot1 < 9 ? slot1 + 36 : slot1, 1, SlotActionType.THROW, mc.player);
               return;
            }
         }
      }

      if (this.stack.getValue()) {
         for(int slot1 = 35; slot1 >= 9; --slot1) {
            ItemStack stack = mc.player.getInventory().getStack(slot1);
            if (!stack.isEmpty() && stack.isStackable() && stack.getCount() != stack.getMaxCount()) {
               for(int slot2 = 0; slot2 < 36; ++slot2) {
                  ItemStack stack2;
                  if (slot1 != slot2 && (stack2 = mc.player.getInventory().getStack(slot2)).getCount() != stack2.getMaxCount() && canMerge(stack, stack2)) {
                     mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot1, 0, SlotActionType.PICKUP, mc.player);
                     mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot2 < 9 ? slot2 + 36 : slot2, 0, SlotActionType.PICKUP, mc.player);
                     mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot1, 0, SlotActionType.PICKUP, mc.player);
                     this.timer.reset();
                     return;
                  }
               }
            }
         }
      }

      if (this.drop.getValue()) {
         for(int slot1 = 35; slot1 >= 0; --slot1) {
            ItemStack stack = mc.player.getInventory().getStack(slot1);
            if (!stack.isEmpty() && this.exceed.getValue() && this.exceed(stack, false)) {
               this.timer.reset();
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot1 < 9 ? slot1 + 36 : slot1, 1, SlotActionType.THROW, mc.player);
               return;
            }
         }
      }

      if (this.sort.getValue()) {
         if (this.kit.getValue()) {
            for(int slot1 = 0; slot1 < 36; ++slot1) {
               if (this.kitMap.containsKey(slot1)) {
                  String target = (String)this.kitMap.get(slot1);
                  String name = mc.player.getInventory().getStack(slot1).getItem().getTranslationKey();
                  if (!name.equals(target)) {
                     for(int slot2 = 0; slot2 < 36; ++slot2) {
                        String slot2Target = (String)this.kitMap.get(slot2);
                        ItemStack stack2 = mc.player.getInventory().getStack(slot2);
                        String itemID;
                        if (!stack2.isEmpty() && !(itemID = stack2.getItem().getTranslationKey()).equals(slot2Target) && itemID.equals(target)) {
                           mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot1 < 9 ? slot1 + 36 : slot1, 0, SlotActionType.PICKUP, mc.player);
                           mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot2 < 9 ? slot2 + 36 : slot2, 0, SlotActionType.PICKUP, mc.player);
                           mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot1 < 9 ? slot1 + 36 : slot1, 0, SlotActionType.PICKUP, mc.player);
                           this.timer.reset();
                           return;
                        }
                     }
                  }
               }
            }
         } else {
            for(int slot1 = 9; slot1 < 36; ++slot1) {
               int id = Item.getRawId(mc.player.getInventory().getStack(slot1).getItem());
               if (mc.player.getInventory().getStack(slot1).isEmpty()) {
                  id = 114514;
               }

               int minId;
               if ((minId = this.getMinId(slot1, id)) < id) {
                  for(int slot2 = 35; slot2 > slot1; --slot2) {
                     ItemStack stack3 = mc.player.getInventory().getStack(slot2);
                     if (!stack3.isEmpty() && Item.getRawId(stack3.getItem()) == minId) {
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot1, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot2, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot1, 0, SlotActionType.PICKUP, mc.player);
                        this.timer.reset();
                        return;
                     }
                  }
               }
            }
         }
      }

      if (this.drop.getValue() && this.kitExceed.getValue()) {
         for(int slot1 = 35; slot1 >= 0; --slot1) {
            if (this.kitMap.containsKey(slot1)) {
               ItemStack stack4 = mc.player.getInventory().getStack(slot1);
               BlockItem blockItem;
               Item item;
               if ((!this.exceed.getValue() || this.exceed(stack4, true)) && !stack4.isEmpty() && !(stack4.getItem() instanceof ArmorItem) && (!((item = stack4.getItem()) instanceof BlockItem) || !((blockItem = (BlockItem)item).getBlock() instanceof ShulkerBoxBlock)) && !stack4.getItem().getTranslationKey().equals(this.kitMap.get(slot1))) {
                  this.timer.reset();
                  mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot1 < 9 ? slot1 + 36 : slot1, 1, SlotActionType.THROW, mc.player);
                  return;
               }
            }
         }
      }

   }

   private boolean shouldDrop(ItemStack stack) {
      Item item = stack.getItem();
      BlockItem blockItem;
      if (this.trash.getValue() && (!(item instanceof BlockItem) || !((blockItem = (BlockItem)item).getBlock() instanceof ShulkerBoxBlock))) {
         if (this.trashMode.is(Sorter.Mode.Kit)) {
            if (this.kitMap.isEmpty()) {
               return false;
            }

            return !this.kitMap.containsValue(item.getTranslationKey());
         }

         boolean inList;
         if (!(inList = Alien.CLEANER.inList(item.getTranslationKey())) && this.trashMode.is(Sorter.Mode.Whitelist) || inList && this.trashMode.is(Sorter.Mode.Blacklist)) {
            return true;
         }
      }

      if (this.rename.getValue()) {
         if (this.trashMode.is(Sorter.Mode.Kit) && this.kitMap.containsValue(item.getTranslationKey())) {
            return false;
         } else {
            return stack.isStackable() && !stack.getName().getString().equals(item.getName().getString());
         }
      } else {
         return false;
      }
   }

   private void ensureKitLoaded() {
      String name = this.kitName.getValue();
      if (name == null) {
         name = "";
      }

      if (!this.kitLoaded || !name.equals(this.kitNameCache)) {
         this.kitNameCache = name;
         this.kitLoaded = true;
         final String finalName = name;
         earthquake.THREAD.execute(() -> {
            this.kitMap.clear();

            try {
               File file = Manager.getFile(finalName + ".kit");
               if (!file.exists()) {
                  return;
               }

               for(String s : IOUtils.readLines(new FileInputStream(file), StandardCharsets.UTF_8)) {
                  String[] split = s.split(":");
                  if (split.length == 2) {
                     this.kitMap.put(Integer.valueOf(split[0]), split[1]);
                  }
               }
            } catch (Exception e) {
               e.printStackTrace();
            }

         });
      }
   }

   private boolean exceed(ItemStack i, boolean dropOther) {
      if (i.getItem().equals(Items.END_CRYSTAL)) {
         if ((double)this.stealCountList[0] > this.crystal.getValue()) {
            int var21 = this.stealCountList[0]--;
            return true;
         } else {
            return false;
         }
      } else if (i.getItem().equals(Items.EXPERIENCE_BOTTLE)) {
         if ((double)this.stealCountList[1] > this.exp.getValue()) {
            int var20 = this.stealCountList[1]--;
            return true;
         } else {
            return false;
         }
      } else if (i.getItem().equals(Items.TOTEM_OF_UNDYING)) {
         if ((double)this.stealCountList[2] > this.totem.getValue()) {
            int var19 = this.stealCountList[2]--;
            return true;
         } else {
            return false;
         }
      } else if (i.getItem().equals(Items.ENCHANTED_GOLDEN_APPLE)) {
         if ((double)this.stealCountList[3] > this.eGapple.getValue()) {
            int var18 = this.stealCountList[3]--;
            return true;
         } else {
            return false;
         }
      } else if (i.getItem().equals(Blocks.OBSIDIAN.asItem())) {
         if ((double)this.stealCountList[4] > this.obsidian.getValue()) {
            int var17 = this.stealCountList[4]--;
            return true;
         } else {
            return false;
         }
      } else if (i.getItem().equals(Blocks.COBWEB.asItem())) {
         if ((double)this.stealCountList[5] > this.web.getValue()) {
            int var16 = this.stealCountList[5]--;
            return true;
         } else {
            return false;
         }
      } else if (i.getItem().equals(Blocks.GLOWSTONE.asItem())) {
         if ((double)this.stealCountList[6] > this.glowstone.getValue()) {
            int var15 = this.stealCountList[6]--;
            return true;
         } else {
            return false;
         }
      } else if (i.getItem().equals(Blocks.RESPAWN_ANCHOR.asItem())) {
         if ((double)this.stealCountList[7] > this.anchor.getValue()) {
            int var14 = this.stealCountList[7]--;
            return true;
         } else {
            return false;
         }
      } else if (i.getItem().equals(Items.ENDER_PEARL)) {
         if ((double)this.stealCountList[8] > this.pearl.getValue()) {
            int var13 = this.stealCountList[8]--;
            return true;
         } else {
            return false;
         }
      } else if (i.getItem() instanceof BlockItem && ((BlockItem)i.getItem()).getBlock() instanceof PistonBlock) {
         if ((double)this.stealCountList[9] > this.piston.getValue()) {
            int var12 = this.stealCountList[9]--;
            return true;
         } else {
            return false;
         }
      } else if (i.getItem().equals(Blocks.REDSTONE_BLOCK.asItem())) {
         if ((double)this.stealCountList[10] > this.redstone.getValue()) {
            int var11 = this.stealCountList[10]--;
            return true;
         } else {
            return false;
         }
      } else if (i.getItem() instanceof BlockItem && ((BlockItem)i.getItem()).getBlock() instanceof BedBlock) {
         if ((double)this.stealCountList[11] > this.bed.getValue()) {
            int var10 = this.stealCountList[11]--;
            return true;
         } else {
            return false;
         }
      } else {
         if (Item.getRawId(i.getItem()) == Item.getRawId(Items.SPLASH_POTION)) {
            PotionContentsComponent potionContentsComponent = (PotionContentsComponent)i.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);

            for(StatusEffectInstance effect : potionContentsComponent.getEffects()) {
               if (effect.getEffectType().value() == StatusEffects.SPEED.value()) {
                  if ((double)this.stealCountList[12] > this.speed.getValue()) {
                     int var7 = this.stealCountList[12]--;
                     return true;
                  }

                  return false;
               }

               if (effect.getEffectType().value() == StatusEffects.RESISTANCE.value()) {
                  if ((double)this.stealCountList[13] > this.turtle.getValue()) {
                     int var6 = this.stealCountList[13]--;
                     return true;
                  }

                  return false;
               }

               if (effect.getEffectType().value() == StatusEffects.STRENGTH.value()) {
                  if ((double)this.stealCountList[16] > this.strength.getValue()) {
                     int var10002 = this.stealCountList[16]--;
                     return true;
                  }

                  return false;
               }
            }
         }

         if (i.getItem().equals(Items.GOLDEN_APPLE) && (double)this.stealCountList[14] > this.gapple.getValue()) {
            int var9 = this.stealCountList[14]--;
            return true;
         } else if (i.getItem() instanceof BlockItem && ((BlockItem)i.getItem()).getBlock() instanceof LadderBlock && (double)this.stealCountList[15] > this.ladder.getValue()) {
            int var8 = this.stealCountList[15]--;
            return true;
         } else {
            return dropOther;
         }
      }
   }

   private void updateItem() {
      this.stealCountList[0] = getItemCount(Items.END_CRYSTAL);
      this.stealCountList[1] = getItemCount(Items.EXPERIENCE_BOTTLE);
      this.stealCountList[2] = getItemCount(Items.TOTEM_OF_UNDYING);
      this.stealCountList[3] = getItemCount(Items.ENCHANTED_GOLDEN_APPLE);
      this.stealCountList[4] = getItemCount(Items.OBSIDIAN);
      this.stealCountList[5] = getItemCount(Items.COBWEB);
      this.stealCountList[6] = getItemCount(Items.GLOWSTONE);
      this.stealCountList[7] = getItemCount(Items.RESPAWN_ANCHOR);
      this.stealCountList[8] = getItemCount(Items.ENDER_PEARL);
      this.stealCountList[9] = getItemCount(Items.PISTON) - getItemCount(Items.STICKY_PISTON);
      this.stealCountList[10] = getItemCount(Items.REDSTONE_BLOCK);
      this.stealCountList[11] = getItemCount(BedBlock.class);
      this.stealCountList[12] = InventoryUtil.getPotionCount((StatusEffect)StatusEffects.SPEED.value());
      this.stealCountList[13] = InventoryUtil.getPotionCount((StatusEffect)StatusEffects.RESISTANCE.value());
      this.stealCountList[14] = getItemCount(Items.GOLDEN_APPLE);
      this.stealCountList[15] = getItemCount(LadderBlock.class);
      this.stealCountList[16] = InventoryUtil.getPotionCount((StatusEffect)StatusEffects.STRENGTH.value());
   }

   private int getMinId(int slot, int currentId) {
      int id = currentId;

      for(int slot1 = slot + 1; slot1 < 36; ++slot1) {
         ItemStack stack = mc.player.getInventory().getStack(slot1);
         int itemID;
         if (!stack.isEmpty() && (itemID = Item.getRawId(stack.getItem())) < id) {
            id = itemID;
         }
      }

      return id;
   }

   public static boolean canMerge(ItemStack source, ItemStack stack) {
      return ItemStack.areItemsAndComponentsEqual(source, stack);
   }

   private static enum Mode {
      Whitelist,
      Blacklist,
      Kit;

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{Whitelist, Blacklist, Kit};
      }
   }
}
