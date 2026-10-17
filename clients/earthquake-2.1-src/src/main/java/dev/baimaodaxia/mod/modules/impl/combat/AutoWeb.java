package dev.baimaodaxia.mod.modules.impl.combat;

import dev.baimaodaxia.Alien;
import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.ClientTickEvent;
import dev.baimaodaxia.api.events.impl.RotationEvent;
import dev.baimaodaxia.api.utils.combat.CombatUtil;
import dev.baimaodaxia.api.utils.math.PredictUtil;
import dev.baimaodaxia.api.utils.math.Timer;
import dev.baimaodaxia.api.utils.player.EntityUtil;
import dev.baimaodaxia.api.utils.player.InventoryUtil;
import dev.baimaodaxia.api.utils.world.BlockPosX;
import dev.baimaodaxia.api.utils.world.BlockUtil;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.impl.client.AntiCheat;
import dev.baimaodaxia.mod.modules.impl.exploit.Blink;
import dev.baimaodaxia.mod.modules.impl.movement.ElytraFly;
import dev.baimaodaxia.mod.modules.impl.movement.Velocity;
import dev.baimaodaxia.mod.modules.settings.enums.Timing;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import net.minecraft.util.Hand;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.hit.BlockHitResult;

public class AutoWeb extends Module {
   public static AutoWeb INSTANCE;
   public static boolean force = false;
   public static boolean ignore = false;
   public final EnumSetting<Page> page;
   public final SliderSetting placeDelay;
   public final SliderSetting blocksPer;
   public final SliderSetting predictTicks;
   public final SliderSetting maxWebs;
   public final SliderSetting offset;
   public final SliderSetting placeRange;
   public final SliderSetting targetRange;
   final ArrayList<BlockPos> pos;
   private final BooleanSetting preferAnchor;
   private final BooleanSetting detectMining;
   private final EnumSetting<Timing> timing;
   private final BooleanSetting feet;
   private final BooleanSetting feetExtend;
   private final BooleanSetting face;
   private final BooleanSetting down;
   private final BooleanSetting inventorySwap;
   private final BooleanSetting usingPause;
   private final BooleanSetting rotate;
   private final BooleanSetting yawStep;
   private final BooleanSetting whenElytra;
   private final SliderSetting steps;
   private final BooleanSetting checkFov;
   private final SliderSetting fov;
   private final SliderSetting priority;
   private final Timer timer;
   public Vec3d directionVec;
   int progress;

   public AutoWeb() {
      super("AutoWeb", Module.Category.Combat);
      this.page = this.add(new EnumSetting("Page", AutoWeb.Page.General));
      this.placeDelay = this.add(new SliderSetting("PlaceDelay", 50, 0, 500, () -> this.page.getValue() == AutoWeb.Page.General));
      this.blocksPer = this.add(new SliderSetting("BlocksPer", 2, 1, 10, () -> this.page.getValue() == AutoWeb.Page.General));
      this.predictTicks = this.add(new SliderSetting("PredictTicks", (double)2.0F, (double)0.0F, (double)50.0F, (double)1.0F, () -> this.page.getValue() == AutoWeb.Page.General));
      this.maxWebs = this.add(new SliderSetting("MaxWebs", (double)2.0F, (double)1.0F, (double)8.0F, (double)1.0F, () -> this.page.getValue() == AutoWeb.Page.General));
      this.offset = this.add(new SliderSetting("Offset", (double)0.25F, (double)0.0F, 0.3, 0.01, () -> this.page.getValue() == AutoWeb.Page.General));
      this.placeRange = this.add(new SliderSetting("PlaceRange", (double)5.0F, (double)0.0F, (double)6.0F, 0.1, () -> this.page.getValue() == AutoWeb.Page.General));
      this.targetRange = this.add(new SliderSetting("TargetRange", (double)8.0F, (double)0.0F, (double)8.0F, 0.1, () -> this.page.getValue() == AutoWeb.Page.General));
      this.pos = new ArrayList();
      this.preferAnchor = this.add(new BooleanSetting("PreferAnchor", true, () -> this.page.getValue() == AutoWeb.Page.General));
      this.detectMining = this.add(new BooleanSetting("DetectMining", true, () -> this.page.getValue() == AutoWeb.Page.General));
      this.timing = this.add(new EnumSetting("Timing", Timing.All, () -> this.page.getValue() == AutoWeb.Page.General));
      this.feet = this.add(new BooleanSetting("Feet", true, () -> this.page.getValue() == AutoWeb.Page.General));
      this.feetExtend = this.add(new BooleanSetting("FeetExtend", true, () -> this.page.getValue() == AutoWeb.Page.General));
      this.face = this.add(new BooleanSetting("Face", true, () -> this.page.getValue() == AutoWeb.Page.General));
      this.down = this.add(new BooleanSetting("Down", true, () -> this.page.getValue() == AutoWeb.Page.General));
      this.inventorySwap = this.add(new BooleanSetting("InventorySwap", true, () -> this.page.getValue() == AutoWeb.Page.General));
      this.usingPause = this.add(new BooleanSetting("UsingPause", true, () -> this.page.getValue() == AutoWeb.Page.General));
      this.rotate = this.add((new BooleanSetting("Rotate", true, () -> this.page.getValue() == AutoWeb.Page.Rotate)).setParent());
      this.yawStep = this.add((new BooleanSetting("YawStep", false, () -> this.rotate.isOpen() && this.page.getValue() == AutoWeb.Page.Rotate)).setParent());
      this.whenElytra = this.add(new BooleanSetting("FallFlying", true, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.page.getValue() == AutoWeb.Page.Rotate));
      this.steps = this.add(new SliderSetting("Steps", 0.3, 0.1, (double)1.0F, 0.01, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.page.getValue() == AutoWeb.Page.Rotate));
      this.checkFov = this.add(new BooleanSetting("OnlyLooking", true, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.page.getValue() == AutoWeb.Page.Rotate));
      this.fov = this.add(new SliderSetting("Fov", (double)20.0F, (double)0.0F, (double)360.0F, 0.1, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.checkFov.getValue() && this.page.getValue() == AutoWeb.Page.Rotate));
      this.priority = this.add(new SliderSetting("Priority", 10, 0, 100, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.page.getValue() == AutoWeb.Page.Rotate));
      this.timer = new Timer();
      this.directionVec = null;
      this.progress = 0;
      this.setChinese("蜘蛛网光环");
      INSTANCE = this;
   }

