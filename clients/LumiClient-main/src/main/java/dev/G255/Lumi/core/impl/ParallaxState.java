package dev.G255.Lumi.core.impl;

public class ParallaxState {
    public static float parallaxOffsetX = 0.0F;
    public static float parallaxOffsetY = 0.0F;
    private static int lastMouseX = -1;
    private static int lastMouseY = -1;

    public static void updateMouseShake(int mouseX, int mouseY, int w, int h) {
        if (mouseX == lastMouseX && mouseY == lastMouseY) return;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        if (ConfigManager.menuMouseShake) {
            float strength = ConfigManager.menuMouseShakeStrength;
            int clampedX = Math.max(0, Math.min(mouseX, w));
            int clampedY = Math.max(0, Math.min(mouseY, h));
            float targetX = (float) (clampedX - w / 2) / 60.0F * strength;
            float targetY = (float) (clampedY - h / 2) / 60.0F * strength;
            if (ConfigManager.menuMouseShakeReverse) {
                targetX = -targetX;
                targetY = -targetY;
            }

            parallaxOffsetX += (targetX - parallaxOffsetX) * 0.15F;
            parallaxOffsetY += (targetY - parallaxOffsetY) * 0.15F;
        } else {
            parallaxOffsetX = 0.0F;
            parallaxOffsetY = 0.0F;
        }
    }
}