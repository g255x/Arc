package dev.baimaodaxia.api.utils.rotation;

import dev.baimaodaxia.Alien;
import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.ClientTickEvent;
import dev.baimaodaxia.api.events.impl.GameLeftEvent;
import dev.baimaodaxia.api.events.impl.KeyboardInputEvent;
import dev.baimaodaxia.api.events.impl.SendMovementPacketsEvent;
import dev.baimaodaxia.api.utils.move.MoveUtils;
import java.util.function.Function;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

public class RotationManager {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final Rotation offset = new Rotation(0.0F, 0.0F);
   public static Rotation rotations;
   public static Rotation lastRotations = new Rotation(0.0F, 0.0F);
   public static Rotation targetRotations;
   public static Rotation animationRotation;
   public static Rotation lastAnimationRotation;
   private static boolean active;
   private static boolean smoothed;
   private static double rotationSpeed;
   private static Function<Rotation, Boolean> raycast;
   private static float randomAngle;
   private static boolean subscribed;

   public static void init() {
      if (!subscribed) {
         Alien.EVENT_BUS.subscribe(new RotationManager());
         subscribed = true;
      }
   }

   public static boolean isActive() {
      return active;
   }

   public static void setActive(boolean active) {
      RotationManager.active = active;
   }

   public static boolean isSmoothed() {
      return smoothed;
   }

   public static void setRotations(Rotation rotations, double rotationSpeed) {
      setRotations(rotations, rotationSpeed, (Function)null);
   }

   public static void setRotations(Rotation rotations, double rotationSpeed, Function<Rotation, Boolean> raycast) {
      targetRotations = rotations;
      RotationManager.rotationSpeed = rotationSpeed;
      RotationManager.raycast = raycast;
      active = true;
      smoothed = false;
      smooth();
   }

   public static void smooth() {
      if (mc.player != null) {
         if (!smoothed) {
            float targetYaw = targetRotations.getYaw();
            float targetPitch = targetRotations.getPitch();
            if (raycast != null && (Math.abs(targetYaw - rotations.getYaw()) > 5.0F || Math.abs(targetPitch - rotations.getPitch()) > 5.0F)) {
               Rotation trueTargetRotations = new Rotation(targetRotations.getYaw(), targetRotations.getPitch());
               double speed = Math.random() * Math.random() * Math.random() * (double)20.0F;
               randomAngle += (float)(((double)20.0F + (double)((float)(Math.random() - (double)0.5F)) * Math.random() * Math.random() * Math.random() * (double)360.0F) * (double)(mc.player.age / 10 % 2 == 0 ? -1 : 1));
               offset.setYaw((float)((double)offset.getYaw() + (double)(-MathHelper.sin((float)Math.toRadians((double)randomAngle))) * speed));
               offset.setPitch((float)((double)offset.getPitch() + (double)MathHelper.cos((float)Math.toRadians((double)randomAngle)) * speed));
               targetYaw += offset.getYaw();
               targetPitch += offset.getPitch();
               if (!(Boolean)raycast.apply(new Rotation(targetYaw, targetPitch))) {
                  randomAngle = (float)Math.toDegrees(Math.atan2((double)(trueTargetRotations.getYaw() - targetYaw), (double)(targetPitch - trueTargetRotations.getPitch()))) - 180.0F;
                  targetYaw -= offset.getYaw();
                  targetPitch -= offset.getPitch();
                  offset.setYaw((float)((double)offset.getYaw() + (double)(-MathHelper.sin((float)Math.toRadians((double)randomAngle))) * speed));
                  offset.setPitch((float)((double)offset.getPitch() + (double)MathHelper.cos((float)Math.toRadians((double)randomAngle)) * speed));
                  targetYaw += offset.getYaw();
                  targetPitch += offset.getPitch();
               }

               if (!(Boolean)raycast.apply(new Rotation(targetYaw, targetPitch))) {
                  offset.setYaw(0.0F);
                  offset.setPitch(0.0F);
                  targetYaw = (float)((double)targetRotations.getYaw() + Math.random() * (double)2.0F);
                  targetPitch = (float)((double)targetRotations.getPitch() + Math.random() * (double)2.0F);
               }
            }

            rotations = RotationUtils.smooth(new Rotation(targetYaw, targetPitch), rotationSpeed + Math.random());
         }

         smoothed = true;
      }
   }

   public static Rotation getRotation() {
      if (mc.player == null) {
         return new Rotation(0.0F, 0.0F);
      } else {
         return active && rotations != null ? rotations : new Rotation(mc.player.getYaw(), mc.player.getPitch());
      }
   }

   @EventListener
   public void onLogout(GameLeftEvent e) {
      rotations = null;
      lastRotations = null;
      targetRotations = null;
      animationRotation = null;
      lastAnimationRotation = null;
      active = false;
      smoothed = false;
   }

   @EventListener(
      priority = 4
   )
   public void onClientTick(ClientTickEvent e) {
      if (e.isPre()) {
         if (mc.player != null) {
            if (!active || rotations == null || lastRotations == null || targetRotations == null) {
               rotations = lastRotations = targetRotations = new Rotation(mc.player.getYaw(), mc.player.getPitch());
            }

            if (active) {
               smooth();
            }

         }
      }
   }

   @EventListener(
      priority = 4
   )
   public void onSendMovementPackets(SendMovementPacketsEvent e) {
      if (mc.player != null) {
         if (active && rotations != null) {
            float yaw = rotations.getYaw();
            float pitch = rotations.getPitch();
            if (!Float.isNaN(yaw) && !Float.isNaN(pitch)) {
               e.setYaw(yaw);
               e.setPitch(pitch);
            }

            if (Math.abs((rotations.getYaw() - mc.player.getYaw()) % 360.0F) < 1.0F && Math.abs(rotations.getPitch() - mc.player.getPitch()) < 1.0F) {
               active = false;
               this.correctDisabledRotations();
            }

            lastRotations = rotations;
         } else {
            lastRotations = new Rotation(mc.player.getYaw(), mc.player.getPitch());
         }

         lastAnimationRotation = animationRotation;
         animationRotation = new Rotation(e.getYaw(), e.getPitch());
         targetRotations = new Rotation(mc.player.getYaw(), mc.player.getPitch());
         smoothed = false;
      }
   }

   @EventListener(
      priority = 100
   )
   public void onKeyboardInput(KeyboardInputEvent event) {
      if (mc.player != null) {
         if (active && rotations != null) {
            MoveUtils.fixMovement(rotations.getYaw());
         }

      }
   }

   private void correctDisabledRotations() {
      Rotation rotations = new Rotation(mc.player.getYaw(), mc.player.getPitch());
      Rotation fixedRotations = RotationUtils.resetRotation(RotationUtils.applySensitivityPatch(rotations, lastRotations));
      mc.player.setYaw(fixedRotations.getYaw());
      mc.player.setPitch(fixedRotations.getPitch());
   }
}
