package dev.G255.Lumi.mod.modules.impl.combat;

import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.UpdateEvent;
import dev.G255.Lumi.api.utils.combat.CombatUtil;
import dev.G255.Lumi.api.utils.player.EntityUtil;
import dev.G255.Lumi.api.utils.world.BlockPosX;
import dev.G255.Lumi.api.utils.world.BlockUtil;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.Module.Category;
import dev.G255.Lumi.mod.modules.impl.exploit.Blink;
import dev.G255.Lumi.mod.modules.impl.player.PacketMine;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.PickaxeItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class Breaker extends Module {
   public static Breaker INSTANCE;
   public final SliderSetting targetRange = this.add(new SliderSetting("TargetRange", 6.0, 0.0, 8.0, 0.1).setSuffix("m"));
   public final SliderSetting range = this.add(new SliderSetting("Range", 6.0, 0.0, 8.0, 0.1).setSuffix("m"));
   private final BooleanSetting burrow = this.add(new BooleanSetting("Burrow", true));
   private final BooleanSetting head = this.add(new BooleanSetting("Head", true));
   private final BooleanSetting face = this.add(new BooleanSetting("Face", true));
   private final BooleanSetting down = this.add(new BooleanSetting("Down", false));
   private final BooleanSetting surround = this.add(new BooleanSetting("Surround", true));
   private final BooleanSetting cevPause = this.add(new BooleanSetting("CevPause", true));
   private final BooleanSetting forceDouble = this.add(new BooleanSetting("ForceDouble", false));
   private final BooleanSetting dynamic = this.add(new BooleanSetting("Dynamic", false));
   private final SliderSetting dynamicDelay = this.add(new SliderSetting("DynamicDelay", 0.3, 0.1, 2.0, 0.05).setSuffix("s"));
   private Thread dynamicThread;
   private volatile boolean dynamicActive;
   public static final List<Block> hard = Arrays.asList(
      Blocks.OBSIDIAN, Blocks.ENDER_CHEST, Blocks.NETHERITE_BLOCK, Blocks.CRYING_OBSIDIAN, Blocks.RESPAWN_ANCHOR, Blocks.ANCIENT_DEBRIS, Blocks.ANVIL
   );

   public Breaker() {
      super("Breaker", "Automatically breaks blocks around target positions.", Module.Category.Combat);
      this.setChinese("自动挖掘");
        this.setChineseDesc("自动挖掘目标位置周围的方块");
      INSTANCE = this;
      this.dynamic.injectTask(() -> {
         if (this.dynamic.getValue()) {
            if (this.isOn()) this.startDynamicThread();
         } else {
            this.stopDynamicThread();
         }
      });
   }

   @Override
   public void onEnable() {
      if (this.dynamic.getValue()) {
         this.startDynamicThread();
      }
   }

   @Override
   public void onDisable() {
      this.stopDynamicThread();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!CevBreaker.INSTANCE.isOn() || !this.cevPause.getValue()) {
         if (!AntiCrawl.INSTANCE.work) {
            if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
               PlayerEntity player = CombatUtil.getClosestEnemy(this.targetRange.getValue());
               if (player != null) {
                  this.doBreak(player);
               }
            }
         }
      }
   }

   private void doBreak(PlayerEntity player) {
      BlockPos target = this.findBreakPos(player);
      if (target != null) {
         PacketMine.INSTANCE.mine(target);
      }
   }

   private BlockPos findBreakPos(PlayerEntity player) {
      if (PacketMine.getBreakPos() == null
         || PacketMine.getBreakPos().equals(PacketMine.secondPos)
         || PacketMine.secondPos == null
         || mc.world.isAir(PacketMine.secondPos)
         || !this.forceDouble.getValue()) {
         double[] yOffset = new double[]{-0.8, 0.3, 1.1};
         double[] xzOffset = new double[]{0.3, -0.3};

         for (PlayerEntity entity : CombatUtil.getEnemies(this.targetRange.getValue())) {
            for (double y : yOffset) {
               for (double x : xzOffset) {
                  for (double z : xzOffset) {
                     BlockPos offsetPos = new BlockPosX(entity.getX() + x, entity.getY() + y, entity.getZ() + z);
                     if (this.canBreak(offsetPos) && offsetPos.equals(PacketMine.getBreakPos())) {
                        return null;
                     }
                  }
               }
            }
         }

         List<Float> yList = new ArrayList();
         if (this.down.getValue()) {
            yList.add(-0.8F);
         }

         if (this.head.getValue()) {
            yList.add(2.3F);
         }

         if (this.burrow.getValue()) {
            yList.add(0.3F);
         }

         if (this.face.getValue()) {
            yList.add(1.1F);
         }

         for (double y : yList) {
            for (double offset : xzOffset) {
               BlockPos offsetPos = new BlockPosX(player.getX() + offset, player.getY() + y, player.getZ() + offset);
               if (this.canBreak(offsetPos)) {
                  return offsetPos;
               }
            }
         }

         for (double y : yList) {
            for (double offsetx : xzOffset) {
               for (double offset2 : xzOffset) {
                  BlockPos offsetPos = new BlockPosX(player.getX() + offset2, player.getY() + y, player.getZ() + offsetx);
                  if (this.canBreak(offsetPos)) {
                     return offsetPos;
                  }
               }
            }
         }

         if (this.surround.getValue()) {
            BlockPos pos = EntityUtil.getEntityPos(player, true);
            for (Direction i : Direction.values()) {
               if (i != Direction.UP
                  && i != Direction.DOWN
                  && !(Math.sqrt(mc.player.getEyePos().squaredDistanceTo(pos.offset(i).toCenterPos())) > this.range.getValue())
                  && (mc.world.isAir(pos.offset(i)) || pos.offset(i).equals(PacketMine.getBreakPos()))
                  && this.canPlaceCrystal(pos.offset(i), false)
                  && !pos.offset(i).equals(PacketMine.secondPos)) {
                  return null;
               }
            }

            ArrayList<BlockPos> list = new ArrayList();

            for (Direction ix : Direction.values()) {
               if (ix != Direction.UP
                  && ix != Direction.DOWN
                  && !(Math.sqrt(mc.player.getEyePos().squaredDistanceTo(pos.offset(ix).toCenterPos())) > this.range.getValue())
                  && this.canBreak(pos.offset(ix))
                  && this.canPlaceCrystal(pos.offset(ix), true)
                  && !this.isSurroundPos(pos.offset(ix))) {
                  list.add(pos.offset(ix));
               }
            }

            if (!list.isEmpty()) {
               return (BlockPos)list.stream().min(Comparator.comparingDouble(E -> E.getSquaredDistance(mc.player.getEyePos()))).get();
            } else {
               for (Direction ixx : Direction.values()) {
                  if (ixx != Direction.UP
                     && ixx != Direction.DOWN
                     && !(Math.sqrt(mc.player.getEyePos().squaredDistanceTo(pos.offset(ixx).toCenterPos())) > this.range.getValue())
                     && this.canBreak(pos.offset(ixx))
                     && this.canPlaceCrystal(pos.offset(ixx), false)) {
                     list.add(pos.offset(ixx));
                  }
               }

               if (!list.isEmpty()) {
                  return (BlockPos)list.stream().min(Comparator.comparingDouble(E -> E.getSquaredDistance(mc.player.getEyePos()))).get();
               }
            }
         }
      }

      return null;
   }

   private boolean isSurroundPos(BlockPos pos) {
      for (Direction i : Direction.values()) {
         if (i != Direction.UP && i != Direction.DOWN) {
            BlockPos self = EntityUtil.getPlayerPos(true);
            if (self.offset(i).equals(pos)) {
               return true;
            }
         }
      }

      return false;
   }

   public boolean canPlaceCrystal(BlockPos pos, boolean block) {
      BlockPos obsPos = pos.down();
      BlockPos boost = obsPos.up();
      return (BlockUtil.getBlock(obsPos) == Blocks.BEDROCK || BlockUtil.getBlock(obsPos) == Blocks.OBSIDIAN || !block)
         && BlockUtil.noEntityBlockCrystal(boost, true, true)
         && BlockUtil.noEntityBlockCrystal(boost.up(), true, true);
   }

   private boolean isObsidian(BlockPos pos) {
      return mc.player.getEyePos().distanceTo(pos.toCenterPos()) <= PacketMine.INSTANCE.range.getValue()
         && hard.contains(BlockUtil.getBlock(pos))
         && BlockUtil.getClickSideStrict(pos) != null;
   }

   private boolean canBreak(BlockPos pos) {
      return this.isObsidian(pos)
         && (BlockUtil.getClickSideStrict(pos) != null || pos.equals(PacketMine.getBreakPos()))
         && (
            !pos.equals(PacketMine.secondPos)
               || !(mc.player.getMainHandStack().getItem() instanceof PickaxeItem)
                  && !PacketMine.INSTANCE.autoSwitch.getValue()
                  && !PacketMine.INSTANCE.noGhostHand.getValue()
         );
   }

   private void dynamicCheck(PlayerEntity player) {
      if (AntiCrawl.INSTANCE.isOn() && AntiCrawl.INSTANCE.work) return;

      BlockPos shouldBreak = this.findBreakPos(player);
      if (shouldBreak == null) return;

      BlockPos currentBreak = PacketMine.getBreakPos();
      if (currentBreak == null) {
         PacketMine.INSTANCE.mine(shouldBreak);
      } else if (!shouldBreak.equals(currentBreak)) {
         PacketMine.INSTANCE.mine(shouldBreak);
      }
   }

   private void startDynamicThread() {
      if (this.dynamicThread != null && this.dynamicActive) return;
      this.dynamicActive = true;
      this.dynamicThread = new Thread(() -> {
         while (this.dynamicActive && this.isOn()) {
            try {
               Thread.sleep((long)(this.dynamicDelay.getValue() * 1000));
            } catch (InterruptedException e) {
               break;
            }
            if (!this.dynamicActive || !this.isOn()) break;

            mc.execute(() -> {
               if (nullCheck() || !this.isOn() || !this.dynamic.getValue()) return;
               PlayerEntity player = CombatUtil.getClosestEnemy(this.targetRange.getValue());
               if (player != null) {
                  this.dynamicCheck(player);
               }
            });
         }
      }, "Breaker-Dynamic");
      this.dynamicThread.setDaemon(true);
      this.dynamicThread.start();
   }

   private void stopDynamicThread() {
      this.dynamicActive = false;
      if (this.dynamicThread != null) {
         this.dynamicThread.interrupt();
         this.dynamicThread = null;
      }
   }
}