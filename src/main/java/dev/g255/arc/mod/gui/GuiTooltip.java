package dev.g255.arc.mod.gui;

import dev.g255.arc.api.utils.render.ColorUtil;
import dev.g255.arc.api.utils.render.Render2DUtil;
import dev.g255.arc.api.utils.render.TextUtil;
import dev.g255.arc.core.impl.FontManager;
import dev.g255.arc.mod.gui.clickgui.ClickGuiFrame;
import dev.g255.arc.mod.gui.clickgui.ClickGuiScreen;
import dev.g255.arc.mod.gui.items.Component;
import dev.g255.arc.mod.gui.items.Item;
import dev.g255.arc.mod.gui.items.buttons.ModuleButton;
import dev.g255.arc.mod.modules.impl.client.ClickGui;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;

/**
 * 悬停说明框。
 *
 * <p>模块行显示模块介绍，展开后的设置项显示该设置的说明（数据来自 ModuleDesc / SettingDesc）。
 * 主页面和 HUD（界面）页共用一套，保证两边看到的说明是同一份。</p>
 */
public final class GuiTooltip {
    private static final int MAX_WIDTH = 190;

    private GuiTooltip() {
    }

    /** 给一整栏做命中：设置项优先于模块行（设置项画在下面、更靠鼠标） */
    public static void drawForComponent(DrawContext context, ClickGuiFrame frame, ClickGuiScreen host,
                                        Component component, int mouseX, int mouseY) {
        if (component == null || !component.isOpen()) {
            return;
        }
        float mx = frame.unitMouseX(mouseX);
        float my = frame.unitMouseY(mouseY);
        for (ModuleButton button : component.getItems()) {
            if (button.isHidden() || !GuiSearch.matches(button.getModule())) {
                continue;
            }
            if (button.subOpen) {
                for (Item item : button.getSettingsItems()) {
                    if (item.isHidden() || item.getVisibleHeight() <= 1.0) {
                        continue;
                    }
                    if (mx < item.getX() || mx > item.getX() + item.getWidth()) {
                        continue;
                    }
                    if (my < item.getY() || my > item.getY() + item.getVisibleHeight()) {
                        continue;
                    }
                    String tip = item.getTooltip();
                    if (tip != null && !tip.isEmpty()) {
                        drawText(context, frame, host, tip, mx, my);
                        return;
                    }
                }
            }
            if (mx >= button.getX() && mx <= button.getX() + button.getWidth()
                    && my >= button.getY() && my <= button.getY() + button.getButtonHeight()) {
                String tip = button.getTooltip();
                if (tip != null && !tip.isEmpty()) {
                    drawText(context, frame, host, tip, mx, my);
                    return;
                }
            }
        }
    }

    public static void drawText(DrawContext context, ClickGuiFrame frame, ClickGuiScreen host,
                                String text, float mx, float my) {
        ClickGui gui = ClickGui.getInstance();
        if (gui == null) {
            return;
        }
        boolean customFont = FontManager.isCustomFontEnabled();
        boolean shadow = FontManager.isShadowEnabled();
        List<String> lines = wrap(host, text, MAX_WIDTH);
        float lineHeight = customFont ? FontManager.ui.getFontHeight() + 1.0f : 10.0f;
        float textW = 0.0f;
        for (String line : lines) {
            textW = Math.max(textW, (float)host.getTextWidth(line));
        }
        float pad = 5.0f;
        float boxW = textW + pad * 2.0f;
        float boxH = (float)lines.size() * lineHeight + pad * 2.0f - 2.0f;
        float x = mx + 10.0f;
        float y = my + 12.0f;
        float maxW = frame.scale == 0.0f ? (float)frame.screenW : (float)frame.screenW / frame.scale;
        float maxH = frame.scale == 0.0f ? (float)frame.screenH : ((float)frame.screenH - frame.slideY) / frame.scale;
        if (x + boxW > maxW - 4.0f) {
            x = mx - boxW - 6.0f;
        }
        if (y + boxH > maxH - 4.0f) {
            y = my - boxH - 6.0f;
        }
        x = Math.max(4.0f, x);
        y = Math.max(4.0f, y);
        Render2DUtil.drawRectWithOutline(context.getMatrices(), x, y, boxW, boxH,
                ColorUtil.injectAlpha(gui.defaultColor.getValue(), 248),
                ColorUtil.injectAlpha(gui.hoverColor.getValue(), 255));
        float ty = y + pad - 1.0f;
        for (String line : lines) {
            TextUtil.drawString(context, line, (double)(x + pad), (double)ty,
                    gui.enableTextColor.getValue().getRGB(), customFont, shadow);
            ty += lineHeight;
        }
    }

    /** 按像素宽度断行（中文没有空格，只能逐字量） */
    private static List<String> wrap(ClickGuiScreen host, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\n') {
                lines.add(line.toString());
                line.setLength(0);
                continue;
            }
            line.append(ch);
            if (host.getTextWidth(line.toString()) <= maxWidth) {
                continue;
            }
            int last = line.length() - 1;
            String overflow = line.substring(last);
            line.setLength(last);
            if (line.length() > 0) {
                lines.add(line.toString());
            }
            line.setLength(0);
            line.append(overflow);
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }
}
