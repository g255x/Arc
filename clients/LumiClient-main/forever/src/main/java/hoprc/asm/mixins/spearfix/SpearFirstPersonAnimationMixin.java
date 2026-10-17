package hoprc.asm.mixins.spearfix;

import dev.kissoo.mod.spearfix.SpearFix;
import dev.kissoo.mod.spearfix.render.SpearUseAnimation;
import hoprc.obf.mixin.MixinObfuscate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@MixinObfuscate
@Mixin(HeldItemRenderer.class)
public abstract class SpearFirstPersonAnimationMixin {
    @Unique
    private static int spearfix$useTicks;

    @Unique
    private static boolean spearfix$wasUsing;

    @Unique
    private static int spearfix$useStartAge;

    @Unique
    private static ItemStack spearfix$useStack = ItemStack.EMPTY;

    @Shadow
    public abstract void renderItem(LivingEntity entity, ItemStack stack, ModelTransformationMode renderMode,
                                    boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light);

    @Shadow
    private void applyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress) {
    }

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void spearfix$interceptAttackAnimation(
            AbstractClientPlayerEntity player,
            float tickDelta,
            float pitch,
            Hand hand,
            float swingProgress,
            ItemStack stack,
            float equipProgress,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci) {
        if (!SpearFix.isRemappedSpear(stack)) return;
        if (!spearfix$hasLungeEnchantment(stack)) return;
        float stabProgress = SpearFix.getStabAnimationProgress(player, stack, tickDelta);
        if (stabProgress < 0.0F) return;

        Arm arm = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        boolean rightHand = arm == Arm.RIGHT;

        matrices.push();
        this.applyEquipOffset(matrices, arm, equipProgress);
        SpearUseAnimation.firstPersonAttack(stabProgress, matrices, rightHand ? 1 : -1);

        this.renderItem(player, stack,
                rightHand ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND,
                !rightHand, matrices, vertexConsumers, light);
        matrices.pop();

        ci.cancel();
    }

    @Inject(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"
            )
    )
    private void spearfix$applyUseAnimation(
            AbstractClientPlayerEntity player,
            float tickDelta,
            float pitch,
            Hand hand,
            float swingProgress,
            ItemStack stack,
            float equipProgress,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci) {
        if (!SpearFix.isRemappedSpear(stack)) return;
        if (swingProgress > 0.0F) return;
        if (!spearfix$isUsingThisSpear(player, hand)) {
            spearfix$resetUseAnimation();
            return;
        }

        Arm arm = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        SpearUseAnimation.firstPersonUse(spearfix$getUseTicks(player, hand, stack, tickDelta), arm, matrices);
    }

    @Redirect(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;isUsingItem()Z",
                    ordinal = 1
            ),
            require = 0
    )
    private boolean spearfix$skipVanillaUseAnimation(
            AbstractClientPlayerEntity player,
            AbstractClientPlayerEntity renderedPlayer,
            float tickDelta,
            float pitch,
            Hand hand,
            float swingProgress,
            ItemStack stack,
            float equipProgress,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light) {
        if (SpearFix.isRemappedSpear(stack) && spearfix$isUsingThisSpear(player, hand)) {
            return false;
        }
        return player.isUsingItem();
    }

    @Unique
    private static boolean spearfix$hasLungeEnchantment(ItemStack stack) {
        ItemEnchantmentsComponent enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantments == null || enchantments.isEmpty()) return false;

        for (RegistryEntry<Enchantment> entry : enchantments.getEnchantments()) {
            if (entry.getKey().map(key -> key.getValue().toString().toLowerCase().contains("lunge")).orElse(false)) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private static boolean spearfix$isUsingThisSpear(AbstractClientPlayerEntity player, Hand hand) {
        if (player.isUsingItem() && player.getActiveHand() == hand) {
            return true;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        return player == mc.player && hand == Hand.MAIN_HAND && mc.options.useKey.isPressed();
    }

    @Unique
    private static float spearfix$getUseTicks(AbstractClientPlayerEntity player, Hand hand, ItemStack stack, float tickDelta) {
        boolean using = SpearFix.isRemappedSpear(stack) && spearfix$isUsingThisSpear(player, hand);
        if (!using) {
            spearfix$resetUseAnimation();
            return 0.0F;
        }

        if (!spearfix$wasUsing || spearfix$useStack.getItem() != stack.getItem()
            || !spearfix$useStack.getName().getString().equals(stack.getName().getString())) {
            spearfix$useTicks = 0;
            spearfix$useStartAge = player.age;
            spearfix$useStack = stack.copy();
            spearfix$wasUsing = true;
        }

        spearfix$useTicks = Math.max(0, player.age - spearfix$useStartAge);
        return spearfix$useTicks + tickDelta;
    }

    @Unique
    private static void spearfix$resetUseAnimation() {
        spearfix$wasUsing = false;
        spearfix$useTicks = 0;
        spearfix$useStartAge = 0;
        spearfix$useStack = ItemStack.EMPTY;
    }
}
