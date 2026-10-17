package dev.perish.mod.modules.impl.hud;

import dev.perish.api.utils.render.Render2DUtil;
import dev.perish.core.impl.FontManager;
import dev.perish.mod.gui.hud.IHudPosition;
import dev.perish.mod.gui.hud.HudEditorManager;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.ColorSetting;
import java.awt.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.gui.DrawContext;

public class TargetHudDisplay extends Module implements IHudPosition {
    public static TargetHudDisplay INSTANCE;
    public final BooleanSetting font = this.add(new BooleanSetting("Font", true));
    public final ColorSetting color = this.add(new ColorSetting("Color", new Color(80, 160, 255)));
    private float animHealth = 1.0f;
    private float animDelayed = 1.0f;
    private long lastUpdate;
    private float hudX, hudY, hudW, hudH;

    public TargetHudDisplay() {
        super("TargetHud", Category.Hud);
        this.setChinese("目标显示");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        hudX = mc.getWindow().getScaledWidth() / 2f + 80f;
        hudY = mc.getWindow().getScaledHeight() / 2f - 25f;
    }

    @Override
    public void onRender2D(DrawContext context, float tickDelta) {
        if (!isOn() || nullCheck()) return;

        boolean editMode = HudEditorManager.INSTANCE.isEditMode();

        PlayerEntity target = findTarget();
        if (target == null && !editMode) { hudW = 0; hudH = 0; return; }

        String name;
        float health, maxHealth, pct;
        boolean isTestCard = false;

        if (target != null) {
            name = target.getName().getString();
            health = target.getHealth() + target.getAbsorptionAmount();
            maxHealth = target.getMaxHealth();
            pct = Math.max(0, Math.min(1, health / maxHealth));
        } else {
            name = "TestPlayer";
            health = 18.5f;
            maxHealth = 20.0f;
            pct = 0.925f;
            isTestCard = true;
        }

        long now = System.currentTimeMillis();
        if (now - lastUpdate > 50) lastUpdate = now;
        animHealth += (pct - animHealth) * 0.15f;
        animDelayed += (pct - animDelayed) * 0.08f;

        float bw = 140, bh = 40, pad = 5;

        Render2DUtil.drawRoundedRect(context.getMatrices(), hudX, hudY, bw, bh, 6f,
                new Color(12, 12, 18, isTestCard ? 200 : 180));

        int accent = this.color.getValue().getRGB();
        Render2DUtil.drawRect(context.getMatrices(), hudX, hudY, 2, bh, accent);

        int nameColor = isTestCard ? new Color(140, 140, 150).getRGB() : this.color.getValue().getRGB();
        if (this.font.getValue()) FontManager.ui.drawString(context.getMatrices(), name, hudX + pad + 34, hudY + 4, nameColor, true);
        else context.drawText(mc.textRenderer, name, (int) (hudX + pad + 34), (int) (hudY + 4), nameColor, true);

        float barX = hudX + pad + 34;
        float barY = hudY + 16;
        float barW = bw - pad - 38;
        float barH = 4;

        Render2DUtil.drawRoundedRect(context.getMatrices(), barX, barY, barW, barH, 2f, new Color(40, 40, 50, 180));
        if (animDelayed > 0.01f)
            Render2DUtil.drawRoundedRect(context.getMatrices(), barX, barY, barW * animDelayed, barH, 2f, new Color(255, 82, 82, 150));
        if (animHealth > 0.01f) {
            Color hc = pct > 0.6f ? new Color(76, 175, 80) : pct > 0.3f ? new Color(255, 152, 0) : new Color(244, 67, 54);
            Render2DUtil.drawRoundedRect(context.getMatrices(), barX, barY, barW * animHealth, barH, 2f, hc);
        }
        String healthText = String.format("%.1f / %.1f", health, maxHealth);
        int healthColor = new Color(this.color.getValue().getRed(), this.color.getValue().getGreen(), this.color.getValue().getBlue(), isTestCard ? 120 : 180).getRGB();
        if (this.font.getValue()) FontManager.small.drawString(context.getMatrices(), healthText, barX, barY + barH + 1, healthColor, true);
        else context.drawText(mc.textRenderer, healthText, (int) barX, (int) (barY + barH + 1), healthColor, true);

        hudW = bw; hudH = bh;
    }

    private PlayerEntity findTarget() {
        PlayerEntity best = null;
        double minDist = Double.MAX_VALUE;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            double d = mc.player.distanceTo(p);
            if (d < 8 && d < minDist) { minDist = d; best = p; }
        }
        return best;
    }

    public float getHudX() { return hudX; }
    public float getHudY() { return hudY; }
    public float getHudW() { return hudW; }
    public float getHudH() { return hudH; }
    public void setHudPos(float x, float y) { this.hudX = x; this.hudY = y; }
}
