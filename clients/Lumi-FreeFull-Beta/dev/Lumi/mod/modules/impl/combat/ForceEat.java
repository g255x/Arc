package dev.Lumi.mod.modules.impl.combat;

import dev.Lumi.api.events.eventbus.EventListener;
import dev.Lumi.api.events.impl.ClientTickEvent;
import dev.Lumi.api.events.impl.PacketEvent;
import dev.Lumi.api.events.impl.UpdateEvent;
import dev.Lumi.api.utils.player.InventoryUtil;
import dev.Lumi.mod.modules.Module;
import dev.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.Lumi.mod.modules.settings.impl.EnumSetting;
import net.minecraft.class_1268;
import net.minecraft.class_1713;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2596;
import net.minecraft.class_2846;
import net.minecraft.class_2886;

public class ForceEat extends Module {
   public static ForceEat INSTANCE;
   private final EnumSetting<Mode> mode;
   private final EnumSetting<HandMode> choose;
   private final BooleanSetting autoSwitch;
   private final BooleanSetting noInteract;
   private boolean active;
   private boolean eating;
   private int eatProgress;
   private int totalEatTicks;
   private int delay;
   private int prevSlot;
   private int eatingSlot;
   public static boolean switchingSlot = false;
   public static boolean movingOffhand = false;

   public ForceEat() {
      super("ForceEat", Module.Category.Combat);
      this.mode = this.add(new EnumSetting("Mode", ForceEat.Mode.Both));
      this.choose = this.add(new EnumSetting("Choose", ForceEat.HandMode.MainHand));
      this.autoSwitch = this.add(new BooleanSetting("AutoSwitch", true));
      this.noInteract = this.add(new BooleanSetting("NoInteract", true));
      this.active = false;
      this.eating = false;
      this.eatProgress = 0;
      this.totalEatTicks = 32;
      this.delay = 0;
      this.prevSlot = -1;
      this.eatingSlot = -1;
      this.setChinese("吃苹果");
      INSTANCE = this;
   }

   public boolean isEating() {
      return this.eating;
   }

   public void onEnable() {
      if (!nullCheck()) {
         this.prevSlot = mc.field_1724.method_31548().field_7545;
         int appleSlot = this.findAppleSlot();
         if (appleSlot == -1) {
            this.disable();
         } else {
            if (this.autoSwitch.getValue() && this.getHand() == class_1268.field_5808) {
               switchingSlot = true;
               InventoryUtil.switchToSlot(appleSlot);
               switchingSlot = false;
            } else if (this.getHand() == class_1268.field_5810) {
               movingOffhand = true;
               this.moveAppleToOffhand(appleSlot);
               movingOffhand = false;
            }

            this.active = false;
            this.eating = false;
            this.eatProgress = 0;
            this.delay = 1;
            this.eatingSlot = mc.field_1724.method_31548().field_7545;
            this.updateTotalEatTicks();
         }
      }
   }

   public void onDisable() {
      if (!nullCheck()) {
         mc.field_1690.field_1904.method_23481(false);
      }

      if (!nullCheck()) {
         if (this.prevSlot != -1 && this.prevSlot < 9 && this.autoSwitch.getValue() && this.getHand() == class_1268.field_5808) {
            switchingSlot = true;
            InventoryUtil.switchToSlot(this.prevSlot);
            switchingSlot = false;
         }

         this.active = false;
         this.eating = false;
         this.eatProgress = 0;
         this.delay = 0;
         this.prevSlot = -1;
         this.eatingSlot = -1;
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.isOn() && !nullCheck()) {
         if (this.delay > 0) {
            --this.delay;
         } else if (!this.active && this.isAppleInHand() && mc.field_1690.field_1904.method_1434()) {
            this.eating = true;
         } else {
            if (this.getHand() == class_1268.field_5808 && this.autoSwitch.getValue() && !this.isAppleInHand()) {
               int slot = this.findAppleSlot();
               if (slot == -1) {
                  this.disable();
                  return;
               }

               switchingSlot = true;
               InventoryUtil.switchToSlot(slot);
               switchingSlot = false;
               this.eatingSlot = slot;
               this.active = false;
               this.eating = false;
               this.eatProgress = 0;
            }

            if (this.getHand() == class_1268.field_5810 && !this.isAppleInHand()) {
               if (!this.autoSwitch.getValue()) {
                  this.disable();
                  return;
               }

               int slot = this.findAppleSlot();
               if (slot == -1) {
                  this.disable();
                  return;
               }

               movingOffhand = true;
               this.moveAppleToOffhand(slot);
               movingOffhand = false;
               this.active = false;
               this.eating = false;
               this.eatProgress = 0;
            }

            if (!this.active) {
               mc.field_1690.field_1904.method_23481(true);
               this.startEating();
               this.active = true;
               this.eating = true;
               this.eatProgress = 0;
            } else {
               mc.field_1690.field_1904.method_23481(true);
               this.eating = true;
               ++this.eatProgress;
               if (this.eatProgress >= this.totalEatTicks) {
                  this.eating = false;
                  if (!this.isAppleInHand()) {
                     if (this.autoSwitch.getValue()) {
                        if (this.getHand() == class_1268.field_5808) {
                           int nextSlot = this.findAppleSlot();
                           if (nextSlot != -1) {
                              switchingSlot = true;
                              InventoryUtil.switchToSlot(nextSlot);
                              switchingSlot = false;
                              this.eatingSlot = nextSlot;
                              this.eatProgress = 0;
                              this.startEating();
                              this.eating = true;
                           } else {
                              this.disable();
                           }
                        } else {
                           int nextSlot = this.findAppleSlot();
                           if (nextSlot != -1) {
                              movingOffhand = true;
                              this.moveAppleToOffhand(nextSlot);
                              movingOffhand = false;
                              this.eatProgress = 0;
                              this.startEating();
                              this.eating = true;
                           } else {
                              this.disable();
                           }
                        }
                     } else {
                        this.disable();
                     }
                  } else {
                     this.eatProgress = 0;
                     this.startEating();
                     this.eating = true;
                  }
               }

            }
         }
      }
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (this.isOn() && !nullCheck()) {
         if (event.isPost()) {
            boolean manualEating = !this.active && this.isAppleInHand() && mc.field_1690.field_1904.method_1434();
            boolean autoEating = this.active;
            if (!manualEating && !autoEating) {
               this.eating = false;
            } else {
               this.eating = true;
               mc.field_1690.field_1904.method_23481(true);
            }
         }
      }
   }

