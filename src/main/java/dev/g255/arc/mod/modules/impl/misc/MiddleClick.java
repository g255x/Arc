package dev.g255.arc.mod.modules.impl.misc;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.ClientTickEvent;
import dev.g255.arc.mod.modules.Module;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.EntityHitResult;

public class MiddleClick extends Module {
   public MiddleClick() {
      super("MiddleClick", "Performs an action (e.g. friend add) when clicking middle mouse button.", Module.Category.Misc);
      this.setChinese("中键动作");
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!nullCheck()) {
         if (mc.options.pickItemKey.isPressed()) {
            HitResult hit = mc.crosshairTarget;
            if (hit instanceof EntityHitResult) {
               Entity entity = ((EntityHitResult)hit).getEntity();
               if (entity != null) {
                  Arc.FRIEND.friend(entity.getName().getString());
               }

            }
         }
      }
   }
}
