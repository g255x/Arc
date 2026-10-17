/*
 * Decompiled with CFR 0.152.
 */
package dev.G255.Lumi.mod.modules.impl.misc;

import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.SendMessageEvent;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.settings.impl.StringSetting;

public class ChatAppend
extends Module {
    public static ChatAppend INSTANCE;
    private final StringSetting message = this.add(new StringSetting("Text", "lumi"));

    public ChatAppend() {
        super("ChatAppend", Module.Category.Misc);
        this.setChinese("\u6d88\u606f\u540e\u7f00");
        INSTANCE = this;
    }

    @EventListener
    public void onSendMessage(SendMessageEvent event) {
        if (ChatAppend.nullCheck() || event.isCancelled() || AutoReconnect.inQueueServer) {
            return;
        }
        Object message = event.message;
        if (((String)message).startsWith("/") || ((String)message).startsWith("!") || ((String)message).startsWith("$") || ((String)message).startsWith("#") || ((String)message).endsWith(this.message.getValue())) {
            return;
        }
        String suffix = this.message.getValue();
        event.message = (String)message + " " + suffix;
    }
}
