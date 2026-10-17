package dev.kissoo.mod.modules.impl.combat;

import dev.kissoo.Forever;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.PlaceBlockEvent;
import dev.kissoo.api.events.impl.Render2DEvent;
import dev.kissoo.api.events.impl.Render3DEvent;
import dev.kissoo.api.events.impl.RotationEvent;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.utils.math.Timer;
import dev.kissoo.api.utils.player.InventoryUtil;
import dev.kissoo.api.utils.render.ProjectionUtils;
import dev.kissoo.api.utils.render.Render2DUtil;
import dev.kissoo.api.utils.world.BlockUtil;
import dev.kissoo.core.Manager;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.impl.movement.ElytraFlight;
import dev.kissoo.mod.modules.impl.movement.Velocity;
import dev.kissoo.mod.modules.impl.player.SpeedMine;
import dev.kissoo.mod.modules.settings.enums.SwingSide;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import dev.kissoo.mod.modules.settings.impl.StringSetting;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Vector3f;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class AntiRegear extends Module {
   public static AntiRegear INSTANCE;

   public enum AntiMode { Stealer, Throw, Smart }
   public enum ActionPriority { MineAndSteal, Crystal }

   public final EnumSetting<AntiMode> antiMode = this.add(new EnumSetting<>("Mode", AntiMode.Smart));
   // 新增：执行模式选择
   public final EnumSetting<ActionPriority> actionPriority = this.add(new EnumSetting<>("PriorityMode", ActionPriority.MineAndSteal));

   public final StringSetting kitName = this.add(new StringSetting("KitName", "default", () -> antiMode.getValue() != AntiMode.Throw));
   private final SliderSetting range = this.add(new SliderSetting("SearchRange", 8.0, 3.0, 15.0, 0.1));
   // 新增：真实交互/放水晶距离（防止手够不到被拉回）
   private final SliderSetting interactRange = this.add(new SliderSetting("InteractRange", 4.5, 3.0, 6.0, 0.1));

   private final SliderSetting safeTime = this.add(new SliderSetting("SafeTime", 10.0, 1.0, 30.0, 1.0).setSuffix("s"));
   private final SliderSetting friendRange = this.add(new SliderSetting("FriendRange", 8.0, 0.0, 20.0, 0.5));

   // 敌人距离检测
   private final BooleanSetting checkEnemy = this.add(new BooleanSetting("CheckEnemy", true).setParent());
   private final SliderSetting enemyRange = this.add(new SliderSetting("EnemyRange", 15.0, 5.0, 50.0, 0.5, () -> checkEnemy.isOpen()).setSuffix("m"));

   // 同步挖掘与页面控制
   private final BooleanSetting mineSync = this.add(new BooleanSetting("MineSync", true));
   private final SliderSetting closeDelay = this.add(new SliderSetting("CloseDelay", 1000, 100, 3000, 10).setSuffix("ms"));

   private final SliderSetting weaponCoin = this.add(new SliderSetting("WeaponCoin", 100, 1, 1000, 1, () -> antiMode.getValue() == AntiMode.Smart));
   private final SliderSetting armorCoin = this.add(new SliderSetting("ArmorCoin", 50, 1, 1000, 1, () -> antiMode.getValue() == AntiMode.Smart));
   private final SliderSetting potCoin = this.add(new SliderSetting("PotCoin", 10, 1, 1000, 1, () -> antiMode.getValue() == AntiMode.Smart));
   private final SliderSetting itemCoin = this.add(new SliderSetting("ItemCoin", 1, 1, 1000, 1, () -> antiMode.getValue() == AntiMode.Smart));
   private final SliderSetting stealThreshold = this.add(new SliderSetting("StealThreshold", 200, 0, 5000, 10, () -> antiMode.getValue() == AntiMode.Smart));
   private final BooleanSetting regearArmor = this.add(new BooleanSetting("RegearArmor", true, () -> antiMode.getValue() != AntiMode.Throw));
   private final BooleanSetting returnExtras = this.add(new BooleanSetting("ReturnExtras", true, () -> antiMode.getValue() != AntiMode.Throw));
   private final BooleanSetting mineAfter = this.add(new BooleanSetting("MineAfter", true, () -> antiMode.getValue() == AntiMode.Stealer));
   private final BooleanSetting continuous = this.add(new BooleanSetting("Continuous", true));
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
   private final BooleanSetting eatingPause = this.add(new BooleanSetting("EatingPause", true));
   private final BooleanSetting yawStep = this.add(new BooleanSetting("YawStep", false, () -> this.rotate.isOpen()).setParent());
   private final BooleanSetting whenElytra = this.add(new BooleanSetting("FallFlying", true, () -> this.rotate.isOpen() && this.yawStep.isOpen()));
   private final SliderSetting steps = this.add(new SliderSetting("Steps", 0.05, 0.0, 1.0, 0.01, () -> this.rotate.isOpen() && this.yawStep.isOpen()));
   private final BooleanSetting checkFov = this.add(new BooleanSetting("OnlyLooking", true, () -> this.rotate.isOpen() && this.yawStep.isOpen()));
   private final SliderSetting fov = this.add(new SliderSetting("Fov", 20.0, 0.0, 360.0, 0.1, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.checkFov.getValue()));
   private final SliderSetting priority = this.add(new SliderSetting("Priority", 10, 0, 100, () -> this.rotate.isOpen() && this.yawStep.isOpen()));
   public final BooleanSetting silentDisplay = this.add(new BooleanSetting("SilentDisplay", true));
   private final SliderSetting height = this.add(new SliderSetting("Height", 1.5, 0.0, 3.0, 0.1, () -> silentDisplay.getValue()));
   private final SliderSetting scale = this.add(new SliderSetting("Scale", 1.0, 0.5, 2.0, 0.1, () -> silentDisplay.getValue()));

   private final Timer stealerTimeout = new Timer();
   private final Map<BlockPos, Long> safeBoxes = new HashMap<>();
   private final List<BlockPos> emptyBoxes = new ArrayList<>();
   private final Set<BlockPos> processedPositions = new HashSet<>();
   private final Map<Integer, Item> targetKit = new HashMap<>();

   private BlockPos openPos = null;
   private BlockPos currentOpenPos = null;
   public boolean silentScreenActive = false;
   private Vector3f currentScreenPos = null;
   public Vec3d directionVec = null;
   private BlockPos lastAutoRegearPos = null;

   private final Timer timer = new Timer();
   private final Timer dropTimer = new Timer();
   private final Timer screenOpenTimer = new Timer();
   private final Timer timeoutTimer = new Timer();
   private final Timer armorTimer = new Timer();
   private final Timer searchTimer = new Timer();
   private final Timer crystalTimer = new Timer();

   private boolean initializedScreen = false;
   private boolean armorPhaseComplete = false;
   private final List<Runnable> armorActionQueue = new ArrayList<>();
   private int armorActionIndex = 0;
   private final List<Pulse> pulses = new ArrayList<>();

   private AntiMode lockedMode = null;
   private boolean smartNeedsThrowAfterSteal = false;
   private boolean smartThrowPhase = false;

   public AntiRegear() {
      super("AntiRegear", Module.Category.Combat);
      this.setChinese("反补给");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.safeBoxes.clear();
      this.emptyBoxes.clear();
      this.processedPositions.clear();
      this.openPos = null;
      this.currentOpenPos = null;
      this.silentScreenActive = false;
      this.currentScreenPos = null;
      this.stealerTimeout.reset();
      this.initializedScreen = false;
      this.directionVec = null;
      this.armorPhaseComplete = false;
      this.armorActionQueue.clear();
      this.armorActionIndex = 0;
      this.pulses.clear();
      this.searchTimer.reset();
      this.crystalTimer.reset();
      this.lastAutoRegearPos = null;
      this.lockedMode = null;
      this.smartNeedsThrowAfterSteal = false;
      this.smartThrowPhase = false;

      if (antiMode.getValue() != AntiMode.Throw && !loadKit(kitName.getValue())) {
         this.disable();
         return;
      }
   }

   @Override
   public void onDisable() {
      if (this.silentScreenActive && mc.player != null) {
         mc.player.closeHandledScreen();
      }
      this.silentScreenActive = false;
      this.currentScreenPos = null;
      this.openPos = null;
      this.currentOpenPos = null;
      this.initializedScreen = false;
      this.directionVec = null;
      this.armorActionQueue.clear();
      this.armorActionIndex = 0;
      this.pulses.clear();
      this.lockedMode = null;
      this.smartNeedsThrowAfterSteal = false;
      this.smartThrowPhase = false;
   }

   @EventListener
   public void onPlace(PlaceBlockEvent event) {
      if (event.block instanceof ShulkerBoxBlock) {
         this.safeBoxes.put(event.blockPos, System.currentTimeMillis());
      }
   }

   @EventListener
   public void onRotate(RotationEvent event) {
      if (this.rotate.getValue() && this.shouldYawStep() && this.directionVec != null) {
         event.setTarget(this.directionVec, this.steps.getValueFloat(), this.priority.getValueFloat());
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (mc.player == null || mc.world == null) return;
      if (AutoRegear.INSTANCE != null && AutoRegear.INSTANCE.isOn()) return;

      updateSafeBoxes();

      if (checkEnemy.getValue() && !hasNearbyEnemy()) {
         if (currentOpenPos != null) {
            if (mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler) {
               mc.player.closeHandledScreen();
            }
            resetToSearching();
            this.silentScreenActive = false;
         }
         return;
      }

      if (mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler shulker) {
         handleShulkerScreen(shulker);
      } else {
         handleNoScreen();
      }
   }

   private boolean hasNearbyEnemy() {
      if (mc.world == null || mc.player == null) return false;
      for (PlayerEntity player : mc.world.getPlayers()) {
         if (player == mc.player) continue;
         if (!player.isAlive()) continue;

         try {
            if (Forever.FRIEND.isFriend(player)) continue;
         } catch (Exception e) {
            // Ignore
         }

         if (mc.player.distanceTo(player) <= enemyRange.getValue()) {
            return true;
         }
      }
      return false;
   }

   private void updateSafeBoxes() {
      long currentTime = System.currentTimeMillis();
      long safeDuration = (long) (this.safeTime.getValue() * 1000L);

      if (AutoRegear.INSTANCE != null && AutoRegear.INSTANCE.placePos != null) {
         BlockPos autoRegearPos = AutoRegear.INSTANCE.placePos;
         if (!autoRegearPos.equals(lastAutoRegearPos)) {
            this.safeBoxes.put(autoRegearPos, currentTime);
            this.lastAutoRegearPos = autoRegearPos;
         }
      }

      this.safeBoxes.entrySet().removeIf(entry -> {
         BlockPos pos = entry.getKey();
         long placedTime = entry.getValue();
         boolean timeExpired = (currentTime - placedTime) > safeDuration;
         boolean notShulker = !(mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock);
         return timeExpired || notShulker;
      });

      this.emptyBoxes.removeIf(pos ->
              !(mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock)
      );
   }

   private void handleShulkerScreen(ShulkerBoxScreenHandler shulker) {
      if (this.currentOpenPos == null || this.openPos == null) {
         return;
      }

      if (!this.initializedScreen) {
         this.screenOpenTimer.reset();
         this.initializedScreen = true;
         this.stealerTimeout.reset();

         if (antiMode.getValue() == AntiMode.Smart) {
            AntiMode decided = determineSmartMode();
            lockedMode = decided;
            smartNeedsThrowAfterSteal = (decided == AntiMode.Stealer);
            smartThrowPhase = false;
         } else {
            lockedMode = antiMode.getValue();
            smartNeedsThrowAfterSteal = false;
            smartThrowPhase = false;
         }
      }

      if (this.initializedScreen && this.screenOpenTimer.passed((long) this.closeDelay.getValue())) {
         closeAndFinish();
         return;
      }

      if (this.silentDisplay.getValue()) {
         this.silentScreenActive = true;
      }

      AntiMode effectiveMode = lockedMode != null ? lockedMode : antiMode.getValue();

      if (effectiveMode == AntiMode.Throw || smartThrowPhase) {
         handleThrowMode(shulker);
      } else {
         handleStealerMode(shulker);
      }
   }

   private void handleThrowMode(ShulkerBoxScreenHandler shulker) {
      if (this.screenOpenTimer.passed(50) && this.dropTimer.passed(50)) {
         boolean hasItems = false;
         for (int i = 0; i < 27; i++) {
            if (!shulker.getSlot(i).getStack().isEmpty()) {
               mc.interactionManager.clickSlot(shulker.syncId, i, 1, SlotActionType.THROW, mc.player);
               hasItems = true;
            }
         }

         if (!hasItems) {
            closeAndFinish();
         }
         this.dropTimer.reset();
      }
   }

   private void handleStealerMode(ShulkerBoxScreenHandler shulker) {
      if (this.regearArmor.getValue() && !armorPhaseComplete) {
         processArmorPhase(shulker);
         return;
      }

      boolean acted = false;
      if (this.returnExtras.getValue()) acted |= returnExtraItems(shulker);
      if (!acted) acted |= sortInventory(shulker);
      if (!acted) acted |= takeNeededItems(shulker);

      if (!acted) {
         boolean shulkerEmpty = true;
         for (int i = 0; i < 27; i++) {
            if (!shulker.getSlot(i).getStack().isEmpty()) {
               shulkerEmpty = false;
               break;
            }
         }

         if (shulkerEmpty && smartNeedsThrowAfterSteal && !smartThrowPhase) {
            smartThrowPhase = true;
            this.dropTimer.reset();
            this.screenOpenTimer.reset();
            stealerTimeout.reset();
            return;
         }

         if (stealerTimeout.passed(75)) {
            if (smartNeedsThrowAfterSteal && !smartThrowPhase) {
               smartThrowPhase = true;
               this.dropTimer.reset();
               this.screenOpenTimer.reset();
               stealerTimeout.reset();
            } else {
               closeAndFinish();
            }
         }
      } else {
         stealerTimeout.reset();
      }
   }

   private AntiMode determineSmartMode() {
      int totalMissingValue = 0;

      for (Map.Entry<Integer, Item> entry : targetKit.entrySet()) {
         int slot = entry.getKey();
         Item desired = entry.getValue();
         if (desired == Items.AIR || slot >= 40) continue;

         ItemStack current = mc.player.getInventory().getStack(slot);
         if (!itemsMatch(current.getItem(), desired)) {
            totalMissingValue += getItemValue(desired);
         } else if (current.isStackable() && current.getCount() < current.getMaxCount()) {
            totalMissingValue += getItemValue(desired) / 2;
         }
      }

      return totalMissingValue >= stealThreshold.getValueInt() ? AntiMode.Stealer : AntiMode.Throw;
   }

   private int getItemValue(Item item) {
      if (isWeaponItem(item)) {
         return weaponCoin.getValueInt();
      } else if (isArmorItem(item)) {
         return armorCoin.getValueInt();
      } else if (isPotionItem(item)) {
         return potCoin.getValueInt();
      } else {
         return itemCoin.getValueInt();
      }
   }

   private void closeAndFinish() {
      this.emptyBoxes.add(this.currentOpenPos);
      this.processedPositions.add(this.currentOpenPos);
      mc.player.closeHandledScreen();

      this.silentScreenActive = false;
      this.currentScreenPos = null;

      boolean shouldMine = false;
      if (mineAfter.getValue()) {
         if (antiMode.getValue() == AntiMode.Stealer) {
            shouldMine = true;
         } else if (antiMode.getValue() == AntiMode.Smart && lockedMode == AntiMode.Stealer && !smartNeedsThrowAfterSteal) {
            shouldMine = true;
         }
      }

      if (shouldMine && currentOpenPos != null) {
         if (mc.world.getBlockState(currentOpenPos).getBlock() instanceof ShulkerBoxBlock) {
            if (SpeedMine.INSTANCE != null && SpeedMine.INSTANCE.isOn()) {
               SpeedMine.INSTANCE.mine(currentOpenPos);
            }
         }
      }

      if (continuous.getValue()) {
         resetToSearching();
      } else {
         this.disable();
      }
   }

   private void handleNoScreen() {
      this.silentScreenActive = false;
      this.currentScreenPos = null;
      this.initializedScreen = false;

      if (this.openPos != null && this.timeoutTimer.passed(1000)) {
         this.openPos = null;
      }

      if (this.eatingPause.getValue() && this.isEating()) return;

      this.directionVec = null;

      if (!searchTimer.passed(100)) return;
      searchTimer.reset();

      if (this.openPos == null) {
         BlockPos target = this.findEnemyShulker();
         if (target != null && this.timer.passed(200)) {

            boolean canInteract = mc.player.getEyePos().distanceTo(target.toCenterPos()) <= interactRange.getValue();

            // 新版优先级行为判定
            if (actionPriority.getValue() == ActionPriority.Crystal) {
               // 优先水晶炸，只要能炸到，不管多近
               if (tryCrystalBomb(target)) {
                  return;
               }
               // 如果实在没水晶或者放不了，退而求其次：若距离够得着，就挖和打开
               if (canInteract) {
                  doMineAndSteal(target);
               }
            } else {
               // 优先秒挖+丢东西
               if (canInteract) {
                  doMineAndSteal(target);
               } else {
                  // 如果距离太远摸不到盒子，退而求其次：找旁边能炸到它的水晶位放炸弹
                  if (tryCrystalBomb(target)) {
                     return;
                  }
               }
            }
         }
      } else if (this.timer.passed(200)) {
         Vec3d vec = this.openPos.toCenterPos();
         if (!this.rotate.getValue() || this.faceVector(vec)) {
            BlockUtil.clickBlock(this.openPos, BlockUtil.getClickSide(this.openPos), true);
            this.timer.reset();
            if (this.rotate.getValue() && !this.shouldYawStep()) {
               Forever.ROTATION.snapBack();
            }
         }
      }
   }

   // 封装的打开盒子并配合 SpeedMine 秒挖的方法
   private void doMineAndSteal(BlockPos target) {
      Vec3d vec = target.toCenterPos();
      if (!this.rotate.getValue() || this.faceVector(vec)) {
         this.openPos = target;
         this.currentOpenPos = target;
         this.timeoutTimer.reset();

         if (this.mineSync.getValue() && SpeedMine.INSTANCE != null && SpeedMine.INSTANCE.isOn()) {
            SpeedMine.INSTANCE.mine(target);
         }

         BlockUtil.clickBlock(target, BlockUtil.getClickSide(target), true);
         this.timer.reset();
         if (this.rotate.getValue() && !this.shouldYawStep()) {
            Forever.ROTATION.snapBack();
         }
      }
   }

   // 远距离智能水晶轰炸机制
   private boolean tryCrystalBomb(BlockPos shulkerPos) {
      if (!crystalTimer.passed(250)) return true; // 防止刷包

      // 1. 查找目前已存在，且爆炸能伤到盒子的水晶，直接打爆
      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof EndCrystalEntity crystal) {
            if (canExplosionReach(crystal.getPos(), shulkerPos) && mc.player.distanceTo(crystal) <= interactRange.getValue()) {
               mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
               mc.player.swingHand(Hand.MAIN_HAND);
               crystalTimer.reset();
               return true;
            }
         }
      }

      // 2. 寻找玩家周围的最佳水晶放置位
      int crystalSlot = InventoryUtil.findItem(Items.END_CRYSTAL);
      if (crystalSlot == -1) return false;

      BlockPos bestSpot = null;
      double bestDist = Double.MAX_VALUE;
      int r = (int) interactRange.getValue() + 1;
      BlockPos playerPos = mc.player.getBlockPos();

      // 遍历玩家附近的方块（寻找能放水晶的黑曜石基岩）
      for (int x = -r; x <= r; x++) {
         for (int y = -r; y <= r; y++) {
            for (int z = -r; z <= r; z++) {
               BlockPos obsPos = playerPos.add(x, y, z);

               // 如果这块黑曜石离玩家太远，放不到，直接跳过
               if (mc.player.getEyePos().distanceTo(obsPos.toCenterPos()) > interactRange.getValue()) continue;

               if (mc.world.getBlockState(obsPos).isOf(Blocks.OBSIDIAN) || mc.world.getBlockState(obsPos).isOf(Blocks.BEDROCK)) {
                  if (canPlaceCrystalOn(obsPos)) {
                     // 模拟放置后水晶的核心位置
                     Vec3d crystalPos = obsPos.toCenterPos().add(0, 0.5, 0);

                     // 判断这个位置的水晶能否炸破潜影盒
                     if (canExplosionReach(crystalPos, shulkerPos)) {
                        // 找一个离玩家最近的（最好放的）位置
                        double dist = mc.player.getEyePos().squaredDistanceTo(obsPos.toCenterPos());
                        if (dist < bestDist) {
                           bestDist = dist;
                           bestSpot = obsPos;
                        }
                     }
                  }
               }
            }
         }
      }

      // 找到最佳位置后切水晶放置
      if (bestSpot != null) {
         int oldSlot = mc.player.getInventory().selectedSlot;
         InventoryUtil.switchToSlot(crystalSlot);
         BlockUtil.clickBlock(bestSpot, Direction.UP, false, Hand.MAIN_HAND, SwingSide.All);
         InventoryUtil.switchToSlot(oldSlot);

         crystalTimer.reset();
         return true;
      }

      return false;
   }

   // 通过距离与原版 Raycast 判断水晶爆炸能否穿透到达盒子（只要没有黑曜石完全阻挡就能炸穿潜影盒）
   private boolean canExplosionReach(Vec3d crystalPos, BlockPos shulkerPos) {
      // 水晶爆炸基础半径是 6，安全起见算 <= 6.0
      if (crystalPos.distanceTo(shulkerPos.toCenterPos()) > 6.0) return false;

      HitResult result = mc.world.raycast(new RaycastContext(
              crystalPos,
              shulkerPos.toCenterPos(),
              RaycastContext.ShapeType.COLLIDER,
              RaycastContext.FluidHandling.NONE,
              mc.player
      ));

      if (result.getType() == HitResult.Type.BLOCK) {
         BlockPos hitPos = ((BlockHitResult) result).getBlockPos();
         // 如果直接射中了盒子，绝对炸得到
         if (hitPos.equals(shulkerPos)) return true;

         // 如果中间被高抗爆方块（黑曜石/基岩/重生锚）挡住，则炸不透
         if (mc.world.getBlockState(hitPos).isOf(Blocks.OBSIDIAN) ||
                 mc.world.getBlockState(hitPos).isOf(Blocks.BEDROCK) ||
                 mc.world.getBlockState(hitPos).isOf(Blocks.RESPAWN_ANCHOR)) {
            return false;
         }
      }
      // 没被方块挡住或者挡住的是泥土之类的低防爆方块，皆可炸透极弱的潜影盒
      return true;
   }

   private boolean canPlaceCrystalOn(BlockPos pos) {
      BlockPos up1 = pos.up();
      BlockPos up2 = pos.up(2);
      return mc.world.isAir(up1) && mc.world.isAir(up2) &&
              mc.world.getOtherEntities(null, new Box(up1)).isEmpty();
   }

   private void resetToSearching() {
      this.openPos = null;
      this.currentOpenPos = null;
      this.armorActionQueue.clear();
      this.armorActionIndex = 0;
      this.armorPhaseComplete = false;
      this.lockedMode = null;
      this.smartNeedsThrowAfterSteal = false;
      this.smartThrowPhase = false;
      this.searchTimer.reset();
   }

   private boolean isEating() {
      if (mc.player == null) return false;
      if (mc.player.isUsingItem() && isFood(mc.player.getActiveItem())) return true;
      if (mc.options.useKey.isPressed() && (isFood(mc.player.getMainHandStack()) || isFood(mc.player.getOffHandStack()))) {
         return true;
      }
      return false;
   }

   private boolean isFood(ItemStack stack) {
      if (stack == null || stack.isEmpty()) return false;
      return stack.contains(DataComponentTypes.FOOD) || stack.getItem() instanceof PotionItem || stack.getItem() == Items.MILK_BUCKET;
   }

   private boolean shouldYawStep() {
      return (this.whenElytra.getValue()
              || !mc.player.isFallFlying()
              && (!ElytraFlight.INSTANCE.isOn() || !ElytraFlight.INSTANCE.isFallFlying()))
              && this.yawStep.getValue()
              && !Velocity.INSTANCE.noRotation();
   }

   private boolean faceVector(Vec3d directionVec) {
      if (directionVec == null) {
         return false;
      } else if (!this.shouldYawStep()) {
         Forever.ROTATION.lookAt(directionVec);
         return true;
      } else {
         this.directionVec = directionVec;
         return Forever.ROTATION.inFov(directionVec, this.fov.getValueFloat()) ? true : !this.checkFov.getValue();
      }
   }

   private BlockPos findEnemyShulker() {
      BlockPos bestPos = null;
      double bestDist = this.range.getValue() * this.range.getValue();

      for (BlockPos pos : BlockUtil.getSphere((float) this.range.getValue())) {
         if (!(mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock))
            continue;
         if (this.safeBoxes.containsKey(pos)) continue;
         if (this.emptyBoxes.contains(pos)) continue;
         if (this.processedPositions.contains(pos)) continue;
         if (AutoRegear.INSTANCE != null && pos.equals(AutoRegear.INSTANCE.placePos))
            continue;

         if (!mc.world.isAir(pos.up()) && !BlockUtil.canReplace(pos.up()))
            continue;

         boolean friendNearby = false;
         double friendRangeSq = friendRange.getValue() * friendRange.getValue();
         for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (Forever.FRIEND.isFriend(player)
                    && player.squaredDistanceTo(pos.toCenterPos()) <= friendRangeSq) {
               friendNearby = true;
               break;
            }
         }
         if (friendNearby) continue;

         double dist = mc.player.squaredDistanceTo(pos.toCenterPos());
         if (dist <= bestDist) {
            bestDist = dist;
            bestPos = pos;
         }
      }
      return bestPos;
   }

   private boolean isWeaponItem(Item item) {
      return item == Items.NETHERITE_SWORD || item == Items.DIAMOND_SWORD
              || item == Items.NETHERITE_PICKAXE || item == Items.DIAMOND_PICKAXE
              || item == Items.NETHERITE_AXE || item == Items.DIAMOND_AXE
              || item == Items.NETHERITE_SHOVEL || item == Items.DIAMOND_SHOVEL;
   }

   private boolean isArmorItem(Item item) {
      return item == Items.NETHERITE_HELMET || item == Items.NETHERITE_CHESTPLATE
              || item == Items.NETHERITE_LEGGINGS || item == Items.NETHERITE_BOOTS
              || item == Items.DIAMOND_HELMET || item == Items.DIAMOND_CHESTPLATE
              || item == Items.DIAMOND_LEGGINGS || item == Items.DIAMOND_BOOTS
              || item == Items.ELYTRA;
   }

   private boolean isPotionItem(Item item) {
      return item instanceof PotionItem || item == Items.POTION
              || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION;
   }

   private boolean itemsMatch(Item item1, Item item2) {
      if (item1 == item2) return true;
      boolean p1 = item1 == Items.PISTON || item1 == Items.STICKY_PISTON;
      boolean p2 = item2 == Items.PISTON || item2 == Items.STICKY_PISTON;
      return p1 && p2;
   }

   private boolean strictMatch(Item item1, Item item2) {
      return item1 == item2;
   }

   private boolean isShulkerBox(ItemStack stack) {
      return !stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
   }

   private void processArmorPhase(ShulkerBoxScreenHandler shulker) {
      if (armorActionQueue.isEmpty() && armorActionIndex == 0) {
         buildArmorActionQueue(shulker);
         armorTimer.reset();

         if (armorActionQueue.isEmpty()) {
            armorPhaseComplete = true;
            return;
         }
      }

      if (armorActionIndex >= armorActionQueue.size()) {
         armorPhaseComplete = true;
         armorActionQueue.clear();
         armorActionIndex = 0;
         timeoutTimer.reset();
         return;
      }

      if (armorTimer.passed(50)) {
         armorActionQueue.get(armorActionIndex).run();
         armorActionIndex++;
         armorTimer.reset();
         timeoutTimer.reset();
      }
   }

   private void buildArmorActionQueue(ShulkerBoxScreenHandler shulker) {
      armorActionQueue.clear();

      for (int i = 36; i < 40; i++) {
         if (!targetKit.containsKey(i)) continue;
         Item desired = targetKit.get(i);
         if (desired == Items.AIR) continue;

         ItemStack current = mc.player.getInventory().getStack(i);
         if (current.getItem() == desired) continue;

         int slotId = -1;
         for (int j = 0; j < 27; j++) {
            if (shulker.getSlot(j).getStack().getItem() == desired) {
               slotId = j;
               break;
            }
         }
         if (slotId == -1) continue;

         final int finalSlotId = slotId;
         final int hotbar = mc.player.getInventory().selectedSlot;

         armorActionQueue.add(() ->
                 windowClick(shulker.syncId, finalSlotId, hotbar, SlotActionType.SWAP));

         armorActionQueue.add(() ->
                 mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(
                         Hand.MAIN_HAND, 0,
                         Forever.ROTATION.getLastYaw(),
                         Forever.ROTATION.getLastPitch())));

         armorActionQueue.add(() ->
                 windowClick(shulker.syncId, finalSlotId, hotbar, SlotActionType.SWAP));
      }
   }

   private boolean returnExtraItems(ShulkerBoxScreenHandler shulker) {
      boolean acted = false;
      for (int playerSlot = 0; playerSlot < 36; playerSlot++) {
         int windowSlot = playerSlot < 9 ? 54 + playerSlot : 27 + (playerSlot - 9);
         ItemStack currentStack = shulker.getSlot(windowSlot).getStack();
         if (currentStack.isEmpty()) continue;
         Item currentItem = currentStack.getItem();
         Item desiredItem = targetKit.getOrDefault(playerSlot, Items.AIR);
         if (itemsMatch(currentItem, desiredItem)) continue;
         if (isItemNeededElsewhere(currentItem, playerSlot)) continue;
         if (currentItem instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock)
            continue;
         int emptyShulkerSlot = findEmptyShulkerSlot(shulker);
         if (emptyShulkerSlot != -1) {
            windowClick(shulker.syncId, windowSlot, 0, SlotActionType.QUICK_MOVE);
            acted = true;
         }
      }
      return acted;
   }

   private boolean sortInventory(ShulkerBoxScreenHandler shulker) {
      boolean acted = false;
      for (int playerSlot = 0; playerSlot < 36; playerSlot++) {
         Item desiredItem = targetKit.getOrDefault(playerSlot, Items.AIR);
         if (desiredItem == Items.AIR) continue;
         int windowSlot = playerSlot < 9 ? 54 + playerSlot : 27 + (playerSlot - 9);
         ItemStack currentStack = shulker.getSlot(windowSlot).getStack();
         if (itemsMatch(currentStack.getItem(), desiredItem)) continue;
         int sourcePlayerSlot = findItemInInventory(shulker, desiredItem, playerSlot);
         if (sourcePlayerSlot != -1) {
            int sourceWindowSlot = sourcePlayerSlot < 9 ? 54 + sourcePlayerSlot : 27 + (sourcePlayerSlot - 9);
            windowClick(shulker.syncId, windowSlot, 0, SlotActionType.PICKUP);
            windowClick(shulker.syncId, sourceWindowSlot, 0, SlotActionType.PICKUP);
            windowClick(shulker.syncId, windowSlot, 0, SlotActionType.PICKUP);
            acted = true;
         }
      }
      return acted;
   }

   private boolean takeNeededItems(ShulkerBoxScreenHandler shulker) {
      boolean acted = false;
      for (Map.Entry<Integer, Item> entry : targetKit.entrySet()) {
         int playerSlot = entry.getKey();
         if (playerSlot >= 36) continue;
         Item desired = entry.getValue();
         if (desired == Items.AIR) continue;
         int windowSlot = playerSlot < 9 ? 54 + playerSlot : 27 + (playerSlot - 9);
         ItemStack currentStack = shulker.getSlot(windowSlot).getStack();

         if (!currentStack.isEmpty()) {
            if (itemsMatch(currentStack.getItem(), desired)) {
               if (currentStack.getCount() >= currentStack.getMaxCount()) continue;

               int shulkerSlot = findStrictMatchingItemInShulker(shulker, currentStack);
               if (shulkerSlot == -1) continue;

               windowClick(shulker.syncId, shulkerSlot, 0, SlotActionType.PICKUP);
               windowClick(shulker.syncId, windowSlot, 0, SlotActionType.PICKUP);
               if (!shulker.getCursorStack().isEmpty()) {
                  windowClick(shulker.syncId, shulkerSlot, 0, SlotActionType.PICKUP);
               }
               acted = true;
            }
            continue;
         }

         int shulkerSlot = findBestItemInShulker(shulker, desired);
         if (shulkerSlot != -1) {
            windowClick(shulker.syncId, shulkerSlot, 0, SlotActionType.PICKUP);
            windowClick(shulker.syncId, windowSlot, 0, SlotActionType.PICKUP);
            if (!shulker.getCursorStack().isEmpty()) {
               windowClick(shulker.syncId, shulkerSlot, 0, SlotActionType.PICKUP);
            }
            acted = true;
         }
      }
      return acted;
   }

   private boolean isItemNeededElsewhere(Item item, int currentSlot) {
      for (Map.Entry<Integer, Item> entry : targetKit.entrySet()) {
         int slotIndex = entry.getKey();
         if (slotIndex >= 36 || slotIndex == currentSlot) continue;
         Item needed = entry.getValue();
         if (itemsMatch(needed, item)) {
            int windowSlot = slotIndex < 9 ? 54 + slotIndex : 27 + (slotIndex - 9);
            if (mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler shulker) {
               ItemStack stackInSlot = shulker.getSlot(windowSlot).getStack();
               if (!itemsMatch(stackInSlot.getItem(), needed)) return true;
            }
         }
      }
      return false;
   }

   private int findItemInInventory(ShulkerBoxScreenHandler shulker, Item item, int ignoreSlot) {
      for (int i = 0; i < 36; i++) {
         if (i == ignoreSlot) continue;
         int windowSlot = i < 9 ? 54 + i : 27 + (i - 9);
         ItemStack stack = shulker.getSlot(windowSlot).getStack();
         if (itemsMatch(stack.getItem(), item)) {
            Item neededAtSource = targetKit.getOrDefault(i, Items.AIR);
            if (!itemsMatch(neededAtSource, item)) return i;
         }
      }
      return -1;
   }

   private int findBestItemInShulker(ShulkerBoxScreenHandler shulker, Item desired) {
      int bestSlot = -1;
      int bestCount = -1;
      for (int i = 0; i < 27; i++) {
         ItemStack stack = shulker.getSlot(i).getStack();
         if (itemsMatch(stack.getItem(), desired)) {
            if (stack.getCount() > bestCount) {
               bestCount = stack.getCount();
               bestSlot = i;
            }
         }
      }
      return bestSlot;
   }

   private int findStrictMatchingItemInShulker(ShulkerBoxScreenHandler shulker, ItemStack targetStack) {
      for (int i = 0; i < 27; i++) {
         ItemStack shulkerStack = shulker.getSlot(i).getStack();
         if (strictMatch(shulkerStack.getItem(), targetStack.getItem())
                 && ItemStack.areItemsAndComponentsEqual(shulkerStack, targetStack)) {
            return i;
         }
      }
      return -1;
   }

   private int findEmptyShulkerSlot(ShulkerBoxScreenHandler shulker) {
      for (int i = 0; i < 27; i++) {
         if (shulker.getSlot(i).getStack().isEmpty()) return i;
      }
      return -1;
   }

   private boolean loadKit(String name) {
      targetKit.clear();
      try {
         File file = Manager.getFile(name + ".kit");
         if (!file.exists()) return false;
         BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
         String line;
         while ((line = reader.readLine()) != null) {
            String[] parts = line.split(":");
            if (parts.length >= 3) {
               int slot = Integer.parseInt(parts[0]);
               if (slot == 40) continue;
               String idStr = parts[1] + ":" + parts[2];
               targetKit.put(slot, Registries.ITEM.get(Identifier.of(idStr)));
            }
         }
         reader.close();
         return true;
      } catch (Exception e) {
         return false;
      }
   }

   private void windowClick(int syncId, int slotId, int button, SlotActionType type) {
      mc.interactionManager.clickSlot(syncId, slotId, button, type, mc.player);
      if (slotId >= 0 && slotId < 27) {
         pulses.add(new Pulse(slotId));
      }
   }

   @EventListener
   public void onRender3D(Render3DEvent event) {
      currentScreenPos = null;
      if (!silentScreenActive || !silentDisplay.getValue() || mc.player == null) return;
      if (!(mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler)) return;

      BlockPos renderPos = currentOpenPos != null ? currentOpenPos : openPos;
      if (renderPos == null) return;

      Vec3d worldCenter = renderPos.toCenterPos().add(0, height.getValue(), 0);
      currentScreenPos = ProjectionUtils.project(worldCenter);
   }

   @EventListener
   public void onRender2D(Render2DEvent event) {
      if (!silentScreenActive) return;
      if (currentScreenPos == null || !silentDisplay.getValue()) return;
      if (mc.player == null) return;
      if (!(mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler handler)) return;

      DrawContext context = event.drawContext;
      MatrixStack matrices = context.getMatrices();

      float screenX = currentScreenPos.x;
      float screenY = currentScreenPos.y;
      if (screenX < -200 || screenX > mc.getWindow().getScaledWidth() + 200
              || screenY < -200 || screenY > mc.getWindow().getScaledHeight() + 200) return;

      int gridWidth = 18 * 9;
      int gridHeight = 18 * 3;
      int padding = 6;
      float totalWidth = gridWidth + padding * 2;
      float totalHeight = gridHeight + padding * 2;
      float currentScale = (float) scale.getValue();

      matrices.push();
      matrices.translate(screenX, screenY, 0);
      matrices.scale(currentScale, currentScale, 1.0f);

      int startX = (int) (-totalWidth / 2f);
      int startY = (int) (-totalHeight / 2f);
      int endX = (int) (startX + totalWidth);
      int endY = (int) (startY + totalHeight);

      int bgColor = new Color(20, 20, 20, 160).getRGB();
      int borderColor = new Color(100, 100, 100, 200).getRGB();

      context.fill(startX, startY, endX, endY, bgColor);
      context.drawBorder(startX, startY, (int) totalWidth, (int) totalHeight, borderColor);

      for (int i = 0; i < 27; i++) {
         ItemStack stack = handler.getSlot(i).getStack();
         int row = i / 9;
         int col = i % 9;
         int slotX = startX + padding + col * 18;
         int slotY = startY + padding + row * 18;

         if (!stack.isEmpty()) {
            context.drawItem(stack, slotX, slotY);
            context.drawItemInSlot(mc.textRenderer, stack, slotX, slotY);
         }
         drawPulse2D(matrices, slotX + 9, slotY + 9, i);
      }
      matrices.pop();

      pulses.removeIf(p -> p.timer.getMs() > p.maxTime);
   }

   private void drawPulse2D(MatrixStack ms, float centerX, float centerY, int slotId) {
      for (Pulse p : pulses) {
         if (p.slotId == slotId) {
            float progress = Math.min(1.0f, (float) p.timer.getMs() / p.maxTime);
            float radius = progress * 10.0f;
            int maxAlpha = 80;
            int alpha = (int) ((1.0f - progress) * maxAlpha);
            if (alpha > 0) {
               Color pulseColor = new Color(255, 255, 255, alpha);
               Render2DUtil.drawCircle(ms, centerX, centerY, radius, pulseColor, 12);
            }
         }
      }
   }

   private static class Pulse {
      public final int slotId;
      public final Timer timer;
      public final long maxTime;

      public Pulse(int slotId) {
         this.slotId = slotId;
         this.timer = new Timer();
         this.timer.reset();
         this.maxTime = 400;
      }
   }
}