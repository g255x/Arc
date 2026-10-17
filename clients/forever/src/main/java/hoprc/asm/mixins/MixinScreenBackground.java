package hoprc.asm.mixins;

import dev.kissoo.mod.gui.background.ShaderBackground;
import dev.kissoo.mod.gui.background.VideoBackground;
import dev.kissoo.mod.gui.background.PhobosBackground;
import dev.kissoo.mod.modules.impl.client.ClientSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class MixinScreenBackground {

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void lyrae$replaceBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (MinecraftClient.getInstance().world != null) {
            return;
        }
        Screen self = (Screen) (Object) this;
        if (self instanceof ProgressScreen || self instanceof DownloadingTerrainScreen || self instanceof ConnectScreen) {
            return;
        }
        ClientSetting.MainMenuBg mode = ClientSetting.INSTANCE == null
                ? ClientSetting.MainMenuBg.SHADER
                : ClientSetting.INSTANCE.mainMenuBg.getValue();
        boolean isVideoMode = mode == ClientSetting.MainMenuBg.VIDEO;

        if (isVideoMode) {
            VideoBackground bg = VideoBackground.getInstance();
            if (bg.isFailed()) {
                return; // 加载失败，走原版背景
            }
            if (!bg.isInitialized() && !bg.isLoading()) {
                bg.initAsync("/assets/lyrae/videos/mainmenu.mp4", "/assets/lyrae/mainmenu.mp4");
            }
            int width = context.getScaledWindowWidth();
            int height = context.getScaledWindowHeight();
            bg.render(context, width, height);
            if (!bg.isFailed()) {
                ci.cancel();
            }
        } else if (mode == ClientSetting.MainMenuBg.PHOBOS) {
            PhobosBackground phobos = PhobosBackground.getInstance();
            phobos.renderMenu(context);
            if (!phobos.isFailed()) {
                ci.cancel();
                return;
            }
            ShaderBackground.getInstance().init();
            ShaderBackground.getInstance().render();
            ci.cancel();
        } else {
            ShaderBackground.getInstance().init();
            ShaderBackground.getInstance().render();
            ci.cancel();
        }
    }
}
