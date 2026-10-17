package hoprc.asm.mixins;

import dev.kissoo.Forever;
import dev.kissoo.api.utils.Wrapper;
import dev.kissoo.core.impl.CommandManager;
import dev.kissoo.mod.modules.impl.client.ClientSetting;
import net.minecraft.client.Keyboard;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class MixinKeyboard implements Wrapper {
   @Inject(method = "onKey", at = @At("HEAD"))
   private void onKey(long windowPointer, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
      try {
         if (action == 1) {
            if (Forever.MODULE != null) {
               Forever.MODULE.onKeyPressed(key);
            }
         }

         if (action == 0) {
            if (Forever.MODULE != null) {
               Forever.MODULE.onKeyReleased(key);
            }
         }
      } catch (Throwable var9) {
         var9.printStackTrace();
         if (ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage(Formatting.DARK_RED + "[ERROR] onKey " + var9.getMessage());
         }
      }
   }

   @Redirect(method = "onKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/NarratorManager;isActive()Z"), require = 0)
   public boolean hook(NarratorManager instance) {
      return false;
   }
}
