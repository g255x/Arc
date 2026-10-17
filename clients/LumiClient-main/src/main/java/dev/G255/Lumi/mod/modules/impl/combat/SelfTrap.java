package dev.G255.Lumi.mod.modules.impl.combat;

import dev.G255.Lumi.Lumi;
import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.ClientTickEvent;
import dev.G255.Lumi.api.events.impl.MoveEvent;
import dev.G255.Lumi.api.events.impl.RotationEvent;
import dev.G255.Lumi.api.utils.Wrapper;
import dev.G255.Lumi.api.utils.combat.CombatUtil;
import dev.G255.Lumi.api.utils.math.Timer;
import dev.G255.Lumi.api.utils.player.EntityUtil;
import dev.G255.Lumi.api.utils.player.InventoryUtil;
import dev.G255.Lumi.api.utils.player.MovementUtil;
import dev.G255.Lumi.api.utils.world.BlockUtil;
import dev.G255.Lumi.core.impl.BreakManager;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.impl.exploit.Blink;
import dev.G255.Lumi.mod.modules.impl.movement.ElytraFly;
import dev.G255.Lumi.mod.modules.impl.movement.Velocity;
import dev.G255.Lumi.mod.modules.impl.player.PacketMine;
import dev.G255.Lumi.mod.modules.settings.enums.Timing;
import dev.G255.Lumi.mod.modules.settings.impl.BindSetting;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.EnumSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.util.Hand;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;

public class SelfTrap extends Module {
   public static SelfTrap INSTANCE;
   private final EnumSetting page;
   private final BooleanSetting godMode;
   private final SliderSetting breakTime;
   private final BooleanSetting failedSkip;
   private final SliderSetting placeDelay;
   private final BooleanSetting mineDownward;
   private final BooleanSetting extend;
   private final Timer timer;
   private final SliderSetting blocksPer;
   private final EnumSetting timing;
   private final BooleanSetting packetPlace;
   private final BooleanSetting breakCrystal;
   private final BooleanSetting eatPause;
   private final BooleanSetting center;
   private final BooleanSetting inventory;
   private final BooleanSetting enderChest;
   private final BooleanSetting head;
   private final BooleanSetting feet;
   private final BooleanSetting chest;
   private final BindSetting headKey;
   private final BooleanSetting inAir;
   private final BooleanSetting detectMining;
   private final BooleanSetting usingPause;
   private final BooleanSetting noBlockDisable;
   private final BooleanSetting moveDisable;
   private final BooleanSetting jumpDisable;
   private final BooleanSetting rotate;
   private final BooleanSetting yawStep;
   private final BooleanSetting whenElytra;
   private final SliderSetting steps;
   private final BooleanSetting checkFov;
   private final SliderSetting fov;
   private final SliderSetting priority;
   public Vec3d directionVec;
   double startX;
   double startY;
   double startZ;
   int progress;
   private boolean shouldCenter;
   public static final List airList = new ArrayList();

