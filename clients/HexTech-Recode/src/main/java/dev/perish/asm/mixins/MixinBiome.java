package dev.perish.asm.mixins;

import dev.perish.mod.modules.impl.render.Ambience;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.*;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.Precipitation;

@Mixin(Biome.class)
public class MixinBiome {
    @Inject(method = "getFogColor", at = @At("HEAD"), cancellable = true)
    private void hookGetFogColor(CallbackInfoReturnable<Integer> cir) {
        if (Ambience.INSTANCE.isOn() && Ambience.INSTANCE.dimensionColor.booleanValue) {
            Color color = Ambience.INSTANCE.dimensionColor.getValue();
            cir.setReturnValue(color.getRGB());
        }
    }

    @Inject(method = "hasPrecipitation", at = @At("HEAD"), cancellable = true)
    private void hookHasPrecipitation(CallbackInfoReturnable<Boolean> cir) {
        if (dev.perish.mod.modules.impl.render.Weather.INSTANCE.isOn()) {
            if (dev.perish.mod.modules.impl.render.Weather.INSTANCE.mode.getValue() != dev.perish.mod.modules.impl.render.Weather.Mode.No) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "getPrecipitation", at = @At("HEAD"), cancellable = true)
    private void hookGetPrecipitation(net.minecraft.util.math.BlockPos pos, CallbackInfoReturnable<Precipitation> cir) {
        if (dev.perish.mod.modules.impl.render.Weather.INSTANCE.isOn()) {
            switch (dev.perish.mod.modules.impl.render.Weather.INSTANCE.mode.getValue()) {
                case Snow -> cir.setReturnValue(Precipitation.SNOW);
                case Rain -> cir.setReturnValue(Precipitation.RAIN);
                case No -> cir.setReturnValue(Precipitation.NONE);
            }
        }
    }

    @Inject(method = "isCold", at = @At("HEAD"), cancellable = true)
    private void hookIsCold(net.minecraft.util.math.BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (dev.perish.mod.modules.impl.render.Weather.INSTANCE.isOn()) {
            if (dev.perish.mod.modules.impl.render.Weather.INSTANCE.mode.getValue() == dev.perish.mod.modules.impl.render.Weather.Mode.Snow) {
                cir.setReturnValue(true);
            } else if (dev.perish.mod.modules.impl.render.Weather.INSTANCE.mode.getValue() == dev.perish.mod.modules.impl.render.Weather.Mode.Rain) {
                cir.setReturnValue(false);
            }
        }
    }
}
