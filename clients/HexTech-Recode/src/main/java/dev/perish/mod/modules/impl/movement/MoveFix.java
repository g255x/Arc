package dev.perish.mod.modules.impl.movement;

import dev.perish.mod.modules.Module;

public class MoveFix extends Module {
    public static MoveFix INSTANCE;

    public MoveFix() {
        super("MoveFix", Module.Category.Movement);
        this.setChinese("移动同步");
        INSTANCE = this;
    }
}
