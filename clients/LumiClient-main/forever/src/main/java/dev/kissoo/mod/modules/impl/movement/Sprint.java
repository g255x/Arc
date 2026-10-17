package dev.kissoo.mod.modules.impl.movement;

import dev.kissoo.Forever;
import dev.kissoo.api.events.impl.MoveEvent;
import dev.kissoo.api.events.impl.MovedEvent;
import dev.kissoo.api.events.impl.PacketEvent;
import dev.kissoo.api.events.impl.SprintEvent;
import dev.kissoo.api.events.impl.TickEvent;
import dev.kissoo.api.events.impl.TickMovementEvent;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.events.impl.UpdateRotateEvent;
import dev.kissoo.api.utils.path.BaritoneUtil;
import dev.kissoo.api.utils.player.EntityUtil;
import dev.kissoo.api.utils.player.MovementUtil;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.impl.client.AntiCheat;
import dev.kissoo.mod.modules.impl.player.Freecam;
import dev.kissoo.mod.modules.impl.player.TimerModule;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import hoprc.api.events.eventbus.EventListener;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.MathHelper;

import java.util.Objects;

public class Sprint extends Module {
   public static Sprint INSTANCE;
   public final EnumSetting<Mode> mode = this.add(new EnumSetting<>("Mode", Mode.Legit));
   public final BooleanSetting inWaterPause = this.add(new BooleanSetting("InWaterPause", true));
   public final BooleanSetting inWebPause = this.add(new BooleanSetting("InWebPause", true));
   public final BooleanSetting sneakingPause = this.add(new BooleanSetting("SneakingPause", false));
   public final BooleanSetting blindnessPause = this.add(new BooleanSetting("BlindnessPause", false));
   public final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", false));
   public final BooleanSetting lagPause = this.add(new BooleanSetting("LagPause", true));

   public final SliderSetting strafeSpeed = this.add(
           new SliderSetting("StrafeSpeed", 0.2873, 0.1, 1.0, 0.01,
                   () -> mode.getValue() == Mode.Instance));
   public final BooleanSetting speedEffect = this.add(
           new BooleanSetting("SpeedEffect", true, () -> mode.getValue() == Mode.Instance));

   public final BooleanSetting entityBoost = this.add(
           new BooleanSetting("EntityBoost", false, () -> mode.getValue() == Mode.Instance));
   public final SliderSetting perBoostDelay = this.add(
           new SliderSetting("PerBoostDelay", 500.0, 0.0, 5000.0, 50.0,
                   () -> mode.getValue() == Mode.Instance && entityBoost.getValue()));

   public final SliderSetting rampUpTicks = this.add(
           new SliderSetting("RampUpTicks", 5, 1, 20, 1,
                   () -> mode.getValue() == Mode.Instance && entityBoost.getValue()));
   public final SliderSetting peakMultiplier = this.add(
           new SliderSetting("PeakMultiplier", 2.5, 0.1, 3.0, 0.05,
                   () -> mode.getValue() == Mode.Instance && entityBoost.getValue()));

   public final SliderSetting decayFriction = this.add(
           new SliderSetting("DecayFriction", 0.945, 0.85, 1.0, 0.005,
                   () -> mode.getValue() == Mode.Instance && entityBoost.getValue()));

   public final SliderSetting cutoffThreshold = this.add(
           new SliderSetting("CutoffThreshold", 0.1, 0.001, 0.1, 0.001,
                   () -> mode.getValue() == Mode.Instance && entityBoost.getValue()));

   public final SliderSetting maxBoostSpeed = this.add(
           new SliderSetting("MaxBoostSpeed", 5.0, 0.5, 5.0, 0.1,
                   () -> mode.getValue() == Mode.Instance && entityBoost.getValue()));

   boolean pause = false;
   private enum BoostPhase {
      IDLE,
      RAMP_UP,
      DECAY
   }

   private BoostPhase boostPhase = BoostPhase.IDLE;
   private double boostPeakSpeed = 0.0;
   private double currentBoostSpeed = 0.0;
   private int rampUpTickCounter = 0;
   private int rampUpTickTarget = 0;
   private double rampUpIncrement = 0.0;
   private double lastDistance = 0.0;
   private long lastBoostTime = 0L;

