package dev.Lumi.asm.mixins;

import com.mojang.blaze3d.platform.GlStateManager.class_4534;
import com.mojang.blaze3d.platform.GlStateManager.class_4535;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.Lumi.MenuStyleScreen;
import dev.Lumi.api.utils.render.Render2DUtil;
import dev.Lumi.core.impl.ConfigManager;
import dev.Lumi.core.impl.ShaderManager;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import javax.imageio.ImageIO;
import net.minecraft.class_1011;
import net.minecraft.class_1043;
import net.minecraft.class_2561;
import net.minecraft.class_276;
import net.minecraft.class_279;
import net.minecraft.class_286;
import net.minecraft.class_287;
import net.minecraft.class_289;
import net.minecraft.class_290;
import net.minecraft.class_2960;
import net.minecraft.class_332;
import net.minecraft.class_429;
import net.minecraft.class_437;
import net.minecraft.class_442;
import net.minecraft.class_4587;
import net.minecraft.class_500;
import net.minecraft.class_526;
import net.minecraft.class_757;
import net.minecraft.class_293.class_5596;
import org.joml.Matrix4f;
import org.ladysnake.satin.api.managed.ManagedShaderEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({class_442.class})
public abstract class MixinTitleScreen extends class_437 {
   private static final int ALIEN_BUTTON_W = 120;
   private static final int ALIEN_BUTTON_H = 35;
   private static final int ALIEN_BUTTON_GAP = 20;
   private static final int AURORA_BUTTON_W = 200;
   private static final int AURORA_BUTTON_H = 20;
   private static final int AURORA_BUTTON_GAP = 4;
   @Unique
   private static class_2960 alienBgTexture = null;
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
   private float parallaxOffsetX = 0.0F;
   @Unique
   private float parallaxOffsetY = 0.0F;

   protected MixinTitleScreen(class_2561 title) {
      super(title);
   }

