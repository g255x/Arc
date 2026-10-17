package dev.kissoo.mod.modules.impl.combat;

import dev.kissoo.Forever;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.utils.player.EntityUtil;
import dev.kissoo.api.utils.player.InventoryUtil;
import dev.kissoo.core.impl.RotationManager;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BindSetting;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.lwjgl.glfw.GLFW;

public class AimPearl extends Module {

    private enum TargetMode { Nearest, FOV, Angle }

    private final BindSetting aimBind = this.add(
            new BindSetting("AimBind", -1)
    );
    private final SliderSetting targetRange = this.add(
            new SliderSetting("TargetRange", 10.0, 1.0, 50.0, 0.5).setSuffix("b")
    );
    private final EnumSetting<TargetMode> targetMode = this.add(
            new EnumSetting<>("TargetMode", TargetMode.Nearest)
    );
    private final BooleanSetting doSwitch = this.add(
            new BooleanSetting("Switch", true).setParent()
    );
    private final BooleanSetting silentSwitch = this.add(
            new BooleanSetting("SilentSwitch", true, this.doSwitch::isOpen)
    );

    private boolean wasPressed = false;

    private static final double THROW_SPEED = 1.5;
    private static final double GRAVITY = 0.03;
    private static final double DRAG = 0.99;
    private static final double WATER_DRAG = 0.8;

    public AimPearl() {
        super("AimPearl", Module.Category.Combat);
        this.setChinese("自瞄珍珠");
    }

    @Override
    public void onEnable() {
        wasPressed = false;
    }

    @Override
    public void onDisable() {
        wasPressed = false;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (nullCheck()) return;

        int key = aimBind.getValue();
        if (key == -1) return;

        long handle = mc.getWindow().getHandle();
        boolean pressed = GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;

        if (pressed && !wasPressed) {
            wasPressed = true;
            doAimThrow();
        }
        if (!pressed) wasPressed = false;
    }

    private void doAimThrow() {
        PlayerEntity target = findTarget();
        if (target == null) return;

        Vec3d targetPos = target.getPos().add(0, target.getHeight() / 2.0, 0);

        float[] rots = solveTrajectory(targetPos);
        if (rots == null) return;

        if (isPathBlocked(rots[0], rots[1], targetPos)) return;
        if (!hasPearl()) return;

        throwPearl(rots[0], rots[1]);
    }

    private PlayerEntity findTarget() {
        double maxDist = targetRange.getValue();
        PlayerEntity best = null;
        double bestScore = Double.MAX_VALUE;

        Vec3d myPos = mc.player.getEyePos();
        float myYaw = mc.player.getYaw();
        float myPitch = mc.player.getPitch();

        for (PlayerEntity p : Forever.THREAD.getPlayers()) {
            if (p == mc.player) continue;
            if (Forever.FRIEND.isFriend(p)) continue;

            double dist = mc.player.distanceTo(p);
            if (dist > maxDist) continue;

            switch (targetMode.getValue()) {
                case Nearest -> {
                    if (dist < bestScore) { bestScore = dist; best = p; }
                }
                case FOV -> {
                    float[] r = RotationManager.getRotation(myPos, p.getPos().add(0, p.getHeight() / 2.0, 0));
                    float dy = Math.abs(MathHelper.wrapDegrees(r[0] - myYaw));
                    float dp = Math.abs(r[1] - myPitch);
                    double fov = Math.sqrt(dy * dy + dp * dp);
                    if (fov < bestScore) { bestScore = fov; best = p; }
                }
                case Angle -> {
                    float[] r = RotationManager.getRotation(myPos, p.getPos().add(0, p.getHeight() / 2.0, 0));
                    float dy = Math.abs(MathHelper.wrapDegrees(r[0] - myYaw));
                    if (dy < bestScore) { bestScore = dy; best = p; }
                }
            }
        }
        return best;
    }

    // ── Path / Trajectory ──

    private boolean isPathBlocked(float yaw, float pitch, Vec3d target) {
        Vec3d hit = tracePath(yaw, pitch);
        if (hit == null) return true;
        return hit.squaredDistanceTo(target) > 4.0;
    }

    private Vec3d tracePath(float yaw, float pitch) {
        double x = mc.player.getX();
        double y = mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()) - 0.1000000014901161;
        double z = mc.player.getZ();

