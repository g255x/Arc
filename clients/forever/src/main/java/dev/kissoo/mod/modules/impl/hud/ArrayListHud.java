package dev.kissoo.mod.modules.impl.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.kissoo.Forever;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.ClientTickEvent;
import dev.kissoo.api.events.impl.InitEvent;
import dev.kissoo.api.utils.math.Animation;
import dev.kissoo.api.utils.math.Easing;
import dev.kissoo.api.utils.render.ColorUtil;
import dev.kissoo.api.utils.render.Render2DUtil;
import dev.kissoo.api.utils.render.TextUtil;
import dev.kissoo.api.utils.skija.SkijaGlowRenderer;
import hoprc.api.core.impl.FontManager;
import dev.kissoo.mod.modules.HudElement;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

public class ArrayListHud extends Module implements HudElement {
    public static ArrayListHud INSTANCE;

    private int hudX = 2, hudY = 1;
    private AlignMode alignMode = AlignMode.LEFT;

    private final BooleanSetting sortByLength = add(new BooleanSetting("Sort", true));
    private final BooleanSetting renderUp = add(new BooleanSetting("RenderUp", false));
    private final BooleanSetting shadow = add(new BooleanSetting("Shadow", true));
    private final BooleanSetting lowerCase = add(new BooleanSetting("LowerCase", false));
    private final BooleanSetting showSpace = add(new BooleanSetting("Space", true));
    private final BooleanSetting forgeHax = add(new BooleanSetting("ForgeHax", false));
    private final BooleanSetting listGlow = add(new BooleanSetting("Glow", false).setParent());
    private final SliderSetting listGlowRadius = add(new SliderSetting("GlowRadius", 3.0, 0.5, 8.0, 0.1, () -> listGlow.isOpen()));
    private final SliderSetting listGlowOpacity = add(new SliderSetting("GlowOpacity", 0.6, 0.1, 1.0, 0.05, () -> listGlow.isOpen()));

    private final SliderSetting interval = add(new SliderSetting("Interval", 0.0, 0.0, 15.0, 0.1));
    private final SliderSetting enableLength = add(new SliderSetting("EnableLength", 200, 0, 1000));
    private final SliderSetting disableLength = add(new SliderSetting("DisableLength", 200, 0, 1000));
    private final SliderSetting fadeLength = add(new SliderSetting("FadeLength", 200, 0, 1000));
    private final EnumSetting<Easing> easing = add(new EnumSetting<>("Easing", Easing.CircInOut));

    private final ColorSetting color = add(new ColorSetting("Color", new Color(208, 0, 0)));
    private final EnumSetting<ColorMode> colorMode = add(new EnumSetting<>("ColorMode", ColorMode.Pulse));
    private final SliderSetting pulseSpeed = add(new SliderSetting("PulseSpeed", 1.0, 0.0, 5.0, 0.1));
    private final SliderSetting pulseCounter = add(new SliderSetting("Counter", 10, 1, 50));
    private final ColorSetting endColor = add(new ColorSetting("SecondColor", new Color(255, 0, 0)).injectBoolean(true));
    private final SliderSetting rainbowSpeed = add(new SliderSetting("RainbowSpeed", 4.0, 1.0, 10.0, 0.1));
    private final SliderSetting saturation = add(new SliderSetting("Saturation", 130, 1, 255));
    private final SliderSetting rainbowDelay = add(new SliderSetting("Delay", 350, 0, 1000));

    private final BooleanSetting backGround = add(new BooleanSetting("BackGround", false).setParent());
    private final SliderSetting bgWidth = add(new SliderSetting("BGWidth", 0.0, 0.0, 15.0, () -> backGround.isOpen()));
    private final ColorSetting bgColor = add(new ColorSetting("BGColor", new Color(0, 0, 0, 100), () -> backGround.isOpen()));
    private final ColorSetting rect = add(new ColorSetting("Rect", new Color(208, 0, 0)).injectBoolean(false));

    private final ArrayList<ModuleInfo> moduleList = new ArrayList<>();
    private boolean initialized = false;

    private final ConcurrentHashMap<String, Identifier> listGlowCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> listGlowUpdateTime = new ConcurrentHashMap<>();

    public ArrayListHud() {
        super("ArrayList", Category.HUD);
        setChinese("模块列表");
        INSTANCE = this;
        Forever.EVENT_BUS.subscribe(new InitHandler());
    }

    public class InitHandler {
        @EventListener
        public void onInit(InitEvent event) {
            moduleList.clear();
            for (Module m : Forever.MODULE.getModules()) {
                moduleList.add(new ModuleInfo(m));
            }
            initialized = true;
            Forever.EVENT_BUS.unsubscribe(this);
        }
    }

    private boolean isRightSide() {
        return alignMode == AlignMode.RIGHT;
    }

