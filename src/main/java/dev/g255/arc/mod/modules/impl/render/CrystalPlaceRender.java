package dev.g255.arc.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.EntitySpawnedEvent;
import dev.g255.arc.api.utils.render.Render3DUtil;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.ColorSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CrystalPlaceRender extends Module {
    private static final int CIRCLE_SEGMENTS = 90;

    private final BooleanSetting checkRange = this.add(new BooleanSetting("CheckRange", true).setParent());
    private final SliderSetting range = this.add(new SliderSetting("Range", 12.0, 0.0, 256.0, 0.5, this.checkRange::isOpen));
    private final SliderSetting animationTime = this.add(new SliderSetting("AnimationTime", 500, 50, 3000, 10).setSuffix("ms"));
    private final SliderSetting fadeSpeed = this.add(new SliderSetting("FadeSpeed", 500, 1, 3000, 10));
    private final SliderSetting lineWidth = this.add(new SliderSetting("LineWidth", 2.0, 0.5, 5.0, 0.1));
    private final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 255)));
    private final EnumSetting<Mode> mode = this.add(new EnumSetting<>("Mode", Mode.Rise));
    private final SliderSetting translateDistance = this.add(
            new SliderSetting("TranslateDistance", 1.0, 0.1, 5.0, 0.1, () -> this.mode.is(Mode.Translate))
    );

    private final Map<EndCrystalEntity, RenderInfo> crystals = new ConcurrentHashMap<>();

    public CrystalPlaceRender() {
        super("CrystalPlaceRender", Category.Render);
        this.setChinese("\u6c34\u6676\u653e\u7f6e\u6e32\u67d3");
    }

    @Override
    public void onEnable() {
        this.crystals.clear();
    }

    @Override
    public void onDisable() {
        this.crystals.clear();
    }

    @Override
    public void onLogout() {
        this.crystals.clear();
    }

    @EventListener
    public void onEntitySpawned(EntitySpawnedEvent event) {
        if (nullCheck() || !(event.getEntity() instanceof EndCrystalEntity crystal)) {
            return;
        }
        if (this.checkRange.getValue() && mc.player.distanceTo(crystal) > this.range.getValue()) {
            return;
        }
        Vec3d direction = new Vec3d(
                crystal.getX() - mc.player.getX(),
                0.0,
                crystal.getZ() - mc.player.getZ()
        );
        if (direction.lengthSquared() < 1.0E-4) {
            double yaw = Math.toRadians(mc.player.getYaw());
            direction = new Vec3d(-Math.sin(yaw), 0.0, Math.cos(yaw));
        } else {
            direction = direction.normalize();
        }
        this.crystals.put(crystal, new RenderInfo(System.currentTimeMillis(), direction.x, direction.z));
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (nullCheck() || this.crystals.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        long duration = this.animationTime.getValueInt();
        this.crystals.entrySet().removeIf(entry -> now - entry.getValue().time() > duration);
        if (this.crystals.isEmpty()) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.lineWidth(this.lineWidth.getValueFloat());

        try {
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            Matrix4f matrix = matrixStack.peek().getPositionMatrix();
            Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
            float tickDelta = mc.getRenderTickCounter().getTickDelta(false);
            Color renderColor = this.color.getValue();

            for (Map.Entry<EndCrystalEntity, RenderInfo> entry : this.crystals.entrySet()) {
                EndCrystalEntity crystal = entry.getKey();
                if (this.checkRange.getValue() && mc.player.distanceTo(crystal) > this.range.getValue()) {
                    continue;
                }

                RenderInfo renderInfo = entry.getValue();
                this.appendAnimatedCircle(
                        buffer,
                        matrix,
                        crystal,
                        renderInfo,
                        now - renderInfo.time(),
                        duration,
                        cameraPos,
                        tickDelta,
                        renderColor
                );
            }

            Render3DUtil.endBuilding(buffer);
        } finally {
            RenderSystem.lineWidth(1.0F);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }

    private void appendAnimatedCircle(
            BufferBuilder buffer,
            Matrix4f matrix,
            Entity entity,
            RenderInfo renderInfo,
            long age,
            long duration,
            Vec3d cameraPos,
            float tickDelta,
            Color color
    ) {
        if (age < 0L || age > duration) {
            return;
        }

        float progress = duration <= 0L ? 1.0F : Math.min(1.0F, age / (float) duration);
        float radius = age / this.fadeSpeed.getValueFloat();
        float height = this.mode.is(Mode.Rise) ? age / 1000.0F : 0.0F;
        double horizontalOffset = this.mode.is(Mode.Translate)
                ? progress * this.translateDistance.getValue()
                : 0.0;
        float alpha = color.getAlpha() / 255.0F * (1.0F - progress);
        if (alpha <= 0.0F) {
            return;
        }

        double x = entity.prevX + (entity.getX() - entity.prevX) * tickDelta - cameraPos.x
                + renderInfo.directionX() * horizontalOffset;
        double y = entity.prevY + (entity.getY() - entity.prevY) * tickDelta - cameraPos.y + height;
        double z = entity.prevZ + (entity.getZ() - entity.prevZ) * tickDelta - cameraPos.z
                + renderInfo.directionZ() * horizontalOffset;
        float red = color.getRed() / 255.0F;
        float green = color.getGreen() / 255.0F;
        float blue = color.getBlue() / 255.0F;

        for (int segment = 0; segment < CIRCLE_SEGMENTS; segment++) {
            double angle1 = Math.PI * 2.0 * segment / CIRCLE_SEGMENTS;
            double angle2 = Math.PI * 2.0 * (segment + 1) / CIRCLE_SEGMENTS;
            buffer.vertex(matrix, (float) (x + radius * Math.cos(angle1)), (float) y, (float) (z + radius * Math.sin(angle1)))
                    .color(red, green, blue, alpha);
            buffer.vertex(matrix, (float) (x + radius * Math.cos(angle2)), (float) y, (float) (z + radius * Math.sin(angle2)))
                    .color(red, green, blue, alpha);
        }
    }

    private record RenderInfo(long time, double directionX, double directionZ) {
    }

    public enum Mode {
        Rise,
        Translate
    }
}
