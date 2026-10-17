package dev.perish.api.utils.combat;

import dev.perish.api.utils.Wrapper;
import dev.perish.api.utils.math.DamageUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.BlockView;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.hit.BlockHitResult;

public class miodamage implements Wrapper {
    public static float calculateDamage(Vec3d explosionPos, LivingEntity target, LivingEntity predict, float power, boolean ignoreTerrain) {
        return calculateDamage(explosionPos, target, predict, power, ignoreTerrain, null, Blocks.AIR.getDefaultState());
    }

    public static float calculateDamage(
            Vec3d explosionPos,
            LivingEntity target,
            LivingEntity predict,
            float power,
            boolean ignoreTerrain,
            BlockPos overridePos,
            BlockState overrideState
    ) {
        if (target == null) {
            return 0.0F;
        }
        if (target instanceof PlayerEntity player && player.getAbilities().creativeMode) {
            return 0.0F;
        }

        LivingEntity sampledEntity = predict != null ? predict : target;
        Box sampledBox = sampledEntity.getBoundingBox();
        Vec3d sampledFeet = new Vec3d(
                MathHelper.lerp(0.5D, sampledBox.minX, sampledBox.maxX),
                sampledBox.minY,
                MathHelper.lerp(0.5D, sampledBox.minZ, sampledBox.maxZ)
        );
        double radius = power * 2.0D;
        double scaledDistance = sampledFeet.distanceTo(explosionPos) / radius;
        if (scaledDistance > 1.0D) {
            return 0.0F;
        }

        float exposure = getExposure(explosionPos, sampledBox, ignoreTerrain, overridePos, overrideState);
        double impact = (1.0D - scaledDistance) * exposure;
        float damage = (float) ((impact * impact + impact) / 2.0D * 7.0D * radius + 1.0D);
        return DamageUtils.calculateReductionsExplosion(damage, target, mc.world.getDamageSources().explosion(null));
    }

    private static float getExposure(
            Vec3d explosionPos,
            Box box,
            boolean ignoreTerrain,
            BlockPos overridePos,
            BlockState overrideState
    ) {
        double xStep = 1.0D / ((box.maxX - box.minX) * 2.0D + 1.0D);
        double yStep = 1.0D / ((box.maxY - box.minY) * 2.0D + 1.0D);
        double zStep = 1.0D / ((box.maxZ - box.minZ) * 2.0D + 1.0D);
        if (xStep <= 0.0D || yStep <= 0.0D || zStep <= 0.0D) {
            return 0.0F;
        }

        double xOffset = (1.0D - Math.floor(1.0D / xStep) * xStep) * 0.5D;
        double zOffset = (1.0D - Math.floor(1.0D / zStep) * zStep) * 0.5D;
        int clear = 0;
        int total = 0;

        for (double xFactor = 0.0D; xFactor <= 1.0D; xFactor += xStep) {
            for (double yFactor = 0.0D; yFactor <= 1.0D; yFactor += yStep) {
                for (double zFactor = 0.0D; zFactor <= 1.0D; zFactor += zStep) {
                    double x = MathHelper.lerp(xFactor, box.minX, box.maxX) + xOffset;
                    double y = MathHelper.lerp(yFactor, box.minY, box.maxY);
                    double z = MathHelper.lerp(zFactor, box.minZ, box.maxZ) + zOffset;
                    Vec3d sample = new Vec3d(x, y, z);
                    if (raycast(sample, explosionPos, ignoreTerrain, overridePos, overrideState) == HitResult.Type.MISS) {
                        clear++;
                    }
                    total++;
                }
            }
        }

        return total == 0 ? 0.0F : (float) clear / (float) total;
    }

    private static HitResult.Type raycast(
            Vec3d start,
            Vec3d end,
            boolean ignoreTerrain,
            BlockPos overridePos,
            BlockState overrideState
    ) {
        if (overridePos == null) {
            return DamageUtils.raycast(start, end, ignoreTerrain);
        }

        BlockHitResult result = DamageUtils.raycast(
                new DamageUtils.ExposureRaycastContext(start, end),
                (context, blockPos) -> {
                    BlockState state = blockPos.equals(overridePos) ? overrideState : mc.world.getBlockState(blockPos);
                    if (ignoreTerrain && !blockPos.equals(overridePos) && state.getBlock().getBlastResistance() < 600.0F) {
                        return null;
                    }
                    return state.getCollisionShape((BlockView) mc.world, blockPos).raycast(context.start(), context.end(), blockPos);
                }
        );
        return result == null ? HitResult.Type.MISS : result.getType();
    }
}
