package dev.G255.Lumi.mod.modules.impl.combat;

import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.OpenScreenEvent;
import dev.G255.Lumi.api.events.impl.PacketEvent;
import dev.G255.Lumi.api.events.impl.UpdateEvent;
import dev.G255.Lumi.api.utils.math.Timer;
import dev.G255.Lumi.api.utils.player.InventoryUtil;
import dev.G255.Lumi.api.utils.world.BlockUtil;
import dev.G255.Lumi.core.Manager;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.impl.player.PacketMine;
import dev.G255.Lumi.mod.modules.settings.impl.BindSetting;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import dev.G255.Lumi.mod.modules.settings.impl.StringSetting;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.block.PistonBlock;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.component.DataComponentTypes;
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
   private final List<BlockPos> openList;
   private final Map kitMap;
   private String kitNameCache;
   private boolean kitLoaded;
   private volatile int kitShulkerCount;
   public BlockPos placePos;
   private BlockPos openPos;
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
      this.setChinese("");
      INSTANCE = this;
   }

   public int findShulker() {
      return this.inventory.getValue() ? InventoryUtil.findClassInventorySlot(ShulkerBoxBlock.class) : InventoryUtil.findClass(ShulkerBoxBlock.class);
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
      int oldSlot = mc.player.getInventory().selectedSlot;
      double distance = (double)100.0F;
      BlockPos bestPos = null;

      for(BlockPos pos : BlockUtil.getSphere((float)this.range.getValue())) {
         if (this.preferOpen.getValue() && mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock) {
            if (!this.openList.contains(pos) && this.canOpenShulker(pos)) {
               if (this.open.getValue()) {
                  this.openPos = pos;
                  BlockUtil.clickBlock(pos, BlockUtil.getClickSide(pos), this.rotate.getValue(), this.packet.getValue());
               }

               return;
            }
         } else if (!((double)MathHelper.sqrt((float)mc.player.squaredDistanceTo(pos.toCenterPos())) < this.minRange.getValue())) {
            Direction placeSide = BlockUtil.getPlaceSide(pos, this.range.getValue());
            if (BlockUtil.clientCanPlace(pos, false) && placeSide != null && this.canOpenShulkerAfterPlace(pos, placeSide) && (bestPos == null || (double)MathHelper.sqrt((float)mc.player.squaredDistanceTo(pos.toCenterPos())) < distance)) {
               distance = (double)MathHelper.sqrt((float)mc.player.squaredDistanceTo(pos.toCenterPos()));
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
         if (event.screen instanceof ShulkerBoxScreen || event.screen instanceof GenericContainerScreen) {
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
         if (this.placePos != null && !this.opend && mc.world.getBlockState(this.placePos).getBlock() instanceof ShulkerBoxBlock && !this.canOpenShulker(this.placePos)) {
            if (this.mine.getValue()) {
               PacketMine.INSTANCE.mine(this.placePos);
            }

            this.placePos = null;
            this.openPos = null;
            this.timer.reset();
         }

         if (this.placePos != null && !this.opend && this.timer.passed(750L) && !(mc.world.getBlockState(this.placePos).getBlock() instanceof ShulkerBoxBlock)) {
            this.placePos = null;
         }

         if (this.placeKey.isPressed() && mc.currentScreen == null) {
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

         this.openList.removeIf((pos) -> !(mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock));
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
                  if (mc.world.getBlockState(this.openPos).getBlock() instanceof ShulkerBoxBlock) {
                     PacketMine.INSTANCE.mine(this.openPos);
                  } else {
                     this.openPos = null;
                  }
               }

            } else {
               if (this.open.getValue()) {
                  if (this.placePos == null || !((double)MathHelper.sqrt((float)mc.player.squaredDistanceTo(this.placePos.toCenterPos())) <= this.range.getValue()) || this.timer.passed(500L) && !(mc.world.getBlockState(this.placePos).getBlock() instanceof ShulkerBoxBlock)) {
                     boolean found = false;

                     for(BlockPos pos2 : BlockUtil.getSphere((float)this.range.getValue())) {
                        if (!this.openList.contains(pos2) && mc.world.getBlockState(pos2).getBlock() instanceof ShulkerBoxBlock && this.canOpenShulker(pos2)) {
                           this.openPos = pos2;
                           BlockUtil.clickBlock(pos2, BlockUtil.getClickSide(pos2), this.rotate.getValue(), this.packet.getValue());
                           found = true;
                           break;
                        }
                     }

                     if (!found && this.autoDisable.getValue()) {
                        this.timeoutToDisable();
                     }
                  } else if (mc.world.getBlockState(this.placePos).getBlock() instanceof ShulkerBoxBlock && this.canOpenShulker(this.placePos)) {
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
               ScreenHandler screenHandler = mc.player.currentScreenHandler;
               if (screenHandler instanceof ShulkerBoxScreenHandler) {
                  ShulkerBoxScreenHandler shulker = (ShulkerBoxScreenHandler)screenHandler;
                  if (this.kit.getValue()) {
                     this.ensureKitLoaded();
                     if (!this.kitMap.isEmpty()) {
                        String airKey = Items.AIR.getTranslationKey();

                        for(int slotIndex = 0; slotIndex < 36; ++slotIndex) {
                           int handlerSlotId = this.getShulkerPlayerSlotId(slotIndex);
                           Slot slot = (Slot)shulker.slots.get(handlerSlotId);
                           ItemStack stack = slot.getStack();
                           if (!stack.isEmpty()) {
                              String target = (String)this.kitMap.get(slotIndex);
                              if (target != null && (target.equals(airKey) || !stack.getItem().getTranslationKey().equals(target))) {
                                 Item item = stack.getItem();
                                 if (!(item instanceof BlockItem) || !(((BlockItem)item).getBlock() instanceof ShulkerBoxBlock)) {
                                    if (!this.hasEmptyShulkerSlot(shulker)) {
                                       mc.interactionManager.clickSlot(shulker.syncId, handlerSlotId, 1, SlotActionType.THROW, mc.player);
                                    } else {
                                       mc.interactionManager.clickSlot(shulker.syncId, handlerSlotId, 0, SlotActionType.QUICK_MOVE, mc.player);
                                    }

                                    take = true;
                                 }
                              }
                           }
                        }

                        for(int slotIndex = 0; slotIndex < 36; ++slotIndex) {
                           int handlerSlotId = this.getShulkerPlayerSlotId(slotIndex);
                           Slot targetSlot = (Slot)shulker.slots.get(handlerSlotId);
                           ItemStack targetStack = targetSlot.getStack();
                           String target = (String)this.kitMap.get(slotIndex);
                           if (target != null && !target.equals(airKey) && (targetStack.isEmpty() || !targetStack.getItem().getTranslationKey().equals(target))) {
                              int sourceSlotId = this.findShulkerSlot(shulker, target);
                              if (sourceSlotId != -1) {
                                 mc.interactionManager.clickSlot(shulker.syncId, sourceSlotId, 0, SlotActionType.PICKUP, mc.player);
                                 mc.interactionManager.clickSlot(shulker.syncId, handlerSlotId, 0, SlotActionType.PICKUP, mc.player);
                                 mc.interactionManager.clickSlot(shulker.syncId, sourceSlotId, 0, SlotActionType.PICKUP, mc.player);
                                 take = true;
                              }
                           }
                        }
                     }
                  } else {
                     for(Slot slot : shulker.slots) {
                        if (slot.id < 27 && !slot.getStack().isEmpty() && (!this.smart.getValue() || this.needSteal(slot.getStack()))) {
                           mc.interactionManager.clickSlot(shulker.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                           take = true;
                        }
                     }
                  }
               }

               if (!take && this.silent.getValue() && this.kit.getValue() && screenHandler instanceof ShulkerBoxScreenHandler) {
                  mc.player.closeHandledScreen();
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
               mc.player.closeHandledScreen();
               if (this.autoDisable.getValue()) {
                  this.timeoutToDisable();
               }

               return true;
            } else if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler)) {
               if (this.enderChestTimer.passed(750L)) {
                  this.enderChestOpening = false;
               }

               return false;
            } else {
               GenericContainerScreenHandler handler = (GenericContainerScreenHandler)mc.player.currentScreenHandler;
               boolean found = false;
               int movedCount = 0;

               for(Slot slot : handler.slots) {
                  if (slot.id < 27) {
                     ItemStack stack = slot.getStack();
                     if (this.isShulkerBoxStack(stack)) {
                        found = true;
                        mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                        if (slot.getStack().isEmpty()) {
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
                  mc.player.closeHandledScreen();
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
         Packet var3 = event.getPacket();
         if (var3 instanceof OpenScreenS2CPacket) {
            OpenScreenS2CPacket packet = (OpenScreenS2CPacket)var3;
            this.enderChestWindowId = packet.getSyncId();
            this.enderChestMoved = 0;
            this.enderChestTimer.reset();
            event.cancel();
         } else {
            var3 = event.getPacket();
            if (var3 instanceof InventoryS2CPacket) {
               InventoryS2CPacket packet = (InventoryS2CPacket)var3;
               if (this.enderChestWindowId == -1 || packet.getSyncId() != this.enderChestWindowId) {
                  return;
               }

               event.cancel();
               int targetCount = this.getKitShulkerCount();
               if (targetCount <= 0) {
                  mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(this.enderChestWindowId));
                  this.enderChestOpening = false;
                  this.enderChestWindowId = -1;
                  this.enderChestMoved = 0;
                  return;
               }

               List<ItemStack> stacks = packet.getContents();
               boolean found = false;

               for(int i = 0; i < stacks.size() && i < 27; ++i) {
                  ItemStack stack = (ItemStack)stacks.get(i);
                  if (this.isShulkerBoxStack(stack)) {
                     found = true;
                     mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(this.enderChestWindowId, 0, i, 0, SlotActionType.QUICK_MOVE, ItemStack.EMPTY, Int2ObjectMaps.emptyMap()));
                     ++this.enderChestMoved;
                     if (this.enderChestMoved >= targetCount) {
                        break;
                     }
                  }
               }

               mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(this.enderChestWindowId));
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
         String finalName = name;
         new Thread(() -> {
            try {
               File file = Manager.getFile(finalName + ".kit");
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

         }).start();
      }
   }

   private boolean isKitItem(ItemStack stack) {
      return this.kitMap.containsValue(stack.getItem().getTranslationKey());
   }

   private int getKitShulkerCount() {
      return this.kitShulkerCount;
   }

   private boolean isShulkerScreenOpen() {
      if (mc.player == null) {
         return false;
      } else {
         return this.silent.getValue() && this.kit.getValue() ? mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler : mc.currentScreen instanceof ShulkerBoxScreen;
      }
   }

   private boolean shouldSilent() {
      if (this.isOn() && this.silent.getValue() && this.kit.getValue()) {
         return this.opend || this.placePos != null || this.openPos != null || this.enderChestOpening;
      } else {
         return false;
      }
   }

   private boolean isShulkerBoxStack(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         Item item = stack.getItem();
         return item instanceof BlockItem && ((BlockItem)item).getBlock() instanceof ShulkerBoxBlock;
      } else {
         return false;
      }
   }

   private boolean isShulkerTranslationKey(String translationKey) {
      if (translationKey == null) {
         return false;
      } else {
         return translationKey.equals(Items.SHULKER_BOX.getTranslationKey()) || translationKey.equals(Items.WHITE_SHULKER_BOX.getTranslationKey()) || translationKey.equals(Items.ORANGE_SHULKER_BOX.getTranslationKey()) || translationKey.equals(Items.MAGENTA_SHULKER_BOX.getTranslationKey()) || translationKey.equals(Items.LIGHT_BLUE_SHULKER_BOX.getTranslationKey()) || translationKey.equals(Items.YELLOW_SHULKER_BOX.getTranslationKey()) || translationKey.equals(Items.LIME_SHULKER_BOX.getTranslationKey()) || translationKey.equals(Items.PINK_SHULKER_BOX.getTranslationKey()) || translationKey.equals(Items.GRAY_SHULKER_BOX.getTranslationKey()) || translationKey.equals(Items.LIGHT_GRAY_SHULKER_BOX.getTranslationKey()) || translationKey.equals(Items.CYAN_SHULKER_BOX.getTranslationKey()) || translationKey.equals(Items.PURPLE_SHULKER_BOX.getTranslationKey()) || translationKey.equals(Items.BLUE_SHULKER_BOX.getTranslationKey()) || translationKey.equals(Items.BROWN_SHULKER_BOX.getTranslationKey()) || translationKey.equals(Items.GREEN_SHULKER_BOX.getTranslationKey()) || translationKey.equals(Items.RED_SHULKER_BOX.getTranslationKey()) || translationKey.equals(Items.BLACK_SHULKER_BOX.getTranslationKey());
      }
   }

   private boolean tryOpenEnderChest() {
      if (this.kit.getValue() && !this.enderChestOpening && this.getKitShulkerCount() > 0) {
         if (!this.hasEmptyPlayerSlot()) {
            return false;
         } else {
            for(BlockPos pos : BlockUtil.getSphere((float)this.range.getValue())) {
               if (mc.world.getBlockState(pos).getBlock() == Blocks.ENDER_CHEST) {
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
         if (mc.player.getInventory().getStack(i).isEmpty()) {
            return true;
         }
      }

      return false;
   }

   private int findShulkerSlot(ShulkerBoxScreenHandler shulker, String translationKey) {
      for(Slot slot : shulker.slots) {
         if (slot.id < 27) {
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty() && stack.getItem().getTranslationKey().equals(translationKey)) {
               return slot.id;
            }
         }
      }

      return -1;
   }

   private boolean hasEmptyShulkerSlot(ShulkerBoxScreenHandler shulker) {
      for(Slot slot : shulker.slots) {
         if (slot.id < 27 && slot.getStack().isEmpty()) {
            return true;
         }
      }

      return false;
   }

   private int getShulkerPlayerSlotId(int slotIndex) {
      return slotIndex < 9 ? 54 + slotIndex : 27 + (slotIndex - 9);
   }

   private boolean canOpenShulkerAfterPlace(BlockPos pos, Direction placeSide) {
      Direction openDir = BlockUtil.allowAirPlace() ? BlockUtil.getClickSide(pos) : placeSide.getOpposite();
      BlockPos front = pos.offset(openDir);
      return mc.world.getBlockState(front).getCollisionShape(mc.world, front).isEmpty();
   }

   private boolean canOpenShulker(BlockPos pos) {
      if (!(mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock)) {
         return false;
      } else {
         Direction openDir = (Direction)mc.world.getBlockState(pos).get(ShulkerBoxBlock.FACING);
         BlockPos front = pos.offset(openDir);
         return mc.world.getBlockState(front).getCollisionShape(mc.world, front).isEmpty();
      }
   }

   private void timeoutToDisable() {
      if (this.timeoutTimer.passed((long)this.disableTime.getValueInt())) {
         this.disable();
      }

   }

   private boolean needSteal(ItemStack i) {
      if (i.getItem().equals(Items.END_CRYSTAL) && this.stealCountList[0] > 0) {
         this.stealCountList[0] -= i.getCount();
         return true;
      } else if (i.getItem().equals(Items.EXPERIENCE_BOTTLE) && this.stealCountList[1] > 0) {
         this.stealCountList[1] -= i.getCount();
         return true;
      } else if (i.getItem().equals(Items.TOTEM_OF_UNDYING) && this.stealCountList[2] > 0) {
         this.stealCountList[2] -= i.getCount();
         return true;
      } else if (i.getItem().equals(Items.ENCHANTED_GOLDEN_APPLE) && this.stealCountList[3] > 0) {
         this.stealCountList[3] -= i.getCount();
         return true;
      } else if (i.getItem().equals(Blocks.OBSIDIAN.asItem()) && this.stealCountList[4] > 0) {
         this.stealCountList[4] -= i.getCount();
         return true;
      } else if (i.getItem().equals(Blocks.COBWEB.asItem()) && this.stealCountList[5] > 0) {
         this.stealCountList[5] -= i.getCount();
         return true;
      } else if (i.getItem().equals(Blocks.GLOWSTONE.asItem()) && this.stealCountList[6] > 0) {
         this.stealCountList[6] -= i.getCount();
         return true;
      } else if (i.getItem().equals(Blocks.RESPAWN_ANCHOR.asItem()) && this.stealCountList[7] > 0) {
         this.stealCountList[7] -= i.getCount();
         return true;
      } else if (i.getItem().equals(Items.ENDER_PEARL) && this.stealCountList[8] > 0) {
         this.stealCountList[8] -= i.getCount();
         return true;
      } else if (i.getItem() instanceof BlockItem && ((BlockItem)i.getItem()).getBlock() instanceof PistonBlock && this.stealCountList[9] > 0) {
         this.stealCountList[9] -= i.getCount();
         return true;
      } else if (i.getItem().equals(Blocks.REDSTONE_BLOCK.asItem()) && this.stealCountList[10] > 0) {
         this.stealCountList[10] -= i.getCount();
         return true;
      } else if (i.getItem() instanceof BlockItem && ((BlockItem)i.getItem()).getBlock() instanceof BedBlock && this.stealCountList[11] > 0) {
         this.stealCountList[11] -= i.getCount();
         return true;
      } else {
         if (i.getItem() == Items.SPLASH_POTION) {
            PotionContentsComponent potionContents = (PotionContentsComponent)i.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);

            for(StatusEffectInstance effect : potionContents.getEffects()) {
               if (effect.getEffectType() == StatusEffects.SPEED) {
                  if (this.stealCountList[12] > 0) {
                     this.stealCountList[12] -= i.getCount();
                     return true;
                  }
               } else if (effect.getEffectType() == StatusEffects.RESISTANCE) {
                  if (this.stealCountList[13] > 0) {
                     this.stealCountList[13] -= i.getCount();
                     return true;
                  }
               } else if (effect.getEffectType() == StatusEffects.STRENGTH && this.stealCountList[14] > 0) {
                  this.stealCountList[14] -= i.getCount();
                  return true;
               }
            }
         }

         return false;
      }
   }

   private void placeBlock(BlockPos pos) {
      BlockUtil.placeBlock(pos, this.rotate.getValue(), this.packet.getValue());
   }
}