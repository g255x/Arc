package dev.perish.core.impl;

import dev.perish.api.utils.player.MovementUtil;
import dev.perish.mod.modules.impl.movement.Speed;
import dev.perish.mod.modules.impl.player.TimerModule;

public class TimerManager {
    public float timer = 1.0F;
    public float lastTimer;

    public void set(float factor) {
        if (factor < 0.1F) {
            factor = 0.1F;
        }

        this.timer = factor;
    }

    public void reset() {
        this.timer = this.getDefault();
        this.lastTimer = this.timer;
    }

    public void tryReset() {
        if (this.lastTimer != this.getDefault()) {
            this.reset();
        }
    }

    public float get() {
        return this.timer;
    }

    public float getDefault() {
        if (TimerModule.INSTANCE.isOn()) {
            return TimerModule.INSTANCE.boostKey.isPressed() ? TimerModule.INSTANCE.boost.getValueFloat() : TimerModule.INSTANCE.multiplier.getValueFloat();
        } else {
            return 1.0f;
        }
    }

}

