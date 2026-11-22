/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.DrawContext
 */
package dev.g255.arc.mod.gui.items.buttons;

import dev.g255.arc.api.utils.render.ColorUtil;
import dev.g255.arc.api.utils.render.Render2DUtil;
import dev.g255.arc.mod.gui.clickgui.ClickGuiScreen;
import dev.g255.arc.mod.gui.items.Component;
import dev.g255.arc.mod.gui.items.Item;
import dev.g255.arc.mod.modules.impl.client.ClickGui;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;

public class Button
extends Item {
    private boolean state;
    public static int hoverColor = -2007673515;
    public static int defaultColor = 0x11555555;
    public static int defaultTextColor = -5592406;
    public static int enableTextColor = -1;

    /** 上一帧是否已经悬停：只在「进入」那一刻播一次，不然每帧都响 */
    private boolean hoverSoundActive;

    public Button(String name) {
        super(name);
        this.setHeight(15);
    }

    /** 悬停音（界面音效模块 → Sound/Pitch/Volume），父级遍历时每帧调一次 */
    @Override
    public void tickHoverSound(int mouseX, int mouseY) {
        boolean hovered = this.isHovering(mouseX, mouseY);
        if (hovered && !this.hoverSoundActive) {
            dev.g255.arc.mod.modules.impl.client.HoverSound.onHover();
        }
        this.hoverSoundActive = hovered;
    }

    @Override
    public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        this.tickHoverSound(mouseX, mouseY);
        boolean hovered = this.isHovering(mouseX, mouseY);
        float h = (float)this.height - 0.5f;
        if (this.getState()) {
            int a = hovered ? ClickGui.getInstance().hoverAlpha.getValueInt() : ClickGui.getInstance().alpha.getValueInt();
            double baseDelay = this.getColorDelay();
            if (ClickGui.getInstance().colorMode.getValue() == ClickGui.ColorMode.Spectrum) {
                Render2DUtil.drawLutRect(context.getMatrices(), this.x, this.y, (float)this.width, h, ClickGui.getInstance().getSpectrumLutId(), ClickGui.getInstance().getSpectrumLutHeight(), a);
            } else {
                Color color = ClickGui.getInstance().getActiveColor(baseDelay);
                Render2DUtil.rect(context.getMatrices(), this.x, this.y, this.x + (float)this.width, this.y + (float)this.height - 0.5f, ColorUtil.injectAlpha(color, a).getRGB());
            }
        } else {
            Render2DUtil.rect(context.getMatrices(), this.x, this.y, this.x + (float)this.width, this.y + (float)this.height - 0.5f, !hovered ? defaultColor : hoverColor);
        }
        float textY = this.getCenteredTextY(this.y, (float)this.height - 0.5f);
        this.drawString(this.getName(), (double)(this.x + 2.3f), (double)textY, this.getState() ? enableTextColor : defaultTextColor);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
            this.onMouseClick();
        }
    }

    public void onMouseClick() {
        this.state = !this.state;
        this.toggle();
        Button.sound();
    }

    public void toggle() {
    }

    public boolean getState() {
        return this.state;
    }

    @Override
    public int getHeight() {
        return this.height - 1;
    }

    public boolean isHovering(int mouseX, int mouseY) {
        for (Component component : ClickGuiScreen.getInstance().getComponents()) {
            if (!component.drag) continue;
            return false;
        }
        return (float)mouseX >= this.getX() && (float)mouseX <= this.getX() + (float)this.getWidth() && (float)mouseY >= this.getY() && (float)mouseY <= this.getY() + (float)this.height - 1.0f;
    }
}

