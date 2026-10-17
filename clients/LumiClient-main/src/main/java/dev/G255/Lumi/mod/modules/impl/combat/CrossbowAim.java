package dev.G255.Lumi.mod.modules.impl.combat;

import dev.G255.Lumi.Lumi;
import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.ClientTickEvent;
import dev.G255.Lumi.api.events.impl.RotationEvent;
import dev.G255.Lumi.api.utils.player.EntityUtil;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.impl.exploit.BowSpam;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.EnumSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * è¿å¼©èªçï¼CrossbowAimï¼?
 * å¤ç¨ Followerï¼èªå¨è¿½è¸ªï¼çç´¢æé»è¾ï¼å¤å å¼©ç®­å¼¹éé¢å¤ï¼
 *   - éåæç©çº¿ï¼éå 0.05 æ¹å/tickÂ²ï¼ç©ºæ°é»å?0.99/tickï¼?
 *   - æäººéåº¦æåéï¼lead æ»åæ§å¶ï¼?
 * ä»å¨ãBowSpamï¼èªå¨å¼å¼©è¿åï¼å¼å?ä¸?ææå¼©ãæ¶èªçç®æ ã?
 */
public class CrossbowAim extends Module {
    public static CrossbowAim INSTANCE;
    private Entity target;

    // å¼©ç®­å¼¹éåæ°ï¼CrossbowItem åå°éåº¦ 3.15ï¼?
    private static final double ARROW_SPEED = 3.15;
    private static final double GRAVITY = 0.05;
    private static final double DRAG = 0.99;
    private static final int MAX_TICKS = 120;

    private final EnumSetting<Mode> mode = this.add(new EnumSetting<>("Mode", Mode.LowestDistance));
    private final SliderSetting range = this.add(new SliderSetting("Range", 50.0, 5.0, 100.0));
    private final BooleanSetting deadLock = this.add(new BooleanSetting("DeadLock", true));
    private final BooleanSetting predict = this.add(new BooleanSetting("Predict", true));
    private final SliderSetting lead = this.add(new SliderSetting("Lead", 100.0, 0.0, 100.0, 1.0));
    private final SliderSetting rotationSpeed = this.add(new SliderSetting("RotationSpeed", 180.0, 10.0, 360.0));
    private final SliderSetting priority = this.add(new SliderSetting("Priority", 5.0, 0.0, 10.0));

    public CrossbowAim() {
        super("CrossbowAim", "Automatically aims crossbow at best target for max damage.", Module.Category.Combat);
        this.setChinese("");
        this.setChineseDesc("");
        INSTANCE = this;
    }

    @Override
    public String getInfo() {
        return this.target == null ? null : this.target.getName().getString();
    }

    @Override
    public void onDisable() {
        this.target = null;
    }

    private boolean isHoldingCrossbow() {
        return mc.player.getMainHandStack().getItem() instanceof CrossbowItem
                || mc.player.getOffHandStack().getItem() instanceof CrossbowItem;
    }

    private boolean shouldAim() {
        return BowSpam.INSTANCE != null && BowSpam.INSTANCE.isOn() && isHoldingCrossbow();
    }

    @EventListener
    public void onTick(ClientTickEvent event) {
        if (event.isPost() || nullCheck()) return;
        if (!shouldAim()) {
            this.target = null;
            return;
        }
        if (this.deadLock.getValue() && this.target != null && this.isTargetStillValid(this.target)) {
            // ä¿æå½åç®æ 
        } else {
            this.target = this.findTarget();
        }
        if (this.target == null) return;

        float[] rot = this.computeRotations(this.target);
        if (rot == null) return;
        // çè½¬åæ/è§è§ï¼å¯è§ç¡¬éï¼å?Follower ç?onTickï¼?
        mc.player.setYaw(rot[0]);
        mc.player.setPitch(rot[1]);
    }

    @EventListener
    public void onRotate(RotationEvent event) {
        if (nullCheck() || this.target == null) return;
        if (!shouldAim()) return;

        float[] rot = this.computeRotations(this.target);
        if (rot == null) return;

        event.setRotation(rot[0], rot[1], this.rotationSpeed.getValueFloat(), this.priority.getValueFloat());
    }

