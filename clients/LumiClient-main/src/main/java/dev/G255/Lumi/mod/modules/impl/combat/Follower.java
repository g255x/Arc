package dev.G255.Lumi.mod.modules.impl.combat;
import dev.G255.Lumi.Lumi;

import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.ClientTickEvent;
import dev.G255.Lumi.api.events.impl.RotationEvent;
import dev.G255.Lumi.api.utils.math.Animation;
import dev.G255.Lumi.api.utils.math.Easing;
import dev.G255.Lumi.api.utils.math.MathUtil;
import dev.G255.Lumi.api.utils.player.EntityUtil;
import dev.G255.Lumi.api.utils.render.ColorUtil;
import dev.G255.Lumi.api.utils.render.JelloUtil;
import dev.G255.Lumi.api.utils.render.Render3DUtil;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.impl.movement.ElytraFlight;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.ColorSetting;
import dev.G255.Lumi.mod.modules.settings.impl.EnumSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class Follower
extends Module {
    public static Follower INSTANCE;
    private Entity target;
    private final EnumSetting<Mode> mode = this.add(new EnumSetting<Mode>("Mode", Mode.LowestDistance));
    private final SliderSetting range = this.add(new SliderSetting("Range", 50.0, 5.0, 100.0));
    private final BooleanSetting onlyElytraFlight = this.add(new BooleanSetting("OnlyElytraFlight", false));
    private final EnumSetting<TargetESP> renderMode = this.add(new EnumSetting<TargetESP>("RenderMode", TargetESP.Fill));
    private final SliderSetting animationTime = this.add(new SliderSetting("AnimationTime", 200.0, 0.0, 2000.0, 1.0));
    private final EnumSetting<Easing> ease = this.add(new EnumSetting<Easing>("Ease", Easing.CubicInOut));
    private final ColorSetting color = this.add(new ColorSetting("Color", new Color(160, 0, 225, 50)));
    private final ColorSetting outlineColor = this.add(new ColorSetting("OutlineColor", new Color(255, 255, 255, 100)));
    private final ColorSetting hitColor = this.add(new ColorSetting("HitColor", new Color(255, 255, 255, 150)));
    private final ColorSetting hitOutlineColor = this.add(new ColorSetting("HitOutlineColor", new Color(255, 255, 255, 150)));
    private final Animation animation = new Animation();
    private final BooleanSetting render = this.add(new BooleanSetting("Render", true));
    private final BooleanSetting deadLock = this.add(new BooleanSetting("DeadLock", true));
    private final SliderSetting stopRange = this.add(new SliderSetting("StopRange", 3.0, 0.0, 20.0, 0.5));
    private final SliderSetting rotationSpeed = this.add(new SliderSetting("RotationSpeed", 180.0, 10.0, 360.0));
    private final SliderSetting priority = this.add(new SliderSetting("Priority", 5.0, 0.0, 10.0));

    public Follower() {
        super("Follower", "Automatically follows and stays close to target player.", Module.Category.Combat);
        this.setChinese("自动追人");
        this.setChineseDesc("自动追踪并紧跟目标玩家");
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

    @EventListener
    public void onTick(ClientTickEvent event) {
        if (event.isPost()) {
            return;
        }
        if (!Follower.nullCheck()) {
            if (this.onlyElytraFlight.getValue() && !ElytraFlight.INSTANCE.isOn()) {
                this.target = null;
                return;
            }
            if (deadLock.getValue() && this.target != null && isTargetStillValid(this.target)) {
            } else {
                this.target = this.findTarget();
            }
            if (this.target != null && mc.player.distanceTo(this.target) > stopRange.getValue()) {
                Vec3d hitVec = this.getHitVec(this.target);
                float[] rot = this.getRotationsToVec(hitVec);
                Follower.mc.player.setYaw(rot[0]);
                Follower.mc.player.setPitch(rot[1]);
            }
        }
    }

    @EventListener
    public void onRotate(RotationEvent event) {
        if (Follower.nullCheck() || this.target == null) return;
        if (this.onlyElytraFlight.getValue() && !ElytraFlight.INSTANCE.isOn()) return;
        if (mc.player.distanceTo(this.target) <= stopRange.getValue()) return;
        Vec3d hitVec = this.getHitVec(this.target);
        event.setTarget(hitVec, this.rotationSpeed.getValueFloat(), this.priority.getValueFloat());
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (this.target != null && this.render.getValue()) {
            float partialTicks = mc.getRenderTickCounter().getTickDelta(true);
            this.doRender(matrixStack, partialTicks, this.target);
        }
    }

    private void doRender(MatrixStack stack, float partialTicks, Entity entity) {
        Box bb = entity.getBoundingBox();
        Vec3d interpolated = new Vec3d(MathUtil.interpolate(entity.prevX, entity.getX(), (double)partialTicks), MathUtil.interpolate(entity.prevY, entity.getY(), (double)partialTicks), MathUtil.interpolate(entity.prevZ, entity.getZ(), (double)partialTicks));
        Box renderBox = bb.offset(interpolated.subtract(entity.getPos()));
        renderBox = renderBox.expand(0.0, 0.1, 0.0);
        switch (this.renderMode.getValue().ordinal()) {
            case 0: {
                Render3DUtil.draw3DBox(stack, renderBox, ColorUtil.fadeColor(this.color.getValue(), this.hitColor.getValue(), this.animation.get(0.0, this.animationTime.getValueInt(), this.ease.getValue())), ColorUtil.fadeColor(this.outlineColor.getValue(), this.hitOutlineColor.getValue(), this.animation.get(0.0, this.animationTime.getValueInt(), this.ease.getValue())), false, true);
                break;
            }
            case 1: {
                Render3DUtil.draw3DBox(stack, renderBox, ColorUtil.fadeColor(this.color.getValue(), this.hitColor.getValue(), this.animation.get(0.0, this.animationTime.getValueInt(), this.ease.getValue())), ColorUtil.fadeColor(this.outlineColor.getValue(), this.hitOutlineColor.getValue(), this.animation.get(0.0, this.animationTime.getValueInt(), this.ease.getValue())), true, true);
                break;
            }
            case 2: {
                JelloUtil.drawJello(stack, entity, this.color.getValue());
                break;
            }
            case 3: {
                Render3DUtil.drawTargetEsp(stack, this.target, this.color.getValue());
            }
        }
    }

    private Entity findTarget() {
        Entity bestTarget = null;
        for (Entity entity : Lumi.THREAD.getEntities()) {
            if (!this.isValidTarget(entity)) continue;
            if (bestTarget == null) {
                bestTarget = entity;
                continue;
            }
            double distance = Follower.mc.player.getEyePos().distanceTo(entity.getPos());
            float health = EntityUtil.getHealth(entity);
            switch (this.mode.getValue().ordinal()) {
                case 0: {
                    double bestDistance = Follower.mc.player.getEyePos().distanceTo(bestTarget.getPos());
                    if (!(distance < bestDistance)) break;
                    bestTarget = entity;
                    break;
                }
                case 1: {
                    double currentTargetDistance;
                    float bestHealth = EntityUtil.getHealth(bestTarget);
                    if (health < bestHealth) {
                        bestTarget = entity;
                        break;
                    }
                    if (health != bestHealth || !(distance < (currentTargetDistance = Follower.mc.player.getEyePos().distanceTo(bestTarget.getPos())))) break;
                    bestTarget = entity;
                }
            }
        }
        return bestTarget;
    }

    private boolean isValidTarget(Entity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        }
        PlayerEntity player = (PlayerEntity)entity;
        if (player == Follower.mc.player) {
            return false;
        }
        if (Lumi.FRIEND.isFriend(player)) {
            return false;
        }
        if (!entity.isAlive()) {
            return false;
        }
        double distance = Follower.mc.player.getEyePos().distanceTo(entity.getPos());
        return !(distance > this.range.getValue());
    }

    private boolean isTargetStillValid(Entity entity) {
        if (entity == null || !entity.isAlive() || entity.isRemoved()) return false;
        if (!(entity instanceof PlayerEntity player)) return false;
        if (Lumi.FRIEND.isFriend(player)) return false;
        return mc.player.getEyePos().distanceTo(entity.getPos()) <= this.range.getValue();
    }

    private Vec3d getHitVec(Entity entity) {
        Vec3d eyePos = Follower.mc.player.getEyePos();
        Box box = entity.getBoundingBox();
        double targetCenterX = (box.minX + box.maxX) / 2.0;
        double targetCenterZ = (box.minZ + box.maxZ) / 2.0;
        double playerEyeHeight = Follower.mc.player.getEyeHeight(Follower.mc.player.getPose());
        double heightOffset = playerEyeHeight * 0.5;
        if (entity instanceof PlayerEntity) {
            heightOffset = (double)entity.getHeight() * 0.5;
        }
        double targetY = box.minY + heightOffset;
        double closestX = Math.max(box.minX, Math.min(eyePos.x, box.maxX));
        double closestZ = Math.max(box.minZ, Math.min(eyePos.z, box.maxZ));
        return new Vec3d(closestX, targetY, closestZ);
    }

    private float[] getRotationsToVec(Vec3d vec) {
        Vec3d eyes = Follower.mc.player.getEyePos();
        double diffX = vec.x - eyes.x;
        double diffY = vec.y - eyes.y;
        double diffZ = vec.z - eyes.z;
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0f;
        float pitch = (float)(-Math.toDegrees(Math.atan2(diffY, dist)));
        return new float[]{yaw, pitch};
    }

    public static enum Mode {
        LowestDistance,
        LowestHealth;

    }

    public static enum TargetESP {
        Fill,
        Box,
        Jello,
        ThunderHack;

    }
}