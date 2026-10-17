package dev.Lumi.api.events.impl;

import dev.Lumi.api.events.Event;

public class ChunkOcclusionEvent extends Event {
   private static final ChunkOcclusionEvent INSTANCE = new ChunkOcclusionEvent();

   private ChunkOcclusionEvent() {
   }

   public static ChunkOcclusionEvent get() {
      INSTANCE.setCancelled(false);
      return INSTANCE;
   }
}
