package dev.G255.Lumi.mod.modules.impl.combat;

import dev.G255.Lumi.Lumi;
import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.UpdateEvent;
import dev.G255.Lumi.api.utils.combat.CombatUtil;
import dev.G255.Lumi.api.utils.math.MathUtil;
import dev.G255.Lumi.api.utils.math.Timer;
import dev.G255.Lumi.api.utils.player.EntityUtil;
import dev.G255.Lumi.api.utils.player.InventoryUtil;
import dev.G255.Lumi.api.utils.world.BlockPosX;
import dev.G255.Lumi.api.utils.world.BlockUtil;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.impl.client.AntiCheat;
import dev.G255.Lumi.mod.modules.impl.exploit.Blink;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import java.util.Objects;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.item.Items;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.ScaffoldingBlock;
import net.minecraft.block.AbstractChestBlock;

public class AntiPhase extends Module {
   private final SliderSetting placeRange = this.add(new SliderSetting("PlaceRange", 4, 0, 8));
   private final BooleanSetting ladder = this.add((new BooleanSetting("Ladder", true)).setParent());
   private final BooleanSetting onlyHard;
   private final BooleanSetting itemFrame;
   private final BooleanSetting fill;
   private final BooleanSetting scaffolding;
   private final BooleanSetting rotate;
   private final BooleanSetting eatingPause;
   private final BooleanSetting collideSkip;
   private final BooleanSetting crawlingSkip;
   private final BooleanSetting onlyGround;
   private final SliderSetting targetRange;
   private final SliderSetting delay;
   private final BooleanSetting inventory;
   private final Timer timer;