   public Sprint() {
      super("Sprint", "Permanently keeps player in sprinting mode.", Module.Category.Movement);
      this.setChinese("强制疾跑");
      INSTANCE = this;
   }

   @Override
   public void onDisable() {
      resetBoostState();
   }

   private void resetBoostState() {
      boostPhase = BoostPhase.IDLE;
      boostPeakSpeed = 0.0;
      currentBoostSpeed = 0.0;
      rampUpTickCounter = 0;
      rampUpTickTarget = 0;
      rampUpIncrement = 0.0;
      lastDistance = 0.0;
   }

   @EventListener
   public void onPacket(PacketEvent.Receive event) {
      if (mc.player == null || mc.world == null) return;

      if (this.lagPause.getValue() && event.getPacket() instanceof PlayerPositionLookS2CPacket) {
         this.pause = true;
         resetBoostState();
      }

      if (this.mode.getValue() != Mode.Instance || !this.entityBoost.getValue()) return;

      if (event.getPacket() instanceof ExplosionS2CPacket packet) {
         onExplosion(packet);
      }
      if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket packet) {
         if (packet.getEntityId() == mc.player.getId()) {
            onVelocity(packet);
         }
      }
   }

   private void onExplosion(ExplosionS2CPacket packet) {
      if (System.currentTimeMillis() - lastBoostTime < perBoostDelay.getValue()) return;

      double vx = packet.getPlayerVelocityX();
      double vz = packet.getPlayerVelocityZ();
      double incomingSpeed = Math.hypot(vx, vz);

      if (incomingSpeed < 0.01) return;
      double newPeak = currentBoostSpeed + incomingSpeed * peakMultiplier.getValue();
      newPeak = Math.min(newPeak, maxBoostSpeed.getValue());
      lastBoostTime = System.currentTimeMillis();
      startRampUp(newPeak);
   }

   private void onVelocity(EntityVelocityUpdateS2CPacket packet) {
      if (System.currentTimeMillis() - lastBoostTime < perBoostDelay.getValue()) return;

      double vx = packet.getVelocityX() / 8000.0;
      double vz = packet.getVelocityZ() / 8000.0;
      double incomingSpeed = Math.hypot(vx, vz);

      if (incomingSpeed < 0.1) return;

      double newPeak = currentBoostSpeed + incomingSpeed * peakMultiplier.getValue();
      newPeak = Math.min(newPeak, maxBoostSpeed.getValue());
      lastBoostTime = System.currentTimeMillis();
      startRampUp(newPeak);
   }

   private void startRampUp(double targetPeak) {
      this.boostPeakSpeed = targetPeak;
      this.rampUpTickTarget = rampUpTicks.getValueInt();
      this.rampUpTickCounter = 0;

      double speedDifference = this.boostPeakSpeed - this.currentBoostSpeed;
      if (speedDifference <= 0) {
         this.currentBoostSpeed = this.boostPeakSpeed;
         this.boostPhase = BoostPhase.DECAY;
         return;
      }

      this.rampUpIncrement = speedDifference / this.rampUpTickTarget;
      this.boostPhase = BoostPhase.RAMP_UP;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (mc.player == null || mc.world == null) return;
      if (this.mode.getValue() != Mode.Instance) return;
      if (!this.entityBoost.getValue()) return;
      if (boostPhase == BoostPhase.IDLE) return;

      switch (boostPhase) {
         case RAMP_UP -> {
            rampUpTickCounter++;
            currentBoostSpeed += rampUpIncrement;
            if (rampUpTickCounter >= rampUpTickTarget) {
               currentBoostSpeed = boostPeakSpeed;
               boostPhase = BoostPhase.DECAY;
            }
         }
         case DECAY -> {
            currentBoostSpeed *= decayFriction.getValue();
            if (lastDistance > 0) {
               double baseSpeed = getInstanceBaseMoveSpeed();
               double expectedTotal = baseSpeed + currentBoostSpeed;
               if (lastDistance < expectedTotal * 0.8) {
                  currentBoostSpeed *= 0.8;
               }
            }
            if (currentBoostSpeed < cutoffThreshold.getValue()) {
               resetBoostState();
            }
         }
      }
   }

   @EventListener
   public void onMoved(MovedEvent event) {
      if (mc.player == null) return;
      if (this.mode.getValue() != Mode.Instance) return;

      double dx = mc.player.getX() - mc.player.prevX;
      double dz = mc.player.getZ() - mc.player.prevZ;
      this.lastDistance = Math.sqrt(dx * dx + dz * dz);
   }

   @EventListener
   public void onStrafe(MoveEvent event) {
      if (this.mode.getValue() != Mode.Instance) return;
      if (mc.player == null || mc.world == null) return;
      if (BaritoneUtil.isActive()) return;
      if (BlockStrafe.INSTANCE != null
              && BlockStrafe.INSTANCE.isOn()
              && EntityUtil.isInsideBlock()) {
         return;
      }

      if (mc.player.isSneaking()
              || Fly.INSTANCE.isOn()
              || HoleSnap.INSTANCE.isOn()
              || Speed.INSTANCE.isOn()
              || mc.player.isFallFlying()
              || mc.player.isInLava()
              || mc.player.isTouchingWater()
              || mc.player.getAbilities().flying) {
         return;
      }

      if (!MovementUtil.isMoving()) {
         MovementUtil.setMotionX(0.0);
         MovementUtil.setMotionZ(0.0);
         if (boostPhase != BoostPhase.IDLE) {
            currentBoostSpeed *= 0.5;
            if (currentBoostSpeed < cutoffThreshold.getValue()) {
               resetBoostState();
            }
         }
         return;
      }

      double baseSpeed = getInstanceBaseMoveSpeed();

      if (Speed.INSTANCE != null && Speed.INSTANCE.isOn()) {
         double currentSpeed = Math.hypot(MovementUtil.getMotionX(), MovementUtil.getMotionZ());
         baseSpeed = Math.max(baseSpeed, currentSpeed);
      }

      double finalSpeed = baseSpeed;
      if (boostPhase != BoostPhase.IDLE && entityBoost.getValue()) {
         finalSpeed = baseSpeed + currentBoostSpeed;
         finalSpeed = Math.min(finalSpeed, baseSpeed + maxBoostSpeed.getValue());
      }

      double[] dir = MovementUtil.directionSpeed(finalSpeed);
      event.setX(dir[0]);
      event.setZ(dir[1]);
   }

   private double getInstanceBaseMoveSpeed() {
      double n = strafeSpeed.getValue();
      if (speedEffect.getValue()
              && mc.player.hasStatusEffect(StatusEffects.SPEED)
              && !mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
         n *= 1.0 + 0.2 * (Objects.requireNonNull(
                 mc.player.getStatusEffect(StatusEffects.SPEED)).getAmplifier() + 1);
      }
      return n;
   }


   public static float getSprintYaw(float yaw) {
      if (mc.options.forwardKey.isPressed() && !mc.options.backKey.isPressed()) {
         if (mc.options.leftKey.isPressed() && !mc.options.rightKey.isPressed()) {
            yaw -= 45.0F;
         } else if (mc.options.rightKey.isPressed() && !mc.options.leftKey.isPressed()) {
            yaw += 45.0F;
         }
      } else if (mc.options.backKey.isPressed() && !mc.options.forwardKey.isPressed()) {
         yaw += 180.0F;
         if (mc.options.leftKey.isPressed() && !mc.options.rightKey.isPressed()) {
            yaw += 45.0F;
         } else if (mc.options.rightKey.isPressed() && !mc.options.leftKey.isPressed()) {
            yaw -= 45.0F;
         }
      } else if (mc.options.leftKey.isPressed() && !mc.options.rightKey.isPressed()) {
         yaw -= 90.0F;
      } else if (mc.options.rightKey.isPressed() && !mc.options.leftKey.isPressed()) {
         yaw += 90.0F;
      }
      return MathHelper.wrapDegrees(yaw);
   }

   @Override
   public String getInfo() {
      return mode.getValue().name();
   }

   @EventListener
   public void onMove(TickMovementEvent event) {
      if (mc.player == null || mc.world == null) return;
      if (BaritoneUtil.isPathing()) return;
      if (this.inWater()) return;

      if (this.mode.getValue() == Mode.Instance) {
         if (MovementUtil.isMoving() && !mc.player.isSneaking()) {
            mc.player.setSprinting(true);
         }
      } else if (this.mode.getValue() == Mode.PressKey) {
         mc.options.sprintKey.setPressed(true);
      } else {
         mc.player.setSprinting(this.shouldSprint());
      }
   }

   @EventListener
   public void tick(TickEvent event) {
      if (event.isPost()) {
         this.pause = false;
      }
   }

   @EventListener
   public void sprint(SprintEvent event) {
      if (mc.player == null || mc.world == null) return;
      if (BaritoneUtil.isPathing()) return;

      if (this.mode.getValue() == Mode.Instance) {
         if (!this.inWater() && MovementUtil.isMoving() && !mc.player.isSneaking()) {
            event.cancel();
            event.setSprint(true);
         }
         return;
      }

      if (!this.mode.is(Mode.PressKey)) {
         if (!this.inWater()) {
            event.cancel();
            event.setSprint(this.shouldSprint());
         }
      }
   }

   @EventListener(priority = -100)
   public void rotate(UpdateRotateEvent event) {
      if (mc.player == null || mc.world == null) return;
      if (BaritoneUtil.isPathing()) return;

      if ((mc.player.getHungerManager().getFoodLevel() > 6 || mc.player.isCreative())
              && MovementUtil.isMoving()
              && !Freecam.INSTANCE.isOn()
              && !mc.player.isFallFlying()
              && (!Forever.PLAYER.isInWeb(mc.player) || !this.inWebPause.getValue())
              && (!mc.player.isSneaking() || !this.sneakingPause.getValue())
              && !mc.player.isRiding()
              && (!mc.player.isUsingItem() || !this.usingPause.getValue())
              && !mc.player.isInFluid()
              && (!mc.player.hasStatusEffect(StatusEffects.BLINDNESS) || !this.blindnessPause.getValue())
              && this.mode.is(Mode.Rotation)
              && !event.isModified()) {
         event.setYaw(getSprintYaw(mc.player.getYaw()));
      }
   }

   public boolean inWater() {
      return this.inWaterPause.getValue() && mc.player.isInFluid();
   }

   private boolean shouldSprint() {
      if ((mc.player.getHungerManager().getFoodLevel() > 6 || mc.player.isCreative())
              && MovementUtil.isMoving()
              && !this.pause
              && (!mc.player.isSneaking() || !this.sneakingPause.getValue())
              && (!Forever.PLAYER.isInWeb(mc.player) || !this.inWebPause.getValue())
              && (!mc.player.isUsingItem() || !this.usingPause.getValue())
              && !mc.player.isRiding()
              && (!mc.player.hasStatusEffect(StatusEffects.BLINDNESS) || !this.blindnessPause.getValue())) {
         switch (this.mode.getValue()) {
            case Legit:
               if (AntiCheat.INSTANCE.movementSync()) {
                  return mc.player.input.movementForward > 0.0F;
               }
               return HoleSnap.INSTANCE.isOn()
                       || mc.options.forwardKey.isPressed()
                       && MathHelper.angleBetween(mc.player.getYaw(), Forever.ROTATION.rotationYaw) < 40.0F;
            case Rage:
               return true;
            case Rotation:
               if (AntiCheat.INSTANCE.movementSync()) {
                  return mc.player.input.movementForward > 0.0F;
               }
               return HoleSnap.INSTANCE.isOn()
                       || MathHelper.angleBetween(getSprintYaw(mc.player.getYaw()), Forever.ROTATION.rotationYaw) < 40.0F;
         }
      }
      return false;
   }

   public static enum Mode {
      PressKey,
      Legit,
      Rage,
      Rotation,
      Instance;
   }
}