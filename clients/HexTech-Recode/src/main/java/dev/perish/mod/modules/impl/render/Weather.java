package dev.perish.mod.modules.impl.render;

import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.impl.ColorSetting;
import dev.perish.mod.modules.settings.impl.EnumSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;

import java.awt.*;

public class Weather extends Module {
    public static Weather INSTANCE;
    public final EnumSetting<Mode> mode = add(new EnumSetting<>("Mode", Mode.Rain));
    public final SliderSetting height = add(new SliderSetting("Height", 0, -50, 50, 1));
    public final SliderSetting strength = add(new SliderSetting("Strength", 1, 0.1, 2, 0.1));
    public final ColorSetting weatherColor = add(new ColorSetting("WeatherColor", new Color(255, 255, 255, 255)));
    public final SliderSetting expandSize = add(new SliderSetting("ExpandSize", 1, 0.1, 5, 0.1));
    public final SliderSetting multi = add(new SliderSetting("Multi", 1, 0.1, 5, 0.1));

    public Weather() {
        super("Weather", Category.Render);
        INSTANCE = this;
    }

    public enum Mode {
        Rain,
        Snow,
        No
    }
}
