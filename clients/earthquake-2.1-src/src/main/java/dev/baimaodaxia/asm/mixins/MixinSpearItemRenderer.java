package dev.baimaodaxia.asm.mixins;

import dev.baimaodaxia.mod.features.SpearPort;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.util.Identifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemModels;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.render.item.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ItemRenderer.class})
public abstract class MixinSpearItemRenderer {
   @Shadow
   public abstract ItemModels getModels();

   @Inject(
      method = {"renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;III)V"},
      at = {@At("HEAD")}
   )
   private void onBeginPortedSpearRenderMode(LivingEntity entity, ItemStack stack, ModelTransformationMode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, World world, int light, int overlay, int seed, CallbackInfo ci) {
      if (SpearPort.isSpear(stack) && isHandRenderMode(renderMode)) {
         SpearPort.setInHandRender(true);
      }

   }

   @Inject(
      method = {"renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;III)V"},
      at = {@At("RETURN")}
   )
   private void onEndPortedSpearRenderMode(LivingEntity entity, ItemStack stack, ModelTransformationMode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, World world, int light, int overlay, int seed, CallbackInfo ci) {
      if (SpearPort.isSpear(stack) && isHandRenderMode(renderMode)) {
         SpearPort.setInHandRender(false);
      }

   }

   @Inject(
      method = {"getModel"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onUsePortedSpearModel(ItemStack stack, World world, LivingEntity entity, int seed, CallbackInfoReturnable<BakedModel> cir) {
      Identifier modelId = SpearPort.getSpearModelId(stack);
      if (modelId != null) {
         BakedModel model = this.getModels().getModelManager().getModel(ModelIdentifier.ofInventoryVariant(modelId));
         if (model != this.getModels().getModelManager().getMissingModel()) {
            cir.setReturnValue(model);
            return;
         }
      }

      ItemStack visualStack = SpearPort.getVisualStack(stack);
      if (visualStack != stack) {
         cir.setReturnValue(this.getModels().getModel(visualStack));
      }

   }

   private static boolean isHandRenderMode(ModelTransformationMode renderMode) {
      return renderMode == ModelTransformationMode.FIRST_PERSON_RIGHT_HAND || renderMode == ModelTransformationMode.FIRST_PERSON_LEFT_HAND || renderMode == ModelTransformationMode.THIRD_PERSON_RIGHT_HAND || renderMode == ModelTransformationMode.THIRD_PERSON_LEFT_HAND;
   }
}
