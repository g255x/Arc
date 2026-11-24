package dev.g255.arc.asm.mixins.loadscreen;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.g255.arc.api.utils.log.LoadingLog;
import dev.g255.arc.api.utils.render.ArcShader;
import dev.g255.arc.core.impl.FontManager;
import dev.g255.arc.mod.gui.fonts.FontRenderer;
import dev.g255.arc.mod.modules.impl.client.ClickGui;
import dev.g255.arc.mod.modules.impl.client.Fonts;
import java.awt.Color;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.resource.ResourceReload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 加载界面（SplashOverlay）整屏接管，效果对齐 OpenHachimi 的 SplashOverlayMixin：
 * 背景是 {@code assets/minecraft/shaders/arc/loading.fsh} 的光柱隧道（{@link ArcShader} 直接编译），
 * 中间 logo 先整张以 40% 灰铺一遍 -> 再按加载进度从左往右用主题色裁切覆盖，
 * 视觉上就是「logo 随装载进度被点亮」，底部保留一条原版进度条。
 *
 * <p>原实现是在 render 的 TAIL 上盖一张 1920x1080 的 loading.png，底下的原版 Mojang 红底、
 * 进度条仍然露在外面。现在改为 HEAD cancel 后自己整屏绘制。
 * loading.png 文件保留在 assets/arc/textures/gui/ 里没有删除，只是不再被这里引用。
 *
 * <p>着色器不可用时（编译失败/驱动不支持）整块退回纯黑底，logo 与进度条不受影响。
 */
@Mixin({SplashOverlay.class})
public abstract class MixinLoadingOverlay {
   @Unique
   private static final int ARC$BACKGROUND = new Color(0, 0, 0, 255).getRGB();
   @Unique
   private static final Identifier ARC$LOGO = Identifier.tryParse("arc", "textures/gui/logo.png");
   @Shadow
   @Final
   private boolean reloading;
   @Shadow
   @Final
   private MinecraftClient client;
   @Shadow
   private float progress;
   @Shadow
   private long reloadCompleteTime;
   @Shadow
   private long reloadStartTime;
   @Shadow
   @Final
   private ResourceReload reload;
   @Shadow
   @Final
   private Consumer<Optional<Throwable>> exceptionHandler;

   @Shadow
   private static int withAlpha(int color, int alpha) {
      throw new AssertionError();
   }

   @Shadow
   protected abstract void renderProgressBar(DrawContext context, int x1, int y1, int x2, int y2, float delta);

