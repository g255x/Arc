package dev.perish.asm.mixins;

import dev.perish.mod.modules.impl.render.Ambience;
import dev.perish.mod.modules.impl.render.SkyColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.*;
import net.minecraft.client.render.DimensionEffects;

@Mixin(DimensionEffects.class)
public class MixinDimensionEffects {
    @Inject(method = "getFogColorOverride", at = @At("HEAD"), cancellable = true)
    private void hookGetFogColorOverride(float skyAngle, float tickDelta, CallbackInfoReturnable<float[]> cir) {
        if (Ambience.INSTANCE.isOn() && Ambience.INSTANCE.dimensionColor.booleanValue) {
            Color color = Ambience.INSTANCE.dimensionColor.getValue();
            cir.setReturnValue(new float[]{color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, 1.0F});
        }
        if (SkyColor.INSTANCE != null && SkyColor.INSTANCE.isActive()) {
            Color color = SkyColor.INSTANCE.fogColor.getValue();
            cir.setReturnValue(new float[]{(float)color.getRed() / 255.0f, (float)color.getGreen() / 255.0f, (float)color.getBlue() / 255.0f, 1.0f});
        }
    }

    // WorldRenderer#renderSky 按 SkyType 分发：END 走 renderEndSky 的固定贴图，
    // NONE（下界）直接不画，只有 NORMAL 才会去取 ClientWorld#getSkyColor。
    // 由模块决定顶成哪种，这也是 SkyMode.End 唯一的落地点。
    @Inject(method = "getSkyType", at = @At("HEAD"), cancellable = true)
    private void hookGetSkyType(CallbackInfoReturnable<DimensionEffects.SkyType> cir) {
        if (SkyColor.INSTANCE == null) {
            return;
        }
        DimensionEffects.SkyType forced = SkyColor.INSTANCE.getForcedSkyType();
        if (forced != null) {
            cir.setReturnValue(forced);
        }
    }
}
