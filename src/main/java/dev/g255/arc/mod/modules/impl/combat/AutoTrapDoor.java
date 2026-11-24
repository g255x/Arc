package dev.g255.arc.mod.modules.impl.combat;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.ClientTickEvent;
import dev.g255.arc.api.events.impl.RotationEvent;
import dev.g255.arc.api.utils.combat.CombatUtil;
import dev.g255.arc.api.utils.math.PredictUtil;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.api.utils.player.EntityUtil;
import dev.g255.arc.api.utils.player.InventoryUtil;
import dev.g255.arc.api.utils.world.BlockPosX;
import dev.g255.arc.api.utils.world.BlockUtil;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.impl.exploit.Blink;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.hit.BlockHitResult;

public class AutoTrapDoor extends Module {
   public static AutoTrapDoor INSTANCE;
   public Vec3d directionVec = null;
   private final EnumSetting<Page> page;
   private final SliderSetting delay;
   private final SliderSetting range;
   private final SliderSetting placeRange;
   private final SliderSetting predictTicks;
   private final SliderSetting closeDelay;
   private final BooleanSetting autoDisable;
   private final BooleanSetting ignoreNakeds;
   private final BooleanSetting antiStandUp;
   private final BooleanSetting ghostHand;
   private final BooleanSetting rotate;
   private final BooleanSetting yawStep;
   private final BooleanSetting whenElytra;
   private final SliderSetting steps;
   private final BooleanSetting checkFov;
   private final SliderSetting fov;
   private final SliderSetting priority;
   private final BooleanSetting silent;
   private final BooleanSetting inventory;
   private final BooleanSetting usingPause;
   private final BooleanSetting pauseEat;
   private final Timer timer;
   private final Timer closeTimer;
   private boolean waitingClose;
   private BlockPos pendingClosePos;
   private BlockPos completedPos;
   private PlayerEntity currentTarget;

   public AutoTrapDoor() {
      super("AutoTrapDoor", Module.Category.Combat);
      this.page = this.add(new EnumSetting("Page", AutoTrapDoor.Page.General));
      this.delay = this.add((new SliderSetting("Delay", 100, 0, 500, () -> this.page.is(AutoTrapDoor.Page.General))).setSuffix("ms"));
      this.range = this.add((new SliderSetting("Range", (double)5.0F, (double)0.0F, (double)8.0F, () -> this.page.is(AutoTrapDoor.Page.General))).setSuffix("m"));
      this.placeRange = this.add((new SliderSetting("PlaceRange", (double)4.5F, (double)0.0F, (double)6.0F, () -> this.page.is(AutoTrapDoor.Page.General))).setSuffix("m"));
      this.predictTicks = this.add(new SliderSetting("PredictTicks", (double)2.0F, (double)0.0F, (double)50.0F, (double)1.0F, () -> this.page.is(AutoTrapDoor.Page.General)));
      this.closeDelay = this.add((new SliderSetting("CloseDelay", 75, 0, 500, () -> this.page.is(AutoTrapDoor.Page.General))).setSuffix("ms"));
      this.autoDisable = this.add(new BooleanSetting("AutoDisable", false, () -> this.page.is(AutoTrapDoor.Page.General)));
      this.ignoreNakeds = this.add(new BooleanSetting("IgnoreNakeds", false, () -> this.page.is(AutoTrapDoor.Page.General)));
      this.antiStandUp = this.add(new BooleanSetting("AntiStandUp", true, () -> this.page.is(AutoTrapDoor.Page.General)));
      this.ghostHand = this.add(new BooleanSetting("GhostHand", true, () -> this.page.is(AutoTrapDoor.Page.General)));
      this.rotate = this.add(new BooleanSetting("Rotate", true, () -> this.page.is(AutoTrapDoor.Page.Rotate)));
      this.yawStep = this.add((new BooleanSetting("YawStep", false, () -> this.rotate.isOpen() && this.page.is(AutoTrapDoor.Page.Rotate))).setParent());
      this.whenElytra = this.add(new BooleanSetting("FallFlying", true, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.page.is(AutoTrapDoor.Page.Rotate)));
      this.steps = this.add(new SliderSetting("Steps", 0.05, (double)0.0F, (double)1.0F, 0.01, () -> this.page.is(AutoTrapDoor.Page.Rotate) && this.yawStep.isOpen()));
      this.checkFov = this.add((new BooleanSetting("OnlyLooking", true, () -> this.page.is(AutoTrapDoor.Page.Rotate) && this.yawStep.isOpen())).setParent());
      this.fov = this.add(new SliderSetting("Fov", (double)20.0F, (double)0.0F, (double)360.0F, 0.1, () -> this.checkFov.isOpen() && this.page.is(AutoTrapDoor.Page.Rotate) && this.yawStep.isOpen()));
      this.priority = this.add(new SliderSetting("Priority", 10, 0, 100, () -> this.page.is(AutoTrapDoor.Page.Rotate) && this.yawStep.isOpen()));
      this.silent = this.add(new BooleanSetting("Silent", false, () -> this.page.is(AutoTrapDoor.Page.Swap)));
      this.inventory = this.add(new BooleanSetting("InventorySwap", true, () -> this.page.is(AutoTrapDoor.Page.Swap)));
      this.usingPause = this.add(new BooleanSetting("UsingPause", true, () -> this.page.is(AutoTrapDoor.Page.Misc)));
      this.pauseEat = this.add(new BooleanSetting("PauseEat", true, () -> this.page.is(AutoTrapDoor.Page.Misc)));
      this.timer = new Timer();
      this.closeTimer = new Timer();
      this.setChinese("活板门光环");
      INSTANCE = this;
   }

