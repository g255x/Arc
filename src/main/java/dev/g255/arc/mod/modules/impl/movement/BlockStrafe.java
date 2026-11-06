package dev.g255.arc.mod.modules.impl.movement;

import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.MoveEvent;
import dev.g255.arc.api.utils.player.EntityUtil;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.impl.combat.AutoAnchor;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;

public class BlockStrafe extends Module {
   public static BlockStrafe INSTANCE;
   private final SliderSetting speed = this.add((new SliderSetting("Speed", (double)10.0F, (double)0.0F, (double)20.0F, (double)1.0F)).setSuffix("%"));
   private final SliderSetting aSpeed = this.add((new SliderSetting("AnchorSpeed", (double)3.0F, (double)0.0F, (double)20.0F, (double)1.0F)).setSuffix("%"));

   public BlockStrafe() {
      super("BlockStrafe", Module.Category.Movement);
      this.setChinese("方块灵活移动");
      INSTANCE = this;
   }

   @EventListener
   public void onMove(MoveEvent event) {
      if (EntityUtil.isInsideBlock()) {
         double speed = AutoAnchor.INSTANCE.currentPos == null ? this.speed.getValue() : this.aSpeed.getValue();
         double moveSpeed = 0.002873 * speed;
         double n = (double)mc.player.input.movementForward;
         double n2 = (double)mc.player.input.movementSideways;
         double n3 = (double)mc.player.getYaw();
         if (n == (double)0.0F && n2 == (double)0.0F) {
            event.setX((double)0.0F);
            event.setZ((double)0.0F);
         } else {
            if (n != (double)0.0F && n2 != (double)0.0F) {
               n *= Math.sin((Math.PI / 4D));
               n2 *= Math.cos((Math.PI / 4D));
            }

            event.setX(n * moveSpeed * -Math.sin(Math.toRadians(n3)) + n2 * moveSpeed * Math.cos(Math.toRadians(n3)));
            event.setZ(n * moveSpeed * Math.cos(Math.toRadians(n3)) - n2 * moveSpeed * -Math.sin(Math.toRadians(n3)));
         }
      }
   }
}
