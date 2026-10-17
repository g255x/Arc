package dev.perish.api.events.impl;

import dev.perish.api.events.Event;

public class DoAttackEvent extends Event {
    public static final DoAttackEvent INSTANCE = new DoAttackEvent();

    public static DoAttackEvent getPre() {
        INSTANCE.stage = Event.Stage.Pre;
        return INSTANCE;
    }

    public static DoAttackEvent getPost() {
        INSTANCE.stage = Event.Stage.Post;
        return INSTANCE;
    }
}
