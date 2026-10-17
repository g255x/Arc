package dev.kissoo.mod.modules.impl.movement;

import dev.kissoo.Forever;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.utils.combat.CombatUtil;
import dev.kissoo.api.utils.player.EntityUtil;
import dev.kissoo.api.utils.player.InventoryUtil;
import dev.kissoo.api.utils.player.MovementUtil;
import dev.kissoo.api.utils.path.TPUtils;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * 风弹传送：丢出风弹把自己崩起后，一次性传送到前方 X 格处完整方块顶上。
 * 逻辑仿照 Fly 模块的 WindJump 模式（风弹起跳 + 等爆炸 + 动作），
 * 只是把「强制向前加速」换成了「一次 TP」。
 *
 * 锤子模式（MaceMode）：开启且手里拿锤子时，同样丢风弹，但等爆炸后传送到
 * 最近玩家（TargetRange 内）周围 X 格半径内的完整方块顶上。
 */
public class WindTP extends Module {
   public static WindTP INSTANCE;

   // 丢出风弹后的等待时间 (等爆炸)
   private final SliderSetting throwDelay = this.add(new SliderSetting("ThrowDelay", 300.0, 0.0, 2000.0, 10.0).setSuffix(" ms"));
   // 普通模式：向前传送的格数；锤子模式：目标周围半径
   private final SliderSetting distance = this.add(new SliderSetting("Distance", 5.0, 1.0, 30.0, 1.0));

   // 锤子模式：手里拿锤子开启时，传送到最近玩家附近
   private final BooleanSetting maceMode = this.add(new BooleanSetting("MaceMode", false));
   // 最远距离：搜索最近玩家的范围
   private final SliderSetting targetRange = this.add(new SliderSetting("TargetRange", 15.0, 1.0, 50.0, 1.0, () -> this.maceMode.getValue()));

   private long boostStartTime = 0L;
   private boolean hasThrown = false;
   private PlayerEntity maceTarget = null;

   public WindTP() {
      super("WindTP", Module.Category.Movement);
      this.setChinese("风弹传送");
      INSTANCE = this;
   }

   @Override
   public String getInfo() {
      return String.valueOf(this.distance.getValueInt());
   }

   @Override
   public void onEnable() {
      if (nullCheck()) return;

      this.hasThrown = false;
      this.boostStartTime = 0L;
      this.maceTarget = null;

      // 锤子模式：手里拿锤子 → 先锁定最近玩家，等风弹爆炸后再传送到目标附近；
      // 找不到目标则回退到标准向前传送
      if (this.maceMode.getValue() && mc.player.getMainHandStack().isOf(Items.MACE)) {
         this.maceTarget = CombatUtil.getClosestEnemy(this.targetRange.getValue());
      }

      // 丢风弹（两种模式都丢），如果没有风弹则直接关闭
      if (!throwWindChargeDown()) {
         this.disable();
         return;
      }

      this.hasThrown = true;
      this.boostStartTime = System.currentTimeMillis();
   }

   @Override
   public void onDisable() {
      this.hasThrown = false;
      this.boostStartTime = 0L;
      this.maceTarget = null;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (mc.player == null || mc.world == null || !this.hasThrown || this.boostStartTime == 0L) return;

      // 等爆炸，然后一次性传送
      if (System.currentTimeMillis() - this.boostStartTime >= (long) this.throwDelay.getValue()) {
         if (this.maceTarget != null) {
            if (!this.teleportNearTarget(this.maceTarget)) {
               sendMessage("§c[风弹传送] 目标附近没有可站立的完整方块");
            }
         } else {
            this.teleportForward();
         }
         this.disable();
      }
   }

   /**
    * 传送到前方 X 格处完整方块顶上
    */
   private void teleportForward() {
      if (mc.player == null || mc.world == null) return;

      double dist = this.distance.getValue();
      float yaw = mc.player.getYaw();
      double rad = Math.toRadians(yaw);

      // 前方 X 格的水平目标列
      int tx = MathHelper.floor(mc.player.getX() - Math.sin(rad) * dist);
      int tz = MathHelper.floor(mc.player.getZ() + Math.cos(rad) * dist);

      BlockPos stand = findStandable(tx, tz, mc.player.getBlockY() + 3);
      if (stand != null) teleportToBlockTop(stand);
   }

   /**
    * 传送到目标玩家周围 X 格半径内的完整方块顶上
    */
   private boolean teleportNearTarget(PlayerEntity target) {
      if (mc.player == null || mc.world == null || target == null) return false;

      int radius = this.distance.getValueInt();
      BlockPos tPos = target.getBlockPos();
      int fromY = tPos.getY() + 2;

      // 从内圈往外扫，找最近的完整实心方块（排除目标自己站的那一格）
      for (int r = 1; r <= radius; r++) {
         for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
               if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue; // 只扫当前圈
               BlockPos stand = findStandable(tPos.getX() + dx, tPos.getZ() + dz, fromY);
               if (stand != null) {
                  teleportToBlockTop(stand);
                  return true;
               }
            }
         }
      }
      return false;
   }

   /**
    * 从 fromY 往下扫，找该列第一个完整实心方块，且其上方可站立
    */
   private BlockPos findStandable(int x, int z, int fromY) {
      for (int y = fromY; y >= mc.world.getBottomY(); y--) {
         BlockPos block = new BlockPos(x, y, z);
         if (mc.world.getBlockState(block).isFullCube(mc.world, block)
                 && mc.world.getBlockState(block.up()).isReplaceable()) {
            return block;
         }
      }
      return null;
   }

   /**
    * 传送到方块顶上并清空速度
    */
   private void teleportToBlockTop(BlockPos block) {
      Vec3d dest = new Vec3d(block.getX() + 0.5, block.getY() + 1, block.getZ() + 0.5);
      TPUtils.newTeleport(dest);
      mc.player.setPosition(dest.x, dest.y, dest.z);
      MovementUtil.setMotionX(0);
      MovementUtil.setMotionY(0);
      MovementUtil.setMotionZ(0);
   }

   /**
    * 向脚下丢弃风弹 (内部处理了物品栏交互逻辑) — 同 Fly.throwWindChargeDown()
    */
   private boolean throwWindChargeDown() {
      int windChargeSlot = -1;
      boolean fromInventory = false;

      if (mc.player.getMainHandStack().getItem() == Items.WIND_CHARGE) {
         windChargeSlot = mc.player.getInventory().selectedSlot;
      } else if ((windChargeSlot = InventoryUtil.findItem(Items.WIND_CHARGE)) != -1) {
         // 在快捷栏找到
      } else if ((windChargeSlot = InventoryUtil.findItemInventorySlotFromZero(Items.WIND_CHARGE)) != -1) {
         fromInventory = true; // 在背包深处找到
      }

      if (windChargeSlot == -1) return false;

      float yaw = mc.player.getYaw();
      float pitch = 90.0F; // 垂直向下

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
}
