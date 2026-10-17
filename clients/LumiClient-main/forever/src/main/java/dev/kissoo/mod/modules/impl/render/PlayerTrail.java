package dev.kissoo.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.kissoo.api.utils.Wrapper;
import dev.kissoo.mod.modules.settings.impl.*;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.UpdateEvent;
import net.minecraft.client.render.*;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import dev.kissoo.mod.modules.Module;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class PlayerTrail extends Module {

    private final ColorSetting trailColor = this.add(new ColorSetting("Color", new Color(255, 100, 200, 180)));
    private final SliderSetting trailLength = this.add(new SliderSetting("Length", 30, 5, 100));
    private final SliderSetting lineWidth = this.add(new SliderSetting("LineWidth", 0.35f, 0.05f, 2.0f));
    private final SliderSetting lineHeight = this.add(new SliderSetting("LineHeight", 0.9f, 0.05f, 2.0f));
    private final SliderSetting yOffset = this.add(new SliderSetting("YOffset", 0.6f, 0.0f, 2.0f, 0.1f));
    private final BooleanSetting onlySelf = this.add(new BooleanSetting("OnlySelf", true));
    private final SliderSetting updateInterval = this.add(new SliderSetting("UpdateInterval", 1, 1, 10));
    private final SliderSetting minStep = this.add(new SliderSetting("MinStep", 0.08f, 0.01f, 0.5f));
    private final BooleanSetting cuteMode = this.add(new BooleanSetting("CuteMode", false));
    private final SliderSetting cuteStep = this.add(new SliderSetting("CuteStep", 0.35f, 0.1f, 1.5f, 0.05f)
            );
    private final SliderSetting rainbowSpeed = this.add(new SliderSetting("RainbowSpeed", 1.0f, 0.1f, 5.0f, 0.1f)
           );

    private final List<PlayerTrailData> trailDataList = new CopyOnWriteArrayList<>();
    private int updateCounter = 0;
    private float rainbowHue = 0f;

    public PlayerTrail() {
        super("PlayerTrail", "Displays continuous trail line behind players", Module.Category.Render);
        this.setChinese("玩家轨迹线");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        trailDataList.clear();
        updateCounter = 0;
        rainbowHue = 0f;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        trailDataList.clear();
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (!this.isOn()) return;
        if (Wrapper.mc.world == null || Wrapper.mc.player == null) return;
        if (cuteMode.getValue()) {
            rainbowHue += rainbowSpeed.getValueFloat() * 0.5f;
            if (rainbowHue > 360f) rainbowHue -= 360f;
        }

        updateCounter++;
        if (updateCounter < updateInterval.getValue()) return;
        updateCounter = 0;

        for (PlayerEntity player : Wrapper.mc.world.getPlayers()) {
            if (onlySelf.getValue() && player != Wrapper.mc.player) continue;
            if (player.isSpectator()) continue;

            PlayerTrailData trailData = getOrCreateTrailData(player);

            float heightOffset = yOffset.getValueFloat();
            Vec3d currentPos = new Vec3d(
                    player.getX(),
                    player.getY() + player.getEyeHeight(player.getPose()) * heightOffset,
                    player.getZ()
            );

            if (trailData.points.isEmpty() ||
                    trailData.points.get(trailData.points.size() - 1).position.distanceTo(currentPos) >= minStep.getValueFloat()) {

                Color pointColor = getTrailColor();
                trailData.points.add(new TrailPoint(currentPos, pointColor, (int)trailLength.getValue()));
                while (trailData.points.size() > trailLength.getValue()) {
                    trailData.points.remove(0);
                }
            }
        }

        trailDataList.forEach(data -> data.points.removeIf(TrailPoint::shouldRemove));
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (!this.isOn()) return;
        if (Wrapper.mc.world == null || Wrapper.mc.player == null) return;

        if (trailDataList.isEmpty()) return;

        Camera camera = Wrapper.mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();
        if (cuteMode.getValue()) {
            renderCuteMode(matrixStack, camPos);
        } else {
            renderNormalMode(matrixStack, camPos);
        }
    }

    private Color getTrailColor() {
        if (cuteMode.getValue()) {
            float hue = rainbowHue / 360f;
            return Color.getHSBColor(hue, 0.8f, 1f);
        }
        return trailColor.getValue();
    }

    private Color applyAlpha(Color c, float alpha) {
        int a = MathHelper.clamp((int)(c.getAlpha() * alpha), 0, 255);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    private void renderNormalMode(MatrixStack matrixStack, Vec3d camPos) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder buffer = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.TRIANGLE_STRIP,
                VertexFormats.POSITION_COLOR
        );

        Matrix4f matrix = matrixStack.peek().getPositionMatrix();
        float width = lineWidth.getValueFloat();
        float height = lineHeight.getValueFloat();

        for (PlayerTrailData trailData : trailDataList) {
            if (onlySelf.getValue() && trailData.player != Wrapper.mc.player) continue;
            if (trailData.points.size() < 2) continue;

            for (TrailPoint point : trailData.points) {
                Vec3d relativePos = point.position.subtract(camPos);
                float alpha = point.getAlpha();

                Color c = applyAlpha(point.color, alpha);

                buffer.vertex(matrix, (float)relativePos.x, (float)relativePos.y, (float)relativePos.z)
                        .color(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f);

                buffer.vertex(matrix, (float)relativePos.x, (float)(relativePos.y + height), (float)relativePos.z)
                        .color(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f);
            }
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void renderCuteMode(MatrixStack matrixStack, Vec3d camPos) {
        Color[] colors = {
                new Color(69, 221, 255, 180),
                new Color(248, 139, 160, 180),
                new Color(255, 255, 255, 180),
                new Color(248, 139, 160, 180),
                new Color(69, 221, 255, 180)
        };

        float step = cuteStep.getValueFloat();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        for (int layer = 0; layer < 5; layer++) {
            renderCuteLayer(matrixStack, camPos, colors[layer], layer * step, step);
        }

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void renderCuteLayer(MatrixStack matrixStack, Vec3d camPos, Color color, float layerYOffset, float layerHeight) {
        boolean hasData = false;

        for (PlayerTrailData trailData : trailDataList) {
            if (onlySelf.getValue() && trailData.player != Wrapper.mc.player) continue;
            if (trailData.points.size() < 2) continue;

            hasData = true;
            break;
        }

        if (!hasData) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder buffer = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.TRIANGLE_STRIP,
                VertexFormats.POSITION_COLOR
        );

        Matrix4f matrix = matrixStack.peek().getPositionMatrix();
        int vertexCountBefore = 0;

        for (PlayerTrailData trailData : trailDataList) {
            if (onlySelf.getValue() && trailData.player != Wrapper.mc.player) continue;
            if (trailData.points.size() < 2) continue;

            for (int i = 0; i < trailData.points.size(); i++) {
                TrailPoint point = trailData.points.get(i);
                Vec3d relativePos = point.position.subtract(camPos);
                float alpha = point.getAlpha();

                Color c = applyAlpha(color, alpha * 0.8f);
                buffer.vertex(matrix,
                                (float)relativePos.x,
                                (float)(relativePos.y + layerYOffset),
                                (float)relativePos.z)
                        .color(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f);
                buffer.vertex(matrix,
                                (float)relativePos.x,
                                (float)(relativePos.y + layerYOffset + layerHeight),
                                (float)relativePos.z)
                        .color(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f);
            }
        }
        try {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("BufferBuilder was empty")) {
                // System.err.println("PlayerTrail: BufferBuilder为空，跳过渲染");
            } else {
                throw e;
            }
        }
    }

    private PlayerTrailData getOrCreateTrailData(PlayerEntity player) {
        for (PlayerTrailData data : trailDataList) {
            if (data.player == player) {
                return data;
            }
        }

        PlayerTrailData newData = new PlayerTrailData(player);
        trailDataList.add(newData);
        return newData;
    }

    private static class PlayerTrailData {
        public final PlayerEntity player;
        public final List<TrailPoint> points;

        public PlayerTrailData(PlayerEntity player) {
            this.player = player;
            this.points = new ArrayList<>();
        }
    }

    private static class TrailPoint {
        public final Vec3d position;
        public final Color color;
        public int life;
        public int prevLife;
        public final int maxLife;

        public TrailPoint(Vec3d position, Color color, int maxLife) {
            this.position = position;
            this.color = color;
            this.life = maxLife;
            this.prevLife = maxLife;
            this.maxLife = maxLife;
        }

        public boolean shouldRemove() {
            prevLife = life;
            return --life <= 0;
        }

        public float getAlpha() {
            float factor = (float)life / maxLife;
            return factor * factor; // 二次衰减
        }
    }
}