package dev.g255.arc.mod.modules.impl.combat;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.ClientTickEvent;
import dev.g255.arc.api.events.impl.TotemEvent;
import dev.g255.arc.api.utils.math.ExplosionUtil;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.api.utils.player.EntityUtil;
import dev.g255.arc.api.utils.player.InventoryUtil;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.enums.Timing;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.util.Objects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.c2s.play.PickFromInventoryC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;

public class Offhand extends Module {
   private final EnumSetting<OffhandItem> item;
   private final BooleanSetting safe;
   private final SliderSetting safeHealth;
   private final BooleanSetting lethalCrystal;
   private final BooleanSetting gapSwitch;
   private final BooleanSetting always;
   private final BooleanSetting gapOnTotem;
   private final BooleanSetting gapOnSword;
   private final BooleanSetting gapOnPick;
   private final BooleanSetting mainHandTotem;
   private final SliderSetting slot;
   private final BooleanSetting forceUpdate;
   private final BooleanSetting withOffhand;
   private final EnumSetting<SwapMode> swapMode;
   private final SliderSetting delay;
   private final EnumSetting<Timing> timing;
   private final Timer timer;

   public Offhand() {
      super("Offhand", Module.Category.Combat);
      this.item = this.add(new EnumSetting("Item", Offhand.OffhandItem.Totem));
      this.safe = this.add((new BooleanSetting("Safe", true)).setParent());
      BooleanSetting var10009 = this.safe;
      Objects.requireNonNull(var10009);
      this.safeHealth = this.add(new SliderSetting("Health", (double)16.0F, (double)0.0F, (double)36.0F, 0.1, var10009::isOpen));
      BooleanSetting var10006 = this.safe;
      Objects.requireNonNull(var10006);
      this.lethalCrystal = this.add(new BooleanSetting("LethalCrystal", true, var10006::isOpen));
      this.gapSwitch = this.add((new BooleanSetting("GapSwitch", true)).setParent());
      var10006 = this.gapSwitch;
      Objects.requireNonNull(var10006);
      this.always = this.add(new BooleanSetting("Always", false, var10006::isOpen));
      var10006 = this.gapSwitch;
      Objects.requireNonNull(var10006);
      this.gapOnTotem = this.add(new BooleanSetting("Gap-Totem", false, var10006::isOpen));
      var10006 = this.gapSwitch;
      Objects.requireNonNull(var10006);
      this.gapOnSword = this.add(new BooleanSetting("Gap-Sword", true, var10006::isOpen));
      var10006 = this.gapSwitch;
      Objects.requireNonNull(var10006);
      this.gapOnPick = this.add(new BooleanSetting("Gap-Pickaxe", false, var10006::isOpen));
      this.mainHandTotem = this.add((new BooleanSetting("MainHandTotem", false)).setParent());
      var10009 = this.mainHandTotem;
      Objects.requireNonNull(var10009);
      this.slot = this.add(new SliderSetting("Slot", (double)1.0F, (double)1.0F, (double)9.0F, (double)1.0F, var10009::isOpen));
      var10006 = this.mainHandTotem;
      Objects.requireNonNull(var10006);
      this.forceUpdate = this.add(new BooleanSetting("ForceUpdate", false, var10006::isOpen));
      var10006 = this.mainHandTotem;
      Objects.requireNonNull(var10006);
      this.withOffhand = this.add(new BooleanSetting("WithOffhand", false, var10006::isOpen));
      this.swapMode = this.add(new EnumSetting("SwapMode", Offhand.SwapMode.OffhandSwap));
      this.delay = this.add(new SliderSetting("Delay", (double)50.0F, (double)0.0F, (double)500.0F, (double)1.0F));
      this.timing = this.add(new EnumSetting("Timing", Timing.All));
      this.timer = new Timer();
      this.setChinese("副手物品");
   }

