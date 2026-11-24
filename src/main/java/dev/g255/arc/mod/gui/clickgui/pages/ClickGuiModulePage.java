package dev.g255.arc.mod.gui.clickgui.pages;

import dev.g255.arc.Arc;
import dev.g255.arc.mod.Mod;
import dev.g255.arc.mod.gui.GuiSearch;
import dev.g255.arc.mod.gui.GuiTooltip;
import dev.g255.arc.mod.gui.clickgui.ClickGuiFrame;
import dev.g255.arc.mod.gui.clickgui.ClickGuiScreen;
import dev.g255.arc.mod.gui.items.Component;
import dev.g255.arc.mod.gui.items.buttons.ModuleButton;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.impl.client.ClickGui;
import dev.g255.arc.mod.modules.impl.client.HUD;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;

public final class ClickGuiModulePage {
    private final ClickGuiScreen host;

    public ClickGuiModulePage(ClickGuiScreen host) {
        this.host = host;
    }

    public void load() {
        ArrayList<Component> components = this.host.getComponents();
        components.clear();

        int categoryWidth = ClickGui.getInstance() != null ? ClickGui.getInstance().categoryWidth.getValueInt() : 101;
        int moduleButtonWidth = ClickGui.getInstance() != null ? ClickGui.getInstance().moduleButtonWidth.getValueInt() : 93;
        int layoutWidth = Math.max(categoryWidth, moduleButtonWidth);
        int spacing = layoutWidth + 1;
        int count = Module.Category.values().length - 1;
        int startX = 10;
        int startY = 4;
        if (dev.g255.arc.api.utils.Wrapper.mc != null && dev.g255.arc.api.utils.Wrapper.mc.getWindow() != null) {
            int screenWidth = dev.g255.arc.api.utils.Wrapper.mc.getWindow().getScaledWidth();
            int screenHeight = dev.g255.arc.api.utils.Wrapper.mc.getWindow().getScaledHeight();
            int totalWidth = count * layoutWidth + (count - 1);
            startX = Math.round(((float)screenWidth - (float)totalWidth) / 2.0f);
            startY = Math.round((float)screenHeight / 6.0f);
        }
        int offsetX = Math.round(((float)layoutWidth - (float)moduleButtonWidth) / 2.0f);
        int x = startX - spacing;
        for (final Module.Category category : Module.Category.values()) {
            // 界面分类已经并入 HUD 页，主页面不再单独留一栏
            if (category == Module.Category.HUD) {
                continue;
            }
            x += spacing;
            components.add(new Component(category.toString(), category, x + offsetX, startY, true) {
                @Override
                public void setupItems() {
                    for (Module module : Arc.MODULE.getModules()) {
                        if (!module.getCategory().equals(category)) {
                            continue;
                        }
                        if (ClickGuiModulePage.this.isHudComponentModule(module)) {
                            continue;
                        }
                        // 界面（HUD）那一栏整体搬到 HUD 页，两处不再各挂一份
                        if (module.getCategory() == Module.Category.HUD) {
                            continue;
                        }
                        this.addButton(new ModuleButton(module));
                    }
                }
            });
        }
        components.forEach(c -> c.getItems().sort(Comparator.comparing(Mod::getName)));
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.host.getComponents().forEach(c -> c.drawScreen(context, mouseX, mouseY, delta));
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        this.host.getComponents().forEach(c -> c.mouseClicked(mouseX, mouseY, mouseButton));
    }

    public void mouseReleased(int mouseX, int mouseY, int releaseButton) {
        this.host.getComponents().forEach(c -> c.mouseReleased(mouseX, mouseY, releaseButton));
    }

    public void keyPressed(int keyCode) {
        this.host.getComponents().forEach(c -> c.onKeyPressed(keyCode));
    }

    public void charTyped(char chr, int modifiers) {
        this.host.getComponents().forEach(c -> c.onKeyTyped(chr, modifiers));
    }

    public void mouseScrolled(double verticalAmount) {
        if (dev.g255.arc.api.utils.Wrapper.mc == null || dev.g255.arc.api.utils.Wrapper.mc.getWindow() == null) {
            return;
        }
        if (InputUtil.isKeyPressed(dev.g255.arc.api.utils.Wrapper.mc.getWindow().getHandle(), 340)) {
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
        return module instanceof HUD;
    }


    /** 悬停介绍：交给统一的提示框绘制（模块行 + 展开后的设置项都覆盖） */
    public void drawTooltip(DrawContext context, ClickGuiFrame frame, int mouseX, int mouseY) {
        for (Component component : this.host.getComponents()) {
            GuiTooltip.drawForComponent(context, frame, this.host, component, mouseX, mouseY);
        }
    }

}
