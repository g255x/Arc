package dev.g255.arc.mod.modules.impl.combat;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.api.utils.combat.CombatUtil;
import dev.g255.arc.api.utils.math.ExplosionUtil;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.api.utils.player.EntityUtil;
import dev.g255.arc.api.utils.player.InventoryUtil;
import dev.g255.arc.api.utils.render.Render3DUtil;
import dev.g255.arc.api.utils.world.BlockUtil;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.ColorSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.PistonHeadBlock;
import net.minecraft.block.RedstoneBlock;
import net.minecraft.block.RedstoneTorchBlock;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

/**
 * 活塞水晶+（逻辑移植自 LeavesHack-main 的 PistonCrystal）
 * <p>
 * 与原版 {@link PistonCrystal} 的阶段式流程不同，这里采用“伤害驱动”的选点方式：
 * 遍历操作范围内的所有爆炸点，按对敌伤害/自伤/防自杀过滤后排序，选最优的爆炸点，
 * 再为它找出「活塞 + 红石 + 水晶」的放置方案。并用 lastBestPos 容差锁点防止来回抖动。
 */
public class PistonCrystalPlus extends Module {
   public static PistonCrystalPlus INSTANCE;

   private final SliderSetting targetRange = this.add(new SliderSetting("TargetRange", 6.0, 1.0, 8.0));
   private final SliderSetting range = this.add(new SliderSetting("Range", 4.0, 1.0, 6.0));
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 50, 0, 500));
   private final SliderSetting breakDelay = this.add(new SliderSetting("BreakDelay", 200, 0, 500));
   private final SliderSetting minDamage = this.add(new SliderSetting("MinDamage", 6.0, 1.0, 36.0));
   private final SliderSetting maxSelfDamage = this.add(new SliderSetting("MaxSelfDamage", 12.0, 1.0, 36.0));
   private final BooleanSetting usingPause = this.add((new BooleanSetting("UsingPause", true)).setParent());
   private final BooleanSetting onlyMain = this.add(new BooleanSetting("OnlyMain", true, () -> this.usingPause.isOpen()));
   private final BooleanSetting packetPlace = this.add(new BooleanSetting("PacketPlace", true));
   private final BooleanSetting preferCrystal = this.add(new BooleanSetting("PreferCrystal", true));
   private final BooleanSetting noSuicide = this.add(new BooleanSetting("NoSuicide", true));
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
   private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
   private final BooleanSetting mine = this.add(new BooleanSetting("Mine", true));
   private final BooleanSetting yawDeceive = this.add(new BooleanSetting("YawDeceive", true));
   private final EnumSetting<RedstoneMode> redstoneMode = this.add(new EnumSetting("RedStone", RedstoneMode.Block));
   private final BooleanSetting renderBox = this.add(new BooleanSetting("RenderBox", true));
   private final ColorSetting crystalColor = this.add(new ColorSetting("Crystal", new Color(255, 0, 0, 80), () -> this.renderBox.getValue()));
   private final ColorSetting pistonColor = this.add(new ColorSetting("Piston", new Color(255, 255, 255, 80), () -> this.renderBox.getValue()));
   private final ColorSetting redstoneColor = this.add(new ColorSetting("Redstone", new Color(255, 100, 0, 80), () -> this.renderBox.getValue()));

   private final Timer actionTimer = new Timer();
   private final Timer breakTimer = new Timer();

   private PlayerEntity target;
   private BlockPos crystalPos;
   private BlockPos pistonPos;
   private BlockPos redstonePos;
   private BlockPos lastPiston;
   private BlockPos lastBestPos;
   private Direction pistonFace;

   public PistonCrystalPlus() {
      super("PistonCrystalPlus", Module.Category.Combat);
      this.setChinese("活塞水晶+");
      INSTANCE = this;
   }

   public void onEnable() {
      this.actionTimer.reset();
      this.breakTimer.setMs(99999999L);
      this.lastBestPos = null;
      this.lastPiston = null;
      this.clearPlan();
   }

   public void onDisable() {
      this.clearPlan();
      this.lastBestPos = null;
      this.lastPiston = null;
   }

   private void clearPlan() {
      this.crystalPos = null;
      this.pistonPos = null;
      this.redstonePos = null;
      this.pistonFace = null;
   }

   public String getInfo() {
      return this.target != null ? this.target.getName().getString() : null;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (nullCheck()) {
         return;
      }

      // 动作间隔，避免同一刻连发多次放置
      if (!this.actionTimer.passedMs(this.delay.getValue())) {
         return;
      }

      // 自动水晶正在打水晶时先让路
      if (this.preferCrystal.getValue() && AutoCrystal.INSTANCE != null && AutoCrystal.INSTANCE.isOn() && AutoCrystal.INSTANCE.crystalPos != null) {
         return;
      }

      this.target = CombatUtil.getClosestEnemy(this.targetRange.getValue());
      if (this.target == null) {
         this.clearPlan();
         return;
      }

      if (this.shouldPause()) {
         return;
      }

      int redstone = this.findRedstone();
      int crystal = this.findItem(Items.END_CRYSTAL);
      int piston = this.findClass(PistonBlock.class);
      if (redstone == -1 || crystal == -1 || piston == -1) {
         this.clearPlan();
         return;
      }

      // 水晶被打掉/位置失效 → 丢掉方案重新选点
      if (this.crystalPos != null && !BlockUtil.hasCrystal(this.crystalPos) && !BlockUtil.canPlaceCrystal(this.crystalPos)) {
         this.actionTimer.reset();
         this.clearPlan();
         this.lastBestPos = null;
      }

      // 水晶已经放好了 → 先打掉它
      if (this.crystalPos != null && BlockUtil.hasCrystal(this.crystalPos) && this.breakTimer.passedMs(this.breakDelay.getValue())) {
         CombatUtil.attackCrystal(this.crystalPos, this.rotate.getValue(), false);
         this.actionTimer.reset();
         this.clearPlan();
         this.breakTimer.reset();
         return;
      }

      // 活塞头还堵在那儿导致放不了水晶 → 直接包挖掉活塞
      if (this.lastPiston != null && this.pistonFace != null && this.mine.getValue()) {
         if (BlockUtil.getBlock(this.lastPiston.offset(this.pistonFace.getOpposite())) instanceof PistonHeadBlock
                 && BlockUtil.getBlock(this.lastPiston) instanceof PistonBlock) {
            mc.interactionManager.attackBlock(this.lastPiston, BlockUtil.getClickSide(this.lastPiston));
            this.lastPiston = null;
         }
      }

      if (this.pistonPos == null && this.crystalPos == null && this.redstonePos == null) {
         this.doPistonCrystal(this.target);
      }

      // 活塞 → 红石 → 水晶，同一刻内按顺序放完（水晶最后放，避免被活塞顶飞）
      if (this.pistonPos != null && BlockUtil.canPlace(this.pistonPos)) {
         this.place(Items.PISTON, piston, this.pistonPos, this.pistonFace);
         this.lastPiston = this.pistonPos;
      }

      if (this.redstonePos != null && BlockUtil.canPlace(this.redstonePos)) {
         if (this.redstoneMode.is(RedstoneMode.Torch)) {
            this.placeTorch(this.redstonePos, redstone);
         } else {
            this.place(Items.REDSTONE_BLOCK, redstone, this.redstonePos, null);
         }
      }

      if (this.crystalPos != null && BlockUtil.canPlaceCrystal(this.crystalPos)) {
         this.placeCrystal(this.crystalPos, crystal);
         this.actionTimer.reset();
      }
   }

   /**
    * 选爆炸点：范围内所有“能放水晶”的位置 → 伤害过滤 → 排序 → 容差锁定 → 找活塞布局。
    */
   private void doPistonCrystal(PlayerEntity target) {
      List<BlockPos> candidates = new ArrayList<>();

      for (BlockPos pos : BlockUtil.getSphere(this.range.getValueFloat())) {
         if (!this.canPlaceCrystalNear(pos)) {
            continue;
         }

         Vec3d center = this.center(pos);
         float targetDamage = ExplosionUtil.calculateDamage(center, target, target, 6.0F);
         if (targetDamage < this.minDamage.getValue()) {
            continue;
         }

         float selfDamage = ExplosionUtil.calculateDamage(center, mc.player, mc.player, 6.0F);
         if (selfDamage > this.maxSelfDamage.getValue()) {
            continue;
         }

         if (this.noSuicide.getValue() && selfDamage > EntityUtil.getHealth(mc.player)) {
            continue;
         }

         candidates.add(pos);
      }

      if (candidates.isEmpty()) {
         this.lastBestPos = null;
         return;
      }

      // 对敌伤害高的优先，同伤害按离玩家近的优先
      candidates.sort((a, b) -> {
         int cmp = Float.compare(this.damageTo(target, b), this.damageTo(target, a));
         if (cmp != 0) {
            return cmp;
         }

         return Double.compare(mc.player.getEyePos().distanceTo(this.center(a)), mc.player.getEyePos().distanceTo(this.center(b)));
      });

      BlockPos best = candidates.get(0);

      // 容差锁点：旧点伤害还在新最优的 95% 以内就继续用旧点，防止两个差不多的点来回跳
      if (this.lastBestPos != null && candidates.contains(this.lastBestPos)) {
         float bestDamage = this.damageTo(target, best);
         float lastDamage = this.damageTo(target, this.lastBestPos);
         float lastSelf = ExplosionUtil.calculateDamage(this.center(this.lastBestPos), mc.player, mc.player, 6.0F);
         if (lastDamage >= bestDamage * 0.95F
                 && lastSelf <= this.maxSelfDamage.getValue()
                 && (!this.noSuicide.getValue() || lastSelf < EntityUtil.getHealth(mc.player))) {
            best = this.lastBestPos;
         }
      }

      this.lastBestPos = best;

      // 目标点上已经有水晶 → 先打掉再来一轮
      if (this.breakTimer.passedMs(this.breakDelay.getValue()) && BlockUtil.hasCrystal(best)) {
         CombatUtil.attackCrystal(best, this.rotate.getValue(), false);
         this.actionTimer.reset();
         this.clearPlan();
         this.breakTimer.reset();
         return;
      }

      // 为爆炸点找「活塞 + 红石 + 水晶」的朝向
      for (Direction dir : Direction.Type.HORIZONTAL) {
         BlockPos placeCrystalPos = best.offset(dir);
         if (!BlockUtil.canPlaceCrystal(placeCrystalPos)) {
            continue;
         }

         BlockPos pistonPos = placeCrystalPos.offset(dir);
         if (!BlockUtil.canPlace(pistonPos) && !(BlockUtil.getBlock(pistonPos) instanceof PistonBlock)) {
            continue;
         }

         BlockPos redstonePos = null;
         for (Direction side : Direction.values()) {
            if (side == dir.getOpposite()) {
               continue;
            }

            BlockPos powerPos = pistonPos.offset(side);
            if (powerPos.equals(placeCrystalPos)) {
               continue;
            }

            // 已经有对应模式的红石就直接用
            if (this.isRedstone(powerPos)) {
               redstonePos = powerPos;
               break;
            }

            if (!BlockUtil.canReplace(powerPos) || !BlockUtil.canPlace(powerPos)) {
               continue;
            }

            redstonePos = powerPos;
            break;
         }

         if (redstonePos == null) {
            continue;
         }

         this.pistonFace = dir;
         this.crystalPos = placeCrystalPos;
         this.pistonPos = pistonPos;
         this.redstonePos = redstonePos;
         return;
      }

      // 这个爆炸点摆不出活塞方案 → 解锁，下一轮重选
      this.lastBestPos = null;
   }

   /** 某个位置的任意水平方向能放出水晶（水晶要立在黑曜石/基岩上） */
   private boolean canPlaceCrystalNear(BlockPos pos) {
      for (Direction dir : Direction.Type.HORIZONTAL) {
         if (BlockUtil.canPlaceCrystal(pos.offset(dir)) || BlockUtil.hasCrystal(pos.offset(dir))) {
            return true;
         }
      }

      return false;
   }

   private boolean isRedstone(BlockPos pos) {
      Block block = BlockUtil.getBlock(pos);
      return this.redstoneMode.is(RedstoneMode.Torch)
              ? block instanceof RedstoneTorchBlock
              : block instanceof RedstoneBlock;
   }

   private float damageTo(PlayerEntity target, BlockPos pos) {
      return ExplosionUtil.calculateDamage(this.center(pos), target, target, 6.0F);
   }

   private Vec3d center(BlockPos pos) {
      return new Vec3d((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5);
   }

   private boolean shouldPause() {
      return this.usingPause.getValue()
              && (mc.options.useKey.isPressed() || mc.player.isUsingItem())
              && (!this.onlyMain.getValue() || mc.player.getActiveHand() == Hand.MAIN_HAND);
   }

   private void place(Item item, int slot, BlockPos pos, Direction facing) {
      if (slot == -1) {
         return;
      }

      Direction side = BlockUtil.getPlaceSide(pos);
      if (side == null) {
         return;
      }

      int old = mc.player.getInventory().selectedSlot;
      this.doSwap(slot);

      if (this.rotate.getValue()) {
         Vec3i vec = side.getVector();
         Arc.ROTATION.snapAt(pos.toCenterPos().add((double)vec.getX() * 0.5, (double)vec.getY() * 0.5, (double)vec.getZ() * 0.5));
      }

      // 活塞的朝向由放置瞬间的视角决定，所以要让视角先对准推的方向
      if (item == Items.PISTON && facing != null && this.yawDeceive.getValue() && this.rotate.getValue()) {
         AutoPush.pistonFacing(facing);
      }

      BlockUtil.placeBlock(pos, false, this.packetPlace.getValue());

      if (this.rotate.getValue()) {
         Arc.ROTATION.snapBack();
      }

      if (this.inventory.getValue()) {
         this.doSwap(slot);
         EntityUtil.syncInventory();
      } else {
         this.doSwap(old);
      }
   }

   private void placeCrystal(BlockPos pos, int slot) {
      if (slot == -1) {
         return;
      }

      BlockPos base = pos.down();
      Direction side = BlockUtil.getClickSide(base);
      if (side == null) {
         return;
      }

      int old = mc.player.getInventory().selectedSlot;
      this.doSwap(slot);
      BlockUtil.clickBlock(base, side, this.rotate.getValue());
      if (this.inventory.getValue()) {
         this.doSwap(slot);
         EntityUtil.syncInventory();
      } else {
         this.doSwap(old);
      }
   }

   private void placeTorch(BlockPos pos, int slot) {
      if (slot == -1 || !BlockUtil.canPlace(pos)) {
         return;
      }

      for (Direction side : Direction.values()) {
         if (side == Direction.UP) {
            continue;
         }

         BlockPos neighbour = pos.offset(side);
         if (BlockUtil.canReplace(neighbour) || BlockUtil.getBlock(neighbour) instanceof PistonBlock) {
            continue;
         }

         if (!BlockUtil.canClick(neighbour)) {
            continue;
         }

         int old = mc.player.getInventory().selectedSlot;
         this.doSwap(slot);
         BlockUtil.clickBlock(neighbour, side.getOpposite(), this.rotate.getValue());
         if (this.inventory.getValue()) {
            this.doSwap(slot);
            EntityUtil.syncInventory();
         } else {
            this.doSwap(old);
         }

         return;
      }
   }

   private void doSwap(int slot) {
      if (this.inventory.getValue()) {
         InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
      } else {
         InventoryUtil.switchToSlot(slot);
      }
   }

   private int findRedstone() {
      return this.redstoneMode.is(RedstoneMode.Torch)
              ? this.findItem(Items.REDSTONE_TORCH)
              : this.findItem(Items.REDSTONE_BLOCK);
   }

   private int findItem(Item item) {
      return this.inventory.getValue() ? InventoryUtil.findItemInventorySlot(item) : InventoryUtil.findItem(item);
   }

   private int findClass(Class<?> clazz) {
      return this.inventory.getValue() ? InventoryUtil.findClassInventorySlot(clazz) : InventoryUtil.findClass(clazz);
   }

   public void onRender3D(MatrixStack matrixStack) {
      if (!this.renderBox.getValue()) {
         return;
      }

      if (this.crystalPos != null) {
         Render3DUtil.draw3DBox(matrixStack, new Box(this.crystalPos), this.crystalColor.getValue(), this.crystalColor.getValue(), true, true);
      }

      if (this.pistonPos != null) {
         Render3DUtil.draw3DBox(matrixStack, new Box(this.pistonPos), this.pistonColor.getValue(), this.pistonColor.getValue(), true, true);
      }

      if (this.redstonePos != null) {
         Render3DUtil.draw3DBox(matrixStack, new Box(this.redstonePos), this.redstoneColor.getValue(), this.redstoneColor.getValue(), true, true);
      }
   }

   public enum RedstoneMode {
      Torch,
      Block
   }
}