   public String getInfo() {
      return this.pos.isEmpty() ? null : "Working";
   }

   private boolean shouldYawStep() {
      if (this.whenElytra.getValue() || !mc.player.isFallFlying() && (!ElytraFly.INSTANCE.isOn() || !ElytraFly.INSTANCE.isFallFlying())) {
         return this.yawStep.getValue() && !Velocity.INSTANCE.noRotation();
      } else {
         return false;
      }
   }

   @EventListener
   public void onRotate(RotationEvent event) {
      if (this.rotate.getValue() && this.shouldYawStep() && this.directionVec != null) {
         event.setTarget(this.directionVec, this.steps.getValueFloat(), this.priority.getValueFloat());
      }

   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!nullCheck()) {
         if ((!this.timing.is(Timing.Pre) || !event.isPost()) && (!this.timing.is(Timing.Post) || !event.isPre())) {
            if (force) {
               ignore = true;
            }

            this.update();
            ignore = false;
         }
      }
   }

   public void onDisable() {
      force = false;
   }

   private void update() {
      if (this.timer.passed((long)this.placeDelay.getValueInt())) {
         if (!this.inventorySwap.getValue() || EntityUtil.inInventory()) {
            this.pos.clear();
            this.progress = 0;
            this.directionVec = null;
            if (!this.preferAnchor.getValue() || AutoAnchor.INSTANCE.currentPos == null) {
               if (this.getWebSlot() != -1) {
                  if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
                     if (!this.usingPause.getValue() || !mc.player.isUsingItem()) {
                        label171:
                        for(PlayerEntity player : CombatUtil.getEnemies(this.targetRange.getValue())) {
                           Vec3d playerPos = this.predictTicks.getValue() > (double)0.0F ? PredictUtil.getPos(player, this.predictTicks.getValueInt()) : player.getPos();
                           int webs = 0;
                           if (this.feet.getValue() && this.placeWeb(new BlockPosX(playerPos.getX(), playerPos.getY(), playerPos.getZ()))) {
                              ++webs;
                           }

                           if (this.down.getValue()) {
                              this.placeWeb(new BlockPosX(playerPos.getX(), playerPos.getY() - 0.8, playerPos.getZ()));
                           }

                           ArrayList<BlockPosX> list = new ArrayList();

                           for(float x : new float[]{0.0F, this.offset.getValueFloat(), -this.offset.getValueFloat()}) {
                              for(float z : new float[]{0.0F, this.offset.getValueFloat(), -this.offset.getValueFloat()}) {
                                 for(float y : new float[]{0.0F, 1.0F, -1.0F}) {
                                    BlockPosX pos = new BlockPosX(playerPos.getX() + (double)x, playerPos.getY() + (double)y, playerPos.getZ() + (double)z);
                                    if (!list.contains(pos)) {
                                       list.add(pos);
                                       if (this.isTargetHere(pos, player) && mc.world.getBlockState(pos).getBlock() == Blocks.COBWEB && !Alien.BREAK.isMining(pos)) {
                                          ++webs;
                                       }
                                    }
                                 }
                              }
                           }

                           if (!((float)webs >= this.maxWebs.getValueFloat()) || ignore) {
                              boolean skip = false;
                              if (this.feetExtend.getValue()) {
                                 label145:
                                 for(float x : new float[]{0.0F, this.offset.getValueFloat(), -this.offset.getValueFloat()}) {
                                    for(float z : new float[]{0.0F, this.offset.getValueFloat(), -this.offset.getValueFloat()}) {
                                       BlockPosX pos = new BlockPosX(playerPos.getX() + (double)x, playerPos.getY(), playerPos.getZ() + (double)z);
                                       if (this.isTargetHere(pos, player) && this.placeWeb(pos)) {
                                          ++webs;
                                          if ((float)webs >= this.maxWebs.getValueFloat()) {
                                             skip = true;
                                             break label145;
                                          }
                                       }
                                    }
                                 }
                              }

                              if (!skip && this.face.getValue()) {
                                 for(float x : new float[]{0.0F, this.offset.getValueFloat(), -this.offset.getValueFloat()}) {
                                    for(float z : new float[]{0.0F, this.offset.getValueFloat(), -this.offset.getValueFloat()}) {
                                       BlockPosX pos = new BlockPosX(playerPos.getX() + (double)x, playerPos.getY() + 1.1, playerPos.getZ() + (double)z);
                                       if (this.isTargetHere(pos, player) && this.placeWeb(pos)) {
                                          ++webs;
                                          if ((float)webs >= this.maxWebs.getValueFloat()) {
                                             continue label171;
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
            }
         }
      }
   }

   private boolean isTargetHere(BlockPos pos, PlayerEntity target) {
      return (new Box(pos)).intersects(target.getBoundingBox());
   }

   private boolean placeWeb(BlockPos pos) {
      if (this.pos.contains(pos)) {
         return false;
      } else {
         this.pos.add(pos);
         if (this.progress >= this.blocksPer.getValueInt()) {
            return false;
         } else if (this.getWebSlot() == -1) {
            return false;
         } else if (this.detectMining.getValue() && Alien.BREAK.isMining(pos)) {
            return false;
         } else if (BlockUtil.getPlaceSide(pos, this.placeRange.getValue()) != null && (mc.world.isAir(pos) || ignore && BlockUtil.getBlock(pos) == Blocks.COBWEB) && pos.getY() < 320) {
            int oldSlot = mc.player.getInventory().selectedSlot;
            int webSlot = this.getWebSlot();
            if (!this.placeBlock(pos, this.rotate.getValue(), webSlot)) {
               return false;
            } else {
               BlockUtil.placedPos.add(pos);
               ++this.progress;
               if (this.inventorySwap.getValue()) {
                  this.doSwap(webSlot);
                  EntityUtil.syncInventory();
               } else {
                  this.doSwap(oldSlot);
               }

               force = false;
               this.timer.reset();
               return true;
            }
         } else {
            return false;
         }
      }
   }

   public boolean placeBlock(BlockPos pos, boolean rotate, int slot) {
      Direction side = BlockUtil.getPlaceSide(pos);
      if (side == null) {
         return BlockUtil.allowAirPlace() ? this.clickBlock(pos, Direction.DOWN, rotate, slot) : false;
      } else {
         return this.clickBlock(pos.offset(side), side.getOpposite(), rotate, slot);
      }
   }

   public boolean clickBlock(BlockPos pos, Direction side, boolean rotate, int slot) {
      Vec3d directionVec = new Vec3d((double)pos.getX() + (double)0.5F + (double)side.getVector().getX() * (double)0.5F, (double)pos.getY() + (double)0.5F + (double)side.getVector().getY() * (double)0.5F, (double)pos.getZ() + (double)0.5F + (double)side.getVector().getZ() * (double)0.5F);
      if (rotate && !this.faceVector(directionVec)) {
         return false;
      } else {
         this.doSwap(slot);
         EntityUtil.swingHand(Hand.MAIN_HAND, AntiCheat.INSTANCE.interactSwing.getValue());
         BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
         Module.sendSequencedPacket((id) -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, id));
         if (rotate && !this.shouldYawStep()) {
            Alien.ROTATION.snapBack();
         }

         return true;
      }
   }

   private boolean faceVector(Vec3d directionVec) {
      if (!this.shouldYawStep()) {
         Alien.ROTATION.lookAt(directionVec);
         return true;
      } else {
         this.directionVec = directionVec;
         if (Alien.ROTATION.inFov(directionVec, this.fov.getValueFloat())) {
            return true;
         } else {
            return !this.checkFov.getValue();
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

   private int getWebSlot() {
      return this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.COBWEB) : InventoryUtil.findBlock(Blocks.COBWEB);
   }

   public static enum Page {
      General,
      Rotate;

      // $FF: synthetic method
      private static Page[] $values() {
         return new Page[]{General, Rotate};
      }
   }
}
