package dev.G255.Lumi.mod.modules.impl.combat;

import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.UpdateEvent;
import dev.G255.Lumi.api.utils.combat.CombatUtil;
import dev.G255.Lumi.api.utils.math.PredictUtil;
import dev.G255.Lumi.api.utils.math.Timer;
import dev.G255.Lumi.api.utils.move.MoveUtils;
import dev.G255.Lumi.api.utils.player.EntityUtil;
import dev.G255.Lumi.api.utils.player.InventoryUtil;
import dev.G255.Lumi.api.utils.rotation.Rotation;
import dev.G255.Lumi.api.utils.rotation.RotationUtils;
import dev.G255.Lumi.api.utils.world.BlockPosX;
import dev.G255.Lumi.api.utils.world.BlockUtil;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.impl.exploit.Blink;
import dev.G255.Lumi.mod.modules.impl.player.PacketMine;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

public class AutoTrapDoor extends Module {
   public static AutoTrapDoor INSTANCE;
   private static final long CLOSE_DELAY_MS = 75L;
   private final SliderSetting delay = this.add((new SliderSetting("Delay", 100, 0, 500)).setSuffix("ms"));
   private final SliderSetting range = this.add((new SliderSetting("Range", (double)5.0F, (double)0.0F, (double)8.0F)).setSuffix("m"));
   private final SliderSetting predictTicks = this.add(new SliderSetting("PredictTicks", (double)2.0F, (double)0.0F, (double)50.0F, (double)1.0F));
   private final SliderSetting placeRange = this.add((new SliderSetting("PlaceRange", (double)4.5F, (double)0.0F, (double)6.0F)).setSuffix("m"));
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
   private final BooleanSetting movementFix = this.add(new BooleanSetting("MovementFix", true));
   private final BooleanSetting silent = this.add(new BooleanSetting("Silent", false));
   private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
   private final BooleanSetting ghostHand = this.add(new BooleanSetting("GhostHand", true));
   private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", true));
   private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", false));
   private final Timer timer = new Timer();
   private final Timer closeTimer = new Timer();
   private boolean waitingClose;
   private BlockPos pendingClosePos;
   private BlockPos completedPos;

