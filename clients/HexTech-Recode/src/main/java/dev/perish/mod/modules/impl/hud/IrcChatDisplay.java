package dev.perish.mod.modules.impl.hud;

import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class IrcChatDisplay extends Module {
    public static IrcChatDisplay INSTANCE;

    private static final int MAX_MESSAGES = 300;
    private static final List<String> MESSAGES = Collections.synchronizedList(new ArrayList<>());

    public final SliderSetting x = add(new SliderSetting("X", 8, 0, 2000, 1));
    public final SliderSetting y = add(new SliderSetting("Y", 120, 0, 2000, 1));
    public final SliderSetting width = add(new SliderSetting("Width", 260, 120, 640, 1));
    public final SliderSetting height = add(new SliderSetting("Height", 120, 50, 360, 1));
    public final SliderSetting scroll = add(new SliderSetting("Scroll", 0, 0, MAX_MESSAGES, 1));
    private final BooleanSetting shadow = add(new BooleanSetting("Shadow", true));

    public IrcChatDisplay() {
        super("IrcChatHUD", Category.Hud);
        setChinese("IrcChatHUD");
        INSTANCE = this;
    }

    public static boolean routeMessage(String text) {
        if (INSTANCE == null || !INSTANCE.isOn()) {
            return false;
        }
        INSTANCE.addLine(text);
        return true;
    }

    private void addLine(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        synchronized (MESSAGES) {
            MESSAGES.add(text);
            while (MESSAGES.size() > MAX_MESSAGES) {
                MESSAGES.remove(0);
            }
        }
        scroll.setValue(0);
    }

    @Override
    public void onRender2D(DrawContext context, float tickDelta) {
        if (!isOn()) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        int px = x.getValueInt();
        int py = y.getValueInt();
        int w = width.getValueInt();
        int h = height.getValueInt();
        int titleH = 18;

        context.fill(px, py, px + w, py + titleH, 0xA8000000);
        context.fill(px, py + titleH, px + w, py + h, 0x5A000000);
        context.drawText(mc.textRenderer, "IRC Chat", px + 6, py + 5, 0xFFFFFFFF, shadow.getValue());

        List<String> snapshot;
        synchronized (MESSAGES) {
            snapshot = new ArrayList<>(MESSAGES);
        }
        int lineH = mc.textRenderer.fontHeight + 2;
        int visible = Math.max(1, (h - titleH - 10) / lineH);
        int maxOffset = Math.max(0, snapshot.size() - visible);
        int offset = Math.min(scroll.getValueInt(), maxOffset);
        if (scroll.getValueInt() != offset) {
            scroll.setValue(offset);
        }
        int start = Math.max(0, snapshot.size() - visible - offset);
        int end = Math.min(snapshot.size(), start + visible);
        int ty = py + titleH + 5;
        context.enableScissor(px, py + titleH, px + w, py + h);
        if (snapshot.isEmpty()) {
            context.drawText(mc.textRenderer, "No IRC messages", px + 6, ty, 0xFFAAAAAA, shadow.getValue());
        } else {
            for (int i = start; i < end; i++) {
                context.drawText(mc.textRenderer, snapshot.get(i), px + 6, ty, 0xFFFFFFFF, shadow.getValue());
                ty += lineH;
            }
        }
        context.disableScissor();

        if (snapshot.size() > visible) {
            int trackH = h - titleH - 10;
            int thumbH = Math.max(12, Math.min(28, (int) (trackH * (visible / (float) snapshot.size()))));
            float progress = maxOffset <= 0 ? 1.0f : 1.0f - offset / (float) maxOffset;
            int thumbY = py + titleH + 5 + Math.round((trackH - thumbH) * progress);
            context.fill(px + w - 6, thumbY, px + w - 3, thumbY + thumbH, 0xDFFFFFFF);
        }
    }
}
