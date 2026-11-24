package dev.g255.arc.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.Render3DEvent;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.ColorSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class LastWalkEsp extends Module {
    private final SliderSetting duration = this.add(new SliderSetting("Duration", 5.0, 1.0, 20.0, 0.1));
    private final SliderSetting width = this.add(new SliderSetting("Width", 1.5, 0.1, 5.0, 0.1));
    private final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 255)));

    private final List<Point> points = new ArrayList<>();

    public LastWalkEsp() {
        super("LastWalkEsp", Module.Category.Render);
        this.setChinese("移动追尾");
    }

    @Override
    public void onEnable() {
        points.clear();
    }

    @EventListener
    private void onUpdate(UpdateEvent event) {
        if (mc.player == null) return;

        long currentTime = System.currentTimeMillis();

        // Remove old points
        points.removeIf(p -> currentTime - p.time > duration.getValue() * 1000);

        // Add new point
        Vec3d pos = mc.player.getPos();
        if (points.isEmpty() || points.get(points.size() - 1).pos.distanceTo(pos) > 0.05) {
            points.add(new Point(pos, currentTime));
        }
    }

    @EventListener
    private void onRender3D(Render3DEvent event) {
        if (points.size() < 2) return;

        long currentTime = System.currentTimeMillis();
        Color c = color.getValue();
        float durationMs = duration.getValueFloat() * 1000.0f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.lineWidth(width.getValueFloat());

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

        Matrix4f matrix = event.matrixStack.peek().getPositionMatrix();
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();

        for (Point p : points) {
            long age = currentTime - p.time;
            if (age > durationMs) continue;

            float alphaRatio = 1.0f - (age / durationMs);
            float a = (c.getAlpha() / 255.0f) * alphaRatio;
            float r = c.getRed() / 255.0f;
            float g = c.getGreen() / 255.0f;
            float b = c.getBlue() / 255.0f;

            float x = (float) (p.pos.x - camPos.x);
            float y = (float) (p.pos.y - camPos.y);
            float z = (float) (p.pos.z - camPos.z);

            buffer.vertex(matrix, x, y, z).color(r, g, b, a);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.lineWidth(1.0F);
        RenderSystem.disableBlend();
    }

    private static class Point {
        public final Vec3d pos;
        public final long time;

        public Point(Vec3d pos, long time) {
            this.pos = pos;
            this.time = time;
        }
    }
}