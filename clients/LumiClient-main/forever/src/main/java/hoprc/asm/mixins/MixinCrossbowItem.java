/*
 * This file is part of the Forever Client distribution.
 */

package hoprc.asm.mixins;

import hoprc.asm.accessors.CrossbowItemAccessor;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CrossbowItem.class)
public class MixinCrossbowItem {

    @Inject(method = "usageTick", at = @At("TAIL"))
    private void onUsageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks, CallbackInfo ci) {
        if (!world.isClient()) {
            float f = (float)(stack.getMaxUseTime(user) - remainingUseTicks) / (float)CrossbowItem.getPullTime(stack, user);
            if (f >= 1.0F && !CrossbowItem.isCharged(stack)) {
                CrossbowItemAccessor.callLoadProjectiles(user, stack);
            }
        }
    }
}