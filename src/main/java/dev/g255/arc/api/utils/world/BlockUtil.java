package dev.g255.arc.api.utils.world;

import dev.g255.arc.Arc;
import dev.g255.arc.api.utils.Wrapper;
import dev.g255.arc.api.utils.combat.PacketLimiter;
import dev.g255.arc.api.utils.math.MathUtil;
import dev.g255.arc.api.utils.player.EntityUtil;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.impl.client.AntiCheat;
import dev.g255.arc.mod.modules.impl.client.ClientSetting;
import dev.g255.arc.mod.modules.impl.combat.AutoWeb;
import dev.g255.arc.mod.modules.impl.player.AirPlace;
import dev.g255.arc.mod.modules.settings.enums.Placement;
import dev.g255.arc.mod.modules.settings.enums.SwingSide;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.AbstractPressurePlateBlock;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.block.LoomBlock;
import net.minecraft.block.NoteBlock;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.block.CartographyTableBlock;
import net.minecraft.block.GrindstoneBlock;
import net.minecraft.block.StonecutterBlock;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.BlockCollisionSpliterator;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import org.jetbrains.annotations.Nullable;

public class BlockUtil implements Wrapper {
   public static final List<BlockPos> placedPos = new ArrayList();

   public static boolean canPlace(BlockPos pos) {
      return canPlace(pos, (double)1000.0F);
   }

   public static boolean canPlace(BlockPos pos, double getDistance) {
      if (getPlaceSide(pos, getDistance) == null) {
         return false;
      } else if (!canReplace(pos)) {
         return false;
      } else {
         return !hasEntity(pos, false);
      }
   }

   public static boolean canPlace(BlockPos pos, double getDistance, boolean ignoreCrystal) {
      if (getPlaceSide(pos, getDistance) == null) {
         return false;
      } else if (!canReplace(pos)) {
         return false;
      } else {
         return !hasEntity(pos, ignoreCrystal);
      }
   }

   public static boolean clientCanPlace(BlockPos pos, boolean ignoreCrystal) {
      if (!canReplace(pos)) {
         return false;
      } else {
         return !hasEntity(pos, ignoreCrystal);
      }
   }

   public static List<Entity> getEntities(Box box) {
      ArrayList<Entity> list = new ArrayList();

      for(Entity entity : Arc.THREAD.getEntities()) {
         if (entity != null && (!(entity instanceof ArmorStandEntity) || !AntiCheat.INSTANCE.ignoreArmorStand.getValue()) && entity.getBoundingBox().intersects(box)) {
            list.add(entity);
         }
      }

      return list;
   }

   public static List<EndCrystalEntity> getEndCrystals(Box box) {
      ArrayList<EndCrystalEntity> list = new ArrayList();

      for(Entity entity : Arc.THREAD.getEntities()) {
         EndCrystalEntity crystal;
         if (entity instanceof EndCrystalEntity && (crystal = (EndCrystalEntity)entity).getBoundingBox().intersects(box)) {
            list.add(crystal);
         }
      }

      return list;
   }

   public static boolean hasEntity(BlockPos pos, boolean ignoreCrystal) {
      return hasEntity(new Box(pos), ignoreCrystal);
   }

   /**
    * 这一格里有没有挡放置的实体。
    *
    * <p>原来是 {@code for (Entity entity : getEntities(box))} —— {@code getEntities} 每次都要新建一个
    * ArrayList 并把所有实体过一遍。这个方法在自动水晶的候选循环里是每格调一次（空气放置一开就是 1700+ 格/轮、
    * 20 轮/秒），光是这些临时列表和每实体一次的 {@code getBoundingBox()} 分配就足以把 GC 打满。
    * 现在直接遍历每刻缓存的实体表，不再建中间列表，判定条件与原来逐条一致。</p>
    */
   public static boolean hasEntity(Box box, boolean ignoreCrystal) {
      for (Entity entity : Arc.THREAD.getEntities()) {
         if (entity == null
            || entity instanceof ArmorStandEntity && AntiCheat.INSTANCE.ignoreArmorStand.getValue()
            || !entity.getBoundingBox().intersects(box)
            || !entity.isAlive()
            || entity instanceof ItemEntity
            || entity instanceof ExperienceOrbEntity
            || entity instanceof ExperienceBottleEntity
            || entity instanceof ArrowEntity) {
            continue;
         }

         if (!ignoreCrystal
            || !(entity instanceof EndCrystalEntity)
            || !(mc.player.getEyePos().distanceTo(MathUtil.getClosestPoint(entity)) <= AntiCheat.INSTANCE.ieRange.getValue())) {
            return true;
         }
      }

      return false;
   }

