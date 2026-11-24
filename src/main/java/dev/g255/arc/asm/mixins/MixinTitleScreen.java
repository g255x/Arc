package dev.g255.arc.asm.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.g255.arc.mod.gui.MenuBackground;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.LogoDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 主菜单：<b>整体布局仍然用原版 {@code TitleScreen}</b>，只替换两处视觉元素。
 *
 * <p>1. 背景：{@code renderBackground}（HEAD，cancel）只把鼠标位置喂给极光相机；
 * {@code renderPanoramaBackground}（HEAD，cancel）把原版 3D 全景换成极光。
 * 这一步发生在 logo、按钮之前，所以界面元素仍然画在背景之上。</p>
 *
 * <p>2. logo：把 {@code LogoDrawer.draw} 的调用整体拦掉，换成
 * {@code textures/gui/logo.png}。绘制方式与加载界面（{@code MixinLoadingOverlay}）
 * 完全一致 —— 直接用 {@code context.drawTexture} 贴一张图，不用原版 LogoDrawer 那套
 * 缩放 / 抖动 / 模糊框架，只是把它摆在原版 logo 的位置上。</p>
 *
 * <p>3. 闪烁标语：<b>不碰渲染</b>。原版 {@code SplashTextRenderer} 的倾斜角、缩放、阴影全部保留，
 * 只是在 {@code init()} 结束时把文本换掉 —— 也就是"只改文字"。</p>
 *
 * <p><b>为什么 width/height 不用 {@code @Shadow}：</b>
 * 这两个字段声明在父类 {@link Screen} 上，不是 {@link TitleScreen} 自己的。
 * {@code @Shadow} 只解析目标类自身声明的成员，写在子类 mixin 里会在运行期炸
 * "was not located in the target class"，所以走父类视图 {@link #arc$screen()}。</p>
 */
@Mixin({TitleScreen.class})
public class MixinTitleScreen {
   @Unique
   private static final Identifier ARC$LOGO = Identifier.tryParse("arc", "textures/gui/logo.png");
   @Unique
   private static final String ARC$SPLASH = "[MIT] Made by G255";
   /** 左下角版本行：Arc Development b1 [MIT] */
   @Unique
   private static final String ARC$VERSION_TEXT = "Arc " + dev.g255.arc.Arc.VERSION + " [MIT]";
   @Unique
   private static final int ARC$VERSION_COLOR = 0xFFFFFFFF;
   @Unique
   private static final int ARC$VERSION_HOVER_COLOR = 0xFFFFFFFF;
   /** 版本行顶边相对屏幕底边的偏移：原版版本号在 height-10，这行压在它上面 */
   @Unique
   private static final int ARC$VERSION_BOTTOM_OFFSET = 22;
   /** logo.png 的原始尺寸，drawTexture 的 UV 必须给全图，否则只会画左上角一块 */
   @Unique
   private static final int ARC$LOGO_TEX_WIDTH = 1057;
   @Unique
   private static final int ARC$LOGO_TEX_HEIGHT = 384;
   /** 宽高比 384 / 1057 ≈ 0.3633，缩放时只算宽度，高度一律由它推出来，避免拉变形 */
   @Unique
   private static final float ARC$LOGO_ASPECT = (float)ARC$LOGO_TEX_HEIGHT / (float)ARC$LOGO_TEX_WIDTH;
   /** 占屏幕逻辑宽度的比例：与原版 256 宽 logo 在常见窗口下的观感接近 */
   @Unique
   private static final float ARC$LOGO_WIDTH_RATIO = 0.29F;
   /** 缩放的上下限：窗口再小也别小于 150（否则看不清），再大也别超过 280（否则压到按钮） */
   @Unique
   private static final float ARC$LOGO_MIN_WIDTH = 150.0F;
   @Unique
   private static final float ARC$LOGO_MAX_WIDTH = 280.0F;

   /** 这个是 TitleScreen 自己声明的字段，可以正常 shadow。 */
   @Shadow
   private SplashTextRenderer splashText;

   /** 父类 Screen 的 public 字段，编译期需要显式取父类视图。 */
   @Unique
   private Screen arc$screen() {
      return (Screen)(Object)this;
   }

   /** 对应 Hachimi: hookBackground → TextDrawScreen.ey(mouseX, mouseY, width, height) */
   @Inject(
      method = {"renderBackground"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void arc$trackMenuMouse(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      ci.cancel();
      Screen screen = this.arc$screen();
      MenuBackground.updateMouse((double)mouseX, (double)mouseY, screen.width, screen.height);
   }

   /** 对应 Hachimi: hookBackground → TextDrawScreen.ez(context, width, height) */
   @Inject(
      method = {"renderPanoramaBackground"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void arc$renderMenuBackground(DrawContext context, float delta, CallbackInfo ci) {
      ci.cancel();
      Screen screen = this.arc$screen();
      MenuBackground.renderScreen(context, screen.width, screen.height);
   }

   /**
    * 闪烁标语只改文字：{@code init()} 末尾把原版随机挑出来的那条换成固定文案，
    * 渲染仍然交给原版 {@link SplashTextRenderer}（角度、缩放、颜色全不动）。
    */
   @Inject(
      method = {"init"},
      at = {@At("TAIL")}
   )
   private void arc$replaceSplashText(CallbackInfo ci) {
      this.splashText = new SplashTextRenderer(ARC$SPLASH);
   }

   /** 版本行顶边 y（屏幕坐标） */
   @Unique
   private int arc$versionY() {
      Screen screen = this.arc$screen();
      return screen.height - ARC$VERSION_BOTTOM_OFFSET;
   }

   @Unique
   private int arc$versionWidth() {
      net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
      return client == null ? 0 : client.textRenderer.getWidth(ARC$VERSION_TEXT);
   }

   @Unique
   private boolean arc$isOverVersion(int mouseX, int mouseY) {
      net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
      if (client == null) {
         return false;
      }
      int x = 2;
      int y = this.arc$versionY();
      int w = this.arc$versionWidth();
      int h = client.textRenderer.fontHeight;
      return mouseX >= x && mouseX <= x + w && mouseY >= y - 1 && mouseY <= y + h + 1;
   }

   /**
    * 左下角的 {@code Arc <version> [MIT]} 超链接。
    *
    * <p>画在 {@code render} 的 TAIL：原版这一步已经写完版本号、版权行和 Mojang 的 logo，
    * 我们在它上方补一行，位置按屏幕高度算，不参与原版布局，所以不会把按钮挤歪。
    */
   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void arc$drawVersionLine(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
      if (client == null) {
         return;
      }
      int x = 2;
      int y = this.arc$versionY();
      boolean hovered = this.arc$isOverVersion(mouseX, mouseY);
      int color = hovered ? ARC$VERSION_HOVER_COLOR : ARC$VERSION_COLOR;
      context.drawTextWithShadow(client.textRenderer, ARC$VERSION_TEXT, x, y, color);
      if (hovered) {
         int w = this.arc$versionWidth();
         context.fill(x, y + client.textRenderer.fontHeight, x + w,
                 y + client.textRenderer.fontHeight + 1, ARC$VERSION_HOVER_COLOR);
      }
   }

   /** 点版本行进鸣谢页；只吃左键，其它点击照常交给原版 */
   @Inject(
      method = {"mouseClicked"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void arc$clickVersionLine(double mouseX, double mouseY, int button, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
      if (button == 0 && this.arc$isOverVersion((int)mouseX, (int)mouseY)) {
         net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
         if (client != null) {
            client.setScreen(new dev.g255.arc.mod.gui.ThanksScreen(this.arc$screen()));
         }
         cir.setReturnValue(true);
      }
   }

   /**
    * 原版：{@code this.logoDrawer.draw(context, this.width, alpha)}。
    *
    * <p>绘制方式沿用 Hachimi 的 {@code TextDrawScreen}：不碰 {@code LogoDrawer} 内部的
    * 缩放 / 亮度动画，直接 {@code drawTexture} 贴一张图，再用 {@code setShaderColor(1,1,1,alpha)}
    * 做淡入。区别是尺寸不再写死 150×54 —— 原版 logo 是固定 256 宽、随窗口占比变化的，
    * 这里同样按屏幕逻辑宽度取固定比例（约 29%），并限制在 150~280 之间，宽高比由贴图原尺寸锁定。</p>
    *
    * <p>位置水平居中、顶边压在原版 logo 的位置 {@code LOGO_BASE_Y}，放大时向下生长，
    * 这样按钮、闪烁标语的排版完全不用动。</p>
    *
    * <p><b>必须先 enableBlend：</b>{@code renderPanoramaBackground} 里的极光是
    * Satin 后处理，跑完后 GL 的混合开关不一定还是开着的。混合一旦是关的，
    * 贴图里全透明的那一圈就会被原样写进帧缓冲 —— 也就是「logo 周围一圈黑边」。</p>
    */
   @Redirect(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/LogoDrawer;draw(Lnet/minecraft/client/gui/DrawContext;IF)V"
      )
   )
   private void arc$drawClientLogo(LogoDrawer drawer, DrawContext context, int screenWidth, float alpha) {
      // 与原版一样「跟着界面走」：宽度取屏幕逻辑宽度的固定比例，窗口拉大 logo 就变大，
      // 窗口缩小就跟着收，两端各留一个上下限防止看不清 / 压到下面的按钮。
      int logoWidth = Math.round(Math.min(Math.max((float)screenWidth * ARC$LOGO_WIDTH_RATIO, ARC$LOGO_MIN_WIDTH), ARC$LOGO_MAX_WIDTH));
      int logoHeight = Math.round((float)logoWidth * ARC$LOGO_ASPECT);
      int x = Math.round((float)screenWidth / 2.0F - (float)logoWidth / 2.0F);
      // 顶边锁在原版 logo 的位置（LOGO_BASE_Y = 30），放大时向下生长。
      // 不能再用「在 44 高的原版区域里居中」——logo 一超过 44 高，居中算出来的 y 会跑到 30 以上，
      // 窗口越大越往上顶，最后直接顶出屏幕。
      int y = LogoDrawer.LOGO_BASE_Y;
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      context.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
      context.drawTexture(ARC$LOGO, x, y, logoWidth, logoHeight, 0.0F, 0.0F, ARC$LOGO_TEX_WIDTH, ARC$LOGO_TEX_HEIGHT, ARC$LOGO_TEX_WIDTH, ARC$LOGO_TEX_HEIGHT);
      context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.disableBlend();
   }
}
