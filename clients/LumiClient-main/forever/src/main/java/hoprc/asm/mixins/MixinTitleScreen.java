package hoprc.asm.mixins;

import dev.kissoo.auth.TokenScreen;
import dev.kissoo.mod.gui.ForeverTitleScreen;
import dev.kissoo.mod.gui.PhobosLoadingScreen;
import dev.kissoo.mod.gui.background.MainMenuModePreference;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class MixinTitleScreen {

   @Inject(method = "init", at = @At("HEAD"), cancellable = true)
   private void replaceTitleScreen(CallbackInfo ci) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (((Object) this) instanceof ForeverTitleScreen) return;
      if (((Object) this) instanceof TokenScreen) return;

      // 认证已注释，直接跳转到主标题界面
      if (MainMenuModePreference.isPhobosSelected() && !PhobosLoadingScreen.isInitialized()) {
         mc.setScreen(new PhobosLoadingScreen());
      } else {
         mc.setScreen(new ForeverTitleScreen());
      }
      ci.cancel();
   }
}