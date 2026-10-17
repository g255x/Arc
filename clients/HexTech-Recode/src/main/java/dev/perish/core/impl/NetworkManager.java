package dev.perish.core.impl;

import dev.perish.Perish;
import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.PacketEvent;
import dev.perish.api.utils.Wrapper;

import java.util.LinkedList;

public class NetworkManager implements Wrapper {

    private final PerSecondCounter outgoingCounter = new PerSecondCounter();
    private final PerSecondCounter incomingCounter = new PerSecondCounter();

    public NetworkManager() {
        Perish.EVENT_BUS.subscribe(this);
    }

    @EventListener
    public void onPacketSend(PacketEvent.Send event) {
        outgoingCounter.updateCounter();
    }

    @EventListener
    public void onPacketReceive(PacketEvent.Receive event) {
        incomingCounter.updateCounter();
    }

    public int getOutgoingPPS() {
        return outgoingCounter.getPerSecond();
    }

    public int getIncomingPPS() {
        return incomingCounter.getPerSecond();
    }

    public static class PerSecondCounter {
        private final LinkedList<Long> counter = new LinkedList<>();

        public void updateCounter() {
            counter.add(System.currentTimeMillis() + 1000L);
        }

        public int getPerSecond() {
            long time = System.currentTimeMillis();
            try {
                while (!counter.isEmpty() && counter.peek() != null && counter.peek() < time) {
                    counter.remove();
                }
            } catch (Exception e) {
                counter.clear();
                e.printStackTrace();
            }
            return counter.size();
        }
    }
}