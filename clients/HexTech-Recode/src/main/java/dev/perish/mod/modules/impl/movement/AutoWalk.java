package dev.perish.mod.modules.impl.movement;

import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.UpdateEvent;
import dev.perish.api.utils.path.BaritoneUtil;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.impl.EnumSetting;

public class AutoWalk extends Module {
    public static AutoWalk INSTANCE;
    private final EnumSetting<AutoWalk.Mode> mode = this.add(new EnumSetting("Mode", AutoWalk.Mode.Forward));
    boolean start = false;

    public AutoWalk() {
        super("AutoWalk", Module.Category.Movement);
        this.setChinese("自动前进");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        this.start = false;
    }

    @Override
    public void onLogout() {
        this.disable();
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (this.mode.is(AutoWalk.Mode.Forward)) {
            mc.options.forwardKey.setPressed(true);
        } else if (this.mode.is(AutoWalk.Mode.Path)) {
            if (!this.start) {
                BaritoneUtil.forward();
                this.start = true;
            } else if (!BaritoneUtil.isActive()) {
                this.disable();
            }
        }
    }

    @Override
    public void onDisable() {
        BaritoneUtil.cancelEverything();
    }

    public boolean forward() {
        return this.isOn() && this.mode.is(AutoWalk.Mode.Forward);
    }

    public enum Mode {
        Forward,
        Path
    }
}