        float yawRad = yaw / 180.0F * (float) Math.PI;
        float pitchRad = pitch / 180.0F * (float) Math.PI;
        x -= MathHelper.cos(yawRad) * 0.16F;
        z -= MathHelper.sin(yawRad) * 0.16F;

        double mx = (-MathHelper.sin(yawRad) * MathHelper.cos(pitchRad)) * THROW_SPEED + mc.player.getVelocity().x;
        double my = (-MathHelper.sin(pitchRad)) * THROW_SPEED + mc.player.getVelocity().y;
        double mz = (MathHelper.cos(yawRad) * MathHelper.cos(pitchRad)) * THROW_SPEED + mc.player.getVelocity().z;

        for (int i = 0; i < 200; i++) {
            Vec3d last = new Vec3d(x, y, z);
            x += mx; y += my; z += mz;

            if (mc.world.getBlockState(BlockPos.ofFloored(x, y, z)).getBlock() == Blocks.WATER) {
                mx *= WATER_DRAG; my *= WATER_DRAG; mz *= WATER_DRAG;
            } else {
                mx *= DRAG; my *= DRAG; mz *= DRAG;
            }
            my -= GRAVITY;

            Vec3d pos = new Vec3d(x, y, z);
            BlockHitResult bhr = mc.world.raycast(
                    new RaycastContext(last, pos, ShapeType.OUTLINE, FluidHandling.NONE, mc.player));
            if (bhr != null && bhr.getType() == Type.BLOCK) return bhr.getPos();
            if (y <= -65) return null;
        }
        return null;
    }

    private float[] solveTrajectory(Vec3d target) {
        float yaw = (float) Math.toDegrees(
                Math.atan2(target.z - mc.player.getZ(), target.x - mc.player.getX())) - 90.0F;

        float bestPitch = -45f;
        double bestError = Double.MAX_VALUE;

        for (float pitch = -45f; pitch <= 60f; pitch += 2.0f) {
            double error = simError(yaw, pitch, target);
            if (error < bestError) { bestError = error; bestPitch = pitch; }
        }
        for (float pitch = bestPitch - 3f; pitch <= bestPitch + 3f; pitch += 0.5f) {
            double error = simError(yaw, pitch, target);
            if (error < bestError) { bestError = error; bestPitch = pitch; }
        }

        return new float[]{yaw, bestPitch};
    }

    private double simError(float yaw, float pitch, Vec3d target) {
        Vec3d landing = tracePath(yaw, pitch);
        if (landing == null) return Double.MAX_VALUE;
        return landing.squaredDistanceTo(target);
    }

    // ── Inventory ──

    private boolean hasPearl() {
        if (InventoryUtil.findItem(Items.ENDER_PEARL) != -1) return true;
        if (doSwitch.getValue() && silentSwitch.getValue()) {
            return InventoryUtil.findItemInventorySlotFromZero(Items.ENDER_PEARL) != -1;
        }
        return false;
    }

    // ── Throw ──

    private void throwPearl(float yaw, float pitch) {
        if (mc.player.getMainHandStack().getItem() == Items.ENDER_PEARL) {
            Forever.ROTATION.snapAt(yaw, pitch);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitch));
            Forever.ROTATION.snapBack();
            return;
        }

        if (!doSwitch.getValue()) return;

        if (silentSwitch.getValue()) {
            int pearlInv = InventoryUtil.findItemInventorySlotFromZero(Items.ENDER_PEARL);
            if (pearlInv == -1) return;
            InventoryUtil.inventorySwap(pearlInv, mc.player.getInventory().selectedSlot);
            Forever.ROTATION.snapAt(yaw, pitch);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitch));
            Forever.ROTATION.snapBack();
            InventoryUtil.inventorySwap(pearlInv, mc.player.getInventory().selectedSlot);
            EntityUtil.syncInventory();
        } else {
            int pearlSlot = InventoryUtil.findItem(Items.ENDER_PEARL);
            if (pearlSlot == -1) return;
            int old = mc.player.getInventory().selectedSlot;
            InventoryUtil.switchToSlot(pearlSlot);
            Forever.ROTATION.snapAt(yaw, pitch);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitch));
            Forever.ROTATION.snapBack();
            InventoryUtil.switchToSlot(old);
        }
    }
}
