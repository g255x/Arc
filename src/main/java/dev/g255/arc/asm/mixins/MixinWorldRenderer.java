package dev.g255.arc.asm.mixins;

import dev.g255.arc.Arc;
import dev.g255.arc.api.utils.Wrapper;
import dev.g255.arc.core.impl.ShaderManager;
import dev.g255.arc.mod.modules.impl.player.Freecam;
import dev.g255.arc.mod.modules.impl.render.Chams;
import dev.g255.arc.mod.modules.impl.render.ShaderModule;
import dev.g255.arc.mod.modules.impl.render.skybox.Skybox;
import net.minecraft.entity.Entity;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.ladysnake.satin.api.managed.ManagedShaderEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({WorldRenderer.class})
public abstract class MixinWorldRenderer {
   @Unique
   boolean renderingChams = false;
   @Unique
   boolean renderingEntity = false;

   @Inject(
      method = {"renderEntity"},
      at = {@At("HEAD")}
   )
   private void injectChamsForEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
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
      ShaderManager.Shader shaders = ShaderModule.INSTANCE.mode.getValue();
      if (ShaderModule.INSTANCE.isOn() && Wrapper.mc.world != null) {
         try {
            ManagedShaderEffect effect = Arc.SHADER.getShaderOutline(shaders);
            if (effect != null && effect.getShaderEffect() != null) {
               Arc.SHADER.setupShader(shaders, effect);
               return;
            }
         } catch (Throwable t) {
            // 着色器出任何岔子都不能让异常冲出渲染线程（那会直接生成 crash report），
            // 退回原版后处理，最差只是没有特效。
            System.out.println("[Arc] outline shader unavailable, falling back to vanilla: " + t);
         }
      }

      instance.render(tickDelta);
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
      at = {@At("TAIL")}
   )
   private void onRenderSky(Matrix4f matrix4f, Matrix4f projectionMatrix, float tickDelta, Camera camera, boolean thickFog, Runnable fogCallback, CallbackInfo ci) {
      if (Skybox.INSTANCE.isOn()) {
         Skybox.INSTANCE.renderSky(matrix4f, projectionMatrix, tickDelta);
      }

   }
}
