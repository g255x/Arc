package dev.perish.asm.mixins;

import dev.perish.Perish;
import dev.perish.api.interfaces.IMouseHook;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MixinMouse implements IMouseHook {
    @Shadow
    private boolean cursorLocked;
    @Final
    @Shadow
    private MinecraftClient client;
    @Shadow
    private double x;
    @Shadow
    private double y;

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void onMouse(long window, int button, int action, int mods, CallbackInfo ci) {
        if (Perish.MODULE == null) {
            return;
        }
        int key = -(button + 2);
        try {
            if (action == 1) {
                Perish.MODULE.onKeyPressed(key);
            }

            if (action == 0) {
                Perish.MODULE.onKeyReleased(key);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void alienClient$lock() {
        if (this.client.isWindowFocused() && !this.cursorLocked) {
            if (!MinecraftClient.IS_SYSTEM_MAC) {
                KeyBinding.updatePressedStates();
            }

            this.cursorLocked = true;
            this.x = this.client.getWindow().getWidth() / 2.0;
            this.y = this.client.getWindow().getHeight() / 2.0;
            InputUtil.setCursorParameters(this.client.getWindow().getHandle(), 212995, this.x, this.y);
        }
    }
}
