package dev.g255.arc.api.utils.path;

import dev.g255.arc.api.utils.Wrapper;
import dev.g255.arc.api.utils.world.BlockPosX;
import java.util.ArrayList;
import net.minecraft.entity.LivingEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.block.PlantBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.WallSignBlock;

public class PathUtils implements Wrapper {
   public static final double range = (double)9.0F;

   private static boolean canPassThrough(BlockPos pos) {
      Block block = mc.world.getBlockState(new BlockPos(pos.getX(), pos.getY(), pos.getZ())).getBlock();
      return block == Blocks.AIR || block instanceof PlantBlock || block == Blocks.VINE || block == Blocks.LADDER || block == Blocks.WATER || block == Blocks.WATER_CAULDRON || block instanceof WallSignBlock;
   }

   public static ArrayList<Vec3> computePath(LivingEntity fromEntity, LivingEntity toEntity) {
      return computePath(new Vec3(fromEntity.getX(), fromEntity.getY(), fromEntity.getZ()), new Vec3(toEntity.getX(), toEntity.getY(), toEntity.getZ()));
   }

   public static ArrayList<Vec3> computePath(Vec3d vec3d) {
      return computePath(new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ()), new Vec3(vec3d.x, vec3d.y, vec3d.z));
   }

   public static ArrayList<Vec3> computePath(Vec3 topFrom, Vec3 to) {
      if (!canPassThrough(new BlockPosX(topFrom.mc()))) {
         topFrom = topFrom.addVector((double)0.0F, (double)1.0F, (double)0.0F);
      }

      AStarCustomPathFinder pathfinder = new AStarCustomPathFinder(topFrom, to);
      pathfinder.compute();
      int i = 0;
      Vec3 lastLoc = null;
      Vec3 lastDashLoc = null;
      ArrayList<Vec3> path = new ArrayList();
      ArrayList<Vec3> pathFinderPath = pathfinder.getPath();

      for(Vec3 pathElm : pathFinderPath) {
         if (i != 0 && i != pathFinderPath.size() - 1) {
            boolean canContinue = true;
            if (pathElm.squareDistanceTo(lastDashLoc) > (double)9.0F) {
               canContinue = false;
            } else {
               double smallX = Math.min(lastDashLoc.x(), pathElm.x());
               double smallY = Math.min(lastDashLoc.y(), pathElm.y());
               double smallZ = Math.min(lastDashLoc.z(), pathElm.z());
               double bigX = Math.max(lastDashLoc.x(), pathElm.x());
               double bigY = Math.max(lastDashLoc.y(), pathElm.y());
               double bigZ = Math.max(lastDashLoc.z(), pathElm.z());

               label65:
               for(int x = (int)smallX; (double)x <= bigX; ++x) {
                  for(int y = (int)smallY; (double)y <= bigY; ++y) {
                     for(int z = (int)smallZ; (double)z <= bigZ; ++z) {
                        if (!AStarCustomPathFinder.checkPositionValidity(x, y, z, false)) {
                           canContinue = false;
                           break label65;
                        }
                     }
                  }
               }
            }

            if (!canContinue) {
               if (!path.contains(lastLoc.addVector((double)0.5F, (double)0.0F, (double)0.5F))) {
                  path.add(lastLoc.addVector((double)0.5F, (double)0.0F, (double)0.5F));
               }

               lastDashLoc = lastLoc;
            }
         } else {
            if (lastLoc != null && !path.contains(lastLoc.addVector((double)0.5F, (double)0.0F, (double)0.5F))) {
               path.add(lastLoc.addVector((double)0.5F, (double)0.0F, (double)0.5F));
            }

            if (!path.contains(pathElm.addVector((double)0.5F, (double)0.0F, (double)0.5F))) {
               path.add(pathElm.addVector((double)0.5F, (double)0.0F, (double)0.5F));
            }

            lastDashLoc = pathElm;
         }

         lastLoc = pathElm;
         ++i;
      }

      return path;
   }
}
