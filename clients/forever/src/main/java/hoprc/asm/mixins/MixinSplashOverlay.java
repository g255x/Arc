package hoprc.asm.mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.resource.ResourceReload;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(SplashOverlay.class)
public abstract class MixinSplashOverlay {

    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private ResourceReload reload;
    @Shadow @Final private Consumer<Optional<Throwable>> exceptionHandler;
    @Shadow @Final private boolean reloading;
    @Shadow private float progress;
    @Shadow private long reloadCompleteTime;
    @Shadow private long reloadStartTime;

    @Overwrite
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        long l = Util.getMeasuringTimeMs();

        if (this.reloading && this.reloadStartTime == -1L) {
            this.reloadStartTime = l;
        }

        float g = this.reloadStartTime > -1L
                ? (float)(l - this.reloadStartTime) / 500.0F
                : -1.0F;

        context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), 0xFF000000);

        if (this.reloadCompleteTime == -1L
                && this.reload.isComplete()
                && (!this.reloading || g >= 2.0F)) {
            try {
                this.reload.throwException();
                this.exceptionHandler.accept(Optional.empty());
            } catch (Throwable throwable) {
                this.exceptionHandler.accept(Optional.of(throwable));
            }
            this.reloadCompleteTime = Util.getMeasuringTimeMs();
            if (this.client.currentScreen != null) {
                this.client.currentScreen.init(
                        this.client,
                        context.getScaledWindowWidth(),
                        context.getScaledWindowHeight()
                );
            }
            this.client.setOverlay(null);
        }
    }
}
