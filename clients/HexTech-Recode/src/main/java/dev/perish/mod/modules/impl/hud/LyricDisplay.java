package dev.perish.mod.modules.impl.hud;

import dev.perish.api.utils.math.Animation;
import dev.perish.api.utils.math.Easing;
import dev.perish.api.utils.render.Render2DUtil;
import dev.perish.core.impl.FontManager;
import dev.perish.mod.gui.fonts.FontRenderer;
import dev.perish.mod.gui.hud.IHudPosition;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.impl.client.HUD;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.ColorSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;
import dev.perish.mod.utils.music.LyricLine;
import dev.perish.mod.utils.music.LyricLineProcessor;
import dev.perish.mod.utils.music.MediaPlayer;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public class LyricDisplay extends Module implements IHudPosition {
    public static LyricDisplay INSTANCE;
    public final BooleanSetting colorSync = this.add(new BooleanSetting("ColorSync", true));
    public final BooleanSetting showBackground = this.add(new BooleanSetting("Background", true));
    public final BooleanSetting showBlur = this.add(new BooleanSetting("BlurEffect", true));
    public final SliderSetting maxLines = this.add(new SliderSetting("MaxLines", 9, 3, 15, 1));
    public final SliderSetting boxWidth = this.add(new SliderSetting("BoxWidth", 200, 100, 400, 10));
    public final SliderSetting lineSpacing = this.add(new SliderSetting("LineSpacing", 3.0, 0.0, 8.0, 0.5));
    public final SliderSetting bgRadius = this.add(new SliderSetting("BgRadius", 8.0, 0.0, 20.0, 1.0));
    public final SliderSetting fontSize = this.add(new SliderSetting("FontSize", 14, 10, 24, 1));
    public final ColorSetting currentLyricColor = this.add(new ColorSetting("CurrentLyricColor", new Color(0xFFFFFF)));
    public final ColorSetting otherLyricColor = this.add(new ColorSetting("OtherLyricColor", new Color(0x808080)));

    private float hudX, hudY, hudW, hudH;
    private int lastCurrentIdx = -1;
    private final Animation scrollAnim = new Animation();
    private final Animation fadeAnim = new Animation();
    private float animatedScrollIdx = 0;
    private final Map<Integer, FontRenderer> fontCache = new HashMap<>();

    public LyricDisplay() {
        super("Lyric", Module.Category.Hud);
        this.setChinese("歌词");
        INSTANCE = this;
    }

    private FontRenderer getFontRenderer(int size) {
        return fontCache.computeIfAbsent(size, s -> {
            try {
                float actualSize = s * 8.0f / 14.0f;
                return FontManager.assets(actualSize, "font", 0);
            } catch (Exception e) {
                return FontManager.ui;
            }
        });
    }

    @Override
    public void onRender2D(DrawContext context, float tickDelta) {
        if (nullCheck()) return;

        MediaPlayer mp = MediaPlayer.INSTANCE;
        if (!mp.isPlaying && mp.currentSong == null) return;

        List<LyricLine> lyrics;
        synchronized (mp.lyric) { lyrics = new ArrayList<>(mp.lyric); }
        if (lyrics.isEmpty()) return;

        int currentIdx = LyricLineProcessor.findCurrentIndex(lyrics, mp.tick);
        if (currentIdx < 0) return;

        float fade = (float) fadeAnim.get(1.0, 400L, Easing.CubicOut);

        int accentColor;
        if (this.colorSync.getValue()) {
            accentColor = HUD.INSTANCE.getColor(20.0);
        } else {
            accentColor = HUD.INSTANCE.color.getValue().getRGB();
        }
        Color accent = new Color(accentColor, true);

        if (currentIdx != lastCurrentIdx) {
            lastCurrentIdx = currentIdx;
        }

        animatedScrollIdx = (float) scrollAnim.get((double) currentIdx, 300L, Easing.CubicOut);

        int fontSizeVal = fontSize.getValueInt();
        FontRenderer fontRenderer = getFontRenderer(fontSizeVal);
        float lineHeight = fontRenderer.getFontHeight();
        float spacing = lineSpacing.getValueFloat();
        float lineStep = lineHeight + spacing;

        int maxLinesVal = maxLines.getValueInt();
        int halfLines = maxLinesVal / 2;

        hudW = boxWidth.getValueFloat();
        hudH = maxLinesVal * lineStep + 16;

        if (hudX == 0) hudX = mc.getWindow().getScaledWidth() / 2f - hudW / 2f;
        if (hudY == 0) hudY = mc.getWindow().getScaledHeight() - hudH - 60;

        float radius = bgRadius.getValueFloat();

        if (showBackground.getValue()) {
            Render2DUtil.drawRoundedRect(context.getMatrices(), hudX, hudY, hudW, hudH, radius, new Color(8, 8, 14, (int) (180 * fade)));
            Render2DUtil.drawRoundedStroke(context.getMatrices(), hudX, hudY, hudW, hudH, radius, new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (30 * fade)), 16);
        }

        if (showBlur.getValue() && showBackground.getValue()) {
            Render2DUtil.drawDropShadow(context.getMatrices(), hudX, hudY, hudW, hudH, radius);
        }

        MatrixStack matrices = context.getMatrices();
        float contentTop = hudY + 8;
        float contentBottom = hudY + hudH - 8;

        context.enableScissor((int) hudX, (int) contentTop, (int) (hudX + hudW), (int) contentBottom);

        int baseIdx = (int) animatedScrollIdx;
        float fracOffset = animatedScrollIdx - baseIdx;

        int renderStart = Math.max(0, baseIdx - halfLines - 1);
        int renderEnd = Math.min(lyrics.size(), baseIdx + halfLines + 2);

        float drawY;
        if (baseIdx <= halfLines) {
            drawY = contentTop + (halfLines - baseIdx) * lineStep;
        } else {
            drawY = contentTop - fracOffset * lineStep;
        }

        float centerX = hudX + hudW / 2f;

        for (int i = renderStart; i < renderEnd; i++) {
            LyricLine line = lyrics.get(i);
            String text = line.text;
            if (text.isEmpty()) text = "···";

            if (drawY + lineHeight < contentTop || drawY > contentBottom) {
                drawY += lineStep;
                continue;
            }

            boolean isCurrent = i == currentIdx;
            boolean isNext = i == currentIdx + 1;
            float tw = fontRenderer.getWidth(text);
            float textX = centerX - tw / 2f;

            if (isCurrent) {
                if (showBackground.getValue()) {
                    Render2DUtil.drawRoundedRect(matrices, hudX + 3, drawY - 2, hudW - 6, lineHeight + 4, 4, new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (20 * fade)));
                }
                Color curColor = currentLyricColor.getValue();
                int lineColor = new Color(curColor.getRed(), curColor.getGreen(), curColor.getBlue(), (int) (255 * fade)).getRGB();
                fontRenderer.drawString(matrices, text, textX, drawY, lineColor, true);
            } else if (isNext) {
                Color othColor = otherLyricColor.getValue();
                int lineAlpha = (int) (0.55f * 255 * fade);
                int lineColor = new Color(othColor.getRed(), othColor.getGreen(), othColor.getBlue(), lineAlpha).getRGB();
                fontRenderer.drawString(matrices, text, textX, drawY, lineColor, false);
            } else {
                int distance = Math.abs(i - currentIdx);
                float alpha;
                if (distance == 1) alpha = 0.55f;
                else if (distance == 2) alpha = 0.3f;
                else if (distance == 3) alpha = 0.15f;
                else alpha = 0.08f;
                int lineAlpha = (int) (alpha * 255 * fade);
                Color othColor = otherLyricColor.getValue();
                int lineColor = new Color(othColor.getRed(), othColor.getGreen(), othColor.getBlue(), lineAlpha).getRGB();
                fontRenderer.drawString(matrices, text, textX, drawY, lineColor, false);
            }
            drawY += lineStep;
        }

        context.disableScissor();
    }

    public float getHudX() { return hudX; }
    public float getHudY() { return hudY; }
    public float getHudW() { return hudW; }
    public float getHudH() { return hudH; }
    public void setHudPos(float x, float y) { this.hudX = x; this.hudY = y; }
}
