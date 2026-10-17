package dev.G255.Lumi.mod.modules.impl.combat;

import dev.G255.Lumi.Lumi;
import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.UpdateEvent;
import dev.G255.Lumi.api.events.impl.UpdateRotateEvent;
import dev.G255.Lumi.api.utils.combat.CombatUtil;
import dev.G255.Lumi.api.utils.math.Timer;
import dev.G255.Lumi.api.utils.player.EntityUtil;
import dev.G255.Lumi.api.utils.player.InventoryUtil;
import dev.G255.Lumi.api.utils.world.BlockUtil;
import dev.G255.Lumi.core.impl.RotationManager;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;

public class PearlBlocker extends Module {
    public static PearlBlocker INSTANCE;

    private final SliderSetting range = this.add(new SliderSetting("Range", 5.0, 1.0, 6.0, 0.1));
    private final SliderSetting enemyRange = this.add(new SliderSetting("EnemyRange", 30.0, 0.0, 60.0, 1.0));

    private final BooleanSetting predict = this.add(new BooleanSetting("Predict", true));
    private final SliderSetting predictTick = this.add(new SliderSetting("PredictTick", 5, 0, 20, 1, () -> predict.getValue()));
    private final SliderSetting maxBlocks = this.add(new SliderSetting("MaxBlocks", 2, 1, 4));
    private final double PING_FACTOR = 1.0;
    private final float ROT_SPEED = 180.0f; // ç¬é´è½¬å¤´
    private final int DELAY_MS = 0; // æ å·å?

    private final Timer timer = new Timer();
    private boolean isRotating = false;
    private Vec3d rotationTarget = null;
    private float currentYaw = 0f;
    private float currentPitch = 0f;

    public PearlBlocker() {
        super("PearlBlocker", Category.Combat);
        this.setChinese("");
        this.setChineseDesc("");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        this.timer.reset();
        this.isRotating = false;
        if (mc.player != null) {
            this.currentYaw = mc.player.getYaw();
            this.currentPitch = mc.player.getPitch();
        }
    }

    @Override
    public void onDisable() {
        this.isRotating = false;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (mc.world == null || mc.player == null) return;
        if (mc.player.isUsingItem()) {
            this.isRotating = false;
            return;
        }

        if (!this.timer.passedMs(DELAY_MS)) return;
        if (!isEnemyNearby()) {
            this.isRotating = false;
            return;
        }

        int obsidianSlot = InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN);
        if (obsidianSlot == -1) {
            this.isRotating = false;
            return;
        }
        EnderPearlEntity targetPearl = findClosestTrackedPearl();
        if (targetPearl == null) {
            this.isRotating = false;
            return;
        }

        List<Vec3d> points = calcPearlTrajectory(targetPearl);
        if (points.isEmpty()) return;

        double maxDistSq = Math.pow(this.range.getValue(), 2);
        int idealTick = 0;
        if (this.predict.getValue()) {
            int ping = 0;
            if (mc.getNetworkHandler() != null && mc.player != null) {
                PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if (entry != null) ping = Math.max(0, entry.getLatency());
            }
            int pingTicks = (int) Math.round((ping * PING_FACTOR) / 50.0);
            idealTick = pingTicks + this.predictTick.getValueInt();
        }
        int bestIndex = -1;
        for (int i = 0; i < points.size(); i++) {
            BlockPos p = BlockPos.ofFloored(points.get(i));
            if (mc.player.getEyePos().squaredDistanceTo(p.toCenterPos()) <= maxDistSq) {
                if (mc.world.getBlockState(p).isReplaceable()) {
                    if (BlockUtil.getPlaceSide(p) != null || (mc.world.getBlockState(p.down()).isReplaceable() && BlockUtil.getPlaceSide(p.down()) != null)) {
                        bestIndex = i;
                        if (i >= idealTick) {
                            break;
                        }
                    }
                }
            }
        }

        if (bestIndex == -1) {
            this.isRotating = false;
            return;
        }
        List<BlockPos> placements = new ArrayList<>();
        BlockPos targetPos = BlockPos.ofFloored(points.get(bestIndex));

        if (BlockUtil.getPlaceSide(targetPos) != null) {
            placements.add(targetPos);
            if (placements.size() < this.maxBlocks.getValueInt() && mc.world.getBlockState(targetPos.up()).isReplaceable()) {
                placements.add(targetPos.up());
            }
        } else {
            BlockPos downPos = targetPos.down();
            placements.add(downPos);
            if (placements.size() < this.maxBlocks.getValueInt()) {
                placements.add(targetPos);
            }
        }

        if (placements.isEmpty()) return;
        for (BlockPos placePos : placements) {
            List<EndCrystalEntity> crystals = BlockUtil.getEndCrystals(new Box(placePos));
            for (EndCrystalEntity c : crystals) {
                if (c != null && c.isAlive() && hitboxBlocksPearl(points, c)) {
                    CombatUtil.attackCrystal(c, true, false);
                    this.timer.reset();
                    return;
                }
            }
        }
        this.rotationTarget = placements.get(0).toCenterPos();
        this.isRotating = true;
        int oldSlot = mc.player.getInventory().selectedSlot;
        boolean swapped = false;

