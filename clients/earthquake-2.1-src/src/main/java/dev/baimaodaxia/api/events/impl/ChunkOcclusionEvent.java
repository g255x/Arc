package dev.baimaodaxia.api.events.impl;

import dev.baimaodaxia.api.events.Event;

public class ChunkOcclusionEvent extends Event {
   private static final ChunkOcclusionEvent INSTANCE = new ChunkOcclusionEvent();

   private ChunkOcclusionEvent() {
   }

   public static ChunkOcclusionEvent get() {
      INSTANCE.setCancelled(false);
      return INSTANCE;
   }
}
