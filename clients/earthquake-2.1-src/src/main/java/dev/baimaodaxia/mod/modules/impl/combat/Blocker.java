package dev.baimaodaxia.mod.modules.impl.combat;

import dev.baimaodaxia.Alien;
import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.UpdateEvent;
import dev.baimaodaxia.api.utils.combat.CombatUtil;
import dev.baimaodaxia.api.utils.math.Timer;
import dev.baimaodaxia.api.utils.player.EntityUtil;
import dev.baimaodaxia.api.utils.player.InventoryUtil;
import dev.baimaodaxia.api.utils.world.BlockPosX;
import dev.baimaodaxia.api.utils.world.BlockUtil;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.impl.client.AntiCheat;
import dev.baimaodaxia.mod.modules.impl.exploit.Blink;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;

public class Blocker extends Module {
   public static Blocker INSTANCE;
   private final EnumSetting<Page> page;
   final List<BlockPos> placePos;
   final List<BlockPos> blockerPos;
   final List<BlockPos> list;
   private final Timer timer;
   private final SliderSetting delay;
   private final SliderSetting blocksPer;
   private final BooleanSetting rotate;
   private final BooleanSetting breakCrystal;
   private final BooleanSetting inventorySwap;
   private final BooleanSetting bevelCev;
   private final BooleanSetting burrow;
   private final BooleanSetting face;
   private final BooleanSetting faceUp;
   private final BooleanSetting feet;
   private final BooleanSetting extend;
   private final BooleanSetting onlySurround;
   private final BooleanSetting inAirPause;
   private final BooleanSetting detectMining;
   private final BooleanSetting eatingPause;
   private int placeProgress;
   private BlockPos playerBP;

