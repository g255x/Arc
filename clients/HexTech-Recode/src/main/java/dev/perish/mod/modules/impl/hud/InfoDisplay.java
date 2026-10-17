package dev.perish.mod.modules.impl.hud;

import dev.perish.Perish;
import dev.perish.api.utils.render.TextUtil;
import dev.perish.core.impl.FontManager;
import dev.perish.mod.gui.hud.IHudPosition;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.impl.client.HUD;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.ColorSetting;
import java.awt.Color;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;

public class InfoDisplay extends Module implements IHudPosition {
    public static InfoDisplay INSTANCE;
    public final BooleanSetting fps = this.add(new BooleanSetting("FPS", true));
    public final BooleanSetting ping = this.add(new BooleanSetting("Ping", true));
    public final BooleanSetting tps = this.add(new BooleanSetting("TPS", true));
    public final BooleanSetting speed = this.add(new BooleanSetting("Speed", true));
    public final BooleanSetting time = this.add(new BooleanSetting("Time", false));
    public final BooleanSetting ip = this.add(new BooleanSetting("IP", false));
    public final BooleanSetting packets = this.add(new BooleanSetting("Packets", false));
    public final BooleanSetting brand = this.add(new BooleanSetting("Brand", false));
    public final BooleanSetting font = this.add(new BooleanSetting("Font", false));
    public final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255)));

    private float hudX, hudY, hudW, hudH;
    private final DecimalFormat df = new DecimalFormat("0.0");

    public InfoDisplay() {
        super("InfoDisplay", Module.Category.Hud);
        this.setChinese("信息列表");
        INSTANCE = this;
    }

    @Override
    public void onRender2D(DrawContext context, float tickDelta) {
        if (nullCheck()) return;

        boolean useFont = this.font.getValue();
        boolean shadow = true;
        float fh = useFont ? FontManager.ui.getFontHeight() : 9;

        java.util.List<String> lines = new java.util.ArrayList<>();
        if (this.fps.getValue()) lines.add("FPS §f" + Perish.FPS.getFps());
        if (this.ping.getValue()) {
            PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            lines.add("Ping §f" + (e == null ? "Unknown" : e.getLatency() + "ms"));
        }
        if (this.tps.getValue()) lines.add("TPS §f" + Perish.SERVER.getTPS() + " [" + Perish.SERVER.getCurrentTPS() + "]");
        if (this.speed.getValue()) {
            double dx = mc.player.getX() - mc.player.prevX;
            double dz = mc.player.getZ() - mc.player.prevZ;
            double dist = Math.sqrt(dx * dx + dz * dz) / 1000.0;
            lines.add(String.format("Speed §f%skm/h", df.format(dist / 1.388888888888889E-5 * Perish.TIMER.get())));
        }
        if (this.time.getValue()) lines.add("Time §f" + new SimpleDateFormat("h:mm a", Locale.ENGLISH).format(new Date()));
        if (this.ip.getValue()) lines.add("Server §f" + (mc.isInSingleplayer() ? "SinglePlayer" : mc.getCurrentServerEntry().address));
        if (this.packets.getValue()) lines.add("Packets §f" + Perish.NETWORK.getIncomingPPS() + "<-" + Perish.NETWORK.getOutgoingPPS());
        if (this.brand.getValue()) lines.add("ServerBrand §f" + (mc.isInSingleplayer() ? "Vanilla" : mc.getNetworkHandler().getBrand().replaceAll("\\(.*?\\)", "")));

        if (lines.isEmpty()) { hudW = 0; hudH = 0; return; }

        if (hudX == 0 && hudY == 0) {
            hudX = mc.getWindow().getScaledWidth() - 110;
            hudY = mc.getWindow().getScaledHeight() - 80;
        }

        float maxW = 0;
        for (String s : lines) maxW = Math.max(maxW, useFont ? FontManager.ui.getWidth(s) : mc.textRenderer.getWidth(s));

        float cy = 0;
        for (String s : lines) {
            int color = this.color.getValue().getRGB();
            float sx = hudX + maxW - (useFont ? FontManager.ui.getWidth(s) : mc.textRenderer.getWidth(s));
            if (this.color.isGlobalPulse()) TextUtil.drawStringSetting(context, s, sx, hudY + cy, this.color, useFont, shadow);
            else if (useFont) FontManager.ui.drawString(context.getMatrices(), s, sx, hudY + cy, color, shadow);
            else context.drawText(mc.textRenderer, s, (int)sx, (int)(hudY + cy), color, shadow);
            cy += fh + 2;
        }

        hudW = maxW + 6;
        hudH = cy;
    }

    public float getHudX() { return hudX; }
    public float getHudY() { return hudY; }
    public float getHudW() { return hudW; }
    public float getHudH() { return hudH; }

    public void setHudPos(float x, float y) { this.hudX = x; this.hudY = y; }
}
