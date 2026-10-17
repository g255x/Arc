package dev.kissoo.mod.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.jetbrains.skija.*;
import org.jetbrains.skija.Canvas;
import org.jetbrains.skija.Paint;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;

public class HelloAnimation {
    private static final long PHASE_WAIT_END     = 1200L;
    private static final long PHASE_WRITE_END    = 3400L;
    private static final long PHASE_HOLD_END     = 5400L;
    private static final long PHASE_FADE_OUT_END = 6200L;
    public  static final long TOTAL_DURATION     = 6200L;
    private static final float HELLO_SCALE_RATIO = 0.45f;
    private static final float PADDING = 30f;

    private static final String SVG_HELLO_COMPLETE =
            "M208,338c38-16.67,73.74-45.72,97.33-66,21.33-18.33,32.67-35.67," +
                    "37.33-52.67C347.12,203.12,344,192,332,192c-11,0-21,10.33-24.94," +
                    "27.68-4.52,19.89-22.06,107.82-29.39,149,15.67-72.33,36.33-81.33," +
                    "53.67-81.33,22.33,0,24.67,18.67,19.42,39-5.43,21.07-7.42,44.32," +
                    "17.91,44.32,18,0,35.53-8.17,52.67-20,14-9.67,23-24,23-40,0-13.42," +
                    "-8-23.33-20.67-23.33s-24.33,12-24.33,33.33c0,27,16.33,48,44,48," +
                    "25.67,0,47.67-19.67,62-44.67,13.61-23.74,30.67-64.67,33.33-92.67s" +
                    "-5.33-36-18.67-36-24.67,17.33-28.67,43.33S486,302,491.33,330s28," +
                    "37.67,46,37.67,38.17-15.67,52-37c16.54-25.51,35.87-67.45,38.67-102," +
                    "2-24.67-8.67-33.33-20-33.33-14.67,0-23.33,13.33-28,38-4.5,23.81," +
                    "-8,64-2,94,4.64,23.21,25.33,40.33,44.67,40.33s32.67-19,36.67-42.33" +
                    " M697.33,287.33C672,287.33,661.33,305,659,327c-2.81,26.54,10.33," +
                    "41.67,29.67,41.67,22,0,34.54-20.78,36.67-40.67,2-18.67-7.39-39.13," +
                    "-28-40.67" +
                    " M714.8,295.12c12.11,12.26,43.53,9.55,56.53-5.79";

    private static final float SVG_MIN_X  = 199f;
    private static final float SVG_MIN_Y  = 183f;
    private static final float SVG_WIDTH  = 582f;
    private static final float SVG_HEIGHT = 197f;
    private static final float CANVAS_W = SVG_WIDTH + PADDING * 2;
    private static final float CANVAS_H = SVG_HEIGHT + PADDING * 2;

    private final Path    helloPath;
    private final float   totalLength;
    private final long    startTime;
    private       boolean finished = false;

    private Surface surface = null;
    private int surfaceW = 0;
    private int surfaceH = 0;

    private Identifier cachedTexId = null;
    private NativeImage cachedNativeImage = null;
    private NativeImageBackedTexture cachedTexture = null;

    public HelloAnimation() {
        this.startTime   = System.currentTimeMillis();
        this.helloPath   = buildHelloPath();
        this.totalLength = measureTotalLength(helloPath);
    }

    public boolean isFinished() {
        return finished;
    }

    private Path buildHelloPath() {
        try {
            Path path = Path.makeFromSVGString(SVG_HELLO_COMPLETE);
            path.transform(Matrix33.makeTranslate(-SVG_MIN_X, -SVG_MIN_Y));
            return path;
        } catch (Exception e) {
            e.printStackTrace();
            return new Path();
        }
    }

    public void render(MatrixStack matrices, int screenW, int screenH) {
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed >= TOTAL_DURATION) {
            finished = true;
            cleanup();
            return;
        }

        float overlayAlpha, writeProgress, globalAlpha;

