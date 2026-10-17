package dev.baimaodaxia.asm.mixins;

import dev.baimaodaxia.earthquake;
import dev.baimaodaxia.api.events.impl.HeldItemRendererEvent;
import net.minecraft.util.Hand;
import net.minecraft.item.ItemStack;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.entity.LivingEntity;

@Mixin({HeldItemRenderer.class})
public abstract class MixinHeldItemRenderer {
   @Inject(
      method = {"renderFirstPersonItem"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"
)}
   )
   private void onRenderItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
      HeldItemRendererEvent event = HeldItemRendererEvent.get(hand, item, equipProgress, matrices);
      earthquake.EVENT_BUS.post(event);
   }
}
