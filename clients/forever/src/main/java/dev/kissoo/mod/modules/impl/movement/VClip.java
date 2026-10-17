package dev.kissoo.mod.modules.impl.movement;

import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.events.impl.TickEvent;
import dev.kissoo.api.utils.player.EntityUtil;
import dev.kissoo.api.utils.player.InventoryUtil;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.api.utils.path.TPUtils;
import dev.kissoo.Forever;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

public class VClip extends Module {
   private final EnumSetting<VClip.Mode> mode = this.add(new EnumSetting<>("Mode", VClip.Mode.Jump));
   private final SliderSetting sameCommandHeight = this.add(new SliderSetting(
           "Height",
           3.0,
           1.0,
           5.0,
           1.0,
           () -> mode.getValue() == Mode.SameCommand
   )).setSuffix(" blocks");

   // ================== 风弹设置 ==================
   private final BooleanSetting useWindCharge = this.add(new BooleanSetting("UseWindCharge", false));
   private final SliderSetting windChargeDelay = this.add(new SliderSetting(
           "WindChargeDelay",
           1.0,
           0.0,
           10.0,
           1.0,
           useWindCharge::getValue
   )).setSuffix(" ticks");
   // ============================================

   private final BooleanSetting auto = this.add(new BooleanSetting("Auto", false));
   private final SliderSetting autoCheckRange = this.add(new SliderSetting(
           "CheckRange",
           1.0,
           1.0,
           3.0,
           1.0,
           auto::getValue
   ));

   private final SliderSetting autoDelay = this.add(new SliderSetting(
           "Delay",
           20,
           0,
           100,
           5,
           auto::getValue
   )).setSuffix(" ticks");

   private int autoTickCounter = 0;
   private boolean wasBlockedAbove = false;
   private boolean hasExecutedOnce = false;

   // 用于处理丢风弹后的延迟跳跃
   private int pendingClipTicks = -1;

   public VClip() {
      super("VClip", Module.Category.Movement);
      this.setChinese("纵向穿墙");
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (!event.isPre() || mc.player == null) {
         return;
      }

      // 处理风弹延迟逻辑
      if (pendingClipTicks > 0) {
         pendingClipTicks--;
         if (pendingClipTicks == 0) {
            doActualClip();
            pendingClipTicks = -1;
         }
         return; // 等待穿墙期间暂停自动检测
      } else if (pendingClipTicks == 0) {
         doActualClip();
         pendingClipTicks = -1;
         return;
      }

      if (auto.getValue() && this.isOn()) {
         if (autoTickCounter++ < autoDelay.getValueInt()) {
            return;
         }
         autoTickCounter = 0;
         boolean shouldClip = shouldAutoClip();
         if (shouldClip && !wasBlockedAbove) {
            startVClipSequence();
         }

         wasBlockedAbove = shouldClip;
      }
   }

   private boolean shouldAutoClip() {
      if (mc.world == null || mc.player == null) {
         return false;
      }

      BlockPos playerPos = new BlockPos(
              (int)Math.floor(mc.player.getX()),
              (int)Math.floor(mc.player.getY()),
              (int)Math.floor(mc.player.getZ())
      );

      int checkRange = (int)autoCheckRange.getValue();
      BlockPos firstBlockPos = playerPos.up(2);
      BlockState firstBlockState = mc.world.getBlockState(firstBlockPos);
      if (!isSolidBlock(firstBlockState, firstBlockPos)) {
         return false;
      }
      boolean hasBlocksInRange = false;
      for (int i = 1; i <= checkRange; i++) {
         BlockPos checkPos = playerPos.up(i);
         BlockState state = mc.world.getBlockState(checkPos);
         if (isSolidBlock(state, checkPos)) {
            hasBlocksInRange = true;
            break;
         }
      }

      if (!hasBlocksInRange) {
         return false;
      }

      for (int i = checkRange + 1; i <= checkRange + 2; i++) {
         BlockPos checkPos = playerPos.up(i);
         BlockState state = mc.world.getBlockState(checkPos);
         if (!isAirBlock(state, checkPos)) {
            return false;
         }
      }

      return true;
   }

