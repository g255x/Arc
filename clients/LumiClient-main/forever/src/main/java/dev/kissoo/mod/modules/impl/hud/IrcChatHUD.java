package dev.kissoo.mod.modules.impl.hud;

import dev.kissoo.api.utils.render.TextUtil;
import dev.kissoo.mod.modules.HudElement;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import hoprc.api.core.impl.FontManager;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IrcChatHUD extends Module implements HudElement {
    public static IrcChatHUD INSTANCE;

    private static final int MAX_MESSAGES = 300;
    private static final int TITLE_H = 18;
    private static final int PAD_X = 6;
    private static final int PAD_Y = 5;
    private static final int SCROLL_W = 3;

    private static final List<String> messages = Collections.synchronizedList(new ArrayList<>());

    private int hudX = 8;
    private int hudY = 120;
    private AlignMode alignMode = AlignMode.LEFT;

    public final SliderSetting width = add(new SliderSetting("Width", 260, 120, 640, 1));
    public final SliderSetting height = add(new SliderSetting("Height", 120, 50, 360, 1));
    public final SliderSetting scroll = add(new SliderSetting("Scroll", 0, 0, MAX_MESSAGES, 1));
    private final BooleanSetting shadow = add(new BooleanSetting("Shadow", true));

    public IrcChatHUD() {
        super("IrcChatHUD", Category.HUD);
        setChinese("IrcChatHUD");
        INSTANCE = this;
    }

    public static boolean routeMessage(String text) {
        if (INSTANCE == null || !INSTANCE.isOn()) {
            return false;
        }
        INSTANCE.addIrcLine(text);
        return true;
    }

    private void addIrcLine(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        synchronized (messages) {
            messages.add(text);
            while (messages.size() > MAX_MESSAGES) {
                messages.remove(0);
            }
        }
        scroll.setValue(0);
    }

    public void scrollBy(double wheelAmount) {
        if (wheelAmount == 0.0) {
            return;
        }
        int step = wheelAmount > 0.0 ? -3 : 3;
        scroll.setValue(scroll.getValue() + step);
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        renderElement(drawContext, tickDelta);
    }

    @Override
    public String getElementName() {
        return getChinese() != null ? getChinese() : getName();
    }

    @Override
    public int getElementX() {
        if (alignMode == AlignMode.RIGHT) {
            return hudX - getElementWidth();
        }
        return hudX;
    }

    @Override
    public void setElementX(int x) {
        if (alignMode == AlignMode.RIGHT) {
            hudX = x + getElementWidth();
        } else {
            hudX = x;
        }
    }

    @Override
    public int getElementY() {
        return hudY;
    }

    @Override
    public void setElementY(int y) {
        hudY = y;
    }

    @Override
    public int getElementWidth() {
        return width.getValueInt();
    }

    @Override
    public int getElementHeight() {
        return height.getValueInt();
    }

    @Override
    public boolean isElementVisible() {
        return isOn();
    }

    @Override
    public AlignMode getAlignMode() {
        return alignMode;
    }

    @Override
    public void setAlignMode(AlignMode mode) {
        if (mode == alignMode) {
            return;
        }
        if (mode == AlignMode.RIGHT && alignMode == AlignMode.LEFT) {
            hudX = hudX + getElementWidth();
        } else if (mode == AlignMode.LEFT && alignMode == AlignMode.RIGHT) {
            hudX = hudX - getElementWidth();
        }
        alignMode = mode;
    }

    @Override
    public int getRightAnchorX() {
        if (alignMode == AlignMode.RIGHT) {
            return hudX;
        }
        return hudX + getElementWidth();
    }

    @Override
    public void setRightAnchorX(int rightX) {
        if (alignMode == AlignMode.RIGHT) {
            hudX = rightX;
        } else {
            hudX = rightX - getElementWidth();
        }
    }

    @Override
    public void renderElement(DrawContext context, float tickDelta) {
        if (!isOn()) {
            return;
        }

        int x = getElementX();
        int y = getElementY();
        int w = getElementWidth();
        int h = getElementHeight();
        int bodyY = y + TITLE_H;
        int bodyH = Math.max(0, h - TITLE_H);

        context.fill(x, y, x + w, y + TITLE_H, 0xA8000000);
        context.fill(x, bodyY, x + w, y + h, 0x5A000000);
        context.fill(x, y, x + w, y + 1, 0x28FFFFFF);
        context.fill(x, y + h - 1, x + w, y + h, 0x18000000);

        TextUtil.drawString(context, "IRC Chat", x + PAD_X, y + 5, 0xFFFFFFFF, shadow.getValue());

        List<String> wrapped = collectWrappedLines(Math.max(30, w - PAD_X * 2 - SCROLL_W - 4));
        int lineH = Math.max(9, (int) FontManager.getFontHeight() + 1);
        int viewportH = Math.max(0, bodyH - PAD_Y * 2);
        int visibleLines = Math.max(1, viewportH / lineH);
        int maxOffset = Math.max(0, wrapped.size() - visibleLines);
        int offset = Math.min(scroll.getValueInt(), maxOffset);
        if (scroll.getValueInt() != offset) {
            scroll.setValue(offset);
        }

        int start = Math.max(0, wrapped.size() - visibleLines - offset);
        int end = Math.min(wrapped.size(), start + visibleLines);
        int textX = x + PAD_X;
        int textY = bodyY + PAD_Y;

        context.enableScissor(x, bodyY, x + w, y + h);
        if (wrapped.isEmpty()) {
            TextUtil.drawString(context, "\u00a77No IRC messages", textX, textY, 0xFFFFFFFF, shadow.getValue());
        } else {
            for (int i = start; i < end; i++) {
                TextUtil.drawString(context, wrapped.get(i), textX, textY, 0xFFFFFFFF, shadow.getValue());
                textY += lineH;
            }
        }
        context.disableScissor();

        drawScrollBar(context, x, bodyY, w, bodyH, visibleLines, wrapped.size(), offset, maxOffset);
    }

    private List<String> collectWrappedLines(int maxWidth) {
        List<String> snapshot;
        synchronized (messages) {
            snapshot = new ArrayList<>(messages);
        }

        List<String> out = new ArrayList<>();
        for (String msg : snapshot) {
            out.addAll(wrap(msg, maxWidth));
        }
        return out;
    }

    private List<String> wrap(String text, int maxWidth) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        String activeColor = "";

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00a7' && i + 1 < text.length()) {
                char code = text.charAt(++i);
                line.append(c).append(code);
                activeColor = "" + c + code;
                continue;
            }
            if (c == '\n') {
                flushWrappedLine(out, line);
                line = new StringBuilder(activeColor);
                continue;
            }

            String candidate = line.toString() + c;
            if (FontManager.getWidth(candidate) > maxWidth && visibleLength(line) > 0) {
                flushWrappedLine(out, line);
                line = new StringBuilder(activeColor);
            }
            line.append(c);
        }

        flushWrappedLine(out, line);
        return out;
    }

    private void flushWrappedLine(List<String> out, StringBuilder line) {
        if (line.length() == 0) {
            out.add("");
            return;
        }
        out.add(line.toString());
    }

    private int visibleLength(StringBuilder line) {
        int count = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == '\u00a7' && i + 1 < line.length()) {
                i++;
                continue;
            }
            count++;
        }
        return count;
    }

    private void drawScrollBar(
            DrawContext context,
            int x,
            int bodyY,
            int w,
            int bodyH,
            int visibleLines,
            int totalLines,
            int offset,
            int maxOffset
    ) {
        if (totalLines <= visibleLines || bodyH <= PAD_Y * 2 + 8) {
            return;
        }

        int trackX = x + w - SCROLL_W - 3;
        int trackY = bodyY + PAD_Y;
        int trackH = bodyH - PAD_Y * 2;
        int thumbH = Math.max(12, Math.min(28, (int) (trackH * (visibleLines / (float) totalLines))));
        float progress = maxOffset <= 0 ? 1.0f : 1.0f - (offset / (float) maxOffset);
        int thumbY = trackY + Math.round((trackH - thumbH) * progress);

        context.fill(trackX, thumbY, trackX + SCROLL_W, thumbY + thumbH, 0xDFFFFFFF);
    }
}
