package hoprc.asm.accessors;

import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TitleScreen.class)
public interface ITitleScreenFade {
    @Accessor("doBackgroundFade")
    boolean forever$getDoBackgroundFade();

    @Accessor("backgroundFadeStart")
    long forever$getBackgroundFadeStart();
}
