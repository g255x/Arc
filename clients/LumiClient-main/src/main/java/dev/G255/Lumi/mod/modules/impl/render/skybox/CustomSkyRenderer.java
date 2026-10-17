package dev.G255.Lumi.mod.modules.impl.render.skybox;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.G255.Lumi.mod.modules.impl.render.Skybox;
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

public class CustomSkyRenderer {

    @FunctionalInterface
    interface BufferFunction {
        BufferBuilder make(Tessellator tessellator, float minSize, float maxSize, int count, long seed);
    }

    private static final Identifier NEBULA_1 = Identifier.of("lumi", "skybox/nebula_2.png");
    private static final Identifier NEBULA_2 = Identifier.of("lumi", "skybox/nebula_3.png");
    private static final Identifier HORIZON = Identifier.of("lumi", "skybox/nebula_1.png");
    private static final Identifier STARS = Identifier.of("lumi", "skybox/stars.png");
    private static final Identifier FOG = Identifier.of("lumi", "skybox/fog.png");

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
    private float lastStarSize = -1.0f;

    private void initialise() {
        if (!initialised) {
            initStars(1.0f);
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

    public void render(float tickDelta, Matrix4f positionMatrix, Matrix4f projectionMatrix) {
        Skybox skybox = Skybox.INSTANCE;
        if (skybox == null || !skybox.isOn()) {
            return;
        }

        initialise();

        float currentStarSize = skybox.starSize.getValueFloat();
        if (currentStarSize != lastStarSize) {
            lastStarSize = currentStarSize;
            initStars(currentStarSize);
        }

        MatrixStack matrices = new MatrixStack();
        matrices.multiplyPositionMatrix(positionMatrix);

        float time = ((skybox.mc.world.getTimeOfDay() + tickDelta) % 360000) * 0.000017453292f;
        float time2 = time * 2;
        float time3 = time * 3;

        BackgroundRenderer.applyFogColor();
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.depthMask(false);

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
        RenderSystem.depthMask(true);
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

    private void initStars(float scale) {
        Tessellator tessellator = Tessellator.getInstance();

        stars1 = buildBuffer(tessellator, stars1, 0.1f * scale, 0.30f * scale, 3500, 41315, this::makeStars);
        stars2 = buildBuffer(tessellator, stars2, 0.1f * scale, 0.35f * scale, 2000, 35151, this::makeStars);
        stars3 = buildBuffer(tessellator, stars3, 0.4f * scale, 1.2f * scale, 1000, 61354, this::makeUVStars);
        stars4 = buildBuffer(tessellator, stars4, 0.4f * scale, 1.2f * scale, 1000, 61355, this::makeUVStars);
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
        final BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION);

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

                float[] vx = new float[4];
                float[] vy = new float[4];
                float[] vz = new float[4];
                for (int index = 0; index < 4; ++index) {
                    float x = (float) ((index & 2) - 1) * size;
                    float y = (float) ((index + 1 & 2) - 1) * size;
                    float aa = x * cos3 - y * sin3;
                    float ab = y * cos3 + x * sin3;
                    float dy = aa * sin2 + 0.0f * cos2;
                    float ae = 0.0f * sin2 - aa * cos2;
                    float dx = ae * sin1 - ab * cos1;
                    float dz = ab * sin1 + ae * cos1;
                    vx[index] = px + dx;
                    vy[index] = py + dy;
                    vz[index] = pz + dz;
                }
                buffer.vertex(vx[0], vy[0], vz[0]);
                buffer.vertex(vx[1], vy[1], vz[1]);
                buffer.vertex(vx[2], vy[2], vz[2]);
                buffer.vertex(vx[0], vy[0], vz[0]);
                buffer.vertex(vx[2], vy[2], vz[2]);
                buffer.vertex(vx[3], vy[3], vz[3]);
            }
        }
        return buffer;
    }

    private BufferBuilder makeUVStars(Tessellator tessellator, float minSize, float maxSize, int count, long seed) {
        CheckedRandom random = new CheckedRandom(seed);
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        final BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE);

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

