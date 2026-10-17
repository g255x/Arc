package dev.kissoo.mod.gui;

import dev.kissoo.Forever;
import dev.kissoo.api.utils.render.Render2DUtil;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.impl.client.ClickGui;
import dev.kissoo.mod.modules.settings.Setting;
import dev.kissoo.mod.modules.settings.impl.BindSetting;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import dev.kissoo.mod.modules.settings.impl.StringSetting;
import hoprc.api.core.impl.FontManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class TrollClickGuiScreen extends Screen {
    private static final int WINDOW_W = 104;
    private static final int HEADER_H = 16;
    private static final int ROW_H = 13;
    private static final int SETTING_H = 12;
    private static final int MIN_WINDOW_H = 78;
    private static final int DEFAULT_WINDOW_H = 310;

    private final ClickGui owner;
    private final List<TrollWindow> windows = new ArrayList<>();
    private final Map<Module, Float> moduleAnim = new HashMap<>();
    private final Map<Setting, Float> settingAnim = new HashMap<>();
    private final Set<Module> expanded = new HashSet<>();

    private TrollWindow draggingWindow;
    private int dragOffsetX;
    private int dragOffsetY;
    private SliderSetting draggingSlider;
    private int draggingSliderX;
    private int draggingSliderW;
    private BindSetting listeningBind;
    private StringSetting activeString;
    private String search = "";
    private boolean searchFocused;
    private boolean closing;

    public TrollClickGuiScreen(ClickGui owner) {
        super(Text.literal("Troll ClickGui"));
        this.owner = owner;
        initWindows();
    }

    private void initWindows() {
        windows.clear();
        int x = 8;
        int y = 24;
        int spacing = 8;
        for (Module.Category category : Module.Category.values()) {
            TrollWindow window = new TrollWindow(category, x, y);
            windows.add(window);
            x += WINDOW_W + spacing;
            if (x + WINDOW_W > 820) {
                x = 8;
                y += 112;
            }
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackdrop(context);
        renderSearch(context, mouseX, mouseY);
        for (TrollWindow window : windows) {
            window.render(context, mouseX, mouseY);
        }
        if (listeningBind != null) {
            drawCenteredHint(context, "Press a key or mouse button for " + listeningBind.getName() + " | ESC / Delete = None");
        }
    }

    private void renderBackdrop(DrawContext context) {
        context.fill(0, 0, width, height, 0x7A050609);
        Render2DUtil.verticalGradient(context.getMatrices(), 0, 0, width, height,
                new Color(18, 20, 24, 140), new Color(3, 4, 6, 180));
        for (int i = 0; i < width; i += 42) {
            Render2DUtil.drawLine(context.getMatrices(), i, 0, i - height * 0.18f, height, 0x0FFFFFFF);
        }
    }

    private void renderSearch(DrawContext context, int mouseX, int mouseY) {
        int x = 8;
        int y = 6;
        int w = Math.min(230, width - 16);
        boolean hovered = hover(mouseX, mouseY, x, y, w, 15);
        Color bg = searchFocused ? new Color(28, 32, 42, 230) : hovered ? new Color(24, 27, 34, 220) : new Color(16, 18, 23, 205);
        Render2DUtil.drawRoundedRect(context.getMatrices(), x, y, w, 15, 4, bg);
        drawText(context, search.isEmpty() && !searchFocused ? "Search modules..." : search + (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : ""),
                x + 7, y + 4, search.isEmpty() ? 0xFF8B919D : 0xFFECEFF4, 7.0f);
        drawText(context, "Troll", width - 45, 9, 0xFF7AF0B8, 7.0f);
    }

    private void drawCenteredHint(DrawContext context, String text) {
        int tw = textWidth(text, 8.0f);
        int x = (width - tw) / 2;
        int y = height - 28;
        Render2DUtil.drawRoundedRect(context.getMatrices(), x - 10, y - 6, tw + 20, 20, 6, new Color(0, 0, 0, 180));
        drawText(context, text, x, y, 0xFFFFFFFF, 8.0f);
    }

    private boolean matches(Module module) {
        if (search.isBlank()) return true;
        String s = search.replace(" ", "").toLowerCase(Locale.ROOT);
        return module.getName().toLowerCase(Locale.ROOT).contains(s)
                || module.getDisplayName().toLowerCase(Locale.ROOT).contains(s)
                || (module.getChinese() != null && module.getChinese().toLowerCase(Locale.ROOT).contains(s));
    }

    private List<Module> modulesFor(Module.Category category) {
        ArrayList<Module> list = new ArrayList<>();
        for (Module module : Forever.MODULE.getModules()) {
            if (module.isHidden()) continue;
            if (module.getCategory() == category && matches(module)) {
                list.add(module);
            }
        }
        list.sort(Comparator.comparing(Module::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    private void drawText(DrawContext context, String text, float x, float y, int color, float size) {
        if (ClickGui.getInstance().font.getValue()) {
            FontManager.get(size).drawString(context.getMatrices(), text, x, y, color, false);
        } else {
            float scale = size / 8.0f;
            context.getMatrices().push();
            context.getMatrices().translate(x, y, 0);
            context.getMatrices().scale(scale, scale, 1.0f);
            context.drawText(client.textRenderer, text, 0, 0, color, false);
            context.getMatrices().pop();
        }
    }

    private int textWidth(String text, float size) {
        if (ClickGui.getInstance().font.getValue()) {
            return (int) FontManager.get(size).getWidth(text);
        }
        return (int) (client.textRenderer.getWidth(text) * (size / 8.0f));
    }

    private static boolean hover(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private <T> float anim(Map<T, Float> map, T key, boolean target) {
        float current = map.getOrDefault(key, target ? 1.0f : 0.0f);
        current += ((target ? 1.0f : 0.0f) - current) * 0.24f;
        if (current < 0.01f) current = 0.0f;
        if (current > 0.99f) current = 1.0f;
        map.put(key, current);
        return current;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (listeningBind != null) {
            if (button >= 0) {
                listeningBind.setValue(-button - 2);
                listeningBind = null;
                return true;
            }
        }
        if (hover(mouseX, mouseY, 8, 6, Math.min(230, width - 16), 15)) {
            searchFocused = true;
            return true;
        } else if (button == 0) {
            searchFocused = false;
        }
        for (int i = windows.size() - 1; i >= 0; i--) {
            if (windows.get(i).mouseClicked(mouseX, mouseY, button)) {
                TrollWindow w = windows.remove(i);
                windows.add(w);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingWindow = null;
        draggingSlider = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingWindow != null && button == 0) {
            draggingWindow.x = (int) mouseX - dragOffsetX;
            draggingWindow.y = (int) mouseY - dragOffsetY;
            draggingWindow.x = Math.max(0, Math.min(width - WINDOW_W, draggingWindow.x));
            draggingWindow.y = Math.max(0, Math.min(height - HEADER_H, draggingWindow.y));
            return true;
        }
        if (draggingSlider != null && button == 0) {
            setSliderFromMouse(draggingSlider, mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (int i = windows.size() - 1; i >= 0; i--) {
            TrollWindow window = windows.get(i);
            if (hover(mouseX, mouseY, window.x, window.y, WINDOW_W, window.height)) {
                window.scroll -= (int) (verticalAmount * 18.0);
                window.clampScroll();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (activeString != null) {
            if (!Character.isISOControl(chr)) {
                activeString.setValue(activeString.getValue() + chr);
                return true;
            }
        }
        if (searchFocused && !Character.isISOControl(chr)) {
            search += chr;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listeningBind != null) {
            listeningBind.setValue(keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE ? -1 : keyCode);
            listeningBind = null;
            return true;
        }
        if (activeString != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
                activeString = null;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                String value = activeString.getValue();
                if (!value.isEmpty()) activeString.setValue(value.substring(0, value.length() - 1));
                return true;
            }
        }
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (!closing) {
            closing = true;
            if (owner.isOn()) {
                owner.disable();
            }
        }
        super.close();
    }

    private void setSliderFromMouse(SliderSetting setting, double mouseX) {
        double percent = (mouseX - draggingSliderX) / Math.max(1.0, draggingSliderW);
        setting.setValue(setting.getMin() + setting.getRange() * percent);
    }

    private final class TrollWindow {
        private final Module.Category category;
        private int x;
        private int y;
        private int height = DEFAULT_WINDOW_H;
        private int scroll;

        private TrollWindow(Module.Category category, int x, int y) {
            this.category = category;
            this.x = x;
            this.y = y;
        }

        private void render(DrawContext context, int mouseX, int mouseY) {
            List<Module> modules = modulesFor(category);
            int contentHeight = contentHeight(modules);
            height = Math.min(DEFAULT_WINDOW_H, Math.max(MIN_WINDOW_H, contentHeight + HEADER_H + 6));
            clampScroll();

            Render2DUtil.drawRoundedRect(context.getMatrices(), x, y, WINDOW_W, height, 5, new Color(9, 10, 13, 222));
            Render2DUtil.drawRoundedRect(context.getMatrices(), x, y, WINDOW_W, HEADER_H, 5, new Color(24, 27, 34, 238));
            Render2DUtil.drawRect(context.getMatrices(), x, y + HEADER_H - 1, WINDOW_W, 1, new Color(122, 240, 184, 155));
            drawText(context, category.name(), x + 6, y + 5, 0xFFECEFF4, 7.0f);
            drawText(context, String.valueOf(modules.size()), x + WINDOW_W - 15, y + 5, 0xFF7AF0B8, 7.0f);

            int sx = x;
            int sy = y + HEADER_H;
            int sw = WINDOW_W;
            int sh = height - HEADER_H;
            context.enableScissor(sx, sy, sx + sw, sy + sh);
            int cy = sy + 3 - scroll;
            for (Module module : modules) {
                cy = renderModule(context, module, x + 3, cy, WINDOW_W - 6, mouseX, mouseY);
            }
            context.disableScissor();

            if (contentHeight > height - HEADER_H) {
                float visible = (float) (height - HEADER_H) / contentHeight;
                int barH = Math.max(18, (int) ((height - HEADER_H) * visible));
                int max = Math.max(1, contentHeight - (height - HEADER_H));
                int barY = y + HEADER_H + (int) ((height - HEADER_H - barH) * (scroll / (float) max));
                Render2DUtil.drawRoundedRect(context.getMatrices(), x + WINDOW_W - 3, barY, 2, barH, 1, new Color(122, 240, 184, 120));
            }
        }

        private int renderModule(DrawContext context, Module module, int rx, int cy, int rw, int mouseX, int mouseY) {
            boolean hovered = hover(mouseX, mouseY, rx, cy, rw, ROW_H);
            boolean on = module.isOn();
            float a = anim(moduleAnim, module, hovered || on);
            Color bg = on
                    ? new Color(48, 96, 78, 120 + (int) (60 * a))
                    : new Color(255, 255, 255, hovered ? 24 : 8);
            Render2DUtil.drawRoundedRect(context.getMatrices(), rx, cy, rw, ROW_H, 3, bg);
            int textColor = on ? 0xFF7AF0B8 : (hovered ? 0xFFFFFFFF : 0xFFC7CDD7);
            drawText(context, module.getDisplayName(), rx + 5, cy + 3, textColor, 7.0f);
            drawText(context, expanded.contains(module) ? "-" : "+", rx + rw - 9, cy + 3, 0xFF8B919D, 7.0f);
            cy += ROW_H + 2;

            if (expanded.contains(module)) {
                for (Setting setting : module.getSettings()) {
                    if (!setting.isVisible()) continue;
                    cy = renderSetting(context, setting, rx + 6, cy, rw - 12, mouseX, mouseY);
                }
                cy += 2;
            }
            return cy;
        }

        private int renderSetting(DrawContext context, Setting setting, int rx, int cy, int rw, int mouseX, int mouseY) {
            boolean hovered = hover(mouseX, mouseY, rx, cy, rw, SETTING_H);
            float a = anim(settingAnim, setting, hovered);
            Render2DUtil.drawRoundedRect(context.getMatrices(), rx, cy, rw, SETTING_H, 2, new Color(255, 255, 255, 8 + (int) (18 * a)));

            if (setting instanceof BooleanSetting bool) {
                drawText(context, setting.getName(), rx + 3, cy + 3, bool.getValue() ? 0xFF7AF0B8 : 0xFFADB4C0, 6.5f);
                Render2DUtil.drawRoundedRect(context.getMatrices(), rx + rw - 14, cy + 3, 9, 6, 3, bool.getValue() ? new Color(122, 240, 184) : new Color(72, 76, 86));
            } else if (setting instanceof SliderSetting slider) {
                drawText(context, setting.getName(), rx + 3, cy + 3, 0xFFADB4C0, 6.5f);
                String value = String.format(Locale.ROOT, "%.2f%s", slider.getValue(), slider.getSuffix());
                drawText(context, value, rx + rw - textWidth(value, 6.5f) - 3, cy + 3, 0xFFECEFF4, 6.5f);
                int bx = rx + 3;
                int by = cy + SETTING_H - 2;
                int bw = rw - 6;
                float fill = (float) ((slider.getValue() - slider.getMin()) / Math.max(0.0001, slider.getRange()));
                Render2DUtil.drawRect(context.getMatrices(), bx, by, bw, 1, new Color(60, 64, 74));
                Render2DUtil.drawRect(context.getMatrices(), bx, by, bw * Math.max(0.0f, Math.min(1.0f, fill)), 1, new Color(122, 240, 184));
            } else if (setting instanceof EnumSetting<?> en) {
                String value = en.getValue().name();
                drawText(context, setting.getName(), rx + 3, cy + 3, 0xFFADB4C0, 6.5f);
                drawText(context, value, rx + rw - textWidth(value, 6.5f) - 3, cy + 3, 0xFFECEFF4, 6.5f);
            } else if (setting instanceof BindSetting bind) {
                String value = listeningBind == bind ? "..." : bind.getKeyString();
                drawText(context, setting.getName(), rx + 3, cy + 3, 0xFFADB4C0, 6.5f);
                drawText(context, value, rx + rw - textWidth(value, 6.5f) - 3, cy + 3, listeningBind == bind ? 0xFF7AF0B8 : 0xFFECEFF4, 6.5f);
            } else if (setting instanceof StringSetting str) {
                String value = activeString == str ? str.getValue() + "_" : str.getValue();
                drawText(context, setting.getName(), rx + 3, cy + 3, 0xFFADB4C0, 6.5f);
                drawText(context, value, rx + rw - Math.min(rw / 2, textWidth(value, 6.5f)) - 3, cy + 3, activeString == str ? 0xFF7AF0B8 : 0xFFECEFF4, 6.5f);
            } else if (setting instanceof ColorSetting color) {
                drawText(context, setting.getName(), rx + 3, cy + 3, 0xFFADB4C0, 6.5f);
                Render2DUtil.drawRoundedRect(context.getMatrices(), rx + rw - 14, cy + 2, 10, 8, 2, color.getValue());
            } else {
                drawText(context, setting.getName(), rx + 3, cy + 3, 0xFFADB4C0, 6.5f);
            }
            return cy + SETTING_H + 1;
        }

        private boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (hover(mouseX, mouseY, x, y, WINDOW_W, HEADER_H)) {
                if (button == 0) {
                    draggingWindow = this;
                    dragOffsetX = (int) mouseX - x;
                    dragOffsetY = (int) mouseY - y;
                    return true;
                }
            }

            if (!hover(mouseX, mouseY, x, y + HEADER_H, WINDOW_W, height - HEADER_H)) {
                return false;
            }

            int cy = y + HEADER_H + 3 - scroll;
            for (Module module : modulesFor(category)) {
                if (hover(mouseX, mouseY, x + 3, cy, WINDOW_W - 6, ROW_H)) {
                    if (button == 0) module.toggle();
                    if (button == 1) {
                        if (expanded.contains(module)) expanded.remove(module);
                        else expanded.add(module);
                    }
                    return true;
                }
                cy += ROW_H + 2;
                if (expanded.contains(module)) {
                    for (Setting setting : module.getSettings()) {
                        if (!setting.isVisible()) continue;
                        if (hover(mouseX, mouseY, x + 9, cy, WINDOW_W - 18, SETTING_H)) {
                            clickSetting(setting, mouseX, button, x + 12, WINDOW_W - 24);
                            return true;
                        }
                        cy += SETTING_H + 1;
                    }
                    cy += 2;
                }
            }
            return true;
        }

        private void clickSetting(Setting setting, double mouseX, int button, int sliderX, int sliderW) {
            if (setting instanceof BooleanSetting bool) {
                if (button == 0) bool.setValue(!bool.getValue());
                else if (button == 1 && bool.hasParent()) bool.setOpen(!bool.isOpen());
            } else if (setting instanceof SliderSetting slider) {
                if (button == 0) {
                    draggingSlider = slider;
                    draggingSliderX = sliderX;
                    draggingSliderW = sliderW;
                    setSliderFromMouse(slider, mouseX);
                }
            } else if (setting instanceof EnumSetting<?> en) {
                if (button == 0 || button == 1) en.increaseEnum();
            } else if (setting instanceof BindSetting bind) {
                if (button == 0) listeningBind = bind;
                if (button == 1) bind.setValue(-1);
            } else if (setting instanceof StringSetting str) {
                if (button == 0) activeString = str;
                if (button == 1) str.setValue("");
            } else if (setting instanceof ColorSetting color) {
                if (button == 0 && color.injectBoolean) color.booleanValue = !color.booleanValue;
                if (button == 1) color.setSync(!color.sync);
            }
        }

        private int contentHeight(List<Module> modules) {
            int h = 6;
            for (Module module : modules) {
                h += ROW_H + 2;
                if (expanded.contains(module)) {
                    for (Setting setting : module.getSettings()) {
                        if (setting.isVisible()) h += SETTING_H + 1;
                    }
                    h += 2;
                }
            }
            return h;
        }

        private void clampScroll() {
            int max = Math.max(0, contentHeight(modulesFor(category)) - (height - HEADER_H));
            scroll = Math.max(0, Math.min(max, scroll));
        }
    }
}
