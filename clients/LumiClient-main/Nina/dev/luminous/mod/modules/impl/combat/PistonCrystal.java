package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.Objects;
import net.minecraft.class_1511;
import net.minecraft.class_1657;
import net.minecraft.class_1792;
import net.minecraft.class_1802;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2318;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_238;
import net.minecraft.class_2665;
import net.minecraft.class_3532;

public class PistonCrystal extends Module {
   public static PistonCrystal INSTANCE;
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", false));
   private final BooleanSetting pistonPacket = this.add(new BooleanSetting("PistonPacket", false));
   private final BooleanSetting noEating = this.add(new BooleanSetting("NoEating", true));
   private final BooleanSetting eatingBreak = this.add(new BooleanSetting("EatingBreak", false));
   private final BooleanSetting aggressive = this.add(new BooleanSetting("Aggressive", false));
   private final SliderSetting burst;
   private final BooleanSetting forceBreak;
   private final SliderSetting placeRange;
   private final SliderSetting range;
   private final BooleanSetting fire;
   private final BooleanSetting switchPos;
   private final BooleanSetting onlyGround;
   private final BooleanSetting onlyStatic;
   private final SliderSetting updateDelay;
   private final SliderSetting posUpdateDelay;
   private final SliderSetting stageSetting;
   private final SliderSetting pistonStage;
   private final SliderSetting pistonMaxStage;
   private final SliderSetting powerStage;
   private final SliderSetting powerMaxStage;
   private final SliderSetting crystalStage;
   private final SliderSetting crystalMaxStage;
   private final SliderSetting fireStage;
   private final SliderSetting fireMaxStage;
   private final BooleanSetting inventory;
   private final BooleanSetting debug;
   private final Timer timer;
   private final Timer crystalTimer;
   public class_2338 bestPos;
   public class_2338 bestOPos;
   public class_2350 bestFacing;
   public double getDistance;
   private double bestDistanceSq;
   public boolean getPos;
   public int stage;
   private class_1657 target;
   private boolean isPiston;
   private int cachedPistonSlot;
   private int cachedPowerSlot;
   private int cachedCrystalSlot;
   private int cachedFireSlot;
   private int slotCacheTick;

   public PistonCrystal() {
      super("PistonCrystal", Module.Category.Combat);
      BooleanSetting var10008 = this.aggressive;
      Objects.requireNonNull(var10008);
      this.burst = this.add(new SliderSetting("Burst", 2, 1, 5, var10008::getValue));
      BooleanSetting var10006 = this.aggressive;
      Objects.requireNonNull(var10006);
      this.forceBreak = this.add(new BooleanSetting("ForceBreak", true, var10006::getValue));
      this.placeRange = this.add(new SliderSetting("PlaceRange", (double)5.0F, (double)1.0F, (double)8.0F));
      this.range = this.add(new SliderSetting("Range", (double)4.0F, (double)1.0F, (double)8.0F));
      this.fire = this.add(new BooleanSetting("Fire", true));
      this.switchPos = this.add(new BooleanSetting("Switch", false));
      this.onlyGround = this.add(new BooleanSetting("SelfGround", true));
      this.onlyStatic = this.add(new BooleanSetting("MovingPause", true));
      this.updateDelay = this.add(new SliderSetting("PlaceDelay", 100, 0, 500));
      this.posUpdateDelay = this.add(new SliderSetting("PosUpdateDelay", 500, 0, 1000));
      this.stageSetting = this.add(new SliderSetting("Stage", 4, 1, 10));
      this.pistonStage = this.add(new SliderSetting("PistonStage", 1, 1, 10));
      this.pistonMaxStage = this.add(new SliderSetting("PistonMaxStage", 1, 1, 10));
      this.powerStage = this.add(new SliderSetting("PowerStage", 3, 1, 10));
      this.powerMaxStage = this.add(new SliderSetting("PowerMaxStage", 3, 1, 10));
      this.crystalStage = this.add(new SliderSetting("CrystalStage", 4, 1, 10));
      this.crystalMaxStage = this.add(new SliderSetting("CrystalMaxStage", 4, 1, 10));
      this.fireStage = this.add(new SliderSetting("FireStage", 2, 1, 10));
      this.fireMaxStage = this.add(new SliderSetting("FireMaxStage", 2, 1, 10));
      this.inventory = this.add(new BooleanSetting("InventorySwap", true));
      this.debug = this.add(new BooleanSetting("Debug", false));
      this.timer = new Timer();
      this.crystalTimer = new Timer();
      this.bestPos = null;
      this.bestOPos = null;
      this.bestFacing = null;
      this.getDistance = (double)100.0F;
      this.bestDistanceSq = Double.POSITIVE_INFINITY;
      this.getPos = false;
      this.stage = 1;
      this.target = null;
      this.isPiston = false;
      this.cachedPistonSlot = -1;
      this.cachedPowerSlot = -1;
      this.cachedCrystalSlot = -1;
      this.cachedFireSlot = -1;
      this.slotCacheTick = -1;
      this.setChinese("活塞水晶");
      INSTANCE = this;
   }

