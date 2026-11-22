package dev.g255.arc.api.events.impl;

import net.minecraft.util.math.Vec3d;

public class RotationEvent {
   private static final RotationEvent instance = new RotationEvent();
   public float priority;
   private Vec3d target;
   private float yaw;
   private float pitch;
   private boolean rotation;
   private float speed;

   private RotationEvent() {
   }

   public static RotationEvent get() {
      instance.priority = 0.0F;
      instance.target = null;
      instance.yaw = 0.0F;
      instance.pitch = 0.0F;
      instance.rotation = false;
      instance.speed = 0.0F;
      return instance;
   }

   public Vec3d getTarget() {
      return this.target;
   }

   public float getSpeed() {
      return this.speed;
   }

   public boolean getRotation() {
      return this.rotation;
   }

   public float getYaw() {
      return this.yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public void setTarget(Vec3d target, float speed, float priority) {
      // 目标坐标或速度含 NaN 时直接忽略，避免把 NaN 带进旋转插值（最终写进玩家朝向）
      if (target == null || !Double.isFinite(target.x) || !Double.isFinite(target.y) || !Double.isFinite(target.z)
            || !Float.isFinite(speed)) {
         return;
      }

      if (priority >= this.priority) {
         this.rotation = false;
         this.priority = priority;
         this.target = target;
         this.speed = speed;
      }

   }

   public void setRotation(float yaw, float pitch, float speed, float priority) {
      if (!Float.isFinite(yaw) || !Float.isFinite(pitch) || !Float.isFinite(speed)) {
         return;
      }

      if (priority >= this.priority) {
         this.rotation = true;
         this.priority = priority;
         this.yaw = yaw;
         this.pitch = pitch;
         this.speed = speed;
      }

   }
}
