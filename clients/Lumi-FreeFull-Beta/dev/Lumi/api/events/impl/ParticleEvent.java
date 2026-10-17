package dev.Lumi.api.events.impl;

import dev.Lumi.api.events.Event;
import net.minecraft.class_703;

public class ParticleEvent extends Event {
   private static final ParticleEvent instance = new ParticleEvent();
   public class_703 particle;

   public static ParticleEvent get(class_703 particle) {
      instance.particle = particle;
      instance.setCancelled(false);
      return instance;
   }
}
