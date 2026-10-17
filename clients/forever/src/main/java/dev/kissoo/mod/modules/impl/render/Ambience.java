package dev.kissoo.mod.modules.impl.render;

import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.PacketEvent;
import dev.kissoo.api.events.impl.PreRender2DEvent;
import dev.kissoo.api.events.impl.UpdateEvent;
import hoprc.api.core.impl.ShaderManager;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;

public class Ambience extends Module {

   public enum SkyMode {
      Off,
      Cine,
      Aurora,
      Local
   }

   public enum WeatherType {
      CLEAR("晴天"),
      RAIN("雨天"),
      SNOW("雪天"),
      STORM("雷暴");

      private final String chinese;

      WeatherType(String chinese) {
         this.chinese = chinese;
      }

      @Override
      public String toString() {
         return chinese;
      }
   }

   public static Ambience INSTANCE;

   public final ColorSetting filter = this.add(
           new ColorSetting("Filter", new Color(255, 255, 255, 20)).injectBoolean(false)
   );
   public final ColorSetting worldColor = this.add(
           new ColorSetting("WorldColor", new Color(-1, true)).injectBoolean(true)
   );
   public final BooleanSetting customTime = this.add(
           new BooleanSetting("CustomTime", false).setParent()
   );
   public final SliderSetting time = this.add(
           new SliderSetting("Time", 0, 0, 24000, this.customTime::isOpen)
   );
   public final ColorSetting fog = this.add(
           new ColorSetting("FogColor", new Color(13401557)).injectBoolean(false)
   );
   public final ColorSetting sky = this.add(
           new ColorSetting("SkyColor", new Color(0)).injectBoolean(false)
   );
   public final ColorSetting cloud = this.add(
           new ColorSetting("CloudColor", new Color(0)).injectBoolean(false)
   );
   public final ColorSetting dimensionColor = this.add(
           new ColorSetting("DimensionColor", new Color(0)).injectBoolean(false)
   );
   public final EnumSetting<SkyMode> skyMode = this.add(
           new EnumSetting<>("SkyMode", SkyMode.Off)
   );
   public final SliderSetting skySpeed = this.add(
           new SliderSetting("SkySpeed", 4.0, 0.0, 20.0, 0.1, () -> this.skyMode.getValue() != SkyMode.Off)
   );
   public final ColorSetting skyShaderColor = this.add(
           new ColorSetting("SkyShaderColor", new Color(255, 255, 255, 200), () -> this.skyMode.getValue() != SkyMode.Off)
   );
   public final ColorSetting skyBackgroundColor = this.add(
           new ColorSetting("SkyBackground", new Color(16, 24, 48, 255), () -> this.skyMode.getValue() == SkyMode.Local)
   );
   public final BooleanSetting fogDistance = this.add(
           new BooleanSetting("FogDistance", false).setParent()
   );
   public final SliderSetting fogStart = this.add(
           new SliderSetting("FogStart", 50, 0, 1000, this.fogDistance::isOpen)
   );
   public final SliderSetting fogEnd = this.add(
           new SliderSetting("FogEnd", 100, 0, 1000, this.fogDistance::isOpen)
   );
   public final BooleanSetting fullBright = this.add(
           new BooleanSetting("FullBright", false)
   );
   public final BooleanSetting customLuminance = this.add(
           new BooleanSetting("CustomLuminance", false).setParent().injectTask(() -> {
              if (!nullCheck()) {
                 mc.worldRenderer.reload();
              }
           })
   );
   public final SliderSetting luminance = this.add(
           new SliderSetting("Luminance", 15, 0, 15, this.customLuminance::isOpen)
                   .injectTask(() -> {
                      if (!nullCheck() && this.customLuminance.getValue()) {
                         mc.worldRenderer.reload();
                      }
                   })
   );

