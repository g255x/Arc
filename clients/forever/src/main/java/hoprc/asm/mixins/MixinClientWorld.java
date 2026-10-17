package hoprc.asm.mixins;

import dev.kissoo.Forever;
import dev.kissoo.api.events.impl.EntitySpawnEvent;
import dev.kissoo.api.events.impl.EntitySpawnedEvent;
import dev.kissoo.api.events.impl.RemoveEntityEvent;
import dev.kissoo.api.events.impl.TickEntityEvent;
import dev.kissoo.mod.modules.impl.render.skybox.Skybox;
import dev.kissoo.mod.modules.impl.render.Ambience;
import dev.kissoo.mod.modules.impl.render.NoRender;
import java.awt.Color;
import java.util.function.Supplier;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.class)
public abstract class MixinClientWorld extends World {
   protected MixinClientWorld(
      MutableWorldProperties properties,
      RegistryKey<World> registryRef,
      DynamicRegistryManager registryManager,
      RegistryEntry<DimensionType> dimensionEntry,
      Supplier<Profiler> profiler,
      boolean isClient,
      boolean debugWorld,
      long biomeAccess,
      int maxChainedNeighborUpdates
   ) {
      super(properties, registryRef, registryManager, dimensionEntry, profiler, isClient, debugWorld, biomeAccess, maxChainedNeighborUpdates);
   }

   @Inject(method = "tickEntity", at = @At("HEAD"), cancellable = true)
   public void onTickEntity(Entity entity, CallbackInfo ci) {
      TickEntityEvent event = TickEntityEvent.get(entity);
      Forever.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
   public void onAddEntity(Entity entity, CallbackInfo ci) {
      EntitySpawnEvent event = EntitySpawnEvent.get(entity);
      Forever.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(method = "removeEntity", at = @At("HEAD"))
   private void hookRemoveEntity(int entityId, RemovalReason removalReason, CallbackInfo ci) {
      Entity entity = this.getEntityById(entityId);
      if (entity != null) {
         RemoveEntityEvent removeEntityEvent = RemoveEntityEvent.get(entity, removalReason);
         Forever.EVENT_BUS.post(removeEntityEvent);
      }
   }

   @Inject(method = "addEntity", at = @At("TAIL"))
   public void onAddEntityTail(Entity entity, CallbackInfo ci) {
      EntitySpawnedEvent event = EntitySpawnedEvent.get(entity);
      Forever.EVENT_BUS.post(event);
   }

   @Inject(method = "getSkyColor", at = @At("HEAD"), cancellable = true)
   private void onGetSkyColor(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Vec3d> info) {
       if (Skybox.INSTANCE != null && Skybox.INSTANCE.isOn()) {
           return;
       }
      if (Ambience.INSTANCE != null && Ambience.INSTANCE.isOn() && Ambience.INSTANCE.sky.booleanValue) {
         Color sky = Ambience.INSTANCE.sky.getValue();
         info.setReturnValue(new Vec3d(sky.getRed() / 255.0, sky.getGreen() / 255.0, sky.getBlue() / 255.0));
      }
   }

   @Inject(method = "getCloudsColor", at = @At("HEAD"), cancellable = true)
   private void hookGetCloudsColor(float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
      if (Ambience.INSTANCE != null && Ambience.INSTANCE.isOn() && Ambience.INSTANCE.cloud.booleanValue) {
         Color sky = Ambience.INSTANCE.cloud.getValue();
         cir.setReturnValue(new Vec3d(sky.getRed() / 255.0, sky.getGreen() / 255.0, sky.getBlue() / 255.0));
      }
   }

   @Override
   public float getRainGradient(float delta) {
      return NoRender.INSTANCE != null && NoRender.INSTANCE.isOn() && NoRender.INSTANCE.weather.getValue() ? 0.0F : super.getRainGradient(delta);
   }

   @Override
   public float getThunderGradient(float delta) {
      return NoRender.INSTANCE != null && NoRender.INSTANCE.isOn() && NoRender.INSTANCE.weather.getValue() ? 0.0F : super.getThunderGradient(delta);
   }
}