   private static boolean canFire(class_2338 pos) {
      if (BlockUtil.canReplace(pos.method_10074())) {
         return false;
      } else if (!mc.field_1687.method_22347(pos)) {
         return false;
      } else {
         return !BlockUtil.canClick(pos.method_10093(class_2350.field_11033)) ? false : BlockUtil.isStrictDirection(pos.method_10074(), class_2350.field_11036);
      }
   }

   public void onTick() {
      if (this.pistonStage.getValue() > this.stageSetting.getValue()) {
         this.pistonStage.setValue(this.stageSetting.getValue());
      }

      if (this.fireStage.getValue() > this.stageSetting.getValue()) {
         this.fireStage.setValue(this.stageSetting.getValue());
      }

      if (this.powerStage.getValue() > this.stageSetting.getValue()) {
         this.powerStage.setValue(this.stageSetting.getValue());
      }

      if (this.crystalStage.getValue() > this.stageSetting.getValue()) {
         this.crystalStage.setValue(this.stageSetting.getValue());
      }

      if (this.pistonMaxStage.getValue() > this.stageSetting.getValue()) {
         this.pistonMaxStage.setValue(this.stageSetting.getValue());
      }

      if (this.fireMaxStage.getValue() > this.stageSetting.getValue()) {
         this.fireMaxStage.setValue(this.stageSetting.getValue());
      }

      if (this.powerMaxStage.getValue() > this.stageSetting.getValue()) {
         this.powerMaxStage.setValue(this.stageSetting.getValue());
      }

      if (this.crystalMaxStage.getValue() > this.stageSetting.getValue()) {
         this.crystalMaxStage.setValue(this.stageSetting.getValue());
      }

      if (this.crystalMaxStage.getValue() < this.crystalStage.getValue()) {
         this.crystalStage.setValue(this.crystalMaxStage.getValue());
      }

      if (this.powerMaxStage.getValue() < this.powerStage.getValue()) {
         this.powerStage.setValue(this.powerMaxStage.getValue());
      }

      if (this.pistonMaxStage.getValue() < this.pistonStage.getValue()) {
         this.pistonStage.setValue(this.pistonMaxStage.getValue());
      }

      if (this.fireMaxStage.getValue() < this.fireStage.getValue()) {
         this.fireStage.setValue(this.fireMaxStage.getValue());
      }

   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.onTick();
      this.target = CombatUtil.getClosestEnemy(this.range.getValue());
      if (this.target != null) {
         boolean aggressive = this.aggressive.getValue();
         if (!this.noEating.getValue() || !mc.field_1724.method_6115() || aggressive) {
            this.refreshSlots();
            if (!this.check(this.onlyStatic.getValue(), !mc.field_1724.method_24828(), this.onlyGround.getValue())) {
               class_2338 pos = EntityUtil.getEntityPos(this.target, true);
               if (!mc.field_1724.method_6115() || this.eatingBreak.getValue() || aggressive) {
                  if (this.checkCrystal(pos.method_10086(0))) {
                     CombatUtil.attackCrystal(pos.method_10086(0), this.rotate.getValue(), true);
                  }

                  if (this.checkCrystal(pos.method_10086(1))) {
                     CombatUtil.attackCrystal(pos.method_10086(1), this.rotate.getValue(), true);
                  }

                  if (this.checkCrystal(pos.method_10086(2))) {
                     CombatUtil.attackCrystal(pos.method_10086(2), this.rotate.getValue(), true);
                  }
               }

               if (this.bestPos != null && mc.field_1687.method_8320(this.bestPos).method_26204() instanceof class_2665) {
                  this.isPiston = true;
               } else if (this.isPiston) {
                  this.isPiston = false;
                  this.crystalTimer.reset();
                  this.bestPos = null;
                  this.bestOPos = null;
                  this.bestFacing = null;
                  this.getPos = false;
                  this.getDistance = (double)100.0F;
                  this.bestDistanceSq = Double.POSITIVE_INFINITY;
               }

               if (aggressive || this.crystalTimer.passedMs((double)this.posUpdateDelay.getValueInt())) {
                  this.stage = 0;
                  this.getDistance = (double)100.0F;
                  this.bestDistanceSq = Double.POSITIVE_INFINITY;
                  this.getPos = false;
                  this.getBestPos(pos.method_10086(2));
                  this.getBestPos(pos.method_10084());
               }

               if (aggressive || this.timer.passedMs((double)this.updateDelay.getValueInt())) {
                  if (this.getPos && this.bestPos != null) {
                     this.timer.reset();
                     if (this.debug.getValue()) {
                        String var10001 = String.valueOf(this.bestPos);
                        this.sendMessage("[Debug] PistonPos:" + var10001 + " Facing:" + String.valueOf(this.bestFacing) + " CrystalPos:" + String.valueOf(this.bestOPos.method_10093(this.bestFacing)));
                     }

                     if (aggressive) {
                        int burst = this.burst.getValueInt();

                        for(int i = 0; i < burst; ++i) {
                           this.doPistonAuraAggressive(this.bestPos, this.bestFacing, this.bestOPos);
                        }
                     } else {
                        this.doPistonAura(this.bestPos, this.bestFacing, this.bestOPos);
                     }
                  }

               }
            }
         }
      }
   }

