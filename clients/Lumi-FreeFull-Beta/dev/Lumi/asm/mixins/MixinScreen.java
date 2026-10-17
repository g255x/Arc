package dev.Lumi.asm.mixins;

import com.mojang.blaze3d.platform.GlStateManager.class_4534;
import com.mojang.blaze3d.platform.GlStateManager.class_4535;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.Lumi.Lumi;
import dev.Lumi.core.impl.ConfigManager;
import dev.Lumi.core.impl.ShaderManager;
import dev.Lumi.mod.modules.impl.client.ClickGui;
import dev.Lumi.mod.modules.impl.client.ClientSetting;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import javax.imageio.ImageIO;
import net.minecraft.class_1011;
import net.minecraft.class_1043;
import net.minecraft.class_276;
import net.minecraft.class_279;
import net.minecraft.class_286;
import net.minecraft.class_287;
import net.minecraft.class_289;
import net.minecraft.class_290;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_364;
import net.minecraft.class_4068;
import net.minecraft.class_437;
import net.minecraft.class_442;
import net.minecraft.class_757;
import net.minecraft.class_8030;
import net.minecraft.class_293.class_5596;
import org.joml.Matrix4f;
import org.ladysnake.satin.api.managed.ManagedShaderEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({class_437.class})
public class MixinScreen {
   @Shadow
   public int field_22789;
   @Shadow
   public int field_22790;
   @Shadow
   protected class_310 field_22787;
   @Unique
   private static final class_2960 ALIEN_BG_TEXTURE = class_2960.method_60655("lumi", "gui/ba");
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
   public void renderInGameBackgroundHook(class_332 context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      ci.cancel();
      if (!(this instanceof class_442)) {
         if (this.field_22787.field_1687 == null) {
            this.renderLumiBackground(context, mouseX, mouseY);
         }

         if (ConfigManager.blurScreenBackground) {
            Lumi.BLUR.applyBlur(ConfigManager.blurScreenRadius, 0.0F, 0.0F, (float)this.field_22789, (float)this.field_22790);
         }

         if (ClientSetting.INSTANCE.darkening.getValue() && !(this instanceof class_442)) {
            this.method_57735(context);
         }

         if (this.field_22787.field_1687 != null && ClickGui.getInstance().tint.booleanValue) {
            context.method_25296(0, 0, this.field_22789, this.field_22790, ClickGui.getInstance().tint.getValue().getRGB(), ClickGui.getInstance().endColor.getValue().getRGB());
         }

      }
   }

   @Unique
   private void renderLumiBackground(class_332 context, int mouseX, int mouseY) {
      int w = this.field_22789;
      int h = this.field_22790;
      String style = ConfigManager.menuStyle;
      if ("alien".equals(style)) {
         this.renderAlienBackground(context, w, h, mouseX, mouseY);
      } else {
         this.renderAuroraBackground(context, w, h, mouseX, mouseY);
      }

   }

   @Unique
   private void renderAlienBackground(class_332 context, int w, int h, int mouseX, int mouseY) {
      context.method_25294(0, 0, w, h, -16777216);
      if (ConfigManager.showMenuBackground) {
         this.loadAlienTextureIfNeeded();
         if (!alienTextureLoaded) {
            context.method_25294(0, 0, w, h, -15066598);
         } else {
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
            context.method_25290(ALIEN_BG_TEXTURE, drawX, drawY, 0.0F, 0.0F, scaledWidth, scaledHeight, scaledWidth, scaledHeight);
            context.method_25294(0, 0, w, h, 285212672);
         }
      }
   }

   @Unique
   private void loadAlienTextureIfNeeded() {
      if (this.field_22787 != null) {
         String path = ConfigManager.menuBackgroundPath;
         if (!alienTextureLoaded || !path.equals(alienLoadedPath)) {
            if (alienTextureLoaded) {
               this.field_22787.method_1531().method_4615(ALIEN_BG_TEXTURE);
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

               class_1011 nativeImage = this.readBgImage(stream, path);
               stream.close();
               if (nativeImage == null) {
                  return;
               }

               alienImageWidth = nativeImage.method_4307();
               alienImageHeight = nativeImage.method_4323();
               class_1043 texture = new class_1043(nativeImage);
               this.field_22787.method_1531().method_4616(ALIEN_BG_TEXTURE, texture);
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
   private class_1011 readBgImage(InputStream stream, String path) {
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
         } catch (Exception var15) {
            return null;
         }
      }
   }

   @Unique
   private void renderAuroraBackground(class_332 context, int w, int h, int mouseX, int mouseY) {
      if (Lumi.SHADER.fullNullCheck()) {
         context.method_25294(0, 0, w, h, -15070680);
      } else {
         ManagedShaderEffect aurora = Lumi.SHADER.getShader(ShaderManager.Shader.Aurora);
         if (aurora != null && aurora.getShaderEffect() != null) {
            try {
               float bgScale = 1.15F;
               RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 1.0F);
               RenderSystem.clear(16640, false);
               ++auroraFrameCount;
               float t = (float)auroraFrameCount * 0.016F;
               aurora.setUniformValue("alpha", 0.35F);
               aurora.setUniformValue("resolution", (float)w * bgScale, (float)h * bgScale);
               aurora.setUniformValue("time", t);
               aurora.render(this.field_22787.method_60646().method_60637(true));
               class_279 effect = aurora.getShaderEffect();
               class_276 outBuffer = effect.method_1264("bufOut");
               if (outBuffer == null) {
                  context.method_25294(0, 0, w, h, -15070680);
                  return;
               }

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

               this.field_22787.method_1522().method_1235(false);
               RenderSystem.enableBlend();
               RenderSystem.blendFuncSeparate(class_4535.SRC_ALPHA, class_4534.ONE_MINUS_SRC_ALPHA, class_4535.ZERO, class_4534.ONE);
               RenderSystem.backupProjectionMatrix();
               int texW = outBuffer.field_1482;
               int texH = outBuffer.field_1481;
               float u0 = (float)(texW - w) / (2.0F * (float)texW) + shakeX / (float)texW;
               float v0 = (float)(texH - h) / (2.0F * (float)texH) + shakeY / (float)texH;
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
            } catch (Exception var22) {
               context.method_25294(0, 0, w, h, -15070680);
            }

         } else {
            context.method_25294(0, 0, w, h, -15070680);
         }
      }
   }

   @Shadow
   protected void method_57728(class_332 context, float delta) {
   }

   @Shadow
   protected void method_57735(class_332 context) {
   }

   @Shadow
   public void method_25419() {
   }

   @Shadow
   public class_8030 method_48202() {
      return null;
   }

   @Shadow
   protected <T extends class_364 & class_4068> T method_37063(T drawableElement) {
      return null;
   }
}