    private int getColor(double counter) {
        if (colorMode.getValue() == ColorMode.Pulse) {
            return endColor.booleanValue
                    ? ColorUtil.pulseColor(color.getValue(), endColor.getValue(), counter, pulseCounter.getValueInt(), pulseSpeed.getValue()).getRGB()
                    : ColorUtil.pulseColor(color.getValue(), counter, pulseCounter.getValueInt(), pulseSpeed.getValue()).getRGB();
        } else if (colorMode.getValue() == ColorMode.Rainbow) {
            double state = Math.ceil((System.currentTimeMillis() * rainbowSpeed.getValue() + counter * rainbowDelay.getValue()) / 20.0);
            return Color.getHSBColor((float) (state % 360.0 / 360.0), saturation.getValueFloat() / 255f, 1f).getRGB();
        }
        return color.getValue().getRGB();
    }

    @EventListener(priority = -999)
    public void onTick(ClientTickEvent event) {
        if (nullCheck() || !initialized) return;
        if (event.isPost()) {
            for (ModuleInfo info : moduleList) {
                info.update();
            }

            if (sortByLength.getValue()) {
                moduleList.sort(Comparator.comparingInt(i ->
                        i.displayText == null ? 0 : -(int) FontManager.getWidth(i.displayText)
                ));
            }

            if (this.listGlow.getValue()) {
                cleanupStaleListGlowEntries();
            }
        }
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
        int maxW = 60;
        for (ModuleInfo info : moduleList) {
            if (info.displayText != null && (info.isOn || info.animX > 0.0)) {
                int w = (int) FontManager.getWidth(info.displayText);
                if (w > maxW) maxW = w;
            }
        }
        return maxW + 4;
    }

    @Override
    public int getElementHeight() {
        int count = 0;
        for (ModuleInfo info : moduleList) {
            if (info.isOn || info.animX > 0) count++;
        }
        return Math.max(20, (int) ((FontManager.getFontHeight() + interval.getValue()) * Math.max(count, 1)));
    }

    @Override
    public boolean isElementVisible() {
        return isEnabled();
    }

    @Override
    public AlignMode getAlignMode() {
        return alignMode;
    }

    @Override
    public void setAlignMode(AlignMode mode) {
        if (mode == alignMode) return;
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
        if (!initialized) return;

        float fontHeight = FontManager.getFontHeight();
        double currentY;
        boolean fromUp = renderUp.getValue();

        float anchorX = getElementX();
        int boxWidth = getElementWidth();
        boolean rightSide = isRightSide();

        if (fromUp) {
            currentY = hudY + 1.0;
        } else {
            int visCount = 0;
            for (ModuleInfo info : moduleList) {
                if (info.isOn || info.animX > 0) visCount++;
            }
            currentY = hudY + (fontHeight + interval.getValue()) * visCount - fontHeight - 1;
        }

        double counter = 20.0;

        for (ModuleInfo info : moduleList) {
            if (info.animX <= 0 && !info.isOn) continue;

            double textWidth = FontManager.getWidth(info.displayText);
            info.animX = info.animation.get(
                    info.isOn ? textWidth + 1 : 0.0,
                    info.isOn ? enableLength.getValueInt() : disableLength.getValueInt(),
                    easing.getValue()
            );

            double fade = info.fadeAnimation.get(
                    info.isOn ? 1.0 : 0.0,
                    fadeLength.getValueInt(),
                    easing.getValue()
            );

            if (fade > 0.04) {
                counter += fromUp ? fade : -fade;

                int c = ColorUtil.injectAlpha(
                        getColor(counter),
                        (int) (color.getValue().getAlpha() * fade)
                );

                double displayWidth = info.animX;
                float textX = rightSide
                        ? anchorX + boxWidth - (float) displayWidth
                        : anchorX;

                if (backGround.getValue()) {
                    float bgX = rightSide
                            ? anchorX + boxWidth - (float) displayWidth - bgWidth.getValueFloat() / 2f
                            : anchorX - bgWidth.getValueFloat() / 2f;

                    Render2DUtil.drawRect(
                            context.getMatrices(),
                            bgX,
                            (float) currentY - 1f - interval.getValueFloat() / 2f,
                            (float) displayWidth + bgWidth.getValueFloat(),
                            fontHeight + interval.getValueFloat(),
                            ColorUtil.injectAlpha(
                                    bgColor.sync ? c : bgColor.getValue().getRGB(),
                                    (int) (bgColor.getValue().getAlpha() * fade)
                            )
                    );
                }

                boolean textDrawnByGlow = false;
                if (this.listGlow.getValue() && info.displayText != null && !info.displayText.isEmpty()) {
                    Color itemColor = new Color(c, true);
                    textDrawnByGlow = this.renderListItemGlow(
                            context,
                            context.getMatrices(),
                            info.displayText,
                            textX,
                            (float) currentY,
                            itemColor,
                            (float) fade
                    );
                }

                if (!textDrawnByGlow) {
                    TextUtil.drawString(context, info.displayText, textX, (float) currentY, c, shadow.getValue());
                }

                if (rect.booleanValue) {
                    float rectX = rightSide
                            ? anchorX + boxWidth + bgWidth.getValueFloat() / 2f
                            : anchorX - bgWidth.getValueFloat() / 2f - 1f;

                    Render2DUtil.drawRect(
                            context.getMatrices(),
                            rectX,
                            (float) currentY - 1f - interval.getValueFloat() / 2f,
                            1f,
                            fontHeight + interval.getValueFloat(),
                            rect.sync
                                    ? c
                                    : ColorUtil.injectAlpha(rect.getValue(), (int) (rect.getValue().getAlpha() * fade)).getRGB()
                    );
                }

                currentY += fromUp
                        ? (fontHeight + interval.getValue()) * fade
                        : -(fontHeight + interval.getValue()) * fade;
            }
        }
    }