        if (elapsed < PHASE_WAIT_END) {
            overlayAlpha  = (float) elapsed / PHASE_WAIT_END;
            writeProgress = 0f;
            globalAlpha   = 1f;
        } else if (elapsed < PHASE_WRITE_END) {
            float t = (float)(elapsed - PHASE_WAIT_END) / (PHASE_WRITE_END - PHASE_WAIT_END);
            overlayAlpha  = 1f;
            writeProgress = easeInOutSine(t);
            globalAlpha   = 1f;
        } else if (elapsed < PHASE_HOLD_END) {
            overlayAlpha  = 1f;
            writeProgress = 1f;
            globalAlpha   = 1f;
        } else {
            float t = (float)(elapsed - PHASE_HOLD_END) / (PHASE_FADE_OUT_END - PHASE_HOLD_END);
            float fade    = easeInOutSine(t);
            overlayAlpha  = 1f - fade;
            writeProgress = 1f;
            globalAlpha   = 1f - fade;
        }

        int blackAlpha = (int)(overlayAlpha * globalAlpha * 160);
        blackAlpha = Math.max(0, Math.min(255, blackAlpha));
        if (blackAlpha > 0) {
            fillScreen(matrices, screenW, screenH, blackAlpha);
        }

        if (writeProgress > 0f) {
            renderFrame(matrices, screenW, screenH, writeProgress, globalAlpha);
        }
    }

    private void fillScreen(MatrixStack matrices, int screenW, int screenH, int alpha) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        buffer.vertex(matrix, 0, 0, 0).color(0, 0, 0, alpha);
        buffer.vertex(matrix, 0, screenH, 0).color(0, 0, 0, alpha);
        buffer.vertex(matrix, screenW, screenH, 0).color(0, 0, 0, alpha);
        buffer.vertex(matrix, screenW, 0, 0).color(0, 0, 0, alpha);

        BuiltBuffer built = buffer.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);
        RenderSystem.disableBlend();
    }

    private void renderFrame(MatrixStack matrices, int screenW, int screenH, float writeProgress, float globalAlpha) {
        double scale = MinecraftClient.getInstance().getWindow().getScaleFactor();

        int targetTexW = (int) (screenW * scale * HELLO_SCALE_RATIO);
        int targetTexH = (int) (targetTexW * (CANVAS_H / CANVAS_W));

        try {
            if (surface == null || surfaceW != targetTexW || surfaceH != targetTexH) {
                cleanup();
                surfaceW = targetTexW;
                surfaceH = targetTexH;

                surface = Surface.makeRasterN32Premul(surfaceW, surfaceH);
                cachedNativeImage = new NativeImage(surfaceW, surfaceH, false);
                cachedTexId = Identifier.of("lyrae", "hello_anim_frame");
                cachedTexture = new NativeImageBackedTexture(cachedNativeImage);
                if (RenderSystem.isOnRenderThread()) {
                    MinecraftClient.getInstance().getTextureManager().registerTexture(cachedTexId, cachedTexture);
                } else {
                    final NativeImageBackedTexture tex = cachedTexture;
                    RenderSystem.recordRenderCall(() ->
                            MinecraftClient.getInstance().getTextureManager().registerTexture(cachedTexId, tex)
                    );
                }
            }

            Canvas canvas = surface.getCanvas();
            canvas.clear(0x00000000);

            int masterAlpha = (int)(globalAlpha * 255);
            drawHello(canvas, targetTexW, targetTexH, writeProgress, masterAlpha);

            try (Image image = surface.makeImageSnapshot()) {
                ByteBuffer skijaBuffer = image.peekPixels();
                if (skijaBuffer != null) {
                    for (int y = 0; y < surfaceH; y++) {
                        for (int x = 0; x < surfaceW; x++) {
                            int offset = (y * surfaceW + x) * 4;
                            int b = skijaBuffer.get(offset)     & 0xFF;
                            int g = skijaBuffer.get(offset + 1) & 0xFF;
                            int r = skijaBuffer.get(offset + 2) & 0xFF;
                            int a = skijaBuffer.get(offset + 3) & 0xFF;
                            if (a > 0 && a < 255) {
                                r = Math.min(255, (r * 255) / a);
                                g = Math.min(255, (g * 255) / a);
                                b = Math.min(255, (b * 255) / a);
                            }

                            cachedNativeImage.setColor(x, y, (a << 24) | (b << 16) | (g << 8) | r);
                        }
                    }
                }
            }

            if (RenderSystem.isOnRenderThread()) {
                cachedTexture.upload();
            } else {
                RenderSystem.recordRenderCall(cachedTexture::upload);
            }

        } catch (Throwable e) {
            e.printStackTrace();
            return;
        }

        blitTextureCenter(matrices, screenW, screenH);
    }

    private void drawHello(Canvas canvas, int texW, int texH, float progress, int masterAlpha) {
        float scaleF = texW / CANVAS_W;

        canvas.save();
        canvas.scale(scaleF, scaleF);
        canvas.translate(PADDING + 0.5f, PADDING + 0.5f);

        float drawLen = totalLength * progress;
        int[] colors = new int[] {
                0xFFFF5555,
                0xFFFFFFAA,
                0xFF55FF55,
                0xFF55FFFF,
                0xFF8888FF,
                0xFFFF77FF
        };

        float[] pos = new float[] { 0f, 0.2f, 0.4f, 0.6f, 0.8f, 1.0f };

        try (Shader rainbowShader = Shader.makeLinearGradient(0, 0, SVG_WIDTH, 0, colors, pos);
             Path segmentPath = new Path();
             PathMeasure pm = new PathMeasure(helloPath, false)) {

            float currentLength = 0;
            do {
                float contourLength = pm.getLength();
                if (drawLen > currentLength + contourLength) {
                    pm.getSegment(0, contourLength, segmentPath, true);
                    currentLength += contourLength;
                } else {
                    float lengthToDraw = drawLen - currentLength;
                    pm.getSegment(0, lengthToDraw, segmentPath, true);
                    break;
                }
            } while (pm.nextContour());

            try (Paint glowPaint = new Paint()) {
                glowPaint.setMode(PaintMode.STROKE);
                glowPaint.setStrokeCap(PaintStrokeCap.ROUND);
                glowPaint.setStrokeJoin(PaintStrokeJoin.ROUND);
                glowPaint.setAntiAlias(true);
                glowPaint.setStrokeWidth(15.0f);

                glowPaint.setShader(rainbowShader);
                glowPaint.setAlpha((int)(masterAlpha * 0.70f));
                try (ImageFilter blur = ImageFilter.makeBlur(12.0f, 12.0f, FilterTileMode.CLAMP)) {
                    glowPaint.setImageFilter(blur);
                    canvas.drawPath(segmentPath, glowPaint);
                }
            }

            try (Paint p = new Paint()) {
                p.setMode(PaintMode.STROKE);
                p.setStrokeCap(PaintStrokeCap.ROUND);
                p.setStrokeJoin(PaintStrokeJoin.ROUND);
                p.setAntiAlias(true);
                p.setStrokeWidth(10.0f);

                p.setShader(rainbowShader);
                p.setAlpha(masterAlpha);

                canvas.drawPath(segmentPath, p);
            }
        }

        canvas.restore();
    }
    private void blitTextureCenter(MatrixStack matrices, int screenW, int screenH) {
        if (cachedTexId == null) return;

        float drawWidth = screenW * HELLO_SCALE_RATIO;
        float drawHeight = drawWidth * (CANVAS_H / CANVAS_W);
        float startX = (screenW - drawWidth) / 2f;
        float startY = (screenH - drawHeight) / 2f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderTexture(0, cachedTexId);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

        buffer.vertex(matrix, startX, startY, 0).texture(0f, 0f);
        buffer.vertex(matrix, startX, startY + drawHeight, 0).texture(0f, 1f);
        buffer.vertex(matrix, startX + drawWidth, startY + drawHeight, 0).texture(1f, 1f);
        buffer.vertex(matrix, startX + drawWidth, startY, 0).texture(1f, 0f);

        BuiltBuffer built = buffer.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);

        RenderSystem.disableBlend();
    }

    private float measureTotalLength(Path path) {
        float total = 0f;
        try (PathMeasure pm = new PathMeasure(path, false)) {
            do {
                total += pm.getLength();
            } while (pm.nextContour());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return total == 0f ? 1f : total;
    }

    private float easeInOutSine(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return -(float)(Math.cos(Math.PI * t) - 1.0) / 2f;
    }

    public void cleanup() {
        if (surface != null) {
            surface.close();
            surface = null;
        }

        if (cachedNativeImage != null) {
            cachedNativeImage.close();
            cachedNativeImage = null;
        }

        if (cachedTexture != null) {
            cachedTexture.close();
            cachedTexture = null;
        }

        if (cachedTexId != null) {
            final Identifier id = cachedTexId;
            cachedTexId = null;
            RenderSystem.recordRenderCall(() -> {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc != null && mc.getTextureManager() != null) {
                    mc.getTextureManager().destroyTexture(id);
                }
            });
        }
    }
}