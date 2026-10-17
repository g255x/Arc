package dev.perish.asm.mixins;

import dev.perish.mod.modules.impl.render.SkyColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.Color;
import net.minecraft.client.render.DimensionEffects;
import net.minecraft.util.math.Vec3d;

// adjustFogColor / useThickFog 在 DimensionEffects 上是抽象的，注入不进去，
// 只能挂到三个具体子类上。这两个方法原先写在 MixinClientWorld 里，
// 既没 @Inject 也没 override 到任何东西，等于死代码——ThickFog 一直是个空设置。
@Mixin({DimensionEffects.Overworld.class, DimensionEffects.Nether.class, DimensionEffects.End.class})
public class MixinDimensionEffectsImpl {
    @Inject(method = "useThickFog", at = @At("HEAD"), cancellable = true)
    private void hookUseThickFog(int camX, int camY, CallbackInfoReturnable<Boolean> cir) {
        if (SkyColor.INSTANCE != null && SkyColor.INSTANCE.isActive() && SkyColor.INSTANCE.usesCustomEffects()) {
            cir.setReturnValue(SkyColor.INSTANCE.thickFog.getValue());
        }
    }

    @Inject(method = "adjustFogColor", at = @At("HEAD"), cancellable = true)
    private void hookAdjustFogColor(Vec3d color, float sunHeight, CallbackInfoReturnable<Vec3d> cir) {
        // 各维度会各自改雾色：末地乘 0.15 压暗，主世界按太阳高度衰减。
        // 自定义模式下直接用设置里的雾色，免得被维度自己的调整吃掉
        if (SkyColor.INSTANCE != null && SkyColor.INSTANCE.isActive() && SkyColor.INSTANCE.usesCustomEffects()) {
            Color fog = SkyColor.INSTANCE.fogColor.getValue();
            cir.setReturnValue(Vec3d.unpackRgb(fog.getRGB()));
        }
    }
}
