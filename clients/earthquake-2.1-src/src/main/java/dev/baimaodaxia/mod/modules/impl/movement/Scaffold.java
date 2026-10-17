package dev.baimaodaxia.mod.modules.impl.movement;

import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.ClientTickEvent;
import dev.baimaodaxia.api.events.impl.KeyboardInputEvent;
import dev.baimaodaxia.api.events.impl.SendMovementPacketsEvent;
import dev.baimaodaxia.api.events.impl.SprintEvent;
import dev.baimaodaxia.api.utils.move.MoveUtils;
import dev.baimaodaxia.api.utils.rotation.RayCastUtil;
import dev.baimaodaxia.api.utils.rotation.Rotation;
import dev.baimaodaxia.api.utils.rotation.RotationManager;
import dev.baimaodaxia.api.utils.rotation.RotationUtils;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.CropBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.block.FlowerBlock;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.block.FungusBlock;

public class Scaffold extends Module {
   public static final List<Block> blacklistedBlocks;
   private final BooleanSetting telly = this.add(new BooleanSetting("Telly", true));
   private final BooleanSetting snap = this.add(new BooleanSetting("Snap", false, () -> !this.telly.getValue()));
   private final SliderSetting rotateSpeed = this.add(new SliderSetting("Rotation Speed", (double)180.0F, (double)1.0F, (double)180.0F, (double)1.0F));
   private final SliderSetting rotateBackSpeed;
   private final SliderSetting tellyTick;
   private final BooleanSetting safeWalk;
   private int airTick;
   private int yLevel;
   private BlockPos blockPos;
   private Direction enumFacing;
   private int oldSlot;

   public Scaffold() {
      super("Scaffold", "Automatically places blocks under you", Module.Category.Movement);
      BooleanSetting var10009 = this.telly;
      Objects.requireNonNull(var10009);
      this.rotateBackSpeed = this.add(new SliderSetting("Rotation Back Speed", (double)180.0F, (double)1.0F, (double)180.0F, (double)1.0F, var10009::getValue));
      var10009 = this.telly;
      Objects.requireNonNull(var10009);
      this.tellyTick = this.add(new SliderSetting("Telly Ticks", (double)1.0F, (double)0.0F, (double)6.0F, (double)1.0F, var10009::getValue));
      this.safeWalk = this.add(new BooleanSetting("SafeWalk", true));
      this.oldSlot = -1;
      this.setChinese("自动搭路");
   }

   public String getInfo() {
      if (this.telly.getValue()) {
         return "Telly";
      } else {
         return this.snap.getValue() ? "Snap" : "Normal";
      }
   }

   public static Vec3d getVec3(BlockPos pos, Direction face) {
      double x = (double)pos.getX() + (double)0.5F;
      double y = (double)pos.getY() + (double)0.5F;
      double z = (double)pos.getZ() + (double)0.5F;
      if (face != Direction.UP && face != Direction.DOWN) {
         y += 0.08;
      } else {
         x += getRandomDoubleInRange(0.3, -0.3);
         z += getRandomDoubleInRange(0.3, -0.3);
      }

      if (face == Direction.WEST || face == Direction.EAST) {
         z += getRandomDoubleInRange(0.3, -0.3);
      }

      if (face == Direction.SOUTH || face == Direction.NORTH) {
         x += getRandomDoubleInRange(0.3, -0.3);
      }

      return new Vec3d(x, y, z);
   }

   private static double getRandomDoubleInRange(double min, double max) {
      return min + (max - min) * ThreadLocalRandom.current().nextDouble();
   }

