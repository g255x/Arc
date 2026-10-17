package dev.G255.Lumi.mod.modules.impl.misc;

import dev.G255.Lumi.Lumi;
import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.UpdateEvent;
import dev.G255.Lumi.api.utils.math.Timer;
import dev.G255.Lumi.api.utils.player.EntityUtil;
import dev.G255.Lumi.api.utils.player.InventoryUtil;
import dev.G255.Lumi.core.impl.RotationManager;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
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

import java.util.HashSet;
import java.util.Set;

public class AntiMonkey extends Module {
    public static AntiMonkey INSTANCE;

    // ââ Follow settings ââ
    private final BooleanSetting follow = this.add(
            new BooleanSetting("Follow", true).setParent()
    );
    private final SliderSetting range = this.add(
            new SliderSetting("Range", 20.0, 5.0, 100.0, 0.5, this.follow::isOpen).setSuffix("b")
    );
    private final SliderSetting cooldown = this.add(
            new SliderSetting("Cooldown", 1.0, 0.0, 10.0, 0.1, this.follow::isOpen).setSuffix("s")
    );
    private final BooleanSetting followPlayer = this.add(
            new BooleanSetting("FollowPlayer", false, this.follow::isOpen).setParent()
    );
    private final SliderSetting followPlayerRange = this.add(
            new SliderSetting("FPRange", 10.0, 2.0, 50.0, 0.5, () -> this.follow.isOpen() && this.followPlayer.isOpen()).setSuffix("b")
    );

    // ââ DeathLock settings (auto only) ââ
    private final BooleanSetting deathLock = this.add(
            new BooleanSetting("DeathLock", false).setParent()
    );
    private final SliderSetting dlRange = this.add(
            new SliderSetting("DLRange", 10.0, 1.0, 50.0, 0.5, this.deathLock::isOpen).setSuffix("b")
    );
    private final SliderSetting dlCooldown = this.add(
            new SliderSetting("DLCooldown", 1.0, 0.0, 10.0, 0.1, this.deathLock::isOpen).setSuffix("s")
    );

    // ââ Shared switch settings ââ
    private final BooleanSetting doSwitch = this.add(
            new BooleanSetting("Switch", true).setParent()
    );
    private final BooleanSetting silentSwitch = this.add(
            new BooleanSetting("SilentSwitch", true, this.doSwitch::isOpen)
    );

    // ââ State ââ
    private final Set<Integer> processedPearls = new HashSet<>();
    private final Timer followTimer = new Timer();
    private final Timer dlTimer = new Timer();

    // ââ Physics ââ
    private static final double THROW_SPEED = 1.5;
    private static final double GRAVITY = 0.03;
    private static final double DRAG = 0.99;
    private static final double WATER_DRAG = 0.8;

    public AntiMonkey() {
        super("AntiMonkey", "Anti-monkey module that auto-disables modules in spawn.", Module.Category.Misc);
        this.setChinese("");
        this.setChineseDesc("");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        processedPearls.clear();
    }

    @Override
    public void onDisable() {
        processedPearls.clear();
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (nullCheck()) return;

        if (follow.isOpen() && follow.getValue()) doFollow();
        if (deathLock.isOpen() && deathLock.getValue()) doDeathLock();
    }

    // âââââââââââââââââââââââââââââââââââââââââââ?
    //  Follow â?mirror enemy pearl landing
    // âââââââââââââââââââââââââââââââââââââââââââ?

    private void doFollow() {
        if (!followTimer.passedS(cooldown.getValue())) return;

        PlayerEntity lockTarget = null;
        if (followPlayer.isOpen() && followPlayer.getValue()) {
            lockTarget = findNearestEnemy(followPlayerRange.getValue());
        }

        for (Entity e : Lumi.THREAD.getEntities()) {
            if (!(e instanceof EnderPearlEntity pearl)) continue;
            if (processedPearls.contains(e.getId())) continue;

            Entity owner = pearl.getOwner();
            if (owner == mc.player) continue;
            if (owner instanceof PlayerEntity po && Lumi.FRIEND.isFriend(po)) continue;
            if (lockTarget != null && owner != lockTarget) continue;

            processedPearls.add(e.getId());

            Vec3d landing = calcPearlLanding(pearl);
            if (landing == null) continue;

            if (mc.player.getPos().distanceTo(landing) < range.getValue()) continue;
            if (!hasPearl()) continue;

            float[] rots = solveTrajectory(landing);
            if (rots == null) continue;

            throwPearl(rots[0], rots[1]);
            followTimer.reset();
            break;
        }
    }

