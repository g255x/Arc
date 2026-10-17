package dev.kissoo.mod.modules.impl.render;

import dev.kissoo.mod.modules.Module;
import dev.kissoo.optimizations.FrameInterpolation;

public class FrameInterp extends Module {
    public static FrameInterp INSTANCE;

    public FrameInterp() {
        super("FrameInterp", Module.Category.Render);
        this.setChinese("插帧");
        INSTANCE = this;
    }

    @Override
    public void onToggle() {
        if (!this.isOn()) {
            FrameInterpolation.cleanup();
        }
    }
}
