package hoprc.asm.mixins;

import dev.kissoo.Forever;
import dev.kissoo.api.events.impl.HeldItemRendererEvent;
import dev.kissoo.mod.modules.impl.combat.CrystalBot;
import dev.kissoo.mod.modules.impl.render.ViewModel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class MixinHeldItemRenderer {

   @Final
   @Shadow
   private MinecraftClient client;

   private static final ThreadLocal<Hand> capturedHand = new ThreadLocal<>();

   @Inject(
           method = "renderFirstPersonItem",
           at = @At("HEAD")
   )
   private void captureHand(
           AbstractClientPlayerEntity player,
           float tickDelta,
           float pitch,
           Hand hand,
           float swingProgress,
           ItemStack item,
           float equipProgress,
           MatrixStack matrices,
           VertexConsumerProvider vertexConsumers,
           int light,
           CallbackInfo ci
   ) {
      capturedHand.set(hand);

      ViewModel viewModel = ViewModel.INSTANCE;
      if (viewModel != null && viewModel.isOn() && viewModel.noSway.getValue()) {
         matrices.push();
         if (this.client.player != null) {
            float pitchRotation = (this.client.player.getPitch(tickDelta) -
                    MathHelper.lerp(tickDelta,
                            this.client.player.lastRenderPitch,
                            this.client.player.renderPitch)) * 0.1F;
            float yawRotation = (this.client.player.getYaw(tickDelta) -
                    MathHelper.lerp(tickDelta,
                            this.client.player.lastRenderYaw,
                            this.client.player.renderYaw)) * 0.1F;
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-pitchRotation));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yawRotation));
         }
      }
   }

   @ModifyVariable(
           method = "renderFirstPersonItem",
           at = @At("HEAD"),
           argsOnly = true,
           ordinal = 0
   )
   private ItemStack crystalBot_spoofItem(ItemStack item) {
      Hand hand = capturedHand.get();
      if (hand != Hand.MAIN_HAND) return item;

      CrystalBot cb = CrystalBot.INSTANCE;
      if (cb == null) return item;

      ItemStack spoofed = cb.getSpoofItem();
      return spoofed != null ? spoofed : item;
   }


   @Inject(
           method = "renderFirstPersonItem",
           at = @At("RETURN")
   )
   private void onRenderFirstPersonItemEnd(
           AbstractClientPlayerEntity player,
           float tickDelta,
           float pitch,
           Hand hand,
           float swingProgress,
           ItemStack item,
           float equipProgress,
           MatrixStack matrices,
           VertexConsumerProvider vertexConsumers,
           int light,
           CallbackInfo ci
   ) {
      ViewModel viewModel = ViewModel.INSTANCE;
      if (viewModel != null && viewModel.isOn() && viewModel.noSway.getValue()) {
         matrices.pop();
      }
   }


   @Inject(
           method = "applySwingOffset",
           at = @At("HEAD"),
           cancellable = true
   )
   private void onApplySwingOffset(
           MatrixStack matrices,
           Arm arm,
           float swingProgress,
           CallbackInfo ci
   ) {
      ViewModel viewModel = ViewModel.INSTANCE;
      if (viewModel != null && viewModel.isOn() && viewModel.noSway.getValue()) {
         int i = arm == Arm.RIGHT ? 1 : -1;
         float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * (45.0F + f * -20.0F)));
         float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) i * g * -20.0F));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80.0F));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * -45.0F));
         ci.cancel();
      }
   }


   @Inject(
           method = "renderFirstPersonItem",
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(" +
                           "Lnet/minecraft/entity/LivingEntity;" +
                           "Lnet/minecraft/item/ItemStack;" +
                           "Lnet/minecraft/client/render/model/json/ModelTransformationMode;" +
                           "ZLnet/minecraft/client/util/math/MatrixStack;" +
                           "Lnet/minecraft/client/render/VertexConsumerProvider;I)V"
           )
   )
   private void onRenderItem(
           AbstractClientPlayerEntity player,
           float tickDelta,
           float pitch,
           Hand hand,
           float swingProgress,
           ItemStack item,
           float equipProgress,
           MatrixStack matrices,
           VertexConsumerProvider vertexConsumers,
           int light,
           CallbackInfo ci
   ) {
      HeldItemRendererEvent event = HeldItemRendererEvent.get(hand, item, equipProgress, matrices);
      Forever.EVENT_BUS.post(event);
   }
}