   @Inject(
      method = {"render(Lnet/minecraft/client/gui/DrawContext;IIF)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void arc$render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo callbackInfo) {
      callbackInfo.cancel();
      this.arc$renderCustom(context, mouseX, mouseY, delta);
   }

   @Unique
   private void arc$renderCustom(DrawContext context, int mouseX, int mouseY, float delta) {
      int width = context.getScaledWindowWidth();
      int height = context.getScaledWindowHeight();
      long now = Util.getMeasuringTimeMs();
      if (this.reloading && this.reloadStartTime == -1L) {
         this.reloadStartTime = now;
      }

      float fadeOut = this.reloadCompleteTime > -1L ? (float)(now - this.reloadCompleteTime) / 1000.0F : -1.0F;
      float fadeIn = this.reloadStartTime > -1L ? (float)(now - this.reloadStartTime) / 500.0F : -1.0F;
      float alpha;
      float background;
      if (fadeOut >= 1.0F) {
         if (this.client.currentScreen != null) {
            this.client.currentScreen.render(context, 0, 0, delta);
         }

         alpha = 1.0F - MathHelper.clamp(fadeOut - 1.0F, 0.0F, 1.0F);
         background = alpha;
      } else if (this.reloading) {
         if (this.client.currentScreen != null && fadeIn < 1.0F) {
            this.client.currentScreen.render(context, mouseX, mouseY, delta);
         }

         // 背景（着色器/黑底）比 logo 更早压上来，所以下限是 0.15 而不是 0
         background = MathHelper.clamp(fadeIn, 0.15F, 1.0F);
         alpha = MathHelper.clamp(fadeIn, 0.0F, 1.0F);
      } else {
         // 启动那几帧帧缓冲里是上一帧的残留，先清成黑的：着色器万一编译失败也有底色顶着
         GlStateManager._clearColor(0.0F, 0.0F, 0.0F, 1.0F);
         GlStateManager._clear(16384, MinecraftClient.IS_SYSTEM_MAC);
         alpha = 1.0F;
         background = 1.0F;
      }

      // 原生 GL 直接写屏幕，之前排队的界面顶点必须先落地，否则会叠成「两帧重合」
      context.draw();
      arc$drawBackground(context, width, height, background);

      int centerX = (int)((float)width * 0.5F);
      int centerY = (int)((float)height * 0.5F);
      double size = Math.min((double)width * 0.75, (double)height) * 0.25;
      int logoHalf = (int)(size * 0.5);
      int logoWidth = (int)(size * 2.5);
      float target = this.reload.getProgress();
      this.progress = MathHelper.clamp(this.progress * 0.95F + target * 0.050000012F, 0.0F, 1.0F);
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      context.setShaderColor(0.4F, 0.4F, 0.4F, alpha);
      context.drawTexture(ARC$LOGO, centerX - logoWidth / 2, centerY - logoHalf, logoWidth, (int)size, 0.0F, 0.0F, 1057, 384, 1057, 384);
      Color accent = arc$accentColor();
      context.setShaderColor((float)accent.getRed() / 255.0F, (float)accent.getGreen() / 255.0F, (float)accent.getBlue() / 255.0F, alpha);
      context.enableScissor(centerX - logoWidth / 2, centerY - logoHalf, (int)((float)(centerX - logoWidth / 2) + (float)logoWidth * this.progress), (int)((float)(centerY - logoHalf) + size));
      context.drawTexture(ARC$LOGO, centerX - logoWidth / 2, centerY - logoHalf, logoWidth, (int)size, 0.0F, 0.0F, 1057, 384, 1057, 384);
      context.disableScissor();
      context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      if (fadeOut < 1.0F) {
         int barY = (int)((float)height * 0.8325F);
         this.renderProgressBar(context, width / 2 - logoWidth, barY - 5, width / 2 + logoWidth, barY + 5, 1.0F - MathHelper.clamp(fadeOut, 0.0F, 1.0F));
      }

      // 进度条下面那一片空白：贴底往上排实时日志，越靠上越淡
      this.arc$drawLog(context, width, height, alpha);

      if (fadeOut >= 2.0F) {
         this.client.setOverlay(null);
      }

      if (this.reloadCompleteTime == -1L && this.reload.isComplete() && (!this.reloading || fadeIn >= 2.0F)) {
         try {
            this.reload.throwException();
            this.exceptionHandler.accept(Optional.empty());
         } catch (Throwable error) {
            this.exceptionHandler.accept(Optional.of(error));
         }

         this.reloadCompleteTime = Util.getMeasuringTimeMs();
         if (this.client.currentScreen != null) {
            this.client.currentScreen.init(this.client, width, height);
         }
      }
   }

   /**
    * 进度条下方的实时日志区。
    *
    * <p>位置：进度条下沿（{@code height * 0.8325 + 5}）再留 11px 缝隙开始，一直排到距离屏幕底 8px。
    * 最下面一行是刚打出来的日志，往上越旧、透明度越低（28 → 200），所以看起来是「从下往上淡出」。
    * 行数由可用高度决定，永远压不进进度条的区域。</p>
    *
    * @param fade 加载完成淡出时的整体透明度，和背景/进度条同一节奏
    */
   @Unique
   private void arc$drawLog(DrawContext context, int width, int height, float fade) {
      TextRenderer font = this.client.textRenderer;
      if (font == null || fade <= 0.01F) {
         return;
      }

      // 做体系统一的字体：Arc 的 UI 字体（assets/arc/fonts/font.ttf，含中文）
      // 在 FontManager.init() 之后就有了，加载界面能用上；万一还没初始化，或者玩家把
      // 「自定义字体」模块关掉了，就照旧用原版 textRenderer —— 不因为字体缺失而整个不画。
      FontRenderer custom = FontManager.ui;
      boolean useCustom = custom != null && (Fonts.INSTANCE == null || Fonts.INSTANCE.isOn());
      float fontHeight = useCustom ? custom.getLineHeight() : (float)font.fontHeight;

      int barY = (int)((float)height * 0.8325F);
      int top = barY + 16;
      int bottom = height - 8;
      int lineHeight = (int)Math.ceil((double)fontHeight) + 2;
      int maxLines = (bottom - top) / lineHeight;
      if (maxLines <= 0) {
         return;
      }

      List<LoadingLog.Line> lines = LoadingLog.snapshot(maxLines);
      int count = lines.size();
      if (count == 0) {
         return;
      }

      int maxWidth = Math.max(80, (int)((float)width * 0.72F));

      for (int i = 0; i < count; ++i) {
         LoadingLog.Line line = lines.get(i);
         int slot = count - 1 - i;
         int y = bottom - lineHeight * (slot + 1);
         if (y < top) {
            continue;
         }

         // i = 0 是最旧的一行（最上），t = 1 是最新的一行（最下）
         float t = count <= 1 ? 1.0F : (float)i / (float)(count - 1);
         int alphaValue = (int)((28.0F + 172.0F * t) * fade);
         if (alphaValue <= 2) {
            continue;
         }

         // log4j 的 intLevel：ERROR=200 / WARN=300 / INFO=400
         int rgb = line.level <= 200 ? 0xFF8080 : (line.level <= 300 ? 0xFFD479 : 0xD0D0D0);
         int color = alphaValue << 24 | rgb;

         if (useCustom) {
            String text = arc$trimToWidth(custom, line.text, maxWidth);
            double x = ((double)width - (double)custom.getWidth(text)) / 2.0;
            custom.drawString(context.getMatrices(), text, x, (double)y, color, true);
         } else {
            String text = font.trimToWidth(line.text, maxWidth);
            int x = (width - font.getWidth(text)) / 2;
            context.drawText(font, text, x, y, color, true);
         }
      }
   }

   /** 按宽度截断（替代 TextRenderer.trimToWidth，自定义字体没有这个方法） */
   @Unique
   private static String arc$trimToWidth(FontRenderer font, String text, int maxWidth) {
      if (font == null || text == null || text.isEmpty() || font.getWidth(text) <= (float)maxWidth) {
         return text;
      }

      String trimmed = text;

      while (trimmed.length() > 1 && font.getWidth(trimmed) > (float)maxWidth) {
         trimmed = trimmed.substring(0, trimmed.length() - 1);
      }

      return trimmed;
   }

   /**
    * 整屏背景：优先用 {@code assets/minecraft/shaders/arc/loading.fsh}（光柱隧道），
    * 着色器不可用（编译失败 / 这帧 GL 出错）时退回原来的纯黑覆盖，保证界面永远画得出来。
    */
   @Unique
   private static void arc$drawBackground(DrawContext context, int width, int height, float alpha) {
      ArcShader shader = ArcShader.get("loading");
      if (shader != null) {
         try {
            // 参数取自 loading.fsh 里 Wallpaper Engine 的默认值，改这里就能换配色
            shader.set("uSpeed", 1.0F);
            shader.set("uTopColor", 0.0F, 0.0F, 1.0F);
            shader.set("uBottomColor", 1.0F, 0.0F, 0.7F);
            shader.set("uIntensity", 1.3F);
            shader.set("uGlowAmount", 0.002F);
            shader.set("uPillarWidth", 2.0F);
            shader.set("uPillarHeight", 0.5F);
            shader.set("uNoiseIntensity", 0.1F);
            shader.set("uPillarRotation", 333.0F);
            shader.set("uOffsetX", 0.0F);
            shader.set("uOffsetY", 0.0F);
            shader.draw(alpha);
            return;
         } catch (Throwable var7) {
         }
      }

      if (alpha > 0.0F) {
         int a = MathHelper.ceil(alpha * 255.0F);
         context.fill(RenderLayer.getGuiOverlay(), 0, 0, width, height, withAlpha(ARC$BACKGROUND, a));
      }
   }

   /** 点亮用的高亮色，跟随 ClickGui 的 FirstColor；splash 阶段配置可能还没读出来，兜底白色 */
   @Unique
   private static Color arc$accentColor() {
      try {
         ClickGui gui = ClickGui.getInstance();
         if (gui != null && gui.color != null) {
            return gui.color.getValue();
         }
      } catch (Throwable var3) {
      }

      return Color.WHITE;
   }
}
