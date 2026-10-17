package dev.perish.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.perish.Perish;
import dev.perish.api.utils.math.MathUtil;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.EnumSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
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

public class EsuESP extends Module {
    private final SliderSetting offsetX = add(new SliderSetting("OffsetX", -42, -500, 500));
    private final SliderSetting offsetY = this.add(new SliderSetting("OffsetY", -27, -500, 500));
    private final SliderSetting width = this.add(new SliderSetting("Width", 84, 0, 500));
    private final SliderSetting height = this.add(new SliderSetting("Height", 40, 0, 500));
    private final BooleanSetting noFriend = this.add(new BooleanSetting("NoFriend", true));
    private final EnumSetting<Mode> mode = this.add(new EnumSetting<>("Mode", Mode.zzg));

    public EsuESP() {
        super("EsuESP", "BigHead", Category.Render);
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (mc.world != null) {
            for (PlayerEntity target : mc.world.getPlayers()) {
                if (!this.invalid(target)) {
                    this.drawBurrowESP(matrixStack, target, target.getX(), target.getY() + 1.5, target.getZ());
                }
            }
        }
    }

    private void drawBurrowESP(MatrixStack matrixStack, PlayerEntity target, double x, double y, double z) {
        matrixStack.push();

        double cameraX = mc.gameRenderer.getCamera().getPos().x;
        double cameraY = mc.gameRenderer.getCamera().getPos().y;
        double cameraZ = mc.gameRenderer.getCamera().getPos().z;

        matrixStack.translate(x - cameraX, y - cameraY, z - cameraZ);
        matrixStack.multiply(mc.gameRenderer.getCamera().getRotation());

        double scale = 0.0245;
        matrixStack.scale((float) scale, (float) scale, (float) scale);

        Identifier texture = null;
        switch (this.mode.getValue()) {
            case zzg:
                texture = Identifier.of("perish", "textures/mugshot/zzg.png");
                break;
            case yx:
                texture = Identifier.of("perish", "textures/mugshot/yx.png");
                break;
            case yx2:
                texture = Identifier.of("perish", "textures/mugshot/yx2.png");
                break;
            case yangchao:
                texture = Identifier.of("perish", "textures/mugshot/yangchao.png");
                break;
        }

        if (texture != null) {
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

            float drawX = this.offsetX.getValueInt();
            float drawY = this.offsetY.getValueInt();
            float drawWidth = this.width.getValueInt();
            float drawHeight = this.height.getValueInt();

            drawTexture(matrixStack, drawX, drawY, drawWidth, drawHeight);

            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
        }

        matrixStack.pop();
    }

    private void drawTexture(MatrixStack matrices, float x, float y, float width, float height) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        bufferBuilder.vertex(matrix, x, y + height, 0).texture(0, 0);
        bufferBuilder.vertex(matrix, x + width, y + height, 0).texture(1, 0);
        bufferBuilder.vertex(matrix, x + width, y, 0).texture(1, 1);
        bufferBuilder.vertex(matrix, x, y, 0).texture(0, 1);

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    private boolean invalid(Entity entity) {
        if (mc.player != null) {
            return entity == null
                    || entity.equals(mc.player)
                    || entity instanceof PlayerEntity && Perish.FRIEND.isFriend(entity.getName().getString()) && this.noFriend.getValue()
                    || mc.player.distanceTo(entity) < MathUtil.square(0.5);
        }
        return false;
    }

    public enum Mode {
        zzg,
        yangchao,
        yx,
        yx2,
    }
}