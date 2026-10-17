package dev.Lumi.api.events.impl;

import dev.Lumi.api.events.Event;
import net.minecraft.class_437;

public class OpenScreenEvent extends Event {
   private static final OpenScreenEvent INSTANCE = new OpenScreenEvent();
   public class_437 screen;

   private OpenScreenEvent() {
   }

   public static OpenScreenEvent get(class_437 screen) {
      INSTANCE.screen = screen;
      INSTANCE.setCancelled(false);
      return INSTANCE;
   }
}
