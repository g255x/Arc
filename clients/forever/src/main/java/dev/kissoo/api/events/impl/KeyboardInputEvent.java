package dev.kissoo.api.events.impl;

import dev.kissoo.api.events.Event;

public class KeyboardInputEvent extends Event {
   private static final KeyboardInputEvent INSTANCE = new KeyboardInputEvent();

   private KeyboardInputEvent() {
   }

   public static KeyboardInputEvent get() {
      INSTANCE.setCancelled(false);
      return INSTANCE;
   }
}
