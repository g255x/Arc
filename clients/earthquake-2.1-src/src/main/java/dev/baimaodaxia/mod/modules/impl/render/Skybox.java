package dev.baimaodaxia.mod.modules.impl.render;

import dev.baimaodaxia.earthquake;
import dev.baimaodaxia.core.impl.ShaderManager;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.ColorSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;

public class Skybox extends Module {
   public static Skybox INSTANCE;
   public final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 255)));
   public final ColorSetting backgroundColor = this.add(new ColorSetting("BackgroundColor", new Color(0, 0, 0, 255)));
   public final SliderSetting scale = this.add(new SliderSetting("Scale", (double)1.0F, 0.1, (double)5.0F, 0.05));
   public final SliderSetting speed = this.add(new SliderSetting("Speed", (double)4.0F, (double)0.0F, (double)20.0F, 0.1));
   public final SliderSetting opacity = this.add(new SliderSetting("Opacity", (double)1.0F, (double)0.0F, (double)1.0F, 0.01));

   public Skybox() {
      super("Skybox", Module.Category.Render);
      this.setChinese("天空盒");
      INSTANCE = this;
   }

   public void renderSkybox() {
      if (this.isOn()) {
         ShaderManager sm = earthquake.SHADER;
         if (sm != null && !sm.fullNullCheck()) {
            sm.renderSkyboxFullscreen(this.color.getValue(), this.backgroundColor.getValue(), this.scale.getValueFloat(), this.speed.getValueFloat(), this.opacity.getValueFloat());
         }
      }
   }
}
