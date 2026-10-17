package dev.Lumi.mod.modules.impl.render;

import dev.Lumi.Lumi;
import dev.Lumi.mod.modules.Module;
import dev.Lumi.mod.modules.settings.impl.ColorSetting;
import dev.Lumi.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;

public class Skybox extends Module {
   public static Skybox INSTANCE;
   public final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 255)));
   public final ColorSetting backgroundColor = this.add(new ColorSetting("BackgroundColor", new Color(0, 0, 0, 255)));
   public final SliderSetting scale = this.add(new SliderSetting("Scale", (double)1.0F, 0.1, (double)5.0F, 0.05));
   public final SliderSetting speed = this.add(new SliderSetting("Speed", (double)1.0F, (double)0.0F, (double)10.0F, 0.1));
   public final SliderSetting opacity = this.add(new SliderSetting("Opacity", (double)1.0F, (double)0.0F, (double)1.0F, 0.01));

   public Skybox() {
      super("Skybox", Module.Category.Render);
      this.setChinese("天空盒");
      INSTANCE = this;
   }

   public void renderSkybox() {
      if (this.isOn()) {
         Lumi.SHADER.renderSkyboxFullscreen(this.color.getValue(), this.backgroundColor.getValue(), this.scale.getValueFloat(), this.speed.getValueFloat(), this.opacity.getValueFloat());
      }

   }
}