   public static boolean hasCrystal(BlockPos pos) {
      for(Entity entity : getEndCrystals(new Box(pos))) {
         if (entity.isAlive() && entity instanceof EndCrystalEntity) {
            return true;
         }
      }

      return false;
   }

   public static boolean noEntityBlockCrystal(BlockPos pos, boolean ignoreCrystal) {
      return noEntityBlockCrystal(pos, ignoreCrystal, false);
   }

   public static boolean noEntityBlockCrystal(BlockPos pos, boolean ignoreCrystal, boolean ignoreItem) {
      for(Entity entity : getEntities(new Box(pos))) {
         if (entity.isAlive() && (!ignoreItem || !(entity instanceof ItemEntity)) && (!ignoreCrystal || !(entity instanceof EndCrystalEntity) || !(mc.player.getEyePos().distanceTo(MathUtil.getClosestPoint(entity)) <= AntiCheat.INSTANCE.ieRange.getValue()))) {
            return false;
         }
      }

      return true;
   }

   public static boolean canPlaceCrystal(BlockPos pos) {
      BlockPos obsPos = pos.down();
      BlockPos boost = obsPos.up();
      return (getBlock(obsPos) == Blocks.BEDROCK || getBlock(obsPos) == Blocks.OBSIDIAN) && getClickSideStrict(obsPos) != null && mc.world.isAir(boost) && noEntityBlockCrystal(boost, false) && noEntityBlockCrystal(boost.up(), false) && (!ClientSetting.INSTANCE.lowVersion.getValue() || mc.world.isAir(boost.up()));
   }

   public static void placeCrystal(BlockPos pos, boolean rotate) {
      boolean offhand = mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL;
      BlockPos obsPos = pos.down();
      Direction facing = getClickSide(obsPos);
      Vec3d vec = obsPos.toCenterPos().add((double)facing.getVector().getX() * (double)0.5F, (double)facing.getVector().getY() * (double)0.5F, (double)facing.getVector().getZ() * (double)0.5F);
      // 朝向配额用完就这一发不发：带着旧朝向放水晶，服务端会顶回来
      if (rotate && !Arc.ROTATION.aim(vec, 0.0F)) {
         return;
      }

      clickBlock(obsPos, facing, false, offhand ? Hand.OFF_HAND : Hand.MAIN_HAND);
   }

   public static void placeBlock(BlockPos pos, boolean rotate) {
      placeBlock(pos, rotate, AntiCheat.INSTANCE.packetPlace.getValue());
   }

   public static boolean placeBlock(BlockPos pos, boolean rotate, boolean packet) {
      if (allowAirPlace()) {
         if (!airPlace(pos, rotate, Hand.MAIN_HAND, packet)) {
            return false;
         }

         placedPos.add(pos);
         return true;
      }

      Direction side = getPlaceSide(pos);
      if (side == null) {
         return false;
      }

      if (!clickBlock(pos.offset(side), side.getOpposite(), rotate, Hand.MAIN_HAND, packet)) {
         return false;
      }

      placedPos.add(pos);
      return true;
   }

   public static boolean clickBlock(BlockPos pos, Direction side, boolean rotate) {
      return clickBlock(pos, side, rotate, Hand.MAIN_HAND);
   }

   public static boolean clickBlock(BlockPos pos, Direction side, boolean rotate, Hand hand) {
      return clickBlock(pos, side, rotate, hand, AntiCheat.INSTANCE.packetPlace.getValue());
   }

   public static boolean clickBlock(BlockPos pos, Direction side, boolean rotate, boolean packet) {
      return clickBlock(pos, side, rotate, Hand.MAIN_HAND, packet);
   }

