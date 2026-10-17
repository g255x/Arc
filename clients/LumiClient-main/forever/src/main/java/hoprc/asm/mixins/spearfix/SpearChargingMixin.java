package hoprc.asm.mixins.spearfix;

import dev.kissoo.mod.spearfix.SpearFix;
import hoprc.obf.mixin.MixinObfuscate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@MixinObfuscate
@Mixin(LivingEntity.class)
public class SpearChargingMixin {
    @Inject(method = "isUsingItem", at = @At("HEAD"), cancellable = true)
    private void spearfix$isUsingItem(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (self != mc.player) return;

        ItemStack mainHand = self.getMainHandStack();
        if (!SpearFix.isRemappedSpear(mainHand)) return;

        if (mc.options.useKey.isPressed()) {
            cir.setReturnValue(true);
        }
    }
}
