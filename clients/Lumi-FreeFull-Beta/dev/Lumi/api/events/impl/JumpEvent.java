package dev.Lumi.api.events.impl;

import dev.Lumi.api.events.Event;

public class JumpEvent extends Event {
   private static final JumpEvent instance = new JumpEvent();

   private JumpEvent() {
   }

   public static JumpEvent get(Event.Stage stage) {
      instance.stage = stage;
      return instance;
   }
}
