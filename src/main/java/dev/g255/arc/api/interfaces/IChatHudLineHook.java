/*
 * Decompiled with CFR 0.152.
 */
package dev.g255.arc.api.interfaces;

import dev.g255.arc.api.utils.math.FadeUtils;

public interface IChatHudLineHook {
    public int arcClient$getMessageId();

    public void arcClient$setMessageId(int var1);

    public boolean arcClient$getSync();

    public void arcClient$setSync(boolean var1);

    public FadeUtils arcClient$getFade();

    public void arcClient$setFade(FadeUtils var1);
}

