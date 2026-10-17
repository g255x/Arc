package dev.baimaodaxia.mod.modules.impl.combat;

import dev.baimaodaxia.Alien;
import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.UpdateEvent;
import dev.baimaodaxia.api.utils.combat.CombatUtil;
import dev.baimaodaxia.api.utils.math.MathUtil;
import dev.baimaodaxia.api.utils.math.Timer;
import dev.baimaodaxia.api.utils.player.EntityUtil;
import dev.baimaodaxia.api.utils.player.InventoryUtil;
import dev.baimaodaxia.api.utils.world.BlockPosX;
import dev.baimaodaxia.api.utils.world.BlockUtil;
import dev.baimaodaxia.core.impl.RotationManager;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.impl.client.ClientSetting;
import dev.baimaodaxia.mod.modules.impl.exploit.Blink;
import dev.baimaodaxia.mod.modules.impl.player.PacketMine;
import dev.baimaodaxia.mod.modules.settings.impl.BindSetting;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.block.FacingBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.BlockState;

public class HolePush extends Module {
   public static HolePush INSTANCE;
   private static final float[] OFFSETS = new float[]{-0.25F, 0.0F, 0.25F};
   private static final Direction[] HORIZONTAL_DIRECTIONS;
   private final EnumSetting<Page> page;
   private final BooleanSetting torch;
   private final BooleanSetting rotate;
   private final BooleanSetting yawDeceive;
   private final BooleanSetting allowWeb;
   public final BindSetting allowKey;
   private final BooleanSetting noEating;
   private final BooleanSetting selfGround;
   private final BooleanSetting onlyGround;
   private final BooleanSetting onlyNoInside;
   private final BooleanSetting onlyInside;
   private final SliderSetting surroundCheck;
   private final BooleanSetting syncCrystal;
   private final BooleanSetting pistonPacket;
   private final BooleanSetting powerPacket;
   private final BooleanSetting mine;
   private final BooleanSetting pauseCity;
   private final SliderSetting updateDelay;
   private final BooleanSetting autoDisable;
   private final SliderSetting range;
   private final SliderSetting placeRange;
   private final BooleanSetting inventory;
   private final Timer timer;

   public HolePush() {
      super("HolePush", Module.Category.Combat);
      this.page = this.add(new EnumSetting("Page", HolePush.Page.General));
      this.torch = this.add(new BooleanSetting("Torch", false, () -> this.page.is(HolePush.Page.General)));
      this.rotate = this.add(new BooleanSetting("Rotate", true, () -> this.page.is(HolePush.Page.Rotate)));
      this.yawDeceive = this.add(new BooleanSetting("YawDeceive", true, () -> this.page.is(HolePush.Page.Rotate)));
      this.allowWeb = this.add(new BooleanSetting("AllowWeb", true, () -> this.page.is(HolePush.Page.Check)));
      this.allowKey = this.add(new BindSetting("AllowKey", -1, () -> !this.allowWeb.getValue() && this.page.is(HolePush.Page.Check)));
      this.noEating = this.add(new BooleanSetting("EatingPause", true, () -> this.page.is(HolePush.Page.Check)));
      this.selfGround = this.add(new BooleanSetting("SelfGround", true, () -> this.page.is(HolePush.Page.Check)));
      this.onlyGround = this.add(new BooleanSetting("OnlyGround", false, () -> this.page.is(HolePush.Page.Check)));
      this.onlyNoInside = this.add(new BooleanSetting("OnlyNoInside", false, () -> this.page.is(HolePush.Page.Check)));
      this.onlyInside = this.add(new BooleanSetting("OnlyInside", true, () -> this.page.is(HolePush.Page.Check)));
      this.surroundCheck = this.add(new SliderSetting("SurroundCheck", 0, 0, 4, () -> this.page.is(HolePush.Page.Check)));
      this.syncCrystal = this.add(new BooleanSetting("SyncCrystal", true, () -> this.page.is(HolePush.Page.Check)));
      this.pistonPacket = this.add(new BooleanSetting("PistonPacket", true, () -> this.page.is(HolePush.Page.General)));
      this.powerPacket = this.add(new BooleanSetting("PowerPacket", true, () -> this.page.is(HolePush.Page.General)));
      this.mine = this.add(new BooleanSetting("Mine", true, () -> this.page.is(HolePush.Page.General)));
      this.pauseCity = this.add(new BooleanSetting("PauseCity", true, () -> this.mine.isOpen() && this.page.is(HolePush.Page.General)));
      this.updateDelay = this.add(new SliderSetting("Delay", 200, 0, 1000, () -> this.page.is(HolePush.Page.General)));
      this.autoDisable = this.add(new BooleanSetting("AutoDisable", true, () -> this.page.is(HolePush.Page.General)));
      this.range = this.add(new SliderSetting("Range", 4.7, (double)0.0F, (double)6.0F, () -> this.page.is(HolePush.Page.General)));
      this.placeRange = this.add(new SliderSetting("PlaceRange", 4.7, (double)0.0F, (double)6.0F, () -> this.page.is(HolePush.Page.General)));
      this.inventory = this.add(new BooleanSetting("InventorySwap", true, () -> this.page.is(HolePush.Page.General)));
      this.timer = new Timer();
      this.setChinese("坑推");
      INSTANCE = this;
   }

