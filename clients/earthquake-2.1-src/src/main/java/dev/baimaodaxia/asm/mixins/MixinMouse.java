package dev.baimaodaxia.asm.mixins;

import dev.baimaodaxia.earthquake;
import dev.baimaodaxia.api.interfaces.IMouseHook;
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

@Mixin({Mouse.class})
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

   @Inject(
      method = {"onMouseButton"},
      at = {@At("HEAD")}
   )
   private void onMouse(long window, int button, int action, int mods, CallbackInfo ci) {
      int key = -(button + 2);
      if (action == 1) {
         earthquake.MODULE.onKeyPressed(key);
      }

      if (action == 0) {
         earthquake.MODULE.onKeyReleased(key);
      }

   }

   public void alienClient$lock() {
      if (this.client.isWindowFocused() && !this.cursorLocked) {
         if (!MinecraftClient.IS_SYSTEM_MAC) {
            KeyBinding.updatePressedStates();
         }

         this.cursorLocked = true;
         this.x = (double)this.client.getWindow().getWidth() / (double)2.0F;
         this.y = (double)this.client.getWindow().getHeight() / (double)2.0F;
         InputUtil.setCursorParameters(this.client.getWindow().getHandle(), 212995, this.x, this.y);
      }

   }
}
