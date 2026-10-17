package dev.kissoo.api.utils.skija;

import com.mojang.blaze3d.systems.RenderSystem;
import hoprc.api.core.impl.FontManager;
import dev.kissoo.mod.modules.impl.client.Fonts.FontMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.jetbrains.skija.*;
import org.jetbrains.skija.Canvas;
import org.jetbrains.skija.Font;
import org.jetbrains.skija.FontMetrics;
import org.jetbrains.skija.Image;
import org.jetbrains.skija.Paint;

import java.awt.Color;
import java.io.InputStream;
import java.util.*;

public class SkijaGlowRenderer {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Map<String, Typeface> typefaceCache = new HashMap<>();
    private static final Map<String, Identifier> textureCache = new HashMap<>();
    private static final Set<Identifier> readyTextures = new HashSet<>();
    private static final Map<Identifier, int[]> textureSizes = new HashMap<>();
    private static volatile boolean skijaAvailable = detectSkijaAvailable();

    public static boolean isAvailable() {
        return skijaAvailable;
    }

    private static boolean detectSkijaAvailable() {
        if (!hoprc.api.utils.win.CpuFeatures.isAvx2Supported()) {
            System.err.println("[Forever] Skija disabled: CPU does not support AVX2");
            return false;
        }
        try {
            Class.forName("org.jetbrains.skija.Path", true, SkijaGlowRenderer.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            System.err.println("[Forever] Skija disabled because native library loading failed: " + t);
            return false;
        }
    }

    public static void disable(Throwable t) {
        if (!skijaAvailable) return;
        skijaAvailable = false;
        System.err.println("[Forever] Skija disabled after runtime failure: " + t);
        try {
            clearAllTextures();
        } catch (Throwable ignored) {
        }
    }

    private static Typeface getTypeface(String fontName) {
        return typefaceCache.computeIfAbsent(fontName, SkijaGlowRenderer::loadTypeface);
    }

    private static Typeface loadTypeface(String fontName) {
        try (InputStream is = SkijaGlowRenderer.class.getClassLoader()
                .getResourceAsStream("assets/alienclient/font/" + fontName)) {
            if (is == null) return null;
            byte[] fontData = is.readAllBytes();
            return Typeface.makeFromData(Data.makeFromBytes(fontData));
        } catch (Exception e) {
            return null;
        }
    }

    private static String getFontFileForCurrentMode() {
        FontMode mode = FontManager.getMode();
        switch (mode) {
            case MISANS:  return "misans-regular.ttf";
            case BOREL:   return "borel-regular.ttf";
            case HOPRC:   return "hoprc.ttf";
            case HYW:     return "borel-regular.ttf";
            case CUSTOM_OTF: return "font.otf";
            case SYSTEM:  return "default.ttf";
            case MINECRAFT:
            case DEFAULT:
            default:      return "default.ttf";
        }
    }

    public static Identifier createGlowTexture(
            String text, float fontSize,
            Color textColor, Color glowColor,
            float glowRadius,
            boolean useGradient, Color gradientEndColor
    ) {
        return createGlowTexture(text, fontSize, textColor, glowColor,
                glowRadius, useGradient, gradientEndColor, null);
    }

    public static Identifier createGlowTexture(
            String text, float fontSize,
            Color textColor, Color glowColor,
            float glowRadius,
            boolean useGradient, Color gradientEndColor,
            String forceFontFile
    ) {
        if (!skijaAvailable) return null;

        double scale = mc.getWindow().getScaleFactor();
        float scaledFontSize = (float) (fontSize * scale);
        float scaledGlowRadius = (float) (glowRadius * scale);

        String fontFile = (forceFontFile != null) ? forceFontFile : getFontFileForCurrentMode();

        String cacheKey = String.format("%s_%s_%.1f_%08x_%08x_%.1f_%b_%s",
                text, fontFile, scaledFontSize,
                textColor.getRGB(), glowColor.getRGB(),
                scaledGlowRadius, useGradient,
                gradientEndColor != null ? Integer.toHexString(gradientEndColor.getRGB()) : "null");

        if (textureCache.containsKey(cacheKey)) return textureCache.get(cacheKey);

        try {
            Typeface typeface = getTypeface(fontFile);
            Font font = new Font(typeface != null ? typeface : Typeface.makeDefault(), scaledFontSize);
            font.setEdging(FontEdging.SUBPIXEL_ANTI_ALIAS);
            font.setSubpixel(true);

            FontMetrics metrics = font.getMetrics();
            float textWidth = font.measureTextWidth(text);
            float textHeight = metrics.getDescent() - metrics.getAscent();
            float padding = Math.max(scaledGlowRadius * 3.0f, 15.0f);
            int width = (int) Math.ceil(textWidth + padding * 2);
            int height = (int) Math.ceil(textHeight + padding * 2);

            try (Surface surface = Surface.makeRasterN32Premul(width, height);
                 Paint paint = new Paint()) {

                Canvas canvas = surface.getCanvas();
                canvas.clear(0x00000000);

                float x = padding;
                float y = padding - metrics.getAscent();

                paint.setAntiAlias(true);
                if (scaledGlowRadius > 0) {
                    try (ImageFilter blur = ImageFilter.makeBlur(
                            scaledGlowRadius, scaledGlowRadius, FilterTileMode.CLAMP)) {
                        paint.setImageFilter(blur);
                        paint.setColor(colorToSkija(glowColor));
                        canvas.drawString(text, x, y, font, paint);
                    }
                }
                paint.setImageFilter(null);
                if (useGradient && gradientEndColor != null) {
                    int[] colors = {colorToSkija(textColor), colorToSkija(gradientEndColor)};
                    paint.setShader(Shader.makeLinearGradient(x, y, x + textWidth, y, colors));
                } else {
                    paint.setColor(colorToSkija(textColor));
                }
                canvas.drawString(text, x, y, font, paint);

                NativeImage nativeImage;
                try (Image image = surface.makeImageSnapshot();
                     Data data = image.encodeToData(EncodedImageFormat.PNG)) {
                    nativeImage = NativeImage.read(data.getBytes());
                }

                if (nativeImage == null) return null;

                Identifier textureId = Identifier.of("alienclient",
                        "glow_" + UUID.randomUUID().toString().substring(0, 8));
                NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);
                textureSizes.put(textureId, new int[]{width, height});

                if (RenderSystem.isOnRenderThread()) {
                    mc.getTextureManager().registerTexture(textureId, texture);
                    RenderSystem.bindTexture(texture.getGlId());
                    RenderSystem.texParameter(3553, 10241, 9729); // GL_LINEAR
                    RenderSystem.texParameter(3553, 10240, 9729); // GL_LINEAR
                    readyTextures.add(textureId);
                } else {
                    RenderSystem.recordRenderCall(() -> {
                        mc.getTextureManager().registerTexture(textureId, texture);
                        RenderSystem.bindTexture(texture.getGlId());
                        RenderSystem.texParameter(3553, 10241, 9729);
                        RenderSystem.texParameter(3553, 10240, 9729);
                        readyTextures.add(textureId);
                    });
                }

                textureCache.put(cacheKey, textureId);
                font.close();
                return textureId;
            }
        } catch (Throwable e) {
            disable(e);
            e.printStackTrace();
            return null;
        }
    }

