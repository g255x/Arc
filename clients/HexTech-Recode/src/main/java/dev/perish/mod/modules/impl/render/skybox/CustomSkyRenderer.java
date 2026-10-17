package dev.perish.mod.modules.impl.render.skybox;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.world.World;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.Identifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.BackgroundRenderer;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.awt.*;

public class CustomSkyRenderer implements DimensionRenderingRegistry.SkyRenderer {

    @FunctionalInterface
    interface BufferFunction {
        BufferBuilder make(Tessellator tessellator, float minSize, float maxSize, int count, long seed);
    }

    private static final Identifier NEBULA_1 = Identifier.of("better_sky", "textures/sky/nebula_2.png");
    private static final Identifier NEBULA_2 = Identifier.of("better_sky", "textures/sky/nebula_3.png");
    private static final Identifier HORIZON = Identifier.of("better_sky", "textures/sky/nebula_1.png");
    private static final Identifier STARS = Identifier.of("better_sky", "textures/sky/stars.png");
    private static final Identifier FOG = Identifier.of("better_sky", "textures/sky/fog.png");

    private VertexBuffer nebula1;
    private VertexBuffer nebula2;
    private VertexBuffer horizon;
    private VertexBuffer stars1;
    private VertexBuffer stars2;
    private VertexBuffer stars3;
    private VertexBuffer stars4;
    private VertexBuffer fog;
    private Vector3f axis1;
    private Vector3f axis2;
    private Vector3f axis3;
    private Vector3f axis4;

    private boolean initialised;

