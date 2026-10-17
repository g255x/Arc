package dev.perish.mod.modules.impl.misc;

import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.SendMessageEvent;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.impl.StringSetting;

public class ChatAppend extends Module {
    public static ChatAppend INSTANCE;
    private final StringSetting message = this.add(new StringSetting("Text", "- Perish-Hyperion"));

    public ChatAppend() {
        super("ChatAppend", Module.Category.Misc);
        this.setChinese("消息后缀");
        INSTANCE = this;
    }

    @EventListener
    public void onSendMessage(SendMessageEvent event) {
        if (!nullCheck() && !event.isCancelled() && !AutoReconnect.inQueueServer) {
            String message = event.message;
            if (!message.startsWith("/")
                    && !message.startsWith("!")
                    && !message.startsWith("$")
                    && !message.startsWith("#")
                    && !message.endsWith(this.message.getValue())) {
                String suffix = this.message.getValue();
                message = message + " " + suffix;
                event.message = message;
            }
        }
    }
}
