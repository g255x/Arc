package dev.perish.api.events.impl;

import net.minecraft.util.math.Vec3d;

public class RotationEvent {
    private static final RotationEvent instance = new RotationEvent();
    public float priority;
    private Vec3d target;
    private float yaw;
    private float pitch;
    private boolean rotation;
    private boolean exclusive;
    private float speed;

    private RotationEvent() {
    }

    public static RotationEvent get() {
        instance.priority = Float.NEGATIVE_INFINITY;
        instance.target = null;
        instance.yaw = 0.0F;
        instance.pitch = 0.0F;
        instance.rotation = false;
        instance.exclusive = false;
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
        this.setTarget(target, speed, priority, false);
    }

    public void setTargetExclusive(Vec3d target, float speed) {
        this.setTarget(target, speed, Float.POSITIVE_INFINITY, true);
    }

    public void setRotation(float yaw, float pitch, float speed, float priority) {
        this.setRotation(yaw, pitch, speed, priority, false);
    }

    public void setRotationExclusive(float yaw, float pitch, float speed) {
        this.setRotation(yaw, pitch, speed, Float.POSITIVE_INFINITY, true);
    }

    private void setTarget(Vec3d target, float speed, float priority, boolean exclusive) {
        if (this.shouldAccept(priority, exclusive)) {
            this.rotation = false;
            this.priority = priority;
            this.target = target;
            this.speed = speed;
            this.exclusive = exclusive;
        }
    }

    private void setRotation(float yaw, float pitch, float speed, float priority, boolean exclusive) {
        if (this.shouldAccept(priority, exclusive)) {
            this.rotation = true;
            this.priority = priority;
            this.yaw = yaw;
            this.pitch = pitch;
            this.speed = speed;
            this.exclusive = exclusive;
        }
    }

    private boolean shouldAccept(float priority, boolean exclusive) {
        if (this.exclusive) {
            return false;
        }
        if (exclusive) {
            return true;
        }
        return priority > this.priority;
    }
}
