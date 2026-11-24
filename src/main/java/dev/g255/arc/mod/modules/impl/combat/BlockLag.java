package dev.g255.arc.mod.modules.impl.combat;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.ClientTickEvent;
import dev.g255.arc.api.events.impl.MoveEvent;
import dev.g255.arc.api.utils.combat.CombatUtil;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.api.utils.player.EntityUtil;
import dev.g255.arc.api.utils.player.InventoryUtil;
import dev.g255.arc.api.utils.player.MovementUtil;
import dev.g255.arc.api.utils.world.BlockPosX;
import dev.g255.arc.api.utils.world.BlockUtil;
import dev.g255.arc.core.impl.CommandManager;
import dev.g255.arc.core.impl.RotationManager;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.impl.client.AntiCheat;
import dev.g255.arc.mod.modules.impl.exploit.Blink;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.util.Hand;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.hit.BlockHitResult;

public class BlockLag extends Module {
   public static BlockLag INSTANCE;
   private final EnumSetting<RotateMode> rotate;
   private final SliderSetting yawStepSteps;
   private final SliderSetting priority;
   private final EnumSetting<LagBackMode> lagMode;
   private final EnumSetting<LagBackMode> aboveLagMode;
   private final List<BlockPos> placePos;
   private final Timer timer;
   private final Timer webTimer;
   private final BooleanSetting disable;
   private final BooleanSetting jumpDisable;
   private final SliderSetting delay;
   private final SliderSetting cuicanHeight;
   private final SliderSetting webTime;
   private final BooleanSetting enderChest;
   private final BooleanSetting antiLag;
   private final BooleanSetting helper;
   private final BooleanSetting cancelBlink;
   private final BooleanSetting autoCenter;
   private final BooleanSetting single;
   private final BooleanSetting detectMine;
   private final BooleanSetting headFill;
   private final BooleanSetting headFillEnder;
   private final BooleanSetting usingPause;
   private final BooleanSetting down;
   private final BooleanSetting noSelfPos;
   private final BooleanSetting packetPlace;
   private final BooleanSetting sound;
   private final SliderSetting blocksPer;
   private final BooleanSetting breakCrystal;
   private final BooleanSetting wait;
   private final BooleanSetting fakeMove;
   private final BooleanSetting center;
   private final SliderSetting preCorrect;
   private final SliderSetting moveDis;
   private final SliderSetting moveDis2;
   private final SliderSetting moveCorrect2;
   private final SliderSetting yOffset;
   private final BooleanSetting inventory;
   private final SliderSetting smartX;
   private final SliderSetting smartUp;
   private final SliderSetting smartDown;
   private final SliderSetting smartDistance;
   private int progress;
   private Vec3d currentPos;
   private boolean shouldCenter;

