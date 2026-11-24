package dev.g255.arc.mod.modules.impl.combat;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.api.utils.combat.CombatUtil;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.api.utils.player.InventoryUtil;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.impl.player.InteractTweaks;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;

public class MaceGodPlus extends Module {
   public static MaceGodPlus INSTANCE;
   private final SliderSetting attackRange = this.add(new SliderSetting("AttackRange", (double)6.0F, (double)1.0F, (double)20.0F, 0.1));
   private final SliderSetting voidY = this.add(new SliderSetting("VoidY", (double)-70.0F, (double)-500.0F, (double)320.0F, (double)1.0F));
   private final SliderSetting apt = this.add(new SliderSetting("APT", (double)1.0F, (double)1.0F, (double)20.0F, (double)1.0F));
   private final BooleanSetting autoMace = this.add(new BooleanSetting("AutoMace", true));
   private final BooleanSetting swap = this.add(new BooleanSetting("Swap", false));
   private final BooleanSetting breachSwap = this.add(new BooleanSetting("BreachSwap", false, () -> this.swap.getValue()));
   private final BooleanSetting noCrystal = this.add(new BooleanSetting("NoCrystal", true));
   private final BooleanSetting inventorySwap = this.add(new BooleanSetting("InventorySwap", false, () -> this.swap.getValue()));
   private final BooleanSetting customDelay = this.add(new BooleanSetting("CustomDelay", false));
   private final SliderSetting attackDelay = this.add(new SliderSetting("AttackDelay", (double)500.0F, (double)50.0F, (double)3000.0F, (double)50.0F, () -> this.customDelay.getValue()));
   private final BooleanSetting mobs = this.add(new BooleanSetting("Mobs", false));
   private final BooleanSetting disableInteract = this.add(new BooleanSetting("DisableInteract", true));
   private final BooleanSetting autoCrystalSync = this.add(new BooleanSetting("AutoCrystalSync", false));
   private final BooleanSetting ghostHand = this.add(new BooleanSetting("GhostHand", false, () -> this.swap.getValue()));
   private final Timer attackTimer = new Timer();
   private Entity target;
   private int prevSlot = -1;
   private boolean holdingMace;
   private boolean interactDisabledByMe;
   private boolean acSynced;

   public MaceGodPlus() {
      super("MaceGodPlus", Module.Category.Combat);
      this.setChinese("虚空锤plus");
      INSTANCE = this;
   }

   public void onEnable() {
      super.onEnable();
      this.interactDisabledByMe = false;
      this.acSynced = false;
      this.prevSlot = -1;
      this.holdingMace = false;
      this.attackTimer.reset();
      if (this.disableInteract.getValue() && InteractTweaks.INSTANCE != null && InteractTweaks.INSTANCE.isOn()) {
         InteractTweaks.INSTANCE.disable();
         this.interactDisabledByMe = true;
      }

      if (this.autoCrystalSync.getValue() && CrystalBot.INSTANCE != null && CrystalBot.INSTANCE.isOn()) {
         CrystalBot.INSTANCE.disable();
         this.acSynced = true;
      }

   }

   public void onDisable() {
      super.onDisable();
      this.restoreSlot();
      if (this.interactDisabledByMe && InteractTweaks.INSTANCE != null && !InteractTweaks.INSTANCE.isOn()) {
         InteractTweaks.INSTANCE.enable();
         this.interactDisabledByMe = false;
      }

      if (this.acSynced && CrystalBot.INSTANCE != null && !CrystalBot.INSTANCE.isOn()) {
         CrystalBot.INSTANCE.enable();
         this.acSynced = false;
      }

      this.target = null;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!nullCheck()) {
         this.target = this.findTarget();
         if (this.target == null) {
            this.holdingMace = false;
            this.restoreSlot();
         } else if (!this.customDelay.getValue() || this.attackTimer.passedMs(this.attackDelay.getValue())) {
            this.doVoidSwap();

            try {
               int attacks = this.apt.getValueInt();

               for(int i = 0; i < attacks; ++i) {
                  mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), this.voidY.getValue(), mc.player.getZ(), false));
                  mc.interactionManager.attackEntity(mc.player, this.target);
                  mc.player.swingHand(Hand.MAIN_HAND);
                  mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                  mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false));
               }
            } finally {
               this.restoreSlot();
            }

            this.attackTimer.reset();
         }
      }
   }

   private void doVoidSwap() {
      if (!this.swap.getValue()) {
         this.holdingMace = mc.player.getMainHandStack().getItem() == Items.MACE;
      } else if (mc.player.getMainHandStack().getItem() == Items.MACE && !this.ghostHand.getValue()) {
         this.holdingMace = true;
      } else if (!this.holdingMace) {
         if (this.ghostHand.getValue()) {
            int invSlot = InventoryUtil.findItemInventorySlot(Items.MACE);
            if (invSlot != -1) {
               InventoryUtil.inventorySwap(invSlot, mc.player.getInventory().selectedSlot);
               this.holdingMace = true;
            }

         } else {
            int slot = this.findMaceSlot();
            if (slot != -1) {
               this.prevSlot = mc.player.getInventory().selectedSlot;
               if (this.inventorySwap.getValue()) {
                  InventoryUtil.inventorySwap(slot + 36, mc.player.getInventory().selectedSlot);
               } else {
                  InventoryUtil.switchToSlot(slot);
               }

               this.holdingMace = true;
            }

         }
      }
   }

   private void restoreSlot() {
      if (this.prevSlot != -1 && !this.inventorySwap.getValue()) {
         InventoryUtil.switchToSlot(this.prevSlot);
      }

      this.prevSlot = -1;
      this.holdingMace = false;
   }

   private int findMaceSlot() {
      int best = -1;

      for(int i = 0; i < 9; ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() == Items.MACE) {
            if (best == -1) {
               best = i;
            }

            if (this.breachSwap.getValue() && stack.hasEnchantments()) {
               best = i;
               break;
            }
         }
      }

      return best;
   }

   private Entity findTarget() {
      Entity best = null;
      double bestDist = this.attackRange.getValue();

      for(Entity entity : Arc.THREAD.getEntities()) {
         if (entity != null && entity != mc.player && entity.isAlive() && CombatUtil.isValid(entity)) {
            if (entity instanceof EndCrystalEntity) {
               if (!this.noCrystal.getValue()) {
                  return entity;
               }
            } else {
               if (entity instanceof PlayerEntity) {
                  if (Arc.FRIEND.isFriend((PlayerEntity)entity)) {
                     continue;
                  }
               } else if (!(entity instanceof MobEntity) || !this.mobs.getValue()) {
                  continue;
               }

               double dist = (double)mc.player.distanceTo(entity);
               if (dist <= bestDist) {
                  bestDist = dist;
                  best = entity;
               }
            }
         }
      }

      return best;
   }
}
