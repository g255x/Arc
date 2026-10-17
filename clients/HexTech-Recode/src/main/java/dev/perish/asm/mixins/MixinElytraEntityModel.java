package dev.perish.asm.mixins;

import dev.perish.api.utils.Wrapper;
import dev.perish.mod.modules.impl.movement.FakeFlight;
import net.minecraft.client.render.entity.model.ElytraEntityModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ElytraEntityModel.class)
public abstract class MixinElytraEntityModel<T extends LivingEntity> implements Wrapper {

    @Redirect(
            method = "setAngles",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isFallFlying()Z")
    )
    private boolean cancelFakeFlightElytraSpread(LivingEntity entity) {
        if (entity == mc.player
                && FakeFlight.INSTANCE != null
                && FakeFlight.INSTANCE.isOn()
                && FakeFlight.INSTANCE.fly) {
            return false;
        }
        return entity.isFallFlying();
    }
}
