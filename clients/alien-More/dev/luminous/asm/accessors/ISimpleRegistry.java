package dev.luminous.asm.accessors;

import java.util.Map;
import net.minecraft.class_2370;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({class_2370.class})
public interface ISimpleRegistry {
   @Accessor("valueToEntry")
   Map getValueToEntry();
}
