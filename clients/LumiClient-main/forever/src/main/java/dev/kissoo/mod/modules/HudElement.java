package dev.kissoo.mod.modules;

import net.minecraft.client.gui.DrawContext;

public interface HudElement {
    String getElementName();

    int getElementX();

    void setElementX(int x);

    int getElementY();

    void setElementY(int y);

    int getElementWidth();

    int getElementHeight();

    default int getDragBoundsHeight() {
        return getElementHeight();
    }

    boolean isElementVisible();

    void renderElement(DrawContext context, float tickDelta);

    default boolean isDraggable() {
        return true;
    }

    default AlignMode getAlignMode() {
        return AlignMode.LEFT;
    }

    default void setAlignMode(AlignMode mode) {
    }

    default int getRightAnchorX() {
        return getElementX() + getElementWidth();
    }

    default void setRightAnchorX(int rightX) {
    }

    enum AlignMode {
        LEFT, RIGHT
    }
}
