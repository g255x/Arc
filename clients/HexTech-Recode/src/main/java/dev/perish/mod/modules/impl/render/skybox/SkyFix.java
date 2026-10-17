package dev.perish.mod.modules.impl.render.skybox;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.perish.mod.modules.Module;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.Identifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.*;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class SkyFix extends Module {
    public static SkyFix INSTANCE;
    private static final Identifier STARFIELD = Identifier.of("perish", "textures/starfield03.png");

    public SkyFix() {
        super("SkyFix", "Replace sky with starfield03", Category.Render);
        this.setChinese("星空修复");
        INSTANCE = this;
    }

    public void renderSky(Matrix4f positionMatrix, Matrix4f projectionMatrix, float tickDelta) {
        if (mc.world == null) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderTexture(0, STARFIELD);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

        MatrixStack matrices = new MatrixStack();
        matrices.multiplyPositionMatrix(positionMatrix);

        for (int i = 0; i < 6; ++i) {
            matrices.push();
            if (i == 1) {
                matrices.multiply(new Quaternionf().rotationX((float) Math.PI / 2f));
            } else if (i == 2) {
                matrices.multiply(new Quaternionf().rotationX((float) -Math.PI / 2f));
            } else if (i == 3) {
                matrices.multiply(new Quaternionf().rotationX((float) Math.PI));
            } else if (i == 4) {
                matrices.multiply(new Quaternionf().rotationZ((float) Math.PI / 2f));
            } else if (i == 5) {
                matrices.multiply(new Quaternionf().rotationZ((float) -Math.PI / 2f));
            }

            Matrix4f matrix4f = matrices.peek().getPositionMatrix();
            buffer.vertex(matrix4f, -100.0f, -100.0f, -100.0f).texture(0.0f, 0.0f);
            buffer.vertex(matrix4f, -100.0f, -100.0f, 100.0f).texture(0.0f, 1.0f);
            buffer.vertex(matrix4f, 100.0f, -100.0f, 100.0f).texture(1.0f, 1.0f);
            buffer.vertex(matrix4f, 100.0f, -100.0f, -100.0f).texture(1.0f, 0.0f);
            matrices.pop();
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }
}
