package dev.g255.arc.asm.mixins;

import dev.g255.arc.api.utils.Wrapper;
import dev.g255.arc.api.utils.math.MathUtil;
import dev.g255.arc.core.impl.RotationManager;
import dev.g255.arc.mod.modules.impl.client.ClientSetting;
import dev.g255.arc.mod.modules.impl.render.NoRender;
import java.awt.Color;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin({LivingEntityRenderer.class})
public abstract class MixinLivingEntityRenderer<T extends LivingEntity, M extends EntityModel<T>> {
   @Unique
   private LivingEntity lastEntity;
   @Unique
   private float originalYaw;
   @Unique
   private float originalHeadYaw;
   @Unique
   private float originalBodyYaw;
   @Unique
   private float originalPitch;
   @Unique
   private float originalPrevYaw;
   @Unique
   private float originalPrevHeadYaw;
   @Unique
   private float originalPrevBodyYaw;

   @Unique
   private boolean arc$rotationOverridden;

   @Inject(
      method = {"render*"},
      at = {@At("HEAD")}
   )
   public void onRenderPre(T livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
      this.arc$rotationOverridden = false;
      if (MinecraftClient.getInstance().player != null && livingEntity == MinecraftClient.getInstance().player && ClientSetting.INSTANCE.rotations.getValue()) {
         float yaw = RotationManager.getRenderYawOffset();
         float headYaw = RotationManager.getRotationYawHead();
         float bodyYaw = RotationManager.getRenderYawOffset();
         float pitch = RotationManager.getRenderPitch();
         float prevYaw = RotationManager.getPrevRenderYawOffset();
         float prevHeadYaw = RotationManager.getPrevRotationYawHead();
         float prevPitch = RotationManager.getPrevRenderPitch();
         // 窗口最小化等情况下旋转数据可能变成 NaN，直接 setYaw 会让原版每帧刷
         // "Invalid entity rotation: NaN, discarding."，这里先拦一次
         if (!Float.isFinite(yaw) || !Float.isFinite(headYaw) || !Float.isFinite(bodyYaw) || !Float.isFinite(pitch)
               || !Float.isFinite(prevYaw) || !Float.isFinite(prevHeadYaw) || !Float.isFinite(prevPitch)) {
            this.lastEntity = livingEntity;
            return;
         }

         this.originalYaw = livingEntity.getYaw();
         this.originalHeadYaw = livingEntity.headYaw;
         this.originalBodyYaw = livingEntity.bodyYaw;
         this.originalPitch = livingEntity.getPitch();
         this.originalPrevYaw = livingEntity.prevYaw;
         this.originalPrevHeadYaw = livingEntity.prevHeadYaw;
         this.originalPrevBodyYaw = livingEntity.prevBodyYaw;
         if (!Float.isFinite(this.originalYaw) || !Float.isFinite(this.originalPitch)) {
            this.lastEntity = livingEntity;
            return;
         }

         livingEntity.setYaw(yaw);
         livingEntity.headYaw = headYaw;
         livingEntity.bodyYaw = bodyYaw;
         livingEntity.setPitch(pitch);
         livingEntity.prevYaw = prevYaw;
         livingEntity.prevHeadYaw = prevHeadYaw;
         livingEntity.prevBodyYaw = prevYaw;
         livingEntity.prevPitch = prevPitch;
         this.arc$rotationOverridden = true;
      }

      this.lastEntity = livingEntity;
   }

   @Inject(
      method = {"render*"},
      at = {@At("TAIL")}
   )
   public void onRenderPost(T livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
      if (this.arc$rotationOverridden && MinecraftClient.getInstance().player != null && livingEntity == MinecraftClient.getInstance().player && ClientSetting.INSTANCE.rotations.getValue()) {
         livingEntity.setYaw(this.originalYaw);
         livingEntity.headYaw = this.originalHeadYaw;
         livingEntity.bodyYaw = this.originalBodyYaw;
         livingEntity.setPitch(this.originalPitch);
         livingEntity.prevYaw = this.originalPrevYaw;
         livingEntity.prevHeadYaw = this.originalPrevHeadYaw;
         livingEntity.prevBodyYaw = this.originalPrevBodyYaw;
         livingEntity.prevPitch = this.originalPitch;
      }

      this.arc$rotationOverridden = false;
   }

   @ModifyArgs(
      method = {"render*"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"
)
   )
   private void renderHook(Args args) {
      float alpha = -1.0F;
      PlayerEntity pl;
      LivingEntity livingEntity;
      if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.antiPlayerCollision.getValue() && this.lastEntity != Wrapper.mc.player && (livingEntity = this.lastEntity) instanceof PlayerEntity && !(pl = (PlayerEntity)livingEntity).isInvisible()) {
         alpha = MathUtil.clamp((float)(Wrapper.mc.player.squaredDistanceTo(this.lastEntity.getPos()) / (double)3.0F) + 0.2F, 0.0F, 1.0F);
      }

      if (alpha != -1.0F) {
         args.set(4, this.applyOpacity(654311423, alpha));
      }

   }

   @Unique
   int applyOpacity(int color_int, float opacity) {
      opacity = Math.min(1.0F, Math.max(0.0F, opacity));
      Color color = new Color(color_int);
      return (new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)((float)color.getAlpha() * opacity))).getRGB();
   }
}