   public static boolean clickBlock(BlockPos pos, Direction side, boolean rotate, Hand hand, boolean packet) {
      Vec3d directionVec = new Vec3d((double)pos.getX() + (double)0.5F + (double)side.getVector().getX() * (double)0.5F, (double)pos.getY() + (double)0.5F + (double)side.getVector().getY() * (double)0.5F, (double)pos.getZ() + (double)0.5F + (double)side.getVector().getZ() * (double)0.5F);
      // 先转头再扣额度：朝向都不到位的话这一发包发出去也是废的
      if (rotate && !Arc.ROTATION.aim(directionVec, 0.0F)) {
         return false;
      }

      // 跨模块的每 tick 交互配额：超了就这一发不打，调用方下一 tick 自然重试
      if (!PacketLimiter.acquireAction()) {
         return false;
      }

      EntityUtil.swingHand(hand, AntiCheat.INSTANCE.interactSwing.getValue());
      BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
      if (packet) {
         Hand useHand = hand;
         Module.sendSequencedPacket((id) -> new PlayerInteractBlockC2SPacket(useHand, result, id));
      } else {
         mc.interactionManager.interactBlock(mc.player, hand, result);
      }

      mc.itemUseCooldown = 4;
      if (rotate) {
         Arc.ROTATION.snapBack();
      }

      return true;
   }

   public static boolean clickBlock(BlockPos pos, Direction side, boolean rotate, Hand hand, SwingSide swingSide) {
      Vec3d directionVec = new Vec3d((double)pos.getX() + (double)0.5F + (double)side.getVector().getX() * (double)0.5F, (double)pos.getY() + (double)0.5F + (double)side.getVector().getY() * (double)0.5F, (double)pos.getZ() + (double)0.5F + (double)side.getVector().getZ() * (double)0.5F);
      if (rotate && !Arc.ROTATION.aim(directionVec, 0.0F)) {
         return false;
      }

      if (!PacketLimiter.acquireAction()) {
         return false;
      }

      EntityUtil.swingHand(hand, swingSide);
      BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
      Module.sendSequencedPacket((id) -> new PlayerInteractBlockC2SPacket(hand, result, id));
      mc.itemUseCooldown = 4;
      if (rotate) {
         Arc.ROTATION.snapBack();
      }

      return true;
   }

   public static void airPlace(BlockPos pos, boolean rotate) {
      airPlace(pos, rotate, Hand.MAIN_HAND, AntiCheat.INSTANCE.packetPlace.getValue());
   }

