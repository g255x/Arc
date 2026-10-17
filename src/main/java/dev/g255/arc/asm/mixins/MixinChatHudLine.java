/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.hud.ChatHudLine
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 */
package dev.g255.arc.asm.mixins;

import dev.g255.arc.api.interfaces.IChatHudLineHook;
import dev.g255.arc.api.utils.math.FadeUtils;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value={ChatHudLine.class})
public class MixinChatHudLine
implements IChatHudLineHook {
    @Unique
    private int id = 0;
    @Unique
    private boolean sync = false;
    @Unique
    private FadeUtils fade;

    @Override
    public int arcClient$getMessageId() {
        return this.id;
    }

    @Override
    public void arcClient$setMessageId(int id) {
        this.id = id;
    }

    @Override
    public boolean arcClient$getSync() {
        return this.sync;
    }

    @Override
    public void arcClient$setSync(boolean sync) {
        this.sync = sync;
    }

    @Override
    public FadeUtils arcClient$getFade() {
        return this.fade;
    }

    @Override
    public void arcClient$setFade(FadeUtils fade) {
        this.fade = fade;
    }
}

