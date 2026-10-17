package hoprc.asm.mixins;

import hoprc.api.utils.memory.FramebufferLeakTracker;
import net.minecraft.client.gl.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Framebuffer.class)
public abstract class MixinFramebuffer {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(boolean useDepth, CallbackInfo ci) {
        FramebufferLeakTracker.INSTANCE.onFramebufferCreated(
            (Framebuffer) (Object) this
        );
    }

    @Inject(method = "delete", at = @At("HEAD"))
    private void onDelete(CallbackInfo ci) {
        FramebufferLeakTracker.INSTANCE.onFramebufferDeleted(
            (Framebuffer) (Object) this
        );
    }

    @Inject(method = "beginWrite", at = @At("HEAD"))
    private void onBeginWrite(boolean joinViewport, CallbackInfo ci) {
        FramebufferLeakTracker.INSTANCE.onFramebufferAccessed(
            (Framebuffer) (Object) this
        );
    }

    @Inject(method = "beginRead", at = @At("HEAD"))
    private void onBeginRead(CallbackInfo ci) {
        FramebufferLeakTracker.INSTANCE.onFramebufferAccessed(
            (Framebuffer) (Object) this
        );
    }

    @Inject(method = "endWrite", at = @At("HEAD"))
    private void onEndWrite(CallbackInfo ci) {
        FramebufferLeakTracker.INSTANCE.onFramebufferAccessed(
            (Framebuffer) (Object) this
        );
    }

    @Inject(method = "endRead", at = @At("HEAD"))
    private void onEndRead(CallbackInfo ci) {
        FramebufferLeakTracker.INSTANCE.onFramebufferAccessed(
            (Framebuffer) (Object) this
        );
    }
}