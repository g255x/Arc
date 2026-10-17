package dev.baimaodaxia.mod.modules.impl.movement;

import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.MoveEvent;
import dev.baimaodaxia.api.events.impl.UpdateEvent;
import dev.baimaodaxia.api.utils.math.Timer;
import dev.baimaodaxia.api.utils.player.EntityUtil;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.impl.combat.AutoAnchor;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Vec3d;

public class BlockStrafe extends Module {
   public static BlockStrafe INSTANCE;
   private final SliderSetting speed = this.add((new SliderSetting("Speed", (double)10.0F, (double)0.0F, (double)20.0F, (double)1.0F)).setSuffix("%"));
   private final SliderSetting aSpeed = this.add((new SliderSetting("AnchorSpeed", (double)3.0F, (double)0.0F, (double)20.0F, (double)1.0F)).setSuffix("%"));
   private final BooleanSetting bypassSlowFalling = this.add(new BooleanSetting("BypassSlowFalling", false));
   private final SliderSetting motionY = this.add(new SliderSetting("MotionY", 0.01, (double)0.0F, (double)1.0F, 0.01));
   private final SliderSetting jumpDelay = this.add((new SliderSetting("JumpDelay", (double)0.0F, (double)0.0F, (double)500.0F, (double)1.0F)).setSuffix("ms"));
   private final Timer jumpTimer = new Timer();

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

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.bypassSlowFalling.getValue()) {
         if (this.jumpTimer.passedMs(this.jumpDelay.getValue())) {
            if (mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
               if (!mc.player.isSpectator()) {
                  if (EntityUtil.isInsideBlock()) {
                     if (mc.player.isOnGround() && mc.player.fallDistance <= 0.0F) {
                        Vec3d velocity = mc.player.getVelocity();
                        mc.player.setVelocity(velocity.x, this.motionY.getValue(), velocity.z);
                        this.jumpTimer.reset();
                     }

                  }
               }
            }
         }
      }
   }
}
