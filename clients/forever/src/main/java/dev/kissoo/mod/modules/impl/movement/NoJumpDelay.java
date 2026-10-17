package dev.kissoo.mod.modules.impl.movement;

import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.UpdateEvent;
import hoprc.asm.accessors.ILivingEntity;
import dev.kissoo.mod.modules.Module;

public class NoJumpDelay extends Module {
   public static NoJumpDelay INSTANCE;

   public NoJumpDelay() {
      super("NoJumpDelay", Module.Category.Movement);
      this.setChinese("无跳跃冷却");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      ((ILivingEntity)mc.player).setLastJumpCooldown(0);
   }
}
