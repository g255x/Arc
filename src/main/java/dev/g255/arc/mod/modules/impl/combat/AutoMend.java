package dev.g255.arc.mod.modules.impl.combat;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.api.events.impl.UpdateRotateEvent;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.api.utils.player.EntityUtil;
import dev.g255.arc.api.utils.player.InventoryUtil;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.enums.SwingSide;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.util.Objects;
import net.minecraft.util.Hand;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.component.type.ItemEnchantmentsComponent;

public class AutoMend extends Module {
   public static AutoMend INSTANCE;
   public final BooleanSetting rotation = this.add((new BooleanSetting("Rotation", true)).setParent());
   private final BooleanSetting instant;
   public final BooleanSetting onlyBroken;
   private final SliderSetting delay;
   private final BooleanSetting inventory;
   public final EnumSetting<SwingSide> interactSwing;
   private final BooleanSetting usingPause;
   private final BooleanSetting onlyGround;
   public final BooleanSetting autoDisable;
   private final Timer delayTimer;
   boolean lookDown;
   int exp;
   private boolean throwing;

   public AutoMend() {
      super("AutoMend", Module.Category.Combat);
      BooleanSetting var10006 = this.rotation;
      Objects.requireNonNull(var10006);
      this.instant = this.add(new BooleanSetting("Instant", false, var10006::isOpen));
      this.onlyBroken = this.add(new BooleanSetting("OnlyBroken", true));
      this.delay = this.add(new SliderSetting("Delay", 3, 0, 5));
      this.inventory = this.add(new BooleanSetting("InventorySwap", true));
      this.interactSwing = this.add(new EnumSetting("InteractSwing", SwingSide.All));
      this.usingPause = this.add(new BooleanSetting("UsingPause", true));
      this.onlyGround = this.add(new BooleanSetting("OnlyGround", true));
      this.autoDisable = this.add(new BooleanSetting("AutoDisable", true));
      this.delayTimer = new Timer();
      this.lookDown = false;
      this.exp = 0;
      this.throwing = false;
      this.setChinese("自动经验瓶");
      INSTANCE = this;
   }

   public void onDisable() {
      this.throwing = false;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.throwing = this.checkThrow();
      if (!this.inventory.getValue() || EntityUtil.inInventory()) {
         if (this.lookDown && this.isThrow() && this.delayTimer.passed((long)this.delay.getValueInt() * 20L) && (!this.onlyGround.getValue() || mc.player.isOnGround())) {
            this.exp = InventoryUtil.getItemCount(Items.EXPERIENCE_BOTTLE) - 1;
            if (this.rotation.getValue() && this.instant.getValue()) {
               Arc.ROTATION.snapAt(Arc.ROTATION.rotationYaw, 88.0F);
            }

            this.throwExp();
            if (this.rotation.getValue() && this.instant.getValue()) {
               Arc.ROTATION.snapBack();
            }
         }

         if (this.autoDisable.getValue() && !this.isThrow()) {
            this.disable();
         }

      }
   }

   public void onEnable() {
      boolean bl = this.lookDown = !this.rotation.getValue() || this.instant.getValue();
      if (nullCheck()) {
         this.disable();
      } else {
         this.exp = InventoryUtil.getItemCount(Items.EXPERIENCE_BOTTLE);
      }
   }

   public String getInfo() {
      return String.valueOf(this.exp);
   }

   public void throwExp() {
      int oldSlot = mc.player.getInventory().selectedSlot;
      int newSlot;
      if (this.inventory.getValue() && (newSlot = InventoryUtil.findItemInventorySlotFromZero(Items.EXPERIENCE_BOTTLE)) != -1) {
         InventoryUtil.inventorySwap(newSlot, mc.player.getInventory().selectedSlot);
         sendSequencedPacket((id) -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Arc.ROTATION.getLastYaw(), Arc.ROTATION.getLastPitch()));
         EntityUtil.swingHand(Hand.MAIN_HAND, this.interactSwing.getValue());
         InventoryUtil.inventorySwap(newSlot, mc.player.getInventory().selectedSlot);
         EntityUtil.syncInventory();
         this.delayTimer.reset();
      } else {
         newSlot = InventoryUtil.findItem(Items.EXPERIENCE_BOTTLE);
         if (newSlot != -1) {
            InventoryUtil.switchToSlot(newSlot);
            sendSequencedPacket((id) -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Arc.ROTATION.getLastYaw(), Arc.ROTATION.getLastPitch()));
            EntityUtil.swingHand(Hand.MAIN_HAND, this.interactSwing.getValue());
            InventoryUtil.switchToSlot(oldSlot);
            this.delayTimer.reset();
         }
      }

   }

   @EventListener(
      priority = -200
   )
   public void RotateEvent(UpdateRotateEvent event) {
      if (this.rotation.getValue() && !this.instant.getValue()) {
         if (this.isThrow()) {
            event.setPitch(88.0F);
            this.lookDown = true;
         }

      }
   }

   public boolean isThrow() {
      return this.throwing;
   }

   public boolean checkThrow() {
      if (this.isOff()) {
         return false;
      } else if (mc.currentScreen != null) {
         return false;
      } else if (this.usingPause.getValue() && mc.player.isUsingItem()) {
         return false;
      } else if (InventoryUtil.findItem(Items.EXPERIENCE_BOTTLE) != -1 || this.inventory.getValue() && InventoryUtil.findItemInventorySlotFromZero(Items.EXPERIENCE_BOTTLE) != -1) {
         if (this.onlyBroken.getValue()) {
            for(ItemStack armor : mc.player.getInventory().armor) {
               if (!armor.isEmpty() && EntityUtil.getDamagePercent(armor) < 100) {
                  ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(armor);
                  return enchants.getEnchantments().contains(mc.world.getRegistryManager().get(Enchantments.MENDING.getRegistryRef()).getEntry(Enchantments.MENDING).get());
               }
            }

            return false;
         } else {
            return true;
         }
      } else {
         return false;
      }
   }
}
