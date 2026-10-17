package dev.luminous.mod.modules.impl.combat;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.math.PredictUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.move.MoveUtils;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.rotation.Rotation;
import dev.luminous.api.utils.rotation.RotationUtils;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.impl.player.PacketMine;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.class_1297;
import net.minecraft.class_1303;
import net.minecraft.class_1542;
import net.minecraft.class_1657;
import net.minecraft.class_1667;
import net.minecraft.class_1683;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_2533;
import net.minecraft.class_2680;
import net.minecraft.class_2868;

public class AutoTrapDoor extends Module {
   public static AutoTrapDoor INSTANCE;
   private static final long CLOSE_DELAY_MS = 75L;
   private final SliderSetting delay = this.add((new SliderSetting("Delay", 100, 0, 500)).setSuffix("ms"));
   private final SliderSetting range = this.add((new SliderSetting("Range", (double)5.0F, (double)0.0F, (double)8.0F)).setSuffix("m"));
   private final SliderSetting predictTicks = this.add(new SliderSetting("PredictTicks", (double)2.0F, (double)0.0F, (double)50.0F, (double)1.0F));
   private final SliderSetting placeRange = this.add((new SliderSetting("PlaceRange", (double)4.5F, (double)0.0F, (double)6.0F)).setSuffix("m"));
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
   private final BooleanSetting movementFix = this.add(new BooleanSetting("MovementFix", true));
   private final BooleanSetting silent = this.add(new BooleanSetting("Silent", false));
   private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
   private final BooleanSetting ghostHand = this.add(new BooleanSetting("GhostHand", true));
   private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", true));
   private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", false));
   private final Timer timer = new Timer();
   private final Timer closeTimer = new Timer();
   private boolean waitingClose;
   private class_2338 pendingClosePos;
   private class_2338 completedPos;

   public AutoTrapDoor() {
      super("AutoTrapDoor", Module.Category.Combat);
      this.setChinese("自动活板门");
      INSTANCE = this;
   }

   public void onEnable() {
      this.reset();
   }

   public void onDisable() {
      this.reset();
   }

