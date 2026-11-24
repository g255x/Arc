package dev.g255.arc.mod.modules.impl.combat;

import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.api.utils.combat.CombatUtil;
import dev.g255.arc.api.utils.math.ExplosionUtil;
import dev.g255.arc.api.utils.player.InventoryUtil;
import dev.g255.arc.api.utils.world.BlockUtil;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class BedAura extends Module {
   private final SliderSetting range = this.add(new SliderSetting("Range", (double)5.0F, (double)0.0F, (double)6.0F, 0.1));
   private final SliderSetting minDamage = this.add(new SliderSetting("MinDamage", (double)5.0F, (double)0.0F, (double)36.0F, 0.1));
   private final SliderSetting maxSelf = this.add(new SliderSetting("MaxSelf", (double)12.0F, (double)0.0F, (double)36.0F, 0.1));
   private final BooleanSetting autoSwap = this.add(new BooleanSetting("AutoSwap", true));

   public BedAura() {
      super("BedAura", "Blows your enemies up using beds.", Module.Category.Combat);
      this.setChinese("床光环");
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!nullCheck()) {
         PlayerEntity target = CombatUtil.getClosestEnemy(this.range.getValue());
         if (target != null) {
            BlockPos bedPos = this.findBedSpot(target);
            if (bedPos != null) {
               if (this.placeBed(bedPos)) {
                  this.breakBed(bedPos);
               }

            }
         }
      }
   }

   private BlockPos findBedSpot(PlayerEntity target) {
      BlockPos feet = target.getBlockPos();
      BlockPos[] candidates = new BlockPos[]{feet, feet.up(), feet.north(), feet.east(), feet.south(), feet.west()};

      for(BlockPos pos : candidates) {
         if (mc.world.getBlockState(pos).isAir() && mc.world.getBlockState(pos.down()).isSideSolidFullSquare(mc.world, pos.down(), Direction.UP)) {
            float selfDamage = this.getDamage(pos, mc.player);
            if (!((double)selfDamage > this.maxSelf.getValue())) {
               float damage = this.getDamage(pos, target);
               if ((double)damage >= this.minDamage.getValue()) {
                  return pos;
               }
            }
         }
      }

      return null;
   }

   private float getDamage(BlockPos pos, PlayerEntity player) {
      return ExplosionUtil.calculateDamage(new Vec3d((double)pos.getX() + (double)0.5F, (double)pos.getY() + (double)0.5F, (double)pos.getZ() + (double)0.5F), player, player, 5.0F);
   }

   private boolean placeBed(BlockPos pos) {
      int bedSlot = InventoryUtil.findItem(Items.RED_BED);
      if (bedSlot == -1) {
         return false;
      } else {
         int old = mc.player.getInventory().selectedSlot;
         InventoryUtil.switchToSlot(bedSlot);
         BlockUtil.placeBlock(pos, false);
         InventoryUtil.switchToSlot(old);
         return true;
      }
   }

   private void breakBed(BlockPos pos) {
      mc.interactionManager.updateBlockBreakingProgress(pos, Direction.UP);
   }
}
