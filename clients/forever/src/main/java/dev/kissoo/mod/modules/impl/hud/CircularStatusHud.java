package dev.kissoo.mod.modules.impl.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.kissoo.api.utils.skija.SkijaGlowRenderer;
import dev.kissoo.mod.modules.HudElement;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

import java.awt.Color;

public class CircularStatusHud extends Module implements HudElement {
    public static CircularStatusHud INSTANCE;

    // 动画控制变量
    private float currentFade = 1.0f;
    private long lastRenderTime = 0;

    // 准星模式枚举 (无, 方形, 弧形拼合, 实心圆)
    public enum CrosshairMode {
        None, Square, Arcs, SolidCircle
    }

    // 准星与中心点设置
    public final BooleanSetting hideVanillaCrosshair = add(new BooleanSetting("HideVanillaCrosshair", true));
    private final EnumSetting<CrosshairMode> crosshairMode = add(new EnumSetting<>("CrosshairMode", CrosshairMode.SolidCircle, () -> hideVanillaCrosshair.getValue()));

    // 准星通用大小与颜色
    private final SliderSetting crosshairSize = add(new SliderSetting("CrosshairSize", 4.0, 1.0, 20.0, 0.5, () -> hideVanillaCrosshair.getValue() && crosshairMode.getValue() != CrosshairMode.None));
    private final ColorSetting crosshairColor = add(new ColorSetting("CrosshairColor", new Color(255, 255, 255, 255)));

    // 准星专属参数 (仅在 Arcs 模式下生效)
    private final SliderSetting crosshairThick = add(new SliderSetting("CrosshairThick", 1.5, 0.5, 5.0, 0.1, () -> hideVanillaCrosshair.getValue() && crosshairMode.getValue() == CrosshairMode.Arcs));
    private final SliderSetting crosshairGap = add(new SliderSetting("CrosshairGap", 30.0, 0.0, 80.0, 1.0, () -> hideVanillaCrosshair.getValue() && crosshairMode.getValue() == CrosshairMode.Arcs));

    // 外圈几何外观设置
    private final SliderSetting radius = add(new SliderSetting("Radius", 25.0, 10.0, 100.0, 1.0));
    private final SliderSetting thickness = add(new SliderSetting("Thickness", 4.0, 1.0, 20.0, 0.5));
    private final SliderSetting gap = add(new SliderSetting("GapDegree", 10.0, 0.0, 30.0, 1.0));

    // 发光参数设置 (Glow 为 0 时即等同于关闭发光)
    private final SliderSetting glow = add(new SliderSetting("Glow", 8.0, 0.0, 20.0, 0.5));
    private final SliderSetting glowIntensity = add(new SliderSetting("GlowIntensity", 2.0, 1.0, 5.0, 1.0));

    // 透明度与淡入淡出
    private final SliderSetting globalAlpha = add(new SliderSetting("GlobalAlpha", 1.0, 0.1, 1.0, 0.01));
    private final SliderSetting fadeRange = add(new SliderSetting("FadeRange", 20.0, 0.0, 120.0, 1.0).setSuffix("m"));

    // 颜色设置
    private final ColorSetting bgColor = add(new ColorSetting("BgColor", new Color(40, 40, 40, 150)));
    private final ColorSetting healthColor = add(new ColorSetting("HealthColor", new Color(255, 50, 50, 255)));
    private final ColorSetting absorbColor = add(new ColorSetting("AbsorbColor", new Color(255, 200, 0, 255)));
    private final ColorSetting turtleColor = add(new ColorSetting("TurtleColor", new Color(0, 200, 150, 255)));

    public CircularStatusHud() {
        super("CircularStatus", Category.HUD);
        setChinese("环形状态栏");
        INSTANCE = this;
    }

    /**
     * 给 Mixin 调用的静态方法，判断是否隐藏原版准星
     */
    public static boolean shouldHideVanillaCrosshair() {
        return INSTANCE != null && INSTANCE.isOn() && INSTANCE.hideVanillaCrosshair.getValue();
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        renderElement(drawContext, tickDelta);
    }

