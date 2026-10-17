package dev.perish.mod.gui.skidspace;

import dev.perish.Perish;
import dev.perish.api.utils.render.Render2DUtil;
import dev.perish.api.utils.math.Animation;
import dev.perish.api.utils.math.Easing;
import dev.perish.mod.gui.skid.ClickGuiTextUtil;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.Setting;
import dev.perish.mod.modules.settings.impl.BindSetting;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.ColorSetting;
import dev.perish.mod.modules.settings.impl.EnumSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;
import dev.perish.mod.modules.settings.impl.StringSetting;
import dev.perish.mod.modules.impl.client.ClickGui;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.awt.datatransfer.DataFlavor;
import java.awt.Toolkit;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.gui.screen.Screen;

public class ClickGuiScreen extends Screen {
    private static final int TAB_WIDTH = 108;
    private static final int HEADER_HEIGHT = 15;
    private static final int ROW_HEIGHT = 15;
    private static final int TAB_SPACING = 0;
    private static final int TAB_START_X = 18;
    private static final int TAB_START_Y = 18;
    private static final int TAB_BOTTOM_MARGIN = 18;

    public static boolean clicked = false;
    public static boolean rightClicked = false;
    public static boolean hoverClicked = false;
    public static boolean MOUSE_BUTTON_4 = false;
    public static boolean MOUSE_BUTTON_5 = false;
    private static final Set<Module> EXPANDED_MODULES = Collections.newSetFromMap(new IdentityHashMap<>());

    private final List<Snow> snow = new ArrayList<>();
    private final List<CategoryTab> tabs = new ArrayList<>();
    private CategoryTab grabbedTab;
    private int dragOffsetX;
    private int dragOffsetY;
    private long renderFrame;

    public ClickGuiScreen() {
        super(Text.of("ClickGui"));
    }

    @Override
    protected void init() {
        if (!tabs.isEmpty()) {
            return;
        }

        int x = TAB_START_X;
        for (Module.Category category : Module.Category.values()) {
            CategoryTab tab = new CategoryTab(category, x, TAB_START_Y);
            tabs.add(tab);
            x += TAB_WIDTH + TAB_SPACING;
        }
    }

