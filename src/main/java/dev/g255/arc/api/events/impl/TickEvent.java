/*
 * Decompiled with CFR 0.152.
 */
package dev.g255.arc.api.events.impl;

import dev.g255.arc.api.events.Event;

public class TickEvent
extends Event {
    private static final TickEvent instance = new TickEvent();

    private TickEvent() {
    }

    public static TickEvent get(Event.Stage stage) {
        TickEvent.instance.stage = stage;
        return instance;
    }
}

