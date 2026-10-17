package dev.perish.api.events.impl;

import dev.perish.api.events.Event;

public class SprintEvent extends Event {
    private static final SprintEvent instance = new SprintEvent();
    private boolean sprint;

    private SprintEvent() {
    }

    public static SprintEvent get() {
        instance.sprint = false;
        instance.setCancelled(false);
        return instance;
    }

    public boolean isSprint() {
        return this.sprint;
    }

    public void setSprint(boolean sprint) {
        this.sprint = sprint;
    }
}
