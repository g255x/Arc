package dev.perish.asm.mixins;

import dev.perish.api.utils.Wrapper;
import dev.perish.mod.modules.impl.movement.FakeFlight;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerEntityRenderer.class)
public abstract class MixinPlayerEntityRenderer implements Wrapper {

    @Redirect(
            method = "setupTransforms",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;isFallFlying()Z")
    )
    private boolean cancelFakeFlightFallFlyingVisual(AbstractClientPlayerEntity player) {
        if (player == mc.player
                && FakeFlight.INSTANCE != null
                && FakeFlight.INSTANCE.isOn()
                && FakeFlight.INSTANCE.fly) {
            return false;
        }
        return player.isFallFlying();
    }
}
