/*
 * Decompiled with CFR 0.152.
 */
package dev.G255.Lumi.mod.modules.impl.movement;

import dev.G255.Lumi.mod.modules.Module;

public class EntityControl
        extends Module {
    public static EntityControl INSTANCE;

    public EntityControl() {
        super("EntityControl", Module.Category.Movement);
        this.setChinese("");
        INSTANCE = this;
    }
}