package dev.g255.arc.mod.modules.impl.movement;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.MoveEvent;
import dev.g255.arc.api.events.impl.RotationEvent;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.api.utils.math.AnimateUtil;
import dev.g255.arc.api.utils.math.MathUtil;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.api.utils.player.InventoryUtil;
import dev.g255.arc.api.utils.player.MovementUtil;
import dev.g255.arc.api.utils.render.ColorUtil;
import dev.g255.arc.api.utils.render.Render3DUtil;
import dev.g255.arc.api.utils.world.BlockUtil;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.ColorSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.Objects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;

public class Scaffold extends Module {
   private static Vec3d lastVec3d;
   public final SliderSetting rotateTime = this.add(new SliderSetting("KeepRotate", (double)1000.0F, (double)0.0F, (double)3000.0F, (double)10.0F));
   private final BooleanSetting tower = this.add(new BooleanSetting("Tower", true));
   private final BooleanSetting packetPlace = this.add(new BooleanSetting("PacketPlace", false));
   private final BooleanSetting safeWalk = this.add(new BooleanSetting("SafeWalk", false));
   private final BooleanSetting rotate = this.add((new BooleanSetting("Rotate", true)).setParent());
   private final BooleanSetting yawStep = this.add((new BooleanSetting("YawStep", false, () -> this.rotate.isOpen() && this.rotate.isOpen())).setParent());
   private final BooleanSetting whenElytra = this.add(new BooleanSetting("FallFlying", true, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.rotate.isOpen()));
   private final SliderSetting steps = this.add(new SliderSetting("Steps", 0.05, (double)0.0F, (double)1.0F, 0.01, () -> this.rotate.isOpen() && this.yawStep.isOpen()));
   private final BooleanSetting checkFov = this.add((new BooleanSetting("OnlyLooking", true, () -> this.rotate.isOpen() && this.yawStep.isOpen())).setParent());
   private final SliderSetting fov = this.add(new SliderSetting("Fov", (double)20.0F, (double)0.0F, (double)360.0F, 0.1, () -> this.checkFov.isOpen() && this.rotate.isOpen() && this.yawStep.isOpen()));
   private final SliderSetting priority = this.add(new SliderSetting("Priority", 10, 0, 100, () -> this.rotate.isOpen() && this.yawStep.isOpen()));
   private final BooleanSetting render = this.add((new BooleanSetting("Render", true)).setParent());
   public final ColorSetting color;
   public final ColorSetting outlineColor;
   public final SliderSetting sliderSpeed;
   private final BooleanSetting esp;
   private final BooleanSetting fill;
   private final BooleanSetting outline;
   private final Timer timer;
   private final Timer towerTimer;
   private Vec3d vec;
   private BlockPos pos;

   public Scaffold() {
      super("Scaffold", Module.Category.Movement);
      Color var10005 = new Color(255, 255, 255, 100);
      BooleanSetting var10006 = this.render;
      Objects.requireNonNull(var10006);
      this.color = this.add(new ColorSetting("Color", var10005, var10006::isOpen));
      var10005 = new Color(255, 255, 255, 100);
      var10006 = this.render;
      Objects.requireNonNull(var10006);
      this.outlineColor = this.add(new ColorSetting("OutlineColor", var10005, var10006::isOpen));
      BooleanSetting var10009 = this.render;
      Objects.requireNonNull(var10009);
      this.sliderSpeed = this.add(new SliderSetting("SliderSpeed", 0.2, 0.01, (double)1.0F, 0.01, var10009::isOpen));
      var10006 = this.render;
      Objects.requireNonNull(var10006);
      this.esp = this.add(new BooleanSetting("ESP", true, var10006::isOpen));
      var10006 = this.render;
      Objects.requireNonNull(var10006);
      this.fill = this.add(new BooleanSetting("Fill", true, var10006::isOpen));
      var10006 = this.render;
      Objects.requireNonNull(var10006);
      this.outline = this.add(new BooleanSetting("Box", true, var10006::isOpen));
      this.timer = new Timer();
      this.towerTimer = new Timer();
      this.setChinese("自动搭路");
   }

   @EventListener(
      priority = -100
   )
   public void onMove(MoveEvent event) {
      if (this.safeWalk.getValue()) {
         SafeWalk.INSTANCE.onMove(event);
      }
   }

   @EventListener
   public void onRotation(RotationEvent event) {
      if (this.rotate.getValue() && !this.timer.passed((long)this.rotateTime.getValueInt()) && this.vec != null) {
         event.setTarget(this.vec, this.steps.getValueFloat(), this.priority.getValueFloat());
      }

   }

