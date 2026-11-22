package dev.g255.arc.asm.mixins;

import dev.g255.arc.mod.gui.MenuBackground;
import dev.g255.arc.mod.modules.impl.client.ClickGui;
import dev.g255.arc.mod.modules.impl.client.ClientSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
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

   @Inject(
      method = {"renderBackground"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void renderInGameBackgroundHook(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      ci.cancel();
      // 加载 / 保存 / 建世界 / 断开连接这类界面，绘制时 client.world 往往还没清空，
      // 但世界早就停止渲染了。如果按「游戏内」处理就只剩一层暗化（黑屏），
      // 所以这里连同它们一起走极光，区块进度框、进度条这些原版自己画的内容不受影响
      // （它们是在 render() 里、背景之后画的）。
      if (this.arc$isLoadingScreen() || this.client == null || this.client.world == null) {
         // 所有"非游戏内"界面统一使用极光背景（对应 OpenHachimi 在 Screen.renderBackground 里
         // WrapOperation 掉 renderPanoramaBackground 的做法）。TitleScreen 覆写了 renderBackground
         // 和 renderPanoramaBackground，由 MixinTitleScreen 单独接管；MessageScreen 覆写
         // renderBackground 且不调用 super，另有专用 mixin。
         MenuBackground.renderScreen(context, this.width, this.height);
         return;
      }

      if (ClientSetting.INSTANCE.darkening.getValue() && !(((Object)this) instanceof TitleScreen)) {
         this.renderDarkening(context);
      }

      if (this.client.world != null && ClickGui.getInstance().isBackgroundTintEnabled()) {
         // 颜色由 ClickGui 现算：SyncBackground 打开时取主题色（含 Pulse / Spectrum 的动态偏移），
         // 这样 ESC 菜单和 ClickGUI 的底层背景永远和标题栏、高亮是同一个色系
         context.fillGradient(0, 0, this.width, this.height,
               ClickGui.getInstance().getBackgroundTop(), ClickGui.getInstance().getBackgroundBottom());
      }

   }

   /**
    * 保存世界、加载世界、退出世界、断开连接这几个界面。
    *
    * <p>它们的共同点是：{@code client.world} 还在（或者刚刚才被清空），
    * 但世界渲染已经停了，原版那条「游戏内暗化」分支画出来就是黑的。
    * 这里把它们显式归到菜单背景，从进入流程的第一帧就有极光。</p>
    */
   @Unique
   private boolean arc$isLoadingScreen() {
      Object self = this;
      return self instanceof ProgressScreen
            || self instanceof DisconnectedScreen
            || self instanceof LevelLoadingScreen;
   }

   @Shadow
   protected void renderPanoramaBackground(DrawContext context, float delta) {
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
