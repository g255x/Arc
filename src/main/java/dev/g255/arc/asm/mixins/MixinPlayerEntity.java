package dev.g255.arc.asm.mixins;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.Event;
import dev.g255.arc.api.events.impl.JumpEvent;
import dev.g255.arc.api.events.impl.TravelEvent;
import dev.g255.arc.api.utils.Wrapper;
import dev.g255.arc.mod.modules.impl.client.ClientSetting;
import dev.g255.arc.mod.modules.impl.player.InteractTweaks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.EntityPose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PlayerEntity.class})
public class MixinPlayerEntity implements Wrapper {
   @Inject(
      method = {"canChangeIntoPose"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void poseNotCollide(EntityPose pose, CallbackInfoReturnable<Boolean> cir) {
      if (PlayerEntity.class.cast(this) == mc.player && !ClientSetting.INSTANCE.crawl.getValue() && pose == EntityPose.SWIMMING) {
         cir.setReturnValue(false);
      }

   }

   @Inject(
      method = {"getBlockInteractionRange"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void getBlockInteractionRangeHook(CallbackInfoReturnable<Double> cir) {
      if (InteractTweaks.INSTANCE.reach()) {
         cir.setReturnValue(InteractTweaks.INSTANCE.blockRange.getValue());
      }

   }

   @Inject(
      method = {"getEntityInteractionRange"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void getEntityInteractionRangeHook(CallbackInfoReturnable<Double> cir) {
      if (InteractTweaks.INSTANCE.reach()) {
         cir.setReturnValue(InteractTweaks.INSTANCE.entityRange.getValue());
      }

   }

   @Inject(
      method = {"jump"},
      at = {@At("HEAD")}
   )
   private void onJumpPre(CallbackInfo ci) {
      Arc.EVENT_BUS.post(JumpEvent.get(Event.Stage.Pre));
   }

   @Inject(
      method = {"jump"},
      at = {@At("RETURN")}
   )
   private void onJumpPost(CallbackInfo ci) {
      Arc.EVENT_BUS.post(JumpEvent.get(Event.Stage.Post));
   }

   @Inject(
      method = {"travel"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onTravelPre(Vec3d movementInput, CallbackInfo ci) {
      PlayerEntity player = (PlayerEntity)PlayerEntity.class.cast(this);
      if (player == mc.player) {
         TravelEvent event = TravelEvent.get(Event.Stage.Pre, player);
         Arc.EVENT_BUS.post(event);
         if (event.isCancelled()) {
            ci.cancel();
            event = TravelEvent.get(Event.Stage.Post, player);
            Arc.EVENT_BUS.post(event);
         }

      }
   }

   @Inject(
      method = {"travel"},
      at = {@At("RETURN")}
   )
   private void onTravelPost(Vec3d movementInput, CallbackInfo ci) {
      PlayerEntity player = (PlayerEntity)PlayerEntity.class.cast(this);
      if (player == mc.player) {
         TravelEvent event = TravelEvent.get(Event.Stage.Post, player);
         Arc.EVENT_BUS.post(event);
      }
   }
}
