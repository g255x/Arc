package dev.g255.arc.mod.modules.impl.misc;

import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.DeathEvent;
import dev.g255.arc.api.events.impl.GameLeftEvent;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.core.impl.CommandManager;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CoordLogger extends Module {
   private final BooleanSetting log = this.add(new BooleanSetting("Log", true));
   private final BooleanSetting death = this.add(new BooleanSetting("Death", true));

   public CoordLogger() {
      super("CoordLogger", "Copies your coordinates whenever you log/die.", Module.Category.Misc);
      this.setChinese("坐标记录");
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!nullCheck()) {
         ;
      }
   }

   @EventListener
   public void onDeath(DeathEvent event) {
      if (!nullCheck() && this.death.getValue()) {
         this.copyCoordinates();
      }
   }

   @EventListener
   public void onLeave(GameLeftEvent event) {
      if (!nullCheck() && this.log.getValue()) {
         this.copyCoordinates();
      }
   }

   private void copyCoordinates() {
      SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm");
      int var10000 = mc.player.getBlockX();
      String text = "X: " + var10000 + ", Y: " + mc.player.getBlockY() + ", Z: " + mc.player.getBlockZ() + " [" + sdf.format(new Date()) + "]";
      mc.keyboard.setClipboard(text);
      CommandManager.sendMessage("Copied coordinates: " + text);
   }
}
