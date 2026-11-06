package dev.g255.arc.api.events.impl;

import dev.g255.arc.api.events.Event;

public class ClientTickEvent extends Event {
   private static final ClientTickEvent instance = new ClientTickEvent();

   private ClientTickEvent() {
   }

   public static ClientTickEvent get(Event.Stage stage) {
      instance.stage = stage;
      return instance;
   }
}
