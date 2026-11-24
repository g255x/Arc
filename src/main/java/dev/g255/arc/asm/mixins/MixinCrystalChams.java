package dev.g255.arc.asm.mixins;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({EndCrystalEntityRenderer.class})
public interface MixinCrystalChams {
   @Accessor("frame")
   ModelPart chams$getFrame();

   @Accessor("bottom")
   ModelPart chams$getBottom();
}
