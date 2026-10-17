/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.text.Text
 */
package dev.g255.arc.api.interfaces;

import net.minecraft.text.Text;

public interface IChatHudHook {
    public void arcClient$addMessage(Text var1, int var2);

    public void arcClient$addMessage(Text var1);

    public void arcClient$addMessageOutSync(Text var1, int var2);
}
