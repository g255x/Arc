package hoprc.asm.mixins;

import dev.kissoo.mod.modules.impl.render.Ambience;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldRenderer.class)
public abstract class MixinWeatherPenetrate {

    @Redirect(
            method = "renderWeather",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;getTopY(Lnet/minecraft/world/Heightmap$Type;II)I"
            )
    )
    private int redirectGetTopY(
            World world,
            Heightmap.Type heightmap,
            int x,
            int z
    ) {
        if (Ambience.INSTANCE != null && Ambience.INSTANCE.shouldPenetrate()) {
            return -64;
        }
        return world.getTopY(heightmap, x, z);
    }
}
