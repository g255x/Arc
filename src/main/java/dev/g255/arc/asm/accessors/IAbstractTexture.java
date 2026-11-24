package dev.g255.arc.asm.accessors;

import net.minecraft.client.texture.AbstractTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({AbstractTexture.class})
public interface IAbstractTexture {
   /**
    * Reads the raw {@code glId} field.
    *
    * <p>The method is deliberately <em>not</em> named {@code getGlId}: {@code AbstractTexture}
    * already declares a public {@code getGlId()} that lazily allocates the GL texture name. A
    * generated accessor with the same name shadows that method, so the allocation never happens,
    * {@code glId} stays {@code -1} and every {@code bindTexture()} call ends up binding an invalid
    * texture name - which corrupts all texture uploads and floods the log with
    * {@code GL_INVALID_VALUE}.</p>
    */
   @Accessor("glId")
   int arc$getGlIdRaw();
}