   public SelfTrap() {
      super("SelfTrap", Module.Category.Combat);
      this.page = this.add(new EnumSetting("Page", SelfTrap.Page.General));
      this.godMode = this.add(new BooleanSetting("GodMode", true, () -> this.page.is(SelfTrap.Page.GodMode)));
      this.breakTime = this.add(new SliderSetting("BreakTime", (double)2.0F, (double)0.0F, (double)3.0F, () -> this.page.is(SelfTrap.Page.GodMode)));
      this.failedSkip = this.add(new BooleanSetting("FailedSkip", true, () -> this.page.is(SelfTrap.Page.GodMode)));
      this.placeDelay = this.add(new SliderSetting("PlaceDelay", 50, 0, 500, () -> this.page.is(SelfTrap.Page.General)));
      this.mineDownward = this.add(new BooleanSetting("MineDownward", false, () -> this.page.is(SelfTrap.Page.General)));
      this.extend = this.add(new BooleanSetting("Extend", true, () -> this.page.is(SelfTrap.Page.General)));
      this.timer = new Timer();
      this.blocksPer = this.add(new SliderSetting("BlocksPer", 1, 1, 8, () -> this.page.is(SelfTrap.Page.General)));
      this.timing = this.add(new EnumSetting("Timing", Timing.All, () -> this.page.is(SelfTrap.Page.General)));
      this.packetPlace = this.add(new BooleanSetting("PacketPlace", true, () -> this.page.is(SelfTrap.Page.General)));
      this.breakCrystal = this.add((new BooleanSetting("Break", true, () -> this.page.is(SelfTrap.Page.General))).setParent());
      this.eatPause = this.add(new BooleanSetting("EatingPause", true, () -> this.page.is(SelfTrap.Page.General) && this.breakCrystal.isOpen()));
      this.center = this.add(new BooleanSetting("Center", true, () -> this.page.is(SelfTrap.Page.General)));
      this.inventory = this.add(new BooleanSetting("InventorySwap", true, () -> this.page.is(SelfTrap.Page.General)));
      this.enderChest = this.add(new BooleanSetting("EnderChest", true, () -> this.page.is(SelfTrap.Page.General)));
      this.head = this.add(new BooleanSetting("Head", true, () -> this.page.is(SelfTrap.Page.General)));
      this.feet = this.add(new BooleanSetting("Feet", true, () -> this.page.is(SelfTrap.Page.General)));
      this.chest = this.add(new BooleanSetting("Chest", true, () -> this.page.is(SelfTrap.Page.General)));
      this.headKey = this.add(new BindSetting("HeadKey", -1, () -> this.page.is(SelfTrap.Page.General)));
      this.inAir = this.add(new BooleanSetting("InAir", true, () -> this.page.is(SelfTrap.Page.Check)));
      this.detectMining = this.add(new BooleanSetting("DetectMining", false, () -> this.page.is(SelfTrap.Page.Check)));
      this.usingPause = this.add(new BooleanSetting("UsingPause", true, () -> this.page.is(SelfTrap.Page.Check)));
      this.noBlockDisable = this.add(new BooleanSetting("NoBlockDisable", true, () -> this.page.is(SelfTrap.Page.Check)));
      this.moveDisable = this.add(new BooleanSetting("MoveDisable", true, () -> this.page.is(SelfTrap.Page.Check)));
      this.jumpDisable = this.add(new BooleanSetting("JumpDisable", true, () -> this.page.is(SelfTrap.Page.Check)));
      this.rotate = this.add(new BooleanSetting("Rotate", true, () -> this.page.getValue() == SelfTrap.Page.Rotate));
      this.yawStep = this.add((new BooleanSetting("YawStep", false, () -> this.rotate.isOpen() && this.page.getValue() == SelfTrap.Page.Rotate)).setParent());
      this.whenElytra = this.add(new BooleanSetting("FallFlying", true, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.page.getValue() == SelfTrap.Page.Rotate));
      this.steps = this.add(new SliderSetting("Steps", 0.05, (double)0.0F, (double)1.0F, 0.01, () -> this.page.getValue() == SelfTrap.Page.Rotate && this.yawStep.isOpen()));
      this.checkFov = this.add((new BooleanSetting("OnlyLooking", true, () -> this.page.getValue() == SelfTrap.Page.Rotate && this.yawStep.isOpen())).setParent());
      this.fov = this.add(new SliderSetting("Fov", (double)20.0F, (double)0.0F, (double)360.0F, 0.1, () -> this.checkFov.isOpen() && this.page.getValue() == SelfTrap.Page.Rotate && this.yawStep.isOpen()));
      this.priority = this.add(new SliderSetting("Priority", 10, 0, 100, () -> this.page.getValue() == SelfTrap.Page.Rotate && this.yawStep.isOpen()));
      this.directionVec = null;
      this.startX = (double)0.0F;
      this.startY = (double)0.0F;
      this.startZ = (double)0.0F;
      this.progress = 0;
      this.shouldCenter = true;
      this.setChinese("");
      INSTANCE = this;
      Lumi.EVENT_BUS.subscribe((Object)(new SelfTrapTick()));
   }

   public static boolean selfIntersectPos(BlockPos pos) {
      return mc.player.getBoundingBox().intersects(new Box(pos));
   }

   public static Vec2f getRotationTo(Vec3d posFrom, Vec3d posTo) {
      Vec3d vec3d = posTo.subtract(posFrom);
      return getRotationFromVec(vec3d);
   }

   private static Vec2f getRotationFromVec(Vec3d vec) {
      double d = vec.x;
      double d2 = vec.z;
      double xz = Math.hypot(d, d2);
      d2 = vec.z;
      double d3 = vec.x;
      double yaw = normalizeAngle(Math.toDegrees(Math.atan2(d2, d3)) - (double)90.0F);
      double pitch = normalizeAngle(Math.toDegrees(-Math.atan2(vec.y, xz)));
      return new Vec2f((float)yaw, (float)pitch);
   }

   @EventListener
   public void onRotate(RotationEvent event) {
      if (this.directionVec != null && this.rotate.getValue() && this.shouldYawStep()) {
         event.setTarget(this.directionVec, this.steps.getValueFloat(), this.priority.getValueFloat());
      }

   }

