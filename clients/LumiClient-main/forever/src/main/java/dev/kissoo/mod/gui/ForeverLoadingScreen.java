package dev.kissoo.mod.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ForeverLoadingScreen extends Screen {

    private static final int BAR_W = 320;
    private static final int BAR_H = 6;
    private static final long TOTAL_MS = 10000L;

    private long startTime = -1;
    private float progress = 0f;
    private boolean transitioning = false;

    public ForeverLoadingScreen() {
        super(Text.literal("Loading"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (startTime < 0) startTime = System.currentTimeMillis();

        long elapsed = System.currentTimeMillis() - startTime;
        float target = Math.min(1f, (float) elapsed / TOTAL_MS);
        progress += (target - progress) * 0.08f;

        context.fillGradient(0, 0, this.width, this.height, 0xFF0A0A12, 0xFF131320);

        int cx = this.width / 2;
        int cy = this.height / 2;

        // ── 正上方：感谢名单 ──────────────────────────────────────────
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("感谢名单"), cx, 16, 0xFFE0E0FF);

        // ── 中间：owner 名（SleepingPill / TestIRC）──────────────────
        String owner = "NekoTeam";
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal(owner), cx, cy - 8, 0xFF9999FF);

        // ── owner 下方：感谢文字 ──────────────────────────────────────
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("此界面是混淆者为您做的感谢界面愿您2b2t游玩顺利"),
                cx, cy + 12, 0xFFAAAAAA);

        // ── 右下角：仅你可见提示 ──────────────────────────────────────
        String hint = "此界面仅你可见";
        int hintX = this.width - this.textRenderer.getWidth(hint) - 8;
        context.drawTextWithShadow(this.textRenderer,
                Text.literal(hint), hintX, this.height - 32, 0xFF555577);

        // ── 底部进度条 ────────────────────────────────────────────────
        int barX = cx - BAR_W / 2;
        int barY = this.height - 14;
        context.fill(barX - 1, barY - 1, barX + BAR_W + 1, barY + BAR_H + 1, 0xFF1A1A2E);
        context.fill(barX, barY, barX + BAR_W, barY + BAR_H, 0xFF0D0D1A);
        int fillW = (int) (BAR_W * progress);
        if (fillW > 0) {
            context.fillGradient(barX, barY, barX + fillW, barY + BAR_H, 0xFF5555FF, 0xFF9955FF);
        }

        // 百分比 — 进度条左侧
        context.drawTextWithShadow(this.textRenderer,
                Text.literal((int) (progress * 100) + "%"),
                barX, barY - 11, 0xFF6666AA);

        super.render(context, mouseX, mouseY, delta);

        if (!transitioning && progress >= 0.999f && elapsed >= TOTAL_MS) {
            transitioning = true;
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.execute(() -> mc.setScreen(new ForeverTitleScreen()));
        }
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }
}
