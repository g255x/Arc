package dev.luminous.asm.mixins;

import dev.luminous.Nina;
import dev.luminous.api.events.impl.AmbientOcclusionEvent;
import dev.luminous.mod.modules.impl.player.PacketMine;
import net.minecraft.class_1922;
import net.minecraft.class_2338;
import net.minecraft.class_259;
import net.minecraft.class_265;
import net.minecraft.class_2680;
import net.minecraft.class_3726;
import net.minecraft.class_4970;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({class_4970.class})
public abstract class MixinAbstractBlock {
   @Inject(
      method = {"getAmbientOcclusionLightLevel"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onGetAmbientOcclusionLightLevel(class_2680 state, class_1922 world, class_2338 pos, CallbackInfoReturnable info) {
      AmbientOcclusionEvent event = (AmbientOcclusionEvent)Nina.EVENT_BUS.post((Object)AmbientOcclusionEvent.get());
      if (event.lightLevel != -1.0F) {
         info.setReturnValue(event.lightLevel);
      }

   }

   @Inject(
      method = {"getCollisionShape"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onComputeNextCollisionBox(class_2680 state, class_1922 world, class_2338 pos, class_3726 context, CallbackInfoReturnable cir) {
      if (PacketMine.INSTANCE != null && pos.equals(PacketMine.getBreakPos()) && PacketMine.INSTANCE.noCollide.getValue() && PacketMine.ghost) {
         cir.setReturnValue(class_259.method_1073());
      }

   }
}
