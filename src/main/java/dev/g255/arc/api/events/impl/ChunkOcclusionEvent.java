/*
 * Decompiled with CFR 0.152.
 */
package dev.g255.arc.api.events.impl;

import dev.g255.arc.api.events.Event;

public class ChunkOcclusionEvent
extends Event {
    private static final ChunkOcclusionEvent INSTANCE = new ChunkOcclusionEvent();

    private ChunkOcclusionEvent() {
    }

    public static ChunkOcclusionEvent get() {
        INSTANCE.setCancelled(false);
        return INSTANCE;
    }
}

