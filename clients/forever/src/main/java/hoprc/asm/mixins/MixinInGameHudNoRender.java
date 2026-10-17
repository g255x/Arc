package hoprc.asm.mixins;

import dev.kissoo.mod.modules.impl.render.NoRender;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.client.gui.hud.InGameHud$HeartType")
public class MixinInGameHudNoRender {

    @Redirect(
            method = "fromPlayerState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;hasStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Z"
            )
    )
    private static boolean redirectHasStatusEffect(PlayerEntity player, RegistryEntry<StatusEffect> effect) {
        if (effect == StatusEffects.WITHER) {
            if (NoRender.INSTANCE != null && NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.witherHearts.getValue()) {
                return false;
            }
        }
        return player.hasStatusEffect(effect);
    }
}