package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.EntitySpawnEvent;
import dev.luminous.api.events.impl.EntitySpawnedEvent;
import dev.luminous.api.events.impl.RemoveEntityEvent;
import dev.luminous.api.events.impl.TickEntityEvent;
import dev.luminous.mod.modules.impl.render.Ambience;
import dev.luminous.mod.modules.impl.render.NoRender;
import java.awt.Color;
import java.util.function.Supplier;
import net.minecraft.class_1297;
import net.minecraft.class_1937;
import net.minecraft.class_243;
import net.minecraft.class_2874;
import net.minecraft.class_3695;
import net.minecraft.class_5269;
import net.minecraft.class_5294;
import net.minecraft.class_5321;
import net.minecraft.class_5455;
import net.minecraft.class_638;
import net.minecraft.class_6880;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({class_638.class})
public abstract class MixinClientWorld extends class_1937 {
   @Unique
   private final class_5294 overworld = new class_5294.class_5297();

   protected MixinClientWorld(class_5269 properties, class_5321 registryRef, class_5455 registryManager, class_6880 dimensionEntry, Supplier profiler, boolean isClient, boolean debugWorld, long biomeAccess, int maxChainedNeighborUpdates) {
      super(properties, registryRef, registryManager, dimensionEntry, profiler, isClient, debugWorld, biomeAccess, maxChainedNeighborUpdates);
   }

   @Inject(
      method = {"tickEntity"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void onTickEntity(class_1297 entity, CallbackInfo ci) {
      TickEntityEvent event = TickEntityEvent.get(entity);
      Alien.EVENT_BUS.post((Object)event);
      if (event.isCancelled()) {
         ci.cancel();
      }

   }

   @Inject(
      method = {"addEntity"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void onAddEntity(class_1297 entity, CallbackInfo ci) {
      EntitySpawnEvent event = EntitySpawnEvent.get(entity);
      Alien.EVENT_BUS.post((Object)event);
      if (event.isCancelled()) {
         ci.cancel();
      }

   }

   @Inject(
      method = {"removeEntity"},
      at = {@At("HEAD")}
   )
   private void hookRemoveEntity(int entityId, class_1297.class_5529 removalReason, CallbackInfo ci) {
      class_1297 entity = this.method_8469(entityId);
      if (entity != null) {
         RemoveEntityEvent removeEntityEvent = RemoveEntityEvent.get(entity, removalReason);
         Alien.EVENT_BUS.post((Object)removeEntityEvent);
      }
   }

   @Inject(
      method = {"addEntity"},
      at = {@At("TAIL")}
   )
   public void onAddEntityTail(class_1297 entity, CallbackInfo ci) {
      EntitySpawnedEvent event = EntitySpawnedEvent.get(entity);
      Alien.EVENT_BUS.post((Object)event);
   }

   @Inject(
      method = {"getSkyColor"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onGetSkyColor(class_243 cameraPos, float tickDelta, CallbackInfoReturnable info) {
      if (Ambience.INSTANCE.isOn() && Ambience.INSTANCE.sky.booleanValue) {
         Color sky = Ambience.INSTANCE.sky.getValue();
         info.setReturnValue(new class_243((double)sky.getRed() / (double)255.0F, (double)sky.getGreen() / (double)255.0F, (double)sky.getBlue() / (double)255.0F));
      }

   }

   @Inject(
      method = {"getCloudsColor"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookGetCloudsColor(float tickDelta, CallbackInfoReturnable cir) {
      if (Ambience.INSTANCE.isOn() && Ambience.INSTANCE.cloud.booleanValue) {
         Color sky = Ambience.INSTANCE.cloud.getValue();
         cir.setReturnValue(new class_243((double)sky.getRed() / (double)255.0F, (double)sky.getGreen() / (double)255.0F, (double)sky.getBlue() / (double)255.0F));
      }

   }

   @Inject(
      method = {"getDimensionEffects"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onGetSkyProperties(CallbackInfoReturnable info) {
      if (Ambience.INSTANCE.isOn() && Ambience.INSTANCE.forceOverworld.getValue()) {
         info.setReturnValue(this.overworld);
      }

   }

   public float method_8430(float delta) {
      return NoRender.INSTANCE.isOn() && NoRender.INSTANCE.weather.getValue() ? 0.0F : super.method_8430(delta);
   }

   public float method_8478(float delta) {
      return NoRender.INSTANCE.isOn() && NoRender.INSTANCE.weather.getValue() ? 0.0F : super.method_8478(delta);
   }
}
