package dev.G255.Lumi.mod.modules.impl.combat;

import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.UpdateEvent;
import dev.G255.Lumi.api.utils.combat.CombatUtil;
import dev.G255.Lumi.api.utils.math.Timer;
import dev.G255.Lumi.api.utils.move.MoveUtils;
import dev.G255.Lumi.api.utils.player.EntityUtil;
import dev.G255.Lumi.api.utils.player.InventoryUtil;
import dev.G255.Lumi.api.utils.rotation.Rotation;
import dev.G255.Lumi.api.utils.rotation.RotationUtils;
import dev.G255.Lumi.api.utils.world.BlockUtil;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.block.ButtonBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class AutoButton extends Module {
   public static AutoButton INSTANCE;
   private final SliderSetting delay = this.add((new SliderSetting("Delay", 100, 0, 500)).setSuffix("ms"));
   private final SliderSetting range = this.add((new SliderSetting("Range", (double)5.0F, (double)0.0F, (double)8.0F)).setSuffix("m"));
   private final SliderSetting placeRange = this.add((new SliderSetting("PlaceRange", (double)4.5F, (double)0.0F, (double)6.0F)).setSuffix("m"));
   private final BooleanSetting inventorySwap = this.add(new BooleanSetting("InventorySwap", true));
   private final BooleanSetting ghostHand = this.add(new BooleanSetting("GhostHand", true));
   private final BooleanSetting movementFix = this.add(new BooleanSetting("MovementFix", true));
   private final Timer timer = new Timer();

   public AutoButton() {
      super("AutoButton", Module.Category.Combat);
      this.setChinese("");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.timer.passed((long)this.delay.getValue())) {
         if (!this.inventorySwap.getValue() || EntityUtil.inInventory()) {
            PlayerEntity target = CombatUtil.getClosestEnemy(this.range.getValue());
            if (target != null) {
               BlockPos pos = EntityUtil.getEntityPos(target, true);
               if (pos.getY() < 320) {
                  if (!this.shouldSkipPlace(pos)) {
                     int buttonSlot = this.getButtonSlot();
                     if (buttonSlot != -1) {
                        int oldSlot = mc.player.getInventory().selectedSlot;
                        this.doSwap(buttonSlot);
                        if (this.movementFix.getValue()) {
                           Direction side = BlockUtil.allowAirPlace() ? BlockUtil.getClickSide(pos) : BlockUtil.getPlaceSide(pos, this.placeRange.getValue());
                           if (side != null) {
                              Vec3d hitVec = pos.toCenterPos().add((double)side.getVector().getX() * (double)0.5F, (double)side.getVector().getY() * (double)0.5F, (double)side.getVector().getZ() * (double)0.5F);
                              Rotation rotation = RotationUtils.calculate(hitVec);
                              MoveUtils.fixMovement(rotation.getYaw());
                           }
                        }

                        BlockUtil.placeBlock(pos, true);
                        if (this.inventorySwap.getValue()) {
                           this.doSwap(buttonSlot);
                           EntityUtil.syncInventory();
                        } else {
                           this.doSwap(oldSlot);
                        }

                        this.timer.reset();
                     }
                  }
               }
            }
         }
      }
   }

   private boolean shouldSkipPlace(BlockPos pos) {
      if (!BlockUtil.canReplace(pos)) {
         return true;
      } else {
         if (this.ghostHand.getValue()) {
            if (this.hasEntityIgnoringPlayers(pos)) {
               return true;
            }
         } else if (BlockUtil.hasEntity(pos, false)) {
            return true;
         }

         if (this.placeRange.getValue() > (double)0.0F && mc.player.getEyePos().distanceTo(pos.toCenterPos()) > this.placeRange.getValue()) {
            return true;
         } else {
            return BlockUtil.getPlaceSide(pos, this.placeRange.getValue()) == null && !BlockUtil.allowAirPlace();
         }
      }
   }

   private boolean hasEntityIgnoringPlayers(BlockPos pos) {
      for(Entity entity : BlockUtil.getEntities(new Box(pos))) {
         if (entity != null && entity.isAlive() && !(entity instanceof ItemEntity) && !(entity instanceof ExperienceOrbEntity) && !(entity instanceof ExperienceBottleEntity) && !(entity instanceof ArrowEntity) && !(entity instanceof PlayerEntity)) {
            return true;
         }
      }

      return false;
   }

   private void doSwap(int slot) {
      if (this.inventorySwap.getValue()) {
         InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
      } else {
         InventoryUtil.switchToSlot(slot);
      }

   }

   private int getButtonSlot() {
      return this.inventorySwap.getValue() ? InventoryUtil.findClassInventorySlot(ButtonBlock.class) : InventoryUtil.findClass(ButtonBlock.class);
   }
}
