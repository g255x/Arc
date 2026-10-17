package dev.perish.api.events.impl;

import dev.perish.api.events.Event;

public class S2CCloseScreenEvent extends Event {
    private static final S2CCloseScreenEvent INSTANCE = new S2CCloseScreenEvent();

    public static S2CCloseScreenEvent get() {
        INSTANCE.setCancelled(false);
        return INSTANCE;
    }
}
