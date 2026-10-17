package dev.kissoo.mod.modules.impl.combat;

import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.Render3DEvent;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.events.impl.UpdateRotateEvent;
import dev.kissoo.api.utils.combat.CombatUtil;
import dev.kissoo.api.utils.math.Timer;
import dev.kissoo.api.utils.player.EntityUtil;
import dev.kissoo.api.utils.player.InventoryUtil;
import dev.kissoo.api.utils.player.MovementUtil;
import dev.kissoo.api.utils.render.Render3DUtil;
import dev.kissoo.api.utils.world.BlockPosX;
import dev.kissoo.api.utils.world.BlockUtil;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.impl.movement.ElytraFlight;
import dev.kissoo.mod.modules.impl.movement.Velocity;
import dev.kissoo.mod.modules.impl.player.AutoPot;
import dev.kissoo.mod.modules.impl.player.SpeedMine;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;

import java.awt.Color;
import java.util.*;

import static dev.kissoo.api.utils.world.BlockUtil.*;

public class AutoPush extends Module {

   public static AutoPush INSTANCE;
   public final SliderSetting targetRange = add(new SliderSetting("Range", 5.0, 2.0, 6.0));
   public final SliderSetting placeRange = add(new SliderSetting("PlaceRange", 5.0, 2.0, 6.0));
   public final SliderSetting delay = add(new SliderSetting("Delay", 50, 0, 500));
   public final BooleanSetting rotate = add(new BooleanSetting("Rotate", true));
   public final EnumSetting<RotateMode> rotateMode = add(new EnumSetting<>("RotateMode", RotateMode.Normal, () -> rotate.getValue()));
   public final BooleanSetting breakCrystal = add(new BooleanSetting("BreakCrystal", true));
   public final BooleanSetting invSwap = add(new BooleanSetting("InvSwap", false));
   public final BooleanSetting pauseOnMove = add(new BooleanSetting("PauseOnMove", false));
   public final BooleanSetting pauseOnUse = add(new BooleanSetting("PauseOnUse", true));
   public final BooleanSetting eatPause = add(new BooleanSetting("EatPause", true));
   public final BooleanSetting syncCrystal = add(new BooleanSetting("SyncCrystal", false));
   public final BooleanSetting containerPause = add(new BooleanSetting("ContainerPause", true));
   public final BooleanSetting autoMine = add(new BooleanSetting("AutoMine", true));
   public final EnumSetting<MoveFixMode> moveFix = add(new EnumSetting<>("MoveFix", MoveFixMode.SprintPacket));
   public final SliderSetting holdTicks = add(new SliderSetting("HoldTicks", 5, 1, 20));
   public final BooleanSetting savePiston = add(new BooleanSetting("SavePiston", false));
   public final SliderSetting tryCount = add(new SliderSetting("TryCount", 3, 1, 20, () -> savePiston.getValue()));
   public final SliderSetting updateDelay = add(new SliderSetting("UpdateDelay", 2000, 500, 10000, () -> savePiston.getValue()));
   public final BooleanSetting forcePush = add(new BooleanSetting("ForcePush", true));
   private final BooleanSetting renderSettings = add(new BooleanSetting("Render", true)).setParent();
   private final BooleanSetting renderPlace = add(new BooleanSetting("RenderPlace", true, () -> renderSettings.isOpen()));
   private final ColorSetting pistonColor = add(new ColorSetting("PistonColor", new Color(0, 150, 255, 80), () -> renderSettings.isOpen() && renderPlace.getValue()));
   private final ColorSetting pistonOutline = add(new ColorSetting("PistonOutline", new Color(0, 150, 255, 180), () -> renderSettings.isOpen() && renderPlace.getValue()));
   private final ColorSetting redstoneColor = add(new ColorSetting("RedstoneColor", new Color(255, 50, 50, 80), () -> renderSettings.isOpen() && renderPlace.getValue()));
   private final ColorSetting redstoneOutline = add(new ColorSetting("RedstoneOutline", new Color(255, 50, 50, 180), () -> renderSettings.isOpen() && renderPlace.getValue()));
   private final SliderSetting renderLineWidth = add(new SliderSetting("LineWidth", 1.5, 0.1, 3.0, 0.1, () -> renderSettings.isOpen() && renderPlace.getValue()));

   public boolean isActive() {
      return isOn() && isPushing && currentTarget != null;
   }

   private final Timer timer = new Timer();
   public PlayerEntity currentTarget = null;
   private boolean isPushing = false;
   private boolean silentRotationActive = false;
   private float silentYaw = 0;
   private float silentPitch = 0;
   private int silentRotationTicksRemaining = 0;
   private float placeYaw = 0;
   private float placePitch = 0;
   private boolean placeLookActive = false;

