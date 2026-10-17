package hoprc.asm.mixins;

import dev.kissoo.mod.modules.impl.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinMotionBlur {

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/WorldRenderer;drawEntityOutlinesFramebuffer()V",
                    shift = At.Shift.AFTER
            )
    )
    private void onRender(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        if (Camera.INSTANCE != null && Camera.INSTANCE.isMotionBlurOn()) {
            Camera.INSTANCE.renderMotionBlur(tickCounter.getTickDelta(true));
        }
    }
}