package hoprc.asm.mixins;

import dev.kissoo.mod.modules.impl.hud.CircularStatusHud;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class MixinInGameHudCrosshair {

    // 拦截准星渲染
    // 忽略除 CallbackInfo 之外的其他参数，这样不管你在哪个版本，都不会报错参数不匹配
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void onRenderCrosshair(CallbackInfo ci) {
        // 如果 CircularStatusHud 开启，并且开启了"隐藏原版准星"的选项，则取消原版渲染
        if (CircularStatusHud.shouldHideVanillaCrosshair()) {
            ci.cancel();
        }
    }
}