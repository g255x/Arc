package hoprc.asm.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.kissoo.Forever;
import dev.kissoo.api.utils.Wrapper;
import hoprc.api.core.impl.ShaderManager;
import dev.kissoo.mod.modules.impl.render.Ambience;
import dev.kissoo.mod.modules.impl.render.skybox.Skybox;
import dev.kissoo.mod.modules.impl.player.Freecam;
import dev.kissoo.mod.modules.impl.render.Chams;
import dev.kissoo.mod.modules.impl.render.ShaderModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LightningEntity;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer {
   @Unique
   boolean renderingChams = false;

   @Unique
   boolean renderingEntity = false;

   @Unique
   boolean renderingLightningShader = false;

   @Shadow
   abstract void renderEntity(
           Entity entity,
           double cameraX,
           double cameraY,
           double cameraZ,
           float tickDelta,
           MatrixStack matrices,
           VertexConsumerProvider vertexConsumers
   );


   @Inject(method = "renderEntity", at = @At("HEAD"))
   private void injectChamsForEntity(
           Entity entity,
           double cameraX,
           double cameraY,
           double cameraZ,
           float tickDelta,
           MatrixStack matrices,
           VertexConsumerProvider vertexConsumers,
           CallbackInfo ci
   ) {
      if (Chams.INSTANCE != null && Chams.INSTANCE.isOn() && Chams.INSTANCE.throughWall.getValue()) {
         if (Chams.INSTANCE.chams(entity)) {
            if (this.renderingEntity) {
               Wrapper.mc.getBufferBuilders().getEntityVertexConsumers().draw();
               this.renderingEntity = false;
            }
            GL11.glEnable(32823);
            GL11.glPolygonOffset(1.0F, -1000000.0F);
            this.renderingChams = true;
         } else {
            this.renderingEntity = true;
         }
      }
   }
    @Inject(method = "renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V", at = @At(value = "HEAD"), cancellable = true)
    private void renderSkyHead(Matrix4f matrices, Matrix4f projectionMatrix, float tickDelta, Camera camera, boolean bl, Runnable runnable, CallbackInfo info) {
        if (Skybox.INSTANCE != null && Skybox.INSTANCE.isOn()) {
            runnable.run();
            Skybox.INSTANCE.renderSky(matrices, projectionMatrix, tickDelta);
            info.cancel();
        }
    }


    @Inject(method = "renderEntity", at = @At("RETURN"))
   private void injectChamsForEntityPost(
           Entity entity,
           double cameraX,
           double cameraY,
           double cameraZ,
           float tickDelta,
           MatrixStack matrices,
           VertexConsumerProvider vertexConsumers,
           CallbackInfo ci
   ) {
      if (Chams.INSTANCE != null && Chams.INSTANCE.isOn() && Chams.INSTANCE.throughWall.getValue() && this.renderingChams) {
         Wrapper.mc.getBufferBuilders().getEntityVertexConsumers().draw();
         GL11.glPolygonOffset(1.0F, 1000000.0F);
         GL11.glDisable(32823);
         this.renderingChams = false;
      }
   }


   @Inject(method = "renderEntity", at = @At("RETURN"))
   private void injectLightningShaderPost(
           Entity entity,
           double cameraX,
           double cameraY,
           double cameraZ,
           float tickDelta,
           MatrixStack matrices,
           VertexConsumerProvider vertexConsumers,
           CallbackInfo ci
   ) {
      if (this.renderingLightningShader) return;

      if (entity instanceof LightningEntity
              && ShaderModule.INSTANCE != null
              && Forever.SHADER != null
              && ShaderModule.INSTANCE.shouldRenderLighting()) {

         this.renderingLightningShader = true;

         Forever.SHADER.applyShader(() -> {
            VertexConsumerProvider.Immediate immediate =
                    MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

            this.renderEntity(
                    entity,
                    cameraX, cameraY, cameraZ,
                    tickDelta,
                    matrices,
                    immediate
            );

            immediate.draw();
         }, (ShaderManager.Shader) ShaderModule.INSTANCE.mode.getValue());

         this.renderingLightningShader = false;
      }
   }


   @Redirect(
           method = "render",
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/gl/PostEffectProcessor;render(F)V",
                   ordinal = 0
           ),
           require = 0
   )
   void replaceShaderHook(PostEffectProcessor instance, float tickDelta) {
      if (ShaderModule.INSTANCE != null && Forever.SHADER != null && ShaderModule.INSTANCE.isOn() && Wrapper.mc.world != null) {
         // 轮廓着色器处理原版发光缓冲（isGlowing 剪影），固定用 Global 组参数：
         // 组选择器只负责"编辑哪组"，不影响实时渲染 → 切 Hand 不会把水晶轮廓变成手部
         ShaderModule mod = ShaderModule.INSTANCE;
         ShaderManager.Shader shaders = mod.getMode(ShaderModule.Group.Global);
         Forever.SHADER.setupShaderWithCustomParams(shaders, Forever.SHADER.getShaderOutline(shaders), mod.getParams(ShaderModule.Group.Global));
      } else {
         instance.render(tickDelta);
      }
   }


   @Redirect(
           method = "render",
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/render/WorldRenderer;renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V"
           )
   )
   private void skyShaderHook(
           WorldRenderer instance,
           Matrix4f viewMatrix,
           Matrix4f projectionMatrix,
           float tickDelta,
           Camera camera,
           boolean thickFog,
           Runnable runnable
   ) {
      if (Forever.SHADER != null && Ambience.INSTANCE != null && Ambience.INSTANCE.shouldRenderSkyShader()) {
         Forever.SHADER.applySky(() ->
                 instance.renderSky(viewMatrix, projectionMatrix, tickDelta, camera, thickFog, runnable)
         );
         RenderSystem.disableDepthTest();
         RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
         RenderSystem.enableDepthTest();
         return;
      }
      instance.renderSky(viewMatrix, projectionMatrix, tickDelta, camera, thickFog, runnable);
   }


   @ModifyArg(
           method = "render",
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/render/WorldRenderer;setupTerrain(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/Frustum;ZZ)V"
           ),
           index = 3
   )
   private boolean renderSetupTerrainModifyArg(boolean spectator) {
      return Freecam.INSTANCE != null && Freecam.INSTANCE.isOn() || spectator;
   }
}
