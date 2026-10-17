package dev.baimaodaxia.mod.modules.impl.misc;

import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.DeathEvent;
import dev.baimaodaxia.api.events.impl.UpdateEvent;
import dev.baimaodaxia.api.utils.math.Timer;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.StringSetting;

public class AutoKit extends Module {
   public static AutoKit INSTANCE;
   final StringSetting command = this.add(new StringSetting("Command", "kit 1"));
   boolean kit = false;
   final Timer timer = new Timer();

   public AutoKit() {
      super("AutoKit", Module.Category.Misc);
      this.setChinese("自动配装命令");
      INSTANCE = this;
   }

   public void onLogin() {
      this.kit = true;
      this.timer.reset();
   }

   @EventListener
   public void onDeath(DeathEvent event) {
      if (event.getPlayer() == mc.player) {
         this.kit = true;
         this.timer.reset();
      }

   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.kit && this.timer.passedS((double)2.0F)) {
         this.kit = false;
         mc.player.networkHandler.sendCommand(this.command.getValue());
      }

   }
}