   public final BooleanSetting customWeather = this.add(
           new BooleanSetting("CustomWeather", false).setParent()
   );
   public final EnumSetting<WeatherType> weatherType = this.add(
           new EnumSetting<>("WeatherType", WeatherType.CLEAR, this.customWeather::isOpen)
   );
   public final SliderSetting weatherIntensity = this.add(
           new SliderSetting("Intensity", 1.0, 0.0, 1.0,  // 改成 double
                   () -> this.customWeather.isOpen()
                           && this.weatherType.getValue() != WeatherType.CLEAR
           )
   );
   public final BooleanSetting weatherPenetrate = this.add(
           new BooleanSetting("Penetrate", false,
                   () -> this.customWeather.isOpen()
                           && this.weatherType.getValue() != WeatherType.CLEAR
           )
   );
   long oldTime;
   private float oldRainGradient;
   private float oldThunderGradient;
   public Ambience() {
      super("Ambience", "Custom ambience", Module.Category.Render);
      this.setChinese("自定义环境");
      INSTANCE = this;
   }
   @EventListener
   public void onRender2D(PreRender2DEvent event) {
      if (this.filter.booleanValue) {
         event.drawContext.fill(
                 0, 0,
                 mc.getWindow().getScaledWidth(),
                 mc.getWindow().getScaledHeight(),
                 this.filter.getValue().getRGB()
         );
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.customTime.getValue()) {
         mc.world.setTimeOfDay((long) this.time.getValue());
      }

      if (this.customWeather.getValue() && !nullCheck()) {
         applyWeather();
      }
   }

   @EventListener
   public void onReceivePacket(PacketEvent.Receive event) {
      if (event.getPacket() instanceof WorldTimeUpdateS2CPacket packet) {
         if (this.customTime.getValue()) {
            this.oldTime = packet.getTime();
            event.cancel();
         }
      }
   }

   @Override
   public void onEnable() {
      if (!nullCheck()) {
         this.oldTime = mc.world.getTimeOfDay();
         if (this.customTime.getValue()) {
            mc.world.setTimeOfDay((long) this.time.getValue());
         }
         this.oldRainGradient = mc.world.getRainGradient(1.0f);
         this.oldThunderGradient = mc.world.getThunderGradient(1.0f);
         if (this.customWeather.getValue()) {
            applyWeather();
         }
      }
   }

   @Override
   public void onDisable() {
      if (!nullCheck()) {
         mc.world.setTimeOfDay(this.oldTime);
         restoreWeather();
      }
   }
   private void applyWeather() {
      float intensity = (float) this.weatherIntensity.getValue();  // 转换为 float

      switch (this.weatherType.getValue()) {
         case CLEAR -> {
            mc.world.setRainGradient(0.0f);
            mc.world.setThunderGradient(0.0f);
         }
         case RAIN -> {
            mc.world.setRainGradient(intensity);
            mc.world.setThunderGradient(0.0f);
         }
         case SNOW -> {
            mc.world.setRainGradient(intensity);
            mc.world.setThunderGradient(0.0f);
         }
         case STORM -> {
            mc.world.setRainGradient(intensity);
            mc.world.setThunderGradient(intensity);
         }
      }
   }

   private void restoreWeather() {
      mc.world.setRainGradient(this.oldRainGradient);
      mc.world.setThunderGradient(this.oldThunderGradient);
   }

   public boolean shouldPenetrate() {
      return this.isEnabled()
              && this.customWeather.getValue()
              && this.weatherPenetrate.getValue()
              && this.weatherType.getValue() != WeatherType.CLEAR;
   }

   public WeatherType getWeatherType() {
      return this.weatherType.getValue();
   }

   public float getWeatherIntensity() {
      return (float) this.weatherIntensity.getValue();
   }

   public boolean shouldRenderSkyShader() {
      return this.isOn() && this.skyMode.getValue() != SkyMode.Off;
   }

   public ShaderManager.Shader getSkyShader() {
      return switch (this.skyMode.getValue()) {
         case Cine -> ShaderManager.Shader.Cine;
         case Aurora -> ShaderManager.Shader.Aurora;
         case Local -> ShaderManager.Shader.Local;
         default -> ShaderManager.Shader.Cine;
      };
   }
}
