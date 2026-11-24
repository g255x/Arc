package dev.g255.arc.asm.accessors;

import net.minecraft.client.render.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({RenderLayer.class})
public interface IRenderLayer {
   @Accessor("ARMOR_ENTITY_GLINT")
   static RenderLayer chams$getArmorEntityGlint() {
      throw new AssertionError();
   }
}
