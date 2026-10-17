package dev.kissoo.mod.modules.impl.combat;

import dev.kissoo.Forever;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.ClientTickEvent;
import dev.kissoo.api.events.impl.RotationEvent;
import dev.kissoo.api.utils.player.EntityUtil;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.impl.exploit.BowSpam;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * 连弩自瞄（CrossbowAim）
 * 复用 Follower（自动追踪）的索敌逻辑，外加弩箭弹道预判：
 *   - 重力抛物线（重力 0.05 方块/tick²，空气阻力 0.99/tick）
 *   - 敌人速度提前量（lead 滑块控制）
 * 仅在「BowSpam（自动弓弩连发）开启 且 手持弩」时自瞄目标。
 */
public class CrossbowAim extends Module {
    public static CrossbowAim INSTANCE;
    private Entity target;

    // 弩箭弹道参数（CrossbowItem 发射速度 3.15）
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
        super("CrossbowAim", Module.Category.Combat);
        this.setChinese("连弩自瞄");
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
            // 保持当前目标
        } else {
            this.target = this.findTarget();
        }
        if (this.target == null) return;

        float[] rot = this.computeRotations(this.target);
        if (rot == null) return;
        // 真转准星/视角（可见硬锁，同 Follower 的 onTick）
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
    // === 索敌逻辑（同 Follower）===============
    // ==========================================

    private Entity findTarget() {
        Entity bestTarget = null;
        for (Entity entity : Forever.THREAD.getEntities()) {
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
        if (Forever.FRIEND.isFriend(player)) return false;
        if (!entity.isAlive()) return false;
        return mc.player.getEyePos().distanceTo(entity.getPos()) <= this.range.getValue();
    }

    private boolean isTargetStillValid(Entity entity) {
        if (entity == null || !entity.isAlive() || entity.isRemoved()) return false;
        if (!(entity instanceof PlayerEntity player)) return false;
        if (Forever.FRIEND.isFriend(player)) return false;
        return mc.player.getEyePos().distanceTo(entity.getPos()) <= this.range.getValue();
    }

    // ==========================================
    // === 直瞄（无预判）=======================
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
    // === 弹道预判（重力 + 移动目标提前量）======
    // ==========================================

    private float[] getPredictedRotations(Entity target) {
        Vec3d start = mc.player.getEyePos();
        Box box = target.getBoundingBox();
        // 瞄准点：目标包围盒中心（XZ 中心，Y 取身高一半）
        Vec3d targetPos = new Vec3d(
                (box.minX + box.maxX) / 2.0,
                box.minY + target.getHeight() * 0.5,
                (box.minZ + box.maxZ) / 2.0
        );
        // 目标速度（按 Lead 滑块缩放提前量）
        Vec3d targetVel = target.getVelocity().multiply(this.lead.getValue() / 100.0);

        // 头顶正上方（水平距离≈0）：atan2(0,0) 退化，直接竖直瞄准
        double dx0 = targetPos.x - start.x;
        double dz0 = targetPos.z - start.z;
        if (dx0 * dx0 + dz0 * dz0 < 1e-6) {
            return new float[]{mc.player.getYaw(), (float) (targetPos.y > start.y ? -90.0 : 90.0)};
        }

        // 初始飞行时间估计
        double t = start.distanceTo(targetPos) / ARROW_SPEED;
        double yaw = 0.0;
        double pitch = 0.0;

        // 定点迭代：预测位置 -> 求俯仰角 -> 重算飞行时间
        for (int iter = 0; iter < 4; iter++) {
            Vec3d predicted = targetPos.add(targetVel.multiply(t));
            double dx = predicted.x - start.x;
            double dz = predicted.z - start.z;
            if (dx * dx + dz * dz < 1e-6) {
                return new float[]{mc.player.getYaw(), (float) (predicted.y > start.y ? -90.0 : 90.0)};
            }

            yaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;

            // 三分搜索俯仰角，最小化箭头轨迹与移动目标的最近距离（扩大到 ±90°，覆盖正上/正下）
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

    // 模拟弩箭飞行，返回 {最近距离²（与移动目标轨迹）, 最近时刻(tick)}
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
