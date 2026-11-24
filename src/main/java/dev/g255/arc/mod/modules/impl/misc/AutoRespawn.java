package dev.g255.arc.mod.modules.impl.misc;

import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.mod.modules.Module;
import net.minecraft.client.gui.screen.DeathScreen;

public class AutoRespawn extends Module {
   private final Timer timer = new Timer();

   public AutoRespawn() {
      super("AutoRespawn", Module.Category.Misc);
      this.setChinese("自动重生");
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!nullCheck()) {
         if (mc.currentScreen instanceof DeathScreen && mc.player != null && this.timer.passed(150L)) {
            mc.player.requestRespawn();
            this.timer.reset();
         }

      }
   }
}
