package dev.baimaodaxia.asm.mixins;

import dev.baimaodaxia.mod.features.SpearPort;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ItemStack.class})
public abstract class MixinSpearItemStack {
   @Inject(
      method = {"use"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onStartUsingPortedSpear(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
      ItemStack stack = (ItemStack)(Object)this;
      if (SpearPort.isSpear(stack)) {
         startUsingSpear(stack, world, user, hand);
         cir.setReturnValue(TypedActionResult.consume(stack));
      }

   }

   @Inject(
      method = {"useOnBlock"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onBlockPortedSpearPlaceOnBlock(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
      ItemStack stack = (ItemStack)(Object)this;
      PlayerEntity player = context.getPlayer();
      if (player != null && SpearPort.isSpear(stack)) {
         startUsingSpear(stack, context.getWorld(), player, context.getHand());
         cir.setReturnValue(ActionResult.CONSUME);
      }

   }

   @Inject(
      method = {"getMaxUseTime"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onGetPortedSpearUseTime(LivingEntity user, CallbackInfoReturnable<Integer> cir) {
      if (SpearPort.isSpear((ItemStack)(Object)this)) {
         cir.setReturnValue(SpearPort.MAX_USE_TIME);
      }

   }

   @Inject(
      method = {"getUseAction"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onGetPortedSpearUseAction(CallbackInfoReturnable<UseAction> cir) {
      if (SpearPort.isSpear((ItemStack)(Object)this)) {
         cir.setReturnValue(UseAction.NONE);
      }

   }

   @Inject(
      method = {"getName"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onFixPortedSpearName(CallbackInfoReturnable<Text> cir) {
      ItemStack stack = (ItemStack)(Object)this;
      Text name = SpearPort.getNameOverride(stack);
      if (name != null) {
         cir.setReturnValue(name);
      }

   }

   @Inject(
      method = {"onStoppedUsing"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onStopUsingPortedSpear(World world, LivingEntity user, int remainingUseTicks, CallbackInfo ci) {
      ItemStack stack = (ItemStack)(Object)this;
      if (SpearPort.isSpear(stack)) {
         SpearPort.onStoppedUsing(stack, world, user, remainingUseTicks);
         ci.cancel();
      }

   }

   @Inject(
      method = {"usageTick"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onTickPortedSpear(World world, LivingEntity user, int remainingUseTicks, CallbackInfo ci) {
      ItemStack stack = (ItemStack)(Object)this;
      if (SpearPort.isSpear(stack)) {
         SpearPort.onUsageTick(stack, world, user, remainingUseTicks);
         ci.cancel();
      }

   }

   @Inject(
      method = {"isUsedOnRelease"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onIsPortedSpearUsedOnRelease(CallbackInfoReturnable<Boolean> cir) {
      if (SpearPort.isSpear((ItemStack)(Object)this)) {
         cir.setReturnValue(true);
      }

   }

   private static void startUsingSpear(ItemStack stack, World world, PlayerEntity user, Hand hand) {
      user.setCurrentHand(hand);
      SpearPort.playUseSound(world, user, stack);
   }
}
