package dev.perish.asm.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.perish.api.utils.Wrapper;
import dev.perish.api.utils.math.MathUtil;
import dev.perish.core.impl.RotationManager;
import dev.perish.mod.modules.impl.client.ClientSetting;
import dev.perish.mod.modules.impl.render.Chams;
import dev.perish.mod.modules.impl.render.NoRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.awt.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.LivingEntityRenderer;

@Mixin(LivingEntityRenderer.class)
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
    private boolean appliedChamsShine;

    @Inject(method = "render*", at = @At("HEAD"))
    public void onRenderPre(T livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player != null && livingEntity == MinecraftClient.getInstance().player && ClientSetting.INSTANCE.rotations.getValue()) {
            this.originalYaw = livingEntity.getYaw();
            this.originalHeadYaw = livingEntity.headYaw;
            this.originalBodyYaw = livingEntity.bodyYaw;
            this.originalPitch = livingEntity.getPitch();
            this.originalPrevYaw = livingEntity.prevYaw;
            this.originalPrevHeadYaw = livingEntity.prevHeadYaw;
            this.originalPrevBodyYaw = livingEntity.prevBodyYaw;
            livingEntity.setYaw(RotationManager.getRenderYawOffset());
            livingEntity.headYaw = RotationManager.getRotationYawHead();
            livingEntity.bodyYaw = RotationManager.getRenderYawOffset();
            livingEntity.setPitch(RotationManager.getRenderPitch());
            livingEntity.prevYaw = RotationManager.getPrevRenderYawOffset();
            livingEntity.prevHeadYaw = RotationManager.getPrevRotationYawHead();
            livingEntity.prevBodyYaw = RotationManager.getPrevRenderYawOffset();
            livingEntity.prevPitch = RotationManager.getPrevRenderPitch();
        }

        this.lastEntity = livingEntity;
        this.appliedChamsShine = false;
        if (Chams.INSTANCE != null && Chams.INSTANCE.shouldRenderEntityChams(livingEntity) && Chams.INSTANCE.shouldApplyEntityShine()) {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(770, 32772);
            this.appliedChamsShine = true;
        }
    }

    @Inject(method = "render*", at = @At("TAIL"))
    public void onRenderPost(T livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player != null && livingEntity == MinecraftClient.getInstance().player && ClientSetting.INSTANCE.rotations.getValue()) {
            livingEntity.setYaw(this.originalYaw);
            livingEntity.headYaw = this.originalHeadYaw;
            livingEntity.bodyYaw = this.originalBodyYaw;
            livingEntity.setPitch(this.originalPitch);
            livingEntity.prevYaw = this.originalPrevYaw;
            livingEntity.prevHeadYaw = this.originalPrevHeadYaw;
            livingEntity.prevBodyYaw = this.originalPrevBodyYaw;
            livingEntity.prevPitch = this.originalPitch;
        }

        if (this.appliedChamsShine) {
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            this.appliedChamsShine = false;
        }
    }

    @ModifyArgs(
            method = "render*",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"
            )
    )
    private void renderHook(Args args) {
        float alpha = -1.0F;
        Color colorOverride = null;
        if (NoRender.INSTANCE.isOn()
                && NoRender.INSTANCE.antiPlayerCollision.getValue()
                && this.lastEntity != Wrapper.mc.player
                && this.lastEntity instanceof PlayerEntity pl
                && !pl.isInvisible()) {
            alpha = MathUtil.clamp((float) (Wrapper.mc.player.squaredDistanceTo(this.lastEntity.getPos()) / 3.0) + 0.2F, 0.0F, 1.0F);
        }

        if (Chams.INSTANCE != null && Chams.INSTANCE.shouldRenderEntityChams(this.lastEntity)) {
            colorOverride = Chams.INSTANCE.getEntityRenderColor(this.lastEntity);
        }

        if (colorOverride != null) {
            int color = colorOverride.getRGB();
            if (alpha != -1.0F) {
                color = this.applyOpacity(color, alpha);
            }
            args.set(4, color);
        } else if (alpha != -1.0F) {
            args.set(4, this.applyOpacity(654311423, alpha));
        }
    }

    @Unique
    int applyOpacity(int color_int, float opacity) {
        opacity = Math.min(1.0F, Math.max(0.0F, opacity));
        Color color = new Color(color_int);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * opacity)).getRGB();
    }
}
