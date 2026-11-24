package dev.g255.arc.mod.gui.clickgui.pages;

import dev.g255.arc.Arc;
import dev.g255.arc.api.utils.Wrapper;
import dev.g255.arc.api.utils.render.ColorUtil;
import dev.g255.arc.api.utils.render.Render2DUtil;
import dev.g255.arc.api.utils.render.TextUtil;
import dev.g255.arc.core.impl.FontManager;
import dev.g255.arc.mod.Mod;
import dev.g255.arc.mod.gui.Alignment;
import dev.g255.arc.mod.gui.GuiSearch;
import dev.g255.arc.mod.gui.GuiTooltip;
import dev.g255.arc.mod.gui.HudElement;
import dev.g255.arc.mod.gui.clickgui.ClickGuiFrame;
import dev.g255.arc.mod.gui.clickgui.ClickGuiScreen;
import dev.g255.arc.mod.gui.items.Component;
import dev.g255.arc.mod.gui.items.buttons.ModuleButton;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.impl.client.ClickGui;
import dev.g255.arc.mod.modules.impl.client.ClientSetting;
import dev.g255.arc.mod.modules.impl.client.HUD;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

/**
 * HUD 页 = 界面编辑器 + 界面设置栏。
 *
 * <p>切到这一页就等于进编辑模式：屏幕上所有 HUD 元素（{@link HudElement}）都会画出虚线框，
 * 左键拖动、右键开关、G 辅助线、R 居中，松手自动吸附并改写左右锚点定位。
 * 左侧那栏「HUD（界面）」放的是界面模块自身的设置（默认展开），
 * 原来主页面里那一栏「界面」已经合并进来，不再重复出现。</p>
 */
public final class ClickGuiHudPage {
    private static final int SNAP = 6;
    /** 元素量出来是 0x0 时（空装备栏、空物品栏）至少给这么大的可点/可见区域 */
    private static final int MIN_BOX = 10;

    private final ClickGuiScreen host;
    private final ArrayList<HudElement> hudElements = new ArrayList<>();
    private final Map<HudElement, Smooth> animX = new HashMap<>();
    private final Map<HudElement, Smooth> animY = new HashMap<>();

    private HudElement dragging;
    private Alignment dragSide = Alignment.LEFT;
    private int dragOffsetX;
    private int dragOffsetY;
    /** 对齐辅助线：默认打开（拖拽时立刻能看到中线/同伴边缘） */
    private boolean guides = true;

    private Component hudComponent;
    /** 界面模块本体那一颗按钮，元素模块都挂在它的「元素」页下面 */
    private ModuleButton hudButton;
    /** 各个界面元素模块的按钮（作为 hudButton 的额外条目，只在 HUD 的「元素」页显示） */
    private final ArrayList<ModuleButton> elementButtons = new ArrayList<>();
    private boolean hudPosInit;
    private float hudLocalX;
    private float hudLocalY;

    public ClickGuiHudPage(ClickGuiScreen host) {
        this.host = host;
    }

    public void init() {
        this.collectHudElements();
        this.initHudButtons();
    }

    public void resetHudLayout() {
        this.hudPosInit = false;
    }

    private void collectHudElements() {
        this.hudElements.clear();
        this.animX.clear();
        this.animY.clear();
        if (Arc.MODULE == null) {
            return;
        }
        for (Module module : Arc.MODULE.getModules()) {
            if (module instanceof HudElement element) {
                this.hudElements.add(element);
            }
        }
    }

    /** 一栏装完：界面模块本体（默认展开），其它界面元素模块全部挂到它的「元素」页里 */
    private void initHudButtons() {
        boolean chinese = ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.chinese.getValue();
        this.hudComponent = new Component("HUD", Module.Category.Client, 100, 100, true);
        this.hudComponent.setDisplayTitle(chinese ? "HUD（界面）" : "HUD (Interface)");
        this.elementButtons.clear();

        for (Module module : Arc.MODULE.getModules()) {
            if (module instanceof HUD) {
                this.hudButton = new ModuleButton(module);
                // 界面模块默认展开，进来就能直接改设置
                this.hudButton.subOpen = true;
                this.hudComponent.addButton(this.hudButton);
            }
        }
        if (this.hudButton == null) {
            return;
        }

        // 其余界面元素模块：作为 HUD 模块设置里的额外条目，只在 HUD 的「元素」页露出来。
        // 不再单独开一栏，界面页就只有「HUD（界面）」这一条链。
        ArrayList<ModuleButton> elements = new ArrayList<>();
        for (Module module : Arc.MODULE.getModules()) {
            if (module.getCategory() == Module.Category.HUD) {
                ModuleButton button = new ModuleButton(module);
                // HUD 元素页里的条目强制显示 + 号：这里本来就该能展开进各自的配置
                button.setForceIndicator(true);
                elements.add(button);
            }
        }
        elements.sort(java.util.Comparator.comparing(Mod::getName));
        for (ModuleButton button : elements) {
            button.setHidden(true);
            this.hudButton.addExtraItem(button);
            this.elementButtons.add(button);
        }
    }

