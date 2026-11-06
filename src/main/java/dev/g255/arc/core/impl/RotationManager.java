package dev.g255.arc.core.impl;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.DoAttackEvent;
import dev.g255.arc.api.events.impl.FireworkShooterRotationEvent;
import dev.g255.arc.api.events.impl.InteractBlockEvent;
import dev.g255.arc.api.events.impl.InteractItemEvent;
import dev.g255.arc.api.events.impl.JumpEvent;
import dev.g255.arc.api.events.impl.KeyboardInputEvent;
import dev.g255.arc.api.events.impl.PacketEvent;
import dev.g255.arc.api.events.impl.RotationEvent;
import dev.g255.arc.api.events.impl.SendMovementPacketsEvent;
import dev.g255.arc.api.events.impl.TickEvent;
import dev.g255.arc.api.events.impl.TickMovementEvent;
import dev.g255.arc.api.events.impl.TravelEvent;
import dev.g255.arc.api.events.impl.UpdateRotateEvent;
import dev.g255.arc.api.utils.Wrapper;
import dev.g255.arc.api.utils.math.MathUtil;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.api.utils.path.BaritoneUtil;
import dev.g255.arc.asm.accessors.IClientPlayerEntity;
import dev.g255.arc.mod.modules.impl.client.AntiCheat;
import dev.g255.arc.mod.modules.impl.client.ClientSetting;
import dev.g255.arc.mod.modules.impl.movement.HoleSnap;
import dev.g255.arc.mod.modules.impl.player.Freecam;
import dev.g255.arc.mod.modules.settings.enums.SnapBack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;

public class RotationManager implements Wrapper {
   public static final Timer ROTATE_TIMER = new Timer();
   public static Vec3d directionVec = null;
   public static boolean snapBack = false;
   private static float renderPitch;
   private static float renderYawOffset;
   private static float prevRenderPitch;
   private static float prevRenderYawOffset;
   private static float prevRotationYawHead;
   private static float rotationYawHead;
   public float nextYaw;
   public float nextPitch;
   public float rotationYaw;
   public float rotationPitch;
   public float lastYaw;
   public float lastPitch;
   public Vec3d crossHairUpdatePos;
   private int ticksExisted;
   public static float fixYaw;
   public static float fixPitch;
   private float prevYaw;
   private float prevPitch;

   public RotationManager() {
      Arc.EVENT_BUS.subscribe(this);
   }

   @EventListener
   public void onInteract(InteractItemEvent event) {
      if (AntiCheat.INSTANCE.interactRotation.getValue() && mc.player != null) {
         if (event.isPre()) {
            this.snapAt(mc.player.getYaw(), mc.player.getPitch());
         } else {
            this.snapBack();
         }
      }

   }

   @EventListener
   public void onInteract(InteractBlockEvent event) {
      if (AntiCheat.INSTANCE.interactRotation.getValue() && mc.player != null) {
         if (event.isPre()) {
            this.snapAt(mc.player.getYaw(), mc.player.getPitch());
         } else {
            this.snapBack();
         }
      }

   }

   @EventListener
   public void doAttack(DoAttackEvent event) {
      if (AntiCheat.INSTANCE.interactRotation.getValue() && mc.player != null) {
         if (event.isPre()) {
            this.snapAt(mc.player.getYaw(), mc.player.getPitch());
         } else {
            this.snapBack();
         }
      }

   }

