package dev.perish.api.events.impl;

import dev.perish.api.events.Event;

public class ClientTickEvent extends Event {
    private static final ClientTickEvent instance = new ClientTickEvent();

    private ClientTickEvent() {
    }

    public static ClientTickEvent get(Event.Stage stage) {
        instance.stage = stage;
        return instance;
    }
}
