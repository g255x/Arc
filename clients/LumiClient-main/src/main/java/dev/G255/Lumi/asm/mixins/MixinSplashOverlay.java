package dev.G255.Lumi.asm.mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Overlay;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SplashOverlay.class)
public abstract class MixinSplashOverlay extends Overlay {

    @Shadow
    private float progress;

    @Shadow
    private long reloadCompleteTime;

    @Shadow
    @Final
    private MinecraftClient client;

    protected MixinSplashOverlay() {
        super();
    }

    @Inject(method = "render", at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;disableDepthTest()V",
            ordinal = 0,
            remap = false
    ))
    private void renderHook(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        int width = this.client.getWindow().getScaledWidth();
        int height = this.client.getWindow().getScaledHeight();

        int alpha = MathHelper.ceil(
                (1.0F - MathHelper.clamp(
                        (this.reloadCompleteTime > -1L
                                ? (float) (Util.getMeasuringTimeMs() - this.reloadCompleteTime) / 1000.0F
                                : -1.0F) - 1.0F,
                        0.0F, 1.0F)) * 255.0F
        );

        float gray = 0.05f + this.progress * 0.55f;
        int c = (int) (gray * 255);
        context.fill(0, 0, width, height, (alpha << 24) | (c << 16) | (c << 8) | c);
    }
}