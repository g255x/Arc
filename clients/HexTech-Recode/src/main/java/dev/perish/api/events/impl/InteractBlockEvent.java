package dev.perish.api.events.impl;

import dev.perish.api.events.Event;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;

public class InteractBlockEvent extends Event {
    private static final InteractBlockEvent INSTANCE = new InteractBlockEvent();
    public Hand hand;
    public BlockHitResult hitResult;

    private InteractBlockEvent() {
    }

    public static InteractBlockEvent getPre(Hand hand, BlockHitResult hitResult) {
        INSTANCE.hand = hand;
        INSTANCE.hitResult = hitResult;
        INSTANCE.stage = Event.Stage.Pre;
        INSTANCE.setCancelled(false);
        return INSTANCE;
    }

    public static InteractBlockEvent getPost(Hand hand, BlockHitResult hitResult) {
        INSTANCE.hand = hand;
        INSTANCE.hitResult = hitResult;
        INSTANCE.stage = Event.Stage.Post;
        return INSTANCE;
    }
}
