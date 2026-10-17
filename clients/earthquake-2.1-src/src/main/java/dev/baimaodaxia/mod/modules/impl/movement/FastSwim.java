package dev.baimaodaxia.mod.modules.impl.movement;

import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.KeyboardInputEvent;
import dev.baimaodaxia.api.events.impl.MoveEvent;
import dev.baimaodaxia.api.utils.player.MovementUtil;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;

public class FastSwim extends Module {
   public static FastSwim INSTANCE;
   public final SliderSetting speed = this.add(new SliderSetting("Speed", 0.2, (double)0.0F, (double)1.0F, 0.01));
   public final SliderSetting downFactor = this.add(new SliderSetting("DownFactor", (double)0.0F, (double)0.0F, (double)1.0F, 1.0E-6));
   private final SliderSetting sneakDownSpeed = this.add(new SliderSetting("DownSpeed", 0.2, (double)0.0F, (double)1.0F, 0.01));
   private final SliderSetting upSpeed = this.add(new SliderSetting("UpSpeed", 0.2, (double)0.0F, (double)1.0F, 0.01));
   private MoveEvent event;

   public FastSwim() {
      super("FastSwim", Module.Category.Movement);
      this.setChinese("快速游泳");
      INSTANCE = this;
   }

   @EventListener
   public void onKeyboardInput(KeyboardInputEvent event) {
      if (mc.player.isInFluid()) {
         mc.player.input.sneaking = false;
      }

   }

   @EventListener
   public void onMove(MoveEvent event) {
      if (!nullCheck()) {
         if (mc.player.isInFluid()) {
            this.event = event;
            if (mc.options.sneakKey.isPressed() && mc.player.input.jumping) {
               this.setY((double)0.0F);
            } else if (mc.options.sneakKey.isPressed()) {
               this.setY(-this.sneakDownSpeed.getValue());
            } else if (mc.player.input.jumping) {
               this.setY(this.upSpeed.getValue());
            } else {
               this.setY(-this.downFactor.getValue());
            }

            double[] dir = MovementUtil.directionSpeed(this.speed.getValue());
            this.setX(dir[0]);
            this.setZ(dir[1]);
         }

      }
   }

   private void setX(double f) {
      this.event.setX(f);
      MovementUtil.setMotionX(f);
   }

   private void setY(double f) {
      this.event.setY(f);
      MovementUtil.setMotionY(f);
   }

   private void setZ(double f) {
      this.event.setZ(f);
      MovementUtil.setMotionZ(f);
   }
}
