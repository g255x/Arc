package dev.baimaodaxia.api.utils.render.panorama;

import net.minecraft.client.main.Main;

public class ShaderPatcher {
   private static final String FRAGMENT_OLD = "gl_FragColor";
   private static final String FRAGMENT_NEW = "fragColor";

   public static String patch(String shader) {
      if (shader != null && !shader.isEmpty()) {
         if (shader.contains("gl_FragColor")) {
            shader = "out vec4 fragColor;\n" + shader.replace("gl_FragColor", "fragColor");
         }

         if (!shader.contains("#version")) {
            shader = "#version 330\n" + shader;
         }

         return shader;
      } else {
         return "#version 330\nvoid Main() {}";
      }
   }
}
