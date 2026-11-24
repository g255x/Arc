package dev.g255.arc.mod.modules.impl.movement;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.ClientTickEvent;
import dev.g255.arc.api.events.impl.SendMovementPacketsEvent;
import dev.g255.arc.api.events.impl.UpdateRotateEvent;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import net.minecraft.util.math.MathHelper;

public class Spin extends Module {
   public static Spin INSTANCE;
   private final SliderSetting speed = this.add(new SliderSetting("Speed", (double)30.0F, (double)1.0F, (double)180.0F, (double)1.0F));
   private final EnumSetting<Direction> direction;
   private final SliderSetting pitchJitter;
   private float spinYaw;
   private int tickCount;

   public Spin() {
      super("Spin", "Spins your character wildly (visible to others), first person unaffected.", Module.Category.Movement);
      this.direction = this.add(new EnumSetting("Direction", Spin.Direction.Right));
      this.pitchJitter = this.add(new SliderSetting("PitchJitter", (double)0.0F, (double)0.0F, (double)30.0F, (double)0.5F));
      this.spinYaw = 0.0F;
      this.tickCount = 0;
      this.setChinese("疯狂旋转");
      INSTANCE = this;
   }

   public void onEnable() {
      if (!nullCheck()) {
         this.spinYaw = mc.player.getYaw();
      }

      this.tickCount = 0;
   }

   public void onDisable() {
      if (!nullCheck()) {
         Arc.ROTATION.snapAt(mc.player.getYaw(), mc.player.getPitch());
      }

   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!nullCheck()) {
         ++this.tickCount;
         float step = this.speed.getValueFloat();
         if (this.direction.is(Spin.Direction.Left)) {
            step = -step;
         }

         this.spinYaw = MathHelper.wrapDegrees(this.spinYaw + step);
      }
   }

   @EventListener(
      priority = 500
   )
   public void onSendMovement(SendMovementPacketsEvent event) {
      if (!nullCheck() && this.isOn()) {
         float pitch = mc.player.getPitch();
         float jitter = this.pitchJitter.getValueFloat();
         if (jitter > 0.0F) {
            pitch = MathHelper.clamp(pitch + MathHelper.sin((float)this.tickCount * 0.7F) * jitter, -90.0F, 90.0F);
         }

         event.setYaw(this.spinYaw);
         event.setPitch(pitch);
         Arc.ROTATION.rotationYaw = this.spinYaw;
         Arc.ROTATION.rotationPitch = pitch;
         Arc.ROTATION.setRenderRotation(this.spinYaw, pitch, true);
      }
   }

   @EventListener(
      priority = 500
   )
   public void onUpdateRotate(UpdateRotateEvent event) {
      if (!nullCheck() && this.isOn()) {
         event.setYaw(this.spinYaw);
      }
   }

   public String getInfo() {
      return this.direction.is(Spin.Direction.Left) ? "<-" : "->";
   }

   public static enum Direction {
      Left,
      Right;

      // $FF: synthetic method
      private static Direction[] $values() {
         return new Direction[]{Left, Right};
      }
   }
}