    // âââââââââââââââââââââââââââââââââââââââââââ?
    //  DeathLock â?auto-throw at nearest enemy
    // âââââââââââââââââââââââââââââââââââââââââââ?

    private void doDeathLock() {
        if (!dlTimer.passedS(dlCooldown.getValue())) return;

        PlayerEntity target = findNearestEnemy(dlRange.getValue());
        if (target == null) return;

        Vec3d targetPos = target.getPos().add(0, target.getHeight() / 2.0, 0);

        float[] rots = solveTrajectory(targetPos);
        if (rots == null) return;

        if (isPathBlocked(rots[0], rots[1], targetPos)) return;
        if (!hasPearl()) return;

        throwPearl(rots[0], rots[1]);
        dlTimer.reset();
    }

    // ââ Nearest enemy ââ

    private PlayerEntity findNearestEnemy(double maxDist) {
        PlayerEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (PlayerEntity p : Lumi.THREAD.getPlayers()) {
            if (p == mc.player) continue;
            if (Lumi.FRIEND.isFriend(p)) continue;
            double d = mc.player.distanceTo(p);
            if (d < maxDist && d < bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }

    // ââ Enemy pearl landing ââ

    private Vec3d calcPearlLanding(EnderPearlEntity entity) {
        double x = entity.getX(), y = entity.getY(), z = entity.getZ();
        double mx = entity.getVelocity().x, my = entity.getVelocity().y, mz = entity.getVelocity().z;

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
                    new RaycastContext(last, pos, ShapeType.OUTLINE, FluidHandling.NONE, entity));
            if (bhr != null && bhr.getType() == Type.BLOCK) return bhr.getPos();
            if (y <= -65) return null;
        }
        return null;
    }

    // ââ Path blocked ââ

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

    // ââ Solve launch angles ââ

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

    // ââ Inventory ââ

    private boolean hasPearl() {
        if (InventoryUtil.findItem(Items.ENDER_PEARL) != -1) return true;
        if (doSwitch.getValue() && silentSwitch.getValue()) {
            return InventoryUtil.findItemInventorySlotFromZero(Items.ENDER_PEARL) != -1;
        }
        return false;
    }

    // ââ Throw ââ

    private void throwPearl(float yaw, float pitch) {
        if (mc.player.getMainHandStack().getItem() == Items.ENDER_PEARL) {
            Lumi.ROTATION.snapAt(yaw, pitch);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitch));
            Lumi.ROTATION.snapBack();
            return;
        }

        if (!doSwitch.getValue()) return;

        if (silentSwitch.getValue()) {
            int pearlInv = InventoryUtil.findItemInventorySlotFromZero(Items.ENDER_PEARL);
            if (pearlInv == -1) return;
            InventoryUtil.inventorySwap(pearlInv, mc.player.getInventory().selectedSlot);
            Lumi.ROTATION.snapAt(yaw, pitch);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitch));
            Lumi.ROTATION.snapBack();
            InventoryUtil.inventorySwap(pearlInv, mc.player.getInventory().selectedSlot);
            EntityUtil.syncInventory();
        } else {
            int pearlSlot = InventoryUtil.findItem(Items.ENDER_PEARL);
            if (pearlSlot == -1) return;
            int old = mc.player.getInventory().selectedSlot;
            InventoryUtil.switchToSlot(pearlSlot);
            Lumi.ROTATION.snapAt(yaw, pitch);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitch));
            Lumi.ROTATION.snapBack();
            InventoryUtil.switchToSlot(old);
        }
    }
}
