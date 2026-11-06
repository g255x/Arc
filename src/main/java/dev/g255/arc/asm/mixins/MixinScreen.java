package dev.g255.arc.asm.mixins;

import dev.g255.arc.mod.gui.MenuBackground;
import dev.g255.arc.mod.modules.impl.client.ClickGui;
import dev.g255.arc.mod.modules.impl.client.ClientSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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
      if (this.client.world == null) {
         // 所有"非游戏内"界面统一使用极光背景（对应 OpenHachimi 在 Screen.renderBackground 里
         // WrapOperation 掉 renderPanoramaBackground 的做法）。TitleScreen 覆写了 renderBackground
         // 和 renderPanoramaBackground，由 MixinTitleScreen 单独接管；MessageScreen 与
         // DownloadingTerrainScreen 覆写 renderBackground 且不调用 super，也各有专用 mixin。
         MenuBackground.renderScreen(context, this.width, this.height);
         return;
      }

      if (ClientSetting.INSTANCE.darkening.getValue() && !(((Object)this) instanceof TitleScreen)) {
         this.renderDarkening(context);
      }

      if (this.client.world != null && ClickGui.getInstance().tint.booleanValue) {
         context.fillGradient(0, 0, this.width, this.height, ClickGui.getInstance().tint.getValue().getRGB(), ClickGui.getInstance().endColor.getValue().getRGB());
      }

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