   private boolean isAirBlock(BlockState state, BlockPos pos) {
      if (state.isAir()) {
         return true;
      }

      if (state.getBlock() == Blocks.WATER ||
              state.getBlock() == Blocks.LAVA ||
              !state.getFluidState().isEmpty()) {
         return true;
      }

      if (state.getBlock() instanceof net.minecraft.block.TransparentBlock ||
              state.getBlock() instanceof net.minecraft.block.AbstractPlantBlock ||
              state.getBlock() instanceof net.minecraft.block.FlowerBlock ||
              state.getBlock() instanceof net.minecraft.block.TorchBlock ||
              state.getBlock() instanceof net.minecraft.block.RedstoneTorchBlock ||
              state.getBlock() instanceof net.minecraft.block.LeverBlock ||
              state.getBlock() instanceof net.minecraft.block.ButtonBlock) {
         return true;
      }
      if (state.getBlock() == Blocks.GLASS ||
              state.getBlock() == Blocks.GLASS_PANE ||
              state.getBlock() == Blocks.ICE ||
              state.getBlock() == Blocks.PACKED_ICE ||
              state.getBlock() == Blocks.BLUE_ICE ||
              state.getBlock() == Blocks.BARRIER) {
         return false;
      }
      return !state.blocksMovement();
   }

   private boolean isSolidBlock(BlockState state, BlockPos pos) {
      if (state.isAir()) {
         return false;
      }

      if (state.getBlock() == Blocks.WATER ||
              state.getBlock() == Blocks.LAVA ||
              !state.getFluidState().isEmpty()) {
         return false;
      }

      if (state.blocksMovement()) {
         return true;
      }

      if (state.getBlock() == Blocks.GLASS ||
              state.getBlock() == Blocks.GLASS_PANE ||
              state.getBlock() == Blocks.ICE ||
              state.getBlock() == Blocks.PACKED_ICE ||
              state.getBlock() == Blocks.BLUE_ICE ||
              state.getBlock() == Blocks.BARRIER) {
         return true;
      }

      return false;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (mc.player == null || mc.getNetworkHandler() == null) {
         return;
      }

      if (!auto.getValue() && this.isOn() && !hasExecutedOnce) {
         hasExecutedOnce = true;
         startVClipSequence();
      }
   }

   // ================== 风弹穿墙序列控制 ==================

   /**
    * 开始 VClip 序列。如果启用了风弹则丢出风弹并按设置延迟执行。
    */
   private void startVClipSequence() {
      if (useWindCharge.getValue() && throwWindChargeDown()) {
         pendingClipTicks = windChargeDelay.getValueInt();
         if (pendingClipTicks <= 0) {
            doActualClip();
         }
      } else {
         // 未开启风弹或没有风弹时，直接穿墙
         doActualClip();
      }
   }