   @Inject(
      method = {"initWidgetsNormal"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onInitWidgets(int y, int spacingY, CallbackInfo ci) {
      ci.cancel();
   }

   @Inject(
      method = {"init"},
      at = {@At("RETURN")}
   )
   private void onInit(CallbackInfo ci) {
   }

   @Unique
   private void loadAlienTexture() {
      if (this.field_22787 != null) {
         String path = ConfigManager.menuBackgroundPath;
         if (alienBgTexture == null || !path.equals(loadedBgPath)) {
            if (alienBgTexture != null) {
               this.field_22787.method_1531().method_4615(alienBgTexture);
               alienBgTexture = null;
            }

            alienTextureLoaded = false;
            loadedBgPath = null;

            try {
               InputStream stream = this.openBgStream(path);
               if (stream == null) {
                  stream = MixinTitleScreen.class.getResourceAsStream("/assets/lumi/textures/gui/ba.png");
                  if (stream == null) {
                     return;
                  }
               }

               class_1011 nativeImage = this.readNativeImage(stream, path);
               stream.close();
               if (nativeImage == null) {
                  return;
               }

               alienImageWidth = nativeImage.method_4307();
               alienImageHeight = nativeImage.method_4323();
               class_1043 texture = new class_1043(nativeImage);
               alienBgTexture = class_2960.method_60655("lumi", "title_background");
               this.field_22787.method_1531().method_4616(alienBgTexture, texture);
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
   private class_1011 readNativeImage(InputStream stream, String path) {
      try {
         return class_1011.method_4309(stream);
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
                  class_1011 ni = new class_1011(bi.getWidth(), bi.getHeight(), false);

                  for(int py = 0; py < bi.getHeight(); ++py) {
                     for(int px = 0; px < bi.getWidth(); ++px) {
                        int argb = bi.getRGB(px, py);
                        int a = argb >> 24 & 255;
                        int r = argb >> 16 & 255;
                        int g = argb >> 8 & 255;
                        int b = argb & 255;
                        ni.method_4305(px, py, a << 24 | b << 16 | g << 8 | r);
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

   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRender(class_332 context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      ci.cancel();
      String style = ConfigManager.menuStyle;
      if ("alien".equals(style)) {
         this.renderAlien(context, mouseX, mouseY, delta);
      } else {
         this.renderAurora(context, mouseX, mouseY, delta);
      }

   }

   @Unique
   private void renderAlien(class_332 context, int mouseX, int mouseY, float delta) {
      this.loadAlienTexture();
      int w = this.field_22789;
      int h = this.field_22790;
      context.method_25294(0, 0, w, h, -16777216);
      if (ConfigManager.showMenuBackground && alienTextureLoaded && alienBgTexture != null) {
         float scale = 0.55F;
         int scaledWidth = (int)((float)alienImageWidth * scale);
         int scaledHeight = (int)((float)alienImageHeight * scale);
         float shakeX = 0.0F;
         float shakeY = 0.0F;
         if (ConfigManager.menuMouseShake) {
            float strength = ConfigManager.menuMouseShakeStrength;
            shakeX = ((float)mouseX - (float)w / 2.0F) / 40.0F * strength;
            shakeY = ((float)mouseY - (float)h / 2.0F) / 40.0F * strength;
            if (ConfigManager.menuMouseShakeReverse) {
               shakeX = -shakeX;
               shakeY = -shakeY;
            }
         }

         int drawX = (w - scaledWidth) / 2 + (int)shakeX;
         int drawY = (h - scaledHeight) / 2 + (int)shakeY;
         context.method_25290(alienBgTexture, drawX, drawY, 0.0F, 0.0F, scaledWidth, scaledHeight, scaledWidth, scaledHeight);
         context.method_25294(0, 0, w, h, 285212672);
      }

      int startX = this.field_22789 - 120 - 30;
      int startY = 80;
      class_4587 matrices = context.method_51448();
      this.drawAlienButton(context, matrices, "单人游戏", startX, startY, mouseX, mouseY, false);
      this.drawAlienButton(context, matrices, "多人游戏", startX, startY + 35 + 20, mouseX, mouseY, false);
      this.drawAlienButton(context, matrices, "选项", startX, startY + 110, mouseX, mouseY, false);
      this.drawAlienButton(context, matrices, "LumiClient", startX, startY + 165, mouseX, mouseY, false);
      this.drawAlienButton(context, matrices, "退出游戏", startX, startY + 220, mouseX, mouseY, true);
   }

   @Unique
   private void renderAurora(class_332 context, int mouseX, int mouseY, float delta) {
      int w = this.field_22789;
      int h = this.field_22790;
      if (!ConfigManager.showMenuBackground) {
         context.method_25294(0, 0, w, h, -16777216);
         this.renderAuroraButtons(context, mouseX, mouseY);
      } else {
         this.updateMouseShake(mouseX, mouseY, w, h);
         ManagedShaderEffect shader = ShaderManager.AURORA;
         if (shader != null && shader.getShaderEffect() != null) {
            try {
               RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 1.0F);
               RenderSystem.clear(16640, false);
               this.renderAuroraShader(shader, delta, w, h);
               class_279 effect = shader.getShaderEffect();
               class_276 outBuffer = effect.method_1264("bufOut");
               if (outBuffer == null) {
                  context.method_25294(0, 0, w, h, -15070680);
                  this.renderAuroraButtons(context, mouseX, mouseY);
                  return;
               }

               this.renderAuroraBackgroundFromBuffer(context, outBuffer, w, h);
            } catch (Exception e) {
               e.printStackTrace();
               context.method_25294(0, 0, w, h, -15070680);
            }

            this.renderAuroraButtons(context, mouseX, mouseY);
         } else {
            context.method_25294(0, 0, w, h, -15070680);
            this.renderAuroraButtons(context, mouseX, mouseY);
         }
      }
   }

   @Unique
   private void renderAuroraButtons(class_332 context, int mouseX, int mouseY) {
      int startX = this.field_22789 / 2 - 100;
      int startY = this.field_22790 / 4 + 48;
      class_4587 matrices = context.method_51448();
      String title = "LumiClient";
      int titleW = this.field_22787.field_1772.method_1727(title);
      float titleScale = (float)this.field_22789 / 192.0F;
      int titleX = (int)((float)(this.field_22789 / 2) - (float)titleW * titleScale / 2.0F);
      int titleY = this.field_22790 / 4 - 24;
      float time = (float)(System.currentTimeMillis() % 4000L) / 4000.0F;
      matrices.method_22903();
      matrices.method_46416((float)titleX, (float)titleY, 0.0F);
      matrices.method_22905(titleScale, titleScale, 1.0F);
      float charX = 0.0F;

      for(int i = 0; i < title.length(); ++i) {
         String ch = title.substring(i, i + 1);
         int chW = this.field_22787.field_1772.method_1727(ch);
         float pos = (charX / (float)titleW + time) % 1.0F;
         float wave = 1.0F - Math.abs(pos * 2.0F - 1.0F);
         int brightness = (int)(51.0F + wave * 204.0F);
         int color = -16777216 | brightness << 16 | brightness << 8 | brightness;
         context.method_51433(this.field_22787.field_1772, ch, (int)charX, 0, color, false);
         charX += (float)chW;
      }

      matrices.method_22909();
      this.drawAuroraButton(context, matrices, "单人游戏", startX, startY, mouseX, mouseY, false);
      this.drawAuroraButton(context, matrices, "多人游戏", startX, startY + 20 + 4, mouseX, mouseY, false);
      this.drawAuroraButton(context, matrices, "选项", startX, startY + 48, mouseX, mouseY, false);
      this.drawAuroraButton(context, matrices, "LumiClient", startX, startY + 72, mouseX, mouseY, false);
      this.drawAuroraButton(context, matrices, "退出游戏", startX, startY + 96, mouseX, mouseY, true);
   }

   @Unique
   private void drawAlienButton(class_332 context, class_4587 matrices, String text, int x, int y, int mouseX, int mouseY, boolean isExit) {
      this.isMouseOver(x, y, 120, 35, mouseX, mouseY);
      this.drawAcrylicButton(context, matrices, text, x, y, 120, 35, mouseX, mouseY, isExit, new Color(40, 40, 50), new Color(60, 60, 80), new Color(200, 100, 100), new Color(220, 120, 120));
   }

   @Unique
   private void drawAuroraButton(class_332 context, class_4587 matrices, String text, int x, int y, int mouseX, int mouseY, boolean isExit) {
      this.isMouseOver(x, y, 200, 20, mouseX, mouseY);
      this.drawAcrylicButton(context, matrices, text, x, y, 200, 20, mouseX, mouseY, isExit, new Color(30, 30, 60), new Color(50, 50, 100), new Color(200, 100, 100), new Color(220, 120, 120));
   }

   @Unique
   private void drawAcrylicButton(class_332 context, class_4587 matrices, String text, int x, int y, int w, int h, int mouseX, int mouseY, boolean isExit, Color normalBg, Color hoverBg, Color exitNormalBg, Color exitHoverBg) {
      boolean hovered = this.isMouseOver(x, y, w, h, mouseX, mouseY);
      float radius = (float)Math.min(w, h) * 0.25F;
      Color baseColor = isExit ? (hovered ? exitHoverBg : exitNormalBg) : (hovered ? hoverBg : normalBg);
      float bgAlpha = hovered ? 20.0F : 12.0F;
      float borderAlpha = hovered ? 50.0F : 30.0F;
      Render2DUtil.drawRoundedRectWithOutline(matrices, (float)x, (float)y, (float)w, (float)h, radius, new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), (int)bgAlpha), new Color(255, 255, 255, (int)borderAlpha));
      int textColor = hovered ? -1 : (isExit ? -5592406 : -1325400065);
      int textWidth = this.field_22787.field_1772.method_1727(text);
      int textX = x + (w - textWidth) / 2;
      int textY = y + (h - 8) / 2;
      context.method_51433(this.field_22787.field_1772, text, textX, textY, textColor, false);
   }

   @Unique
   private void updateMouseShake(int mouseX, int mouseY, int w, int h) {
      if (ConfigManager.menuMouseShake) {
         float strength = ConfigManager.menuMouseShakeStrength;
         float targetX = (float)(mouseX - w / 2) / 60.0F * strength;
         float targetY = (float)(mouseY - h / 2) / 60.0F * strength;
         if (ConfigManager.menuMouseShakeReverse) {
            targetX = -targetX;
            targetY = -targetY;
         }

         this.parallaxOffsetX += (targetX - this.parallaxOffsetX) * 0.15F;
         this.parallaxOffsetY += (targetY - this.parallaxOffsetY) * 0.15F;
      } else {
         this.parallaxOffsetX = 0.0F;
         this.parallaxOffsetY = 0.0F;
      }

   }

   @Unique
   private void renderAuroraShader(ManagedShaderEffect shader, float delta, int w, int h) {
      ++this.auroraFrameCount;
      float t = (float)this.auroraFrameCount * 0.016F;
      long now = System.currentTimeMillis();
      if (now - this.lastShaderTime >= 41L) {
         float bgScale = 1.15F;
         shader.setUniformValue("alpha", 1.0F);
         shader.setUniformValue("resolution", (float)w * bgScale, (float)h * bgScale);
         shader.setUniformValue("time", t);
         shader.render(delta);
         this.lastShaderTime = now;
      }

   }

   @Unique
   private void renderAuroraBackgroundFromBuffer(class_332 context, class_276 outBuffer, int w, int h) {
      this.field_22787.method_1522().method_1235(false);
      RenderSystem.enableBlend();
      RenderSystem.blendFuncSeparate(class_4535.SRC_ALPHA, class_4534.ONE_MINUS_SRC_ALPHA, class_4535.ZERO, class_4534.ONE);
      RenderSystem.backupProjectionMatrix();
      int texW = outBuffer.field_1482;
      int texH = outBuffer.field_1481;
      float u0 = (float)(texW - w) / (2.0F * (float)texW) + this.parallaxOffsetX / (float)texW;
      float v0 = (float)(texH - h) / (2.0F * (float)texH) + this.parallaxOffsetY / (float)texH;
      float u1 = u0 + (float)w / (float)texW;
      float v1 = v0 + (float)h / (float)texH;
      RenderSystem.setShaderTexture(0, outBuffer.method_30277());
      RenderSystem.setShader(class_757::method_34542);
      Matrix4f matrix = context.method_51448().method_23760().method_23761();
      class_289 tessellator = class_289.method_1348();
      class_287 buffer = tessellator.method_60827(class_5596.field_27382, class_290.field_1585);
      buffer.method_22918(matrix, 0.0F, (float)h, 0.0F).method_22913(u0, v1);
      buffer.method_22918(matrix, (float)w, (float)h, 0.0F).method_22913(u1, v1);
      buffer.method_22918(matrix, (float)w, 0.0F, 0.0F).method_22913(u1, v0);
      buffer.method_22918(matrix, 0.0F, 0.0F, 0.0F).method_22913(u0, v0);
      class_286.method_43433(buffer.method_60800());
      RenderSystem.restoreProjectionMatrix();
      RenderSystem.disableBlend();
   }

   @Unique
   private boolean isMouseOver(int x, int y, int width, int height, int mouseX, int mouseY) {
      return mouseX >= x && mouseY >= y && mouseX <= x + width && mouseY <= y + height;
   }

   @Inject(
      method = {"mouseClicked"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
      if (button == 0) {
         String style = ConfigManager.menuStyle;
         if ("alien".equals(style)) {
            this.handleAlienClick(mouseX, mouseY, cir);
         } else {
            this.handleAuroraClick(mouseX, mouseY, cir);
         }

      }
   }

   @Unique
   private void handleAlienClick(double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir) {
      int startX = this.field_22789 - 120 - 30;
      int startY = 80;
      int[] ys = new int[]{startY, startY + 35 + 20, startY + 110, startY + 165, startY + 220};
      this.doClick(startX, ys, 120, 35, mouseX, mouseY, cir);
   }

   @Unique
   private void handleAuroraClick(double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir) {
      int startX = this.field_22789 / 2 - 100;
      int startY = this.field_22790 / 4 + 48;
      int[] ys = new int[]{startY, startY + 20 + 4, startY + 48, startY + 72, startY + 96};
      this.doClick(startX, ys, 200, 20, mouseX, mouseY, cir);
   }

   @Unique
   private void doClick(int startX, int[] ys, int bw, int bh, double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir) {
      if (this.isMouseOver(startX, ys[0], bw, bh, (int)mouseX, (int)mouseY)) {
         this.field_22787.method_1507(new class_526(this));
         cir.setReturnValue(true);
      } else if (this.isMouseOver(startX, ys[1], bw, bh, (int)mouseX, (int)mouseY)) {
         this.field_22787.method_1507(new class_500(this));
         cir.setReturnValue(true);
      } else if (this.isMouseOver(startX, ys[2], bw, bh, (int)mouseX, (int)mouseY)) {
         this.field_22787.method_1507(new class_429(this, this.field_22787.field_1690));
         cir.setReturnValue(true);
      } else if (this.isMouseOver(startX, ys[3], bw, bh, (int)mouseX, (int)mouseY)) {
         this.field_22787.method_1507(new MenuStyleScreen(this, (String)null));
         cir.setReturnValue(true);
      } else {
         if (this.isMouseOver(startX, ys[4], bw, bh, (int)mouseX, (int)mouseY)) {
            this.field_22787.method_1592();
            cir.setReturnValue(true);
         }

      }
   }

   @Inject(
      method = {"removed"},
      at = {@At("HEAD")}
   )
   private void onRemoved(CallbackInfo ci) {
   }
}
