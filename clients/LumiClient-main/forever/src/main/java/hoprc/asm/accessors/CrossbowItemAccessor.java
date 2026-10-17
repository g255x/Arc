/*
 * This file is part of the Forever Client distribution.
 */

package hoprc.asm.accessors;

import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CrossbowItem.class)
public interface CrossbowItemAccessor {

    @Invoker("getSpeed")
    static float getSpeed(ChargedProjectilesComponent itemStack) {
        throw new AssertionError();
    }

    @Invoker("loadProjectiles")
    static boolean callLoadProjectiles(LivingEntity shooter, ItemStack crossbow) {
        throw new AssertionError();
    }
}