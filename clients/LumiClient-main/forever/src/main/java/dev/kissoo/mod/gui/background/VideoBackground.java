package dev.kissoo.mod.gui.background;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;

public class VideoBackground {

    private static VideoBackground INSTANCE;

    public static VideoBackground getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new VideoBackground();
        }
        return INSTANCE;
    }

    private FFmpegFrameGrabber grabber;
    private NativeImageBackedTexture texture;
    private Identifier textureLocation;
    private NativeImage nativeImage;
    private long lastFrameTime;
    private int frameDelay;
    private int width, height;
    private volatile boolean asyncLoading = false;
    private volatile boolean readyToRegisterTexture = false;
    private volatile boolean initialized = false;
    private volatile boolean failed = false;
    private volatile Frame preloadedFirstFrame = null;

    private Java2DFrameConverter converter;

    private VideoBackground() {}

    public void initAsync(String resourcePath, String fallbackResourcePath) {
        if (initialized || asyncLoading) return;
        if (!hoprc.api.utils.win.CpuFeatures.isAvx2Supported()) {
            failed = true;
            return;
        }
        asyncLoading = true;

        CompletableFuture.runAsync(() -> {
            try {
                Path gameDir = MinecraftClient.getInstance().runDirectory.toPath();
                Path videoDir = gameDir.resolve("Lyrae").resolve("videos");
                Files.createDirectories(videoDir);
                Path videoFile = videoDir.resolve("mainmenu.mp4");

                // 1. 拷贝视频文件 (如果本地没有)
                if (!Files.exists(videoFile)) {
                    try (InputStream is = openBundledVideo(resourcePath, fallbackResourcePath)) {
                        if (is == null) {
                            asyncLoading = false;
                            failed = true;
                            return;
                        }
                        Files.copy(is, videoFile, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                grabber = new FFmpegFrameGrabber(videoFile.toString());
                grabber.start();

                width = grabber.getImageWidth();
                height = grabber.getImageHeight();
                int videoFps = (int) grabber.getFrameRate();
                frameDelay = 1000 / Math.max(24, videoFps);
                converter = new Java2DFrameConverter();
                preloadedFirstFrame = grabber.grabImage();
                readyToRegisterTexture = true;

            } catch (Exception e) {
                e.printStackTrace();
                asyncLoading = false;
                failed = true;
            }
        });
    }

    private InputStream openBundledVideo(String resourcePath, String fallbackResourcePath) {
        InputStream is = VideoBackground.class.getResourceAsStream(resourcePath);
        return is != null ? is : VideoBackground.class.getResourceAsStream(fallbackResourcePath);
    }

    public void render(DrawContext context, int screenWidth, int screenHeight) {
        if (failed) return;
        if (readyToRegisterTexture) {
            try {
                nativeImage = new NativeImage(NativeImage.Format.RGBA, width, height, true);
                texture = new NativeImageBackedTexture(nativeImage);
                textureLocation = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture("video_bg", texture);
                if (preloadedFirstFrame != null) {
                    updateTexture(preloadedFirstFrame);
                    preloadedFirstFrame = null;
                }

                lastFrameTime = System.currentTimeMillis();
                initialized = true;
            } catch (Exception e) {
                e.printStackTrace();
                failed = true;
            } finally {
                readyToRegisterTexture = false;
                asyncLoading = false;
            }
        }
        if (failed) return;
        if (!initialized || grabber == null || textureLocation == null) {
            context.fill(0, 0, screenWidth, screenHeight, 0xFF000000);
            return;
        }
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameTime >= frameDelay) {
            try {
                Frame frame = grabber.grabImage();
                if (frame == null) {
                    grabber.setFrameNumber(0);
                    frame = grabber.grabImage();
                }
                if (frame != null) {
                    updateTexture(frame);
                }
            } catch (Exception e) {
                e.printStackTrace();
                failed = true;
            }
            lastFrameTime = currentTime;
        }
        context.drawTexture(textureLocation,
                0, 0, screenWidth, screenHeight,
                0, 0, width, height, width, height);
    }

    private void updateTexture(Frame frame) throws Exception {
        if (frame.image == null || frame.image.length == 0 || frame.image[0] == null) return;
        if (frame.image[0] instanceof java.nio.ByteBuffer) {
            java.nio.ByteBuffer buffer = (java.nio.ByteBuffer) frame.image[0];
            buffer.rewind();
            int pixelCount = width * height;
            int remaining = buffer.remaining();
            if (remaining < pixelCount * 3) return;
            boolean isRGBA = remaining >= pixelCount * 4;

            if (isRGBA) {
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int idx = (y * width + x) * 4;
                        int r = buffer.get(idx) & 0xFF;
                        int g = buffer.get(idx + 1) & 0xFF;
                        int b = buffer.get(idx + 2) & 0xFF;
                        int a = buffer.get(idx + 3) & 0xFF;
                        nativeImage.setColor(x, y, (a << 24) | (r << 16) | (g << 8) | b);
                    }
                }
            } else {
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int idx = (y * width + x) * 3;
                        int r = buffer.get(idx) & 0xFF;
                        int g = buffer.get(idx + 1) & 0xFF;
                        int b = buffer.get(idx + 2) & 0xFF;
                        nativeImage.setColor(x, y, (255 << 24) | (r << 16) | (g << 8) | b);
                    }
                }
            }
            texture.upload();
        } else {
            BufferedImage image = converter.convert(frame);
            if (image != null) {
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int rgb = image.getRGB(x, y);
                        int a = (rgb >> 24) & 0xFF;
                        int r = (rgb >> 16) & 0xFF;
                        int g = (rgb >> 8) & 0xFF;
                        int b = rgb & 0xFF;
                        nativeImage.setColor(x, y, (a << 24) | (r << 16) | (g << 8) | b);
                    }
                }
                texture.upload();
            }
        }
    }

    public void close() {
        try {
            if (grabber != null) {
                grabber.stop();
                grabber.close();
            }
            if (converter != null) converter.close();
            if (textureLocation != null) MinecraftClient.getInstance().getTextureManager().destroyTexture(textureLocation);
            if (nativeImage != null) nativeImage.close();
            initialized = false;
            asyncLoading = false;
            readyToRegisterTexture = false;
            failed = false;
            preloadedFirstFrame = null;
            INSTANCE = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isInitialized() { return initialized; }
    public boolean isLoading() { return asyncLoading; }
    public boolean isFailed() { return failed; }
}
