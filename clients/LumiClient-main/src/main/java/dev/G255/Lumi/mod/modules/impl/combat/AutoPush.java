package dev.G255.Lumi.mod.modules.impl.combat;

import dev.G255.Lumi.Lumi;
import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.UpdateEvent;
import dev.G255.Lumi.api.utils.combat.CombatUtil;
import dev.G255.Lumi.api.utils.math.MathUtil;
import dev.G255.Lumi.api.utils.math.Timer;
import dev.G255.Lumi.api.utils.player.EntityUtil;
import dev.G255.Lumi.api.utils.player.InventoryUtil;
import dev.G255.Lumi.api.utils.world.BlockPosX;
import dev.G255.Lumi.api.utils.world.BlockUtil;
import dev.G255.Lumi.core.impl.RotationManager;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.impl.client.ClientSetting;
import dev.G255.Lumi.mod.modules.impl.exploit.Blink;
import dev.G255.Lumi.mod.modules.impl.player.PacketMine;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
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

public class AutoPush extends Module {
   public static AutoPush INSTANCE;
   private static final float[] OFFSETS = new float[]{-0.25F, 0.0F, 0.25F};
   private static final Direction[] HORIZONTAL_DIRECTIONS;
   private final BooleanSetting torch = this.add(new BooleanSetting("Torch", false));
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotation", true));
   private final BooleanSetting yawDeceive = this.add(new BooleanSetting("YawDeceive", true));
   private final BooleanSetting pistonPacket = this.add(new BooleanSetting("PistonPacket", false));
   private final BooleanSetting powerPacket = this.add(new BooleanSetting("PowerPacket", true));
   private final BooleanSetting noEating = this.add(new BooleanSetting("EatingPause", true));
   private final BooleanSetting mine = this.add(new BooleanSetting("Mine", true));
   private final BooleanSetting allowWeb = this.add(new BooleanSetting("AllowWeb", true));
   private final BooleanSetting force = this.add(new BooleanSetting("Force", false));
   private final SliderSetting updateDelay = this.add(new SliderSetting("Delay", 100, 0, 1000));
   private final BooleanSetting selfGround = this.add(new BooleanSetting("SelfGround", true));
   private final BooleanSetting onlyGround = this.add(new BooleanSetting("OnlyGround", false));
   private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true));
   private final SliderSetting range = this.add(new SliderSetting("Range", (double)5.0F, (double)0.0F, (double)6.0F));
   private final SliderSetting placeRange = this.add(new SliderSetting("PlaceRange", (double)5.0F, (double)0.0F, (double)6.0F));
   private final SliderSetting surroundCheck = this.add(new SliderSetting("SurroundCheck", 2, 0, 4));
   private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
   private final Timer timer = new Timer();

   public AutoPush() {
      super("AutoPush", Module.Category.Combat);
      this.setChinese("");
      INSTANCE = this;
   }

   public static void pistonFacing(Direction i) {
      if (i == Direction.EAST) {
         Lumi.ROTATION.snapAt(-90.0F, 5.0F);
      } else if (i == Direction.WEST) {
         Lumi.ROTATION.snapAt(90.0F, 5.0F);
      } else if (i == Direction.NORTH) {
         Lumi.ROTATION.snapAt(180.0F, 5.0F);
      } else if (i == Direction.SOUTH) {
         Lumi.ROTATION.snapAt(0.0F, 5.0F);
      }

   }

   public void onEnable() {
      AutoCrystal.INSTANCE.lastBreakTimer.reset();
   }

   boolean isTargetHere(BlockPos pos, Entity target) {
      Box bb = target.getBoundingBox();
      double minX = (double)pos.getX();
      double minY = (double)pos.getY();
      double minZ = (double)pos.getZ();
      double maxX = minX + (double)1.0F;
      double maxY = minY + (double)1.0F;
      double maxZ = minZ + (double)1.0F;
      return bb.maxX > minX && bb.minX < maxX && bb.maxY > minY && bb.minY < maxY && bb.maxZ > minZ && bb.minZ < maxZ;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      boolean forceMode = this.force.getValue();
      if (forceMode || this.timer.passedMs(this.updateDelay.getValue())) {
         if (forceMode || !this.selfGround.getValue() || mc.player.isOnGround()) {
            Block blockType = this.getBlockType();
            int powerSlot = this.findBlock(blockType);
            int pistonSlot = this.findClass(PistonBlock.class);
            if (powerSlot != -1 && pistonSlot != -1) {
               if (!this.noEating.getValue() || !mc.player.isUsingItem()) {
                  if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
                     for(PlayerEntity player : CombatUtil.getEnemies(this.range.getValue())) {
                        if (this.canPush(player)) {
                           boolean pushed = this.tryPushFourBlockBurrowCenter(player, blockType, pistonSlot, powerSlot, forceMode);
                           if (!pushed) {
                              label122:
                              for(float x : OFFSETS) {
                                 for(float z : OFFSETS) {
                                    BlockPosX playerPos = new BlockPosX(player.getX() + (double)x, player.getY() + (double)0.5F, player.getZ() + (double)z);

                                    for(Direction i : HORIZONTAL_DIRECTIONS) {
                                       BlockPos pos;
                                       if (this.isTargetHere(pos = playerPos.offset(i), player) && (forceMode || mc.world.canCollide(player, new Box(pos))) && (this.tryPush(playerPos.offset(i.getOpposite()), i, blockType, pistonSlot, powerSlot) || this.tryPush(playerPos.offset(i.getOpposite()).up(), i, blockType, pistonSlot, powerSlot))) {
                                          pushed = true;
                                          break label122;
                                       }
                                    }
                                 }
                              }
                           }

                           if (!pushed && (forceMode || !mc.world.canCollide(player, new Box(new BlockPosX(player.getX(), player.getY() + (double)2.5F, player.getZ()))))) {
                              BlockPos playerBasePos = EntityUtil.getEntityPos(player);

                              for(Direction i : HORIZONTAL_DIRECTIONS) {
                                 BlockPos pos2 = playerBasePos.offset(i);
                                 Box box = player.getBoundingBox().offset(new Vec3d((double)i.getOffsetX(), (double)i.getOffsetY(), (double)i.getOffsetZ()));
                                 if (this.getBlock(pos2.up()) != Blocks.PISTON_HEAD && !mc.world.canCollide(player, box.offset((double)0.0F, (double)1.0F, (double)0.0F)) && !this.isTargetHere(pos2, player) && (this.tryPush(playerBasePos.offset(i.getOpposite()).up(), i, blockType, pistonSlot, powerSlot) || this.tryPush(playerBasePos.offset(i.getOpposite()), i, blockType, pistonSlot, powerSlot))) {
                                    pushed = true;
                                    break;
                                 }
                              }
                           }

                           if (!pushed) {
                              label167:
                              for(float x : OFFSETS) {
                                 for(float z : OFFSETS) {
                                    BlockPosX playerPos = new BlockPosX(player.getX() + (double)x, player.getY() + (double)0.5F, player.getZ() + (double)z);

                                    for(Direction i : HORIZONTAL_DIRECTIONS) {
                                       if (this.isTargetHere(playerPos.offset(i), player) && (this.tryPush(playerPos.offset(i.getOpposite()).up(), i, blockType, pistonSlot, powerSlot) || this.tryPush(playerPos.offset(i.getOpposite()), i, blockType, pistonSlot, powerSlot))) {
                                          pushed = true;
                                          break label167;
                                       }
                                    }
                                 }
                              }
                           }

                           if (pushed) {
                              this.timer.reset();
                              if (!forceMode) {
                                 return;
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

   private boolean tryPush(BlockPos piston, Direction direction, Block blockType, int pistonSlot, int powerSlot) {
      if (!mc.world.isAir(piston.offset(direction))) {
         return false;
      } else {
         if (this.isTrueFacing(piston, direction) && this.facingCheck(piston) && BlockUtil.clientCanPlace(piston, false)) {
            boolean canPower = false;
            boolean is2x2Trap = false;
            Direction trapPowerSide = null;
            Direction pistonPlaceSide = BlockUtil.getPlaceSide(piston, this.placeRange.getValue());
            if (pistonPlaceSide != null) {
               CombatUtil.modifyPos = piston;
               CombatUtil.modifyBlockState = Blocks.PISTON.getDefaultState();

               for(Direction direction2 : Direction.values()) {
                  if (this.getBlock(piston.offset(direction2)) == blockType) {
                     canPower = true;
                     break;
                  }
               }

               if (!canPower) {
                  for(Direction i : Direction.values()) {
                     if (i != direction && i != direction.getOpposite() && BlockUtil.canPlace(piston.offset(i), this.placeRange.getValue())) {
                        canPower = true;
                        break;
                     }
                  }

                  if (!canPower) {
                     Direction back = direction.getOpposite();
                     BlockPos backPos = piston.offset(back);
                     BlockPos backUpPos = backPos.up();
                     if (BlockUtil.canPlace(backPos, this.placeRange.getValue()) && BlockUtil.canPlace(backUpPos, this.placeRange.getValue())) {
                        canPower = true;
                        is2x2Trap = true;
                        trapPowerSide = back;
                     }
                  }

                  if (!canPower && BlockUtil.canPlace(piston.offset(direction.getOpposite()), this.placeRange.getValue())) {
                     canPower = true;
                  }

                  if (!canPower) {
                     for(Direction side : Direction.values()) {
                        if (side != Direction.UP && side != Direction.DOWN && side != direction && side != direction.getOpposite()) {
                           BlockPos sidePos = piston.offset(side);
                           BlockPos upPos = sidePos.up();
                           if (BlockUtil.canPlace(sidePos, this.placeRange.getValue()) && BlockUtil.canPlace(upPos, this.placeRange.getValue())) {
                              canPower = true;
                              is2x2Trap = true;
                              trapPowerSide = side;
                              break;
                           }
                        }
                     }
                  }
               }

               CombatUtil.modifyPos = null;
               if (canPower) {
                  if (pistonSlot == -1) {
                     return false;
                  }

                  if (pistonPlaceSide != null) {
                     if (this.rotate.getValue()) {
                        Lumi.ROTATION.lookAt(piston.offset(pistonPlaceSide), pistonPlaceSide.getOpposite());
                     }

                     if (this.yawDeceive.getValue()) {
                        pistonFacing(direction.getOpposite());
                     }

                     int old = mc.player.getInventory().selectedSlot;
                     this.doSwap(pistonSlot);
                     if (!BlockUtil.clientCanPlace(piston, false)) {
                        if (this.inventory.getValue()) {
                           this.doSwap(pistonSlot);
                           EntityUtil.syncInventory();
                        } else {
                           this.doSwap(old);
                        }

                        return false;
                     }

                     BlockUtil.placeBlock(piston, false, this.pistonPacket.getValue());
                     if (this.inventory.getValue()) {
                        this.doSwap(pistonSlot);
                        EntityUtil.syncInventory();
                     } else {
                        this.doSwap(old);
                     }

                     if (this.rotate.getValue() && this.yawDeceive.getValue()) {
                        Lumi.ROTATION.lookAt(piston.offset(pistonPlaceSide), pistonPlaceSide.getOpposite());
                     }

                     if (this.rotate.getValue()) {
                        Lumi.ROTATION.snapBack();
                     }

                     for(Direction i : Direction.values()) {
                        if (this.getBlock(piston.offset(i)) == blockType) {
                           if (this.mine.getValue()) {
                              PacketMine.INSTANCE.mine(piston.offset(i));
                           }

                           if (this.autoDisable.getValue()) {
                              this.disable();
                           }

                           return true;
                        }
                     }

                     if (is2x2Trap && trapPowerSide != null) {
                        int oldSlot = mc.player.getInventory().selectedSlot;
                        if (powerSlot == -1) {
                           return false;
                        }

                        this.doSwap(powerSlot);
                        BlockPos powerBasePos = piston.offset(trapPowerSide);
                        BlockPos powerTopPos = powerBasePos.up();
                        if (BlockUtil.canPlace(powerBasePos, this.placeRange.getValue()) && BlockUtil.canPlace(powerTopPos, this.placeRange.getValue())) {
                           BlockUtil.placeBlock(powerBasePos, this.rotate.getValue(), this.powerPacket.getValue());
                           BlockUtil.placeBlock(powerTopPos, this.rotate.getValue(), this.powerPacket.getValue());
                           if (this.inventory.getValue()) {
                              this.doSwap(powerSlot);
                              EntityUtil.syncInventory();
                           } else {
                              this.doSwap(oldSlot);
                           }

                           return true;
                        }

                        if (this.inventory.getValue()) {
                           this.doSwap(powerSlot);
                           EntityUtil.syncInventory();
                        } else {
                           this.doSwap(oldSlot);
                        }

                        return false;
                     }

                     for(Direction i : Direction.values()) {
                        BlockPos powerPos;
                        if (i != direction && (i != Direction.UP || !this.torch.getValue()) && BlockUtil.canPlace(powerPos = piston.offset(i), this.placeRange.getValue())) {
                           int oldSlot = mc.player.getInventory().selectedSlot;
                           if (powerSlot == -1) {
                              return false;
                           }

                           this.doSwap(powerSlot);
                           if (!BlockUtil.canPlace(powerPos, this.placeRange.getValue())) {
                              if (this.inventory.getValue()) {
                                 this.doSwap(powerSlot);
                                 EntityUtil.syncInventory();
                              } else {
                                 this.doSwap(oldSlot);
                              }

                              return false;
                           }

                           BlockUtil.placeBlock(powerPos, this.rotate.getValue(), this.powerPacket.getValue());
                           if (this.inventory.getValue()) {
                              this.doSwap(powerSlot);
                              EntityUtil.syncInventory();
                           } else {
                              this.doSwap(oldSlot);
                           }

                           if (this.mine.getValue()) {
                              PacketMine.INSTANCE.mine(powerPos);
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
                  if (powerSlot == -1) {
                     return false;
                  }

                  this.doSwap(powerSlot);
                  if (!BlockUtil.canPlace(piston.offset(powerFacing), this.placeRange.getValue())) {
                     if (this.inventory.getValue()) {
                        this.doSwap(powerSlot);
                        EntityUtil.syncInventory();
                     } else {
                        this.doSwap(oldSlot);
                     }

                     return false;
                  }

                  BlockUtil.placeBlock(piston.offset(powerFacing), this.rotate.getValue(), this.powerPacket.getValue());
                  if (this.inventory.getValue()) {
                     this.doSwap(powerSlot);
                     EntityUtil.syncInventory();
                  } else {
                     this.doSwap(oldSlot);
                  }

                  CombatUtil.modifyPos = piston.offset(powerFacing);
                  CombatUtil.modifyBlockState = blockType.getDefaultState();
                  if (pistonSlot == -1) {
                     CombatUtil.modifyPos = null;
                     return false;
                  }

                  Direction side2 = BlockUtil.getPlaceSide(piston, this.placeRange.getValue());
                  if (side2 != null) {
                     if (this.rotate.getValue()) {
                        Lumi.ROTATION.lookAt(piston.offset(side2), side2.getOpposite());
                     }

                     if (this.yawDeceive.getValue()) {
                        pistonFacing(direction.getOpposite());
                     }

                     int old = mc.player.getInventory().selectedSlot;
                     this.doSwap(pistonSlot);
                     if (!BlockUtil.clientCanPlace(piston, false)) {
                        if (this.inventory.getValue()) {
                           this.doSwap(pistonSlot);
                           EntityUtil.syncInventory();
                        } else {
                           this.doSwap(old);
                        }

                        CombatUtil.modifyPos = null;
                        return false;
                     }

                     BlockUtil.placeBlock(piston, false, this.pistonPacket.getValue());
                     if (this.inventory.getValue()) {
                        this.doSwap(pistonSlot);
                        EntityUtil.syncInventory();
                     } else {
                        this.doSwap(old);
                     }

                     if (this.rotate.getValue() && this.yawDeceive.getValue()) {
                        Lumi.ROTATION.lookAt(piston.offset(side2), side2.getOpposite());
                     }

                     if (this.rotate.getValue()) {
                        Lumi.ROTATION.snapBack();
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
               if (direction5 != direction && (direction5 != Direction.UP || !this.torch.getValue()) && BlockUtil.canPlace(piston.offset(direction5), this.placeRange.getValue())) {
                  int oldSlot = mc.player.getInventory().selectedSlot;
                  if (powerSlot == -1) {
                     return false;
                  }

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

   private boolean tryPushFourBlockBurrowCenter(PlayerEntity player, Block blockType, int pistonSlot, int powerSlot, boolean forceMode) {
      if (!this.isFourBlockBurrowCenter(player)) {
         return false;
      } else {
         double x = player.getX();
         double z = player.getZ();
         int cornerX = (int)Math.round(x);
         int cornerZ = (int)Math.round(z);
         int yFeet = (new BlockPosX(player.getX(), player.getY() + (double)0.5F, player.getZ())).getY();
         BlockPos[] targets = new BlockPos[]{new BlockPos(cornerX, yFeet + 1, cornerZ), new BlockPos(cornerX - 1, yFeet + 1, cornerZ), new BlockPos(cornerX, yFeet + 1, cornerZ - 1), new BlockPos(cornerX - 1, yFeet + 1, cornerZ - 1), new BlockPos(cornerX, yFeet + 2, cornerZ), new BlockPos(cornerX - 1, yFeet + 2, cornerZ), new BlockPos(cornerX, yFeet + 2, cornerZ - 1), new BlockPos(cornerX - 1, yFeet + 2, cornerZ - 1)};

         for(Direction dir : HORIZONTAL_DIRECTIONS) {
            if (forceMode || this.canSideEject(player, dir)) {
               for(BlockPos target : targets) {
                  if (this.isTargetHere(target, player) && mc.world.isAir(target)) {
                     BlockPos pistonPos = target.offset(dir.getOpposite());
                     if (this.tryPush(pistonPos, dir, blockType, pistonSlot, powerSlot)) {
                        return true;
                     }

                     if (this.tryPush(pistonPos.up(), dir, blockType, pistonSlot, powerSlot)) {
                        return true;
                     }
                  }
               }
            }
         }

         return false;
      }
   }

   private boolean canSideEject(PlayerEntity player, Direction dir) {
      Vec3d delta = new Vec3d((double)dir.getOffsetX(), (double)0.0F, (double)dir.getOffsetZ());
      return !mc.world.canCollide(player, player.getBoundingBox().offset(delta));
   }

   private boolean isFourBlockBurrowCenter(PlayerEntity player) {
      double x = player.getX();
      double z = player.getZ();
      double fx = x - Math.floor(x);
      double fz = z - Math.floor(z);
      double dx = Math.min(fx, (double)1.0F - fx);
      double dz = Math.min(fz, (double)1.0F - fz);
      if (!(dx > 0.15) && !(dz > 0.15)) {
         int cornerX = (int)Math.round(x);
         int cornerZ = (int)Math.round(z);
         int y = (new BlockPosX(player.getX(), player.getY() + (double)0.5F, player.getZ())).getY();
         int inside = 0;
         BlockPos p1 = new BlockPos(cornerX, y, cornerZ);
         BlockPos p2 = new BlockPos(cornerX - 1, y, cornerZ);
         BlockPos p3 = new BlockPos(cornerX, y, cornerZ - 1);
         BlockPos p4 = new BlockPos(cornerX - 1, y, cornerZ - 1);
         if (this.isTargetHere(p1, player) && this.isCollidableBlock(p1)) {
            ++inside;
         }

         if (this.isTargetHere(p2, player) && this.isCollidableBlock(p2)) {
            ++inside;
         }

         if (this.isTargetHere(p3, player) && this.isCollidableBlock(p3)) {
            ++inside;
         }

         if (this.isTargetHere(p4, player) && this.isCollidableBlock(p4)) {
            ++inside;
         }

         return inside >= 3;
      } else {
         return false;
      }
   }

   private boolean isCollidableBlock(BlockPos pos) {
      return !mc.world.getBlockState(pos).getCollisionShape(mc.world, pos).isEmpty();
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

   public int findClass(Class clazz) {
      return this.inventory.getValue() ? InventoryUtil.findClassInventorySlot(clazz) : InventoryUtil.findClass(clazz);
   }

   private Boolean canPush(PlayerEntity player) {
      if (this.onlyGround.getValue() && !player.isOnGround()) {
         return false;
      } else if (!this.allowWeb.getValue() && Lumi.PLAYER.isInWeb(player)) {
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

         return (double)progress > this.surroundCheck.getValue() - (double)1.0F || Lumi.HOLE.isHard((BlockPos)(new BlockPosX(player.getX(), player.getY() + (double)0.5F, player.getZ())));
      }
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
}