    private void initialise() {
        if (!initialised) {
            initStars();
            CheckedRandom random = new CheckedRandom(131L);
            axis1 = new Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat());
            axis2 = new Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat());
            axis3 = new Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat());
            axis4 = new Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat());
            axis1.normalize();
            axis2.normalize();
            axis3.normalize();
            axis4.normalize();
            initialised = true;
        }
    }

    @Override
    public void render(WorldRenderContext context) {
        if (context.world() == null) {
            return;
        }

        render(context.world(), context.tickCounter().getTickDelta(false), context.matrixStack(), context.positionMatrix(), context.projectionMatrix());
    }

    public void render(World world, float tickDelta, Matrix4f positionMatrix, Matrix4f projectionMatrix) {
        render(world, tickDelta, null, positionMatrix, projectionMatrix);
    }

    private void render(World world, float tickDelta, MatrixStack inputMatrices, Matrix4f positionMatrix, Matrix4f projectionMatrix) {
        Skybox skybox = Skybox.INSTANCE;
        if (skybox == null || !skybox.isOn()) {
            return;
        }

        initialise();

        MatrixStack matrices = inputMatrices;
        if (matrices == null) {
            matrices = new MatrixStack();
            matrices.multiplyPositionMatrix(positionMatrix);
        }

        float time = ((world.getTimeOfDay() + tickDelta) % 360000) * 0.000017453292f;
        float time2 = time * 2;
        float time3 = time * 3;

        BackgroundRenderer.applyFogColor();
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);

        float blindA = 1.0f - BackgroundInfo.blindness;
        float blind02 = blindA * 0.2f;
        float blind06 = blindA * 0.6f;

        Color horizonColor = skybox.horizonColor.getValue();
        Color nebula1Color = skybox.nebula1Color.getValue();
        Color nebula2Color = skybox.nebula2Color.getValue();
        Color uvStars1Color = skybox.uvStars1Color.getValue();
        Color uvStars2Color = skybox.uvStars2Color.getValue();
        Color stars1Color = skybox.stars1Color.getValue();
        Color stars2Color = skybox.stars2Color.getValue();

        if (blindA > 0) {
            matrices.push();
            matrices.multiply(new Quaternionf().rotationXYZ(0, time, 0));
            RenderSystem.setShaderTexture(0, HORIZON);
            renderBuffer(
                    matrices,
                    projectionMatrix,
                    horizon,
                    VertexFormats.POSITION_TEXTURE,
                    horizonColor.getRed() / 255.0f,
                    horizonColor.getGreen() / 255.0f,
                    horizonColor.getBlue() / 255.0f,
                    0.7f * blindA
            );
            matrices.pop();

            matrices.push();
            matrices.multiply(new Quaternionf().rotationXYZ(0, -time, 0));
            RenderSystem.setShaderTexture(0, NEBULA_1);
            renderBuffer(
                    matrices,
                    projectionMatrix,
                    nebula1,
                    VertexFormats.POSITION_TEXTURE,
                    nebula1Color.getRed() / 255.0f,
                    nebula1Color.getGreen() / 255.0f,
                    nebula1Color.getBlue() / 255.0f,
                    blind02
            );
            matrices.pop();

            matrices.push();
            matrices.multiply(new Quaternionf().rotationXYZ(0, time2, 0));
            RenderSystem.setShaderTexture(0, NEBULA_2);
            renderBuffer(
                    matrices,
                    projectionMatrix,
                    nebula2,
                    VertexFormats.POSITION_TEXTURE,
                    nebula2Color.getRed() / 255.0f,
                    nebula2Color.getGreen() / 255.0f,
                    nebula2Color.getBlue() / 255.0f,
                    blind02
            );
            matrices.pop();

            if (skybox.stars.getValue()) {
                RenderSystem.setShaderTexture(0, STARS);

                matrices.push();
                matrices.multiply(new Quaternionf().setAngleAxis(time, axis3.x, axis3.y, axis3.z));
                renderBuffer(
                        matrices,
                        projectionMatrix,
                        stars3,
                        VertexFormats.POSITION_TEXTURE,
                        uvStars1Color.getRed() / 255.0f,
                        uvStars1Color.getGreen() / 255.0f,
                        uvStars1Color.getBlue() / 255.0f,
                        blind06
                );
                matrices.pop();

                matrices.push();
                matrices.multiply(new Quaternionf().setAngleAxis(time2, axis4.x, axis4.y, axis4.z));
                renderBuffer(matrices, projectionMatrix, stars4, VertexFormats.POSITION_TEXTURE,
                        uvStars2Color.getRed() / 255.0f,
                        uvStars2Color.getGreen() / 255.0f,
                        uvStars2Color.getBlue() / 255.0f,
                        blind06);
                matrices.pop();
            }
        }

        float a = (BackgroundInfo.fogDensity - 1.0f);
        if (a > 0) {
            if (a > 1) a = 1;
            RenderSystem.setShaderTexture(0, FOG);
            renderBuffer(
                    matrices,
                    projectionMatrix,
                    fog,
                    VertexFormats.POSITION_TEXTURE,
                    BackgroundInfo.fogColorRed,
                    BackgroundInfo.fogColorGreen,
                    BackgroundInfo.fogColorBlue,
                    a
            );
        }

        if (blindA > 0 && skybox.stars.getValue()) {
            matrices.push();
            matrices.multiply(new Quaternionf().setAngleAxis(time3, axis1.x, axis1.y, axis1.z));
            renderBuffer(matrices, projectionMatrix, stars1, VertexFormats.POSITION,
                    stars1Color.getRed() / 255.0f,
                    stars1Color.getGreen() / 255.0f,
                    stars1Color.getBlue() / 255.0f,
                    blind06);
            matrices.pop();

            matrices.push();
            matrices.multiply(new Quaternionf().setAngleAxis(time2, axis2.x, axis2.y, axis2.z));
            renderBuffer(
                    matrices,
                    projectionMatrix,
                    stars2,
                    VertexFormats.POSITION,
                    stars2Color.getRed() / 255.0f,
                    stars2Color.getGreen() / 255.0f,
                    stars2Color.getBlue() / 255.0f,
                    blind06
            );
            matrices.pop();
        }
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void renderBuffer(
            MatrixStack matrices,
            Matrix4f matrix4f,
            VertexBuffer buffer,
            VertexFormat format,
            float r,
            float g,
            float b,
            float a
    ) {
        RenderSystem.setShaderColor(r, g, b, a);
        buffer.bind();
        if (format == VertexFormats.POSITION) {
            buffer.draw(matrices.peek().getPositionMatrix(), matrix4f, GameRenderer.getPositionProgram());
        } else {
            buffer.draw(matrices.peek().getPositionMatrix(), matrix4f, GameRenderer.getPositionTexProgram());
        }
        VertexBuffer.unbind();
    }

    private void initStars() {
        Tessellator tessellator = Tessellator.getInstance();

        stars1 = buildBuffer(tessellator, stars1, 0.1f, 0.30f, 3500, 41315, this::makeStars);
        stars2 = buildBuffer(tessellator, stars2, 0.1f, 0.35f, 2000, 35151, this::makeStars);
        stars3 = buildBuffer(tessellator, stars3, 0.4f, 1.2f, 1000, 61354, this::makeUVStars);
        stars4 = buildBuffer(tessellator, stars4, 0.4f, 1.2f, 1000, 61355, this::makeUVStars);
        nebula1 = buildBuffer(tessellator, nebula1, 40, 60, 30, 11515, this::makeFarFog);
        nebula2 = buildBuffer(tessellator, nebula2, 40, 60, 10, 14151, this::makeFarFog);
        horizon = buildBufferHorizon(tessellator, horizon);
        fog = buildBufferFog(tessellator, fog);
    }

    private VertexBuffer buildBuffer(
            Tessellator tessellator,
            VertexBuffer vertexBuffer,
            float minSize,
            float maxSize,
            int count,
            long seed,
            BufferFunction fkt
    ) {
        if (vertexBuffer != null) {
            vertexBuffer.close();
        }

        vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        BufferBuilder bufferBuilder = fkt.make(tessellator, minSize, maxSize, count, seed);
        var builtBuffer = bufferBuilder.end();
        vertexBuffer.bind();
        vertexBuffer.upload(builtBuffer);

        return vertexBuffer;
    }

    private VertexBuffer buildBufferHorizon(Tessellator tessellator, VertexBuffer buffer) {
        return buildBuffer(
                tessellator, buffer, 0, 0, 0, 0,
                (_builder, _minSize, _maxSize, _count, _seed) -> makeCylinder(_builder, 16, 50, 100)
        );
    }

    private VertexBuffer buildBufferFog(Tessellator tessellator, VertexBuffer buffer) {
        return buildBuffer(
                tessellator, buffer, 0, 0, 0, 0,
                (_builder, _minSize, _maxSize, _count, _seed) -> makeCylinder(_builder, 16, 50, 70)
        );
    }

    private BufferBuilder makeStars(Tessellator tessellator, float minSize, float maxSize, int count, long seed) {
        CheckedRandom random = new CheckedRandom(seed);
        RenderSystem.setShader(GameRenderer::getPositionProgram);
        final BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);

        for (int i = 0; i < count; ++i) {
            float posX = random.nextFloat() * 2.0f - 1.0f;
            float posY = random.nextFloat() * 2.0f - 1.0f;
            float posZ = random.nextFloat() * 2.0f - 1.0f;
            float size = randRange(minSize, maxSize, random);
            float length = posX * posX + posY * posY + posZ * posZ;

            if (length < 1.0f && length > 0.001f) {
                length = 1.0f / (float) Math.sqrt(length);
                posX *= length;
                posY *= length;
                posZ *= length;

                float px = posX * 100.0f;
                float py = posY * 100.0f;
                float pz = posZ * 100.0f;

                float angle = (float) Math.atan2(posX, posZ);
                float sin1 = (float) Math.sin(angle);
                float cos1 = (float) Math.cos(angle);
                angle = (float) Math.atan2((float) Math.sqrt(posX * posX + posZ * posZ), posY);
                float sin2 = (float) Math.sin(angle);
                float cos2 = (float) Math.cos(angle);
                angle = random.nextFloat() * (float) Math.PI * 2.0f;
                float sin3 = (float) Math.sin(angle);
                float cos3 = (float) Math.cos(angle);

                for (int index = 0; index < 4; ++index) {
                    float x = (float) ((index & 2) - 1) * size;
                    float y = (float) ((index + 1 & 2) - 1) * size;
                    float aa = x * cos3 - y * sin3;
                    float ab = y * cos3 + x * sin3;
                    float dy = aa * sin2 + 0.0f * cos2;
                    float ae = 0.0f * sin2 - aa * cos2;
                    float dx = ae * sin1 - ab * cos1;
                    float dz = ab * sin1 + ae * cos1;
                    buffer.vertex(px + dx, py + dy, pz + dz);
                }
            }
        }

        return buffer;
    }

    private BufferBuilder makeUVStars(Tessellator tessellator, float minSize, float maxSize, int count, long seed) {
        CheckedRandom random = new CheckedRandom(seed);
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        final BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

        for (int i = 0; i < count; ++i) {
            float posX = random.nextFloat() * 2.0f - 1.0f;
            float posY = random.nextFloat() * 2.0f - 1.0f;
            float posZ = random.nextFloat() * 2.0f - 1.0f;
            float size = randRange(minSize, maxSize, random);
            float length = posX * posX + posY * posY + posZ * posZ;

            if (length < 1.0f && length > 0.001f) {
                length = 1.0f / (float) Math.sqrt(length);
                posX *= length;
                posY *= length;
                posZ *= length;

                float px = posX * 100.0f;
                float py = posY * 100.0f;
                float pz = posZ * 100.0f;

                float angle = (float) Math.atan2(posX, posZ);
                float sin1 = (float) Math.sin(angle);
                float cos1 = (float) Math.cos(angle);
                angle = (float) Math.atan2((float) Math.sqrt(posX * posX + posZ * posZ), posY);
                float sin2 = (float) Math.sin(angle);
                float cos2 = (float) Math.cos(angle);
                angle = random.nextFloat() * (float) Math.PI * 2.0f;
                float sin3 = (float) Math.sin(angle);
                float cos3 = (float) Math.cos(angle);

                float minV = random.nextInt(4) / 4.0f;
                for (int index = 0; index < 4; ++index) {
                    float x = (float) ((index & 2) - 1) * size;
                    float y = (float) ((index + 1 & 2) - 1) * size;
                    float aa = x * cos3 - y * sin3;
                    float ab = y * cos3 + x * sin3;
                    float dy = aa * sin2 + 0.0f * cos2;
                    float ae = 0.0f * sin2 - aa * cos2;
                    float dx = ae * sin1 - ab * cos1;
                    float dz = ab * sin1 + ae * cos1;
                    float texU = (index >> 1) & 1;
                    float texV = (((index + 1) >> 1) & 1) / 4.0f + minV;
                    buffer.vertex(px + dx, py + dy, pz + dz).texture(texU, texV);
                }
            }
        }
        return buffer;
    }

    private BufferBuilder makeFarFog(Tessellator tessellator, float minSize, float maxSize, int count, long seed) {
        CheckedRandom random = new CheckedRandom(seed);
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        final BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

        for (int i = 0; i < count; ++i) {
            float posX = random.nextFloat() * 2.0f - 1.0f;
            float posY = random.nextFloat() - 0.5f;
            float posZ = random.nextFloat() * 2.0f - 1.0f;
            float size = randRange(minSize, maxSize, random);
            float length = posX * posX + posY * posY + posZ * posZ;
            float distance = 2.0f;

            if (length < 1.0f && length > 0.001f) {
                length = distance / (float) Math.sqrt(length);
                size *= distance;
                posX *= length;
                posY *= length;
                posZ *= length;

                float px = posX * 100.0f;
                float py = posY * 100.0f;
                float pz = posZ * 100.0f;

                float angle = (float) Math.atan2(posX, posZ);
                float sin1 = (float) Math.sin(angle);
                float cos1 = (float) Math.cos(angle);
                angle = (float) Math.atan2((float) Math.sqrt(posX * posX + posZ * posZ), posY);
                float sin2 = (float) Math.sin(angle);
                float cos2 = (float) Math.cos(angle);
                angle = random.nextFloat() * (float) Math.PI * 2.0f;
                float sin3 = (float) Math.sin(angle);
                float cos3 = (float) Math.cos(angle);

                for (int index = 0; index < 4; ++index) {
                    float x = (float) ((index & 2) - 1) * size;
                    float y = (float) ((index + 1 & 2) - 1) * size;
                    float aa = x * cos3 - y * sin3;
                    float ab = y * cos3 + x * sin3;
                    float dy = aa * sin2 + 0.0f * cos2;
                    float ae = 0.0f * sin2 - aa * cos2;
                    float dx = ae * sin1 - ab * cos1;
                    float dz = ab * sin1 + ae * cos1;
                    float texU = (index >> 1) & 1;
                    float texV = ((index + 1) >> 1) & 1;
                    buffer.vertex(px + dx, py + dy, pz + dz).texture(texU, texV);
                }
            }
        }
        return buffer;
    }

    private BufferBuilder makeCylinder(Tessellator tessellator, int segments, float height, float radius) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        final BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        for (int i = 0; i < segments; i++) {
            float a1 = (float) i * (float) Math.PI * 2.0f / (float) segments;
            float a2 = (float) (i + 1) * (float) Math.PI * 2.0f / (float) segments;
            float px1 = (float) Math.sin(a1) * radius;
            float pz1 = (float) Math.cos(a1) * radius;
            float px2 = (float) Math.sin(a2) * radius;
            float pz2 = (float) Math.cos(a2) * radius;

            float u0 = (float) i / (float) segments;
            float u1 = (float) (i + 1) / (float) segments;

            buffer.vertex(px1, -height, pz1).texture(u0, 0);
            buffer.vertex(px1, height, pz1).texture(u0, 1);
            buffer.vertex(px2, height, pz2).texture(u1, 1);
            buffer.vertex(px2, -height, pz2).texture(u1, 0);
        }
        return buffer;
    }

    private float randRange(float min, float max, CheckedRandom random) {
        return min + random.nextFloat() * (max - min);
    }
}
