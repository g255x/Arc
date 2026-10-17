package dev.kissoo.mod.modules.impl.hud;

import dev.kissoo.Forever;
import dev.kissoo.api.utils.render.Render2DUtil;
import hoprc.api.core.impl.FontManager;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DFHealth extends Module {
    public static DFHealth INSTANCE;

    public final SliderSetting yOffset = this.add(new SliderSetting("YOffset", -67, -300, 300, 1));
    public final SliderSetting maxDisplay = this.add(new SliderSetting("MaxDisplay", 3, 1, 10, 1));

    public final BooleanSetting hideVanilla = this.add(new BooleanSetting("HideVanilla", true));

    public final BooleanSetting showArmor = this.add(new BooleanSetting("Armor", true).setParent());
    public final BooleanSetting showDurability = this.add(new BooleanSetting("Durability", false, () -> showArmor.isOpen()));
    public final SliderSetting armorScale = this.add(new SliderSetting("ArmorScale", 0.6, 0.1, 1.0, 0.1, () -> showArmor.isOpen()));
    public final SliderSetting armorXOffset = this.add(new SliderSetting("ArmorX", 68, 0, 110, 1, () -> showArmor.isOpen()));
    public final SliderSetting armorYOffset = this.add(new SliderSetting("ArmorY", -12, -50, 50, 1, () -> showArmor.isOpen()));

    public final BooleanSetting showGapple = this.add(new BooleanSetting("Gapple", true));

    private static final int BASE_X = 6;
    private static final int BASE_BOTTOM_OFFSET = 42;
    private static final float AVATAR_SIZE = 25.0f;
    private static final float AVATAR_OFFSET_X = 1.0f;
    private static final float SPACING = 8.0f;

    private static final float INDEX_OFFSET_X = 1.0f;
    private static final float INDEX_OFFSET_Y = 2.0f;

    private static final float BAR_OFFSET_Y = 10.0f;
    private static final float BAR_WIDTH = 110.0f;
    private static final float BAR_HEIGHT = 3.0f;
    private static final int BAR_OUTLINE = 1;

    private static final float HP_TEXT_OFFSET_Y = -9.0f;
    private static final float NAME_TEXT_OFFSET_Y = 4.0f;

    private static final int ENTRY_HEIGHT = 31;
    private static final int ENTRY_GAP = 3;

    private static final Color HP_TEXT_COLOR = new Color(255, 255, 255, 245);
    private static final Color NAME_TEXT_COLOR = new Color(255, 255, 255, 190);
    private static final Color BAR_COLOR = new Color(255, 255, 255, 255);
    private static final Color BAR_BACKGROUND_COLOR = new Color(40, 40, 40, 136);
    private static final Color BAR_OUTLINE_COLOR = new Color(0, 0, 0, 140);

    private static final int GAPPLE_X_OFFSET = 126;
    private static final int GAPPLE_Y_OFFSET = -13;
    private static final int GAPPLE_BOX_SIZE = 30;
    private static final float GAPPLE_RADIUS = 4.0f;
    private static final float GAPPLE_ICON_SCALE = 1.7f;
    private static final Color GAPPLE_BG_COLOR = new Color(0, 0, 0, 100);
    private static final Color GAPPLE_TEXT_COLOR = new Color(255, 255, 255, 255);

    private static final ItemStack ENCHANTED_GOLDEN_APPLE = new ItemStack(Items.ENCHANTED_GOLDEN_APPLE);

    public DFHealth() {
        super("DFHealth", Category.HUD);
        this.setChinese("三角洲血量");
        INSTANCE = this;
    }

    public static boolean shouldHideVanilla() {
        return INSTANCE != null && INSTANCE.isOn() && INSTANCE.hideVanilla.getValue();
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        if (mc.player == null || mc.world == null) return;

        int selfBaseY = mc.getWindow().getScaledHeight() - BASE_BOTTOM_OFFSET + yOffset.getValueInt();

        List<AbstractClientPlayerEntity> friends = new ArrayList<>();
        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            if (player == null || player == mc.player) continue;
            if (!player.isAlive()) continue;
            if (Forever.FRIEND != null && Forever.FRIEND.isFriend(player)) {
                friends.add(player);
            }
        }

        friends.sort(Comparator.comparingDouble(p -> mc.player.distanceTo(p)));

        int displayCount = Math.min(maxDisplay.getValueInt(), friends.size());

        for (int i = 0; i < displayCount; i++) {
            AbstractClientPlayerEntity friend = friends.get(i);
            int entryY = selfBaseY - ((displayCount - i) * (ENTRY_HEIGHT + ENTRY_GAP));
            renderEntry(drawContext, friend, entryY, i + 2, false);
        }

        renderEntry(drawContext, mc.player, selfBaseY, 1, true);
    }

    private void renderEntry(DrawContext drawContext, AbstractClientPlayerEntity player, int baseY, int indexNumber, boolean isSelf) {
        MatrixStack matrices = drawContext.getMatrices();

        int avatarX = (int) (BASE_X + AVATAR_OFFSET_X);
        int avatarY = baseY;

        int rectX = (int) (avatarX + AVATAR_SIZE + SPACING);
        int rectY = (int) (avatarY + BAR_OFFSET_Y);
        drawPlayerHead(drawContext, player, avatarX, avatarY, AVATAR_SIZE);
        drawScaledText(drawContext, String.valueOf(indexNumber), avatarX + INDEX_OFFSET_X, avatarY + INDEX_OFFSET_Y, Color.WHITE, 1.0f, true);
        drawOutline(matrices, rectX - BAR_OUTLINE, rectY - BAR_OUTLINE, BAR_WIDTH + BAR_OUTLINE * 2, BAR_HEIGHT + BAR_OUTLINE * 2, BAR_OUTLINE, BAR_OUTLINE_COLOR);
        Render2DUtil.drawRect(matrices, rectX, rectY, BAR_WIDTH, BAR_HEIGHT, BAR_BACKGROUND_COLOR);

        float realHealth = player.getHealth() + player.getAbsorptionAmount();
        float ratio = Math.max(0.0f, Math.min(realHealth / 20.0f, 1.0f));
        if (ratio > 0) {
            Render2DUtil.drawRect(matrices, rectX, rectY, (int) (BAR_WIDTH * ratio), BAR_HEIGHT, BAR_COLOR);
        }
        drawScaledText(drawContext, format(realHealth) + "/20", rectX + 1.0f, rectY + HP_TEXT_OFFSET_Y, HP_TEXT_COLOR, 1.0f, true);
        drawScaledText(drawContext, player.getName().getString(), rectX, rectY + BAR_HEIGHT + NAME_TEXT_OFFSET_Y, NAME_TEXT_COLOR, 1.0f, true);

        if (showArmor.getValue()) {
            renderArmorAndDurability(drawContext, player, rectX + armorXOffset.getValueInt(), rectY + armorYOffset.getValueInt());
        }

        if (isSelf && showGapple.getValue()) {
            renderGapple(drawContext, rectX + GAPPLE_X_OFFSET, rectY + GAPPLE_Y_OFFSET);
        }
    }

    private void renderGapple(DrawContext drawContext, int x, int y) {
        MatrixStack matrices = drawContext.getMatrices();
        Render2DUtil.drawRoundedRect(matrices, x, y, GAPPLE_BOX_SIZE, GAPPLE_BOX_SIZE, GAPPLE_RADIUS, GAPPLE_BG_COLOR);
        int gappleCount = countEnchantedGoldenApples();
        float scaledIconSize = 16 * GAPPLE_ICON_SCALE;
        float iconX = x + (GAPPLE_BOX_SIZE - scaledIconSize) / 2.0f;
        float iconY = y + (GAPPLE_BOX_SIZE - scaledIconSize) / 2.0f;

        matrices.push();
        matrices.translate(iconX, iconY, 0);
        matrices.scale(GAPPLE_ICON_SCALE, GAPPLE_ICON_SCALE, 1.0f);
        drawContext.drawItem(ENCHANTED_GOLDEN_APPLE, 0, 0);
        matrices.pop();
        if (gappleCount > 0) {
            String countText = String.valueOf(gappleCount);
            int textWidth = mc.textRenderer.getWidth(countText);
            float textX = x + (GAPPLE_BOX_SIZE - textWidth) / 2.0f;
            float textY = y - 10;
            drawScaledText(drawContext, countText, textX, textY, GAPPLE_TEXT_COLOR, 1.0f, true);
        }
    }

    private int countEnchantedGoldenApples() {
        if (mc.player == null) return 0;

        int count = 0;

        for (int i = 0; i < mc.player.getInventory().main.size(); i++) {
            ItemStack stack = mc.player.getInventory().main.get(i);
            if (stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
                count += stack.getCount();
            }
        }

        ItemStack offhand = mc.player.getOffHandStack();
        if (offhand.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
            count += offhand.getCount();
        }

        return count;
    }

    private void renderArmorAndDurability(DrawContext drawContext, AbstractClientPlayerEntity player, int x, int y) {
        MatrixStack matrices = drawContext.getMatrices();
        float scale = armorScale.getValueFloat();

        matrices.push();
        matrices.translate(x, y, 0);
        matrices.scale(scale, scale, 1.0f);

        int xOffset = 0;
        for (int i = 3; i >= 0; i--) {
            ItemStack armorStack = player.getInventory().getArmorStack(i);
            if (!armorStack.isEmpty()) {
                if (showDurability.getValue() && armorStack.isDamageable()) {
                    int maxDamage = armorStack.getMaxDamage();
                    int currentDamage = armorStack.getDamage();
                    int durabilityPercent = (int) (((float) (maxDamage - currentDamage) / maxDamage) * 100);

                    Color durColor = getDurabilityColor(durabilityPercent);
                    String durText = durabilityPercent + "%";
                    matrices.push();
                    float textPaddingX = 8.0f - (mc.textRenderer.getWidth(durText) / 2.0f);
                    matrices.translate(xOffset + textPaddingX, -9.5f, 0);
                    drawContext.drawText(mc.textRenderer, durText, 0, 0, durColor.getRGB(), true);
                    matrices.pop();
                }

                drawContext.drawItem(armorStack, xOffset, 0);
                xOffset += 18;
            }
        }

        matrices.pop();
    }

    private Color getDurabilityColor(int percent) {
        if (percent > 75) return Color.GREEN;
        if (percent > 45) return Color.YELLOW;
        if (percent > 20) return Color.ORANGE;
        return Color.RED;
    }

    private String format(float value) {
        return value == (int) value ? String.valueOf((int) value) : String.format("%.1f", value);
    }

    private void drawOutline(MatrixStack matrices, float x, float y, float width, float height, int thickness, Color color) {
        Render2DUtil.drawRect(matrices, x, y, width, thickness, color);
        Render2DUtil.drawRect(matrices, x, y + height - thickness, width, thickness, color);
        Render2DUtil.drawRect(matrices, x, y, thickness, height, color);
        Render2DUtil.drawRect(matrices, x + width - thickness, y, thickness, height, color);
    }

    private void drawScaledText(DrawContext drawContext, String text, float x, float y, Color color, float scale, boolean shadow) {
        MatrixStack matrices = drawContext.getMatrices();
        matrices.push();
        matrices.translate(x, y, 0.0f);
        matrices.scale(scale, scale, 1.0f);
        if (FontManager.isCustom()) {
            FontManager.get().drawString(matrices, text, 0, 0, color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f, shadow);
        } else {
            drawContext.drawText(mc.textRenderer, text, 0, 0, color.getRGB(), shadow);
        }
        matrices.pop();
    }

    private void drawPlayerHead(DrawContext drawContext, AbstractClientPlayerEntity player, int x, int y, float size) {
        Identifier skin = player.getSkinTextures().texture();
        MatrixStack matrices = drawContext.getMatrices();
        float scale = size / 8.0f;
        matrices.push();
        matrices.translate(x, y, 0.0f);
        matrices.scale(scale, scale, 1.0f);
        drawContext.drawTexture(skin, 0, 0, 8.0f, 8.0f, 8, 8, 64, 64);
        drawContext.drawTexture(skin, 0, 0, 40.0f, 8.0f, 8, 8, 64, 64);
        matrices.pop();
    }
}