   public Blocker() {
      super("Blocker", Module.Category.Combat);
      this.page = this.add(new EnumSetting("Page", Blocker.Page.General));
      this.placePos = new ArrayList();
      this.blockerPos = new ArrayList();
      this.list = new ArrayList();
      this.timer = new Timer();
      this.delay = this.add(new SliderSetting("PlaceDelay", 50, 0, 500, () -> this.page.getValue() == Blocker.Page.General));
      this.blocksPer = this.add(new SliderSetting("BlocksPer", 1, 1, 8, () -> this.page.getValue() == Blocker.Page.General));
      this.rotate = this.add(new BooleanSetting("Rotate", true, () -> this.page.getValue() == Blocker.Page.General));
      this.breakCrystal = this.add(new BooleanSetting("Break", true, () -> this.page.getValue() == Blocker.Page.General));
      this.inventorySwap = this.add(new BooleanSetting("InventorySwap", true, () -> this.page.getValue() == Blocker.Page.General));
      this.bevelCev = this.add(new BooleanSetting("BevelCev", true, () -> this.page.getValue() == Blocker.Page.Target));
      this.burrow = this.add(new BooleanSetting("Burrow", true, () -> this.page.getValue() == Blocker.Page.Target));
      this.face = this.add((new BooleanSetting("Face", true, () -> this.page.getValue() == Blocker.Page.Target)).setParent());
      this.faceUp = this.add(new BooleanSetting("FaceUp", false, () -> this.page.getValue() == Blocker.Page.Target && this.face.isOpen()));
      this.feet = this.add((new BooleanSetting("Feet", true, () -> this.page.getValue() == Blocker.Page.Target)).setParent());
      this.extend = this.add(new BooleanSetting("Extend", false, () -> this.page.getValue() == Blocker.Page.Target && this.feet.isOpen()));
      this.onlySurround = this.add(new BooleanSetting("OnlySurround", true, () -> this.page.getValue() == Blocker.Page.Target && this.feet.isOpen()));
      this.inAirPause = this.add(new BooleanSetting("InAirPause", false, () -> this.page.getValue() == Blocker.Page.Check));
      this.detectMining = this.add(new BooleanSetting("DetectMining", true, () -> this.page.getValue() == Blocker.Page.Check));
      this.eatingPause = this.add(new BooleanSetting("EatingPause", true, () -> this.page.getValue() == Blocker.Page.Check));
      this.placeProgress = 0;
      this.setChinese("水晶阻挡");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.list.clear();
      if (!this.inventorySwap.getValue() || EntityUtil.inInventory()) {
         if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
            if (this.timer.passedMs(this.delay.getValue())) {
               if (!this.eatingPause.getValue() || !mc.player.isUsingItem()) {
                  this.placeProgress = 0;
                  if (this.playerBP != null && !this.playerBP.equals(EntityUtil.getPlayerPos(true))) {
                     this.placePos.clear();
                     this.blockerPos.clear();
                  }

                  this.playerBP = EntityUtil.getPlayerPos(true);
                  double[] offset = new double[]{AntiCheat.getOffset(), -AntiCheat.getOffset(), (double)0.0F};
                  if (this.bevelCev.getValue()) {
                     for(Direction i : Direction.values()) {
                        BlockPos blockerPos;
                        if (i != Direction.DOWN && !this.isBedrock(this.playerBP.offset(i).up()) && this.crystalHere(blockerPos = this.playerBP.offset(i).up(2)) && !this.placePos.contains(blockerPos)) {
                           this.placePos.add(blockerPos);
                        }
                     }
                  }

                  if (this.face.getValue() && (!this.onlySurround.getValue() || Surround.INSTANCE.isOn() || SelfTrap.INSTANCE.isOn())) {
                     for(double x : offset) {
                        for(double z : offset) {
                           for(Direction i : Direction.values()) {
                              for(int d = 0; d < 3; ++d) {
                                 BlockPos aroundPos = (new BlockPosX(mc.player.getX() + x, mc.player.getY() + (double)0.5F, mc.player.getZ() + z)).offset(i, 1).up();
                                 BlockPos blockerPos2 = (new BlockPosX(mc.player.getX() + x, mc.player.getY() + (double)0.5F, mc.player.getZ() + z)).offset(i, d).up();
                                 if (this.crystalHere(blockerPos2) && !this.placePos.contains(blockerPos2) && !Alien.HOLE.isHard(aroundPos)) {
                                    this.placePos.add(blockerPos2);
                                 }
                              }
                           }
                        }
                     }

                     if (this.faceUp.getValue()) {
                        for(Direction i : Direction.values()) {
                           BlockPos blockerPos;
                           if (i != Direction.DOWN && !this.isBedrock(this.playerBP.offset(i).up()) && this.crystalHere(blockerPos = this.playerBP.offset(i).up(2)) && !this.placePos.contains(blockerPos)) {
                              this.placePos.add(blockerPos);
                           }
                        }
                     }
                  }

                  if (this.getObsidian() != -1) {
                     this.placePos.removeIf((pos) -> !BlockUtil.clientCanPlace(pos, true));
                     if (this.burrow.getValue()) {
                        for(double x : offset) {
                           for(double z : offset) {
                              BlockPosX surroundPos = new BlockPosX(mc.player.getX() + x, mc.player.getY(), mc.player.getZ() + z);
                              if (!this.isBedrock(surroundPos) && Alien.BREAK.isMining(surroundPos)) {
                                 for(Direction direction : Direction.values()) {
                                    if (direction != Direction.DOWN && direction != Direction.UP) {
                                       BlockPos defensePos = surroundPos.offset(direction);
                                       if (!this.detectMining.getValue() || !Alien.BREAK.isMining(defensePos)) {
                                          if (this.breakCrystal.getValue()) {
                                             CombatUtil.attackCrystal(defensePos, this.rotate.getValue(), false);
                                          }

                                          if (BlockUtil.canPlace(defensePos, (double)6.0F, this.breakCrystal.getValue())) {
                                             this.blockerPos.add(defensePos);
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }

                     if (this.feet.getValue() && (!this.onlySurround.getValue() || Surround.INSTANCE.isOn() || SelfTrap.INSTANCE.isOn())) {
                        for(double x : offset) {
                           for(double z : offset) {
                              for(Direction i : Direction.values()) {
                                 BlockPos surroundPos = (new BlockPosX(mc.player.getX() + x, mc.player.getY() + (double)0.5F, mc.player.getZ() + z)).offset(i);
                                 if (!this.isBedrock(surroundPos) && Alien.BREAK.isMining(surroundPos)) {
                                    for(Direction direction : Direction.values()) {
                                       BlockPos defensePos = surroundPos.offset(direction);
                                       if (!this.detectMining.getValue() || !Alien.BREAK.isMining(defensePos)) {
                                          if (this.breakCrystal.getValue()) {
                                             CombatUtil.attackCrystal(defensePos, this.rotate.getValue(), false);
                                          }

                                          if (BlockUtil.canPlace(defensePos, (double)6.0F, this.breakCrystal.getValue())) {
                                             this.blockerPos.add(defensePos);
                                          } else if (BlockUtil.canReplace(defensePos) && !BlockUtil.hasEntity(defensePos, true) && this.getHelper(defensePos) != null) {
                                             this.blockerPos.add(this.getHelper(defensePos));
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }

                     if (this.feet.getValue() && this.extend.getValue() && (!this.onlySurround.getValue() || Surround.INSTANCE.isOn() || SelfTrap.INSTANCE.isOn())) {
                        for(double x : offset) {
                           for(double z : offset) {
                              for(Direction i : Direction.values()) {
                                 BlockPos surroundPos;
                                 if (i != Direction.UP && i != Direction.DOWN && !this.isBedrock(surroundPos = (new BlockPosX(mc.player.getX() + x, mc.player.getY() + (double)0.5F, mc.player.getZ() + z)).offset(i))) {
                                    for(Direction direction : Direction.values()) {
                                       BlockPos blockPos;
                                       if (direction != Direction.UP && direction != Direction.DOWN && AutoCrystal.INSTANCE.canPlaceCrystal(blockPos = surroundPos.offset(direction), true, true) && (!this.detectMining.getValue() || !Alien.BREAK.isMining(blockPos))) {
                                          if (this.breakCrystal.getValue()) {
                                             CombatUtil.attackCrystal(blockPos, this.rotate.getValue(), false);
                                          }

                                          if (BlockUtil.canPlace(blockPos, (double)6.0F, this.breakCrystal.getValue())) {
                                             this.blockerPos.add(blockPos);
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }

                     this.blockerPos.removeIf((pos) -> !BlockUtil.clientCanPlace(pos, true));
                     if (!this.inAirPause.getValue() || mc.player.isOnGround()) {
                        if (!this.blockerPos.isEmpty()) {
                           int n = mc.player.getInventory().selectedSlot;
                           int block = this.getObsidian();
                           if (block != -1) {
                              this.doSwap(block);

                              for(BlockPos defensePos : this.blockerPos) {
                                 if (BlockUtil.canPlace(defensePos, (double)6.0F, this.breakCrystal.getValue())) {
                                    this.doPlace(defensePos);
                                 }
                              }

                              if (this.inventorySwap.getValue()) {
                                 this.doSwap(block);
                                 EntityUtil.syncInventory();
                              } else {
                                 this.doSwap(n);
                              }

                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public BlockPos getHelper(BlockPos pos) {
      for(Direction i : Direction.values()) {
         if (i != Direction.UP && (!this.detectMining.getValue() || !Alien.BREAK.isMining(pos.offset(i))) && BlockUtil.isStrictDirection(pos.offset(i), i.getOpposite()) && BlockUtil.canPlace(pos.offset(i), (double)6.0F)) {
            return pos.offset(i);
         }
      }

      return null;
   }

   private boolean crystalHere(BlockPos pos) {
      return BlockUtil.getEndCrystals(new Box(pos)).stream().anyMatch((entity) -> entity.getBlockPos().equals(pos));
   }

   private boolean isBedrock(BlockPos pos) {
      return mc.world.getBlockState(pos).getBlock() == Blocks.BEDROCK;
   }

   private void doPlace(BlockPos pos) {
      if (!this.list.contains(pos)) {
         this.list.add(pos);
         if ((double)this.placeProgress < this.blocksPer.getValue()) {
            BlockUtil.placeBlock(pos, this.rotate.getValue());
            this.timer.reset();
            ++this.placeProgress;
         }
      }
   }

   private void tryPlaceObsidian(BlockPos pos) {
      if (!this.list.contains(pos)) {
         this.list.add(pos);
         if ((double)this.placeProgress < this.blocksPer.getValue()) {
            if (!this.detectMining.getValue() || !Alien.BREAK.isMining(pos)) {
               int oldSlot = mc.player.getInventory().selectedSlot;
               int block = this.getObsidian();
               if (block != -1) {
                  this.doSwap(block);
                  BlockUtil.placeBlock(pos, this.rotate.getValue());
                  this.timer.reset();
                  if (this.inventorySwap.getValue()) {
                     this.doSwap(block);
                     EntityUtil.syncInventory();
                  } else {
                     this.doSwap(oldSlot);
                  }

                  ++this.placeProgress;
               }
            }
         }
      }
   }

   private void doSwap(int slot) {
      if (this.inventorySwap.getValue()) {
         InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
      } else {
         InventoryUtil.switchToSlot(slot);
      }

   }

   private int getObsidian() {
      return this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN) : InventoryUtil.findBlock(Blocks.OBSIDIAN);
   }

   public static enum Page {
      General,
      Target,
      Check;

      // $FF: synthetic method
      private static Page[] $values() {
         return new Page[]{General, Target, Check};
      }
   }
}
