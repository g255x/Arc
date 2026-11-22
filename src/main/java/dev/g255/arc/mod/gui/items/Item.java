/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.DrawContext
 *  net.minecraft.client.sound.PositionedSoundInstance
 *  net.minecraft.client.sound.SoundInstance
 *  net.minecraft.registry.entry.RegistryEntry
 *  net.minecraft.sound.SoundEvents
 */
package dev.g255.arc.mod.gui.items;

import dev.g255.arc.api.utils.math.Animation;
import dev.g255.arc.api.utils.math.Easing;
import dev.g255.arc.core.impl.FontManager;
import dev.g255.arc.mod.Mod;
import dev.g255.arc.mod.modules.impl.client.ClickGui;
import java.awt.Color;
import java.util.Objects;
import net.minecraft.client.gui.DrawContext;

public class Item
extends Mod {
    public static DrawContext context;
    protected float x;
    protected float y;
    protected int width;
    protected int height;
    private boolean hidden;
    private final Animation visibleHeightAnimation = new Animation();

    public Item(String name) {
        super(name);
    }

    public void setLocation(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public static void sound() {
        // 统一走 ClickGui 的「界面音效」开关（GuiSound）；以前这里认另一个 Sound 开关，
        // 两个开关状态不一致时就会出现「界面音效明明开着却没声音」。
        if (ClickGui.getInstance() != null) {
            ClickGui.getInstance().playGuiSound();
        }
    }

    public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
    }

    /** 每帧调用一次，默认什么都不做；按钮类用它来播「界面音效」的悬停音 */
    public void tickHoverSound(int mouseX, int mouseY) {
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    }

    public void mouseReleased(int mouseX, int mouseY, int releaseButton) {
    }

    public void update() {
    }

    public void onKeyTyped(char typedChar, int keyCode) {
    }

    public void onKeyPressed(int key) {
    }

    /** 悬停说明：有内容才弹提示框，空串/null 表示这一项没有说明 */
    public String getTooltip() {
        return null;
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public int getWidth() {
        return this.width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return this.height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean isHidden() {
        return this.hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public double getVisibleHeight() {
        double target = this.hidden ? 0.0 : (double)this.getHeight();
        return this.visibleHeightAnimation.get(target, 200L, Easing.CubicInOut);
    }

    protected double getColorDelay() {
        return (double)this.y / 10.0;
    }

    protected void drawString(String text, double x, double y, Color color) {
        this.drawString(text, x, y, color.hashCode());
    }

    protected void drawString(String text, double x, double y, int color) {
        boolean shadow = FontManager.isShadowEnabled();
        if (FontManager.isCustomFontEnabled()) {
            FontManager.ui.drawString(context.getMatrices(), text, (double)((int)x), (double)((int)y), color, shadow);
        } else {
            context.drawText(Item.mc.textRenderer, text, (int)x, (int)y, color, shadow);
        }
    }

    protected int getFontHeight() {
        if (FontManager.isCustomFontEnabled()) {
            return (int)FontManager.ui.getFontHeight();
        }
        Objects.requireNonNull(Item.mc.textRenderer);
        return 9;
    }

    protected float getCenteredTextY(float baseY, float boxHeight) {
        return baseY + (boxHeight - (float)this.getFontHeight()) / 2.0f + (float)ClickGui.getInstance().textOffset.getValueInt();
    }

    protected int getWidth(String s) {
        if (FontManager.isCustomFontEnabled()) {
            return (int)FontManager.ui.getWidth(s);
        }
        return Item.mc.textRenderer.getWidth(s);
    }
}