    @Override
    public void onDisplayed() {
        super.onDisplayed();
        snow.clear();
        Random random = new Random();
        for (int i = 0; i < 100; ++i) {
            for (int y = 0; y < 3; ++y) {
                snow.add(new Snow(25 * i, y * -50, random.nextInt(3) + 1, random.nextInt(2) + 1));
            }
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        if (ClickGui.INSTANCE == null || ClickGui.INSTANCE.vanillaBackground.getValue()) {
            super.renderBackground(context, mouseX, mouseY, delta);
        }

        if (ClickGui.INSTANCE.blur.getValue()) {
            try {
                float radius = (float) ClickGui.INSTANCE.radius.getValue();
                float sw = MinecraftClient.getInstance().getWindow().getScaledWidth();
                float sh = MinecraftClient.getInstance().getWindow().getScaledHeight();
                Perish.BLUR.applyBlur(radius, 0, 0, sw, sh);
            } catch (Exception ignored) {
            }
        }

        Render2DUtil.rectFilled(
                context.getMatrices(),
                0,
                0,
                MinecraftClient.getInstance().getWindow().getScaledWidth(),
                MinecraftClient.getInstance().getWindow().getScaledHeight(),
                new Color(0, 0, 0, 82).getRGB()
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        renderFrame++;

        if (ClickGui.INSTANCE != null && ClickGui.INSTANCE.snow.getValue()) {
            for (Snow particle : snow) {
                particle.draw(context);
            }
        }

        for (CategoryTab tab : tabs) {
            tab.updateAnimations(renderFrame);
            tab.render(context, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            hoverClicked = false;
            clicked = true;
        } else if (button == 1) {
            rightClicked = true;
        } else if (button == 3) {
            MOUSE_BUTTON_4 = true;
        } else if (button == 4) {
            MOUSE_BUTTON_5 = true;
        }

        for (int i = tabs.size() - 1; i >= 0; i--) {
            CategoryTab tab = tabs.get(i);

            if (tab.isHeaderHovered(mouseX, mouseY)) {
                bringToFront(tab);
                if (button == 0) {
                    grabbedTab = tab;
                    dragOffsetX = (int) mouseX - tab.x;
                    dragOffsetY = (int) mouseY - tab.y;
                } else if (button == 1) {
                    tab.expanded = !tab.expanded;
                }
                return true;
            }

            if (tab.mouseClicked(mouseX, mouseY, button)) {
                bringToFront(tab);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            clicked = false;
            hoverClicked = false;
        } else if (button == 1) {
            rightClicked = false;
        } else if (button == 3) {
            MOUSE_BUTTON_4 = false;
        } else if (button == 4) {
            MOUSE_BUTTON_5 = false;
        }

        grabbedTab = null;
        for (CategoryTab tab : tabs) {
            tab.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && grabbedTab != null) {
            grabbedTab.x = (int) mouseX - dragOffsetX;
            grabbedTab.y = (int) mouseY - dragOffsetY;
            return true;
        }

        for (CategoryTab tab : tabs) {
            if (tab.mouseDragged(mouseX, mouseY, button)) {
                return true;
            }
        }

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (int i = tabs.size() - 1; i >= 0; i--) {
            CategoryTab tab = tabs.get(i);
            if (tab.mouseScrolled(mouseX, mouseY, verticalAmount)) {
                bringToFront(tab);
                return true;
            }
        }

        if (verticalAmount != 0.0) {
            int offset = (int) (verticalAmount * 30.0);
            for (CategoryTab tab : tabs) {
                tab.y += offset;
            }
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (int i = tabs.size() - 1; i >= 0; i--) {
            if (tabs.get(i).keyPressed(keyCode)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        for (int i = tabs.size() - 1; i >= 0; i--) {
            if (tabs.get(i).charTyped(chr)) {
                return true;
            }
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        super.close();
        grabbedTab = null;
        rightClicked = false;
        hoverClicked = false;
        MOUSE_BUTTON_4 = false;
        MOUSE_BUTTON_5 = false;
        clicked = false;
    }

    private void bringToFront(CategoryTab tab) {
        tabs.remove(tab);
        tabs.add(tab);
    }

    private Color applyGlobalAlpha(Color color) {
        if (ClickGui.INSTANCE != null && ClickGui.INSTANCE.fade.getValue()) {
            int alpha = (int) (color.getAlpha() * ClickGui.INSTANCE.alphaValue);
            return new Color(color.getRed(), color.getGreen(), color.getBlue(), MathHelper.clamp(alpha, 0, 255));
        }
        return color;
    }

    private int applyGlobalAlpha(int color) {
        if (ClickGui.INSTANCE != null && ClickGui.INSTANCE.fade.getValue()) {
            int a = (color >> 24) & 0xFF;
            int alpha = (int) (a * ClickGui.INSTANCE.alphaValue);
            return (color & 0x00FFFFFF) | (MathHelper.clamp(alpha, 0, 255) << 24);
        }
        return color;
    }

    private Color getAccent() {
        return applyGlobalAlpha(ClickGui.INSTANCE != null ? ClickGui.INSTANCE.color.getValue() : new Color(140, 146, 255));
    }

    private Color getAccentDark() {
        Color c = getAccent();
        return new Color(
                Math.max(0, c.getRed() - 36),
                Math.max(0, c.getGreen() - 36),
                Math.max(0, c.getBlue() - 36),
                c.getAlpha()
        );
    }

    private Color getTabBodyColor() {
        return applyGlobalAlpha(new Color(0, 0, 0, 84));
    }

    private Color getSettingBodyColor() {
        return applyGlobalAlpha(new Color(0, 0, 0, 92));
    }

    private Color getModuleBodyColor() {
        return applyGlobalAlpha(new Color(0, 0, 0, 72));
    }

    private Color getHoverColor() {
        if (ClickGui.INSTANCE != null) {
            return applyGlobalAlpha(ClickGui.INSTANCE.skidspaceHoverColor.getValue());
        }
        return applyGlobalAlpha(new Color(255, 255, 255, 40));
    }

    private Color getHoverEndColor() {
        if (ClickGui.INSTANCE != null) {
            return applyGlobalAlpha(ClickGui.INSTANCE.skidspaceHoverEndColor.getValue());
        }
        return applyGlobalAlpha(new Color(0, 0, 0, 0));
    }

    private Color getEnabledColor() {
        if (ClickGui.INSTANCE != null) {
            return applyGlobalAlpha(ClickGui.INSTANCE.skidspaceEnabledColor.getValue());
        }
        return applyGlobalAlpha(new Color(255, 255, 255, 25));
    }

    private Color getEnabledEndColor() {
        if (ClickGui.INSTANCE != null) {
            return applyGlobalAlpha(ClickGui.INSTANCE.skidspaceEnabledEndColor.getValue());
        }
        return applyGlobalAlpha(new Color(0, 0, 0, 0));
    }

    private Color getOutlineColor() {
        return applyGlobalAlpha(new Color(0, 0, 0, 160));
    }

    private int getTextColor() {
        if (ClickGui.INSTANCE != null) {
            return applyGlobalAlpha(ClickGui.INSTANCE.onModulesetting.getValue().getRGB());
        }
        return applyGlobalAlpha(new Color(255, 255, 255, 255).getRGB());
    }

    private int getSecondaryTextColor() {
        if (ClickGui.INSTANCE != null) {
            return applyGlobalAlpha(ClickGui.INSTANCE.offModulesetting.getValue().getRGB());
        }
        return applyGlobalAlpha(new Color(196, 196, 196, 255).getRGB());
    }

    private int getCategoryTextColor() {
        if (ClickGui.INSTANCE != null) {
            return applyGlobalAlpha(ClickGui.INSTANCE.skidspaceCategoryColor.getValue().getRGB());
        }
        return applyGlobalAlpha(new Color(255, 255, 255, 255).getRGB());
    }

    private int getCategoryCountColor() {
        if (ClickGui.INSTANCE != null) {
            return applyGlobalAlpha(ClickGui.INSTANCE.skidspaceCategoryCountColor.getValue().getRGB());
        }
        return applyGlobalAlpha(new Color(196, 196, 196, 255).getRGB());
    }

    private Color getCategoryBgColor() {
        if (ClickGui.INSTANCE != null) {
            return applyGlobalAlpha(ClickGui.INSTANCE.skidspaceCategoryBgColor.getValue());
        }
        return applyGlobalAlpha(new Color(0, 0, 0, 0));
    }

    private Color getCategoryBgEndColor() {
        if (ClickGui.INSTANCE != null) {
            return applyGlobalAlpha(ClickGui.INSTANCE.skidspaceCategoryBgEndColor.getValue());
        }
        return applyGlobalAlpha(new Color(0, 0, 0, 0));
    }

    private int getHeaderTextColor() {
        if (ClickGui.INSTANCE != null) {
            return applyGlobalAlpha(ClickGui.INSTANCE.topColorsetting.getValue().getRGB());
        }
        return applyGlobalAlpha(new Color(255, 255, 255, 255).getRGB());
    }

    private boolean drawShadow() {
        return ClickGui.INSTANCE != null && ClickGui.INSTANCE.shadow.getValue();
    }

    private float getTextWidth(String text) {
        return ClickGuiTextUtil.getWidth(text);
    }

    private float getTextHeight() {
        return ClickGuiTextUtil.getHeight();
    }

    private void drawText(DrawContext context, String text, double x, double y, int color) {
        ClickGuiTextUtil.drawString(context, text, x, y, color, drawShadow());
    }

    private void drawTextShadow(DrawContext context, String text, double x, double y, int color) {
        ClickGuiTextUtil.drawString(context, text, x, y, color, true);
    }

    private boolean isHovered(double mouseX, double mouseY, double x, double y, double width, double height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private String formatDouble(double value) {
        BigDecimal bd = BigDecimal.valueOf(value).stripTrailingZeros();
        return bd.scale() < 0 ? bd.setScale(0).toPlainString() : bd.toPlainString();
    }

    private static boolean isAllowedCharacter(char character) {
        return character != 167 && character >= ' ' && character != 127;
    }

    private final class CategoryTab {
        private final Module.Category category;
        private final List<ModuleRow> moduleRows = new ArrayList<>();
        private int x;
        private int y;
        private int scroll;
        private boolean expanded = true;

        private CategoryTab(Module.Category category, int x, int y) {
            this.category = category;
            this.x = x;
            this.y = y;

            for (Module module : Perish.MODULE.getModulesFromCategory(category)) {
                moduleRows.add(new ModuleRow(module));
            }
        }

        private void render(DrawContext context, int mouseX, int mouseY) {
            Render2DUtil.verticalGradient(
                    context.getMatrices(),
                    x,
                    y - 2,
                    x + TAB_WIDTH,
                    y + HEADER_HEIGHT - 2,
                    getCategoryBgColor(),
                    getCategoryBgEndColor()
            );
            Render2DUtil.drawRect(context.getMatrices(), x, y + HEADER_HEIGHT - 1, TAB_WIDTH, 1, new Color(255, 255, 255, 38));

            String title = category.name();
            // Capitalize first letter, lowercase rest
            title = title.substring(0, 1).toUpperCase() + title.substring(1).toLowerCase();
            
            drawText(
                    context,
                    title,
                    x + TAB_WIDTH / 2.0 - getTextWidth(title) / 2.0,
                    y + 2,
                    getCategoryTextColor()
            );

            String count = "[" + moduleRows.size() + "]";
            drawText(
                    context,
                    count,
                    x + TAB_WIDTH - 4 - getTextWidth(count),
                    y + 2,
                    getCategoryCountColor()
            );

            if (!expanded) {
                return;
            }

            int totalContentHeight = 0;
            for (ModuleRow row : moduleRows) {
                totalContentHeight += row.getHeight();
            }

            int maxContentHeight = Math.min(totalContentHeight, MinecraftClient.getInstance().getWindow().getScaledHeight() - y - HEADER_HEIGHT - TAB_BOTTOM_MARGIN);
            maxContentHeight = Math.max(ROW_HEIGHT, maxContentHeight);
            int maxScroll = Math.max(0, totalContentHeight - maxContentHeight);
            scroll = MathHelper.clamp(scroll, 0, maxScroll);

            Render2DUtil.drawRect(context.getMatrices(), x, y + HEADER_HEIGHT - 1, TAB_WIDTH, maxContentHeight + 1, getTabBodyColor());

            context.enableScissor(x, y + HEADER_HEIGHT - 1, x + TAB_WIDTH, y + HEADER_HEIGHT + maxContentHeight);
            int drawY = y + HEADER_HEIGHT - scroll;
            for (ModuleRow row : moduleRows) {
                row.render(context, x, drawY, TAB_WIDTH, mouseX, mouseY);
                drawY += row.getHeight();
            }
            context.disableScissor();

            if (maxScroll > 0) {
                int barHeight = Math.max(16, (int) ((maxContentHeight / (double) totalContentHeight) * maxContentHeight));
                int barY = y + HEADER_HEIGHT + (int) ((scroll / (double) maxScroll) * (maxContentHeight - barHeight));
                Render2DUtil.drawRect(context.getMatrices(), x + TAB_WIDTH - 2, y + HEADER_HEIGHT, 1, maxContentHeight, new Color(0, 0, 0, 90));
                Render2DUtil.drawRect(context.getMatrices(), x + TAB_WIDTH - 2, barY, 1, barHeight, getAccent());
            }
        }

        private void updateAnimations(long frame) {
            for (ModuleRow row : moduleRows) {
                row.updateAnimation(frame);
            }
        }

        private boolean isHeaderHovered(double mouseX, double mouseY) {
            return isHovered(mouseX, mouseY, x, y - 2, TAB_WIDTH, HEADER_HEIGHT);
        }

        private boolean isBodyHovered(double mouseX, double mouseY) {
            if (!expanded) {
                return false;
            }
            return isHovered(mouseX, mouseY, x, y + HEADER_HEIGHT - 1, TAB_WIDTH, getVisibleBodyHeight());
        }

        private int getVisibleBodyHeight() {
            int contentHeight = 0;
            for (ModuleRow row : moduleRows) {
                contentHeight += row.getHeight();
            }
            return Math.min(contentHeight, MinecraftClient.getInstance().getWindow().getScaledHeight() - y - HEADER_HEIGHT - TAB_BOTTOM_MARGIN);
        }

        private boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!expanded || !isBodyHovered(mouseX, mouseY)) {
                return false;
            }

            int drawY = y + HEADER_HEIGHT - scroll;
            for (ModuleRow row : moduleRows) {
                if (row.mouseClicked(mouseX, mouseY, button, x, drawY, TAB_WIDTH)) {
                    return true;
                }
                drawY += row.getHeight();
            }
            return false;
        }

        private void mouseReleased(double mouseX, double mouseY, int button) {
            for (ModuleRow row : moduleRows) {
                row.mouseReleased(mouseX, mouseY, button);
            }
        }

        private boolean mouseDragged(double mouseX, double mouseY, int button) {
            for (ModuleRow row : moduleRows) {
                if (row.mouseDragged(mouseX, mouseY, button)) {
                    return true;
                }
            }
            return false;
        }

        private boolean mouseScrolled(double mouseX, double mouseY, double amount) {
            if (!isBodyHovered(mouseX, mouseY)) {
                return false;
            }
            scroll = Math.max(0, scroll - (int) (amount * 18));
            return true;
        }

        private boolean keyPressed(int keyCode) {
            for (ModuleRow row : moduleRows) {
                if (row.keyPressed(keyCode)) {
                    return true;
                }
            }
            return false;
        }

        private boolean charTyped(char chr) {
            for (ModuleRow row : moduleRows) {
                if (row.charTyped(chr)) {
                    return true;
                }
            }
            return false;
        }
    }

    private final class ModuleRow {
        private final Module module;
        private final List<SettingRow> settingRows = new ArrayList<>();
        private final Animation expandAnimation = new Animation();
        private boolean expanded;
        private long lastAnimationFrame = -1L;
        private double expandProgress;

        private ModuleRow(Module module) {
            this.module = module;
            this.expanded = EXPANDED_MODULES.contains(module);
            this.expandAnimation.from = this.expanded ? 1.0 : 0.0;
            this.expandAnimation.to = this.expanded ? 1.0 : 0.0;
            this.expandProgress = this.expanded ? 1.0 : 0.0;
            for (Setting setting : module.getSettings()) {
                if (setting instanceof BooleanSetting booleanSetting) {
                    settingRows.add(new BooleanRow(booleanSetting));
                } else if (setting instanceof SliderSetting sliderSetting) {
                    settingRows.add(new SliderRow(sliderSetting));
                } else if (setting instanceof EnumSetting<?> enumSetting) {
                    settingRows.add(new EnumRow(enumSetting));
                } else if (setting instanceof BindSetting bindSetting) {
                    settingRows.add(new BindRow(bindSetting));
                } else if (setting instanceof StringSetting stringSetting) {
                    settingRows.add(new StringRow(stringSetting));
                } else if (setting instanceof ColorSetting colorSetting) {
                    settingRows.add(new ColorRow(colorSetting));
                }
            }
        }

        private int getHeight() {
            int height = ROW_HEIGHT;
            int settingsHeight = this.getSettingsHeight();
            if (settingsHeight > 0) {
                height += Math.round(settingsHeight * this.getExpandProgress());
            }
            return height;
        }

        private int getSettingsHeight() {
            int height = 0;
            for (SettingRow row : settingRows) {
                if (row.isVisible()) {
                    height += row.getHeight();
                }
            }
            return height;
        }

        private void updateAnimation(long frame) {
            if (this.lastAnimationFrame == frame) {
                return;
            }

            this.lastAnimationFrame = frame;
            double speed = ClickGui.INSTANCE == null ? 1.0 : ClickGui.INSTANCE.expandSpeed.getValue();
            long duration = Math.max(40L, (long) (180.0 / speed));
            this.expandProgress = this.expandAnimation.get(this.expanded ? 1.0 : 0.0, duration, Easing.SineOut);
        }

        private double getExpandProgress() {
            return this.expandProgress;
        }

        private void setExpanded(boolean expanded) {
            this.expanded = expanded;
            if (expanded) {
                EXPANDED_MODULES.add(this.module);
            } else {
                EXPANDED_MODULES.remove(this.module);
            }
        }

        private float getTextOffsetY() {
            return (ROW_HEIGHT - getTextHeight()) / 2.0F;
        }

        private void render(DrawContext context, int x, int y, int width, int mouseX, int mouseY) {
            boolean hovered = isHovered(mouseX, mouseY, x + 1, y, width - 2, ROW_HEIGHT - 1);

            Render2DUtil.drawRect(context.getMatrices(), x, y, width, ROW_HEIGHT - 1, getModuleBodyColor());
            
            if (module.isOn()) {
                Render2DUtil.horizontalGradient(
                        context.getMatrices(),
                        x,
                        y,
                        x + width,
                        y + ROW_HEIGHT,
                        getEnabledColor(),
                        getEnabledEndColor()
                );
            }

            if (hovered) {
                Render2DUtil.horizontalGradient(
                        context.getMatrices(),
                        x,
                        y,
                        x + width,
                        y + ROW_HEIGHT,
                        getHoverColor(),
                        getHoverEndColor()
                );
            }

            int textColor = module.isOn() ? getTextColor() : getSecondaryTextColor();
            drawText(context, module.getDisplayName(), x + 4, y + getTextOffsetY(), textColor);

            if (ClickGui.INSTANCE.showBind.getValue() && module.getBindSetting().getValue() != -1) {
                String bind = "[" + module.getBindSetting().getKeyString() + "]";
                drawText(
                        context,
                        bind,
                        x + 6 + getTextWidth(module.getDisplayName()),
                        y + getTextOffsetY() + 1,
                        getSecondaryTextColor()
                );
            }

            if (!settingRows.isEmpty() && ClickGui.INSTANCE.gear.getValue()) {
                String marker = expanded ? "-" : "+";
                drawText(context, marker, x + width - 12, y + getTextOffsetY(), getSecondaryTextColor());
            }

            int visibleSettingsHeight = (int) Math.round(this.getSettingsHeight() * this.getExpandProgress());
            if (visibleSettingsHeight <= 0) {
                return;
            }

            int childY = y + ROW_HEIGHT;
            context.enableScissor(x + 1, childY, x + width - 1, childY + visibleSettingsHeight);
            for (SettingRow row : settingRows) {
                if (!row.isVisible()) {
                    continue;
                }
                row.render(context, x + 1, childY, width - 2, mouseX, mouseY);
                childY += row.getHeight();
            }
            context.disableScissor();
        }

        private boolean mouseClicked(double mouseX, double mouseY, int button, int x, int y, int width) {
            if (isHovered(mouseX, mouseY, x + 1, y, width - 2, ROW_HEIGHT - 1)) {
                if (button == 0) {
                    long handle = MinecraftClient.getInstance().getWindow().getHandle();
                    if (GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                            || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS) {
                        module.drawn.setValue(!module.drawn.getValue());
                    } else {
                        module.toggle();
                    }
                } else if (button == 1 && !settingRows.isEmpty()) {
                    this.setExpanded(!expanded);
                }
                return true;
            }

            if (!expanded) {
                return false;
            }

            int childY = y + ROW_HEIGHT;
            for (SettingRow row : settingRows) {
                if (!row.isVisible()) {
                    continue;
                }
                if (row.mouseClicked(mouseX, mouseY, button, x + 1, childY, width - 2)) {
                    return true;
                }
                childY += row.getHeight();
            }
            return false;
        }

        private void mouseReleased(double mouseX, double mouseY, int button) {
            for (SettingRow row : settingRows) {
                row.mouseReleased(mouseX, mouseY, button);
            }
        }

        private boolean mouseDragged(double mouseX, double mouseY, int button) {
            for (SettingRow row : settingRows) {
                if (row.mouseDragged(mouseX, mouseY, button)) {
                    return true;
                }
            }
            return false;
        }

        private boolean keyPressed(int keyCode) {
            for (SettingRow row : settingRows) {
                if (row.keyPressed(keyCode)) {
                    return true;
                }
            }
            return false;
        }

        private boolean charTyped(char chr) {
            for (SettingRow row : settingRows) {
                if (row.charTyped(chr)) {
                    return true;
                }
            }
            return false;
        }
    }

    private abstract class SettingRow {
        protected final Setting setting;
        protected int x;
        protected int y;
        protected int width;

        private SettingRow(Setting setting) {
            this.setting = setting;
        }

        protected boolean isVisible() {
            return setting.isVisible();
        }

        protected int getHeight() {
            return ROW_HEIGHT;
        }

        protected float getTextOffsetY() {
            return (ROW_HEIGHT - getTextHeight()) / 2.0F;
        }

        protected void setBounds(int x, int y, int width) {
            this.x = x;
            this.y = y;
            this.width = width;
        }

        protected void drawRow(DrawContext context, boolean hovered) {
            Render2DUtil.drawRect(context.getMatrices(), x, y, width, ROW_HEIGHT - 1, getSettingBodyColor());
            if (hovered) {
                Render2DUtil.horizontalGradient(
                        context.getMatrices(),
                        x,
                        y,
                        x + width,
                        y + ROW_HEIGHT,
                        getHoverColor(),
                        getHoverEndColor()
                );
            }
        }

        protected abstract void render(DrawContext context, int x, int y, int width, int mouseX, int mouseY);

        protected boolean mouseClicked(double mouseX, double mouseY, int button, int x, int y, int width) {
            setBounds(x, y, width);
            return false;
        }

        protected void mouseReleased(double mouseX, double mouseY, int button) {
        }

        protected boolean mouseDragged(double mouseX, double mouseY, int button) {
            return false;
        }

        protected boolean keyPressed(int keyCode) {
            return false;
        }

        protected boolean charTyped(char chr) {
            return false;
        }
    }

    private final class BooleanRow extends SettingRow {
        private final BooleanSetting booleanSetting;

        private BooleanRow(BooleanSetting setting) {
            super(setting);
            this.booleanSetting = setting;
        }

        @Override
        protected void render(DrawContext context, int x, int y, int width, int mouseX, int mouseY) {
            setBounds(x, y, width);
            boolean boxHovered = isHovered(mouseX, mouseY, x + width - 14, y + 2, 10, 10);
            boolean hovered = isHovered(mouseX, mouseY, x, y, width, ROW_HEIGHT - 1);
            drawRow(context, hovered);

            int textColor = booleanSetting.getValue() ? getTextColor() : getSecondaryTextColor();
            drawText(context, booleanSetting.getName(), x + 4, y + getTextOffsetY(), textColor);

            Render2DUtil.drawRect(context.getMatrices(), x + width - 14, y + 2, 10, 10, boxHovered ? new Color(55, 55, 55, 255) : new Color(16, 16, 16, 255));
            if (booleanSetting.getValue()) {
                Render2DUtil.drawRect(context.getMatrices(), x + width - 13, y + 3, 8, 8, getAccent());
            }

            if (booleanSetting.hasParent()) {
                String marker = booleanSetting.isOpen() ? ClickGui.INSTANCE.close.getValue() : ClickGui.INSTANCE.open.getValue();
                drawText(context, marker, x + width - 24 - getTextWidth(marker), y + getTextOffsetY(), textColor);
            }
        }

        @Override
        protected boolean mouseClicked(double mouseX, double mouseY, int button, int x, int y, int width) {
            setBounds(x, y, width);
            boolean rowHovered = isHovered(mouseX, mouseY, x, y, width, ROW_HEIGHT - 1);
            boolean boxHovered = isHovered(mouseX, mouseY, x + width - 14, y + 2, 10, 10);

            if (!rowHovered) {
                return false;
            }

            if (button == 0) {
                if (boxHovered) {
                    booleanSetting.setValue(!booleanSetting.getValue());
                } else if (booleanSetting.hasParent()) {
                    booleanSetting.setOpen(!booleanSetting.isOpen());
                } else {
                    booleanSetting.setValue(!booleanSetting.getValue());
                }
                return true;
            }

            if (button == 1 && booleanSetting.hasParent()) {
                booleanSetting.setOpen(!booleanSetting.isOpen());
                return true;
            }

            return false;
        }
    }

    private final class SliderRow extends SettingRow {
        private final SliderSetting sliderSetting;
        private boolean dragging;
        private boolean listening;
        private String inputText = "";
        private boolean suppressNextMinusChar;

        private SliderRow(SliderSetting setting) {
            super(setting);
            this.sliderSetting = setting;
        }

        @Override
        protected void render(DrawContext context, int x, int y, int width, int mouseX, int mouseY) {
            setBounds(x, y, width);
            boolean hovered = isHovered(mouseX, mouseY, x, y, width, ROW_HEIGHT - 1);
            drawRow(context, hovered);

            double value = sliderSetting.getValue();
            double range = sliderSetting.getMax() - sliderSetting.getMin();
            double progress = range <= 0.0 ? 0.0 : (value - sliderSetting.getMin()) / range;
            progress = MathHelper.clamp(progress, 0.0, 1.0);
            int fillWidth = (int) ((width - 2) * progress);
            Render2DUtil.drawRect(context.getMatrices(), x, y, fillWidth, ROW_HEIGHT - 1, new Color(getAccent().getRed(), getAccent().getGreen(), getAccent().getBlue(), 150));

            String valueText = listening
                    ? inputText + ((System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "")
                    : formatDouble(value) + sliderSetting.getSuffix();
            drawText(context, sliderSetting.getName(), x + 4, y + getTextOffsetY(), getTextColor());
            drawText(context, valueText, x + width - 5 - getTextWidth(valueText), y + getTextOffsetY(), getTextColor());
        }

        @Override
        protected boolean mouseClicked(double mouseX, double mouseY, int button, int x, int y, int width) {
            setBounds(x, y, width);
            if (!isHovered(mouseX, mouseY, x, y, width, ROW_HEIGHT - 1)) {
                return false;
            }
            if (button == 0) {
                listening = false;
                dragging = true;
                updateValue(mouseX);
                return true;
            }
            if (button == 1) {
                dragging = false;
                listening = true;
                inputText = formatDouble(sliderSetting.getValue());
                suppressNextMinusChar = false;
                return true;
            }
            return false;
        }

        @Override
        protected void mouseReleased(double mouseX, double mouseY, int button) {
            dragging = false;
        }

        @Override
        protected boolean mouseDragged(double mouseX, double mouseY, int button) {
            if (dragging && button == 0) {
                updateValue(mouseX);
                return true;
            }
            return false;
        }

        @Override
        protected boolean keyPressed(int keyCode) {
            if (!listening) {
                return false;
            }

            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                submitInput();
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                listening = false;
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!inputText.isEmpty()) {
                    inputText = inputText.substring(0, inputText.length() - 1);
                }
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_MINUS || keyCode == GLFW.GLFW_KEY_KP_SUBTRACT) {
                appendSliderInput('-');
                suppressNextMinusChar = true;
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_V
                    && (GLFW.glfwGetKey(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                    || GLFW.glfwGetKey(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS)) {
                try {
                    String clipboard = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
                    if (clipboard != null) {
                        for (int i = 0; i < clipboard.length(); i++) {
                            appendSliderInput(clipboard.charAt(i));
                        }
                    }
                } catch (Exception ignored) {
                }
                return true;
            }

            return false;
        }

        @Override
        protected boolean charTyped(char chr) {
            if (!listening) {
                suppressNextMinusChar = false;
                return false;
            }

            if (suppressNextMinusChar && chr == '-') {
                suppressNextMinusChar = false;
                return true;
            }

            suppressNextMinusChar = false;
            return appendSliderInput(chr);
        }

        private boolean appendSliderInput(char chr) {
            if (chr >= '0' && chr <= '9') {
                inputText += chr;
                return true;
            }
            if (chr == '.' && !inputText.contains(".")) {
                inputText = inputText.isEmpty() || "-".equals(inputText) ? inputText + "0." : inputText + ".";
                return true;
            }
            if (chr == '-' && inputText.isEmpty()) {
                inputText = "-";
                return true;
            }
            return false;
        }

        private void submitInput() {
            if (inputText.isEmpty() || "-".equals(inputText) || ".".equals(inputText) || "-.".equals(inputText)) {
                return;
            }

            try {
                double value = Double.parseDouble(inputText);
                value = MathHelper.clamp(value, sliderSetting.getMin(), sliderSetting.getMax());
                sliderSetting.setValue(value);
                listening = false;
            } catch (NumberFormatException ignored) {
            }
        }

        private void updateValue(double mouseX) {
            double percent = (mouseX - x) / width;
            percent = MathHelper.clamp(percent, 0.0, 1.0);
            sliderSetting.setValue(sliderSetting.getMin() + sliderSetting.getRange() * percent);
        }
    }

    private final class EnumRow extends SettingRow {
        private final EnumSetting<?> enumSetting;

        private EnumRow(EnumSetting<?> setting) {
            super(setting);
            this.enumSetting = setting;
        }

        @Override
        protected void render(DrawContext context, int x, int y, int width, int mouseX, int mouseY) {
            setBounds(x, y, width);
            boolean hovered = isHovered(mouseX, mouseY, x, y, width, ROW_HEIGHT - 1);
            drawRow(context, hovered);
            String value = enumSetting.getValue().name();
            drawText(context, enumSetting.getName(), x + 4, y + getTextOffsetY(), getTextColor());
            drawText(context, value, x + width - 5 - getTextWidth(value), y + getTextOffsetY(), getSecondaryTextColor());
        }

        @Override
        protected boolean mouseClicked(double mouseX, double mouseY, int button, int x, int y, int width) {
            setBounds(x, y, width);
            if (!isHovered(mouseX, mouseY, x, y, width, ROW_HEIGHT - 1)) {
                return false;
            }
            if (button == 0) {
                enumSetting.increaseEnum();
                return true;
            }
            if (button == 1) {
                Object[] constants = enumSetting.getValue().getDeclaringClass().getEnumConstants();
                int current = enumSetting.getValue().ordinal();
                int previous = (current - 1 + constants.length) % constants.length;
                enumSetting.setEnumValue(constants[previous].toString());
                return true;
            }
            return false;
        }
    }

    private final class BindRow extends SettingRow {
        private final BindSetting bindSetting;
        private boolean listening;

        private BindRow(BindSetting setting) {
            super(setting);
            this.bindSetting = setting;
        }

        @Override
        protected void render(DrawContext context, int x, int y, int width, int mouseX, int mouseY) {
            setBounds(x, y, width);
            boolean hovered = isHovered(mouseX, mouseY, x, y, width, ROW_HEIGHT - 1);
            drawRow(context, hovered);

            String text = listening ? "Press key..." : bindSetting.getName() + ": " + bindSetting.getKeyString();
            if (hovered && "Key".equalsIgnoreCase(bindSetting.getName())
                    && GLFW.glfwGetKey(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS) {
                text = "Hold " + (bindSetting.isHoldEnable() ? "On" : "Off");
            }
            drawText(context, text, x + 4, y + getTextOffsetY(), getTextColor());
        }

        @Override
        protected boolean mouseClicked(double mouseX, double mouseY, int button, int x, int y, int width) {
            setBounds(x, y, width);
            if (listening) {
                bindSetting.setValue(-button - 2);
                listening = false;
                return true;
            }

            if (!isHovered(mouseX, mouseY, x, y, width, ROW_HEIGHT - 1) || button != 0) {
                return false;
            }

            if ("Key".equalsIgnoreCase(bindSetting.getName())
                    && GLFW.glfwGetKey(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS) {
                bindSetting.setHoldEnable(!bindSetting.isHoldEnable());
            } else {
                listening = !listening;
            }
            return true;
        }

        @Override
        protected boolean keyPressed(int keyCode) {
            if (!listening) {
                return false;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                bindSetting.setValue(-1);
            } else {
                bindSetting.setValue(keyCode);
            }
            listening = false;
            return true;
        }
    }

    private final class StringRow extends SettingRow {
        private final StringSetting stringSetting;
        private boolean listening;
        private String currentText = "";
        private boolean suppressNextMinusChar;

        private StringRow(StringSetting setting) {
            super(setting);
            this.stringSetting = setting;
        }

        @Override
        protected void render(DrawContext context, int x, int y, int width, int mouseX, int mouseY) {
            setBounds(x, y, width);
            boolean hovered = isHovered(mouseX, mouseY, x, y, width, ROW_HEIGHT - 1);
            drawRow(context, hovered);

            String value = listening ? currentText + ((System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "") : stringSetting.getValue();
            String prefix = listening ? "[E]" : stringSetting.getName();
            drawText(context, prefix, x + 4, y + getTextOffsetY(), getSecondaryTextColor());
            drawText(context, value, x + 8 + getTextWidth(prefix) / 2.0, y + getTextOffsetY(), getTextColor());
        }

        @Override
        protected boolean mouseClicked(double mouseX, double mouseY, int button, int x, int y, int width) {
            setBounds(x, y, width);
            if (!isHovered(mouseX, mouseY, x, y, width, ROW_HEIGHT - 1)) {
                listening = false;
                return false;
            }
            
            if (button == 0) {
                listening = !listening;
                if (listening) {
                    currentText = stringSetting.getValue();
                }
                return true;
            } else if (button == 1) {
                try {
                    String clipboard = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
                    if (clipboard != null) {
                        if (listening) {
                            currentText += clipboard;
                        } else {
                            stringSetting.setValue(stringSetting.getValue() + clipboard);
                        }
                    }
                } catch (Exception ignored) {
                }
                return true;
            }
            return false;
        }

        @Override
        protected boolean keyPressed(int keyCode) {
            if (!listening) {
                return false;
            }

            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                stringSetting.setValue(currentText);
                listening = false;
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                listening = false;
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!currentText.isEmpty()) {
                    currentText = currentText.substring(0, currentText.length() - 1);
                }
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_MINUS || keyCode == GLFW.GLFW_KEY_KP_SUBTRACT) {
                currentText += "-";
                suppressNextMinusChar = true;
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_V
                    && (GLFW.glfwGetKey(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                    || GLFW.glfwGetKey(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS)) {
                try {
                    currentText += Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
                } catch (Exception ignored) {
                }
                return true;
            }

            return false;
        }

        @Override
        protected boolean charTyped(char chr) {
            if (!listening || !isAllowedCharacter(chr)) {
                if (chr != '-') {
                    suppressNextMinusChar = false;
                }
                return false;
            }

            if (suppressNextMinusChar && chr == '-') {
                suppressNextMinusChar = false;
                return true;
            }

            suppressNextMinusChar = false;
            currentText += chr;
            return true;
        }
    }

    private final class ColorRow extends SettingRow {
        private final ColorSetting colorSetting;
        private boolean expanded;
        private boolean draggingPicker;
        private boolean draggingHue;
        private boolean draggingAlpha;
        private float hue;
        private float saturation;
        private float brightness;
        private float alpha;

        private ColorRow(ColorSetting setting) {
            super(setting);
            this.colorSetting = setting;
            syncFromSetting();
        }

        @Override
        protected int getHeight() {
            return expanded ? 64 : ROW_HEIGHT;
        }

        @Override
        protected void render(DrawContext context, int x, int y, int width, int mouseX, int mouseY) {
            setBounds(x, y, width);
            boolean hovered = isHovered(mouseX, mouseY, x, y, width, ROW_HEIGHT - 1);
            drawRow(context, hovered);

            drawText(context, colorSetting.getName(), x + 4, y + getTextOffsetY(), getTextColor());

            if (colorSetting.injectBoolean) {
                Render2DUtil.drawRect(context.getMatrices(), x + width - 52, y + 3, 8, 8, new Color(16, 16, 16, 255));
                if (colorSetting.booleanValue) {
                    Render2DUtil.drawRect(context.getMatrices(), x + width - 51, y + 4, 6, 6, getAccent());
                }
            }

            Render2DUtil.drawRect(context.getMatrices(), x + width - 40, y + 3, 8, 8, new Color(16, 16, 16, 255));
            if (colorSetting.pulse) {
                drawText(context, "P", x + width - 39, y + 1, getTextColor());
            }

            Render2DUtil.drawRect(context.getMatrices(), x + width - 28, y + 3, 8, 8, new Color(16, 16, 16, 255));
            if (colorSetting.sync) {
                drawText(context, "S", x + width - 27, y + 1, getTextColor());
            }

            Render2DUtil.drawRect(context.getMatrices(), x + width - 16, y + 3, 10, 8, new Color(16, 16, 16, 255));
            Color c = colorSetting.getValue();
            Render2DUtil.drawRect(context.getMatrices(), x + width - 15, y + 4, 8, 6, new Color(c.getRed(), c.getGreen(), c.getBlue(), 255));

            if (!expanded) {
                return;
            }

            float pickerX = x + 4;
            float pickerY = y + ROW_HEIGHT + 3;
            float pickerWidth = width - 18;
            float pickerHeight = 34;
            float hueX = pickerX + pickerWidth + 2;
            float alphaY = pickerY + pickerHeight + 4;

            Render2DUtil.drawRect(context.getMatrices(), x, y + ROW_HEIGHT - 1, width, getHeight() - ROW_HEIGHT + 1, new Color(14, 14, 14, 130));

            Color hueColor = Color.getHSBColor(hue, 1.0F, 1.0F);
            Render2DUtil.horizontalGradient(context.getMatrices(), pickerX, pickerY, pickerX + pickerWidth, pickerY + pickerHeight, Color.WHITE, hueColor);
            Render2DUtil.verticalGradient(context.getMatrices(), pickerX, pickerY, pickerX + pickerWidth, pickerY + pickerHeight, new Color(0, 0, 0, 0), Color.BLACK);

            for (int i = 0; i < pickerHeight; i++) {
                float progress = i / pickerHeight;
                Render2DUtil.drawRect(context.getMatrices(), hueX, pickerY + i, 4, 1, Color.getHSBColor(progress, 1.0F, 1.0F));
            }

            Color base = Color.getHSBColor(hue, saturation, brightness);
            Render2DUtil.horizontalGradient(
                    context.getMatrices(),
                    pickerX,
                    alphaY,
                    pickerX + pickerWidth,
                    alphaY + 4,
                    new Color(base.getRed(), base.getGreen(), base.getBlue(), 0),
                    new Color(base.getRed(), base.getGreen(), base.getBlue(), 255)
            );

            float sx = pickerX + saturation * pickerWidth;
            float sy = pickerY + (1.0F - brightness) * pickerHeight;
            Render2DUtil.drawRect(context.getMatrices(), sx - 1, sy - 1, 3, 3, Color.WHITE);

            float hueY = pickerY + hue * pickerHeight;
            Render2DUtil.drawRect(context.getMatrices(), hueX - 1, hueY, 6, 1, Color.WHITE);

            float alphaX = pickerX + alpha * pickerWidth;
            Render2DUtil.drawRect(context.getMatrices(), alphaX, alphaY - 1, 1, 6, Color.WHITE);
        }

        @Override
        protected boolean mouseClicked(double mouseX, double mouseY, int button, int x, int y, int width) {
            setBounds(x, y, width);

            boolean rowHovered = isHovered(mouseX, mouseY, x, y, width, ROW_HEIGHT - 1);
            if (rowHovered) {
                if (button == 1) {
                    expanded = !expanded;
                    return true;
                }

                if (button == 0) {
                    if (colorSetting.injectBoolean && isHovered(mouseX, mouseY, x + width - 52, y + 3, 8, 8)) {
                        colorSetting.booleanValue = !colorSetting.booleanValue;
                        return true;
                    }
                    if (isHovered(mouseX, mouseY, x + width - 40, y + 3, 8, 8)) {
                        colorSetting.pulse = !colorSetting.pulse;
                        return true;
                    }
                    if (isHovered(mouseX, mouseY, x + width - 28, y + 3, 8, 8)) {
                        colorSetting.setSync(!colorSetting.sync);
                        return true;
                    }
                }
            }

            if (!expanded || button != 0) {
                return rowHovered;
            }

            float pickerX = x + 4;
            float pickerY = y + ROW_HEIGHT + 3;
            float pickerWidth = width - 18;
            float pickerHeight = 34;
            float hueX = pickerX + pickerWidth + 2;
            float alphaY = pickerY + pickerHeight + 4;

            if (isHovered(mouseX, mouseY, pickerX, pickerY, pickerWidth, pickerHeight)) {
                draggingPicker = true;
                updatePicker(mouseX, mouseY);
                return true;
            }
            if (isHovered(mouseX, mouseY, hueX, pickerY, 4, pickerHeight)) {
                draggingHue = true;
                updateHue(mouseY);
                return true;
            }
            if (isHovered(mouseX, mouseY, pickerX, alphaY, pickerWidth, 4)) {
                draggingAlpha = true;
                updateAlpha(mouseX);
                return true;
            }

            return rowHovered;
        }

        @Override
        protected void mouseReleased(double mouseX, double mouseY, int button) {
            draggingPicker = false;
            draggingHue = false;
            draggingAlpha = false;
        }

        @Override
        protected boolean mouseDragged(double mouseX, double mouseY, int button) {
            if (button != 0) {
                return false;
            }
            if (draggingPicker) {
                updatePicker(mouseX, mouseY);
                return true;
            }
            if (draggingHue) {
                updateHue(mouseY);
                return true;
            }
            if (draggingAlpha) {
                updateAlpha(mouseX);
                return true;
            }
            return false;
        }

        private void syncFromSetting() {
            Color current = colorSetting.getValue();
            float[] hsb = Color.RGBtoHSB(current.getRed(), current.getGreen(), current.getBlue(), null);
            hue = hsb[0];
            saturation = hsb[1];
            brightness = hsb[2];
            alpha = current.getAlpha() / 255.0F;
        }

        private void updatePicker(double mouseX, double mouseY) {
            float pickerX = x + 4;
            float pickerY = y + ROW_HEIGHT + 3;
            float pickerWidth = width - 18;
            float pickerHeight = 34;
            saturation = MathHelper.clamp((float) ((mouseX - pickerX) / pickerWidth), 0.0F, 1.0F);
            brightness = 1.0F - MathHelper.clamp((float) ((mouseY - pickerY) / pickerHeight), 0.0F, 1.0F);
            applyColor();
        }

        private void updateHue(double mouseY) {
            float pickerY = y + ROW_HEIGHT + 3;
            float pickerHeight = 34;
            hue = MathHelper.clamp((float) ((mouseY - pickerY) / pickerHeight), 0.0F, 1.0F);
            applyColor();
        }

        private void updateAlpha(double mouseX) {
            float pickerX = x + 4;
            float pickerWidth = width - 18;
            alpha = MathHelper.clamp((float) ((mouseX - pickerX) / pickerWidth), 0.0F, 1.0F);
            applyColor();
        }

        private void applyColor() {
            Color color = Color.getHSBColor(hue, saturation, brightness);
            colorSetting.setValue(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.round(alpha * 255.0F)));
        }
    }

    private static final class Snow {
        private int x;
        private int y;
        private int fallingSpeed;
        private int size;

        private Snow(int x, int y, int fallingSpeed, int size) {
            this.x = x;
            this.y = y;
            this.fallingSpeed = fallingSpeed;
            this.size = size;
        }

        private void draw(DrawContext context) {
            Render2DUtil.drawRect(context.getMatrices(), x, y, size, size, new Color(153, 193, 255, 120));
            y += fallingSpeed;
            int height = MinecraftClient.getInstance().getWindow().getScaledHeight();
            if (y > height + 10 || y < -10) {
                y = -10;
                Random random = new Random();
                fallingSpeed = random.nextInt(3) + 1;
                size = random.nextInt(2) + 1;
            }
        }
    }
}
