package dev.perish.mod.gui.hud;

import dev.perish.api.utils.Wrapper;
import dev.perish.core.impl.FontManager;
import java.awt.*;
import net.minecraft.client.gui.DrawContext;

public abstract class HudComponent implements Wrapper {
    private final String id;
    private final String name;

    protected float x;
    protected float y;
    protected float width;
    protected float height;
    protected boolean dragging;
    protected float dragOffsetX;
    protected float dragOffsetY;
    protected boolean visible = true;

    public HudComponent(String id, String name, float defaultX, float defaultY) {
        this.id = id;
        this.name = name;
        this.x = defaultX;
        this.y = defaultY;
    }

    public abstract void render(DrawContext context, float tickDelta);

    public void renderOverlay(DrawContext context, boolean selected) {
        if (width <= 0 || height <= 0) return;

        int bgColor = selected
                ? new Color(100, 149, 237, 60).getRGB()
                : new Color(255, 255, 255, 15).getRGB();
        int borderColor = selected
                ? new Color(100, 200, 255, 220).getRGB()
                : new Color(255, 255, 255, 80).getRGB();

        dev.perish.api.utils.render.Render2DUtil.drawRect(context.getMatrices(), x, y, width, height, bgColor);

        float t = selected ? 1.5f : 1.0f;
        dev.perish.api.utils.render.Render2DUtil.drawLine(context.getMatrices(), x, y, x + width, y, borderColor);
        dev.perish.api.utils.render.Render2DUtil.drawLine(context.getMatrices(), x + width, y, x + width, y + height, borderColor);
        dev.perish.api.utils.render.Render2DUtil.drawLine(context.getMatrices(), x, y + height, x + width, y + height, borderColor);
        dev.perish.api.utils.render.Render2DUtil.drawLine(context.getMatrices(), x, y, x, y + height, borderColor);

        String label = name;
        if (selected) {
            label += " [" + (int)(x) + "," + (int)(y) + "]";
        }
        float textWidth = FontManager.ui.getWidth(label);
        float labelY = y + height + 3;

        dev.perish.api.utils.render.Render2DUtil.drawRoundedRect(
                context.getMatrices(), x, labelY,
                textWidth + 6, FontManager.ui.getFontHeight() + 2,
                2.0f, new Color(20, 20, 25, 200)
        );
        FontManager.ui.drawString(context.getMatrices(), label, x + 3, labelY + 1,
                selected ? new Color(150, 210, 255).getRGB() : new Color(180, 180, 180).getRGB(), false);
    }

    public boolean isHovering(double mouseX, double mouseY) {
        if (!visible || width <= 0 || height <= 0) return false;
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean onMouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isHovering(mouseX, mouseY)) {
            dragging = true;
            dragOffsetX = (float) (mouseX - x);
            dragOffsetY = (float) (mouseY - y);
            return true;
        }
        return false;
    }

    public void onMouseReleased(int button) {
        if (button == 0) dragging = false;
    }

    public void onDrag(double mouseX, double mouseY) {
        if (!dragging) return;
        if (dev.perish.mod.gui.hud.HudEditorManager.INSTANCE.isEditMode()) {
            HudSnapHelper.INSTANCE.applySmoothDrag(this, mouseX, mouseY);
        } else {
            x = (float) (mouseX - dragOffsetX);
            y = (float) (mouseY - dragOffsetY);
            float sw = mc.getWindow().getScaledWidth();
            float sh = mc.getWindow().getScaledHeight();
            x = Math.max(0, Math.min(x, sw - width));
            y = Math.max(0, Math.min(y, sh - height));
        }
    }

    public void onScroll(double amount) {
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public boolean isVisible() { return visible; }
    public boolean isDragging() { return dragging; }

    public void setX(float x) { this.x = x; }
    public void setY(float y) { this.y = y; }
    public void setWidth(float w) { this.width = Math.max(w, 1); }
    public void setHeight(float h) { this.height = Math.max(h, 1); }
    public void setVisible(boolean v) { this.visible = v; }
}
