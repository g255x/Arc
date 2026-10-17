package dev.baimaodaxia.asm.mixins;

import dev.baimaodaxia.earthquake;
import dev.baimaodaxia.api.utils.Wrapper;
import dev.baimaodaxia.core.impl.ShaderManager;
import dev.baimaodaxia.mod.modules.impl.player.Freecam;
import dev.baimaodaxia.mod.modules.impl.render.Chams;
import dev.baimaodaxia.mod.modules.impl.render.ShaderModule;
import dev.baimaodaxia.mod.modules.impl.render.Skybox;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.render.Frustum;

@Mixin({WorldRenderer.class})
public abstract class MixinWorldRenderer {
   @Unique
   boolean renderingChams = false;
   @Unique
   boolean renderingEntity = false;

   @Inject(
      method = {"renderEntity"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void injectChamsForEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
      if (Chams.INSTANCE.isOn() && Chams.INSTANCE.player.getValue() && entity instanceof PlayerEntity) {
         ci.cancel();
      } else {
         if (Chams.INSTANCE.isOn() && Chams.INSTANCE.throughWall.getValue()) {
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
   }

   @Inject(
      method = {"renderEntity"},
      at = {@At("RETURN")}
   )
   private void injectChamsForEntityPost(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
      if (Chams.INSTANCE.isOn() && Chams.INSTANCE.throughWall.getValue() && this.renderingChams) {
         Wrapper.mc.getBufferBuilders().getEntityVertexConsumers().draw();
         GL11.glPolygonOffset(1.0F, 1000000.0F);
         GL11.glDisable(32823);
         this.renderingChams = false;
      }

   }

   @Redirect(
      method = {"render"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gl/PostEffectProcessor;render(F)V",
   ordinal = 0
),
      require = 0
   )
   void replaceShaderHook(PostEffectProcessor instance, float tickDelta) {
      ShaderModule shaderModule = ShaderModule.INSTANCE;
      if (shaderModule != null && shaderModule.isOn() && Wrapper.mc.world != null) {
         ShaderManager.Shader shaders = shaderModule.mode.getValue();
         earthquake.SHADER.setupShader(shaders, earthquake.SHADER.getShaderOutline(shaders));
      } else {
         instance.render(tickDelta);
      }

   }

   @ModifyArg(
      method = {"render"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/render/WorldRenderer;setupTerrain(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/Frustum;ZZ)V"
),
      index = 3
   )
   private boolean renderSetupTerrainModifyArg(boolean spectator) {
      return Freecam.INSTANCE.isOn() || spectator;
   }

   @Inject(
      method = {"renderSky"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderSky(Matrix4f matrix4f, Matrix4f matrix4f2, float tickDelta, Camera camera, boolean thickFog, Runnable runnable, CallbackInfo ci) {
      Skybox skybox = Skybox.INSTANCE;
      if (skybox != null && skybox.isOn()) {
         skybox.renderSkybox();
         ci.cancel();
      }

   }
}
