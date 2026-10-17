package hoprc.asm.mixins;

import dev.kissoo.Forever;
import dev.kissoo.chat.ChatClient;
import dev.kissoo.api.events.Event;
import dev.kissoo.api.events.impl.JumpEvent;
import dev.kissoo.api.events.impl.TravelEvent;
import dev.kissoo.api.utils.Wrapper;
import dev.kissoo.mod.modules.impl.client.ClientSetting;
import dev.kissoo.mod.modules.impl.player.InteractTweaks;
import net.minecraft.entity.EntityPose;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class MixinPlayerEntity implements Wrapper {
   @Inject(method = "canChangeIntoPose", at = @At("RETURN"), cancellable = true)
   private void poseNotCollide(EntityPose pose, CallbackInfoReturnable<Boolean> cir) {
      if (PlayerEntity.class.cast(this) == mc.player
              && ClientSetting.INSTANCE != null
              && !ClientSetting.INSTANCE.crawl.getValue()
              && pose == EntityPose.SWIMMING) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "getBlockInteractionRange", at = @At("HEAD"), cancellable = true)
   public void getBlockInteractionRangeHook(CallbackInfoReturnable<Double> cir) {
      if (InteractTweaks.INSTANCE != null && InteractTweaks.INSTANCE.reach()) {
         cir.setReturnValue(InteractTweaks.INSTANCE.blockRange.getValue());
      }
   }

   @Inject(method = "getEntityInteractionRange", at = @At("HEAD"), cancellable = true)
   public void getEntityInteractionRangeHook(CallbackInfoReturnable<Double> cir) {
      if (InteractTweaks.INSTANCE != null && InteractTweaks.INSTANCE.reach()) {
         cir.setReturnValue(InteractTweaks.INSTANCE.entityRange.getValue());
      }
   }

   @Inject(method = "jump", at = @At("HEAD"))
   private void onJumpPre(CallbackInfo ci) {
      Forever.EVENT_BUS.post(JumpEvent.get(Event.Stage.Pre));
   }

   @Inject(method = "jump", at = @At("RETURN"))
   private void onJumpPost(CallbackInfo ci) {
      Forever.EVENT_BUS.post(JumpEvent.get(Event.Stage.Post));
   }

   @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
   private void onTravelPre(Vec3d movementInput, CallbackInfo ci) {
      PlayerEntity player = (PlayerEntity)PlayerEntity.class.cast(this);
      if (player == mc.player) {
         TravelEvent event = TravelEvent.get(Event.Stage.Pre, player);
         Forever.EVENT_BUS.post(event);
         if (event.isCancelled()) {
            ci.cancel();
            event = TravelEvent.get(Event.Stage.Post, player);
            Forever.EVENT_BUS.post(event);
         }
      }
   }

   @Inject(method = "travel", at = @At("RETURN"))
   private void onTravelPost(Vec3d movementInput, CallbackInfo ci) {
      PlayerEntity player = (PlayerEntity)PlayerEntity.class.cast(this);
      if (player == mc.player) {
         TravelEvent event = TravelEvent.get(Event.Stage.Post, player);
         Forever.EVENT_BUS.post(event);
      }
   }

}
