package dev.perish.asm.mixins;

import dev.perish.Perish;
import dev.perish.api.events.impl.EntitySpawnEvent;
import dev.perish.api.events.impl.EntitySpawnedEvent;
import dev.perish.api.events.impl.RemoveEntityEvent;
import dev.perish.api.events.impl.TickEntityEvent;
import dev.perish.mod.modules.impl.render.Ambience;
import dev.perish.mod.modules.impl.render.NoRender;
import dev.perish.mod.modules.impl.render.SkyColor;
import dev.perish.mod.modules.impl.render.skybox.Skybox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.*;
import java.util.function.Supplier;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.world.World;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.client.render.DimensionEffects;
import net.minecraft.client.render.DimensionEffects.Overworld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.entry.RegistryEntry;

@Mixin(ClientWorld.class)
public abstract class MixinClientWorld extends World {
    @Unique
    private final DimensionEffects overworld = new Overworld();

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
        Perish.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
    @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
    public void onAddEntity(Entity entity, CallbackInfo ci) {
        EntitySpawnEvent event = EntitySpawnEvent.get(entity);
        Perish.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "removeEntity", at = @At("HEAD"))
    private void hookRemoveEntity(int entityId, RemovalReason removalReason, CallbackInfo ci) {
        Entity entity = this.getEntityById(entityId);
        if (entity != null) {
            RemoveEntityEvent removeEntityEvent = RemoveEntityEvent.get(entity, removalReason);
            Perish.EVENT_BUS.post(removeEntityEvent);
        }
    }

    @Inject(method = "addEntity", at = @At("TAIL"))
    public void onAddEntityTail(Entity entity, CallbackInfo ci) {
        EntitySpawnedEvent event = EntitySpawnedEvent.get(entity);
        Perish.EVENT_BUS.post(event);
    }

    @Inject(method = "getSkyColor", at = @At("HEAD"), cancellable = true)
    private void onGetSkyColor(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Vec3d> info) {
        if (Ambience.INSTANCE != null && Ambience.INSTANCE.isOn() && Ambience.INSTANCE.sky.booleanValue) {
            Color sky = Ambience.INSTANCE.sky.getValue();
            info.setReturnValue(new Vec3d(sky.getRed() / 255.0, sky.getGreen() / 255.0, sky.getBlue() / 255.0));
            return;
        }
        if (Skybox.INSTANCE != null && Skybox.INSTANCE.isOn()) {
            return;
        }
        if (SkyColor.INSTANCE != null && SkyColor.INSTANCE.isActive()) {
            Color sky = SkyColor.INSTANCE.skyColor.getValue();
            info.setReturnValue(new Vec3d((double)sky.getRed() / 255.0, (double)sky.getGreen() / 255.0, (double)sky.getBlue() / 255.0));
        }
    }

    @Inject(method = "getCloudsColor", at = @At("HEAD"), cancellable = true)
    private void hookGetCloudsColor(float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
        if (Ambience.INSTANCE != null && Ambience.INSTANCE.isOn() && Ambience.INSTANCE.cloud.booleanValue) {
            Color sky = Ambience.INSTANCE.cloud.getValue();
            cir.setReturnValue(new Vec3d(sky.getRed() / 255.0, sky.getGreen() / 255.0, sky.getBlue() / 255.0));
        }
    }

    @Inject(method = "getDimensionEffects", at = @At("HEAD"), cancellable = true)
    private void onGetSkyProperties(CallbackInfoReturnable<DimensionEffects> info) {
        if (Ambience.INSTANCE != null && Ambience.INSTANCE.isOn() && Ambience.INSTANCE.forceOverworld.getValue()) {
            info.setReturnValue(this.overworld);
        }
    }

    @Inject(method = "getDimensionEffects", at = @At("RETURN"), cancellable = true)
    private void onGetSkyPropertiesReturn(CallbackInfoReturnable<DimensionEffects> info) {
        if (info.getReturnValue() == null) {
            info.setReturnValue(this.overworld);
        }
    }

    @Override
    public float getRainGradient(float delta) {
        if (dev.perish.mod.modules.impl.render.Weather.INSTANCE != null && dev.perish.mod.modules.impl.render.Weather.INSTANCE.isOn()) {
            if (dev.perish.mod.modules.impl.render.Weather.INSTANCE.mode.getValue() == dev.perish.mod.modules.impl.render.Weather.Mode.No) {
                return 0.0f;
            }
            return 1.0f * (float) dev.perish.mod.modules.impl.render.Weather.INSTANCE.strength.getValue();
        }
        return NoRender.INSTANCE != null && NoRender.INSTANCE.isOn() && NoRender.INSTANCE.weather.getValue() ? 0.0F : super.getRainGradient(delta);
    }
    @Override
    public float getThunderGradient(float delta) {
        if (dev.perish.mod.modules.impl.render.Weather.INSTANCE != null && dev.perish.mod.modules.impl.render.Weather.INSTANCE.isOn()) {
            if (dev.perish.mod.modules.impl.render.Weather.INSTANCE.mode.getValue() == dev.perish.mod.modules.impl.render.Weather.Mode.No) {
                return 0.0f;
            }
            return 1.0f * (float) dev.perish.mod.modules.impl.render.Weather.INSTANCE.strength.getValue();
        }
        return NoRender.INSTANCE != null && NoRender.INSTANCE.isOn() && NoRender.INSTANCE.weather.getValue() ? 0.0F : super.getThunderGradient(delta);
    }
}
