package dev.g255.arc.mod.modules.impl.combat;

import dev.g255.arc.api.utils.combat.CombatUtil;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.api.utils.world.BlockUtil;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.MinecraftClient;

public class AutoCrystalSync {
   private static BlockPos lastPlayerPos;
   private static BlockPos lastTarget;
   private static boolean moved;
   private static volatile long lastMoveTime;
   private static volatile boolean feetActive;
   private static volatile long feetFailUntil;

   public static void onCalc(BooleanSetting booleanSetting, Timer timer) {
      try {
         if (booleanSetting == null || !booleanSetting.getValue()) {
            return;
         }

         BlockPos class_23382 = MinecraftClient.getInstance().player.getBlockPos();
         if (lastPlayerPos == null || !lastPlayerPos.equals(class_23382)) {
            lastPlayerPos = class_23382;
            moved = true;
            lastMoveTime = System.currentTimeMillis();
            timer.setMs(1000000L);
         }
      } catch (Throwable var3) {
      }

   }

   public static void onCalcDone(BooleanSetting booleanSetting, BlockPos class_23382, Timer timer) {
      try {
         if (feetActive) {
            feetActive = false;
            feetFailUntil = class_23382 == null ? System.currentTimeMillis() + 5000L : 0L;
         }

         if (booleanSetting == null || !booleanSetting.getValue() || !moved) {
            return;
         }

         moved = false;
         if (class_23382 != null && (lastTarget == null || !lastTarget.equals(class_23382))) {
            timer.setMs(1000000L);
         }

         lastTarget = class_23382;
      } catch (Throwable var4) {
      }

   }

   public static boolean shouldTargetFeet() {
      return System.currentTimeMillis() - lastMoveTime < 400L;
   }

   public static boolean isFeetSuspended() {
      return System.currentTimeMillis() < feetFailUntil;
   }

   public static void markFeetActive() {
      feetActive = true;
   }

   public static ArrayList<BlockPos> getFeetCandidates(float f) {
      try {
         MinecraftClient class_3102 = MinecraftClient.getInstance();
         if (class_3102.player != null && class_3102.world != null) {
            Vec3d VanillaChestLootTableGenerator = class_3102.player.getEyePos();
            List<PlayerEntity> list = CombatUtil.getEnemies((double)24.0F);
            if (list != null && !list.isEmpty()) {
               ArrayList<BlockPos> arrayList = new ArrayList(list.size() * 10);

               for(PlayerEntity class_16572 : list) {
                  BlockPos class_23383 = class_16572.getBlockPos();
                  BlockPos class_23384 = class_23383.down();

                  for(BlockPos class_23385 : new BlockPos[]{class_23383, class_23384, class_23383.add(1, 0, 0), class_23383.add(-1, 0, 0), class_23383.add(0, 0, 1), class_23383.add(0, 0, -1), class_23384.add(1, 0, 0), class_23384.add(-1, 0, 0), class_23384.add(0, 0, 1), class_23384.add(0, 0, -1)}) {
                     Block class_22482 = BlockUtil.getBlock(class_23385.down());
                     double d;
                     double d2;
                     double d3;
                     if ((class_22482 == Blocks.OBSIDIAN || class_22482 == Blocks.BEDROCK) && BlockUtil.getBlock(class_23385) == Blocks.AIR && !BlockUtil.canCollide(class_16572, new Box(class_23385)) && !BlockUtil.canCollide(class_16572, new Box(class_23385.up())) && !((d3 = (double)class_23385.getX() + (double)0.5F - VanillaChestLootTableGenerator.x) * d3 + (d2 = (double)class_23385.getY() + (double)0.5F - VanillaChestLootTableGenerator.y) * d2 + (d = (double)class_23385.getZ() + (double)0.5F - VanillaChestLootTableGenerator.z) * d > (double)(f * f))) {
                        arrayList.add(class_23385);
                     }
                  }
               }

               if (arrayList.isEmpty()) {
                  return null;
               } else {
                  arrayList.sort(Comparator.comparingDouble((class_23382) -> VanillaChestLootTableGenerator.distanceTo(new Vec3d((double)class_23382.getX() + (double)0.5F, (double)class_23382.getY() + (double)0.5F, (double)class_23382.getZ() + (double)0.5F))));
                  return arrayList;
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      } catch (Throwable var21) {
         return null;
      }
   }
}