   public AutoTrapDoor() {
      super("AutoTrapDoor", Module.Category.Combat);
      this.setChinese("");
      INSTANCE = this;
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
      this.timer.reset();
      this.closeTimer.reset();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.waitingClose) {
         if (this.pendingClosePos != null && this.closeTimer.passed(75L)) {
            if (this.toggleTrapdoor(this.pendingClosePos, false)) {
               this.completedPos = this.pendingClosePos;
               this.timer.reset();
            }

            this.pendingClosePos = null;
            this.waitingClose = false;
         }

      } else if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
         if (!this.usingPause.getValue() || !mc.player.isUsingItem()) {
            if (this.timer.passed((long)this.delay.getValue())) {
               PlayerEntity target = CombatUtil.getClosestEnemy(this.range.getValue());
               if (target == null) {
                  if (this.autoDisable.getValue()) {
                     this.disable();
                  }

               } else {
                  Vec3d playerPos = this.predictTicks.getValue() > (double)0.0F ? PredictUtil.getPos(target, this.predictTicks.getValueInt()) : target.getPos();
                  BlockPos trapdoorPos = new BlockPosX(playerPos.getX(), playerPos.getY() + (double)1.0F, playerPos.getZ());
                  if (trapdoorPos.getY() < 320) {
                     if (this.isTrapdoor(trapdoorPos)) {
                        BlockState state = mc.world.getBlockState(trapdoorPos);
                        boolean open = Boolean.TRUE.equals(state.get(TrapdoorBlock.OPEN));
                        if (open) {
                           if (this.toggleTrapdoor(trapdoorPos, false)) {
                              this.completedPos = trapdoorPos;
                              this.timer.reset();
                           }

                        } else if (this.completedPos == null || !this.completedPos.equals(trapdoorPos)) {
                           if (this.toggleTrapdoor(trapdoorPos, true)) {
                              this.waitingClose = true;
                              this.pendingClosePos = trapdoorPos;
                              this.closeTimer.reset();
                              this.timer.reset();
                           }

                        }
                     } else {
                        if (this.completedPos != null && this.completedPos.equals(trapdoorPos)) {
                           this.completedPos = null;
                        }

                        int trapdoorSlot = this.getTrapdoorSlot();
                        if (trapdoorSlot != -1) {
                           if (!this.shouldSkipPlace(trapdoorPos)) {
                              int oldSlot = mc.player.getInventory().selectedSlot;
                              this.selectSlot(trapdoorSlot, oldSlot);
                              BlockUtil.placeBlock(trapdoorPos, this.rotate.getValue());
                              this.restoreSlot(trapdoorSlot, oldSlot);
                              if (this.toggleTrapdoor(trapdoorPos, true)) {
                                 this.waitingClose = true;
                                 this.pendingClosePos = trapdoorPos;
                                 this.closeTimer.reset();
                              }

                              this.timer.reset();
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private boolean toggleTrapdoor(BlockPos trapdoorPos, boolean desiredOpen) {
      BlockState state = mc.world.getBlockState(trapdoorPos);
      if (!(state.getBlock() instanceof TrapdoorBlock)) {
         return false;
      } else {
         boolean open = Boolean.TRUE.equals(state.get(TrapdoorBlock.OPEN));
         if (open == desiredOpen) {
            return false;
         } else if (this.placeRange.getValue() > (double)0.0F && mc.player.getEyePos().distanceTo(trapdoorPos.toCenterPos()) > this.placeRange.getValue()) {
            return false;
         } else {
            Direction clickSide = BlockUtil.getClickSide(trapdoorPos);
            if (this.movementFix.getValue() && this.rotate.getValue() && clickSide != null) {
               Vec3d hitVec = trapdoorPos.toCenterPos().add((double)clickSide.getVector().getX() * (double)0.5F, (double)clickSide.getVector().getY() * (double)0.5F, (double)clickSide.getVector().getZ() * (double)0.5F);
               Rotation rotation = RotationUtils.calculate(hitVec);
               MoveUtils.fixMovement(rotation.getYaw());
            }

            BlockUtil.clickBlock(trapdoorPos, clickSide, this.rotate.getValue());
            return true;
         }
      }
   }

   private boolean isTrapdoor(BlockPos pos) {
      return mc.world.getBlockState(pos).getBlock() instanceof TrapdoorBlock;
   }

   private boolean shouldSkipPlace(BlockPos trapdoorPos) {
      if (!BlockUtil.canReplace(trapdoorPos)) {
         return true;
      } else {
         if (this.ghostHand.getValue()) {
            if (this.hasEntityIgnoringPlayers(trapdoorPos)) {
               return true;
            }
         } else if (BlockUtil.hasEntity(trapdoorPos, false)) {
            return true;
         }

         if (this.placeRange.getValue() > (double)0.0F && mc.player.getEyePos().distanceTo(trapdoorPos.toCenterPos()) > this.placeRange.getValue()) {
            return true;
         } else if (BlockUtil.getPlaceSide(trapdoorPos, this.placeRange.getValue()) == null && !BlockUtil.allowAirPlace()) {
            return true;
         } else {
            BlockPos breakPos = PacketMine.getBreakPos();
            return PacketMine.INSTANCE.isOn() && breakPos != null && trapdoorPos.equals(breakPos);
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

   private int getTrapdoorSlot() {
      return this.inventory.getValue() ? InventoryUtil.findClassInventorySlot(TrapdoorBlock.class) : InventoryUtil.findClass(TrapdoorBlock.class);
   }

   private void selectSlot(int slot, int selectedSlot) {
      if (this.inventory.getValue()) {
         InventoryUtil.inventorySwap(slot, selectedSlot);
      } else if (this.silent.getValue()) {
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
      } else {
         InventoryUtil.switchToSlot(slot);
      }
   }

   private void restoreSlot(int slot, int selectedSlot) {
      if (this.inventory.getValue()) {
         InventoryUtil.inventorySwap(slot, selectedSlot);
         EntityUtil.syncInventory();
      } else if (this.silent.getValue()) {
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(selectedSlot));
      } else {
         InventoryUtil.switchToSlot(selectedSlot);
      }
   }
}
