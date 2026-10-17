package dev.perish.mod.modules.impl.movement;

import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.UpdateEvent;
import dev.perish.asm.accessors.ILivingEntity;
import dev.perish.mod.modules.Module;

public class NoJumpDelay extends Module {
    public static NoJumpDelay INSTANCE;

    public NoJumpDelay() {
        super("NoJumpDelay", Module.Category.Movement);
        this.setChinese("无跳跃冷却");
        INSTANCE = this;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        ((ILivingEntity) mc.player).setLastJumpCooldown(0);
    }
}