    @Override
    public void renderElement(DrawContext context, float tickDelta) {
        if (mc.player == null || mc.getWindow() == null) return;

        int centerX = mc.getWindow().getScaledWidth() / 2;
        int centerY = mc.getWindow().getScaledHeight() / 2;

        // =======================================================
        // 绘制自定义准星 (常驻显示，不参与附近的玩家淡入淡出判断)
        // =======================================================
        if (hideVanillaCrosshair.getValue() && crosshairMode.getValue() != CrosshairMode.None) {
            float size = crosshairSize.getValueFloat();

            if (crosshairMode.getValue() == CrosshairMode.Square) {
                // 1. 绘制方形点
                float offset = size / 2.0f;
                context.fill((int)(centerX - offset), (int)(centerY - offset),
                        (int)(centerX - offset + size), (int)(centerY - offset + size),
                        crosshairColor.getValue().getRGB());

            } else if (crosshairMode.getValue() == CrosshairMode.SolidCircle) {
                // 2. 绘制实心圆 (利用 Skija 完美抗锯齿：线粗设为直径的一半，半径设为直径的四分之一，画满360度即可完全填满圆心)
                float r = size / 4.0f;
                float thick = size / 2.0f;
                drawSkijaArc(context, centerX, centerY, r, thick, 0.0f, 360.0f, crosshairColor.getValue(), 0.0f, 0, 1.0f);

            } else if (crosshairMode.getValue() == CrosshairMode.Arcs) {
                // 3. 绘制四个弧形组成的准星圈
                float thick = crosshairThick.getValueFloat();
                float gapDeg = crosshairGap.getValueFloat();
                float sweep = 90.0f - gapDeg;

                if (sweep > 0) {
                    float[] centers = {0, 90, 180, 270};
                    for (float cAngle : centers) {
                        float start = cAngle - sweep / 2.0f;
                        drawSkijaArc(context, centerX, centerY, size, thick, start, sweep, crosshairColor.getValue(), 0.0f, 0, 1.0f);
                    }
                }
            }
        }

        // =======================================================
        // 绘制外圈状态栏 (参与周围玩家淡出判断)
        // =======================================================

        long currentTime = System.currentTimeMillis();
        if (lastRenderTime == 0) lastRenderTime = currentTime;
        long delta = currentTime - lastRenderTime;
        lastRenderTime = currentTime;

        float fadeSpeed = delta / 300.0f;
        if (hasPlayerInRange()) {
            currentFade = Math.min(1.0f, currentFade + fadeSpeed);
        } else {
            currentFade = Math.max(0.0f, currentFade - fadeSpeed);
        }

        if (currentFade <= 0.0f) return;

        float gR = glow.getValueFloat();
        int gInt = glowIntensity.getValueInt();

        float r = radius.getValueFloat();
        float t = thickness.getValueFloat();
        float g = gap.getValueFloat();

        float maxSweep = 120 - g;

        // 左瓣(血量): 逆时针
        float leftBgStart = 270 - g / 2f;
        float leftBgSweep = -maxSweep;
        int hpSteps = Math.round(mc.player.getHealth());
        float hpRatio = MathHelper.clamp(hpSteps / 20.0f, 0.0f, 1.0f);
        float leftSweep = -maxSweep * hpRatio;

        // 右瓣(额外血): 逆时针
        float rightBgStart = 30 - g / 2f;
        float rightBgSweep = -maxSweep;
        int abSteps = Math.round(mc.player.getAbsorptionAmount());
        float absorbRatio = MathHelper.clamp(abSteps / 16.0f, 0.0f, 1.0f);
        float rightSweep = -maxSweep * absorbRatio;

        // 下瓣(神龟): 顺时针
        float bottomBgStart = 30 + g / 2f;
        float bottomBgSweep = maxSweep;
        float turtleRatio = 0.0f;
        StatusEffectInstance resistance = mc.player.getStatusEffect(StatusEffects.RESISTANCE);
        if (resistance != null && resistance.getAmplifier() >= 2) {
            int ticks = resistance.getDuration();
            int turtleSteps = Math.round((ticks / 400.0f) * 100.0f);
            turtleRatio = MathHelper.clamp(turtleSteps / 100.0f, 0.0f, 1.0f);
        }
        float bottomSweep = maxSweep * turtleRatio;

        float currentGlobalAlpha = currentFade * globalAlpha.getValueFloat();

        if (bgColor.getValue().getAlpha() > 0) {
            drawSkijaArc(context, centerX, centerY, r, t, leftBgStart, leftBgSweep, bgColor.getValue(), gR, gInt, currentGlobalAlpha);
            drawSkijaArc(context, centerX, centerY, r, t, rightBgStart, rightBgSweep, bgColor.getValue(), gR, gInt, currentGlobalAlpha);
            drawSkijaArc(context, centerX, centerY, r, t, bottomBgStart, bottomBgSweep, bgColor.getValue(), gR, gInt, currentGlobalAlpha);
        }

        if (hpRatio > 0) {
            drawSkijaArc(context, centerX, centerY, r, t, leftBgStart, leftSweep, healthColor.getValue(), gR, gInt, currentGlobalAlpha);
        }
        if (absorbRatio > 0) {
            drawSkijaArc(context, centerX, centerY, r, t, rightBgStart, rightSweep, absorbColor.getValue(), gR, gInt, currentGlobalAlpha);
        }
        if (turtleRatio > 0) {
            drawSkijaArc(context, centerX, centerY, r, t, bottomBgStart, bottomSweep, turtleColor.getValue(), gR, gInt, currentGlobalAlpha);
        }
    }

