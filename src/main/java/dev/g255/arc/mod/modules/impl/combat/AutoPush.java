package dev.g255.arc.mod.modules.impl.combat;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.api.utils.combat.CombatUtil;
import dev.g255.arc.api.utils.math.MathUtil;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.api.utils.player.EntityUtil;
import dev.g255.arc.api.utils.player.InventoryUtil;
import dev.g255.arc.api.utils.world.BlockPosX;
import dev.g255.arc.api.utils.world.BlockUtil;
import dev.g255.arc.core.impl.RotationManager;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.impl.client.ClientSetting;
import dev.g255.arc.mod.modules.impl.exploit.Blink;
import dev.g255.arc.mod.modules.impl.player.PacketMine;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
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
   private final BooleanSetting torch = this.add(new BooleanSetting("Torch", false));
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotation", true));
   private final BooleanSetting yawDeceive = this.add(new BooleanSetting("YawDeceive", true));
   private final BooleanSetting pistonPacket = this.add(new BooleanSetting("PistonPacket", false));
   private final BooleanSetting powerPacket = this.add(new BooleanSetting("PowerPacket", true));
   private final BooleanSetting noEating = this.add(new BooleanSetting("EatingPause", true));
   private final BooleanSetting mine = this.add(new BooleanSetting("Mine", true));
   private final BooleanSetting allowWeb = this.add(new BooleanSetting("AllowWeb", true));
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
      this.setChinese("活塞推人");
      INSTANCE = this;
   }

   public static void pistonFacing(Direction i) {
      if (i == Direction.EAST) {
         Arc.ROTATION.snapAt(-90.0F, 5.0F);
      } else if (i == Direction.WEST) {
         Arc.ROTATION.snapAt(90.0F, 5.0F);
      } else if (i == Direction.NORTH) {
         Arc.ROTATION.snapAt(180.0F, 5.0F);
      } else if (i == Direction.SOUTH) {
         Arc.ROTATION.snapAt(0.0F, 5.0F);
      }

   }

   boolean isTargetHere(BlockPos pos, Entity target) {
      return (new Box(pos)).intersects(target.getBoundingBox());
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.timer.passedMs(this.updateDelay.getValue())) {
         if (!this.selfGround.getValue() || mc.player.isOnGround()) {
            if (this.findBlock(this.getBlockType()) != -1 && this.findClass(PistonBlock.class) != -1) {
               if (!this.noEating.getValue() || !mc.player.isUsingItem()) {
                  if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
                     for(PlayerEntity player : CombatUtil.getEnemies(this.range.getValue())) {
                        if (this.canPush(player)) {
                           float[] offset;
                           for(float x : offset = new float[]{-0.25F, 0.0F, 0.25F}) {
                              for(float z : offset) {
                                 BlockPosX playerPos = new BlockPosX(player.getX() + (double)x, player.getY() + (double)0.5F, player.getZ() + (double)z);

                                 for(Direction i : Direction.values()) {
                                    BlockPos pos;
                                    if (i != Direction.UP && i != Direction.DOWN && this.isTargetHere(pos = playerPos.offset(i), player) && mc.world.canCollide(player, new Box(pos))) {
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
                              for(Direction i : Direction.values()) {
                                 if (i != Direction.UP && i != Direction.DOWN) {
                                    BlockPos pos2 = EntityUtil.getEntityPos(player).offset(i);
                                    Box box = player.getBoundingBox().offset(new Vec3d((double)i.getOffsetX(), (double)i.getOffsetY(), (double)i.getOffsetZ()));
                                    if (this.getBlock(pos2.up()) != Blocks.PISTON_HEAD && !mc.world.canCollide(player, box.offset((double)0.0F, (double)1.0F, (double)0.0F)) && !this.isTargetHere(pos2, player)) {
                                       if (this.tryPush(EntityUtil.getEntityPos(player).offset(i.getOpposite()).up(), i)) {
                                          this.timer.reset();
                                          return;
                                       }

                                       if (this.tryPush(EntityUtil.getEntityPos(player).offset(i.getOpposite()), i)) {
                                          this.timer.reset();
                                          return;
                                       }
                                    }
                                 }
                              }
                           }

                           for(float x : offset) {
                              for(float z : offset) {
                                 BlockPosX playerPos = new BlockPosX(player.getX() + (double)x, player.getY() + (double)0.5F, player.getZ() + (double)z);

                                 for(Direction i : Direction.values()) {
                                    if (i != Direction.UP && i != Direction.DOWN && this.isTargetHere(playerPos.offset(i), player)) {
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
            } else {
               if (this.autoDisable.getValue()) {
                  this.disable();
               }

            }
         }
      }
   }

   private boolean tryPush(BlockPos piston, Direction direction) {
      if (!mc.world.isAir(piston.offset(direction))) {
         return false;
      } else {
         if (this.isTrueFacing(piston, direction) && this.facingCheck(piston) && BlockUtil.clientCanPlace(piston, false)) {
            boolean canPower = false;
            if (BlockUtil.getPlaceSide(piston, this.placeRange.getValue()) != null) {
               CombatUtil.modifyPos = piston;
               CombatUtil.modifyBlockState = Blocks.PISTON.getDefaultState();

               for(Direction direction2 : Direction.values()) {
                  if (this.getBlock(piston.offset(direction2)) == this.getBlockType()) {
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
                  Direction side = BlockUtil.getPlaceSide(piston);
                  if (side != null) {
                     if (this.rotate.getValue()) {
                        Arc.ROTATION.lookAt(piston.offset(side), side.getOpposite());
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
                        Arc.ROTATION.lookAt(piston.offset(side), side.getOpposite());
                     }

                     if (this.rotate.getValue()) {
                        Arc.ROTATION.snapBack();
                     }

                     for(Direction i : Direction.values()) {
                        if (this.getBlock(piston.offset(i)) == this.getBlockType()) {
                           if (this.mine.getValue()) {
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
                           int powerSlot = this.findBlock(this.getBlockType());
                           this.doSwap(powerSlot);
                           BlockUtil.placeBlock(piston.offset(i), this.rotate.getValue(), this.powerPacket.getValue());
                           if (this.inventory.getValue()) {
                              this.doSwap(powerSlot);
                              EntityUtil.syncInventory();
                           } else {
                              this.doSwap(oldSlot);
                           }

                           if (this.mine.getValue()) {
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
                     CombatUtil.modifyBlockState = this.getBlockType().getDefaultState();
                     if (BlockUtil.getPlaceSide(piston) != null) {
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
                  int powerSlot = this.findBlock(this.getBlockType());
                  this.doSwap(powerSlot);
                  BlockUtil.placeBlock(piston.offset(powerFacing), this.rotate.getValue(), this.powerPacket.getValue());
                  if (this.inventory.getValue()) {
                     this.doSwap(powerSlot);
                     EntityUtil.syncInventory();
                  } else {
                     this.doSwap(oldSlot);
                  }

                  CombatUtil.modifyPos = piston.offset(powerFacing);
                  CombatUtil.modifyBlockState = this.getBlockType().getDefaultState();
                  int n = this.findClass(PistonBlock.class);
                  Direction side2 = BlockUtil.getPlaceSide(piston);
                  if (side2 != null) {
                     if (this.rotate.getValue()) {
                        Arc.ROTATION.lookAt(piston.offset(side2), side2.getOpposite());
                     }

                     if (this.yawDeceive.getValue()) {
                        pistonFacing(direction.getOpposite());
                     }

                     int old = mc.player.getInventory().selectedSlot;
                     this.doSwap(n);
                     BlockUtil.placeBlock(piston, false, this.pistonPacket.getValue());
                     if (this.inventory.getValue()) {
                        this.doSwap(n);
                        EntityUtil.syncInventory();
                     } else {
                        this.doSwap(old);
                     }

                     if (this.rotate.getValue() && this.yawDeceive.getValue()) {
                        Arc.ROTATION.lookAt(piston.offset(side2), side2.getOpposite());
                     }

                     if (this.rotate.getValue()) {
                        Arc.ROTATION.snapBack();
                     }
                  }

                  CombatUtil.modifyPos = null;
                  return true;
               }
            }
         }

         if (mc.world.getBlockState(piston).getBlock() instanceof PistonBlock && this.getBlockState(piston).get(FacingBlock.FACING) == direction) {
            for(Direction direction4 : Direction.values()) {
               if (this.getBlock(piston.offset(direction4)) == this.getBlockType()) {
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
                  int powerSlot = this.findBlock(this.getBlockType());
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

   private Boolean canPush(PlayerEntity player) {
      if (this.onlyGround.getValue() && !player.isOnGround()) {
         return false;
      } else if (!this.allowWeb.getValue() && Arc.PLAYER.isInWeb(player)) {
         return false;
      } else {
         float[] offset = new float[]{-0.25F, 0.0F, 0.25F};
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

         for(float x : offset) {
            for(float z : offset) {
               BlockPosX playerPos = new BlockPosX(player.getX() + (double)x, player.getY() + (double)0.5F, player.getZ() + (double)z);

               for(Direction i : Direction.values()) {
                  BlockPos pos;
                  if (i != Direction.UP && i != Direction.DOWN && this.isTargetHere(pos = playerPos.offset(i), player)) {
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
            for(Direction i : Direction.values()) {
               if (i != Direction.UP && i != Direction.DOWN) {
                  BlockPos pos = EntityUtil.getEntityPos(player).offset(i);
                  Box box = player.getBoundingBox().offset(new Vec3d((double)i.getOffsetX(), (double)i.getOffsetY(), (double)i.getOffsetZ()));
                  if (this.getBlock(pos.up()) != Blocks.PISTON_HEAD && !mc.world.canCollide(player, box.offset((double)0.0F, (double)1.0F, (double)0.0F)) && !this.isTargetHere(pos, player) && mc.world.canCollide(player, new Box(new BlockPosX(player.getX(), player.getY() + (double)0.5F, player.getZ())))) {
                     return true;
                  }
               }
            }
         }

         return (double)progress > this.surroundCheck.getValue() - (double)1.0F || Arc.HOLE.isHard((BlockPos)(new BlockPosX(player.getX(), player.getY() + (double)0.5F, player.getZ())));
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
}