   public static void pistonFacing(Direction i) {
      if (i == Direction.EAST) {
         Alien.ROTATION.snapAt(-90.0F, 5.0F);
      } else if (i == Direction.WEST) {
         Alien.ROTATION.snapAt(90.0F, 5.0F);
      } else if (i == Direction.NORTH) {
         Alien.ROTATION.snapAt(180.0F, 5.0F);
      } else if (i == Direction.SOUTH) {
         Alien.ROTATION.snapAt(0.0F, 5.0F);
      }

   }

   public void onEnable() {
      if (AutoCrystal.INSTANCE != null) {
         AutoCrystal.INSTANCE.lastBreakTimer.reset();
      }

   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.timer.passedMs(this.updateDelay.getValue())) {
         if (!this.selfGround.getValue() || mc.player.isOnGround()) {
            Block blockType = this.getBlockType();
            int powerSlot = this.findBlock(blockType);
            int pistonSlot = this.findClass(PistonBlock.class);
            if (powerSlot != -1 && pistonSlot != -1) {
               if (!this.syncCrystal.getValue() || AutoCrystal.INSTANCE == null || AutoCrystal.INSTANCE.crystalPos == null) {
                  if (!this.noEating.getValue() || !mc.player.isUsingItem()) {
                     if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
                        for(PlayerEntity player : CombatUtil.getEnemies(this.range.getValue())) {
                           if (this.canPush(player)) {
                              BlockPos basePos = EntityUtil.getEntityPos(player);

                              for(Direction i : HORIZONTAL_DIRECTIONS) {
                                 BlockPos pos2;
                                 if (this.isTargetHere(pos2 = basePos.offset(i), player) && mc.world.canCollide(player, new Box(pos2))) {
                                    if (this.tryPush(basePos.offset(i.getOpposite()), i)) {
                                       this.timer.reset();
                                       return;
                                    }

                                    if (this.tryPush(basePos.offset(i.getOpposite()).up(), i)) {
                                       this.timer.reset();
                                       return;
                                    }
                                 }
                              }

                              for(float x : OFFSETS) {
                                 for(float z : OFFSETS) {
                                    BlockPosX playerPos = new BlockPosX(player.getX() + (double)x, player.getY() + (double)0.5F, player.getZ() + (double)z);

                                    for(Direction i : HORIZONTAL_DIRECTIONS) {
                                       BlockPos pos;
                                       if (this.isTargetHere(pos = playerPos.offset(i), player) && mc.world.canCollide(player, new Box(pos))) {
                                          if (this.tryPush(playerPos.offset(i.getOpposite()), i)) {
                                             this.timer.reset();
                                             return;
                                          }

                                          if (this.tryPush(playerPos.offset(i.getOpposite()).up(), i)) {
                                             this.timer.reset();
                                             return;
                                          }
                                       }
                                    }
                                 }
                              }

                              if (!mc.world.canCollide(player, new Box(new BlockPosX(player.getX(), player.getY() + (double)2.5F, player.getZ())))) {
                                 for(Direction i : HORIZONTAL_DIRECTIONS) {
                                    BlockPos pos3 = basePos.offset(i);
                                    Box box = player.getBoundingBox().offset(new Vec3d((double)i.getOffsetX(), (double)i.getOffsetY(), (double)i.getOffsetZ()));
                                    if (this.getBlock(pos3.up()) != Blocks.PISTON_HEAD && !mc.world.canCollide(player, box.offset((double)0.0F, (double)1.0F, (double)0.0F)) && !this.isTargetHere(pos3, player)) {
                                       if (this.tryPush(basePos.offset(i.getOpposite()).up(), i)) {
                                          this.timer.reset();
                                          return;
                                       }

                                       if (this.tryPush(basePos.offset(i.getOpposite()), i)) {
                                          this.timer.reset();
                                          return;
                                       }
                                    }
                                 }
                              }

                              for(float x : OFFSETS) {
                                 for(float z : OFFSETS) {
                                    BlockPosX playerPos = new BlockPosX(player.getX() + (double)x, player.getY() + (double)0.5F, player.getZ() + (double)z);

                                    for(Direction i : HORIZONTAL_DIRECTIONS) {
                                       if (this.isTargetHere(playerPos.offset(i), player)) {
                                          if (this.tryPush(playerPos.offset(i.getOpposite()).up(), i)) {
                                             this.timer.reset();
                                             return;
                                          }

                                          if (this.tryPush(playerPos.offset(i.getOpposite()), i)) {
                                             this.timer.reset();
                                             return;
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
            } else {
               if (this.autoDisable.getValue()) {
                  this.disable();
               }

            }
         }
      }
   }

   private boolean tryPush(BlockPos piston, Direction direction) {
      Block blockType = this.getBlockType();
      if (!mc.world.isAir(piston.offset(direction))) {
         return false;
      } else {
         if (this.isTrueFacing(piston, direction) && this.facingCheck(piston) && BlockUtil.clientCanPlace(piston, false)) {
            boolean canPower = false;
            if (BlockUtil.getPlaceSide(piston, this.placeRange.getValue()) != null) {
               CombatUtil.modifyPos = piston;
               CombatUtil.modifyBlockState = Blocks.PISTON.getDefaultState();

               for(Direction direction2 : Direction.values()) {
                  if (this.getBlock(piston.offset(direction2)) == blockType) {
                     canPower = true;
                     break;
                  }
               }

               for(Direction direction3 : Direction.values()) {
                  if (canPower) {
                     break;
                  }

                  if (BlockUtil.canPlace(piston.offset(direction3), this.placeRange.getValue())) {
                     canPower = true;
                  }
               }

               CombatUtil.modifyPos = null;
               if (canPower) {
                  int pistonSlot = this.findClass(PistonBlock.class);
                  Direction side = BlockUtil.getPlaceSide(piston, this.placeRange.getValue());
                  if (side != null) {
                     if (this.rotate.getValue()) {
                        Alien.ROTATION.lookAt(piston.offset(side), side.getOpposite());
                     }

                     if (this.yawDeceive.getValue()) {
                        pistonFacing(direction.getOpposite());
                     }

                     int old = mc.player.getInventory().selectedSlot;
                     this.doSwap(pistonSlot);
                     BlockUtil.placeBlock(piston, false, this.pistonPacket.getValue());
                     if (this.inventory.getValue()) {
                        this.doSwap(pistonSlot);
                        EntityUtil.syncInventory();
                     } else {
                        this.doSwap(old);
                     }

                     if (this.rotate.getValue() && this.yawDeceive.getValue()) {
                        Alien.ROTATION.lookAt(piston.offset(side), side.getOpposite());
                     }

                     if (this.rotate.getValue()) {
                        Alien.ROTATION.snapBack();
                     }

                     for(Direction i : Direction.values()) {
                        if (this.getBlock(piston.offset(i)) == blockType) {
                           if (this.shouldMine()) {
                              PacketMine.INSTANCE.mine(piston.offset(i));
                           }

                           if (this.autoDisable.getValue()) {
                              this.disable();
                           }

                           return true;
                        }
                     }

                     for(Direction i : Direction.values()) {
                        if ((i != Direction.UP || !this.torch.getValue()) && BlockUtil.canPlace(piston.offset(i), this.placeRange.getValue())) {
                           int oldSlot = mc.player.getInventory().selectedSlot;
                           int powerSlot = this.findBlock(blockType);
                           this.doSwap(powerSlot);
                           BlockUtil.placeBlock(piston.offset(i), this.rotate.getValue(), this.powerPacket.getValue());
                           if (this.inventory.getValue()) {
                              this.doSwap(powerSlot);
                              EntityUtil.syncInventory();
                           } else {
                              this.doSwap(oldSlot);
                           }

                           if (this.shouldMine()) {
                              PacketMine.INSTANCE.mine(piston.offset(i));
                           }

                           return true;
                        }
                     }

                     return true;
                  }
               }
            } else {
               Direction powerFacing = null;

               for(Direction i : Direction.values()) {
                  if (i != Direction.UP || !this.torch.getValue()) {
                     if (powerFacing != null) {
                        break;
                     }

                     CombatUtil.modifyPos = piston.offset(i);
                     CombatUtil.modifyBlockState = blockType.getDefaultState();
                     if (BlockUtil.getPlaceSide(piston, this.placeRange.getValue()) != null) {
                        powerFacing = i;
                     }

                     CombatUtil.modifyPos = null;
                     if (powerFacing != null && !BlockUtil.canPlace(piston.offset(powerFacing))) {
                        powerFacing = null;
                     }
                  }
               }

               if (powerFacing != null) {
                  int oldSlot = mc.player.getInventory().selectedSlot;
                  int powerSlot = this.findBlock(blockType);
                  this.doSwap(powerSlot);
                  BlockUtil.placeBlock(piston.offset(powerFacing), this.rotate.getValue(), this.powerPacket.getValue());
                  if (this.inventory.getValue()) {
                     this.doSwap(powerSlot);
                     EntityUtil.syncInventory();
                  } else {
                     this.doSwap(oldSlot);
                  }

                  CombatUtil.modifyPos = piston.offset(powerFacing);
                  CombatUtil.modifyBlockState = blockType.getDefaultState();
                  int pistonSlot = this.findClass(PistonBlock.class);
                  Direction side2 = BlockUtil.getPlaceSide(piston, this.placeRange.getValue());
                  if (side2 != null) {
                     if (this.rotate.getValue()) {
                        Alien.ROTATION.lookAt(piston.offset(side2), side2.getOpposite());
                     }

                     if (this.yawDeceive.getValue()) {
                        pistonFacing(direction.getOpposite());
                     }

                     int old2 = mc.player.getInventory().selectedSlot;
                     this.doSwap(pistonSlot);
                     BlockUtil.placeBlock(piston, false, this.pistonPacket.getValue());
                     if (this.inventory.getValue()) {
                        this.doSwap(pistonSlot);
                        EntityUtil.syncInventory();
                     } else {
                        this.doSwap(old2);
                     }

                     if (this.rotate.getValue() && this.yawDeceive.getValue()) {
                        Alien.ROTATION.lookAt(piston.offset(side2), side2.getOpposite());
                     }

                     if (this.rotate.getValue()) {
                        Alien.ROTATION.snapBack();
                     }
                  }

                  CombatUtil.modifyPos = null;
                  return true;
               }
            }
         }

         if (mc.world.getBlockState(piston).getBlock() instanceof PistonBlock && this.getBlockState(piston).get(FacingBlock.FACING) == direction) {
            for(Direction direction4 : Direction.values()) {
               if (this.getBlock(piston.offset(direction4)) == blockType) {
                  if (this.autoDisable.getValue()) {
                     this.disable();
                     return true;
                  }

                  return false;
               }
            }

            for(Direction direction5 : Direction.values()) {
               if ((direction5 != Direction.UP || !this.torch.getValue()) && BlockUtil.canPlace(piston.offset(direction5), this.placeRange.getValue())) {
                  int oldSlot = mc.player.getInventory().selectedSlot;
                  int powerSlot = this.findBlock(blockType);
                  this.doSwap(powerSlot);
                  BlockUtil.placeBlock(piston.offset(direction5), this.rotate.getValue(), this.powerPacket.getValue());
                  if (this.inventory.getValue()) {
                     this.doSwap(powerSlot);
                     EntityUtil.syncInventory();
                  } else {
                     this.doSwap(oldSlot);
                  }

                  return true;
               }
            }
         }

         return false;
      }
   }

   public String getInfo() {
      if (this.syncCrystal.getValue() && AutoCrystal.INSTANCE != null && AutoCrystal.INSTANCE.crystalPos != null) {
         return "WaitSync";
      } else {
         return !this.allowWeb.getValue() && !this.allowKey.isPressed() ? "SmartPush" : "CrazyDog";
      }
   }

   private boolean shouldMine() {
      if (!this.mine.getValue()) {
         return false;
      } else if (!this.pauseCity.getValue()) {
         return true;
      } else {
         BlockPos breakPos = PacketMine.getBreakPos();
         return breakPos != null && breakPos.equals(PacketMine.secondPos);
      }
   }

   private boolean facingCheck(BlockPos pos) {
      if (!ClientSetting.INSTANCE.lowVersion.getValue()) {
         return true;
      } else {
         Direction direction = MathUtil.getDirectionFromEntityLiving(pos, mc.player);
         return direction != Direction.UP && direction != Direction.DOWN;
      }
   }

   private boolean isTrueFacing(BlockPos pos, Direction facing) {
      if (this.yawDeceive.getValue()) {
         return true;
      } else {
         Direction side = BlockUtil.getPlaceSide(pos);
         if (side == null) {
            return false;
         } else {
            Vec3d directionVec = new Vec3d((double)pos.getX() + (double)0.5F + (double)side.getVector().getX() * (double)0.5F, (double)pos.getY() + (double)0.5F + (double)side.getVector().getY() * (double)0.5F, (double)pos.getZ() + (double)0.5F + (double)side.getVector().getZ() * (double)0.5F);
            float[] rotation = RotationManager.getRotation(directionVec);
            return MathUtil.getFacingOrder(rotation[0], rotation[1]).getOpposite() == facing;
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

   public int findBlock(Block blockIn) {
      return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(blockIn) : InventoryUtil.findBlock(blockIn);
   }

   public int findClass(Class<?> clazz) {
      return this.inventory.getValue() ? InventoryUtil.findClassInventorySlot(clazz) : InventoryUtil.findClass(clazz);
   }

   private boolean burrowUpdate(PlayerEntity player) {
      for(float x : new float[]{0.0F, 0.3F, -0.3F}) {
         for(float z : new float[]{0.0F, 0.3F, -0.3F}) {
            BlockPosX pos = new BlockPosX(player.getX() + (double)x, player.getY() + (double)1.5F, player.getZ() + (double)z);
            Block block = mc.world.getBlockState(pos).getBlock();
            if ((new Box(pos)).intersects(player.getBoundingBox()) && (block == Blocks.OBSIDIAN || block == Blocks.RESPAWN_ANCHOR || block == Blocks.BEDROCK || block == Blocks.ENDER_CHEST)) {
               return true;
            }
         }
      }

      return false;
   }

   private Boolean canPush(PlayerEntity player) {
      if (this.onlyGround.getValue() && !player.isOnGround()) {
         return false;
      } else if (!this.allowWeb.getValue() && Alien.PLAYER.isInWeb(player) && !this.allowKey.isPressed()) {
         return false;
      } else if (this.onlyNoInside.getValue() && this.burrowUpdate(player)) {
         return false;
      } else if (this.onlyInside.getValue() && !this.burrowUpdate(player)) {
         return false;
      } else {
         int progress = 0;
         if (mc.world.canCollide(player, new Box(new BlockPosX(player.getX() + (double)1.0F, player.getY() + (double)0.5F, player.getZ())))) {
            ++progress;
         }

         if (mc.world.canCollide(player, new Box(new BlockPosX(player.getX() - (double)1.0F, player.getY() + (double)0.5F, player.getZ())))) {
            ++progress;
         }

         if (mc.world.canCollide(player, new Box(new BlockPosX(player.getX(), player.getY() + (double)0.5F, player.getZ() + (double)1.0F)))) {
            ++progress;
         }

         if (mc.world.canCollide(player, new Box(new BlockPosX(player.getX(), player.getY() + (double)0.5F, player.getZ() - (double)1.0F)))) {
            ++progress;
         }

         Box baseBox = new Box(new BlockPosX(player.getX(), player.getY() + (double)0.5F, player.getZ()));

         for(float x : OFFSETS) {
            for(float z : OFFSETS) {
               BlockPosX playerPos = new BlockPosX(player.getX() + (double)x, player.getY() + (double)0.5F, player.getZ() + (double)z);

               for(Direction i : HORIZONTAL_DIRECTIONS) {
                  BlockPos pos;
                  if (this.isTargetHere(pos = playerPos.offset(i), player)) {
                     if (mc.world.canCollide(player, new Box(pos))) {
                        return true;
                     }

                     if ((double)progress > this.surroundCheck.getValue() - (double)1.0F) {
                        return true;
                     }
                  }
               }
            }
         }

         if (!mc.world.canCollide(player, new Box(new BlockPosX(player.getX(), player.getY() + (double)2.5F, player.getZ())))) {
            BlockPos playerBasePos = EntityUtil.getEntityPos(player);

            for(Direction i : HORIZONTAL_DIRECTIONS) {
               BlockPos pos = playerBasePos.offset(i);
               Box box = player.getBoundingBox().offset(new Vec3d((double)i.getOffsetX(), (double)i.getOffsetY(), (double)i.getOffsetZ()));
               if (this.getBlock(pos.up()) != Blocks.PISTON_HEAD && !mc.world.canCollide(player, box.offset((double)0.0F, (double)1.0F, (double)0.0F)) && !this.isTargetHere(pos, player) && mc.world.canCollide(player, baseBox)) {
                  return true;
               }
            }
         }

         return (double)progress > this.surroundCheck.getValue() - (double)1.0F || Alien.HOLE.isHard((BlockPos)(new BlockPosX(player.getX(), player.getY() + (double)0.5F, player.getZ())));
      }
   }

   private boolean isTargetHere(BlockPos pos, Entity target) {
      return (new Box(pos)).intersects(target.getBoundingBox());
   }

   private Block getBlock(BlockPos pos) {
      return mc.world.getBlockState(pos).getBlock();
   }

   private Block getBlockType() {
      return this.torch.getValue() ? Blocks.REDSTONE_TORCH : Blocks.REDSTONE_BLOCK;
   }

   private BlockState getBlockState(BlockPos pos) {
      return mc.world.getBlockState(pos);
   }

   static {
      HORIZONTAL_DIRECTIONS = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
   }

   public static enum Page {
      General,
      Rotate,
      Check;

      // $FF: synthetic method
      private static Page[] $values() {
         return new Page[]{General, Rotate, Check};
      }
   }
}
