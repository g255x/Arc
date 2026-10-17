/*
 * Decompiled with CFR 0.152.
 */
package dev.g255.arc.api.events.impl;

import dev.g255.arc.api.events.Event;

public class SendMessageEvent
extends Event {
    private static final SendMessageEvent INSTANCE = new SendMessageEvent();
    public String defaultMessage;
    public String message;

    private SendMessageEvent() {
    }

    public static SendMessageEvent get(String message) {
        SendMessageEvent.INSTANCE.defaultMessage = message;
        SendMessageEvent.INSTANCE.message = message;
        INSTANCE.setCancelled(false);
        return INSTANCE;
    }
}