   public boolean check(boolean onlyStatic, boolean onGround, boolean onlyGround) {
      if (MovementUtil.isMoving() && onlyStatic) {
         return true;
      } else if (onGround && onlyGround) {
         return true;
      } else if (this.cachedPowerSlot == -1) {
         return true;
      } else if (this.cachedPistonSlot == -1) {
         return true;
      } else {
         return this.cachedCrystalSlot == -1;
      }
   }

   private boolean checkCrystal(class_2338 pos) {
      if (this.aggressive.getValue() && this.forceBreak.getValue()) {
         return !BlockUtil.getEndCrystals(new class_238(pos)).isEmpty();
      } else {
         for(class_1511 entity : BlockUtil.getEndCrystals(new class_238(pos))) {
            if (AutoCrystal.INSTANCE.calculateDamage(entity.method_19538(), this.target, this.target) > 7.0F) {
               return true;
            }
         }

         return false;
      }
   }

   private boolean checkCrystal2(class_2338 pos) {
      for(class_1511 entity : BlockUtil.getEndCrystals(new class_238(pos))) {
         if (EntityUtil.getEntityPos(entity).equals(pos)) {
            return true;
         }
      }

      return false;
   }

   public String getInfo() {
      return this.target != null ? this.target.method_5477().getString() : null;
   }

   private void getBestPos(class_2338 pos) {
      for(class_2350 i : class_2350.values()) {
         if (i != class_2350.field_11033 && i != class_2350.field_11036) {
            this.getPos(pos, i);
         }
      }

   }