   private void reset() {
      this.waitingClose = false;
      this.pendingClosePos = null;
      this.completedPos = null;
      this.timer.reset();
      this.closeTimer.reset();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.waitingClose) {
         if (this.pendingClosePos != null && this.closeTimer.passed(75L)) {
            if (this.toggleTrapdoor(this.pendingClosePos, false)) {
               this.completedPos = this.pendingClosePos;
               this.timer.reset();
            }

            this.pendingClosePos = null;
            this.waitingClose = false;
         }

      } else if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
         if (!this.usingPause.getValue() || !mc.field_1724.method_6115()) {
            if (this.timer.passed((long)this.delay.getValue())) {
               class_1657 target = CombatUtil.getClosestEnemy(this.range.getValue());
               if (target == null) {
                  if (this.autoDisable.getValue()) {
                     this.disable();
                  }

               } else {
                  class_243 playerPos = this.predictTicks.getValue() > (double)0.0F ? PredictUtil.getPos(target, this.predictTicks.getValueInt()) : target.method_19538();
                  class_2338 trapdoorPos = new BlockPosX(playerPos.method_10216(), playerPos.method_10214() + (double)1.0F, playerPos.method_10215());
                  if (trapdoorPos.method_10264() < 320) {
                     if (this.isTrapdoor(trapdoorPos)) {
                        class_2680 state = mc.field_1687.method_8320(trapdoorPos);
                        boolean open = Boolean.TRUE.equals(state.method_11654(class_2533.field_11631));
                        if (open) {
                           if (this.toggleTrapdoor(trapdoorPos, false)) {
                              this.completedPos = trapdoorPos;
                              this.timer.reset();
                           }

                        } else if (this.completedPos == null || !this.completedPos.equals(trapdoorPos)) {
                           if (this.toggleTrapdoor(trapdoorPos, true)) {
                              this.waitingClose = true;
                              this.pendingClosePos = trapdoorPos;
                              this.closeTimer.reset();
                              this.timer.reset();
                           }

                        }
                     } else {
                        if (this.completedPos != null && this.completedPos.equals(trapdoorPos)) {
                           this.completedPos = null;
                        }

                        int trapdoorSlot = this.getTrapdoorSlot();
                        if (trapdoorSlot != -1) {
                           if (!this.shouldSkipPlace(trapdoorPos)) {
                              int oldSlot = mc.field_1724.method_31548().field_7545;
                              this.selectSlot(trapdoorSlot, oldSlot);
                              BlockUtil.placeBlock(trapdoorPos, this.rotate.getValue());
                              this.restoreSlot(trapdoorSlot, oldSlot);
                              if (this.toggleTrapdoor(trapdoorPos, true)) {
                                 this.waitingClose = true;
                                 this.pendingClosePos = trapdoorPos;
                                 this.closeTimer.reset();
                              }

                              this.timer.reset();
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private boolean toggleTrapdoor(class_2338 trapdoorPos, boolean desiredOpen) {
      class_2680 state = mc.field_1687.method_8320(trapdoorPos);
      if (!(state.method_26204() instanceof class_2533)) {
         return false;
      } else {
         boolean open = Boolean.TRUE.equals(state.method_11654(class_2533.field_11631));
         if (open == desiredOpen) {
            return false;
         } else if (this.placeRange.getValue() > (double)0.0F && mc.field_1724.method_33571().method_1022(trapdoorPos.method_46558()) > this.placeRange.getValue()) {
            return false;
         } else {
            class_2350 clickSide = BlockUtil.getClickSide(trapdoorPos);
            if (this.movementFix.getValue() && this.rotate.getValue() && clickSide != null) {
               class_243 hitVec = trapdoorPos.method_46558().method_1031((double)clickSide.method_10163().method_10263() * (double)0.5F, (double)clickSide.method_10163().method_10264() * (double)0.5F, (double)clickSide.method_10163().method_10260() * (double)0.5F);
               Rotation rotation = RotationUtils.calculate(hitVec);
               MoveUtils.fixMovement(rotation.getYaw());
            }

            BlockUtil.clickBlock(trapdoorPos, clickSide, this.rotate.getValue());
            return true;
         }
      }
   }

   private boolean isTrapdoor(class_2338 pos) {
      return mc.field_1687.method_8320(pos).method_26204() instanceof class_2533;
   }

   private boolean shouldSkipPlace(class_2338 trapdoorPos) {
      if (!BlockUtil.canReplace(trapdoorPos)) {
         return true;
      } else {
         if (this.ghostHand.getValue()) {
            if (this.hasEntityIgnoringPlayers(trapdoorPos)) {
               return true;
            }
         } else if (BlockUtil.hasEntity(trapdoorPos, false)) {
            return true;
         }

         if (this.placeRange.getValue() > (double)0.0F && mc.field_1724.method_33571().method_1022(trapdoorPos.method_46558()) > this.placeRange.getValue()) {
            return true;
         } else if (BlockUtil.getPlaceSide(trapdoorPos, this.placeRange.getValue()) == null && !BlockUtil.allowAirPlace()) {
            return true;
         } else {
            class_2338 breakPos = PacketMine.getBreakPos();
            return PacketMine.INSTANCE.isOn() && breakPos != null && trapdoorPos.equals(breakPos);
         }
      }
   }

   private boolean hasEntityIgnoringPlayers(class_2338 pos) {
      for(class_1297 entity : BlockUtil.getEntities(new class_238(pos))) {
         if (entity != null && entity.method_5805() && !(entity instanceof class_1542) && !(entity instanceof class_1303) && !(entity instanceof class_1683) && !(entity instanceof class_1667) && !(entity instanceof class_1657)) {
            return true;
         }
      }

      return false;
   }

   private int getTrapdoorSlot() {
      return this.inventory.getValue() ? InventoryUtil.findClassInventorySlot(class_2533.class) : InventoryUtil.findClass(class_2533.class);
   }

   private void selectSlot(int slot, int selectedSlot) {
      if (this.inventory.getValue()) {
         InventoryUtil.inventorySwap(slot, selectedSlot);
      } else if (this.silent.getValue()) {
         mc.method_1562().method_52787(new class_2868(slot));
      } else {
         InventoryUtil.switchToSlot(slot);
      }
   }

   private void restoreSlot(int slot, int selectedSlot) {
      if (this.inventory.getValue()) {
         InventoryUtil.inventorySwap(slot, selectedSlot);
         EntityUtil.syncInventory();
      } else if (this.silent.getValue()) {
         mc.method_1562().method_52787(new class_2868(selectedSlot));
      } else {
         InventoryUtil.switchToSlot(selectedSlot);
      }
   }
}
