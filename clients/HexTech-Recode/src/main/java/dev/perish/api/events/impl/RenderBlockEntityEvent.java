package dev.perish.api.events.impl;

import dev.perish.api.events.Event;
import net.minecraft.block.entity.BlockEntity;

public class RenderBlockEntityEvent extends Event {
    private static final RenderBlockEntityEvent INSTANCE = new RenderBlockEntityEvent();
    public BlockEntity blockEntity;

    public static RenderBlockEntityEvent get(BlockEntity blockEntity) {
        INSTANCE.setCancelled(false);
        INSTANCE.blockEntity = blockEntity;
        return INSTANCE;
    }
}
