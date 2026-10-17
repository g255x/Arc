package dev.kissoo.mod.modules.impl.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.kissoo.mod.modules.HudElement;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GifHud extends Module implements HudElement {
    public static GifHud INSTANCE;

    private int hudX = 20;
    private int hudY = 20;
    private AlignMode alignMode = AlignMode.LEFT;

    public enum GifMode {
        cat("cat.gif"),
        kou("kou.gif"),
        kiss_cum("kiss_cum.gif"),
        shazi("shazi.gif");

        private final String fileName;
        GifMode(String fileName) {
            this.fileName = fileName;
        }
        public String getFileName() {
            return fileName;
        }
    }

    public final EnumSetting<GifMode> mode = this.add(new EnumSetting<>("Mode", GifMode.kiss_cum).injectTask(this::loadGifFromResources));
    public final SliderSetting size = this.add(new SliderSetting("WidthSize", 100, 50, 500, 1));

    private static class FrameData {
        Identifier textureId;
        int delayMs;
        FrameData(Identifier textureId, int delayMs) {
            this.textureId = textureId;
            this.delayMs = delayMs;
        }
    }

    private final List<FrameData> frames = new ArrayList<>();
    private boolean loaded = false;
    private int originalWidth = 100;
    private int originalHeight = 100;
    private int totalDurationMs = 0;

    public GifHud() {
        super("AnimatedGif", Category.HUD);
        this.setChinese("屏幕动图");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        if (!loaded) {
            loadGifFromResources();
        }
    }

    private void clearOldFrames() {
        if (mc != null && mc.getTextureManager() != null) {
            for (FrameData frame : frames) {
                mc.getTextureManager().destroyTexture(frame.textureId);
            }
        }
        frames.clear();
        totalDurationMs = 0;
    }

    private void loadGifFromResources() {
        if (mc == null || mc.getResourceManager() == null || mc.getTextureManager() == null) {
            return;
        }
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(this::loadGifFromResources);
            return;
        }
        loaded = true;
        clearOldFrames();

        try {
            String currentFileName = mode.getValue().getFileName();
            Identifier gifId = Identifier.of("alienclient", "gif/" + currentFileName);

            var resourceOpt = mc.getResourceManager().getResource(gifId);
            if (resourceOpt.isEmpty()) {
                System.out.println("[GifHud] 未找到 GIF: " + currentFileName);
                return;
            }

            try (InputStream is = resourceOpt.get().getInputStream();
                 ImageInputStream iis = ImageIO.createImageInputStream(is)) {

                Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
                if (!readers.hasNext()) return;

                ImageReader reader = readers.next();
                reader.setInput(iis, false);
                int numFrames = reader.getNumImages(true);

                BufferedImage masterCanvas = null;
                Graphics2D g2d = null;
                BufferedImage backupCanvas = null;

                for (int i = 0; i < numFrames; i++) {
                    BufferedImage rawFrame = reader.read(i);
                    IIOMetadata metadata = reader.getImageMetadata(i);
                    Node root = metadata.getAsTree(metadata.getNativeMetadataFormatName());
                    int offsetX = 0, offsetY = 0;
                    int delayTime = 10; // 默认 100ms
                    String disposalMethod = "none";
                    NodeList children = root.getChildNodes();
                    for (int j = 0; j < children.getLength(); j++) {
                        Node node = children.item(j);
                        if (node.getNodeName().equalsIgnoreCase("ImageDescriptor")) {
                            NamedNodeMap attrs = node.getAttributes();
                            offsetX = Integer.parseInt(attrs.getNamedItem("imageLeftPosition").getNodeValue());
                            offsetY = Integer.parseInt(attrs.getNamedItem("imageTopPosition").getNodeValue());
                            if (masterCanvas == null) {
                                originalWidth = Integer.parseInt(attrs.getNamedItem("imageWidth").getNodeValue()) + offsetX;
                                originalHeight = Integer.parseInt(attrs.getNamedItem("imageHeight").getNodeValue()) + offsetY;
                            }
                        } else if (node.getNodeName().equalsIgnoreCase("GraphicControlExtension")) {
                            NamedNodeMap attrs = node.getAttributes();
                            disposalMethod = attrs.getNamedItem("disposalMethod").getNodeValue();
                            delayTime = Integer.parseInt(attrs.getNamedItem("delayTime").getNodeValue());
                        }
                    }
                    int frameDelayMs = (delayTime == 0) ? 100 : (delayTime * 10);
                    if (masterCanvas == null) {
                        originalWidth = Math.max(originalWidth, rawFrame.getWidth());
                        originalHeight = Math.max(originalHeight, rawFrame.getHeight());
                        masterCanvas = new BufferedImage(originalWidth, originalHeight, BufferedImage.TYPE_INT_ARGB);
                        g2d = masterCanvas.createGraphics();
                        g2d.setBackground(new Color(0, 0, 0, 0));
                    }
                    if (disposalMethod.equals("restoreToPrevious")) {
                        backupCanvas = new BufferedImage(originalWidth, originalHeight, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D bg = backupCanvas.createGraphics();
                        bg.drawImage(masterCanvas, 0, 0, null);
                        bg.dispose();
                    }
                    g2d.drawImage(rawFrame, offsetX, offsetY, null);
                    NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, originalWidth, originalHeight, false);
                    for (int x = 0; x < originalWidth; x++) {
                        for (int y = 0; y < originalHeight; y++) {
                            int argb = masterCanvas.getRGB(x, y);
                            int a = (argb >> 24) & 0xFF;
                            int r = (argb >> 16) & 0xFF;
                            int g = (argb >> 8) & 0xFF;
                            int b = argb & 0xFF;
                            int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                            nativeImage.setColor(x, y, abgr);
                        }
                    }

                    Identifier frameId = Identifier.of("alienclient", "dyn_gif_" + currentFileName.replace(".gif", "") + "_" + i + "_" + System.currentTimeMillis());
                    mc.getTextureManager().registerTexture(frameId, new NativeImageBackedTexture(nativeImage));
                    frames.add(new FrameData(frameId, frameDelayMs));
                    totalDurationMs += frameDelayMs;
                    if (disposalMethod.equals("restoreToBackgroundColor")) {
                        g2d.setComposite(AlphaComposite.Clear);
                        g2d.fillRect(offsetX, offsetY, rawFrame.getWidth(), rawFrame.getHeight());
                        g2d.setComposite(AlphaComposite.SrcOver);
                    } else if (disposalMethod.equals("restoreToPrevious") && backupCanvas != null) {
                        g2d.setComposite(AlphaComposite.Clear);
                        g2d.fillRect(0, 0, originalWidth, originalHeight);
                        g2d.setComposite(AlphaComposite.SrcOver);
                        g2d.drawImage(backupCanvas, 0, 0, null);
                    }
                }

                if (g2d != null) g2d.dispose();
                System.out.println("[GifHud] 解析完毕！共 " + numFrames + " 帧。");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[GifHud] GIF解析严重失败！");
            loaded = false;
        }
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        renderElement(drawContext, tickDelta);
    }

    @Override
    public void renderElement(DrawContext context, float tickDelta) {
        if (!loaded) {
            loadGifFromResources();
        }

        if (mc.player == null || mc.world == null || frames.isEmpty() || totalDurationMs == 0) return;

        int drawW = size.getValueInt();
        int drawH = (int) (drawW * ((float) originalHeight / originalWidth));

        int x = getElementX();
        int y = getElementY();
        long currentTimeInLoop = System.currentTimeMillis() % totalDurationMs;
        long timeAccumulator = 0;
        Identifier currentTexture = frames.get(0).textureId;

        for (FrameData frame : frames) {
            timeAccumulator += frame.delayMs;
            if (currentTimeInLoop < timeAccumulator) {
                currentTexture = frame.textureId;
                break;
            }
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        context.drawTexture(currentTexture, x, y, 0, 0, drawW, drawH, drawW, drawH);
        RenderSystem.disableBlend();
    }

    @Override public String getElementName() { return getChinese() != null ? getChinese() : getName(); }
    @Override public int getElementX() { return alignMode == AlignMode.RIGHT ? hudX - getElementWidth() : hudX; }
    @Override public void setElementX(int x) { hudX = alignMode == AlignMode.RIGHT ? x + getElementWidth() : x; }
    @Override public int getElementY() { return hudY; }
    @Override public void setElementY(int y) { hudY = y; }
    @Override public int getElementWidth() { return size.getValueInt(); }
    @Override public int getElementHeight() { return (int) (size.getValueInt() * ((float) originalHeight / originalWidth)); }
    @Override public boolean isElementVisible() { return isOn(); }
    @Override public AlignMode getAlignMode() { return alignMode; }
    @Override public void setAlignMode(AlignMode mode) {
        if (mode == alignMode) return;
        if (mode == AlignMode.RIGHT && alignMode == AlignMode.LEFT) hudX = hudX + getElementWidth();
        else if (mode == AlignMode.LEFT && alignMode == AlignMode.RIGHT) hudX = hudX - getElementWidth();
        alignMode = mode;
    }
    @Override public int getRightAnchorX() { return alignMode == AlignMode.RIGHT ? hudX : hudX + getElementWidth(); }
    @Override public void setRightAnchorX(int rightX) { hudX = alignMode == AlignMode.RIGHT ? rightX : rightX - getElementWidth(); }
}