    // ---------------------------------------------------------------- 渲染

    public void render(DrawContext context, int mouseX, int mouseY, float delta, ClickGuiFrame frame) {
        ClickGui gui = ClickGui.getInstance();
        if (gui == null || this.hudComponent == null) {
            return;
        }
        // 编辑器只在「HUD（界面）」页生效：停在模块/配置页时这里整块不画，
        // 屏幕上就只剩正常 HUD，不会出现虚线框和拖拽状态
        if (this.host.getPage() != ClickGuiScreen.Page.Hud) {
            this.dragging = null;
            return;
        }
        this.updateDragging(frame, mouseX, mouseY);
        this.renderEditor(context, frame, mouseX, mouseY, delta);
        this.renderPanel(context, frame, mouseX, mouseY, delta);
        GuiTooltip.drawForComponent(context, frame, this.host, this.hudComponent, mouseX, mouseY);
    }

    /** 屏幕上的元素：内容 + 虚线框 + 悬停标签 + 底部操作提示 */
    private void renderEditor(DrawContext context, ClickGuiFrame frame, int mouseX, int mouseY, float delta) {
        int mx = (int)frame.unitMouseX(mouseX);
        int my = (int)frame.unitMouseY(mouseY);
        int unitW = this.unitW(frame);
        int unitH = this.unitH(frame);

        if (this.guides && this.dragging != null) {
            context.fill(unitW / 2, 0, unitW / 2 + 1, unitH, 0x15FFFFFF);
            context.fill(0, unitH / 2, unitW, unitH / 2 + 1, 0x15FFFFFF);
        }

        for (HudElement element : this.hudElements) {
            this.renderElement(context, element, mx, my, delta);
        }

        if (this.guides && this.dragging != null) {
            this.renderGuides(context, unitW, unitH);
        }
        this.renderHint(context, frame, unitW, unitH);
    }

    private void renderElement(DrawContext context, HudElement element, int mouseX, int mouseY, float delta) {
        int w = element.getWidth();
        int h = element.getHeight();
        if (w <= 0 || h <= 0) {
            // 宽度算成 0 的元素（没穿装备时的装备显示、没东西可显示的物品栏）也要留一个
            // 能看见、能拖的最小框，否则屏幕上什么都没有，等于这个元素彻底拖不动
            w = Math.max(w, MIN_BOX);
            h = Math.max(h, MIN_BOX);
        }
        int x = element.getX();
        int y = element.getY();
        float renderX = this.anim(element, this.animX, x);
        float renderY = this.anim(element, this.animY, y);

        if (element.isVisible()) {
            try {
                context.getMatrices().push();
                context.getMatrices().translate(renderX - (float)x, renderY - (float)y, 0.0f);
                element.render(context, delta);
                context.getMatrices().pop();
            } catch (Throwable ignored) {
            }
        }

        boolean hovered = this.isHovering(element, mouseX, mouseY);
        boolean active = this.dragging == element || hovered;
        this.drawDashedRect(context, Math.round(renderX), Math.round(renderY), w, h,
                this.dragging == element ? 0xFFFFFFFF : (hovered ? 0x8022A2FF : 0xC0000000));

        if (active) {
            String name = element.getDisplayName();
            int nameWidth = this.textWidth(name);
            int labelX = Math.round(renderX) + w + 4;
            int labelY = Math.round(renderY) - 2;
            if (labelX + nameWidth + 6 > this.unitW(this.frameOfLastRender)) {
                labelX = Math.round(renderX) - nameWidth - 6;
            }
            if (labelY < 2) {
                labelY = Math.round(renderY) + h + 2;
            }
            Render2DUtil.rect(context.getMatrices(), (float)(labelX - 2), (float)(labelY - 1),
                    (float)(labelX + nameWidth + 2), (float)(labelY + this.fontHeight() + 1), new Color(0, 0, 0, 176).getRGB());
            TextUtil.drawString(context, name, (double)labelX, (double)labelY, -1,
                    FontManager.isCustomFontEnabled(), FontManager.isShadowEnabled());
        }
    }