    private float[] computeRotations(Entity target) {
        if (this.predict.getValue()) {
            return this.getPredictedRotations(target);
        }
        return this.getRotationsToVec(this.getHitVec(target));
    }

    // ==========================================
    // === ç´¢æé»è¾ï¼å Followerï¼?==============
    // ==========================================

    private Entity findTarget() {
        Entity bestTarget = null;
        for (Entity entity : Lumi.THREAD.getEntities()) {
            if (!this.isValidTarget(entity)) continue;
            if (bestTarget == null) {
                bestTarget = entity;
                continue;
            }
            double distance = mc.player.getEyePos().distanceTo(entity.getPos());
            float health = EntityUtil.getHealth(entity);
            switch (this.mode.getValue().ordinal()) {
                case 0: { // LowestDistance
                    double bestDistance = mc.player.getEyePos().distanceTo(bestTarget.getPos());
                    if (distance < bestDistance) bestTarget = entity;
                    break;
                }
                case 1: { // LowestHealth
                    float bestHealth = EntityUtil.getHealth(bestTarget);
                    if (health < bestHealth) {
                        bestTarget = entity;
                        break;
                    }
                    if (health == bestHealth && distance < mc.player.getEyePos().distanceTo(bestTarget.getPos())) {
                        bestTarget = entity;
                    }
                }
            }
        }
        return bestTarget;
    }

    private boolean isValidTarget(Entity entity) {
        if (!(entity instanceof PlayerEntity player)) return false;
        if (player == mc.player) return false;
        if (Lumi.FRIEND.isFriend(player)) return false;
        if (!entity.isAlive()) return false;
        return mc.player.getEyePos().distanceTo(entity.getPos()) <= this.range.getValue();
    }

    private boolean isTargetStillValid(Entity entity) {
        if (entity == null || !entity.isAlive() || entity.isRemoved()) return false;
        if (!(entity instanceof PlayerEntity player)) return false;
        if (Lumi.FRIEND.isFriend(player)) return false;
        return mc.player.getEyePos().distanceTo(entity.getPos()) <= this.range.getValue();
    }

    // ==========================================
    // === ç´çï¼æ é¢å¤ï¼?======================
    // ==========================================

    private Vec3d getHitVec(Entity entity) {
        Vec3d eyePos = mc.player.getEyePos();
        Box box = entity.getBoundingBox();
        double heightOffset = entity instanceof PlayerEntity
                ? entity.getHeight() * 0.5
                : mc.player.getEyeHeight(mc.player.getPose()) * 0.5;
        double targetY = box.minY + heightOffset;
        double closestX = Math.max(box.minX, Math.min(eyePos.x, box.maxX));
        double closestZ = Math.max(box.minZ, Math.min(eyePos.z, box.maxZ));
        return new Vec3d(closestX, targetY, closestZ);
    }

    private float[] getRotationsToVec(Vec3d vec) {
        Vec3d eyes = mc.player.getEyePos();
        double diffX = vec.x - eyes.x;
        double diffY = vec.y - eyes.y;
        double diffZ = vec.z - eyes.z;
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0f;
        float pitch = (float) (-Math.toDegrees(Math.atan2(diffY, dist)));
        return new float[]{yaw, pitch};
    }

    // ==========================================
    // === å¼¹éé¢å¤ï¼éå?+ ç§»å¨ç®æ æåéï¼======
    // ==========================================

