package dev.g255.arc.mod.modules.impl.player;

import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.KeyboardInputEvent;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.api.events.impl.UpdateRotateEvent;
import dev.g255.arc.api.utils.math.MathUtil;
import dev.g255.arc.api.utils.path.BaritoneUtil;
import dev.g255.arc.api.utils.player.MovementUtil;
import dev.g255.arc.core.impl.RotationManager;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.util.math.MatrixStack;

public class Freecam extends Module {
   public static Freecam INSTANCE;
   private final SliderSetting speed = this.add(new SliderSetting("HSpeed", (double)1.0F, (double)0.0F, (double)3.0F));
   private final SliderSetting hspeed = this.add(new SliderSetting("VSpeed", 0.42, (double)0.0F, (double)3.0F));
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
   private float fakeYaw;
   private float fakePitch;
   private float prevFakeYaw;
   private float prevFakePitch;
   private double fakeX;
   private double fakeY;
   private double fakeZ;
   private double prevFakeX;
   private double prevFakeY;
   private double prevFakeZ;
   private float playerYaw;
   private float playerPitch;

   public Freecam() {
      super("Freecam", Module.Category.Player);
      this.setChinese("自由相机");
      INSTANCE = this;
   }

   public void onEnable() {
      if (nullCheck()) {
         this.disable();
      } else {
         this.playerYaw = this.getYaw();
         this.playerPitch = this.getPitch();
         this.fakePitch = this.getPitch();
         this.fakeYaw = this.getYaw();
         this.prevFakePitch = this.fakePitch;
         this.prevFakeYaw = this.fakeYaw;
         this.fakeX = mc.player.getX();
         this.fakeY = mc.player.getY() + (double)mc.player.getEyeHeight(mc.player.getPose());
         this.fakeZ = mc.player.getZ();
         this.prevFakeX = this.fakeX;
         this.prevFakeY = this.fakeY;
         this.prevFakeZ = this.fakeZ;
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.rotate.getValue() && mc.crosshairTarget != null && mc.crosshairTarget.getPos() != null) {
         float[] angle = RotationManager.getRotation(mc.crosshairTarget.getPos());
         this.playerYaw = angle[0];
         this.playerPitch = angle[1];
      }

      if (BaritoneUtil.isPathing()) {
         double[] motion = MovementUtil.directionSpeed(this.speed.getValue());
         this.prevFakeX = this.fakeX;
         this.prevFakeY = this.fakeY;
         this.prevFakeZ = this.fakeZ;
         this.fakeX += motion[0];
         this.fakeZ += motion[1];
         if (mc.options.jumpKey.isPressed()) {
            this.fakeY += this.hspeed.getValue();
         }

         if (mc.options.sneakKey.isPressed()) {
            this.fakeY -= this.hspeed.getValue();
         }
      }

   }

   @EventListener(
      priority = 200
   )
   public void onRotate(UpdateRotateEvent event) {
      if (!BaritoneUtil.isPathing()) {
         if (!event.isModified()) {
            event.setYawWithoutSync(this.playerYaw);
            event.setPitchWithoutSync(this.playerPitch);
         }
      }
   }

   public void onRender3D(MatrixStack matrixStack) {
      this.prevFakeYaw = this.fakeYaw;
      this.prevFakePitch = this.fakePitch;
      this.fakeYaw = this.getYaw();
      this.fakePitch = this.getPitch();
   }

   private float getYaw() {
      return mc.player.getYaw();
   }

   private float getPitch() {
      return mc.player.getPitch();
   }

   @EventListener
   public void onKeyboardInput(KeyboardInputEvent event) {
      if (mc.player != null) {
         double[] motion = MovementUtil.directionSpeed(this.speed.getValue());
         this.prevFakeX = this.fakeX;
         this.prevFakeY = this.fakeY;
         this.prevFakeZ = this.fakeZ;
         this.fakeX += motion[0];
         this.fakeZ += motion[1];
         if (mc.options.jumpKey.isPressed()) {
            this.fakeY += this.hspeed.getValue();
         }

         if (mc.options.sneakKey.isPressed()) {
            this.fakeY -= this.hspeed.getValue();
         }

         mc.player.input.movementForward = 0.0F;
         mc.player.input.movementSideways = 0.0F;
         mc.player.input.jumping = false;
         mc.player.input.sneaking = false;
      }
   }

   public float getFakeYaw() {
      return MathUtil.interpolate(this.prevFakeYaw, this.fakeYaw, mc.getRenderTickCounter().getTickDelta(true));
   }

   public float getFakePitch() {
      return MathUtil.interpolate(this.prevFakePitch, this.fakePitch, mc.getRenderTickCounter().getTickDelta(true));
   }

   public double getFakeX() {
      return MathUtil.interpolate(this.prevFakeX, this.fakeX, (double)mc.getRenderTickCounter().getTickDelta(true));
   }

   public double getFakeY() {
      return MathUtil.interpolate(this.prevFakeY, this.fakeY, (double)mc.getRenderTickCounter().getTickDelta(true));
   }

   public double getFakeZ() {
      return MathUtil.interpolate(this.prevFakeZ, this.fakeZ, (double)mc.getRenderTickCounter().getTickDelta(true));
   }
}
