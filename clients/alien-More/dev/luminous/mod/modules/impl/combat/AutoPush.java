package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.core.impl.RotationManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.impl.player.PacketMine;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2318;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_2665;
import net.minecraft.class_2680;

public class AutoPush extends Module {
   public static AutoPush INSTANCE;
   private static final float[] OFFSETS = new float[]{-0.25F, 0.0F, 0.25F};
   private static final class_2350[] HORIZONTAL_DIRECTIONS;
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
      this.setChinese("活塞推人");
      INSTANCE = this;
   }

   public static void pistonFacing(class_2350 i) {
      if (i == class_2350.field_11034) {
         Alien.ROTATION.snapAt(-90.0F, 5.0F);
      } else if (i == class_2350.field_11039) {
         Alien.ROTATION.snapAt(90.0F, 5.0F);
      } else if (i == class_2350.field_11043) {
         Alien.ROTATION.snapAt(180.0F, 5.0F);
      } else if (i == class_2350.field_11035) {
         Alien.ROTATION.snapAt(0.0F, 5.0F);
      }

   }

   public void onEnable() {
      AutoCrystal.INSTANCE.lastBreakTimer.reset();
   }

   boolean isTargetHere(class_2338 pos, class_1297 target) {
      class_238 bb = target.method_5829();
      double minX = (double)pos.method_10263();
      double minY = (double)pos.method_10264();
      double minZ = (double)pos.method_10260();
      double maxX = minX + (double)1.0F;
      double maxY = minY + (double)1.0F;
      double maxZ = minZ + (double)1.0F;
      return bb.field_1320 > minX && bb.field_1323 < maxX && bb.field_1325 > minY && bb.field_1322 < maxY && bb.field_1324 > minZ && bb.field_1321 < maxZ;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      boolean forceMode = this.force.getValue();
      if (forceMode || this.timer.passedMs(this.updateDelay.getValue())) {
         if (forceMode || !this.selfGround.getValue() || mc.field_1724.method_24828()) {
            class_2248 blockType = this.getBlockType();
            int powerSlot = this.findBlock(blockType);
            int pistonSlot = this.findClass(class_2665.class);
            if (powerSlot != -1 && pistonSlot != -1) {
               if (!this.noEating.getValue() || !mc.field_1724.method_6115()) {
                  if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
                     for(class_1657 player : CombatUtil.getEnemies(this.range.getValue())) {
                        if (this.canPush(player)) {
                           boolean pushed = this.tryPushFourBlockBurrowCenter(player, blockType, pistonSlot, powerSlot, forceMode);
                           if (!pushed) {
                              label122:
                              for(float x : OFFSETS) {
                                 for(float z : OFFSETS) {
                                    BlockPosX playerPos = new BlockPosX(player.method_23317() + (double)x, player.method_23318() + (double)0.5F, player.method_23321() + (double)z);

                                    for(class_2350 i : HORIZONTAL_DIRECTIONS) {
                                       class_2338 pos;
                                       if (this.isTargetHere(pos = playerPos.method_10093(i), player) && (forceMode || mc.field_1687.method_39454(player, new class_238(pos))) && (this.tryPush(playerPos.method_10093(i.method_10153()), i, blockType, pistonSlot, powerSlot) || this.tryPush(playerPos.method_10093(i.method_10153()).method_10084(), i, blockType, pistonSlot, powerSlot))) {
                                          pushed = true;
                                          break label122;
                                       }
                                    }
                                 }
                              }
                           }

                           if (!pushed && (forceMode || !mc.field_1687.method_39454(player, new class_238(new BlockPosX(player.method_23317(), player.method_23318() + (double)2.5F, player.method_23321()))))) {
                              class_2338 playerBasePos = EntityUtil.getEntityPos(player);

                              for(class_2350 i : HORIZONTAL_DIRECTIONS) {
                                 class_2338 pos2 = playerBasePos.method_10093(i);
                                 class_238 box = player.method_5829().method_997(new class_243((double)i.method_10148(), (double)i.method_10164(), (double)i.method_10165()));
                                 if (this.getBlock(pos2.method_10084()) != class_2246.field_10379 && !mc.field_1687.method_39454(player, box.method_989((double)0.0F, (double)1.0F, (double)0.0F)) && !this.isTargetHere(pos2, player) && (this.tryPush(playerBasePos.method_10093(i.method_10153()).method_10084(), i, blockType, pistonSlot, powerSlot) || this.tryPush(playerBasePos.method_10093(i.method_10153()), i, blockType, pistonSlot, powerSlot))) {
                                    pushed = true;
                                    break;
                                 }
                              }
                           }

                           if (!pushed) {
                              label167:
                              for(float x : OFFSETS) {
                                 for(float z : OFFSETS) {
                                    BlockPosX playerPos = new BlockPosX(player.method_23317() + (double)x, player.method_23318() + (double)0.5F, player.method_23321() + (double)z);

                                    for(class_2350 i : HORIZONTAL_DIRECTIONS) {
                                       if (this.isTargetHere(playerPos.method_10093(i), player) && (this.tryPush(playerPos.method_10093(i.method_10153()).method_10084(), i, blockType, pistonSlot, powerSlot) || this.tryPush(playerPos.method_10093(i.method_10153()), i, blockType, pistonSlot, powerSlot))) {
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

   private boolean tryPush(class_2338 piston, class_2350 direction, class_2248 blockType, int pistonSlot, int powerSlot) {
      if (!mc.field_1687.method_22347(piston.method_10093(direction))) {
         return false;
      } else {
         if (this.isTrueFacing(piston, direction) && this.facingCheck(piston) && BlockUtil.clientCanPlace(piston, false)) {
            boolean canPower = false;
            boolean is2x2Trap = false;
            class_2350 trapPowerSide = null;
            class_2350 pistonPlaceSide = BlockUtil.getPlaceSide(piston, this.placeRange.getValue());
            if (pistonPlaceSide != null) {
               CombatUtil.modifyPos = piston;
               CombatUtil.modifyBlockState = class_2246.field_10560.method_9564();

               for(class_2350 direction2 : class_2350.values()) {
                  if (this.getBlock(piston.method_10093(direction2)) == blockType) {
                     canPower = true;
                     break;
                  }
               }

               if (!canPower) {
                  for(class_2350 i : class_2350.values()) {
                     if (i != direction && i != direction.method_10153() && BlockUtil.canPlace(piston.method_10093(i), this.placeRange.getValue())) {
                        canPower = true;
                        break;
                     }
                  }

                  if (!canPower) {
                     class_2350 back = direction.method_10153();
                     class_2338 backPos = piston.method_10093(back);
                     class_2338 backUpPos = backPos.method_10084();
                     if (BlockUtil.canPlace(backPos, this.placeRange.getValue()) && BlockUtil.canPlace(backUpPos, this.placeRange.getValue())) {
                        canPower = true;
                        is2x2Trap = true;
                        trapPowerSide = back;
                     }
                  }

                  if (!canPower && BlockUtil.canPlace(piston.method_10093(direction.method_10153()), this.placeRange.getValue())) {
                     canPower = true;
                  }

                  if (!canPower) {
                     for(class_2350 side : class_2350.values()) {
                        if (side != class_2350.field_11036 && side != class_2350.field_11033 && side != direction && side != direction.method_10153()) {
                           class_2338 sidePos = piston.method_10093(side);
                           class_2338 upPos = sidePos.method_10084();
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
                        Alien.ROTATION.lookAt(piston.method_10093(pistonPlaceSide), pistonPlaceSide.method_10153());
                     }

                     if (this.yawDeceive.getValue()) {
                        pistonFacing(direction.method_10153());
                     }

                     int old = mc.field_1724.method_31548().field_7545;
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
                        Alien.ROTATION.lookAt(piston.method_10093(pistonPlaceSide), pistonPlaceSide.method_10153());
                     }

                     if (this.rotate.getValue()) {
                        Alien.ROTATION.snapBack();
                     }

                     for(class_2350 i : class_2350.values()) {
                        if (this.getBlock(piston.method_10093(i)) == blockType) {
                           if (this.mine.getValue()) {
                              PacketMine.INSTANCE.mine(piston.method_10093(i));
                           }

                           if (this.autoDisable.getValue()) {
                              this.disable();
                           }

                           return true;
                        }
                     }

                     if (is2x2Trap && trapPowerSide != null) {
                        int oldSlot = mc.field_1724.method_31548().field_7545;
                        if (powerSlot == -1) {
                           return false;
                        }

                        this.doSwap(powerSlot);
                        class_2338 powerBasePos = piston.method_10093(trapPowerSide);
                        class_2338 powerTopPos = powerBasePos.method_10084();
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

                     for(class_2350 i : class_2350.values()) {
                        class_2338 powerPos;
                        if (i != direction && (i != class_2350.field_11036 || !this.torch.getValue()) && BlockUtil.canPlace(powerPos = piston.method_10093(i), this.placeRange.getValue())) {
                           int oldSlot = mc.field_1724.method_31548().field_7545;
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
               class_2350 powerFacing = null;

               for(class_2350 i : class_2350.values()) {
                  if (i != class_2350.field_11036 || !this.torch.getValue()) {
                     if (powerFacing != null) {
                        break;
                     }

                     CombatUtil.modifyPos = piston.method_10093(i);
                     CombatUtil.modifyBlockState = blockType.method_9564();
                     if (BlockUtil.getPlaceSide(piston, this.placeRange.getValue()) != null) {
                        powerFacing = i;
                     }

                     CombatUtil.modifyPos = null;
                     if (powerFacing != null && !BlockUtil.canPlace(piston.method_10093(powerFacing))) {
                        powerFacing = null;
                     }
                  }
               }

               if (powerFacing != null) {
                  int oldSlot = mc.field_1724.method_31548().field_7545;
                  if (powerSlot == -1) {
                     return false;
                  }

                  this.doSwap(powerSlot);
                  if (!BlockUtil.canPlace(piston.method_10093(powerFacing), this.placeRange.getValue())) {
                     if (this.inventory.getValue()) {
                        this.doSwap(powerSlot);
                        EntityUtil.syncInventory();
                     } else {
                        this.doSwap(oldSlot);
                     }

                     return false;
                  }

                  BlockUtil.placeBlock(piston.method_10093(powerFacing), this.rotate.getValue(), this.powerPacket.getValue());
                  if (this.inventory.getValue()) {
                     this.doSwap(powerSlot);
                     EntityUtil.syncInventory();
                  } else {
                     this.doSwap(oldSlot);
                  }

                  CombatUtil.modifyPos = piston.method_10093(powerFacing);
                  CombatUtil.modifyBlockState = blockType.method_9564();
                  if (pistonSlot == -1) {
                     CombatUtil.modifyPos = null;
                     return false;
                  }

                  class_2350 side2 = BlockUtil.getPlaceSide(piston, this.placeRange.getValue());
                  if (side2 != null) {
                     if (this.rotate.getValue()) {
                        Alien.ROTATION.lookAt(piston.method_10093(side2), side2.method_10153());
                     }

                     if (this.yawDeceive.getValue()) {
                        pistonFacing(direction.method_10153());
                     }

                     int old = mc.field_1724.method_31548().field_7545;
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
                        Alien.ROTATION.lookAt(piston.method_10093(side2), side2.method_10153());
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

         if (mc.field_1687.method_8320(piston).method_26204() instanceof class_2665 && this.getBlockState(piston).method_11654(class_2318.field_10927) == direction) {
            for(class_2350 direction4 : class_2350.values()) {
               if (this.getBlock(piston.method_10093(direction4)) == blockType) {
                  if (this.autoDisable.getValue()) {
                     this.disable();
                     return true;
                  }

                  return false;
               }
            }

            for(class_2350 direction5 : class_2350.values()) {
               if (direction5 != direction && (direction5 != class_2350.field_11036 || !this.torch.getValue()) && BlockUtil.canPlace(piston.method_10093(direction5), this.placeRange.getValue())) {
                  int oldSlot = mc.field_1724.method_31548().field_7545;
                  if (powerSlot == -1) {
                     return false;
                  }

                  this.doSwap(powerSlot);
                  BlockUtil.placeBlock(piston.method_10093(direction5), this.rotate.getValue(), this.powerPacket.getValue());
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

   private boolean tryPushFourBlockBurrowCenter(class_1657 player, class_2248 blockType, int pistonSlot, int powerSlot, boolean forceMode) {
      if (!this.isFourBlockBurrowCenter(player)) {
         return false;
      } else {
         double x = player.method_23317();
         double z = player.method_23321();
         int cornerX = (int)Math.round(x);
         int cornerZ = (int)Math.round(z);
         int yFeet = (new BlockPosX(player.method_23317(), player.method_23318() + (double)0.5F, player.method_23321())).method_10264();
         class_2338[] targets = new class_2338[]{new class_2338(cornerX, yFeet + 1, cornerZ), new class_2338(cornerX - 1, yFeet + 1, cornerZ), new class_2338(cornerX, yFeet + 1, cornerZ - 1), new class_2338(cornerX - 1, yFeet + 1, cornerZ - 1), new class_2338(cornerX, yFeet + 2, cornerZ), new class_2338(cornerX - 1, yFeet + 2, cornerZ), new class_2338(cornerX, yFeet + 2, cornerZ - 1), new class_2338(cornerX - 1, yFeet + 2, cornerZ - 1)};

         for(class_2350 dir : HORIZONTAL_DIRECTIONS) {
            if (forceMode || this.canSideEject(player, dir)) {
               for(class_2338 target : targets) {
                  if (this.isTargetHere(target, player) && mc.field_1687.method_22347(target)) {
                     class_2338 pistonPos = target.method_10093(dir.method_10153());
                     if (this.tryPush(pistonPos, dir, blockType, pistonSlot, powerSlot)) {
                        return true;
                     }

                     if (this.tryPush(pistonPos.method_10084(), dir, blockType, pistonSlot, powerSlot)) {
                        return true;
                     }
                  }
               }
            }
         }

         return false;
      }
   }

   private boolean canSideEject(class_1657 player, class_2350 dir) {
      class_243 delta = new class_243((double)dir.method_10148(), (double)0.0F, (double)dir.method_10165());
      return !mc.field_1687.method_39454(player, player.method_5829().method_997(delta));
   }

   private boolean isFourBlockBurrowCenter(class_1657 player) {
      double x = player.method_23317();
      double z = player.method_23321();
      double fx = x - Math.floor(x);
      double fz = z - Math.floor(z);
      double dx = Math.min(fx, (double)1.0F - fx);
      double dz = Math.min(fz, (double)1.0F - fz);
      if (!(dx > 0.15) && !(dz > 0.15)) {
         int cornerX = (int)Math.round(x);
         int cornerZ = (int)Math.round(z);
         int y = (new BlockPosX(player.method_23317(), player.method_23318() + (double)0.5F, player.method_23321())).method_10264();
         int inside = 0;
         class_2338 p1 = new class_2338(cornerX, y, cornerZ);
         class_2338 p2 = new class_2338(cornerX - 1, y, cornerZ);
         class_2338 p3 = new class_2338(cornerX, y, cornerZ - 1);
         class_2338 p4 = new class_2338(cornerX - 1, y, cornerZ - 1);
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

   private boolean isCollidableBlock(class_2338 pos) {
      return !mc.field_1687.method_8320(pos).method_26220(mc.field_1687, pos).method_1110();
   }

   private boolean facingCheck(class_2338 pos) {
      if (!ClientSetting.INSTANCE.lowVersion.getValue()) {
         return true;
      } else {
         class_2350 direction = MathUtil.getDirectionFromEntityLiving(pos, mc.field_1724);
         return direction != class_2350.field_11036 && direction != class_2350.field_11033;
      }
   }

   private boolean isTrueFacing(class_2338 pos, class_2350 facing) {
      if (this.yawDeceive.getValue()) {
         return true;
      } else {
         class_2350 side = BlockUtil.getPlaceSide(pos);
         if (side == null) {
            return false;
         } else {
            class_243 directionVec = new class_243((double)pos.method_10263() + (double)0.5F + (double)side.method_10163().method_10263() * (double)0.5F, (double)pos.method_10264() + (double)0.5F + (double)side.method_10163().method_10264() * (double)0.5F, (double)pos.method_10260() + (double)0.5F + (double)side.method_10163().method_10260() * (double)0.5F);
            float[] rotation = RotationManager.getRotation(directionVec);
            return MathUtil.getFacingOrder(rotation[0], rotation[1]).method_10153() == facing;
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

   public int findBlock(class_2248 blockIn) {
      return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(blockIn) : InventoryUtil.findBlock(blockIn);
   }

   public int findClass(Class clazz) {
      return this.inventory.getValue() ? InventoryUtil.findClassInventorySlot(clazz) : InventoryUtil.findClass(clazz);
   }

   private Boolean canPush(class_1657 player) {
      if (this.onlyGround.getValue() && !player.method_24828()) {
         return false;
      } else if (!this.allowWeb.getValue() && Alien.PLAYER.isInWeb(player)) {
         return false;
      } else {
         int progress = 0;
         if (mc.field_1687.method_39454(player, new class_238(new BlockPosX(player.method_23317() + (double)1.0F, player.method_23318() + (double)0.5F, player.method_23321())))) {
            ++progress;
         }

         if (mc.field_1687.method_39454(player, new class_238(new BlockPosX(player.method_23317() - (double)1.0F, player.method_23318() + (double)0.5F, player.method_23321())))) {
            ++progress;
         }

         if (mc.field_1687.method_39454(player, new class_238(new BlockPosX(player.method_23317(), player.method_23318() + (double)0.5F, player.method_23321() + (double)1.0F)))) {
            ++progress;
         }

         if (mc.field_1687.method_39454(player, new class_238(new BlockPosX(player.method_23317(), player.method_23318() + (double)0.5F, player.method_23321() - (double)1.0F)))) {
            ++progress;
         }

         class_238 baseBox = new class_238(new BlockPosX(player.method_23317(), player.method_23318() + (double)0.5F, player.method_23321()));

         for(float x : OFFSETS) {
            for(float z : OFFSETS) {
               BlockPosX playerPos = new BlockPosX(player.method_23317() + (double)x, player.method_23318() + (double)0.5F, player.method_23321() + (double)z);

               for(class_2350 i : HORIZONTAL_DIRECTIONS) {
                  class_2338 pos;
                  if (this.isTargetHere(pos = playerPos.method_10093(i), player)) {
                     if (mc.field_1687.method_39454(player, new class_238(pos))) {
                        return true;
                     }

                     if ((double)progress > this.surroundCheck.getValue() - (double)1.0F) {
                        return true;
                     }
                  }
               }
            }
         }

         if (!mc.field_1687.method_39454(player, new class_238(new BlockPosX(player.method_23317(), player.method_23318() + (double)2.5F, player.method_23321())))) {
            class_2338 playerBasePos = EntityUtil.getEntityPos(player);

            for(class_2350 i : HORIZONTAL_DIRECTIONS) {
               class_2338 pos = playerBasePos.method_10093(i);
               class_238 box = player.method_5829().method_997(new class_243((double)i.method_10148(), (double)i.method_10164(), (double)i.method_10165()));
               if (this.getBlock(pos.method_10084()) != class_2246.field_10379 && !mc.field_1687.method_39454(player, box.method_989((double)0.0F, (double)1.0F, (double)0.0F)) && !this.isTargetHere(pos, player) && mc.field_1687.method_39454(player, baseBox)) {
                  return true;
               }
            }
         }

         return (double)progress > this.surroundCheck.getValue() - (double)1.0F || Alien.HOLE.isHard((class_2338)(new BlockPosX(player.method_23317(), player.method_23318() + (double)0.5F, player.method_23321())));
      }
   }

   private class_2248 getBlock(class_2338 pos) {
      return mc.field_1687.method_8320(pos).method_26204();
   }

   private class_2248 getBlockType() {
      return this.torch.getValue() ? class_2246.field_10523 : class_2246.field_10002;
   }

   private class_2680 getBlockState(class_2338 pos) {
      return mc.field_1687.method_8320(pos);
   }

   static {
      HORIZONTAL_DIRECTIONS = new class_2350[]{class_2350.field_11043, class_2350.field_11035, class_2350.field_11039, class_2350.field_11034};
   }
}
