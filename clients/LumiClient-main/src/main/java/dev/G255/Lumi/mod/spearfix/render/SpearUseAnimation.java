package dev.G255.Lumi.mod.spearfix.render;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public final class SpearUseAnimation {
    private static final int DELAY_TICKS = 20;
    private static final int KNOCKBACK_START_TICKS = 150;
    private static final int KNOCKBACK_END_TICKS = 190;
    private static final int DAMAGE_TICKS = 210;

    private SpearUseAnimation() {
    }

    public static void firstPersonUse(AbstractClientPlayerEntity player, float tickDelta, Arm arm, MatrixStack matrices) {
        firstPersonUse(player.getItemUseTime() + tickDelta, arm, matrices);
    }

    public static void firstPersonUse(float ticksUsing, Arm arm, MatrixStack matrices) {
        UseParams params = UseParams.fromTicks(ticksUsing);
        int side = arm == Arm.RIGHT ? 1 : -1;

        matrices.translate(
                side * (params.raiseProgress * 0.15F + params.raiseProgressEnd * -0.05F
                        + params.swayProgress * -0.1F + params.swayScaleSlow * 0.005F),
                params.raiseProgress * -0.075F + params.raiseProgressMiddle * 0.075F
                        + params.swayScaleFast * 0.01F,
                params.raiseProgressStart * 0.05D + params.raiseProgressEnd * -0.05D
                        + params.swayScaleSlow * 0.005D
        );

        matrices.multiply(
                RotationAxis.POSITIVE_X.rotationDegrees(
                        -65.0F * easeInOutBack(params.raiseProgress)
                                - 35.0F * params.lowerProgress
                                + 100.0F * params.raiseBackProgress
                                - 0.5F * params.swayScaleFast
                ),
                0.0F,
                0.1F,
                0.0F
        );
        matrices.multiply(
                RotationAxis.NEGATIVE_Y.rotationDegrees(side * (-90.0F * progress(params.raiseProgress, 0.5F, 0.55F)
                        + 90.0F * params.swayProgress
                        + 2.0F * params.swayScaleSlow)),
                side * 0.15F,
                0.0F,
                0.0F
        );
    }

    public static void firstPersonAttack(float attackTime, MatrixStack matrices, int side) {
        float raise = easeInOutSine(progress(attackTime, 0.0F, 0.05F));
        float pullBack = easeOutBack(progress(attackTime, 0.05F, 0.2F));
        float recover = easeInOutExpo(progress(attackTime, 0.4F, 1.0F));

        matrices.translate(
                side * 0.1F * (raise - pullBack),
                0.2F * (raise - recover),
                0.65F * (raise - pullBack)
        );
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-70.0F * (raise - recover)));
        matrices.translate(0.0D, 0.0D, -0.25D * (recover - pullBack));
    }

    private static float progress(float value, float start, float end) {
        return MathHelper.clamp(MathHelper.getLerpProgress(value, start, end), 0.0F, 1.0F);
    }

    private static float easeInOutSine(float value) {
        return (float) (-(Math.cos(Math.PI * value) - 1.0D) / 2.0D);
    }

    private static float easeOutBack(float value) {
        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;
        return 1.0F + c3 * (float) Math.pow(value - 1.0F, 3.0D) + c1 * (float) Math.pow(value - 1.0F, 2.0D);
    }

    private static float easeInOutExpo(float value) {
        if (value == 0.0F) return 0.0F;
        if (value == 1.0F) return 1.0F;
        return value < 0.5F
                ? (float) Math.pow(2.0D, 20.0F * value - 10.0F) / 2.0F
                : (2.0F - (float) Math.pow(2.0D, -20.0F * value + 10.0F)) / 2.0F;
    }

    private static float easeInOutBack(float value) {
        float c1 = 1.70158F;
        float c2 = c1 * 1.525F;
        return value < 0.5F
                ? (MathHelper.square(2.0F * value) * ((c2 + 1.0F) * 2.0F * value - c2)) / 2.0F
                : (MathHelper.square(2.0F * value - 2.0F) * ((c2 + 1.0F) * (2.0F * value - 2.0F) + c2) + 2.0F) / 2.0F;
    }

    private static float easeInOutElastic(float value) {
        if (value == 0.0F || value == 1.0F) {
            return value;
        }

        float c5 = (float) ((2.0D * Math.PI) / 4.5D);
        return value < 0.5F
                ? -(float) (Math.pow(2.0D, 20.0D * value - 10.0D)
                * Math.sin((20.0D * value - 11.125D) * c5)) / 2.0F
                : (float) (Math.pow(2.0D, -20.0D * value + 10.0D)
                * Math.sin((20.0D * value - 11.125D) * c5)) / 2.0F + 1.0F;
    }

    private static float easeOutCubic(float value) {
        return 1.0F - (float) Math.pow(1.0F - value, 3.0D);
    }

    private static float easeOutCirc(float value) {
        return (float) Math.sqrt(1.0F - Math.pow(value - 1.0F, 2.0D));
    }

    private static float easeInCirc(float value) {
        return 1.0F - (float) Math.sqrt(1.0F - value * value);
    }

    private record UseParams(
            float raiseProgress,
            float raiseProgressStart,
            float raiseProgressMiddle,
            float raiseProgressEnd,
            float swayProgress,
            float lowerProgress,
            float raiseBackProgress,
            float swayIntensity,
            float swayScaleSlow,
            float swayScaleFast
    ) {
        private static UseParams fromTicks(float ticksUsing) {
            float raiseProgress = progress(ticksUsing, 0.0F, DELAY_TICKS);
            float raiseProgressStart = progress(raiseProgress, 0.0F, 0.5F);
            float raiseProgressMiddle = progress(raiseProgress, 0.5F, 0.8F);
            float raiseProgressEnd = progress(raiseProgress, 0.8F, 1.0F);
            float swayProgress = progress(ticksUsing, KNOCKBACK_START_TICKS - 20.0F, KNOCKBACK_END_TICKS - 40.0F);
            float lowerProgress = easeOutCubic(easeInOutElastic(progress(ticksUsing - 20.0F, KNOCKBACK_END_TICKS - 40.0F, KNOCKBACK_END_TICKS)));
            float raiseBackProgress = progress(ticksUsing, DAMAGE_TICKS - 5.0F, DAMAGE_TICKS);
            float swayIntensity = 2.0F * easeOutCirc(swayProgress) - 2.0F * easeInCirc(raiseBackProgress);
            float swayScaleSlow = MathHelper.sin(ticksUsing * 19.0F * MathHelper.RADIANS_PER_DEGREE) * swayIntensity;
            float swayScaleFast = MathHelper.sin(ticksUsing * 30.0F * MathHelper.RADIANS_PER_DEGREE) * swayIntensity;

            return new UseParams(
                    raiseProgress,
                    raiseProgressStart,
                    raiseProgressMiddle,
                    raiseProgressEnd,
                    swayProgress,
                    lowerProgress,
                    raiseBackProgress,
                    swayIntensity,
                    swayScaleSlow,
                    swayScaleFast
            );
        }
    }
}