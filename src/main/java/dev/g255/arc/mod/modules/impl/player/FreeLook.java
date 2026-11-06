package dev.g255.arc.mod.modules.impl.player;

import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.LookDirectionEvent;
import dev.g255.arc.api.utils.math.MathUtil;
import dev.g255.arc.mod.modules.Module;
import net.minecraft.client.util.math.MatrixStack;

public class FreeLook extends Module {
   public static FreeLook INSTANCE;
   private float fakeYaw;
   private float fakePitch;
   private float prevFakeYaw;
   private float prevFakePitch;

   public FreeLook() {
      super("FreeLook", Module.Category.Player);
      this.setChinese("自由视角");
      INSTANCE = this;
   }

   public void onEnable() {
      if (nullCheck()) {
         this.disable();
      } else {
         this.fakePitch = mc.player.getPitch();
         this.fakeYaw = mc.player.getYaw();
         this.prevFakePitch = this.fakePitch;
         this.prevFakeYaw = this.fakeYaw;
      }
   }

   @EventListener
   public void onLookDirection(LookDirectionEvent event) {
      this.fakeYaw += (float)event.getCursorDeltaX() * 0.15F;
      this.fakePitch += (float)event.getCursorDeltaY() * 0.15F;
      event.cancel();
   }

   public void onRender3D(MatrixStack matrixStack) {
      this.prevFakeYaw = this.fakeYaw;
      this.prevFakePitch = this.fakePitch;
   }

   public float getFakeYaw() {
      return MathUtil.interpolate(this.prevFakeYaw, this.fakeYaw, mc.getRenderTickCounter().getTickDelta(true));
   }

   public float getFakePitch() {
      return MathUtil.interpolate(this.prevFakePitch, this.fakePitch, mc.getRenderTickCounter().getTickDelta(true));
   }
}