   public BlockLag() {
      super("BlockLag", Module.Category.Combat);
      this.rotate = this.add(new EnumSetting("RotateMode", BlockLag.RotateMode.Bypass));
      this.yawStepSteps = this.add(new SliderSetting("YawStepSteps", 0.3, 0.1, (double)1.0F, 0.01, () -> this.rotate.getValue() == BlockLag.RotateMode.Yawstep));
      this.priority = this.add(new SliderSetting("Priority", 10, 0, 100, () -> this.rotate.getValue() == BlockLag.RotateMode.Yawstep));
      this.lagMode = this.add(new EnumSetting("LagMode", BlockLag.LagBackMode.TrollHack));
      this.aboveLagMode = this.add(new EnumSetting("MoveLagMode", BlockLag.LagBackMode.Smart));
      this.placePos = new ArrayList();
      this.timer = new Timer();
      this.webTimer = new Timer();
      this.disable = this.add(new BooleanSetting("Disable", true));
      this.jumpDisable = this.add(new BooleanSetting("JumpDisable", true, () -> !this.disable.getValue()));
      this.delay = this.add(new SliderSetting("Delay", 500, 0, 1000, () -> !this.disable.getValue()));
      this.cuicanHeight = this.add(new SliderSetting("cancuiHeight", (double)4.0F, (double)-10.0F, (double)10.0F, 0.1));
      this.webTime = this.add(new SliderSetting("WebTime", 0, 0, 500));
      this.enderChest = this.add(new BooleanSetting("EnderChest", true));
      this.antiLag = this.add(new BooleanSetting("AntiLag", false));
      this.helper = this.add(new BooleanSetting("Helper", true));
      this.cancelBlink = this.add(new BooleanSetting("CancelBlink", true));
      this.autoCenter = this.add(new BooleanSetting("AutoCenter", false));
      this.single = this.add(new BooleanSetting("SingleBlock", false));
      this.detectMine = this.add(new BooleanSetting("DetectMining", false));
      this.headFill = this.add(new BooleanSetting("HeadFill", false));
      this.headFillEnder = this.add(new BooleanSetting("2QE_", false, () -> this.headFill.getValue()));
      this.usingPause = this.add(new BooleanSetting("UsingPause", false));
      this.down = this.add(new BooleanSetting("Down", true));
      this.noSelfPos = this.add(new BooleanSetting("NoSelfPos", false));
      this.packetPlace = this.add(new BooleanSetting("PacketPlace", true));
      this.sound = this.add(new BooleanSetting("Sound", true));
      this.blocksPer = this.add(new SliderSetting("BlocksPer", (double)4.0F, (double)1.0F, (double)4.0F, (double)1.0F));
      this.breakCrystal = this.add(new BooleanSetting("Break", true));
      BooleanSetting var10006 = this.disable;
      Objects.requireNonNull(var10006);
      this.wait = this.add(new BooleanSetting("Wait", true, var10006::getValue));
      this.fakeMove = this.add((new BooleanSetting("FakeMove", true)).setParent());
      var10006 = this.fakeMove;
      Objects.requireNonNull(var10006);
      this.center = this.add(new BooleanSetting("AllowCenter", true, var10006::isOpen));
      BooleanSetting var10009 = this.fakeMove;
      Objects.requireNonNull(var10009);
      this.preCorrect = this.add(new SliderSetting("PreCorrect", (double)0.25F, (double)0.0F, (double)1.0F, 0.001, var10009::isOpen));
      var10009 = this.fakeMove;
      Objects.requireNonNull(var10009);
      this.moveDis = this.add(new SliderSetting("MoveDis", (double)0.25F, (double)0.0F, (double)1.0F, 0.001, var10009::isOpen));
      var10009 = this.fakeMove;
      Objects.requireNonNull(var10009);
      this.moveDis2 = this.add(new SliderSetting("MoveDis2", (double)0.25F, (double)0.0F, (double)1.0F, 0.001, var10009::isOpen));
      var10009 = this.fakeMove;
      Objects.requireNonNull(var10009);
      this.moveCorrect2 = this.add(new SliderSetting("Correct", (double)0.25F, (double)0.0F, (double)1.0F, 0.001, var10009::isOpen));
      var10009 = this.fakeMove;
      Objects.requireNonNull(var10009);
      this.yOffset = this.add(new SliderSetting("YOffset", 0.01, (double)0.0F, (double)1.0F, 0.001, var10009::isOpen));
      this.inventory = this.add(new BooleanSetting("InventorySwap", true));
      this.smartX = this.add(new SliderSetting("SmartXZ", (double)3.0F, (double)0.0F, (double)10.0F, 0.1, () -> this.lagMode.getValue() == BlockLag.LagBackMode.Smart || this.aboveLagMode.getValue() == BlockLag.LagBackMode.Smart));
      this.smartUp = this.add(new SliderSetting("SmartUp", (double)3.0F, (double)0.0F, (double)10.0F, 0.1, () -> this.lagMode.getValue() == BlockLag.LagBackMode.Smart || this.aboveLagMode.getValue() == BlockLag.LagBackMode.Smart));
      this.smartDown = this.add(new SliderSetting("SmartDown", (double)3.0F, (double)0.0F, (double)10.0F, 0.1, () -> this.lagMode.getValue() == BlockLag.LagBackMode.Smart || this.aboveLagMode.getValue() == BlockLag.LagBackMode.Smart));
      this.smartDistance = this.add(new SliderSetting("SmartDistance", (double)2.0F, (double)0.0F, (double)10.0F, 0.1, () -> this.lagMode.getValue() == BlockLag.LagBackMode.Smart || this.aboveLagMode.getValue() == BlockLag.LagBackMode.Smart));
      this.progress = 0;
      this.shouldCenter = true;
      this.setChinese("卡黑曜石+");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(ClientTickEvent event) {
      if ((!this.inventory.getValue() || EntityUtil.inInventory()) && event.isPost()) {
         if (this.cancelBlink.getValue() && Blink.INSTANCE.isOn()) {
            return;
         }

         if (EntityUtil.isInsideBlock() && mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING) && MovementUtil.isMoving()) {
            MovementUtil.setMotionY(0.01);
         }

         if (!this.disable.getValue() && this.jumpDisable.getValue() && mc.player.input.jumping) {
            this.disable();
            return;
         }

         if (Arc.PLAYER.isInWeb(mc.player)) {
            this.webTimer.reset();
            return;
         }

         if (this.usingPause.getValue() && mc.player.isUsingItem()) {
            return;
         }

         if (!this.webTimer.passedMs(this.webTime.getValue())) {
            return;
         }

         if (!this.disable.getValue() && !this.timer.passedMs(this.delay.getValue())) {
            return;
         }

         if (!mc.player.isOnGround()) {
            return;
         }

         if (this.antiLag.getValue() && !BlockUtil.canCollide(mc.player, new Box(EntityUtil.getPlayerPos(true).down()))) {
            return;
         }

         if (this.single.getValue() && EntityUtil.isInsideBlock()) {
            if (this.disable.getValue()) {
               this.disable();
            }

            return;
         }

         if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
            return;
         }

         int oldSlot = mc.player.getInventory().selectedSlot;
         int block = this.getBlock();
         if (block == -1) {
            CommandManager.sendMessageId("§4No block found.", this.hashCode() - 1);
            this.disable();
            return;
         }

         this.progress = 0;
         this.placePos.clear();
         double offset = this.single.getValue() ? (double)0.0F : AntiCheat.getOffset();
         BlockPosX pos1 = new BlockPosX(mc.player.getX() + offset, mc.player.getY() + (double)0.5F, mc.player.getZ() + offset);
         BlockPosX pos2 = new BlockPosX(mc.player.getX() - offset, mc.player.getY() + (double)0.5F, mc.player.getZ() + offset);
         BlockPosX pos3 = new BlockPosX(mc.player.getX() + offset, mc.player.getY() + (double)0.5F, mc.player.getZ() - offset);
         BlockPosX pos4 = new BlockPosX(mc.player.getX() - offset, mc.player.getY() + (double)0.5F, mc.player.getZ() - offset);
         BlockPosX pos5 = new BlockPosX(mc.player.getX() + offset, mc.player.getY() + (double)1.5F, mc.player.getZ() + offset);
         BlockPosX pos6 = new BlockPosX(mc.player.getX() - offset, mc.player.getY() + (double)1.5F, mc.player.getZ() + offset);
         BlockPosX pos7 = new BlockPosX(mc.player.getX() + offset, mc.player.getY() + (double)1.5F, mc.player.getZ() - offset);
         BlockPosX pos8 = new BlockPosX(mc.player.getX() - offset, mc.player.getY() + (double)1.5F, mc.player.getZ() - offset);
         BlockPosX pos9 = new BlockPosX(mc.player.getX() + offset, mc.player.getY() - (double)1.0F, mc.player.getZ() + offset);
         BlockPosX pos10 = new BlockPosX(mc.player.getX() - offset, mc.player.getY() - (double)1.0F, mc.player.getZ() + offset);
         BlockPosX pos11 = new BlockPosX(mc.player.getX() + offset, mc.player.getY() - (double)1.0F, mc.player.getZ() - offset);
         BlockPosX pos12 = new BlockPosX(mc.player.getX() - offset, mc.player.getY() - (double)1.0F, mc.player.getZ() - offset);
         BlockPos playerPos = EntityUtil.getPlayerPos(true);
         boolean headFill = false;
         if (!this.canPlace(pos1) && !this.canPlace(pos2) && !this.canPlace(pos3) && !this.canPlace(pos4)) {
            boolean cantHeadFill = !this.headFill.getValue() || !this.canPlace(pos5) && !this.canPlace(pos6) && !this.canPlace(pos7) && !this.canPlace(pos8);
            boolean cantDown = !this.down.getValue() || !this.canPlace(pos9) && !this.canPlace(pos10) && !this.canPlace(pos11) && !this.canPlace(pos12);
            if (cantHeadFill) {
               if (cantDown) {
                  if (!this.wait.getValue() && this.disable.getValue()) {
                     this.disable();
                  }

                  return;
               }
            } else {
               headFill = true;
            }
         }

         boolean above = false;
         BlockPos headPos = EntityUtil.getPlayerPos(true).up(2);
         boolean rotate = this.rotate.getValue() == BlockLag.RotateMode.Normal || this.rotate.getValue() == BlockLag.RotateMode.Yawstep;
         CombatUtil.attackCrystal((BlockPos)pos1, rotate, false);
         CombatUtil.attackCrystal((BlockPos)pos2, rotate, false);
         CombatUtil.attackCrystal((BlockPos)pos3, rotate, false);
         CombatUtil.attackCrystal((BlockPos)pos4, rotate, false);
         if (!headFill && !mc.player.isCrawling() && !this.trapped(headPos) && !this.trapped(headPos.add(1, 0, 0)) && !this.trapped(headPos.add(-1, 0, 0)) && !this.trapped(headPos.add(0, 0, 1)) && !this.trapped(headPos.add(0, 0, -1)) && !this.trapped(headPos.add(1, 0, -1)) && !this.trapped(headPos.add(-1, 0, -1)) && !this.trapped(headPos.add(1, 0, 1)) && !this.trapped(headPos.add(-1, 0, 1))) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.4199999868869781, mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.7531999805212017, mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.9999957640154541, mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.1661092609382138, mc.player.getZ(), false));
            this.currentPos = new Vec3d(mc.player.getX(), mc.player.getY() + 1.1661092609382138, mc.player.getZ());
         } else {
            above = true;
            if (!this.fakeMove.getValue()) {
               if (!this.wait.getValue() && this.disable.getValue()) {
                  this.disable();
               }

               return;
            }

            if (!this.fakeMove()) {
               return;
            }
         }

         this.timer.reset();
         this.doSwap(block);
         if (this.rotate.getValue() == BlockLag.RotateMode.Bypass) {
            if (above) {
               float[] angle = RotationManager.getRotation(this.currentPos.add((double)0.0F, (double)mc.player.getEyeHeight(mc.player.getPose()), (double)0.0F), mc.player.getPos());
               Arc.ROTATION.snapAt(angle[0], angle[1]);
            } else {
               Arc.ROTATION.snapAt(Arc.ROTATION.rotationYaw, 90.0F);
            }
         } else if (this.rotate.getValue() == BlockLag.RotateMode.Yawstep) {
            if (above) {
               this.applyYawStepRotation(mc.player.getPos());
            } else {
               float[] angle = new float[]{Arc.ROTATION.rotationYaw, 90.0F};
               float[] stepped = Arc.ROTATION.injectStep(angle, this.yawStepSteps.getValueFloat());
               Arc.ROTATION.snapAt(stepped[0], stepped[1]);
            }
         }

         this.placeBlock(playerPos, rotate);
         this.placeBlock(pos1, rotate);
         this.placeBlock(pos2, rotate);
         this.placeBlock(pos3, rotate);
         this.placeBlock(pos4, rotate);
         if (this.down.getValue()) {
            this.placeBlock(pos9, rotate);
            this.placeBlock(pos10, rotate);
            this.placeBlock(pos11, rotate);
            this.placeBlock(pos12, rotate);
         }

         if (this.headFill.getValue() && above) {
            int headSlot = -1;
            if (this.headFillEnder.getValue()) {
               headSlot = this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.ENDER_CHEST) : InventoryUtil.findBlock(Blocks.ENDER_CHEST);
            }

            boolean swapped = false;
            if (headSlot != -1 && headSlot != mc.player.getInventory().selectedSlot) {
               this.doSwap(headSlot);
               swapped = true;
            }

            this.placeBlock(pos5, rotate);
            this.placeBlock(pos6, rotate);
            this.placeBlock(pos7, rotate);
            this.placeBlock(pos8, rotate);
            if (swapped) {
               if (this.inventory.getValue()) {
                  this.doSwap(headSlot);
               } else {
                  this.doSwap(block);
               }
            }
         }

         if (rotate && this.rotate.getValue() == BlockLag.RotateMode.Yawstep) {
            Arc.ROTATION.snapBack();
         }

         if (this.inventory.getValue()) {
            this.doSwap(block);
            EntityUtil.syncInventory();
         } else {
            this.doSwap(oldSlot);
         }

         switch ((above ? (LagBackMode)this.aboveLagMode.getValue() : (LagBackMode)this.lagMode.getValue()).ordinal()) {
            case 0:
               ArrayList<BlockPosX> list = new ArrayList();

               for(double x = mc.player.getPos().getX() - this.smartX.getValue(); x < mc.player.getPos().getX() + this.smartX.getValue(); ++x) {
                  for(double z = mc.player.getPos().getZ() - this.smartX.getValue(); z < mc.player.getPos().getZ() + this.smartX.getValue(); ++z) {
                     for(double d = mc.player.getPos().getY() - this.smartDown.getValue(); d < mc.player.getPos().getY() + this.smartUp.getValue(); ++d) {
                        list.add(new BlockPosX(x, d, z));
                     }
                  }
               }

               double distance = (double)0.0F;
               BlockPos bestPos = null;

               for(BlockPos blockPos : list) {
                  if (this.canMove(blockPos) && !((double)MathHelper.sqrt((float)mc.player.squaredDistanceTo(blockPos.toCenterPos().add((double)0.0F, (double)-0.5F, (double)0.0F))) < this.smartDistance.getValue()) && (bestPos == null || mc.player.squaredDistanceTo(blockPos.toCenterPos()) < distance)) {
                     bestPos = blockPos;
                     distance = mc.player.squaredDistanceTo(blockPos.toCenterPos());
                  }
               }

               if (bestPos != null) {
                  mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround((double)bestPos.getX() + (double)0.5F, (double)bestPos.getY(), (double)bestPos.getZ() + (double)0.5F, false));
               }
               break;
            case 1:
               for(int i = 0; i < 20; ++i) {
                  mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + (double)1337.0F, mc.player.getZ(), false));
               }
               break;
            case 2:
               mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 2.3400880035762786, mc.player.getZ(), false));
               break;
            case 3:
               mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), (double)-70.0F, mc.player.getZ(), false));
               break;
            case 4:
               mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - this.cuicanHeight.getValue(), mc.player.getZ(), false));
               break;
            case 5:
               mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), (double)-7.0F, mc.player.getZ(), false));
               break;
            case 6:
               mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.9, mc.player.getZ(), false));
               break;
            case 7:
               mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(-180.0F, -90.0F, false));
               mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(180.0F, 90.0F, false));
               break;
            case 8:
               mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.16610926093821, mc.player.getZ(), false));
               mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.170005801788139, mc.player.getZ(), false));
               mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.2426308013947485, mc.player.getZ(), false));
               mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 2.3400880035762786, mc.player.getZ(), false));
               mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 2.640088003576279, mc.player.getZ(), false));
               break;
            case 9:
               mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.1001, mc.player.getZ(), false));
               mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.0605, mc.player.getZ(), false));
               mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.0802, mc.player.getZ(), false));
               mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.1127, mc.player.getZ(), false));
         }

         if (this.disable.getValue()) {
            this.disable();
         }
      }

   }

   @EventListener(
      priority = -1
   )
   public void onMove(MoveEvent event) {
      if (!nullCheck() && this.isOn() && this.autoCenter.getValue() && !mc.player.isSpectator()) {
         BlockPos blockPos = EntityUtil.getPlayerPos(true);
         if (mc.player.getX() - (double)blockPos.getX() - (double)0.5F <= 0.2 && mc.player.getX() - (double)blockPos.getX() - (double)0.5F >= -0.2 && mc.player.getZ() - (double)blockPos.getZ() - (double)0.5F <= 0.2 && mc.player.getZ() - (double)0.5F - (double)blockPos.getZ() >= -0.2) {
            if (this.shouldCenter && (mc.player.isOnGround() || MovementUtil.isMoving())) {
               event.setX((double)0.0F);
               event.setZ((double)0.0F);
               this.shouldCenter = false;
            }
         } else if (this.shouldCenter) {
            Vec3d centerPos = blockPos.toCenterPos();
            Vec2f rotation = this.getRotationTo(mc.player.getPos(), centerPos);
            float yawRad = rotation.x / 180.0F * (float)Math.PI;
            double dist = mc.player.getPos().distanceTo(new Vec3d(centerPos.x, mc.player.getY(), centerPos.z));
            double cappedSpeed = Math.min(0.2873, dist);
            double x = -Math.sin((double)yawRad) * cappedSpeed;
            double z = Math.cos((double)yawRad) * cappedSpeed;
            event.setX(x);
            event.setZ(z);
         }

      }
   }

   private Vec2f getRotationTo(Vec3d posFrom, Vec3d posTo) {
      Vec3d vec3d = posTo.subtract(posFrom);
      double diffXZ = Math.sqrt(vec3d.x * vec3d.x + vec3d.z * vec3d.z);
      float yaw = (float)Math.toDegrees(Math.atan2(vec3d.z, vec3d.x)) - 90.0F;
      float pitch = (float)(-Math.toDegrees(Math.atan2(vec3d.y, diffXZ)));
      return new Vec2f(yaw, pitch);
   }

   private boolean fakeMove() {
      double[] offsets = new double[]{(double)1.0F, (double)0.0F, (double)-1.0F};
      ArrayList<BlockPosX> offList = new ArrayList();
      BlockPos playerPos = mc.player.getBlockPos();

      for(double x : offsets) {
         for(double z : offsets) {
            offList.add(new BlockPosX(mc.player.getX() + x, mc.player.getY(), mc.player.getZ() + z));
         }
      }

      for(BlockPos offPos : offList) {
         if (this.checkSelf(offPos) && !BlockUtil.canReplace(offPos) && (!this.headFill.getValue() || !BlockUtil.canReplace(offPos.up()))) {
            this.gotoPos(offPos);
            return true;
         }
      }

      ArrayList<BlockPos> pos = new ArrayList();

      for(BlockPos blockPos : offList) {
         if (!playerPos.equals(blockPos) && this.checkSelf(blockPos) && this.canMove(blockPos)) {
            pos.add(blockPos);
         }
      }

      if (!pos.isEmpty()) {
         double dis = (double)10.0F;
         BlockPos target = null;

         for(BlockPos p : pos) {
            double distance = mc.player.getPos().distanceTo(p.toCenterPos().add((double)0.0F, (double)-0.5F, (double)0.0F));
            if (distance < dis || target == null) {
               target = p;
               dis = distance;
            }
         }

         this.gotoPos(target);
         return true;
      } else {
         for(BlockPos blockPos : offList) {
            if (!playerPos.equals(blockPos) && this.checkSelf(blockPos)) {
               pos.add(blockPos);
            }
         }

         if (pos.isEmpty()) {
            if (!this.center.getValue()) {
               return false;
            } else {
               offList.clear();
               offList.add(new BlockPosX(mc.player.getX() + (double)1.0F, mc.player.getY(), mc.player.getZ()));
               offList.add(new BlockPosX(mc.player.getX() - (double)1.0F, mc.player.getY(), mc.player.getZ()));
               offList.add(new BlockPosX(mc.player.getX(), mc.player.getY(), mc.player.getZ() - (double)1.0F));
               offList.add(new BlockPosX(mc.player.getX(), mc.player.getY(), mc.player.getZ() + (double)1.0F));

               for(BlockPos blockPos : offList) {
                  if (this.canMove(blockPos)) {
                     this.gotoPos(blockPos);
                     return true;
                  }
               }

               if (!this.wait.getValue() && this.disable.getValue()) {
                  this.disable();
               }

               return false;
            }
         } else {
            double dis = (double)10.0F;
            BlockPos target = null;

            for(BlockPos p : pos) {
               double distance = mc.player.getPos().distanceTo(p.toCenterPos().add((double)0.0F, (double)-0.5F, (double)0.0F));
               if (distance < dis || target == null) {
                  target = p;
                  dis = distance;
               }
            }

            this.gotoPos(target);
            return true;
         }
      }
   }

   private void placeBlock(BlockPos pos, boolean rotate) {
      this.placeBlock(pos, rotate, true);
   }

   private void placeBlock(BlockPos pos, boolean rotate, boolean allowHelper) {
      if (this.canPlace(pos) && !this.placePos.contains(pos) && this.progress < this.blocksPer.getValueInt()) {
         BlockPos helperPos;
         if (allowHelper && this.helper.getValue() && !BlockUtil.allowAirPlace() && BlockUtil.getPlaceSide(pos) == null && this.canPlace(helperPos = pos.down()) && !this.placePos.contains(helperPos) && this.progress < this.blocksPer.getValueInt()) {
            this.placeBlock(helperPos, rotate, false);
         }

         if (this.placePos.contains(pos) || this.progress >= this.blocksPer.getValueInt()) {
            return;
         }

         this.placePos.add(pos);
         if (BlockUtil.allowAirPlace()) {
            ++this.progress;
            BlockUtil.placedPos.add(pos);
            if (this.sound.getValue()) {
               mc.world.playSound(mc.player, pos, SoundEvents.BLOCK_STONE_PLACE, SoundCategory.BLOCKS, 1.0F, 0.8F);
            }

            this.clickBlock(pos, Direction.DOWN, rotate, Hand.MAIN_HAND, this.packetPlace.getValue());
         }

         Direction side;
         if ((side = BlockUtil.getPlaceSide(pos)) == null) {
            return;
         }

         ++this.progress;
         BlockUtil.placedPos.add(pos);
         if (this.sound.getValue()) {
            mc.world.playSound(mc.player, pos, SoundEvents.BLOCK_STONE_PLACE, SoundCategory.BLOCKS, 1.0F, 0.8F);
         }

         this.clickBlock(pos.offset(side), side.getOpposite(), rotate, Hand.MAIN_HAND, this.packetPlace.getValue());
      }

   }

   public void clickBlock(BlockPos pos, Direction side, boolean rotate, Hand hand, boolean packet) {
      Vec3d directionVec = new Vec3d((double)pos.getX() + (double)0.5F + (double)side.getVector().getX() * (double)0.5F, (double)pos.getY() + (double)0.5F + (double)side.getVector().getY() * (double)0.5F, (double)pos.getZ() + (double)0.5F + (double)side.getVector().getZ() * (double)0.5F);
      if (rotate && this.rotate.getValue() != BlockLag.RotateMode.Yawstep) {
         float[] angle = RotationManager.getRotation(this.currentPos.add((double)0.0F, (double)mc.player.getEyeHeight(mc.player.getPose()), (double)0.0F), directionVec);
         Arc.ROTATION.snapAt(angle[0], angle[1]);
      }

      EntityUtil.swingHand(hand, AntiCheat.INSTANCE.interactSwing.getValue());
      BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
      if (packet) {
         Module.sendSequencedPacket((id) -> new PlayerInteractBlockC2SPacket(hand, result, id));
      } else {
         mc.interactionManager.interactBlock(mc.player, hand, result);
      }

      if (rotate && this.rotate.getValue() != BlockLag.RotateMode.Yawstep) {
         Arc.ROTATION.snapBack();
      }

   }

   private void applyYawStepRotation(Vec3d directionVec) {
      Vec3d eyesPos = this.currentPos != null ? this.currentPos.add((double)0.0F, (double)mc.player.getEyeHeight(mc.player.getPose()), (double)0.0F) : mc.player.getEyePos();
      float[] angle = RotationManager.getRotation(eyesPos, directionVec);
      float[] stepped = Arc.ROTATION.injectStep(angle, this.yawStepSteps.getValueFloat());
      Arc.ROTATION.snapAt(stepped[0], stepped[1]);
   }

   private void doSwap(int slot) {
      if (this.inventory.getValue()) {
         InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
      } else {
         InventoryUtil.switchToSlot(slot);
      }

   }

   private void gotoPos(BlockPos offPos) {
      double targetX = (double)offPos.getX() + (double)0.5F;
      double targetZ = (double)offPos.getZ() + (double)0.5F;
      double x = mc.player.getX();
      double z = mc.player.getZ();
      double y = mc.player.getY() + this.yOffset.getValue();
      double xDiff = Math.abs(x - targetX);
      double zDiff = Math.abs(z - targetZ);
      double moveDis = this.preCorrect.getValue();
      if (moveDis > (double)0.0F) {
         if (xDiff >= moveDis) {
            x = x > targetX ? x - moveDis : x + moveDis;
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false));
         }

         if (zDiff >= moveDis) {
            z = z > targetZ ? z - moveDis : z + moveDis;
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false));
         }
      }

      xDiff = Math.abs(x - targetX);
      zDiff = Math.abs(z - targetZ);
      moveDis = this.moveDis.getValue();
      if (moveDis > (double)0.0F) {
         while(xDiff > moveDis) {
            x = x > targetX ? x - moveDis : x + moveDis;
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false));
            xDiff = Math.abs(x - targetX);
         }

         while(zDiff > moveDis) {
            z = z > targetZ ? z - moveDis : z + moveDis;
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false));
            zDiff = Math.abs(z - targetZ);
         }
      }

      if ((moveDis = this.moveDis2.getValue()) > (double)0.0F) {
         while(xDiff > moveDis) {
            x = x > targetX ? x - moveDis : x + moveDis;
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false));
            xDiff = Math.abs(x - targetX);
         }

         while(zDiff > moveDis) {
            z = z > targetZ ? z - moveDis : z + moveDis;
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false));
            zDiff = Math.abs(z - targetZ);
         }
      }

      if ((moveDis = this.moveCorrect2.getValue()) > (double)0.0F) {
         if (xDiff >= moveDis) {
            x = x > targetX ? x - moveDis : x + moveDis;
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false));
         }

         if (zDiff >= moveDis) {
            z = z > targetZ ? z - moveDis : z + moveDis;
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false));
         }
      }

      this.currentPos = new Vec3d(x, y, z);
   }

   private boolean canMove(BlockPos pos) {
      return mc.world.isAir(pos) && mc.world.isAir(pos.up());
   }

   private boolean canPlace(BlockPos pos) {
      if (this.noSelfPos.getValue() && pos.equals(EntityUtil.getPlayerPos(true))) {
         return false;
      } else if (!BlockUtil.allowAirPlace() && BlockUtil.getPlaceSide(pos) == null) {
         return false;
      } else if (!BlockUtil.canReplace(pos)) {
         return false;
      } else if (this.detectMine.getValue() && Arc.BREAK.isMining(pos)) {
         return false;
      } else {
         return !this.hasEntity(pos);
      }
   }

   private boolean hasEntity(BlockPos pos) {
      for(Entity entity : BlockUtil.getEntities(new Box(pos))) {
         if (entity != mc.player && entity.isAlive() && !(entity instanceof ItemEntity) && !(entity instanceof ExperienceOrbEntity) && !(entity instanceof ExperienceBottleEntity) && !(entity instanceof ArrowEntity) && (!(entity instanceof EndCrystalEntity) || !this.breakCrystal.getValue())) {
            return true;
         }
      }

      return false;
   }

   private boolean checkSelf(BlockPos pos) {
      return mc.player.getBoundingBox().intersects(new Box(pos));
   }

   private boolean trapped(BlockPos pos) {
      return (BlockUtil.canCollide(mc.player, new Box(pos)) || BlockUtil.getBlock(pos) == Blocks.COBWEB) && this.checkSelf(pos.down(2));
   }

   private int getBlock() {
      if (this.inventory.getValue()) {
         int cryingObsidianSlot = InventoryUtil.findBlockInventorySlot(Blocks.CRYING_OBSIDIAN);
         if (cryingObsidianSlot != -1) {
            return cryingObsidianSlot;
         } else {
            int obsidianSlot = InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN);
            if (obsidianSlot != -1) {
               return obsidianSlot;
            } else {
               return this.enderChest.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.ENDER_CHEST) : -1;
            }
         }
      } else {
         int cryingObsidian = InventoryUtil.findBlock(Blocks.CRYING_OBSIDIAN);
         if (cryingObsidian != -1) {
            return cryingObsidian;
         } else {
            int obsidian = InventoryUtil.findBlock(Blocks.OBSIDIAN);
            if (obsidian != -1) {
               return obsidian;
            } else {
               return this.enderChest.getValue() ? InventoryUtil.findBlock(Blocks.ENDER_CHEST) : -1;
            }
         }
      }
   }

   public void onEnable() {
      this.shouldCenter = true;
      this.currentPos = null;
   }

   public void onDisable() {
      this.shouldCenter = true;
      this.currentPos = null;
   }

   public static enum RotateMode {
      None,
      Normal,
      Bypass,
      Grim,
      Yawstep;

      // $FF: synthetic method
      private static RotateMode[] $values() {
         return new RotateMode[]{None, Normal, Bypass, Grim, Yawstep};
      }
   }

   private static enum LagBackMode {
      Smart,
      Invalid,
      TrollHack,
      ToVoid,
      CuiCan,
      ToVoid2,
      Normal,
      Rotation,
      Fly,
      Glide;

      // $FF: synthetic method
      private static LagBackMode[] $values() {
         return new LagBackMode[]{Smart, Invalid, TrollHack, ToVoid, CuiCan, ToVoid2, Normal, Rotation, Fly, Glide};
      }
   }
}
