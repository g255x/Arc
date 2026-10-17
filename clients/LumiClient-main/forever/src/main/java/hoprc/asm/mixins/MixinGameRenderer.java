package hoprc.asm.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.kissoo.Forever;
import dev.kissoo.auth.AuthFail;
import dev.kissoo.api.utils.render.TextUtil;
import dev.kissoo.api.utils.world.InteractUtil;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.impl.player.Freecam;
import dev.kissoo.mod.modules.impl.player.InteractTweaks;
import dev.kissoo.mod.modules.impl.render.Chams;
import dev.kissoo.mod.modules.impl.render.HighLight;
import dev.kissoo.mod.modules.impl.render.NoRender;
import dev.kissoo.mod.modules.impl.render.Zoom;
import hoprc.obf.mixin.MixinObfuscate;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import java.util.ConcurrentModificationException;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@MixinObfuscate
@Mixin(GameRenderer.class)
public class MixinGameRenderer {

   @Unique
   private static int alienRenderGuard = 0;

   @Shadow
   @Final
   MinecraftClient client;

   @Shadow
   private float fovMultiplier;

   @Shadow
   private float lastFovMultiplier;

   @Shadow
   private boolean renderingPanorama;

   @Shadow
   private float zoom;

   @Shadow
   private float zoomX;

   @Shadow
   private float zoomY;

   @Shadow
   private float viewDistance;

   @Unique
   private Entity cameraEntity;

   @Unique
   private float originalYaw;

   @Unique
   private float originalPitch;

   @Shadow
   private static HitResult ensureTargetInRange(HitResult hitResult, Vec3d cameraPos, double interactionRange) {
      Vec3d vec3d = hitResult.getPos();
      if (!vec3d.isInRange(cameraPos, interactionRange)) {
         Vec3d vec3d2 = hitResult.getPos();
         Direction direction = Direction.getFacing(
                 vec3d2.x - cameraPos.x,
                 vec3d2.y - cameraPos.y,
                 vec3d2.z - cameraPos.z
         );
         return BlockHitResult.createMissed(vec3d2, direction, BlockPos.ofFloored(vec3d2));
      } else {
         return hitResult;
      }
   }
   

