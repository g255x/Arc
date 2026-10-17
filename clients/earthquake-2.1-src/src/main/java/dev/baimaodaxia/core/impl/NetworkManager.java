package dev.baimaodaxia.core.impl;

import dev.baimaodaxia.earthquake;
import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.PacketEvent;
import dev.baimaodaxia.api.utils.Wrapper;
import java.util.LinkedList;
public class NetworkManager implements Wrapper {
   private final PerSecondCounter outgoingCounter = new PerSecondCounter();
   private final PerSecondCounter incomingCounter = new PerSecondCounter();

   public NetworkManager() {
      earthquake.EVENT_BUS.subscribe(this);
   }

   @EventListener
   public void onPacketSend(PacketEvent.Send event) {
      this.outgoingCounter.updateCounter();
   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive event) {
      this.incomingCounter.updateCounter();
   }

   public int getOutgoingPPS() {
      return this.outgoingCounter.getPerSecond();
   }

   public int getIncomingPPS() {
      return this.incomingCounter.getPerSecond();
   }

   public static class PerSecondCounter {
      private final LinkedList<Long> counter = new LinkedList();

      public void updateCounter() {
         this.counter.add(System.currentTimeMillis() + 1000L);
      }

      public int getPerSecond() {
         long time = System.currentTimeMillis();

         while(!this.counter.isEmpty() && this.counter.peek() != null && (Long)this.counter.peek() < time) {
            try {
               this.counter.remove();
            } catch (Exception e) {
               this.counter.clear();
               e.printStackTrace();
            }
         }

         return this.counter.size();
      }
   }
}