                float[] vx = new float[4];
                float[] vy = new float[4];
                float[] vz = new float[4];
                for (int index = 0; index < 4; ++index) {
                    float x = (float) ((index & 2) - 1) * size;
                    float y = (float) ((index + 1 & 2) - 1) * size;
                    float aa = x * cos3 - y * sin3;
                    float ab = y * cos3 + x * sin3;
                    float dy = aa * sin2 + 0.0f * cos2;
                    float ae = 0.0f * sin2 - aa * cos2;
                    float dx = ae * sin1 - ab * cos1;
                    float dz = ab * sin1 + ae * cos1;
                    vx[index] = px + dx;
                    vy[index] = py + dy;
                    vz[index] = pz + dz;
                }
                float u0 = 0.0f, v0 = 0.0f;
                float u1 = 0.0f, v1 = 1.0f;
                float u2 = 1.0f, v2 = 1.0f;
                float u3 = 1.0f, v3 = 0.0f;
                buffer.vertex(vx[0], vy[0], vz[0]).texture(u0, v0);
                buffer.vertex(vx[1], vy[1], vz[1]).texture(u1, v1);
                buffer.vertex(vx[2], vy[2], vz[2]).texture(u2, v2);
                buffer.vertex(vx[0], vy[0], vz[0]).texture(u0, v0);
                buffer.vertex(vx[2], vy[2], vz[2]).texture(u2, v2);
                buffer.vertex(vx[3], vy[3], vz[3]).texture(u3, v3);
            }
        }
        return buffer;
    }

    private BufferBuilder makeFarFog(Tessellator tessellator, float minSize, float maxSize, int count, long seed) {
        CheckedRandom random = new CheckedRandom(seed);
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        final BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE);

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

                float[] vx = new float[4];
                float[] vy = new float[4];
                float[] vz = new float[4];
                for (int index = 0; index < 4; ++index) {
                    float x = (float) ((index & 2) - 1) * size;
                    float y = (float) ((index + 1 & 2) - 1) * size;
                    float aa = x * cos3 - y * sin3;
                    float ab = y * cos3 + x * sin3;
                    float dy = aa * sin2 + 0.0f * cos2;
                    float ae = 0.0f * sin2 - aa * cos2;
                    float dx = ae * sin1 - ab * cos1;
                    float dz = ab * sin1 + ae * cos1;
                    vx[index] = px + dx;
                    vy[index] = py + dy;
                    vz[index] = pz + dz;
                }
                float u0 = 0.0f, v0 = 0.0f;
                float u1 = 0.0f, v1 = 1.0f;
                float u2 = 1.0f, v2 = 1.0f;
                float u3 = 1.0f, v3 = 0.0f;
                buffer.vertex(vx[0], vy[0], vz[0]).texture(u0, v0);
                buffer.vertex(vx[1], vy[1], vz[1]).texture(u1, v1);
                buffer.vertex(vx[2], vy[2], vz[2]).texture(u2, v2);
                buffer.vertex(vx[0], vy[0], vz[0]).texture(u0, v0);
                buffer.vertex(vx[2], vy[2], vz[2]).texture(u2, v2);
                buffer.vertex(vx[3], vy[3], vz[3]).texture(u3, v3);
            }
        }
        return buffer;
    }

    private BufferBuilder makeCylinder(Tessellator tessellator, int segments, float radius, float height) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_TEXTURE);

        for (int i = 0; i <= segments; ++i) {
            float angle = (float) (i * 2.0 * Math.PI / segments);
            float x = (float) Math.cos(angle) * radius;
            float z = (float) Math.sin(angle) * radius;
            float u = (float) i / (float) segments;
            buffer.vertex(x, 0.0f, z).texture(u, 0.0f);
            buffer.vertex(x, height, z).texture(u, 1.0f);
        }
        return buffer;
    }

    private static float randRange(float min, float max, CheckedRandom random) {
        return min + random.nextFloat() * (max - min);
    }
}