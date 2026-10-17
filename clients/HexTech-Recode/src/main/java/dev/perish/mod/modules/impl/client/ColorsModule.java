package dev.perish.mod.modules.impl.client;

import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.ColorSetting;

import java.awt.*;

public class ColorsModule extends Module {
    public static ColorsModule INSTANCE;
    public final ColorSetting clientColor = this.add(new ColorSetting("Color", new Color(-6710785)).allowClientColor(false));
    public final BooleanSetting pulse = this.add(new BooleanSetting("Pulse", false));
    public final ColorSetting pulseStart = this.add(
            new ColorSetting("PulseStart", new Color(-6710785), this.pulse::getValue).allowClientColor(false).allowGlobalPulse(false)
    );
    public final ColorSetting pulseEnd = this.add(
            new ColorSetting("PulseEnd", new Color(153, 153, 255), this.pulse::getValue).allowClientColor(false).allowGlobalPulse(false)
    );

    public ColorsModule() {
        super("Colors", Module.Category.Client);
        this.setChinese("颜色");
        INSTANCE = this;
    }

    @Override
    public void enable() {
        this.state = true;
    }

    @Override
    public void disable() {
        this.state = true;
    }

    @Override
    public boolean isOn() {
        return true;
    }
}
