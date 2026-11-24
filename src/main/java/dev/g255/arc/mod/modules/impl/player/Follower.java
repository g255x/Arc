package dev.g255.arc.mod.modules.impl.player;

import dev.g255.arc.Arc;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.impl.movement.FakeFlight;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.ClientTickEvent;
import dev.g255.arc.api.events.impl.UpdateRotateEvent;

import java.util.ArrayList;
import java.util.List;

public class Follower extends Module {
    public static Follower INSTANCE;

    public final EnumSetting<FollowerMode> mode = this.add(new EnumSetting<>("Mode", FollowerMode.Normal));
    public final SliderSetting fov = this.add(new SliderSetting("Fov", 60.0, 1.0, 180.0, 1.0));
    public final SliderSetting predict = this.add(new SliderSetting("Predict", 6.0, 0.0, 10.0, 1.0));
    private final BooleanSetting groundDisable = this.add(new BooleanSetting("GroundDisable", true));
    public final SliderSetting fallDistance = this.add(new SliderSetting("FallDistance", 10.0, 0.0, 20.0, 1.0, () -> this.mode.is(FollowerMode.Mace)));
    private final BooleanSetting lockView = this.add(new BooleanSetting("Aim", false));
    private final BooleanSetting checkGround = this.add(new BooleanSetting("CheckGround", true));
    private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true));
    private final BooleanSetting sendMsg = this.add(new BooleanSetting("SendMSG", true));

    public PlayerEntity target = null;
    public boolean canFollow = false;
    public boolean waitMace = true;
    public boolean firstFollower = true;

    private float targetYaw = 0.0F;
    private float targetPitch = 0.0F;
    private float prevYaw = 0.0F;
    private float prevPitch = 0.0F;

    public Follower() {
        super("Follower", Module.Category.Player);
        this.setChinese("追踪者");
        INSTANCE = this;
    }

    @Override
    public void onDisable() {
        if (!nullCheck()) {
            this.target = null;
            this.canFollow = false;
        }
    }

    @EventListener
    public void onTick(ClientTickEvent event) {
        if (nullCheck()) return;

        // Post: 还原视角（travel 和 sendMovementPackets 已在 Pre 和 Post 之间执行完毕）
        if (event.isPost()) {
            if (this.canFollow) {
                mc.player.setYaw(prevYaw);
                mc.player.setPitch(prevPitch);
            }
            return;
        }

        if (this.groundDisable.getValue() && mc.player.isOnGround()) {
            this.disable();
            return;
        }

        switch (this.mode.getValue()) {
            case Normal -> {
                this.canFollow = false;
                if (FakeFlight.INSTANCE.isOff() && this.autoDisable.getValue()) {
                    if (this.sendMsg.getValue()) sendMessage("§cFakeFly was disable");
                    this.disable();
                    return;
                }
                if (mc.player.isOnGround() && this.checkGround.getValue()) {
                    if (this.sendMsg.getValue()) sendMessage("§cOnGround disable");
                    this.disable();
                    return;
                }
                if (this.target == null) {
                    this.target = findPlayerInFov((int) this.fov.getValue());
                }
                if (this.target == null) return;
                if (!this.target.isAlive()) {
                    this.target = findPlayerInFov((int) this.fov.getValue());
                }
                if (this.target == null) return;

                Vec3d predictPos = predictTargetPos(this.target, (int) this.predict.getValue());
                float[] rots = getRotation(mc.player.getEyePos(), predictPos.add(0, this.target.getEyeY() - this.target.getY(), 0));
                targetYaw = rots[0];
                targetPitch = rots[1];
                this.canFollow = true;
                prevYaw = mc.player.getYaw();
                prevPitch = mc.player.getPitch();
                mc.player.setYaw(targetYaw);
                mc.player.setPitch(getFollowPitch(targetPitch));
            }
            case Mace -> {
                this.canFollow = false;
                if (FakeFlight.INSTANCE.isOff() && this.autoDisable.getValue()) {
                    if (this.sendMsg.getValue()) sendMessage("§cFakeFly was disable");
                    this.disable();
                    return;
                }
                if (mc.player.isOnGround() && this.checkGround.getValue()
                        && FakeFlight.INSTANCE.fly && FakeFlight.INSTANCE.isOn()) {
                    if (this.sendMsg.getValue()) sendMessage("§cOnGround");
                    this.waitMace = true;
                }
                if (this.target == null) {
                    if (this.firstFollower) {
                        this.target = findPlayerInFov((int) this.fov.getValue());
                    } else {
                        this.target = findClosestPlayer(999.0);
                    }
                }
                if (this.target == null) return;
                if (!this.target.isAlive()) {
                    if (this.firstFollower) {
                        this.target = findPlayerInFov((int) this.fov.getValue());
                    } else {
                        this.target = findClosestPlayer(999.0);
                    }
                }
                if (this.target == null) return;

                if (mc.player.getY() - this.target.getY() >= this.fallDistance.getValue()) {
                    this.waitMace = false;
                }

                Vec3d predictPosMace = predictTargetPos(this.target, (int) this.predict.getValue());
                float[] rotsMace = getRotation(mc.player.getEyePos(), predictPosMace.add(0, this.target.getEyeY() - this.target.getY(), 0));
                targetYaw = rotsMace[0];
                targetPitch = rotsMace[1];
                this.canFollow = true;
                prevYaw = mc.player.getYaw();
                prevPitch = mc.player.getPitch();
                mc.player.setYaw(targetYaw);
                mc.player.setPitch(getFollowPitch(targetPitch));
            }
        }
    }

    @EventListener(priority = -9999)
    public void onRotation(UpdateRotateEvent event) {
        if (nullCheck() || !this.canFollow || this.target == null) return;

        float yaw = targetYaw;
        float pitch = targetPitch;

        if (this.mode.is(FollowerMode.Mace) && this.waitMace) {
            pitch = -Math.abs(targetPitch);
        }

        event.setYaw(yaw);
        event.setPitch(pitch);
    }

    public float getFollowPitch(float targetPitch) {
        if (this.canFollow && this.target != null) {
            if (this.mode.is(FollowerMode.Mace) && this.waitMace) {
                return -Math.abs(targetPitch);
            }
            return targetPitch;
        }
        return mc.player.getPitch();
    }

    public void reset() {
        this.target = null;
        this.canFollow = false;
    }

    public void resetFirst() {
        this.firstFollower = true;
    }

    private Vec3d predictTargetPos(PlayerEntity target, int ticks) {
        Vec3d targetPos = target.getPos();
        if (ticks <= 0) return targetPos;
        Vec3d velocity = target.getVelocity();
        return targetPos.add(velocity.multiply(ticks));
    }

    private float[] getRotation(Vec3d eyesPos, Vec3d vec) {
        double diffX = vec.x - eyesPos.x;
        double diffY = vec.y - eyesPos.y;
        double diffZ = vec.z - eyesPos.z;
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float) (-Math.toDegrees(Math.atan2(diffY, diffXZ)));
        return new float[]{MathHelper.wrapDegrees(yaw), MathHelper.wrapDegrees(pitch)};
    }

    private PlayerEntity findPlayerInFov(int inFov) {
        PlayerEntity closest = null;
        float bestFov = (float) inFov;

        for (PlayerEntity player : getPlayersInRange(999.0)) {
            if (!player.isAlive()) continue;
            Vec3d playerPos = player.getPos();
            float[] rots = getRotation(mc.player.getEyePos(), playerPos);
            float yawDiff = Math.abs(MathHelper.wrapDegrees(rots[0] - mc.player.getYaw()));
            float pitchDiff = Math.abs(MathHelper.wrapDegrees(rots[1] - mc.player.getPitch()));
            float fovDist = (float) Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
            if (fovDist < bestFov) {
                closest = player;
                bestFov = fovDist;
            }
        }

        if (closest == null) {
            return findClosestPlayer(999.0);
        }
        return closest;
    }

    private PlayerEntity findClosestPlayer(double distance) {
        PlayerEntity closest = null;
        for (PlayerEntity player : getPlayersInRange(distance)) {
            if (closest == null) {
                closest = player;
            } else if (mc.player.distanceTo(player) < mc.player.distanceTo(closest)) {
                closest = player;
            }
        }
        return closest;
    }

    private List<PlayerEntity> getPlayersInRange(double range) {
        List<PlayerEntity> list = new ArrayList<>();
        for (Entity entity : Arc.THREAD.getEntities()) {
            if (entity instanceof PlayerEntity player && entity != mc.player && !Arc.FRIEND.isFriend(player.getName().getString())) {
                if (mc.player.distanceTo(entity) <= range) {
                    list.add(player);
                }
            }
        }
        return list;
    }

    public enum FollowerMode {
        Normal,
        Mace
    }
}
