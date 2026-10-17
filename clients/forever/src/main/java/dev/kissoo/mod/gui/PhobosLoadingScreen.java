package dev.kissoo.mod.gui;

import dev.kissoo.mod.gui.background.PhobosBackground;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class PhobosLoadingScreen extends Screen {
    private static boolean initialized;

    private final long start;
    private float alpha;

    public PhobosLoadingScreen() {
        super(Text.literal(""));
        this.start = System.currentTimeMillis();
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static void reset() {
        initialized = false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        float target = this.start + 6000L >= now ? 1.0F : 0.0F;
        updateAlpha(target);

        PhobosBackground.getInstance().renderLoading(context, this.alpha);

        if (this.alpha <= 0.05F && this.start + 8000L < now && this.client != null) {
            this.client.setScreen(new ForeverTitleScreen());
        }
    }

    private void updateAlpha(float target) {
        float direction = target > this.alpha ? 1.0F : -1.0F;
        float distance = Math.abs(target - this.alpha);
        float step = Math.min(10.0F, distance * 0.02F) * Math.max(0.0F, MinecraftClient.getInstance().getRenderTickCounter().getLastFrameDuration()) * 6.7F;
        if (distance <= 0.001F || step >= distance) {
            this.alpha = target;
        } else {
            this.alpha += direction * step;
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
