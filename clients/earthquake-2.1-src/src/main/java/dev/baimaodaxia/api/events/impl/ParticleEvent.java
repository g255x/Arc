package dev.baimaodaxia.api.events.impl;

import dev.baimaodaxia.api.events.Event;
import net.minecraft.client.particle.Particle;

public class ParticleEvent extends Event {
   private static final ParticleEvent instance = new ParticleEvent();
   public Particle particle;

   public static ParticleEvent get(Particle particle) {
      instance.particle = particle;
      instance.setCancelled(false);
      return instance;
   }
}