   public static boolean airPlace(BlockPos pos, boolean rotate, Hand hand, boolean packet) {
      Direction side = getClickSide(pos);
      Vec3d directionVec = new Vec3d((double)pos.getX() + (double)0.5F + (double)side.getVector().getX() * (double)0.5F, (double)pos.getY() + (double)0.5F + (double)side.getVector().getY() * (double)0.5F, (double)pos.getZ() + (double)0.5F + (double)side.getVector().getZ() * (double)0.5F);
      if (rotate && !Arc.ROTATION.aim(directionVec, 0.0F)) {
         return false;
      }

      if (!PacketLimiter.acquireAction()) {
         return false;
      }

      // Grim 绕过：先把主手物品换到副手，再从副手交互。只对主手有意义，
      // 否则会在副手上再换一次，把服务端的副手槽搞乱。
      boolean bypass = hand == Hand.MAIN_HAND && AirPlace.INSTANCE != null && AirPlace.INSTANCE.grimBypass.getValue();
      if (bypass) {
         mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.SWAP_ITEM_WITH_OFFHAND, new BlockPos(0, 0, 0), Direction.DOWN));
         hand = Hand.OFF_HAND;
      }

      EntityUtil.swingHand(hand, AntiCheat.INSTANCE.interactSwing.getValue());
      BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
      if (packet) {
         Hand useHand = hand;
         Module.sendSequencedPacket((id) -> new PlayerInteractBlockC2SPacket(useHand, result, id));
      } else {
         mc.interactionManager.interactBlock(mc.player, hand, result);
      }

      mc.itemUseCooldown = 4;
      if (rotate) {
         Arc.ROTATION.snapBack();
      }

      if (bypass) {
         mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.SWAP_ITEM_WITH_OFFHAND, new BlockPos(0, 0, 0), Direction.DOWN));
      }

      return true;
   }

   public static double distanceToXZ(double x, double z, double x2, double z2) {
      double dx = x2 - x;
      double dz = z2 - z;
      return Math.sqrt(dx * dx + dz * dz);
   }

   public static double distanceToXZ(double x, double z) {
      return distanceToXZ(x, z, mc.player.getX(), mc.player.getZ());
   }

   public static Direction getPlaceSide(BlockPos pos) {
      if (allowAirPlace()) {
         return getClickSide(pos);
      } else {
         double minDistance = Double.MAX_VALUE;
         Direction side = null;

         for(Direction i : Direction.values()) {
            double vecDis;
            if (canClick(pos.offset(i)) && !canReplace(pos.offset(i)) && isStrictDirection(pos.offset(i), i.getOpposite()) && !((vecDis = mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos().add((double)i.getVector().getX() * (double)0.5F, (double)i.getVector().getY() * (double)0.5F, (double)i.getVector().getZ() * (double)0.5F))) > minDistance)) {
               side = i;
               minDistance = vecDis;
            }
         }

         return side;
      }
   }

   public static Direction getBestNeighboring(BlockPos pos, Direction facing) {
      Direction bestFacing = null;
      double getDistance = (double)0.0F;

      for(Direction i : Direction.values()) {
         if ((facing == null || !pos.offset(i).equals(pos.offset(facing, -1))) && i != Direction.DOWN && getPlaceSide(pos) != null && (bestFacing == null || mc.player.getEyePos().squaredDistanceTo(pos.offset(i).toCenterPos()) < getDistance)) {
            bestFacing = i;
            getDistance = mc.player.getEyePos().squaredDistanceTo(pos.offset(i).toCenterPos());
         }
      }

      return bestFacing;
   }

   public static Direction getPlaceSide(BlockPos pos, double reachDistance) {
      if (allowAirPlace()) {
         Direction i = getClickSide(pos);
         double vecDis = mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos().add((double)i.getVector().getX() * (double)0.5F, (double)i.getVector().getY() * (double)0.5F, (double)i.getVector().getZ() * (double)0.5F));
         return Math.sqrt(vecDis) > reachDistance ? null : Direction.DOWN;
      } else {
         double minDistance = Double.MAX_VALUE;
         Direction side = null;

         for(Direction i : Direction.values()) {
            double vecDis;
            if (canClick(pos.offset(i)) && !canReplace(pos.offset(i)) && isStrictDirection(pos.offset(i), i.getOpposite()) && !(Math.sqrt(vecDis = mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos().add((double)i.getVector().getX() * (double)0.5F, (double)i.getVector().getY() * (double)0.5F, (double)i.getVector().getZ() * (double)0.5F))) > reachDistance) && !(vecDis > minDistance)) {
               side = i;
               minDistance = vecDis;
            }
         }

         return side;
      }
   }

   public static Direction getClickSide(BlockPos pos) {
      Direction side = Direction.UP;
      if (pos == null) {
         return side;
      }

      double minDistance = Double.MAX_VALUE;

      for(Direction i : Direction.values()) {
         double disSq;
         if (isStrictDirection(pos, i) && !((disSq = mc.player.getEyePos().squaredDistanceTo(pos.offset(i).toCenterPos())) > minDistance)) {
            side = i;
            minDistance = disSq;
         }
      }

      return side;
   }

   public static Direction getClickSideStrict(BlockPos pos) {
      Direction side = null;
      if (pos == null) {
         return side;
      }

      double minDistance = Double.MAX_VALUE;

      for(Direction i : Direction.values()) {
         double disSq;
         if (isStrictDirection(pos, i) && !((disSq = mc.player.getEyePos().squaredDistanceTo(pos.offset(i).toCenterPos())) > minDistance)) {
            side = i;
            minDistance = disSq;
         }
      }

      return side;
   }

   public static boolean isStrictDirection(BlockPos pos, Direction side, double reachDistance) {
      double vecDis = mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos().add((double)side.getVector().getX() * (double)0.5F, (double)side.getVector().getY() * (double)0.5F, (double)side.getVector().getZ() * (double)0.5F));
      return Math.sqrt(vecDis) > reachDistance ? false : isStrictDirection(pos, side);
   }

   public static boolean isStrictDirection(BlockPos pos, Direction side) {
      switch ((Placement)AntiCheat.INSTANCE.placement.getValue()) {
         case Vanilla:
            return true;
         case Legit:
            return EntityUtil.canSee(pos, side);
         case Grim:
            return grimStrictDirectionCheck(pos, side, mc.world, mc.player);
         case NCP:
            if (mc.world.getBlockState(pos.offset(side)).isFullCube(mc.world, pos.offset(side))) {
               return false;
            }

            Vec3d eyePos = mc.player.getEyePos();
            Vec3d blockCenter = pos.toCenterPos();
            ArrayList<Direction> validAxis = new ArrayList();
            validAxis.addAll(checkAxis(eyePos.x - blockCenter.x, Direction.WEST, Direction.EAST, false));
            validAxis.addAll(checkAxis(eyePos.y - blockCenter.y, Direction.DOWN, Direction.UP, true));
            validAxis.addAll(checkAxis(eyePos.z - blockCenter.z, Direction.NORTH, Direction.SOUTH, false));
            return validAxis.contains(side);
         default:
            return true;
      }
   }

   public static boolean grimStrictDirectionCheck(BlockPos pos, Direction direction, ClientWorld level, ClientPlayerEntity player) {
      Box combined = getCombinedBox(pos, level);
      Box eyePositions = (new Box(player.getX(), player.getY() + 0.4, player.getZ(), player.getX(), player.getY() + 1.62, player.getZ())).expand(2.0E-4);
      if (isIntersected(eyePositions, combined)) {
         return true;
      } else {
         boolean bl;
         label50: {
            switch (direction) {
               case NORTH:
                  if (!(eyePositions.minZ > combined.minZ)) {
                     break label50;
                  }
                  break;
               case SOUTH:
                  if (!(eyePositions.maxZ < combined.maxZ)) {
                     break label50;
                  }
                  break;
               case EAST:
                  if (!(eyePositions.maxX < combined.maxX)) {
                     break label50;
                  }
                  break;
               case WEST:
                  if (!(eyePositions.minX > combined.minX)) {
                     break label50;
                  }
                  break;
               case UP:
                  if (!(eyePositions.maxY < combined.maxY)) {
                     break label50;
                  }
                  break;
               case DOWN:
                  if (!(eyePositions.minY > combined.minY)) {
                     break label50;
                  }
                  break;
               default:
                  throw new MatchException((String)null, (Throwable)null);
            }

            bl = false;
            return bl;
         }

         bl = true;
         return bl;
      }
   }

   private static Box getCombinedBox(BlockPos pos, World level) {
      VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos).offset((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
      Box combined = new Box(pos);

      for(Box box : shape.getBoundingBoxes()) {
         double minX = Math.max(box.minX, combined.minX);
         double minY = Math.max(box.minY, combined.minY);
         double minZ = Math.max(box.minZ, combined.minZ);
         double maxX = Math.min(box.maxX, combined.maxX);
         double maxY = Math.min(box.maxY, combined.maxY);
         double maxZ = Math.min(box.maxZ, combined.maxZ);
         combined = new Box(minX, minY, minZ, maxX, maxY, maxZ);
      }

      return combined;
   }

   private static boolean isIntersected(Box bb, Box other) {
      return other.maxX - 1.0E-7 > bb.minX && other.minX + 1.0E-7 < bb.maxX && other.maxY - 1.0E-7 > bb.minY && other.minY + 1.0E-7 < bb.maxY && other.maxZ - 1.0E-7 > bb.minZ && other.minZ + 1.0E-7 < bb.maxZ;
   }

   public static ArrayList<Direction> checkAxis(double diff, Direction negativeSide, Direction positiveSide, boolean vertical) {
      ArrayList<Direction> valid = new ArrayList();
      if (vertical) {
         if (diff < (double)-0.5F) {
            valid.add(negativeSide);
         }

         if (AntiCheat.INSTANCE.upDirectionLimit.getValue()) {
            if (diff > (double)0.5F) {
               valid.add(positiveSide);
            }
         } else if (diff > (double)-0.5F) {
            valid.add(positiveSide);
         }
      } else {
         if (diff < (double)-0.5F) {
            valid.add(negativeSide);
         }

         if (diff > (double)0.5F) {
            valid.add(positiveSide);
         }
      }

      return valid;
   }

   public static ArrayList<BlockEntity> getTileEntities() {
      return (ArrayList)getLoadedChunks().flatMap((chunk) -> chunk.getBlockEntities().values().stream()).collect(Collectors.toCollection(ArrayList::new));
   }

   public static Stream<WorldChunk> getLoadedChunks() {
      int radius = Math.max(2, mc.options.getClampedViewDistance()) + 3;
      int diameter = radius * 2 + 1;
      ChunkPos center = mc.player.getChunkPos();
      ChunkPos min = new ChunkPos(center.x - radius, center.z - radius);
      ChunkPos max = new ChunkPos(center.x + radius, center.z + radius);
      return Stream.iterate(min, (pos) -> {
         int x = pos.x;
         int z = pos.z;
         ++x;
         if (x > max.x) {
            x = min.x;
            ++z;
         }

         return new ChunkPos(x, z);
      }).limit((long)diameter * (long)diameter).filter((c) -> mc.world.isChunkLoaded(c.x, c.z)).map((c) -> mc.world.getChunk(c.x, c.z)).filter(Objects::nonNull);
   }

   public static ArrayList<BlockPos> getSphere(float range) {
      return getSphere(range, mc.player.getEyePos());
   }

   public static BlockPos getBlock(Block block, float range) {
      for(BlockPos pos : getSphere(range)) {
         if (mc.world.getBlockState(pos).getBlock() == block) {
            return pos;
         }
      }

      return null;
   }

   public static BlockPos getBlock(Class<?> block, float range) {
      for(BlockPos pos : getSphere(range)) {
         if (block.isInstance(mc.world.getBlockState(pos).getBlock())) {
            return pos;
         }
      }

      return null;
   }

   public static ArrayList<BlockPos> getSphere(float range, Vec3d pos) {
      ArrayList<BlockPos> list = new ArrayList();
      double rangeSq = (double)range * (double)range;

      for(double y = pos.getY() + (double)range; y > pos.getY() - (double)range; --y) {
         if (!(y < (double)-64.0F)) {
            double dy = Math.floor(y) + (double)0.5F - pos.y;

            for(double x = pos.getX() - (double)range; x < pos.getX() + (double)range; ++x) {
               double dx = Math.floor(x) + (double)0.5F - pos.x;

               for(double z = pos.getZ() - (double)range; z < pos.getZ() + (double)range; ++z) {
                  double dz = Math.floor(z) + (double)0.5F - pos.z;
                  // 平方距离代替 sqrt（等价判定），省掉每格开方和一次 Vec3d 分配
                  if (dx * dx + dy * dy + dz * dz <= rangeSq) {
                     list.add(new BlockPosX(x, y, z));
                  }
               }
            }
         }
      }

      return list;
   }

   public static Block getBlock(BlockPos pos) {
      return mc.world.getBlockState(pos).getBlock();
   }

   public static boolean canReplace(BlockPos pos) {
      if (pos.getY() >= 320) {
         return false;
      } else if (AntiCheat.INSTANCE.multiPlace.getValue() && placedPos.contains(pos)) {
         return false;
      } else {
         BlockState state = mc.world.getBlockState(pos);
         return state.isReplaceable();
      }
   }

   /**
    * 对齐 OpenHachimi 的 {@code PlacementUtils.Fs / Ft / Fu}：给一个暂时没面的目标格找一条垫脚链。
    *
    * <p>思路是从目标格往外逐层摸，先看有没有「邻居本身就能放」的（{@link #findDirectSupport}），
    * 没有就沿这个邻居再往外递归一层，最多 {@code depth} 层。返回的顺序是「最远（能自己贴墙）→ 最近（贴着目标格）」，
    * 依次放下去就能把整条路垫通。</p>
    *
    * @return 可能为空，表示这一侧垫不出来
    */
   public static List<BlockPos> supportChain(BlockPos target, double range, int depth) {
      List<BlockPos> chain = supportChain0(target, range, depth, null);
      if (chain == null || chain.isEmpty()) {
         return new ArrayList<>();
      }

      Collections.reverse(chain);
      return chain;
   }

   private static List<BlockPos> supportChain0(BlockPos pos, double range, int depth, List<BlockPos> acc) {
      if (depth <= 0) {
         return null;
      }

      List<BlockPos> list = acc == null ? new ArrayList<>() : acc;
      BlockPos direct = findDirectSupport(pos, range);
      if (direct != null) {
         list.add(direct);
         return list;
      }

      if (depth <= 1) {
         return null;
      }

      for(Direction dir : Direction.values()) {
         BlockPos next = pos.offset(dir);
         if (list.contains(next)) {
            continue;
         }

         if (!canReplace(next) || !isStrictDirection(next.offset(dir), dir.getOpposite()) || !canPlace(next, range, true)) {
            continue;
         }

         // 每个方向都用一份新的副本往下探，走死了一条路不会污染别的分支
         List<BlockPos> branch = new ArrayList<>(list);
         branch.add(next);
         if (getPlaceSide(next, range) != null) {
            return branch;
         }

         List<BlockPos> result = supportChain0(next, range, depth - 1, branch);
         if (result != null && !result.isEmpty() && getPlaceSide(result.get(result.size() - 1), range) != null) {
            return result;
         }
      }

      return null;
   }

   /** 对齐 Hachimi 的 {@code Fu}：目标格旁边本身就有一个能落子的格子 */
   private static BlockPos findDirectSupport(BlockPos pos, double range) {
      for(Direction dir : Direction.values()) {
         BlockPos side = pos.offset(dir);
         if (canReplace(side) && isStrictDirection(side.offset(dir), dir.getOpposite()) && canPlace(side, range, true) && getPlaceSide(side, range) != null) {
            return side;
         }
      }

      return null;
   }

   public static boolean canClick(BlockPos pos) {
      if (AntiCheat.INSTANCE.multiPlace.getValue() && placedPos.contains(pos)) {
         return true;
      } else {
         BlockState state = mc.world.getBlockState(pos);
         Block block = state.getBlock();
         if (block == Blocks.COBWEB && AutoWeb.ignore) {
            return false;
         } else {
            return mc.player.isSneaking() || !isClickable(block);
         }
      }
   }

   public static boolean isClickable(Block block) {
      return block instanceof CraftingTableBlock || block instanceof AnvilBlock || block instanceof LoomBlock || block instanceof CartographyTableBlock || block instanceof GrindstoneBlock || block instanceof StonecutterBlock || block instanceof ButtonBlock || block instanceof AbstractPressurePlateBlock || block instanceof BlockWithEntity || block instanceof BedBlock || block instanceof FenceGateBlock || block instanceof DoorBlock || block instanceof NoteBlock || block instanceof TrapdoorBlock;
   }

   public static boolean canCollide(Box box) {
      return canCollide(mc.player, box);
   }

   public static boolean canCollide(@Nullable Entity entity, Box box) {
      BlockCollisionSpliterator blockCollisionSpliterator = new BlockCollisionSpliterator(mc.world, entity, box, false, (pos, voxelShape) -> voxelShape);

      while(blockCollisionSpliterator.hasNext()) {
         if (!((VoxelShape)blockCollisionSpliterator.next()).isEmpty()) {
            return true;
         }
      }

      return false;
   }

   /**
    * 其它模块能不能走「空中直接放置」这条路。
    *
    * <p>必须由 AirPlace 模块授权：模块开着才算允许，否则一律回到「找相邻方块贴面」的老逻辑。
    * 这里的 INSTANCE 判空是为了防止模块还没构造完（静态初始化顺序）时被调用。
    */
   public static boolean allowAirPlace() {
      return AirPlace.INSTANCE != null && AirPlace.INSTANCE.isOn() && AirPlace.INSTANCE.module.getValue();
   }
}
