package dev.kissoo.mod.modules.impl.hud;

import dev.kissoo.api.utils.render.TextUtil;
import hoprc.api.core.impl.FontManager;
import dev.kissoo.mod.modules.HudElement;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.awt.*;

public class CoordsHud extends Module implements HudElement {
    public static CoordsHud INSTANCE;
    private int hudX = 2, hudY = -1;

    private final BooleanSetting shadow = add(new BooleanSetting("Shadow", true));
    private final ColorSetting color = add(new ColorSetting("Color", new Color(208, 0, 0)));

    private final BooleanSetting fakeCoords = add(new BooleanSetting("FakeCoords", false));
    private final SliderSetting offsetX = add(new SliderSetting("OffsetX", 0, -10000000, 10000000, 1, () -> fakeCoords.getValue()));
    private final SliderSetting offsetZ = add(new SliderSetting("OffsetZ", 0, -10000000, 10000000, 1, () -> fakeCoords.getValue()));
    private final SliderSetting safeRadius = add(new SliderSetting("SafeRadius", 1000, 0, 100000, 1, () -> fakeCoords.getValue()));

    public CoordsHud() {
        super("Coords", Category.HUD);
        setChinese("坐标显示");
        INSTANCE = this;
    }

    private String getText() {
        if (mc.player == null || mc.world == null) return "XYZ §f0, 0, 0";
        boolean inNether = mc.world.getRegistryKey().equals(World.NETHER);

        double dispX = mc.player.getX();
        double dispZ = mc.player.getZ();
        if (shouldFake()) {
            dispX += offsetX.getValue();
            dispZ += offsetZ.getValue();
        }

        int pX = MathHelper.floor(dispX);
        int pY = mc.player.getBlockY();
        int pZ = MathHelper.floor(dispZ);
        float factor = !inNether ? 0.125F : 8.0F;
        int aX = (int) (dispX * factor), aZ = (int) (dispZ * factor);
        return "XYZ §f" + pX + ", " + pY + ", " + pZ + " §7[§f" + aX + ", " + aZ + "§7]";
    }

    private boolean shouldFake() {
        if (!fakeCoords.getValue() || mc.player == null || mc.world == null) return false;
        // 非主世界（下界/末地）不受安全区限制，直接启用
        if (!mc.world.getRegistryKey().equals(World.OVERWORLD)) return true;
        double x = Math.abs(mc.player.getX());
        double z = Math.abs(mc.player.getZ());
        double r = safeRadius.getValue();
        // 主世界：x 或 z 任一在安全半径内则不启用（显示真实坐标）
        return !(x < r || z < r);
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) { renderElement(drawContext, tickDelta); }

    @Override public String getElementName() { return getChinese() != null ? getChinese() : getName(); }
    @Override public int getElementX() { return hudX; }
    @Override public void setElementX(int x) { hudX = x; }

    @Override
    public int getElementY() {
        if (hudY < 0) return mc.getWindow().getScaledHeight() - (int) FontManager.getFontHeight() - 2;
        return hudY;
    }

    @Override public void setElementY(int y) { hudY = y; }
    @Override public int getElementWidth() { return (int) FontManager.getWidth(getText()) + 4; }
    @Override public int getElementHeight() { return (int) FontManager.getFontHeight() + 2; }
    @Override public boolean isElementVisible() { return isOn(); }

    @Override
    public void renderElement(DrawContext context, float tickDelta) {
        TextUtil.drawString(context, getText(), getElementX(), getElementY(), color.getValue().getRGB(), shadow.getValue());
    }
}