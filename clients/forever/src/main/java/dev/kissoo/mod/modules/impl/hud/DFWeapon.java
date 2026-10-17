package dev.kissoo.mod.modules.impl.hud;

import dev.kissoo.api.utils.render.Render2DUtil;
import hoprc.api.core.impl.FontManager;
import hoprc.api.gui.fonts.FontRenderer;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.awt.*;

public class DFWeapon extends Module {
    public static DFWeapon INSTANCE;

    // 整体位置
    public final SliderSetting xOffset = this.add(new SliderSetting("XOffset", -16, -400, 400, 1));
    public final SliderSetting yOffset = this.add(new SliderSetting("YOffset", -52, -400, 400, 1));

    // 额外可调
    public final SliderSetting iconX = this.add(new SliderSetting("IconX", 46, 0, 120, 1));
    public final SliderSetting iconY = this.add(new SliderSetting("IconY", 17, 0, 80, 1));
    public final SliderSetting leftCountY = this.add(new SliderSetting("LeftCountY", 0, -20, 20, 1));
    public final SliderSetting rightCountY = this.add(new SliderSetting("RightCountY", 0, -20, 20, 1));

    // 固定样式
    private static final float WIDTH = 118.0f;
    private static final float HEIGHT = 34.0f;
    private static final float RADIUS = 2.5f;

    private static final Color BG_COLOR = new Color(1593835520, true);

    private static final String DISPLAY_NAME = "EndCrystal";
    private static final Color NAME_COLOR = new Color(-1426063361, true);

    private static final Color HOTBAR_COLOR = new Color(-1, true);
    private static final Color OTHER_COLOR = new Color(-1090519041, true);

    private static final boolean TEXT_SHADOW = true;

    private static final float NAME_X = 3.0f;
    private static final float NAME_BOTTOM_MARGIN = 3.0f;

    private static final float ICON_SCALE = 1.75f;

    private static final float COUNT_RIGHT_MARGIN = 6.0f;
    private static final float COUNT_BOTTOM_TO_BOX_TOP = 2.0f;

    private static final float NAME_FONT_SIZE = 8.0f;
    private static final float BIG_COUNT_FONT_SIZE = 18.0f;
    private static final float SMALL_COUNT_FONT_SIZE = 9.0f;

    private static final float COUNT_GAP = 1.0f;

    private static final ItemStack END_CRYSTAL_STACK = new ItemStack(Items.END_CRYSTAL);

    public DFWeapon() {
        super("DFWeapon", Category.HUD);
        this.setChinese("三角洲武器");
        INSTANCE = this;
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        if (mc.player == null || mc.world == null) return;

        MatrixStack matrices = drawContext.getMatrices();

        float screenWidth = mc.getWindow().getScaledWidth();
        float screenHeight = mc.getWindow().getScaledHeight();

        float x = screenWidth - WIDTH + xOffset.getValueFloat();
        float y = screenHeight - HEIGHT + yOffset.getValueFloat();

        Render2DUtil.drawRoundedRect(matrices, x, y, WIDTH, HEIGHT, RADIUS, BG_COLOR);

        int hotbarCrystalCount = countHotbarCrystals();
        int totalCrystalCount = countTotalCrystals();

        renderCrystalIcon(drawContext, x, y);
        renderName(drawContext, x, y);
        renderCount(drawContext, x, y, hotbarCrystalCount, totalCrystalCount);
    }

    private void renderCrystalIcon(DrawContext drawContext, float x, float y) {
        MatrixStack matrices = drawContext.getMatrices();

        float scaledSize = 16.0f * ICON_SCALE;
        float drawX = x + iconX.getValueFloat() - scaledSize / 2.0f;
        float drawY = y + iconY.getValueFloat() - scaledSize / 2.0f;

        matrices.push();
        matrices.translate(drawX, drawY, 0.0f);
        matrices.scale(ICON_SCALE, ICON_SCALE, 1.0f);
        drawContext.drawItem(END_CRYSTAL_STACK, 0, 0);
        matrices.pop();
    }

    private void renderName(DrawContext drawContext, float x, float y) {
        FontRenderer font = FontManager.get(NAME_FONT_SIZE);

        float textX = x + NAME_X;
        float textY = y + HEIGHT - font.getFontHeight(DISPLAY_NAME) - NAME_BOTTOM_MARGIN;

        drawFontText(drawContext, font, DISPLAY_NAME, textX, textY, NAME_COLOR, TEXT_SHADOW);
    }

    private void renderCount(DrawContext drawContext, float x, float y, int hotbarCount, int totalCount) {
        FontRenderer bigFont = FontManager.get(BIG_COUNT_FONT_SIZE);
        FontRenderer smallFont = FontManager.get(SMALL_COUNT_FONT_SIZE);

        String bigText = String.valueOf(hotbarCount);
        String smallText = "/" + totalCount;

        float bigWidth = bigFont.getWidth(bigText);
        float smallWidth = smallFont.getWidth(smallText);

        float bigHeight = bigFont.getFontHeight(bigText);
        float smallHeight = smallFont.getFontHeight(smallText);

        float totalWidth = bigWidth + COUNT_GAP + smallWidth;
        float baselineBottom = y - COUNT_BOTTOM_TO_BOX_TOP;

        float startX = x + WIDTH - COUNT_RIGHT_MARGIN - totalWidth;

        float bigY = baselineBottom - bigHeight + leftCountY.getValueFloat();
        float smallY = baselineBottom - smallHeight + rightCountY.getValueFloat();

        drawFontText(drawContext, bigFont, bigText, startX, bigY, HOTBAR_COLOR, TEXT_SHADOW);
        drawFontText(drawContext, smallFont, smallText, startX + bigWidth + COUNT_GAP, smallY, OTHER_COLOR, TEXT_SHADOW);
    }

    private void drawFontText(DrawContext drawContext, FontRenderer font, String text, float x, float y, Color color, boolean shadow) {
        MatrixStack matrices = drawContext.getMatrices();
        font.drawString(
                matrices,
                text,
                x,
                y,
                color.getRed() / 255.0f,
                color.getGreen() / 255.0f,
                color.getBlue() / 255.0f,
                color.getAlpha() / 255.0f,
                shadow
        );
    }

    private int countHotbarCrystals() {
        if (mc.player == null) return 0;

        int count = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.END_CRYSTAL) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private int countTotalCrystals() {
        if (mc.player == null) return 0;

        int count = 0;

        for (int i = 0; i < mc.player.getInventory().main.size(); i++) {
            ItemStack stack = mc.player.getInventory().main.get(i);
            if (stack.getItem() == Items.END_CRYSTAL) {
                count += stack.getCount();
            }
        }

        ItemStack offhand = mc.player.getOffHandStack();
        if (offhand.getItem() == Items.END_CRYSTAL) {
            count += offhand.getCount();
        }

        return count;
    }
}