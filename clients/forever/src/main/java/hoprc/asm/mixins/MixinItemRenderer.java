package hoprc.asm.mixins;

import dev.kissoo.api.utils.render.SimpleItemModel;
import dev.kissoo.mod.spearfix.SpearFix;
import dev.kissoo.mod.spearfix.SpearModelLoadingPlugin;
import dev.kissoo.mod.spearfix.render.SpearModelWrapper;
import dev.kissoo.mod.modules.impl.render.NoRender;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.item.ItemModels;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public class MixinItemRenderer {
   @Shadow
   private ItemModels models;

   @Unique
   private SimpleItemModel flattenedModel;
   @Unique
   private ModelTransformationMode renderMode;

   @Inject(method = "renderItem*", at = @At("HEAD"))
   private void getRenderType(
      ItemStack itemStack,
      ModelTransformationMode transformationMode,
      boolean leftHand,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light,
      int overlay,
      BakedModel model,
      CallbackInfo ci
   ) {
      this.renderMode = transformationMode;
   }

   @ModifyVariable(
           method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
           at = @At("HEAD"),
           ordinal = 0,
           argsOnly = true,
           require = 0
   )
   private BakedModel spearfix$swapModelBeforeTransforms(BakedModel model, ItemStack stack, ModelTransformationMode transformationMode) {
      return this.spearfix$wrapSpearModel(model, stack, transformationMode);
   }

   @ModifyVariable(method = "renderBakedItemModel", at = @At("HEAD"), index = 1, argsOnly = true, require = 0)
   private BakedModel replaceItemModelClass(
      BakedModel model, BakedModel arg, ItemStack stack, int light, int overlay, MatrixStack matrices, VertexConsumer vertices
   ) {
      if (NoRender.INSTANCE != null
         && NoRender.INSTANCE.isOn()
         && NoRender.INSTANCE.fastItem.getValue()
         && !NoRender.INSTANCE.renderSidesOfItems.getValue()
         && !stack.isEmpty()
         && !model.hasDepth()
         && this.renderMode == ModelTransformationMode.GROUND) {
         if (this.flattenedModel == null) {
            this.flattenedModel = new SimpleItemModel();
         }
         this.flattenedModel.setItem(model);
         return this.flattenedModel;
      } else {
         return model;
      }
   }

   @Unique
   private BakedModel spearfix$wrapSpearModel(BakedModel model, ItemStack stack, ModelTransformationMode transformationMode) {
      String spearType = SpearFix.getSpearType(stack);
      if (spearType != null && transformationMode != null) {
         Identifier modelId = SpearModelLoadingPlugin.SPEAR_MODEL_IDS.get(spearType);
         Identifier inHandModelId = SpearModelLoadingPlugin.SPEAR_IN_HAND_MODEL_IDS.get(spearType);
         if (modelId != null && inHandModelId != null) {
            BakedModel spearModel = ((FabricBakedModelManager) this.models.getModelManager()).getModel(modelId);
            BakedModel spearInHandModel = ((FabricBakedModelManager) this.models.getModelManager()).getModel(inHandModelId);
            BakedModel originalModel = this.models.getModel(stack.getItem());
            return new SpearModelWrapper(spearModel, spearInHandModel, originalModel, transformationMode);
         }
      }
      return model;
   }
}
