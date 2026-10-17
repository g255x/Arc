package dev.perish.asm.mixins;

import dev.perish.mod.modules.impl.render.NameTag2D;
import dev.perish.mod.modules.impl.render.NameTags;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer<T extends Entity> {
    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void onRenderLabel(T entity, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float tickDelta, CallbackInfo ci) {
        if (entity instanceof PlayerEntity && (NameTags.INSTANCE.isOn() || NameTag2D.INSTANCE.isOn())) {
            ci.cancel();
        }
    }
}
