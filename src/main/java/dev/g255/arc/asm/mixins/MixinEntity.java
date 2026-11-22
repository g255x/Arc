package dev.g255.arc.asm.mixins;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.impl.LookDirectionEvent;
import dev.g255.arc.api.events.impl.SprintEvent;
import dev.g255.arc.api.utils.Wrapper;
import dev.g255.arc.api.utils.rotation.RotationNaNGuard;
import dev.g255.arc.mod.modules.impl.movement.Velocity;
import dev.g255.arc.mod.modules.impl.render.NoRender;
import dev.g255.arc.mod.modules.impl.render.ShaderModule;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin({Entity.class})
public abstract class MixinEntity {
   @Shadow
   private static final int SPRINTING_FLAG_INDEX = 3;
   @Shadow
   private float yaw;
   @Shadow
   private float pitch;

   @Inject(
      method = {"changeLookDirection(DD)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookChangeLookDirection(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
      // 鼠标增量非有限（窗口最小化/失焦时偶发）会让朝向直接算成 NaN，先掐掉
      if (!Double.isFinite(cursorDeltaX) || !Double.isFinite(cursorDeltaY)) {
         RotationNaNGuard.report("changeLookDirection", (float)cursorDeltaX);
         ci.cancel();
         return;
      }

      if (Entity.class.cast(this) == Wrapper.mc.player) {
         LookDirectionEvent lookDirectionEvent = LookDirectionEvent.get((Entity)Entity.class.cast(this), cursorDeltaX, cursorDeltaY);
         Arc.EVENT_BUS.post(lookDirectionEvent);
         if (lookDirectionEvent.isCancelled()) {
            ci.cancel();
         }
      }

   }

   /**
    * 原版 setYaw/setPitch 收到 NaN 时会丢弃并打印 "Invalid entity rotation"。
    * 这里提前拦下：行为与原版一致（不写入），但不再每帧刷日志，而且会把调用来源打出来。
    * 若字段本身已经被污染成 NaN，顺手恢复，避免渲染插值一直是 NaN。
    */
   @Inject(
      method = {"setYaw(F)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void arc$guardSetYaw(float value, CallbackInfo ci) {
      if (!Float.isFinite(this.yaw)) {
         this.yaw = 0.0F;
      }

      if (!RotationNaNGuard.isValid(value)) {
         RotationNaNGuard.report("Entity.setYaw", value);
         ci.cancel();
      }
   }

   @Inject(
      method = {"setPitch(F)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void arc$guardSetPitch(float value, CallbackInfo ci) {
      if (!Float.isFinite(this.pitch)) {
         this.pitch = 0.0F;
      }

      if (!RotationNaNGuard.isValid(value)) {
         RotationNaNGuard.report("Entity.setPitch", value);
         ci.cancel();
      }
   }

   @Inject(
      at = {@At("HEAD")},
      method = {"isInvisibleTo(Lnet/minecraft/entity/player/PlayerEntity;)Z"},
      cancellable = true
   )
   private void onIsInvisibleCheck(PlayerEntity message, CallbackInfoReturnable<Boolean> cir) {
      if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.invisible.getValue()) {
         cir.setReturnValue(false);
      }

   }

   @Inject(
      method = {"isGlowing"},
      at = {@At("HEAD")},
      cancellable = true
   )
   void isGlowingHook(CallbackInfoReturnable<Boolean> cir) {
      if (ShaderModule.INSTANCE.isOn()) {
         cir.setReturnValue(ShaderModule.INSTANCE.shouldRender((Entity)Entity.class.cast(this)));
      }

   }

   @ModifyArgs(
      method = {"pushAwayFrom"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/entity/Entity;addVelocity(DDD)V"
)
   )
   private void pushAwayFromHook(Args args) {
      if (Entity.class.cast(this) == MinecraftClient.getInstance().player && Velocity.INSTANCE.isOn() && Velocity.INSTANCE.entityPush.getValue()) {
         args.set(0, (double)0.0F);
         args.set(1, (double)0.0F);
         args.set(2, (double)0.0F);
      }

   }

   @Inject(
      method = {"isOnFire"},
      at = {@At("HEAD")},
      cancellable = true
   )
   void isOnFireHook(CallbackInfoReturnable<Boolean> cir) {
      if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.fireEntity.getValue()) {
         cir.setReturnValue(false);
      }

   }

   @Inject(
      method = {"setSprinting"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void setSprintingHook(boolean sprinting, CallbackInfo ci) {
      if (Entity.class.cast(this) == MinecraftClient.getInstance().player) {
         SprintEvent event = SprintEvent.get();
         Arc.EVENT_BUS.post(event);
         if (event.isCancelled()) {
            ci.cancel();
            sprinting = event.isSprint();
            this.setFlag(3, sprinting);
         }
      }

   }

   @Shadow
   protected void setFlag(int index, boolean value) {
   }
}