   public void onEnable() {
      lastVec3d = null;
      this.pos = null;
   }

   public void onRender3D(MatrixStack matrixStack) {
      if (this.render.getValue()) {
         if (this.esp.getValue()) {
            GL11.glEnable(3042);
            double temp = 0.01;

            for(double i = (double)0.0F; i < 0.8; i += temp) {
               HoleSnap.drawCircle(matrixStack, ColorUtil.injectAlpha(this.color.getValue(), (int)Math.min((double)(this.color.getValue().getAlpha() * 2) / (0.8 / temp), (double)255.0F)), i, new Vec3d(MathUtil.interpolate(mc.player.lastRenderX, mc.player.getX(), (double)mc.getRenderTickCounter().getTickDelta(true)), MathUtil.interpolate(mc.player.lastRenderY, mc.player.getY(), (double)mc.getRenderTickCounter().getTickDelta(true)), MathUtil.interpolate(mc.player.lastRenderZ, mc.player.getZ(), (double)mc.getRenderTickCounter().getTickDelta(true))), 5);
            }

            GL11.glDisable(3042);
         }

         if (this.pos != null) {
            Vec3d cur = this.pos.toCenterPos();
            lastVec3d = lastVec3d == null ? cur : new Vec3d(AnimateUtil.animate(lastVec3d.getX(), cur.x, this.sliderSpeed.getValue()), AnimateUtil.animate(lastVec3d.getY(), cur.y, this.sliderSpeed.getValue()), AnimateUtil.animate(lastVec3d.getZ(), cur.z, this.sliderSpeed.getValue()));
            Render3DUtil.draw3DBox(matrixStack, new Box(lastVec3d.add((double)0.5F, (double)0.5F, (double)0.5F), lastVec3d.add((double)-0.5F, (double)-0.5F, (double)-0.5F)), ColorUtil.injectAlpha(this.color.getValue(), this.color.getValue().getAlpha()), ColorUtil.injectAlpha(this.outlineColor.getValue(), this.outlineColor.getValue().getAlpha()), this.outline.getValue(), this.fill.getValue());
         }
      }

   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      int block = InventoryUtil.findBlock();
      if (block != -1) {
         BlockPos placePos = mc.player.getBlockPos().down();
         if (BlockUtil.clientCanPlace(placePos, false)) {
            int old = mc.player.getInventory().selectedSlot;
            if (BlockUtil.getPlaceSide(placePos) == null) {
               double getDistance = (double)1000.0F;
               BlockPos bestPos = null;

               for(Direction i : Direction.values()) {
                  if (i != Direction.UP && BlockUtil.canPlace(placePos.offset(i)) && (bestPos == null || mc.player.squaredDistanceTo(placePos.offset(i).toCenterPos()) < getDistance)) {
                     bestPos = placePos.offset(i);
                     getDistance = mc.player.squaredDistanceTo(placePos.offset(i).toCenterPos());
                  }
               }

               if (bestPos == null) {
                  return;
               }

               placePos = bestPos;
            }

            if (this.rotate.getValue()) {
               Direction side = BlockUtil.getPlaceSide(placePos);
               this.vec = placePos.offset(side).toCenterPos().add((double)side.getOpposite().getVector().getX() * (double)0.5F, (double)side.getOpposite().getVector().getY() * (double)0.5F, (double)side.getOpposite().getVector().getZ() * (double)0.5F);
               this.timer.reset();
               if (!this.faceVector(this.vec)) {
                  return;
               }
            }

            InventoryUtil.switchToSlot(block);
            BlockUtil.placeBlock(placePos, false, this.packetPlace.getValue());
            InventoryUtil.switchToSlot(old);
            if (this.rotate.getValue()) {
               Arc.ROTATION.snapBack();
            }

            this.pos = placePos;
            if (this.tower.getValue() && mc.options.jumpKey.isPressed() && !MovementUtil.isMoving()) {
               MovementUtil.setMotionY(0.42);
               MovementUtil.setMotionX((double)0.0F);
               MovementUtil.setMotionZ((double)0.0F);
               if (this.towerTimer.passed(1500L)) {
                  MovementUtil.setMotionY(-0.28);
                  this.towerTimer.reset();
               }
            } else {
               this.towerTimer.reset();
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

   private boolean faceVector(Vec3d directionVec) {
      if (!this.shouldYawStep()) {
         Arc.ROTATION.lookAt(directionVec);
         return true;
      } else if (Arc.ROTATION.inFov(directionVec, this.fov.getValueFloat())) {
         return true;
      } else {
         return !this.checkFov.getValue();
      }
   }
}
