package dev.baimaodaxia.mod.modules.impl.combat;

import dev.baimaodaxia.Alien;
import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.UpdateEvent;
import dev.baimaodaxia.api.utils.combat.CombatUtil;
import dev.baimaodaxia.api.utils.math.PredictUtil;
import dev.baimaodaxia.api.utils.math.Timer;
import dev.baimaodaxia.api.utils.player.EntityUtil;
import dev.baimaodaxia.api.utils.player.InventoryUtil;
import dev.baimaodaxia.api.utils.world.BlockUtil;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.impl.exploit.Blink;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import java.util.List;
import java.util.Objects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

public class HoleFiller extends Module {
   public static HoleFiller INSTANCE;
   public final SliderSetting placeDelay = this.add((new SliderSetting("PlaceDelay", 50, 0, 500)).setSuffix("ms"));
   public final BooleanSetting inAirPause = this.add(new BooleanSetting("InAirPause", true));
   public final SliderSetting blocksPer = this.add(new SliderSetting("BlocksPer", 1, 1, 8));
   public final SliderSetting placeRange = this.add((new SliderSetting("PlaceRange", (double)5.0F, (double)0.0F, (double)8.0F)).setSuffix("m"));
   public final SliderSetting enemyRange = this.add((new SliderSetting("EnemyRange", (double)6.0F, (double)0.0F, (double)8.0F)).setSuffix("m"));
   public final SliderSetting holeRange = this.add((new SliderSetting("HoleRange", (double)2.0F, (double)0.0F, (double)8.0F)).setSuffix("m"));
   public final SliderSetting selfRange = this.add((new SliderSetting("SelfRange", (double)2.0F, (double)0.0F, (double)8.0F)).setSuffix("m"));
   public final SliderSetting predictTicks = this.add((new SliderSetting("Predict", 1, 1, 8)).setSuffix("tick"));
   public final BooleanSetting detectMining = this.add(new BooleanSetting("DetectMining", false));
   public final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
   public final BooleanSetting packetPlace = this.add(new BooleanSetting("PacketPlace", true));
   public final BooleanSetting breakCrystal = this.add((new BooleanSetting("Break", true)).setParent());
   public final BooleanSetting eatPause;
   public final BooleanSetting usingPause;
   public final BooleanSetting inventory;
   public final BooleanSetting silent;
   public final EnumSetting<FillBlock> fillBlock;
   private final Timer timer;
   private int progress;

   public HoleFiller() {
      super("HoleFiller", Module.Category.Combat);
      BooleanSetting var10006 = this.breakCrystal;
      Objects.requireNonNull(var10006);
      this.eatPause = this.add(new BooleanSetting("EatingPause", true, var10006::isOpen));
      this.usingPause = this.add(new BooleanSetting("UsingPause", true));
      this.inventory = this.add(new BooleanSetting("InventorySwap", true));
      this.silent = this.add(new BooleanSetting("Silent", true));
      this.fillBlock = this.add(new EnumSetting("FillBlock", HoleFiller.FillBlock.Obsidian));
      this.timer = new Timer();
      this.progress = 0;
      this.setChinese("自动填坑");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.timer.passed((long)this.placeDelay.getValue())) {
         if (!this.inventory.getValue() || EntityUtil.inInventory()) {
            this.progress = 0;
            if (this.getBlock() != -1) {
               if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
                  if (!this.usingPause.getValue() || !mc.player.isUsingItem()) {
                     if (!this.inAirPause.getValue() || mc.player.isOnGround()) {
                        boolean enemyNearby = CombatUtil.getEnemies(this.selfRange.getValue()).stream().anyMatch((e) -> true);
                        if (enemyNearby) {
                           CombatUtil.getEnemies(this.enemyRange.getValue()).stream().flatMap((enemy) -> BlockUtil.getSphere(this.holeRange.getValueFloat(), PredictUtil.getPos(enemy, this.predictTicks.getValueInt())).stream()).filter((pos) -> Alien.HOLE.isHole(pos, true, true, false) || Alien.HOLE.isDoubleHole(pos)).distinct().forEach(this::tryPlaceBlock);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void tryPlaceBlock(BlockPos pos) {
      if (pos != null) {
         if (!this.detectMining.getValue() || !Alien.BREAK.isMining(pos)) {
            if (!((double)this.progress >= this.blocksPer.getValue())) {
               int blockSlot = this.getBlock();
               if (blockSlot != -1) {
                  if (BlockUtil.canPlace(pos, this.placeRange.getValue(), true)) {
                     Box box = new Box(pos);
                     List<Entity> entities = BlockUtil.getEntities(box);
                     boolean hasCrystal = false;
                     boolean hasOther = false;

                     for(Entity e : entities) {
                        if (e instanceof EndCrystalEntity) {
                           hasCrystal = true;
                        } else if (!(e instanceof ItemEntity) && !(e instanceof ExperienceOrbEntity)) {
                           hasOther = true;
                        }
                     }

                     if (!hasOther) {
                        if (hasCrystal) {
                           if (this.breakCrystal.getValue()) {
                              CombatUtil.attackCrystal(pos.up(), this.rotate.getValue(), this.eatPause.getValue());
                           }
                        } else {
                           int oldSlot = mc.player.getInventory().selectedSlot;
                           this.doSwap(blockSlot);
                           BlockUtil.placeBlock(pos, this.rotate.getValue(), this.packetPlace.getValue());
                           this.doSwapBack(oldSlot);
                           ++this.progress;
                           this.timer.reset();
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
      } else if (this.silent.getValue()) {
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
      } else {
         InventoryUtil.switchToSlot(slot);
      }

   }

   private void doSwapBack(int slot) {
      if (this.inventory.getValue()) {
         InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
         EntityUtil.syncInventory();
      } else if (this.silent.getValue()) {
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
      } else {
         InventoryUtil.switchToSlot(slot);
      }

   }

   private int getBlock() {
      Block block;
      switch (((FillBlock)this.fillBlock.getValue()).ordinal()) {
         case 0 -> block = Blocks.OBSIDIAN;
         case 1 -> block = Blocks.COBWEB;
         case 2 -> block = Blocks.LADDER;
         case 3 -> block = Blocks.BRICKS;
         default -> block = Blocks.OBSIDIAN;
      }

      return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(block) : InventoryUtil.findBlock(block);
   }

   public static enum FillBlock {
      Obsidian,
      Web,
      Ladder,
      Bricks;

      // $FF: synthetic method
      private static FillBlock[] $values() {
         return new FillBlock[]{Obsidian, Web, Ladder, Bricks};
      }
   }
}
