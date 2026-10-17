package dev.G255.Lumi.asm.mixins;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.G255.Lumi.Lumi;
import dev.G255.Lumi.core.impl.ConfigManager;
import dev.G255.Lumi.core.impl.ParallaxState;
import dev.G255.Lumi.core.impl.ShaderManager;
import dev.G255.Lumi.mod.modules.impl.client.ClickGui;
import dev.G255.Lumi.mod.modules.impl.client.ClientSetting;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import javax.imageio.ImageIO;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.ladysnake.satin.api.managed.ManagedShaderEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Screen.class})
public class MixinScreen {
    @Shadow
    public int width;
    @Shadow
    public int height;
    @Shadow
    protected MinecraftClient client;
    @Unique
    private static final Identifier ALIEN_BG_TEXTURE = Identifier.of("lumi", "gui/ba");
    @Unique
    private static boolean alienTextureLoaded = false;
    @Unique
    private static int alienImageWidth = 0;
    @Unique
    private static int alienImageHeight = 0;
    @Unique
    private static String alienLoadedPath = null;
    @Unique
    private static int auroraFrameCount = 0;

    @Inject(
        method = {"renderBackground"},
        at = {@At("HEAD")},
        cancellable = true
    )
    public void renderInGameBackgroundHook(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.client == null) return;
        if (this.client.getFramebuffer() == null) return;
        if (this.client.getFramebuffer().textureWidth <= 0 || this.client.getFramebuffer().textureHeight <= 0) return;
        ci.cancel();
        if (!((Object) this instanceof TitleScreen)) {
            if (this.client.world == null) {
                this.renderLumiBackground(context, mouseX, mouseY);
            }

            if (ConfigManager.blurScreenBackground) {
                Lumi.BLUR.applyBlur(ConfigManager.blurScreenRadius, 0.0F, 0.0F, (float) this.width, (float) this.height);
            }

            if (ClientSetting.INSTANCE.darkening.getValue() && !((Object) this instanceof TitleScreen)) {
                this.renderDarkening(context);
            }

            if (this.client.world != null && ClickGui.getInstance().tint.booleanValue) {
                context.fillGradient(0, 0, this.width, this.height, ClickGui.getInstance().tint.getValue().getRGB(), ClickGui.getInstance().endColor.getValue().getRGB());
            }
        }
    }

    @Unique
    private void renderLumiBackground(DrawContext context, int mouseX, int mouseY) {
        int w = this.width;
        int h = this.height;
        String style = ConfigManager.menuStyle;
        if ("alien".equals(style)) {
            this.renderAlienBackground(context, w, h, mouseX, mouseY);
        } else {
            this.renderAuroraBackground(context, w, h, mouseX, mouseY);
        }
    }

    @Unique
    private void renderAlienBackground(DrawContext context, int w, int h, int mouseX, int mouseY) {
        context.fill(0, 0, w, h, -16777216);
        if (ConfigManager.showMenuBackground) {
            this.loadAlienTextureIfNeeded();
            if (!alienTextureLoaded) {
                context.fill(0, 0, w, h, -15066598);
            } else {
                float scale = 0.55F;
                int scaledWidth = (int) ((float) alienImageWidth * scale);
                int scaledHeight = (int) ((float) alienImageHeight * scale);
                updateMouseShake(mouseX, mouseY, w, h);

                int drawX = (w - scaledWidth) / 2 + (int) ParallaxState.parallaxOffsetX;
                int drawY = (h - scaledHeight) / 2 + (int) ParallaxState.parallaxOffsetY;
                context.drawTexture(ALIEN_BG_TEXTURE, drawX, drawY, 0.0F, 0.0F, scaledWidth, scaledHeight, scaledWidth, scaledHeight);
                context.fill(0, 0, w, h, 0x11000000);
            }
        }
    }

    @Unique
    private void loadAlienTextureIfNeeded() {
        if (this.client != null) {
            String path = ConfigManager.menuBackgroundPath;
            if (!alienTextureLoaded || !path.equals(alienLoadedPath)) {
                if (alienTextureLoaded) {
                    this.client.getTextureManager().destroyTexture(ALIEN_BG_TEXTURE);
                    alienTextureLoaded = false;
                }

                alienLoadedPath = null;

                try {
                    InputStream stream = this.openBgStream(path);
                    if (stream == null) {
                        stream = MixinScreen.class.getResourceAsStream("/assets/lumi/textures/gui/ba.png");
                        if (stream == null) {
                            return;
                        }
                    }

                    NativeImage nativeImage = this.readBgImage(stream, path);
                    stream.close();
                    if (nativeImage == null) {
                        return;
                    }

                    alienImageWidth = nativeImage.getWidth();
                    alienImageHeight = nativeImage.getHeight();
                    NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);
                    this.client.getTextureManager().registerTexture(ALIEN_BG_TEXTURE, texture);
                    alienTextureLoaded = true;
                    alienLoadedPath = path;
                } catch (Exception var5) {
                }
            }
        }
    }

    @Unique
    private InputStream openBgStream(String path) {
        try {
            if ((path.length() < 2 || path.charAt(1) != ':') && !path.startsWith("/") && !path.startsWith("\\")) {
                return MixinScreen.class.getResourceAsStream("/assets/lumi/textures/gui/" + path);
            } else {
                File file = new File(path);
                return !file.exists() ? null : new BufferedInputStream(new FileInputStream(file));
            }
        } catch (Exception var3) {
            return null;
        }
    }

    @Unique
    private NativeImage readBgImage(InputStream stream, String path) {
        try {
            return NativeImage.read(stream);
        } catch (Exception var16) {
            try {
                stream.close();
            } catch (Exception var14) {
            }

            try {
                InputStream is = this.openBgStream(path);
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
            } catch (Exception var15) {
                return null;
            }
        }
    }

    @Unique
    private void renderAuroraBackground(DrawContext context, int w, int h, int mouseX, int mouseY) {
        if (Lumi.SHADER.fullNullCheck()) {
            context.fill(0, 0, w, h, -15070680);
        } else {
            ManagedShaderEffect aurora = ShaderManager.AURORA;
            if (aurora != null && aurora.getShaderEffect() != null) {
                try {
                    float bgScale = 1.15F;
                    RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 1.0F);
                    RenderSystem.clear(16640, false);
                    ++auroraFrameCount;
                    float t = (float) auroraFrameCount * 0.016F;
                    aurora.setUniformValue("alpha", 0.35F);
                    aurora.setUniformValue("resolution", (float) w * bgScale, (float) h * bgScale);
                    aurora.setUniformValue("time", t);
                    aurora.render(this.client.getRenderTickCounter().getTickDelta(true));
                    PostEffectProcessor effect = aurora.getShaderEffect();
                    Framebuffer outBuffer = effect.getSecondaryTarget("bufOut");
                    if (outBuffer == null || outBuffer.textureWidth <= 0 || outBuffer.textureHeight <= 0) {
                        context.fill(0, 0, w, h, -15070680);
                        return;
                    }

                    float shakeX = 0.0F;
                    float shakeY = 0.0F;
                    if (ConfigManager.menuMouseShake) {
                        float strength = ConfigManager.menuMouseShakeStrength;
                        shakeX = ((float) mouseX - (float) w / 2.0F) / 40.0F * strength;
                        shakeY = ((float) mouseY - (float) h / 2.0F) / 40.0F * strength;
                        if (ConfigManager.menuMouseShakeReverse) {
                            shakeX = -shakeX;
                            shakeY = -shakeY;
                        }
                    }

                    this.client.getFramebuffer().beginWrite(false);
                    RenderSystem.enableBlend();
                    RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);
                    RenderSystem.backupProjectionMatrix();
                    int texW = outBuffer.textureWidth;
                    int texH = outBuffer.textureHeight;
                    float u0 = (float) (texW - w) / (2.0F * (float) texW) + shakeX / (float) texW;
                    float v0 = (float) (texH - h) / (2.0F * (float) texH) + shakeY / (float) texH;
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
                } catch (Exception var22) {
                    context.fill(0, 0, w, h, -15070680);
                }
            } else {
                context.fill(0, 0, w, h, -15070680);
            }
        }
    }

    @Unique
    private void updateMouseShake(int mouseX, int mouseY, int w, int h) {
        ParallaxState.updateMouseShake(mouseX, mouseY, w, h);
    }

    @Shadow
    protected void renderDarkening(DrawContext context) {
    }

    @Shadow
    public void close() {
    }

    @Shadow
    public ScreenRect getNavigationFocus() {
        return null;
    }

    @Shadow
    protected <T extends Element & Drawable> T addDrawableChild(T drawableElement) {
        return null;
    }
}