    private void drawGlowTexture(DrawContext drawContext, MatrixStack matrices, Identifier texture,
                                 float screenX, float screenY, float screenW, float screenH, float alpha) {
        if (!SkijaGlowRenderer.isTextureReady(texture)) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);

        matrices.push();
        matrices.translate(screenX, screenY, 0);

        int w = (int) screenW;
        int h = (int) screenH;

        drawContext.drawTexture(texture, 0, 0, 0, 0, w, h, w, h);

        matrices.pop();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private boolean renderListItemGlow(DrawContext drawContext, MatrixStack matrices, String text,
                                       float x, float y, Color itemColor, float fade) {
        if (text == null || text.isEmpty() || fade < 0.05f) return false;

        String strippedText = hoprc.api.gui.fonts.FontRenderer.stripControlCodes(text);
        if (strippedText.isEmpty()) return false;

        float glowRadius = (float) this.listGlowRadius.getValue();
        float glowOpacity = (float) this.listGlowOpacity.getValue();
        Color textColor = itemColor;
        Color glowColor = new Color(
                textColor.getRed(),
                textColor.getGreen(),
                textColor.getBlue(),
                Math.min(255, (int) (textColor.getAlpha() * glowOpacity))
        );

        return renderListItemSkijaGlow(drawContext, matrices, strippedText, x, y, textColor, glowColor, glowRadius, fade);
    }

    private boolean renderListItemSkijaGlow(DrawContext drawContext, MatrixStack matrices,
                                            String text, float x, float y,
                                            Color textColor, Color glowColor,
                                            float glowRadius, float fade) {
        String cacheKey = String.format(
                "list_%s_%.1f_%08x_%08x_%.2f_%s_%.1f",
                text,
                glowRadius,
                textColor.getRGB(),
                glowColor.getRGB(),
                this.listGlowOpacity.getValue(),
                FontManager.getMode().name(),
                FontManager.getSize()
        );

        Identifier glowTexture = listGlowCache.get(cacheKey);
        Long lastUpdate = listGlowUpdateTime.get(cacheKey);

        boolean needsUpdate = glowTexture == null || lastUpdate == null || System.currentTimeMillis() - lastUpdate > 5000;

        if (needsUpdate) {
            if (glowTexture != null) {
                SkijaGlowRenderer.cleanupTexture(glowTexture);
            }

            float fontSize = FontManager.isCustom() ? FontManager.getSize() : 9.0f;
            Identifier newTexture = SkijaGlowRenderer.createGlowTexture(
                    text,
                    fontSize,
                    textColor,
                    glowColor,
                    glowRadius,
                    false,
                    null,
                    null
            );

            if (newTexture != null) {
                listGlowCache.put(cacheKey, newTexture);
                listGlowUpdateTime.put(cacheKey, System.currentTimeMillis());
                glowTexture = newTexture;
            } else {
                return false;
            }
        }

        if (glowTexture != null && SkijaGlowRenderer.isTextureReady(glowTexture)) {
            float textWidth = FontManager.getWidth(text);
            float textHeight = FontManager.getFontHeight();
            float glowPadding = glowRadius * 3.0f;

            drawGlowTexture(
                    drawContext,
                    matrices,
                    glowTexture,
                    x - glowPadding,
                    y - glowPadding,
                    textWidth + glowPadding * 2,
                    textHeight + glowPadding * 2,
                    fade
            );
            return true;
        }

        return false;
    }

    private void cleanupStaleListGlowEntries() {
        long now = System.currentTimeMillis();
        listGlowUpdateTime.entrySet().removeIf(entry -> {
            if (now - entry.getValue() > 30000) {
                Identifier id = listGlowCache.remove(entry.getKey());
                if (id != null) SkijaGlowRenderer.cleanupTexture(id);
                return true;
            }
            return false;
        });
    }

    private class ModuleInfo {
        final Module module;
        String displayText;
        boolean isOn;
        double animX = 0.0;
        final Animation animation = new Animation();
        final Animation fadeAnimation = new Animation();

        ModuleInfo(Module module) {
            this.module = module;
            this.displayText = module.getArrayName();
        }

        void update() {
            isOn = module.isOn() && module.drawn.getValue();
            if (isOn) {
                String text = module.getArrayName();
                if (showSpace.getValue()) {
                    text = text.replaceAll("([a-z])([A-Z])", "$1 $2");
                    if (text.startsWith(" ")) text = text.replaceFirst(" ", "");
                }
                if (lowerCase.getValue()) {
                    text = text.toLowerCase();
                }
                if (forgeHax.getValue()) {
                    text = text + "§r<";
                }
                displayText = text;
            }
        }
    }

    private enum ColorMode {
        Custom, Pulse, Rainbow
    }
}