package dev.G255.Lumi.mod.modules.impl.client;

import dev.G255.Lumi.api.utils.render.Render2DUtil;
import dev.G255.Lumi.core.impl.FontManager;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.ColorSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.Iterator;
import net.minecraft.client.gui.DrawContext;

public class NotificationsHud extends Module {
    public static NotificationsHud INSTANCE;
    private final SliderSetting scale = this.add(new SliderSetting("Scale", 1.0, 0.5, 2.0, 0.1));
    private final SliderSetting backgroundAlpha = this.add(new SliderSetting("BackgroundAlpha", 200.0, 0.0, 255.0, 1.0));
    private final SliderSetting displayTime = this.add(new SliderSetting("DisplayTime", 2000.0, 500.0, 5000.0, 100.0));
    private final SliderSetting notifyX = this.add(new SliderSetting("X", 0.0, 0.0, 1000.0, 1.0));
    private final SliderSetting notifyY = this.add(new SliderSetting("Y", 330.0, 0.0, 1000.0, 1.0));
    private final BooleanSetting customColors = this.add(new BooleanSetting("CustomColors", false).setParent());
    public final BooleanSetting moduleSound = this.add(new BooleanSetting("ModuleSound", true));
    private final ColorSetting enableColor = this.add(new ColorSetting("EnableColor", new Color(0, 255, 0), this.customColors::isOpen));
    private final ColorSetting disableColor = this.add(new ColorSetting("DisableColor", new Color(255, 0, 0), this.customColors::isOpen));

    public NotificationsHud() {
        super("NotificationsHud", "", Category.Client);
        this.setChinese("");
        INSTANCE = this;
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        if (mc.getWindow() == null) {
            return;
        }
        NotificationManager.INSTANCE.update();
        if (NotificationManager.INSTANCE.isEmpty()) {
            return;
        }

        float s = this.scale.getValueFloat();
        float boxHeight = FontManager.ui.getFontHeight() * s + 8.0F * s;
        float spacing = boxHeight + 4.0F * s;
        int bgAlpha = this.backgroundAlpha.getValueInt();
        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();
        float anchorX = (float)(screenW - this.notifyX.getValueInt());
        float anchorY = (float)(screenH - this.notifyY.getValueInt());

        Iterator<NotificationInfo> iterator = NotificationManager.INSTANCE.getNotifications().iterator();
        int index = 0;

        while (iterator.hasNext()) {
            NotificationInfo n = iterator.next();
            n.update();
            if (n.isExpired()) {
                continue;
            }

            float boxWidth = Math.max(100.0F * s, 4.0F * s + FontManager.ui.getWidth(n.getTitle() + " " + n.getSubTitle()) * s);
            float renderX = anchorX - boxWidth;
            float targetY = anchorY - (float)(index + 1) * spacing;
            if (Math.abs(targetY - n.getCurrentY()) > 0.5F) {
                n.setTargetY(targetY);
            }

            float y = n.getCurrentY();
            long time = System.currentTimeMillis() - n.getCreateTime();
            long exitTime = time - (long)n.getDisplayDuration();
            boolean skipIntro = n.shouldSkipIntroAnimation();
            Color modeColor = getModeColor(n.getMode(), 255);

            if (!skipIntro && time <= 300L) {
                float p = easeOutCubic((float)time / 300.0F);
                float w = boxWidth * p;
                Render2DUtil.drawRect(drawContext.getMatrices(), renderX + boxWidth - w, y, w, boxHeight, modeColor);
            } else if (!skipIntro && time <= 500L) {
                float p = easeOutCubic((float)(time - 300L) / 200.0F);
                int a = (int)((float)bgAlpha * p);
                Render2DUtil.drawRect(drawContext.getMatrices(), renderX, y, boxWidth, boxHeight, new Color(0, 0, 0, a));
                float sliderWidth = 3.0F * s + (boxWidth - 3.0F * s) * (1.0F - p);
                Render2DUtil.drawRect(drawContext.getMatrices(), renderX, y, sliderWidth, boxHeight, modeColor);
                renderText(drawContext, n, renderX, y, boxHeight, s, (int)(255.0F * p));
            } else if (exitTime < 0L) {
                Render2DUtil.drawRect(drawContext.getMatrices(), renderX, y, boxWidth, boxHeight, new Color(0, 0, 0, bgAlpha));
                Render2DUtil.drawRect(drawContext.getMatrices(), renderX, y, 3.0F * s, boxHeight, modeColor);
                renderText(drawContext, n, renderX, y, boxHeight, s, 255);
            } else if (exitTime <= 200L) {
                float p = easeOutCubicDec((float)exitTime / 200.0F);
                int a = (int)((float)bgAlpha * p);
                Render2DUtil.drawRect(drawContext.getMatrices(), renderX, y, boxWidth, boxHeight, new Color(0, 0, 0, a));
                float sliderWidth = 3.0F * s + (boxWidth - 3.0F * s) * (1.0F - p);
                Render2DUtil.drawRect(drawContext.getMatrices(), renderX, y, sliderWidth, boxHeight, modeColor);
                renderText(drawContext, n, renderX, y, boxHeight, s, (int)(255.0F * p));
            } else if (exitTime <= 500L) {
                float p = easeOutCubicDec((float)(exitTime - 200L) / 300.0F);
                float w = boxWidth * p;
                Render2DUtil.drawRect(drawContext.getMatrices(), renderX + boxWidth - w, y, w, boxHeight, modeColor);
            }

            ++index;
        }
    }

    private void renderText(DrawContext drawContext, NotificationInfo n, float x, float y, float boxHeight, float s, int alpha) {
        float textY = y + boxHeight * 0.5F - FontManager.ui.getFontHeight() * s * 0.5F;
        Color titleColor = getModeColor(n.getMode(), alpha);
        FontManager.ui.drawString(drawContext.getMatrices(), n.getTitle(), (double)(x + 4.0F * s), (double)textY, titleColor.getRGB());
        FontManager.ui.drawString(drawContext.getMatrices(), " " + n.getSubTitle(), (double)(x + 4.0F * s + FontManager.ui.getWidth(n.getTitle()) * s), (double)textY, new Color(255, 255, 255, alpha).getRGB());
    }

    private Color getModeColor(NotificationMode mode, int alpha) {
        if (this.customColors.getValue()) {
            return switch (mode) {
                case Success -> new Color(this.enableColor.getValue().getRed(), this.enableColor.getValue().getGreen(), this.enableColor.getValue().getBlue(), alpha);
                case Error -> new Color(this.disableColor.getValue().getRed(), this.disableColor.getValue().getGreen(), this.disableColor.getValue().getBlue(), alpha);
                case Info -> new Color(140, 145, 215, alpha);
            };
        }
        return switch (mode) {
            case Success -> new Color(140, 145, 215, alpha);
            case Error -> new Color(215, 100, 100, alpha);
            case Info -> new Color(140, 145, 215, alpha);
        };
    }

    private static float easeOutCubic(float t) {
        return 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
    }

    private static float easeOutCubicDec(float t) {
        return 1.0F - easeOutCubic(t);
    }

    public static void addModuleNotification(String moduleName, boolean enabled) {
        if (INSTANCE != null) {
            NotificationManager.INSTANCE.postModuleNotification(moduleName, enabled, INSTANCE.displayTime.getValueInt());
        }
    }
}