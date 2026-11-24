package dev.g255.arc.asm.accessors;

import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({LivingEntityRenderer.class})
public interface ILivingEntityRenderer {
   @Invoker("addFeature")
   boolean invokeAddFeature(FeatureRenderer<?, ?> var1);
}