   public AntiPhase() {
      super("AntiPhase", Module.Category.Combat);
      BooleanSetting var10006 = this.ladder;
      Objects.requireNonNull(var10006);
      this.onlyHard = this.add(new BooleanSetting("OnlyHard", true, var10006::isOpen));
      this.itemFrame = this.add((new BooleanSetting("ItemFrame", true)).setParent());
      var10006 = this.itemFrame;
      Objects.requireNonNull(var10006);
      this.fill = this.add(new BooleanSetting("Fill", false, var10006::isOpen));
      this.scaffolding = this.add(new BooleanSetting("Scaffolding", true));
      this.rotate = this.add(new BooleanSetting("Rotate", true));
      this.eatingPause = this.add(new BooleanSetting("EatingPause", true));
      this.collideSkip = this.add(new BooleanSetting("CollideSkip", true));
      this.crawlingSkip = this.add(new BooleanSetting("CrawlingSkip", true));
      this.onlyGround = this.add(new BooleanSetting("InAirSkip", false));
      this.targetRange = this.add(new SliderSetting("TargetRange", (double)5.0F, (double)0.0F, (double)7.0F, 0.1));
      this.delay = this.add(new SliderSetting("Delay", (double)100.0F, (double)0.0F, (double)2000.0F, (double)1.0F));
      this.inventory = this.add(new BooleanSetting("InventorySwap", true));
      this.timer = new Timer();
      this.setChinese("");
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!this.eatingPause.getValue() || !mc.player.isUsingItem()) {
         if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
            for(Entity entity : CombatUtil.getEnemies(this.targetRange.getValue())) {
               if ((!this.crawlingSkip.getValue() || !entity.isCrawling()) && (!this.onlyGround.getValue() || entity.isOnGround()) && (!this.collideSkip.getValue() || !BlockUtil.canCollide(entity, entity.getBoundingBox())) && this.timer.passed((long)this.delay.getValueInt()) && !this.shouldSkipContainer(entity)) {
                  int block;
                  if (this.scaffolding.getValue() && BlockUtil.canReplace(entity.getBlockPos()) && (block = this.getScaffolding()) != -1) {
                     BlockPos bp = entity.getBlockPos();
                     Direction downSide = null;
                     Direction placeSide = BlockUtil.getPlaceSide(bp, (double)6.0F);
                     if (placeSide != null || (downSide = this.getSideIgnore(bp.down())) != null && BlockUtil.getBlock(bp.down()) instanceof ScaffoldingBlock && !mc.player.isSneaking() || mc.player.isSneaking() && (downSide = this.getSideOnly(bp.down())) != null && BlockUtil.getBlock(bp.down()) instanceof ScaffoldingBlock) {
                        Vec3d targetPos = placeSide != null ? bp.offset(placeSide).toCenterPos().add((double)placeSide.getOpposite().getVector().getX() * (double)0.5F, (double)placeSide.getOpposite().getVector().getY() * (double)0.5F, (double)placeSide.getOpposite().getVector().getZ() * (double)0.5F) : bp.down().toCenterPos().add((double)downSide.getVector().getX() * (double)0.5F, (double)downSide.getVector().getY() * (double)0.5F, (double)downSide.getVector().getZ() * (double)0.5F);
                        double getDistance = mc.player.getEyePos().distanceTo(targetPos);
                        if (getDistance <= this.placeRange.getValue()) {
                           int old = mc.player.getInventory().selectedSlot;
                           this.doSwap(block);
                           if (BlockUtil.getBlock(bp.down()) instanceof ScaffoldingBlock && downSide != null) {
                              BlockUtil.clickBlock(bp.down(), downSide, this.rotate.getValue());
                           } else {
                              BlockUtil.placeBlock(bp, this.rotate.getValue());
                           }

                           this.timer.reset();
                           if (this.inventory.getValue()) {
                              this.doSwap(block);
                              EntityUtil.syncInventory();
                           } else {
                              this.doSwap(old);
                           }
                        }
                     }
                  }

                  if (this.itemFrame.getValue() && mc.world.isAir(entity.getBlockPos())) {
                     ItemFrameEntity itemFrameEntity = this.hasItemFrame(new Box(entity.getBlockPos()));
                     int block2;
                     if (itemFrameEntity == null && (block2 = this.getItemFrame()) != -1) {
                        BlockPos bp = entity.getBlockPos().down();
                        double getDistance = mc.player.getEyePos().distanceTo(bp.toBottomCenterPos().add((double)0.0F, (double)1.0F, (double)0.0F));
                        if (getDistance <= this.placeRange.getValue() && BlockUtil.isStrictDirection(bp, Direction.UP) && !BlockUtil.canReplace(bp) && BlockUtil.canClick(bp)) {
                           int old = mc.player.getInventory().selectedSlot;
                           this.doSwap(block2);
                           BlockUtil.clickBlock(bp, Direction.UP, this.rotate.getValue());
                           this.timer.reset();
                           if (this.inventory.getValue()) {
                              this.doSwap(block2);
                              EntityUtil.syncInventory();
                           } else {
                              this.doSwap(old);
                           }
                        }
                     }

                     if (this.fill.getValue() && itemFrameEntity != null && itemFrameEntity.getHeldItemStack().isEmpty()) {
                        Vec3d hitVec = MathUtil.getClosestPointToBox(mc.player.getEyePos(), itemFrameEntity.getBoundingBox());
                        int block3;
                        if (mc.player.getEyePos().distanceTo(hitVec) <= AntiCheat.INSTANCE.ieRange.getValue() && (block3 = this.getObsidian()) != -1) {
                           int old = mc.player.getInventory().selectedSlot;
                           this.doSwap(block3);
                           if (this.rotate.getValue()) {
                              Lumi.ROTATION.snapAt(hitVec);
                           }

                           mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.interact(itemFrameEntity, mc.player.isSneaking(), Hand.MAIN_HAND));
                           this.timer.reset();
                           if (this.inventory.getValue()) {
                              this.doSwap(block3);
                              EntityUtil.syncInventory();
                           } else {
                              this.doSwap(old);
                           }

                           if (this.rotate.getValue()) {
                              Lumi.ROTATION.snapBack();
                           }
                        }
                     }
                  }

                  Direction facing;
                  if (this.ladder.getValue() && (block = this.getLadder()) != -1 && BlockUtil.canReplace(entity.getBlockPos()) && (facing = this.targetFacing(entity.getPos())) != null) {
                     BlockPos bp = entity.getBlockPos().offset(facing);
                     double getDistance = mc.player.getEyePos().distanceTo(bp.toCenterPos().add((double)facing.getOpposite().getVector().getX() * (double)0.5F, (double)facing.getOpposite().getVector().getY() * (double)0.5F, (double)facing.getOpposite().getVector().getZ() * (double)0.5F));
                     if (getDistance <= this.placeRange.getValue()) {
                        BlockUtil.placedPos.add(entity.getBlockPos());
                        int old = mc.player.getInventory().selectedSlot;
                        this.doSwap(block);
                        BlockUtil.clickBlock(bp, facing.getOpposite(), this.rotate.getValue());
                        this.timer.reset();
                        if (this.inventory.getValue()) {
                           this.doSwap(block);
                           EntityUtil.syncInventory();
                        } else {
                           this.doSwap(old);
                        }
                     }
                  }
               }
            }

         }
      }
   }

   private Direction getSideOnly(BlockPos pos) {
      return BlockUtil.isStrictDirection(pos, Direction.UP) ? Direction.UP : null;
   }

   private Direction getSideIgnore(BlockPos pos) {
      for(Direction i : Direction.values()) {
         if (i != Direction.UP && BlockUtil.isStrictDirection(pos, i)) {
            return i;
         }
      }

      return null;
   }

   private ItemFrameEntity hasItemFrame(Box box) {
      for(Entity entity : BlockUtil.getEntities(box)) {
         if (entity instanceof ItemFrameEntity itemFrameEntity) {
            if (entity.getFacing() == Direction.UP) {
               return itemFrameEntity;
            }
         }
      }

      return null;
   }

   private static Box getBox(Direction facing, BlockPos bp) {
      Box box = null;
      double wide = (double)0.1875F;
      double x = (double)facing.getOffsetX() * (double)0.5F + (double)bp.getX() + (double)0.5F;
      double y = (double)bp.getY();
      double z = (double)facing.getOffsetZ() * (double)0.5F + (double)bp.getZ() + (double)0.5F;
      switch (facing) {
         case WEST -> box = new Box(x, y, z, x + wide, y + (double)1.0F, z + (double)1.0F);
         case EAST -> box = new Box(x, y, z, x - wide, y + (double)1.0F, z + (double)1.0F);
         case NORTH -> box = new Box(x, y, z, x + (double)1.0F, y + (double)1.0F, z + wide);
         case SOUTH -> box = new Box(x, y, z, x + (double)1.0F, y + (double)1.0F, z - wide);
      }

      return box;
   }

   private void doSwap(int slot) {
      if (this.inventory.getValue()) {
         InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
      } else {
         InventoryUtil.switchToSlot(slot);
      }

   }

   private int getFlintAndSteel() {
      return this.inventory.getValue() ? InventoryUtil.findItemInventorySlot(Items.FLINT_AND_STEEL) : InventoryUtil.findItem(Items.FLINT_AND_STEEL);
   }

   private int getObsidian() {
      return this.inventory.getValue() ? InventoryUtil.findItemInventorySlot(Items.OBSIDIAN) : InventoryUtil.findItem(Items.OBSIDIAN);
   }

   private int getItemFrame() {
      return this.inventory.getValue() ? InventoryUtil.findItemInventorySlot(Items.ITEM_FRAME) : InventoryUtil.findItem(Items.ITEM_FRAME);
   }

   private int getLadder() {
      return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.LADDER) : InventoryUtil.findBlock(Blocks.LADDER);
   }

   private int getScaffolding() {
      return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.SCAFFOLDING) : InventoryUtil.findBlock(Blocks.SCAFFOLDING);
   }

   private Direction targetFacing(Vec3d vec3d) {
      BlockPosX blockPos = new BlockPosX(vec3d);
      Vec3d centerPos = blockPos.toBottomCenterPos();
      float factorValue = 0.4F;
      double minDistance = Double.MAX_VALUE;
      Direction facing = null;

      for(Direction direction : Direction.values()) {
         BlockPos bp;
         if (direction != Direction.UP && direction != Direction.DOWN && BlockUtil.isStrictDirection(bp = blockPos.offset(direction), direction.getOpposite())) {
            if (!this.onlyHard.getValue()) {
               if (BlockUtil.canReplace(bp) || !BlockUtil.canClick(bp)) {
                  continue;
               }
            } else if (!Lumi.HOLE.isHard(bp)) {
               continue;
            }

            Box box = getBox(direction, blockPos);
            double getDistance;
            if (box != null && !hasEntity(box) && (getDistance = centerPos.add((double)((float)direction.getOffsetX() * factorValue), (double)0.0F, (double)((float)direction.getOffsetZ() * factorValue)).distanceTo(vec3d)) < minDistance) {
               minDistance = getDistance;
               facing = direction;
            }
         }
      }

      return facing;
   }

   public static boolean hasEntity(Box box) {
      for(Entity entity : BlockUtil.getEntities(box)) {
         if (entity.isAlive() && !(entity instanceof ItemEntity) && !(entity instanceof ExperienceOrbEntity) && !(entity instanceof ExperienceBottleEntity) && !(entity instanceof ArrowEntity) && !(entity instanceof ItemFrameEntity)) {
            return true;
         }
      }

      return false;
   }

   private boolean shouldSkipContainer(Entity entity) {
      Box box = entity.getBoundingBox();
      int minX = (int)Math.floor(box.minX + 0.001);
      int minY = (int)Math.floor(box.minY + 0.001);
      int minZ = (int)Math.floor(box.minZ + 0.001);
      int maxX = (int)Math.floor(box.maxX - 0.001);
      int maxY = (int)Math.floor(box.maxY - 0.001);
      int maxZ = (int)Math.floor(box.maxZ - 0.001);
      boolean isShulker = false;
      boolean isOtherContainer = false;

      for(int x = minX; x <= maxX; ++x) {
         for(int y = minY; y <= maxY; ++y) {
            for(int z = minZ; z <= maxZ; ++z) {
               BlockPos pos = new BlockPos(x, y, z);
               Block block = BlockUtil.getBlock(pos);
               if (block instanceof ShulkerBoxBlock) {
                  isShulker = true;
               } else if (block instanceof ChestBlock || block instanceof EnderChestBlock || block instanceof BarrelBlock || block instanceof AbstractChestBlock) {
                  isOtherContainer = true;
               }
            }
         }
      }

      return isOtherContainer && !isShulker;
   }
}