   @EventListener
   public void onPacketSend(PacketEvent.Send event) {
      if (this.isOn() && !nullCheck()) {
         class_2596<?> packet = event.getPacket();
         if (this.noInteract.getValue()) {
            if (packet instanceof class_2886 && ((class_2886)packet).method_12551() != this.getHand()) {
               event.cancel();
            }

            if (packet instanceof class_2846) {
               event.cancel();
            }
         }

      }
   }

   private void startEating() {
      mc.field_1752 = 0;
      sendSequencedPacket((id) -> new class_2886(this.getHand(), id, mc.field_1724.method_36454(), mc.field_1724.method_36455()));
   }

   private void moveAppleToOffhand(int appleSlot) {
      mc.field_1761.method_2906(mc.field_1724.field_7512.field_7763, appleSlot, 0, class_1713.field_7790, mc.field_1724);
      mc.field_1761.method_2906(mc.field_1724.field_7512.field_7763, 45, 0, class_1713.field_7790, mc.field_1724);
      mc.field_1761.method_2906(mc.field_1724.field_7512.field_7763, appleSlot, 0, class_1713.field_7790, mc.field_1724);
   }

   private class_1268 getHand() {
      return this.choose.getValue() == ForceEat.HandMode.OffHand ? class_1268.field_5810 : class_1268.field_5808;
   }

   private int findAppleSlot() {
      int var10000;
      switch (((Mode)this.mode.getValue()).ordinal()) {
         case 0:
            var10000 = InventoryUtil.findItem(class_1802.field_8463);
            break;
         case 1:
            var10000 = InventoryUtil.findItem(class_1802.field_8367);
            break;
         case 2:
            int slot = InventoryUtil.findItem(class_1802.field_8367);
            if (slot == -1) {
               slot = InventoryUtil.findItem(class_1802.field_8463);
            }

            var10000 = slot;
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   private boolean isAppleInHand() {
      class_1799 stack = this.choose.getValue() == ForceEat.HandMode.OffHand ? mc.field_1724.method_6079() : mc.field_1724.method_6047();
      if (stack.method_7960()) {
         return false;
      } else {
         boolean var10000;
         switch (((Mode)this.mode.getValue()).ordinal()) {
            case 0 -> var10000 = stack.method_7909() == class_1802.field_8463;
            case 1 -> var10000 = stack.method_7909() == class_1802.field_8367;
            case 2 -> var10000 = stack.method_7909() == class_1802.field_8463 || stack.method_7909() == class_1802.field_8367;
            default -> throw new MatchException((String)null, (Throwable)null);
         }

         return var10000;
      }
   }

   private void updateTotalEatTicks() {
      class_1799 stack = this.getHand() == class_1268.field_5808 ? mc.field_1724.method_6047() : mc.field_1724.method_6079();
      this.totalEatTicks = stack.method_7960() ? 32 : stack.method_7935(mc.field_1724);
   }

   public static enum HandMode {
      MainHand("MainHand"),
      OffHand("OffHand");

      private final String name;

      private HandMode(String name) {
         this.name = name;
      }

      public String toString() {
         return this.name;
      }

      // $FF: synthetic method
      private static HandMode[] $values() {
         return new HandMode[]{MainHand, OffHand};
      }
   }

   public static enum Mode {
      GoldenApple("Gapple"),
      EnchantedGoldenApple("EnchantedGapple"),
      Both("Both");

      private final String name;

      private Mode(String name) {
         this.name = name;
      }

      public String toString() {
         return this.name;
      }

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{GoldenApple, EnchantedGoldenApple, Both};
      }
   }
}