   private void getPos(class_2338 pos, class_2350 i) {
      class_2338 crystalPos = pos.method_10093(i);
      if (this.checkCrystal2(crystalPos) || BlockUtil.canPlaceCrystal(crystalPos)) {
         int sideX = i.method_10165();
         int sideZ = i.method_10148();
         class_2338 base3 = pos.method_10079(i, 3);
         class_2338 base2 = pos.method_10079(i, 2);
         this.getPos(base3, i, pos);
         this.getPos(base3.method_10084(), i, pos);
         this.getPos(base3.method_10069(sideX, 0, sideZ), i, pos);
         this.getPos(base3.method_10069(-sideX, 0, -sideZ), i, pos);
         this.getPos(base3.method_10069(sideX, 1, sideZ), i, pos);
         this.getPos(base3.method_10069(-sideX, 1, -sideZ), i, pos);
         this.getPos(base2, i, pos);
         this.getPos(base2.method_10084(), i, pos);
         this.getPos(base2.method_10069(sideX, 0, sideZ), i, pos);
         this.getPos(base2.method_10069(-sideX, 0, -sideZ), i, pos);
         this.getPos(base2.method_10069(sideX, 1, sideZ), i, pos);
         this.getPos(base2.method_10069(-sideX, 1, -sideZ), i, pos);
      }
   }

