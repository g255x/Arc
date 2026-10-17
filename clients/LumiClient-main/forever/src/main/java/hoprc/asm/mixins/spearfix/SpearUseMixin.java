package hoprc.asm.mixins.spearfix;

import dev.kissoo.mod.spearfix.SpearFix;
import hoprc.obf.mixin.MixinObfuscate;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@MixinObfuscate
@Mixin(ItemStack.class)
public class SpearUseMixin {
    @Inject(method = "getMaxUseTime", at = @At("HEAD"), cancellable = true)
    private void spearfix$getMaxUseTime(LivingEntity user, CallbackInfoReturnable<Integer> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (SpearFix.isRemappedSpear(self)) {
            cir.setReturnValue(72000);
        }
    }
}
