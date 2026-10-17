package dev.perish.asm.accessors;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.registry.entry.RegistryEntry.Reference;

@Mixin(SimpleRegistry.class)
public interface ISimpleRegistry<T> {
    @Accessor("valueToEntry")
    Map<T, Reference<T>> getValueToEntry();
}
