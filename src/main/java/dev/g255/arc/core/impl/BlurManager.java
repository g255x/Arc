package dev.g255.arc.core.impl;

import dev.g255.arc.api.utils.Wrapper;
import net.minecraft.util.Identifier;
import org.ladysnake.satin.api.managed.ManagedShaderEffect;
import org.ladysnake.satin.api.managed.ShaderEffectManager;

public class BlurManager implements Wrapper {
   public static final ManagedShaderEffect BLUR = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/blurarea.json"));

   public void applyBlur(float radius, float startX, float startY, float width, float height) {
      this.applyBlur(radius, startX, startY, width, height, 0.0F);
   }

   public void applyBlur(float radius, float startX, float startY, float width, float height, float blurType) {
      float factor = (float)mc.getWindow().getScaleFactor() / 2.0F;
      BLUR.setUniformValue("Radius", radius);
      BLUR.setUniformValue("BlurType", blurType);
      BLUR.setUniformValue("BlurXY", startX * factor, (float)mc.getWindow().getHeight() / 2.0F - (startY + height) * factor);
      BLUR.setUniformValue("BlurCoord", width * factor, height * factor);
      BLUR.render(mc.getRenderTickCounter().getTickDelta(true));
   }
}