    private float[] getPredictedRotations(Entity target) {
        Vec3d start = mc.player.getEyePos();
        Box box = target.getBoundingBox();
        // çåç¹ï¼ç®æ åå´çä¸­å¿ï¼XZ ä¸­å¿ï¼Y åèº«é«ä¸åï¼
        Vec3d targetPos = new Vec3d(
                (box.minX + box.maxX) / 2.0,
                box.minY + target.getHeight() * 0.5,
                (box.minZ + box.maxZ) / 2.0
        );
        // ç®æ éåº¦ï¼æ Lead æ»åç¼©æ¾æåéï¼
        Vec3d targetVel = target.getVelocity().multiply(this.lead.getValue() / 100.0);

        // å¤´é¡¶æ­£ä¸æ¹ï¼æ°´å¹³è·ç¦»â?ï¼ï¼atan2(0,0) éåï¼ç´æ¥ç«ç´çå
        double dx0 = targetPos.x - start.x;
        double dz0 = targetPos.z - start.z;
        if (dx0 * dx0 + dz0 * dz0 < 1e-6) {
            return new float[]{mc.player.getYaw(), (float) (targetPos.y > start.y ? -90.0 : 90.0)};
        }

        // åå§é£è¡æ¶é´ä¼°è®¡
        double t = start.distanceTo(targetPos) / ARROW_SPEED;
        double yaw = 0.0;
        double pitch = 0.0;

        // å®ç¹è¿­ä»£ï¼é¢æµä½ç½?-> æ±ä¿¯ä»°è§ -> éç®é£è¡æ¶é´
        for (int iter = 0; iter < 4; iter++) {
            Vec3d predicted = targetPos.add(targetVel.multiply(t));
            double dx = predicted.x - start.x;
            double dz = predicted.z - start.z;
            if (dx * dx + dz * dz < 1e-6) {
                return new float[]{mc.player.getYaw(), (float) (predicted.y > start.y ? -90.0 : 90.0)};
            }

            yaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;

            // ä¸åæç´¢ä¿¯ä»°è§ï¼æå°åç®­å¤´è½¨è¿¹ä¸ç§»å¨ç®æ çæè¿è·ç¦»ï¼æ©å¤§å?Â±90Â°ï¼è¦çæ­£ä¸?æ­£ä¸ï¼?
            double lo = -90.0, hi = 90.0;
            for (int b = 0; b < 30; b++) {
                double m1 = lo + (hi - lo) / 3.0;
                double m2 = hi - (hi - lo) / 3.0;
                if (this.simulate(start, yaw, m1, targetPos, targetVel)[0] < this.simulate(start, yaw, m2, targetPos, targetVel)[0]) {
                    hi = m2;
                } else {
                    lo = m1;
                }
            }
            pitch = (lo + hi) / 2.0;
            t = this.simulate(start, yaw, pitch, targetPos, targetVel)[1];
        }

        return new float[]{(float) yaw, (float) pitch};
    }

    // æ¨¡æå¼©ç®­é£è¡ï¼è¿å?{æè¿è·ç¦»Â²ï¼ä¸ç§»å¨ç®æ è½¨è¿¹ï¼, æè¿æ¶å?tick)}
    private double[] simulate(Vec3d start, double yawDeg, double pitchDeg, Vec3d targetPos, Vec3d targetVel) {
        double yawR = Math.toRadians(yawDeg);
        double pitchR = Math.toRadians(pitchDeg);
        double vx = -Math.sin(yawR) * Math.cos(pitchR) * ARROW_SPEED;
        double vy = -Math.sin(pitchR) * ARROW_SPEED;
        double vz = Math.cos(yawR) * Math.cos(pitchR) * ARROW_SPEED;

        double px = start.x, py = start.y, pz = start.z;
        double bestDist = Double.MAX_VALUE;
        double bestTick = 0.0;

        for (int tick = 0; tick < MAX_TICKS; tick++) {
            px += vx; py += vy; pz += vz;
            vx *= DRAG; vy *= DRAG; vz *= DRAG;
            vy -= GRAVITY;

            double tx = targetPos.x + targetVel.x * tick;
            double ty = targetPos.y + targetVel.y * tick;
            double tz = targetPos.z + targetVel.z * tick;

            double dx = px - tx, dy = py - ty, dz = pz - tz;
            double dist = dx * dx + dy * dy + dz * dz;
            if (dist < bestDist) {
                bestDist = dist;
                bestTick = tick;
            }
        }
        return new double[]{bestDist, bestTick};
    }

    public enum Mode {
        LowestDistance,
        LowestHealth
    }
}
