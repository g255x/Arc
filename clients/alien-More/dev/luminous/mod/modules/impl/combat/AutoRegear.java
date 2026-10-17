package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Nina;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.OpenScreenEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.core.Manager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.player.PacketMine;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.class_1291;
import net.minecraft.class_1293;
import net.minecraft.class_1294;
import net.minecraft.class_1703;
import net.minecraft.class_1707;
import net.minecraft.class_1713;
import net.minecraft.class_1733;
import net.minecraft.class_1735;
import net.minecraft.class_1747;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1844;
import net.minecraft.class_2244;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_2480;
import net.minecraft.class_2596;
import net.minecraft.class_2649;
import net.minecraft.class_2665;
import net.minecraft.class_2813;
import net.minecraft.class_2815;
import net.minecraft.class_3532;
import net.minecraft.class_3944;
import net.minecraft.class_476;
import net.minecraft.class_495;
import net.minecraft.class_9334;
import org.apache.commons.io.IOUtils;

public class AutoRegear extends Module {
   public static AutoRegear INSTANCE;
   public final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
   public final Timer timeoutTimer = new Timer();
   final int[] stealCountList = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
   private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true));
   private final SliderSetting disableTime = this.add(new SliderSetting("DisableTime", 500, 0, 1000));
   private final BooleanSetting place = this.add(new BooleanSetting("Place", true));
   private final SliderSetting placeDelay = this.add(new SliderSetting("PlaceDelay", 0, 0, 1000));
   private final BooleanSetting packet = this.add(new BooleanSetting("PacketPlace", false));
   private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
   private final BooleanSetting preferOpen = this.add(new BooleanSetting("PerferOpen", true));
   private final BooleanSetting open = this.add(new BooleanSetting("Open", true));
   private final SliderSetting range = this.add(new SliderSetting("MaxRange", (double)4.0F, (double)0.0F, (double)6.0F, 0.1));
   private final SliderSetting minRange = this.add(new SliderSetting("MinRange", (double)1.0F, (double)0.0F, (double)3.0F, 0.1));
   private final BooleanSetting mine = this.add(new BooleanSetting("Mine", true));
   private final BooleanSetting take = this.add(new BooleanSetting("Take", true));
   private final BooleanSetting smart;
   private final BooleanSetting kit;
   private final StringSetting kitName;
   private final BooleanSetting silent;
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
   private final BindSetting placeKey;
   private final Timer timer;
   private final Timer enderChestTimer;
   private final List openList;
   private final Map kitMap;
   private String kitNameCache;
   private boolean kitLoaded;
   private volatile int kitShulkerCount;
   public class_2338 placePos;
   private class_2338 openPos;
   private boolean opend;
   private boolean enderChestOpening;
   private int enderChestWindowId;
   private int enderChestMoved;
   private boolean on;

   public AutoRegear() {
      super("AutoRegear", Module.Category.Combat);
      BooleanSetting var10006 = this.take;
      Objects.requireNonNull(var10006);
      this.smart = this.add((new BooleanSetting("Smart", true, var10006::getValue)).setParent());
      var10006 = this.take;
      Objects.requireNonNull(var10006);
      this.kit = this.add((new BooleanSetting("Kit", false, var10006::getValue)).setParent().injectTask(this::syncKitSettings));
      this.kitName = this.add(new StringSetting("KitName", "kit1", () -> this.take.getValue() && this.kit.getValue()));
      this.silent = this.add(new BooleanSetting("Silent", false, () -> this.take.getValue() && this.kit.getValue()));
      this.forceMove = this.add(new BooleanSetting("ForceQuickMove", true, () -> this.take.getValue() && this.smart.isOpen()));
      this.crystal = this.add(new SliderSetting("Crystal", 256, 0, 512, () -> this.take.getValue() && this.smart.isOpen()));
      this.exp = this.add(new SliderSetting("Exp", 256, 0, 512, () -> this.take.getValue() && this.smart.isOpen()));
      this.totem = this.add(new SliderSetting("Totem", 6, 0, 36, () -> this.take.getValue() && this.smart.isOpen()));
      this.gapple = this.add(new SliderSetting("Gapple", 128, 0, 512, () -> this.take.getValue() && this.smart.isOpen()));
      this.obsidian = this.add(new SliderSetting("Obsidian", 64, 0, 512, () -> this.take.getValue() && this.smart.isOpen()));
      this.web = this.add(new SliderSetting("Web", 64, 0, 512, () -> this.take.getValue() && this.smart.isOpen()));
      this.glowstone = this.add(new SliderSetting("Glowstone", 128, 0, 512, () -> this.take.getValue() && this.smart.isOpen()));
      this.anchor = this.add(new SliderSetting("Anchor", 128, 0, 512, () -> this.take.getValue() && this.smart.isOpen()));
      this.pearl = this.add(new SliderSetting("Pearl", 16, 0, 64, () -> this.take.getValue() && this.smart.isOpen()));
      this.piston = this.add(new SliderSetting("Piston", 64, 0, 512, () -> this.take.getValue() && this.smart.isOpen()));
      this.redstone = this.add(new SliderSetting("RedStone", 64, 0, 512, () -> this.take.getValue() && this.smart.isOpen()));
      this.bed = this.add(new SliderSetting("Bed", 256, 0, 512, () -> this.take.getValue() && this.smart.isOpen()));
      this.speed = this.add(new SliderSetting("Speed", 1, 0, 8, () -> this.take.getValue() && this.smart.isOpen()));
      this.resistance = this.add(new SliderSetting("Resistance", 1, 0, 8, () -> this.take.getValue() && this.smart.isOpen()));
      this.strength = this.add(new SliderSetting("Strength", 1, 0, 8, () -> this.take.getValue() && this.smart.isOpen()));
      this.placeKey = this.add(new BindSetting("PlaceKey", -1));
      this.timer = new Timer();
      this.enderChestTimer = new Timer();
      this.openList = new ArrayList();
      this.kitMap = new ConcurrentHashMap();
      this.kitNameCache = "";
      this.kitLoaded = false;
      this.kitShulkerCount = 0;
      this.placePos = null;
      this.opend = false;
      this.enderChestOpening = false;
      this.enderChestWindowId = -1;
      this.enderChestMoved = 0;
      this.on = false;
      this.setChinese("自动补给");
      INSTANCE = this;
   }

   public int findShulker() {
      return this.inventory.getValue() ? InventoryUtil.findClassInventorySlot(class_2480.class) : InventoryUtil.findClass(class_2480.class);
   }

   public void onEnable() {
      this.opend = false;
      this.openPos = null;
      this.openList.clear();
      this.timeoutTimer.reset();
      this.placePos = null;
      this.timer.reset();
      if (this.kit.getValue()) {
         this.syncKitSettings();
      }

      if (!nullCheck()) {
         ;
      }
   }

   private void doPlace() {
      int oldSlot = mc.field_1724.method_31548().field_7545;
      double distance = (double)100.0F;
      class_2338 bestPos = null;

      for(class_2338 pos : BlockUtil.getSphere((float)this.range.getValue())) {
         if (this.preferOpen.getValue() && mc.field_1687.method_8320(pos).method_26204() instanceof class_2480) {
            if (!this.openList.contains(pos) && this.canOpenShulker(pos)) {
               if (this.open.getValue()) {
                  this.openPos = pos;
                  BlockUtil.clickBlock(pos, BlockUtil.getClickSide(pos), this.rotate.getValue(), this.packet.getValue());
               }

               return;
            }
         } else if (!((double)class_3532.method_15355((float)mc.field_1724.method_5707(pos.method_46558())) < this.minRange.getValue())) {
            class_2350 placeSide = BlockUtil.getPlaceSide(pos, this.range.getValue());
            if (BlockUtil.clientCanPlace(pos, false) && placeSide != null && this.canOpenShulkerAfterPlace(pos, placeSide) && (bestPos == null || (double)class_3532.method_15355((float)mc.field_1724.method_5707(pos.method_46558())) < distance)) {
               distance = (double)class_3532.method_15355((float)mc.field_1724.method_5707(pos.method_46558()));
               bestPos = pos;
            }
         }
      }

      if (bestPos != null) {
         int shulkerSlot = this.findShulker();
         if (shulkerSlot == -1) {
            if (this.tryOpenEnderChest()) {
               return;
            }

            return;
         }

         if (this.inventory.getValue()) {
            InventoryUtil.inventorySwap(shulkerSlot, oldSlot);
            this.placeBlock(bestPos);
            this.placePos = bestPos;
            InventoryUtil.inventorySwap(shulkerSlot, oldSlot);
         } else {
            InventoryUtil.switchToSlot(shulkerSlot);
            this.placeBlock(bestPos);
            this.placePos = bestPos;
            InventoryUtil.switchToSlot(oldSlot);
         }

         this.timer.reset();
      }

   }

   private void update() {
      this.stealCountList[0] = (int)(this.crystal.getValue() - (double)InventoryUtil.getItemCount(class_1802.field_8301));
      this.stealCountList[1] = (int)(this.exp.getValue() - (double)InventoryUtil.getItemCount(class_1802.field_8287));
      this.stealCountList[2] = (int)(this.totem.getValue() - (double)InventoryUtil.getItemCount(class_1802.field_8288));
      this.stealCountList[3] = (int)(this.gapple.getValue() - (double)InventoryUtil.getItemCount(class_1802.field_8367));
      this.stealCountList[4] = (int)(this.obsidian.getValue() - (double)InventoryUtil.getItemCount(class_2246.field_10540.method_8389()));
      this.stealCountList[5] = (int)(this.web.getValue() - (double)InventoryUtil.getItemCount(class_2246.field_10343.method_8389()));
      this.stealCountList[6] = (int)(this.glowstone.getValue() - (double)InventoryUtil.getItemCount(class_2246.field_10171.method_8389()));
      this.stealCountList[7] = (int)(this.anchor.getValue() - (double)InventoryUtil.getItemCount(class_2246.field_23152.method_8389()));
      this.stealCountList[8] = (int)(this.pearl.getValue() - (double)InventoryUtil.getItemCount(class_1802.field_8634));
      this.stealCountList[9] = (int)(this.piston.getValue() - (double)InventoryUtil.getItemCount(class_2246.field_10560.method_8389()) - (double)InventoryUtil.getItemCount(class_2246.field_10615.method_8389()));
      this.stealCountList[10] = (int)(this.redstone.getValue() - (double)InventoryUtil.getItemCount(class_2246.field_10002.method_8389()));
      this.stealCountList[11] = (int)(this.bed.getValue() - (double)InventoryUtil.getItemCount(class_2244.class));
      this.stealCountList[12] = (int)(this.speed.getValue() - (double)InventoryUtil.getPotionCount((class_1291)class_1294.field_5904.comp_349()));
      this.stealCountList[13] = (int)(this.resistance.getValue() - (double)InventoryUtil.getPotionCount((class_1291)class_1294.field_5907.comp_349()));
      this.stealCountList[14] = (int)(this.strength.getValue() - (double)InventoryUtil.getPotionCount((class_1291)class_1294.field_5910.comp_349()));
   }

   public void onDisable() {
      this.opend = false;
      this.enderChestOpening = false;
      this.enderChestWindowId = -1;
      this.enderChestMoved = 0;
      if (this.mine.getValue() && this.placePos != null) {
         PacketMine.INSTANCE.mine(this.placePos);
      }

      this.openPos = null;
      this.placePos = null;
      this.openList.clear();
   }

   @EventListener
   public void onOpenScreen(OpenScreenEvent event) {
      if (this.shouldSilent()) {
         if (event.screen instanceof class_495 || event.screen instanceof class_476) {
            event.cancel();
         }

      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.kit.getValue()) {
         this.syncKitSettings();
      } else if (this.smart.getValue()) {
         this.update();
      }

      if (!this.handleEnderChestSupply()) {
         if (this.placePos != null && !this.opend && mc.field_1687.method_8320(this.placePos).method_26204() instanceof class_2480 && !this.canOpenShulker(this.placePos)) {
            if (this.mine.getValue()) {
               PacketMine.INSTANCE.mine(this.placePos);
            }

            this.placePos = null;
            this.openPos = null;
            this.timer.reset();
         }

         if (this.placePos != null && !this.opend && this.timer.passed(750L) && !(mc.field_1687.method_8320(this.placePos).method_26204() instanceof class_2480)) {
            this.placePos = null;
         }

         if (this.placeKey.isPressed() && mc.field_1755 == null) {
            if (!this.on) {
               this.opend = false;
               this.openPos = null;
               this.timeoutTimer.reset();
               this.placePos = null;
               this.doPlace();
            }

            this.on = true;
            if (this.placePos == null && this.openPos == null && !this.opend && this.timer.passed((long)this.placeDelay.getValueInt())) {
               this.doPlace();
            }
         } else {
            this.on = false;
         }

         this.openList.removeIf((pos) -> !(mc.field_1687.method_8320(pos).method_26204() instanceof class_2480));
         if (this.place.getValue() && this.placePos == null && !this.opend && this.timer.passed((long)this.placeDelay.getValueInt())) {
            this.doPlace();
         }

         boolean shulkerOpen = this.isShulkerScreenOpen();
         if (!shulkerOpen) {
            if (this.opend) {
               this.opend = false;
               if (this.autoDisable.getValue()) {
                  this.timeoutToDisable();
               }

               if (this.mine.getValue() && this.openPos != null) {
                  if (mc.field_1687.method_8320(this.openPos).method_26204() instanceof class_2480) {
                     PacketMine.INSTANCE.mine(this.openPos);
                  } else {
                     this.openPos = null;
                  }
               }

            } else {
               if (this.open.getValue()) {
                  if (this.placePos == null || !((double)class_3532.method_15355((float)mc.field_1724.method_5707(this.placePos.method_46558())) <= this.range.getValue()) || this.timer.passed(500L) && !(mc.field_1687.method_8320(this.placePos).method_26204() instanceof class_2480)) {
                     boolean found = false;

                     for(class_2338 pos2 : BlockUtil.getSphere((float)this.range.getValue())) {
                        if (!this.openList.contains(pos2) && mc.field_1687.method_8320(pos2).method_26204() instanceof class_2480 && this.canOpenShulker(pos2)) {
                           this.openPos = pos2;
                           BlockUtil.clickBlock(pos2, BlockUtil.getClickSide(pos2), this.rotate.getValue(), this.packet.getValue());
                           found = true;
                           break;
                        }
                     }

                     if (!found && this.autoDisable.getValue()) {
                        this.timeoutToDisable();
                     }
                  } else if (mc.field_1687.method_8320(this.placePos).method_26204() instanceof class_2480 && this.canOpenShulker(this.placePos)) {
                     this.openPos = this.placePos;
                     BlockUtil.clickBlock(this.placePos, BlockUtil.getClickSide(this.placePos), this.rotate.getValue(), this.packet.getValue());
                  }
               } else if (!this.take.getValue() && this.autoDisable.getValue()) {
                  this.timeoutToDisable();
               }

            }
         } else {
            this.opend = true;
            if (this.openPos != null) {
               this.openList.add(this.openPos);
            }

            if (!this.take.getValue()) {
               if (this.autoDisable.getValue()) {
                  this.timeoutToDisable();
               }

            } else {
               boolean take = false;
               class_1703 screenHandler = mc.field_1724.field_7512;
               if (screenHandler instanceof class_1733) {
                  class_1733 shulker = (class_1733)screenHandler;
                  if (this.kit.getValue()) {
                     this.ensureKitLoaded();
                     if (!this.kitMap.isEmpty()) {
                        String airKey = class_1802.field_8162.method_7876();

                        for(int slotIndex = 0; slotIndex < 36; ++slotIndex) {
                           int handlerSlotId = this.getShulkerPlayerSlotId(slotIndex);
                           class_1735 slot = (class_1735)shulker.field_7761.get(handlerSlotId);
                           class_1799 stack = slot.method_7677();
                           if (!stack.method_7960()) {
                              String target = (String)this.kitMap.get(slotIndex);
                              if (target != null && (target.equals(airKey) || !stack.method_7909().method_7876().equals(target))) {
                                 class_1792 item = stack.method_7909();
                                 if (!(item instanceof class_1747) || !(((class_1747)item).method_7711() instanceof class_2480)) {
                                    if (!this.hasEmptyShulkerSlot(shulker)) {
                                       mc.field_1761.method_2906(shulker.field_7763, handlerSlotId, 1, class_1713.field_7795, mc.field_1724);
                                    } else {
                                       mc.field_1761.method_2906(shulker.field_7763, handlerSlotId, 0, class_1713.field_7794, mc.field_1724);
                                    }

                                    take = true;
                                 }
                              }
                           }
                        }

                        for(int slotIndex = 0; slotIndex < 36; ++slotIndex) {
                           int handlerSlotId = this.getShulkerPlayerSlotId(slotIndex);
                           class_1735 targetSlot = (class_1735)shulker.field_7761.get(handlerSlotId);
                           class_1799 targetStack = targetSlot.method_7677();
                           String target = (String)this.kitMap.get(slotIndex);
                           if (target != null && !target.equals(airKey) && (targetStack.method_7960() || !targetStack.method_7909().method_7876().equals(target))) {
                              int sourceSlotId = this.findShulkerSlot(shulker, target);
                              if (sourceSlotId != -1) {
                                 mc.field_1761.method_2906(shulker.field_7763, sourceSlotId, 0, class_1713.field_7790, mc.field_1724);
                                 mc.field_1761.method_2906(shulker.field_7763, handlerSlotId, 0, class_1713.field_7790, mc.field_1724);
                                 mc.field_1761.method_2906(shulker.field_7763, sourceSlotId, 0, class_1713.field_7790, mc.field_1724);
                                 take = true;
                              }
                           }
                        }
                     }
                  } else {
                     for(class_1735 slot : shulker.field_7761) {
                        if (slot.field_7874 < 27 && !slot.method_7677().method_7960() && (!this.smart.getValue() || this.needSteal(slot.method_7677()))) {
                           mc.field_1761.method_2906(shulker.field_7763, slot.field_7874, 0, class_1713.field_7794, mc.field_1724);
                           take = true;
                        }
                     }
                  }
               }

               if (!take && this.silent.getValue() && this.kit.getValue() && screenHandler instanceof class_1733) {
                  mc.field_1724.method_7346();
                  this.opend = false;
               }

               if (this.autoDisable.getValue() && !take) {
                  this.timeoutToDisable();
               }

            }
         }
      }
   }

   private void syncKitSettings() {
      if (this.smart.getValue()) {
         this.smart.setValueWithoutTask(false);
      }

      this.ensureKitLoaded();
   }

   private boolean handleEnderChestSupply() {
      if (this.kit.getValue() && this.enderChestOpening) {
         if (this.silent.getValue()) {
            if (this.enderChestTimer.passed(1500L)) {
               this.enderChestOpening = false;
               this.enderChestWindowId = -1;
               this.enderChestMoved = 0;
            }

            return false;
         } else {
            int targetCount = this.getKitShulkerCount();
            if (targetCount <= 0) {
               this.enderChestOpening = false;
               return false;
            } else if (!this.hasEmptyPlayerSlot()) {
               this.enderChestOpening = false;
               mc.field_1724.method_7346();
               if (this.autoDisable.getValue()) {
                  this.timeoutToDisable();
               }

               return true;
            } else if (!(mc.field_1724.field_7512 instanceof class_1707)) {
               if (this.enderChestTimer.passed(750L)) {
                  this.enderChestOpening = false;
               }

               return false;
            } else {
               class_1707 handler = (class_1707)mc.field_1724.field_7512;
               boolean found = false;
               int movedCount = 0;

               for(class_1735 slot : handler.field_7761) {
                  if (slot.field_7874 < 27) {
                     class_1799 stack = slot.method_7677();
                     if (this.isShulkerBoxStack(stack)) {
                        found = true;
                        mc.field_1761.method_2906(handler.field_7763, slot.field_7874, 0, class_1713.field_7794, mc.field_1724);
                        if (slot.method_7677().method_7960()) {
                           ++movedCount;
                        }

                        if (movedCount >= targetCount) {
                           break;
                        }
                     }
                  }
               }

               if (!found || movedCount >= targetCount) {
                  this.enderChestOpening = false;
                  mc.field_1724.method_7346();
                  if (!found && this.autoDisable.getValue()) {
                     this.timeoutToDisable();
                  }
               }

               return true;
            }
         }
      } else {
         return false;
      }
   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive event) {
      if (this.kit.getValue() && this.enderChestOpening && this.silent.getValue()) {
         class_2596 var3 = event.getPacket();
         if (var3 instanceof class_3944) {
            class_3944 packet = (class_3944)var3;
            this.enderChestWindowId = packet.method_17592();
            this.enderChestMoved = 0;
            this.enderChestTimer.reset();
            event.cancel();
         } else {
            var3 = event.getPacket();
            if (var3 instanceof class_2649) {
               class_2649 packet = (class_2649)var3;
               if (this.enderChestWindowId == -1 || packet.method_11440() != this.enderChestWindowId) {
                  return;
               }

               event.cancel();
               int targetCount = this.getKitShulkerCount();
               if (targetCount <= 0) {
                  mc.method_1562().method_52787(new class_2815(this.enderChestWindowId));
                  this.enderChestOpening = false;
                  this.enderChestWindowId = -1;
                  this.enderChestMoved = 0;
                  return;
               }

               List<class_1799> stacks = packet.method_11441();
               boolean found = false;

               for(int i = 0; i < stacks.size() && i < 27; ++i) {
                  class_1799 stack = (class_1799)stacks.get(i);
                  if (this.isShulkerBoxStack(stack)) {
                     found = true;
                     mc.method_1562().method_52787(new class_2813(this.enderChestWindowId, 0, i, 0, class_1713.field_7794, class_1799.field_8037, Int2ObjectMaps.emptyMap()));
                     ++this.enderChestMoved;
                     if (this.enderChestMoved >= targetCount) {
                        break;
                     }
                  }
               }

               mc.method_1562().method_52787(new class_2815(this.enderChestWindowId));
               this.enderChestOpening = false;
               this.enderChestWindowId = -1;
               this.enderChestMoved = 0;
               if (!found && this.autoDisable.getValue()) {
                  this.timeoutToDisable();
               }
            }

         }
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
         this.kitMap.clear();
         this.kitShulkerCount = 0;
         Nina.THREAD.execute(() -> {
            try {
               File file = Manager.getFile(name + ".kit");
               if (!file.exists()) {
                  return;
               }

               List<String> list = IOUtils.readLines(new FileInputStream(file), StandardCharsets.UTF_8);
               int shulkerCount = 0;

               for(String s : list) {
                  String[] split = s.split(":");
                  if (split.length == 2) {
                     this.kitMap.put(Integer.valueOf(split[0]), split[1]);
                     if (this.isShulkerTranslationKey(split[1])) {
                        ++shulkerCount;
                     }
                  }
               }

               this.kitShulkerCount = shulkerCount;
            } catch (Exception e) {
               e.printStackTrace();
            }

         });
      }
   }

   private boolean isKitItem(class_1799 stack) {
      return this.kitMap.containsValue(stack.method_7909().method_7876());
   }

   private int getKitShulkerCount() {
      return this.kitShulkerCount;
   }

   private boolean isShulkerScreenOpen() {
      if (mc.field_1724 == null) {
         return false;
      } else {
         return this.silent.getValue() && this.kit.getValue() ? mc.field_1724.field_7512 instanceof class_1733 : mc.field_1755 instanceof class_495;
      }
   }

   private boolean shouldSilent() {
      if (this.isOn() && this.silent.getValue() && this.kit.getValue()) {
         return this.opend || this.placePos != null || this.openPos != null || this.enderChestOpening;
      } else {
         return false;
      }
   }

   private boolean isShulkerBoxStack(class_1799 stack) {
      if (stack != null && !stack.method_7960()) {
         class_1792 item = stack.method_7909();
         return item instanceof class_1747 && ((class_1747)item).method_7711() instanceof class_2480;
      } else {
         return false;
      }
   }

   private boolean isShulkerTranslationKey(String translationKey) {
      if (translationKey == null) {
         return false;
      } else {
         return translationKey.equals(class_1802.field_8545.method_7876()) || translationKey.equals(class_1802.field_8722.method_7876()) || translationKey.equals(class_1802.field_8380.method_7876()) || translationKey.equals(class_1802.field_8050.method_7876()) || translationKey.equals(class_1802.field_8829.method_7876()) || translationKey.equals(class_1802.field_8271.method_7876()) || translationKey.equals(class_1802.field_8548.method_7876()) || translationKey.equals(class_1802.field_8520.method_7876()) || translationKey.equals(class_1802.field_8627.method_7876()) || translationKey.equals(class_1802.field_8451.method_7876()) || translationKey.equals(class_1802.field_8213.method_7876()) || translationKey.equals(class_1802.field_8816.method_7876()) || translationKey.equals(class_1802.field_8350.method_7876()) || translationKey.equals(class_1802.field_8584.method_7876()) || translationKey.equals(class_1802.field_8461.method_7876()) || translationKey.equals(class_1802.field_8676.method_7876()) || translationKey.equals(class_1802.field_8268.method_7876());
      }
   }

   private boolean tryOpenEnderChest() {
      if (this.kit.getValue() && !this.enderChestOpening && this.getKitShulkerCount() > 0) {
         if (!this.hasEmptyPlayerSlot()) {
            return false;
         } else {
            for(class_2338 pos : BlockUtil.getSphere((float)this.range.getValue())) {
               if (mc.field_1687.method_8320(pos).method_26204() == class_2246.field_10443) {
                  this.enderChestOpening = true;
                  this.enderChestTimer.reset();
                  BlockUtil.clickBlock(pos, BlockUtil.getClickSide(pos), this.rotate.getValue(), this.packet.getValue());
                  return true;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   private boolean hasEmptyPlayerSlot() {
      for(int i = 0; i < 36; ++i) {
         if (mc.field_1724.method_31548().method_5438(i).method_7960()) {
            return true;
         }
      }

      return false;
   }

   private int findShulkerSlot(class_1733 shulker, String translationKey) {
      for(class_1735 slot : shulker.field_7761) {
         if (slot.field_7874 < 27) {
            class_1799 stack = slot.method_7677();
            if (!stack.method_7960() && stack.method_7909().method_7876().equals(translationKey)) {
               return slot.field_7874;
            }
         }
      }

      return -1;
   }

   private boolean hasEmptyShulkerSlot(class_1733 shulker) {
      for(class_1735 slot : shulker.field_7761) {
         if (slot.field_7874 < 27 && slot.method_7677().method_7960()) {
            return true;
         }
      }

      return false;
   }

   private int getShulkerPlayerSlotId(int slotIndex) {
      return slotIndex < 9 ? 54 + slotIndex : 27 + (slotIndex - 9);
   }

   private boolean canOpenShulkerAfterPlace(class_2338 pos, class_2350 placeSide) {
      class_2350 openDir = BlockUtil.allowAirPlace() ? BlockUtil.getClickSide(pos) : placeSide.method_10153();
      class_2338 front = pos.method_10093(openDir);
      return mc.field_1687.method_8320(front).method_26220(mc.field_1687, front).method_1110();
   }

   private boolean canOpenShulker(class_2338 pos) {
      if (!(mc.field_1687.method_8320(pos).method_26204() instanceof class_2480)) {
         return false;
      } else {
         class_2350 openDir = (class_2350)mc.field_1687.method_8320(pos).method_11654(class_2480.field_11496);
         class_2338 front = pos.method_10093(openDir);
         return mc.field_1687.method_8320(front).method_26220(mc.field_1687, front).method_1110();
      }
   }

   private void timeoutToDisable() {
      if (this.timeoutTimer.passed((long)this.disableTime.getValueInt())) {
         this.disable();
      }

   }

   private boolean needSteal(class_1799 i) {
      if (i.method_7909().equals(class_1802.field_8301) && this.stealCountList[0] > 0) {
         this.stealCountList[0] -= i.method_7947();
         return true;
      } else if (i.method_7909().equals(class_1802.field_8287) && this.stealCountList[1] > 0) {
         this.stealCountList[1] -= i.method_7947();
         return true;
      } else if (i.method_7909().equals(class_1802.field_8288) && this.stealCountList[2] > 0) {
         this.stealCountList[2] -= i.method_7947();
         return true;
      } else if (i.method_7909().equals(class_1802.field_8367) && this.stealCountList[3] > 0) {
         this.stealCountList[3] -= i.method_7947();
         return true;
      } else if (i.method_7909().equals(class_2246.field_10540.method_8389()) && this.stealCountList[4] > 0) {
         this.stealCountList[4] -= i.method_7947();
         return true;
      } else if (i.method_7909().equals(class_2246.field_10343.method_8389()) && this.stealCountList[5] > 0) {
         this.stealCountList[5] -= i.method_7947();
         return true;
      } else if (i.method_7909().equals(class_2246.field_10171.method_8389()) && this.stealCountList[6] > 0) {
         this.stealCountList[6] -= i.method_7947();
         return true;
      } else if (i.method_7909().equals(class_2246.field_23152.method_8389()) && this.stealCountList[7] > 0) {
         this.stealCountList[7] -= i.method_7947();
         return true;
      } else if (i.method_7909().equals(class_1802.field_8634) && this.stealCountList[8] > 0) {
         this.stealCountList[8] -= i.method_7947();
         return true;
      } else if (i.method_7909() instanceof class_1747 && ((class_1747)i.method_7909()).method_7711() instanceof class_2665 && this.stealCountList[9] > 0) {
         this.stealCountList[9] -= i.method_7947();
         return true;
      } else if (i.method_7909().equals(class_2246.field_10002.method_8389()) && this.stealCountList[10] > 0) {
         this.stealCountList[10] -= i.method_7947();
         return true;
      } else if (i.method_7909() instanceof class_1747 && ((class_1747)i.method_7909()).method_7711() instanceof class_2244 && this.stealCountList[11] > 0) {
         this.stealCountList[11] -= i.method_7947();
         return true;
      } else {
         if (i.method_7909() == class_1802.field_8436) {
            class_1844 potionContents = (class_1844)i.method_57825(class_9334.field_49651, class_1844.field_49274);

            for(class_1293 effect : potionContents.method_57397()) {
               if (effect.method_5579() == class_1294.field_5904) {
                  if (this.stealCountList[12] > 0) {
                     this.stealCountList[12] -= i.method_7947();
                     return true;
                  }
               } else if (effect.method_5579() == class_1294.field_5907) {
                  if (this.stealCountList[13] > 0) {
                     this.stealCountList[13] -= i.method_7947();
                     return true;
                  }
               } else if (effect.method_5579() == class_1294.field_5910 && this.stealCountList[14] > 0) {
                  this.stealCountList[14] -= i.method_7947();
                  return true;
               }
            }
         }

         return false;
      }
   }

   private void placeBlock(class_2338 pos) {
      BlockUtil.placeBlock(pos, this.rotate.getValue(), this.packet.getValue());
   }
}
