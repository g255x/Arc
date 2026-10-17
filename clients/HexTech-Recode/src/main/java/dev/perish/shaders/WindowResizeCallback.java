package dev.perish.shaders;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.util.Window;
import net.minecraft.client.MinecraftClient;

public interface WindowResizeCallback {
    Event<WindowResizeCallback> EVENT = EventFactory.createArrayBacked(WindowResizeCallback.class, callbacks -> (client, window) -> {
        for (var callback : callbacks) {
            callback.onResized(client, window);
        }
    });

    void onResized(MinecraftClient client, Window window);
}