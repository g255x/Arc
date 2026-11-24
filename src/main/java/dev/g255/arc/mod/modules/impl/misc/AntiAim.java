package dev.g255.arc.mod.modules.impl.misc;

import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.RotationEvent;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;

public class AntiAim extends Module {
   private final EnumSetting<Mode> mode;
   private final SliderSetting speed;
   private float angle;

   public AntiAim() {
      super("AntiAim", "Makes other people's aimbots aim at the wrong place.", Module.Category.Misc);
      this.mode = this.add(new EnumSetting("Mode", AntiAim.Mode.Spin));
      this.speed = this.add(new SliderSetting("Speed", (double)10.0F, (double)1.0F, (double)30.0F, (double)0.5F));
      this.angle = 0.0F;
      this.setChinese("反瞄准");
   }

   @EventListener(
      priority = 60
   )
   public void onRotate(RotationEvent event) {
      if (!nullCheck()) {
         switch (((Mode)this.mode.getValue()).ordinal()) {
            case 0:
               this.angle += this.speed.getValueFloat();
               event.setRotation(this.angle, mc.player.getPitch(), 0.0F, 0.0F);
               break;
            case 1:
               event.setRotation(mc.player.getYaw() + 180.0F, mc.player.getPitch(), 0.0F, 0.0F);
               break;
            case 2:
               event.setRotation((float)(Math.random() * (double)360.0F), mc.player.getPitch(), 0.0F, 0.0F);
         }

      }
   }

   public static enum Mode {
      Spin,
      Back,
      Random;

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{Spin, Back, Random};
      }
   }
}
