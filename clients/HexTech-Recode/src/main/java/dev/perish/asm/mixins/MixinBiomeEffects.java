package dev.perish.asm.mixins;

import dev.perish.mod.modules.impl.render.Ambience;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.*;
import net.minecraft.world.biome.BiomeEffects;

@Mixin(BiomeEffects.class)
public class MixinBiomeEffects {
    @Inject(method = "getSkyColor", at = @At("HEAD"), cancellable = true)
    private void hookGetSkyColor(CallbackInfoReturnable<Integer> cir) {
        if (Ambience.INSTANCE.isOn() && Ambience.INSTANCE.sky.booleanValue) {
            Color sky = Ambience.INSTANCE.sky.getValue();
            cir.setReturnValue(sky.getRGB());
        }
    }
}
