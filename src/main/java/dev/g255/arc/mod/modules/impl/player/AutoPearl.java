package dev.g255.arc.mod.modules.impl.player;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.RotationEvent;
import dev.g255.arc.api.events.impl.TickEvent;
import dev.g255.arc.api.utils.player.EntityUtil;
import dev.g255.arc.api.utils.player.InventoryUtil;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.impl.exploit.Blink;
import dev.g255.arc.mod.modules.impl.movement.ElytraFly;
import dev.g255.arc.mod.modules.impl.movement.Velocity;
import dev.g255.arc.mod.modules.settings.enums.SwingSide;
import dev.g255.arc.mod.modules.settings.enums.Timing;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.util.Objects;
import net.minecraft.util.Hand;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;

public class AutoPearl extends Module {
   public static AutoPearl INSTANCE;
   public static boolean throwing = false;
   public final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
   private final EnumSetting<Timing> timing;
   public final EnumSetting<SwingSide> interactSwing;
   private final BooleanSetting rotation;
   private final BooleanSetting yawStep;
   private final BooleanSetting whenElytra;
   private final SliderSetting steps;
   private final SliderSetting fov;
   private final SliderSetting priority;
   private final BooleanSetting sync;

   public AutoPearl() {
      super("AutoPearl", Module.Category.Player);
      this.timing = this.add(new EnumSetting("Timing", Timing.All));
      this.interactSwing = this.add(new EnumSetting("Swing", SwingSide.All));
      this.rotation = this.add(new BooleanSetting("Rotation", true));
      this.yawStep = this.add((new BooleanSetting("YawStep", false)).setParent());
      BooleanSetting var10006 = this.yawStep;
      Objects.requireNonNull(var10006);
      this.whenElytra = this.add(new BooleanSetting("FallFlying", true, var10006::isOpen));
      BooleanSetting var10009 = this.yawStep;
      Objects.requireNonNull(var10009);
      this.steps = this.add(new SliderSetting("Steps", 0.05, (double)0.0F, (double)1.0F, 0.01, var10009::isOpen));
      var10009 = this.yawStep;
      Objects.requireNonNull(var10009);
      this.fov = this.add(new SliderSetting("Fov", (double)20.0F, (double)0.0F, (double)360.0F, 0.1, var10009::isOpen));
      BooleanSetting var10008 = this.yawStep;
      Objects.requireNonNull(var10008);
      this.priority = this.add(new SliderSetting("Priority", 100, 0, 100, var10008::isOpen));
      var10006 = this.yawStep;
      Objects.requireNonNull(var10006);
      this.sync = this.add(new BooleanSetting("Sync", true, var10006::isOpen));
      this.setChinese("扔珍珠");
      INSTANCE = this;
   }