    private ClickGuiFrame frameOfLastRender;

    private void renderHint(DrawContext context, ClickGuiFrame frame, int unitW, int unitH) {
        boolean chinese = ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.chinese.getValue();
        String info;
        if (this.dragging != null) {
            String side = this.dragging.getSide() == Alignment.RIGHT
                    ? (chinese ? "右锚点" : "right")
                    : (chinese ? "左锚点" : "left");
            info = (chinese ? "拖拽中 (" : "Dragging (") + this.dragging.getX() + ", " + this.dragging.getY() + ") ["
                    + side + "]";
        } else {
            info = chinese
                    ? "界面编辑器 | 左键拖拽 | 右键开关 | G 辅助线 | R 居中"
                    : "HUD Editor | LMB drag | RMB toggle | G guides | R center";
        }
        int textY = unitH - this.fontHeight() - 3;
        Render2DUtil.rect(context.getMatrices(), 0.0f, (float)(unitH - this.fontHeight() - 6), (float)unitW, (float)unitH,
                new Color(0, 0, 0, 120).getRGB());
        TextUtil.drawString(context, info, (double)((unitW - this.textWidth(info)) / 2), (double)textY,
                new Color(200, 200, 200).getRGB(), FontManager.isCustomFontEnabled(), FontManager.isShadowEnabled());
    }

    private void renderPanel(DrawContext context, ClickGuiFrame frame, int mouseX, int mouseY, float delta) {
        boolean chinese = ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.chinese.getValue();
        ClickGui gui = ClickGui.getInstance();
        this.hudComponent.setDisplayTitle(chinese ? "HUD（界面）" : "HUD (Interface)");
        // 尺寸每帧对齐 ClickGUI 设置：模块列宽度 / 标题条高度改了，这里和模块页一起变
        int layoutWidth = gui.moduleButtonWidth.getValueInt();
        int barHeight = gui.categoryBarHeight.getValueInt() + 5;
        this.hudComponent.setWidth(layoutWidth);
        this.hudComponent.setHeight(barHeight);
        // 元素模块只在 HUD 设置的「元素」页里出现（页签一换就收起来，高度跟着动画归零）
        boolean showElements = HUD.INSTANCE != null && HUD.INSTANCE.page.is(HUD.Page.Element);
        for (ModuleButton button : this.elementButtons) {
            button.setHidden(!showElements || !GuiSearch.matches(button.getModule()));
        }

        float baseX = frame.baseX(ClickGuiScreen.Page.Hud);
        float unitW = frame.scale == 0.0f ? (float)frame.screenW : (float)frame.screenW / frame.scale;
        float panelXf = Math.max(8.0f, (unitW - (float)frame.panelW) / 2.0f);
        float defaultLocalX = panelXf + 10.0f;
        float defaultLocalY = (float)frame.panelY + 10.0f;

        this.hudComponent.setMouseMoveOffset(frame.totalOffsetX, frame.totalOffsetY);
        this.hudComponent.setPageOffsetX(baseX);
        if (!this.hudPosInit) {
            this.hudLocalX = defaultLocalX;
            this.hudLocalY = defaultLocalY;
            this.hudComponent.setX((int)this.hudLocalX);
            this.hudComponent.setY((int)this.hudLocalY);
            this.hudPosInit = true;
        }
        if (this.hudComponent.drag) {
            this.hudLocalX = (float)this.hudComponent.getX() - baseX - frame.totalOffsetX;
            this.hudLocalY = (float)this.hudComponent.getY() - frame.totalOffsetY;
        } else {
            this.hudComponent.setX((int)this.hudLocalX);
            this.hudComponent.setY((int)this.hudLocalY);
        }
        int umx = (int)frame.unitMouseX(mouseX);
        int umy = (int)frame.unitMouseY(mouseY);
        this.hudComponent.drawScreen(context, umx, umy, delta);
    }

