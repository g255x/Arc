package dev.baimaodaxia.mod.modules.impl.movement;

import dev.baimaodaxia.Alien;
import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.PacketEvent;
import dev.baimaodaxia.api.events.impl.SprintEvent;
import dev.baimaodaxia.api.events.impl.TickEvent;
import dev.baimaodaxia.api.events.impl.TickMovementEvent;
import dev.baimaodaxia.api.events.impl.UpdateRotateEvent;
import dev.baimaodaxia.api.utils.path.BaritoneUtil;
import dev.baimaodaxia.api.utils.player.MovementUtil;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.impl.client.AntiCheat;
import dev.baimaodaxia.mod.modules.impl.player.Freecam;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.MathHelper;

public class Sprint extends Module {
   public static Sprint INSTANCE;
   public final EnumSetting<Mode> mode;
   public final BooleanSetting inWaterPause;
   public final BooleanSetting inWebPause;
   public final BooleanSetting sneakingPause;
   public final BooleanSetting blindnessPause;
   public final BooleanSetting usingPause;
   public final BooleanSetting lagPause;
   boolean pause;

   public Sprint() {
      super("Sprint", "Permanently keeps player in sprinting mode.", Module.Category.Movement);
      this.mode = this.add(new EnumSetting("Mode", Sprint.Mode.Legit));
      this.inWaterPause = this.add(new BooleanSetting("InWaterPause", true));
      this.inWebPause = this.add(new BooleanSetting("InWebPause", true));
      this.sneakingPause = this.add(new BooleanSetting("SneakingPause", false));
      this.blindnessPause = this.add(new BooleanSetting("BlindnessPause", false));
      this.usingPause = this.add(new BooleanSetting("UsingPause", false));
      this.lagPause = this.add(new BooleanSetting("LagPause", true));
      this.pause = false;
      this.setChinese("强制疾跑");
      INSTANCE = this;
   }

   public static float getSprintYaw(float yaw) {
      if (mc.options.forwardKey.isPressed() && !mc.options.backKey.isPressed()) {
         if (mc.options.leftKey.isPressed() && !mc.options.rightKey.isPressed()) {
            yaw -= 45.0F;
         } else if (mc.options.rightKey.isPressed() && !mc.options.leftKey.isPressed()) {
            yaw += 45.0F;
         }
      } else if (mc.options.backKey.isPressed() && !mc.options.forwardKey.isPressed()) {
         yaw += 180.0F;
         if (mc.options.leftKey.isPressed() && !mc.options.rightKey.isPressed()) {
            yaw += 45.0F;
         } else if (mc.options.rightKey.isPressed() && !mc.options.leftKey.isPressed()) {
            yaw -= 45.0F;
         }
      } else if (mc.options.leftKey.isPressed() && !mc.options.rightKey.isPressed()) {
         yaw -= 90.0F;
      } else if (mc.options.rightKey.isPressed() && !mc.options.leftKey.isPressed()) {
         yaw += 90.0F;
      }

      return MathHelper.wrapDegrees(yaw);
   }

   public String getInfo() {
      return ((Mode)this.mode.getValue()).name();
   }

   @EventListener
   public void onPacket(PacketEvent.Receive event) {
      if (this.lagPause.getValue() && event.getPacket() instanceof PlayerPositionLookS2CPacket) {
         this.pause = true;
      }

   }

   public boolean inWater() {
      return this.inWaterPause.getValue() && mc.player.isInFluid();
   }

   @EventListener
   public void onMove(TickMovementEvent event) {
      if (!BaritoneUtil.isPathing()) {
         if (!this.inWater()) {
            if (this.mode.getValue() == Sprint.Mode.PressKey) {
               mc.options.sprintKey.setPressed(true);
            } else {
               mc.player.setSprinting(this.shouldSprint());
            }

         }
      }
   }

   @EventListener
   public void tick(TickEvent event) {
      if (event.isPost()) {
         this.pause = false;
      }

   }

   @EventListener
   public void sprint(SprintEvent event) {
      if (!BaritoneUtil.isPathing() && !this.mode.is(Sprint.Mode.PressKey)) {
         if (!this.inWater()) {
            event.cancel();
            event.setSprint(this.shouldSprint());
         }
      }
   }

   private boolean shouldSprint() {
      if ((mc.player.getHungerManager().getFoodLevel() > 6 || mc.player.isCreative()) && MovementUtil.isMoving() && !this.pause && (!mc.player.isSneaking() || !this.sneakingPause.getValue()) && (!Alien.PLAYER.isInWeb(mc.player) || !this.inWebPause.getValue()) && (!mc.player.isUsingItem() || !this.usingPause.getValue()) && !mc.player.isRiding() && (!mc.player.hasStatusEffect(StatusEffects.BLINDNESS) || !this.blindnessPause.getValue())) {
         switch (((Mode)this.mode.getValue()).ordinal()) {
            case 1:
               if (AntiCheat.INSTANCE.movementSync()) {
                  return mc.player.input.movementForward > 0.0F;
               }

               return HoleSnap.INSTANCE.isOn() || mc.options.forwardKey.isPressed() && MathHelper.angleBetween(mc.player.getYaw(), Alien.ROTATION.rotationYaw) < 40.0F;
            case 2:
               return true;
            case 3:
               if (AntiCheat.INSTANCE.movementSync()) {
                  return mc.player.input.movementForward > 0.0F;
               }

               return HoleSnap.INSTANCE.isOn() || MathHelper.angleBetween(getSprintYaw(mc.player.getYaw()), Alien.ROTATION.rotationYaw) < 40.0F;
         }
      }

      return false;
   }

   @EventListener(
      priority = -100
   )
   public void rotate(UpdateRotateEvent event) {
      if (!BaritoneUtil.isPathing()) {
         if (((mc.player.getHungerManager().getFoodLevel() > 6 || mc.player.isCreative()) && MovementUtil.isMoving() && !Freecam.INSTANCE.isOn() && !mc.player.isFallFlying() && (!Alien.PLAYER.isInWeb(mc.player) || !this.inWebPause.getValue()) && (!mc.player.isSneaking() || !this.sneakingPause.getValue()) && !mc.player.isRiding() && (!mc.player.isUsingItem() || !this.usingPause.getValue()) && !mc.player.isInFluid() && Freecam.INSTANCE.isOff() && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS) || !this.blindnessPause.getValue()) && this.mode.is(Sprint.Mode.Rotation) && !event.isModified()) {
            event.setYaw(getSprintYaw(mc.player.getYaw()));
         }

      }
   }

   public static enum Mode {
      PressKey,
      Legit,
      Rage,
      Rotation;

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{PressKey, Legit, Rage, Rotation};
      }
   }
}