   private void getPos(class_2338 pos, class_2350 facing, class_2338 oPos) {
      if (!this.switchPos.getValue() || this.bestPos == null || !this.bestPos.equals(pos) || !mc.field_1687.method_22347(this.bestPos)) {
         if (this.cachedPistonSlot != -1) {
            double eyeX = mc.field_1724.method_23317();
            double eyeY = mc.field_1724.method_23320();
            double eyeZ = mc.field_1724.method_23321();
            double dx = (double)pos.method_10263() + (double)0.5F - eyeX;
            double dy = (double)pos.method_10264() + (double)0.5F - eyeY;
            double dz = (double)pos.method_10260() + (double)0.5F - eyeZ;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (!this.getPos || !(distSq >= this.bestDistanceSq)) {
               class_2248 block = this.getBlock(pos);
               boolean canPlace = !(block instanceof class_2665) && BlockUtil.canPlace(pos, this.placeRange.getValue());
               if (canPlace || block instanceof class_2665) {
                  if (!ClientSetting.INSTANCE.lowVersion.getValue() || block instanceof class_2665 || !(mc.field_1724.method_23318() - (double)pos.method_10264() <= (double)-2.0F) && !(mc.field_1724.method_23318() - (double)pos.method_10264() >= (double)3.0F) || !(BlockUtil.distanceToXZ((double)pos.method_10263() + (double)0.5F, (double)pos.method_10260() + (double)0.5F) < 2.6)) {
                     class_2338 backPos = pos.method_10093(facing.method_10153());
                     class_2248 backBlock = this.getBlock(backPos);
                     if (mc.field_1687.method_22347(backPos) || backBlock == class_2246.field_10036 || backBlock == class_2246.field_10008) {
                        if (backBlock != class_2246.field_10008 || this.checkCrystal2(backPos)) {
                           if (canPlace || this.isPiston(pos, facing)) {
                              this.bestPos = pos;
                              this.bestOPos = oPos;
                              this.bestFacing = facing;
                              this.bestDistanceSq = distSq;
                              this.getDistance = (double)class_3532.method_15355((float)distSq);
                              this.getPos = true;
                              this.crystalTimer.reset();
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void doPistonAura(class_2338 pos, class_2350 facing, class_2338 oPos) {
      if ((double)this.stage >= this.stageSetting.getValue()) {
         this.stage = 0;
      }

      ++this.stage;
      if (mc.field_1687.method_22347(pos)) {
         if (!BlockUtil.canPlace(pos)) {
            return;
         }

         if ((double)this.stage >= this.pistonStage.getValue() && (double)this.stage <= this.pistonMaxStage.getValue()) {
            class_2350 side = BlockUtil.getPlaceSide(pos);
            if (side == null) {
               return;
            }

            int old = mc.field_1724.method_31548().field_7545;
            AutoPush.pistonFacing(facing);
            int piston = this.cachedPistonSlot;
            if (piston == -1) {
               return;
            }

            this.doSwap(piston);
            BlockUtil.placeBlock(pos, false, this.pistonPacket.getValue());
            if (this.inventory.getValue()) {
               this.doSwap(piston);
               EntityUtil.syncInventory();
            } else {
               this.doSwap(old);
            }

            class_2338 neighbour = pos.method_10093(side);
            class_2350 opposite = side.method_10153();
            if (this.rotate.getValue()) {
               Alien.ROTATION.lookAt(neighbour, opposite);
            }
         }
      }

      if ((double)this.stage >= this.powerStage.getValue() && (double)this.stage <= this.powerMaxStage.getValue()) {
         this.doRedStone(pos, facing, oPos.method_10093(facing));
      }

      if ((double)this.stage >= this.crystalStage.getValue() && (double)this.stage <= this.crystalMaxStage.getValue()) {
         this.placeCrystal(oPos, facing);
      }

      if ((double)this.stage >= this.fireStage.getValue() && (double)this.stage <= this.fireMaxStage.getValue()) {
         this.doFire(oPos, facing);
      }

   }

   private void doPistonAuraAggressive(class_2338 pos, class_2350 facing, class_2338 oPos) {
      if (mc.field_1687.method_22347(pos)) {
         if (!BlockUtil.canPlace(pos)) {
            return;
         }

         class_2350 side = BlockUtil.getPlaceSide(pos);
         if (side == null) {
            return;
         }

         int old = mc.field_1724.method_31548().field_7545;
         AutoPush.pistonFacing(facing);
         int piston = this.cachedPistonSlot;
         if (piston == -1) {
            return;
         }

         this.doSwap(piston);
         BlockUtil.placeBlock(pos, false, this.pistonPacket.getValue());
         if (this.inventory.getValue()) {
            this.doSwap(piston);
            EntityUtil.syncInventory();
         } else {
            this.doSwap(old);
         }

         class_2338 neighbour = pos.method_10093(side);
         class_2350 opposite = side.method_10153();
         if (this.rotate.getValue()) {
            Alien.ROTATION.lookAt(neighbour, opposite);
         }

         if (mc.field_1687.method_22347(pos)) {
            return;
         }
      }

      this.doRedStone(pos, facing, oPos.method_10093(facing));
      this.placeCrystal(oPos, facing);
      this.doFire(oPos, facing);
   }

   private void placeCrystal(class_2338 pos, class_2350 facing) {
      if (BlockUtil.canPlaceCrystal(pos.method_10093(facing))) {
         int crystal = this.cachedCrystalSlot;
         if (crystal != -1) {
            int old = mc.field_1724.method_31548().field_7545;
            this.doSwap(crystal);
            BlockUtil.placeCrystal(pos.method_10093(facing), true);
            if (this.inventory.getValue()) {
               this.doSwap(crystal);
               EntityUtil.syncInventory();
            } else {
               this.doSwap(old);
            }

         }
      }
   }

   private boolean isPiston(class_2338 pos, class_2350 facing) {
      if (!(mc.field_1687.method_8320(pos).method_26204() instanceof class_2665)) {
         return false;
      } else if (((class_2350)mc.field_1687.method_8320(pos).method_11654(class_2318.field_10927)).method_10153() != facing) {
         return false;
      } else {
         return mc.field_1687.method_22347(pos.method_10079(facing, -1)) || this.getBlock(pos.method_10079(facing, -1)) == class_2246.field_10036 || this.getBlock(pos.method_10093(facing.method_10153())) == class_2246.field_10008;
      }
   }

   private void doFire(class_2338 pos, class_2350 facing) {
      if (this.fire.getValue()) {
         int fire = this.cachedFireSlot;
         if (fire != -1) {
            int old = mc.field_1724.method_31548().field_7545;
            int[] xOffset = new int[]{0, facing.method_10165(), -facing.method_10165()};
            int[] yOffset = new int[]{0, 1};
            int[] zOffset = new int[]{0, facing.method_10148(), -facing.method_10148()};

            for(int x : xOffset) {
               for(int y : yOffset) {
                  for(int z : zOffset) {
                     if (this.getBlock(pos.method_10069(x, y, z)) == class_2246.field_10036) {
                        return;
                     }
                  }
               }
            }

            for(int x : xOffset) {
               for(int y : yOffset) {
                  for(int z : zOffset) {
                     if (canFire(pos.method_10069(x, y, z))) {
                        this.doSwap(fire);
                        this.placeFire(pos.method_10069(x, y, z));
                        if (this.inventory.getValue()) {
                           this.doSwap(fire);
                           EntityUtil.syncInventory();
                        } else {
                           this.doSwap(old);
                        }

                        return;
                     }
                  }
               }
            }

         }
      }
   }

   public void placeFire(class_2338 pos) {
      class_2338 neighbour = pos.method_10093(class_2350.field_11033);
      BlockUtil.clickBlock(neighbour, class_2350.field_11036, this.rotate.getValue());
   }

   private void doRedStone(class_2338 pos, class_2350 facing, class_2338 crystalPos) {
      class_2338 backPos = pos.method_10093(facing.method_10153());
      class_2248 backBlock = this.getBlock(backPos);
      if (mc.field_1687.method_22347(backPos) || backBlock == class_2246.field_10036 || backBlock == class_2246.field_10008) {
         for(class_2350 i : class_2350.values()) {
            if (this.getBlock(pos.method_10093(i)) == class_2246.field_10002) {
               return;
            }
         }

         int power = this.cachedPowerSlot;
         if (power != -1) {
            if (backBlock != class_2246.field_10008 || this.checkCrystal2(backPos)) {
               int old = mc.field_1724.method_31548().field_7545;
               class_2350 bestNeighboring = BlockUtil.getBestNeighboring(pos, facing);
               if (bestNeighboring != null && bestNeighboring != facing.method_10153() && BlockUtil.canPlace(pos.method_10093(bestNeighboring), this.placeRange.getValue()) && !pos.method_10093(bestNeighboring).equals(crystalPos)) {
                  this.doSwap(power);
                  BlockUtil.placeBlock(pos.method_10093(bestNeighboring), this.rotate.getValue());
                  if (this.inventory.getValue()) {
                     this.doSwap(power);
                     EntityUtil.syncInventory();
                  } else {
                     this.doSwap(old);
                  }

               } else {
                  for(class_2350 i : class_2350.values()) {
                     if (BlockUtil.canPlace(pos.method_10093(i), this.placeRange.getValue()) && !pos.method_10093(i).equals(crystalPos) && i != facing.method_10153()) {
                        this.doSwap(power);
                        BlockUtil.placeBlock(pos.method_10093(i), this.rotate.getValue());
                        if (this.inventory.getValue()) {
                           this.doSwap(power);
                           EntityUtil.syncInventory();
                        } else {
                           this.doSwap(old);
                        }

                        return;
                     }
                  }

               }
            }
         }
      }
   }

   private void doSwap(int slot) {
      if (this.inventory.getValue()) {
         InventoryUtil.inventorySwap(slot, mc.field_1724.method_31548().field_7545);
      } else {
         InventoryUtil.switchToSlot(slot);
      }

   }

   private void refreshSlots() {
      if (mc.field_1724 != null) {
         int tick = mc.field_1724.field_6012;
         if (this.slotCacheTick != tick) {
            this.slotCacheTick = tick;
            this.cachedPistonSlot = this.findClass(class_2665.class);
            this.cachedPowerSlot = this.findBlock(class_2246.field_10002);
            this.cachedCrystalSlot = this.findItem(class_1802.field_8301);
            this.cachedFireSlot = this.findItem(class_1802.field_8884);
         }
      }
   }

   public int findItem(class_1792 itemIn) {
      return this.inventory.getValue() ? InventoryUtil.findItemInventorySlot(itemIn) : InventoryUtil.findItem(itemIn);
   }

   public int findBlock(class_2248 blockIn) {
      return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(blockIn) : InventoryUtil.findBlock(blockIn);
   }

   public int findClass(Class<?> clazz) {
      return this.inventory.getValue() ? InventoryUtil.findClassInventorySlot(clazz) : InventoryUtil.findClass(clazz);
   }

   private class_2248 getBlock(class_2338 pos) {
      return mc.field_1687.method_8320(pos).method_26204();
   }
}
