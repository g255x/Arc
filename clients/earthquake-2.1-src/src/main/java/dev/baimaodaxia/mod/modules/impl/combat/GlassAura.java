package dev.baimaodaxia.mod.modules.impl.combat;

import dev.baimaodaxia.Alien;
import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.UpdateEvent;
import dev.baimaodaxia.api.utils.combat.CombatUtil;
import dev.baimaodaxia.api.utils.math.PredictUtil;
import dev.baimaodaxia.api.utils.math.Timer;
import dev.baimaodaxia.api.utils.player.EntityUtil;
import dev.baimaodaxia.api.utils.player.InventoryUtil;
import dev.baimaodaxia.api.utils.world.BlockPosX;
import dev.baimaodaxia.api.utils.world.BlockUtil;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.impl.exploit.Blink;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;

public class GlassAura extends Module {
   public static GlassAura INSTANCE;
   private static final List<Block> GLASS_BLOCKS;
   public final SliderSetting delay = this.add((new SliderSetting("Delay", 100, 0, 500)).setSuffix("ms"));
   private final EnumSetting<TargetMode> targetMod;
   private final SliderSetting placeRange;
   private final SliderSetting blocksPer;
   public final SliderSetting predictTicks;
   private final BooleanSetting rotate;
   private final BooleanSetting autoDisable;
   private final SliderSetting range;
   private final BooleanSetting checkMine;
   private final BooleanSetting helper;
   private final BooleanSetting inventory;
   private final BooleanSetting breakCrystal;
   private final BooleanSetting usingPause;
   private final BooleanSetting selfGround;
   private final BooleanSetting legs;
   final ArrayList<BlockPos> placeList;
   private final Timer timer;
   public PlayerEntity target;
   int progress;

