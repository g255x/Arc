package dev.G255.Lumi.mod.gui.clickgui.pages;

import dev.G255.Lumi.Lumi;
import dev.G255.Lumi.api.utils.math.Timer;
import dev.G255.Lumi.api.utils.render.ColorUtil;
import dev.G255.Lumi.api.utils.render.Render2DUtil;
import dev.G255.Lumi.core.impl.FontManager;
import dev.G255.Lumi.mod.Mod;
import dev.G255.Lumi.mod.gui.clickgui.ClickGuiScreen;
import dev.G255.Lumi.mod.gui.items.Component;
import dev.G255.Lumi.mod.gui.items.Item;
import dev.G255.Lumi.mod.gui.items.buttons.ModuleButton;
import dev.G255.Lumi.mod.modules.HudModule;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.impl.client.ClickGui;
import dev.G255.Lumi.mod.modules.impl.client.hud.HudSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;

public final class ClickGuiModulePage {
    private final ClickGuiScreen host;
    private String searchText = "";
    private boolean searchActive;
    private final Timer cursorTimer = new Timer();
    private boolean cursorVisible = true;

    private static final int SEARCH_WIDTH = 90;
    private static final int SEARCH_HEIGHT = 14;

    public ClickGuiModulePage(ClickGuiScreen host) {
        this.host = host;
    }

    public void load() {
        ArrayList<Component> components = this.host.getComponents();
        components.clear();

        Map<Module.Category, List<Module>> byCategory = new HashMap<>();
        for (Module module : Lumi.MODULE.getModules()) {
            if (this.isHudComponentModule(module)) continue;
            byCategory.computeIfAbsent(module.getCategory(), k -> new ArrayList<>()).add(module);
        }

        int categoryWidth = ClickGui.getInstance() != null ? ClickGui.getInstance().categoryWidth.getValueInt() : 101;
        int moduleButtonWidth = ClickGui.getInstance() != null ? ClickGui.getInstance().moduleButtonWidth.getValueInt() : 93;
        int layoutWidth = Math.max(categoryWidth, moduleButtonWidth);
        int spacing = layoutWidth + 1;
        int count = Module.Category.values().length;
        int startX = 10;
        int startY = 4;
        if (dev.G255.Lumi.api.utils.Wrapper.mc != null && dev.G255.Lumi.api.utils.Wrapper.mc.getWindow() != null) {
            int screenWidth = dev.G255.Lumi.api.utils.Wrapper.mc.getWindow().getScaledWidth();
            int screenHeight = dev.G255.Lumi.api.utils.Wrapper.mc.getWindow().getScaledHeight();
            int totalWidth = count * layoutWidth + (count - 1);
            startX = Math.round(((float)screenWidth - (float)totalWidth) / 2.0f);
            startY = Math.round((float)screenHeight / 6.0f);
        }
        int offsetX = Math.round(((float)layoutWidth - (float)moduleButtonWidth) / 2.0f);
        int x = startX - spacing;
        for (Module.Category category : Module.Category.values()) {
            x += spacing;
            Component comp = new Component(category.toString(), category, x + offsetX, startY, true, true);
            List<Module> catModules = byCategory.get(category);
            if (catModules != null) {
                catModules.sort(Comparator.comparing(Mod::getName));
                for (Module module : catModules) {
                    comp.addButton(new ModuleButton(module));
                }
            }
            components.add(comp);
        }
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.host.getComponents().forEach(c -> c.drawScreen(context, mouseX, mouseY, delta));
        this.renderSearchBar(context, mouseX, mouseY);
    }

    private void renderSearchBar(DrawContext context, int mouseX, int mouseY) {
        ArrayList<Component> components = this.host.getComponents();
        if (components.isEmpty()) return;

        Component lastComp = components.get(components.size() - 1);
        int categoryWidth = ClickGui.getInstance() != null ? ClickGui.getInstance().categoryWidth.getValueInt() : 101;
        int barX = lastComp.getX() + categoryWidth + 6;
        int barY = lastComp.getY();

        boolean hovering = mouseX >= barX && mouseX <= barX + SEARCH_WIDTH && mouseY >= barY && mouseY <= barY + SEARCH_HEIGHT;

        int bgColor = this.searchActive || hovering
            ? new Color(40, 40, 40, 200).getRGB()
            : new Color(30, 30, 30, 160).getRGB();
        Render2DUtil.drawRect(context.getMatrices(), barX, barY, SEARCH_WIDTH, SEARCH_HEIGHT, bgColor);

        int borderColor = this.searchActive
            ? ColorUtil.injectAlpha(ClickGui.getInstance().getColor(barY / 10.0), 200).getRGB()
            : new Color(60, 60, 60, 180).getRGB();
        Render2DUtil.drawRectWithOutline(context.getMatrices(), barX, barY, SEARCH_WIDTH, SEARCH_HEIGHT,
            new Color(0, 0, 0, 0), new Color(borderColor));

        boolean useFont = FontManager.isCustomFontEnabled();
        boolean shadow = FontManager.isShadowEnabled();
        float fontHeight = useFont ? FontManager.ui.getFontHeight() : 9.0f;

        if (this.searchText.isEmpty() && !this.searchActive) {
            String placeholder = "Search...";
            float textWidth = useFont ? FontManager.ui.getWidth(placeholder) : dev.G255.Lumi.api.utils.Wrapper.mc.textRenderer.getWidth(placeholder);
            float tx = barX + (SEARCH_WIDTH - textWidth) / 2.0f;
            float ty = barY + (SEARCH_HEIGHT - fontHeight) / 2.0f;
            if (useFont) {
                FontManager.ui.drawString(context.getMatrices(), placeholder, tx, ty, 0x80AAAAAA, shadow);
            } else {
                context.drawText(dev.G255.Lumi.api.utils.Wrapper.mc.textRenderer, placeholder, (int)tx, (int)ty, 0x80AAAAAA, shadow);
            }
        } else {
            if (this.cursorTimer.passed(500L)) {
                this.cursorVisible = !this.cursorVisible;
                this.cursorTimer.reset();
            }
            String displayText = this.searchText + (this.searchActive && this.cursorVisible ? "_" : "");
            float textWidth = useFont ? FontManager.ui.getWidth(displayText) : dev.G255.Lumi.api.utils.Wrapper.mc.textRenderer.getWidth(displayText);
            float tx = barX + 3;
            float ty = barY + (SEARCH_HEIGHT - fontHeight) / 2.0f;

            int scrollOffset = 0;
            if (textWidth > SEARCH_WIDTH - 6) {
                scrollOffset = (int)(textWidth - SEARCH_WIDTH + 6);
            }

            context.enableScissor(barX + 2, barY, barX + SEARCH_WIDTH - 2, barY + SEARCH_HEIGHT);
            if (useFont) {
                FontManager.ui.drawString(context.getMatrices(), displayText, tx - scrollOffset, ty, -1, shadow);
            } else {
                context.drawText(dev.G255.Lumi.api.utils.Wrapper.mc.textRenderer, displayText, (int)(tx - scrollOffset), (int)ty, -1, shadow);
            }
            context.disableScissor();
        }
    }