   public static boolean isValidStack(ItemStack stack) {
      if (stack != null && stack.getItem() instanceof BlockItem && stack.getCount() > 0) {
         String string = stack.getName().getString();
         if (!string.contains("Click") && !string.contains("点击")) {
            Block block = ((BlockItem)stack.getItem()).getBlock();
            if (block instanceof FlowerBlock) {
               return false;
            } else if (block instanceof PlantBlock) {
               return false;
            } else if (block instanceof FungusBlock) {
               return false;
            } else if (block instanceof CropBlock) {
               return false;
            } else {
               return !(block instanceof SlabBlock) && !blacklistedBlocks.contains(block);
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (mc.player != null && mc.world != null && event.isPre()) {
         int slotID = -1;

         for(int i = 0; i < 9; ++i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isValidStack(stack)) {
               slotID = i;
               break;
            }
         }

         if (slotID != -1 && mc.player.getInventory().selectedSlot != slotID) {
            mc.player.getInventory().selectedSlot = slotID;
         }

         if (mc.player.isOnGround()) {
            this.yLevel = (int)Math.floor(mc.player.getY()) - 1;
         }

         this.getBlockInfo();
         if (this.telly.getValue()) {
            if (mc.player.isOnGround()) {
               this.airTick = 0;
               this.blockPos = null;
               this.enumFacing = null;
               Rotation rotation = new Rotation(mc.player.getYaw(), mc.player.getPitch());
               RotationManager.setRotations(rotation, this.rotateBackSpeed.getValue());
            } else {
               if ((double)this.airTick >= this.tellyTick.getValue()) {
                  Rotation rotation = this.getRotation(this.blockPos, this.enumFacing);
                  RotationManager.setRotations(rotation, this.rotateSpeed.getValue());
                  this.place();
               }

               ++this.airTick;
            }
         } else {
            if (this.blockPos == null) {
               RotationManager.setRotations(new Rotation(MathHelper.wrapDegrees(mc.player.getYaw() - 180.0F), 89.64F), this.rotateSpeed.getValue());
            } else if (this.onAir() || !this.snap.getValue()) {
               Rotation rotation = this.getRotation(this.blockPos, this.enumFacing);
               RotationManager.setRotations(rotation, this.rotateSpeed.getValue());
            }

            this.place();
         }

      }
   }

   @EventListener(
      priority = -200
   )
   public void onKeyboardInput(KeyboardInputEvent event) {
      if (mc.player != null && mc.world != null) {
         if (this.telly.getValue() && MoveUtils.isMoving() && !mc.player.isSneaking()) {
            mc.player.setSprinting(true);
         }

         if (mc.player.isOnGround() && !mc.options.jumpKey.isPressed() && MoveUtils.isMoving() && this.telly.getValue()) {
            mc.player.input.jumping = true;
         }

      }
   }

   @EventListener(
      priority = -100
   )
   public void onSprint(SprintEvent event) {
      if (this.telly.getValue() && MoveUtils.isMoving() && !mc.player.isSneaking() && !mc.player.isUsingItem()) {
         event.setSprint(true);
         event.cancel();
      }

   }

   public void place() {
      if (this.onAir() && this.blockPos != null && this.enumFacing != null) {
         boolean hasRotated = RayCastUtil.overBlock(RotationManager.getRotation(), this.enumFacing, this.blockPos, false);
         if (hasRotated && mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(getVec3(this.blockPos, this.enumFacing), this.enumFacing, this.blockPos, false)) == ActionResult.SUCCESS) {
            mc.player.swingHand(Hand.MAIN_HAND);
         }

      }
   }

   public int getYLevel() {
      return !mc.options.jumpKey.isPressed() && MoveUtils.isMoving() && (double)mc.player.fallDistance <= (double)0.25F && this.telly.getValue() ? this.yLevel : (int)Math.floor(mc.player.getY()) - 1;
   }

   public void getBlockInfo() {
      Vec3d baseVec = mc.player.getEyePos();
      BlockPos base = new BlockPos(MathHelper.floor(baseVec.x), this.getYLevel(), MathHelper.floor(baseVec.z));
      int baseX = base.getX();
      int baseZ = base.getZ();
      if (!this.isSolidAndNonInteractive(mc.world.getBlockState(base), mc.world, base)) {
         if (!this.checkBlock(baseVec, base)) {
            for(int d = 1; d <= 6; ++d) {
               if (this.checkBlock(baseVec, new BlockPos(baseX, this.getYLevel() - d, baseZ))) {
                  return;
               }

               for(int x = 0; x <= d; ++x) {
                  for(int z = 0; z <= d - x; ++z) {
                     int y = d - x - z;

                     for(int rev1 = 0; rev1 <= 1; ++rev1) {
                        for(int rev2 = 0; rev2 <= 1; ++rev2) {
                           if (this.checkBlock(baseVec, new BlockPos(baseX + (rev1 == 0 ? x : -x), this.getYLevel() - y, baseZ + (rev2 == 0 ? z : -z)))) {
                              return;
                           }
                        }
                     }
                  }
               }
            }

         }
      }
   }

   public boolean isSolidAndNonInteractive(BlockState state, World level, BlockPos pos) {
      boolean hasCollision = !state.getCollisionShape(level, pos).isEmpty();
      boolean hasNoMenu = state.createScreenHandlerFactory(level, pos) == null;
      return hasCollision && hasNoMenu;
   }

   private boolean checkBlock(Vec3d baseVec, BlockPos pos) {
      BlockState state = mc.world.getBlockState(pos);
      if (!state.isReplaceable()) {
         return false;
      } else if (pos.getY() > this.getYLevel()) {
         return false;
      } else {
         Vec3d center = new Vec3d((double)pos.getX() + (double)0.5F, (double)pos.getY() + (double)0.5F, (double)pos.getZ() + (double)0.5F);

         for(Direction dir : Direction.values()) {
            Vec3d hit = center.add((double)dir.getOffsetX() * (double)0.5F, (double)dir.getOffsetY() * (double)0.5F, (double)dir.getOffsetZ() * (double)0.5F);
            BlockPos baseBlockPos = pos.offset(dir);
            if (this.isSolidAndNonInteractive(mc.world.getBlockState(baseBlockPos), mc.world, baseBlockPos)) {
               Vec3d relevant = hit.subtract(baseVec);
               if (relevant.lengthSquared() <= (double)20.25F && relevant.dotProduct(new Vec3d((double)dir.getOffsetX(), (double)dir.getOffsetY(), (double)dir.getOffsetZ())) >= (double)0.0F && (dir.getOpposite() != Direction.UP || !MoveUtils.isMoving() || mc.options.jumpKey.isPressed())) {
                  this.blockPos = baseBlockPos;
                  this.enumFacing = dir.getOpposite();
                  return true;
               }
            }
         }

         return false;
      }
   }

   @EventListener
   public void onPacket(SendMovementPacketsEvent e) {
      if (mc.player != null && mc.world != null) {
         if (this.safeWalk.getValue() && !this.telly.getValue()) {
            boolean isHoldingShift = InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.sneakKey.getDefaultKey().getCode());
            boolean shouldSafewalk = mc.player.isOnGround() && this.isOnBlockEdge(0.3);
            mc.options.sneakKey.setPressed(isHoldingShift || shouldSafewalk);
         }

      }
   }

