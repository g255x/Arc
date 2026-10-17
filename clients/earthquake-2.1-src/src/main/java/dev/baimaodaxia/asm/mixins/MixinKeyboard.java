package dev.baimaodaxia.asm.mixins;

import dev.baimaodaxia.Alien;
import dev.baimaodaxia.api.utils.Wrapper;
import dev.baimaodaxia.core.impl.CommandManager;
import dev.baimaodaxia.mod.modules.impl.client.ClientSetting;
import net.minecraft.util.Formatting;
import net.minecraft.client.Keyboard;
import net.minecraft.client.util.NarratorManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin({Keyboard.class})
public class MixinKeyboard implements Wrapper {
   @Inject(
      method = {"onKey"},
      at = {@At("HEAD")}
   )
   private void onKey(long windowPointer, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
      try {
         if (action == 1) {
            Alien.MODULE.onKeyPressed(key);
         }

         if (action == 0) {
            Alien.MODULE.onKeyReleased(key);
         }
      } catch (Exception e) {
         e.printStackTrace();
         if (ClientSetting.INSTANCE.debug.getValue()) {
            String var10000 = String.valueOf(Formatting.DARK_RED);
            CommandManager.sendMessage(var10000 + "[ERROR] onKey " + e.getMessage());
         }
      }

   }

   @Redirect(
      method = {"onKey"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/util/NarratorManager;isActive()Z"
),
      require = 0
   )
   public boolean hook(NarratorManager instance) {
      return false;
   }
}