   private BlockPos renderPistonPos = null;
   private BlockPos renderRedstonePos = null;
   private final Map<UUID, Integer> playerTryCountMap = new HashMap<>();
   private final Map<UUID, Long> playerCooldownMap = new HashMap<>();
   private Direction pistonFacing = null;

   public enum MoveFixMode {
      Off,
      SprintPacket,
      SneakPacket
   }

   public enum RotateMode {
      Normal,
      C06
   }

   public AutoPush() {
      super("AutoPush", Category.Combat);
      INSTANCE = this;
      this.setChinese("自动推人");
   }

   @Override
   public void onEnable() {
      currentTarget = null;
      isPushing = false;
      silentRotationActive = false;
      silentRotationTicksRemaining = 0;
      placeLookActive = false;
      renderPistonPos = null;
      renderRedstonePos = null;
      playerTryCountMap.clear();
      playerCooldownMap.clear();
   }

   @Override
   public void onDisable() {
      currentTarget = null;
      isPushing = false;
      silentRotationActive = false;
      silentRotationTicksRemaining = 0;
      placeLookActive = false;
      renderPistonPos = null;
      renderRedstonePos = null;
      playerTryCountMap.clear();
      playerCooldownMap.clear();
   }

   private boolean isOnCooldown(PlayerEntity target) {
      if (!savePiston.getValue()) return false;

      UUID uuid = target.getUuid();
      Long cooldownEnd = playerCooldownMap.get(uuid);
      if (cooldownEnd != null) {
         if (System.currentTimeMillis() < cooldownEnd) {
            return true;
         } else {
            playerCooldownMap.remove(uuid);
            playerTryCountMap.remove(uuid);
         }
      }
      return false;
   }

   private void recordTry(PlayerEntity target) {
      if (!savePiston.getValue()) return;

      UUID uuid = target.getUuid();
      int count = playerTryCountMap.getOrDefault(uuid, 0) + 1;
      playerTryCountMap.put(uuid, count);

      if (count >= tryCount.getValueInt()) {
         long cooldownEnd = System.currentTimeMillis() + (long) updateDelay.getValueInt();
         playerCooldownMap.put(uuid, cooldownEnd);
         playerTryCountMap.remove(uuid);
      }
   }

   private void clearRecord(PlayerEntity target) {
      UUID uuid = target.getUuid();
      playerTryCountMap.remove(uuid);
      playerCooldownMap.remove(uuid);
   }

   private void cleanupRecords(List<PlayerEntity> currentEnemies) {
      Set<UUID> activeUUIDs = new HashSet<>();
      for (PlayerEntity p : currentEnemies) {
         if (p != null && p.isAlive()) {
            activeUUIDs.add(p.getUuid());
         }
      }
      playerTryCountMap.keySet().removeIf(uuid -> !activeUUIDs.contains(uuid));
   }

   private boolean isFood(ItemStack stack) {
      return stack.contains(DataComponentTypes.FOOD)
              || stack.getItem() instanceof PotionItem
              || stack.getItem() == Items.MILK_BUCKET;
   }

   private boolean shouldEatPause() {
      if (!eatPause.getValue() || mc.player == null) return false;
      if (mc.player.isUsingItem()) {
         ItemStack usingStack = mc.player.getActiveItem();
         if (isFood(usingStack)) return true;
      }
      if (mc.options.useKey.isPressed()
              && (isFood(mc.player.getMainHandStack()) || isFood(mc.player.getOffHandStack()))) {
         return true;
      }
      return false;
   }

   @EventListener
   public void onRender3D(Render3DEvent event) {
      if (!renderSettings.getValue() || !renderPlace.getValue()) return;
      if (mc.player == null || mc.world == null) return;

      if (renderPistonPos != null) {
         Box pistonBox = new Box(renderPistonPos);
         Render3DUtil.draw3DBox(
                 event.matrixStack,
                 pistonBox,
                 pistonColor.getValue(),
                 pistonOutline.getValue(),
                 true,
                 true
         );
      }

      if (renderRedstonePos != null) {
         Box redstoneBox = new Box(renderRedstonePos);
         Render3DUtil.draw3DBox(
                 event.matrixStack,
                 redstoneBox,
                 redstoneColor.getValue(),
                 redstoneOutline.getValue(),
                 true,
                 true
         );
      }
   }

