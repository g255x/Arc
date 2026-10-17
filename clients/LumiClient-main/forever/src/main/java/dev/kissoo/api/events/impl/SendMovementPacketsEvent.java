package dev.kissoo.api.events.impl;

import net.minecraft.util.math.MathHelper;

public class SendMovementPacketsEvent {
   public static final SendMovementPacketsEvent instance = new SendMovementPacketsEvent();
   private float yaw;
   private float pitch;

   private SendMovementPacketsEvent() {
   }

   public static SendMovementPacketsEvent get(float yaw, float pitch) {
      instance.yaw = safeYaw(yaw, 0.0F);
      instance.pitch = safePitch(pitch, 0.0F);
      return instance;
   }

   public float getYaw() {
      return this.yaw;
   }

   public void setYaw(float yaw) {
      this.yaw = safeYaw(yaw, this.yaw);
   }

   public float getPitch() {
      return this.pitch;
   }

   public void setPitch(float pitch) {
      this.pitch = safePitch(pitch, this.pitch);
   }

   private static float safeYaw(float yaw, float fallback) {
      return Float.isFinite(yaw) ? MathHelper.wrapDegrees(yaw) : fallback;
   }

   private static float safePitch(float pitch, float fallback) {
      return Float.isFinite(pitch) ? MathHelper.clamp(pitch, -90.0F, 90.0F) : fallback;
   }
}