   @EventListener
   public void totem(TotemEvent event) {
      if (event.getPlayer() == mc.player) {
         if (mc.player.getMainHandStack().getItem() == Items.TOTEM_OF_UNDYING) {
            mc.player.getInventory().removeStack(0);
         } else if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) {
            mc.player.getInventory().offHand.set(0, ItemStack.EMPTY);
         }
      }

   }

   private boolean lethalCrystal() {
      if (!this.lethalCrystal.getValue()) {
         return false;
      } else {
         for(Entity entity : Arc.THREAD.getEntities()) {
            if (entity instanceof EndCrystalEntity && mc.player.distanceTo(entity) <= 12.0F) {
               Vec3d vec3d = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
               if (ExplosionUtil.calculateDamage(vec3d, mc.player, mc.player, 6.0F) >= EntityUtil.getHealth(mc.player)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!nullCheck()) {
         if ((!this.timing.is(Timing.Pre) || !event.isPost()) && (!this.timing.is(Timing.Post) || !event.isPre())) {
            if (this.timer.passed((long)this.delay.getValueInt())) {
               if (EntityUtil.inInventory()) {
                  boolean switchMainHandTotem = mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING || this.withOffhand.getValue();
                  boolean unsafe = (double)EntityUtil.getHealth(mc.player) < this.safeHealth.getValue() || this.lethalCrystal();
                  int totemSlot;
                  if (this.mainHandTotem.getValue() && (totemSlot = InventoryUtil.findItemInventorySlot(Items.TOTEM_OF_UNDYING)) != -1 && mc.player.getInventory().getStack(this.slot.getValueInt() - 1).getItem() != Items.TOTEM_OF_UNDYING) {
                     switch (((SwapMode)this.swapMode.getValue()).ordinal()) {
                        case 0:
                           mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, totemSlot, 0, SlotActionType.PICKUP, mc.player);
                           mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, this.slot.getValueInt() - 1 + 36, 0, SlotActionType.PICKUP, mc.player);
                           mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, totemSlot, 0, SlotActionType.PICKUP, mc.player);
                           EntityUtil.syncInventory();
                           break;
                        case 1:
                           mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, totemSlot, this.slot.getValueInt() - 1, SlotActionType.SWAP, mc.player);
                           EntityUtil.syncInventory();
                           break;
                        case 2:
                           int old = mc.player.getInventory().selectedSlot;
                           InventoryUtil.switchToSlot(this.slot.getValueInt() - 1);
                           mc.getNetworkHandler().sendPacket(new PickFromInventoryC2SPacket(totemSlot));
                           InventoryUtil.switchToSlot(old);
                     }

                     if (switchMainHandTotem && (!this.safe.getValue() || unsafe) && (this.slot.getValueInt() - 1 != mc.player.getInventory().selectedSlot || this.forceUpdate.getValue())) {
                        InventoryUtil.switchToSlot(this.slot.getValueInt() - 1);
                     }

                     this.timer.reset();
                  } else {
                     if (!this.safe.getValue()) {
                        int hotBarSlot;
                        if (this.mainHandTotem.getValue() && switchMainHandTotem && (hotBarSlot = InventoryUtil.findItem(Items.TOTEM_OF_UNDYING)) != -1 && (hotBarSlot != mc.player.getInventory().selectedSlot || this.forceUpdate.getValue())) {
                           InventoryUtil.switchToSlot(hotBarSlot);
                        }
                     } else if (unsafe) {
                        if (!this.mainHandTotem.getValue() || !switchMainHandTotem) {
                           this.swap(Items.TOTEM_OF_UNDYING);
                           this.timer.reset();
                           return;
                        }

                        int hotBarSlot = InventoryUtil.findItem(Items.TOTEM_OF_UNDYING);
                        if (hotBarSlot != -1 && (hotBarSlot != mc.player.getInventory().selectedSlot || this.forceUpdate.getValue())) {
                           InventoryUtil.switchToSlot(hotBarSlot);
                        }
                     }

                     if ((this.gapOnSword.getValue() && mc.player.getMainHandStack().getItem() instanceof SwordItem || this.always.getValue() && mc.player.getMainHandStack().getItem() != Items.GOLDEN_APPLE && mc.player.getMainHandStack().getItem() != Items.ENCHANTED_GOLDEN_APPLE || this.gapOnPick.getValue() && mc.player.getMainHandStack().getItem() instanceof PickaxeItem || this.gapOnTotem.getValue() && mc.player.getMainHandStack().getItem() == Items.TOTEM_OF_UNDYING) && mc.options.useKey.isPressed() && this.gapSwitch.getValue()) {
                        this.swap(Items.GOLDEN_APPLE);
                        this.timer.reset();
                     } else {
                        EnumSetting<OffhandItem> item = this.item;
                        OffhandItem i = item.getValue();
                        if (i == Offhand.OffhandItem.Shield) {
                           this.swap(Items.SHIELD);
                           this.timer.reset();
                        } else if (i == Offhand.OffhandItem.Chorus) {
                           this.swap(Items.CHORUS_FRUIT);
                           this.timer.reset();
                        } else if (i == Offhand.OffhandItem.Crystal) {
                           this.swap(Items.END_CRYSTAL);
                           this.timer.reset();
                        } else if (i == Offhand.OffhandItem.Totem) {
                           this.swap(Items.TOTEM_OF_UNDYING);
                           this.timer.reset();
                        } else {
                           if (i == Offhand.OffhandItem.Gapple) {
                              this.swap(Items.GOLDEN_APPLE);
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

   private void swap(Item item) {
      int itemSlot = item == Items.GOLDEN_APPLE ? this.getGAppleSlot() : this.findItemInventorySlot(item);
      if (itemSlot != -1) {
         switch (((SwapMode)this.swapMode.getValue()).ordinal()) {
            case 0:
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, itemSlot, 0, SlotActionType.PICKUP, mc.player);
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 45, 0, SlotActionType.PICKUP, mc.player);
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, itemSlot, 0, SlotActionType.PICKUP, mc.player);
               EntityUtil.syncInventory();
               break;
            case 1:
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, itemSlot, 40, SlotActionType.SWAP, mc.player);
               EntityUtil.syncInventory();
               break;
            case 2:
               mc.getNetworkHandler().sendPacket(new PickFromInventoryC2SPacket(itemSlot));
               mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.SWAP_ITEM_WITH_OFFHAND, new BlockPos(0, 0, 0), Direction.DOWN, 0));
               mc.getNetworkHandler().sendPacket(new PickFromInventoryC2SPacket(itemSlot));
         }

      }
   }

   private int getGAppleSlot() {
      return this.findItemInventorySlot(Items.ENCHANTED_GOLDEN_APPLE) != -1 ? this.findItemInventorySlot(Items.ENCHANTED_GOLDEN_APPLE) : this.findItemInventorySlot(Items.GOLDEN_APPLE);
   }

   public String getInfo() {
      return ((OffhandItem)this.item.getValue()).name();
   }

   public int findItemInventorySlot(Item item) {
      if (mc.player.getOffHandStack().getItem() == Items.GOLDEN_APPLE && item == Items.GOLDEN_APPLE) {
         return -1;
      } else if (mc.player.getOffHandStack().getItem() != Items.ENCHANTED_GOLDEN_APPLE || item != Items.GOLDEN_APPLE && item != Items.ENCHANTED_GOLDEN_APPLE) {
         if (item == mc.player.getOffHandStack().getItem()) {
            return -1;
         } else {
            switch (((SwapMode)this.swapMode.getValue()).ordinal()) {
               case 0:
               case 1:
                  for(int i = 44; i >= 0; --i) {
                     ItemStack stack = mc.player.getInventory().getStack(i);
                     if (stack.getItem() == item) {
                        return i < 9 ? i + 36 : i;
                     }
                  }
                  break;
               case 2:
                  for(int i = 9; i < mc.player.getInventory().size() + 1; ++i) {
                     ItemStack s = mc.player.getInventory().getStack(i);
                     if (s.getItem() == item) {
                        return i;
                     }
                  }

                  for(int var6 = 0; var6 < 9; ++var6) {
                     ItemStack s = mc.player.getInventory().getStack(var6);
                     if (s.getItem() == item) {
                        return var6;
                     }
                  }
            }

            return -1;
         }
      } else {
         return -1;
      }
   }

   public static enum OffhandItem {
      None,
      Totem,
      Crystal,
      Gapple,
      Shield,
      Chorus;

      // $FF: synthetic method
      private static OffhandItem[] $values() {
         return new OffhandItem[]{None, Totem, Crystal, Gapple, Shield, Chorus};
      }
   }

   public static enum SwapMode {
      ClickSlot,
      OffhandSwap,
      Pick;

      // $FF: synthetic method
      private static SwapMode[] $values() {
         return new SwapMode[]{ClickSlot, OffhandSwap, Pick};
      }
   }
}