    // ---------------------------------------------------------------- 输入

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, ClickGuiFrame frame) {
        int mx = (int)frame.unitMouseX(mouseX);
        int my = (int)frame.unitMouseY(mouseY);

        if (mouseButton == 0) {
            HudElement hovered = this.getHovered(mx, my);
            if (hovered != null && hovered.isEditable()) {
                this.dragging = hovered;
                this.dragSide = hovered.getSide();
                this.dragOffsetX = mx - hovered.getX();
                this.dragOffsetY = my - hovered.getY();
                return true;
            }
        }
        if (mouseButton == 1) {
            HudElement hovered = this.getHovered(mx, my);
            if (hovered instanceof Module module) {
                module.toggle();
                return true;
            }
        }
        if (this.hudComponent != null && this.hudComponent.isMouseOver(mx, my)) {
            this.hudComponent.mouseClicked(mx, my, mouseButton);
            return true;
        }
        return false;
    }

    public void mouseReleased(int mouseX, int mouseY, int releaseButton) {
        if (releaseButton == 0 && this.dragging != null) {
            this.finishDrag();
        }
        if (this.hudComponent != null) {
            this.hudComponent.mouseReleased(mouseX, mouseY, releaseButton);
        }
    }

    public void mouseScrolled(double verticalAmount) {
        if (this.hudComponent == null || !this.hudPosInit) {
            return;
        }
        if (Wrapper.mc != null && Wrapper.mc.getWindow() != null
                && org.lwjgl.glfw.GLFW.glfwGetKey(Wrapper.mc.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT) == 1) {
            this.hudLocalX += verticalAmount > 0.0 ? 15.0f : -15.0f;
        } else {
            this.hudLocalY += verticalAmount > 0.0 ? 15.0f : -15.0f;
        }
    }

    public void keyPressed(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_G) {
            this.guides = !this.guides;
            return;
        }
        if (keyCode == GLFW.GLFW_KEY_R && this.dragging == null) {
            ClickGuiFrame frame = this.frameOfLastRender;
            if (frame == null) {
                return;
            }
            HudElement hovered = this.getHovered((int)frame.unitMouseX(this.lastMouseX), (int)frame.unitMouseY(this.lastMouseY));
            if (hovered != null) {
                this.center(hovered, frame);
            }
            return;
        }
        if (this.hudComponent != null) {
            this.hudComponent.onKeyPressed(keyCode);
        }
    }

    public void charTyped(char chr, int modifiers) {
        if (this.hudComponent != null) {
            this.hudComponent.onKeyTyped(chr, modifiers);
        }
    }

    // ---------------------------------------------------------------- 拖拽核心

    private int lastMouseX;
    private int lastMouseY;

    private void updateDragging(ClickGuiFrame frame, int mouseX, int mouseY) {
        this.frameOfLastRender = frame;
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        if (this.dragging == null) {
            return;
        }
        if (Wrapper.mc == null || Wrapper.mc.getWindow() == null
                || GLFW.glfwGetMouseButton(Wrapper.mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
            this.finishDrag();
            return;
        }
        int mx = (int)frame.unitMouseX(mouseX);
        int my = (int)frame.unitMouseY(mouseY);
        this.moveHud(this.dragging, mx - this.dragOffsetX, my - this.dragOffsetY, frame);
    }

    private void finishDrag() {
        if (this.dragging == null) {
            return;
        }
        ClickGuiFrame frame = this.frameOfLastRender;
        if (frame != null) {
            this.clampHud(this.dragging, frame);
            this.updateAlign(this.dragging, frame);
        }
        this.dragging = null;
        if (Arc.CONFIG != null) {
            Arc.CONFIG.save();
        }
    }

    private void moveHud(HudElement element, int nx, int ny, ClickGuiFrame frame) {
        int dw = element.getWidth();
        int dh = element.getHeight();
        int dragDh = element.getRenderHeight();
        int unitW = this.unitW(frame);
        int unitH = this.unitH(frame);

        if (Math.abs(nx) < SNAP) {
            nx = 0;
        }
        if (Math.abs(ny) < SNAP) {
            ny = 0;
        }
        if (Math.abs(nx + dw - unitW) < SNAP) {
            nx = unitW - dw;
        }
        if (Math.abs(ny + dragDh - unitH) < SNAP) {
            ny = unitH - dragDh;
        }
        int cx = (unitW - dw) / 2;
        int cy = (unitH - dh) / 2;
        if (Math.abs(nx - cx) < SNAP) {
            nx = cx;
        }
        if (Math.abs(ny - cy) < SNAP) {
            ny = cy;
        }

        boolean snappedX = false;
        boolean snappedY = false;
        for (HudElement other : this.hudElements) {
            if (other == element || other.getWidth() <= 0) {
                continue;
            }
            int ox = other.getX();
            int oy = other.getY();
            int ow = other.getWidth();
            int oh = other.getHeight();
            boolean verticalNeighbour = Math.abs(ny - oy - oh) < SNAP || Math.abs(ny + dh - oy) < SNAP;
            if (verticalNeighbour && !snappedX) {
                int range = SNAP * 3;
                if (Math.abs(nx + dw - (ox + ow)) < range) {
                    nx = ox + ow - dw;
                    snappedX = true;
                } else if (Math.abs(nx - ox) < range) {
                    nx = ox;
                    snappedX = true;
                }
            }
            if (!snappedY) {
                if (Math.abs(ny - oy - oh) < SNAP) {
                    ny = oy + oh;
                    snappedY = true;
                } else if (Math.abs(ny + dh - oy) < SNAP) {
                    ny = oy - dh;
                    snappedY = true;
                }
            }
        }

        if (this.dragSide == Alignment.RIGHT) {
            element.setRightX(nx + dw);
        } else {
            element.setX(nx);
        }
        element.setY(ny);
        this.clampHud(element, frame);
    }

    private void center(HudElement element, ClickGuiFrame frame) {
        int unitW = this.unitW(frame);
        int unitH = this.unitH(frame);
        element.setX((unitW - element.getWidth()) / 2);
        element.setY((unitH - element.getHeight()) / 2);
        this.clampHud(element, frame);
    }

    private void clampHud(HudElement element, ClickGuiFrame frame) {
        int unitW = this.unitW(frame);
        int unitH = this.unitH(frame);
        element.setX(Math.max(0, Math.min(element.getX(), unitW - element.getWidth())));
        element.setY(Math.max(0, Math.min(element.getY(), unitH - element.getRenderHeight())));
    }

    /** 松手后按「有没有右边缘对齐的同伴 / 是否过了屏幕中线」决定左右锚点 */
    private void updateAlign(HudElement element, ClickGuiFrame frame) {
        int rightEdge = element.getX() + element.getWidth();
        boolean partner = false;
        for (HudElement other : this.hudElements) {
            if (other != element && other.getWidth() > 0
                    && Math.abs(rightEdge - (other.getX() + other.getWidth())) <= 1) {
                partner = true;
                break;
            }
        }
        boolean right = partner || element.getX() + element.getWidth() / 2 > this.unitW(frame) / 2;
        Alignment want = right ? Alignment.RIGHT : Alignment.LEFT;
        if (element.getSide() != want) {
            // 关键：换锚点前先把「屏幕左边缘」记下来，换完立刻按新锚点写回去。
            // 否则像 ArmorHUD / TextRadar 这种 getX() 会按 Side 镜像的元素，
            // 内部存的值还是旧锚点的语义，getX() 当场跳到屏幕另一侧 —— 松手就弹回/乱跳。
            int left = element.getX();
            element.setAlignment(want);
            element.setX(left);
        }
    }

    private HudElement getHovered(int mouseX, int mouseY) {
        HudElement best = null;
        int bestArea = Integer.MAX_VALUE;
        for (HudElement element : this.hudElements) {
            if (!this.isHovering(element, mouseX, mouseY)) {
                continue;
            }
            // 面积用最小尺寸兜底：量出来 0x0 的元素（空装备栏）不然会被 area > 0 直接过滤掉，
            // 表现为这个元素「永远选不中、拖不动」
            int area = Math.max(MIN_BOX, element.getWidth()) * Math.max(MIN_BOX, element.getHeight());
            if (area < bestArea) {
                bestArea = area;
                best = element;
            }
        }
        return best;
    }

    private boolean isHovering(HudElement element, int mouseX, int mouseY) {
        // 最小命中尺寸和信息框一致：宽度只有 1px 的元素（空装备栏）用原始尺寸
        // 几乎点不中，表现出来就是「拖不动」
        int w = Math.max(MIN_BOX, element.getWidth());
        int h = Math.max(MIN_BOX, element.getHeight());
        int x = element.getX();
        int y = element.getY();
        return mouseX >= x - 4 && mouseX <= x + w + 4 && mouseY >= y - 4 && mouseY <= y + h + 4;
    }

    private void renderGuides(DrawContext context, int unitW, int unitH) {
        int dx = this.dragging.getX();
        int dy = this.dragging.getY();
        int dw = this.dragging.getWidth();
        int dh = this.dragging.getHeight();
        if (Math.abs(dx + dw / 2 - unitW / 2) <= SNAP) {
            context.fill(unitW / 2, 0, unitW / 2 + 1, unitH, 0xFFFF3B3B);
        }
        if (Math.abs(dy + dh / 2 - unitH / 2) <= SNAP) {
            context.fill(0, unitH / 2, unitW, unitH / 2 + 1, 0xFFFF3B3B);
        }
        for (HudElement other : this.hudElements) {
            if (other == this.dragging || other.getWidth() <= 0) {
                continue;
            }
            int ox = other.getX();
            int oy = other.getY();
            int ow = other.getWidth();
            int oh = other.getHeight();
            int minY = Math.min(dy, oy);
            int maxY = Math.max(dy + dh, oy + oh);
            int minX = Math.min(dx, ox);
            int maxX = Math.max(dx + dw, ox + ow);
            if (Math.abs(dx - ox) <= SNAP) {
                context.fill(ox, minY, ox + 1, maxY, 0xFFFF3B3B);
            }
            if (Math.abs(dx + dw - ox - ow) <= SNAP) {
                context.fill(ox + ow - 1, minY, ox + ow, maxY, 0xFFFF3B3B);
            }
            if (Math.abs(dy - oy) <= SNAP) {
                context.fill(minX, oy, maxX, oy + 1, 0xFFFF3B3B);
            }
            if (Math.abs(dy + dh - oy - oh) <= SNAP) {
                context.fill(minX, oy + oh - 1, maxX, oy + oh, 0xFFFF3B3B);
            }
        }
    }

    private void drawDashedRect(DrawContext context, int x, int y, int width, int height, int color) {
        int dash = 4;
        int gap = 2;
        for (int i = 0; i < width; i += dash + gap) {
            int end = Math.min(i + dash, width);
            context.fill(x + i, y, x + end, y + 1, color);
            context.fill(x + i, y + height - 1, x + end, y + height, color);
        }
        for (int i = 0; i < height; i += dash + gap) {
            int end = Math.min(i + dash, height);
            context.fill(x, y + i, x + 1, y + end, color);
            context.fill(x + width - 1, y + i, x + width, y + end, color);
        }
    }

    private float anim(HudElement element, Map<HudElement, Smooth> map, int target) {
        Smooth smooth = map.computeIfAbsent(element, k -> new Smooth(target));
        smooth.target = target;
        return smooth.update();
    }

    private int unitW(ClickGuiFrame frame) {
        return Math.round(frame.scale == 0.0f ? (float)frame.screenW : (float)frame.screenW / frame.scale);
    }

    private int unitH(ClickGuiFrame frame) {
        return Math.round(frame.scale == 0.0f ? (float)frame.screenH : ((float)frame.screenH - frame.slideY) / frame.scale);
    }

    private int textWidth(String text) {
        return FontManager.isCustomFontEnabled()
                ? (int)FontManager.ui.getWidth(text)
                : (int)TextUtil.getWidth(text);
    }

    private int fontHeight() {
        return FontManager.isCustomFontEnabled()
                ? (int)FontManager.ui.getFontHeight()
                : (int)TextUtil.getHeight();
    }

    /** 位置插值，拖完不硬跳 */
    private static final class Smooth {
        private float value;
        private float target;

        private Smooth(float start) {
            this.value = start;
            this.target = start;
        }

        private float update() {
            this.value += (this.target - this.value) * 0.28f;
            if (Math.abs(this.target - this.value) < 0.2f) {
                this.value = this.target;
            }
            return this.value;
        }
    }
}
