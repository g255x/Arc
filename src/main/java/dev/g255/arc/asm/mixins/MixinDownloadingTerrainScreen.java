package dev.g255.arc.asm.mixins;

import dev.g255.arc.mod.gui.MenuBackground;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 「正在下载地形」界面。
 *
 * <p>它自己覆写了 {@code renderBackground}（按维度贴下界门 / 末地门那张图），
 * 所以 {@link MixinScreen} 的统一接管到不了这里。现在统一换成极光背景，
 * 上面那行居中的提示文字保留（压一层暗色保证可读）。</p>
 */
@Mixin({DownloadingTerrainScreen.class})
public abstract class MixinDownloadingTerrainScreen {
   @Inject(
      method = {"renderBackground(Lnet/minecraft/client/gui/DrawContext;IIF)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void arc$auroraBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      ci.cancel();
      int width = context.getScaledWindowWidth();
      int height = context.getScaledWindowHeight();
      MenuBackground.renderScreen(context, width, height);
      MenuBackground.dim(context, width, height);
   }
}
