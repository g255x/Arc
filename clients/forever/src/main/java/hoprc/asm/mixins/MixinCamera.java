package hoprc.asm.mixins;

import dev.kissoo.mod.modules.impl.player.FreeLook;
import dev.kissoo.mod.modules.impl.player.Freecam;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public abstract class MixinCamera {
   @Shadow
   private boolean thirdPerson;

   @Shadow
   protected abstract float clipToSpace(float var1);

   @ModifyArgs(
           method = "update",
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/render/Camera;moveBy(FFF)V",
                   ordinal = 0
           )
   )
   private void modifyCameraDistance(Args args) {
      if (dev.kissoo.mod.modules.impl.render.Camera.INSTANCE != null
              && dev.kissoo.mod.modules.impl.render.Camera.INSTANCE.isCameraClipOn()) {
         args.set(0, -this.clipToSpace((float) dev.kissoo.mod.modules.impl.render.Camera.INSTANCE.getClipDistance()));
      }
   }

   @Inject(method = "clipToSpace", at = @At("HEAD"), cancellable = true)
   private void onClipToSpace(float f, CallbackInfoReturnable<Float> cir) {
      if (dev.kissoo.mod.modules.impl.render.Camera.INSTANCE != null
              && dev.kissoo.mod.modules.impl.render.Camera.INSTANCE.isCameraClipOn()) {
         cir.setReturnValue((float) dev.kissoo.mod.modules.impl.render.Camera.INSTANCE.getClipDistance());
      }
   }

   @Inject(method = "update", at = @At("TAIL"))
   private void updateHook(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
      if (Freecam.INSTANCE != null && Freecam.INSTANCE.isOn()) {
         this.thirdPerson = true;
      }
   }

   @ModifyArgs(
           method = "update",
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"
           )
   )
   private void setRotationHook(Args args) {
      if (Freecam.INSTANCE != null && Freecam.INSTANCE.isOn()) {
         args.setAll(Freecam.INSTANCE.getFakeYaw(), Freecam.INSTANCE.getFakePitch());
      } else if (FreeLook.INSTANCE != null && FreeLook.INSTANCE.isOn()) {
         args.setAll(FreeLook.INSTANCE.getFakeYaw(), FreeLook.INSTANCE.getFakePitch());
      }
   }

   @ModifyArgs(
           method = "update",
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V"
           )
   )
   private void setPosHook(Args args) {
      if (Freecam.INSTANCE != null && Freecam.INSTANCE.isOn()) {
         args.setAll(Freecam.INSTANCE.getFakeX(), Freecam.INSTANCE.getFakeY(), Freecam.INSTANCE.getFakeZ());
      } else if (dev.kissoo.mod.modules.impl.render.Camera.INSTANCE != null
              && dev.kissoo.mod.modules.impl.render.Camera.INSTANCE.isMotionCameraOn()) {
         args.setAll(
                 dev.kissoo.mod.modules.impl.render.Camera.INSTANCE.getMotionFakeX(),
                 dev.kissoo.mod.modules.impl.render.Camera.INSTANCE.getMotionFakeY(),
                 dev.kissoo.mod.modules.impl.render.Camera.INSTANCE.getMotionFakeZ()
         );
      }
   }
}
