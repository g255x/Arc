package dev.g255.arc.asm.mixins;

import net.minecraft.entity.LivingEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({LivingEntityRenderer.class})
public interface MixinLivingEntityChams {
   @Invoker("setupTransforms")
   void chams$invokeSetupTransforms(LivingEntity var1, MatrixStack var2, float var3, float var4, float var5, float var6);

   @Invoker("scale")
   void chams$invokeScale(LivingEntity var1, MatrixStack var2, float var3);

   @Accessor("model")
   EntityModel chams$getModel();
}
