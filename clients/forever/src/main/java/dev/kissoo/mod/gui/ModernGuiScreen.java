package dev.kissoo.mod.gui;

import dev.kissoo.mod.modules.impl.client.ClickGui;
import dev.kissoo.Forever;
import dev.kissoo.api.utils.render.Render2DUtil;
import dev.kissoo.core.impl.CommandManager;
import hoprc.api.core.impl.FontManager;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.Setting;
import dev.kissoo.mod.modules.settings.impl.BindSetting;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import dev.kissoo.mod.modules.settings.impl.StringSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.systems.RenderSystem;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class ModernGuiScreen extends Screen {
    private final ClickGui module;

    private final int V_WIDTH = 840;
    private final int V_HEIGHT = 520;
    private static Module.Category selectedCategory = Module.Category.Combat;

    private float scrollTarget = 0;
    private float scrollCurrent = 0;
    private float maxScroll = 0;

    private final Map<String, Float> animationMap = new HashMap<>();
    private static final Set<Module> expandedModules = new HashSet<>();
    private static boolean initializedExpanded = false;

    private final Set<ColorSetting> expandedColors = new HashSet<>();

    private final List<ClickAction> clickActions = new ArrayList<>();

    private SliderSetting draggingSlider = null;
    private double dragStartX = 0;
    private double dragStartValue = 0;
    private float dragStartScale = 1.0f;

    private ColorSetting pickingColorSV = null;
    private ColorSetting pickingColorHue = null;
    private ColorSetting pickingColorAlpha = null;

    private double physicalMouseX = 0;
    private double physicalMouseY = 0;
    private float currentScale = 0f;

    private BindSetting activeBind = null;
    private StringSetting activeStringSetting = null;
    private EnumSetting<?> expandedEnum = null;

    private String searchText = "";
    private boolean searchFocused = false;

    private final Color C_ACCENT     = new Color(75, 110, 240, 255);
    private final Color C_TEXT_TITLE = new Color(240, 240, 245, 255);
    private final Color C_TEXT_MUTED = new Color(130, 130, 135, 255);
    private final Color C_TOGGLE_OFF = new Color(40, 40, 45, 255);
    private final Color C_HOVER      = new Color(35, 35, 40, 255);

    public ModernGuiScreen(ClickGui module) {
        super(Text.literal("Modern ClickGui"));
        this.module = module;

        module.tintColor.booleanValue = false;
        if(module.toggleShader.getValue()) module.toggleShader.setValue(false);

        // ==================== [此处已修改：动态强制将 Scale 上限设为 1.0] ====================
        module.modernScale.setRuntimeMax(1.0);

        // 如果当前保存的配置大于 1.0，则强制重置为 1.0
        if (module.modernScale.getValue() > 1.0) {
            module.modernScale.setValue(1.0);
        }
        // =================================================================================

        if (!initializedExpanded) {
            for(Module m : Forever.MODULE.getModules()) {
                if(m.getName().equalsIgnoreCase("Aura") || m.getName().equalsIgnoreCase("Velocity") || m.getName().equalsIgnoreCase("PearlRun")) {
                    expandedModules.add(m);
                }
            }
            initializedExpanded = true;
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}
    public void renderInGameBackground(DrawContext context) {}

    private void drawCustomText(DrawContext context, String text, float x, float y, Color color) {
        drawCustomText(context, text, x, y, color, ClickGui.getInstance().modernFontSize.getValue().getSize());
    }

    private void drawCustomText(DrawContext context, String text, float x, float y, Color color, float baseSize) {
        if (ClickGui.getInstance().font.getValue()) {
            float renderScale = Math.max(0.01f, currentScale);
            float targetPhysicalSize = baseSize * renderScale;

            context.getMatrices().push();
            context.getMatrices().translate(x, y, 0);
            context.getMatrices().scale(1f / renderScale, 1f / renderScale, 1f);

            FontManager.get(targetPhysicalSize).drawString(context.getMatrices(), text, 0, 0,
                    color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f, false);

            context.getMatrices().pop();
        } else {
            float scale = baseSize / 8.0f;
            context.getMatrices().push();
            context.getMatrices().translate(x, y, 0);
            context.getMatrices().scale(scale, scale, 1f);
            context.drawText(client.textRenderer, text, 0, 0, color.getRGB(), false);
            context.getMatrices().pop();
        }
    }

    private int getCustomTextWidth(String text) {
        return getCustomTextWidth(text, ClickGui.getInstance().modernFontSize.getValue().getSize());
    }

    private int getCustomTextWidth(String text, float baseSize) {
        if (ClickGui.getInstance().font.getValue()) {
            float renderScale = Math.max(0.01f, currentScale);
            float targetPhysicalSize = baseSize * renderScale;
            return (int) (FontManager.get(targetPhysicalSize).getWidth(text) / renderScale);
        } else {
            float scale = baseSize / 8.0f;
            return (int) (client.textRenderer.getWidth(text) * scale);
        }
    }

    private void drawCutRect(DrawContext context, int x, int y, int w, int h, int cut, Color color) {
        int c = color.getRGB();
        context.fill(x + cut, y, x + w - cut, y + h, c);
        context.fill(x, y + cut, x + cut, y + h - cut, c);
        context.fill(x + w - cut, y + cut, x + w, y + h - cut, c);

        for (int i = 0; i < cut; i++) {
            int wStep = i + 1;
            context.fill(x + cut - wStep, y + i, x + cut, y + i + 1, c);
            context.fill(x + w - cut, y + i, x + w - cut + wStep, y + i + 1, c);
            context.fill(x + cut - wStep, y + h - 1 - i, x + cut, y + h - i, c);
            context.fill(x + w - cut, y + h - 1 - i, x + w - cut + wStep, y + h - i, c);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        clickActions.clear();
        scrollCurrent += (scrollTarget - scrollCurrent) * 0.3f;

        this.physicalMouseX = mouseX;
        this.physicalMouseY = mouseY;

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

        // ==================== [此处已修改：渲染端进行安全拦截，确保不会突破 1.0f] ====================
        float targetScale = Math.min(1.0f, module.modernScale.getValueFloat());
        // =========================================================================================

        if (currentScale == 0f) {
            currentScale = targetScale;
        } else {
            currentScale += (targetScale - currentScale) * 0.15f;
        }
        float scale = currentScale;

        if (Forever.BLUR != null) Forever.BLUR.applyBlur(25f, 0, 0, screenW, screenH);

        context.getMatrices().push();
        context.getMatrices().translate(screenW / 2f, screenH / 2f, 0);
        context.getMatrices().scale(scale, scale, 1f);
        context.getMatrices().translate(-V_WIDTH / 2f, -V_HEIGHT / 2f, 0);

        double vMouseX = (mouseX - screenW / 2.0) / scale + (V_WIDTH / 2.0);
        double vMouseY = (mouseY - screenH / 2.0) / scale + (V_HEIGHT / 2.0);

        int sidebarW = 180;

        int pX = (int) ((-V_WIDTH / 2f) * scale + screenW / 2f);
        int pY = (int) ((-V_HEIGHT / 2f) * scale + screenH / 2f);
        int pSidebarW = (int) (sidebarW * scale);
        int pMainW = (int) ((V_WIDTH - sidebarW) * scale);
        int pH = (int) (V_HEIGHT * scale);

        context.enableScissor(pX, pY, pX + pSidebarW, pY + pH);
        Render2DUtil.drawRoundedRect(context.getMatrices(), 0, 0, sidebarW + 20, V_HEIGHT, 12f, module.modernSidebarColor.getValue());
        renderSidebar(context, vMouseX, vMouseY);
        context.disableScissor();

        int rightPad = (int) (30 * scale);
        context.enableScissor(pX + pSidebarW, pY, pX + pSidebarW + pMainW + rightPad, pY + pH);
        Render2DUtil.drawRoundedRect(context.getMatrices(), sidebarW - 20, 0, V_WIDTH - sidebarW + 20, V_HEIGHT, 12f, module.modernMainColor.getValue());
        renderMainContent(context, vMouseX, vMouseY, sidebarW, 0, V_WIDTH - sidebarW);
        context.disableScissor();

        context.getMatrices().pop();
    }

    private void renderSidebar(DrawContext context, double mx, double my) {
        int sidebarW = 180;
        drawCustomText(context, "ForeverClient", 25, 25, C_TEXT_TITLE);
        drawCustomText(context, "ClickGui", 25, 40, C_TEXT_MUTED);
        int catY = 75;
        for (Module.Category category : Module.Category.values()) {
            boolean isSelected = (selectedCategory == category);
            int itemH = 36;
            boolean hovered = isHovering(mx, my, 15, catY, sidebarW - 30, itemH);
            String animKey = "CAT_" + category.name();
            float anim = getAnim(animKey);
            setAnim(animKey, anim + ((hovered || isSelected ? 1f : 0f) - anim) * 0.2f);

            if (anim > 0.01f) {
                Color bgCol = new Color(C_HOVER.getRed(), C_HOVER.getGreen(), C_HOVER.getBlue(), (int)(255 * (isSelected ? 1f : anim * 0.6f)));
                Render2DUtil.drawRoundedRect(context.getMatrices(), 15, catY, sidebarW - 30, itemH, 6f, bgCol);
            }

            Color textColor = isSelected ? C_TEXT_TITLE : blendColor(C_TEXT_MUTED, C_TEXT_TITLE, anim);
            drawCustomText(context, category.name(), 35, catY + (itemH - 8)/2, textColor);

            int finalCatY = catY;
            clickActions.add(new ClickAction(15, finalCatY, sidebarW - 30, itemH, (btn) -> {
                if (btn == 0 && selectedCategory != category) {
                    selectedCategory = category;
                    scrollTarget = 0;
                }
            }));
            catY += itemH + 6;
        }

        int profileY = V_HEIGHT - 70;
        Render2DUtil.drawRoundedRect(context.getMatrices(), 15, profileY, sidebarW - 30, 55, 10f, C_TOGGLE_OFF);

        Identifier skinId = null;
        if (client.player != null) {
            skinId = client.player.getSkinTextures().texture();
        }

        if (skinId != null) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            context.drawTexture(skinId, 25, profileY + 12, 32, 32, 8, 8, 8, 8, 64, 64);
            context.drawTexture(skinId, 25, profileY + 12, 32, 32, 40, 8, 8, 8, 64, 64);
            RenderSystem.disableBlend();
        }

        String userName = client.getSession().getUsername();
        drawCustomText(context, userName, 65, profileY + 18, C_TEXT_TITLE);
        drawCustomText(context, "Welcome", 65, profileY + 32, C_TEXT_MUTED, 6.5f);
    }

    private void renderMainContent(DrawContext context, double mx, double my, int rx, int ry, int rWidth) {
        List<Module> allModules = getModulesByCategory(selectedCategory);

        List<Module> modules = new ArrayList<>();
        if (searchText.isEmpty()) {
            modules.addAll(allModules);
        } else {
            String low = searchText.toLowerCase();
            for (Module m : allModules) {
                if (m.getName().toLowerCase().contains(low)) modules.add(m);
            }
        }

        float currentY = ry + 25 - scrollCurrent;
        int searchBoxX = rx + rWidth - 170;
        int searchBoxY = (int) currentY;
        int searchBoxW = 140;
        int searchBoxH = 22;
        Color searchBg = searchFocused ? C_HOVER : C_TOGGLE_OFF;
        context.fill(searchBoxX, searchBoxY, searchBoxX + searchBoxW, searchBoxY + searchBoxH, searchBg.getRGB());

        String dispSearch;
        if (searchText.isEmpty() && !searchFocused) {
            dispSearch = "Search...";
            drawCustomText(context, dispSearch, searchBoxX + 10, searchBoxY + 7, C_TEXT_MUTED);
        } else {
            dispSearch = searchText;
            if (searchFocused && (System.currentTimeMillis() % 1000 < 500)) dispSearch += "_";
            drawCustomText(context, dispSearch, searchBoxX + 10, searchBoxY + 7, C_TEXT_TITLE);
        }

        final int fSX = searchBoxX, fSY = searchBoxY, fSW = searchBoxW, fSH = searchBoxH;
        clickActions.add(new ClickAction(fSX, fSY, fSW, fSH, (btn) -> {
            if (btn == 0) {
                searchFocused = true;
                activeStringSetting = null;
                activeBind = null;
            } else if (btn == 1) {
                searchText = "";
                searchFocused = false;
            }
        }));

        currentY += 50;

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();
        float scale = currentScale;

        for (Module mod : modules) {
            renderModuleHeader(context, mod, rx + 30, (int)currentY, rWidth - 60, mx, my);
            currentY += 35;

            String expandKey = "MOD_EXPAND_" + mod.getName();
            float progress = getAnim(expandKey);
            float target = expandedModules.contains(mod) ? 1.0f : 0.0f;
            progress += (target - progress) * 0.16f;
            setAnim(expandKey, progress);

            if (progress > 0.01f) {
                List<Setting> visibleSettings = new ArrayList<>();
                for (Setting s : mod.getSettings()) if (s.isVisible()) visibleSettings.add(s);

                int colWidth = (rWidth - 90) / 2;

                float col1Height = 0;
                float col2Height = 0;
                for (Setting setting : visibleSettings) {
                    boolean isLeft = col1Height <= col2Height;
                    int sH = getSettingHeight(setting, colWidth);
                    if (isLeft) col1Height += sH + 10;
                    else col2Height += sH + 10;
                }
                float totalSettingsHeight = Math.max(col1Height, col2Height);
                float animatedHeight = totalSettingsHeight * progress;

                int sX = (int) (((rx + 30 - V_WIDTH / 2.0f) * scale) + screenW / 2.0f);
                int sY = (int) (((currentY - V_HEIGHT / 2.0f) * scale) + screenH / 2.0f);
                int sW = (int) (((rWidth - 60) + 16) * scale);
                int scissorH = (int) (animatedHeight * scale);

                context.enableScissor(sX, sY, sX + sW, sY + scissorH);

                float col1Y = currentY;
                float col2Y = currentY;
                for (Setting setting : visibleSettings) {
                    boolean isLeft = col1Y <= col2Y;
                    float setX = rx + 30 + (isLeft ? 0 : colWidth + 30);
                    float setY = isLeft ? col1Y : col2Y;
                    int sH = renderSetting(context, setting, (int)setX, (int)setY, colWidth, mx, my);
                    if (isLeft) col1Y += sH + 10;
                    else col2Y += sH + 10;
                }

                context.disableScissor();
                currentY += animatedHeight + 15;
            } else {
                currentY += 10;
            }
        }

        maxScroll = currentY + scrollCurrent - ry - V_HEIGHT + 30;
        if (maxScroll < 0) maxScroll = 0;
        if (scrollTarget > maxScroll) scrollTarget = maxScroll;
        if (scrollTarget < 0) scrollTarget = 0;
    }

    private void renderModuleHeader(DrawContext context, Module mod, int sx, int sy, int sWidth, double mx, double my) {
        drawCustomText(context, mod.getName(), sx, sy + 6, C_TEXT_MUTED);

        boolean isOn = mod.isOn();
        String animKey = "MOD_TOGGLE_" + mod.getName();
        float anim = getAnim(animKey);
        setAnim(animKey, anim + ((isOn ? 1f : 0f) - anim) * 0.3f);

        String iconStr = isOn ? FontManager.ICON_ENABLED : FontManager.ICON_DISABLED;
        Color iconColor = blendColor(C_TEXT_MUTED, C_ACCENT, anim);

        float iconSize = 28.0f;
        float targetPhysicalSize = iconSize * currentScale;
        float iconWidth = FontManager.getIconNewFont(targetPhysicalSize).getWidth(iconStr) / Math.max(0.01f, currentScale);

        float iconX = sx + sWidth - iconWidth;
        float iconY = sy - 3;

        int textW = getCustomTextWidth(mod.getName()) + 8;
        int lineStart = sx + textW + 15;
        int lineEnd = (int)(iconX - 16);
        if (lineEnd > lineStart) {
            Render2DUtil.drawLine(context.getMatrices(), lineStart, sy + 10, lineEnd, sy + 10, C_TOGGLE_OFF.getRGB());
        }

        context.getMatrices().push();
        context.getMatrices().translate(iconX, iconY, 0);
        context.getMatrices().scale(1f / Math.max(0.01f, currentScale), 1f / Math.max(0.01f, currentScale), 1f);

        FontManager.getIconNewFont(targetPhysicalSize).drawString(context.getMatrices(), iconStr, 0, 0,
                iconColor.getRed() / 255f, iconColor.getGreen() / 255f, iconColor.getBlue() / 255f, iconColor.getAlpha() / 255f, false);

        context.getMatrices().pop();

        final float clickIconX = iconX - 6;
        clickActions.add(new ClickAction(sx, sy, sWidth, 28, (btn) -> {
            if (btn == 0) {
                if (mx >= clickIconX) mod.toggle();
            } else if (btn == 1) {
                if (expandedModules.contains(mod)) expandedModules.remove(mod);
                else expandedModules.add(mod);
            }
        }));
    }

    private int renderSetting(DrawContext context, Setting setting, int sx, int sy, int sWidth, double mx, double my) {
        int height = 24;

        if (setting instanceof BooleanSetting boolSetting) {
            String name = boolSetting.getName();
            if (boolSetting.isOpen()) name += " -";
            else name += " +";

            drawCustomText(context, name, sx, sy + 6, boolSetting.isOpen() ? C_TEXT_TITLE : C_TEXT_MUTED);

            boolean isOn = boolSetting.getValue();
            String animKey = "TOGGLE_" + setting.hashCode();
            float anim = getAnim(animKey);
            setAnim(animKey, anim + ((isOn ? 1f : 0f) - anim) * 0.3f);

            String iconStr = isOn ? FontManager.ICON_ENABLED : FontManager.ICON_DISABLED;
            Color iconColor = blendColor(C_TEXT_MUTED, C_ACCENT, anim);

            float iconSize = 24.0f;
            float targetPhysicalSize = iconSize * currentScale;
            float iconWidth = FontManager.getIconNewFont(targetPhysicalSize).getWidth(iconStr) / Math.max(0.01f, currentScale);
            float iconX = sx + sWidth - iconWidth;
            float iconY = sy - 1;

            context.getMatrices().push();
            context.getMatrices().translate(iconX, iconY, 0);
            context.getMatrices().scale(1f / Math.max(0.01f, currentScale), 1f / Math.max(0.01f, currentScale), 1f);
            FontManager.getIconNewFont(targetPhysicalSize).drawString(context.getMatrices(), iconStr, 0, 0,
                    iconColor.getRed() / 255f, iconColor.getGreen() / 255f, iconColor.getBlue() / 255f, iconColor.getAlpha() / 255f, false);
            context.getMatrices().pop();

            final float clickIconX = iconX - 6;
            clickActions.add(new ClickAction(sx, sy, sWidth, 24, (btn) -> {
                if(btn == 0 && mx >= clickIconX) {
                    boolSetting.setValue(!isOn);
                    if (boolSetting.task != null && boolSetting.injectTask) boolSetting.task.run();
                } else if(btn == 1) {
                    boolSetting.setOpen(!boolSetting.isOpen());
                }
            }));
        }
        else if (setting instanceof SliderSetting slider) {
            drawCustomText(context, slider.getName(), sx, sy + 6, C_TEXT_TITLE);
            String valStr = String.format("%.2f", slider.getValueFloat());
            int textW = getCustomTextWidth(valStr);
            drawCustomText(context, valStr, sx + sWidth - textW, sy + 6, C_TEXT_MUTED);

            int trackW = sWidth / 2 - 25, trackX = sx + sWidth / 2 - 10, trackY = sy + 11;

            if (draggingSlider == slider) {
                double deltaX = physicalMouseX - dragStartX;
                double physicalTrackW = trackW * dragStartScale;
                double deltaPercent = deltaX / physicalTrackW;

                double newVal = dragStartValue + (slider.getMax() - slider.getMin()) * deltaPercent;
                newVal = Math.max(slider.getMin(), Math.min(slider.getMax(), newVal));

                if (slider.getIncrement() > 0) {
                    newVal = Math.round(newVal / slider.getIncrement()) * slider.getIncrement();
                }
                slider.setValue(newVal);
                if (slider.task != null && slider.injectTask) slider.task.run();
            }

            Render2DUtil.drawRoundedRect(context.getMatrices(), trackX, trackY, trackW, 3f, 1.5f, C_TOGGLE_OFF);
            float fillPercent = Math.max(0, Math.min(1, (float) ((slider.getValueFloat() - slider.getMin()) / (slider.getMax() - slider.getMin()))));
            if (fillPercent > 0.02f) Render2DUtil.drawRoundedRect(context.getMatrices(), trackX, trackY, trackW * fillPercent, 3f, 1.5f, C_ACCENT);

            // ==================== [此处已修改：圆形改为字体符号] ====================
            String knobSymbol = "●"; // 你可以更改为其他符号，例如 "◆" (菱形), "■" (方块) 或自定义字体字符
            float knobFontSize = 12f; // 符号的大小
            int symbolW = getCustomTextWidth(knobSymbol, knobFontSize);
            float symbolX = trackX + (trackW * fillPercent) - (symbolW / 2f);
            float symbolY = trackY + 1.5f - (knobFontSize / 2f) - 1f; // 垂直居中微调偏移

            drawCustomText(context, knobSymbol, symbolX, symbolY, Color.WHITE, knobFontSize);
            // ====================================================================

            clickActions.add(new ClickAction(trackX - 5, trackY - 5, trackW + 10, 14, (btn) -> {
                if(btn==0) {
                    draggingSlider = slider;
                    dragStartX = physicalMouseX;
                    dragStartValue = slider.getValue();
                    dragStartScale = currentScale;
                }
            }));
        }
        else if (setting instanceof EnumSetting<?> enumSetting) {
            boolean isExpanded = (expandedEnum == enumSetting);
            Object[] values = enumSetting.getValue().getClass().getEnumConstants();
            int listHeight = isExpanded && values != null ? (values.length * 16 + 4) : 0;

            height = 24 + listHeight;

            drawCustomText(context, enumSetting.getName(), sx, sy + 6, C_TEXT_TITLE);
            int boxW = 85, boxX = sx + sWidth - boxW;

            Color bgTrans = new Color(75, 75, 80, 120);
            Color hoverTrans = new Color(95, 95, 100, 140);
            Color boxColor = isExpanded ? hoverTrans : (isHovering(mx, my, boxX, sy + 2, boxW, 20) ? hoverTrans : bgTrans);

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            context.fill(boxX, sy + 2, boxX + boxW, sy + 22, boxColor.getRGB());
            drawCustomText(context, enumSetting.getValue().toString(), boxX + 8, sy + 8, C_TEXT_TITLE);
            drawCustomText(context, isExpanded ? "▲" : "▼", boxX + boxW - 16, sy + 8, C_TEXT_MUTED);

            clickActions.add(new ClickAction(boxX, sy + 2, boxW, 20, (btn) -> {
                if(btn == 0) {
                    enumSetting.increaseEnum();
                    expandedEnum = null;
                } else if (btn == 1) {
                    if (isExpanded) expandedEnum = null;
                    else expandedEnum = enumSetting;
                }
            }));

            if (isExpanded && values != null) {
                int listY = sy + 24;
                context.fill(boxX, listY, boxX + boxW, listY + listHeight, bgTrans.getRGB());

                int itemY = listY + 4;
                for (Object val : values) {
                    boolean isSelected = val.equals(enumSetting.getValue());
                    Color textColor = isSelected ? C_ACCENT : (isHovering(mx, my, boxX, itemY, boxW, 16) ? C_TEXT_TITLE : C_TEXT_MUTED);
                    drawCustomText(context, val.toString(), boxX + 8, itemY + 2, textColor);

                    int finalItemY = itemY;
                    clickActions.add(new ClickAction(boxX, finalItemY, boxW, 16, (btn) -> {
                        if (btn == 0) {
                            int maxCycles = values.length;
                            int currentCycle = 0;
                            while (!enumSetting.getValue().equals(val) && currentCycle < maxCycles) {
                                enumSetting.increaseEnum();
                                currentCycle++;
                            }
                            expandedEnum = null;
                        }
                    }));
                    itemY += 16;
                }
            }
        }
        else if (setting instanceof BindSetting bind) {
            drawCustomText(context, bind.getName(), sx, sy + 6, C_TEXT_TITLE);

            int boxW = 105, boxX = sx + sWidth - boxW;
            context.fill(boxX, sy + 2, boxX + boxW, sy + 22, C_TOGGLE_OFF.getRGB());

            String bindName = activeBind == bind ? "..." : bind.getKeyString();
            String modeStr = bind.isHoldEnable() ? "[Hold] " : "[Toggle] ";

            String displayText = modeStr + bindName;
            if (getCustomTextWidth(displayText) > boxW - 10 && bindName.length() > 3) {
                displayText = modeStr + bindName.substring(0, 3) + "..";
            }

            drawCustomText(context, displayText, boxX + 6, sy + 8, C_TEXT_MUTED);

            clickActions.add(new ClickAction(boxX, sy + 2, boxW, 20, (btn) -> {
                if (btn == 0) {
                    activeBind = bind;
                } else if (btn == 1 || btn == 2) {
                    bind.setHoldEnable(!bind.isHoldEnable());
                }
            }));
        }
        else if (setting instanceof StringSetting strSetting) {
            drawCustomText(context, strSetting.getName(), sx, sy + 6, C_TEXT_TITLE);
            int boxW = 100, boxX = sx + sWidth - boxW;
            boolean isActive = (activeStringSetting == strSetting);
            Color boxColor = isActive ? C_HOVER : C_TOGGLE_OFF;

            drawCutRect(context, boxX, sy + 2, boxW, 20, 3, boxColor);

            String displayTxt = strSetting.getValue();
            if (isActive && (System.currentTimeMillis() % 1000 < 500)) displayTxt += "_";

            if (displayTxt.length() > 14) displayTxt = "..." + displayTxt.substring(displayTxt.length() - 13);

            drawCustomText(context, displayTxt, boxX + 6, sy + 8, isActive ? C_TEXT_TITLE : C_TEXT_MUTED);

            clickActions.add(new ClickAction(boxX, sy + 2, boxW, 20, (btn) -> { if(btn==0) activeStringSetting = strSetting; }));
        }
        else if (setting instanceof ColorSetting colorSet) {
            boolean isPickerOpen = expandedColors.contains(colorSet);
            int pickerHeight = 130;
            height = isPickerOpen ? (28 + pickerHeight) : 24;

            drawCustomText(context, colorSet.getName(), sx, sy + 6, C_TEXT_TITLE);
            int boxW = 28, boxX = sx + sWidth - boxW;
            Color currentColor = colorSet.getValue();

            drawCutRect(context, boxX, sy + 2, boxW, 16, 2, currentColor);

            clickActions.add(new ClickAction(boxX, sy + 2, boxW, 16, (btn) -> {
                if(btn==0) {
                    if (isPickerOpen) expandedColors.remove(colorSet);
                    else expandedColors.add(colorSet);
                }
            }));

            if (isPickerOpen) {
                drawColorPicker(context, colorSet, sx, sy + 26, sWidth, mx, my);
            }
        }
        return height;
    }

    private void drawColorPicker(DrawContext context, ColorSetting colorSet, int sx, int sy, int sWidth, double mx, double my) {
        int padding = 4;
        int innerW = sWidth - padding * 2;

        int svX = sx + padding;
        int svY = sy + padding;
        int svW = innerW;
        int svH = 70;

        int hueY = svY + svH + 4;
        int hueH = 7;

        int alphaY = hueY + hueH + 4;
        int alphaH = 7;

        int btnY = alphaY + alphaH + 4;

        drawCutRect(context, sx, sy, sWidth, (alphaY + alphaH + 4 + 14) - sy, 4, C_TOGGLE_OFF);

        Color cur = colorSet.getValue();
        float[] hsb = Color.RGBtoHSB(cur.getRed(), cur.getGreen(), cur.getBlue(), null);
        float alpha = cur.getAlpha() / 255f;

        if (pickingColorSV == colorSet) {
            double clampedX = Math.min(Math.max(svX, mx), svX + svW);
            double clampedY = Math.min(Math.max(svY, my), svY + svH);
            hsb[1] = (float)((clampedX - svX) / svW);
            hsb[2] = 1.0f - (float)((clampedY - svY) / svH);
            applyHSBA(colorSet, hsb, alpha);
            cur = colorSet.getValue();
            hsb = Color.RGBtoHSB(cur.getRed(), cur.getGreen(), cur.getBlue(), null);
        }
        if (pickingColorHue == colorSet) {
            double clampedX = Math.min(Math.max(svX, mx), svX + svW);
            hsb[0] = (float)((clampedX - svX) / svW);
            applyHSBA(colorSet, hsb, alpha);
            cur = colorSet.getValue();
            hsb = Color.RGBtoHSB(cur.getRed(), cur.getGreen(), cur.getBlue(), null);
        }
        if (pickingColorAlpha == colorSet) {
            double clampedX = Math.min(Math.max(svX, mx), svX + svW);
            alpha = (float)((clampedX - svX) / svW);
            applyHSBA(colorSet, hsb, alpha);
            cur = colorSet.getValue();
        }

        MatrixStack matrices = context.getMatrices();
        int pureHueColor = Color.HSBtoRGB(hsb[0], 1f, 1f);
        float pr = ((pureHueColor >> 16) & 0xFF) / 255f;
        float pg = ((pureHueColor >> 8) & 0xFF) / 255f;
        float pb = (pureHueColor & 0xFF) / 255f;
        drawSVPanel(matrices, svX, svY, svW, svH, pr, pg, pb);

        int cursorX = (int)(svX + hsb[1] * svW);
        int cursorY = (int)(svY + (1f - hsb[2]) * svH);
        Render2DUtil.drawCircle(matrices, cursorX, cursorY, 3f, Color.WHITE, 16);

        drawHueBar(matrices, svX, hueY, svW, hueH);
        int hueCursorX = (int)(svX + hsb[0] * svW);
        context.fill(hueCursorX - 1, hueY - 1, hueCursorX + 1, hueY + hueH + 1, 0xFFFFFFFF);

        drawAlphaBar(matrices, svX, alphaY, svW, alphaH, pr, pg, pb);
        int alphaCursorX = (int)(svX + alpha * svW);
        context.fill(alphaCursorX - 1, alphaY - 1, alphaCursorX + 1, alphaY + alphaH + 1, 0xFFFFFFFF);

        String copyStr = "Copy";
        String pasteStr = "Paste";
        String syncStr = "Sync";

        boolean hoverCopy = isHovering(mx, my, svX, btnY, 30, 12);
        boolean hoverPaste = isHovering(mx, my, svX + 40, btnY, 35, 12);
        boolean hoverSync = isHovering(mx, my, svX + 85, btnY, 30, 12);

        drawCustomText(context, copyStr, svX, btnY + 2, hoverCopy ? C_TEXT_TITLE : C_TEXT_MUTED);
        drawCustomText(context, pasteStr, svX + 40, btnY + 2, hoverPaste ? C_TEXT_TITLE : C_TEXT_MUTED);
        drawCustomText(context, syncStr, svX + 85, btnY + 2, colorSet.sync ? C_ACCENT : (hoverSync ? C_TEXT_TITLE : C_TEXT_MUTED));

        final int fSvX = svX, fSvY = svY, fSvW = svW, fSvH = svH;
        final int fHueY = hueY, fHueH = hueH;
        final int fAlphaY = alphaY, fAlphaH = alphaH;
        final int fBtnY = btnY;
        final ColorSetting fCS = colorSet;

        clickActions.add(new ClickAction(fSvX, fSvY, fSvW, fSvH, (btn) -> {
            if (btn == 0) pickingColorSV = fCS;
        }));
        clickActions.add(new ClickAction(fSvX, fHueY, fSvW, fHueH, (btn) -> {
            if (btn == 0) pickingColorHue = fCS;
        }));
        clickActions.add(new ClickAction(fSvX, fAlphaY, fSvW, fAlphaH, (btn) -> {
            if (btn == 0) pickingColorAlpha = fCS;
        }));

        clickActions.add(new ClickAction(fSvX, fBtnY, 30, 12, (btn) -> {
            if (btn == 0) {
                Color c = fCS.getValue();
                String hex = String.format("#%02x%02x%02x%02x", c.getAlpha(), c.getRed(), c.getGreen(), c.getBlue());
                StringSelection sel = new StringSelection(hex);
                Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
                cb.setContents(sel, sel);
                try { CommandManager.sendMessage("Copied: " + hex); } catch (Throwable ignored) {}
            }
        }));
        clickActions.add(new ClickAction(fSvX + 40, fBtnY, 35, 12, (btn) -> {
            if (btn == 0) {
                try {
                    String hex = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
                    if (hex != null && hex.startsWith("#") && hex.length() >= 9) {
                        int a = Integer.valueOf(hex.substring(1, 3), 16);
                        int r = Integer.valueOf(hex.substring(3, 5), 16);
                        int g = Integer.valueOf(hex.substring(5, 7), 16);
                        int b = Integer.valueOf(hex.substring(7, 9), 16);
                        fCS.setValue(new Color(r, g, b, a));
                    }
                } catch (Exception ignored) {}
            }
        }));
        clickActions.add(new ClickAction(fSvX + 85, fBtnY, 30, 12, (btn) -> {
            if (btn == 0) fCS.setSync(!fCS.sync);
        }));
    }

    private void applyHSBA(ColorSetting cs, float[] hsb, float alpha) {
        int rgb = Color.HSBtoRGB(hsb[0], Math.max(0f, Math.min(1f, hsb[1])), Math.max(0f, Math.min(1f, hsb[2])));
        Color base = new Color(rgb);
        int a = Math.max(0, Math.min(255, (int)(alpha * 255)));
        cs.setValue(new Color(base.getRed(), base.getGreen(), base.getBlue(), a));
    }

    private void drawSVPanel(MatrixStack matrices, int x, int y, int w, int h, float pr, float pg, float pb) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buf.vertex(matrices.peek().getPositionMatrix(), x, y, 0).color(1f, 1f, 1f, 1f);
        buf.vertex(matrices.peek().getPositionMatrix(), x, y + h, 0).color(1f, 1f, 1f, 1f);
        buf.vertex(matrices.peek().getPositionMatrix(), x + w, y + h, 0).color(pr, pg, pb, 1f);
        buf.vertex(matrices.peek().getPositionMatrix(), x + w, y, 0).color(pr, pg, pb, 1f);
        BufferRenderer.drawWithGlobalProgram(buf.end());

        buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buf.vertex(matrices.peek().getPositionMatrix(), x, y, 0).color(0f, 0f, 0f, 0f);
        buf.vertex(matrices.peek().getPositionMatrix(), x, y + h, 0).color(0f, 0f, 0f, 1f);
        buf.vertex(matrices.peek().getPositionMatrix(), x + w, y + h, 0).color(0f, 0f, 0f, 1f);
        buf.vertex(matrices.peek().getPositionMatrix(), x + w, y, 0).color(0f, 0f, 0f, 0f);
        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.disableBlend();
    }

    private void drawHueBar(MatrixStack matrices, int x, int y, int w, int h) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < 6; i++) {
            int left = x + (int)(w * (i / 6f));
            int right = x + (int)(w * ((i + 1) / 6f));
            int colL = Color.HSBtoRGB(i / 6f, 1f, 1f);
            int colR = Color.HSBtoRGB((i + 1) / 6f, 1f, 1f);
            float lr = ((colL >> 16) & 0xFF) / 255f, lg = ((colL >> 8) & 0xFF) / 255f, lb = (colL & 0xFF) / 255f;
            float rr = ((colR >> 16) & 0xFF) / 255f, rg = ((colR >> 8) & 0xFF) / 255f, rb = (colR & 0xFF) / 255f;
            buf.vertex(matrices.peek().getPositionMatrix(), right, y, 0).color(rr, rg, rb, 1f);
            buf.vertex(matrices.peek().getPositionMatrix(), left, y, 0).color(lr, lg, lb, 1f);
            buf.vertex(matrices.peek().getPositionMatrix(), left, y + h, 0).color(lr, lg, lb, 1f);
            buf.vertex(matrices.peek().getPositionMatrix(), right, y + h, 0).color(rr, rg, rb, 1f);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.disableBlend();
    }

    private void drawAlphaBar(MatrixStack matrices, int x, int y, int w, int h, float pr, float pg, float pb) {
        int sq = h / 2;
        boolean toggle = false;
        for (int i = 0; i < w; i += sq) {
            int rw = Math.min(sq, w - i);
            int col1 = toggle ? 0xFFCCCCCC : 0xFFFFFFFF;
            int col2 = toggle ? 0xFFFFFFFF : 0xFFCCCCCC;
            fillRect(matrices, x + i, y, x + i + rw, y + sq, col1);
            fillRect(matrices, x + i, y + sq, x + i + rw, y + h, col2);
            toggle = !toggle;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buf.vertex(matrices.peek().getPositionMatrix(), x + w, y, 0).color(pr, pg, pb, 0f);
        buf.vertex(matrices.peek().getPositionMatrix(), x, y, 0).color(pr, pg, pb, 1f);
        buf.vertex(matrices.peek().getPositionMatrix(), x, y + h, 0).color(pr, pg, pb, 1f);
        buf.vertex(matrices.peek().getPositionMatrix(), x + w, y + h, 0).color(pr, pg, pb, 0f);
        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.disableBlend();
    }

    private void fillRect(MatrixStack matrices, int x1, int y1, int x2, int y2, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buf.vertex(matrices.peek().getPositionMatrix(), x2, y1, 0).color(r, g, b, a);
        buf.vertex(matrices.peek().getPositionMatrix(), x1, y1, 0).color(r, g, b, a);
        buf.vertex(matrices.peek().getPositionMatrix(), x1, y2, 0).color(r, g, b, a);
        buf.vertex(matrices.peek().getPositionMatrix(), x2, y2, 0).color(r, g, b, a);
        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.disableBlend();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float scale = currentScale;
        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

        double vMouseX = (mouseX - screenW / 2.0) / scale + (V_WIDTH / 2.0);
        double vMouseY = (mouseY - screenH / 2.0) / scale + (V_HEIGHT / 2.0);

        if (activeBind != null) {
            activeBind.setValue(-(button + 1));
            activeBind = null;
            return true;
        }

        boolean clickedAny = false;
        for (ClickAction action : clickActions) {
            if (isHovering(vMouseX, vMouseY, action.x, action.y, action.w, action.h)) {
                action.onClick.accept(button);
                clickedAny = true;
                break;
            }
        }

        if (button == 0 && !clickedAny) {
            activeBind = null;
            activeStringSetting = null;
            expandedEnum = null;
            searchFocused = false;
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingSlider = null;
            pickingColorSV = null;
            pickingColorHue = null;
            pickingColorAlpha = null;
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float scale = currentScale;
        double vMouseX = (mouseX - client.getWindow().getScaledWidth() / 2.0) / scale + (V_WIDTH / 2.0);
        if (vMouseX > 180 && vMouseX < V_WIDTH) {
            scrollTarget -= verticalAmount * 45f;
            expandedEnum = null;
        }
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchFocused) {
            searchText += chr;
            return true;
        }
        if (activeStringSetting != null) {
            activeStringSetting.setValue(activeStringSetting.getValue() + chr);
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!searchText.isEmpty()) searchText = searchText.substring(0, searchText.length() - 1);
            } else if (keyCode == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
            } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                searchFocused = false;
                searchText = "";
            }
            return true;
        }
        if (activeStringSetting != null) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                String val = activeStringSetting.getValue();
                if (!val.isEmpty()) {
                    activeStringSetting.setValue(val.substring(0, val.length() - 1));
                }
            } else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) {
                activeStringSetting = null;
            }
            return true;
        }

        if (activeBind != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) {
                activeBind.setValue(-1);
            } else {
                activeBind.setValue(keyCode);
            }
            activeBind = null;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.close();
            return true;
        }
        return true;
    }

    @Override
    public boolean shouldPause() { return false; }

    private boolean isHovering(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private List<Module> getModulesByCategory(Module.Category category) {
        List<Module> list = new ArrayList<>();
        for (Module m : Forever.MODULE.getModules()) {
            if (m.isHidden()) continue;
            if (m.getCategory() == category) list.add(m);
        }
        return list;
    }

    private float getAnim(String key) { return animationMap.getOrDefault(key, 0f); }
    private void setAnim(String key, float value) { animationMap.put(key, Math.max(0f, Math.min(1f, value))); }

    private Color blendColor(Color c1, Color c2, float ratio) {
        ratio = Math.max(0f, Math.min(1f, ratio));
        return new Color(
                (int) (c1.getRed() + (c2.getRed() - c1.getRed()) * ratio),
                (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * ratio),
                (int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * ratio),
                (int) (c1.getAlpha() + (c2.getAlpha() - c1.getAlpha()) * ratio)
        );
    }

    private int getSettingHeight(Setting setting, int sWidth) {
        if (setting instanceof EnumSetting<?> enumSetting) {
            boolean isExpanded = (expandedEnum == enumSetting);
            Object[] values = enumSetting.getValue().getClass().getEnumConstants();
            int listHeight = isExpanded && values != null ? (values.length * 16 + 4) : 0;
            return 24 + listHeight;
        } else if (setting instanceof ColorSetting colorSet) {
            boolean isPickerOpen = expandedColors.contains(colorSet);
            return isPickerOpen ? (28 + 130) : 24;
        }
        return 24;
    }

    private record ClickAction(int x, int y, int w, int h, Consumer<Integer> onClick) {}
}
