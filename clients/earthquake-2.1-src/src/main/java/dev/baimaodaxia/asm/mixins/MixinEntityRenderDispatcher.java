package dev.baimaodaxia.asm.mixins;

import dev.baimaodaxia.earthquake;
import dev.baimaodaxia.api.events.impl.RenderEntityEvent;
import net.minecraft.entity.Entity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({EntityRenderDispatcher.class})
public class MixinEntityRenderDispatcher {
   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public <E extends Entity> void onRender(E entity, double x, double y, double z, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
      RenderEntityEvent event = RenderEntityEvent.get(entity);
      earthquake.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         ci.cancel();
      }

   }
}
