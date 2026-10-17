package dev.perish.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.ClientTickEvent;
import dev.perish.api.events.impl.Render3DEvent;
import dev.perish.api.utils.Wrapper;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.ColorSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JumpCircle extends Module {
    // 图片资源
    private static final Identifier CIRCLE_IMAGE = Identifier.of("perish", "textures/jumpcircle/1.png");
    // 基本设置
    private final BooleanSetting onlyOnGround = this.add(new BooleanSetting("OnlyOnGround", false));
    private final BooleanSetting onlyJump = this.add(new BooleanSetting("OnlyJump", true));
    private final SliderSetting maxCircles = this.add(new SliderSetting("MaxCircles", 5, 1, 20, 1));
    // 外观设置
    private final SliderSetting startSize = this.add(new SliderSetting("StartSize", 0.3, 0.1, 5.0, 0.1));
    private final SliderSetting endSize = this.add(new SliderSetting("EndSize", 3.9, 0.5, 10.0, 0.1));
    private final SliderSetting duration = this.add(new SliderSetting("Duration", 2.0, 0.5, 10.0, 0.1));
    private final SliderSetting fadeStart = this.add(new SliderSetting("FadeStart", 0.46, 0.0, 1.0, 0.01));
    private final ColorSetting color = this.add(new ColorSetting("Color", new java.awt.Color(255, 255, 255, 180)));
    // 旋转设置
    private final SliderSetting rotationSpeed = this.add(new SliderSetting("RotationSpeed", 1.0, -5.0, 5.0, 0.1));
    private final SliderSetting imageOpacity = this.add(new SliderSetting("ImageOpacity", 0.51, 0.1, 1.0, 0.01));
    // 平面设置
    private final SliderSetting heightOffset = this.add(new SliderSetting("HeightOffset", -0.4, -0.5, 0.5, 0.01));
    private final BooleanSetting faceUp = this.add(new BooleanSetting("FaceUp", false));
    private final List<CircleData> circles = new ArrayList<>();
    private boolean wasOnGround = true;
    private double lastY = 0;
    private long lastJumpTime = 0;
    public JumpCircle() {
        super("JumpCircle", Module.Category.Render);
        this.setChinese("跳跃圆环");
    }

    @Override
    public void onEnable() {
        circles.clear();
        wasOnGround = true;
        if (Wrapper.mc.player != null) {
            lastY = Wrapper.mc.player.getY();
        }
    }

    @Override
    public void onDisable() {
        circles.clear();
    }

    @EventListener
    private void onTick(ClientTickEvent event) {
        if (!event.isPre() || Wrapper.mc.player == null) return;

        boolean onGround = Wrapper.mc.player.isOnGround();
        double currentY = Wrapper.mc.player.getY();

        // 检测跳跃
        boolean isJumping = false;
        if (onlyJump.getValue()) {
            // 检测跳跃：Y坐标增加且之前在地面上
            isJumping = currentY > lastY && wasOnGround;
            long currentTime = System.currentTimeMillis();
            if (isJumping && currentTime - lastJumpTime > 500) {
                lastJumpTime = currentTime;
            }
        } else {
            // 只要离开地面就触发
            isJumping = !onGround && wasOnGround;
        }

        // 创建新圆环
        if (isJumping) {
            if (!onlyOnGround.getValue() || Wrapper.mc.player.isOnGround()) {
                createCircle();
            }
        }

        wasOnGround = onGround;
        lastY = currentY;

        // 更新现有圆环
        updateCircles();
    }

    private void createCircle() {
        if (Wrapper.mc.player == null) return;

        // 限制最大圆环数量
        if (circles.size() >= maxCircles.getValueInt()) {
            circles.remove(0);
        }

        // 在地面位置创建圆环
        Vec3d pos = Wrapper.mc.player.getPos().add(0, heightOffset.getValue(), 0);
        circles.add(new CircleData(pos));
    }

    private void updateCircles() {
        long currentTime = System.currentTimeMillis();
        Iterator<CircleData> iterator = circles.iterator();

        while (iterator.hasNext()) {
            CircleData circle = iterator.next();
            long elapsed = currentTime - circle.startTime;
            float elapsedSeconds = elapsed / 1000.0f;

            // 计算进度
            circle.progress = Math.min(elapsedSeconds / duration.getValueFloat(), 1.0f);

            // 计算旋转
            circle.rotation += rotationSpeed.getValueFloat() * 2.0f;
            if (circle.rotation > 360) circle.rotation -= 360;
            if (circle.rotation < 0) circle.rotation += 360;

            // 移除完成的圆环
            if (circle.progress >= 1.0f) {
                iterator.remove();
            }
        }
    }

    @EventListener
    private void onRender3D(Render3DEvent event) {
        if (circles.isEmpty()) return;

        // 保存渲染状态
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);

        // 绑定纹理
        RenderSystem.setShaderTexture(0, CIRCLE_IMAGE);
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);

        for (CircleData circle : circles) {
            renderImageOnGround(event.matrixStack, circle);
        }

        // 恢复渲染状态
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void renderImageOnGround(MatrixStack matrices, CircleData circle) {
        // 计算大小
        float progress = easeOutCubic(circle.progress);
        float size = startSize.getValueFloat() + (endSize.getValueFloat() - startSize.getValueFloat()) * progress;

        // 计算透明度
        float alpha = 1.0f;
        if (circle.progress > fadeStart.getValueFloat()) {
            float fadeProgress = (circle.progress - fadeStart.getValueFloat()) / (1.0f - fadeStart.getValueFloat());
            alpha = 1.0f - fadeProgress;
        }

        if (alpha <= 0) return;

        // 设置颜色
        java.awt.Color awtColor = color.getValue();
        float r = awtColor.getRed() / 255.0f;
        float g = awtColor.getGreen() / 255.0f;
        float b = awtColor.getBlue() / 255.0f;
        float a = awtColor.getAlpha() / 255.0f * alpha * imageOpacity.getValueFloat();

        RenderSystem.setShaderColor(r, g, b, a);

        // 渲染地面图片
        renderFlatImage(matrices, circle.position, size, circle.rotation);
    }

    private void renderFlatImage(MatrixStack matrices, Vec3d pos, float size, float rotation) {
        matrices.push();

        // 平移到图片位置
        Camera camera = Wrapper.mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();
        matrices.translate(
                pos.x - cameraPos.x,
                pos.y - cameraPos.y,
                pos.z - cameraPos.z
        );

        // 确保图片平躺在地面上（Y轴旋转用于方向，Z轴旋转用于自转）
        if (faceUp.getValue()) {
            // 图片朝上平躺
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(90));
        }

        // 应用旋转（在地面平面上旋转）
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(rotation));

        // 应用缩放
        float halfSize = size / 2.0f;
        matrices.scale(halfSize, 1.0f, halfSize);

        // 渲染四边形
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // 定义地面的四个顶点（在XZ平面上）
        buffer.vertex(matrix, -1, 0, -1)  // 后左
                .texture(0.0f, 1.0f)
                .color(1.0f, 1.0f, 1.0f, 1.0f);

        buffer.vertex(matrix, -1, 0, 1)   // 前左
                .texture(0.0f, 0.0f)
                .color(1.0f, 1.0f, 1.0f, 1.0f);

        buffer.vertex(matrix, 1, 0, 1)    // 前右
                .texture(1.0f, 0.0f)
                .color(1.0f, 1.0f, 1.0f, 1.0f);

        buffer.vertex(matrix, 1, 0, -1)   // 后右
                .texture(1.0f, 1.0f)
                .color(1.0f, 1.0f, 1.0f, 1.0f);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        matrices.pop();
    }

    // 或者使用更简单的版本：在玩家脚下渲染旋转的平面
    private void renderSimpleFlatImage(MatrixStack matrices, Vec3d pos, float size, float rotation) {
        matrices.push();

        // 平移到位置
        Camera camera = Wrapper.mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();
        matrices.translate(
                pos.x - cameraPos.x,
                pos.y - cameraPos.y,
                pos.z - cameraPos.z
        );

        // 直接渲染平面四边形，不进行额外旋转
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        // 计算旋转后的四个顶点
        float angle = (float) Math.toRadians(rotation);
        float sin = MathHelper.sin(angle);
        float cos = MathHelper.cos(angle);
        float halfSize = size / 2.0f;

        // 四个顶点的原始位置
        float[][] vertices = {
                {-halfSize, 0, -halfSize}, // 左下
                {-halfSize, 0, halfSize},  // 左上
                {halfSize, 0, halfSize},   // 右上
                {halfSize, 0, -halfSize}   // 右下
        };

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // 应用旋转并渲染每个顶点
        for (int i = 0; i < 4; i++) {
            float x = vertices[i][0];
            float z = vertices[i][2];

            // 应用旋转（绕Y轴）
            float rotatedX = x * cos - z * sin;
            float rotatedZ = x * sin + z * cos;

            buffer.vertex(matrix, rotatedX, vertices[i][1], rotatedZ)
                    .texture(i == 0 || i == 3 ? 0.0f : 1.0f, i == 0 || i == 1 ? 1.0f : 0.0f)
                    .color(1.0f, 1.0f, 1.0f, 1.0f);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        matrices.pop();
    }

    // 缓动函数
    private float easeOutCubic(float x) {
        return 1 - (float) Math.pow(1 - x, 3);
    }

    // 触发圆环（测试用）
    public void trigger() {
        if (isOn()) {
            createCircle();
        }
    }

    // 圆环数据类
    private static class CircleData {
        public Vec3d position;
        public long startTime;
        public float progress = 0.0f;
        public float rotation = 0.0f;

        public CircleData(Vec3d position) {
            this.position = position;
            this.startTime = System.currentTimeMillis();
        }
    }
}