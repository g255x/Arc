package dev.baimaodaxia.mod.modules.impl.movement;

import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.MoveEvent;
import dev.baimaodaxia.api.utils.path.BaritoneUtil;
import dev.baimaodaxia.api.utils.player.EntityUtil;
import dev.baimaodaxia.api.utils.player.MovementUtil;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import java.util.Objects;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class Strafe extends Module {
   public static Strafe INSTANCE;
   private final BooleanSetting airStop = this.add(new BooleanSetting("AirStop", true));
   private final BooleanSetting slowCheck = this.add(new BooleanSetting("SlowCheck", true));

   public Strafe() {
      super("Strafe", "Modifies sprinting", Module.Category.Movement);
      this.setChinese("灵活移动");
      INSTANCE = this;
   }

   @EventListener
   public void onStrafe(MoveEvent event) {
      if (!BaritoneUtil.isActive()) {
         if (!mc.player.isSneaking() && !Fly.INSTANCE.isOn() && !HoleSnap.INSTANCE.isOn() && !Speed.INSTANCE.isOn() && !mc.player.isFallFlying() && !EntityUtil.isInsideBlock() && !mc.player.isInLava() && !mc.player.isTouchingWater() && !mc.player.getAbilities().flying) {
            if (!MovementUtil.isMoving()) {
               if (this.airStop.getValue()) {
                  MovementUtil.setMotionX((double)0.0F);
                  MovementUtil.setMotionZ((double)0.0F);
               }

            } else {
               double[] dir = MovementUtil.directionSpeed(this.getBaseMoveSpeed());
               event.setX(dir[0]);
               event.setZ(dir[1]);
            }
         }
      }
   }

   public double getBaseMoveSpeed() {
      double n = 0.2873;
      if (mc.player.hasStatusEffect(StatusEffects.SPEED) && (!this.slowCheck.getValue() || !mc.player.hasStatusEffect(StatusEffects.SLOWNESS))) {
         n *= (double)1.0F + 0.2 * (double)(((StatusEffectInstance)Objects.requireNonNull(mc.player.getStatusEffect(StatusEffects.SPEED))).getAmplifier() + 1);
      }

      return n;
   }
}