   public String getInfo() {
      return this.currentTarget != null ? this.currentTarget.getName().getString() : null;
   }

   public void onEnable() {
      this.reset();
   }

   public void onDisable() {
      this.reset();
   }

   private void reset() {
      this.waitingClose = false;
      this.pendingClosePos = null;
      this.completedPos = null;
      this.currentTarget = null;
      this.timer.reset();
      this.closeTimer.reset();
      this.directionVec = null;
   }

   @EventListener
   public void onRotate(RotationEvent event) {
      if (this.directionVec != null && this.rotate.getValue() && this.shouldYawStep()) {
         event.setTarget(this.directionVec, this.steps.getValueFloat(), this.priority.getValueFloat());
      }

   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!nullCheck()) {
         if (!event.isPost()) {
            if (this.waitingClose) {
               if (this.pendingClosePos != null && this.closeTimer.passed((long)this.closeDelay.getValueInt())) {
                  if (this.toggleTrapdoor(this.pendingClosePos, false)) {
                     this.completedPos = this.pendingClosePos;
                     this.timer.reset();
                  }

                  this.pendingClosePos = null;
                  this.waitingClose = false;
               }

            } else if (!this.shouldPause()) {
               if (this.timer.passed((long)this.delay.getValueInt())) {
                  PlayerEntity target = CombatUtil.getClosestEnemy(this.range.getValue());
                  if (target == null) {
                     if (this.autoDisable.getValue()) {
                        this.disable();
                     }

                     this.currentTarget = null;
                  } else {
                     this.currentTarget = target;
                     if (!this.ignoreNakeds.getValue() || !this.isNaked(target)) {
                        Vec3d targetPos = this.predictTicks.getValue() > (double)0.0F ? PredictUtil.getPos(target, this.predictTicks.getValueInt()) : target.getPos();
                        BlockPosX trapdoorPos = new BlockPosX(Math.floor(targetPos.getX()), targetPos.getY() + (double)1.0F, Math.floor(targetPos.getZ()));
                        if (trapdoorPos.getY() < 320 && trapdoorPos.getY() >= -64) {
                           if (this.isTrapdoor(trapdoorPos)) {
                              this.handleExistingTrapdoor(trapdoorPos);
                           } else {
                              if (this.completedPos != null && this.completedPos.equals(trapdoorPos)) {
                                 this.completedPos = null;
                              }

                              this.placeBlockOnTarget(trapdoorPos);
                           }

                        }
                     }
                  }
               }
            }
         }
      }
   }

   private boolean shouldPause() {
      if (Blink.INSTANCE != null && Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
         return true;
      } else if (this.usingPause.getValue() && mc.player.isUsingItem()) {
         return true;
      } else {
         return this.pauseEat.getValue() && mc.player.isUsingItem();
      }
   }

   private boolean shouldYawStep() {
      return !this.whenElytra.getValue() && mc.player.isFallFlying() ? false : this.yawStep.getValue();
   }

   private void handleExistingTrapdoor(BlockPos trapdoorPos) {
      boolean isOpen = (Boolean)mc.world.getBlockState(trapdoorPos).get(TrapdoorBlock.OPEN);
      if (isOpen) {
         if (this.toggleTrapdoor(trapdoorPos, false)) {
            this.completedPos = trapdoorPos;
            this.timer.reset();
         }
      } else {
         if (this.completedPos != null && this.completedPos.equals(trapdoorPos)) {
            return;
         }

         if (this.toggleTrapdoor(trapdoorPos, true)) {
            this.waitingClose = true;
            this.pendingClosePos = trapdoorPos;
            this.closeTimer.reset();
            this.timer.reset();
         }
      }

   }

   private void placeBlockOnTarget(BlockPos trapdoorPos) {
      if (this.canPlaceBlock(trapdoorPos)) {
         int slot = this.findTrapdoorSlot();
         if (slot != -1) {
            int oldSlot = mc.player.getInventory().selectedSlot;
            this.selectSlot(slot, oldSlot);
            Direction side = BlockUtil.getPlaceSide(trapdoorPos, this.placeRange.getValue());
            if (side != null) {
               Vec3d boxCenter = new Vec3d((double)trapdoorPos.getX() + (double)0.5F, (double)trapdoorPos.getY() + (double)0.5F, (double)trapdoorPos.getZ() + (double)0.5F);
               if (this.faceVector(boxCenter)) {
                  BlockUtil.placeBlock(trapdoorPos, false);
               }
            }

            this.restoreSlot(slot, oldSlot);
            if (this.antiStandUp.getValue()) {
               this.waitingClose = true;
               this.pendingClosePos = trapdoorPos;
               this.closeTimer.reset();
            }

            this.timer.reset();
         }
      }
   }

   private boolean canPlaceBlock(BlockPos pos) {
      if (!BlockUtil.canReplace(pos)) {
         return false;
      } else {
         if (this.ghostHand.getValue()) {
            if (this.hasEntityIgnoringPlayers(pos)) {
               return false;
            }
         } else if (BlockUtil.hasEntity(pos, false)) {
            return false;
         }

         if (this.placeRange.getValue() > (double)0.0F && mc.player.getPos().squaredDistanceTo(pos.toCenterPos()) > this.placeRange.getValue() * this.placeRange.getValue()) {
            return false;
         } else {
            return BlockUtil.getPlaceSide(pos, this.placeRange.getValue()) != null || BlockUtil.allowAirPlace();
         }
      }
   }

   private int findTrapdoorSlot() {
      return this.inventory.getValue() ? InventoryUtil.findClassInventorySlot(TrapdoorBlock.class) : InventoryUtil.findClass(TrapdoorBlock.class);
   }

   private boolean toggleTrapdoor(BlockPos trapdoorPos, boolean desiredOpen) {
      if (!this.isTrapdoor(trapdoorPos)) {
         return false;
      } else {
         boolean isOpen = (Boolean)mc.world.getBlockState(trapdoorPos).get(TrapdoorBlock.OPEN);
         if (isOpen == desiredOpen) {
            return false;
         } else if (this.placeRange.getValue() > (double)0.0F && mc.player.getPos().squaredDistanceTo(trapdoorPos.toCenterPos()) > this.placeRange.getValue() * this.placeRange.getValue()) {
            return false;
         } else {
            Direction side = BlockUtil.getClickSide(trapdoorPos);
            if (side == null) {
               return false;
            } else {
               Vec3d hitPoint = new Vec3d((double)trapdoorPos.getX() + (double)0.5F + (double)side.getVector().getX() * (double)0.5F, (double)trapdoorPos.getY() + (double)0.5F + (double)side.getVector().getY() * (double)0.5F, (double)trapdoorPos.getZ() + (double)0.5F + (double)side.getVector().getZ() * (double)0.5F);
               if (this.faceVector(hitPoint)) {
                  BlockHitResult bhr = new BlockHitResult(hitPoint, side, trapdoorPos, false);
                  if (mc.interactionManager != null) {
                     mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                  }

                  return true;
               } else {
                  return false;
               }
            }
         }
      }
   }

   private boolean faceVector(Vec3d vec) {
      if (!this.shouldYawStep()) {
         Arc.ROTATION.lookAt(vec);
         return true;
      } else {
         this.directionVec = vec;
         if (Arc.ROTATION.inFov(vec, this.fov.getValueFloat())) {
            return true;
         } else {
            return !this.checkFov.getValue();
         }
      }
   }

   private boolean isTrapdoor(BlockPos pos) {
      BlockState state = mc.world.getBlockState(pos);
      return state.getBlock() instanceof TrapdoorBlock;
   }

   private boolean hasEntityIgnoringPlayers(BlockPos pos) {
      for(Entity entity : BlockUtil.getEntities(new Box((double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), (double)pos.getX() + (double)1.0F, (double)pos.getY() + (double)1.0F, (double)pos.getZ() + (double)1.0F))) {
         if (entity != null && entity.isAlive() && !(entity instanceof PlayerEntity)) {
            return true;
         }
      }

      return false;
   }

   private boolean isNaked(PlayerEntity player) {
      for(int i = 0; i < 4; ++i) {
         ItemStack stack = (ItemStack)player.getInventory().armor.get(i);
         if (!stack.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   private void selectSlot(int slot, int selectedSlot) {
      if (this.inventory.getValue()) {
         InventoryUtil.inventorySwap(slot, selectedSlot);
      } else if (this.silent.getValue()) {
         if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
         }
      } else {
         InventoryUtil.switchToSlot(slot);
      }

   }

   private void restoreSlot(int slot, int selectedSlot) {
      if (this.inventory.getValue()) {
         InventoryUtil.inventorySwap(slot, selectedSlot);
         EntityUtil.syncInventory();
      } else if (this.silent.getValue()) {
         if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(selectedSlot));
         }
      } else {
         InventoryUtil.switchToSlot(selectedSlot);
      }

   }

   public static enum Page {
      General,
      Rotate,
      Swap,
      Misc;

      // $FF: synthetic method
      private static Page[] $values() {
         return new Page[]{General, Rotate, Swap, Misc};
      }
   }
}
