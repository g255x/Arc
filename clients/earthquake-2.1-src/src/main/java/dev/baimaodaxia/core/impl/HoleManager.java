package dev.baimaodaxia.core.impl;

import dev.baimaodaxia.earthquake;
import dev.baimaodaxia.api.utils.Wrapper;
import dev.baimaodaxia.api.utils.world.BlockUtil;
import dev.baimaodaxia.mod.modules.impl.combat.Breaker;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

public class HoleManager implements Wrapper {
   public boolean isHole(BlockPos pos) {
      return this.isHole(pos, true, false, false);
   }

   public boolean isHole(BlockPos pos, boolean canStand, boolean checkTrap, boolean anyBlock) {
      int blockProgress = 0;

      for(Direction i : Direction.values()) {
         if (i != Direction.UP && i != Direction.DOWN && (anyBlock && !mc.world.isAir(pos.offset(i)) || earthquake.HOLE.isHard(pos.offset(i)))) {
            ++blockProgress;
         }
      }

      return (!checkTrap || mc.world.isAir(pos) && mc.world.isAir(pos.up()) && mc.world.isAir(pos.up(1)) && mc.world.isAir(pos.up(2)) && (mc.player.getBlockY() - 1 <= pos.getY() || mc.world.isAir(pos.up(3))) && (mc.player.getBlockY() - 2 <= pos.getY() || mc.world.isAir(pos.up(4)))) && blockProgress > 3 && (!canStand || BlockUtil.canCollide(new Box(pos.add(0, -1, 0))));
   }

   public BlockPos getHole(float range, boolean doubleHole, boolean any, boolean up) {
      BlockPos bestPos = null;
      double bestDistance = (double)(range + 1.0F);

      for(BlockPos pos : BlockUtil.getSphere(range, mc.player.getPos())) {
         if ((pos.getX() == mc.player.getBlockX() && pos.getZ() == mc.player.getBlockZ() || up || !((double)(pos.getY() + 1) > mc.player.getY())) && (earthquake.HOLE.isHole(pos, true, true, any) || doubleHole && this.isDoubleHole(pos)) && pos.getY() - mc.player.getBlockY() <= 1) {
            double getDistance = (double)MathHelper.sqrt((float)mc.player.squaredDistanceTo((double)pos.getX() + (double)0.5F, (double)pos.getY() + (double)0.5F, (double)pos.getZ() + (double)0.5F));
            if (bestPos == null || getDistance < bestDistance) {
               bestPos = pos;
               bestDistance = getDistance;
            }
         }
      }

      return bestPos;
   }

   public boolean isDoubleHole(BlockPos pos) {
      Direction unHardFacing = this.is3Block(pos);
      if (unHardFacing != null) {
         return this.is3Block(pos.offset(unHardFacing)) != null;
      } else {
         return false;
      }
   }

   public Direction is3Block(BlockPos pos) {
      if (!this.isHard(pos.down())) {
         return null;
      } else if (mc.world.isAir(pos) && mc.world.isAir(pos.up()) && mc.world.isAir(pos.up(2))) {
         int progress = 0;
         Direction unHardFacing = null;

         for(Direction facing : Direction.values()) {
            if (facing != Direction.UP && facing != Direction.DOWN) {
               if (this.isHard(pos.offset(facing))) {
                  ++progress;
               } else {
                  int progress2 = 0;

                  for(Direction facing2 : Direction.values()) {
                     if (facing2 != Direction.DOWN && facing2 != facing.getOpposite() && this.isHard(pos.offset(facing).offset(facing2))) {
                        ++progress2;
                     }
                  }

                  if (progress2 == 4) {
                     ++progress;
                  } else {
                     unHardFacing = facing;
                  }
               }
            }
         }

         if (progress == 3) {
            return unHardFacing;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public boolean isHard(BlockPos pos) {
      Block block = mc.world.getBlockState(pos).getBlock();
      return this.isHard(block);
   }

   public boolean isHard(Block block) {
      return block == Blocks.BEDROCK || Breaker.hard.contains(block);
   }
}
