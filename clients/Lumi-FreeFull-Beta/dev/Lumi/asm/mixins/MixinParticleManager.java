package dev.Lumi.asm.mixins;

import dev.Lumi.Lumi;
import dev.Lumi.api.events.impl.ParticleEvent;
import net.minecraft.class_702;
import net.minecraft.class_703;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({class_702.class})
public class MixinParticleManager {
   @Inject(
      at = {@At("HEAD")},
      method = {"addParticle(Lnet/minecraft/client/particle/Particle;)V"},
      cancellable = true
   )
   public void onAddParticle(class_703 particle, CallbackInfo ci) {
      ParticleEvent event = ParticleEvent.get(particle);
      Lumi.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         ci.cancel();
      }

   }
}