   private boolean isOnBlockEdge(double sensitivity) {
      return !mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().offset((double)0.0F, (double)-0.5F, (double)0.0F).expand(-sensitivity, (double)0.0F, -sensitivity)).iterator().hasNext();
   }

   public void onEnable() {
      RotationManager.init();
      if (mc.player != null) {
         this.oldSlot = mc.player.getInventory().selectedSlot;
      }

      this.airTick = 0;
      this.blockPos = null;
      this.enumFacing = null;
   }

   public void onDisable() {
      boolean isHoldingShift = InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.sneakKey.getDefaultKey().getCode());
      mc.options.sneakKey.setPressed(isHoldingShift);
      if (mc.player != null && this.oldSlot != -1) {
         mc.player.getInventory().selectedSlot = this.oldSlot;
      }

   }

   public Rotation getRotation(BlockPos pos, Direction direction) {
      if (pos == null) {
         return new Rotation(mc.player.getYaw(), mc.player.getPitch());
      } else {
         Rotation rotations = this.onAir() ? RotationUtils.calculate(pos, direction) : RotationUtils.calculate(pos.toCenterPos());
         Rotation reverseYaw = new Rotation(MathHelper.wrapDegrees(mc.player.getYaw() - 180.0F), rotations.getPitch());
         Direction rayFacing = direction == null ? Direction.UP : direction;
         boolean hasRotated = RayCastUtil.overBlock(reverseYaw, rayFacing, pos, false);
         return hasRotated ? reverseYaw : rotations;
      }
   }

   private boolean onAir() {
      Vec3d baseVec = mc.player.getEyePos();
      BlockPos base = new BlockPos(MathHelper.floor(baseVec.x), this.getYLevel(), MathHelper.floor(baseVec.z));
      return mc.world.getBlockState(base).isReplaceable();
   }

   public String getArrayInfo() {
      return this.telly.getValue() ? "Telly" : (this.snap.getValue() ? "Snap" : "Normal");
   }

   static {
      blacklistedBlocks = Arrays.asList(Blocks.AIR, Blocks.WATER, Blocks.LAVA, Blocks.ENCHANTING_TABLE, Blocks.GLASS_PANE, Blocks.GLASS_PANE, Blocks.IRON_BARS, Blocks.SNOW, Blocks.COAL_ORE, Blocks.DIAMOND_ORE, Blocks.EMERALD_ORE, Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.TORCH, Blocks.ANVIL, Blocks.TRAPPED_CHEST, Blocks.NOTE_BLOCK, Blocks.JUKEBOX, Blocks.TNT, Blocks.GOLD_ORE, Blocks.IRON_ORE, Blocks.LAPIS_ORE, Blocks.STONE_PRESSURE_PLATE, Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE, Blocks.STONE_BUTTON, Blocks.LEVER, Blocks.TALL_GRASS, Blocks.TRIPWIRE, Blocks.TRIPWIRE_HOOK, Blocks.RAIL, Blocks.CORNFLOWER, Blocks.RED_MUSHROOM, Blocks.BROWN_MUSHROOM, Blocks.VINE, Blocks.SUNFLOWER, Blocks.LADDER, Blocks.FURNACE, Blocks.SAND, Blocks.CACTUS, Blocks.DISPENSER, Blocks.DROPPER, Blocks.CRAFTING_TABLE, Blocks.COBWEB, Blocks.PUMPKIN, Blocks.COBBLESTONE_WALL, Blocks.OAK_FENCE, Blocks.REDSTONE_TORCH, Blocks.FLOWER_POT);
   }
}
