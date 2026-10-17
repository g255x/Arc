package dev.perish.asm.mixins;

import dev.perish.mod.modules.impl.render.Chams;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(EndCrystalEntityRenderer.class)
public abstract class MixinEndCrystalEntityRenderer {
    @Unique
    private static final Identifier BLANK = Identifier.of("textures/blank.png");
    @Mutable
    @Shadow
    @Final
    private static RenderLayer END_CRYSTAL;
    @Shadow
    @Final
    private static Identifier TEXTURE;

    @Inject(
            method = "render(Lnet/minecraft/entity/decoration/EndCrystalEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD")
    )
    private void onRenderHead(EndCrystalEntity entity, float yaw, float tickDelta, net.minecraft.client.util.math.MatrixStack matrices, net.minecraft.client.render.VertexConsumerProvider consumers, int light, CallbackInfo ci) {
        Chams module = Chams.INSTANCE;
        if (module != null && module.customCrystal()) {
            Identifier texture = module.texture.getValue() ? TEXTURE : BLANK;
            END_CRYSTAL = RenderLayer.getEntityTranslucent(texture);
            return;
        }

        END_CRYSTAL = RenderLayer.getEntityCutoutNoCull(TEXTURE);
    }

    @ModifyArgs(
            method = "render(Lnet/minecraft/entity/decoration/EndCrystalEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V", ordinal = 0)
    )
    private void onScale(Args args) {
        Chams module = Chams.INSTANCE;
        if (module != null && module.customCrystal()) {
            float scale = 2.0F * module.scale.getValueFloat();
            args.set(0, scale);
            args.set(1, scale);
            args.set(2, scale);
        }
    }

    @ModifyConstant(
            method = "render(Lnet/minecraft/entity/decoration/EndCrystalEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            constant = @Constant(floatValue = 3.0f)
    )
    private float modifySpinSpeed(float original) {
        Chams module = Chams.INSTANCE;
        if (module != null && module.customCrystal()) {
            return module.spinValue.getValueFloat() * 3.0f;
        }
        return original;
    }

    @Inject(
            method = "getYOffset(Lnet/minecraft/entity/decoration/EndCrystalEntity;F)F",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void onGetYOffset(EndCrystalEntity crystal, float tickDelta, CallbackInfoReturnable<Float> cir) {
        Chams module = Chams.INSTANCE;
        if (module != null && module.customCrystal()) {
            int age = module.spinSync.getValue() ? module.age : crystal.endCrystalAge;
            float animation = (age + tickDelta) * module.floatValue.getValueFloat();
            float bounce = MathHelper.sin(animation * 0.2F) / 2.0F + 0.5F;
            bounce = (bounce * bounce + bounce) * 0.4F * module.bounceHeight.getValueFloat();
            cir.setReturnValue(bounce - 1.4F + module.floatOffset.getValueFloat());
        }
    }
}
