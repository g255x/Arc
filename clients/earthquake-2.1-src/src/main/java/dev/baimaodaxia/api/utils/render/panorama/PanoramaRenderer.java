package dev.baimaodaxia.api.utils.render.panorama;

import net.minecraft.client.MinecraftClient;

public interface PanoramaRenderer extends AutoCloseable {
   void draw(MinecraftClient var1, double var2, long var4, float var6, float var7, int var8, int var9, float var10);

   default void close() {
   }
}
