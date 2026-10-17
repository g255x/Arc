package dev.baimaodaxia.mod.modules.impl.combat;

import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.UpdateEvent;
import dev.baimaodaxia.api.utils.combat.CombatUtil;
import dev.baimaodaxia.api.utils.player.EntityUtil;
import dev.baimaodaxia.api.utils.world.BlockPosX;
import dev.baimaodaxia.api.utils.world.BlockUtil;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.impl.exploit.Blink;
import dev.baimaodaxia.mod.modules.impl.player.PacketMine;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.PickaxeItem;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class Breaker extends Module {
   public static Breaker INSTANCE;
   public final SliderSetting targetRange = this.add((new SliderSetting("TargetRange", (double)6.0F, (double)0.0F, (double)8.0F, 0.1)).setSuffix("m"));
   public final SliderSetting range = this.add((new SliderSetting("Range", (double)6.0F, (double)0.0F, (double)8.0F, 0.1)).setSuffix("m"));
   private final BooleanSetting burrow = this.add(new BooleanSetting("Burrow", true));
   private final BooleanSetting head = this.add(new BooleanSetting("Head", true));
   private final BooleanSetting face = this.add(new BooleanSetting("Face", true));
   private final BooleanSetting down = this.add(new BooleanSetting("Down", false));
   private final BooleanSetting surround = this.add(new BooleanSetting("Surround", true));
   private final BooleanSetting cevPause = this.add(new BooleanSetting("CevPause", true));
   private final BooleanSetting forceDouble = this.add(new BooleanSetting("ForceDouble", false));
   private final BooleanSetting noWeb = this.add(new BooleanSetting("NoWeb", false));
   private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", true));
   private final BooleanSetting noAnu = this.add(new BooleanSetting("NoAnu", false));
   public static final List<Block> hard;

   public Breaker() {
      super("Breaker", Module.Category.Combat);
      this.setChinese("自动挖掘");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!this.usingPause.getValue() || !mc.player.isUsingItem()) {
         if (!this.cevPause.getValue()) {
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
   }

   private void doBreak(PlayerEntity player) {
      BlockPos pos = EntityUtil.getEntityPos(player, true);
      if (PacketMine.getBreakPos() == null || PacketMine.getBreakPos().equals(PacketMine.secondPos) || PacketMine.secondPos == null || mc.world.isAir(PacketMine.secondPos) || !this.forceDouble.getValue()) {
         double[] dArray = new double[]{-0.8, 0.3, 1.1};
         double[] xzOffset = new double[]{0.3, -0.3};

         for(PlayerEntity playerEntity : CombatUtil.getEnemies(this.targetRange.getValue())) {
            for(double y : dArray) {
               for(double x : xzOffset) {
                  for(double z : xzOffset) {
                     BlockPosX offsetPos = new BlockPosX(playerEntity.getX() + x, playerEntity.getY() + y, playerEntity.getZ() + z);
                     if (this.canBreak(offsetPos) && offsetPos.equals(PacketMine.getBreakPos())) {
                        return;
                     }
                  }
               }
            }
         }

         ArrayList<Float> yList = new ArrayList();
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

         Iterator iterator = yList.iterator();

         while(iterator.hasNext()) {
            double y = (double)(Float)iterator.next();

            for(double offset : xzOffset) {
               BlockPosX offsetPos = new BlockPosX(player.getX() + offset, player.getY() + y, player.getZ() + offset);
               if (this.canBreak(offsetPos)) {
                  PacketMine.INSTANCE.mine(offsetPos);
                  return;
               }
            }
         }

         Iterator iterator2 = yList.iterator();

         while(iterator2.hasNext()) {
            double y = (double)(Float)iterator2.next();

            for(double offset : xzOffset) {
               for(double offset2 : xzOffset) {
                  BlockPosX offsetPos = new BlockPosX(player.getX() + offset2, player.getY() + y, player.getZ() + offset);
                  if (this.canBreak(offsetPos)) {
                     PacketMine.INSTANCE.mine(offsetPos);
                     return;
                  }
               }
            }
         }

         if (this.surround.getValue()) {
            for(Direction direction : Direction.values()) {
               if (direction != Direction.UP && direction != Direction.DOWN && !(Math.sqrt(mc.player.getEyePos().squaredDistanceTo(pos.offset(direction).toCenterPos())) > this.range.getValue()) && (mc.world.isAir(pos.offset(direction)) || pos.offset(direction).equals(PacketMine.getBreakPos())) && this.canPlaceCrystal(pos.offset(direction), false) && !pos.offset(direction).equals(PacketMine.secondPos)) {
                  return;
               }
            }

            ArrayList<BlockPos> arrayList = new ArrayList();

            for(Direction i : Direction.values()) {
               if (i != Direction.UP && i != Direction.DOWN && !(Math.sqrt(mc.player.getEyePos().squaredDistanceTo(pos.offset(i).toCenterPos())) > this.range.getValue()) && this.canBreak(pos.offset(i)) && this.canPlaceCrystal(pos.offset(i), true) && !this.isSurroundPos(pos.offset(i))) {
                  arrayList.add(pos.offset(i));
               }
            }

            if (!arrayList.isEmpty()) {
               PacketMine.INSTANCE.mine((BlockPos)arrayList.stream().min(Comparator.comparingDouble((E) -> E.getSquaredDistance(mc.player.getEyePos()))).get());
            } else {
               for(Direction i : Direction.values()) {
                  if (i != Direction.UP && i != Direction.DOWN && !(Math.sqrt(mc.player.getEyePos().squaredDistanceTo(pos.offset(i).toCenterPos())) > this.range.getValue()) && this.canBreak(pos.offset(i)) && this.canPlaceCrystal(pos.offset(i), false)) {
                     arrayList.add(pos.offset(i));
                  }
               }

               if (!arrayList.isEmpty()) {
                  PacketMine.INSTANCE.mine((BlockPos)arrayList.stream().min(Comparator.comparingDouble((E) -> E.getSquaredDistance(mc.player.getEyePos()))).get());
               }
            }
         }

      }
   }

   private boolean isSurroundPos(BlockPos pos) {
      for(Direction i : Direction.values()) {
         if (i != Direction.UP && i != Direction.DOWN && EntityUtil.getPlayerPos(true).offset(i).equals(pos)) {
            return true;
         }
      }

      return false;
   }

   public boolean canPlaceCrystal(BlockPos pos, boolean block) {
      BlockPos obsPos = pos.down();
      BlockPos boost = obsPos.up();
      return (BlockUtil.getBlock(obsPos) == Blocks.BEDROCK || BlockUtil.getBlock(obsPos) == Blocks.OBSIDIAN || !block) && BlockUtil.noEntityBlockCrystal(boost, true, true) && BlockUtil.noEntityBlockCrystal(boost.up(), true, true);
   }

   private boolean isObsidian(BlockPos pos) {
      if (this.noAnu.getValue() && mc.world.getBlockState(pos).getBlock() == Blocks.RESPAWN_ANCHOR) {
         return false;
      } else if (this.noWeb.getValue() && mc.world.getBlockState(pos).getBlock() == Blocks.COBWEB) {
         return false;
      } else {
         return mc.player.getEyePos().distanceTo(pos.toCenterPos()) <= PacketMine.INSTANCE.range.getValue() && !mc.world.isAir(pos) && mc.world.getBlockState(pos).isSolid() && BlockUtil.getClickSideStrict(pos) != null;
      }
   }

   private boolean canBreak(BlockPos pos) {
      return this.isObsidian(pos) && (BlockUtil.getClickSideStrict(pos) != null || pos.equals(PacketMine.getBreakPos())) && (!pos.equals(PacketMine.secondPos) || !(mc.player.getMainHandStack().getItem() instanceof PickaxeItem));
   }

   static {
      hard = Arrays.asList(Blocks.OBSIDIAN, Blocks.ENDER_CHEST, Blocks.NETHERITE_BLOCK, Blocks.CRYING_OBSIDIAN, Blocks.RESPAWN_ANCHOR, Blocks.ANCIENT_DEBRIS, Blocks.ANVIL);
   }
}
