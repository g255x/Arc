package dev.luminous.asm.accessors;

import java.util.List;
import java.util.Map;
import net.minecraft.class_630;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({class_630.class})
public interface IModelPart {
   @Accessor("cuboids")
   List getCuboids();

   @Accessor("children")
   Map getChildren();
}
