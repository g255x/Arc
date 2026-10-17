package dev.kissoo.mod.modules.impl.hud;

import dev.kissoo.api.utils.math.AnimateUtil;
import dev.kissoo.api.utils.math.Easing;
import dev.kissoo.api.utils.render.Render2DUtil;
import dev.kissoo.core.impl.BlurManager;
import hoprc.api.core.impl.FontManager;
import hoprc.api.gui.fonts.FontRenderer;
import dev.kissoo.mod.modules.HudElement;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NotificationHud extends Module implements HudElement {
    public static NotificationHud INSTANCE;
    private static final float BASE_CARD_WIDTH       = 170.0f;
    private static final float BASE_CARD_HEIGHT      = 36.0f;
    private static final float BASE_ACCENT_WIDTH     = 2.0f;
    private static final float BASE_PADDING_LEFT     = 10.0f;
    private static final float BASE_PADDING_TOP      = 12.0f;
    private static final float BASE_LINE_GAP         = 2.0f;
    private static final float BASE_CARD_SPACING     = 4.0f;
    private static final float BASE_TITLE_FONT_SIZE  = 8.5f;
    private static final float BASE_MSG_FONT_SIZE    = 9.0f;
    private static final long  DISPLAY_TIME     = 1000L;
    private static final long  ANIM_IN_TIME     = 300L;
    private static final long  ANIM_TEXT_TIME   = 200L;
    private final SliderSetting scale = add(new SliderSetting("Scale", 1.0, 0.5, 2.0, 0.05));
    private final ColorSetting enableColor  = add(new ColorSetting("Enable Color",  new Color(48, 226, 178, 255)));
    private final ColorSetting disableColor = add(new ColorSetting("Disable Color", new Color(220, 50, 50, 255)));
    private final ColorSetting infoColor    = add(new ColorSetting("Info Color",    new Color(99, 150, 145, 255)));
    private final ColorSetting bgColor      = add(new ColorSetting("Bg Color",      new Color(20, 20, 20, 180)));
    private final ColorSetting titleColor   = add(new ColorSetting("Title Color",   new Color(255, 255, 255, 255)));
    private final ColorSetting msgColor     = add(new ColorSetting("Msg Color",     new Color(181, 181, 181, 255)));
    private final BooleanSetting blur = add(new BooleanSetting("Blur", true));
    private final SliderSetting blurStrength = add(new SliderSetting("Blur Strength", 30, 0, 100, 1));
    private final SliderSetting textOffset = add(new SliderSetting("Text Offset", 0, -10, 10, 1));
    private int hudX = 10;
    private int hudY = 10;
    private final List<Notification> notifications = new CopyOnWriteArrayList<>();

    public NotificationHud() {
        super("Notification", Category.HUD);
        setChinese("通知消息");
        INSTANCE = this;
    }

    private static float getCardWidth()      { return BASE_CARD_WIDTH * INSTANCE.scale.getValueFloat(); }
    private static float getCardHeight()     { return BASE_CARD_HEIGHT * INSTANCE.scale.getValueFloat(); }
    private static float getAccentWidth()    { return BASE_ACCENT_WIDTH * INSTANCE.scale.getValueFloat(); }
    private static float getPaddingLeft()    { return BASE_PADDING_LEFT * INSTANCE.scale.getValueFloat(); }
    private static float getLineGap()        { return BASE_LINE_GAP * INSTANCE.scale.getValueFloat(); }
    private static float getCardSpacing()    { return BASE_CARD_SPACING * INSTANCE.scale.getValueFloat(); }
    private static float getTitleFontSize()  { return BASE_TITLE_FONT_SIZE * INSTANCE.scale.getValueFloat(); }
    private static float getMsgFontSize()    { return BASE_MSG_FONT_SIZE * INSTANCE.scale.getValueFloat(); }

    public static void send(String title, String message, NotificationType type) {
        if (INSTANCE == null || !INSTANCE.isOn()) return;
        INSTANCE.notifications.add(new Notification(title, message, type));
    }

    @Override
    public void onRender2D(DrawContext context, float tickDelta) {
        renderElement(context, tickDelta);
    }

    @Override
    public void renderElement(DrawContext context, float tickDelta) {
        if (notifications.isEmpty()) return;

        float currentY = hudY;

        for (Notification notif : notifications) {
            notif.updateY(currentY);
            notif.render(context);
            if (!notif.isDead) {
                currentY += (getCardHeight() + getCardSpacing()) * notif.getHeightProgress();
            }
        }

        notifications.removeIf(n -> n.isDead);
    }

    @Override public String  getElementName()    { return getChinese() != null ? getChinese() : getName(); }
    @Override public int     getElementX()       { return hudX; }
    @Override public void    setElementX(int x)  { this.hudX = x; }
    @Override public int     getElementY()       { return hudY; }
    @Override public void    setElementY(int y)  { this.hudY = y; }
    @Override public int     getElementWidth()   { return (int) getCardWidth(); }
    @Override public int     getElementHeight()  { return (int) getCardHeight(); }
    @Override public boolean isElementVisible()  { return isEnabled(); }

    private static class Notification {

        private final String           title;
        private final String           message;
        private final NotificationType type;
        private final long             startTime;

        private float   renderY        = -1f;
        private float   heightProgress = 1.0f;
        public  boolean isDead         = false;

        public Notification(String title, String message, NotificationType type) {
            this.title     = title;
            this.message   = message;
            this.type      = type;
            this.startTime = System.currentTimeMillis();
        }

        public float getHeightProgress() { return heightProgress; }

        public void updateY(float targetY) {
            if (renderY < 0) renderY = targetY;
            renderY = AnimateUtil.fast(targetY, renderY, 16f);
        }

        public void render(DrawContext context) {
            if (NotificationHud.INSTANCE == null) return;

            long elapsed = System.currentTimeMillis() - startTime;

            long t_s1_end  = ANIM_IN_TIME;
            long t_s2_end  = ANIM_IN_TIME + ANIM_TEXT_TIME;
            long t_s3_end  = ANIM_IN_TIME + ANIM_TEXT_TIME + DISPLAY_TIME;
            long t_s2r_end = t_s3_end + ANIM_TEXT_TIME;
            long t_s1r_end = t_s3_end + ANIM_TEXT_TIME + ANIM_IN_TIME;

            if (elapsed > t_s1r_end) {
                isDead = true;
                return;
            }

            int x = NotificationHud.INSTANCE.hudX;
            int y = Math.round(renderY);

            if (elapsed <= t_s1_end) {
                float delta    = elapsed / (float) ANIM_IN_TIME;
                float progress = (float) Easing.CubicOut.ease(delta);
                heightProgress = progress;
                renderStage1(context, x, y, progress);

            } else if (elapsed <= t_s2_end) {
                float delta    = (elapsed - t_s1_end) / (float) ANIM_TEXT_TIME;
                float progress = (float) Easing.CubicOut.ease(delta);
                heightProgress = 1.0f;
                renderStage2(context, x, y, progress);

            } else if (elapsed <= t_s3_end) {
                heightProgress = 1.0f;
                renderStage3(context, x, y);

            } else if (elapsed <= t_s2r_end) {
                float delta    = (elapsed - t_s3_end) / (float) ANIM_TEXT_TIME;
                float progress = (float) Easing.CubicOut.ease(delta);
                heightProgress = 1.0f;
                renderStage2(context, x, y, 1.0f - progress);

            } else {
                float delta    = (elapsed - t_s2r_end) / (float) ANIM_IN_TIME;
                float progress = (float) Easing.CubicOut.ease(delta);
                heightProgress = 1.0f - progress;
                renderStage1(context, x, y, 1.0f - progress);
            }
        }
        private void renderStage1(DrawContext context, int x, int y, float progress) {
            Color       accent = getAccentColor();
            MatrixStack ms     = context.getMatrices();

            float cardW = getCardWidth();
            float cardH = getCardHeight();
            float drawW = cardW * progress;

            Render2DUtil.drawRect(ms, x, y, drawW, cardH, accent);
        }
        private void renderStage2(DrawContext context, int x, int y, float progress) {
            NotificationHud inst    = NotificationHud.INSTANCE;
            Color            bg      = inst.bgColor.getValue();
            Color            accent  = getAccentColor();
            MatrixStack      ms      = context.getMatrices();

            float cardW    = getCardWidth();
            float cardH    = getCardHeight();
            float accentW  = getAccentWidth();

            applyBlurIfNeeded(context, x, y, (int) cardW, (int) cardH);
            Render2DUtil.drawRect(ms, x, y, cardW, cardH, bg);

            int alpha = (int) (255 * progress);
            if (alpha > 4) {
                drawNotifText(context, x, y, alpha);
            }

            float accentStartX = x + (cardW - accentW) * progress;
            float accentDrawW  = cardW - (accentStartX - x);
            Render2DUtil.drawRect(ms, accentStartX, y, accentDrawW, cardH, accent);
        }
        private void renderStage3(DrawContext context, int x, int y) {
            NotificationHud inst    = NotificationHud.INSTANCE;
            Color            bg      = inst.bgColor.getValue();
            Color            accent  = getAccentColor();
            MatrixStack      ms      = context.getMatrices();

            float cardW   = getCardWidth();
            float cardH   = getCardHeight();
            float accentW = getAccentWidth();

            applyBlurIfNeeded(context, x, y, (int) cardW, (int) cardH);
            Render2DUtil.drawRect(ms, x, y, cardW, cardH, bg);
            Render2DUtil.drawRect(ms, x + cardW - accentW, y, accentW, cardH, accent);
            drawNotifText(context, x, y, 255);
        }
        private void drawNotifText(DrawContext context, int x, int y, int alpha) {
            NotificationHud inst       = NotificationHud.INSTANCE;
            int              textOffsetY = inst.textOffset.getValueInt();

            float titleSize = getTitleFontSize();
            float msgSize   = getMsgFontSize();

            FontRenderer titleFont = FontManager.getNotificationFont(titleSize);
            FontRenderer msgFont   = FontManager.getNotificationFont(msgSize);

            float titleH     = titleFont.getFontHeight("A");
            float msgH       = msgFont.getFontHeight("A");
            float lineGap    = getLineGap();
            float totalTextH = titleH + lineGap + msgH;

            float cardH      = getCardHeight();
            float paddingL   = getPaddingLeft();

            float textStartY = y + (cardH - totalTextH) * 0.5f + textOffsetY;
            float textX      = x + paddingL;

            Color titleC = inst.titleColor.getValue();
            Color msgC   = inst.msgColor.getValue();
            float tA     = (alpha / 255f) * (titleC.getAlpha() / 255f);
            float mA     = (alpha / 255f) * (msgC.getAlpha() / 255f);

            MatrixStack ms = context.getMatrices();

            titleFont.drawString(ms, title,
                    textX,
                    (float) Math.round(textStartY),
                    titleC.getRed() / 255f,
                    titleC.getGreen() / 255f,
                    titleC.getBlue() / 255f,
                    tA,
                    false);

            msgFont.drawString(ms, message,
                    textX,
                    (float) Math.round(textStartY + titleH + lineGap),
                    msgC.getRed() / 255f,
                    msgC.getGreen() / 255f,
                    msgC.getBlue() / 255f,
                    mA,
                    false);
        }
        private void applyBlurIfNeeded(DrawContext context, int x, int y, int w, int h) {
            NotificationHud inst = NotificationHud.INSTANCE;
            if (!inst.blur.getValue()) return;

            float percentage   = inst.blurStrength.getValueInt() / 100.0f;
            float actualRadius = percentage * 20.0f;
            if (actualRadius <= 0) return;

            context.draw();
            new BlurManager().applyBlur(actualRadius, x, y, w, h);
        }

        private Color getAccentColor() {
            if (NotificationHud.INSTANCE == null) return new Color(100, 200, 100);
            return switch (type) {
                case ENABLE  -> NotificationHud.INSTANCE.enableColor.getValue();
                case DISABLE -> NotificationHud.INSTANCE.disableColor.getValue();
                case INFO    -> NotificationHud.INSTANCE.infoColor.getValue();
            };
        }
    }

    public enum NotificationType {
        ENABLE, DISABLE, INFO
    }
}