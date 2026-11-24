package dev.g255.arc.asm.accessors;

import net.minecraft.client.render.entity.model.AnimalModel;
import net.minecraft.client.model.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({AnimalModel.class})
public interface IAnimalModel {
   @Invoker("getHeadParts")
   Iterable<ModelPart> chams$getHeadParts();

   @Invoker("getBodyParts")
   Iterable<ModelPart> chams$getBodyParts();
}