   private float calcSafePistonPitch(BlockPos pistonPos) {
      if (mc.player == null) return 0.0f;
      double dy = (pistonPos.getY() + 0.5) - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
      double dx = (pistonPos.getX() + 0.5) - mc.player.getX();
      double dz = (pistonPos.getZ() + 0.5) - mc.player.getZ();
      double horizontalDist = Math.sqrt(dx * dx + dz * dz);
      float naturalPitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontalDist)));
      return MathHelper.clamp(naturalPitch, -30.0f, 30.0f);
   }

   private void setSilentPistonRotation(Direction dir, float pitch) {
      if (rotateMode.getValue() == RotateMode.C06) return;
      switch (dir) {
         case EAST:  silentYaw = -90.0f; break;
         case WEST:  silentYaw = 90.0f; break;
         case NORTH: silentYaw = 180.0f; break;
         case SOUTH: silentYaw = 0.0f; break;
         default: return;
      }
      silentPitch = pitch;
      silentRotationActive = true;
      silentRotationTicksRemaining = (int) holdTicks.getValue();
   }

   private void setSilentPistonRotation(Direction dir) {
      setSilentPistonRotation(dir, 0.0f);
   }

   public void pistonFacing(Direction facing) {
      this.pistonFacing = facing;
   }

   @EventListener(priority = -190)
   public void onRotation(UpdateRotateEvent event) {
      if (mc.player == null) return;
      if (rotateMode.getValue() == RotateMode.C06) return;
      if (!this.shouldYawStep()) return;

      if (silentRotationActive && silentRotationTicksRemaining > 0) {
         float targetYaw = silentYaw;
         float targetPitch = silentPitch;
         if (placeLookActive) {
            targetPitch = placePitch;
            placeLookActive = false;
         }

         event.setYaw(targetYaw);
         event.setPitch(targetPitch);
         silentRotationTicksRemaining--;

         if (silentRotationTicksRemaining <= 0) {
            silentRotationActive = false;
         }
      } else if (placeLookActive) {
         event.setYaw(placeYaw);
         event.setPitch(placePitch);
         placeLookActive = false;
      }
   }

   private boolean wasSprinting = false;
   private boolean wasSneaking = false;

   private void beginPlace() {
      if (mc.player == null || moveFix.getValue() == MoveFixMode.Off) return;

      switch (moveFix.getValue()) {
         case SprintPacket:
            wasSprinting = mc.player.isSprinting();
            if (wasSprinting) {
               mc.player.networkHandler.sendPacket(
                       new ClientCommandC2SPacket(mc.player,
                               ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            }
            break;
         case SneakPacket:
            wasSneaking = mc.player.isSneaking();
            if (!wasSneaking) {
               mc.player.networkHandler.sendPacket(
                       new ClientCommandC2SPacket(mc.player,
                               ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
            }
            break;
      }
   }

   private void endPlace() {
      if (mc.player == null || moveFix.getValue() == MoveFixMode.Off) return;

      switch (moveFix.getValue()) {
         case SprintPacket:
            if (wasSprinting) {
               mc.player.networkHandler.sendPacket(
                       new ClientCommandC2SPacket(mc.player,
                               ClientCommandC2SPacket.Mode.START_SPRINTING));
            }
            break;
         case SneakPacket:
            if (!wasSneaking) {
               mc.player.networkHandler.sendPacket(
                       new ClientCommandC2SPacket(mc.player,
                               ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
            }
            break;
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (mc.player == null || mc.world == null) return;
      if (!timer.passedMs(delay.getValue())) return;
      if (pauseOnMove.getValue() && MovementUtil.isMoving()) return;
      if (pauseOnUse.getValue() && mc.player.isUsingItem()) return;
      if (containerPause.getValue() && mc.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen) {
         isPushing = false;
         return;
      }
      if (shouldEatPause()) {
         isPushing = false;
         return;
      }
      if (AutoPot.INSTANCE != null && AutoPot.INSTANCE.isOn() && AutoPot.INSTANCE.isActivelyThrowing()) return;
      if (syncCrystal.getValue() && isEstraCrystalReadySafe()) {
         isPushing = false;
         return;
      }

      if (findRedstone() == -1 || findPiston() == -1) {
         isPushing = false;
         renderPistonPos = null;
         renderRedstonePos = null;
         return;
      }

      timer.reset();

      currentTarget = null;
      isPushing = false;

      List<PlayerEntity> enemies = CombatUtil.getEnemies(targetRange.getValue());
      if (enemies == null || enemies.isEmpty()) {
         renderPistonPos = null;
         renderRedstonePos = null;
         return;
      }

      if (savePiston.getValue()) {
         cleanupRecords(enemies);
      }

      for (PlayerEntity target : enemies) {
         if (target == null || !target.isAlive() || target == mc.player) continue;
         if (!isInHole(target)) {
            if (savePiston.getValue()) clearRecord(target);
            continue;
         }
         if (isOnCooldown(target)) continue;

         BlockPos targetPos = EntityUtil.getEntityPos(target);
         if (doPush(targetPos, target)) {
            currentTarget = target;
            isPushing = true;
            return;
         }
      }

      if (!isPushing) {
         renderPistonPos = null;
         renderRedstonePos = null;
      }
   }

   private boolean isEstraCrystalReadySafe() {
      try {
         Class<?> bridgeClass = Class.forName("net.Estradiol.client.util.HoPRCBridge");
         java.lang.reflect.Method method = bridgeClass.getMethod("isEstraCrystalReady");
         return (Boolean) method.invoke(null);
      } catch (Throwable e) {
         return false;
      }
   }

   private boolean doPush(BlockPos targetPos, PlayerEntity target) {
      boolean ceilingBlocked = mc.world.getBlockState(targetPos.up(2)).blocksMovement();
      if (!ceilingBlocked) {
         return doPushOpenTop(targetPos, target);
      } else {
         return doPushClosedTop(targetPos, target);
      }
   }

   private boolean doPushOpenTop(BlockPos targetPos, PlayerEntity target) {
      for (Direction i : getBestDirections(targetPos, target)) {
         BlockPos pos = targetPos.offset(i).up();
         if (!(getBlock(pos) instanceof PistonBlock)) continue;
         if (!checkPistonFacing(pos, i)) continue;
         if (!checkDestOpenOrMine(targetPos, i)) continue;

         for (Direction d2 : Direction.values()) {
            if (getBlock(pos.offset(d2)) == Blocks.REDSTONE_BLOCK) {
               if (autoMine.getValue()) SpeedMine.INSTANCE.mine(pos.offset(d2));
               if (rotate.getValue()) setSilentPistonRotation(i);
               renderPistonPos = pos;
               renderRedstonePos = pos.offset(d2);
               recordTry(target);
               return true;
            }
         }
      }
      for (Direction i : getBestDirections(targetPos, target)) {
         BlockPos pos = targetPos.offset(i).up();
         if (!(getBlock(pos) instanceof PistonBlock)) continue;
         if (!checkPistonFacing(pos, i)) continue;
         if (!checkDestOpenOrMine(targetPos, i)) continue;

         renderPistonPos = pos;
         if (doPower(pos, i)) {
            recordTry(target);
            return true;
         }
      }
      for (Direction i : getBestDirections(targetPos, target)) {
         BlockPos pos = targetPos.offset(i).up();
         if (tooCloseToSelf(pos, target)) continue;
         clearCrystalAt(pos);
         if (!checkDestOpenOrMine(targetPos, i)) continue;
         if (!clientCanPlace(pos, false)) continue;

         if (getPlaceSide(pos) == null) {
            if (downPower(pos)) break;
         }
         if (forcePush.getValue() && !checkDestOpen(targetPos, i)) {
            mineDestBlocks(targetPos, i);
         }

         doPiston(i, pos, autoMine.getValue());
         recordTry(target);
         return true;
      }

      if (getBlock(targetPos) == Blocks.AIR) {
         if (!forcePush.getValue()) return false;
      }
      for (Direction i : getBestDirections(targetPos, target)) {
         BlockPos pos = targetPos.offset(i).up();
         for (Direction d2 : Direction.values()) {
            if (getBlock(pos) instanceof PistonBlock && getBlock(pos.offset(d2)) == Blocks.REDSTONE_BLOCK) {
               if (!checkPistonFacing(pos, i)) continue;

               if (autoMine.getValue()) SpeedMine.INSTANCE.mine(pos.offset(d2));
               if (rotate.getValue()) setSilentPistonRotation(i);

               renderPistonPos = pos;
               renderRedstonePos = pos.offset(d2);
               recordTry(target);
               return true;
            }
         }
      }
      for (Direction i : getBestDirections(targetPos, target)) {
         BlockPos pos = targetPos.offset(i).up();
         if (!(getBlock(pos) instanceof PistonBlock)) continue;
         if (!checkPistonFacing(pos, i)) continue;

         for (Direction d2 : Direction.values()) {
            if (mc.world.isAir(pos.offset(d2))) {
               clearCrystalAt(pos.offset(d2));
               renderPistonPos = pos;
               if (!doPowerAt(pos, d2, i)) {
                  if (autoMine.getValue()) SpeedMine.INSTANCE.mine(pos.offset(d2));
                  recordTry(target);
                  return true;
               }
            }
         }
      }
      for (Direction i : getBestDirections(targetPos, target)) {
         BlockPos pos = targetPos.offset(i).up();
         if (tooCloseToSelf(pos, target)) continue;
         clearCrystalAt(pos);
         if (!clientCanPlace(pos, false)) continue;

         if (getPlaceSide(pos) == null) {
            if (downPower(pos)) continue;
         }
         if (forcePush.getValue()) {
            mineDestBlocks(targetPos, i);
         }

         doPiston(i, pos, true);
         recordTry(target);
         return true;
      }

      return false;
   }

   private boolean doPushClosedTop(BlockPos targetPos, PlayerEntity target) {
      for (Direction i : getBestDirections(targetPos, target)) {
         BlockPos pos = targetPos.offset(i).up();
         if (!(getBlock(pos) instanceof PistonBlock)) continue;
         if (!checkPistonFacing(pos, i)) continue;
         if (!checkDestClosedTopOrMine(targetPos, i, target)) continue;

         for (Direction d2 : Direction.values()) {
            if (getBlock(pos.offset(d2)) == Blocks.REDSTONE_BLOCK) {
               if (autoMine.getValue()) SpeedMine.INSTANCE.mine(pos.offset(d2));
               if (rotate.getValue()) setSilentPistonRotation(i);
               renderPistonPos = pos;
               renderRedstonePos = pos.offset(d2);
               recordTry(target);
               return true;
            }
         }
      }
      for (Direction i : getBestDirections(targetPos, target)) {
         BlockPos pos = targetPos.offset(i).up();
         if (!(getBlock(pos) instanceof PistonBlock)) continue;
         if (!checkPistonFacing(pos, i)) continue;
         if (!checkDestClosedTopOrMine(targetPos, i, target)) continue;

         renderPistonPos = pos;
         if (doPower(pos, i)) {
            recordTry(target);
            return true;
         }
      }
      for (Direction i : getBestDirections(targetPos, target)) {
         BlockPos pos = targetPos.offset(i).up();
         if (tooCloseToSelf(pos, target)) continue;
         clearCrystalAt(pos);
         if (!checkDestClosedTopOrMine(targetPos, i, target)) continue;
         if (!clientCanPlace(pos, false)) continue;
         if (mc.world.getBlockState(targetPos.offset(i, -1).up()).blocksMovement()) continue;

         if (getPlaceSide(pos) == null) {
            if (downPower(pos)) break;
         }
         if (forcePush.getValue() && !checkDestClosedTop(targetPos, i, target)) {
            mineDestBlocksClosedTop(targetPos, i);
         }

         doPiston(i, pos, autoMine.getValue());
         recordTry(target);
         return true;
      }
      if (forcePush.getValue()) {
         for (Direction i : getBestDirections(targetPos, target)) {
            BlockPos pos = targetPos.offset(i).up();
            if (tooCloseToSelf(pos, target)) continue;
            clearCrystalAt(pos);
            if (!clientCanPlace(pos, false)) continue;

            if (getPlaceSide(pos) == null) {
               if (downPower(pos)) continue;
            }

            mineDestBlocksClosedTop(targetPos, i);

            doPiston(i, pos, autoMine.getValue());
            recordTry(target);
            return true;
         }
      }

      return false;
   }

   private boolean checkDestOpenOrMine(BlockPos targetPos, Direction i) {
      if (checkDestOpen(targetPos, i)) return true;
      return forcePush.getValue();
   }

   private boolean checkDestClosedTopOrMine(BlockPos targetPos, Direction i, PlayerEntity target) {
      if (checkDestClosedTop(targetPos, i, target)) return true;
      return forcePush.getValue();
   }

   private void mineDestBlocks(BlockPos targetPos, Direction i) {
      Direction pushDir = i.getOpposite();
      BlockPos dest = targetPos.offset(pushDir);
      BlockPos destUp = dest.up();

      if (mc.world.getBlockState(dest).blocksMovement()) {
         SpeedMine.INSTANCE.mine(dest);
      }
      if (mc.world.getBlockState(destUp).blocksMovement()) {
         SpeedMine.INSTANCE.mine(destUp);
      }
   }

   private void mineDestBlocksClosedTop(BlockPos targetPos, Direction i) {
      Direction pushDir = i.getOpposite();
      BlockPos dest = targetPos.offset(pushDir);
      BlockPos destDown = dest.down();

      if (mc.world.getBlockState(dest).blocksMovement()) {
         SpeedMine.INSTANCE.mine(dest);
      }
      if (mc.world.getBlockState(destDown).blocksMovement()) {
         SpeedMine.INSTANCE.mine(destDown);
      }
   }

   private boolean checkPistonFacing(BlockPos pistonPos, Direction i) {
      Direction facing = mc.world.getBlockState(pistonPos).get(FacingBlock.FACING);
      return facing.getOpposite() == i;
   }

   private boolean checkDestOpen(BlockPos targetPos, Direction i) {
      BlockPos dest = targetPos.offset(i, -1);
      return !mc.world.getBlockState(dest).blocksMovement()
              && !mc.world.getBlockState(dest.up()).blocksMovement();
   }

   private boolean checkDestClosedTop(BlockPos targetPos, Direction i, PlayerEntity target) {
      BlockPos dest = targetPos.offset(i, -1);
      return (mc.world.isAir(dest) && mc.world.isAir(dest.down()))
              || isTargetHere(targetPos.offset(i, 2), target);
   }

   private void sendRotationPacket(float yaw, float pitch) {
      mc.getNetworkHandler().sendPacket(
              new PlayerMoveC2SPacket.Full(
                      mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                      yaw, pitch, mc.player.isOnGround()
              )
      );
   }

   private void sendRestoreRotationPacket() {
      mc.getNetworkHandler().sendPacket(
              new PlayerMoveC2SPacket.Full(
                      mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                      mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround()
              )
      );
   }

   public static Vec2f getRotationTo(Vec3d posFrom, Vec3d posTo) {
      Vec3d vec3d = posTo.subtract(posFrom);
      return getRotationFromVec(vec3d);
   }

   private static Vec2f getRotationFromVec(Vec3d vec) {
      double xz = Math.hypot(vec.x, vec.z);
      double yaw = normalizeAngle(Math.toDegrees(Math.atan2(vec.z, vec.x)) - 90.0);
      double pitch = normalizeAngle(Math.toDegrees(-Math.atan2(vec.y, xz)));
      return new Vec2f((float) yaw, (float) pitch);
   }

   private static double normalizeAngle(double angleIn) {
      double angle = angleIn % 360.0;
      if (angle >= 180.0) {
         angle -= 360.0;
      }
      if (angle < -180.0) {
         angle += 360.0;
      }
      return angle;
   }

   private void doPiston(Direction lookDir, BlockPos pos, boolean mine) {
      if (!canPlace(pos, placeRange.getValue())) return;

      int piston = findPiston();
      if (piston == -1) return;

      Direction side = getPlaceSide(pos);
      if (side == null) return;
      this.pistonFacing(lookDir);

      renderPistonPos = pos;

      float pistonYaw = dirToYaw(lookDir);
      float pistonPitch = calcSafePistonPitch(pos);

      if (rotate.getValue()) {
         if (rotateMode.getValue() == RotateMode.C06) {
            sendRotationPacket(pistonYaw, pistonPitch);
         } else {
            sendRotationPacket(pistonYaw, pistonPitch);
            setSilentPistonRotation(lookDir, pistonPitch);
         }
      }

      beginPlace();

      int oldSlot = mc.player.getInventory().selectedSlot;
      doSwap(piston);
      BlockUtil.placeBlock(pos, false, true);
      if (invSwap.getValue()) {
         doSwap(piston);
         EntityUtil.syncInventory();
      } else {
         doSwap(oldSlot);
      }

      endPlace();

      if (rotate.getValue() && rotateMode.getValue() == RotateMode.C06) {
         sendRestoreRotationPacket();
      }

      for (Direction d : Direction.values()) {
         if (getBlock(pos.offset(d)) == Blocks.REDSTONE_BLOCK) {
            if (mine) SpeedMine.INSTANCE.mine(pos.offset(d));
            renderRedstonePos = pos.offset(d);
            return;
         }
      }

      doPower(pos, lookDir);
   }

   private Direction getPistonFacing(BlockPos pistonPos) {
      if (getBlock(pistonPos) instanceof PistonBlock) {
         try {
            return mc.world.getBlockState(pistonPos).get(FacingBlock.FACING);
         } catch (Exception ignored) {}
      }
      return null;
   }

   private boolean doPower(BlockPos pistonPos, Direction pistonDir) {
      Direction pistonFacing = getPistonFacing(pistonPos);

      Direction best = getBestNeighboring(pistonPos, null);
      if (best != null && best != pistonFacing) {
         clearCrystalAt(pistonPos.offset(best));
         if (!doPowerAt(pistonPos, best, pistonDir)) return true;
      }
      for (Direction d : Direction.values()) {
         if (d == pistonFacing) continue;
         clearCrystalAt(pistonPos.offset(d));
         if (!doPowerAt(pistonPos, d, pistonDir)) return true;
      }
      return false;
   }

   private boolean doPowerAt(BlockPos pistonPos, Direction dir, Direction pistonDir) {
      BlockPos powerPos = pistonPos.offset(dir);
      if (!canPlace(powerPos, placeRange.getValue())) return true;

      int red = findRedstone();
      if (red == -1) return true;

      Direction side = getPlaceSide(powerPos);
      if (side == null) return true;

      Vec3d hitVec = powerPos.offset(side).toCenterPos().add(
              side.getOpposite().getVector().getX() * 0.5,
              side.getOpposite().getVector().getY() * 0.5,
              side.getOpposite().getVector().getZ() * 0.5
      );

      if (rotate.getValue()) {
         float[] angles = dev.kissoo.core.impl.RotationManager.getRotation(hitVec);
         if (rotateMode.getValue() == RotateMode.C06) {
            sendRotationPacket(angles[0], angles[1]);
         } else {
            sendRotationPacket(angles[0], angles[1]);
         }
      }

      renderRedstonePos = powerPos;

      beginPlace();

      int oldSlot = mc.player.getInventory().selectedSlot;
      doSwap(red);
      BlockUtil.placeBlock(powerPos, false, true);
      if (invSwap.getValue()) {
         doSwap(red);
         EntityUtil.syncInventory();
      } else {
         doSwap(oldSlot);
      }

      endPlace();

      if (rotate.getValue() && rotateMode.getValue() == RotateMode.C06) {
         sendRestoreRotationPacket();
      }

      return false;
   }

   private boolean downPower(BlockPos pistonPos) {
      if (getPlaceSide(pistonPos) != null) return false;

      boolean noPower = true;
      for (Direction d : Direction.values()) {
         if (getBlock(pistonPos.offset(d)) == Blocks.REDSTONE_BLOCK) {
            noPower = false;
            break;
         }
      }

      if (noPower) {
         BlockPos below = pistonPos.down();
         if (!canPlace(below, placeRange.getValue())) return true;

         int red = findRedstone();
         if (red == -1) return true;

         Direction side = getPlaceSide(below);
         if (side == null) return true;

         Vec3d hitVec = below.offset(side).toCenterPos().add(
                 side.getOpposite().getVector().getX() * 0.5,
                 side.getOpposite().getVector().getY() * 0.5,
                 side.getOpposite().getVector().getZ() * 0.5
         );

         if (rotate.getValue()) {
            float[] angles = dev.kissoo.core.impl.RotationManager.getRotation(hitVec);
            if (rotateMode.getValue() == RotateMode.C06) {
               sendRotationPacket(angles[0], angles[1]);
            } else {
               sendRotationPacket(angles[0], angles[1]);
            }
         }

         renderRedstonePos = below;

         beginPlace();

         int oldSlot = mc.player.getInventory().selectedSlot;
         doSwap(red);
         BlockUtil.placeBlock(below, false, true);
         if (invSwap.getValue()) {
            doSwap(red);
            EntityUtil.syncInventory();
         } else {
            doSwap(oldSlot);
         }

         endPlace();

         if (rotate.getValue() && rotateMode.getValue() == RotateMode.C06) {
            sendRestoreRotationPacket();
         }
      }

      return false;
   }

   private float dirToYaw(Direction dir) {
      switch (dir) {
         case EAST:  return -90.0f;
         case WEST:  return 90.0f;
         case NORTH: return 180.0f;
         case SOUTH: return 0.0f;
         default:    return 0.0f;
      }
   }

   private List<Direction> getBestDirections(BlockPos targetPos, PlayerEntity target) {
      List<DirectionScore> scored = new ArrayList<>();
      for (Direction dir : Direction.Type.HORIZONTAL) {
         scored.add(new DirectionScore(dir, scoreDirection(targetPos, target, dir)));
      }
      scored.sort(Comparator.comparingDouble(d -> -d.score));
      List<Direction> result = new ArrayList<>();
      for (DirectionScore ds : scored) result.add(ds.dir);
      return result;
   }

   private double scoreDirection(BlockPos targetPos, PlayerEntity target, Direction i) {
      Direction pushOut = i.getOpposite();
      BlockPos destPos = targetPos.offset(pushOut);
      double score = 0;
      int destSolid = 0;
      for (Direction d : Direction.Type.HORIZONTAL) {
         if (mc.world.getBlockState(destPos.offset(d)).blocksMovement()) destSolid++;
      }
      if (destSolid < 3) score += 25;

      int open = 0;
      for (Direction d : Direction.Type.HORIZONTAL) {
         if (!mc.world.getBlockState(destPos.offset(d)).blocksMovement()
                 && !mc.world.getBlockState(destPos.offset(d).up()).blocksMovement()) {
            open++;
         }
      }
      score += open * 8;

      if (!mc.world.getBlockState(destPos.down()).blocksMovement()) score += 20;
      if (!mc.world.getBlockState(destPos).blocksMovement()
              && !mc.world.getBlockState(destPos.up()).blocksMovement()) {
         score += 50;
      }

      if (mc.player != null && mc.player.distanceTo(target) > 3.0) {
         Vec3d toSelf = mc.player.getPos().subtract(target.getPos()).normalize();
         Vec3d pushVec = new Vec3d(pushOut.getOffsetX(), 0, pushOut.getOffsetZ());
         if (toSelf.dotProduct(pushVec) > 0.3) score += 15;
      }

      BlockPos pistonPos = targetPos.offset(i).up();
      if (clientCanPlace(pistonPos, false)) score += 40;
      if (getPlaceSide(pistonPos) != null) score += 20;

      if (getBlock(pistonPos) instanceof PistonBlock && checkPistonFacing(pistonPos, i)) {
         score += 100;
      }

      return score;
   }

   private boolean isInHole(PlayerEntity player) {
      if (player == null || mc.world == null) return false;

      double px = player.getX(), py = player.getY(), pz = player.getZ();

      int progress = 0;
      if (!mc.world.isAir(new BlockPosX(px + 1, py + 0.5, pz))) progress++;
      if (!mc.world.isAir(new BlockPosX(px - 1, py + 0.5, pz))) progress++;
      if (!mc.world.isAir(new BlockPosX(px, py + 0.5, pz + 1))) progress++;
      if (!mc.world.isAir(new BlockPosX(px, py + 0.5, pz - 1))) progress++;

      if (!mc.world.isAir(new BlockPosX(px, py + 2.5, pz))) {
         BlockPos feetPos = EntityUtil.getEntityPos(player);
         for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos sidePos = feetPos.offset(dir);
            if ((mc.world.isAir(sidePos) && mc.world.isAir(sidePos.up()))
                    || isTargetHere(sidePos, player)) {
               if (!mc.world.isAir(new BlockPosX(px, py + 0.5, pz))) return true;
               return progress >= 2;
            }
         }
         return false;
      }

      if (!mc.world.canCollide(player, new Box(new BlockPosX(px, py + 2.5, pz)))) {
         BlockPos feetPos = EntityUtil.getEntityPos(player);
         for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos pos = feetPos.offset(dir);
            Box box = player.getBoundingBox().offset(dir.getOffsetX(), dir.getOffsetY(), dir.getOffsetZ());
            if (getBlock(pos.up()) != Blocks.PISTON_HEAD
                    && !mc.world.canCollide(player, box.offset(0, 1, 0))
                    && !isTargetHere(pos, player)) {
               if (mc.world.canCollide(player, new Box(new BlockPosX(px, py + 0.5, pz)))) {
                  return true;
               }
            }
         }
      }

      return progress >= 2 || CombatUtil.isHard(new BlockPosX(px, py + 0.5, pz));
   }

   private boolean isTargetHere(BlockPos pos, Entity target) {
      return new Box(pos).intersects(target.getBoundingBox());
   }

   private boolean tooCloseToSelf(BlockPos pos, PlayerEntity target) {
      if (mc.player == null) return false;
      double hDiff = mc.player.getY() - target.getY();
      if ((hDiff <= -1.0 || hDiff >= 2.0) && distanceToXZ(pos.getX() + 0.5, pos.getZ() + 0.5) < 2.6) {
         return true;
      }
      return false;
   }

   private Block getBlock(BlockPos pos) {
      return mc.world.getBlockState(pos).getBlock();
   }

   private void clearCrystalAt(BlockPos pos) {
      if (!breakCrystal.getValue() || mc.world == null || pos == null) return;
      for (Entity entity : mc.world.getEntities()) {
         if (!(entity instanceof EndCrystalEntity)) continue;
         if (entity.squaredDistanceTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) <= 4.0) {
            CombatUtil.attackCrystal(entity, rotate.getValue(), false);
            return;
         }
      }
   }

   private int findRedstone() {
      if (invSwap.getValue()) return InventoryUtil.findBlockInventorySlot(Blocks.REDSTONE_BLOCK);
      return InventoryUtil.findBlock(Blocks.REDSTONE_BLOCK);
   }

   private int findPiston() {
      if (invSwap.getValue()) return InventoryUtil.findClassInventorySlot(PistonBlock.class);
      return InventoryUtil.findClass(PistonBlock.class);
   }

   private boolean shouldYawStep() {
      if (mc.player == null) return false;
      boolean allowWhileFlying = ElytraFlight.INSTANCE == null
              || !ElytraFlight.INSTANCE.isOn()
              || !ElytraFlight.INSTANCE.isFallFlying();
      boolean rotationBlocked = Velocity.INSTANCE != null && Velocity.INSTANCE.noRotation();
      return (!mc.player.isFallFlying() || allowWhileFlying) && !rotationBlocked;
   }

   private void doSwap(int slot) {
      if (invSwap.getValue()) {
         if (mc.player != null) InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
      } else {
         InventoryUtil.switchToSlot(slot);
      }
   }

   @Override
   public String getInfo() {
      if (currentTarget == null) return null;
      String info = currentTarget.getName().getString();
      if (savePiston.getValue()) {
         UUID uuid = currentTarget.getUuid();
         int count = playerTryCountMap.getOrDefault(uuid, 0);
         Long cooldownEnd = playerCooldownMap.get(uuid);
         if (cooldownEnd != null) {
            long remaining = cooldownEnd - System.currentTimeMillis();
            if (remaining > 0) {
               info += " §c[CD:" + (remaining / 1000) + "s]";
            }
         } else {
            info += " §e[" + count + "/" + tryCount.getValueInt() + "]";
         }
      }
      return info;
   }

   private static class DirectionScore {
      final Direction dir;
      final double score;
      DirectionScore(Direction dir, double score) { this.dir = dir; this.score = score; }
   }
}
