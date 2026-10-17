package dev.baimaodaxia.mod.modules.impl.combat;

import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.OpenScreenEvent;
import dev.baimaodaxia.api.events.impl.UpdateEvent;
import dev.baimaodaxia.api.utils.math.Timer;
import dev.baimaodaxia.api.utils.player.InventoryUtil;
import dev.baimaodaxia.api.utils.world.BlockUtil;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.impl.misc.ShulkerViewer;
import dev.baimaodaxia.mod.modules.impl.player.PacketMine;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.util.Formatting;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;

public class ChestStealer extends Module {
   public static ChestStealer INSTANCE;
   public final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
   public final Timer timeoutTimer = new Timer();
   final int[] stealCountList = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
   private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true));
   private final SliderSetting disableTime = this.add(new SliderSetting("DisableTime", 500, 0, 1000));
   private final BooleanSetting place = this.add(new BooleanSetting("Place", true));
   private final BooleanSetting packet = this.add(new BooleanSetting("PacketPlace", false));
   private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
   private final BooleanSetting preferOpen = this.add(new BooleanSetting("PerferOpen", true));
   private final BooleanSetting open = this.add(new BooleanSetting("Open", true));
   private final SliderSetting range = this.add(new SliderSetting("MaxRange", (double)4.0F, (double)0.0F, (double)6.0F, 0.1));
   private final SliderSetting minRange = this.add(new SliderSetting("MinRange", (double)1.0F, (double)0.0F, (double)3.0F, 0.1));
   private final BooleanSetting mine = this.add(new BooleanSetting("Mine", true));
   private final EnumSetting<Mode> mode;
   private final BooleanSetting silent;
   private final BooleanSetting preview;
   private final SliderSetting previewScale;
   private final SliderSetting previewHeight;
   private final BooleanSetting previewInventory;
   private final Map<BlockPos, List<ItemStack>> shulkerContents;
   private final BooleanSetting take;
   private final BooleanSetting smart;
   private final BooleanSetting forceMove;
   private final SliderSetting crystal;
   private final SliderSetting exp;
   private final SliderSetting totem;
   private final SliderSetting gapple;
   private final SliderSetting obsidian;
   private final SliderSetting web;
   private final SliderSetting glowstone;
   private final SliderSetting anchor;
   private final SliderSetting pearl;
   private final SliderSetting piston;
   private final SliderSetting redstone;
   private final SliderSetting bed;
   private final SliderSetting speed;
   private final SliderSetting resistance;
   private final SliderSetting strength;
   private final BooleanSetting closeAfterRegear;
   private final SliderSetting regearSpeed;
   private final SliderSetting regearClicks;
   private final Timer timer;
   private final Timer regearTimer;
   private final List<BlockPos> openList;
   private final Map<Integer, String> regearExpectedInv;
   private boolean regearSetupDone;
   private static final File KITS_FILE;
   public BlockPos placePos;
   private BlockPos openPos;
   private boolean opend;
   private BlockPos lastOpenedPos;
   private boolean stealing;
   private List<ItemStack> stealingItems;

   public ChestStealer() {
      super("ChestStealer", Module.Category.Combat);
      this.mode = this.add(new EnumSetting("Mode", ChestStealer.Mode.SMART));
      this.silent = this.add(new BooleanSetting("Silent", false, () -> this.mode.getValue() == ChestStealer.Mode.REGEAR));
      this.preview = this.add(new BooleanSetting("Preview", true));
      BooleanSetting var10009 = this.preview;
      Objects.requireNonNull(var10009);
      this.previewScale = this.add(new SliderSetting("PreviewScale", (double)0.5F, 0.1, (double)1.0F, 0.05, var10009::getValue));
      var10009 = this.preview;
      Objects.requireNonNull(var10009);
      this.previewHeight = this.add(new SliderSetting("PreviewHeight", (double)1.0F, (double)0.5F, (double)2.0F, 0.1, var10009::getValue));
      BooleanSetting var10006 = this.preview;
      Objects.requireNonNull(var10006);
      this.previewInventory = this.add(new BooleanSetting("PreviewInventory", true, var10006::getValue));
      this.shulkerContents = new HashMap();
      this.take = this.add(new BooleanSetting("Take", true, () -> this.mode.getValue() == ChestStealer.Mode.SMART));
      this.smart = this.add((new BooleanSetting("Smart", true, () -> this.mode.getValue() == ChestStealer.Mode.SMART && this.take.getValue())).setParent());
      this.forceMove = this.add(new BooleanSetting("ForceQuickMove", true, () -> this.mode.getValue() == ChestStealer.Mode.SMART && this.take.getValue() && this.smart.isOpen()));
      this.crystal = this.add(new SliderSetting("Crystal", 256, 0, 512, () -> this.mode.getValue() == ChestStealer.Mode.SMART && this.take.getValue() && this.smart.isOpen()));
      this.exp = this.add(new SliderSetting("Exp", 256, 0, 512, () -> this.mode.getValue() == ChestStealer.Mode.SMART && this.take.getValue() && this.smart.isOpen()));
      this.totem = this.add(new SliderSetting("Totem", 6, 0, 36, () -> this.mode.getValue() == ChestStealer.Mode.SMART && this.take.getValue() && this.smart.isOpen()));
      this.gapple = this.add(new SliderSetting("Gapple", 128, 0, 512, () -> this.mode.getValue() == ChestStealer.Mode.SMART && this.take.getValue() && this.smart.isOpen()));
      this.obsidian = this.add(new SliderSetting("Obsidian", 64, 0, 512, () -> this.mode.getValue() == ChestStealer.Mode.SMART && this.take.getValue() && this.smart.isOpen()));
      this.web = this.add(new SliderSetting("Web", 64, 0, 512, () -> this.mode.getValue() == ChestStealer.Mode.SMART && this.take.getValue() && this.smart.isOpen()));
      this.glowstone = this.add(new SliderSetting("Glowstone", 128, 0, 512, () -> this.mode.getValue() == ChestStealer.Mode.SMART && this.take.getValue() && this.smart.isOpen()));
      this.anchor = this.add(new SliderSetting("Anchor", 128, 0, 512, () -> this.mode.getValue() == ChestStealer.Mode.SMART && this.take.getValue() && this.smart.isOpen()));
      this.pearl = this.add(new SliderSetting("Pearl", 16, 0, 64, () -> this.mode.getValue() == ChestStealer.Mode.SMART && this.take.getValue() && this.smart.isOpen()));
      this.piston = this.add(new SliderSetting("Piston", 64, 0, 512, () -> this.mode.getValue() == ChestStealer.Mode.SMART && this.take.getValue() && this.smart.isOpen()));
      this.redstone = this.add(new SliderSetting("RedStone", 64, 0, 512, () -> this.mode.getValue() == ChestStealer.Mode.SMART && this.take.getValue() && this.smart.isOpen()));
      this.bed = this.add(new SliderSetting("Bed", 256, 0, 512, () -> this.mode.getValue() == ChestStealer.Mode.SMART && this.take.getValue() && this.smart.isOpen()));
      this.speed = this.add(new SliderSetting("Speed", 1, 0, 8, () -> this.mode.getValue() == ChestStealer.Mode.SMART && this.take.getValue() && this.smart.isOpen()));
      this.resistance = this.add(new SliderSetting("Resistance", 1, 0, 8, () -> this.mode.getValue() == ChestStealer.Mode.SMART && this.take.getValue() && this.smart.isOpen()));
      this.strength = this.add(new SliderSetting("Strength", 1, 0, 8, () -> this.mode.getValue() == ChestStealer.Mode.SMART && this.take.getValue() && this.smart.isOpen()));
      this.closeAfterRegear = this.add(new BooleanSetting("CloseAfter", false, () -> this.mode.getValue() == ChestStealer.Mode.REGEAR));
      this.regearSpeed = this.add(new SliderSetting("RegearSpeed", (double)1.0F, (double)1.0F, (double)1000.0F, (double)1.0F, () -> this.mode.getValue() == ChestStealer.Mode.REGEAR));
      this.regearClicks = this.add(new SliderSetting("ClicksPerTick", (double)1.0F, (double)1.0F, (double)36.0F, (double)1.0F, () -> this.mode.getValue() == ChestStealer.Mode.REGEAR));
      this.timer = new Timer();
      this.regearTimer = new Timer();
      this.openList = new ArrayList();
      this.regearExpectedInv = new HashMap();
      this.regearSetupDone = false;
      this.placePos = null;
      this.opend = false;
      this.lastOpenedPos = null;
      this.stealing = false;
      this.stealingItems = new ArrayList();
      this.setChinese("NCP自动补给");
      INSTANCE = this;
   }

   public void loadCurrentKit() {
      try {
         Map<String, Object> kits = this.loadKits();
         String currentKitName = (String)kits.getOrDefault("pointer", "none");
         if (currentKitName.equals("none")) {
            this.sendMessage(String.valueOf(Formatting.RED) + "No kit selected! Use .regear load <name>");
            return;
         }

         if (!kits.containsKey(currentKitName)) {
            String var6 = String.valueOf(Formatting.RED);
            this.sendMessage(var6 + "Kit '" + currentKitName + "' not found!");
            return;
         }

         String kitItems = (String)kits.get(currentKitName);
         this.setupRegear(kitItems);
         String var5 = String.valueOf(Formatting.GREEN);
         this.sendMessage(var5 + "Loaded kit: " + currentKitName);
      } catch (Exception e) {
         String var10001 = String.valueOf(Formatting.RED);
         this.sendMessage(var10001 + "Error loading kit: " + e.getMessage());
         e.printStackTrace();
      }

   }

   public String getCurrentKitName() {
      try {
         Map<String, Object> kits = this.loadKits();
         return (String)kits.getOrDefault("pointer", "none");
      } catch (Exception var2) {
         return "none";
      }
   }

   private Map<String, Object> loadKits() throws IOException {
      Map<String, Object> kits = new HashMap();
      if (!KITS_FILE.exists()) {
         KITS_FILE.getParentFile().mkdirs();
         KITS_FILE.createNewFile();
         kits.put("pointer", "none");
         this.saveKits(kits);
         return kits;
      } else {
         BufferedReader reader = new BufferedReader(new FileReader(KITS_FILE));

         String line;
         try {
            while((line = reader.readLine()) != null) {
               line = line.trim();
               if (!line.isEmpty() && !line.startsWith("#") && line.contains(":")) {
                  String[] parts = line.split(":", 2);
                  if (parts.length == 2) {
                     kits.put(parts[0].trim(), parts[1].trim());
                  }
               }
            }
         } catch (Throwable var6) {
            try {
               reader.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }

            throw var6;
         }

         reader.close();
         if (!kits.containsKey("pointer")) {
            kits.put("pointer", "none");
         }

         return kits;
      }
   }

   private void saveKits(Map<String, Object> kits) throws IOException {
      BufferedWriter writer = new BufferedWriter(new FileWriter(KITS_FILE));

      try {
         writer.write("# Luminous Regear Kits");
         writer.newLine();
         writer.newLine();

         for(Map.Entry<String, Object> entry : kits.entrySet()) {
            String var10001 = (String)entry.getKey();
            writer.write(var10001 + ": " + String.valueOf(entry.getValue()));
            writer.newLine();
         }
      } catch (Throwable var6) {
         try {
            writer.close();
         } catch (Throwable var5) {
            var6.addSuppressed(var5);
         }

         throw var6;
      }

      writer.close();
   }

   public void setupRegear(String kitItems) {
      if (!kitItems.isEmpty() && kitItems.split(" ").length == 36) {
         String[] items = kitItems.split(" ");
         this.regearExpectedInv.clear();

         for(int i = 0; i < 36; ++i) {
            if (!items[i].equals("minecraft:air")) {
               this.regearExpectedInv.put(i, items[i]);
            }
         }

         this.regearSetupDone = true;
      } else {
         this.sendMessage(String.valueOf(Formatting.RED) + "Invalid kit! create it again");
      }
   }

   private boolean isSlotCorrect(int slot, String expectedItemKey, ItemStack currentStack) {
      if (currentStack.isEmpty()) {
         return expectedItemKey.equals("minecraft:air");
      } else {
         String currentItemKey = currentStack.getItem().getTranslationKey();
         if (currentStack.getItem() instanceof PotionItem && expectedItemKey.contains("potion")) {
            boolean isSplashPotion = currentStack.getItem() instanceof SplashPotionItem;
            boolean expectedIsSplash = expectedItemKey.contains("splash");
            return isSplashPotion == expectedIsSplash;
         } else {
            return currentItemKey.equals(expectedItemKey);
         }
      }
   }

   private boolean needToTakeItem(ItemStack currentStack, String expectedItem) {
      if (currentStack.isEmpty()) {
         return true;
      } else {
         return !this.isSlotCorrect(0, expectedItem, currentStack);
      }
   }

   private int findItemInShulker(String itemName, ShulkerBoxScreenHandler handler, int excludeSlot) {
      int maxSearch = handler.slots.size() == 63 ? 27 : 54;
      int bestSlot = -1;
      int bestCount = 0;

      for(int i = 0; i < maxSearch; ++i) {
         if (i != excludeSlot) {
            ItemStack stack = ((Slot)handler.slots.get(i)).getStack();
            if (!stack.isEmpty()) {
               String stackKey = stack.getItem().getTranslationKey();
               if (stack.getItem() instanceof PotionItem && itemName.contains("potion")) {
                  boolean isSplashPotion = stack.getItem() instanceof SplashPotionItem;
                  boolean expectedIsSplash = itemName.contains("splash");
                  if (isSplashPotion == expectedIsSplash && stack.getCount() > bestCount) {
                     bestCount = stack.getCount();
                     bestSlot = i;
                  }
               } else if (stackKey.equals(itemName) && stack.getCount() > bestCount) {
                  bestCount = stack.getCount();
                  bestSlot = i;
                  if (bestCount >= 64) {
                     break;
                  }
               }
            }
         }
      }

      return bestSlot;
   }

   private int findEmptySlotInShulker(ShulkerBoxScreenHandler handler) {
      int maxSearch = handler.slots.size() == 63 ? 27 : 54;

      for(int i = 0; i < maxSearch; ++i) {
         ItemStack stack = ((Slot)handler.slots.get(i)).getStack();
         if (stack.isEmpty()) {
            return i;
         }
      }

      return -1;
   }

   private int findMergeableStack(String itemName, ShulkerBoxScreenHandler handler, int targetSlot) {
      int maxSearch = handler.slots.size() == 63 ? 27 : 54;
      ItemStack targetStack = ((Slot)handler.slots.get(targetSlot)).getStack();
      if (!targetStack.isEmpty() && targetStack.getCount() < targetStack.getMaxCount()) {
         for(int i = 0; i < maxSearch; ++i) {
            if (i != targetSlot) {
               ItemStack stack = ((Slot)handler.slots.get(i)).getStack();
               if (!stack.isEmpty() && stack.getItem() == targetStack.getItem()) {
                  if (stack.getItem() instanceof PotionItem) {
                     boolean stackIsSplash = stack.getItem() instanceof SplashPotionItem;
                     boolean itemInSlotIsSplash = targetStack.getItem() instanceof SplashPotionItem;
                     if (stackIsSplash == itemInSlotIsSplash) {
                        return i;
                     }
                  } else if (stack.getItem().getTranslationKey().equals(targetStack.getItem().getTranslationKey())) {
                     return i;
                  }
               }
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   private int getTargetSlot(int playerSlot, ShulkerBoxScreenHandler handler) {
      boolean is63SlotContainer = handler.slots.size() == 63;
      return playerSlot < 9 ? playerSlot + (is63SlotContainer ? 54 : 81) : playerSlot + (is63SlotContainer ? 18 : 45);
   }

   private void performPickupAction(ShulkerBoxScreenHandler handler, int sourceSlot, int targetSlot) {
      mc.interactionManager.clickSlot(handler.syncId, sourceSlot, 0, SlotActionType.PICKUP, mc.player);
      mc.interactionManager.clickSlot(handler.syncId, targetSlot, 0, SlotActionType.PICKUP, mc.player);
      ItemStack cursorStack = handler.getCursorStack();
      if (!cursorStack.isEmpty()) {
         mc.interactionManager.clickSlot(handler.syncId, sourceSlot, 0, SlotActionType.PICKUP, mc.player);
      }

   }

   private void performMergeAction(ShulkerBoxScreenHandler handler, int sourceSlot, int targetSlot) {
      ItemStack sourceStack = ((Slot)handler.slots.get(sourceSlot)).getStack();
      ItemStack targetStack = ((Slot)handler.slots.get(targetSlot)).getStack();
      if (!sourceStack.isEmpty() && !targetStack.isEmpty()) {
         int spaceLeft = targetStack.getMaxCount() - targetStack.getCount();
         int available = Math.min(spaceLeft, sourceStack.getCount());
         if (available > 0) {
            if (sourceStack.getCount() == available) {
               mc.interactionManager.clickSlot(handler.syncId, sourceSlot, 0, SlotActionType.QUICK_MOVE, mc.player);
            } else {
               mc.interactionManager.clickSlot(handler.syncId, sourceSlot, 0, SlotActionType.PICKUP, mc.player);
               mc.interactionManager.clickSlot(handler.syncId, targetSlot, 0, SlotActionType.PICKUP, mc.player);
               ItemStack cursorStack = handler.getCursorStack();
               if (!cursorStack.isEmpty()) {
                  mc.interactionManager.clickSlot(handler.syncId, sourceSlot, 0, SlotActionType.PICKUP, mc.player);
               }
            }
         }

      }
   }

   private boolean checkAllItemsCorrect(ShulkerBoxScreenHandler handler) {
      for(Map.Entry<Integer, String> entry : this.regearExpectedInv.entrySet()) {
         int targetSlot = this.getTargetSlot((Integer)entry.getKey(), handler);
         if (targetSlot >= 0 && targetSlot < handler.slots.size()) {
            ItemStack itemInSlot = ((Slot)handler.slots.get(targetSlot)).getStack();
            String expected = (String)entry.getValue();
            if (expected.contains("shulker_box")) {
               if (!itemInSlot.isEmpty()) {
                  return false;
               }
            } else {
               if (itemInSlot.isEmpty()) {
                  return false;
               }

               if (!this.isSlotCorrect((Integer)entry.getKey(), expected, itemInSlot)) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   private void doRegear(ShulkerBoxScreenHandler handler) {
      if (this.regearTimer.passed((long)((double)1000.0F / this.regearSpeed.getValue()))) {
         boolean allItemsCorrect = true;
         int actionsTaken = 0;
         List<Map.Entry<Integer, String>> itemsToProcess = new ArrayList(this.regearExpectedInv.entrySet());
         itemsToProcess.sort((a, b) -> {
            int slotA = (Integer)a.getKey();
            int slotB = (Integer)b.getKey();
            String expectedA = (String)a.getValue();
            String expectedB = (String)b.getValue();
            if (expectedA.contains("shulker_box") && !expectedB.contains("shulker_box")) {
               return 1;
            } else if (!expectedA.contains("shulker_box") && expectedB.contains("shulker_box")) {
               return -1;
            } else {
               ItemStack stackA = ((Slot)handler.slots.get(this.getTargetSlot(slotA, handler))).getStack();
               ItemStack stackB = ((Slot)handler.slots.get(this.getTargetSlot(slotB, handler))).getStack();
               boolean isEmptyA = stackA.isEmpty();
               boolean isEmptyB = stackB.isEmpty();
               boolean isCorrectA = this.isSlotCorrect(slotA, expectedA, stackA);
               boolean isCorrectB = this.isSlotCorrect(slotB, expectedB, stackB);
               if (isEmptyA && !isEmptyB) {
                  return -1;
               } else if (!isEmptyA && isEmptyB) {
                  return 1;
               } else if (!isCorrectA && isCorrectB) {
                  return -1;
               } else {
                  return isCorrectA && !isCorrectB ? 1 : 0;
               }
            }
         });

         for(Map.Entry<Integer, String> entry : itemsToProcess) {
            if (actionsTaken >= this.regearClicks.getValueInt()) {
               break;
            }

            int slotIndex = (Integer)entry.getKey();
            String expectedItem = (String)entry.getValue();
            int targetSlot = this.getTargetSlot(slotIndex, handler);
            if (targetSlot >= 0 && targetSlot < handler.slots.size()) {
               ItemStack targetStack = ((Slot)handler.slots.get(targetSlot)).getStack();
               if (expectedItem.contains("shulker_box")) {
                  if (!targetStack.isEmpty()) {
                     int emptyShulkerSlot = this.findEmptySlotInShulker(handler);
                     if (emptyShulkerSlot != -1) {
                        mc.interactionManager.clickSlot(handler.syncId, targetSlot, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(handler.syncId, emptyShulkerSlot, 0, SlotActionType.PICKUP, mc.player);
                        if (!handler.getCursorStack().isEmpty()) {
                           mc.interactionManager.clickSlot(handler.syncId, targetSlot, 0, SlotActionType.PICKUP, mc.player);
                        }

                        ++actionsTaken;
                        allItemsCorrect = false;
                     }
                  }
               } else if (this.needToTakeItem(targetStack, expectedItem)) {
                  int sourceSlot = this.findItemInShulker(expectedItem, handler, targetSlot);
                  if (sourceSlot != -1) {
                     this.performPickupAction(handler, sourceSlot, targetSlot);
                     ++actionsTaken;
                     allItemsCorrect = false;
                  } else if (!targetStack.isEmpty() && !this.isSlotCorrect(slotIndex, expectedItem, targetStack)) {
                     int emptySlot = this.findEmptySlotInShulker(handler);
                     if (emptySlot != -1) {
                        mc.interactionManager.clickSlot(handler.syncId, targetSlot, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(handler.syncId, emptySlot, 0, SlotActionType.PICKUP, mc.player);
                        ++actionsTaken;
                        allItemsCorrect = false;
                     }
                  }
               } else if (targetStack.getCount() < targetStack.getMaxCount()) {
                  int mergeSlot = this.findMergeableStack(expectedItem, handler, targetSlot);
                  if (mergeSlot != -1) {
                     this.performMergeAction(handler, mergeSlot, targetSlot);
                     ++actionsTaken;
                  }
               }
            }
         }

         this.regearTimer.reset();
         if ((allItemsCorrect || this.checkAllItemsCorrect(handler)) && this.closeAfterRegear.getValue()) {
            mc.currentScreen.close();
         }

      }
   }

   public int findShulker() {
      if (this.inventory.getValue()) {
         for(int i = 0; i < 36; ++i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
               Item var4 = stack.getItem();
               if (var4 instanceof BlockItem) {
                  BlockItem blockItem = (BlockItem)var4;
                  if (blockItem.getBlock() instanceof ShulkerBoxBlock) {
                     return i < 9 ? i + 36 : i;
                  }
               }
            }
         }

         return -1;
      } else {
         return InventoryUtil.findClass(ShulkerBoxBlock.class);
      }
   }

   public void onEnable() {
      this.opend = false;
      this.openPos = null;
      this.lastOpenedPos = null;
      this.timeoutTimer.reset();
      this.placePos = null;
      this.regearSetupDone = false;
      this.shulkerContents.clear();
      if (!nullCheck() && this.place.getValue()) {
         this.doPlace();
      }

   }

   private void doPlace() {
      int oldSlot = mc.player.getInventory().selectedSlot;
      BlockPos bestPos = this.findBestPlacePosition();
      if (bestPos != null) {
         if (this.findShulker() == -1) {
            this.sendMessage("§4No shulkerbox found.");
            return;
         }

         int slot = this.findShulker();
         if (this.inventory.getValue()) {
            InventoryUtil.inventorySwap(slot, oldSlot);
            this.placeBlock(bestPos);
            this.placePos = bestPos;
            InventoryUtil.inventorySwap(slot, oldSlot);
         } else {
            InventoryUtil.switchToSlot(slot);
            this.placeBlock(bestPos);
            this.placePos = bestPos;
            InventoryUtil.switchToSlot(oldSlot);
         }

         this.timer.reset();
      } else {
         this.sendMessage("§4No place position found.");
      }

   }

   private BlockPos findBestPlacePosition() {
      BlockPos playerPos = BlockPos.ofFloored(mc.player.getPos());

      for(BlockPos pos : BlockUtil.getSphere((float)this.range.getValue())) {
         if (this.isValidPit(pos)) {
            return pos;
         }
      }

      for(int y = 0; y >= -2; --y) {
         for(int x = -1; x <= 1; ++x) {
            for(int z = -1; z <= 1; ++z) {
               if (x != 0 || z != 0) {
                  BlockPos feetPos = playerPos.add(x, y, z);
                  if (this.isValidPosition(feetPos)) {
                     return feetPos;
                  }
               }
            }
         }
      }

      for(BlockPos pos : BlockUtil.getSphere((float)this.range.getValue())) {
         if (this.isValidPosition(pos) && this.isAdjacentToPlayer(pos, playerPos)) {
            return pos;
         }
      }

      for(BlockPos pos : BlockUtil.getSphere((float)this.range.getValue())) {
         if (this.isValidPosition(pos)) {
            return pos;
         }
      }

      return null;
   }

   private boolean isValidPit(BlockPos pos) {
      if (!mc.world.isAir(pos.up())) {
         return false;
      } else if (!BlockUtil.clientCanPlace(pos, false)) {
         return false;
      } else if ((double)MathHelper.sqrt((float)mc.player.squaredDistanceTo(pos.toCenterPos())) < this.minRange.getValue()) {
         return false;
      } else if (!BlockUtil.isStrictDirection(pos.offset(Direction.DOWN), Direction.UP)) {
         return false;
      } else if (!BlockUtil.canClick(pos.offset(Direction.DOWN))) {
         return false;
      } else if (this.preferOpen.getValue() && mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock) {
         return false;
      } else {
         BlockState north = mc.world.getBlockState(pos.north());
         BlockState south = mc.world.getBlockState(pos.south());
         BlockState east = mc.world.getBlockState(pos.east());
         BlockState west = mc.world.getBlockState(pos.west());
         return this.isObsidianOrBedrock(north) && this.isObsidianOrBedrock(south) && this.isObsidianOrBedrock(east) && this.isObsidianOrBedrock(west);
      }
   }

   private boolean isObsidianOrBedrock(BlockState state) {
      return state.getBlock() == Blocks.OBSIDIAN || state.getBlock() == Blocks.BEDROCK;
   }

   private boolean isValidPosition(BlockPos pos) {
      if (!mc.world.isAir(pos.up())) {
         return false;
      } else if (!BlockUtil.clientCanPlace(pos, false)) {
         return false;
      } else if ((double)MathHelper.sqrt((float)mc.player.squaredDistanceTo(pos.toCenterPos())) < this.minRange.getValue()) {
         return false;
      } else if (!BlockUtil.isStrictDirection(pos.offset(Direction.DOWN), Direction.UP)) {
         return false;
      } else if (!BlockUtil.canClick(pos.offset(Direction.DOWN))) {
         return false;
      } else {
         return !this.preferOpen.getValue() || !(mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock);
      }
   }

   private boolean isAdjacentToPlayer(BlockPos pos, BlockPos playerPos) {
      int dx = Math.abs(pos.getX() - playerPos.getX());
      int dy = Math.abs(pos.getY() - playerPos.getY());
      int dz = Math.abs(pos.getZ() - playerPos.getZ());
      return dx <= 1 && dy <= 1 && dz <= 1 && dx + dy + dz > 0;
   }

   private void update() {
      this.stealCountList[0] = (int)(this.crystal.getValue() - (double)InventoryUtil.getItemCount(Items.END_CRYSTAL));
      this.stealCountList[1] = (int)(this.exp.getValue() - (double)InventoryUtil.getItemCount(Items.EXPERIENCE_BOTTLE));
      this.stealCountList[2] = (int)(this.totem.getValue() - (double)InventoryUtil.getItemCount(Items.TOTEM_OF_UNDYING));
      this.stealCountList[3] = (int)(this.gapple.getValue() - (double)InventoryUtil.getItemCount(Items.ENCHANTED_GOLDEN_APPLE));
      this.stealCountList[4] = (int)(this.obsidian.getValue() - (double)InventoryUtil.getItemCount(Blocks.OBSIDIAN.asItem()));
      this.stealCountList[5] = (int)(this.web.getValue() - (double)InventoryUtil.getItemCount(Blocks.COBWEB.asItem()));
      this.stealCountList[6] = (int)(this.glowstone.getValue() - (double)InventoryUtil.getItemCount(Blocks.GLOWSTONE.asItem()));
      this.stealCountList[7] = (int)(this.anchor.getValue() - (double)InventoryUtil.getItemCount(Blocks.RESPAWN_ANCHOR.asItem()));
      this.stealCountList[8] = (int)(this.pearl.getValue() - (double)InventoryUtil.getItemCount(Items.ENDER_PEARL));
      this.stealCountList[9] = (int)(this.piston.getValue() - (double)InventoryUtil.getItemCount(Blocks.PISTON.asItem()) - (double)InventoryUtil.getItemCount(Blocks.STICKY_PISTON.asItem()));
      this.stealCountList[10] = (int)(this.redstone.getValue() - (double)InventoryUtil.getItemCount(Blocks.REDSTONE_BLOCK.asItem()));
      this.stealCountList[11] = (int)(this.bed.getValue() - (double)InventoryUtil.getItemCount(BedBlock.class));
      this.stealCountList[12] = (int)(this.speed.getValue() - (double)InventoryUtil.getPotionCount((StatusEffect)StatusEffects.SPEED.value()));
      this.stealCountList[13] = (int)(this.resistance.getValue() - (double)InventoryUtil.getPotionCount((StatusEffect)StatusEffects.RESISTANCE.value()));
      this.stealCountList[14] = (int)(this.strength.getValue() - (double)InventoryUtil.getPotionCount((StatusEffect)StatusEffects.STRENGTH.value()));
   }

   public void onDisable() {
      this.opend = false;
      this.regearSetupDone = false;
      if (this.mine.getValue() && this.placePos != null) {
         PacketMine.INSTANCE.mine(this.placePos);
      }

   }

   @EventListener
   public void onOpenScreen(OpenScreenEvent event) {
      if (this.shouldSilent() || this.shouldPreview()) {
         Screen var3 = event.screen;
         if (var3 instanceof ShulkerBoxScreen) {
            ShulkerBoxScreen shulkerScreen = (ShulkerBoxScreen)var3;
            if (this.shouldPreview() && this.openPos != null) {
               ShulkerBoxScreenHandler handler = (ShulkerBoxScreenHandler)shulkerScreen.getScreenHandler();
               List<ItemStack> items = new ArrayList();

               for(int i = 0; i < 27; ++i) {
                  items.add(handler.getSlot(i).getStack().copy());
               }

               this.shulkerContents.put(this.openPos, items);
            }

            event.cancel();
         }

      }
   }

   private boolean shouldPreview() {
      return this.isOn() && this.preview.getValue() && this.openPos != null;
   }

   private boolean isShulkerScreenOpen() {
      if (mc.player == null) {
         return false;
      } else {
         return !this.shouldSilent() && !this.shouldPreview() ? mc.currentScreen instanceof ShulkerBoxScreen : mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler;
      }
   }

   private boolean shouldSilent() {
      if (this.isOn() && this.silent.getValue() && this.mode.getValue() == ChestStealer.Mode.REGEAR) {
         return this.opend || this.placePos != null || this.openPos != null;
      } else {
         return false;
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.mode.getValue() == ChestStealer.Mode.SMART && this.smart.getValue()) {
         this.update();
      }

      this.openList.removeIf((posx) -> !(mc.world.getBlockState(posx).getBlock() instanceof ShulkerBoxBlock));
      if (!this.isShulkerScreenOpen()) {
         if (this.opend) {
            this.opend = false;
            if (this.autoDisable.getValue()) {
               this.timeoutToDisable();
            }

            if (this.mine.getValue() && this.openPos != null) {
               if (mc.world.getBlockState(this.openPos).getBlock() instanceof ShulkerBoxBlock) {
                  PacketMine.INSTANCE.mine(this.openPos);
               } else {
                  this.openPos = null;
               }
            }
         } else if (this.open.getValue()) {
            if (this.placePos == null || !((double)MathHelper.sqrt((float)mc.player.squaredDistanceTo(this.placePos.toCenterPos())) <= this.range.getValue()) || !mc.world.isAir(this.placePos.up()) || this.timer.passed(500L) && !(mc.world.getBlockState(this.placePos).getBlock() instanceof ShulkerBoxBlock)) {
               boolean found = false;

               for(BlockPos pos : BlockUtil.getSphere((float)this.range.getValue())) {
                  if (!this.openList.contains(pos) && !pos.equals(this.lastOpenedPos) && (mc.world.isAir(pos.up()) || BlockUtil.canReplace(pos.up())) && mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock) {
                     this.openPos = pos;
                     this.lastOpenedPos = pos;
                     BlockUtil.clickBlock(pos, BlockUtil.getClickSide(pos), this.rotate.getValue(), this.packet.getValue());
                     found = true;
                     break;
                  }
               }

               if (!found && this.autoDisable.getValue()) {
                  this.timeoutToDisable();
               }
            } else if (mc.world.getBlockState(this.placePos).getBlock() instanceof ShulkerBoxBlock && !this.placePos.equals(this.lastOpenedPos)) {
               this.openPos = this.placePos;
               this.lastOpenedPos = this.placePos;
               BlockUtil.clickBlock(this.placePos, BlockUtil.getClickSide(this.placePos), this.rotate.getValue(), this.packet.getValue());
            }
         } else if (this.mode.getValue() == ChestStealer.Mode.SMART && !this.take.getValue() && this.autoDisable.getValue()) {
            this.timeoutToDisable();
         }
      } else {
         this.opend = true;
         if (this.openPos != null) {
            this.openList.add(this.openPos);
         }

         if (this.mode.getValue() == ChestStealer.Mode.REGEAR) {
            ScreenHandler var12 = mc.player.currentScreenHandler;
            if (var12 instanceof ShulkerBoxScreenHandler) {
               ShulkerBoxScreenHandler shulker = (ShulkerBoxScreenHandler)var12;
               if (!this.regearSetupDone) {
                  this.loadCurrentKit();
                  this.regearSetupDone = true;
               }

               if (this.regearSetupDone && !this.regearExpectedInv.isEmpty()) {
                  this.doRegear(shulker);
               } else {
                  String currentKit = this.getCurrentKitName();
                  if (!currentKit.equals("none")) {
                     String var10001 = String.valueOf(Formatting.RED);
                     this.sendMessage(var10001 + "Failed to load kit: " + currentKit);
                  } else {
                     this.sendMessage(String.valueOf(Formatting.RED) + "No kit selected! Use .regear load <name>");
                  }
               }

               if (this.autoDisable.getValue() && this.closeAfterRegear.getValue()) {
                  this.timeoutToDisable();
               }
            }
         } else if (this.mode.getValue() == ChestStealer.Mode.SMART) {
            if (!this.take.getValue()) {
               if (this.autoDisable.getValue()) {
                  this.timeoutToDisable();
               }
            } else {
               boolean take = false;
               ScreenHandler var15 = mc.player.currentScreenHandler;
               if (var15 instanceof ShulkerBoxScreenHandler) {
                  ShulkerBoxScreenHandler shulker = (ShulkerBoxScreenHandler)var15;

                  for(Slot slot : shulker.slots) {
                     if (slot.id < 27 && !slot.getStack().isEmpty()) {
                        Type type = this.needSteal(slot.getStack());
                        if (this.smart.getValue() && type != ChestStealer.Type.QuickMove && (type != ChestStealer.Type.Stack || !this.forceMove.getValue())) {
                           if (type == ChestStealer.Type.Stack) {
                              for(int slot1 = 0; slot1 < 36; ++slot1) {
                                 ItemStack stack = mc.player.getInventory().getStack(slot1);
                                 if (!stack.isEmpty() && stack.isStackable() && stack.getItem() == slot.getStack().getItem() && stack.getCount() < stack.getMaxCount()) {
                                    int i = (slot1 < 9 ? slot1 + 36 : slot1) + 18;
                                    mc.interactionManager.clickSlot(shulker.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                                    mc.interactionManager.clickSlot(shulker.syncId, i, 0, SlotActionType.PICKUP, mc.player);
                                    mc.interactionManager.clickSlot(shulker.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                                    take = true;
                                    break;
                                 }
                              }
                           }
                        } else {
                           mc.interactionManager.clickSlot(shulker.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                           take = true;
                        }
                     }
                  }
               }

               if (this.autoDisable.getValue() && !take) {
                  this.timeoutToDisable();
               }
            }
         }
      }

   }

   private void timeoutToDisable() {
      if (this.timeoutTimer.passed((long)this.disableTime.getValueInt())) {
         this.disable();
      }

   }

   private Type needSteal(ItemStack i) {
      if (i.getItem().equals(Items.END_CRYSTAL) && this.stealCountList[0] > 0) {
         this.stealCountList[0] -= i.getCount();
         return this.stealCountList[0] < 0 ? ChestStealer.Type.Stack : ChestStealer.Type.QuickMove;
      } else if (i.getItem().equals(Items.EXPERIENCE_BOTTLE) && this.stealCountList[1] > 0) {
         this.stealCountList[1] -= i.getCount();
         return this.stealCountList[1] < 0 ? ChestStealer.Type.Stack : ChestStealer.Type.QuickMove;
      } else if (i.getItem().equals(Items.TOTEM_OF_UNDYING) && this.stealCountList[2] > 0) {
         this.stealCountList[2] -= i.getCount();
         return this.stealCountList[2] < 0 ? ChestStealer.Type.Stack : ChestStealer.Type.QuickMove;
      } else if (i.getItem().equals(Items.ENCHANTED_GOLDEN_APPLE) && this.stealCountList[3] > 0) {
         this.stealCountList[3] -= i.getCount();
         return this.stealCountList[3] < 0 ? ChestStealer.Type.Stack : ChestStealer.Type.QuickMove;
      } else if (i.getItem().equals(Blocks.OBSIDIAN.asItem()) && this.stealCountList[4] > 0) {
         this.stealCountList[4] -= i.getCount();
         return this.stealCountList[4] < 0 ? ChestStealer.Type.Stack : ChestStealer.Type.QuickMove;
      } else if (i.getItem().equals(Blocks.COBWEB.asItem()) && this.stealCountList[5] > 0) {
         this.stealCountList[5] -= i.getCount();
         return this.stealCountList[5] < 0 ? ChestStealer.Type.Stack : ChestStealer.Type.QuickMove;
      } else if (i.getItem().equals(Blocks.GLOWSTONE.asItem()) && this.stealCountList[6] > 0) {
         this.stealCountList[6] -= i.getCount();
         return this.stealCountList[6] < 0 ? ChestStealer.Type.Stack : ChestStealer.Type.QuickMove;
      } else if (i.getItem().equals(Blocks.RESPAWN_ANCHOR.asItem()) && this.stealCountList[7] > 0) {
         this.stealCountList[7] -= i.getCount();
         return this.stealCountList[7] < 0 ? ChestStealer.Type.Stack : ChestStealer.Type.QuickMove;
      } else if (i.getItem().equals(Items.ENDER_PEARL) && this.stealCountList[8] > 0) {
         this.stealCountList[8] -= i.getCount();
         return this.stealCountList[8] < 0 ? ChestStealer.Type.Stack : ChestStealer.Type.QuickMove;
      } else if (i.getItem() instanceof BlockItem && ((BlockItem)i.getItem()).getBlock() instanceof PistonBlock && this.stealCountList[9] > 0) {
         this.stealCountList[9] -= i.getCount();
         return this.stealCountList[9] < 0 ? ChestStealer.Type.Stack : ChestStealer.Type.QuickMove;
      } else if (i.getItem().equals(Blocks.REDSTONE_BLOCK.asItem()) && this.stealCountList[10] > 0) {
         this.stealCountList[10] -= i.getCount();
         return this.stealCountList[10] < 0 ? ChestStealer.Type.Stack : ChestStealer.Type.QuickMove;
      } else if (i.getItem() instanceof BlockItem && ((BlockItem)i.getItem()).getBlock() instanceof BedBlock && this.stealCountList[11] > 0) {
         this.stealCountList[11] -= i.getCount();
         return this.stealCountList[11] < 0 ? ChestStealer.Type.Stack : ChestStealer.Type.QuickMove;
      } else {
         if (Item.getRawId(i.getItem()) == Item.getRawId(Items.SPLASH_POTION)) {
            PotionContentsComponent potionContentsComponent = (PotionContentsComponent)i.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);

            for(StatusEffectInstance effect : potionContentsComponent.getEffects()) {
               if (effect.getEffectType().value() == StatusEffects.SPEED.value()) {
                  if (this.stealCountList[12] > 0) {
                     this.stealCountList[12] -= i.getCount();
                     if (this.stealCountList[12] < 0) {
                        return ChestStealer.Type.Stack;
                     }

                     return ChestStealer.Type.QuickMove;
                  }
               } else if (effect.getEffectType().value() == StatusEffects.RESISTANCE.value()) {
                  if (this.stealCountList[13] > 0) {
                     this.stealCountList[13] -= i.getCount();
                     if (this.stealCountList[13] < 0) {
                        return ChestStealer.Type.Stack;
                     }

                     return ChestStealer.Type.QuickMove;
                  }
               } else if (effect.getEffectType().value() == StatusEffects.STRENGTH.value() && this.stealCountList[14] > 0) {
                  this.stealCountList[14] -= i.getCount();
                  if (this.stealCountList[14] < 0) {
                     return ChestStealer.Type.Stack;
                  }

                  return ChestStealer.Type.QuickMove;
               }
            }
         }

         return ChestStealer.Type.None;
      }
   }

   private void placeBlock(BlockPos pos) {
      AntiRegear.INSTANCE.safe.add(pos);
      BlockUtil.clickBlock(pos.offset(Direction.DOWN), Direction.UP, this.rotate.getValue(), this.packet.getValue());
   }

   public void onRender3D(MatrixStack matrixStack) {
      if (this.preview.getValue()) {
         this.shulkerContents.clear();

         for(BlockPos pos : BlockUtil.getSphere((float)this.range.getValue())) {
            if (mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock) {
               BlockEntity blockEntity = mc.world.getBlockEntity(pos);
               if (blockEntity instanceof ShulkerBoxBlockEntity) {
                  ShulkerBoxBlockEntity shulkerEntity = (ShulkerBoxBlockEntity)blockEntity;
                  List<ItemStack> items = new ArrayList();

                  for(int i = 0; i < 27; ++i) {
                     items.add(shulkerEntity.getStack(i).copy());
                  }

                  this.shulkerContents.put(pos, items);
                  this.renderShulkerItems(matrixStack, pos, items);
               }
            }
         }

         if (this.previewInventory.getValue()) {
            for(int i = 0; i < mc.player.getInventory().size(); ++i) {
               ItemStack stack = mc.player.getInventory().getStack(i);
               Item items = stack.getItem();
               if (items instanceof BlockItem) {
                  BlockItem blockItem = (BlockItem)items;
                  if (blockItem.getBlock() instanceof ShulkerBoxBlock) {
                     ItemStack[] shulkerItems = new ItemStack[27];
                     ShulkerViewer.getItemsInContainerItem(stack, shulkerItems);
                     List<ItemStack> itemList = new ArrayList();

                     for(ItemStack item : shulkerItems) {
                        itemList.add(item);
                     }

                     BlockPos playerPos = mc.player.getBlockPos().up(2 + i / 9);
                     this.renderShulkerItems(matrixStack, playerPos, itemList);
                  }
               }
            }
         }

      }
   }

   private void renderShulkerItems(MatrixStack matrixStack, BlockPos pos, List<ItemStack> items) {
      double x = (double)pos.getX() + (double)0.5F;
      double y = (double)pos.getY() + this.previewHeight.getValue();
      double z = (double)pos.getZ() + (double)0.5F;
      float scale = (float)this.previewScale.getValue();
      matrixStack.push();
      matrixStack.translate(x - mc.gameRenderer.getCamera().getPos().x, y - mc.gameRenderer.getCamera().getPos().y, z - mc.gameRenderer.getCamera().getPos().z);
      float yaw = mc.gameRenderer.getCamera().getYaw();
      float pitch = mc.gameRenderer.getCamera().getPitch();
      matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
      matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
      matrixStack.scale(scale, scale, scale);
      int row = 0;
      int col = 0;
      float spacing = 0.4F;
      float startX = -spacing * 4.0F;
      float startY = spacing * 1.5F;

      for(int i = 0; i < items.size(); ++i) {
         ItemStack stack = (ItemStack)items.get(i);
         if (!stack.isEmpty()) {
            matrixStack.push();
            float offsetX = startX + (float)col * spacing;
            float offsetY = startY - (float)row * spacing;
            matrixStack.translate(offsetX, offsetY, 0.0F);
            matrixStack.scale(0.5F, 0.5F, 0.5F);
            mc.getItemRenderer().renderItem(stack, ModelTransformationMode.GUI, 15728880, OverlayTexture.DEFAULT_UV, matrixStack, mc.getBufferBuilders().getEntityVertexConsumers(), mc.world, 0);
            matrixStack.pop();
         }

         ++col;
         if (col >= 9) {
            col = 0;
            ++row;
         }
      }

      matrixStack.pop();
   }

   public boolean isStealing() {
      return this.stealing;
   }

   public void setStealing(boolean stealing) {
      this.stealing = stealing;
   }

   public List<ItemStack> getStealingItems() {
      return this.stealingItems;
   }

   public void updateStealingItems(List<ItemStack> items) {
      this.stealingItems = new ArrayList(items);
   }

   public void clearStealingItems() {
      this.stealingItems.clear();
      this.stealing = false;
   }

   static {
      String var10002 = System.getProperty("user.dir");
      KITS_FILE = new File(var10002 + File.separator + "nina" + File.separator + "kits.cfg");
   }

   public static enum Mode {
      SMART("Smart"),
      REGEAR("Regear");

      private final String name;

      private Mode(String name) {
         this.name = name;
      }

      public String toString() {
         return this.name;
      }

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{SMART, REGEAR};
      }
   }

   private static enum Type {
      None,
      Stack,
      QuickMove;

      // $FF: synthetic method
      private static Type[] $values() {
         return new Type[]{None, Stack, QuickMove};
      }
   }
}
