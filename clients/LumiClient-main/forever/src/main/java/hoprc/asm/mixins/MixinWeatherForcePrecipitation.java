package hoprc.asm.mixins;

import dev.kissoo.mod.modules.impl.render.Ambience;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Biome.class)
public abstract class MixinWeatherForcePrecipitation {

    @Inject(
            method = "hasPrecipitation",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forceHasPrecipitation(CallbackInfoReturnable<Boolean> cir) {
        if (Ambience.INSTANCE == null) return;
        if (!Ambience.INSTANCE.isEnabled()) return;
        if (!Ambience.INSTANCE.customWeather.getValue()) return;
        if (Ambience.INSTANCE.weatherType.getValue() == Ambience.WeatherType.CLEAR) return;
        cir.setReturnValue(true);
    }

    @Inject(
            method = "getPrecipitation",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forcePrecipitationType(
            BlockPos pos,
            CallbackInfoReturnable<Biome.Precipitation> cir
    ) {
        if (Ambience.INSTANCE == null) return;
        if (!Ambience.INSTANCE.isEnabled()) return;
        if (!Ambience.INSTANCE.customWeather.getValue()) return;

        Ambience.WeatherType type = Ambience.INSTANCE.getWeatherType();

        if (type == Ambience.WeatherType.CLEAR) {
            cir.setReturnValue(Biome.Precipitation.NONE);
        } else if (type == Ambience.WeatherType.RAIN) {
            cir.setReturnValue(Biome.Precipitation.RAIN);
        } else if (type == Ambience.WeatherType.SNOW) {
            cir.setReturnValue(Biome.Precipitation.SNOW);
        } else if (type == Ambience.WeatherType.STORM) {
            cir.setReturnValue(Biome.Precipitation.RAIN);
        }
    }
}
