package dev.perish.mod.modules.impl.misc;

import dev.perish.mod.gui.MusicPlayerScreen;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.impl.SliderSetting;

public class MusicPlayerModule extends Module {
    public static MusicPlayerModule INSTANCE;
    public final SliderSetting opacity = this.add(new SliderSetting("Opacity", 100, 20, 100, 5));

    public MusicPlayerModule() {
        super("MusicPlayer", Module.Category.Misc);
        this.setChinese("音乐播放器");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        mc.setScreen(new MusicPlayerScreen());
        this.disable();
    }
}