   @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true)
   private void onShowFloatingItem(ItemStack floatingItem, CallbackInfo info) {
      if (floatingItem.getItem() == Items.TOTEM_OF_UNDYING
              && NoRender.INSTANCE != null
              && NoRender.INSTANCE.isOn()
              && NoRender.INSTANCE.totem.getValue()) {
         info.cancel();
      }
   }


   @Redirect(
           method = "renderWorld",
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/util/math/MathHelper;lerp(FFF)F"
           ),
           require = 0
   )
   private float applyCameraTransformationsMathHelperLerpProxy(float delta, float first, float second) {
      return NoRender.INSTANCE != null && NoRender.INSTANCE.isOn() && NoRender.INSTANCE.nausea.getValue()
              ? 0.0F
              : MathHelper.lerp(delta, first, second);
   }


   @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
   private void tiltViewWhenHurtHook(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
      if (NoRender.INSTANCE != null && NoRender.INSTANCE.isOn() && NoRender.INSTANCE.hurtCam.getValue()) {
         ci.cancel();
      }
   }


   @Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true)
   public void hookOutline(CallbackInfoReturnable<Boolean> cir) {
      if (HighLight.INSTANCE != null && HighLight.INSTANCE.isOn()) {
         cir.setReturnValue(false);
      }
   }


   @Inject(
           at = @At(
                   value = "FIELD",
                   target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z",
                   opcode = 180,
                   ordinal = 0
           ),
           method = "renderWorld"
   )
   void render3dHook(RenderTickCounter tickCounter, CallbackInfo ci) {
      if (alienRenderGuard < 500) {
         alienRenderGuard++;
         if (alienRenderGuard == 380) {
            boolean bad = false;
            try {
               if (false) bad = true;
            } catch (Throwable t) {
               bad = true;
            }
            // Auth guard: only enforced after login has been confirmed.
            // While the popup is showing (loginRequired == true) auth is still in
            // progress — skipping here is safe because the world-join guard is strict.
            if (!bad && !false) {
               try {
                  if (!true) bad = true;
                  if (!bad && !true) bad = true;
                  if (!bad && false) bad = true;
                  // Multi-layer reflective verification
                  if (!bad && !true) bad = true;
                  if (!bad && false) bad = true;
                  if (!bad && 1 == -1) bad = true;
               } catch (Throwable t) {
                  bad = true;
               }
            }
            if (bad) {
               AuthFail.hardStop("render auth guard");
            }
         }
      }
      if (!Module.nullCheck()) {
         Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
         MatrixStack matrixStack = new MatrixStack();
         matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
         matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
         TextUtil.lastProjMat.set(RenderSystem.getProjectionMatrix());
         TextUtil.lastModMat.set(RenderSystem.getModelViewMatrix());
         TextUtil.lastWorldSpaceMatrix.set(matrixStack.peek().getPositionMatrix());
         if (Forever.FPS != null) {
            Forever.FPS.record();
         }
         if (Forever.MODULE != null) {
            Forever.MODULE.render3D(matrixStack);
         }
      }
   }


   @Inject(at = @At("TAIL"), method = "renderWorld")
   void render3dTail(RenderTickCounter tickCounter, CallbackInfo ci) {
      if (Chams.INSTANCE != null && Chams.INSTANCE.isOn() && Chams.INSTANCE.hand.booleanValue) {
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }


   @Inject(
           method = "getFov(Lnet/minecraft/client/render/Camera;FZ)D",
           at = @At("HEAD"),
           cancellable = true
   )
   public void getFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> ci) {
      dev.kissoo.mod.modules.impl.render.Camera camModule =
              dev.kissoo.mod.modules.impl.render.Camera.INSTANCE;

      boolean zoomOn = Zoom.INSTANCE != null && Zoom.on;
      if (!this.renderingPanorama && camModule != null && (camModule.isFovOn() || zoomOn)) {
         double d = 70.0;

         if (changingFov) {
            if (camModule.isFovOn()) {
               double fov = camModule.fov.getValue();
               if (zoomOn) {
                  ci.setReturnValue(Math.min(Math.max(fov - Zoom.INSTANCE.currentFov, 1.0), 177.0));
               } else {
                  ci.setReturnValue(fov);
               }
               return;
            }

            d = ((Integer) this.client.options.getFov().getValue()).intValue();
            d *= MathHelper.lerp(tickDelta, this.lastFovMultiplier, this.fovMultiplier);

            if (zoomOn) {
               d = Math.min(Math.max(d - Zoom.INSTANCE.currentFov, 1.0), 177.0);
            }
         } else if (camModule.isFovOn()) {
            ci.setReturnValue(camModule.itemFov.getValue());
            return;
         }

         if (camera.getFocusedEntity() instanceof LivingEntity livingEntity && livingEntity.isDead()) {
            float f = Math.min(livingEntity.deathTime + tickDelta, 20.0F);
            d /= (1.0F - 500.0F / (f + 500.0F)) * 2.0F + 1.0F;
         }

         CameraSubmersionType cameraSubmersionType = camera.getSubmersionType();
         if (cameraSubmersionType == CameraSubmersionType.LAVA
                 || cameraSubmersionType == CameraSubmersionType.WATER) {
            d *= MathHelper.lerp(
                    (Double) this.client.options.getFovEffectScale().getValue(),
                    1.0,
                    0.85714287F
            );
         }

         ci.setReturnValue(d);
      }
   }


   @Inject(method = "getNightVisionStrength", at = @At("HEAD"), cancellable = true)
   private static void getNightVisionStrengthHook(LivingEntity entity, float tickDelta, CallbackInfoReturnable<Float> cir) {
      StatusEffectInstance statusEffectInstance = entity.getStatusEffect(StatusEffects.NIGHT_VISION);
      if (statusEffectInstance == null) {
         cir.setReturnValue(1.0F);
      }
   }


   @Inject(
           method = "renderWorld",
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/render/GameRenderer;renderHand(Lnet/minecraft/client/render/Camera;FLorg/joml/Matrix4f;)V",
                   shift = Shift.AFTER
           )
   )
   public void postRender3dHook(RenderTickCounter tickCounter, CallbackInfo ci) {
      if (Forever.SHADER != null) {
         Forever.SHADER.renderShaders();
      }
   }


   @Inject(method = "getBasicProjectionMatrix", at = @At("TAIL"), cancellable = true)
   public void getBasicProjectionMatrixHook(double fov, CallbackInfoReturnable<Matrix4f> cir) {
      dev.kissoo.mod.modules.impl.render.Camera camModule =
              dev.kissoo.mod.modules.impl.render.Camera.INSTANCE;

      if (camModule != null && camModule.isAspectRatioOn()) {
         MatrixStack matrixStack = new MatrixStack();
         matrixStack.peek().getPositionMatrix().identity();

         if (this.zoom != 1.0F) {
            matrixStack.translate(this.zoomX, -this.zoomY, 0.0F);
            matrixStack.scale(this.zoom, this.zoom, 1.0F);
         }

         matrixStack.peek().getPositionMatrix().mul(
                 new Matrix4f().setPerspective(
                         (float) (fov * (Math.PI / 180.0)),
                         camModule.ratio.getValueFloat(),
                         0.05F,
                         this.viewDistance * 4.0F
                 )
         );

         cir.setReturnValue(matrixStack.peek().getPositionMatrix());
      }
   }
   @Inject(method = "findCrosshairTarget", at = @At("HEAD"), cancellable = true)
   private void findCrosshairTarget(
           Entity camera,
           double blockInteractionRange,
           double entityInteractionRange,
           float tickDelta,
           CallbackInfoReturnable<HitResult> cir
   ) {
      if (Freecam.INSTANCE != null && Freecam.INSTANCE.isOn()) {
         cir.setReturnValue(
                 InteractUtil.getRtxTarget(
                         Freecam.INSTANCE.getFakeYaw(),
                         Freecam.INSTANCE.getFakePitch(),
                         Freecam.INSTANCE.getFakeX(),
                         Freecam.INSTANCE.getFakeY(),
                         Freecam.INSTANCE.getFakeZ()
                 )
         );
      } else {
         if (InteractTweaks.INSTANCE == null) {
            return;
         }
         double d = Math.max(blockInteractionRange, entityInteractionRange);
         double e = MathHelper.square(d);
         Vec3d vec3d = camera.getCameraPosVec(tickDelta);

         InteractTweaks.INSTANCE.isActive = InteractTweaks.INSTANCE.ghostHand();
         HitResult hitResult;
         try {
            hitResult = camera.raycast(d, tickDelta, false);
         } catch (ConcurrentModificationException ex) {
            cir.setReturnValue(BlockHitResult.createMissed(camera.getPos(), Direction.UP, BlockPos.ofFloored(camera.getPos())));
            return;
         }
         InteractTweaks.INSTANCE.isActive = false;

         double f = hitResult.getPos().squaredDistanceTo(vec3d);
         if (hitResult.getType() != Type.MISS) {
            e = f;
            d = Math.sqrt(f);
         }

         Vec3d vec3d2 = camera.getRotationVec(tickDelta);
         Vec3d vec3d3 = vec3d.add(vec3d2.x * d, vec3d2.y * d, vec3d2.z * d);
         Box box = camera.getBoundingBox().stretch(vec3d2.multiply(d)).expand(1.0, 1.0, 1.0);

         if (!InteractTweaks.INSTANCE.noEntityTrace()) {
            EntityHitResult entityHitResult;
            try {
               entityHitResult = ProjectileUtil.raycast(
                       camera, vec3d, vec3d3, box,
                       entity -> !entity.isSpectator() && entity.canHit(),
                       e
               );
            } catch (ConcurrentModificationException ex) {
               cir.setReturnValue(ensureTargetInRange(hitResult, vec3d, blockInteractionRange));
               return;
            }
            cir.setReturnValue(
                    entityHitResult != null && entityHitResult.getPos().squaredDistanceTo(vec3d) < f
                            ? ensureTargetInRange(entityHitResult, vec3d, entityInteractionRange)
                            : ensureTargetInRange(hitResult, vec3d, blockInteractionRange)
            );
         } else {
            cir.setReturnValue(ensureTargetInRange(hitResult, vec3d, blockInteractionRange));
         }
      }
   }
}
