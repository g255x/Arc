package dev.perish.asm.accessors;

import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.component.type.ContainerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ContainerComponent.class)
public interface IContainerComponent {
    @Accessor
    DefaultedList<ItemStack> getStacks();
}