    private int getSearchBarX() {
        ArrayList<Component> components = this.host.getComponents();
        if (components.isEmpty()) return 0;
        Component lastComp = components.get(components.size() - 1);
        int categoryWidth = ClickGui.getInstance() != null ? ClickGui.getInstance().categoryWidth.getValueInt() : 101;
        return lastComp.getX() + categoryWidth + 6;
    }

    private int getSearchBarY() {
        ArrayList<Component> components = this.host.getComponents();
        if (components.isEmpty()) return 0;
        return components.get(components.size() - 1).getY();
    }

    private boolean isSearchBarHovering(int mouseX, int mouseY) {
        int bx = this.getSearchBarX();
        int by = this.getSearchBarY();
        return mouseX >= bx && mouseX <= bx + SEARCH_WIDTH && mouseY >= by && mouseY <= by + SEARCH_HEIGHT;
    }

    private void applySearchFilter() {
        String query = this.searchText.toLowerCase().trim();
        ArrayList<Component> components = this.host.getComponents();
        for (Component comp : components) {
            for (ModuleButton btn : comp.getItems()) {
                if (query.isEmpty()) {
                    btn.setHidden(false);
                    continue;
                }
                Module mod = btn.getModule();
                String name = mod.getName().toLowerCase();
                String chinese = mod.getChinese();
                boolean matches = name.contains(query)
                    || (chinese != null && chinese.toLowerCase().contains(query));
                btn.setHidden(!matches);
            }
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (this.isSearchBarHovering(mouseX, mouseY)) {
            this.searchActive = true;
            this.cursorVisible = true;
            this.cursorTimer.reset();
            return;
        }
        this.searchActive = false;
        this.host.getComponents().forEach(c -> c.mouseClicked(mouseX, mouseY, mouseButton));
    }

    public void mouseReleased(int mouseX, int mouseY, int releaseButton) {
        this.host.getComponents().forEach(c -> c.mouseReleased(mouseX, mouseY, releaseButton));
    }

    public void keyPressed(int keyCode) {
        if (this.searchActive) {
            if (keyCode == 256) {
                this.searchActive = false;
                return;
            }
            if (keyCode == 259) {
                if (!this.searchText.isEmpty()) {
                    this.searchText = this.searchText.substring(0, this.searchText.length() - 1);
                    this.applySearchFilter();
                }
                return;
            }
            if (keyCode == 257 || keyCode == 335) {
                this.searchActive = false;
                return;
            }
        }
        this.host.getComponents().forEach(c -> c.onKeyPressed(keyCode));
    }

    public void charTyped(char chr, int modifiers) {
        if (this.searchActive) {
            if (chr >= ' ' && chr != '`') {
                this.searchText += chr;
                this.applySearchFilter();
            }
            return;
        }
        this.host.getComponents().forEach(c -> c.onKeyTyped(chr, modifiers));
    }

    public void mouseScrolled(double verticalAmount) {
        if (dev.G255.Lumi.api.utils.Wrapper.mc == null || dev.G255.Lumi.api.utils.Wrapper.mc.getWindow() == null) {
            return;
        }
        if (InputUtil.isKeyPressed(dev.G255.Lumi.api.utils.Wrapper.mc.getWindow().getHandle(), 340)) {
            if (verticalAmount < 0.0) {
                this.host.getComponents().forEach(component -> component.setX(component.getTargetX() - 15));
            } else if (verticalAmount > 0.0) {
                this.host.getComponents().forEach(component -> component.setX(component.getTargetX() + 15));
            }
        } else if (verticalAmount < 0.0) {
            this.host.getComponents().forEach(component -> component.setY(component.getTargetY() - 15));
        } else if (verticalAmount > 0.0) {
            this.host.getComponents().forEach(component -> component.setY(component.getTargetY() + 15));
        }
    }

    private boolean isHudComponentModule(Module module) {
        return module instanceof HudModule || module instanceof HudSetting;
    }
}