   public void onEnable() {
      if (nullCheck()) {
         this.disable();
      } else {
         if (INSTANCE.inventory.getValue()) {
            if (InventoryUtil.findItemInventorySlotFromZero(Items.ENDER_PEARL) == -1) {
               this.disable();
               return;
            }
         } else if (InventoryUtil.findItem(Items.ENDER_PEARL) == -1) {
            this.disable();
            return;
         }

         if (!this.shouldYawStep()) {
            if (!this.inventory.getValue() || EntityUtil.inInventory()) {
               if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
                  this.throwPearl(mc.player.getYaw(), mc.player.getPitch());
                  this.disable();
               }
            }
         }
      }
   }

   @EventListener
   public void onUpdate(TickEvent event) {
      if (!nullCheck()) {
         if ((!this.timing.is(Timing.Pre) || !event.isPost()) && (!this.timing.is(Timing.Post) || !event.isPre())) {
            if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
               if (!this.shouldYawStep()) {
                  this.throwPearl(mc.player.getYaw(), mc.player.getPitch());
                  this.disable();
               } else if (Arc.ROTATION.inFov(mc.player.getYaw(), mc.player.getPitch(), this.fov.getValueFloat())) {
                  if (this.sync.getValue()) {
                     this.throwPearl(mc.player.getYaw(), mc.player.getPitch());
                  } else {
                     throwing = true;
                     if (mc.player.getMainHandStack().getItem() == Items.ENDER_PEARL) {
                        sendSequencedPacket((id) -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Arc.ROTATION.getLastYaw(), Arc.ROTATION.getLastPitch()));
                        EntityUtil.swingHand(Hand.MAIN_HAND, this.interactSwing.getValue());
                     } else {
                        int pearl;
                        if (this.inventory.getValue() && (pearl = InventoryUtil.findItemInventorySlotFromZero(Items.ENDER_PEARL)) != -1) {
                           InventoryUtil.inventorySwap(pearl, mc.player.getInventory().selectedSlot);
                           sendSequencedPacket((id) -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Arc.ROTATION.getLastYaw(), Arc.ROTATION.getLastPitch()));
                           EntityUtil.swingHand(Hand.MAIN_HAND, this.interactSwing.getValue());
                           InventoryUtil.inventorySwap(pearl, mc.player.getInventory().selectedSlot);
                           EntityUtil.syncInventory();
                        } else {
                           pearl = InventoryUtil.findItem(Items.ENDER_PEARL);
                           if (pearl != -1) {
                              int old = mc.player.getInventory().selectedSlot;
                              InventoryUtil.switchToSlot(pearl);
                              sendSequencedPacket((id) -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Arc.ROTATION.getLastYaw(), Arc.ROTATION.getLastPitch()));
                              EntityUtil.swingHand(Hand.MAIN_HAND, this.interactSwing.getValue());
                              InventoryUtil.switchToSlot(old);
                           }
                        }
                     }

                     throwing = false;
                  }

                  this.disable();
               }

            }
         }
      }
   }

   @EventListener
   public void onRotate(RotationEvent event) {
      if (this.shouldYawStep()) {
         event.setRotation(mc.player.getYaw(), mc.player.getPitch(), this.steps.getValueFloat(), this.priority.getValueFloat());
      }

   }

   private boolean shouldYawStep() {
      if (this.whenElytra.getValue() || !mc.player.isFallFlying() && (!ElytraFly.INSTANCE.isOn() || !ElytraFly.INSTANCE.isFallFlying())) {
         return this.yawStep.getValue() && !Velocity.INSTANCE.noRotation();
      } else {
         return false;
      }
   }

   public void throwPearl(float yaw, float pitch) {
      throwing = true;
      if (mc.player.getMainHandStack().getItem() == Items.ENDER_PEARL) {
         if (this.rotation.getValue()) {
            Arc.ROTATION.snapAt(yaw, pitch);
         }

         sendSequencedPacket((id) -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitch));
         EntityUtil.swingHand(Hand.MAIN_HAND, this.interactSwing.getValue());
         if (this.rotation.getValue()) {
            Arc.ROTATION.snapBack();
         }
      } else {
         int pearl;
         if (this.inventory.getValue() && (pearl = InventoryUtil.findItemInventorySlotFromZero(Items.ENDER_PEARL)) != -1) {
            InventoryUtil.inventorySwap(pearl, mc.player.getInventory().selectedSlot);
            if (this.rotation.getValue()) {
               Arc.ROTATION.snapAt(yaw, pitch);
            }

            sendSequencedPacket((id) -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitch));
            EntityUtil.swingHand(Hand.MAIN_HAND, this.interactSwing.getValue());
            InventoryUtil.inventorySwap(pearl, mc.player.getInventory().selectedSlot);
            EntityUtil.syncInventory();
            if (this.rotation.getValue()) {
               Arc.ROTATION.snapBack();
            }
         } else {
            pearl = InventoryUtil.findItem(Items.ENDER_PEARL);
            if (pearl != -1) {
               int old = mc.player.getInventory().selectedSlot;
               InventoryUtil.switchToSlot(pearl);
               if (this.rotation.getValue()) {
                  Arc.ROTATION.snapAt(yaw, pitch);
               }

               sendSequencedPacket((id) -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitch));
               EntityUtil.swingHand(Hand.MAIN_HAND, this.interactSwing.getValue());
               InventoryUtil.switchToSlot(old);
               if (this.rotation.getValue()) {
                  Arc.ROTATION.snapBack();
               }
            }
         }
      }

      throwing = false;
   }
}