   public void snapBack() {
      if (AntiCheat.INSTANCE.snapBackEnum.is(SnapBack.Force)) {
         mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), this.rotationYaw, this.rotationPitch, mc.player.isOnGround()));
      } else if (AntiCheat.INSTANCE.snapBackEnum.is(SnapBack.Tick)) {
         snapBack = true;
      }

   }

   public void lookAt(Vec3d directionVec) {
      this.rotationTo(directionVec);
      this.snapAt(directionVec);
   }

   public void lookAt(BlockPos pos, Direction side) {
      Vec3d hitVec = pos.toCenterPos().add(new Vec3d((double)side.getVector().getX() * (double)0.5F, (double)side.getVector().getY() * (double)0.5F, (double)side.getVector().getZ() * (double)0.5F));
      this.lookAt(hitVec);
   }

   public void snapAt(float yaw, float pitch) {
      this.setRenderRotation(yaw, pitch, true);
      if (AntiCheat.INSTANCE.grimRotation.getValue()) {
         mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.isOnGround()));
      } else {
         mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, mc.player.isOnGround()));
      }

   }

   public void snapAt(Vec3d directionVec) {
      float[] angle = getRotation(directionVec);
      this.snapAt(angle[0], angle[1]);
   }

   public void rotationTo(Vec3d vec3d) {
      ROTATE_TIMER.reset();
      directionVec = vec3d;
   }

   public boolean inFov(Vec3d directionVec, float fov) {
      float[] angle = getRotation(this.crossHairUpdatePos != null ? this.crossHairUpdatePos : new Vec3d(mc.player.getX(), mc.player.getY() + (double)mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ()), directionVec);
      return this.inFov(angle[0], angle[1], fov);
   }

   public boolean inFov(float yaw, float pitch, float fov) {
      float yawDifferent = MathHelper.angleBetween(yaw, this.rotationYaw);
      float pitchDifferent;
      return yawDifferent * yawDifferent + (pitchDifferent = Math.abs(pitch - this.rotationPitch)) * pitchDifferent <= fov * fov;
   }

   @EventListener
   public void onTickMovement(TickMovementEvent event) {
      if (mc.player != null) {
         this.crossHairUpdatePos = new Vec3d(mc.player.getX(), mc.player.getY() + (double)mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
      }
   }

   @EventListener
   public void update(SendMovementPacketsEvent event) {
      if (AntiCheat.INSTANCE.movementSync() && !BaritoneUtil.isActive()) {
         event.setYaw(this.nextYaw);
         event.setPitch(this.nextPitch);
      } else {
         UpdateRotateEvent updateRotateEvent = UpdateRotateEvent.get(event.getYaw(), event.getPitch());
         Arc.EVENT_BUS.post(updateRotateEvent);
         event.setYaw(updateRotateEvent.getYaw());
         event.setPitch(updateRotateEvent.getPitch());
      }

   }

   @EventListener(
      priority = 999
   )
   public void update(TickMovementEvent event) {
      if (mc.player != null) {
         if (AntiCheat.INSTANCE.movementSync() && !BaritoneUtil.isActive()) {
            UpdateRotateEvent updateRotateEvent = UpdateRotateEvent.get(mc.player.getYaw(), mc.player.getPitch());
            Arc.EVENT_BUS.post(updateRotateEvent);
            this.nextYaw = updateRotateEvent.getYaw();
            this.nextPitch = updateRotateEvent.getPitch();
            fixYaw = this.nextYaw;
            fixPitch = this.nextPitch;
         }

      }
   }

   @EventListener(
      priority = -200
   )
   public void onLastRotation(UpdateRotateEvent event) {
      RotationEvent rotationEvent = RotationEvent.get();
      Arc.EVENT_BUS.post(rotationEvent);
      if (rotationEvent.getRotation()) {
         float[] newAngle = this.injectStep(new float[]{rotationEvent.getYaw(), rotationEvent.getPitch()}, rotationEvent.getSpeed());
         event.setYaw(newAngle[0]);
         event.setPitch(newAngle[1]);
      } else if (rotationEvent.getTarget() != null) {
         float[] newAngle = this.injectStep(rotationEvent.getTarget(), rotationEvent.getSpeed());
         event.setYaw(newAngle[0]);
         event.setPitch(newAngle[1]);
      } else if (!event.isModified() && AntiCheat.INSTANCE.look.getValue() && directionVec != null && !ROTATE_TIMER.passed((long)(AntiCheat.INSTANCE.rotateTime.getValue() * (double)1000.0F))) {
         float[] newAngle = this.injectStep(directionVec, AntiCheat.INSTANCE.steps.getValueFloat());
         event.setYaw(newAngle[0]);
         event.setPitch(newAngle[1]);
      }

   }

   @EventListener
   public void travel(TravelEvent e) {
      if (AntiCheat.INSTANCE.movementSync()) {
         if (!BaritoneUtil.isActive()) {
            if (!mc.player.isRiding()) {
               if (e.isPre()) {
                  this.prevYaw = mc.player.getYaw();
                  this.prevPitch = mc.player.getPitch();
                  mc.player.setYaw(fixYaw);
                  mc.player.setPitch(fixPitch);
               } else {
                  mc.player.setYaw(this.prevYaw);
                  mc.player.setPitch(this.prevPitch);
               }

            }
         }
      }
   }

   @EventListener
   public void onJump(JumpEvent e) {
      if (AntiCheat.INSTANCE.movementSync()) {
         if (!BaritoneUtil.isActive()) {
            if (!mc.player.isRiding()) {
               if (e.isPre()) {
                  this.prevYaw = mc.player.getYaw();
                  this.prevPitch = mc.player.getPitch();
                  mc.player.setYaw(fixYaw);
                  mc.player.setPitch(fixPitch);
               } else {
                  mc.player.setYaw(this.prevYaw);
                  mc.player.setPitch(this.prevPitch);
               }

            }
         }
      }
   }

   @EventListener
   public void onFirework(FireworkShooterRotationEvent event) {
      if (AntiCheat.INSTANCE.movementSync()) {
         if (!BaritoneUtil.isActive()) {
            if (event.shooter == mc.player) {
               event.yaw = fixYaw;
               event.pitch = fixPitch;
               event.cancel();
            }

         }
      }
   }

   @EventListener(
      priority = -999
   )
   public void onKeyInput(KeyboardInputEvent e) {
      if (AntiCheat.INSTANCE.movementSync()) {
         if (!BaritoneUtil.isActive()) {
            if (!HoleSnap.INSTANCE.isOn()) {
               if (!mc.player.isRiding() && !Freecam.INSTANCE.isOn()) {
                  float mF = mc.player.input.movementForward;
                  float mS = mc.player.input.movementSideways;
                  float delta = (mc.player.getYaw() - fixYaw) * ((float)Math.PI / 180F);
                  float cos = MathHelper.cos(delta);
                  float sin = MathHelper.sin(delta);
                  mc.player.input.movementSideways = (float)Math.round(mS * cos - mF * sin);
                  mc.player.input.movementForward = (float)Math.round(mF * cos + mS * sin);
               }
            }
         }
      }
   }

   public float[] injectStep(Vec3d vec, float steps) {
      float currentYaw = AntiCheat.INSTANCE.serverSide.getValue() ? this.getLastYaw() : this.rotationYaw;
      float currentPitch = AntiCheat.INSTANCE.serverSide.getValue() ? this.getLastPitch() : this.rotationPitch;
      float yawDelta = MathHelper.wrapDegrees((float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vec.z - mc.player.getZ(), vec.x - mc.player.getX())) - (double)90.0F) - currentYaw);
      float pitchDelta = (float)(-Math.toDegrees(Math.atan2(vec.y - (mc.player.getPos().y + (double)mc.player.getEyeHeight(mc.player.getPose())), Math.sqrt(Math.pow(vec.x - mc.player.getX(), (double)2.0F) + Math.pow(vec.z - mc.player.getZ(), (double)2.0F))))) - currentPitch;
      if (AntiCheat.INSTANCE.random.getValue()) {
         float angleToRad = (float)Math.toRadians((double)(27 * (mc.player.age % 30)));
         yawDelta = (float)((double)yawDelta + Math.sin((double)angleToRad) * (double)3.0F) + MathUtil.random(-1.0F, 1.0F);
         pitchDelta += MathUtil.random(-0.6F, 0.6F);
      }

      if (yawDelta > 180.0F) {
         yawDelta -= 180.0F;
      }

      float yawStepVal = 180.0F * steps;
      float clampedYawDelta = MathHelper.clamp(MathHelper.abs(yawDelta), -yawStepVal, yawStepVal);
      float clampedPitchDelta = MathHelper.clamp(pitchDelta, -45.0F, 45.0F);
      float newYaw = currentYaw + (yawDelta > 0.0F ? clampedYawDelta : -clampedYawDelta);
      float newPitch = MathHelper.clamp(currentPitch + clampedPitchDelta, -90.0F, 90.0F);
      return new float[]{newYaw, newPitch};
   }

   public float[] injectStep(float[] angle, float steps) {
      float currentYaw = AntiCheat.INSTANCE.serverSide.getValue() ? this.getLastYaw() : this.rotationYaw;
      float currentPitch = AntiCheat.INSTANCE.serverSide.getValue() ? this.getLastPitch() : this.rotationPitch;
      float yawDelta = MathHelper.wrapDegrees(angle[0] - currentYaw);
      float pitchDelta = angle[1] - currentPitch;
      if (AntiCheat.INSTANCE.random.getValue()) {
         float angleToRad = (float)Math.toRadians((double)(27 * (mc.player.age % 30)));
         yawDelta = (float)((double)yawDelta + Math.sin((double)angleToRad) * (double)3.0F) + MathUtil.random(-1.0F, 1.0F);
         pitchDelta += MathUtil.random(-0.6F, 0.6F);
      }

      if (yawDelta > 180.0F) {
         yawDelta -= 180.0F;
      }

      float yawStepVal = 180.0F * steps;
      float pitchStepVal = 90.0F * steps;
      float clampedYawDelta = MathHelper.clamp(MathHelper.abs(yawDelta), -yawStepVal, yawStepVal);
      float clampedPitchDelta = MathHelper.clamp(pitchDelta, -pitchStepVal, pitchStepVal);
      float newYaw = currentYaw + (yawDelta > 0.0F ? clampedYawDelta : -clampedYawDelta);
      float newPitch = MathHelper.clamp(currentPitch + clampedPitchDelta, -90.0F, 90.0F);
      return new float[]{newYaw, newPitch};
   }

   @EventListener(
      priority = -999
   )
   public void onPacketSend(PacketEvent.Sent event) {
      if (mc.player != null) {
         Packet<?> packet2 = event.getPacket();
         PlayerMoveC2SPacket packet;
         if (packet2 instanceof PlayerMoveC2SPacket && (packet = (PlayerMoveC2SPacket)packet2).changesLook()) {
            this.setLastYaw(packet.getYaw(this.getLastYaw()));
            this.setLastPitch(packet.getPitch(this.getLastPitch()));
            this.setRenderRotation(this.getLastYaw(), this.getLastPitch(), ClientSetting.INSTANCE.sync.getValue());
         }

      }
   }

   @EventListener
   public void onUpdateWalkingPost(TickEvent event) {
      if (event.isPost()) {
         this.setRenderRotation(this.getLastYaw(), this.getLastPitch(), false);
      }

   }

   public void setRenderRotation(float yaw, float pitch, boolean force) {
      if (mc.player != null) {
         if (mc.player.age != this.ticksExisted || force) {
            this.ticksExisted = mc.player.age;
            prevRenderPitch = renderPitch;
            prevRenderYawOffset = renderYawOffset;
            renderYawOffset = this.getRenderYawOffset(yaw, prevRenderYawOffset);
            prevRotationYawHead = rotationYawHead;
            rotationYawHead = yaw;
            renderPitch = pitch;
         }
      }
   }

   private float getRenderYawOffset(float yaw, float offsetIn) {
      float result = offsetIn;
      double xDif = mc.player.getX() - mc.player.prevX;
      double zDif;
      if (xDif * xDif + (zDif = mc.player.getZ() - mc.player.prevZ) * zDif > (double)0.0025000002F) {
         float offset = (float)MathHelper.atan2(zDif, xDif) * (180F / (float)Math.PI) - 90.0F;
         float wrap = MathHelper.abs(MathHelper.wrapDegrees(yaw) - offset);
         result = 95.0F < wrap && wrap < 265.0F ? offset - 180.0F : offset;
      }

      if (mc.player.handSwingProgress > 0.0F) {
         result = yaw;
      }

      float offset;
      if ((offset = MathHelper.wrapDegrees(yaw - (offsetIn + MathHelper.wrapDegrees(result - offsetIn) * 0.3F))) < -75.0F) {
         offset = -75.0F;
      } else if (offset >= 75.0F) {
         offset = 75.0F;
      }

      result = yaw - offset;
      if (offset * offset > 2500.0F) {
         result += offset * 0.2F;
      }

      return result;
   }

   public static float[] getRotation(Vec3d eyesPos, Vec3d vec) {
      double diffX = vec.x - eyesPos.x;
      double diffY = vec.y - eyesPos.y;
      double diffZ = vec.z - eyesPos.z;
      double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
      float yaw = (float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
      float pitch = (float)(-Math.toDegrees(Math.atan2(diffY, diffXZ)));
      return new float[]{MathHelper.wrapDegrees(yaw), MathHelper.wrapDegrees(pitch)};
   }

   public static float[] getRotation(Vec3d vec) {
      Vec3d eyesPos = mc.player.getEyePos();
      return getRotation(eyesPos, vec);
   }

   public static float getRenderPitch() {
      return renderPitch;
   }

   public static float getRotationYawHead() {
      return rotationYawHead;
   }

   public static float getRenderYawOffset() {
      return renderYawOffset;
   }

   public static float getPrevRenderPitch() {
      return prevRenderPitch;
   }

   public static float getPrevRotationYawHead() {
      return prevRotationYawHead;
   }

   public static float getPrevRenderYawOffset() {
      return prevRenderYawOffset;
   }

   public float getLastYaw() {
      return this.lastYaw;
   }

   public void setLastYaw(float lastYaw) {
      this.lastYaw = lastYaw;
      if (AntiCheat.INSTANCE.forceSync.getValue() && Arc.SERVER.playerNull.passedS(0.15)) {
         ((IClientPlayerEntity)mc.player).setLastYaw(lastYaw);
      }

   }

   public float getLastPitch() {
      return this.lastPitch;
   }

   public void setLastPitch(float lastPitch) {
      this.lastPitch = lastPitch;
      if (AntiCheat.INSTANCE.forceSync.getValue() && Arc.SERVER.playerNull.passedS(0.15)) {
         ((IClientPlayerEntity)mc.player).setLastPitch(lastPitch);
      }

   }
}