    // =========================================================================
    // 抗锯齿高质量圆弧生成 (附带叠加次数实现的光晕强度控制)
    // =========================================================================
    public static Identifier createArcTexture(
            float radius, float thickness,
            float startAngle, float sweepAngle,
            Color color, float glowRadius, int glowIntensity
    ) {
        if (!skijaAvailable) return null;

        double scale = mc.getWindow().getScaleFactor();
        float scaledRadius = (float) (radius * scale);
        float scaledThickness = (float) (thickness * scale);
        float scaledGlow = (float) (glowRadius * scale);

        // 高效缓存键，确保避免无限重复生成
        String cacheKey = String.format("arc_%.1f_%.1f_%.1f_%.1f_%08x_%.1f_%d",
                scaledRadius, scaledThickness, startAngle, sweepAngle, color.getRGB(), scaledGlow, glowIntensity);

        if (textureCache.containsKey(cacheKey)) return textureCache.get(cacheKey);

        float padding = Math.max(scaledGlow * 2.0f, scaledThickness * 2.0f);
        int texSize = (int) Math.ceil((scaledRadius + padding) * 2);

        try (Surface surface = Surface.makeRasterN32Premul(texSize, texSize);
             Paint paint = new Paint()) {

            Canvas canvas = surface.getCanvas();
            canvas.clear(0x00000000);

            paint.setAntiAlias(true);
            paint.setMode(PaintMode.STROKE);
            paint.setStrokeWidth(scaledThickness);
            // 设定为 ROUND 端点更加好看，保证弧线头尾都是圆润的
            paint.setStrokeCap(PaintStrokeCap.ROUND);

            float cx = texSize / 2f;
            float cy = texSize / 2f;
            Rect bounds = Rect.makeLTRB(cx - scaledRadius, cy - scaledRadius, cx + scaledRadius, cy + scaledRadius);

            if (Math.abs(sweepAngle) > 0.1f) {
                // 如果启用了发光并且设定了强度
                if (scaledGlow > 0 && glowIntensity > 0) {
                    try (ImageFilter blur = ImageFilter.makeBlur(scaledGlow, scaledGlow, FilterTileMode.CLAMP)) {
                        paint.setImageFilter(blur);
                        paint.setColor(colorToSkija(color));
                        // 通过强度设置多次叠加绘制，使光晕更亮、更饱满
                        for (int i = 0; i < glowIntensity; i++) {
                            canvas.drawArc(bounds.getLeft(), bounds.getTop(), bounds.getRight(), bounds.getBottom(), startAngle, sweepAngle, false, paint);
                        }
                    }
                }

                // 最后再绘制一次清晰的实线实体，保证进度条中心颜色不会模糊消散
                paint.setImageFilter(null);
                paint.setColor(colorToSkija(color));
                canvas.drawArc(bounds.getLeft(), bounds.getTop(), bounds.getRight(), bounds.getBottom(), startAngle, sweepAngle, false, paint);
            }

            NativeImage nativeImage;
            try (Image image = surface.makeImageSnapshot();
                 Data data = image.encodeToData(EncodedImageFormat.PNG)) {
                nativeImage = NativeImage.read(data.getBytes());
            }

            if (nativeImage == null) return null;

            Identifier textureId = Identifier.of("alienclient", "arc_" + UUID.randomUUID().toString().substring(0, 8));
            NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);
            textureSizes.put(textureId, new int[]{texSize, texSize});

            if (RenderSystem.isOnRenderThread()) {
                mc.getTextureManager().registerTexture(textureId, texture);
                RenderSystem.bindTexture(texture.getGlId());
                RenderSystem.texParameter(3553, 10241, 9729);
                RenderSystem.texParameter(3553, 10240, 9729);
                readyTextures.add(textureId);
            } else {
                RenderSystem.recordRenderCall(() -> {
                    mc.getTextureManager().registerTexture(textureId, texture);
                    RenderSystem.bindTexture(texture.getGlId());
                    RenderSystem.texParameter(3553, 10241, 9729);
                    RenderSystem.texParameter(3553, 10240, 9729);
                    readyTextures.add(textureId);
                });
            }

            textureCache.put(cacheKey, textureId);
            return textureId;
        } catch (Throwable e) {
            disable(e);
            e.printStackTrace();
            return null;
        }
    }

    public static Identifier createGlowDiamondTexture(
            float size,
            Color innerColor,
            Color glowColor,
            float glowRadius
    ) {
        if (!skijaAvailable) return null;

        double scale = mc.getWindow().getScaleFactor();
        float scaledSize = (float) (size * scale);
        float scaledGlowRadius = (float) (glowRadius * scale);

        scaledSize = Math.max(2.0f, Math.min(128.0f, scaledSize));
        scaledGlowRadius = Math.max(0.0f, Math.min(48.0f, scaledGlowRadius));

        String cacheKey = String.format("diamond_%.2f_%08x_%08x_%.2f",
                scaledSize, innerColor.getRGB(), glowColor.getRGB(), scaledGlowRadius);

        if (textureCache.containsKey(cacheKey)) return textureCache.get(cacheKey);

        try {
            float padding = Math.max(scaledGlowRadius * 1.6f, 6.0f);
            int texSize = (int) Math.ceil(scaledSize * 2.0f + padding * 2.0f);
            texSize = Math.max(8, Math.min(texSize, 1024));

            try (Surface surface = Surface.makeRasterN32Premul(texSize, texSize);
                 Paint paint = new Paint();
                 Path glowPath = new Path();
                 Path innerPath = new Path()) {

                Canvas canvas = surface.getCanvas();
                canvas.clear(0x00000000);

                float cx = texSize / 2.0f;
                float cy = texSize / 2.0f;
                float outer = Math.min(scaledSize, texSize / 2.0f - 2.0f);
                float inner = outer * 0.72f;

                glowPath.moveTo(cx, cy - outer);
                glowPath.lineTo(cx + outer, cy);
                glowPath.lineTo(cx, cy + outer);
                glowPath.lineTo(cx - outer, cy);
                glowPath.closePath();

                innerPath.moveTo(cx, cy - inner);
                innerPath.lineTo(cx + inner, cy);
                innerPath.lineTo(cx, cy + inner);
                innerPath.lineTo(cx - inner, cy);
                innerPath.closePath();

                paint.setAntiAlias(true);

                if (scaledGlowRadius > 0.0f) {
                    try (ImageFilter blur = ImageFilter.makeBlur(scaledGlowRadius, scaledGlowRadius, FilterTileMode.CLAMP)) {
                        paint.setImageFilter(blur);
                        paint.setShader(null);
                        paint.setColor(colorToSkija(glowColor));
                        canvas.drawPath(glowPath, paint);
                    }
                }

                paint.setImageFilter(null);
                paint.setShader(null);
                paint.setColor(colorToSkija(innerColor));
                canvas.drawPath(innerPath, paint);

                NativeImage nativeImage;
                try (Image image = surface.makeImageSnapshot();
                     Data data = image.encodeToData(EncodedImageFormat.PNG)) {
                    nativeImage = NativeImage.read(data.getBytes());
                }
                if (nativeImage == null) return null;

                Identifier textureId = Identifier.of("alienclient", "glow_diamond_" + UUID.randomUUID().toString().substring(0, 8));
                NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);
                textureSizes.put(textureId, new int[]{texSize, texSize});

                if (RenderSystem.isOnRenderThread()) {
                    mc.getTextureManager().registerTexture(textureId, texture);
                    RenderSystem.bindTexture(texture.getGlId());
                    RenderSystem.texParameter(3553, 10241, 9729);
                    RenderSystem.texParameter(3553, 10240, 9729);
                    readyTextures.add(textureId);
                } else {
                    RenderSystem.recordRenderCall(() -> {
                        mc.getTextureManager().registerTexture(textureId, texture);
                        RenderSystem.bindTexture(texture.getGlId());
                        RenderSystem.texParameter(3553, 10241, 9729);
                        RenderSystem.texParameter(3553, 10240, 9729);
                        readyTextures.add(textureId);
                    });
                }
                textureCache.put(cacheKey, textureId);
                return textureId;
            }
        } catch (Throwable t) {
            disable(t);
            t.printStackTrace();
            return null;
        }
    }

    public static boolean isTextureReady(Identifier id) {
        return id != null && readyTextures.contains(id);
    }

    public static int[] getTextureSize(Identifier id) {
        return textureSizes.get(id);
    }

    public static void renderSimpleGlow(net.minecraft.client.util.math.MatrixStack matrices, String text,
                                        float x, float y, Color color, float scale, float glowRadius,
                                        int layers, boolean shadow) {
        hoprc.api.gui.fonts.FontRenderer font = FontManager.ui;
        if (font == null) return;
        for (int i = layers; i > 0; i--) {
            float offset = i * 0.5f * scale;
            int alpha = color.getAlpha() / (i + 1);
            Color glowColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(255, alpha));
            font.drawString(matrices, text, x - offset, y - offset,
                    glowColor.getRed() / 255.0f, glowColor.getGreen() / 255.0f,
                    glowColor.getBlue() / 255.0f, glowColor.getAlpha() / 255.0f, shadow);
            font.drawString(matrices, text, x + offset, y - offset,
                    glowColor.getRed() / 255.0f, glowColor.getGreen() / 255.0f,
                    glowColor.getBlue() / 255.0f, glowColor.getAlpha() / 255.0f, shadow);
            font.drawString(matrices, text, x - offset, y + offset,
                    glowColor.getRed() / 255.0f, glowColor.getGreen() / 255.0f,
                    glowColor.getBlue() / 255.0f, glowColor.getAlpha() / 255.0f, shadow);
            font.drawString(matrices, text, x + offset, y + offset,
                    glowColor.getRed() / 255.0f, glowColor.getGreen() / 255.0f,
                    glowColor.getBlue() / 255.0f, glowColor.getAlpha() / 255.0f, shadow);
        }
        font.drawString(matrices, text, x, y,
                color.getRed() / 255.0f, color.getGreen() / 255.0f,
                color.getBlue() / 255.0f, color.getAlpha() / 255.0f, shadow);
    }

    private static int colorToSkija(Color c) {
        return (c.getAlpha() << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
    }

    public static void cleanupTexture(Identifier id) {
        if (id != null) {
            readyTextures.remove(id);
            textureSizes.remove(id);
            RenderSystem.recordRenderCall(() -> {
                if (mc.getTextureManager() != null) {
                    mc.getTextureManager().destroyTexture(id);
                }
            });
            textureCache.entrySet().removeIf(e -> e.getValue().equals(id));
        }
    }

    public static void clearAllTextures() {
        for (Identifier id : textureCache.values()) {
            readyTextures.remove(id);
            textureSizes.remove(id);
            RenderSystem.recordRenderCall(() -> {
                if (mc.getTextureManager() != null) {
                    mc.getTextureManager().destroyTexture(id);
                }
            });
        }
        textureCache.clear();
        readyTextures.clear();
        textureSizes.clear();
    }
}