   private static double normalizeAngle(double angleIn) {
      double angle = angleIn % (double)360.0F;
      if (angle >= (double)180.0F) {
         angle -= (double)360.0F;
      }

      if (angle < (double)-180.0F) {
         angle += (double)360.0F;
      }

      return angle;
   }

   public void onEnable() {
      if (!nullCheck()) {
         this.startX = mc.player.getX();
         this.startY = mc.player.getY();
         this.startZ = mc.player.getZ();
         this.shouldCenter = true;
      } else {
         if (this.moveDisable.getValue() || this.jumpDisable.getValue()) {
            this.disable();
         }

      }
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!nullCheck()) {
         if (!this.inventory.getValue() || EntityUtil.inInventory()) {
            if ((!this.timing.is(Timing.Pre) || !event.isPost()) && (!this.timing.is(Timing.Post) || !event.isPre())) {
               if (this.timer.passed((long)this.placeDelay.getValue())) {
                  this.directionVec = null;
                  this.progress = 0;
                  if (!MovementUtil.isMoving() && !mc.options.jumpKey.isPressed()) {
                     this.startX = mc.player.getX();
                     this.startY = mc.player.getY();
                     this.startZ = mc.player.getZ();
                  }

                  BlockPos pos = EntityUtil.getPlayerPos(true);
                  double distanceToStart = (double)MathHelper.sqrt((float)mc.player.squaredDistanceTo(this.startX, this.startY, this.startZ));
                  if (this.getBlock() == -1) {
                     if (this.noBlockDisable.getValue()) {
                        this.disable();
                     }

                  } else if ((!this.moveDisable.getValue() || !(distanceToStart > (double)1.0F)) && (!this.jumpDisable.getValue() || !mc.player.input.jumping)) {
                     if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
                        if (!this.usingPause.getValue() || !mc.player.isUsingItem()) {
                           if (this.inAir.getValue() || mc.player.isOnGround()) {
                              if (this.head.getValue()) {
                                 Block block = BlockUtil.getBlock(pos.up());
                                 if (block != Blocks.OBSIDIAN && block != Blocks.BEDROCK) {
                                    this.tryPlaceBlock(pos.up(2));
                                 } else {
                                    this.clickDown(pos.up(2));
                                 }
                              }

                              if (this.feet.getValue()) {
                                 this.doSurround(pos);
                              }

                              if (this.chest.getValue()) {
                                 this.doSurround(pos.up());
                              }

                              airList.clear();
                           }
                        }
                     }
                  } else {
                     this.disable();
                  }
               }
            }
         }
      }
   }

   private boolean shouldYawStep() {
      if (this.whenElytra.getValue() || !mc.player.isFallFlying() && (!ElytraFly.INSTANCE.isOn() || !ElytraFly.INSTANCE.isFallFlying())) {
         return this.yawStep.getValue() && !Velocity.INSTANCE.noRotation();
      } else {
         return false;
      }
   }

   @EventListener(
      priority = -1
   )
   public void onMove(MoveEvent event) {
      if (!nullCheck() && this.center.getValue() && !mc.player.isFallFlying()) {
         BlockPos blockPos = EntityUtil.getPlayerPos(true);
         if (mc.player.getX() - (double)blockPos.getX() - (double)0.5F <= 0.2 && mc.player.getX() - (double)blockPos.getX() - (double)0.5F >= -0.2 && mc.player.getZ() - (double)blockPos.getZ() - (double)0.5F <= 0.2 && mc.player.getZ() - (double)0.5F - (double)blockPos.getZ() >= -0.2) {
            if (this.shouldCenter && (mc.player.isOnGround() || MovementUtil.isMoving())) {
               event.setX((double)0.0F);
               event.setZ((double)0.0F);
               this.shouldCenter = false;
            }
         } else if (this.shouldCenter) {
            Vec3d centerPos = EntityUtil.getPlayerPos(true).toCenterPos();
            float rotation = getRotationTo(mc.player.getPos(), centerPos).x;
            float yawRad = rotation / 180.0F * (float)Math.PI;
            double dist = mc.player.getPos().distanceTo(new Vec3d(centerPos.x, mc.player.getY(), centerPos.z));
            double cappedSpeed = Math.min(0.2873, dist);
            double x = (double)(-((float)Math.sin((double)yawRad))) * cappedSpeed;
            double z = (double)((float)Math.cos((double)yawRad)) * cappedSpeed;
            event.setX(x);
            event.setZ(z);
         }

      }
   }

   private void doSurround(BlockPos pos) {
      for(Direction i : Direction.values()) {
         if (i != Direction.UP) {
            BlockPos offsetPos = pos.offset(i);
            if (this.godMode.getValue()) {
               for(BreakManager.BreakData breakData : Lumi.BREAK.breakMap.values()) {
                  if (breakData.getEntity() != null && (!this.failedSkip.getValue() || !breakData.failed) && breakData.pos.equals(offsetPos)) {
                     if ((double)breakData.timer.getMs() >= this.breakTime.getValue() * (double)1000.0F) {
                        airList.add(offsetPos);
                     }
                     break;
                  }
               }
            }

            if (BlockUtil.getPlaceSide(offsetPos) != null) {
               this.tryPlaceBlock(offsetPos);
            } else if (BlockUtil.canReplace(offsetPos)) {
               this.tryPlaceBlock(this.getHelperPos(offsetPos));
            }

            if (selfIntersectPos(offsetPos) && this.extend.getValue()) {
               for(Direction i2 : Direction.values()) {
                  if (i2 != Direction.UP) {
                     BlockPos offsetPos2 = offsetPos.offset(i2);
                     if (this.godMode.getValue()) {
                        for(BreakManager.BreakData breakData : Lumi.BREAK.breakMap.values()) {
                           if (breakData.getEntity() != null && (!this.failedSkip.getValue() || !breakData.failed) && breakData.pos.equals(offsetPos2)) {
                              if ((double)breakData.timer.getMs() >= this.breakTime.getValue() * (double)1000.0F) {
                                 airList.add(offsetPos2);
                              }
                              break;
                           }
                        }
                     }

                     if (selfIntersectPos(offsetPos2)) {
                        for(Direction i3 : Direction.values()) {
                           if (i3 != Direction.UP) {
                              this.tryPlaceBlock(offsetPos2);
                              BlockPos offsetPos3 = offsetPos2.offset(i3);
                              this.tryPlaceBlock(BlockUtil.getPlaceSide(offsetPos3) == null && BlockUtil.canReplace(offsetPos3) ? this.getHelperPos(offsetPos3) : offsetPos3);
                           }
                        }
                     }

                     this.tryPlaceBlock(BlockUtil.getPlaceSide(offsetPos2) == null && BlockUtil.canReplace(offsetPos2) ? this.getHelperPos(offsetPos2) : offsetPos2);
                  }
               }
            }
         }
      }

   }

   private boolean faceVector(Vec3d directionVec) {
      if (!this.shouldYawStep()) {
         Lumi.ROTATION.lookAt(directionVec);
         return true;
      } else {
         this.directionVec = directionVec;
         if (Lumi.ROTATION.inFov(directionVec, this.fov.getValueFloat())) {
            return true;
         } else {
            return !this.checkFov.getValue();
         }
      }
   }

   private void clickDown(BlockPos pos) {
      if (pos != null) {
         if (!this.detectMining.getValue() || !Lumi.BREAK.isMining(pos)) {
            int block = this.getBlock();
            if (block != -1) {
               Direction side = Direction.DOWN;
               Vec3d directionVec = new Vec3d((double)pos.getX() + (double)0.5F + (double)side.getVector().getX() * (double)0.5F, (double)pos.getY() + (double)0.5F + (double)side.getVector().getY() * (double)0.5F, (double)pos.getZ() + (double)0.5F + (double)side.getVector().getZ() * (double)0.5F);
               if (BlockUtil.clientCanPlace(pos, true) && airList.contains(pos)) {
                  if (!this.rotate.getValue() || this.faceVector(directionVec)) {
                     if (this.breakCrystal.getValue()) {
                        CombatUtil.attackCrystal(pos, this.rotate.getValue(), this.eatPause.getValue());
                     } else if (BlockUtil.hasEntity(pos, false)) {
                        return;
                     }

                     int old = mc.player.getInventory().selectedSlot;
                     this.doSwap(block);
                     BlockUtil.placedPos.add(pos);
                     if (BlockUtil.allowAirPlace()) {
                        BlockUtil.airPlace(pos, false, Hand.MAIN_HAND, this.packetPlace.getValue());
                     } else {
                        BlockUtil.clickBlock(pos.offset(side), side.getOpposite(), false, Hand.MAIN_HAND, this.packetPlace.getValue());
                     }

                     if (this.inventory.getValue()) {
                        this.doSwap(block);
                        EntityUtil.syncInventory();
                     } else {
                        this.doSwap(old);
                     }

                     if (this.rotate.getValue() && !this.shouldYawStep()) {
                        Lumi.ROTATION.snapBack();
                     }

                     ++this.progress;
                     this.timer.reset();
                  }
               }
            }
         }
      }
   }

   private void tryPlaceBlock(BlockPos pos) {
      if (pos != null) {
         if (!this.detectMining.getValue() || !Lumi.BREAK.isMining(pos)) {
            BlockPos self = EntityUtil.getPlayerPos(true);
            if (!this.mineDownward.getValue() || !Objects.equals(PacketMine.getBreakPos(), self.down()) || !Objects.equals(PacketMine.getBreakPos(), pos)) {
               if ((double)this.progress < this.blocksPer.getValue()) {
                  int block = this.getBlock();
                  if (block != -1) {
                     Direction side = BlockUtil.getPlaceSide(pos);
                     if (side != null) {
                        Vec3d directionVec = new Vec3d((double)pos.getX() + (double)0.5F + (double)side.getVector().getX() * (double)0.5F, (double)pos.getY() + (double)0.5F + (double)side.getVector().getY() * (double)0.5F, (double)pos.getZ() + (double)0.5F + (double)side.getVector().getZ() * (double)0.5F);
                        if (BlockUtil.canPlace(pos, (double)6.0F, true)) {
                           if (!this.rotate.getValue() || this.faceVector(directionVec)) {
                              if (this.breakCrystal.getValue()) {
                                 CombatUtil.attackCrystal(pos, this.rotate.getValue(), this.eatPause.getValue());
                              } else if (BlockUtil.hasEntity(pos, false)) {
                                 return;
                              }

                              int old = mc.player.getInventory().selectedSlot;
                              this.doSwap(block);
                              BlockUtil.placedPos.add(pos);
                              if (BlockUtil.allowAirPlace()) {
                                 BlockUtil.airPlace(pos, false, Hand.MAIN_HAND, this.packetPlace.getValue());
                              } else {
                                 BlockUtil.clickBlock(pos.offset(side), side.getOpposite(), false, Hand.MAIN_HAND, this.packetPlace.getValue());
                              }

                              this.timer.reset();
                              if (this.inventory.getValue()) {
                                 this.doSwap(block);
                                 EntityUtil.syncInventory();
                              } else {
                                 this.doSwap(old);
                              }

                              if (this.rotate.getValue() && !this.shouldYawStep()) {
                                 Lumi.ROTATION.snapBack();
                              }

                              ++this.progress;
                           }
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
      if (this.inventory.getValue()) {
         return InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN) == -1 && this.enderChest.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.ENDER_CHEST) : InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN);
      } else {
         return InventoryUtil.findBlock(Blocks.OBSIDIAN) == -1 && this.enderChest.getValue() ? InventoryUtil.findBlock(Blocks.ENDER_CHEST) : InventoryUtil.findBlock(Blocks.OBSIDIAN);
      }
   }

   public BlockPos getHelperPos(BlockPos pos) {
      for(Direction i : Direction.values()) {
         if ((!this.detectMining.getValue() || !Lumi.BREAK.isMining(pos.offset(i))) && BlockUtil.isStrictDirection(pos.offset(i), i.getOpposite()) && BlockUtil.canPlace(pos.offset(i))) {
            return pos.offset(i);
         }
      }

      return null;
   }

   public static enum Page {
      General,
      Rotate,
      Check,
      GodMode;

      // $FF: synthetic method
      private static Page[] $values() {
         return new Page[]{General, Rotate, Check, GodMode};
      }
   }

   public class SelfTrapTick {
      boolean pressed = false;

      @EventListener
      public void onTick(ClientTickEvent event) {
         if (!Module.nullCheck()) {
            if ((!SelfTrap.this.timing.is(Timing.Pre) || !event.isPost()) && (!SelfTrap.this.timing.is(Timing.Post) || !event.isPre())) {
               if (SelfTrap.this.headKey.isPressed()) {
                  if (!this.pressed) {
                     this.pressed = true;
                     SelfTrap.this.directionVec = null;
                     SelfTrap.this.progress = 0;
                     BlockPos pos = EntityUtil.getPlayerPos(true);
                     if (SelfTrap.this.getBlock() != -1) {
                        if (SelfTrap.this.inAir.getValue() || Wrapper.mc.player.isOnGround()) {
                           Block block = BlockUtil.getBlock(pos.up());
                           if (block != Blocks.OBSIDIAN && block != Blocks.BEDROCK) {
                              SelfTrap.this.tryPlaceBlock(pos.up(2));
                           } else {
                              SelfTrap.this.clickDown(pos.up(2));
                           }

                        }
                     }
                  }
               } else {
                  this.pressed = false;
               }
            }
         }
      }
   }
}
