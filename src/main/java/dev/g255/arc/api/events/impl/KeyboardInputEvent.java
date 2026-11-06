package dev.g255.arc.api.events.impl;

import dev.g255.arc.api.events.Event;

public class KeyboardInputEvent extends Event {
   private static final KeyboardInputEvent INSTANCE = new KeyboardInputEvent();

   private KeyboardInputEvent() {
   }

   public static KeyboardInputEvent get() {
      INSTANCE.setCancelled(false);
      return INSTANCE;
   }
}