   public GlassAura() {
      super("GlassAura", Module.Category.Combat);
      this.targetMod = this.add(new EnumSetting("TargetMode", GlassAura.TargetMode.Single));
      this.placeRange = this.add((new SliderSetting("PlaceRange", (double)4.0F, (double)1.0F, (double)6.0F)).setSuffix("m"));
      this.blocksPer = this.add(new SliderSetting("BlocksPer", 1, 1, 8));
      this.predictTicks = this.add(new SliderSetting("PredictTicks", (double)2.0F, (double)0.0F, (double)50.0F, (double)1.0F));
      this.rotate = this.add(new BooleanSetting("Rotate", true));
      this.autoDisable = this.add(new BooleanSetting("AutoDisable", true));
      this.range = this.add((new SliderSetting("Range", (double)5.0F, (double)1.0F, (double)8.0F)).setSuffix("m"));
      this.checkMine = this.add(new BooleanSetting("DetectMining", false));
      this.helper = this.add(new BooleanSetting("Helper", true));
      this.inventory = this.add(new BooleanSetting("InventorySwap", true));
      this.breakCrystal = this.add(new BooleanSetting("Break", true));
      this.usingPause = this.add(new BooleanSetting("UsingPause", true));
      this.selfGround = this.add(new BooleanSetting("SelfGround", true));
      this.legs = this.add(new BooleanSetting("Legs", true));
      this.placeList = new ArrayList();
      this.timer = new Timer();
      this.progress = 0;
      this.setChinese("玻璃大炮");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.placeList.clear();
      this.progress = 0;
      this.target = null;
      if (!this.selfGround.getValue() || mc.player.isOnGround()) {
         if (!this.inventory.getValue() || EntityUtil.inInventory()) {
            if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
               if (!this.usingPause.getValue() || !mc.player.isUsingItem()) {
                  if (this.timer.passed((long)this.delay.getValue())) {
                     if (this.targetMod.getValue() == GlassAura.TargetMode.Single) {
                        this.target = CombatUtil.getClosestEnemy(this.range.getValue());
                        if (this.target == null) {
                           if (this.autoDisable.getValue()) {
                              this.disable();
                           }

                           return;
                        }

                        this.trapTarget(this.target);
                     } else if (this.targetMod.getValue() == GlassAura.TargetMode.Multi) {
                        boolean found = false;

                        for(PlayerEntity player : CombatUtil.getEnemies(this.range.getValue())) {
                           found = true;
                           this.target = player;
                           this.trapTarget(this.target);
                        }

                        if (!found) {
                           if (this.autoDisable.getValue()) {
                              this.disable();
                           }

                           this.target = null;
                        }
                     }

                  }
               }
            }
         }
      }
   }

   private void trapTarget(PlayerEntity target) {
      Vec3d playerPos = this.predictTicks.getValue() > (double)0.0F ? PredictUtil.getPos(target, this.predictTicks.getValueInt()) : target.getPos();
      this.doTrap(target, new BlockPosX(playerPos.getX(), playerPos.getY(), playerPos.getZ()));
   }

   private void doTrap(PlayerEntity player, BlockPos pos) {
      if (pos != null && this.legs.getValue()) {
         for(Direction i : Direction.values()) {
            if (i != Direction.DOWN && i != Direction.UP) {
               BlockPos legPos = pos.offset(i);
               this.tryPlaceBlock(legPos);
               if (BlockUtil.getPlaceSide(legPos) == null && BlockUtil.clientCanPlace(legPos, this.breakCrystal.getValue()) && this.getHelper(legPos) != null) {
                  this.tryPlaceObsidian(this.getHelper(legPos));
               }
            }
         }

      }
   }

   public String getInfo() {
      return this.target != null ? this.target.getName().getString() : null;
   }

   public BlockPos getHelper(BlockPos pos) {
      if (!this.helper.getValue()) {
         return null;
      } else {
         for(Direction i : Direction.values()) {
            if ((!this.checkMine.getValue() || !Alien.BREAK.isMining(pos.offset(i))) && BlockUtil.isStrictDirection(pos.offset(i), i.getOpposite()) && BlockUtil.canPlace(pos.offset(i), this.placeRange.getValue(), this.breakCrystal.getValue())) {
               return pos.offset(i);
            }
         }

         return null;
      }
   }

   private void tryPlaceBlock(BlockPos pos) {
      if (!this.placeList.contains(pos)) {
         if (!Alien.BREAK.isMining(pos)) {
            if (BlockUtil.canPlace(pos, (double)6.0F, this.breakCrystal.getValue())) {
               if ((double)this.progress < this.blocksPer.getValue()) {
                  if (!((double)MathHelper.sqrt((float)mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos())) > this.placeRange.getValue())) {
                     int old = mc.player.getInventory().selectedSlot;
                     int block = this.getBlock();
                     if (block != -1) {
                        this.placeList.add(pos);
                        CombatUtil.attackCrystal(pos, this.rotate.getValue(), this.usingPause.getValue());
                        this.doSwap(block);
                        BlockUtil.placeBlock(pos, this.rotate.getValue());
                        if (this.inventory.getValue()) {
                           this.doSwap(block);
                           EntityUtil.syncInventory();
                        } else {
                           this.doSwap(old);
                        }

                        this.timer.reset();
                        ++this.progress;
                     }
                  }
               }
            }
         }
      }
   }

   private void tryPlaceObsidian(BlockPos pos) {
      if (pos != null) {
         if (!this.placeList.contains(pos)) {
            if (!Alien.BREAK.isMining(pos)) {
               if (BlockUtil.canPlace(pos, (double)6.0F, this.breakCrystal.getValue())) {
                  if ((double)this.progress < this.blocksPer.getValue()) {
                     if (!((double)MathHelper.sqrt((float)mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos())) > this.placeRange.getValue())) {
                        int old = mc.player.getInventory().selectedSlot;
                        int block = this.getBlock();
                        if (block != -1) {
                           BlockUtil.placedPos.add(pos);
                           this.placeList.add(pos);
                           CombatUtil.attackCrystal(pos, this.rotate.getValue(), this.usingPause.getValue());
                           this.doSwap(block);
                           BlockUtil.placeBlock(pos, this.rotate.getValue());
                           if (this.inventory.getValue()) {
                              this.doSwap(block);
                              EntityUtil.syncInventory();
                           } else {
                              this.doSwap(old);
                           }

                           this.timer.reset();
                           ++this.progress;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void doSwap(int slot) {
      if (this.inventory.getValue()) {
         InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
      } else {
         InventoryUtil.switchToSlot(slot);
      }

   }

   private int getBlock() {
      for(Block glass : GLASS_BLOCKS) {
         int slot;
         if (this.inventory.getValue()) {
            slot = InventoryUtil.findBlockInventorySlot(glass);
         } else {
            slot = InventoryUtil.findBlock(glass);
         }

         if (slot != -1) {
            return slot;
         }
      }

      return -1;
   }

   static {
      GLASS_BLOCKS = Arrays.asList(Blocks.GLASS, Blocks.TINTED_GLASS, Blocks.WHITE_STAINED_GLASS, Blocks.ORANGE_STAINED_GLASS, Blocks.MAGENTA_STAINED_GLASS, Blocks.LIGHT_BLUE_STAINED_GLASS, Blocks.YELLOW_STAINED_GLASS, Blocks.LIME_STAINED_GLASS, Blocks.PINK_STAINED_GLASS, Blocks.GRAY_STAINED_GLASS, Blocks.LIGHT_GRAY_STAINED_GLASS, Blocks.CYAN_STAINED_GLASS, Blocks.PURPLE_STAINED_GLASS, Blocks.BLUE_STAINED_GLASS, Blocks.BROWN_STAINED_GLASS, Blocks.GREEN_STAINED_GLASS, Blocks.RED_STAINED_GLASS, Blocks.BLACK_STAINED_GLASS, Blocks.GLASS_PANE, Blocks.WHITE_STAINED_GLASS_PANE, Blocks.ORANGE_STAINED_GLASS_PANE, Blocks.MAGENTA_STAINED_GLASS_PANE, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE, Blocks.YELLOW_STAINED_GLASS_PANE, Blocks.LIME_STAINED_GLASS_PANE, Blocks.PINK_STAINED_GLASS_PANE, Blocks.GRAY_STAINED_GLASS_PANE, Blocks.LIGHT_GRAY_STAINED_GLASS_PANE, Blocks.CYAN_STAINED_GLASS_PANE, Blocks.PURPLE_STAINED_GLASS_PANE, Blocks.BLUE_STAINED_GLASS_PANE, Blocks.BROWN_STAINED_GLASS_PANE, Blocks.GREEN_STAINED_GLASS_PANE, Blocks.RED_STAINED_GLASS_PANE, Blocks.BLACK_STAINED_GLASS_PANE);
   }

   public static enum TargetMode {
      Single,
      Multi;

      // $FF: synthetic method
      private static TargetMode[] $values() {
         return new TargetMode[]{Single, Multi};
      }
   }
}
