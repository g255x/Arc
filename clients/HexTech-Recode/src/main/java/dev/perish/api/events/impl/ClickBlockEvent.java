package dev.perish.api.events.impl;

import dev.perish.api.events.Event;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class ClickBlockEvent extends Event {
    private static final ClickBlockEvent INSTANCE = new ClickBlockEvent();
    private BlockPos pos;
    private Direction direction;

    private ClickBlockEvent() {
    }

    public static ClickBlockEvent get(BlockPos pos, Direction direction) {
        INSTANCE.pos = pos;
        INSTANCE.direction = direction;
        INSTANCE.setCancelled(false);
        return INSTANCE;
    }

    public Direction getDirection() {
        return this.direction;
    }

    public BlockPos getPos() {
        return this.pos;
    }
}
