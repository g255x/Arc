package dev.luminous.asm.mixins;

import dev.luminous.Nina;
import dev.luminous.api.events.impl.PlaySoundEvent;
import net.minecraft.class_1113;
import net.minecraft.class_1140;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({class_1140.class})
public class MixinSoundSystem {
   @Inject(
      method = {"play(Lnet/minecraft/client/sound/SoundInstance;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onPlay(class_1113 soundInstance, CallbackInfo info) {
      PlaySoundEvent event = PlaySoundEvent.get(soundInstance);
      Nina.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         info.cancel();
      }

   }
}
