package dev.G255.Lumi.mod.modules.impl.hud;

import dev.G255.Lumi.api.utils.player.InventoryUtil;
import dev.G255.Lumi.core.impl.FontManager;
import dev.G255.Lumi.mod.modules.HudModule;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

public class ItemCounterHudModule extends HudModule {
    // åä¸ªç©åæ¨¡å¼ç¨çå æ åè®¡æ°æä¾è?
    private final ItemStack stack;
    private final IntSupplier countSupplier;
    private final boolean useManager;

    // åºå®ç©ååè¡¨ï¼useManager = true æ¶ä½¿ç¨ï¼
    private static final List<Item> FIXED_ITEMS = List.of(
            Items.END_CRYSTAL,
            Items.OBSIDIAN,
            Items.RESPAWN_ANCHOR,
            Items.GLOWSTONE,
            Items.TOTEM_OF_UNDYING,
            Items.GOLDEN_APPLE,
            Items.ENCHANTED_GOLDEN_APPLE,
            Items.EXPERIENCE_BOTTLE,
            Items.FIREWORK_ROCKET
    );

    // ========== åè¡¨æ¨¡å¼è®¾ç½®ï¼ä¸è¡ä¸ä¸ªï¼ç´æ¥åå§åï¼ ==========
    private final BooleanSetting showCrystal = this.add(new BooleanSetting("Crystal", true));
    private final BooleanSetting showObsidian = this.add(new BooleanSetting("Obsidian", true));
    private final BooleanSetting showAnchor = this.add(new BooleanSetting("Anchor", true));
    private final BooleanSetting showGlowstone = this.add(new BooleanSetting("Glowstone", true));
    private final BooleanSetting showTotem = this.add(new BooleanSetting("Totem", true));
    private final BooleanSetting showGapple = this.add(new BooleanSetting("Gapple", true));
    private final BooleanSetting showGodApple = this.add(new BooleanSetting("GodApple", true));
    private final BooleanSetting showExp = this.add(new BooleanSetting("Exp", true));
    private final BooleanSetting showFirework = this.add(new BooleanSetting("Firework", true));
    private final SliderSetting perRow = this.add(new SliderSetting("PerRow", 8, 1, 20));
    private final BooleanSetting showZero = this.add(new BooleanSetting("ShowZero", false));

    // ----- æé?ï¼åä¸ªç©åï¼ä¿çï¼?-----
    public ItemCounterHudModule(String name, String chinese, Item item, int defaultX, int defaultY) {
        this(name, chinese, item, defaultX, defaultY, () -> InventoryUtil.getItemCount(item));
    }

    public ItemCounterHudModule(String name, String chinese, Item item, int defaultX, int defaultY, IntSupplier countSupplier) {
        super(name, chinese, defaultX, defaultY);
        this.stack = new ItemStack(item);
        this.countSupplier = countSupplier;
        this.useManager = false;
        // åè¡¨æ¨¡å¼çè®¾ç½®è½ç¶å·²åå§åï¼ä½?useManager=false æ¶ä¸ä¼è¢«ä½¿ç¨ï¼æ éç½?null
    }

    // ----- æé?ï¼åºå®åè¡¨æ¨¡å¼ï¼ä¸»ç¨ï¼?-----
    public ItemCounterHudModule(String name, String chinese, int defaultX, int defaultY) {
        super(name, chinese, defaultX, defaultY);
        this.stack = new ItemStack(Items.AIR);
        this.countSupplier = null;
        this.useManager = true;
        // ææè®¾ç½®å·²å¨å­æ®µå£°æå¤åå§åï¼æé ä¸­æ éé¢å¤ä»£ç 
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        // åä¸ªç©åæ¨¡å¼
        if (!this.useManager) {
            int count = this.countSupplier.getAsInt();
            if (count <= 0) {
                this.clearHudBounds();
                return;
            }
            this.stack.setCount(count);
            int px = this.getHudRenderX(16);
            int py = this.getHudRenderY(16);
            this.setHudBounds(px, py, 16, 16);
            drawContext.drawItem(this.stack, px, py);
            if (HudSetting.useFont()) {
                String s = String.valueOf(count);
                int tx = px + 16 - (int) Math.ceil(FontManager.ui.getWidth(s));
                int ty = py + 16 - (int) Math.ceil(FontManager.ui.getFontHeight());
                drawContext.getMatrices().push();
                drawContext.getMatrices().translate(0.0f, 0.0f, 200.0f);
                FontManager.ui.drawString(drawContext.getMatrices(), s, tx + 1, ty + 1, -1, HudSetting.useShadow());
                drawContext.getMatrices().pop();
            } else {
                drawContext.drawItemInSlot(mc.textRenderer, this.stack, px, py);
            }
            return;
        }

        // åºå®åè¡¨æ¨¡å¼
        List<ItemStack> stacks = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();

        for (int i = 0; i < FIXED_ITEMS.size(); i++) {
            Item item = FIXED_ITEMS.get(i);
            BooleanSetting switchSetting = getSwitchForIndex(i);
            if (switchSetting != null && !switchSetting.getValue()) {
                continue;
            }
            int count = InventoryUtil.getItemCount(item);
            if (count <= 0 && !this.showZero.getValue()) {
                continue;
            }
            ItemStack stack = new ItemStack(item);
            stack.setCount(Math.max(1, count));
            stacks.add(stack);
            counts.add(count);
        }

        if (stacks.isEmpty()) {
            this.clearHudBounds();
            return;
        }

        int perRowValue = Math.max(1, this.perRow.getValueInt());
        int rows = (stacks.size() + perRowValue - 1) / perRowValue;
        int columns = Math.min(perRowValue, stacks.size());
        int width = columns * 18;
        int height = rows * 18;

        int px = this.getHudRenderX(width);
        int py = this.getHudRenderY(height);
        this.setHudBounds(px, py, width, height);

        for (int i = 0; i < stacks.size(); i++) {
            int row = i / perRowValue;
            int col = i % perRowValue;
            int itemX = px + col * 18;
            int itemY = py + row * 18;
            ItemStack stack = stacks.get(i);
            int count = counts.get(i);

            if (HudSetting.useFont()) {
                drawContext.drawItem(stack, itemX, itemY);
                String s = String.valueOf(count);
                int tx = itemX + 16 - (int) Math.ceil(FontManager.ui.getWidth(s));
                int ty = itemY + 16 - (int) Math.ceil(FontManager.ui.getFontHeight());
                drawContext.getMatrices().push();
                drawContext.getMatrices().translate(0.0f, 0.0f, 200.0f);
                FontManager.ui.drawString(drawContext.getMatrices(), s, tx + 1, ty + 1, -1, HudSetting.useShadow());
                drawContext.getMatrices().pop();
            } else {
                drawContext.drawItemInSlot(mc.textRenderer, stack, itemX, itemY);
            }
        }
    }

    // è¾å©æ¹æ³ï¼æ ¹æ®ç´¢å¼è·åå¯¹åºçå¼å?
    private BooleanSetting getSwitchForIndex(int index) {
        switch (index) {
            case 0: return showCrystal;
            case 1: return showObsidian;
            case 2: return showAnchor;
            case 3: return showGlowstone;
            case 4: return showTotem;
            case 5: return showGapple;
            case 6: return showGodApple;
            case 7: return showExp;
            case 8: return showFirework;
            default: return null;
        }
    }
}