package dev.baimaodaxia.api.utils.render.panorama;

import dev.baimaodaxia.api.utils.render.panorama.impl.PanoramaFallbackRenderer;
import dev.baimaodaxia.api.utils.render.panorama.impl.PanoramaShaderRenderer;
import net.minecraft.util.Identifier;

public class PanoramaShader {
   private final String vertex;
   private final String fragment;
   private final Identifier texture;

   public PanoramaShader(String vertex, String fragment, Identifier texture) {
      this.vertex = ShaderPatcher.patch(vertex);
      this.fragment = ShaderPatcher.patch(fragment);
      this.texture = texture;
   }

   public PanoramaRenderer compile() {
      try {
         return new PanoramaShaderRenderer(this.vertex, this.fragment, this.texture);
      } catch (Exception e) {
         e.printStackTrace();
         return new PanoramaFallbackRenderer(15675965, 16777215);
      }
   }
}