   /**
    * 向脚下扔出风弹的逻辑 (移植自 MaceSpoof)
    */
   private boolean throwWindChargeDown() {
      if (mc.player == null) return false;

      int windChargeSlot = -1;
      boolean fromInventory = false;

      if (mc.player.getMainHandStack().getItem() == Items.WIND_CHARGE) {
         windChargeSlot = mc.player.getInventory().selectedSlot;
      } else if ((windChargeSlot = InventoryUtil.findItem(Items.WIND_CHARGE)) != -1) {
         // 在快捷栏中找到
      } else if ((windChargeSlot = InventoryUtil.findItemInventorySlotFromZero(Items.WIND_CHARGE)) != -1) {
         fromInventory = true; // 在背包中找到
      }

      if (windChargeSlot == -1) return false;

      float yaw = mc.player.getYaw();
      float pitch = 90.0F; // 向正下方看

      // 强行锁定视角防止发包异常
      Forever.ROTATION.snapAt(yaw, pitch);

      if (mc.player.getMainHandStack().getItem() == Items.WIND_CHARGE) {
         sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitch));
         mc.player.swingHand(Hand.MAIN_HAND);
      } else if (fromInventory) {
         InventoryUtil.inventorySwap(windChargeSlot, mc.player.getInventory().selectedSlot);
         sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitch));
         mc.player.swingHand(Hand.MAIN_HAND);
         InventoryUtil.inventorySwap(windChargeSlot, mc.player.getInventory().selectedSlot);
         EntityUtil.syncInventory();
      } else {
         int oldSlot = mc.player.getInventory().selectedSlot;
         InventoryUtil.switchToSlot(windChargeSlot);
         sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitch));
         mc.player.swingHand(Hand.MAIN_HAND);
         InventoryUtil.switchToSlot(oldSlot);
      }

      Forever.ROTATION.snapBack();
      return true;
   }

   /**
    * 真正执行穿墙及关闭模块的逻辑
    */
   private void doActualClip() {
      executeVClip();

      // 如果是手动模式，穿墙完成后关闭模块
      if (!auto.getValue()) {
         new Thread(() -> {
            try {
               Thread.sleep(10);
            } catch (InterruptedException e) {
               e.printStackTrace();
            }
            if (this.isOn() && !auto.getValue()) {
               this.disable();
            }
         }).start();
      }
   }

   // ======================================================

   private void executeVClip() {
      if (mc.player == null || mc.getNetworkHandler() == null) {
         return;
      }

      switch (this.mode.getValue()) {
         case Glitch:
            executeGlitchMode();
            break;
         case Teleport:
            executeTeleportMode();
            break;
         case Jump:
            executeJumpMode();
            break;
         case SameCommand:
            executeSameCommandMode();
            break;
         case Tne:
            executeTneMode();
            break;
      }
   }

   private void executeGlitchMode() {
      double posX = mc.player.getX();
      double posY = Math.round(mc.player.getY());
      double posZ = mc.player.getZ();
      boolean onGround = mc.player.isOnGround();
      mc.getNetworkHandler().sendPacket(new PositionAndOnGround(posX, posY, posZ, onGround));
      double halfY = 0.005;
      posY -= halfY;
      mc.player.setPosition(posX, posY, posZ);
      mc.getNetworkHandler().sendPacket(new PositionAndOnGround(posX, posY, posZ, onGround));
      posY -= halfY * 300.0;
      mc.player.setPosition(posX, posY, posZ);
      mc.getNetworkHandler().sendPacket(new PositionAndOnGround(posX, posY, posZ, onGround));
   }

   private void executeTeleportMode() {
      mc.player.setPosition(mc.player.getX(), mc.player.getY() + 3.0, mc.player.getZ());
      mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true));
   }

   private void executeJumpMode() {
      mc.getNetworkHandler()
              .sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.4199999868869781, mc.player.getZ(), false));
      mc.getNetworkHandler()
              .sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.7531999805212017, mc.player.getZ(), false));
      mc.player.setPosition(mc.player.getX(), mc.player.getY() + 1.0, mc.player.getZ());
      mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true));
   }

   private void executeSameCommandMode() {
      double currentX = mc.player.getX();
      double currentY = mc.player.getY();
      double currentZ = mc.player.getZ();
      double height = sameCommandHeight.getValue();
      Vec3d targetPos = new Vec3d(currentX, currentY + height, currentZ);
      TPUtils.newTeleport(targetPos);
      mc.player.setPosition(targetPos.x, targetPos.y, targetPos.z);
   }

   private void executeTneMode() {
      BlockPos playerPos = mc.player.getBlockPos();
      BlockPos headPos = playerPos.up(2);
      java.util.List<BlockPos> candidatePositions = new java.util.ArrayList<>();

      for (int x = -1; x <= 1; x++) {
         for (int z = -1; z <= 1; z++) {
            if (x == 0 && z == 0) continue;
            BlockPos checkPos = headPos.add(x, 0, z);
            if (hasBlock(checkPos)) {
               candidatePositions.add(checkPos.up());
            }
         }
      }
      if (hasBlock(headPos)) {
         candidatePositions.add(headPos.up());
      }
      for (BlockPos pos : candidatePositions) {
         if (!hasBlock(pos) && !hasBlock(pos.up()) && !hasBlock(pos.up(2))) {
            teleport(pos);
            disable();
            return;
         }
      }
   }

   private boolean hasBlock(BlockPos pos) {
      return !mc.world.isAir(pos);
   }

   private void teleport(BlockPos pos) {
      if (hasBlock(pos) || hasBlock(pos.up()) || hasBlock(pos.up(2))) {
         return;
      }
      double x = pos.getX() + 0.5;
      double y = pos.getY();
      double z = pos.getZ() + 0.5;
      mc.player.setPosition(x, y, z);
      mc.getNetworkHandler().sendPacket(new PositionAndOnGround(x, y, z, true));
   }

   @Override
   public void onEnable() {
      autoTickCounter = 0;
      wasBlockedAbove = false;
      hasExecutedOnce = false;
      pendingClipTicks = -1; // 初始化重置延迟

      if (auto.getValue()) {
         return;
      }
   }

   @Override
   public void onDisable() {
      wasBlockedAbove = false;
      hasExecutedOnce = false;
      pendingClipTicks = -1;
   }

   @Override
   public String getInfo() {
      if (auto.getValue()) {
         return "Auto";
      }
      return mode.getValue().toString();
   }

   public enum Mode {
      Glitch,
      Teleport,
      Jump,
      SameCommand,
      Tne
   }
}