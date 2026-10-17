package dev.baimaodaxia.asm.mixins;

import dev.baimaodaxia.Alien;
import dev.baimaodaxia.api.events.impl.ParticleEvent;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ParticleManager.class})
public class MixinParticleManager {
   @Inject(
      at = {@At("HEAD")},
      method = {"addParticle(Lnet/minecraft/client/particle/Particle;)V"},
      cancellable = true
   )
   public void onAddParticle(Particle particle, CallbackInfo ci) {
      ParticleEvent event = ParticleEvent.get(particle);
      Alien.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         ci.cancel();
      }

   }
}
