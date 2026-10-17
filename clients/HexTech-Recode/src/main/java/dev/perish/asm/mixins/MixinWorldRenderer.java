package dev.perish.asm.mixins;

import dev.perish.Perish;
import dev.perish.api.utils.Wrapper;
import dev.perish.core.impl.ShaderManager;
import dev.perish.mod.modules.impl.player.Freecam;
import dev.perish.mod.modules.impl.render.Chams;
import dev.perish.mod.modules.impl.render.Weather;
import dev.perish.mod.modules.impl.render.ShaderModule;
import dev.perish.mod.modules.impl.render.skybox.Skybox;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.world.Heightmap;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.BuiltBuffer;
import dev.perish.mod.modules.impl.render.skybox.SkyFix;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer {
    @Shadow
    @Final
    private MinecraftClient client;
    @Shadow
    @Final
    private float[] NORMAL_LINE_DX;
    @Shadow
    @Final
    private float[] NORMAL_LINE_DZ;
    @Shadow
    private int ticks;
    @Shadow
    @Final
    private static Identifier SNOW;
    @Shadow
    private static int getLightmapCoordinates(net.minecraft.world.BlockRenderView world, BlockPos pos) {
        throw new AssertionError();
    }

    @Unique
    boolean renderingChams = false;
    @Unique
    boolean renderingEntity = false;


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
        if (Chams.INSTANCE.isOn() && Chams.INSTANCE.throughWall.getValue() && this.renderingChams) {
            Wrapper.mc.getBufferBuilders().getEntityVertexConsumers().draw();
            GL11.glPolygonOffset(1.0F, 1000000.0F);
            GL11.glDisable(32823);
            this.renderingChams = false;
        }
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/PostEffectProcessor;render(F)V", ordinal = 0), require = 0)
    void replaceShaderHook(PostEffectProcessor instance, float tickDelta) {
        if (ShaderModule.INSTANCE == null) {
            instance.render(tickDelta);
            return;
        }
        ShaderManager.Shader shaders = ShaderModule.INSTANCE.mode.getValue();
        if (ShaderModule.INSTANCE.isOn() && Wrapper.mc.world != null && Perish.SHADER != null) {
            Perish.SHADER.setupShader(shaders, Perish.SHADER.getShaderOutline(shaders));
        } else {
            instance.render(tickDelta);
        }
    }

    @Inject(method = "renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V", at = @At(value = "HEAD"), cancellable = true)
    private void renderSkyHead(Matrix4f matrices, Matrix4f projectionMatrix, float tickDelta, Camera camera, boolean bl, Runnable runnable, CallbackInfo info) {
        if (ShaderModule.INSTANCE != null && ShaderModule.INSTANCE.isOn() && ShaderModule.INSTANCE.sky.getValue() && Perish.SHADER != null) {
            if (runnable != null) runnable.run();
            Perish.SHADER.applyShader(() -> ShaderModule.INSTANCE.renderSkyMask(matrices), ShaderModule.INSTANCE.skyMode.getValue(), ShaderModule.INSTANCE.skyColor.getValue());
            info.cancel();
            return;
        }
        if (SkyFix.INSTANCE != null && SkyFix.INSTANCE.isOn()) {
            if (runnable != null) runnable.run();
            SkyFix.INSTANCE.renderSky(matrices, projectionMatrix, tickDelta);
            info.cancel();
            return;
        }
        if (Skybox.INSTANCE != null && Skybox.INSTANCE.isOn()) {
            if (runnable != null) runnable.run();
            Skybox.INSTANCE.renderSky(matrices, projectionMatrix, tickDelta);
            info.cancel();
        }
    }

    @Inject(method = "renderWeather", at = @At("HEAD"), cancellable = true)
    private void hookRenderWeatherHead(LightmapTextureManager manager, float tickDelta, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
        if (Weather.INSTANCE.isOn() && Weather.INSTANCE.mode.getValue() == Weather.Mode.Snow) {
            float scale = (float) Weather.INSTANCE.expandSize.getValue();
            com.mojang.blaze3d.systems.RenderSystem.getModelViewStack().pushMatrix();
            com.mojang.blaze3d.systems.RenderSystem.getModelViewStack().scale(scale, 1.0f, scale);
            com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
            try {
                this.renderForcedSnowWeather(manager, tickDelta, cameraX, cameraY + Weather.INSTANCE.height.getValue(), cameraZ);
            } finally {
                com.mojang.blaze3d.systems.RenderSystem.getModelViewStack().popMatrix();
                com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
            }
            ci.cancel();
            return;
        }
        if (Weather.INSTANCE.isOn()) {
            float scale = (float) Weather.INSTANCE.expandSize.getValue();
            com.mojang.blaze3d.systems.RenderSystem.getModelViewStack().pushMatrix();
            com.mojang.blaze3d.systems.RenderSystem.getModelViewStack().scale(scale, 1.0f, scale);
            com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
        }
    }

    @Redirect(method = "renderWeather", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getTopY(Lnet/minecraft/world/Heightmap$Type;II)I"), require = 0)
    private int hookRenderWeatherTopY(World instance, Heightmap.Type type, int x, int z) {
        if (dev.perish.mod.modules.impl.render.Weather.INSTANCE.isOn()) {
            return instance.getBottomY();
        }
        return instance.getTopY(type, x, z);
    }

    @Unique
    private void renderForcedSnowWeather(LightmapTextureManager manager, float tickDelta, double cameraX, double cameraY, double cameraZ) {
        if (this.client.world == null) {
            return;
        }

        float strength = this.client.world.getRainGradient(tickDelta);
        if (strength <= 0.0f) {
            return;
        }

        World world = this.client.world;
        int cameraBlockX = MathHelper.floor(cameraX);
        int cameraBlockY = MathHelper.floor(cameraY);
        int cameraBlockZ = MathHelper.floor(cameraZ);
        int weatherRange = MinecraftClient.isFancyGraphicsOrBetter() ? 10 : 5;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = null;
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        java.awt.Color color = Weather.INSTANCE.weatherColor.getValue();

        manager.enable();
        com.mojang.blaze3d.systems.RenderSystem.disableCull();
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        com.mojang.blaze3d.systems.RenderSystem.depthMask(MinecraftClient.isFabulousGraphicsOrBetter());
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRenderer::getParticleProgram);
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, SNOW);

        for (int z = cameraBlockZ - weatherRange; z <= cameraBlockZ + weatherRange; z++) {
            for (int x = cameraBlockX - weatherRange; x <= cameraBlockX + weatherRange; x++) {
                int normalIndex = (z - cameraBlockZ + 16) * 32 + x - cameraBlockX + 16;
                double offsetX = this.NORMAL_LINE_DX[normalIndex] * 0.5d;
                double offsetZ = this.NORMAL_LINE_DZ[normalIndex] * 0.5d;
                int topY = world.getBottomY();
                int yMin = Math.max(cameraBlockY - weatherRange, topY);
                int yMax = Math.max(cameraBlockY + weatherRange, topY);
                int lightY = Math.max(topY, cameraBlockY);
                if (yMin == yMax) {
                    continue;
                }

                if (bufferBuilder == null) {
                    bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR_LIGHT);
                }

                Random random = Random.create((long) (x * x * 3121 + x * 45238971 ^ z * z * 418711 + z * 13761));
                float uOffset = random.nextFloat();
                float vOffset = -((this.ticks & 511) + tickDelta) / 512.0f;
                double relativeX = x + 0.5d - cameraX;
                double relativeZ = z + 0.5d - cameraZ;
                float distance = (float) Math.sqrt(relativeX * relativeX + relativeZ * relativeZ) / weatherRange;
                float alpha = ((1.0f - distance * distance) * 0.3f + 0.5f) * strength;
                int alphaByte = MathHelper.clamp((int) (alpha * color.getAlpha()), 0, 255);
                mutable.set(x, lightY, z);
                int light = getLightmapCoordinates(world, mutable);
                int skyLight = (light >> 16) & 65535;
                int blockLight = light & 65535;
                int adjustedSkyLight = (skyLight * 3 + 240) / 4;
                int adjustedBlockLight = (blockLight * 3 + 240) / 4;

                this.vertexSnow(bufferBuilder, x, yMax, z, cameraX, cameraY, cameraZ, offsetX, offsetZ, 0.0f, yMin * 0.25f + vOffset + uOffset, color, alphaByte, adjustedBlockLight, adjustedSkyLight, false, false);
                this.vertexSnow(bufferBuilder, x, yMax, z, cameraX, cameraY, cameraZ, offsetX, offsetZ, 1.0f, yMin * 0.25f + vOffset + uOffset, color, alphaByte, adjustedBlockLight, adjustedSkyLight, true, true);
                this.vertexSnow(bufferBuilder, x, yMin, z, cameraX, cameraY, cameraZ, offsetX, offsetZ, 1.0f, yMax * 0.25f + vOffset + uOffset, color, alphaByte, adjustedBlockLight, adjustedSkyLight, true, true);
                this.vertexSnow(bufferBuilder, x, yMin, z, cameraX, cameraY, cameraZ, offsetX, offsetZ, 0.0f, yMax * 0.25f + vOffset + uOffset, color, alphaByte, adjustedBlockLight, adjustedSkyLight, false, false);
            }
        }

        if (bufferBuilder != null) {
            BuiltBuffer builtBuffer = bufferBuilder.end();
            BufferRenderer.drawWithGlobalProgram(builtBuffer);
        }
        com.mojang.blaze3d.systems.RenderSystem.enableCull();
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        manager.disable();
    }

    @Unique
    private void vertexSnow(
            VertexConsumer vertexConsumer,
            int x,
            int y,
            int z,
            double cameraX,
            double cameraY,
            double cameraZ,
            double offsetX,
            double offsetZ,
            float u,
            float v,
            java.awt.Color color,
            int alpha,
            int blockLight,
            int skyLight,
            boolean positiveX,
            boolean positiveZ
    ) {
        double worldX = x - cameraX + (positiveX ? offsetX : -offsetX) + 0.5d;
        double worldZ = z - cameraZ + (positiveZ ? offsetZ : -offsetZ) + 0.5d;
        vertexConsumer.vertex((float) worldX, (float) (y - cameraY), (float) worldZ)
                .texture(u, v)
                .color(color.getRed(), color.getGreen(), color.getBlue(), alpha)
                .light(blockLight, skyLight);
    }

    @Inject(method = "renderWeather", at = @At("RETURN"))
    private void hookRenderWeatherReturn(LightmapTextureManager manager, float tickDelta, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
        if (Weather.INSTANCE.isOn() && Weather.INSTANCE.mode.getValue() != Weather.Mode.Snow) {
            com.mojang.blaze3d.systems.RenderSystem.getModelViewStack().popMatrix();
            com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
        }
    }

    @ModifyVariable(method = "renderWeather", at = @At("HEAD"), ordinal = 1, argsOnly = true, require = 0)
    private double modifyCameraY(double cameraY) {
        if (Weather.INSTANCE.isOn() && Weather.INSTANCE.mode.getValue() != Weather.Mode.Snow) {
            return cameraY + Weather.INSTANCE.height.getValue();
        }
        return cameraY;
    }

    @Redirect(method = "renderWeather", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexConsumer;color(FFFF)Lnet/minecraft/client/render/VertexConsumer;"), require = 0)
    private net.minecraft.client.render.VertexConsumer hookRenderWeatherColor(net.minecraft.client.render.VertexConsumer instance, float red, float green, float blue, float alpha) {
        if (dev.perish.mod.modules.impl.render.Weather.INSTANCE.isOn()) {
            java.awt.Color c = dev.perish.mod.modules.impl.render.Weather.INSTANCE.weatherColor.getValue();
            return instance.color(c.getRed() / 255.0f, c.getGreen() / 255.0f, c.getBlue() / 255.0f, alpha * (c.getAlpha() / 255.0f));
        }
        return instance.color(red, green, blue, alpha);
    }

    @Redirect(method = "renderWeather", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/biome/Biome;hasPrecipitation()Z"), require = 0)
    private boolean hookRenderWeatherHasPrecipitation(Biome instance) {
        if (dev.perish.mod.modules.impl.render.Weather.INSTANCE.isOn()) {
            return dev.perish.mod.modules.impl.render.Weather.INSTANCE.mode.getValue() != dev.perish.mod.modules.impl.render.Weather.Mode.No;
        }
        return instance.hasPrecipitation();
    }

    @Redirect(method = "renderWeather", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/biome/Biome;getPrecipitation(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/world/biome/Biome$Precipitation;"), require = 0)
    private Biome.Precipitation hookRenderWeatherPrecipitation(Biome instance, BlockPos pos) {
        if (dev.perish.mod.modules.impl.render.Weather.INSTANCE.isOn()) {
            return this.getWeatherPrecipitation();
        }
        return instance.getPrecipitation(pos);
    }

    @Redirect(method = "tickRainSplashing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/biome/Biome;getPrecipitation(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/world/biome/Biome$Precipitation;"), require = 0)
    private Biome.Precipitation hookTickRainSplashingPrecipitation(Biome instance, BlockPos pos) {
        if (dev.perish.mod.modules.impl.render.Weather.INSTANCE.isOn()) {
            return this.getWeatherPrecipitation();
        }
        return instance.getPrecipitation(pos);
    }

    @Unique
    private Biome.Precipitation getWeatherPrecipitation() {
        return switch (dev.perish.mod.modules.impl.render.Weather.INSTANCE.mode.getValue()) {
            case Snow -> Biome.Precipitation.SNOW;
            case Rain -> Biome.Precipitation.RAIN;
            case No -> Biome.Precipitation.NONE;
        };
    }

    @ModifyArg(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/WorldRenderer;setupTerrain(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/Frustum;ZZ)V"
            ),
            index = 3,
            require = 0
    )
    private boolean renderSetupTerrainModifyArg(boolean spectator) {
        return Freecam.INSTANCE.isOn() || spectator;
    }
}
