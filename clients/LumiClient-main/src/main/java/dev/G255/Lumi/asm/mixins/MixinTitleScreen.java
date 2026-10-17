package dev.G255.Lumi.asm.mixins;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.G255.Lumi.MenuStyleScreen;
import dev.G255.Lumi.api.utils.render.Render2DUtil;
import dev.G255.Lumi.core.impl.ConfigManager;
import dev.G255.Lumi.core.impl.FontManager;
import dev.G255.Lumi.core.impl.ParallaxState;
import dev.G255.Lumi.core.impl.ShaderManager;
import dev.G255.Lumi.mod.gui.fonts.FontRenderer;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import javax.imageio.ImageIO;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.ladysnake.satin.api.managed.ManagedShaderEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen extends Screen {
    @Unique
    private static final int ALIEN_BUTTON_W = 120;
    @Unique
    private static final int ALIEN_BUTTON_H = 35;
    @Unique
    private static final int ALIEN_BUTTON_GAP = 20;
    @Unique
    private static final int AURORA_BUTTON_W = 200;
    @Unique
    private static final int AURORA_BUTTON_H = 20;
    @Unique
    private static final int AURORA_BUTTON_GAP = 4;
    @Unique
    private static Identifier alienBgTexture = null;
    @Unique
    private static int alienImageWidth = 0;
    @Unique
    private static int alienImageHeight = 0;
    @Unique
    private static boolean alienTextureLoaded = false;
    @Unique
    private static String loadedBgPath = null;
    @Unique
    private int auroraFrameCount = 0;
    @Unique
    private long lastShaderTime = 0L;
    @Unique
    private static final long SHADER_INTERVAL = 41L;
    @Unique
    private final List<ClickableWidget> savedModMenuWidgets = new ArrayList<>();

    protected MixinTitleScreen(Text title) {
        super(title);
    }

    @Inject(method = "initWidgetsNormal", at = @At("RETURN"))
    private void onInitWidgets(int y, int spacingY, CallbackInfo ci) {
        this.savedModMenuWidgets.clear();
        for (Element element : this.children()) {
            if (element instanceof ClickableWidget widget) {
                String text = widget.getMessage().getString();
                if (text.contains("Mod") || text.contains("mod") || text.contains("模组") || text.contains("mods")) {
                    this.savedModMenuWidgets.add(widget);
                }
            }
        }
        this.clearChildren();
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
    }

    @Unique
    private void loadAlienTexture() {
        if (this.client != null) {
            String path = ConfigManager.menuBackgroundPath;
            if (alienBgTexture == null || !path.equals(loadedBgPath)) {
                if (alienBgTexture != null) {
                    this.client.getTextureManager().destroyTexture(alienBgTexture);
                    alienBgTexture = null;
                }
                alienTextureLoaded = false;
                loadedBgPath = null;

                try {
                    InputStream stream = openBgStream(path);
                    if (stream == null) {
                        stream = MixinTitleScreen.class.getResourceAsStream("/assets/lumi/textures/gui/ba.png");
                        if (stream == null) {
                            return;
                        }
                    }

                    NativeImage nativeImage = readNativeImage(stream, path);
                    stream.close();
                    if (nativeImage == null) {
                        return;
                    }

                    alienImageWidth = nativeImage.getWidth();
                    alienImageHeight = nativeImage.getHeight();
                    NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);
                    alienBgTexture = Identifier.of("lumi", "title_background");
                    this.client.getTextureManager().registerTexture(alienBgTexture, texture);
                    alienTextureLoaded = true;
                    loadedBgPath = path;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Unique
    private InputStream openBgStream(String path) {
        try {
            if ((path.length() < 2 || path.charAt(1) != ':') && !path.startsWith("/") && !path.startsWith("\\")) {
                return MixinTitleScreen.class.getResourceAsStream("/assets/lumi/textures/gui/" + path);
            } else {
                File file = new File(path);
                return !file.exists() ? null : new BufferedInputStream(new FileInputStream(file));
            }
        } catch (Exception var3) {
            return null;
        }
    }

    @Unique
    private NativeImage readNativeImage(InputStream stream, String path) {
        try {
            return NativeImage.read(stream);
        } catch (Exception var16) {
            try {
                stream.close();
            } catch (Exception var14) {
            }

            try {
                InputStream is = openBgStream(path);
                if (is == null) {
                    return null;
                } else {
                    BufferedImage bi = ImageIO.read(is);
                    is.close();
                    if (bi == null) {
                        return null;
                    } else {
                        NativeImage ni = new NativeImage(bi.getWidth(), bi.getHeight(), false);

                        for (int py = 0; py < bi.getHeight(); ++py) {
                            for (int px = 0; px < bi.getWidth(); ++px) {
                                int argb = bi.getRGB(px, py);
                                int a = argb >> 24 & 255;
                                int r = argb >> 16 & 255;
                                int g = argb >> 8 & 255;
                                int b = argb & 255;
                                ni.setColor(px, py, a << 24 | b << 16 | g << 8 | r);
                            }
                        }

                        return ni;
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ci.cancel();
        String style = ConfigManager.menuStyle;
        if ("alien".equals(style)) {
            renderAlien(context, mouseX, mouseY, delta);
        } else {
            renderAurora(context, mouseX, mouseY, delta);
        }
    }

    @Unique
    private void renderAlien(DrawContext context, int mouseX, int mouseY, float delta) {
        loadAlienTexture();
        int w = this.width;
        int h = this.height;
        context.fill(0, 0, w, h, -16777216);
        if (ConfigManager.showMenuBackground && alienTextureLoaded && alienBgTexture != null) {
            float scale = 0.55F;
            int scaledWidth = (int) ((float) alienImageWidth * scale);
            int scaledHeight = (int) ((float) alienImageHeight * scale);
            ParallaxState.updateMouseShake(mouseX, mouseY, w, h);

            int drawX = (w - scaledWidth) / 2 + (int) ParallaxState.parallaxOffsetX;
            int drawY = (h - scaledHeight) / 2 + (int) ParallaxState.parallaxOffsetY;
            context.drawTexture(alienBgTexture, drawX, drawY, 0.0F, 0.0F, scaledWidth, scaledHeight, scaledWidth, scaledHeight);
            context.fill(0, 0, w, h, 0x11000000);
        }

        int startX = this.width - 120 - 30;
        int startY = 80;
        MatrixStack matrices = context.getMatrices();
        String singleplayer = Text.translatable("menu.singleplayer").getString();
        String multiplayer = Text.translatable("menu.multiplayer").getString();
        String options = Text.translatable("menu.options").getString();
        String quit = Text.translatable("menu.quit").getString();
        drawAlienButton(context, matrices, singleplayer, startX, startY, mouseX, mouseY, false);
        drawAlienButton(context, matrices, multiplayer, startX, startY + 35 + 20, mouseX, mouseY, false);
        drawAlienButton(context, matrices, options, startX, startY + 110, mouseX, mouseY, false);
        drawAlienButton(context, matrices, "LumiClient", startX, startY + 165, mouseX, mouseY, false);
        drawAlienButton(context, matrices, quit, startX, startY + 220, mouseX, mouseY, true);
        renderModMenuWidgets(context, mouseX, mouseY);
    }

    @Unique
    private void renderAurora(DrawContext context, int mouseX, int mouseY, float delta) {
        int w = this.width;
        int h = this.height;
        if (!ConfigManager.showMenuBackground) {
            context.fill(0, 0, w, h, -16777216);
            renderAuroraButtons(context, mouseX, mouseY);
        } else {
            ParallaxState.updateMouseShake(mouseX, mouseY, w, h);
            ManagedShaderEffect shader = ShaderManager.AURORA;
            if (shader != null && shader.getShaderEffect() != null) {
                try {
                    RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 1.0F);
                    RenderSystem.clear(16640, false);
                    renderAuroraShader(shader, delta, w, h);
                    net.minecraft.client.gl.PostEffectProcessor effect = shader.getShaderEffect();
                    net.minecraft.client.gl.Framebuffer outBuffer = effect.getSecondaryTarget("bufOut");
                    if (outBuffer == null) {
                        context.fill(0, 0, w, h, 0xFF1A1A2E);
                        renderAuroraButtons(context, mouseX, mouseY);
                        return;
                    }

                    renderAuroraBackgroundFromBuffer(context, outBuffer, w, h);
                } catch (Exception e) {
                    e.printStackTrace();
                    context.fill(0, 0, w, h, 0xFF1A1A2E);
                }

                renderAuroraButtons(context, mouseX, mouseY);
            } else {
                context.fill(0, 0, w, h, 0xFF1A1A2E);
                renderAuroraButtons(context, mouseX, mouseY);
            }
        }
    }

    @Unique
    private void renderAuroraButtons(DrawContext context, int mouseX, int mouseY) {
        int startX = this.width / 2 - 100;
        int startY = this.height / 4 + 48;
        MatrixStack matrices = context.getMatrices();
        String title = "LumiClient";
        int titleW = this.client.textRenderer.getWidth(title);
        float titleScale = (float) this.width / 192.0F;
        int titleX = (int) ((float) (this.width / 2) - (float) titleW * titleScale / 2.0F);
        int titleY = this.height / 4 - 24;
        float time = (float) (System.currentTimeMillis() % 4000L) / 4000.0F;
        matrices.push();
        matrices.translate((float) titleX, (float) titleY, 0.0F);
        matrices.scale(titleScale, titleScale, 1.0F);
        float charX = 0.0F;

        for (int i = 0; i < title.length(); ++i) {
            String ch = title.substring(i, i + 1);
            int chW = this.client.textRenderer.getWidth(ch);
            float pos = (charX / (float) titleW + time) % 1.0F;
            float wave = 1.0F - Math.abs(pos * 2.0F - 1.0F);
            int brightness = (int) (51.0F + wave * 204.0F);
            int color = 0xFF000000 | brightness << 16 | brightness << 8 | brightness;
            context.drawText(this.client.textRenderer, ch, (int) charX, 0, color, false);
            charX += (float) chW;
        }

        matrices.pop();
        String singleplayer = Text.translatable("menu.singleplayer").getString();
        String multiplayer = Text.translatable("menu.multiplayer").getString();
        String options = Text.translatable("menu.options").getString();
        String quit = Text.translatable("menu.quit").getString();
        drawAuroraButton(context, matrices, singleplayer, startX, startY, mouseX, mouseY, false, "5");
        drawAuroraButton(context, matrices, multiplayer, startX, startY + 20 + 4, mouseX, mouseY, false, "4");
        drawAuroraButton(context, matrices, options, startX, startY + 48, mouseX, mouseY, false, "7");
        drawAuroraButton(context, matrices, "LumiClient", startX, startY + 72, mouseX, mouseY, false, "H");
        drawAuroraButton(context, matrices, quit, startX, startY + 96, mouseX, mouseY, true, ":");
        renderModMenuWidgets(context, mouseX, mouseY);
    }

    @Unique
    private void drawAlienButton(DrawContext context, MatrixStack matrices, String text, int x, int y, int mouseX, int mouseY, boolean isExit) {
        isMouseOver(x, y, 120, 35, mouseX, mouseY);
        drawAcrylicButton(context, matrices, text, x, y, 120, 35, mouseX, mouseY, isExit, new Color(40, 40, 50), new Color(60, 60, 80), new Color(200, 100, 100), new Color(220, 120, 120), null);
    }

    @Unique
    private void drawAuroraButton(DrawContext context, MatrixStack matrices, String text, int x, int y, int mouseX, int mouseY, boolean isExit, String iconChar) {
        isMouseOver(x, y, 200, 20, mouseX, mouseY);
        drawAcrylicButton(context, matrices, text, x, y, 200, 20, mouseX, mouseY, isExit, new Color(30, 30, 60), new Color(50, 50, 100), new Color(200, 100, 100), new Color(220, 120, 120), iconChar);
    }

    @Unique
    private void drawAcrylicButton(DrawContext context, MatrixStack matrices, String text, int x, int y, int w, int h, int mouseX, int mouseY, boolean isExit, Color normalBg, Color hoverBg, Color exitNormalBg, Color exitHoverBg, String iconChar) {
        boolean hovered = isMouseOver(x, y, w, h, mouseX, mouseY);
        float radius = (float) Math.min(w, h) * 0.25F;
        Color baseColor = isExit ? (hovered ? exitHoverBg : exitNormalBg) : (hovered ? hoverBg : normalBg);
        float bgAlpha = hovered ? 20.0F : 12.0F;
        float borderAlpha = hovered ? 50.0F : 30.0F;
        Render2DUtil.drawRoundedRectWithOutline(matrices, (float) x, (float) y, (float) w, (float) h, radius, new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), (int) bgAlpha), new Color(255, 255, 255, (int) borderAlpha));
        int textColor = hovered ? -1 : (isExit ? 0xFFAAAAAA : 0xFFB0B0B0);
        FontRenderer font = FontManager.ui;
        float textWidth = font.getWidth(text);
        float iconWidth = 0;
        if (iconChar != null) {
            iconWidth = FontManager.icon.getWidth(iconChar) + 4;
        }
        float totalWidth = textWidth + iconWidth;
        float textX = (float) x + ((float) w - totalWidth) / 2.0f + iconWidth;
        float textY = (float) y + ((float) h - font.getFontHeight()) / 2.0f;
        font.drawString(matrices, text, textX, textY, textColor, false);
        if (iconChar != null) {
            float iconX = (float) x + ((float) w - totalWidth) / 2.0f;
            float iconY = (float) y + ((float) h - FontManager.icon.getFontHeight()) / 2.0f;
            FontManager.icon.drawString(matrices, iconChar, iconX, iconY, textColor, false);
        }
    }

    @Unique
    private void renderModMenuWidgets(DrawContext context, int mouseX, int mouseY) {
        for (ClickableWidget widget : this.savedModMenuWidgets) {
            if ("aurora".equals(ConfigManager.menuStyle)) {
                MatrixStack matrices = context.getMatrices();
                float iconWidth = FontManager.icon.getWidth("T");
                float iconX = (float) widget.getX() - iconWidth - 4;
                float iconY = (float) widget.getY() + ((float) widget.getHeight() - FontManager.icon.getFontHeight()) / 2.0f;
                FontManager.icon.drawString(matrices, "T", iconX, iconY, 0xFFB0B0B0, false);
            }
            widget.render(context, mouseX, mouseY, 0);
        }
    }

    @Unique
    private void renderAuroraShader(ManagedShaderEffect shader, float delta, int w, int h) {
        ++this.auroraFrameCount;
        float t = (float) this.auroraFrameCount * 0.016F;
        long now = System.currentTimeMillis();
        if (now - this.lastShaderTime >= 41L) {
            float bgScale = 1.15F;
            shader.setUniformValue("alpha", 1.0F);
            shader.setUniformValue("resolution", (float) w * bgScale, (float) h * bgScale);
            shader.setUniformValue("time", t);
            shader.render(delta);
            this.lastShaderTime = now;
        }
    }

    @Unique
    private void renderAuroraBackgroundFromBuffer(DrawContext context, net.minecraft.client.gl.Framebuffer outBuffer, int w, int h) {
        this.client.getFramebuffer().beginWrite(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);
        RenderSystem.backupProjectionMatrix();
        int texW = outBuffer.textureWidth;
        int texH = outBuffer.textureHeight;
        float u0 = (float) (texW - w) / (2.0F * (float) texW) + ParallaxState.parallaxOffsetX / (float) texW;
        float v0 = (float) (texH - h) / (2.0F * (float) texH) + ParallaxState.parallaxOffsetY / (float) texH;
        float u1 = u0 + (float) w / (float) texW;
        float v1 = v0 + (float) h / (float) texH;
        RenderSystem.setShaderTexture(0, outBuffer.getColorAttachment());
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(matrix, 0.0F, (float) h, 0.0F).color(0xFFFFFFFF).texture(u0, v1);
        buffer.vertex(matrix, (float) w, (float) h, 0.0F).color(0xFFFFFFFF).texture(u1, v1);
        buffer.vertex(matrix, (float) w, 0.0F, 0.0F).color(0xFFFFFFFF).texture(u1, v0);
        buffer.vertex(matrix, 0.0F, 0.0F, 0.0F).color(0xFFFFFFFF).texture(u0, v0);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.restoreProjectionMatrix();
        RenderSystem.disableBlend();
    }

    @Unique
    private boolean isMouseOver(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= x && mouseY >= y && mouseX <= x + width && mouseY <= y + height;
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button == 0) {
            for (ClickableWidget widget : this.savedModMenuWidgets) {
                if (widget.isMouseOver(mouseX, mouseY)) {
                    widget.mouseClicked(mouseX, mouseY, button);
                    cir.setReturnValue(true);
                    return;
                }
            }
            String style = ConfigManager.menuStyle;
            if ("alien".equals(style)) {
                handleAlienClick(mouseX, mouseY, cir);
            } else {
                handleAuroraClick(mouseX, mouseY, cir);
            }
        }
    }

    @Unique
    private void handleAlienClick(double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir) {
        int startX = this.width - 120 - 30;
        int startY = 80;
        int[] ys = new int[]{startY, startY + 35 + 20, startY + 110, startY + 165, startY + 220};
        doClick(startX, ys, 120, 35, mouseX, mouseY, cir);
    }

    @Unique
    private void handleAuroraClick(double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir) {
        int startX = this.width / 2 - 100;
        int startY = this.height / 4 + 48;
        int[] ys = new int[]{startY, startY + 20 + 4, startY + 48, startY + 72, startY + 96};
        doClick(startX, ys, 200, 20, mouseX, mouseY, cir);
    }

    @Unique
    private void doClick(int startX, int[] ys, int bw, int bh, double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir) {
        if (isMouseOver(startX, ys[0], bw, bh, (int) mouseX, (int) mouseY)) {
            this.client.setScreen(new SelectWorldScreen(this));
            cir.setReturnValue(true);
        } else if (isMouseOver(startX, ys[1], bw, bh, (int) mouseX, (int) mouseY)) {
            this.client.setScreen(new MultiplayerScreen(this));
            cir.setReturnValue(true);
        } else if (isMouseOver(startX, ys[2], bw, bh, (int) mouseX, (int) mouseY)) {
            this.client.setScreen(new OptionsScreen(this, this.client.options));
            cir.setReturnValue(true);
        } else if (isMouseOver(startX, ys[3], bw, bh, (int) mouseX, (int) mouseY)) {
            this.client.setScreen(new MenuStyleScreen(this, null));
            cir.setReturnValue(true);
        } else {
            if (isMouseOver(startX, ys[4], bw, bh, (int) mouseX, (int) mouseY)) {
                this.client.scheduleStop();
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void onRemoved(CallbackInfo ci) {
        this.savedModMenuWidgets.clear();
    }
}