        for (BlockPos placePos : placements) {
            if (!swapped) {
                InventoryUtil.inventorySwap(obsidianSlot, oldSlot);
                swapped = true;
            }
            BlockUtil.placeBlock(placePos, false);
        }

        if (swapped) {
            InventoryUtil.inventorySwap(obsidianSlot, oldSlot);
            EntityUtil.syncInventory();
            this.timer.reset();
        }
    }

    @EventListener
    public void onRotate(UpdateRotateEvent event) {
        if (mc.player == null) return;

        if (!this.isRotating || this.rotationTarget == null) {
            this.currentYaw = mc.player.getYaw();
            this.currentPitch = mc.player.getPitch();
            return;
        }

        float[] targetRots = RotationManager.getRotation(this.rotationTarget);
        float yawDiff = MathHelper.wrapDegrees(targetRots[0] - this.currentYaw);
        this.currentYaw += MathHelper.clamp(yawDiff, -ROT_SPEED, ROT_SPEED);

        float pitchDiff = targetRots[1] - this.currentPitch;
        this.currentPitch += MathHelper.clamp(pitchDiff, -ROT_SPEED, ROT_SPEED);
        this.currentPitch = MathHelper.clamp(this.currentPitch, -90f, 90f);

        event.setYaw(this.currentYaw);
        event.setPitch(this.currentPitch);
    }
    private boolean isEnemyNearby() {
        if (enemyRange.getValue() <= 0) return true;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !player.isAlive()) continue;
            try {
                if (Lumi.FRIEND.isFriend(player)) continue;
            } catch (Exception ignored) {}
            if (mc.player.distanceTo(player) <= enemyRange.getValue()) {
                return true;
            }
        }
        return false;
    }

    private EnderPearlEntity findClosestTrackedPearl() {
        EnderPearlEntity best = null;
        double bestDist = Double.MAX_VALUE;

        for (Entity e : Lumi.THREAD.getEntities()) {
            if (e instanceof EnderPearlEntity pearl) {
                Entity owner = pearl.getOwner();
                if (owner == mc.player) continue;
                if (owner instanceof PlayerEntity playerOwner) {
                    if (Lumi.FRIEND.isFriend(playerOwner)) continue;
                }

                double d = mc.player.getPos().distanceTo(pearl.getPos());
                if (d > 30.0) continue;

                if (d < bestDist) {
                    bestDist = d;
                    best = pearl;
                }
            }
        }
        return best;
    }

    private List<Vec3d> calcPearlTrajectory(EnderPearlEntity entity) {
        List<Vec3d> points = new ArrayList<>();
        double x = entity.getX(), y = entity.getY(), z = entity.getZ();
        double motionX = entity.getVelocity().x, motionY = entity.getVelocity().y, motionZ = entity.getVelocity().z;
        points.add(new Vec3d(x, y, z));

        for (int i = 0; i < 200; i++) {
            Vec3d lastPos = new Vec3d(x, y, z);
            x += motionX; y += motionY; z += motionZ;

            if (mc.world.getBlockState(BlockPos.ofFloored(x, y, z)).getBlock() == Blocks.WATER) {
                motionX *= 0.8; motionY *= 0.8; motionZ *= 0.8;
            } else {
                motionX *= 0.99; motionY *= 0.99; motionZ *= 0.99;
            }
            motionY -= 0.03;

            Vec3d pos = new Vec3d(x, y, z);
            BlockHitResult bhr = mc.world.raycast(new RaycastContext(lastPos, pos, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, entity));
            if (bhr != null && bhr.getType() == HitResult.Type.BLOCK) {
                points.add(bhr.getPos());
                return points;
            }
            if (y <= -65) break;
            points.add(pos);
        }
        return points;
    }

    private static boolean hitboxBlocksPearl(List<Vec3d> points, EndCrystalEntity crystal) {
        if (points.size() < 2) return false;
        Box box = crystal.getBoundingBox().expand(0.1);
        for (int i = 1; i < points.size(); i++) {
            Vec3d a = points.get(i - 1);
            Vec3d b = points.get(i);
            if (segmentIntersectsBox(a, b, box)) return true;
        }
        return false;
    }

    private static boolean segmentIntersectsBox(Vec3d start, Vec3d end, Box box) {
        double[] t = {0.0, 1.0};
        if (!updateSlab(start.x, end.x - start.x, box.minX, box.maxX, t)) return false;
        if (!updateSlab(start.y, end.y - start.y, box.minY, box.maxY, t)) return false;
        return updateSlab(start.z, end.z - start.z, box.minZ, box.maxZ, t);
    }

    private static boolean updateSlab(double s, double d, double min, double max, double[] t) {
        if (Math.abs(d) < 1.0E-9) return s >= min && s <= max;
        double ood = 1.0 / d;
        double t1 = (min - s) * ood, t2 = (max - s) * ood;
        if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
        t[0] = Math.max(t[0], t1);
        t[1] = Math.min(t[1], t2);
        return t[0] <= t[1];
    }
}