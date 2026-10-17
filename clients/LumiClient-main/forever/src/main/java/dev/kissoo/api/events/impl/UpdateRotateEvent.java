package dev.kissoo.api.events.impl;

import net.minecraft.util.math.MathHelper;

public class UpdateRotateEvent {
   private static final UpdateRotateEvent instance = new UpdateRotateEvent();
   private float yaw;
   private float pitch;
   private boolean modified;

   private UpdateRotateEvent() {
   }

   public static UpdateRotateEvent get(float yaw, float pitch) {
      instance.yaw = safeYaw(yaw, 0.0F);
      instance.pitch = safePitch(pitch, 0.0F);
      instance.modified = false;
      return instance;
   }

   public float getYaw() {
      return this.yaw;
   }

   public void setYaw(float yaw) {
      this.modified = true;
      this.setYawWithoutSync(yaw);
   }

   public float getPitch() {
      return this.pitch;
   }

   public void setPitch(float pitch) {
      this.modified = true;
      this.setPitchWithoutSync(pitch);
   }

   public boolean isModified() {
      return this.modified;
   }

   public void setRotation(float yaw, float pitch) {
      this.setYaw(yaw);
      this.setPitch(pitch);
   }

   public void setYawWithoutSync(float yaw) {
      this.yaw = safeYaw(yaw, this.yaw);
   }

   public void setPitchWithoutSync(float pitch) {
      this.pitch = safePitch(pitch, this.pitch);
   }

   private static float safeYaw(float yaw, float fallback) {
      return Float.isFinite(yaw) ? MathHelper.wrapDegrees(yaw) : fallback;
   }

   private static float safePitch(float pitch, float fallback) {
      return Float.isFinite(pitch) ? MathHelper.clamp(pitch, -90.0F, 90.0F) : fallback;
   }
}
