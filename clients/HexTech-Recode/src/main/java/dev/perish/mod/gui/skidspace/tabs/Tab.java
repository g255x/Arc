package dev.perish.mod.gui.skidspace.tabs;

import net.minecraft.client.MinecraftClient;

public abstract class Tab {
    public static final int DEFAULT_HEADER_HEIGHT = 15;
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected final MinecraftClient mc = MinecraftClient.getInstance();

    public abstract void update(double mouseX, double mouseY, boolean mouseClicked);

    public void moveWindow(int deltaX, int deltaY) {
        this.x -= deltaX;
        this.y -= deltaY;
    }

    public int getX() {
        return this.x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return this.y;
    }

    public void setY(int y) {
        this.y = y;
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
}