    /**
     * 将指令下发给 Skija 绘制高清圆弧并映射到屏幕上
     */
    private void drawSkijaArc(DrawContext context, float centerX, float centerY, float radius, float thickness, float startAngle, float sweepAngle, Color color, float glowRad, int glowIntensity, float alphaMultiplier) {
        if (Math.abs(sweepAngle) < 0.1f) return;

        if (!SkijaGlowRenderer.isAvailable()) {
            drawFallbackArc(context, centerX, centerY, radius, thickness, startAngle, sweepAngle, color, alphaMultiplier);
            return;
        }

        Color pureColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 255);

        Identifier tex = SkijaGlowRenderer.createArcTexture(
                radius, thickness, startAngle, sweepAngle, pureColor, glowRad, glowIntensity
        );

        if (!SkijaGlowRenderer.isTextureReady(tex)) {
            drawFallbackArc(context, centerX, centerY, radius, thickness, startAngle, sweepAngle, color, alphaMultiplier);
            return;
        }

        int[] size = SkijaGlowRenderer.getTextureSize(tex);
        if (size == null) {
            drawFallbackArc(context, centerX, centerY, radius, thickness, startAngle, sweepAngle, color, alphaMultiplier);
            return;
        }

        int texWidth = size[0];
        int texHeight = size[1];

        double scale = mc.getWindow().getScaleFactor();
        float displayWidth = (float) (texWidth / scale);
        float displayHeight = (float) (texHeight / scale);

        float drawX = centerX - displayWidth / 2.0f;
        float drawY = centerY - displayHeight / 2.0f;

        float finalAlpha = (color.getAlpha() / 255.0f) * alphaMultiplier;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, finalAlpha);

        context.drawTexture(tex, (int)drawX, (int)drawY, 0, 0, (int)displayWidth, (int)displayHeight, (int)displayWidth, (int)displayHeight);

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private void drawFallbackArc(DrawContext context, float centerX, float centerY, float radius, float thickness,
                                 float startAngle, float sweepAngle, Color color, float alphaMultiplier) {
        float alpha = MathHelper.clamp((color.getAlpha() / 255.0f) * alphaMultiplier, 0.0f, 1.0f);
        if (alpha <= 0.01f) return;

        int argb = ((int) (alpha * 255.0f) << 24)
                | (color.getRed() << 16)
                | (color.getGreen() << 8)
                | color.getBlue();

        float outer = Math.max(0.5f, radius + thickness / 2.0f);
        float inner = Math.max(0.0f, radius - thickness / 2.0f);
        int segments = Math.max(8, (int) (Math.abs(sweepAngle) / 4.0f));
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            float t = i / (float) segments;
            double angle = Math.toRadians(startAngle + sweepAngle * t);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);

            buffer.vertex(matrix, centerX + cos * outer, centerY + sin * outer, 0.0f).color(argb);
            buffer.vertex(matrix, centerX + cos * inner, centerY + sin * inner, 0.0f).color(argb);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private boolean hasPlayerInRange() {
        if (mc.world == null || mc.player == null) return false;
        float range = fadeRange.getValueFloat();
        if (range <= 0.0f) return true;

        float rangeSq = range * range;
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !player.isAlive()) continue;
            if (player.squaredDistanceTo(mc.player) <= rangeSq) {
                return true;
            }
        }
        return false;
    }

    // --- HudElement 接口实现 ---

    @Override
    public String getElementName() {
        return getChinese() != null ? getChinese() : getName();
    }

    @Override
    public int getElementX() {
        if (mc.getWindow() == null) return 0;
        return mc.getWindow().getScaledWidth() / 2 - getElementWidth() / 2;
    }

    @Override
    public void setElementX(int x) { }

    @Override
    public int getElementY() {
        if (mc.getWindow() == null) return 0;
        return mc.getWindow().getScaledHeight() / 2 - getElementHeight() / 2;
    }

    @Override
    public void setElementY(int y) { }

    @Override
    public int getElementWidth() {
        return (int) (radius.getValueFloat() * 2);
    }

    @Override
    public int getElementHeight() {
        return (int) (radius.getValueFloat() * 2);
    }

    @Override
    public boolean isElementVisible() {
        return isOn();
    }
}
