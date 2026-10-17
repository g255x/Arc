package hoprc.asm.mixins;

import dev.kissoo.Forever;
import dev.kissoo.api.events.impl.TimerEvent;
import dev.kissoo.mod.gui.background.MainMenuModePreference;
import dev.kissoo.mod.gui.background.PhobosBackground;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderTickCounter.Dynamic.class)
public class MixinRenderTickCounter {
   @Shadow
   private float lastFrameDuration;
   @Shadow
   private float tickDelta;
   @Shadow
   private long prevTimeMillis;
   @Final
   @Shadow
   private float tickTime;

   @Inject(method = "beginRenderTick(J)I", at = @At("HEAD"), cancellable = true)
   private void beginRenderTickHook(long timeMillis, CallbackInfoReturnable<Integer> cir) {
      TimerEvent event = TimerEvent.getEvent();
      Forever.EVENT_BUS.post(event);
      if (MainMenuModePreference.isPhobosSelected()) {
         PhobosBackground.getInstance().advanceTick();
      }
      if (!event.isCancelled()) {
         float timer;
         if (event.isModified()) {
            timer = event.get();
         } else if (Forever.TIMER == null) {
            timer = 1.0F;
         } else {
            timer = Forever.TIMER.get();
         }

         if (timer == 1.0F) {
            return;
         }

         this.lastFrameDuration = (float)(timeMillis - this.prevTimeMillis) / this.tickTime * timer;
         this.prevTimeMillis = timeMillis;
         this.tickDelta = this.tickDelta + this.lastFrameDuration;
         int i = (int)this.tickDelta;
         this.tickDelta -= i;
         cir.setReturnValue(i);
      }
   }
}
